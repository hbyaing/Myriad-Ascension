package cn.blockforge.generated.generatedmod.acupoint;

import cn.blockforge.generated.generatedmod.CultivationData;
import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.Network;
import cn.blockforge.generated.generatedmod.ParalysisControl;
import cn.blockforge.generated.generatedmod.items.CultivationLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static cn.blockforge.generated.generatedmod.acupoint.AcupointProgress.Result;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class AcupointService {
    private static final int LEASE_TICKS = 30;
    private static final int TAP_INTERVAL = 5;
    private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();

    private static final class Session {
        String channel = "";
        String active = "";
        int leaseUntil;
        int nextCharge;
        int nextTap;
        int nextSync;
        int nextRequest;
        int interruptedUntil;
        Vec3 channelOrigin = Vec3.ZERO;
        Result result = Result.IDLE;
        boolean dirty;
    }

    public static void handle(ServerPlayer player, AcupointPackets.Request request) {
        CultivationData data = GeneratedMod.data(player);
        if (data == null) return;
        AcupointProgress progress = data.acupoints();
        var catalog = progress.catalog();
        Session session = SESSIONS.computeIfAbsent(player, ignored -> new Session());
        int now = player.server.getTickCount();
        if (request.action() == AcupointPackets.Action.STOP) {
            stop(session);
            return;
        }
        if (request.action() == AcupointPackets.Action.SNAPSHOT) {
            if (now >= session.nextRequest) {
                session.nextRequest = now + 20;
                send(player, progress, session, !catalog.version().equals(request.catalogVersion()));
            }
            return;
        }
        if (!player.isAlive() || player.isSpectator()) return;
        if (!catalog.version().equals(request.catalogVersion())) {
            stop(session);
            update(session, Result.CONFIG_MISMATCH);
            return;
        }

        String id = request.id();
        var point = catalog.points().get(id);
        switch (request.action()) {
            case SELECT -> {
                if (point == null) { update(session, Result.UNKNOWN_POINT); return; }
                if (!session.channel.equals(id)) stop(session);
                if (!id.equals(progress.selected())) {
                    progress.select(id);
                    session.dirty = true;
                }
                update(session, Result.IDLE);
            }
            case TAP, START -> {
                if (now < session.nextTap || now < session.interruptedUntil || !session.channel.isEmpty()
                        || !session.active.isEmpty()) {
                    update(session, Result.COOLDOWN);
                    return;
                }
                session.nextTap = now + TAP_INTERVAL;
                if (point == null) { update(session, Result.UNKNOWN_POINT); return; }
                if (!id.equals(progress.selected())) {
                    progress.select(id);
                    session.dirty = true;
                }
                if (request.action() == AcupointPackets.Action.TAP) {
                    applyCharge(player, data, point, catalog.charge().tap(), session);
                } else {
                    Result eligibility = progress.eligibility(point, AcupointProgress.stage(data));
                    if (eligibility != Result.IDLE) { update(session, eligibility); return; }
                    session.channel = id;
                    session.active = id;
                    session.dirty = true;
                    session.leaseUntil = now + LEASE_TICKS;
                    session.nextCharge = now + catalog.charge().interval();
                    session.channelOrigin = player.position();
                    update(session, Result.IDLE);
                }
            }
            case RENEW -> {
                if (!session.channel.isEmpty() && session.channel.equals(id) && now < session.leaseUntil)
                    session.leaseUntil = now + LEASE_TICKS;
            }
            case AUTO_ON, AUTO_OFF -> {
                if (!catalog.meridians().containsKey(id)) { update(session, Result.INVALID_AUTO); return; }
                stop(session);
                progress.configureAuto(id, request.action() == AcupointPackets.Action.AUTO_ON);
                session.nextCharge = now + catalog.charge().interval();
                session.dirty = true;
                update(session, Result.IDLE);
            }
            default -> { }
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        CultivationData data = GeneratedMod.data(player);
        if (data == null) return;
        AcupointProgress progress = data.acupoints();
        Session session = SESSIONS.get(player);
        if (session == null && !progress.auto()) return;
        if (session == null) session = SESSIONS.computeIfAbsent(player, ignored -> new Session());
        int now = player.server.getTickCount();
        var catalog = progress.catalog();
        if (!player.isAlive() || player.isSpectator() || ParalysisControl.blocked(player) || now < session.interruptedUntil) {
            stop(session);
        } else {
            if (!session.channel.isEmpty() && (now >= session.leaseUntil || (!catalog.charge().moving()
                    && session.channelOrigin.distanceToSqr(player.position()) > 0.0025))) stop(session);
            if (now >= session.nextCharge) advance(player, data, session, now);
        }
        if (session.dirty && now >= session.nextSync) send(player, progress, session, false);
    }

    private static void advance(ServerPlayer player, CultivationData data, Session session, int now) {
        AcupointProgress progress = data.acupoints();
        var catalog = progress.catalog();
        var rates = catalog.charge();
        var point = catalog.points().get(session.channel);
        if (point == null && progress.auto() && (!rates.autoOnlyDuringMeditation() || data.wuxingxiantu$isMeditating())) {
            var route = catalog.routes().getOrDefault(progress.autoMeridian(), List.of());
            int stage = AcupointProgress.stage(data);
            point = route.stream().filter(candidate -> progress.eligibility(candidate, stage) == Result.IDLE).findFirst().orElse(null);
        }
        session.nextCharge = now + rates.interval();
        if (point == null) {
            stop(session);
            return;
        }
        if (!point.id().equals(session.active)) {
            session.active = point.id();
            session.dirty = true;
        }
        Result result = applyCharge(player, data, point, rates.perInterval(data.getMaxMana()), session);
        if (result == Result.OPENED || (result != Result.SPENT && result != Result.MANA_EMPTY)) stop(session);
    }

    private static Result applyCharge(ServerPlayer player, CultivationData data, AcupointCatalog.Point point, int amount, Session session) {
        Result result = data.acupoints().charge(point, data, amount);
        update(session, result);
        if (result == Result.OPENED) {
            stop(session);
            CultivationLogic.applyHealthBonus(player, data);
            send(player, data.acupoints(), session, false);
            GeneratedMod.sync(player);

        }
        return result;
    }

    private static void update(Session session, Result result) {
        if (session.result != result || result == Result.SPENT || result == Result.OPENED) session.dirty = true;
        session.result = result;
    }

    private static void stop(Session session) {
        if (!session.channel.isEmpty() || !session.active.isEmpty()) {
            session.channel = "";
            session.active = "";
            session.dirty = true;
        }
    }

    private static void send(ServerPlayer player, AcupointProgress progress, Session session, boolean includeCatalog) {
        var catalog = progress.catalog();
        if (player.connection != null) {
            var message = new AcupointPackets.State(progress.save(), catalog.version(),
                    includeCatalog ? catalog.documents() : List.of(), session.active, session.result);
            Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
        session.nextSync = player.server.getTickCount() + catalog.charge().syncInterval();
        session.dirty = false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0) return;
        Session session = SESSIONS.get(player);
        if (session != null && AcupointCatalog.server().charge().interrupt()) {
            stop(session);
            session.interruptedUntil = player.server.getTickCount() + AcupointCatalog.server().charge().interval();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) transition(player, true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) transition(player, false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) transition(player, false);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SESSIONS.remove(player);
    }

    @SubscribeEvent
    public static void reload(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) return;
        for (ServerPlayer player : event.getPlayerList().getPlayers()) {
            CultivationData data = GeneratedMod.data(player);
            if (data != null) {
                CultivationLogic.applyHealthBonus(player, data);
                transition(player, true);
                GeneratedMod.sync(player);
            }
        }
    }

    private static void transition(ServerPlayer player, boolean includeCatalog) {
        CultivationData data = GeneratedMod.data(player);
        if (data == null) return;
        Session session = new Session();
        SESSIONS.put(player, session);
        send(player, data.acupoints(), session, includeCatalog);
    }

    private AcupointService() {}
}
