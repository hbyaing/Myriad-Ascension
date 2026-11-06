package cn.blockforge.generated.generatedmod.mixin;

import cn.blockforge.generated.generatedmod.acupoint.AcupointAttributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Group;

@Mixin(AttributeInstance.class)
public abstract class AcupointAttributeMixin {
    @Group(name = "baiqiao", min = 1, max = 1)
    @ModifyArg(method = "m_22137_", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/attributes/Attribute;m_6740_(D)D"), require = 0, remap = false)
    private double wuxingxiantu$finishFlatRewardSrg(double value) {
        return wuxingxiantu$finishFlatReward(value);
    }

    @Group(name = "baiqiao", min = 1, max = 1)
    @ModifyArg(method = "calculateValue", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/attributes/Attribute;sanitizeValue(D)D"), require = 0, remap = false)
    private double wuxingxiantu$finishFlatReward(double value) {
        var instance = (AttributeInstance) (Object) this;
        var reward = instance.getModifier(AcupointAttributes.FLAT);
        if (reward == null) return value;
        double scale = 1;
        for (var modifier : instance.getModifiers(AttributeModifier.Operation.MULTIPLY_BASE)) scale += modifier.getAmount();
        for (var modifier : instance.getModifiers(AttributeModifier.Operation.MULTIPLY_TOTAL)) scale *= 1 + modifier.getAmount();
        return value + reward.getAmount() * (1 - scale);
    }
}
