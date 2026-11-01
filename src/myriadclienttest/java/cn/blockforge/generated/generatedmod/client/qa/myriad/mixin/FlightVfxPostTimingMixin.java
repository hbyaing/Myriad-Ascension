package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;
import cn.blockforge.generated.generatedmod.client.SwordFlightTrailRenderer;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightVfxWorkTimings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=SwordFlightTrailRenderer.class,remap=false)
public abstract class FlightVfxPostTimingMixin {
 @Inject(method="drawPost",at=@At("HEAD"),remap=false) private static void a(CallbackInfoReturnable<Boolean> c){FlightVfxWorkTimings.span(2,true);}
 @Inject(method="drawPost",at=@At("RETURN"),remap=false) private static void b(CallbackInfoReturnable<Boolean> c){FlightVfxWorkTimings.span(2,false);}
}
