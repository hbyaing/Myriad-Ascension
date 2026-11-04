package cn.blockforge.generated.generatedmod.client.realmflight;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.HudConfig;
import cn.blockforge.generated.generatedmod.client.TrailShaderBridge;
import cn.blockforge.generated.generatedmod.realmflight.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import java.util.*;

/** Open moving body radiance and one continuous world-space meteor plume. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class RealmFlightEffects {
    private static final int MAX_FLIGHTS=32;
    public static float firstPersonViewWeight;
    public static int firstPersonHistoryFrames;
    private static boolean drawingFirstPerson;
    private static Vec3 firstPersonSight=Vec3.ZERO;
    private static final Map<UUID,Flight> FLIGHTS=new LinkedHashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel world;
    private static long clock;
    private static final class Flight {
        RealmFlightVisualPacket state;
        final RealmMeteorPath path=new RealmMeteorPath();
        final RealmFlightAppearance.Orientation orientation=new RealmFlightAppearance.Orientation();
        int absentTicks;
        double glow,previousGlow,trailLength=2,previousTrailLength=2;
        double speedWeight,previousSpeedWeight,turnWeight,previousTurnWeight,flowPhase,previousFlowPhase,lastHeading;
        boolean headingSeen;
        Flight(RealmFlightVisualPacket state){this.state=state;}
    }
    private static final MultiBufferSource.BufferSource BUFFERS=MultiBufferSource.immediate(new BufferBuilder(524288));
    private static final BufferBuilder POST=new BufferBuilder(524288),EMISSION=new BufferBuilder(524288);
    // One render thread processes actors sequentially. Each flow layer reuses
    // these bounded samples of the same transported world-space history.
    private static final class TubeFrame {
        RealmMeteorPath.Frame frame;
        double width,behind,age,bendSide,bendUp,headNose,headRadius;
        double hot,transition,drawWidth;
        float opacity;
        float drawAlpha,drawU;
        int drawColor;
        final RealmMeteorVertexRing ring=new RealmMeteorVertexRing(Math.max(6,RealmMeteorTuning.NEAR_RADIAL_SEGMENTS));
        int packedBehind,packedAge,packedSide,packedUp;
    }
    private static final TubeFrame[] TUBE_FRAMES=new TubeFrame[RealmMeteorPath.MAX_SAMPLES*6+20];
    static { for(int i=0;i<TUBE_FRAMES.length;i++)TUBE_FRAMES[i]=new TubeFrame(); }
    private static BufferBuilder.RenderedBuffer pending,pendingEmission;
    private static List<MeteorHeadRenderer.Head> pendingHeads=List.of();
    private static Matrix4f projection,view,inverseProjection;
    private static Vec3 eye;
    public static int renderedVertices,renderedShells,renderedTrailSegments;
    public static int renderedSectionEvaluations,uncachedSectionEvaluations;
    public static final int[] renderedTierHeads=new int[6];
    public static boolean lastPostDrawSucceeded;
    private static abstract class Types extends RenderType {
        private Types(){super("unused",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
        static final RenderType ENERGY=create("wuxing_realm_meteor",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,524288,false,true,
                CompositeState.builder().setShaderState(new ShaderStateShard(TrailShaderBridge::meteorShader))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    }
    private static void checkWorld(){
        var current=Minecraft.getInstance().level;
        if(world!=current){world=current;FLIGHTS.clear();clock=0;discardPending();}
    }
    private static void discardPending(){
        if(pending!=null){pending.release();pending=null;}
        if(pendingEmission!=null){pendingEmission.release();pendingEmission=null;}
        pendingHeads=List.of();
    }
    public static void accept(RealmFlightVisualPacket packet){
        checkWorld();if(world==null)return;var existing=FLIGHTS.get(packet.player());
        if(!packet.active()||packet.realm()<3||packet.realm()>8){
            if(existing!=null)existing.state=new RealmFlightVisualPacket(packet.player(),false,packet.realm(),packet.stage(),packet.ghost(),packet.boost(),packet.visualRealm());return;
        }
        if(existing==null&&FLIGHTS.size()>=MAX_FLIGHTS){
            var mc=Minecraft.getInstance();var newcomer=world.getPlayerByUUID(packet.player());
            Vec3 camera=mc.gameRenderer.getMainCamera().getPosition();UUID victim=null;double score=-1;
            for(var entry:FLIGHTS.entrySet()){
                if(mc.player!=null&&entry.getKey().equals(mc.player.getUUID()))continue;
                var player=world.getPlayerByUUID(entry.getKey());
                double candidate=!entry.getValue().state.active()?Double.MAX_VALUE:player==null?1e12:player.position().distanceToSqr(camera);
                if(candidate>score){victim=entry.getKey();score=candidate;}
            }
            boolean local=mc.player!=null&&packet.player().equals(mc.player.getUUID());
            if(victim==null||!local&&newcomer!=null&&newcomer.position().distanceToSqr(camera)>=score)return;
            FLIGHTS.remove(victim);
        }
        FLIGHTS.computeIfAbsent(packet.player(),id->new Flight(packet)).state=packet;
    }
    @SubscribeEvent public static void leave(net.minecraftforge.event.entity.EntityLeaveLevelEvent event){
        if(event.getLevel().isClientSide()&&event.getEntity() instanceof Player player)FLIGHTS.remove(player.getUUID());
    }
    public static boolean transformed(Player player){
        var mc=Minecraft.getInstance();
        if(!player.isAlive()||player.isPassenger()||player.isSpectator()||player.isCreative()||player.isInWater()||player.isFallFlying())return false;
        if(player==mc.player)return HudConfig.REALM_FLIGHT_ANIMATION.get()&&RealmFlightControl.eligible(player)&&player.getAbilities().flying;
        var flight=FLIGHTS.get(player.getUUID());return flight!=null&&flight.state.active();
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        checkWorld();var mc=Minecraft.getInstance();if(!HudConfig.REALM_FLIGHT_EFFECTS.get()){FLIGHTS.clear();discardPending();return;}if(world==null||mc.isPaused())return;clock++;
        if(mc.player!=null&&transformed(mc.player)&&!FLIGHTS.containsKey(mc.player.getUUID())){
            var data=GeneratedMod.data(mc.player);
            accept(new RealmFlightVisualPacket(mc.player.getUUID(),true,data.wuxingxiantu$getRealm(),data.wuxingxiantu$getCultivationStage(),data.wuxingxiantu$isGhostImmortal(),false));
        }
        for(var it=FLIGHTS.entrySet().iterator();it.hasNext();){
            var entry=it.next();var flight=entry.getValue();var player=world.getPlayerByUUID(entry.getKey());
            if(player==null){if(++flight.absentTicks>20)it.remove();continue;}flight.absentTicks=0;
            boolean active=transformed(player);flight.path.sample(vec(player.position().add(0,player.getBbHeight()*.5,0)),clock,active);
            flight.previousGlow=flight.glow;flight.previousTrailLength=flight.trailLength;
            flight.previousSpeedWeight=flight.speedWeight;flight.previousTurnWeight=flight.turnWeight;flight.previousFlowPhase=flight.flowPhase;
            flight.glow=RealmMeteorTuning.filter(flight.glow,active?1:0,active?RealmMeteorTuning.TAKEOFF_SECONDS:RealmMeteorTuning.LANDING_SECONDS);
            flight.speedWeight=RealmMeteorTuning.filter(flight.speedWeight,active?RealmMeteorTuning.speedWeight(flight.path.speed(),flight.state.realm(),flight.state.stage()):0,.15);
            var heading=flight.path.direction();double yaw=Math.toDegrees(Math.atan2(-heading.x(),heading.z())),turn=0;
            if(active&&heading.x()*heading.x()+heading.z()*heading.z()>.0001&&flight.path.speed()>.06){
                if(flight.headingSeen)turn=Mth.clamp(FlightVisualDynamics.wrapDegrees((float)(yaw-flight.lastHeading))/9,-1,1);
                flight.lastHeading=yaw;flight.headingSeen=true;
            }else flight.headingSeen=false;
            flight.turnWeight=RealmMeteorTuning.filter(flight.turnWeight,turn,.15);
            // Integrate flow once per tick. Speed changes cannot teleport the
            // texture phase as multiplying elapsed world time by speed would.
            flight.flowPhase+=flight.speedWeight*.125;
            double target=RealmMeteorTuning.targetLength(flight.path.speed(),flight.state.realm(),flight.state.stage(),visualRealm(player,flight.state));
            flight.trailLength=RealmMeteorTuning.filter(flight.trailLength,target,RealmMeteorTuning.LENGTH_SECONDS);
            flight.orientation.tick(flight.path.direction(),active&&flight.path.speed()>.015);
            if(!active&&flight.path.sampleCount()==0&&flight.glow<.01)it.remove();
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void frame(TickEvent.RenderTickEvent event){
        if(event.phase==TickEvent.Phase.START)discardPending();else if(Minecraft.getInstance().options.hideGui)submit();
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void hud(RenderGuiEvent.Pre event){
        if(pending!=null||pendingEmission!=null||!pendingHeads.isEmpty()){event.getGuiGraphics().flush();submit();}
    }
    private static void submit(){
        if(Minecraft.getInstance().level!=world){discardPending();return;}
        cn.blockforge.generated.generatedmod.client.tribulation.LightningShaderBridge.flushBeforeForeground();
        if(pending!=null){var mesh=pending;pending=null;lastPostDrawSucceeded=TrailShaderBridge.drawMeteor(mesh,projection,view);}
        var heads=pendingHeads;pendingHeads=List.of();MeteorHeadRenderer.drawBase(heads,inverseProjection);
        if(pendingEmission!=null||!heads.isEmpty()){
            var mesh=pendingEmission;pendingEmission=null;
            if(Boolean.getBoolean("wuxingxiantu.meteorBloomDisabled")){if(mesh!=null)mesh.release();}
            else MeteorBloomRenderer.draw(mesh,projection,view,inverseProjection,heads);
        }
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        renderedVertices=renderedShells=renderedTrailSegments=0;
        firstPersonViewWeight=0;firstPersonHistoryFrames=0;
        renderedSectionEvaluations=uncachedSectionEvaluations=0;
        Arrays.fill(renderedTierHeads,0);
        checkWorld();if(!HudConfig.REALM_FLIGHT_EFFECTS.get()){discardPending();return;}if(world==null||FLIGHTS.isEmpty()||!TrailShaderBridge.meteorReady()||!MeteorHeadRenderer.ready())return;
        eye=event.getCamera().getPosition();view=new Matrix4f(RenderSystem.getModelViewMatrix());
        projection=new Matrix4f(event.getProjectionMatrix()).mul(view);inverseProjection=new Matrix4f(event.getProjectionMatrix()).invert();
        boolean post=TrailShaderBridge.shaderPackDetected()||Boolean.getBoolean("wuxingxiantu.meteorForcePost")
                ||cn.blockforge.generated.generatedmod.client.tribulation.LightningShaderBridge.hasPendingWorldEffects();
        TrailShaderBridge.meteorShader().safeGetUniform("EffectTime").set((world.getGameTime()+event.getPartialTick())/20F);
        for(var shader:new net.minecraft.client.renderer.ShaderInstance[]{TrailShaderBridge.meteorShader(),TrailShaderBridge.meteorPostShader()})
            shader.safeGetUniform("VisualTuning").set((float)RealmMeteorTuning.ENERGY_PULSE_STRENGTH,(float)RealmMeteorTuning.TRAIL_TURBULENCE_STRENGTH,(float)RealmMeteorTuning.TRAIL_FLOW_SPEED);
        VertexConsumer buffer;
        if(post){POST.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.NEW_ENTITY);buffer=POST;}
        else buffer=BUFFERS.getBuffer(Types.ENERGY);
        EMISSION.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.NEW_ENTITY);buffer=VertexMultiConsumer.create(buffer,EMISSION);
        var pose=event.getPoseStack().last();double now=cn.blockforge.generated.generatedmod.client.SwordTrailPhase.renderTick(clock,event.getPartialTick());
        var flights=new ArrayList<>(FLIGHTS.entrySet());
        flights.sort(Comparator.comparingDouble((Map.Entry<UUID,Flight> entry)->{
            var p=world.getPlayerByUUID(entry.getKey());return p==null?Double.MAX_VALUE:p.position().distanceToSqr(eye);
        }).reversed());
        var near=new HashSet<UUID>();
        for(int i=flights.size()-1;i>=0&&near.size()<4;i--){var p=world.getPlayerByUUID(flights.get(i).getKey());if(p!=null&&p.position().distanceToSqr(eye)<1024)near.add(p.getUUID());}
        var heads=new ArrayList<MeteorHeadRenderer.Head>();
        for(var entry:flights){
            var flight=entry.getValue();var player=world.getPlayerByUUID(entry.getKey());if(player==null)continue;
            double distance=player.position().distanceTo(eye);
            boolean localPlayer=player==Minecraft.getInstance().player;
            if(distance>RealmMeteorTuning.FAR_DETAIL_DISTANCE)continue;
            boolean firstPerson=localPlayer&&Minecraft.getInstance().options.getCameraType().isFirstPerson();
            if(firstPerson&&!HudConfig.REALM_FLIGHT_FIRST_PERSON_EFFECTS.get())continue;
            drawingFirstPerson=firstPerson;
            float firstPersonWeight=1;
            if(firstPerson){
                // Use the view actually being rendered, including free-look camera events.
                var look=new Matrix4f(view).mul(pose.pose()).invert().transformDirection(new org.joml.Vector3f(0,0,-1));
                firstPersonWeight=FirstPersonRealmTrail.viewWeight(new RealmMeteorPath.Vec(look.x(),look.y(),look.z()),flight.path.direction());
                firstPersonViewWeight=firstPersonWeight;
                if(firstPersonWeight<=.001)continue;
            }
            float distanceOpacity=(float)(1-RealmMeteorTuning.smooth((distance-112)/16));
            float viewOpacity=(localPlayer?.78F:1F)*distanceOpacity*firstPersonWeight;
            int visualRealm=visualRealm(player,flight.state);
            var appearance=RealmFlightAppearance.profile(visualRealm);
            boolean selectedAppearance=RealmFlightSelection.sanitizePreference(flight.state.realm(),
                    localPlayer?HudConfig.REALM_FLIGHT_VISUAL_REALM.get():flight.state.visualRealm())!=0;
            int lod=distance>RealmMeteorTuning.MEDIUM_DETAIL_DISTANCE?2:distance>RealmMeteorTuning.NEAR_DETAIL_DISTANCE?1:0,color=flight.state.ghost()&&!selectedAppearance?0xBC9EFF:appearance.color();
            Vec3 center=player.getPosition(event.getPartialTick()).add(0,player.getBbHeight()*.5,0);
            double bodyScale=Math.max(1,player.getBbHeight()/1.8);
            boolean active=transformed(player),boost=flight.state.boost(),advanced=visualRealm>=4;
            double trailLength=advanced?Mth.lerp(event.getPartialTick(),flight.previousTrailLength,flight.trailLength):boost?appearance.boostLength():appearance.length();
            var path=advanced?flight.path.renderAdaptive(now,vec(center),Math.max(.01,trailLength),RealmMeteorTuning.TRAIL_LIFETIME_TICKS,lod)
                    :flight.path.render(now,vec(center),lod==0?2:1,Math.max(.01,trailLength));
            double flowPhase=Mth.lerp(event.getPartialTick(),flight.previousFlowPhase,flight.flowPhase);
            float shellWeight=active&&!firstPerson?RealmFlightVisuals.forwardShellWeight(player,event.getPartialTick()):0;
            if(firstPerson&&!path.isEmpty()){
                double headArc=path.get(path.size()-1).arc();
                var firstPersonLook=new Matrix4f(view).mul(pose.pose()).invert().transformDirection(new org.joml.Vector3f(0,0,-1));
                Vec3 clearLook=new Vec3(firstPersonLook.x,firstPersonLook.y,firstPersonLook.z).normalize();
                firstPersonSight=clearLook;
                var filtered=new ArrayList<RealmMeteorPath.Frame>(path.size());
                for(var f:path){double distanceToEye=vec(f.position()).distanceTo(eye);
                    if(f.opacity()*FirstPersonRealmTrail.historyWeight(headArc-f.arc(),distanceToEye,bodyScale)>.001)firstPersonHistoryFrames++;
                    filtered.add(new RealmMeteorPath.Frame(f.position(),f.tangent(),f.side(),f.up(),f.tick(),f.arc(),
                            f.opacity()*FirstPersonRealmTrail.historyWeight(headArc-f.arc(),distanceToEye,bodyScale)));}
                path=filtered;
            }
            double motion=Mth.lerp(event.getPartialTick(),flight.previousSpeedWeight,flight.speedWeight);
            var orientation=flight.orientation;
            Vec3 forward=unit(vec(orientation.previousHeading).lerp(vec(orientation.heading),event.getPartialTick()),vec(orientation.heading));
            Vec3 side=vec(orientation.previousSide).lerp(vec(orientation.side),event.getPartialTick());
            side=unit(side.subtract(forward.scale(side.dot(forward))),perpendicular(forward));
            Vec3 up=forward.cross(side).normalize();
            var scaledPose=new PoseStack();scaledPose.last().pose().set(pose.pose());scaledPose.last().normal().set(pose.normal());
            Vec3 pivot=center.subtract(eye);
            scaledPose.translate(pivot.x,pivot.y,pivot.z);scaledPose.scale((float)bodyScale,(float)bodyScale,(float)bodyScale);scaledPose.translate(-pivot.x,-pivot.y,-pivot.z);
            drawTrail(buffer,scaledPose.last(),path,color,boost,lod,(float)((world.getGameTime()+event.getPartialTick())/20),entry.getKey().hashCode(),appearance,viewOpacity,trailLength,flowPhase,shellWeight,now,motion,vec(forward),vec(side),vec(up));
            double glow=Mth.lerp(event.getPartialTick(),flight.previousGlow,flight.glow);
            // Advanced tiers are wrapped by the flowing trail itself, not a separate front volume.
            if(active&&!advanced&&!firstPerson){
                double moving=Mth.clamp(flight.path.speed()/.18,0,1);
                double turn=Mth.lerp(event.getPartialTick(),flight.previousTurnWeight,flight.turnWeight);
                double radius=advanced?RealmMeteorTuning.headRadius(motion):boost?.90:.85,length=advanced?RealmMeteorTuning.headLength(motion):.78+moving*.16;
                radius*=bodyScale;length*=bodyScale;
                var local=new Matrix4f().m00((float)(side.x*radius)).m01((float)(side.y*radius)).m02((float)(side.z*radius))
                        .m10((float)(up.x*radius)).m11((float)(up.y*radius)).m12((float)(up.z*radius))
                        .m20((float)(forward.x*length)).m21((float)(forward.y*length)).m22((float)(forward.z*length))
                        .m30((float)(center.x-eye.x)).m31((float)(center.y-eye.y)).m32((float)(center.z-eye.z));
                var head=MeteorHeadRenderer.create(new Matrix4f(view).mul(pose.pose()).mul(local),event.getProjectionMatrix(),color,RealmMeteorTuning.ORB_ALPHA*(float)(advanced?glow*shellWeight:1)*(localPlayer?.50F:1F)*distanceOpacity,near.contains(entry.getKey())?24:lod==2?8:12,(entry.getKey().hashCode()&255)*.037F+(float)(advanced?flowPhase*.30:0),appearance.style(),(float)motion,(float)turn);
                if(head!=null){heads.add(head);renderedShells++;renderedTierHeads[appearance.style()]++;renderedVertices+=4;}
            }
        }
        if(post){POST.setQuadSorting(RenderSystem.getVertexSorting());pending=POST.endOrDiscardIfEmpty();}
        else try(var saved=new MeteorBloomRenderer.State()){BUFFERS.endBatch(Types.ENERGY);}
        pendingEmission=EMISSION.endOrDiscardIfEmpty();pendingHeads=heads;
        if((pending!=null||pendingEmission!=null||!heads.isEmpty())&&!TrailShaderBridge.captureDepth())discardPending();
    }
    /** Appearance is independent of the server-authored movement realm. */
    private static int visualRealm(Player player,RealmFlightVisualPacket state){
        int selected=player==Minecraft.getInstance().player
                ?RealmFlightSelection.resolve(state.realm(),HudConfig.REALM_FLIGHT_VISUAL_REALM.get())
                :RealmFlightSelection.resolve(state.realm(),state.visualRealm());
        return selected;
    }
    private static RealmMeteorPath.Vec vec(Vec3 p){return new RealmMeteorPath.Vec(p.x,p.y,p.z);}
    private static Vec3 vec(RealmMeteorPath.Vec p){return new Vec3(p.x(),p.y(),p.z());}
    private static Vec3 unit(Vec3 value,Vec3 fallback){return value.lengthSqr()<1e-10?fallback:value.normalize();}
    private static Vec3 perpendicular(Vec3 forward){return unit(forward.cross(Math.abs(forward.y)>.92?new Vec3(0,0,1):new Vec3(0,1,0)),new Vec3(1,0,0));}
    /** Fade nearby world-space wake centres before they obscure the third-person avatar. */
    private static float cameraFade(Vec3 position){
        if(drawingFirstPerson){double fade=Mth.clamp((position.distanceTo(eye)-.15)/.65,0,1);return (float)(fade*fade*(3-2*fade));}
        double fade=Mth.clamp((position.distanceTo(eye)-.6)/2.4,0,1);
        return (float)(fade*fade*(3-2*fade));
    }
    private record Plume(Vec3 position,Vec3 side,double width,float opacity,float u) {}
    private static Plume plume(RealmMeteorPath.Frame frame,double headArc,double maxLength,int strand,int count,boolean boost,int seed,float time,int layer,double origin){
        double behind=headArc-frame.arc(),fraction=Mth.clamp(behind/maxLength,0,1);
        double taper=Math.pow(1-fraction,.6)*Math.pow(Math.max(0,frame.opacity()),.30);
        double phase=strand*Math.PI*2/count+(seed&63)*.019;
        Vec3 axis=vec(frame.side()),up=vec(frame.up()),center=vec(frame.position());
        // All material layers share one centre. Fine flowing veins live inside
        // the shader; orbiting independent ribbons produced separate tails.
        double wave=Math.sin(frame.arc()*.48+phase)*.10;
        Vec3 offset=axis.scale(wave*taper).add(up.scale(Math.sin(frame.arc()*.31+phase)*.07*taper));
        Vec3 tangent=vec(frame.tangent()),face=unit(tangent.cross(eye.subtract(center)),axis);
        Vec3 cross=axis.scale(Math.cos(phase+.4)).add(up.scale(Math.sin(phase+.4)));
        if(cross.dot(face)<0)cross=cross.scale(-1);
        Vec3 side=unit(face.scale(.72).add(cross.scale(.28)),face);
        double flare=.78+.22*Math.min(1,behind/2.5);
        double width=(layer==2?1.38:layer==1?.98:.88)*(boost?1.18:1)*taper*flare*(1+.08*Math.sin(frame.arc()*.44+phase));
        float opacity=(float)(frame.opacity()*(layer==2?.27:layer==1?.87:.38));
        // Let the body field cover the join: a nonzero, full-width end at the
        // player exposed the ribbon's straight terminal edge when turning.
        double start=Mth.clamp(behind/1.8,0,1);opacity*=(float)(start*start*(3-2*start));
        return new Plume(center.add(offset),side,width,opacity,(float)(frame.arc()-origin));
    }
    private static void drawTrail(VertexConsumer buffer,PoseStack.Pose pose,List<RealmMeteorPath.Frame> path,int color,boolean boost,int lod,float time,int seed,RealmFlightAppearance.Profile appearance,float viewOpacity,double length,double flowPhase,float shellWeight,double renderTick,double motion,RealmMeteorPath.Vec headForward,RealmMeteorPath.Vec headSide,RealmMeteorPath.Vec headUp){
        if(appearance.style()>0){drawAdvancedTrail(buffer,pose,path,color,boost,lod,seed,appearance,viewOpacity,length,flowPhase,shellWeight,renderTick,motion,headForward,headSide,headUp);return;}
        if(path.size()<2)return;int count=1;
        double headArc=path.get(path.size()-1).arc(),origin=Math.floor(path.get(0).arc()/4096)*4096;
        // Omit the narrow central lance; retain the surrounding energy wake.
        for(int layer=2;layer>=1;layer--)for(int strand=0;strand<count;strand++)for(int i=0;i<path.size()-1;i++){
            var a=plume(path.get(i),headArc,length,strand,count,boost,seed,time,layer,origin);
            var b=plume(path.get(i+1),headArc,length,strand,count,boost,seed,time,layer,origin);
            if(a.opacity()<.001&&b.opacity()<.001)continue;
            if(lod>0){
                float visibility=lod==2?1.35F:1.15F;
                a=new Plume(a.position(),a.side(),a.width()*1.12,Math.min(.94F,a.opacity()*visibility),a.u());
                b=new Plume(b.position(),b.side(),b.width()*1.12,Math.min(.94F,b.opacity()*visibility),b.u());
            }
            float alphaA=a.opacity()*viewOpacity*cameraFade(a.position()),alphaB=b.opacity()*viewOpacity*cameraFade(b.position());
            if(alphaA<.001&&alphaB<.001)continue;
            int tint=layer==2?(color==0xBC9EFF?0x9269DB:0x3287ED):layer==0?0xE4FCFF:color;
            Vec3 normal=unit(b.position().subtract(a.position()).cross(a.side()),new Vec3(0,1,0));float v=layer*2+strand*8;
            vertex(buffer,pose,a.position().subtract(a.side().scale(a.width())),normal,a.u(),v,tint,alphaA);
            vertex(buffer,pose,a.position().add(a.side().scale(a.width())),normal,a.u(),v+1,tint,alphaA);
            vertex(buffer,pose,b.position().add(b.side().scale(b.width())),normal,b.u(),v+1,tint,alphaB);
            vertex(buffer,pose,b.position().subtract(b.side().scale(b.width())),normal,b.u(),v,tint,alphaB);
            if(layer==1)renderedTrailSegments++;
        }
        if(lod==0)for(int i=1;i<path.size()-1;i+=7){
            var frame=path.get(i);double age=path.get(path.size()-1).tick()-frame.tick();
            if(age<3||age>17)continue;
            Vec3 side=vec(frame.side()),up=vec(frame.up()),center=vec(frame.position());
            double phase=(seed&127)*.023+i*2.39;
            center=center.add(side.scale(Math.cos(phase)*(.9+age*.04))).add(up.scale(Math.sin(phase)*(.8+age*.03)));
            Vec3 normal=unit(eye.subtract(center),new Vec3(0,0,1)),right=perpendicular(normal),vertical=normal.cross(right).normalize();
            double size=.028+.012*Math.sin(phase);float alpha=(float)(frame.opacity()*(1-age/18)*.68*viewOpacity*cameraFade(center));
            vertex(buffer,pose,center.subtract(right.scale(size)).subtract(vertical.scale(size)),normal,0,6,color,alpha);
            vertex(buffer,pose,center.add(right.scale(size)).subtract(vertical.scale(size)),normal,1,6,color,alpha);
            vertex(buffer,pose,center.add(right.scale(size)).add(vertical.scale(size)),normal,1,7,color,alpha);
            vertex(buffer,pose,center.subtract(right.scale(size)).add(vertical.scale(size)),normal,0,7,color,alpha);
        }
    }
    /** All flow sections and brief bend haze share one camera-independent historical path. */
    private static void drawAdvancedTrail(VertexConsumer buffer,PoseStack.Pose pose,List<RealmMeteorPath.Frame> path,int color,boolean boost,int lod,int seed,RealmFlightAppearance.Profile appearance,float viewOpacity,double length,double flowPhase,float shellWeight,double renderTick,double motion,RealmMeteorPath.Vec headForward,RealmMeteorPath.Vec headSide,RealmMeteorPath.Vec headUp){
        if(path.size()<2)return;
        double headArc=path.get(path.size()-1).arc(),origin=Math.floor(path.get(0).arc()/4096)*4096;
        double nose=RealmMeteorTuning.headLength(motion),headRadius=RealmMeteorTuning.headRadius(motion);
        // Rotate the final history frames gradually into the filtered heading,
        // carrying their own transported radial phase. Never replace only the
        // body ring with an unrelated orientation at the history/skin seam.
        if(shellWeight>.001)path=RealmMeteorJoin.attachHead(path,nose,headForward);
        var radiusSupport=new RealmMeteorJoin.RadiusSupport(path);
        int count=path.size(),radial=lod==0?RealmMeteorTuning.NEAR_RADIAL_SEGMENTS:lod==1?RealmMeteorTuning.MEDIUM_RADIAL_SEGMENTS:RealmMeteorTuning.FAR_RADIAL_SEGMENTS;
        if(count>TUBE_FRAMES.length)return;
        double baseRadius=RealmFlightAppearance.profile(4).radius();
        double whiteFadeLength=Math.max(.01,RealmFlightAppearance.profile(4).length()*.95);
        for(int index=0;index<count;index++){
            var frame=path.get(index);var tube=TUBE_FRAMES[index];tube.frame=frame;
            double behind=headArc-frame.arc(),remaining=RealmMeteorFlow.remaining(behind,length);
            double life=Mth.clamp(frame.opacity(),0,1);
            double envelope=RealmMeteorTuning.headWrapWidth(behind,nose,headRadius);
            double wake=RealmMeteorFlow.width(Math.max(0,behind),length,baseRadius,appearance.radius());
            tube.width=(behind<0?envelope:envelope+(wake-envelope)*RealmMeteorTuning.smooth(behind/4))*Math.pow(life,.10);
            double bound=RealmMeteorFlow.sectionBound(appearance.style(),behind)*RealmMeteorTuning.LAYER_WIDTH[0];
            double limitedWidth=radiusSupport.limit(index,tube.width*bound)/bound;
            // Apply the same stable curvature limit on both sides of the body
            // join; gradually restore the front cap away from the equator.
            tube.width=behind>=0?limitedWidth:Mth.lerp(RealmMeteorTuning.smooth(-behind/nose),limitedWidth,tube.width);
            tube.opacity=(float)(Math.pow(life,.25)*Math.pow(remaining,.25)*(behind<0?RealmMeteorTuning.smooth((behind+nose)/(nose*.12)):1)*(behind<=0?shellWeight:1-(1-shellWeight)*(1-RealmMeteorTuning.smooth(behind/baseRadius)))*viewOpacity*cameraFade(vec(frame.position())));
            var bend=RealmMeteorFlow.bend(path,index);
            tube.headNose=nose;tube.headRadius=headRadius;
            tube.behind=behind;tube.age=Math.max(0,renderTick-frame.tick());tube.bendSide=bend.side();tube.bendUp=bend.up();
            tube.hot=1-RealmMeteorTuning.smooth(Math.max(0,behind)/whiteFadeLength);
            tube.transition=RealmMeteorTuning.bodyRadiance(behind);
            tube.drawU=(float)(frame.arc()-origin+flowPhase);
            tube.packedBehind=RealmMeteorFlow.packBehind(behind);tube.packedAge=RealmMeteorFlow.packAge(tube.age);
            tube.packedSide=RealmMeteorFlow.packBend(bend.side());tube.packedUp=RealmMeteorFlow.packBend(bend.up());
        }
        for(int layer=1;layer<3;layer++){
            float v=64+(appearance.style()-1)*6+layer*2;
            double layerWidth=RealmMeteorTuning.LAYER_WIDTH[layer];
            float layerAlpha=RealmMeteorTuning.LAYER_ALPHA[layer];
            for(int i=0;i<count;i++){
                var tube=TUBE_FRAMES[i];
                tube.drawWidth=tube.width*(layer==2?layerWidth+.48*tube.hot:layerWidth);
                tube.drawColor=layer==2?hotWakeColor(color,tube.hot):color;
                tube.drawAlpha=Math.min(.95F,tube.opacity*layerAlpha*(float)(layer==2?(1+1.4*tube.hot)*tube.transition:1));
                if(ringNeeded(i,count,false))prepareTubeRing(tube,appearance.style(),layer,flowPhase,radial,false);
            }
            for(int i=1;i<count;i++){
                var a=TUBE_FRAMES[i-1];var b=TUBE_FRAMES[i];
                if(a.opacity<=.001&&b.opacity<=.001)continue;
                for(int segment=0;segment<radial;segment++){
                    tubeVertex(buffer,pose,a,segment,v+(float)segment/radial);
                    tubeVertex(buffer,pose,b,segment,v+(float)segment/radial);
                    tubeVertex(buffer,pose,b,segment+1,v+(float)(segment+1)/radial);
                    tubeVertex(buffer,pose,a,segment+1,v+(float)(segment+1)/radial);
                }
                if(layer==1)renderedTrailSegments++;
            }
        }
        // Only the already-curved, 3..20-tick-old wake gets an external residual.
        // It is one thin section on that history, never an auxiliary long rail.
        if(appearance.style()>=4&&lod<2){
            int hazeRadial=lod==0?6:4;float hazeV=128+(appearance.style()-1)*2;
            for(int i=0;i<count;i++){
                var tube=TUBE_FRAMES[i];tube.drawWidth=tube.width;tube.drawColor=color;
                tube.drawAlpha=tube.opacity*(float)RealmMeteorFlow.residualWeight(appearance.style(),tube.behind,tube.age,tube.bendSide,tube.bendUp);
            }
            for(int i=0;i<count;i++)if(ringNeeded(i,count,true))prepareTubeRing(TUBE_FRAMES[i],appearance.style(),0,flowPhase,hazeRadial,true);
            for(int i=1;i<count;i++){
                var a=TUBE_FRAMES[i-1];var b=TUBE_FRAMES[i];
                if(a.drawAlpha<=.001&&b.drawAlpha<=.001)continue;
                for(int segment=0;segment<hazeRadial;segment++){
                    tubeVertex(buffer,pose,a,segment,hazeV+(float)segment/hazeRadial);
                    tubeVertex(buffer,pose,b,segment,hazeV+(float)segment/hazeRadial);
                    tubeVertex(buffer,pose,b,segment+1,hazeV+(float)(segment+1)/hazeRadial);
                    tubeVertex(buffer,pose,a,segment+1,hazeV+(float)(segment+1)/hazeRadial);
                }
            }
        }
        // Short, detached embers follow fixed history ages. No render-frame spawning or long auxiliary tails.
        int sparkStride=lod==0?appearance.sparkStride():appearance.sparkStride()+4;
        for(int i=1;i<path.size()-1;i++){
            var frame=path.get(i);double age=renderTick-frame.tick(),behind=headArc-frame.arc();
            long birthTick=Math.round(frame.tick());
            if(Math.abs(frame.tick()-birthTick)>.001||Math.floorMod(birthTick+seed,sparkStride)!=0)continue;
            double detail=RealmMeteorFlow.detailWeight(behind);
            if(age<2||age>24||detail<=.001)continue;
            double phase=(seed&127)*.023+frame.arc()*2.39,spread=.45+age*.07;
            Vec3 center=vec(frame.position()).add(vec(frame.side()).scale(Math.cos(phase)*spread)).add(vec(frame.up()).scale(Math.sin(phase)*spread));
            Vec3 normal=unit(eye.subtract(center),new Vec3(0,0,1)),right=perpendicular(normal),vertical=normal.cross(right).normalize();
            double size=(lod>0?.10:.065)+.030*(.5+.5*Math.sin(phase));
            float alpha=(float)(frame.opacity()*Math.pow(1-age/25,1.2)*.83*detail*viewOpacity*cameraFade(center));
            float sparkV=40+(appearance.style()-1)*2;
            int sparkColor=hotWakeColor(color,.60);
            vertex(buffer,pose,center.subtract(right.scale(size)).subtract(vertical.scale(size)),normal,0,sparkV,sparkColor,alpha);
            vertex(buffer,pose,center.add(right.scale(size)).subtract(vertical.scale(size)),normal,1,sparkV,sparkColor,alpha);
            vertex(buffer,pose,center.add(right.scale(size)).add(vertical.scale(size)),normal,1,sparkV+1,sparkColor,alpha);
            vertex(buffer,pose,center.subtract(right.scale(size)).add(vertical.scale(size)),normal,0,sparkV+1,sparkColor,alpha);
        }
    }
    private static int hotWakeColor(int color,double weight){
        int r=(int)Mth.lerp(weight*.12,(color>>16)&255,255),g=(int)Mth.lerp(weight*.12,(color>>8)&255,255),b=(int)Mth.lerp(weight*.12,color&255,255);
        return r<<16|g<<8|b;
    }
    private static boolean ringNeeded(int index,int count,boolean residual){
        for(int i=Math.max(0,index-1);i<=Math.min(count-1,index+1);i++)
            if((residual?TUBE_FRAMES[i].drawAlpha:TUBE_FRAMES[i].opacity)>.001)return true;
        return false;
    }
    private static void prepareTubeRing(TubeFrame tube,int style,int layer,double phase,int radial,boolean residual){
        tube.ring.prepare(tube.frame,tube.drawWidth,tube.behind,tube.headNose,tube.headRadius,style,layer,phase,tube.bendSide,tube.bendUp,radial,residual);
        renderedSectionEvaluations+=radial+1;
    }
    private static void tubeVertex(VertexConsumer buffer,PoseStack.Pose pose,TubeFrame tube,int segment,float v){
        var ring=tube.ring;int color=tube.drawColor;
        float alpha=tube.drawAlpha;
        if(drawingFirstPerson)alpha*=FirstPersonRealmTrail.centerClearWeight(firstPersonSight.dot(new Vec3(ring.x(segment)-eye.x,ring.y(segment)-eye.y,ring.z(segment)-eye.z).normalize()));
        buffer.vertex(pose.pose(),(float)(ring.x(segment)-eye.x),(float)(ring.y(segment)-eye.y),(float)(ring.z(segment)-eye.z))
                .color((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,alpha)
                .uv(tube.drawU,v).overlayCoords(tube.packedBehind,tube.packedAge).uv2(tube.packedSide,tube.packedUp)
                .normal(pose.normal(),(float)ring.nx(segment),(float)ring.ny(segment),(float)ring.nz(segment)).endVertex();
        renderedVertices++;uncachedSectionEvaluations++;
    }
    private static void vertex(VertexConsumer buffer,PoseStack.Pose pose,Vec3 position,Vec3 normal,float u,float v,int color,float alpha){
        Vec3 relative=position.subtract(eye);
        if(drawingFirstPerson)alpha*=FirstPersonRealmTrail.centerClearWeight(firstPersonSight.dot(relative.normalize()));
        buffer.vertex(pose.pose(),(float)relative.x,(float)relative.y,(float)relative.z)
                .color((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,alpha)
                .uv(u,v).overlayCoords(0,0).uv2(0,0)
                .normal(pose.normal(),(float)normal.x,(float)normal.y,(float)normal.z).endVertex();renderedVertices++;
    }
    private RealmFlightEffects(){}
}
