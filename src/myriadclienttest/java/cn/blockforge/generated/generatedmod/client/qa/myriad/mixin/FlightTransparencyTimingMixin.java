package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightDrawTimings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets="cn.blockforge.generated.generatedmod.client.DunxuTransparency",remap=false)
public abstract class FlightTransparencyTimingMixin {
 @Inject(method="wrap",at=@At("HEAD"),remap=false) private static void a0(CallbackInfoReturnable<?> c){FlightDrawTimings.span(7,true);}
 @Inject(method="wrap",at=@At("RETURN"),remap=false) private static void b0(CallbackInfoReturnable<?> c){FlightDrawTimings.span(7,false);}
 @Inject(method="translucent",at=@At("HEAD"),remap=false) private static void a1(CallbackInfoReturnable<?> c){FlightDrawTimings.span(8,true);}
 @Inject(method="translucent",at=@At("RETURN"),remap=false) private static void b1(CallbackInfoReturnable<?> c){FlightDrawTimings.span(8,false);}
}
