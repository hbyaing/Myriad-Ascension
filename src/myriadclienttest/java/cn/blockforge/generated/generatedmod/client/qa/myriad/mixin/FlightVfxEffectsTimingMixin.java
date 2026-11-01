package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;
import cn.blockforge.generated.generatedmod.client.FlyingSwordEffects;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightVfxWorkTimings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=FlyingSwordEffects.class,remap=false)
public abstract class FlightVfxEffectsTimingMixin {
 @Inject(method="render",at=@At("HEAD"),remap=false) private static void a(CallbackInfo c){FlightVfxWorkTimings.span(0,true);}
 @Inject(method="render",at=@At("RETURN"),remap=false) private static void b(CallbackInfo c){FlightVfxWorkTimings.span(0,false);}
 @Inject(method="submitPending",at=@At("HEAD"),remap=false) private static void c(CallbackInfo c){FlightVfxWorkTimings.span(1,true);}
 @Inject(method="submitPending",at=@At("RETURN"),remap=false) private static void d(CallbackInfo c){FlightVfxWorkTimings.span(1,false);}
}
