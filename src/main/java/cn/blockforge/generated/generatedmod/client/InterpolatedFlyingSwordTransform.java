package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.yujian.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One immutable sample per root per render frame, shared by every presentation consumer. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public record InterpolatedFlyingSwordTransform(Vec3 position,float yaw,float pitch,float roll,
        float manualRoll,float angular,float speed,float speedFactor,float sprintFactor,float spiralFactor,float brace,float huahong) {
    private static final java.util.Map<ControlledSwordEntity,InterpolatedFlyingSwordTransform> FRAMES=new java.util.IdentityHashMap<>();
    private static float framePartial;
    private static boolean rendering;
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event){
        rendering=event.phase==TickEvent.Phase.START;
        if(rendering){FRAMES.clear();framePartial=Mth.clamp(event.renderTickTime,0,1);}
    }
    public static float partial(float fallback){return rendering?framePartial:Mth.clamp(fallback,0,1);}
    public static InterpolatedFlyingSwordTransform get(ControlledSwordEntity sword,float partial){
        return FRAMES.computeIfAbsent(sword,s->{
            // A model animation's age parameter or a secondary render pass must
            // not choose a different interpolation interval for the shared frame.
            float t=partial(partial);
            var d=FlyingSwordRegistry.visual(s.display());
            float speed=s.renderSpeed(t);
            double ref=d.advanced().maxSpeed(YujianPhase.JIYU,d)*FlyingSwordConfig.CRUISE_FRACTION.get();
            float factor=(float)(speed/Math.max(.01,ref));
            float base=(float)(d.advanced().maxSpeed(YujianPhase.BASIC,d)*FlyingSwordConfig.CRUISE_FRACTION.get()/ref);
            float sprint=s.renderPhase()==YujianPhase.JIYU||s.renderPhase().burst()
                    ?Mth.clamp((factor-base)/Math.max(.01F,1-base),0,1):0;
            float angular=s.renderAngular(t);
            float spiral=s.renderPhase()==YujianPhase.LINGKONG?0:Mth.clamp(
                    (float)(Math.abs(angular)*20*Mth.RAD_TO_DEG/FlyingSwordConfig.ROLL_VISUAL_FULL.get()),0,1);
            return new InterpolatedFlyingSwordTransform(new Vec3(Mth.lerp(t,s.xOld,s.getX()),Mth.lerp(t,s.yOld,s.getY()),Mth.lerp(t,s.zOld,s.getZ())),
                    Mth.rotLerp(t,s.yRotO,s.getYRot()),Mth.lerp(t,s.xRotO,s.getXRot()),s.renderRoll(t),s.renderRoll(t)-s.renderBank(t),
                    angular,speed,factor,sprint,spiral,Mth.clamp((Math.abs(angular)*20* Mth.RAD_TO_DEG-45)/240,0,1),s.renderHuahong(t));
        });
    }
    public Quaternionf rotation(){return FlightFrame.rotation(yaw,pitch,roll);}
    public Vec3 forward(){return new Vec3(FlightFrame.rotation(yaw,pitch,0).transform(new Vector3f(0,0,1)));}
    public Vec3 local(double x,double y,double z){return position.add(new Vec3(rotation().transform(new Vector3f((float)x,(float)y,(float)z))));}
}
