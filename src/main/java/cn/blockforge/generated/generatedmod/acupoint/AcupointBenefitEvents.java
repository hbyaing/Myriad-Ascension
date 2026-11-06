package cn.blockforge.generated.generatedmod.acupoint;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static cn.blockforge.generated.generatedmod.acupoint.AcupointCatalog.Stat.*;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class AcupointBenefitEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void mine(PlayerEvent.BreakSpeed event) {
        var data = GeneratedMod.data(event.getEntity());
        if (data != null && event.getNewSpeed() > 0)
            event.setNewSpeed(event.getNewSpeed() * data.acupoints().bonuses().multiplier(MINING_SPEED_PCT));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void jump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        var data = GeneratedMod.data(player);
        var movement = player.getDeltaMovement();
        if (data == null || movement.y <= 0) return;
        double bonus = data.acupoints().bonuses().get(JUMP_POWER_PCT);
        // Add a share of the ordinary launch impulse without multiplying potion or transformation effects.
        if (bonus > 0) player.setDeltaMovement(movement.add(0, .42 * bonus / 100, 0));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void ranged(LivingHurtEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow)
                || !(event.getSource().getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0) return;
        var data = GeneratedMod.data(player);
        if (data != null) event.setAmount(event.getAmount() * data.acupoints().bonuses().multiplier(RANGED_DAMAGE_PCT));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        var data = GeneratedMod.data(player);
        if (data != null) {
            double reduction = data.acupoints().bonuses().get(FALL_RESIST_PCT) / 100;
            event.setDamageMultiplier(event.getDamageMultiplier() * (float) Math.max(0, 1 - reduction));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void critical(CriticalHitEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof LivingEntity)
                || event.getResult() == Event.Result.DENY) return;
        var data = GeneratedMod.data(player);
        if (data == null) return;
        var bonuses = data.acupoints().bonuses();
        boolean critical = event.isVanillaCritical() || event.getResult() == Event.Result.ALLOW;
        if (!critical) {
            double chance = Math.min(1, bonuses.get(CRIT_CHANCE_PCT) / 100);
            if (chance <= 0 || player.getAttackStrengthScale(.5F) <= .9F || player.getRandom().nextFloat() >= chance) return;
            event.setResult(Event.Result.ALLOW);
            event.setDamageModifier(1.5F);
        }
        // Forge supplies the one melee critical multiplier consumed by Player.attack.
        event.setDamageModifier(event.getDamageModifier() + (float) (bonuses.get(CRIT_DAMAGE_PCT) / 100));
    }

    private AcupointBenefitEvents() {}
}
