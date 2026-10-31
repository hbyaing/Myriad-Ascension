package cn.blockforge.generated.generatedmod.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Fixed-tick sword-tail history. Kept independent of rendering and Minecraft. */
public final class SwordFlightTrailHistory {
    public record Point(double x,double y,double z){
        public boolean finite(){return Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z);}
        public double distance(Point other){return Math.sqrt((x-other.x)*(x-other.x)+(y-other.y)*(y-other.y)+(z-other.z)*(z-other.z));}
        public Point lerp(Point other,double t){return new Point(x+(other.x-x)*t,y+(other.y-y)*t,z+(other.z-z)*t);}
    }
    public record Node(Point point,double tick,double arc){}
    private final double SPACING,LIFETIME,MAX_LENGTH;
    private final int CAPACITY;
    public SwordFlightTrailHistory(){this(.09,22,22,256);}
    public SwordFlightTrailHistory(double spacing,double lifetime,double length,int capacity){
        SPACING=spacing;LIFETIME=lifetime;MAX_LENGTH=length;CAPACITY=capacity;
    }
    private final ArrayList<Node> nodes=new ArrayList<>();
    private final List<Node> view=Collections.unmodifiableList(nodes);
    private final ArrayList<Node> presentation=new ArrayList<>();
    private double lastTick=Double.NEGATIVE_INFINITY,arc;
    public List<Node> nodes(){return view;}
    /** Clip at the render time and finish at the very same model attachment.
     * Interpolate arc too: dropping the newest tick must never reset the material phase. */
    public List<Node> renderNodes(double now,Point attachment){
        presentation.clear();
        if(!attachment.finite()||nodes.isEmpty())return presentation;
        int i=0;
        while(i<nodes.size()&&nodes.get(i).tick()<=now)presentation.add(nodes.get(i++));
        if(presentation.isEmpty())return presentation;
        Node last=presentation.get(presentation.size()-1);
        if(attachment.distance(last.point())<.00001){
            presentation.set(presentation.size()-1,new Node(attachment,last.tick(),last.arc()));
            return presentation;
        }
        double headArc=last.arc();
        if(i<nodes.size()){
            Node next=nodes.get(i);
            double fraction=Math.max(0,Math.min(1,(now-last.tick())/Math.max(1e-9,next.tick()-last.tick())));
            headArc+=(next.arc()-last.arc())*fraction;
        }else headArc+=attachment.distance(last.point());
        if(Math.abs(now-last.tick())<1e-8)presentation.remove(presentation.size()-1);
        presentation.add(new Node(attachment,now,headArc));
        return presentation;
    }
    public void sample(Point point,double tick){
        sample(point,tick,8);
    }
    /** The caller supplies a server-validated per-tick speed allowance, not a guessed global cap. */
    public void sample(Point point,double tick,double allowedStep){
        if(!point.finite()||!Double.isFinite(tick))return;
        if(tick<lastTick){nodes.clear();arc=0;}
        else if(tick==lastTick)return;
        double previousTick=lastTick;lastTick=tick;
        nodes.removeIf(n->tick-n.tick()>LIFETIME);
        if(nodes.isEmpty()){nodes.add(new Node(point,tick,arc));return;}
        var last=nodes.get(nodes.size()-1);
        double distance=last.point().distance(point);
        double permitted=Double.isFinite(allowedStep)&&allowedStep>0?allowedStep:8;
        if(distance>permitted*Math.max(1,tick-previousTick)){
            nodes.clear();arc=0;nodes.add(new Node(point,tick,arc));return;
        }
        // A stationary sword must not refresh the age of its old plume.
        if(distance<.002)return;
        int steps=Math.min(CAPACITY,(int)Math.ceil(distance/SPACING));
        double startTick=Math.max(last.tick(),previousTick);
        // Discard the prefix once, rather than shifting up to 256 entries for
        // each new sample during high-speed flight. Skip samples that would be
        // removed immediately by the length/capacity limits anyway.
        int first=Math.max(1,steps-CAPACITY+1);
        while(first<steps&&distance*(1-first/(double)steps)>MAX_LENGTH)first++;
        int keepFrom=0,added=steps-first+1;
        while(keepFrom<nodes.size()&&(nodes.size()-keepFrom+added>CAPACITY||last.arc()+distance-nodes.get(keepFrom).arc()>MAX_LENGTH))keepFrom++;
        if(keepFrom>0)nodes.subList(0,keepFrom).clear();
        for(int i=first;i<=steps;i++){
            double t=i/(double)steps;
            nodes.add(new Node(last.point().lerp(point,t),startTick+(tick-startTick)*t,last.arc()+distance*t));
        }
        arc+=distance;
    }
}
