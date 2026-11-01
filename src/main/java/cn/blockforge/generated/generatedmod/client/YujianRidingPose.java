package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The render root rotates about the blade; bones only describe the standing stance. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class YujianRidingPose {
    private record Saved(float body,float oldBody,float walk) {}
    private static final java.util.Map<java.util.UUID,Saved> SAVED=new java.util.HashMap<>();
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void before(net.minecraftforge.client.event.RenderPlayerEvent.Pre event){
        var player=event.getEntity();
        if(cn.blockforge.generated.generatedmod.ParalysisControl.blocked(player)||event.isCanceled() || JindouCloudRenderState.isActive(player)
                || !(player.getVehicle() instanceof ControlledSwordEntity sword) || !sword.flightRoot())return;
        SAVED.put(player.getUUID(),new Saved(player.yBodyRot,player.yBodyRotO,player.walkAnimation.speed()));
        var frame=InterpolatedFlyingSwordTransform.get(sword,event.getPartialTick());
        player.yBodyRot=frame.yaw();player.yBodyRotO=frame.yaw();
        player.walkAnimation.setSpeed(0);
        float partial=event.getPartialTick();
        float yaw=frame.yaw();
        var delta=frame.position().subtract(Mth.lerp(partial,player.xOld,player.getX()),Mth.lerp(partial,player.yOld,player.getY()),Mth.lerp(partial,player.zOld,player.getZ()));
        var pose=event.getPoseStack();pose.pushPose();
        pose.translate(delta.x,delta.y,delta.z);
        pose.mulPose(frame.rotation());
        pose.translate(0,sword.riderOffset(),0);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
    }
    @SubscribeEvent
    public static void after(net.minecraftforge.client.event.RenderPlayerEvent.Post event){
        var saved=SAVED.remove(event.getEntity().getUUID());
        if(saved==null)return;
        var player=event.getEntity();player.yBodyRot=saved.body;player.yBodyRotO=saved.oldBody;
        player.walkAnimation.setSpeed(saved.walk);event.getPoseStack().popPose();
    }

    public static void animate(PlayerModel<?> model,ControlledSwordEntity sword,float partial){
        var frame=InterpolatedFlyingSwordTransform.get(sword,partial);
        float sprint=frame.sprintFactor();
        if(sword.renderPhase().burst())sprint=Math.min(1.5F,sprint+0.4F);
        float brace=frame.brace();
        model.riding=false;model.crouching=false;
        model.body.setPos(0,0,0);model.body.xRot=0.20F*sprint+0.10F*brace;model.body.yRot=0;model.body.zRot=0;
        model.head.setPos(0,0,-1.8F*sprint);model.head.xRot=-0.12F*sprint;model.head.yRot=0;model.head.zRot=0;
        model.rightArm.setPos(-5,2,0);model.leftArm.setPos(5,2,0);
        model.rightArm.xRot=model.leftArm.xRot=0.48F*sprint;
        model.rightArm.yRot=0.18F;model.leftArm.yRot=-0.18F;
        model.rightArm.zRot=-0.10F;model.leftArm.zRot=0.10F;
        model.rightLeg.setPos(-1.6F,12,-1.8F-1.4F*sprint);
        model.leftLeg.setPos(1.6F,12,1.8F+0.7F*sprint);
        model.rightLeg.xRot=-0.10F-0.25F*sprint;model.leftLeg.xRot=0.10F+0.16F*sprint;
        model.rightLeg.yRot=model.leftLeg.yRot=0;model.rightLeg.zRot=model.leftLeg.zRot=0;
        model.hat.copyFrom(model.head);model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);model.leftPants.copyFrom(model.leftLeg);
    }
}
