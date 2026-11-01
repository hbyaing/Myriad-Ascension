package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.JindouCloudConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Tick/distance sampled world positions. Rendering never appends history. */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class JindouCloudTrailCache {
    private static final Map<UUID, Track> TRACKS = new HashMap<>();
    private static ClientLevel world;

    private JindouCloudTrailCache() {}

    static void remove(UUID player) { TRACKS.remove(player); }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        if (world != client.level) { TRACKS.clear(); world = client.level; }
        if (world == null || client.isPaused()) return;
        var active = new HashSet<UUID>();
        for (Player player : world.players()) {
            if (!JindouCloudRenderState.isActive(player)) continue;
            if (client.player != null && player.distanceToSqr(client.player) > 112 * 112) continue;
            active.add(player.getUUID());
            Track track = TRACKS.computeIfAbsent(player.getUUID(), id -> new Track());
            track.sample(player.position(), world.getGameTime(),player.getDeltaMovement(),player.getPosition(0));
        }
        TRACKS.keySet().retainAll(active);
    }

    static Vec3 velocity(Player player, float partial) {
        Track track = TRACKS.get(player.getUUID());
        if (track == null) return Vec3.ZERO;
        return track.previousVelocity.lerp(track.velocity, partial);
    }

    static Vec3 heading(Player player, float partial) {
        Track track = TRACKS.get(player.getUUID());
        float yaw = track == null ? 0 : Mth.rotLerp(partial, track.previousHeadingYaw, track.headingYaw);
        double radians = Math.toRadians(yaw);
        return new Vec3(Math.sin(radians), 0, Math.cos(radians));
    }

    static float motion(Player player, float partial) {
        Track track = TRACKS.get(player.getUUID());
        return track == null ? 0 : Mth.lerp(partial, track.previousMotion, track.motion);
    }

    static double flow(Player player, float partial) {
        Track track = TRACKS.get(player.getUUID());
        return track == null ? 0 : track.previousFlow + (track.flow - track.previousFlow) * partial;
    }

    static boolean discontinuity(Player player) {
        Track track = TRACKS.get(player.getUUID());
        return track != null && track.discontinuityTick == player.level().getGameTime();
    }

    static List<Point> points(Player player, Vec3 renderedFeet, float partial) {
        Track track = TRACKS.get(player.getUUID());
        if (track == null || track.points.isEmpty()) return List.of();
        double now = player.level().getGameTime() - 1 + (double) partial;
        List<Point> visible = new ArrayList<>();
        for (Point point : track.points) {
            if (point.time() <= now && now - point.time() < JindouCloudConfig.TRAIL_LIFETIME.get()) visible.add(point);
        }
        // Connect only to the interpolated head, never to the next tick's future sample.
        if (!visible.isEmpty() && visible.get(visible.size() - 1).position().distanceToSqr(renderedFeet) > 0.00000001
                && track.velocity.lengthSqr() > 0.0001) {
            visible.add(new Point(renderedFeet, now, track.velocity));
        }
        return visible;
    }

    static Map<String,Object> diagnostics(Player player,Vec3 feet,float partial){
        Track track=TRACKS.get(player.getUUID());
        var result=new java.util.LinkedHashMap<String,Object>();
        double now=player.level().getGameTime()-1+(double)partial;
        List<Point> visible=points(player,feet,partial);
        result.put("renderTime",now);result.put("storedPoints",track==null?0:track.points.size());
        result.put("visiblePoints",visible.size());result.put("sampledSpeed",track==null?0:track.velocity.length());
        result.put("discontinuity",discontinuity(player));
        result.put("latestVisibleTime",visible.stream().mapToDouble(Point::time).max().orElse(now));
        result.put("oldestStoredTime",track==null||track.points.isEmpty()?now:track.points.getFirst().time());
        return result;
    }

    record Point(Vec3 position, double time, Vec3 velocity, float width, float density, float lifetime) {
        Point(Vec3 position, double time, Vec3 velocity) {
            this(position, time, velocity, JindouCloudConfig.RADIUS_X.get().floatValue(),
                    JindouCloudConfig.DENSITY.get().floatValue(), lifetime(velocity));
        }

        static float lifetime(Vec3 velocity) {
            return (float) Math.min(JindouCloudConfig.TRAIL_LIFETIME.get(),
                    JindouCloudConfig.TRAIL_TRAVEL.get() / Math.max(0.01, velocity.length()));
        }
    }

    private static final class Track {
        final ArrayDeque<Point> points = new ArrayDeque<>();
        Vec3 last, velocity = Vec3.ZERO, previousVelocity = Vec3.ZERO;
        float headingYaw, previousHeadingYaw, targetHeadingYaw;
        long lastTick = Long.MIN_VALUE, discontinuityTick = Long.MIN_VALUE;
        double remainder;
        float motion, previousMotion;
        double flow, previousFlow;

        void sample(Vec3 current, long tick,Vec3 expectedVelocity,Vec3 interpolationStart) {
            if (tick == lastTick) return;
            previousHeadingYaw = headingYaw;
            points.removeIf(p -> tick - p.time() > JindouCloudConfig.TRAIL_LIFETIME.get() + 1);
            Vec3 delta = last == null ? Vec3.ZERO : current.subtract(last);
            double length = delta.length();
            // Fast direction changes are continuous when the camera's previous
            // endpoint is our last tick sample. True teleports reset that pair.
            boolean cameraReset=length>JindouCloudConfig.TELEPORT_DISTANCE.get()
                    &&current.distanceToSqr(interpolationStart)<.00000001;
            boolean continuousFast=length>JindouCloudConfig.TELEPORT_DISTANCE.get()&&!cameraReset
                    && (last!=null&&last.distanceToSqr(interpolationStart)<.00000001
                    ||delta.distanceToSqr(expectedVelocity)<=Math.max(1,expectedVelocity.lengthSqr()*.25)
                    || delta.distanceToSqr(velocity)<=Math.max(1,velocity.lengthSqr()*.04));
            if (last == null || tick != lastTick + 1 || length > JindouCloudConfig.TELEPORT_DISTANCE.get()&&!continuousFast) {
                points.clear(); remainder = 0; velocity = previousVelocity = Vec3.ZERO;
                motion = previousMotion = 0;
                flow = previousFlow = 0;
                discontinuityTick = tick;
            } else {
                previousVelocity = velocity;
                velocity = delta;
                previousMotion = motion;
                motion += ((float) (length / (length + 0.7)) - motion) * 0.3F;
                previousFlow = flow;
                flow += length * 0.035;
                if (delta.horizontalDistanceSqr() > 0.0004) targetHeadingYaw = (float) Math.toDegrees(Math.atan2(delta.x, delta.z));
                headingYaw = Mth.wrapDegrees(headingYaw + Mth.clamp(Mth.wrapDegrees(targetHeadingYaw - headingYaw) * 0.30F, -18, 18));
                if (length > 0.01) {
                    if (points.isEmpty()) points.add(new Point(last, lastTick, delta));
                    // Cover the configured lifetime at high speed instead of chopping a dense far end.
                    int budget = Math.min(32, JindouCloudConfig.MAX_TRAIL_POINTS.get());
                    double spacing = Math.max(JindouCloudConfig.TRAIL_SPACING.get(),
                            length * Point.lifetime(delta) / Math.max(2, budget - 3));
                    remainder = Math.min(remainder, spacing);
                    double distance = spacing - remainder;
                    while (distance <= length) {
                        double fraction = distance / length;
                        points.addLast(new Point(last.lerp(current, fraction), tick - 1 + fraction, delta));
                        while (points.size() > JindouCloudConfig.MAX_TRAIL_POINTS.get()) points.removeFirst();
                        distance += spacing;
                    }
                    remainder = (remainder + length) % spacing;
                    // Slow travel still records turns, at most once every two ticks.
                    if (!points.isEmpty() && tick - points.getLast().time() >= 2
                            && points.getLast().position().distanceToSqr(current) > 0.0144) {
                        points.addLast(new Point(current, tick, delta));
                        remainder = 0;
                    }
                } else {
                    remainder = 0;
                    // Finish the last moving tick's sample once; stationary ticks emit nothing.
                    if (previousVelocity.lengthSqr() > 0.0001 && !points.isEmpty()
                            && points.getLast().position().distanceToSqr(last) > 0.000001) {
                        points.addLast(new Point(last, lastTick, previousVelocity));
                    }
                }
            }
            while (points.size() > JindouCloudConfig.MAX_TRAIL_POINTS.get()) points.removeFirst();
            last = current; lastTick = tick;
        }
    }
}
