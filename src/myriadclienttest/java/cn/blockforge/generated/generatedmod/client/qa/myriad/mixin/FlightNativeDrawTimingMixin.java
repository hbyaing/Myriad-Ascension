package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightDrawTimings;
import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=GlStateManager.class,remap=false)
public abstract class FlightNativeDrawTimingMixin {
    @Inject(method="_drawElements",at=@At("HEAD"),remap=false) private static void a(CallbackInfo c){FlightDrawTimings.span(0,true);}
    @Inject(method="_drawElements",at=@At("RETURN"),remap=false) private static void b(CallbackInfo c){FlightDrawTimings.span(0,false);}
}
