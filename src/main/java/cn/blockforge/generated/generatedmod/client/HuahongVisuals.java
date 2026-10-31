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

/** Forge consumer of minecraft-huahong's current independent meteor_prism geometry.
 * Preserves its full nucleus and the 2026-10-31 forward glow coverage correction.
 * No user-supplied Meteor Test111 / V13 reference shader is copied here. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class HuahongVisuals {
    private static final double WIDTH=.19,LIFE=116,SPREAD=.65,HEAD_LENGTH=.6+2.5*.65;
    private record Spark(Vec3 origin,Vec3 forward,Vec3 side,Vec3 up,long born,double phase,double drift,double backward){}
    private record Row(Vec3 point,double arc,double width,float alpha,int color,Vec3 side){
        Row(Vec3 point,double arc,double width,float alpha,int color){this(point,arc,width,alpha,color,null);}
    }
    private static final class Trail {
        final SwordFlightTrailHistory history=new SwordFlightTrailHistory(.25,LIFE,160,720);
        final ArrayDeque<Spark> sparks=new ArrayDeque<>();
        Vec3 last;long lastEmission;
        cn.blockforge.generated.generatedmod.yujian.HuahongSkin skin;
    }
    private static final Map<ControlledSwordEntity,Trail> TRAILS=new IdentityHashMap<>();
    private static Object level;
    private static double corePixelScale;
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
        double projectionY=Math.abs(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix().m11());
        corePixelScale=Double.isFinite(projectionY)&&projectionY>1e-6
                ?.85/(projectionY*Math.max(1,mc.getMainRenderTarget().viewHeight)):0;
        for(var entry:TRAILS.entrySet()){
            var sword=entry.getKey();var trail=entry.getValue();
            if(mc.player!=null&&mc.options.getCameraType().isFirstPerson()&&sword==mc.player.getVehicle())continue;
            if(trail.last==null||trail.last.distanceToSqr(eye)>4096)continue;
            var frame=InterpolatedFlyingSwordTransform.get(sword,partial);
            Vec3 center=frame.position().add(0,.85,0),forward=frame.forward();
            boolean active=!sword.isRemoved()&&sword.huahongStrength()>.001F;
            var nodes=active?trail.history.renderNodes(now,point(center)):trail.history.nodes();
            Vec3 modelSide=frame.local(1,0,0).subtract(frame.position());
            if(trail.skin==cn.blockforge.generated.generatedmod.yujian.HuahongSkin.DENSE){
                var clip=new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix())
                        .mul(com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix()).mul(pose.pose());
                HuahongVolumeRenderer.queue(nodes,eye,now,active?sword.renderHuahong(partial):0,clip,1);
            }
            if(trail.skin!=cn.blockforge.generated.generatedmod.yujian.HuahongSkin.METEOR_PRISM){
                vertices+=HuahongSkinGeometry.draw(trail.skin,buffer,pose,eye,center,forward,modelSide,nodes,now,active?sword.renderHuahong(partial):0);
                continue;
            }
            var path=new HuahongFlightPath(nodes,center,forward,modelSide);
            double origin=nodes.isEmpty()?0:Math.floor(nodes.get(0).arc()/256)*256;
            ArrayList<Row> rows=new ArrayList<>(nodes.size());
            for(int i=0;i<nodes.size();i++){
                var n=nodes.get(i);
                if(n.tick()>now)continue;
                double age=Math.max(0,(now-n.tick())/20),left=clamp(1-age/3.8);
                Vec3 tangent=vec(nodes.get(Math.min(nodes.size()-1,i+1)).point()).subtract(vec(nodes.get(Math.max(0,i-1)).point()));
                Vec3 side=path.frame(vec(n.point()),tangent).side();
                rows.add(new Row(vec(n.point()),n.arc()-origin,Math.pow(left,1.3)*(.22+.78*Math.exp(-age*.35)),
                        (float)Math.pow(left,1.7),mix(0x8274ff,0xe8faff,Math.exp(-age*2)),side));
            }
            strip(buffer,pose,eye,rows,WIDTH*1.4,.045F,10,path);
            strip(buffer,pose,eye,rows,WIDTH*.42,.35F,6,path);
            strip(buffer,pose,eye,rows,.008+WIDTH*.045,.48F,8,path);
            // Offset each filament from its own world-path tangent, not the current head axis.
            for(int strand=0;strand<4;strand++){
                rows.clear();double phase=strand*2.399;
                for(int i=0;i<nodes.size();i++){
                    var n=nodes.get(i);if(n.tick()>now)continue;
                    double age=Math.max(0,(now-n.tick())/20),left=clamp(1-age/3.8);
                    Vec3 f=vec(nodes.get(Math.min(nodes.size()-1,i+1)).point()).subtract(vec(nodes.get(Math.max(0,i-1)).point()));
                    if(f.lengthSqr()<1e-10)f=forward;else f=f.normalize();
                    var section=path.frame(vec(n.point()),f);Vec3 side=section.side(),up=section.up();
                    double curl=Math.sin(n.arc()*.65+strand*1.7)*.24+Math.sin(n.arc()*1.73+strand)*.11;
                    double open=smooth((age-.025)/.375),spread=open*SPREAD*.035*(1+age*.24);
                    Vec3 p=vec(n.point()).add(side.scale(Math.cos(phase+curl)*spread)).add(up.scale(Math.sin(phase+curl)*spread));
                    rows.add(new Row(p,n.arc()-origin+strand*.93,.15+.85*Math.pow(left,1.2),
                            (float)Math.pow(left,1.5),mix(0x8274ff,0xe8faff,Math.exp(-age*2)*.5),side));
                }
                strip(buffer,pose,eye,rows,WIDTH*.16,.24F,6,path);
            }
            if(active){
                float strength=(float)Math.pow(sword.renderHuahong(partial),1.4);rows.clear();
                for(int i=0;i<=68;i++){
                    double q=i/68.0,w=Math.max(.012,Math.pow(Math.max(0,Math.sin(Math.PI*q)),.65)*Math.exp(-q*2.7));
                    var section=path.behind(q*HEAD_LENGTH);
                    rows.add(new Row(section.p().add(forward.scale(.26)),-q*HEAD_LENGTH,w,strength*(float)(1-q),0xe8faff,section.side()));
                }
                strip(buffer,pose,eye,rows,WIDTH*2.1,.054F,10,path);
                strip(buffer,pose,eye,rows,WIDTH*.19,.68F,8,path);
                noseGlow(buffer,pose,eye,center,forward,strength);
                for(int strand=0;strand<4;strand++){
                    rows.clear();double phase=strand*2.399;
                    for(int i=0;i<=60;i++){
                        double q=i/60.0,reach=Math.sin(Math.PI*q)*SPREAD*.075;
                        double angle=phase+q*.5+Math.sin(q*7-now/20*1.3+strand)*q*.22;
                        var section=path.behind(q*HEAD_LENGTH*(.55+random(strand+7)*.45));
                        Vec3 p=section.p().add(forward.scale(.17))
                                .add(section.side().scale(Math.cos(angle)*reach)).add(section.up().scale(Math.sin(angle)*reach));
                        rows.add(new Row(p,-q*HEAD_LENGTH+strand*.83,Math.pow(Math.max(0,Math.sin(Math.PI*q)),.65)*(1-q*.65),
                                strength*(float)(1-q),mix(0x8274ff,0xe8faff,Math.pow(1-q,4)*.8),section.side()));
                    }
                    strip(buffer,pose,eye,rows,WIDTH*.3,.22F,6,path);
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
        strip(b,pose,eye,rows,width,opacity,mode,null);
    }
    /** Project a prolate glow envelope without normalizing projected flight direction.
     * The positive matrix square root stays continuous even when looking directly
     * into the nose, where a direction-normalized billboard flips its long axis. */
    private static void noseGlow(VertexConsumer b,PoseStack.Pose pose,Vec3 eye,Vec3 center,Vec3 forward,float strength){
        var rotation=Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        var r=new org.joml.Vector3f(1,0,0).rotate(rotation);
        var u=new org.joml.Vector3f(0,1,0).rotate(rotation);
        Vec3 right=new Vec3(r.x,r.y,r.z),up=new Vec3(u.x,u.y,u.z);
        double major=.46+WIDTH*.85,minor=.075+WIDTH*.30;
        double fx=forward.dot(right),fy=forward.dot(up),stretch=major*major-minor*minor;
        double xx=minor*minor+stretch*fx*fx,xy=stretch*fx*fy,yy=minor*minor+stretch*fy*fy;
        double detRoot=Math.sqrt(Math.max(0,xx*yy-xy*xy));
        double denominator=Math.sqrt(xx+yy+2*detRoot);
        Vec3 axisX=right.scale((xx+detRoot)/denominator).add(up.scale(xy/denominator));
        Vec3 axisY=right.scale(xy/denominator).add(up.scale((yy+detRoot)/denominator));
        Vec3 glowCenter=center.add(forward.scale(.14));float alpha=strength*.181F;
        vertex(b,pose,eye,glowCenter.subtract(axisX).subtract(axisY),0,32,alpha,0xd3dfff);
        vertex(b,pose,eye,glowCenter.add(axisX).subtract(axisY),1,32,alpha,0xd3dfff);
        vertex(b,pose,eye,glowCenter.add(axisX).add(axisY),1,33,alpha,0xd3dfff);
        vertex(b,pose,eye,glowCenter.subtract(axisX).add(axisY),0,33,alpha,0xd3dfff);
    }
    private static void strip(VertexConsumer b,PoseStack.Pose pose,Vec3 eye,List<Row> rows,double width,float opacity,float mode,HuahongFlightPath localPath){
        if(localPath==null){strip(b,pose,eye,rows,width,opacity,mode,null,false);return;}
        // Crossed transport planes keep the local nucleus visible from every
        // side without an axis-dependent billboard roll. Split its opacity.
        strip(b,pose,eye,rows,width,opacity*.70710677F,mode,localPath,false);
        strip(b,pose,eye,rows,width,opacity*.70710677F,mode,localPath,true);
    }
    private static void strip(VertexConsumer b,PoseStack.Pose pose,Vec3 eye,List<Row> rows,double width,float opacity,float mode,HuahongFlightPath localPath,boolean crossPlane){
        Vec3 previous=null,previousSide=null;Row old=null;float previousAlpha=0;
        for(int i=0;i<rows.size();i++){
            Row row=rows.get(i);Vec3 f=rows.get(Math.min(rows.size()-1,i+1)).point().subtract(rows.get(Math.max(0,i-1)).point());
            if(f.lengthSqr()<1e-12)continue;f=f.normalize();
            Vec3 side=row.side()!=null?row.side().subtract(f.scale(row.side().dot(f))):localPath==null?f.cross(eye.subtract(row.point())):localPath.ribbonSide(row.point(),f,eye);
            if(crossPlane)side=f.cross(side);
            if(side.lengthSqr()<1e-10&&previousSide!=null)side=previousSide.subtract(f.scale(previousSide.dot(f)));
            if(side.lengthSqr()<1e-10)side=perpendicular(f);else side=side.normalize();
            if(previousSide!=null&&side.dot(previousSide)<0)side=side.scale(-1);
            double physical=width*row.width(),covered=mode==8?coreHalfWidth(row.point(),side,eye,physical):physical;
            float alpha=row.alpha()*opacity*(float)(covered>0?physical/covered:1);
            side=side.scale(covered);
            if(previous!=null){
                vertex(b,pose,eye,previous.subtract(previousSide),(float)old.arc(),mode,previousAlpha,old.color());
                vertex(b,pose,eye,previous.add(previousSide),(float)old.arc(),mode+1,previousAlpha,old.color());
                vertex(b,pose,eye,row.point().add(side),(float)row.arc(),mode+1,alpha,row.color());
                vertex(b,pose,eye,row.point().subtract(side),(float)row.arc(),mode,alpha,row.color());
            }
            previous=row.point();previousSide=side;old=row;previousAlpha=alpha;
        }
    }
    /** Smooth pixel coverage for thin axial ridges; callers conserve energy by
     * multiplying alpha by physical/covered width. The soft nose has its own envelope. */
    static double coreHalfWidth(Vec3 p,Vec3 side,Vec3 eye,double physical){
        if(!(physical>0)||!(corePixelScale>0))return Math.max(0,physical);
        Vec3 view=p.subtract(eye);double distance=view.length();
        if(distance<1e-6)return physical;
        double facing=side.cross(view.scale(1/distance)).length();
        double pixels=distance*corePixelScale/Math.max(.12,facing);
        return Math.max(physical,Math.min(physical*8,pixels));
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
