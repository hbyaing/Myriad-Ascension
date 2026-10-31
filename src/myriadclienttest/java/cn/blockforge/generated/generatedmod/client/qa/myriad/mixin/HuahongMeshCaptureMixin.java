package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.FlyingSwordEffects;
import cn.blockforge.generated.generatedmod.client.HuahongVisuals;
import cn.blockforge.generated.generatedmod.client.qa.myriad.HuahongMotionQa;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Intercepts the real shared normal/emission consumer once. Opt-in QA source set only. */
@Mixin(value=FlyingSwordEffects.class,remap=false)
public abstract class HuahongMeshCaptureMixin {
    @Redirect(method="render",at=@At(value="INVOKE",target="Lcn/blockforge/generated/generatedmod/client/HuahongVisuals;draw(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/world/phys/Vec3;DF)V"),remap=false)
    private static void captureSubmittedMesh(VertexConsumer buffer,PoseStack.Pose pose,Vec3 eye,double now,float partial){
        HuahongVisuals.draw(HuahongMotionQa.captureBuffer(buffer,pose,eye,now,partial),pose,eye,now,partial);
    }
}
