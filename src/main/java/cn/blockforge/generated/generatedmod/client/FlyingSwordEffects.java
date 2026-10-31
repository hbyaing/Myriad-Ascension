package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.client.realmflight.MeteorBloomRenderer;
import cn.blockforge.generated.generatedmod.yujian.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Fixed-tick real sword history with one flowing blade plume and optional roll helix. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class FlyingSwordEffects {
    private static final Map<ControlledSwordEntity,SwordFlightTrailHistory> TRAILS=new IdentityHashMap<>();
    private static final MultiBufferSource.BufferSource BUFFERS=MultiBufferSource.immediate(new BufferBuilder(65536));
    public static int renderedSegments;
    public static int normalTrailVertexCount,spiralVertexCount;
    public static boolean normalTrailSubmitted,spiralSubmitted,shaderPackDetected;
    public static String activeTrailRenderPath="未绘制",renderStage="未绘制";
    public static int activeRenderTarget;
    private static final BufferBuilder POST_BUFFER=new BufferBuilder(65536);
    private static final BufferBuilder EMISSION_BUFFER=new BufferBuilder(65536);
    private static BufferBuilder.RenderedBuffer pending;
    private static BufferBuilder.RenderedBuffer pendingEmission;
    private static org.joml.Matrix4f projection;
    private static org.joml.Matrix4f view,inverseProjection;
    private static Vec3 cameraPosition=Vec3.ZERO;
    private static final SwordTrailPhase.TickClock TRAIL_CLOCK=new SwordTrailPhase.TickClock();
    private static long capturedTrailTick;
    private static float effectTime;
    private static boolean postPath,spiralPass;
    public record TrailDiagnostics(int swordId,int historyNodes,double firstArc,double headArc,double origin,
            float headCoord,float time,float strength,double width,Vec3 attachment,Vec3 renderedHead){}
    private static final java.util.List<TrailDiagnostics> DIAGNOSTICS=new java.util.ArrayList<>();
    public static java.util.List<TrailDiagnostics> trailDiagnostics(){return java.util.List.copyOf(DIAGNOSTICS);}
    public static float capturedEffectTime(){return effectTime;}
    /** Actual fixed sampler clock and the same clock captured by this render frame. */
    public static long samplerTick(){return TRAIL_CLOCK.tick();}
    public static long capturedTrailTick(){return capturedTrailTick;}
    private static void syncTrailLevel(Object level){
        if(TRAIL_CLOCK.setLevel(level)){
            TRAILS.clear();DIAGNOSTICS.clear();capturedTrailTick=0;effectTime=0;
        }
    }

    @SubscribeEvent public static void sampleTick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        var client=Minecraft.getInstance();
        syncTrailLevel(client.level);
        TRAIL_CLOCK.advance(client.level==null?0:client.level.getGameTime(),!client.isPaused());
        if(client.level==null||client.player==null||!FlyingSwordConfig.TRAILS.get()){TRAILS.clear();HuahongVisuals.clear();return;}
        if(client.isPaused()||!TRAIL_CLOCK.initialized())return;
        TRAILS.keySet().removeIf(s->s.isRemoved()||s.level()!=client.level||!s.flightRoot()||s.distanceToSqr(client.player)>4096);
        if(client.options.getCameraType().isFirstPerson()&&client.player.getVehicle() instanceof ControlledSwordEntity own)TRAILS.remove(own);
        HuahongVisuals.sample(TRAIL_CLOCK.tick());
        int count=0;
        for(var entity:client.level.entitiesForRendering()){
            if(!(entity instanceof ControlledSwordEntity sword)||!sword.flightRoot()||sword.distanceToSqr(client.player)>4096)continue;
            if(client.options.getCameraType().isFirstPerson()&&sword==client.player.getVehicle())continue;
            if(++count>32)break;
            // Read the tick pose directly; the shared render-frame transform may
            // still contain an older partial tick and must not seed history.
            Vec3 anchor=SwordFlightAttachment.tail(sword,sword.position(),sword.getYRot(),sword.getXRot(),sword.renderRoll(1));
            // Synced speed already describes server-validated displacement. Fast Dunxu
            // can exceed eight blocks/tick; that is not a teleport. Anchor rotation and
            // interpolation receive a small bounded allowance without accepting jumps.
            double allowedStep=Math.max(8,Math.max(sword.flightSpeed(),sword.renderSpeed(1))*1.5+2);
            TRAILS.computeIfAbsent(sword,s->new SwordFlightTrailHistory()).sample(point(anchor),TRAIL_CLOCK.tick(),allowedStep);
        }
    }
    private static SwordFlightTrailHistory.Point point(Vec3 p){return new SwordFlightTrailHistory.Point(p.x,p.y,p.z);}
    private static Vec3 vec(SwordFlightTrailHistory.Point p){return new Vec3(p.x(),p.y(),p.z());}

    @SubscribeEvent public static void beginFrame(net.minecraftforge.event.TickEvent.RenderTickEvent event){
        if(event.phase==net.minecraftforge.event.TickEvent.Phase.END){
            if(pending!=null||pendingEmission!=null){
                renderStage="RenderTickEvent.END";
                submitPending();
            }
            return;
        }
        if(pending!=null){pending.release();pending=null;}
        if(pendingEmission!=null){pendingEmission.release();pendingEmission=null;}
        TrailShaderBridge.resetFrame();
        renderedSegments=normalTrailVertexCount=spiralVertexCount=0;
        DIAGNOSTICS.clear();
        normalTrailSubmitted=spiralSubmitted=false;
        syncTrailLevel(Minecraft.getInstance().level);
        if(Minecraft.getInstance().level==null){TRAILS.clear();TrailShaderBridge.release();}
    }
    private static void submitPending(){
        if(pending!=null){
            var mesh=pending;pending=null;
            activeRenderTarget=Minecraft.getInstance().getMainRenderTarget().frameBufferId;
            boolean submitted=SwordFlightTrailRenderer.drawPost(mesh,projection,effectTime);
            normalTrailSubmitted=submitted&&normalTrailVertexCount>0;
            spiralSubmitted=submitted&&spiralVertexCount>0;
        }
        if(pendingEmission!=null){
            var mesh=pendingEmission;pendingEmission=null;
            MeteorBloomRenderer.drawSword(mesh,projection,view,inverseProjection);
        }
    }
    public static boolean shaderCompat(){return postPath;}
    private static boolean usePostPath(){
        shaderPackDetected=TrailShaderBridge.shaderPackDetected();
        return switch(FlyingSwordConfig.TRAIL_PATH.get()){
            case AUTO -> shaderPackDetected;
            case NORMAL_TRAIL_RENDER_PATH -> false;
            case SHADER_COMPAT_TRAIL_RENDER_PATH,POST_SHADER_TRAIL_RENDER_PATH -> true;
        };
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS){
            // Save terrain/entity/glass depth before cloud rendering writes its
            // depth pass. Cloud depth is not an opaque obstacle for the plume.
            if((!TRAILS.isEmpty()||HuahongVisuals.hasTrails())&&FlyingSwordConfig.TRAILS.get()){TrailShaderBridge.resetFrame();TrailShaderBridge.captureDepth();}
            return;
        }
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var client=Minecraft.getInstance();
        syncTrailLevel(client.level);
        if(client.level==null||client.player==null||!FlyingSwordConfig.TRAILS.get()){TRAILS.clear();return;}
        if((TRAILS.isEmpty()&&!HuahongVisuals.hasTrails())||!TRAIL_CLOCK.initialized()||!SwordFlightTrailRenderer.ready())return;
        postPath=usePostPath()||client.options.getCloudsType()!=net.minecraft.client.CloudStatus.OFF;renderStage="AFTER_PARTICLES";
        activeTrailRenderPath=postPath?"POST_SHADER_TRAIL_RENDER_PATH":"NORMAL_TRAIL_RENDER_PATH";
        activeRenderTarget=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        view=new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix());
        projection=new org.joml.Matrix4f(event.getProjectionMatrix()).mul(view);
        inverseProjection=new org.joml.Matrix4f(event.getProjectionMatrix()).invert();
        Vec3 eye=event.getCamera().getPosition();
        TRAILS.keySet().removeIf(s->s.isRemoved()||s.level()!=client.level||!s.flightRoot()||s.distanceToSqr(eye)>4096);
        cameraPosition=eye;
        var pose=event.getPoseStack();pose.pushPose();
        // Entity interpolation runs between the previous and current tick pose.
        capturedTrailTick=TRAIL_CLOCK.tick();
        double now=capturedTrailTick-1+event.getPartialTick();
        effectTime=SwordTrailPhase.time(capturedTrailTick,event.getPartialTick());
        RenderType type=SwordFlightTrailRenderer.renderType(effectTime);
        VertexConsumer buffer;
        if(postPath){POST_BUFFER.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);buffer=POST_BUFFER;}
        else buffer=BUFFERS.getBuffer(type);
        EMISSION_BUFFER.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer=VertexMultiConsumer.create(buffer,EMISSION_BUFFER);
        try{
            int count=0;
            for(var entry:TRAILS.entrySet()){
                var sword=entry.getKey();
                if(client.options.getCameraType().isFirstPerson()&&sword==client.player.getVehicle())continue;
                if(!sword.flightRoot()||sword.distanceToSqr(eye)>4096)continue;
                if(++count>32)break;
                var f=InterpolatedFlyingSwordTransform.get(sword,event.getPartialTick());
                var history=entry.getValue();
                Vec3 anchor=SwordFlightAttachment.tail(sword,f.position(),f.yaw(),f.pitch(),f.roll());
                var nodes=history.renderNodes(now,point(anchor));if(nodes.size()<2)continue;
                float jinghong=sword.renderJinghong(event.getPartialTick());
                if(sword.huahongStrength()>.5F)continue;
                float strength=sword.advancedPhase().trailIntensity*(1+1.5F*jinghong);
                double length=Math.min(22,(4+5*f.sprintFactor()+7*f.spiralFactor())*strength);
                double width=Math.min(.85,(.36+.18*f.sprintFactor()+.09*f.spiralFactor())*Math.sqrt(strength));
                float alpha=Math.min(.94F,(.72F+.11F*f.sprintFactor()+.10F*f.spiralFactor())*(float)Math.sqrt(strength));
                int start=0;double headArc=nodes.get(nodes.size()-1).arc();
                while(start<nodes.size()-2&&headArc-nodes.get(start).arc()>length)start++;
                double origin=SwordTrailPhase.origin(nodes.get(start).arc(),sword.getUUID().hashCode());
                DIAGNOSTICS.add(new TrailDiagnostics(sword.getId(),nodes.size(),nodes.get(start).arc(),headArc,
                        origin,SwordTrailPhase.coordinate(headArc,origin),effectTime,strength,width,anchor,vec(nodes.get(nodes.size()-1).point())));
                drawHistory(buffer,pose.last(),nodes,start,eye,headArc,length,now,width,alpha,origin);
                sparks(buffer,pose.last(),nodes,start,eye,headArc,length,now,sword.getUUID().hashCode());
                spiralPass=true;
                spiral(buffer,pose.last(),f,anchor,eye,jinghong,SwordTrailPhase.coordinate(headArc,origin));
                spiralPass=false;
            }
            HuahongVisuals.draw(buffer,pose.last(),eye,now,event.getPartialTick());
            normalTrailVertexCount+=HuahongVisuals.vertices;
            if(postPath){
                pending=POST_BUFFER.endOrDiscardIfEmpty();
            }else{
                try(var saved=new MeteorBloomRenderer.State()){BUFFERS.endBatch(type);}
                normalTrailSubmitted=normalTrailVertexCount>0;spiralSubmitted=spiralVertexCount>0;
                if(normalTrailSubmitted||spiralSubmitted)SwordFlightTrailRenderer.normalDraws++;
                SwordFlightTrailRenderer.status="Sword flow / native depth shader";
            }
            pendingEmission=EMISSION_BUFFER.endOrDiscardIfEmpty();
            if((pending!=null||pendingEmission!=null)&&!TrailShaderBridge.captureDepth()){
                if(pending!=null){pending.release();pending=null;}
                if(pendingEmission!=null){pendingEmission.release();pendingEmission=null;}
            }
        }finally{spiralPass=false;pose.popPose();}
    }
    private static double fade(SwordFlightTrailHistory.Node node,double headArc,double length,double now){
        double tail=Mth.clamp(1-(headArc-node.arc())/Math.max(.1,length),0,1);
        double age=Mth.clamp(1-(now-node.tick())/22,0,1);
        return Math.pow(tail,.72)*Math.pow(age,1.15);
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void postShader(net.minecraftforge.client.event.RenderGuiEvent.Pre event){
        if(pending==null&&pendingEmission==null)return;
        event.getGuiGraphics().flush();renderStage="RenderGuiEvent.Pre";submitPending();
    }
    /** Shared cross sections keep strip edges continuous through turns and near-axis views. */
    private static void drawHistory(VertexConsumer buffer,PoseStack.Pose pose,List<SwordFlightTrailHistory.Node> nodes,
            int start,Vec3 eye,double headArc,double length,double now,double width,float alpha,double origin){
        Vec3 previous=null,previousSide=null;float previousU=0,previousA=0;
        for(int i=start;i<nodes.size();i++){
            var node=nodes.get(i);Vec3 center=vec(node.point());
            Vec3 tangent=vec(nodes.get(Math.min(nodes.size()-1,i+1)).point()).subtract(vec(nodes.get(Math.max(start,i-1)).point()));
            if(tangent.lengthSqr()<1e-12)continue;
            tangent=tangent.normalize();Vec3 side=tangent.cross(eye.subtract(center));
            if(side.lengthSqr()<1e-8&&previousSide!=null)side=previousSide.subtract(tangent.scale(previousSide.dot(tangent)));
            if(side.lengthSqr()<1e-8)side=perpendicular(tangent);else side=side.normalize();
            if(previousSide!=null&&side.dot(previousSide)<0)side=side.scale(-1);
            double ageFade=fade(node,headArc,length,now);
            side=side.scale(width*ageFade*swordJoin(headArc-node.arc()));
            float u=SwordTrailPhase.coordinate(node.arc(),origin),a=alpha*(float)ageFade;
            if(previous!=null&&previous.distanceToSqr(center)>1e-12){
                vertex(buffer,pose,previous.add(previousSide),previousU,0,previousA);
                vertex(buffer,pose,previous.subtract(previousSide),previousU,1,previousA);
                vertex(buffer,pose,center.subtract(side),u,1,a);
                vertex(buffer,pose,center.add(side),u,0,a);renderedSegments++;
            }
            previous=center;previousSide=side;previousU=u;previousA=a;
        }
    }
    private static void spiral(VertexConsumer buffer,PoseStack.Pose pose,InterpolatedFlyingSwordTransform f,Vec3 anchor,Vec3 eye,float jinghong,float arc){
        float factor=Math.max(f.spiralFactor(),jinghong);if(factor<.15F||f.speed()<.03)return;
        Vec3 forward=f.forward(),side=perpendicular(forward),up=side.cross(forward).normalize();
        double intensity=Math.max(f.brace(),jinghong)*(.18+.82*factor),length=1.2+6.5*factor,radius=.06+.40*intensity;
        int strands=factor>.52?2:1;
        for(int strand=0;strand<strands;strand++){
            Vec3 previous=null;
            for(int i=0;i<=72;i++){
                double t=i/72.0,phase=Math.toRadians(f.manualRoll())-t*(4.0+factor*7.0)+strand*Math.PI;
                Vec3 p=anchor.subtract(forward.scale(t*length)).add(side.scale(Math.cos(phase)*radius)).add(up.scale(Math.sin(phase)*radius));
                if(previous!=null){
                    double w=(.022+.035*intensity)*(1-t*.7);float alpha=(float)((.42+.35*factor)*intensity*(1-t));
                    ribbon(buffer,pose,previous,p,eye,w,w,alpha,alpha,arc-(float)((i-1)*length/72),arc-(float)(t*length),2);
                }
                previous=p;
            }
        }
        if(factor>.72F)windRing(buffer,pose,f,anchor,eye,(float)intensity,arc);
    }
    private static void windRing(VertexConsumer buffer,PoseStack.Pose pose,InterpolatedFlyingSwordTransform f,Vec3 anchor,Vec3 eye,float intensity,float arc){
        Vec3 forward=f.forward(),side=perpendicular(forward),up=side.cross(forward).normalize();
        Vec3 previous=null;double radius=.12+.52*intensity;
        for(int i=0;i<=36;i++){
            double a=i*Math.PI*2/36;Vec3 p=anchor.add(side.scale(Math.cos(a)*radius)).add(up.scale(Math.sin(a)*radius));
            if(previous!=null)ribbon(buffer,pose,previous,p,eye,.025,.025,.24F*intensity,.24F*intensity,arc+(float)((i-1)*radius*Math.PI*2/36),arc+(float)(i*radius*Math.PI*2/36),2);
            previous=p;
        }
    }
    private static Vec3 perpendicular(Vec3 direction){
        Vec3 axis=Math.abs(direction.y)>.92?new Vec3(1,0,0):new Vec3(0,1,0);
        return direction.cross(axis).normalize();
    }
    /** Narrow at the actual sword tail, smoothly opening over 1.4 blocks. */
    private static double swordJoin(double behind){
        double t=Mth.clamp(behind/1.4,0,1);
        return .10+.90*t*t*(3-2*t);
    }
    private static void ribbon(VertexConsumer buffer,PoseStack.Pose pose,Vec3 a,Vec3 c,Vec3 eye,double aw,double cw,float aa,float ca,float au,float cu,float mode){
        Vec3 tangent=c.subtract(a);double distance=tangent.lengthSqr();
        if(!finite(a)||!finite(c)||distance<1e-10||distance>64||Math.max(aa,ca)<.002)return;
        Vec3 cross=tangent.cross(eye.subtract(a.add(c).scale(.5)));
        // A view along the path retains a finite narrow cross-section, instead
        // of exploding a normalized zero vector or dropping an entire strip.
        Vec3 side=cross.lengthSqr()<1e-10?perpendicular(tangent.normalize()):cross.normalize();
        vertex(buffer,pose,a.add(side.scale(aw)),au,mode,aa);vertex(buffer,pose,a.subtract(side.scale(aw)),au,mode+1,aa);
        vertex(buffer,pose,c.subtract(side.scale(cw)),cu,mode+1,ca);vertex(buffer,pose,c.add(side.scale(cw)),cu,mode,ca);renderedSegments++;
    }
    private static void sparks(VertexConsumer buffer,PoseStack.Pose pose,List<SwordFlightTrailHistory.Node> nodes,int start,Vec3 eye,double headArc,double length,double now,int seed){
        int emitted=0;
        for(int i=start+1;i<nodes.size()-2&&emitted<9;i++){
            var node=nodes.get(i);int cell=(int)Math.floor(node.arc()/.65);
            if(cell==(int)Math.floor(nodes.get(i-1).arc()/.65)||((cell*31+seed)&3)!=0)continue;
            double age=now-node.tick();if(age<2||age>18)continue;
            Vec3 center=vec(node.point()),tangent=vec(nodes.get(i+1).point()).subtract(center);
            if(tangent.lengthSqr()<1e-10)continue;
            Vec3 side=perpendicular(tangent.normalize()),up=side.cross(tangent.normalize());
            double phase=cell*2.399+(seed&63)*.11;
            center=center.add(side.scale(Math.cos(phase)*(.10+age*.007))).add(up.scale(Math.sin(phase)*(.10+age*.006)));
            Vec3 facing=eye.subtract(center);if(facing.lengthSqr()<1e-10)continue;
            Vec3 right=perpendicular(facing.normalize()),vertical=facing.normalize().cross(right);
            double size=.030+.015*(.5+.5*Math.sin(phase));float alpha=(float)(fade(node,headArc,length,now)*.78);
            vertex(buffer,pose,center.subtract(right.scale(size)).subtract(vertical.scale(size)),0,4,alpha);
            vertex(buffer,pose,center.add(right.scale(size)).subtract(vertical.scale(size)),1,4,alpha);
            vertex(buffer,pose,center.add(right.scale(size)).add(vertical.scale(size)),1,5,alpha);
            vertex(buffer,pose,center.subtract(right.scale(size)).add(vertical.scale(size)),0,5,alpha);emitted++;
        }
    }
    private static boolean finite(Vec3 p){return Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z);}
    private static void vertex(VertexConsumer buffer,PoseStack.Pose pose,Vec3 p,float u,float v,float alpha){
        Vec3 relative=p.subtract(cameraPosition);
        buffer.vertex(pose.pose(),(float)relative.x,(float)relative.y,(float)relative.z).uv(u,v).color(.28F,.82F,1F,alpha).endVertex();
        if(spiralPass)spiralVertexCount++;else normalTrailVertexCount++;
    }
    @SubscribeEvent public static void debug(net.minecraftforge.client.event.RenderGuiEvent.Post event){
        var client=Minecraft.getInstance();
        if(client.player==null||!(client.player.getVehicle() instanceof ControlledSwordEntity s)
                ||(!FlyingSwordConfig.DEBUG_OVERLAY.get()&&!s.debugFlight()))return;
        var data=cn.blockforge.generated.generatedmod.GeneratedMod.data(client.player);
        String[] lines={"飞剑状态："+s.flightState()+" / "+net.minecraft.network.chat.Component.translatable(s.advancedPhase().translationKey()).getString()
                        +(s.huahongStage()>0?" / "+net.minecraft.network.chat.Component.translatable("yujian.wuxingxiantu.huahong.stage."+s.huahongStage()).getString():"")
                        +(s.renderJinghong(1)>0?" / "+net.minecraft.network.chat.Component.translatable("yujian.wuxingxiantu.passive.jinghong").getString():""),
                String.format(Locale.ROOT,"速度 %.2f 目标 %.2f 加速 %.2f",s.flightSpeed(),s.targetSpeed(),s.flightAcceleration()),
                "Aura="+(data==null?0:data.wuxingxiantu$getAura())+" SwordSpirit="+cn.blockforge.generated.generatedmod.items.SpiritSwordItem.getSpirit(s.display()),
                "Sword="+net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.display().getItem())+" Loaded="+s.aheadLoaded()+" Collision="+s.colliding(),
                "shaderPackDetected="+shaderPackDetected+"（"+TrailShaderBridge.detection+"）",
                "activeTrailRenderPath="+activeTrailRenderPath,
                "剑气材质："+SwordFlightTrailRenderer.status+" generation="+SwordFlightTrailRenderer.generation,
                "activeRenderTarget="+activeRenderTarget+" 世界源="+TrailShaderBridge.sourceFramebuffer,
                "renderStage="+renderStage,
                "normalTrailVertexCount="+normalTrailVertexCount+" normalTrailSubmitted="+normalTrailSubmitted,
                "spiralVertexCount="+spiralVertexCount+" spiralSubmitted="+spiralSubmitted,
                "深度："+TrailShaderBridge.depthStatus,
                "镜头模式："+FlyingSwordConfig.CAMERA_MODE.get()+" 释放："+FlyingSwordConfig.ROLL_RELEASE_MODE.get(),
                "输入 左="+YujianClientController.rollInputLeft()+" 右="+YujianClientController.rollInputRight()+" 持续刻="+YujianClientController.rollHoldTime(),
                String.format(Locale.ROOT,"manualRollAngle=%.2f velocity=%.2f",s.roll(),s.angularSpeed()),
                String.format(Locale.ROOT,"cameraRollTarget=%.2f applied=%.2f recovery=%s",YujianClientController.cameraTargetRoll(),YujianClientController.cameraRoll(),s.rollRecoveryActive())};
        int top = WuXingClient.visible(client) ? WuXingClient.hudBottom(client) + 6 : 8;
        for(int i=0;i<lines.length;i++)event.getGuiGraphics().drawString(client.font,lines[i],8,top+i*11,0xB6FFFF,true);
    }
}
