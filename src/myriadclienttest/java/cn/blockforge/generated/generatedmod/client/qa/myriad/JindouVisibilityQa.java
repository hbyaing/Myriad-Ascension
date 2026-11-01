package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.client.TrailShaderBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Read-only culling/player draw observations. Never changes a return, pose or entity field. */
@Mod.EventBusSubscriber(modid="wuxingxiantu",value=Dist.CLIENT)
public final class JindouVisibilityQa {
    private static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.jindouRepro");
    private static final Map<String,Object> FRAME=new LinkedHashMap<>();
    private static org.joml.Matrix4f prePose;
    @SubscribeEvent public static void reset(TickEvent.RenderTickEvent e){if(ENABLED&&e.phase==TickEvent.Phase.START){FRAME.clear();prePose=null;FRAME.put("playerPre",false);FRAME.put("playerPost",false);}}
    public static void cull(Entity e,Frustum frustum,double x,double y,double z){
        var mc=Minecraft.getInstance();if(!ENABLED||e!=mc.player||TrailShaderBridge.shadowPass())return;float t=mc.getFrameTime();Vec3 current=e.position(),interpolated=e.getPosition(t),rendered=new Vec3(net.minecraft.util.Mth.lerp(t,e.xOld,e.getX()),net.minecraft.util.Mth.lerp(t,e.yOld,e.getY()),net.minecraft.util.Mth.lerp(t,e.zOld,e.getZ()));
        boolean inRange=e.shouldRender(x+e.getX()-interpolated.x,y+e.getY()-interpolated.y,z+e.getZ()-interpolated.z);boolean ip=frustum.isVisible(e.getBoundingBox().move(interpolated.subtract(current)));
        FRAME.put("cullObserved",true);FRAME.put("partial",t);FRAME.put("currentInFrustum",frustum.isVisible(e.getBoundingBox()));FRAME.put("getPositionInFrustum",ip);FRAME.put("xOldInterpolatedInFrustum",frustum.isVisible(e.getBoundingBox().move(rendered.subtract(current))));FRAME.put("interpolatedInRange",inRange);FRAME.put("expectedVisible",inRange&&(e.noCulling||ip)&&!e.isInvisible()&&!mc.options.getCameraType().isFirstPerson());
        FRAME.put("current",v(current));FRAME.put("xo",List.of(e.xo,e.yo,e.zo));FRAME.put("xOld",List.of(e.xOld,e.yOld,e.zOld));FRAME.put("getPosition",v(interpolated));FRAME.put("xOldInterpolated",v(rendered));FRAME.put("getPositionToCurrent",interpolated.distanceTo(current));FRAME.put("xOldInterpolatedToGetPosition",rendered.distanceTo(interpolated));FRAME.put("invisible",e.isInvisible());
    }
    public static void chunkGate(net.minecraft.core.BlockPos pos,boolean result){
        var mc=Minecraft.getInstance();if(!ENABLED||mc.player==null||TrailShaderBridge.shadowPass()||!pos.equals(mc.player.blockPosition()))return;
        FRAME.put("currentPlayerChunkGateObserved",true);FRAME.put("currentPlayerChunkGateReturned",result);FRAME.put("currentPlayerChunkGatePosition",List.of(pos.getX(),pos.getY(),pos.getZ()));
        FRAME.put("chunkGateScope","Observed actual LevelRenderer.isChunkCompiled RETURN for the current player's BlockPos; includes any production Mixin override, no extra chunk query or changed return.");
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void pre(RenderPlayerEvent.Pre e){if(ENABLED&&e.getEntity()==Minecraft.getInstance().player&&!TrailShaderBridge.shadowPass()){FRAME.put("playerPre",true);FRAME.put("playerPrePartial",e.getPartialTick());prePose=new org.joml.Matrix4f(e.getPoseStack().last().pose());FRAME.put("preHighestMatrix",prePose.get(new float[16]));}}
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public static void preFinal(RenderPlayerEvent.Pre e){if(ENABLED&&e.getEntity()==Minecraft.getInstance().player&&!TrailShaderBridge.shadowPass())FRAME.put("playerPreFinallyCanceled",e.isCanceled());}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void post(RenderPlayerEvent.Post e){if(ENABLED&&e.getEntity()==Minecraft.getInstance().player&&!TrailShaderBridge.shadowPass()){
        FRAME.put("playerPost",true);var matrix=new org.joml.Matrix4f(e.getPoseStack().last().pose());FRAME.put("postHighestMatrixBeforeProductionRestore",matrix.get(new float[16]));
        if(prePose!=null){var change=new org.joml.Matrix4f(prePose).invert().mul(matrix);Vec3 observed=new Vec3(change.m30(),change.m31(),change.m32());float t=e.getPartialTick();var p=e.getEntity();Vec3 rendered=new Vec3(net.minecraft.util.Mth.lerp(t,p.xOld,p.getX()),net.minecraft.util.Mth.lerp(t,p.yOld,p.getY()),net.minecraft.util.Mth.lerp(t,p.zOld,p.getZ()));Vec3 expected=p.getPosition(t).subtract(rendered);FRAME.put("observedModelPoseDelta",v(observed));FRAME.put("expectedGetPositionMinusXOldDelta",v(expected));FRAME.put("modelPoseDeltaError",observed.distanceTo(expected));}
    }}
    public static Map<String,Object> snapshot(){var m=new LinkedHashMap<String,Object>(FRAME);
        try{var c=Class.forName("me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer");var renderer=c.getMethod("instanceNullable").invoke(null);var pos=Minecraft.getInstance().player.blockPosition();m.put("rawEmbeddiumCurrentSectionReady",c.getMethod("isSectionReady",int.class,int.class,int.class).invoke(renderer,pos.getX()>>4,pos.getY()>>4,pos.getZ()>>4));}
        catch(ReflectiveOperationException|LinkageError unavailable){m.put("rawEmbeddiumGateError",unavailable.toString());}
        m.put("scope","Read-only current/getPosition/xOld frustum, actual LevelRenderer returned gate and raw Embeddium section-map readiness plus actual RenderPlayerEvent pre/post; no forced visibility or position/camera changes.");return m;}
    private static List<Double> v(Vec3 p){return List.of(p.x,p.y,p.z);}
    private JindouVisibilityQa(){}
}
