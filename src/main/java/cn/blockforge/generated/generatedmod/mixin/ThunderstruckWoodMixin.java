package cn.blockforge.generated.generatedmod.mixin;

import cn.blockforge.generated.generatedmod.blocks.ThunderstruckWood;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LightningBolt.class, remap = false)
public abstract class ThunderstruckWoodMixin {
    @Shadow(aliases = "f_20860_") private int life;
    @Shadow(aliases = "f_20862_") private boolean visualOnly;

    @Shadow(aliases = "m_147162_")
    protected abstract BlockPos getStrikePosition();

    @Inject(method = {"tick", "m_8119_"}, at = @At("HEAD"))
    private void wuxing$charPeachWoodOnFirstImpact(CallbackInfo callback) {
        if (life != 2 || visualOnly) return;
        LightningBolt bolt = (LightningBolt) (Object) this;
        if (bolt.level() instanceof ServerLevel level
                && !bolt.getPersistentData().getBoolean("wuxingxiantu_artificial_lightning")) {
            ThunderstruckWood.strike(level, getStrikePosition());
        }
    }
}
