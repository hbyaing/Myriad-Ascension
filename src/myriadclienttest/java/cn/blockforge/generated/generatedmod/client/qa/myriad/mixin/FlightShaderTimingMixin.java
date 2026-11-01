package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightDrawTimings;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ShaderInstance.class,remap=false)
public abstract class FlightShaderTimingMixin {
    @Inject(method="apply",at=@At("HEAD"),remap=false) private void a(CallbackInfo c){FlightDrawTimings.span(1,true);}
    @Inject(method="apply",at=@At("RETURN"),remap=false) private void b(CallbackInfo c){FlightDrawTimings.span(1,false);}
    @Inject(method="clear",at=@At("HEAD"),remap=false) private void c(CallbackInfo c){FlightDrawTimings.span(2,true);}
    @Inject(method="clear",at=@At("RETURN"),remap=false) private void d(CallbackInfo c){FlightDrawTimings.span(2,false);}
}
