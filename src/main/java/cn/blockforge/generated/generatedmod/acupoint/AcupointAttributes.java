package cn.blockforge.generated.generatedmod.acupoint;

import cn.blockforge.generated.generatedmod.CultivationData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

import static cn.blockforge.generated.generatedmod.acupoint.AcupointCatalog.Stat.*;

public final class AcupointAttributes {
    // Ordinary synchronized modifiers carry the raw reward. The attribute mixin
    // moves the flat contribution to the end of vanilla's calculation.
    public static final UUID FLAT = UUID.fromString("9bc7897f-2b7f-4f97-b659-742b22cd604c");
    public static final UUID SPEED = UUID.fromString("b6687681-aac5-4ee1-ad35-ab05b3e78058");

    public static void apply(LivingEntity player, CultivationData data) {
        var bonus = data.acupoints().bonuses();
        update(player, Attributes.MAX_HEALTH, FLAT, AttributeModifier.Operation.ADDITION, bonus.get(MAX_HEALTH));
        update(player, Attributes.ATTACK_DAMAGE, FLAT, AttributeModifier.Operation.ADDITION, bonus.get(ATTACK));
        update(player, Attributes.ARMOR, FLAT, AttributeModifier.Operation.ADDITION, bonus.get(DEFENSE));
        update(player, Attributes.ARMOR_TOUGHNESS, FLAT, AttributeModifier.Operation.ADDITION, bonus.get(TOUGHNESS));
        update(player, Attributes.MOVEMENT_SPEED, SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL, bonus.get(MOVE_SPEED_PCT) / 100);
        update(player, Attributes.ATTACK_SPEED, SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL, bonus.get(ATTACK_SPEED_PCT) / 100);
        update(player, ForgeMod.SWIM_SPEED.get(), SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL, bonus.get(SWIM_SPEED_PCT) / 100);
        update(player, ForgeMod.ENTITY_GRAVITY.get(), SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL,
                -Math.min(.85, bonus.get(LIGHTNESS_PCT) / 100));
        update(player, ForgeMod.STEP_HEIGHT_ADDITION.get(), FLAT, AttributeModifier.Operation.ADDITION, bonus.get(STEP_HEIGHT));
        update(player, Attributes.LUCK, FLAT, AttributeModifier.Operation.ADDITION, bonus.get(LUCK));
        update(player, ForgeMod.BLOCK_REACH.get(), FLAT, AttributeModifier.Operation.ADDITION, bonus.get(BLOCK_REACH));
        update(player, ForgeMod.ENTITY_REACH.get(), FLAT, AttributeModifier.Operation.ADDITION, bonus.get(ENTITY_REACH));
    }

    private static void update(LivingEntity entity, Attribute key, UUID id, AttributeModifier.Operation operation, double amount) {
        var attribute = entity.getAttribute(key);
        if (attribute == null) return;
        var previous = attribute.getModifier(id);
        if (previous != null && previous.getAmount() == amount) return;
        if (previous != null) attribute.removeModifier(id);
        if (amount != 0) attribute.addTransientModifier(new AttributeModifier(id, "wuxingxiantu.baiqiao", amount, operation));
    }

    public static float armorForDamage(LivingEntity entity, float vanillaArmor) {
        var attribute = entity.getAttribute(Attributes.ARMOR);
        var modifier = attribute == null ? null : attribute.getModifier(FLAT);
        if (modifier == null) return vanillaArmor;
        // The engine's displayed armor is an integer, but CombatRules accepts
        // fractional armor. Use the clamped attribute value for Baiqiao armor.
        return (float) attribute.getValue();
    }

    private AcupointAttributes() {}
}
