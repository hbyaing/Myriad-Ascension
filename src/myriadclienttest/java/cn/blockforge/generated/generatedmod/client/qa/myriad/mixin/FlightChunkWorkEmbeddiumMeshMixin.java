package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightChunkWorkTimings;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightChunkWorkTimings.Kind;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask",remap=false)
public abstract class FlightChunkWorkEmbeddiumMeshMixin {
    @Inject(method="execute(Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lme/jellysquid/mods/sodium/client/util/task/CancellationToken;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",at=@At("HEAD"),require=0,remap=false)
    private void begin(CallbackInfoReturnable<?> ci){FlightChunkWorkTimings.begin(Kind.EMBEDDIUM_MESH);}
    @Inject(method="execute(Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lme/jellysquid/mods/sodium/client/util/task/CancellationToken;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",at=@At("RETURN"),require=0,remap=false)
    private void end(CallbackInfoReturnable<?> ci){FlightChunkWorkTimings.end(Kind.EMBEDDIUM_MESH);}
}
