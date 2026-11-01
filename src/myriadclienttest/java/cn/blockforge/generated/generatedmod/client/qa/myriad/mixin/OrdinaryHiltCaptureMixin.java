package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.client.qa.myriad.OrdinaryHiltQa;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=FlyingSwordEffects.class,remap=false)
public abstract class OrdinaryHiltCaptureMixin {
    @Shadow private static boolean spiralPass;
    @Inject(method="vertex",at=@At("HEAD"),remap=false)
    private static void capture(VertexConsumer buffer,PoseStack.Pose pose,Vec3 p,float u,float v,float alpha,CallbackInfo ci){OrdinaryHiltQa.vertex(pose,p,u,v,alpha,spiralPass);}
}
