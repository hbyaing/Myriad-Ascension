package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightDrawTimings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets="cn.blockforge.generated.generatedmod.client.ControlledSwordRenderer",remap=false)
public abstract class FlightSwordEntityTimingMixin {
 @Inject(method="render(Lcn/blockforge/generated/generatedmod/yujian/ControlledSwordEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("HEAD"),remap=false) private void a(CallbackInfo c){FlightDrawTimings.span(5,true);}
 @Inject(method="render(Lcn/blockforge/generated/generatedmod/yujian/ControlledSwordEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("RETURN"),remap=false) private void b(CallbackInfo c){FlightDrawTimings.span(5,false);}
}
