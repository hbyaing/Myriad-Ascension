package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.qa.myriad.JindouVisibilityQa;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes the real post-culling entity-section gate. Never queries extra chunks. */
@Mixin(value=LevelRenderer.class,priority=500,remap=false)
public abstract class JindouChunkGateProbeMixin {
    @Inject(method={"isChunkCompiled(Lnet/minecraft/core/BlockPos;)Z","m_202430_(Lnet/minecraft/core/BlockPos;)Z"},at=@At("RETURN"),remap=false,require=0)
    private void observe(BlockPos pos,CallbackInfoReturnable<Boolean> result){JindouVisibilityQa.chunkGate(pos,result.getReturnValue());}
}
