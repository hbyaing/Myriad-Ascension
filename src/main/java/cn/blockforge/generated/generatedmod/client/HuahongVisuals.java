package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Forge consumer of minecraft-huahong's independent meteor_prism V9 geometry.
 * White axial nucleus, sparse chromatic filaments and birth-anchored fragments.
 * No user-supplied Meteor Test111 / V13 reference shader is copied here. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class HuahongVisuals {
    private static final double WIDTH=.38,LIFE=116,SPREAD=.65,HEAD_LENGTH=.6+2.5*.65;
    private record Spark(Vec3 origin,Vec3 forward,Vec3 side,Vec3 up,long born,double phase,double drift,double backward){}
    private record Row(Vec3 point,double arc,double width,float alpha,int color){}
    private static final class Trail {
        final SwordFlightTrailHistory history=new SwordFlightTrailHistory(.25,LIFE,160,720);
        final ArrayDeque<Spark> sparks=new ArrayDeque<>();
        Vec3 last;long lastEmission;
        cn.blockforge.generated.generatedmod.yujian.HuahongSkin skin;
    }
    private static final Map<ControlledSwordEntity,Trail> TRAILS=new IdentityHashMap<>();
    private static Object level;
    public static int vertices;
    public static boolean hasTrails(){return !TRAILS.isEmpty();}
    public static void clear(){TRAILS.clear();vertices=0;}
    public static void sample(long tick){
        var mc=Minecraft.getInstance();
        if(level!=mc.level){clear();level=mc.level;}
        if(mc.level==null)return;
        for(var entity:mc.level.entitiesForRendering()){
            if(!(entity instanceof ControlledSwordEntity sword)||!sword.flightRoot()||mc.player!=null&&sword.distanceToSqr(mc.player)>4096)continue;
            if(sword.huahongStage()==0&&!TRAILS.containsKey(sword))continue;
            var trail=TRAILS.get(sword);
            if(trail==null||trail.skin!=sword.huahongSkin()){
                trail=new Trail();trail.skin=sword.huahongSkin();TRAILS.put(sword,trail);
            }
            if(sword.huahongStrength()>.001F){
                Vec3 center=sword.position().add(0,.85,0);trail.last=center;trail.lastEmission=tick;
                if(tick%3==0&&sword.flightSpeed()>.05){
                    long seed=tick+sword.getUUID().hashCode();
                    if(random(seed)<6.0/14){
                        Vec3 f=sword.getLookAngle().normalize(),side=perpendicular(f),up=side.cross(f).normalize();
                        trail.sparks.addLast(new Spark(center,f,side,up,tick,random(seed+1)*Math.PI*2,
                                (.18+random(seed+2)*.55)*SPREAD,.3+random(seed+4)));
                    }
                }
            }
        }
        TRAILS.entrySet().removeIf(e->{
            if(mc.player!=null&&e.getKey().distanceToSqr(mc.player)>4096)return true;
            var t=e.getValue();t.sparks.removeIf(s->tick-s.born()>33);
            if((e.getKey().isRemoved()||e.getKey().huahongStage()==0)&&tick-t.lastEmission>LIFE)return true;
            if(t.last!=null)t.history.sample(point(t.last),tick,Math.max(8,e.getKey().flightSpeed()*1.7+3));
            return false;
        });
    }
    public static void draw(VertexConsumer buffer,PoseStack.Pose pose,Vec3 eye,double now,float partial){
        vertices=0;var mc=Minecraft.getInstance();
        for(var entry:TRAILS.entrySet()){
            var sword=entry.getKey();var trail=entry.getValue();
            if(mc.player!=null&&mc.options.getCameraType().isFirstPerson()&&sword==mc.player.getVehicle())continue;
            if(trail.last==null||trail.last.distanceToSqr(eye)>4096)continue;
            var frame=InterpolatedFlyingSwordTransform.get(sword,partial);
            Vec3 center=frame.position().add(0,.85,0),forward=frame.forward();
            boolean active=!sword.isRemoved()&&sword.huahongStrength()>.001F;
            var nodes=active?trail.history.renderNodes(now,point(center)):trail.history.nodes();
            if(trail.skin!=cn.blockforge.generated.generatedmod.yujian.HuahongSkin.METEOR_PRISM){
                vertices+=HuahongSkinGeometry.draw(trail.skin,buffer,pose,eye,center,forward,nodes,now,active?sword.renderHuahong(partial):0);
                continue;
            }
            double origin=nodes.isEmpty()?0:Math.floor(nodes.get(0).arc()/256)*256;
            ArrayList<Row> rows=new ArrayList<>(nodes.size());
            for(var n:nodes){
                if(n.tick()>now)continue;
                double age=Math.max(0,(now-n.tick())/20),left=clamp(1-age/3.8);
                rows.add(new Row(vec(n.point()),n.arc()-origin,Math.pow(left,1.3)*(.22+.78*Math.exp(-age*.35)),
                        (float)Math.pow(left,1.7),mix(0x8274ff,0xe8faff,Math.exp(-age*2))));
            }
            strip(buffer,pose,eye,rows,WIDTH*1.4,.10F,10);
            strip(buffer,pose,eye,rows,WIDTH*.42,.62F,6);
            strip(buffer,pose,eye,rows,.008+WIDTH*.10,.85F,8);
            // Offset each filament from its own world-path tangent, not the current head axis.
            for(int strand=0;strand<4;strand++){
                rows.clear();double phase=strand*2.399;
                for(int i=0;i<nodes.size();i++){
                    var n=nodes.get(i);if(n.tick()>now)continue;
                    double age=Math.max(0,(now-n.tick())/20),left=clamp(1-age/3.8);
                    Vec3 f=vec(nodes.get(Math.min(nodes.size()-1,i+1)).point()).subtract(vec(nodes.get(Math.max(0,i-1)).point()));
                    if(f.lengthSqr()<1e-10)f=forward;else f=f.normalize();
                    Vec3 side=perpendicular(f),up=side.cross(f).normalize();
                    double curl=Math.sin(n.arc()*.65+strand*1.7)*.24+Math.sin(n.arc()*1.73+strand)*.11;
                    double open=smooth((age-.025)/.375),spread=open*SPREAD*.035*(1+age*.24);
                    Vec3 p=vec(n.point()).add(side.scale(Math.cos(phase+curl)*spread)).add(up.scale(Math.sin(phase+curl)*spread));
                    rows.add(new Row(p,n.arc()-origin+strand*.93,.15+.85*Math.pow(left,1.2),
                            (float)Math.pow(left,1.5),mix(0x8274ff,0xe8faff,Math.exp(-age*2)*.5)));
                }
                strip(buffer,pose,eye,rows,WIDTH*.16,.40F,6);
            }
            if(active){
                float strength=(float)Math.pow(sword.renderHuahong(partial),1.4);rows.clear();
                for(int i=0;i<=68;i++){
                    double q=i/68.0,w=Math.max(.012,Math.pow(Math.max(0,Math.sin(Math.PI*q)),.65)*Math.exp(-q*2.7));
                    rows.add(new Row(center.add(forward.scale(.26-q*HEAD_LENGTH)),-q*HEAD_LENGTH,w,strength*(float)(1-q),0xe8faff));
                }
                strip(buffer,pose,eye,rows,WIDTH*2.1,.11F,10);
                strip(buffer,pose,eye,rows,WIDTH*.44,1.0F,8);
                Vec3 side=perpendicular(forward),up=side.cross(forward).normalize();
                for(int strand=0;strand<4;strand++){
                    rows.clear();double phase=strand*2.399;
                    for(int i=0;i<=60;i++){
                        double q=i/60.0,reach=Math.sin(Math.PI*q)*SPREAD*.075;
                        double angle=phase+q*.5+Math.sin(q*7-now/20*1.3+strand)*q*.22;
                        Vec3 p=center.add(forward.scale(.17-q*HEAD_LENGTH*(.55+random(strand+7)*.45)))
                                .add(side.scale(Math.cos(angle)*reach)).add(up.scale(Math.sin(angle)*reach));
                        rows.add(new Row(p,-q*HEAD_LENGTH+strand*.83,Math.pow(Math.max(0,Math.sin(Math.PI*q)),.65)*(1-q*.65),
                                strength*(float)(1-q),mix(0x8274ff,0xe8faff,Math.pow(1-q,4)*.8)));
                    }
                    strip(buffer,pose,eye,rows,WIDTH*.3,.36F,6);
                }
            }
            for(var spark:trail.sparks){
                double age=(now-spark.born())/20;if(age<.035||age>=1.65)continue;rows.clear();
                for(int i=0;i<=8;i++){
                    double t=Math.max(0,age-i*.016);
                    Vec3 p=spark.origin().add(spark.forward().scale(-spark.backward()*t-.52*t*t))
                            .add(spark.side().scale(Math.cos(spark.phase())*spark.drift()*t))
                            .add(spark.up().scale(Math.sin(spark.phase())*spark.drift()*t-.11*t*t));
                    rows.add(new Row(p,t*4,Math.max(0,Math.sin(Math.PI*i/8)),(float)Math.pow(1-age/1.65,1.6),0xe8faff));
                }
                strip(buffer,pose,eye,rows,.008+WIDTH*.03,.35F,8);
            }
        }
    }
    private static void strip(VertexConsumer b,PoseStack.Pose pose,Vec3 eye,List<Row> rows,double width,float opacity,float mode){
        Vec3 previous=null,previousSide=null;Row old=null;
        for(int i=0;i<rows.size();i++){
            Row row=rows.get(i);Vec3 f=rows.get(Math.min(rows.size()-1,i+1)).point().subtract(rows.get(Math.max(0,i-1)).point());
            if(f.lengthSqr()<1e-12)continue;f=f.normalize();Vec3 side=f.cross(eye.subtract(row.point()));
            if(side.lengthSqr()<1e-10)side=perpendicular(f);else side=side.normalize();
            if(previousSide!=null&&side.dot(previousSide)<0)side=side.scale(-1);
            side=side.scale(width*row.width());
            if(previous!=null){
                vertex(b,pose,eye,previous.subtract(previousSide),(float)old.arc(),mode,old.alpha()*opacity,old.color());
                vertex(b,pose,eye,previous.add(previousSide),(float)old.arc(),mode+1,old.alpha()*opacity,old.color());
                vertex(b,pose,eye,row.point().add(side),(float)row.arc(),mode+1,row.alpha()*opacity,row.color());
                vertex(b,pose,eye,row.point().subtract(side),(float)row.arc(),mode,row.alpha()*opacity,row.color());
            }
            previous=row.point();previousSide=side;old=row;
        }
    }
    private static void vertex(VertexConsumer b,PoseStack.Pose pose,Vec3 eye,Vec3 p,float u,float v,float alpha,int color){
        Vec3 r=p.subtract(eye);b.vertex(pose.pose(),(float)r.x,(float)r.y,(float)r.z).uv(u,v)
                .color((color>>16)&255,(color>>8)&255,color&255,Mth.clamp(Math.round(alpha*255),0,255)).endVertex();vertices++;
    }
    private static double clamp(double x){return Math.max(0,Math.min(1,x));}
    private static double smooth(double x){x=clamp(x);return x*x*(3-2*x);}
    private static double random(long x){double a=Math.sin(x*71.371+19.713)*15317.317;return a-Math.floor(a);}
    private static int mix(int a,int b,double t){t=clamp(t);return ((int)(((a>>16)&255)*(1-t)+((b>>16)&255)*t)<<16)|((int)(((a>>8)&255)*(1-t)+((b>>8)&255)*t)<<8)|(int)((a&255)*(1-t)+(b&255)*t);}
    private static Vec3 perpendicular(Vec3 axis){Vec3 s=axis.cross(Math.abs(axis.y)>.95?new Vec3(0,0,1):new Vec3(0,1,0));return s.normalize();}
    private static SwordFlightTrailHistory.Point point(Vec3 p){return new SwordFlightTrailHistory.Point(p.x,p.y,p.z);}
    private static Vec3 vec(SwordFlightTrailHistory.Point p){return new Vec3(p.x(),p.y(),p.z());}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void hide(net.minecraftforge.client.event.RenderPlayerEvent.Pre event){
        if(cn.blockforge.generated.generatedmod.yujian.FlyingSwordConfig.TRAILS.get()
                &&event.getEntity().getVehicle() instanceof ControlledSwordEntity sword&&sword.huahongStrength()>=.995F)event.setCanceled(true);
    }
    @SubscribeEvent public static void hand(net.minecraftforge.client.event.RenderHandEvent event){
        var p=Minecraft.getInstance().player;
        if(cn.blockforge.generated.generatedmod.yujian.FlyingSwordConfig.TRAILS.get()
                &&p!=null&&p.getVehicle() instanceof ControlledSwordEntity sword&&sword.huahongStrength()>.5F)event.setCanceled(true);
    }
    @SubscribeEvent public static void edge(net.minecraftforge.client.event.RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();if(!cn.blockforge.generated.generatedmod.yujian.FlyingSwordConfig.TRAILS.get()||mc.player==null||!mc.options.getCameraType().isFirstPerson()
                ||!(mc.player.getVehicle() instanceof ControlledSwordEntity sword))return;
        float s=sword.renderHuahong(mc.getFrameTime());if(s<.01)return;
        int w=event.getGuiGraphics().guiWidth(),h=event.getGuiGraphics().guiHeight();
        int color=((int)(26*s)<<24)|0x78dfff;
        event.getGuiGraphics().fillGradient(0,0,Math.max(3,w/32),h,color,0);
        event.getGuiGraphics().fillGradient(w-Math.max(3,w/32),0,w,h,0,color);
    }
    private HuahongVisuals(){}
}
