package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightChunkWorkTimings;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightChunkWorkTimings.Kind;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager",remap=false)
public abstract class FlightChunkWorkEmbeddiumManagerMixin {
    @Inject(method="updateChunks",at=@At("HEAD"),require=0,remap=false)
    private void beginUpdate(boolean immediate,CallbackInfo ci){FlightChunkWorkTimings.begin(Kind.EMBEDDIUM_UPDATE);FlightChunkWorkTimings.immediateUpdate(immediate);}
    @Inject(method="updateChunks",at=@At("RETURN"),require=0,remap=false)
    private void endUpdate(boolean immediate,CallbackInfo ci){FlightChunkWorkTimings.end(Kind.EMBEDDIUM_UPDATE);}
    @Inject(method="uploadChunks",at=@At("HEAD"),require=0,remap=false)
    private void beginUpload(CallbackInfo ci){FlightChunkWorkTimings.begin(Kind.EMBEDDIUM_UPLOAD);}
    @Inject(method="uploadChunks",at=@At("RETURN"),require=0,remap=false)
    private void endUpload(CallbackInfo ci){FlightChunkWorkTimings.end(Kind.EMBEDDIUM_UPLOAD);}
    @Inject(method="collectChunkBuildResults",at=@At("RETURN"),require=0,remap=false)
    private void collected(CallbackInfoReturnable<?> ci){
        if(ci.getReturnValue() instanceof java.util.Collection<?> results)FlightChunkWorkTimings.uploadResults(results.size());
    }
}
