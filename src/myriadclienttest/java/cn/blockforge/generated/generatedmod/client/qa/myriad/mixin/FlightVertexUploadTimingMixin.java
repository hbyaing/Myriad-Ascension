package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightDrawTimings;
import com.mojang.blaze3d.vertex.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=VertexBuffer.class,remap=false)
public abstract class FlightVertexUploadTimingMixin {
    @Inject(method="upload",at=@At("HEAD"),remap=false) private void a(CallbackInfo c){FlightDrawTimings.span(3,true);}
    @Inject(method="upload",at=@At("RETURN"),remap=false) private void b(CallbackInfo c){FlightDrawTimings.span(3,false);}
}
