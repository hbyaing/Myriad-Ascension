package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightChunkWorkTimings;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightChunkWorkTimings.Kind;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets="net.minecraft.client.renderer.chunk.ChunkRenderDispatcher",remap=false)
public abstract class FlightChunkWorkVanillaUploadMixin {
    @Inject(method="uploadAllPendingUploads",at=@At("HEAD"),require=0,remap=false)
    private void begin(CallbackInfo ci){FlightChunkWorkTimings.begin(Kind.VANILLA_UPLOAD);}
    @Inject(method="uploadAllPendingUploads",at=@At("RETURN"),require=0,remap=false)
    private void end(CallbackInfo ci){FlightChunkWorkTimings.end(Kind.VANILLA_UPLOAD);}
}
