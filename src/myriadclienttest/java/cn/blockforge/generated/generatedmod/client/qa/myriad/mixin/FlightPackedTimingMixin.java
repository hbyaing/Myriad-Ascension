package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightDrawTimings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets="cn.blockforge.generated.generatedmod.client.DunxuTransparency$PackedBulk",remap=false)
public abstract class FlightPackedTimingMixin {
 @Inject(method="bind",at=@At("HEAD"),remap=false) private static void a0(CallbackInfoReturnable<?> c){FlightDrawTimings.span(9,true);}
 @Inject(method="bind",at=@At("RETURN"),remap=false) private static void b0(CallbackInfoReturnable<?> c){FlightDrawTimings.span(9,false);}
 @Inject(method="write",at=@At("HEAD"),remap=false) private static void a1(CallbackInfoReturnable<?> c){FlightDrawTimings.span(10,true);}
 @Inject(method="write",at=@At("RETURN"),remap=false) private static void b1(CallbackInfoReturnable<?> c){FlightDrawTimings.span(10,false);}
}
