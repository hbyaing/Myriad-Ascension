package cn.blockforge.generated.generatedmod;

import java.util.function.Consumer;

/**
 * PLAY_TO_CLIENT 包的客户端处理桥接。
 *
 * <p>包记录的 handle 方法若直接引用 net.minecraft.client 下的类，Forge 的 RuntimeDistCleaner
 * 会在模组注册阶段（专用服务端同样执行 registerMessage）就把这些 common 类连同客户端引用一起
 * 加载并拒绝，导致服务端启动即崩。这里用只含 common 类型的无操作 Consumer 做占位，真正的客户端
 * 逻辑由 client 包在启动期替换进来，专用服务端则始终只加载到占位实现。
 */
public final class ClientPacketSink {
    public static Consumer<cn.blockforge.generated.generatedmod.acupoint.AcupointPackets.State> acupointState = packet -> {};
    public static Consumer<net.minecraft.nbt.CompoundTag> playerProgression=n->{};
    public static java.util.function.Consumer<net.minecraft.core.BlockPos> coreBlueprint=pos->{};
    public static java.util.function.Consumer<cn.blockforge.generated.generatedmod.network.packet.CoreBuildProgressPacket> coreBuildProgress=packet->{};
    public static Consumer<AlchemyFurnaceBlockEntity> furnaceLoaded=furnace->{};
    public static java.util.function.Consumer<net.minecraft.nbt.ListTag> manaLoss=rows->{};
    public static Consumer<Boolean> manaLossTracking=enabled->{};
    public static Consumer<cn.blockforge.generated.generatedmod.tribulation.TribulationVisualPacket> tribulationVisual=packet->{};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.divineart.DipperVisualPacket> dipperVisualPacket=packet->{};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.cultivation.CultivationVitalsPacket> cultivationVitals=packet->{};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.cultivation.WeaponFlameHitPacket> weaponFlameHit=packet->{};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.heartsword.HeartForgeContractPacket> heartForgeContract=packet->{};
    public static java.util.function.Predicate<net.minecraft.world.item.ItemStack> heartForgeVisible=stack->false;
    public static Consumer<cn.blockforge.generated.generatedmod.yujian.FlightSnapshot> flightSnapshot=packet->{};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.mindscape.MindscapeStatePacket> mindscapeState=packet->{};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.yujian.RobeSyncPacket> robeSync=packet->{};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.cultivation.SyncPacket> syncPacket = packet -> {};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.cultivation.OpenScreenPacket> openScreenPacket = packet -> {};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.cultivation.TribulationStatePacket> tribulationStatePacket = packet -> {};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.cultivation.SoulTradeStatePacket> soulTradeStatePacket = packet -> {};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.cultivation.StopUsingItemPacket> stopUsingItemPacket = packet -> {};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.cultivation.BannerStatePacket> bannerStatePacket = packet -> {};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.divineart.DivineArtBannerPacket> divineArtBannerPacket = packet -> {};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.cultivation.FireSyncPacket> fireSyncPacket = packet -> {};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.cultivation.FireStatePacket> fireStatePacket = packet -> {};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.cultivation.ExoticBurnPacket> exoticBurnPacket = packet -> {};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.cultivation.WrapVisualPacket> wrapVisualPacket = packet -> {};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.divineart.DivineSyncPacket> divineSyncPacket = packet -> {};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.divineart.CloudVisualPacket> cloudVisualPacket = packet -> {};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.divineart.OpenTransformSelectionPacket> openTransformSelectionPacket = packet -> {};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.cultivation.CompassResultPacket> compassResultPacket = packet -> {};
    public static Consumer<cn.blockforge.generated.generatedmod.network.packet.heartsword.HeartSwordVisualPacket> heartSwordVisualPacket = packet -> {};


    private ClientPacketSink() {}
    public static Consumer<net.minecraft.nbt.CompoundTag> heavenlyFurnace = tag -> {};

    /** 执行客户端处理；异常只记日志，绝不让它冒泡到网络/渲染线程把游戏踢出。 */
    public static <T> void safe(Consumer<T> handler, T packet) {
        try {
            handler.accept(packet);
        } catch (Throwable throwable) {
            LOGGER.warn("处理客户端数据包 {} 失败", packet.getClass().getSimpleName(), throwable);
        }
    }

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
}
