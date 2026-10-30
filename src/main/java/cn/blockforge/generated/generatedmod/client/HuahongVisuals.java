package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import cn.blockforge.generated.generatedmod.yujian.SwordProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.IdentityHashMap;
import java.util.Map;

/** Narrow, world-history rainbow in the sword's native/late depth and selective bloom passes. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class HuahongVisuals {
    private static final class Trail {
        final SwordFlightTrailHistory history=new SwordFlightTrailHistory(.45,28,180,512);
        Vec3 last,origin,axis;
        double born=-100;
        long lastEmission;
        int stage;
        boolean golden;
    }
    private static final Map<ControlledSwordEntity,Trail> TRAILS=new IdentityHashMap<>();
    private static Object level;
    public static int vertices;
    public static boolean hasTrails(){return !TRAILS.isEmpty();}
    public static void clear(){TRAILS.clear();vertices=0;}
    public static void sample(long tick){
        var mc=Minecraft.getInstance();
        if(level!=mc.level){TRAILS.clear();level=mc.level;}
        if(mc.level==null)return;
        for(var entity:mc.level.entitiesForRendering()){
            if(!(entity instanceof ControlledSwordEntity sword)||!sword.flightRoot())continue;
            if(sword.huahongStage()==0&&!TRAILS.containsKey(sword))continue;
            var trail=TRAILS.computeIfAbsent(sword,s->new Trail());
            Vec3 center=sword.position().add(0,.85,0);
            if(sword.huahongStage()==1&&trail.stage!=1){
                trail.origin=center;trail.axis=sword.getLookAngle();trail.born=tick;
            }
            trail.stage=sword.huahongStage();trail.golden=SwordProfile.of(sword.display()).tier()>=7;
            if(sword.huahongStrength()>.001F){trail.last=center;trail.lastEmission=tick;}
        }
        TRAILS.entrySet().removeIf(e->{
            var t=e.getValue();
            // The generic sampler retains a resting point after its old nodes age out.
            // Retire the instance explicitly so recalled swords do not stay in this map.
            if((e.getKey().isRemoved()||e.getKey().huahongStage()==0)&&tick-t.lastEmission>28)return true;
            if(t.last!=null)t.history.sample(new SwordFlightTrailHistory.Point(t.last.x,t.last.y,t.last.z),tick,
                    Math.max(8,e.getKey().flightSpeed()*1.7+3));
            return t.history.nodes().isEmpty()&&(e.getKey().isRemoved()||e.getKey().huahongStage()==0);
        });
    }
    public static void draw(VertexConsumer buffer,PoseStack.Pose pose,Vec3 eye,double now,float partial){
        vertices=0;var mc=Minecraft.getInstance();
        for(var entry:TRAILS.entrySet()){
            var sword=entry.getKey();var trail=entry.getValue();
            if(mc.options.getCameraType().isFirstPerson()&&mc.player!=null&&sword==mc.player.getVehicle())continue;
            var nodes=trail.history.nodes();if(nodes.isEmpty())continue;
            double arc=nodes.get(nodes.size()-1).arc();
            double origin= Math.floor(arc/256)*256;
            for(int i=1;i<nodes.size();i++){
                var a=nodes.get(i-1);var b=nodes.get(i);
                if(b.tick()>now)continue;
                double tail=Mth.clamp(1-(arc-b.arc())/180,0,1);
                float alpha=(float)(Math.pow(Mth.clamp(1-(now-b.tick())/28,0,1),1.3)*Math.pow(tail,.65));
                double width=(trail.golden?.26:.30)*(.25+.75*tail);
                ribbon(buffer,pose,eye,vec(a.point()),vec(b.point()),width,alpha,
                        (float)(a.arc()-origin),(float)(b.arc()-origin),6,trail.golden);
            }
            if(!sword.isRemoved()&&sword.huahongStrength()>.001F){
                var frame=InterpolatedFlyingSwordTransform.get(sword,partial);
                Vec3 center=frame.position().add(0,.85,0),forward=frame.forward();
                float strength=sword.renderHuahong(partial);
                // U increases towards the actual leading tip. The concentrated glow
                // is anchored there, separately from the trailing axial gradient.
                Vec3 tip=center.add(forward.scale(.8));
                ribbon(buffer,pose,eye,center.subtract(forward.scale(1.4)),tip,
                        .16*strength,strength,0,1,8,trail.golden);
                tipGlow(buffer,pose,eye,tip,.38*strength,strength,trail.golden);
                var last=nodes.get(nodes.size()-1);
                if(vec(last.point()).distanceToSqr(center)<Math.pow(Math.max(8,sword.flightSpeed()*2),2))
                    ribbon(buffer,pose,eye,vec(last.point()),center,.28,strength,
                            (float)(last.arc()-origin),(float)(last.arc()-origin+vec(last.point()).distanceTo(center)),6,trail.golden);
                if(sword.huahongStage()==1){
                    Vec3 side=perpendicular(forward),up=side.cross(forward).normalize();
                    for(int strand=0;strand<3;strand++){
                        Vec3 previous=null;
                        for(int i=0;i<=22;i++){
                            double t=i/22.0,angle=t*3.6+strand*Math.PI*2/3+now*.2;
                            double radius=(1-strength)*.8*(1-t);
                            Vec3 p=center.subtract(forward.scale((1-t)*1.8)).add(side.scale(Math.cos(angle)*radius)).add(up.scale(Math.sin(angle)*radius));
                            if(previous!=null)ribbon(buffer,pose,eye,previous,p,.055,(float)(t*.65),0,t==1?1:(float)t,6,trail.golden);
                            previous=p;
                        }
                    }
                }
            }
            double age=now-trail.born;
            if(trail.origin!=null&&age>=0&&age<10){
                Vec3 side=perpendicular(trail.axis),up=side.cross(trail.axis).normalize();
                double radius=.15+age*.095;
                for(int i=0;i<48;i++){
                    if(i%16>11)continue;
                    double a=i*Math.PI*2/48,b=(i+1)*Math.PI*2/48;
                    Vec3 p=trail.origin.add(side.scale(Math.cos(a)*radius)).add(up.scale(Math.sin(a)*radius));
                    Vec3 q=trail.origin.add(side.scale(Math.cos(b)*radius)).add(up.scale(Math.sin(b)*radius));
                    ribbon(buffer,pose,eye,p,q,.04,(float)((1-age/10)*.55),0,1,6,trail.golden);
                }
            }
        }
    }
    private static Vec3 vec(SwordFlightTrailHistory.Point p){return new Vec3(p.x(),p.y(),p.z());}
    private static void tipGlow(VertexConsumer b,PoseStack.Pose pose,Vec3 eye,Vec3 tip,double radius,float alpha,boolean gold){
        Vec3 facing=eye.subtract(tip);if(facing.lengthSqr()<1e-8)return;
        facing=facing.normalize();Vec3 right=perpendicular(facing).scale(radius),up=facing.cross(right);
        vertex(b,pose,eye,tip.subtract(right).subtract(up),0,10,alpha,gold);
        vertex(b,pose,eye,tip.add(right).subtract(up),1,10,alpha,gold);
        vertex(b,pose,eye,tip.add(right).add(up),1,11,alpha,gold);
        vertex(b,pose,eye,tip.subtract(right).add(up),0,11,alpha,gold);
    }
    private static Vec3 perpendicular(Vec3 axis){
        Vec3 side=axis.cross(new Vec3(0,1,0));return side.lengthSqr()<1e-8?new Vec3(1,0,0):side.normalize();
    }
    private static void ribbon(VertexConsumer b,PoseStack.Pose pose,Vec3 eye,Vec3 a,Vec3 c,double width,float alpha,float u,float v,float kind,boolean gold){
        Vec3 direction=c.subtract(a);if(direction.lengthSqr()<1e-10||alpha<.002)return;
        Vec3 side=direction.cross(eye.subtract(a));
        side=(side.lengthSqr()<1e-10?perpendicular(direction.normalize()):side.normalize()).scale(width);
        vertex(b,pose,eye,a.subtract(side),u,kind,alpha,gold);vertex(b,pose,eye,a.add(side),u,kind+1,alpha,gold);
        vertex(b,pose,eye,c.add(side),v,kind+1,alpha,gold);vertex(b,pose,eye,c.subtract(side),v,kind,alpha,gold);
    }
    private static void vertex(VertexConsumer b,PoseStack.Pose pose,Vec3 eye,Vec3 p,float u,float v,float alpha,boolean gold){
        Vec3 r=p.subtract(eye);b.vertex(pose.pose(),(float)r.x,(float)r.y,(float)r.z).uv(u,v)
                .color(gold?.95F:.32F,gold?.78F:.85F,gold?.38F:.94F,alpha).endVertex();vertices++;
    }
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
