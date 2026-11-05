package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.yujian.HuahongSkin;
import cn.blockforge.generated.generatedmod.client.HuahongFlightPath.Frame;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Independent Forge geometry for the active huahong form families.
 * World history drives every tail; local sheets bend along that same path.
 * No reference-only V13 shader, carrier model or browser code is embedded. */
final class HuahongSkinGeometry {
    private record Row(Vec3 p,double u,double width,float alpha,int color,Vec3 side){}
    private record Birth(Vec3 p,double tick,double arc,Vec3 direction,long cell){}
    private record FanSample(Frame frame,double behind,double opacity){}
    private record Cloud(Vec3 p,double seed,double radius,float alpha,int color,int mode){}
    static void linkCarriers(){
        // Define these data carriers before cold world generation is competing
        // with first-use class loading. This performs no geometry or GPU work.
        for(Class<?> type:new Class<?>[]{Row.class,Birth.class,Cloud.class,HuahongFlowField.Smoke.class,HuahongFlowField.Parcel.class}){
            try{Class.forName(type.getName(),true,type.getClassLoader());}
            catch(ClassNotFoundException impossible){throw new IllegalStateException(impossible);}
        }
    }
    private final HuahongFlightPath path;
    private final VertexConsumer out;
    private final PoseStack.Pose pose;
    private final Vec3 eye,center,forward;
    private final Vec3 cameraRight,cameraUp;
    private final List<SwordFlightTrailHistory.Node> nodes;
    private final List<SwordFlightTrailHistory.Node> canonical;
    private final Frame[] historyFrames;
    private final double now,origin;
    private final float strength;
    private final HuahongSkin skin;
    private int vertices;
    private HuahongSkinGeometry(HuahongSkin skin,VertexConsumer out,PoseStack.Pose pose,Vec3 eye,Vec3 center,Vec3 forward,Vec3 headSide,
            List<SwordFlightTrailHistory.Node> nodes,List<SwordFlightTrailHistory.Node> canonical,double now,float strength){
        this.skin=skin;this.out=out;this.pose=pose;this.eye=eye;this.center=center;this.forward=forward;this.nodes=nodes;this.canonical=canonical;this.now=now;this.strength=strength;
        if(skin==HuahongSkin.SMOKE||skin==HuahongSkin.DENSE){
            // Use the submitted view transform. Camera's cached vectors may
            // precede Forge camera-angle overrides and leave smoke edge-on.
            var view=new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix()).mul(pose.pose());
            cameraRight=new Vec3(view.m00(),view.m10(),view.m20()).normalize();
            cameraUp=new Vec3(view.m01(),view.m11(),view.m21()).normalize();
        }else{cameraRight=cameraUp=null;}
        historyFrames=skin==HuahongSkin.DENSE||skin==HuahongSkin.FAN?null:new Frame[nodes.size()];
        // DENSE's sprite and FAN's world-birth films never query transport. SMOKE uses
        // at most24.05m, including its zero-alpha last station, with32m reserve.
        path=skin==HuahongSkin.DENSE||skin==HuahongSkin.FAN?null:new HuahongFlightPath(canonical,center,forward,headSide,
                skin==HuahongSkin.SMOKE?32:Double.POSITIVE_INFINITY,true,HuahongHeadBridge.attachmentWeight(strength));
        // Keep material transport independent of travel speed. Geometry still
        // follows recorded world points, while the surface's phase is measured
        // behind the rendered head and advected by explicit shader time.
        origin=nodes.isEmpty()?0:nodes.get(nodes.size()-1).arc();
    }
    static int draw(HuahongSkin skin,VertexConsumer out,PoseStack.Pose pose,Vec3 eye,Vec3 center,Vec3 forward,Vec3 headSide,
            List<SwordFlightTrailHistory.Node> nodes,List<SwordFlightTrailHistory.Node> canonical,double now,float strength){
        var g=new HuahongSkinGeometry(skin,out,pose,eye,center,forward,headSide,nodes,canonical,now,strength);
        switch(skin){
            case NEEDLE -> g.needle();
            case METEOR_FLAME -> g.fire(false);
            case FLAME -> g.fire(true);
            case FEATHER -> g.feather();
            case FAN -> g.fan();
            case SMOKE -> g.smoke();
            case DENSE -> g.dense();
            default -> throw new IllegalArgumentException("Prism has its own consumer");
        }
        return g.vertices;
    }
    private static double fade(double age,double life){return Math.pow(Mth.clamp(1-age/life,0,1),1.6);}
    private static double rnd(double x){double v=Math.sin(x*71.371+19.713)*15317.317;return v-Math.floor(v);}
    private static Vec3 vec(SwordFlightTrailHistory.Point p){return new Vec3(p.x(),p.y(),p.z());}
    private static Vec3 side(Vec3 f){return f.cross(Math.abs(f.y)>.95?new Vec3(0,0,1):new Vec3(0,1,0)).normalize();}
    private Frame frame(Vec3 p,Vec3 f){return path.frame(p,f);}
    private HuahongFlightPath.Frame at(int i){
        if(historyFrames[i]!=null)return historyFrames[i];
        var node=nodes.get(i);
        double behind=origin-node.arc();
        // Head stations have known arc distances. A nearest-position search can
        // select another arm of a tight turn even when their centers are rigid.
        Frame frame=path.behind(behind);
        return historyFrames[i]=frame;
    }
    /** Walk the recorded path backwards; turn local flames with the trail, not a rigid spine. */
    private HuahongFlightPath.Frame back(double distance){return path.behind(distance);}
    private Frame axial(double x){
        if(x<=0)return back(-x);
        Frame head=back(0);return new Frame(head.p().add(head.forward().scale(x)),head.forward(),head.side(),head.up());
    }
    private Vec3 offset(HuahongFlightPath.Frame f,double x,double y){return f.p().add(f.side().scale(x*HuahongHeadBridge.BODY_SCALE)).add(f.up().scale(y*HuahongHeadBridge.BODY_SCALE));}
    private static double smooth(double a,double b,double x){double q=Mth.clamp((x-a)/(b-a),0,1);return q*q*(3-2*q);}
    private static double curve(double a,double b,double c,double q){return a*(1-q)*(1-q)+2*b*q*(1-q)+c*q*q;}
    private double headArc(){return nodes.isEmpty()?0:nodes.get(nodes.size()-1).arc();}
    private static int mix(int a,int b,double t){t=Mth.clamp(t,0,1);return ((int)(((a>>16)&255)*(1-t)+((b>>16)&255)*t)<<16)|((int)(((a>>8)&255)*(1-t)+((b>>8)&255)*t)<<8)|(int)((a&255)*(1-t)+(b&255)*t);}
    private List<Row> history(double life,double width,int color,int strand,double spread,boolean oriented){
        return history(life,width,color,strand,spread,oriented,0);
    }
    private List<Row> history(double life,double width,int color,int strand,double spread,boolean oriented,double visualSpeed){
        List<Row> rows=new ArrayList<>();
        for(int i=0;i<nodes.size();i++){
            var n=nodes.get(i);double age=(now-n.tick())/20;
            if(age<0)continue;
            double behind=headArc()-n.arc();
            if(skin==HuahongSkin.NEEDLE)age=HuahongHeadBridge.luminousSupportAge(age,behind);
            if(visualSpeed>0)age=Math.max(age,(headArc()-n.arc())/visualSpeed);
            if(age<0||age>=life)continue;
            Frame f=at(i);double phase=strand*2.399+Math.sin((n.arc()-origin)*.47+strand)*.28;
            double open=(1-Math.exp(-age*5))*spread;
            Vec3 normal=f.side().scale(Math.cos(phase)).add(f.up().scale(Math.sin(phase)));
            double join=skin==HuahongSkin.NEEDLE?HuahongHeadBridge.luminousCoreJoin(behind):smooth(1.5,3.5,behind);
            rows.add(new Row(f.p().add(normal.scale(open*HuahongHeadBridge.BODY_SCALE)),n.arc()-origin,width*(.18+.82*fade(age,life)),(float)(fade(age,life)*join),color,oriented?normal:null));
        }
        return rows;
    }
    private static List<Row> opacity(List<Row> rows,float opacity){
        var scaled=new ArrayList<Row>(rows.size());
        for(var r:rows)scaled.add(new Row(r.p(),r.u(),r.width(),r.alpha()*opacity,r.color(),r.side()));
        return scaled;
    }
    private void nucleus(int color,double length,double width,float opacity){
        List<Row> rows=new ArrayList<>();
        for(int i=0;i<=36;i++){
            double q=i/36.0;Frame f=back(q*length);
            // The white maximum belongs to a short pointed ridge, not a second
            // broad spindle pasted over every form family.
            rows.add(new Row(f.p().add(f.forward().scale(.18*(1-q))),-q*length,width*Math.pow(Math.max(0,Math.sin(Math.PI*q)),1.1)*Math.exp(-q*1.8),strength*opacity*(float)Math.pow(1-q,1.2),color,f.side()));
        }
        strip(rows,22,true);
    }
    private void needle(){
        // A single centreline and width envelope owns both nose and history.
        // Separate crossed local spindles created a view-dependent thin neck.
        double half=.026+.14*.20;
        for(int layer=0;layer<3;layer++){
            double width=half*(layer==0?4.8:layer==1?1.35:.23);
            int color=layer==0?0xffca74:layer==1?0xffd69b:0xfff3d6;
            float opacity=layer==0?.05F:layer==1?.40F:.63F;
            var rows=new ArrayList<Row>();
            for(int i=0;i<nodes.size();i++){
                var n=nodes.get(i);double d=headArc()-n.arc(),age=HuahongHeadBridge.luminousSupportAge((now-n.tick())/20,d);
                if(age<0||age>=5.8)continue;
                rows.add(new Row(at(i).p(),n.arc()-origin,width*HuahongHeadBridge.luminousEnvelope(d),strength*opacity*(float)fade(age,5.8),color,null));
            }
            Frame head=back(0);
            for(int i=1;i<=8;i++){double d=-.65*i/8;rows.add(new Row(head.p().subtract(head.forward().scale(d)),-d,width*HuahongHeadBridge.luminousEnvelope(d),strength*opacity,color,null));}
            strip(rows,layer==0?10:layer==1?34:22);
        }
    }
    private void fire(boolean torn){if(torn)tornFlameV3();else meteorFlameV9();}
    private static double formHash(double seed){double n=Math.sin(seed*127.13+173.71)*43758.5453;return n-Math.floor(n);}
    private static double fireNoise(double x,int salt){
        double cell=Math.floor(x),f=x-cell,q=f*f*f*(f*(f*6-15)+10);
        double a=Math.sin((cell+173*17+salt*61)*127.13)*43758.5453;
        double b=Math.sin((cell+1+173*17+salt*61)*127.13)*43758.5453;
        return ((a-Math.floor(a))*2-1)*(1-q)+((b-Math.floor(b))*2-1)*q;
    }
    private List<Birth> timedBirths(double interval,double life){
        var result=new ArrayList<Birth>();if(canonical.size()<2)return result;
        double step=interval*20,start=Math.max(canonical.get(0).tick(),now-life*20);int cursor=0;
        for(long cell=(long)Math.ceil(start/step);cell*step<=now;cell++){
            double tick=cell*step;while(cursor<canonical.size()-2&&canonical.get(cursor+1).tick()<tick)cursor++;
            var a=canonical.get(cursor);var b=canonical.get(cursor+1);double duration=b.tick()-a.tick();if(duration<1e-8||tick>b.tick())continue;
            double t=Mth.clamp((tick-a.tick())/duration,0,1);Vec3 direction=vec(b.point()).subtract(vec(a.point()));
            if(direction.lengthSqr()<1e-10)continue;
            result.add(new Birth(vec(a.point()).lerp(vec(b.point()),t),tick,a.arc()+(b.arc()-a.arc())*t,direction.normalize(),cell));
        }
        return result;
    }
    private void meteorFlameV9(){
        final double length=5.35,time=now/20; // current meteor-huahong.js defaults
        for(int k=0;k<8;k++){
            var rows=new ArrayList<Row>();double phase=k*2.399,span=length*(.55+rnd(k+7)*.45);
            for(int i=0;i<=60;i++){
                double q=i/60.0,window=Math.max(0,Math.sin(Math.PI*q)),reach=window*1.25*.21;
                double angle=phase+q*2.6+Math.sin(q*7-time*1.3+k)*q*.22;
                Frame f=axial(.17-q*span);Vec3 transverse=f.side().scale(Math.cos(angle)).add(f.up().scale(Math.sin(angle)));
                rows.add(new Row(offset(f,Math.cos(angle)*reach,Math.sin(angle)*reach),-q*span+k*.83,
                        .32*.21*Math.pow(window,.65)*(1-q*.65)*(.85+.15*Math.sin(q*11-time*.7+k)),
                        strength*.26F*(float)(1-q),mix(0xff6535,0xfff1c8,Math.pow(1-q,4)*.8),transverse));
            }
            strip(rows,44);
            // Eight fine outer streams retain world history around the continuous primary.
            var history=new ArrayList<Row>();
            for(int i=0;i<nodes.size();i++){
                var n=nodes.get(i);double age=Math.max((now-n.tick())/20,(origin-n.arc())/(10.8*HuahongHeadBridge.BODY_SCALE)),left=Mth.clamp(1-age/2.9,0,1);if(age<0||left<=0)continue;
                Frame f=at(i);double open=smooth(.025,.4,age),curl=Math.sin(n.arc()*.65+k*1.7)*.24+Math.sin(n.arc()*1.73+k)*.11;
                double spread=open*1.25*.10*(1+age*.24);
                Vec3 transverse=f.side().scale(Math.cos(phase)).add(f.up().scale(Math.sin(phase)));
                history.add(new Row(offset(f,Math.cos(phase+curl)*spread,Math.sin(phase+curl)*spread),(n.arc()-origin)/HuahongHeadBridge.BODY_SCALE+k*.93,
                        .32*.12*(.15+.85*Math.pow(left,1.2))*.8,(float)(strength*.10*Math.pow(left,2.1)*smooth(0,.45,origin-n.arc())),
                        mix(0xff6535,0xfff1c8,Math.exp(-age*2)*.5),transverse));
            }
            strip(history,44);
        }
        meteorLightBand();
        for(var b:timedBirths(.05,1.65)){
            double age=(now-b.tick())/20;if(age<=.035||rnd(b.cell())>8/14.0)continue;
            Vec3 side=side(b.direction()),up=side.cross(b.direction()).normalize();double angle=rnd(b.cell()+1)*Math.PI*2,drift=(.18+rnd(b.cell()+2)*.55)*1.25;
            var rows=new ArrayList<Row>();
            for(int i=0;i<=8;i++){
                double t=Math.max(0,age-i*.016);Vec3 p=b.p().add(b.direction().scale((-(.3+rnd(b.cell()+4))*t-.52*t*t)*HuahongHeadBridge.BODY_SCALE))
                        .add(side.scale(Math.cos(angle)*drift*t*HuahongHeadBridge.BODY_SCALE)).add(up.scale((Math.sin(angle)*drift*t-.11*t*t)*HuahongHeadBridge.BODY_SCALE));
                rows.add(new Row(p,t*4, (.008+.32*.03)*Math.sin(Math.PI*i/8),strength*.35F*(float)Math.pow(1-age/1.65,1.6),0xffbc82,null));
            }
            strip(rows,56);
        }
    }
    /** One pointed, camera-facing light band owns nose and world history.
     * A short crossed spindle followed by fading filaments made a bright ball
     * and a thin dark tail. Only the remote end ages/tapers; surface motion
     * belongs to the surrounding tongues, never to this primary emission. */
    private void meteorLightBand(){
        var core=new ArrayList<Row>();var halo=new ArrayList<Row>();
        for(int i=0;i<nodes.size();i++){
            var n=nodes.get(i);double d=origin-n.arc(),age=Math.max(0,(now-n.tick())/20);
            double life=1-smooth(3.8,5.8,HuahongHeadBridge.luminousSupportAge(age,d));
            double end=1-smooth(105,158,d);if(life<=0||end<=0)continue;
            double width=.098*smooth(-.4,.35,d)*end;
            float alpha=(float)(strength*life*end);
            Frame f=at(i);
            core.add(new Row(f.p(),n.arc()-origin,width*2.8,alpha*.95F,0xfff8f2,null));
            halo.add(new Row(f.p(),n.arc()-origin,width*2.4,alpha*.12F,0xffb389,null));
        }
        Frame head=back(0);
        for(int i=1;i<=12;i++){
            double d=-.4*i/12,width=.098*smooth(-.4,.35,d);
            Vec3 p=head.p().subtract(head.forward().scale(d));
            core.add(new Row(p,-d,width*2.8,strength*.95F,0xfff8f2,null));
            halo.add(new Row(p,-d,width*2.4,strength*.12F,0xffb389,null));
        }
        strip(halo,22);strip(core,58);
    }
    private void tornFlameV3(){
        final double time=now/20,spread=.95,speedStretch=.82+8.4*.038;
        for(int k=0;k<29;k++){
            double seed=k*7.31+173,phase=formHash(seed)*Math.PI*2;
            double length=(.55+4.2*(.18+.87*formHash(seed+1)))*speedStretch,reach=(.10+.58*formHash(seed+5))*spread;
            double rx=.48+formHash(seed+4)*.16,ry=Math.cos(phase)*reach*.72,rz=Math.sin(phase)*reach*.62;
            double cy=ry+Math.cos(phase)*spread*.18,cz=rz+Math.sin(phase)*spread*.18+.09;
            double ty=ry+Math.cos(phase)*spread*(.24+.75*formHash(seed+2)),tz=rz+Math.sin(phase)*spread*.6+.12;
            var rows=new ArrayList<Row>();
            for(int i=0;i<=56;i++){
                double q=Math.pow(i/56.0,1.6),window=Math.max(0,Math.sin(Math.PI*q));
                double middle=fireNoise(q*7.2-time*(2.5+8.4*.04)+k*3.37,81),flutter=fireNoise(q*13-time*4.5+k*2.7,82);
                // Separate pointed roots formed a crown of spikes. All tongues
                // now open from one compact shoulder before they tear apart aft.
                Frame f=axial(curve(.52,rx-length*.32,rx-length,q)*HuahongHeadBridge.BODY_SCALE);
                double shoulder=smooth(0,.22,q);
                double y=(curve(ry,cy,ty,q)+(middle*.07+flutter*.018)*window*spread)*shoulder;
                double z=(curve(rz,cz,tz,q)+fireNoise(q*8.5-time*3.1+k*2.7,83)*window*spread*.08)*shoulder;
                double width=.27*(.48+.96*formHash(seed+9))*Math.pow(window,.72)*Math.pow(1-q,.24)*(.44+.66*formHash(seed+8))*(.76+.24*middle);
                rows.add(new Row(offset(f,y,z),q*length+phase,width,strength*(float)((.96-.81*q)*smooth(0,.10,q)),0xff2810,null));
            }
            var red=new ArrayList<Row>();var gold=new ArrayList<Row>();var hot=new ArrayList<Row>();
            for(int i=0;i<rows.size();i++){
                var r=rows.get(i);red.add(new Row(r.p(),r.u(),r.width()*1.65,r.alpha()*.0936F,0xff0b02,null));
                if((r.u()-phase)/length<25/56.0)gold.add(new Row(r.p(),r.u(),r.width()*.20,r.alpha()*.25F,0xff5935,null));
                if((r.u()-phase)/length<15/56.0&&k%6==0)hot.add(new Row(r.p(),r.u(),r.width()*.035,r.alpha()*.18F,0xfff3bd,null));
                rows.set(i,new Row(r.p(),r.u(),r.width(),r.alpha()*.2448F,0xff2810,null));
            }
            strip(red,48);strip(rows,46);strip(gold,50);strip(hot,52);
            if(k%3==0){
                Row root=rows.get((int)Math.round(56*Math.pow(32/56.0,1/1.6)));var fork=new ArrayList<Row>();
                double rootQ=(root.u()-phase)/length;Frame f=axial(curve(.52,rx-length*.32,rx-length,rootQ)*HuahongHeadBridge.BODY_SCALE);
                Vec3 tail=root.p().subtract(f.forward().scale(length*.34*HuahongHeadBridge.BODY_SCALE)).add(f.side().scale((k%2==0?-1:1)*spread*.31*HuahongHeadBridge.BODY_SCALE)).add(f.up().scale(.25*HuahongHeadBridge.BODY_SCALE));
                for(int i=0;i<=22;i++){double q=i/22.0;Vec3 p=root.p().lerp(tail,q).add(f.up().scale(Math.sin(q*5+time*3)*q*(1-q)*.3*HuahongHeadBridge.BODY_SCALE));
                    fork.add(new Row(p,q*length+phase+.7,.27*.43*Math.pow(Math.sin(Math.PI*q),.8)*(.7-.5*q),strength*.2016F*(float)(1-q),0xff2810,null));}
                strip(fork,46);
            }
        }
        nucleus(0xfff3bd,.32,.27*.08,.28F);
        // A soft continuous base carries the local tongues into the older
        // curved wake. Individual embers can die out without breaking it.
        var glow=new ArrayList<Row>();
        var wake=new ArrayList<Row>();
        for(int i=0;i<nodes.size();i++){
            var node=nodes.get(i);
            double behind=headArc()-node.arc();
            double age=Math.max((now-node.tick())/20,behind/24);
            double alpha=smooth(4,13,behind)*(1-smooth(.8,2.4,age));
            if(behind>58)continue;
            Vec3 point=at(i).p();
            double width=.24+.31*smooth(4,18,behind);
            double u=behind*.42;
            glow.add(new Row(point,u,width*1.9,(float)(strength*alpha*.09),0xff220a,null));
            wake.add(new Row(point,u,width,(float)(strength*alpha*.20),0xff5426,null));
        }
        strip(glow,48);strip(wake,46);
        for(var b:timedBirths(.10,2.8)){
            double age=(now-b.tick())/20;if(age<.09)continue;double fade=Math.pow(Mth.clamp(1-age/2.8,0,1),2);
            Vec3 side=side(b.direction()),up=side.cross(b.direction()).normalize();
            for(int k=0;k<5;k++){
                double phase=formHash(b.cell()+173)*Math.PI*2+k*2.399,rise=age*(.27+.30*formHash(b.cell()+k+5));
                Vec3 center=b.p().add(b.direction().scale((-.12-age*8.4*(.035+.014*k))*HuahongHeadBridge.BODY_SCALE))
                    .add(side.scale(Math.cos(phase)*spread*(.12+age*.19)*HuahongHeadBridge.BODY_SCALE)).add(up.scale((Math.sin(phase)*spread*.31+rise)*HuahongHeadBridge.BODY_SCALE));
                double length=(.25+4.2*.095)*(.40+fade);var red=new ArrayList<Row>();var fire=new ArrayList<Row>();
                for(int i=0;i<=20;i++){
                    double q=i/20.0,w=Math.pow(Math.sin(Math.PI*q),.65)*(.85-.6*q);Vec3 p=center.subtract(b.direction().scale(q*length*HuahongHeadBridge.BODY_SCALE))
                        .add(side.scale(Math.sin(q*4+phase+age*2.1)*q*.21*HuahongHeadBridge.BODY_SCALE)).add(up.scale(q*(.17+age*.06)*HuahongHeadBridge.BODY_SCALE));
                    red.add(new Row(p,q*length+b.tick()*.15+k,.27*(.40+.37*fade)*w,(float)(strength*fade*(1-q*.84)*.12*smooth(0,.12,q)),0xff0b02,null));
                    fire.add(new Row(p,q*length+b.tick()*.15+k,.27*(.16+.24*fade)*w,(float)(strength*fade*(1-q*.84)*.23*smooth(0,.12,q)),0xff2810,null));
                }
                strip(red,48);strip(fire,46);
            }
        }
    }
    private void feather(){
        // Open V4 membrane family: irregular three-dimensional rear endpoints,
        // offset birth roots and a few middle-born forks. No removed V3 form.
        float formFade=(float)Math.pow(strength,2.1);
        double[][] lobes={{1,-.84,.3,1.18},{.68,.91,.4,1.38},{1.15,.38,-.16,.8},{.51,-.55,-.70,1.12},{.84,.21,.88,1.05},{.61,-1.03,.05,.84},{.96,.93,-.33,.73},{.44,.43,-.58,1.24},{1.24,-.31,.03,.58},{.72,-.35,.72,.95},{.57,.73,.12,.87},{1.07,.10,-.65,.62}};
        for(int k=0;k<lobes.length;k++){
            List<Row> rows=new ArrayList<>(),rib=new ArrayList<>();var l=lobes[k];
            double span=4.146*l[0],reach=1.1*(.55+.24*rnd(k+8));
            boolean middleBorn=k==3||k==6||k==9;
            double rootX=middleBorn?-.10-span*.17:.64-.055*(k%4),controlX=.58-span*(.30+.14*rnd(k+94)),tipX=.45-span;
            double rootReach=middleBorn?reach*.28:.023;
            double controlReach=reach*1.12,tipReach=reach*(1.02+.46*rnd(k+36));
            for(int i=0;i<=64;i++){
                double q=i/64.0,envelope=Math.max(0,Math.sin(Math.PI*q));Frame f=axial(curve(rootX,controlX,tipX,q));
                double spread=curve(rootReach,controlReach,tipReach,q);
                double x=l[1]*spread,y=l[2]*curve(rootReach,reach*.82,reach*(1.12+.40*rnd(k+91)),q)*2.05;
                double flutter=Math.sin(q*7-now*.022+k*.83)*envelope*q*.045;
                double hook=smooth(.55,1,q)*Math.sin(q*4.4+k*1.61)*(k%3==1?.20:.07);
                Vec3 p=offset(f,x,y+flutter+hook);
                Vec3 s=f.side().scale(l[1]).add(f.up().scale(l[2]+Math.sin(q*3+k)*.20)).normalize();
                double width=(.055+.14*.83)*l[3]*Math.pow(envelope,.67)*Math.pow(1-q,.34);
                int color=k==2||k==8||k==11?0x8063d8:mix(0x55b3db,0x2884d0,q*.6);
                rows.add(new Row(p,q*span+k*2.37,width,formFade*(k==1||k==4||k==8?.3864F:.14835F)*(float)((.86-.46*q)*smooth(0,.1,q)),color,s));
                if(i>=3+k%4&&i<=36+k%12)rib.add(new Row(p.add(s.scale(width*(k%2==0?-.28:.35))),q*span+k*2.37,.006*(1-q),strength*(k%4==0?.24F:.12F)*(float)(1-q),0xa5d6ed,s));
            }
            strip(rows,26);strip(rib,22);
            if(k%2==0){
                var fork=new ArrayList<Row>();double start=.36;
                for(int i=0;i<=30;i++){
                    double q=i/30.0;Frame f=axial(curve(rootX,controlX,tipX,start)-q*span*.42);
                    double spread=curve(rootReach,controlReach,tipReach,start);
                    Vec3 p=offset(f,l[1]*(spread+q*.38),l[2]*spread*2.05+Math.sin(q*Math.PI)*.16);
                    Vec3 s=f.side().scale(l[1]).add(f.up().scale(l[2])).normalize();
                    fork.add(new Row(p,q*span+k*1.71,.075*Math.pow(Math.sin(Math.PI*q),.8),formFade*(k==4||k==8?.2184F:.1014F)*(float)(1-q),0x6e8dd1,s));
                }
                strip(fork,26);
            }
        }
        strip(history(3.2,.045,0x4598cf,0,0,false),10);
        for(int k=0;k<3;k++)strip(history(3.2,.008,0x6ea7da,k,.07,false),26);
        var haze=new ArrayList<Row>();var hot=new ArrayList<Row>();
        for(int i=0;i<=24;i++){
            double q=i/24.0,w=Math.pow(Math.max(0,Math.sin(Math.PI*q)),1.45);Frame f=axial(.38+q*.48);
            float a=(float)(Math.pow(strength,2.1)*(.3+.7*Math.sin(Math.PI*q)));
            haze.add(new Row(f.p(),q,w*(.045+.14*.32),a*.1105F,0x6ec9ff,f.side()));
            hot.add(new Row(f.p(),q,w*(.007+.14*.058),a*.72F,0xecffff,f.side()));
        }
        strip(haze,10,true);strip(hot,22,true);
    }
    private static final double FAN_OPENING_SPEED=11.5,FAN_LIFETIME=2.4,FAN_FLOW_SPEED=4;
    private record FanPoint(Vec3 p,Vec3 normal,double alpha,double phase){}
    private List<FanSample> fanSamples(){
        if(canonical.size()<2)return List.of();
        double length=FAN_OPENING_SPEED*FAN_LIFETIME;
        double head=headArc();
        var history=SwordFlightTrailHistory.uniformHead(canonical,.175,length);
        var curve=new FlightTrailCurve(canonical,vec(canonical.get(canonical.size()-1).point()),forward);
        var samples=new ArrayList<FanSample>();
        for(var node:history){
            double behind=head-node.arc();
            if(behind>length+1e-8)continue;
            var sample=curve.at(behind);
            Vec3 f=sample.forward(),s=side(f);
            // Age only retires the underlying history. It never drives the
            // displayed opening, or makes a wave follow the rider's speed.
            double opacity=1-smooth(1.85,FAN_LIFETIME,(now-node.tick())/20);
            samples.add(new FanSample(new Frame(sample.point(),f,s,s.cross(f).normalize()),behind,opacity));
        }
        return samples;
    }
    private double fanFlow(FanSample sample){
        return -sample.behind()+now/20*FAN_FLOW_SPEED;
    }
    private FanPoint fanPoint(FanSample h,int j,int sign,double age,double u){
        double rank=(j+.55)/3.15,phase=j*1.79+(sign<0?.94:0);
        double life=2.4*(.43+.43*(1-rank))*(sign>0?1:.92),v=Mth.clamp(1-age/life,0,1);
        double open=1-Math.exp(-age*2.208),flow=fanFlow(h);
        Frame basis=h.frame();
        Vec3 f=basis.forward(),s=basis.side(),up=basis.up();
        double lateral=(.014+1.6*(.26+rank*1.45)*open)*(1+.08*Math.sin(flow*.27+phase)*open);
        double wave=(Math.sin(flow*.36+phase)*.44+Math.sin(flow*.48+phase*1.7)*.055)*open;
        double half=(.045+.43*(.50+.60*(1-rank)))*smooth(0,.21,age)*Math.pow(v,.65)*(1+.20*Math.sin(flow*.32+phase));
        // Separate birth-anchored azimuths form an open spatial fan. Previously
        // every centre used the same .79/.61 diagonal, so all films shared a plane.
        double azimuth=.15+(j-1)*.78+(sign<0?Math.PI:0);
        Vec3 radial=s.scale(Math.cos(azimuth)).add(up.scale(Math.sin(azimuth))),around=f.cross(radial).normalize();
        double roll=.25+sign*.32+Math.sin(flow*.31+phase)*.45+age*.24;
        Vec3 across=radial.scale(Math.cos(roll)).add(around.scale(Math.sin(roll))),normal=f.cross(across).normalize();
        double curlGain=.80+.42*Math.sin(flow*.35+phase);
        double curl=half*(u*u-.27)*curlGain;
        double wrinkle=(.015*Math.sin(flow*.92+u*2.5+phase)+.012*Math.sin(flow*.65-u*3.2+phase*.6))*open;
        Vec3 p=basis.p().add(f.scale((.12*Math.exp(-age*7)-half*.22*(1-u*u)*open)*HuahongHeadBridge.BODY_SCALE))
                .add(radial.scale(lateral*HuahongHeadBridge.BODY_SCALE)).add(around.scale(wave*.55*HuahongHeadBridge.BODY_SCALE))
                .add(up.scale(-age*.06*HuahongHeadBridge.BODY_SCALE))
                .add(across.scale(half*u*HuahongHeadBridge.BODY_SCALE)).add(normal.scale((curl+wrinkle)*HuahongHeadBridge.BODY_SCALE));
        Vec3 du=across.scale(half).add(normal.scale(2*half*u*curlGain)).add(f.scale(half*.44*u*open));
        return new FanPoint(p,f.cross(du).normalize(),h.opacity()*Math.pow(v,1.8)*(1-.22*rank),phase);
    }
    private void fan(){
        // V7 fan-lookdev.js: nine columns per open curled film, asymmetric
        // life, full-arc end caps, two embedded ridges and four diffuse wisps.
        var samples=fanSamples();
        if(samples.size()<4)return;
        for(int sign:new int[]{-1,1})for(int j=0;j<3;j++){
            double life=2.4*(.43+.43*(1-(j+.55)/3.15))*(sign>0?1:.92);
            double total=Math.min(samples.get(0).behind(),life*FAN_OPENING_SPEED);
            double cap=Math.min(1.35,total*.22);
            Vec3[] previous=null;float oldAlpha=0;int[] oldColors=null;double oldU=0;
            for(var b:samples){
                if(b.behind()>total)continue;
                double age=b.behind()/FAN_OPENING_SPEED,fade=smooth(0,cap,b.behind())*smooth(0,cap,total-b.behind());
                Vec3[] row=new Vec3[9];int[] colors=new int[9];FanPoint q=null;
                for(int k=0;k<=8;k++){
                    q=fanPoint(b,j,sign,age,(k/4.0-1)*Math.pow(fade,.6));row[k]=q.p();
                    double facing=Math.abs(q.normal().dot(eye.subtract(q.p()).normalize()));
                    double rim=Math.pow(Math.max(0,1-facing),1.7);
                    colors[k]=mix(0xefb75e,0xfff5d9,.16+.20*(1-age/life)+rim*.42);
                }
                float alpha=(float)(strength*q.alpha()*fade*.72);
                double u=-b.behind()+q.phase()*2.2;
                if(previous!=null)for(int k=0;k<8;k++){
                    vertex(previous[k],oldU,28+k/8.0,oldAlpha,oldColors[k]);vertex(previous[k+1],oldU,28+(k+1)/8.0,oldAlpha,oldColors[k+1]);
                    vertex(row[k+1],u,28+(k+1)/8.0,alpha,colors[k+1]);vertex(row[k],u,28+k/8.0,alpha,colors[k]);
                }
                previous=row;oldAlpha=alpha;oldColors=colors;oldU=u;
            }
            for(double ridge:new double[]{-.32,.41}){
                var rows=new ArrayList<Row>();
                for(var b:samples){
                    if(b.behind()>total)continue;
                    double age=b.behind()/FAN_OPENING_SPEED,flow=fanFlow(b);
                    var q=fanPoint(b,j,sign,age,ridge+.09*Math.sin(flow*.42+j));
                    rows.add(new Row(q.p(),-b.behind()+q.phase(),(.005+.36*.017)*(.2+.8*Mth.clamp(1-age/life,0,1)),
                        (float)(strength*q.alpha()*smooth(0,cap,b.behind())*smooth(0,cap,total-b.behind())
                                *(.5+.5*smooth(-.5,1,Math.sin(flow*.35+j)))*(j==0?.23:.15)),0xffe6b4,null));}
                strip(rows,38);
            }
        }
        for(int sign:new int[]{-1,1})for(int j=0;j<2;j++){
            var rows=new ArrayList<Row>();for(var b:samples){double age=b.behind()/FAN_OPENING_SPEED,v=Mth.clamp(1-age/FAN_LIFETIME,0,1);var q=fanPoint(b,j,sign,age,.25);
                rows.add(new Row(q.p().add(b.frame().up().scale(smooth(.65,2.2,age)*(j==0?.24:-.22)*HuahongHeadBridge.BODY_SCALE)),-b.behind()+j*1.4,
                    (.009+.36*.026)*(.2+.8*v),(float)(strength*b.opacity()*Math.pow(v,2.4)*smooth(0,.10,age)*.13),0xc7ad7e,null));}
            strip(rows,38);
        }
    }
    private void smoke(){
        // Time-born parcels advect, roll and grow along the coherent curved wake.
        // Their identities belong to emission time, never to fast world-arc cells.
        var clouds=new ArrayList<Cloud>();
        double recorded=nodes.isEmpty()?0:Math.max(0,headArc()-nodes.get(0).arc());
        if(strength>.001){
            for(var parcel:HuahongFlowField.parcels(now)){
                double behind=parcel.behind();
                Frame f=back(behind);
                double envelope=parcel.alpha()*smooth(17,7,behind)
                        *smooth(behind-.7,behind+.7,recorded);
                if(envelope<=0)continue;
                double rear=smooth(12,24,behind);
                Vec3 p=offset(f,parcel.side(),parcel.up());
                double radius=parcel.radius(),seed=Math.floorMod(parcel.id(),64);
                double nose=1-smooth(.5,4.5,behind);
                float alpha=(float)(strength*envelope*(1-.35*rear)*(.34+.23*nose));
                clouds.add(new Cloud(p,seed,radius,alpha,0x060101,30));
                clouds.add(new Cloud(p.add(f.side().scale(radius*.12)),Math.floorMod(parcel.id()+19,64),radius*.48,
                        (float)(strength*envelope*(1-.35*rear)*parcel.crimson()*(.08+.095*nose)),0xce252f,20));
            }
        }
        // Several depth slices give packets actual spatial thickness and
        // intersect world depth independently; this is layered smoke, not a
        // claim of ray-marched density or a fluid solver.
        var slices=new ArrayList<Cloud>();
        for(var c:clouds){
            Vec3 view=eye.subtract(c.p());view=view.lengthSqr()<1e-10?forward:view.normalize();
            for(int layer=0;layer<6;layer++){
                double z=(layer-2.5)*.28,radius=c.radius()*Math.sqrt(1-z*z);
                slices.add(new Cloud(c.p().add(view.scale(z*c.radius()*.72*HuahongHeadBridge.WIDTH_MULTIPLIER)),c.seed(),radius,c.alpha()*.34F,c.color(),c.mode()));
            }
        }
        slices.sort(Comparator.comparingDouble((Cloud c)->c.p().distanceToSqr(eye)).reversed());
        for(var c:slices)billboard(new Row(c.p(),c.seed(),c.radius(),c.alpha(),c.color(),null),c.mode());
    }
    private void dense(){
        // Only the compact source nucleus uses a sprite. The surrounding body
        // is the independent ray-integrated world field, queued by HuahongVisuals.
        if(strength>.001F)billboard(new Row(center,0,.12+3.5*.045+.52*.10,strength,0xe6fbff,null),42);
    }
    private void billboard(Row r,int mode){
        // One camera basis for every sprite: the old world-axis fallback
        // snapped 90 degrees when a sprite-to-eye direction crossed |y|=.95.
        double scale=r.width()*HuahongHeadBridge.BODY_SCALE;
        double rx=cameraRight.x*scale,ry=cameraRight.y*scale,rz=cameraRight.z*scale;
        double ux=cameraUp.x*scale,uy=cameraUp.y*scale,uz=cameraUp.z*scale;Vec3 p=r.p();
        vertexRelative((p.x-rx)-ux-eye.x,(p.y-ry)-uy-eye.y,(p.z-rz)-uz-eye.z,Math.floor(r.u())*4,mode,r.alpha(),r.color());
        vertexRelative((p.x+rx)-ux-eye.x,(p.y+ry)-uy-eye.y,(p.z+rz)-uz-eye.z,Math.floor(r.u())*4+1,mode,r.alpha(),r.color());
        vertexRelative((p.x+rx)+ux-eye.x,(p.y+ry)+uy-eye.y,(p.z+rz)+uz-eye.z,Math.floor(r.u())*4+1,mode+1,r.alpha(),r.color());
        vertexRelative((p.x-rx)+ux-eye.x,(p.y-ry)+uy-eye.y,(p.z-rz)+uz-eye.z,Math.floor(r.u())*4,mode+1,r.alpha(),r.color());
    }
    private void strip(List<Row> rows,int mode){
        strip(rows,mode,false);
    }
    private void strip(List<Row> rows,int mode,boolean localHead){
        if(!localHead){strip(rows,mode,false,false);return;}
        strip(rows,mode,true,false);strip(rows,mode,true,true);
    }
    private void strip(List<Row> rows,int mode,boolean localHead,boolean crossPlane){
        if(rows.size()<2)return;
        double[] lengths=new double[rows.size()];for(int i=1;i<rows.size();i++)lengths[i]=lengths[i-1]+rows.get(i).p().distanceTo(rows.get(i-1).p());
        double total=lengths[lengths.length-1];if(total<1e-8)return;
        double cap=Math.min(total*.12,.25);
        boolean softFire=mode==46||mode==48||mode==50||mode==52;
        int columns=mode==44?8:softFire||mode==24||mode==26||mode==28||mode==34?6:1;
        Vec3[] previous=null;Vec3 previousSide=null;Row old=null;float previousAlpha=0;
        for(int i=0;i<rows.size();i++){
            Row row=rows.get(i);Vec3 f=rows.get(Math.min(rows.size()-1,i+1)).p().subtract(rows.get(Math.max(0,i-1)).p());
            if(f.lengthSqr()<1e-12)continue;f=f.normalize();
            // Local nuclei and membrane ridges carry the exact section's side
            // through every position offset. Never infer a nearest path point
            // from that offset: a U-turn can put another arm closer to it.
            Vec3 supplied=row.side();
            if(supplied==null&&localHead)supplied=path.behind(0).side();
            Vec3 s=supplied==null?f.cross(eye.subtract(row.p())):supplied.subtract(f.scale(supplied.dot(f)));
            if(crossPlane)s=f.cross(s);
            if(s.lengthSqr()<1e-10&&previousSide!=null)s=previousSide.subtract(f.scale(previousSide.dot(f)));
            if(s.lengthSqr()<1e-10)s=side(f);else s=s.normalize();
            if(previousSide!=null&&s.dot(previousSide)<0)s=s.scale(-1);
            if(softFire&&previousSide!=null){
                Vec3 carried=previousSide.subtract(f.scale(previousSide.dot(f)));
                if(carried.lengthSqr()>1e-10)s=carried.normalize().scale(.75).add(s.scale(.25)).normalize();
            }
            Vec3 normal=f.cross(s).normalize();
            double endFade=rows.size()==2?1:smooth(0,cap,lengths[i])*smooth(0,cap,total-lengths[i]);
            float alpha=row.alpha()*(float)endFade*(localHead?.70710677F:1);
            double width=row.width()*HuahongHeadBridge.BODY_SCALE;
            if(softFire&&i>0&&i+1<rows.size()){
                Vec3 a=row.p().subtract(rows.get(i-1).p()),b=rows.get(i+1).p().subtract(row.p());
                double la=a.length(),lb=b.length();
                if(la>1e-8&&lb>1e-8){
                    double turn=Math.sqrt(Math.max(1e-10,2*(1-Mth.clamp(a.dot(b)/(la*lb),-1,1))));
                    width=Math.min(width,Math.max(width*.20,.75*Math.min(la,lb)/turn));
                }
            }
            if(mode==22&&width>0){
                double covered=HuahongVisuals.coreHalfWidth(row.p(),s,eye,width);
                if(covered>width){alpha*=width/covered;width=covered;}
            }
            Vec3[] section=new Vec3[columns+1];
            for(int j=0;j<=columns;j++){
                double across=j*2.0/columns-1;
                double camber=columns==1||mode==34?0:width*(1-across*across)*(mode==44?.25:.24+.08*Math.sin(row.u()*(Math.PI*2*33/256)+now*.013));
                double transverse=width*across;Vec3 p=row.p();
                section[j]=new Vec3((p.x+s.x*transverse)+normal.x*camber,(p.y+s.y*transverse)+normal.y*camber,(p.z+s.z*transverse)+normal.z*camber);
            }
            if(previous!=null){
                for(int j=0;j<columns;j++){
                    double a=mode+j/(double)columns,b=mode+(j+1)/(double)columns;
                    vertex(previous[j],old.u(),a,previousAlpha,old.color());
                    vertex(previous[j+1],old.u(),b,previousAlpha,old.color());
                    vertex(section[j+1],row.u(),b,alpha,row.color());
                    vertex(section[j],row.u(),a,alpha,row.color());
                }
            }
            previous=section;previousSide=s;old=row;previousAlpha=alpha;
        }
    }
    private void vertex(Vec3 p,double u,double v,float alpha,int color){
        vertexRelative(p.x-eye.x,p.y-eye.y,p.z-eye.z,u,v,alpha,color);
    }
    private void vertexRelative(double x,double y,double z,double u,double v,float alpha,int color){
        out.vertex(pose.pose(),(float)x,(float)y,(float)z).uv((float)u,(float)v)
                .color((color>>16)&255,(color>>8)&255,color&255,Mth.clamp(Math.round(alpha*255),0,255)).endVertex();vertices++;
    }
}
