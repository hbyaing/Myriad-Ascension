package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightDrawTimings;
import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=BufferBuilder.class,remap=false)
public abstract class FlightBufferEndTimingMixin {
    @Inject(method="end",at=@At("HEAD"),remap=false) private void a(CallbackInfoReturnable<BufferBuilder.RenderedBuffer> c){FlightDrawTimings.span(4,true);}
    @Inject(method="end",at=@At("RETURN"),remap=false) private void b(CallbackInfoReturnable<BufferBuilder.RenderedBuffer> c){FlightDrawTimings.span(4,false);}
}
