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
    private static final SwordTailClipper TAIL_CLIPPER=new SwordTailClipper();
    private static boolean clippingTail;
    private static VertexConsumer clippedOutput;
    private static PoseStack.Pose clippedPose;
    private static final java.util.function.Consumer<SwordTailClipper.Vertex> CLIPPED_VERTEX=p->
            vertex(clippedOutput,clippedPose,new Vec3(p.x(),p.y(),p.z()),p.u(),p.v(),p.alpha());
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
        int count=0;
        for(var entity:client.level.entitiesForRendering()){
            if(!(entity instanceof ControlledSwordEntity sword)||!sword.flightRoot()||sword.distanceToSqr(client.player)>4096)continue;
            if(client.options.getCameraType().isFirstPerson()&&sword==client.player.getVehicle())continue;
            if(++count>32)break;
            // Read the tick pose directly; the shared render-frame transform may
            // still contain an older partial tick and must not seed history.
            Vec3 anchor=morphAnchor(sword,sword.position(),sword.getYRot(),sword.getXRot(),sword.renderRoll(1),InterpolatedFlyingSwordTransform.morphProgress(sword));
            // Synced speed already describes server-validated displacement. Fast Dunxu
            // can exceed eight blocks/tick; that is not a teleport. Anchor rotation and
            // interpolation receive a small bounded allowance without accepting jumps.
            double allowedStep=Math.max(8,Math.max(sword.flightSpeed(),sword.renderSpeed(1))*1.7+3);
            TRAILS.computeIfAbsent(sword,s->new SwordFlightTrailHistory(.25,116,160,720)).sample(point(anchor),TRAIL_CLOCK.tick(),allowedStep);
        }
        HuahongVisuals.sample(TRAIL_CLOCK.tick());
    }
    static SwordFlightTrailHistory sharedHistory(ControlledSwordEntity sword){return TRAILS.get(sword);}
    static float morphWeight(float strength){float t=Mth.clamp(strength,0,1);return t*t*(3-2*t);}
    static Vec3 morphAnchor(ControlledSwordEntity sword,Vec3 position,float yaw,float pitch,float roll,float strength){
        float t=morphWeight(strength);
        return SwordFlightAttachment.tail(sword,position,yaw,pitch,roll).lerp(position.add(0,.85,0),t);
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
            boolean submitted=SwordFlightTrailRenderer.drawPost(mesh,projection,inverseProjection,effectTime);
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
        // Shadow passes must not replace the main-view depth snapshot or queued plume.
        if(TrailShaderBridge.shadowPass())return;
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
        TRAILS.keySet().removeIf(s->s.isRemoved()||s.level()!=client.level||!s.flightRoot());
        cameraPosition=eye;
        var pose=event.getPoseStack();pose.pushPose();
        // Entity interpolation runs between the previous and current tick pose.
        capturedTrailTick=TRAIL_CLOCK.tick();
        float partial=InterpolatedFlyingSwordTransform.partial(event.getPartialTick());
        double now=capturedTrailTick-1+partial;
        effectTime=SwordTrailPhase.time(capturedTrailTick,partial);
        RenderType type=SwordFlightTrailRenderer.renderType(effectTime);
        VertexConsumer buffer;
        if(postPath){POST_BUFFER.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);buffer=POST_BUFFER;}
        else buffer=BUFFERS.getBuffer(type);
        EMISSION_BUFFER.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        // Ordinary plume light stays inside its own rear-facing geometry.
        // Full-screen blur spreads a perfectly measured root over the blade.
        try{
            int count=0;
            for(var entry:TRAILS.entrySet()){
                var sword=entry.getKey();
                if(client.options.getCameraType().isFirstPerson()&&sword==client.player.getVehicle())continue;
                if(!sword.flightRoot())continue;
                if(++count>32)break;
                var f=InterpolatedFlyingSwordTransform.get(sword,event.getPartialTick());
                // Culling the newer tick pose against an interpolated camera
                // hides a local rider for part of every tick at high speed.
                if(f.position().distanceToSqr(eye)>4096)continue;
                var history=entry.getValue();
                Vec3 anchor=morphAnchor(sword,f.position(),f.yaw(),f.pitch(),f.roll(),f.huahong());
                var nodes=history.renderNodes(now,point(anchor));if(nodes.size()<2)continue;
                if(f.huahong()>.001F)nodes=HuahongHeadBridge.presentationNodes(
                        SwordFlightTrailHistory.uniformHead(nodes,.125,HuahongHeadBridge.WORLD_HISTORY_DISTANCE),
                        anchor,f.forward(),HuahongHeadBridge.attachmentWeight(f.huahong()));
                float jinghong=sword.renderJinghong(event.getPartialTick());
                float ordinary=1-morphWeight(f.huahong());
                if(ordinary<=.001F)continue;
                float strength=sword.renderPhase().trailIntensity*(1+1.5F*jinghong);
                double length=Math.min(22,(4+5*f.sprintFactor()+7*f.spiralFactor())*strength);
                double width=Math.min(.85,(.36+.18*f.sprintFactor()+.09*f.spiralFactor())*Math.sqrt(strength));
                float alpha=ordinary*Math.min(.94F,(.90F+.035F*f.sprintFactor()+.025F*f.spiralFactor())*(float)Math.sqrt(strength));
                int start=0;double headArc=nodes.get(nodes.size()-1).arc();
                while(start<nodes.size()-2&&headArc-nodes.get(start).arc()>length)start++;
                double origin=SwordTrailPhase.origin(headArc,sword.getUUID().hashCode());
                DIAGNOSTICS.add(new TrailDiagnostics(sword.getId(),nodes.size(),nodes.get(start).arc(),headArc,
                        origin,SwordTrailPhase.coordinate(headArc,origin),effectTime,strength,width,anchor,vec(nodes.get(nodes.size()-1).point())));
                clippedOutput=buffer;clippedPose=pose.last();clippingTail=true;
                Vec3 forward=f.forward();
                TAIL_CLIPPER.begin(anchor.x,anchor.y,anchor.z,forward.x,forward.y,forward.z,.01,.22);
                drawHistory(buffer,pose.last(),nodes,start,eye,headArc,length,now,width,alpha,origin,forward,f.local(1,0,0).subtract(f.position()));
                spiralPass=true;
                spiral(buffer,pose.last(),f,anchor,eye,jinghong,SwordTrailPhase.coordinate(headArc,origin),ordinary);
                spiralPass=false;
                TAIL_CLIPPER.end();clippingTail=false;
            }
            HuahongVisuals.draw(VertexMultiConsumer.create(buffer,EMISSION_BUFFER),pose.last(),eye,now,partial);
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
        }finally{spiralPass=clippingTail=false;clippedOutput=null;clippedPose=null;pose.popPose();}
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
    /** The local head uses crossed carrier-transported planes. A normalized
     * camera cross at a near-axial view cannot define a continuous width. Fade
     * the three real strips, rather than lerp oriented sides through a sign cut.
     * Beyond the bridge the historical positions and original billboard remain.
     */
    private static void drawHistory(VertexConsumer buffer,PoseStack.Pose pose,List<SwordFlightTrailHistory.Node> nodes,
            int start,Vec3 eye,double headArc,double length,double now,double width,float alpha,double origin,Vec3 forward,Vec3 modelSide){
        Vec3 attachment=vec(nodes.get(nodes.size()-1).point());
        var path=new HuahongFlightPath(nodes,attachment,forward,modelSide);
        double bridge=Math.min(length*.5,Math.max(2.5,Math.min(6,path.headTransitionLength())));
        // A single camera-facing light ribbon has stable projected width;
        // crossed model planes varied their coverage as the sword rolled.
        drawHistoryPlane(buffer,pose,nodes,start,eye,headArc,length,now,width,alpha,origin,path,bridge,0);
    }
    private static void drawHistoryPlane(VertexConsumer buffer,PoseStack.Pose pose,List<SwordFlightTrailHistory.Node> nodes,
            int start,Vec3 eye,double headArc,double length,double now,double width,float alpha,double origin,
            HuahongFlightPath path,double bridge,int plane){
        Vec3 previous=null,previousSide=null;float previousU=0,previousA=0;
        for(int i=start;i<nodes.size();i++){
            var node=nodes.get(i);Vec3 center=vec(node.point());
            Vec3 tangent=vec(nodes.get(Math.min(nodes.size()-1,i+1)).point()).subtract(vec(nodes.get(Math.max(start,i-1)).point()));
            if(tangent.lengthSqr()<1e-12)continue;
            tangent=tangent.normalize();
            double behind=headArc-node.arc(),q=Mth.clamp(behind/Math.max(.01,bridge),0,1),near=1-q*q*(3-2*q);
            Vec3 side;
            if(plane==0){
                side=tangent.cross(eye.subtract(center));
                if(side.lengthSqr()<1e-8&&previousSide!=null)side=previousSide.subtract(tangent.scale(previousSide.dot(tangent)));
                if(side.lengthSqr()<1e-8)side=path.frame(center,tangent).side();else side=side.normalize();
            }else{
                var section=path.frame(center,tangent);side=plane==1||plane==3?section.side():section.up();
            }
            if(previousSide!=null&&side.dot(previousSide)<0)side=side.scale(-1);
            double ageFade=fade(node,headArc,length,now),weight=plane==0?1:plane>=3?.7071067811865476:near*.7071067811865476;
            side=side.scale(width*Math.sqrt(ageFade)*swordJoin(behind));
            float u=SwordTrailPhase.coordinate(node.arc(),origin),a=alpha*(float)(ageFade*weight);
            float mode=plane>=3?36:0;
            if(previous!=null&&previous.distanceToSqr(center)>1e-12&&(a>0||previousA>0)){
                tailVertex(buffer,pose,previous.add(previousSide),previousU,mode,previousA);
                tailVertex(buffer,pose,previous.subtract(previousSide),previousU,mode+1,previousA);
                tailVertex(buffer,pose,center.subtract(side),u,mode+1,a);
                tailVertex(buffer,pose,center.add(side),u,mode,a);renderedSegments++;
            }
            previous=center;previousSide=side;previousU=u;previousA=a;
        }
    }
    private static void spiral(VertexConsumer buffer,PoseStack.Pose pose,InterpolatedFlyingSwordTransform f,Vec3 anchor,Vec3 eye,float jinghong,float arc,float opacity){
        float factor=Math.max(f.spiralFactor(),jinghong);if(factor<.15F||f.speed()<.03)return;
        float birth=Mth.clamp((factor-.15F)/.15F,0,1);birth=birth*birth*(3-2*birth);
        Vec3 forward=f.forward(),side=f.local(1,0,0).subtract(f.position()).normalize(),up=side.cross(forward).normalize();
        double intensity=Math.max(f.brace(),jinghong)*(.18+.82*factor)*opacity,length=1.2+6.5*factor,radius=.06+.40*intensity;
        int strands=factor>.52?2:1;
        for(int strand=0;strand<strands;strand++){
            float strandBirth=Mth.clamp((factor-.52F)/.16F,0,1);
            strandBirth=strand==0?1:strandBirth*strandBirth*(3-2*strandBirth);
            Vec3 previous=null;
            for(int i=0;i<=72;i++){
                double t=i/72.0,phase=-t*(4.0+factor*7.0)+strand*Math.PI;
                double join=t*t*(3-2*t);
                Vec3 p=anchor.subtract(forward.scale(t*length)).add(side.scale(Math.cos(phase)*radius*join)).add(up.scale(Math.sin(phase)*radius*join));
                if(previous!=null){
                    double previousT=(i-1)/72.0,w=.022+.035*intensity;
                    double aw=w*(1-previousT*.7)*rootOpening(previousT*length),cw=w*(1-t*.7)*rootOpening(t*length);
                    float alpha=(float)((.42+.35*factor)*intensity*(1-t))*birth*strandBirth;
                    ribbon(buffer,pose,previous,p,eye,aw,cw,alpha,alpha,arc-(float)((i-1)*length/72),arc-(float)(t*length),2);
                }
                previous=p;
            }
        }
        if(factor>.72F){
            float birthRing=Mth.clamp((factor-.72F)/.18F,0,1);birthRing=birthRing*birthRing*(3-2*birthRing);
            windRing(buffer,pose,f,anchor,eye,(float)intensity,arc,birthRing);
        }
    }
    private static void windRing(VertexConsumer buffer,PoseStack.Pose pose,InterpolatedFlyingSwordTransform f,Vec3 anchor,Vec3 eye,float intensity,float arc,float birth){
        Vec3 forward=f.forward(),side=perpendicular(forward),up=side.cross(forward).normalize();
        Vec3 previous=null;double radius=.12+.52*intensity;
        anchor=anchor.subtract(forward.scale(.35+radius));
        for(int i=0;i<=36;i++){
            double a=i*Math.PI*2/36;Vec3 p=anchor.add(side.scale(Math.cos(a)*radius)).add(up.scale(Math.sin(a)*radius));
            if(previous!=null)ribbon(buffer,pose,previous,p,eye,.025,.025,.24F*intensity*birth,.24F*intensity*birth,arc+(float)((i-1)*radius*Math.PI*2/36),arc+(float)(i*radius*Math.PI*2/36),2);
            previous=p;
        }
    }
    private static Vec3 perpendicular(Vec3 direction){
        Vec3 axis=Math.abs(direction.y)>.92?new Vec3(1,0,0):new Vec3(0,1,0);
        return direction.cross(axis).normalize();
    }
    /** Spread the opening over several blocks so the thin root does not flare abruptly. */
    private static double swordJoin(double behind){
        double t=Mth.clamp(behind/4.5,0,1);
        return rootOpening(behind)*(.08+.92*t*t*(3-2*t));
    }
    private static double rootOpening(double behind){double t=Mth.clamp(behind/.35,0,1);return t*t*(3-2*t);}
    private static void ribbon(VertexConsumer buffer,PoseStack.Pose pose,Vec3 a,Vec3 c,Vec3 eye,double aw,double cw,float aa,float ca,float au,float cu,float mode){
        Vec3 tangent=c.subtract(a);double distance=tangent.lengthSqr();
        if(!finite(a)||!finite(c)||distance<1e-10||distance>64||Math.max(aa,ca)<.002)return;
        Vec3 cross=tangent.cross(eye.subtract(a.add(c).scale(.5)));
        // A view along the path retains a finite narrow cross-section, instead
        // of exploding a normalized zero vector or dropping an entire strip.
        Vec3 side=cross.lengthSqr()<1e-10?perpendicular(tangent.normalize()):cross.normalize();
        tailVertex(buffer,pose,a.add(side.scale(aw)),au,mode,aa);tailVertex(buffer,pose,a.subtract(side.scale(aw)),au,mode+1,aa);
        tailVertex(buffer,pose,c.subtract(side.scale(cw)),cu,mode+1,ca);tailVertex(buffer,pose,c.add(side.scale(cw)),cu,mode,ca);renderedSegments++;
    }
    private static boolean finite(Vec3 p){return Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z);}
    private static void tailVertex(VertexConsumer buffer,PoseStack.Pose pose,Vec3 p,float u,float v,float alpha){
        if(clippingTail)TAIL_CLIPPER.vertex(p.x,p.y,p.z,u,v,alpha,CLIPPED_VERTEX);
        else vertex(buffer,pose,p,u,v,alpha);
    }
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
