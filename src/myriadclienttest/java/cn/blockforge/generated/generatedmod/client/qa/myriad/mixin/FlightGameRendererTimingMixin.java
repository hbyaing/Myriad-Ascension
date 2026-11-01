package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightWorldTimings;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=GameRenderer.class,remap=false)
public abstract class FlightGameRendererTimingMixin {
    @Inject(method="renderLevel",at=@At("HEAD"),remap=false) private void a(CallbackInfo c){FlightWorldTimings.mark("world.begin");}
    @Inject(method="renderLevel",at=@At("RETURN"),remap=false) private void b(CallbackInfo c){FlightWorldTimings.mark("world.end");}
    @Inject(method="render",at=@At("HEAD"),remap=false) private void c(CallbackInfo c){FlightWorldTimings.mark("gameRenderer.begin");}
    @Inject(method="render",at=@At("RETURN"),remap=false) private void d(CallbackInfo c){FlightWorldTimings.mark("gameRenderer.end");}
}
