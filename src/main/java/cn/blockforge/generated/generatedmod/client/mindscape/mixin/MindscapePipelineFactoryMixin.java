package cn.blockforge.generated.generatedmod.client.mindscape.mixin;

import cn.blockforge.generated.generatedmod.client.mindscape.MindscapeShaderPipelineCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.function.Function;

/** No Iris types link on a client without Oculus. Other dimensions delegate. */
@Pseudo
@Mixin(targets = {"net.irisshaders.iris.pipeline.PipelineManager", "net.coderbot.iris.pipeline.PipelineManager"}, remap = false)
public abstract class MindscapePipelineFactoryMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private static Function<Object, Object> wuxingxiantu$interiorFactory(Function<Object, Object> original) {
        return MindscapeShaderPipelineCompat.wrapPipelineFactory(original);
    }
}
