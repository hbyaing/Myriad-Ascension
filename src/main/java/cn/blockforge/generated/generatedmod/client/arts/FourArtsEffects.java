package cn.blockforge.generated.generatedmod.client.arts;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.client.TrailShaderBridge;
import cn.blockforge.generated.generatedmod.client.realmflight.MeteorBloomRenderer;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.FourArtsPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.FourArtsPacket.Action;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static cn.blockforge.generated.generatedmod.arts.FourArtsVisuals.*;

@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class FourArtsEffects {
    private static final double RANGE_SQUARED=56*56;
    private static final BufferBuilder BUFFER=new BufferBuilder(65536);
    private static final Map<AbstractClientPlayer,State> STATES=new HashMap<>();
    private static final Map<AbstractClientPlayer,FourArtsPose.Sample> POSES=new HashMap<>();
    private static ClientLevel world;
    private static Frame pending;
    private static ShaderInstance shader;
    private static long drawnFrames;
    private static int drawnMaterials;

    private FourArtsEffects() {}

    public static void accept(FourArtsPacket packet) {
        var level=Minecraft.getInstance().level;
        setWorld(level);
        if(level==null || !(level.getEntity(packet.playerId()) instanceof AbstractClientPlayer player)) return;
        if(packet.action()==Action.STATE && packet.state()==0 && !STATES.containsKey(player)) return;
        var state=STATES.computeIfAbsent(player,ignored->new State());
        long tick=level.getGameTime();
        switch(packet.action()) {
            case STATE -> {
                int bits=packet.state() & (WIND|PASSIVE_WIND|SPIRIT|BLOOD_AURA);
                if((bits&WIND)!=0 && (state.bits&WIND)==0) state.wind=tick;
                if((bits&SPIRIT)!=0 && (state.bits&SPIRIT)==0) state.spirit=tick;
                if((bits&BLOOD_AURA)!=0 && (state.bits&BLOOD_AURA)==0) state.blood=tick;
                if((bits&(WIND|PASSIVE_WIND))==0) state.history.clear();
                state.bits=bits;
                state.lastState=tick;
            }
            case RENEWAL, PASSIVE_RENEWAL -> {
                state.renewal=tick;
                state.passiveRenewal=packet.action()==Action.PASSIVE_RENEWAL;
            }
            case PURIFICATION, PASSIVE_PURIFICATION -> {
                state.purification=tick;
                state.passivePurification=packet.action()==Action.PASSIVE_PURIFICATION;
                state.removedEffects=state.passivePurification?0:Math.min(4,packet.state());
            }
            case GATHER_PULSE -> state.gatherPulse=tick;
            case BLOOD_HIT, PASSIVE_BLOOD_HIT -> {
                Vec3 p=packet.position();
                if(!Double.isFinite(p.x)||!Double.isFinite(p.y)||!Double.isFinite(p.z)
                        ||p.distanceToSqr(player.position())>16*16) return;
                if(state.hits.size()>=8) state.hits.removeFirst();
                state.hits.addLast(new Hit(tick,p,p.subtract(player.getEyePosition()).normalize(),
                        packet.action()==Action.PASSIVE_BLOOD_HIT));
            }
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        var level=Minecraft.getInstance().level;
        setWorld(level);
        if(level==null) return;
        long now=level.getGameTime();
        var iterator=STATES.entrySet().iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next();
            var player=entry.getKey();
            State state=entry.getValue();
            state.hits.removeIf(hit->now-hit.tick>=14);
            if(now-state.lastState>27) state.bits=0;
            if(player.isRemoved() || !player.isAlive() || player.isSpectator() ||
                    state.bits==0&&now-state.renewal>32&&now-state.purification>30&&now-state.gatherPulse>20&&state.hits.isEmpty()) {
                iterator.remove();
                continue;
            }
            if((state.bits&(WIND|PASSIVE_WIND))!=0) sampleWind(player,state,now);
            else state.history.clear();
        }
    }

    private static void sampleWind(AbstractClientPlayer player,State state,long now) {
        Vec3 p=player.position();
        WindPoint previous=state.history.peekLast();
        if(previous!=null && previous.time==now) return;
        Vec3 move=previous==null?Vec3.ZERO:p.subtract(previous.point);
        if(move.lengthSqr()>9) {
            state.history.clear();
            move=Vec3.ZERO;
        }
        Vec3 horizontal=new Vec3(move.x,0,move.z);
        Vec3 front=horizontal.lengthSqr()>.00005?horizontal.normalize():Vec3.directionFromRotation(0,player.yBodyRot);
        if(state.history.size()>=17) state.history.removeFirst();
        state.history.addLast(new WindPoint(now,p,front));
    }

    static boolean needsPose(AbstractClientPlayer player) {
        var state=STATES.get(player);
        return state!=null && ((state.bits&BLOOD_AURA)!=0 || needsSurface(player))
                && !TrailShaderBridge.shadowPass();
    }

    static boolean needsSurface(AbstractClientPlayer player) {
        var state=STATES.get(player);
        long now=player.level().getGameTime();
        return state!=null && (now-state.renewal<(state.passiveRenewal?16:32)
                || now-state.purification<(state.passivePurification?12:20));
    }

    static void pose(AbstractClientPlayer player,FourArtsPose.Sample pose) { POSES.put(player,pose); }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void prepare(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES || TrailShaderBridge.shadowPass()) return;
        clearPending();
        var mc=Minecraft.getInstance();
        if(mc.level==null || STATES.isEmpty() || shader==null) { POSES.clear(); return; }
        double now=mc.level.getGameTime()+event.getPartialTick();
        Vec3 camera=event.getCamera().getPosition();
        Vec3 right=FourArtsPose.vec(new Vector3f(1,0,0).rotate(event.getCamera().rotation()));
        Vec3 up=FourArtsPose.vec(new Vector3f(0,1,0).rotate(event.getCamera().rotation()));
        var geometry=new FourArtsGeometry(camera,right,up);
        var scenePose=new Matrix4f(event.getPoseStack().last().pose());
        var inverse=new Matrix4f(scenePose).invert();
        float partial=event.getPartialTick();
        for(var entry:STATES.entrySet()) {
            var player=entry.getKey();
            if(!player.isAlive() || player.isSpectator() || player.distanceToSqr(camera)>RANGE_SQUARED) continue;
            var state=entry.getValue();
            Vec3 base=new Vec3(Mth.lerp(partial,player.xOld,player.getX()),
                    Mth.lerp(partial,player.yOld,player.getY()),Mth.lerp(partial,player.zOld,player.getZ()));
            if((state.bits&(WIND|PASSIVE_WIND))!=0) {
                boolean passive=(state.bits&WIND)==0;
                boolean descending=(state.bits&PASSIVE_WIND)!=0 && !player.onGround()
                        && !player.isFallFlying() && !player.isInWater() && player.getY()-player.yOld<-.007;
                var history=new ArrayList<>(state.history);
                if(!history.isEmpty()) {
                    var last=history.get(history.size()-1);
                    history.set(history.size()-1,new WindPoint(last.time,base,last.forward));
                }
                FourArtsAnimation.wind(geometry,base,Vec3.directionFromRotation(0,player.yBodyRot),
                        history,passive,descending,(now%24000)/20,(now-state.wind)/20);
            }
            if((state.bits&SPIRIT)!=0) FourArtsAnimation.gathering(geometry,base,(now-state.spirit)/20,false);
            if(now-state.gatherPulse<20 && (state.bits&SPIRIT)==0)
                FourArtsAnimation.gathering(geometry,base,(now-state.gatherPulse)/20,true);
            var sample=POSES.get(player);
            var surface=sample==null?java.util.List.<Vec3>of()
                    :sample.surface().stream().map(point->FourArtsPose.Sample.inWorld(point,inverse,camera)).toList();
            if(now-state.renewal<32) {
                double age=(now-state.renewal)/20;
                FourArtsAnimation.renewal(geometry,base,player.getBbHeight(),age,state.passiveRenewal);
                FourArtsAnimation.renewalSurface(geometry,surface,age,state.passiveRenewal);
            }
            if(now-state.purification<30)
                FourArtsAnimation.purification(geometry,surface,base,player.getBbHeight(),(now-state.purification)/20,
                        state.passivePurification,state.removedEffects);
            if((state.bits&BLOOD_AURA)!=0) {
                Vec3 hand=sample==null?FourArtsPose.fallbackHand(player,base,partial)
                        :FourArtsPose.Sample.inWorld(sample.hand(),inverse,camera);
                if(hand.distanceToSqr(base)>9) hand=FourArtsPose.fallbackHand(player,base,partial);
                if(mc.getCameraEntity()==player && mc.options.getCameraType().isFirstPerson()) {
                    double sign=player.getMainArm()==HumanoidArm.RIGHT?1:-1;
                    hand=camera.add(player.getViewVector(partial).scale(.87)).add(right.scale(sign*.36)).subtract(up.scale(.44));
                }
                FourArtsAnimation.bloodCharge(geometry,hand,(now-state.blood)/20);
            }
            for(var hit:state.hits) FourArtsAnimation.bloodHit(geometry,hit.point,hit.direction,(now-hit.tick)/20,hit.passive);
        }
        POSES.clear();
        var mesh=geometry.bake(BUFFER);
        if(mesh==null) return;
        if(!TrailShaderBridge.captureDepth()) { mesh.release(); return; }
        var clip=new Matrix4f(event.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix()).mul(scenePose);
        pending=new Frame(mesh,clip,(float)((now%24000)/20),geometry.materials);
    }

    @SubscribeEvent(priority=EventPriority.HIGH)
    public static void composite(RenderGuiEvent.Pre event) {
        if(pending==null) return;
        event.getGuiGraphics().flush();
        submit();
    }

    @SubscribeEvent(priority=EventPriority.HIGH)
    public static void renderTick(TickEvent.RenderTickEvent event) {
        if(event.phase==TickEvent.Phase.START) clearPending();
        else if(Minecraft.getInstance().options.hideGui) submit();
    }

    private static void submit() {
        if(pending==null) return;
        Frame frame=pending;
        pending=null;
        int depth=TrailShaderBridge.capturedDepthTexture();
        if(depth<0 || shader==null) { frame.mesh.release(); return; }
        var target=Minecraft.getInstance().getMainRenderTarget();
        try(var saved=new MeteorBloomRenderer.State()) {
            target.bindWrite(true);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.disableScissor();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
            shader.safeGetUniform("ClipMatrix").set(frame.clip);
            if((frame.materials&(1<<FourArtsGeometry.RECOVERY))!=0)
                shader.safeGetUniform("InverseClip").set(new Matrix4f(frame.clip).invert());
            shader.safeGetUniform("OutputSize").set((float)target.viewWidth,(float)target.viewHeight);
            shader.safeGetUniform("time").set(frame.time);
            RenderSystem.setShaderTexture(0,depth);
            RenderSystem.setShader(()->shader);
            BufferUploader.drawWithShader(frame.mesh);
            drawnFrames++;
            drawnMaterials=frame.materials;
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { setWorld(null); }

    private static void setWorld(ClientLevel level) {
        if(world==level) return;
        world=level;
        STATES.clear();
        POSES.clear();
        release();
    }

    private static void clearPending() {
        if(pending!=null) pending.mesh.release();
        pending=null;
    }

    public static void release() {
        clearPending();
    }

    @Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Shaders {
        @SubscribeEvent
        public static void register(RegisterShadersEvent event) throws IOException {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    new ResourceLocation(GeneratedMod.MOD_ID,"four_arts"),DefaultVertexFormat.POSITION_TEX_COLOR),s->shader=s);
        }
    }

    record WindPoint(long time,Vec3 point,Vec3 forward) {}
    private record Hit(long tick,Vec3 point,Vec3 direction,boolean passive) {}
    private record Frame(BufferBuilder.RenderedBuffer mesh,Matrix4f clip,float time,int materials) {}

    private static final class State {
        int bits;
        long lastState,wind,spirit,blood;
        long renewal=Long.MIN_VALUE/2,purification=Long.MIN_VALUE/2,gatherPulse=Long.MIN_VALUE/2;
        boolean passiveRenewal,passivePurification;
        int removedEffects;
        final ArrayDeque<WindPoint> history=new ArrayDeque<>();
        final ArrayDeque<Hit> hits=new ArrayDeque<>();
    }
}
