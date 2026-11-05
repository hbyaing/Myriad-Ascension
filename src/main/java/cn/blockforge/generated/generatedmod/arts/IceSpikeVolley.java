package cn.blockforge.generated.generatedmod.arts;

import cn.blockforge.generated.generatedmod.ArtCatalog;
import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.items.HumanEmperorBannerItem;
import cn.blockforge.generated.generatedmod.network.NetworkChannel;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.IceSpikePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Authoritative flight and impact. Clients interpolate the same tips used for collision. */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class IceSpikeVolley {
    public record Lance(int delay, double length, double width, double bearing, double height, double speed) {}

    public static final List<Lance> FORMATION = List.of(
            new Lance(0, 3.74, .425, .80, 1.20, .58),
            new Lance(2, 2.50, .282, -1.60, 1.88, .53),
            new Lance(3, 2.16, .232, 1.61, 1.75, .56),
            new Lance(1, 2.42, .259, -2.66, .84, .54),
            new Lance(3, 2.07, .226, 2.42, .78, .52),
            new Lance(2, 1.83, .188, -.46, 2.19, .50),
            new Lance(4, 1.59, .171, 3.02, 1.58, .49));
    public static final int WINDUP = 12;
    public static final int FLIGHT = 19;
    private static final Map<ServerLevel, List<Cast>> CASTS = new IdentityHashMap<>();

    private IceSpikeVolley() {}

    public static boolean canCast(ServerPlayer player) {
        var casts = CASTS.get(player.serverLevel());
        if (casts == null) return true;
        long tick = player.level().getGameTime();
        return casts.stream().noneMatch(cast -> cast.player == player && cast.started == tick)
                && casts.stream().filter(cast -> cast.player == player).count() < 4;
    }

    public static void cast(ServerPlayer player, float damage) {
        List<Cast> casts = CASTS.computeIfAbsent(player.serverLevel(), ignored -> new ArrayList<>());
        Cast cast = new Cast(player, damage);
        casts.add(cast);
        cast.advance();
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        List<Cast> casts = CASTS.get(level);
        if (casts == null) return;
        casts.removeIf(cast -> cast.player.isRemoved() || !cast.player.isAlive() || cast.player.isSpectator()
                || cast.player.serverLevel() != level || !cast.advance());
        if (casts.isEmpty()) CASTS.remove(level);
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) CASTS.remove(level);
    }

    private static final class Cast {
        private final ServerPlayer player;
        private final ServerLevel level;
        private final float damage;
        private final double range = ArtCatalog.attackProfile(FourArtsVisuals.ICE).range();
        private final Vec3 forward;
        private final Vec3 side;
        private final long started;
        private final Shot[] shots = new Shot[FORMATION.size()];
        private final Set<Integer> struck = new HashSet<>();
        private List<LivingEntity> targets = List.of();
        private int lastAge = -1;

        Cast(ServerPlayer player, float damage) {
            this.player = player;
            this.damage = damage;
            level = player.serverLevel();
            forward = Vec3.directionFromRotation(0, player.getYRot());
            side = new Vec3(-forward.z, 0, forward.x);
            started = level.getGameTime();
            for (int i = 0; i < shots.length; i++) shots[i] = new Shot();
        }

        boolean advance() {
            int age = (int)(level.getGameTime() - started);
            if (lastAge == age) return true;
            lastAge = age;
            Vec3 base = player.position();
            if (age == WINDUP - 5 || age == WINDUP) {
                targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                                entity -> eligible(entity) && entity.distanceToSqr(player) <= range * range
                                        && player.hasLineOfSight(entity))
                        .stream().sorted(Comparator.comparingDouble((LivingEntity entity) -> entity.distanceToSqr(player))
                                .thenComparingInt(LivingEntity::getId)).limit(shots.length).toList();
            }
            List<IceSpikePacket.Sample> samples = new ArrayList<>(shots.length);
            for (int i = 0; i < shots.length; i++) {
                Shot shot = shots[i];
                if (shot.done) continue;
                Lance shape = FORMATION.get(i);
                int flight = age - WINDUP - shape.delay;
                if (flight > FLIGHT) {
                    shot.done = true;
                    continue;
                }
                if (flight <= 0) {
                    double settle = smooth(age / (double)(WINDUP + shape.delay));
                    double bearing = shape.bearing + .39 * settle;
                    Vec3 out = forward.scale(Math.cos(bearing)).add(side.scale(Math.sin(bearing)));
                    Vec3 center = base.add(0, shape.height + .063 * Math.sin(age * .325 + shape.bearing), 0);
                    Vec3 anchor = center.add(out.scale(1.36 - .19 * settle));
                    var wall = block(center, anchor);
                    shot.tip = wall.getType() == HitResult.Type.BLOCK ? wall.getLocation().subtract(out.scale(.06)) : anchor;
                    if (!targets.isEmpty()) shot.target = targets.get(i % targets.size());
                    else shot.target = null;
                    shot.free = forward.add(side.scale(Math.sin(shape.bearing) * .13)).add(0, (shape.height - 1) * .037, 0).normalize();
                    Vec3 aim = validLock(shot, base) ? aim(shot.target).subtract(shot.tip).normalize() : shot.free;
                    shot.axis = shot.free.lerp(aim, settle).normalize();
                    if (flight == 0) shot.axis = clearCaster(shot.tip, shot.axis, shape);
                    samples.add(new IceSpikePacket.Sample(i, flight, shot.tip, shot.axis, false));
                    continue;
                }

                Vec3 from = shot.tip;
                double step = shape.speed * (.72 + .28 * smooth(flight / 4.0));
                if (!validLock(shot, base) && shot.target != null) {
                    shot.target = targets.stream().filter(target -> eligible(target) && !struck.contains(target.getId())
                                    && target.position().distanceToSqr(base) <= range * range && player.hasLineOfSight(target))
                            .min(Comparator.comparingDouble(target -> target.position().distanceToSqr(from))).orElse(null);
                }
                Vec3 toward = shot.target == null ? shot.free : aim(shot.target).subtract(from);
                Vec3 desired = toward.normalize();
                if (desired.lengthSqr() > .1) {
                    if (flight <= 6) desired = clearCaster(from, desired, shape);
                    shot.axis = shot.axis.lerp(desired, toward.lengthSqr() <= step * step ? 1 : .65).normalize();
                }
                Vec3 end = from.add(shot.axis.scale(step));
                var wall = block(from, end);
                boolean contact = wall.getType() == HitResult.Type.BLOCK;
                if (contact) end = wall.getLocation();
                var hit = ProjectileUtil.getEntityHitResult(level, player, from, end,
                        new AABB(from, end).inflate(.60), entity -> entity instanceof LivingEntity target && eligible(target));
                if (hit != null && hit.getEntity() instanceof LivingEntity target) {
                    end = hit.getLocation();
                    contact = true;
                    if (struck.add(target.getId()) && target.hurt(player.damageSources().indirectMagic(player, player), damage))
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false, true), player);
                }
                shot.tip = end;
                shot.done = contact;
                samples.add(new IceSpikePacket.Sample(i, flight, end, shot.axis, contact));
            }
            if (samples.isEmpty()) return false;
            NetworkChannel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new IceSpikePacket(player.getId(), started, age, base, forward, samples));
            return true;
        }

        private boolean eligible(LivingEntity target) {
            return target instanceof Enemy && target.isAlive() && !target.isSpectator()
                    && !target.isAlliedTo(player) && !HumanEmperorBannerItem.isServant(target);
        }

        private boolean validLock(Shot shot, Vec3 base) {
            return shot.target != null && eligible(shot.target)
                    && shot.target.position().distanceToSqr(base) <= (range + 2) * (range + 2);
        }

        private Vec3 aim(LivingEntity target) {
            return target.getBoundingBox().getCenter().add(target.getDeltaMovement().scale(1.4));
        }

        private Vec3 clearCaster(Vec3 from, Vec3 direction, Lance shape) {
            if (player.getBoundingBox().inflate(.24).clip(from, from.add(direction.scale(1.9))).isEmpty()) return direction;
            Vec3 lateral = new Vec3(-direction.z, 0, direction.x).normalize();
            if (lateral.lengthSqr() < .1) lateral = side;
            double offset = from.subtract(player.position()).dot(lateral);
            double sign = Math.copySign(1, Math.abs(offset) < .03 ? Math.sin(shape.bearing) : offset);
            return direction.scale(.38).add(lateral.scale(sign)).normalize();
        }

        private HitResult block(Vec3 start, Vec3 end) {
            return level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        }
    }

    private static double smooth(double amount) {
        double t = Math.max(0, Math.min(1, amount));
        return t * t * (3 - 2 * t);
    }

    private static final class Shot {
        Vec3 tip;
        Vec3 axis;
        Vec3 free;
        LivingEntity target;
        boolean done;
    }
}
