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
    // Entity rendering still interpolates the previous fixed-tick interval.
    // Its tail may need nodes that the new fixed sample has already evicted.
    // Keep one bounded previous sample plus its cutoff predecessor, so the
    // exact render boundary can be interpolated even with irregular spacing.
    private final ArrayList<Node> previousNodes=new ArrayList<>();
    private final ArrayList<Node> renderSource=new ArrayList<>();
    private final ArrayList<Node> presentation=new ArrayList<>();
    private double lastTick=Double.NEGATIVE_INFINITY,arc;
    public List<Node> nodes(){return view;}
    /** Drawing stations belong to the moving head, not the fixed tick lattice.
     * Preserve the canonical polyline/clock and keep distant node identities.
     * Otherwise even a steady line changes its tapered triangles at 20Hz. */
    public static List<Node> uniformHead(List<Node> source,double spacing,double window){
        if(source.size()<2)return source;
        if(!(spacing>0)||!Double.isFinite(spacing)||!(window>0)||!Double.isFinite(window))
            throw new IllegalArgumentException("Invalid presentation grid");
        Node head=source.get(source.size()-1);
        double length=Math.min(window,Math.max(0,head.arc()-source.get(0).arc()));
        var result=new ArrayList<Node>();double cutoff=head.arc()-length;
        for(var n:source){if(n.arc()<cutoff-1e-9)result.add(n);else break;}
        int intervals=(int)Math.floor(length/spacing+1e-9),cursor=0;
        for(int i=intervals+1;i>=0;i--){
            double behind=i==intervals+1?length:i*spacing;
            if(i==intervals+1&&Math.abs(length-intervals*spacing)<1e-9)continue;
            double arc=head.arc()-behind;
            while(cursor<source.size()-2&&source.get(cursor+1).arc()<arc)cursor++;
            Node a=source.get(cursor),b=source.get(cursor+1);
            double t=Math.max(0,Math.min(1,(arc-a.arc())/Math.max(1e-12,b.arc()-a.arc())));
            result.add(new Node(a.point().lerp(b.point(),t),a.tick()+(b.tick()-a.tick())*t,arc));
        }
        return result;
    }
    /** Clip at the render time and finish at the very same model attachment.
     * Interpolate arc too: dropping the newest tick must never reset the material phase. */
    public List<Node> renderNodes(double now,Point attachment){
        presentation.clear();
        if(!attachment.finite()||nodes.isEmpty())return presentation;
        renderSource.clear();
        double firstArc=nodes.get(0).arc();
        for(var node:previousNodes)if(node.arc()<firstArc)renderSource.add(node);
        renderSource.addAll(nodes);
        int i=0;
        while(i<renderSource.size()&&renderSource.get(i).tick()<=now)presentation.add(renderSource.get(i++));
        if(presentation.isEmpty())return presentation;
        Node last=presentation.get(presentation.size()-1);
        double headArc=last.arc();
        boolean stationary=attachment.distance(last.point())<.00001;
        if(!stationary&&i<renderSource.size()){
            Node next=renderSource.get(i);
            double fraction=Math.max(0,Math.min(1,(now-last.tick())/Math.max(1e-9,next.tick()-last.tick())));
            headArc+=(next.arc()-last.arc())*fraction;
        }else if(!stationary)headArc+=attachment.distance(last.point());
        Node head=new Node(attachment,stationary?last.tick():now,headArc);
        if(stationary||Math.abs(now-last.tick())<1e-8)
            presentation.remove(presentation.size()-1);
        presentation.add(head);
        // Clip continuously at a fixed arc distance behind the live attachment.
        // Dropping whole nodes here made the near-head tangent jump at every
        // sample boundary; skipping exact tick endpoints reintroduced tiny edges.
        double joinArc=headArc-SPACING*2;
        int join=presentation.size()-2;
        while(join>0&&presentation.get(join).arc()>joinArc)join--;
        if(join>=0){
            Node a=presentation.get(join),b=presentation.get(join+1);
            double fraction=(joinArc-a.arc())/Math.max(1e-9,b.arc()-a.arc());
            presentation.subList(join+1,presentation.size()).clear();
            if(fraction>1e-8&&fraction<1-1e-8)
                presentation.add(new Node(a.point().lerp(b.point(),fraction),
                        a.tick()+(b.tick()-a.tick())*fraction,joinArc));
            else if(fraction>=1-1e-8&&b!=head)presentation.add(b);
            presentation.add(head);
        }
        // Clip against the interpolated head, not the newer simulation pose.
        // At 30 blocks/tick the old sampler cutoff removed 30 still-bright
        // world blocks at once while the rendered head had barely advanced.
        double tailArc=headArc-MAX_LENGTH,tailTick=now-LIFETIME;
        int first=0;
        while(first+1<presentation.size()&&(presentation.get(first+1).arc()<=tailArc
                ||presentation.get(first+1).tick()<=tailTick))first++;
        if(first>0)presentation.subList(0,first).clear();
        if(presentation.size()>1&&(presentation.get(0).arc()<tailArc||presentation.get(0).tick()<tailTick)){
            Node a=presentation.get(0),b=presentation.get(1);
            double fraction=Math.max((tailArc-a.arc())/Math.max(1e-9,b.arc()-a.arc()),
                    (tailTick-a.tick())/Math.max(1e-9,b.tick()-a.tick()));
            fraction=Math.max(0,Math.min(1,fraction));
            presentation.set(0,new Node(a.point().lerp(b.point(),fraction),
                    a.tick()+(b.tick()-a.tick())*fraction,a.arc()+(b.arc()-a.arc())*fraction));
        }
        return presentation;
    }
    public void sample(Point point,double tick){
        sample(point,tick,8);
    }
    /** The caller supplies a server-validated per-tick speed allowance, not a guessed global cap. */
    public void sample(Point point,double tick,double allowedStep){
        if(!point.finite()||!Double.isFinite(tick))return;
        if(tick<lastTick){nodes.clear();previousNodes.clear();arc=0;}
        else if(tick==lastTick)return;
        Node predecessor=null;
        if(!nodes.isEmpty())for(var node:previousNodes){
            if(node.arc()<nodes.get(0).arc())predecessor=node;else break;
        }
        previousNodes.clear();
        if(predecessor!=null)previousNodes.add(predecessor);
        previousNodes.addAll(nodes);
        double previousTick=lastTick;lastTick=tick;
        nodes.removeIf(n->tick-n.tick()>LIFETIME);
        if(nodes.isEmpty()){previousNodes.clear();nodes.add(new Node(point,tick,arc));return;}
        var last=nodes.get(nodes.size()-1);
        double distance=last.point().distance(point);
        double permitted=Double.isFinite(allowedStep)&&allowedStep>0?allowedStep:8;
        if(distance>permitted*Math.max(1,tick-previousTick)){
            nodes.clear();previousNodes.clear();arc=0;nodes.add(new Node(point,tick,arc));return;
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
