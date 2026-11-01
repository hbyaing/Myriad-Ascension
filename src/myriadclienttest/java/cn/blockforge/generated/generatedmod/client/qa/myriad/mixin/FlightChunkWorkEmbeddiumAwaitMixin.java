package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightChunkWorkTimings;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightChunkWorkTimings.Kind;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="me.jellysquid.mods.sodium.client.render.chunk.compile.executor.ChunkJobCollector",remap=false)
public abstract class FlightChunkWorkEmbeddiumAwaitMixin {
    @Shadow @Final private java.util.List<?> submitted;
    @Inject(method="awaitCompletion",at=@At("HEAD"),require=0,remap=false)
    private void begin(CallbackInfo ci){FlightChunkWorkTimings.begin(Kind.EMBEDDIUM_AWAIT);FlightChunkWorkTimings.awaitJobs(submitted.size());}
    @Inject(method="awaitCompletion",at=@At("RETURN"),require=0,remap=false)
    private void end(CallbackInfo ci){FlightChunkWorkTimings.end(Kind.EMBEDDIUM_AWAIT);}
}
