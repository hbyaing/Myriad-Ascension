package cn.blockforge.generated.generatedmod.mixin;

import cn.blockforge.generated.generatedmod.acupoint.AcupointAttributes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Group;

@Mixin(LivingEntity.class)
public abstract class AcupointArmorMixin {
    @Group(name = "baiqiao", min = 1, max = 1)
    @ModifyArg(method = "m_21161_", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/damagesource/CombatRules;m_19272_(FFF)F"), index = 1, require = 0, remap = false)
    private float wuxingxiantu$keepFractionalArmorSrg(float armor) {
        return wuxingxiantu$keepFractionalArmor(armor);
    }

    @Group(name = "baiqiao", min = 1, max = 1)
    @ModifyArg(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(FFF)F"), index = 1, require = 0, remap = false)
    private float wuxingxiantu$keepFractionalArmor(float armor) {
        return AcupointAttributes.armorForDamage((LivingEntity) (Object) this, armor);
    }
}
