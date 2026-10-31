package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.yujian.HuahongSkin;
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
    private record Frame(Vec3 p,Vec3 forward,Vec3 side,Vec3 up){}
    private final VertexConsumer out;
    private final PoseStack.Pose pose;
    private final Vec3 eye,center,forward;
    private final List<SwordFlightTrailHistory.Node> nodes;
    private final double now,origin;
    private final float strength;
    private int vertices;
    private HuahongSkinGeometry(VertexConsumer out,PoseStack.Pose pose,Vec3 eye,Vec3 center,Vec3 forward,
            List<SwordFlightTrailHistory.Node> nodes,double now,float strength){
        this.out=out;this.pose=pose;this.eye=eye;this.center=center;this.forward=forward;this.nodes=nodes;this.now=now;this.strength=strength;
        origin=nodes.isEmpty()?0:Math.floor(nodes.get(0).arc()/256)*256;
    }
    static int draw(HuahongSkin skin,VertexConsumer out,PoseStack.Pose pose,Vec3 eye,Vec3 center,Vec3 forward,
            List<SwordFlightTrailHistory.Node> nodes,double now,float strength){
        var g=new HuahongSkinGeometry(out,pose,eye,center,forward,nodes,now,strength);
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
    private Frame frame(Vec3 p,Vec3 f){if(f.lengthSqr()<1e-10)f=forward;else f=f.normalize();Vec3 s=side(f);return new Frame(p,f,s,s.cross(f).normalize());}
    private Frame at(int i){return frame(vec(nodes.get(i).point()),vec(nodes.get(Math.min(nodes.size()-1,i+1)).point()).subtract(vec(nodes.get(Math.max(0,i-1)).point())));}
    /** Walk the recorded path backwards; turn local flames with the trail, not a rigid spine. */
    private Frame back(double distance){
        Vec3 p=center;Frame last=frame(p,forward);
        for(int i=nodes.size()-1;i>=0;i--){
            Vec3 next=vec(nodes.get(i).point());double d=p.distanceTo(next);if(d<1e-8)continue;
            last=frame(p,p.subtract(next));if(distance<=d)return frame(p.lerp(next,distance/d),last.forward());
            distance-=d;p=next;
        }
        return frame(p.subtract(last.forward().scale(distance)),last.forward());
    }
    private Vec3 offset(Frame f,double x,double y){return f.p().add(f.side().scale(x)).add(f.up().scale(y));}
    private List<Row> history(double life,double width,int color,int strand,double spread,boolean oriented){
        List<Row> rows=new ArrayList<>();
        for(int i=0;i<nodes.size();i++){
            var n=nodes.get(i);double age=(now-n.tick())/20;if(age<0||age>=life)continue;
            Frame f=at(i);double phase=strand*2.399+Math.sin(n.arc()*.47+strand)*.28;
            double open=(1-Math.exp(-age*5))*spread;
            Vec3 normal=f.side().scale(Math.cos(phase)).add(f.up().scale(Math.sin(phase)));
            rows.add(new Row(f.p().add(normal.scale(open)),n.arc()-origin,width*(.18+.82*fade(age,life)),(float)fade(age,life),color,oriented?normal:null));
        }
        return rows;
    }
    private void nucleus(int color,double length,double width,float opacity){
        List<Row> rows=new ArrayList<>();
        for(int i=0;i<=36;i++){
            double q=i/36.0;Frame f=back(q*length);
            rows.add(new Row(f.p().add(forward.scale(.22*(1-q))),-q*length,width*Math.pow(Math.sin(Math.PI*q),.65)*Math.exp(-q*1.2),strength*opacity*(float)(1-q),color,null));
        }
        strip(rows,22);
    }
    private void needle(){
        strip(history(5.8,.30,0xffc55a,0,0,false),10);
        strip(history(5.8,.050,0xfff3c9,0,0,false),22);
        for(int k=0;k<3;k++)strip(history(3.3,.025,0xffdf8a,k,.08,false),16);
        nucleus(0xfff6d4,4.0,.23,1);
    }
    private void fire(boolean torn){
        int count=torn?26:10;double width=torn?.43:.28,length=torn?5.8:3.8;
        for(int k=0;k<count;k++){
            List<Row> rows=new ArrayList<>();double phase=k*2.399,span=length*(.52+.60*rnd(k+8));
            for(int i=0;i<=48;i++){
                double q=i/48.0;Frame f=back(q*span);
                double reach=Math.sin(Math.PI*q)*(torn?.95:.43)*(1+.25*Math.sin(q*8-now*.06+k));
                Vec3 transverse=f.side().scale(Math.cos(phase)).add(f.up().scale(Math.sin(phase)));
                double curl=.12*Math.sin(q*13-now*.11+k)*q;
                Vec3 p=offset(f,Math.cos(phase+curl)*reach,Math.sin(phase+curl)*reach);
                rows.add(new Row(p,-q*span+k*.7,width*Math.pow(Math.sin(Math.PI*q),.8)*(1-q*.55),strength*(float)Math.pow(1-q,1.1),torn?0xff502e:0xffb345,transverse));
            }
            strip(rows,12);
            if(!torn)strip(history(3.4,.065,0xff9540,k,.16,true),12);
        }
        nucleus(0xfff3c5,torn?1.3:2.6,torn?.19:.15,.85F);
        if(torn){
            long last=Long.MIN_VALUE;
            for(int i=0;i<nodes.size();i++){
                var n=nodes.get(i);long tick=(long)n.tick();if(tick==last||tick%2!=0)continue;last=tick;
                double age=(now-n.tick())/20;if(age<.1||age>2)continue;Frame f=at(i);
                for(int k=0;k<3;k++){
                    List<Row> shard=new ArrayList<>();double angle=tick*.71+k*2.399;
                    Vec3 birth=offset(f,Math.cos(angle)*(.2+age*.25),Math.sin(angle)*.3+age*.25);
                    for(int j=0;j<=12;j++){
                        double q=j/12.0;Vec3 p=birth.subtract(f.forward().scale(q*(.6+fade(age,2)))).add(f.up().scale(Math.sin(q*4+angle)*q*.14));
                        shard.add(new Row(p,q*2+tick,.15*Math.sin(Math.PI*q), (float)fade(age,2)*(float)(1-q),0xff6230,f.side()));
                    }
                    strip(shard,12);
                }
            }
        }
        embers(torn?0xff662b:0xffbd69,torn?.09:.05);
    }
    private void feather(){
        // Staggered lobes occupy different transverse planes; no mirrored wing model.
        double[][] lobes={{1,-.84,.3},{.68,.91,.4},{1.15,.38,-.16},{.51,-.55,-.70},{.84,.21,.88},{.61,-1.03,.05},{.96,.93,-.33},{.44,.43,-.58},{1.24,-.31,.03},{.72,-.35,.72}};
        for(int k=0;k<lobes.length;k++){
            List<Row> rows=new ArrayList<>(),rib=new ArrayList<>();var l=lobes[k];
            for(int i=0;i<=42;i++){
                double q=i/42.0;Frame f=back(.1+q*4.2*l[0]);
                double reach=Math.sin(q*Math.PI*.9)*1.7;
                Vec3 p=offset(f,l[1]*reach,l[2]*reach+.10*Math.sin(q*9-now*.045+k)*q);
                Vec3 s=f.side().scale(l[1]).add(f.up().scale(l[2])).normalize();
                double w=.34*Math.pow(Math.sin(Math.PI*q),.65)*(1-q*.7);
                rows.add(new Row(p,q*5+k*.63,w,strength*(float)Math.pow(1-q,1.3)*.85F,k%3==0?0x947bff:0x54bcff,s));
                rib.add(new Row(p,q*5+k*.63,.016*(1-q),strength*(float)(1-q)*.65F,0xc7edff,null));
            }
            strip(rows,16);strip(rib,22);
        }
        for(int k=0;k<3;k++)strip(history(3.2,.045,0x709cff,k,.18,true),16);
        nucleus(0xccefff,1.1,.12,.85F);
    }
    private void fan(){
        for(int k=0;k<9;k++){
            double phase=(k-4)/4.0,span=3.6+2.5*rnd(k+22);
            List<Row> rows=new ArrayList<>();
            for(int i=0;i<=52;i++){
                double q=i/52.0;Frame f=back(q*span);
                double spread=(1-Math.exp(-q*5))*q*2.4;
                Vec3 p=offset(f,phase*spread,Math.sin(k*1.9)*spread*.32+.18*Math.sin(q*6-now*.035+k)*q);
                rows.add(new Row(p,q*span+k,.25*Math.pow(Math.sin(Math.PI*q),.6),strength*(float)Math.pow(1-q,.8)*.50F,k%3==0?0xffecbd:0xe8a34b,f.side()));
            }
            strip(rows,16);
            strip(history(2.4,.038,0xffdc8a,k,.5,true),16);
        }
        nucleus(0xfff1c9,1.2,.18,.8F);
    }
    private void smoke(){
        // Births remain in the world; far-to-near alpha ordering retains dark absorption.
        List<Row> clouds=new ArrayList<>();long last=Long.MIN_VALUE;
        for(int i=0;i<nodes.size();i++){
            var n=nodes.get(i);long tick=(long)Math.floor(n.tick());if(tick==last||tick%2!=0)continue;last=tick;
            double age=(now-n.tick())/20;if(age<0||age>1.25)continue;
            Frame f=at(i);double phase=rnd(tick)*Math.PI*2;
            Vec3 p=offset(f,Math.cos(phase)*(.10+age*.23),Math.sin(phase)*.16+age*.16);
            clouds.add(new Row(p,tick*.37,.50*(1+age*.55),(float)fade(age,1.25),0x20060b,null));
        }
        clouds.sort(Comparator.comparingDouble((Row r)->r.p().distanceToSqr(eye)).reversed());
        for(var r:clouds){billboard(r,14);if(r.alpha()>.65)billboard(new Row(r.p().add(0,.03,0),r.u(),r.width()*.52,r.alpha()*.23F,0xef3018,null),20);}
        if(strength>.001){
            for(int k=0;k<5;k++){
                var f=back(k*.24);billboard(new Row(offset(f,Math.sin(k*2.3)*.16,Math.cos(k*2.3)*.12),now*.017+k,.43,strength*.45F,0x170409,null),14);
            }
        }
        embers(0xff341b,.026);
    }
    private void dense(){
        // Actual circular cross sections, layered shells and interior veins in world space.
        List<Row> rows=history(3.6,1,0x60baff,0,0,true);
        for(int layer=0;layer<3;layer++){
            List<Vec3> prev=null;Row old=null;int sides=12;
            for(int i=0;i<rows.size();i++){
                var r=rows.get(i);Vec3 f=rows.get(Math.min(rows.size()-1,i+1)).p().subtract(rows.get(Math.max(0,i-1)).p());Frame a=frame(r.p(),f);
                double distance=r.p().distanceTo(center),radius=(.14+.62*(1-Math.exp(-distance*.7)))*r.width()*(1-layer*.27);
                List<Vec3> ring=new ArrayList<>();
                for(int j=0;j<=sides;j++){
                    double angle=j*Math.PI*2/sides,warp=1+.12*Math.sin(r.u()*1.4+angle*3-now*.06);
                    ring.add(offset(a,Math.cos(angle)*radius*warp,Math.sin(angle)*radius*warp));
                }
                if(prev!=null)for(int j=0;j<sides;j++){
                    vertex(prev.get(j),old.u(),18+j/(double)sides,old.alpha()*.29F,old.color());
                    vertex(prev.get(j+1),old.u(),18+(j+1)/(double)sides,old.alpha()*.29F,old.color());
                    vertex(ring.get(j+1),r.u(),18+(j+1)/(double)sides,r.alpha()*.29F,r.color());
                    vertex(ring.get(j),r.u(),18+j/(double)sides,r.alpha()*.29F,r.color());
                }
                prev=ring;old=r;
            }
        }
        for(int k=0;k<7;k++)strip(history(3.6,.035,0xccfaff,k,.32,true),6);
        strip(history(3.6,.065,0xcaf4ff,0,0,false),22);
        nucleus(0xe7ffff,2,.25,1);
        embers(0x8ceaff,.045);
    }
    private void embers(int color,double size){
        long previous=Long.MIN_VALUE;
        for(int i=0;i<nodes.size();i++){
            var n=nodes.get(i);long tick=(long)Math.floor(n.tick());if(tick==previous||tick%3!=0)continue;previous=tick;
            double age=(now-n.tick())/20;if(age<.03||age>1.6)continue;Frame f=at(i);
            Vec3 p=offset(f,Math.sin(tick*2.3)*age*.65,Math.cos(tick*2.3)*age*.44+age*age*.17);
            strip(List.of(new Row(p,n.arc()-origin,size,(float)fade(age,1.6),color,null),new Row(p.subtract(f.forward().scale(.14+age*.1)),n.arc()-origin-.2,0,0,color,null)),12);
        }
    }
    private void billboard(Row r,int mode){
        Vec3 normal=eye.subtract(r.p()).normalize(),right=side(normal).scale(r.width()),up=normal.cross(right).normalize().scale(r.width());
        vertex(r.p().subtract(right).subtract(up),Math.floor(r.u())*4,mode,r.alpha(),r.color());
        vertex(r.p().add(right).subtract(up),Math.floor(r.u())*4+1,mode,r.alpha(),r.color());
        vertex(r.p().add(right).add(up),Math.floor(r.u())*4+1,mode+1,r.alpha(),r.color());
        vertex(r.p().subtract(right).add(up),Math.floor(r.u())*4,mode+1,r.alpha(),r.color());
    }
    private void strip(List<Row> rows,int mode){
        Vec3 previous=null,previousSide=null;Row old=null;
        for(int i=0;i<rows.size();i++){
            Row row=rows.get(i);Vec3 f=rows.get(Math.min(rows.size()-1,i+1)).p().subtract(rows.get(Math.max(0,i-1)).p());
            if(f.lengthSqr()<1e-12)continue;f=f.normalize();Vec3 s=row.side()==null?f.cross(eye.subtract(row.p())):row.side().subtract(f.scale(row.side().dot(f)));
            if(s.lengthSqr()<1e-10)s=side(f);else s=s.normalize();
            if(previousSide!=null&&s.dot(previousSide)<0)s=s.scale(-1);s=s.scale(row.width());
            if(previous!=null){
                vertex(previous.subtract(previousSide),old.u(),mode,old.alpha(),old.color());
                vertex(previous.add(previousSide),old.u(),mode+1,old.alpha(),old.color());
                vertex(row.p().add(s),row.u(),mode+1,row.alpha(),row.color());
                vertex(row.p().subtract(s),row.u(),mode,row.alpha(),row.color());
            }
            previous=row.p();previousSide=s;old=row;
        }
    }
    private void vertex(Vec3 p,double u,double v,float alpha,int color){
        Vec3 r=p.subtract(eye);out.vertex(pose.pose(),(float)r.x,(float)r.y,(float)r.z).uv((float)u,(float)v)
                .color((color>>16)&255,(color>>8)&255,color&255,Mth.clamp(Math.round(alpha*255),0,255)).endVertex();vertices++;
    }
}
