package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;
import cn.blockforge.generated.generatedmod.plants.SpiritPlantRegions;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightEcologyTimings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=SpiritPlantRegions.class,remap=false)
public abstract class FlightEcologyTimingMixin {
    @Inject(method="advance",at=@At("HEAD"),remap=false) private void a(CallbackInfo c){FlightEcologyTimings.span(0,true);}
    @Inject(method="advance",at=@At("RETURN"),remap=false) private void b(CallbackInfo c){FlightEcologyTimings.span(0,false);}
    @Inject(method="surfaceBiome",at=@At("HEAD"),remap=false) private void c(CallbackInfoReturnable<String> c){FlightEcologyTimings.span(1,true);}
    @Inject(method="surfaceBiome",at=@At("RETURN"),remap=false) private void d(CallbackInfoReturnable<String> c){FlightEcologyTimings.span(1,false);}
}
