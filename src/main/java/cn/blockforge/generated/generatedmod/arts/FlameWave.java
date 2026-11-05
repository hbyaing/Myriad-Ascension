package cn.blockforge.generated.generatedmod.arts;

import cn.blockforge.generated.generatedmod.ArtCatalog;
import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.items.HumanEmperorBannerItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class FlameWave {
    private static final ArtCatalog.AttackProfile PROFILE = ArtCatalog.attackProfile(FourArtsVisuals.FLAME);
    public static final double RANGE = PROFILE.range();
    public static final double HALF_ANGLE = Math.toRadians(PROFILE.angleDegrees() / 2);
    public static final double RELEASE = .35;
    private static final double TRAVEL = .90;
    private static final double FALL_OFF = 1.37;
    private static final double CONE_EDGE = Math.cos(HALF_ANGLE);
    private static final Map<ServerLevel, List<Cast>> CASTS = new IdentityHashMap<>();

    private FlameWave() {}

    public static double front(double age) {
        return RANGE * (1 - Math.pow(1 - Mth.clamp((age - RELEASE) / TRAVEL, 0, 1), FALL_OFF));
    }

    public static double arrival(double distance) {
        return RELEASE + TRAVEL * (1 - Math.pow(1 - Mth.clamp(distance / RANGE, 0, 1), 1 / FALL_OFF));
    }

    public static boolean canCast(ServerPlayer player) {
        var casts = CASTS.get(player.serverLevel());
        if (casts == null) return true;
        long now = player.serverLevel().getGameTime();
        return casts.stream().noneMatch(cast -> cast.caster == player && cast.started == now)
                && casts.stream().filter(cast -> cast.caster == player).count() < 4;
    }

    public static void cast(ServerPlayer caster, float damage) {
        var level = caster.serverLevel();
        Vec3 origin = caster.position();
        float yaw = caster.getYRot();
        CASTS.computeIfAbsent(level, ignored -> new ArrayList<>()).add(new Cast(caster, level.getGameTime(),
                origin, Vec3.directionFromRotation(0, yaw), caster.getBoundingBox().inflate(RANGE), damage, new HashSet<>()));
        FourArtsVisuals.flame(caster, origin, yaw);
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        var casts = CASTS.get(level);
        if (casts == null) return;
        casts.removeIf(cast -> !cast.advance(level));
        if (casts.isEmpty()) CASTS.remove(level);
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) CASTS.remove(level);
    }

    private record Cast(ServerPlayer caster, long started, Vec3 origin, Vec3 direction, AABB bounds, float damage, Set<Integer> struck) {
        boolean advance(ServerLevel level) {
            if (caster.isRemoved() || !caster.isAlive() || caster.isSpectator() || caster.serverLevel() != level) return false;
            double age = (level.getGameTime() - started) / 20.0;
            if (level.getGameTime() - started == Math.round(RELEASE * 20)) caster.swing(InteractionHand.MAIN_HAND, true);
            double outer = front(age);
            if (outer <= 0) return true;
            double inner = front(age - .05);
            for (var target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                    entity -> entity != caster && entity.isAlive() && !entity.isAlliedTo(caster)
                            && !HumanEmperorBannerItem.isServant(entity))) {
                Vec3 offset = target.position().subtract(origin);
                double distance = offset.horizontalDistance();
                if (distance <= inner || distance > outer || offset.dot(direction) < distance * CONE_EDGE) continue;
                if (struck.add(target.getId()) && target.hurt(caster.damageSources().magic(), damage)) target.setSecondsOnFire(4);
            }
            return outer < RANGE;
        }
    }
}
