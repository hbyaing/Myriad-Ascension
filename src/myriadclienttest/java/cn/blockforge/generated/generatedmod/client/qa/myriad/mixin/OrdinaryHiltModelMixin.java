package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.ControlledSwordRenderer;
import cn.blockforge.generated.generatedmod.client.qa.myriad.OrdinaryHiltQa;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=ControlledSwordRenderer.class,remap=false)
public abstract class OrdinaryHiltModelMixin {
    @Inject(method="render(Lcn/blockforge/generated/generatedmod/yujian/ControlledSwordEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("HEAD"),remap=false)
    private void captureModel(ControlledSwordEntity sword,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci){OrdinaryHiltQa.model(sword,partial,pose);}
}
