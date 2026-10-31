package cn.blockforge.generated.generatedmod.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import cn.blockforge.generated.generatedmod.network.packet.heartsword.HeartSwordVisualPacket;
import cn.blockforge.generated.generatedmod.network.packet.heartsword.HeartSwordRequestPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.CompassResultPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.CompassOpenPacket;
import cn.blockforge.generated.generatedmod.network.packet.divineart.DivineSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.divineart.CloudVisualPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.FireSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.WrapVisualPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.ExoticBurnPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.FireStatePacket;
import cn.blockforge.generated.generatedmod.network.packet.divineart.PossessionInputPacket;
import cn.blockforge.generated.generatedmod.network.packet.divineart.TransformAttackPacket;
import cn.blockforge.generated.generatedmod.network.packet.divineart.PossessTargetPacket;
import cn.blockforge.generated.generatedmod.network.packet.divineart.OpenTransformSelectionPacket;
import cn.blockforge.generated.generatedmod.network.packet.divineart.TransformCancelPacket;
import cn.blockforge.generated.generatedmod.network.packet.divineart.TransformSelectPacket;
import cn.blockforge.generated.generatedmod.network.packet.divineart.TransformCreeperReleasePacket;
import cn.blockforge.generated.generatedmod.network.packet.divineart.TransformCyclePacket;
import cn.blockforge.generated.generatedmod.network.packet.divineart.DivineReleasePacket;
import cn.blockforge.generated.generatedmod.network.packet.divineart.DivineArtBannerPacket;
import cn.blockforge.generated.generatedmod.network.packet.divineart.EquipDivineArtPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.BannerActionPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.OpenBannerPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.BannerStatePacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.StopUsingItemPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.SoulTradeStatePacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.SoulTradeFavoritePacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.FireDomainPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.WoodDomainPacket;
import cn.blockforge.generated.generatedmod.network.packet.yujian.SwordFlightStatePacket;
import cn.blockforge.generated.generatedmod.network.packet.yujian.SwordNurtureActionPacket;
import cn.blockforge.generated.generatedmod.network.packet.yujian.SwordFlightInputPacket;
import cn.blockforge.generated.generatedmod.network.packet.yujian.OpenRobePacket;
import cn.blockforge.generated.generatedmod.network.packet.yujian.RobeSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.mindscape.MindscapeCancelPacket;
import cn.blockforge.generated.generatedmod.network.packet.mindscape.MindscapeStatePacket;
import cn.blockforge.generated.generatedmod.network.packet.yujian.SwordFlightPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.EquipArtPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.TribulationStatePacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.TribulationPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.CrystallizePacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.SoulFurnaceCyclePacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.RootFormPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.FireSelectPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.OpenScreenPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.SyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.SelectHeartMethodPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.SelectRootPacket;
import cn.blockforge.generated.generatedmod.network.packet.alchemy.AlchemyPacket;
import cn.blockforge.generated.generatedmod.network.packet.alchemy.RefiningPacket;
import cn.blockforge.generated.generatedmod.network.packet.alchemy.CollectRefiningPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.ArtReleasePacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.GoldDomainPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.WaterDomainPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.EarthDomainPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.CompassTeleportPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.CompassSearchPacket;
import cn.blockforge.generated.generatedmod.network.packet.divineart.TransformAbilityPacket;
import cn.blockforge.generated.generatedmod.network.packet.talisman.TalismanSubmitPacket;
import cn.blockforge.generated.generatedmod.network.packet.talisman.TalismanRubPacket;
import cn.blockforge.generated.generatedmod.network.packet.talisman.TalismanSelectPacket;
import cn.blockforge.generated.generatedmod.*;


public final class NetworkChannel {
    // New formation block/menu registries require matching client and server builds.
    private static final String VERSION = "108";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GeneratedMod.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);
    private static final java.util.Set<Integer> IDS=new java.util.HashSet<>();

    public static void register() {
        c2s(96,cn.blockforge.generated.generatedmod.yujian.HuahongSkinPacket.class,cn.blockforge.generated.generatedmod.yujian.HuahongSkinPacket::encode,cn.blockforge.generated.generatedmod.yujian.HuahongSkinPacket::decode,cn.blockforge.generated.generatedmod.yujian.HuahongSkinPacket::handle);
        c2s(80,cn.blockforge.generated.generatedmod.network.packet.cultivation.ExoticFireTargetingPacket.class,cn.blockforge.generated.generatedmod.network.packet.cultivation.ExoticFireTargetingPacket::encode,cn.blockforge.generated.generatedmod.network.packet.cultivation.ExoticFireTargetingPacket::decode,cn.blockforge.generated.generatedmod.network.packet.cultivation.ExoticFireTargetingPacket::handle);
        c2s(79,cn.blockforge.generated.generatedmod.network.packet.cultivation.ManaLossTogglePacket.class,cn.blockforge.generated.generatedmod.network.packet.cultivation.ManaLossTogglePacket::encode,cn.blockforge.generated.generatedmod.network.packet.cultivation.ManaLossTogglePacket::decode,cn.blockforge.generated.generatedmod.network.packet.cultivation.ManaLossTogglePacket::handle);
        s2c(77,cn.blockforge.generated.generatedmod.network.packet.cultivation.ShieldShatterPacket.class,cn.blockforge.generated.generatedmod.network.packet.cultivation.ShieldShatterPacket::encode,cn.blockforge.generated.generatedmod.network.packet.cultivation.ShieldShatterPacket::decode,cn.blockforge.generated.generatedmod.network.packet.cultivation.ShieldShatterPacket::handle);
        s2c(78,cn.blockforge.generated.generatedmod.network.packet.cultivation.ManaLossPacket.class,cn.blockforge.generated.generatedmod.network.packet.cultivation.ManaLossPacket::encode,cn.blockforge.generated.generatedmod.network.packet.cultivation.ManaLossPacket::decode,cn.blockforge.generated.generatedmod.network.packet.cultivation.ManaLossPacket::handle);
        s2c(76,cn.blockforge.generated.generatedmod.network.packet.cultivation.ShieldImpactPacket.class,cn.blockforge.generated.generatedmod.network.packet.cultivation.ShieldImpactPacket::encode,cn.blockforge.generated.generatedmod.network.packet.cultivation.ShieldImpactPacket::decode,cn.blockforge.generated.generatedmod.network.packet.cultivation.ShieldImpactPacket::handle);
        s2c(75,cn.blockforge.generated.generatedmod.tribulation.TribulationVisualPacket.class,cn.blockforge.generated.generatedmod.tribulation.TribulationVisualPacket::encode,cn.blockforge.generated.generatedmod.tribulation.TribulationVisualPacket::decode,cn.blockforge.generated.generatedmod.tribulation.TribulationVisualPacket::handle);
        c2s(74,cn.blockforge.generated.generatedmod.realmflight.RealmFlightTogglePacket.class,
                cn.blockforge.generated.generatedmod.realmflight.RealmFlightTogglePacket::encode,
                cn.blockforge.generated.generatedmod.realmflight.RealmFlightTogglePacket::decode,
                cn.blockforge.generated.generatedmod.realmflight.RealmFlightTogglePacket::handle);
        c2s(73,cn.blockforge.generated.generatedmod.realmflight.RealmFlightInputPacket.class,
                cn.blockforge.generated.generatedmod.realmflight.RealmFlightInputPacket::encode,
                cn.blockforge.generated.generatedmod.realmflight.RealmFlightInputPacket::decode,
                cn.blockforge.generated.generatedmod.realmflight.RealmFlightInputPacket::handle);
        s2c(72,cn.blockforge.generated.generatedmod.network.packet.divineart.DipperVisualPacket.class,
                cn.blockforge.generated.generatedmod.network.packet.divineart.DipperVisualPacket::encode,
                cn.blockforge.generated.generatedmod.network.packet.divineart.DipperVisualPacket::decode,
                cn.blockforge.generated.generatedmod.network.packet.divineart.DipperVisualPacket::handle);
        c2s(71,cn.blockforge.generated.generatedmod.realmflight.RealmFlightModePacket.class,
                cn.blockforge.generated.generatedmod.realmflight.RealmFlightModePacket::encode,
                cn.blockforge.generated.generatedmod.realmflight.RealmFlightModePacket::decode,
                cn.blockforge.generated.generatedmod.realmflight.RealmFlightModePacket::handle);
        s2c(70,cn.blockforge.generated.generatedmod.realmflight.RealmFlightVisualPacket.class,
                cn.blockforge.generated.generatedmod.realmflight.RealmFlightVisualPacket::encode,
                cn.blockforge.generated.generatedmod.realmflight.RealmFlightVisualPacket::decode,
                cn.blockforge.generated.generatedmod.realmflight.RealmFlightVisualPacket::handle);
        s2c(68, cn.blockforge.generated.generatedmod.network.packet.cultivation.ArtVisualPacket.class,
                cn.blockforge.generated.generatedmod.network.packet.cultivation.ArtVisualPacket::encode,
                cn.blockforge.generated.generatedmod.network.packet.cultivation.ArtVisualPacket::decode,
                cn.blockforge.generated.generatedmod.network.packet.cultivation.ArtVisualPacket::handle);
        s2c(67,cn.blockforge.generated.generatedmod.network.packet.boss.BossTransformMotionPacket.class,cn.blockforge.generated.generatedmod.network.packet.boss.BossTransformMotionPacket::encode,cn.blockforge.generated.generatedmod.network.packet.boss.BossTransformMotionPacket::decode,cn.blockforge.generated.generatedmod.network.packet.boss.BossTransformMotionPacket::handle);
        s2c(66, cn.blockforge.generated.generatedmod.network.packet.cultivation.CultivationVitalsPacket.class,
                cn.blockforge.generated.generatedmod.network.packet.cultivation.CultivationVitalsPacket::encode,
                cn.blockforge.generated.generatedmod.network.packet.cultivation.CultivationVitalsPacket::decode,
                cn.blockforge.generated.generatedmod.network.packet.cultivation.CultivationVitalsPacket::handle);
        s2c(0, cn.blockforge.generated.generatedmod.network.packet.boss.BossCombatFxPacket.class, cn.blockforge.generated.generatedmod.network.packet.boss.BossCombatFxPacket::encode, cn.blockforge.generated.generatedmod.network.packet.boss.BossCombatFxPacket::decode, cn.blockforge.generated.generatedmod.network.packet.boss.BossCombatFxPacket::handle);
        s2c(1, cn.blockforge.generated.generatedmod.network.packet.cultivation.WeaponFlameHitPacket.class, cn.blockforge.generated.generatedmod.network.packet.cultivation.WeaponFlameHitPacket::encode, cn.blockforge.generated.generatedmod.network.packet.cultivation.WeaponFlameHitPacket::decode, cn.blockforge.generated.generatedmod.network.packet.cultivation.WeaponFlameHitPacket::handle);
        s2c(2, cn.blockforge.generated.generatedmod.network.packet.heartsword.HeartForgeContractPacket.class, cn.blockforge.generated.generatedmod.network.packet.heartsword.HeartForgeContractPacket::encode, cn.blockforge.generated.generatedmod.network.packet.heartsword.HeartForgeContractPacket::decode, cn.blockforge.generated.generatedmod.network.packet.heartsword.HeartForgeContractPacket::handle);
        c2s(3, SelectRootPacket.class, SelectRootPacket::encode, SelectRootPacket::decode, SelectRootPacket::handle);
        c2s(4, SelectHeartMethodPacket.class, SelectHeartMethodPacket::encode, SelectHeartMethodPacket::decode, SelectHeartMethodPacket::handle);
        s2c(5, SyncPacket.class, SyncPacket::encode, SyncPacket::decode, SyncPacket::handle);
        c2s(6, HeartSwordRequestPacket.class, HeartSwordRequestPacket::encode, HeartSwordRequestPacket::decode, HeartSwordRequestPacket::handle);
        s2c(7, HeartSwordVisualPacket.class, HeartSwordVisualPacket::encode, HeartSwordVisualPacket::decode, HeartSwordVisualPacket::handle);
        s2c(8, OpenScreenPacket.class, OpenScreenPacket::encode, OpenScreenPacket::decode, OpenScreenPacket::handle);
        c2s(95, cn.blockforge.generated.generatedmod.network.packet.alchemy.AlchemyHeatPacket.class, cn.blockforge.generated.generatedmod.network.packet.alchemy.AlchemyHeatPacket::encode, cn.blockforge.generated.generatedmod.network.packet.alchemy.AlchemyHeatPacket::decode, cn.blockforge.generated.generatedmod.network.packet.alchemy.AlchemyHeatPacket::handle);
        c2s(9, AlchemyPacket.class, AlchemyPacket::encode, AlchemyPacket::decode, AlchemyPacket::handle);
        c2s(10, RefiningPacket.class, RefiningPacket::encode, RefiningPacket::decode, RefiningPacket::handle);
        c2s(11, CollectRefiningPacket.class, CollectRefiningPacket::encode, CollectRefiningPacket::decode, CollectRefiningPacket::handle);
        c2s(12, SoulFurnaceCyclePacket.class, SoulFurnaceCyclePacket::encode, SoulFurnaceCyclePacket::decode, SoulFurnaceCyclePacket::handle);
        c2s(13, CrystallizePacket.class, CrystallizePacket::encode, CrystallizePacket::decode, CrystallizePacket::handle);
        c2s(14, TribulationPacket.class, TribulationPacket::encode, TribulationPacket::decode, TribulationPacket::handle);
        s2c(15, TribulationStatePacket.class, TribulationStatePacket::encode, TribulationStatePacket::decode, TribulationStatePacket::handle);
        c2s(16, EquipArtPacket.class, EquipArtPacket::encode, EquipArtPacket::decode, EquipArtPacket::handle);
        c2s(69, cn.blockforge.generated.generatedmod.network.packet.cultivation.EquipPassiveArtPacket.class,
                cn.blockforge.generated.generatedmod.network.packet.cultivation.EquipPassiveArtPacket::encode,
                cn.blockforge.generated.generatedmod.network.packet.cultivation.EquipPassiveArtPacket::decode,
                cn.blockforge.generated.generatedmod.network.packet.cultivation.EquipPassiveArtPacket::handle);
        c2s(17, ArtReleasePacket.class, ArtReleasePacket::encode, ArtReleasePacket::decode, ArtReleasePacket::handle);
        c2s(18, SwordFlightPacket.class, SwordFlightPacket::encode, SwordFlightPacket::decode, SwordFlightPacket::handle);
        c2s(19, SwordFlightInputPacket.class, SwordFlightInputPacket::encode, SwordFlightInputPacket::decode, SwordFlightInputPacket::handle);
        s2c(20, MindscapeStatePacket.class, MindscapeStatePacket::encode, MindscapeStatePacket::decode, MindscapeStatePacket::handle);
        c2s(21, MindscapeCancelPacket.class, MindscapeCancelPacket::encode, MindscapeCancelPacket::decode, MindscapeCancelPacket::handle);
        s2c(22, RobeSyncPacket.class, RobeSyncPacket::encode, RobeSyncPacket::decode, RobeSyncPacket::handle);
        c2s(23, OpenRobePacket.class, OpenRobePacket::encode, OpenRobePacket::decode, OpenRobePacket::handle);
        s2c(24, SwordFlightStatePacket.class, SwordFlightStatePacket::encode, SwordFlightStatePacket::decode, SwordFlightStatePacket::handle);
         c2s(25, SwordNurtureActionPacket.class, SwordNurtureActionPacket::encode, SwordNurtureActionPacket::decode, SwordNurtureActionPacket::handle);
        c2s(26, WoodDomainPacket.class, WoodDomainPacket::encode, WoodDomainPacket::decode, WoodDomainPacket::handle);
        c2s(27, FireDomainPacket.class, FireDomainPacket::encode, FireDomainPacket::decode, FireDomainPacket::handle);
        c2s(28, GoldDomainPacket.class, GoldDomainPacket::encode, GoldDomainPacket::decode, GoldDomainPacket::handle);
        c2s(29, WaterDomainPacket.class, WaterDomainPacket::encode, WaterDomainPacket::decode, WaterDomainPacket::handle);
        c2s(30, EarthDomainPacket.class, EarthDomainPacket::encode, EarthDomainPacket::decode, EarthDomainPacket::handle);
        c2s(31, SoulTradeFavoritePacket.class, SoulTradeFavoritePacket::encode, SoulTradeFavoritePacket::decode, SoulTradeFavoritePacket::handle);
        s2c(32, SoulTradeStatePacket.class, SoulTradeStatePacket::encode, SoulTradeStatePacket::decode, SoulTradeStatePacket::handle);
        s2c(33, StopUsingItemPacket.class, StopUsingItemPacket::encode, StopUsingItemPacket::decode, StopUsingItemPacket::handle);
        c2s(34, OpenBannerPacket.class, OpenBannerPacket::encode, OpenBannerPacket::decode, OpenBannerPacket::handle);
        c2s(35, BannerActionPacket.class, BannerActionPacket::encode, BannerActionPacket::decode, BannerActionPacket::handle);
        s2c(36, BannerStatePacket.class, BannerStatePacket::encode, BannerStatePacket::decode, BannerStatePacket::handle);
        c2s(37, EquipDivineArtPacket.class, EquipDivineArtPacket::encode, EquipDivineArtPacket::decode, EquipDivineArtPacket::handle);
        c2s(38, DivineReleasePacket.class, DivineReleasePacket::encode, DivineReleasePacket::decode, DivineReleasePacket::handle);
        s2c(39, DivineArtBannerPacket.class, DivineArtBannerPacket::encode, DivineArtBannerPacket::decode, DivineArtBannerPacket::handle);
        c2s(40, TransformCyclePacket.class, TransformCyclePacket::encode, TransformCyclePacket::decode, TransformCyclePacket::handle);
        c2s(41, TransformAbilityPacket.class, TransformAbilityPacket::encode, TransformAbilityPacket::decode, TransformAbilityPacket::handle);
        c2s(42, TransformCreeperReleasePacket.class, TransformCreeperReleasePacket::encode, TransformCreeperReleasePacket::decode, TransformCreeperReleasePacket::handle);
        c2s(43, TransformSelectPacket.class, TransformSelectPacket::encode, TransformSelectPacket::decode, TransformSelectPacket::handle);
        c2s(44, TransformCancelPacket.class, TransformCancelPacket::encode, TransformCancelPacket::decode, TransformCancelPacket::handle);
        c2s(45, PossessionInputPacket.class, PossessionInputPacket::encode, PossessionInputPacket::decode, PossessionInputPacket::handle);
        c2s(46, cn.blockforge.generated.generatedmod.yujian.SwordRecallPacket.class, cn.blockforge.generated.generatedmod.yujian.SwordRecallPacket::encode, cn.blockforge.generated.generatedmod.yujian.SwordRecallPacket::decode, cn.blockforge.generated.generatedmod.yujian.SwordRecallPacket::handle);
        c2s(47, PossessTargetPacket.class, PossessTargetPacket::encode, PossessTargetPacket::decode, PossessTargetPacket::handle);
         c2s(48, TransformAttackPacket.class, TransformAttackPacket::encode, TransformAttackPacket::decode, TransformAttackPacket::handle);
        s2c(49, DivineSyncPacket.class, DivineSyncPacket::encode, DivineSyncPacket::decode, DivineSyncPacket::handle);
        s2c(50, OpenTransformSelectionPacket.class, OpenTransformSelectionPacket::encode, OpenTransformSelectionPacket::decode, OpenTransformSelectionPacket::handle);
        c2s(51, FireSelectPacket.class, FireSelectPacket::encode, FireSelectPacket::decode, FireSelectPacket::handle);
        c2s(52, RootFormPacket.class, RootFormPacket::encode, RootFormPacket::decode, RootFormPacket::handle);
        s2c(53, FireSyncPacket.class, FireSyncPacket::encode, FireSyncPacket::decode, FireSyncPacket::handle);
         s2c(54, FireStatePacket.class, FireStatePacket::encode, FireStatePacket::decode, FireStatePacket::handle);
         s2c(55, ExoticBurnPacket.class, ExoticBurnPacket::encode, ExoticBurnPacket::decode, ExoticBurnPacket::handle);
        s2c(56, WrapVisualPacket.class, WrapVisualPacket::encode, WrapVisualPacket::decode, WrapVisualPacket::handle);
        c2s(57, TalismanSubmitPacket.class, TalismanSubmitPacket::encode, TalismanSubmitPacket::decode, TalismanSubmitPacket::handle);
        c2s(58, TalismanRubPacket.class, TalismanRubPacket::encode, TalismanRubPacket::decode, TalismanRubPacket::handle);
        c2s(59, TalismanSelectPacket.class, TalismanSelectPacket::encode, TalismanSelectPacket::decode, TalismanSelectPacket::handle);
        c2s(60, CompassSearchPacket.class, CompassSearchPacket::encode, CompassSearchPacket::decode, CompassSearchPacket::handle);
        s2c(61, CompassResultPacket.class, CompassResultPacket::encode, CompassResultPacket::decode, CompassResultPacket::handle);
        c2s(62, CompassOpenPacket.class, CompassOpenPacket::encode, CompassOpenPacket::decode, CompassOpenPacket::handle);
        c2s(63, CompassTeleportPacket.class, CompassTeleportPacket::encode, CompassTeleportPacket::decode, CompassTeleportPacket::handle);
        s2c(64, CloudVisualPacket.class, CloudVisualPacket::encode, CloudVisualPacket::decode, CloudVisualPacket::handle);
        s2c(65, cn.blockforge.generated.generatedmod.yujian.FlightSnapshot.class, cn.blockforge.generated.generatedmod.yujian.FlightSnapshot::encode, cn.blockforge.generated.generatedmod.yujian.FlightSnapshot::decode, cn.blockforge.generated.generatedmod.yujian.FlightSnapshot::handle);
        s2c(83, cn.blockforge.generated.generatedmod.network.packet.HeavenlyFurnaceStatePacket.class, cn.blockforge.generated.generatedmod.network.packet.HeavenlyFurnaceStatePacket::encode, cn.blockforge.generated.generatedmod.network.packet.HeavenlyFurnaceStatePacket::decode, cn.blockforge.generated.generatedmod.network.packet.HeavenlyFurnaceStatePacket::handle);
        c2s(84, cn.blockforge.generated.generatedmod.network.packet.HeavenlyFurnaceStartPacket.class, cn.blockforge.generated.generatedmod.network.packet.HeavenlyFurnaceStartPacket::encode, cn.blockforge.generated.generatedmod.network.packet.HeavenlyFurnaceStartPacket::decode, cn.blockforge.generated.generatedmod.network.packet.HeavenlyFurnaceStartPacket::handle);
        c2s(85, cn.blockforge.generated.generatedmod.network.packet.ProgressionActionPacket.class, cn.blockforge.generated.generatedmod.network.packet.ProgressionActionPacket::encode, cn.blockforge.generated.generatedmod.network.packet.ProgressionActionPacket::decode, cn.blockforge.generated.generatedmod.network.packet.ProgressionActionPacket::handle);
        s2c(86, cn.blockforge.generated.generatedmod.network.packet.ProgressionStatePacket.class, cn.blockforge.generated.generatedmod.network.packet.ProgressionStatePacket::encode, cn.blockforge.generated.generatedmod.network.packet.ProgressionStatePacket::decode, cn.blockforge.generated.generatedmod.network.packet.ProgressionStatePacket::handle);
        c2s(81, cn.blockforge.generated.generatedmod.network.packet.CoreBuildInputPacket.class, cn.blockforge.generated.generatedmod.network.packet.CoreBuildInputPacket::encode, cn.blockforge.generated.generatedmod.network.packet.CoreBuildInputPacket::decode, cn.blockforge.generated.generatedmod.network.packet.CoreBuildInputPacket::handle);
        s2c(82, cn.blockforge.generated.generatedmod.network.packet.CoreBuildProgressPacket.class, cn.blockforge.generated.generatedmod.network.packet.CoreBuildProgressPacket::encode, cn.blockforge.generated.generatedmod.network.packet.CoreBuildProgressPacket::decode, cn.blockforge.generated.generatedmod.network.packet.CoreBuildProgressPacket::handle);
        s2c(87, cn.blockforge.generated.generatedmod.formation.SwordArrayStatePacket.class,
                cn.blockforge.generated.generatedmod.formation.SwordArrayStatePacket::encode,
                cn.blockforge.generated.generatedmod.formation.SwordArrayStatePacket::decode,
                cn.blockforge.generated.generatedmod.formation.SwordArrayStatePacket::handle);
        s2c(88, cn.blockforge.generated.generatedmod.storage.network.BagStatePacket.class, cn.blockforge.generated.generatedmod.storage.network.BagStatePacket::encode, cn.blockforge.generated.generatedmod.storage.network.BagStatePacket::new, cn.blockforge.generated.generatedmod.storage.network.BagStatePacket::handle);
        c2s(89, cn.blockforge.generated.generatedmod.storage.network.BagScrollPacket.class, cn.blockforge.generated.generatedmod.storage.network.BagScrollPacket::encode, cn.blockforge.generated.generatedmod.storage.network.BagScrollPacket::new, cn.blockforge.generated.generatedmod.storage.network.BagScrollPacket::handle);
        c2s(90, cn.blockforge.generated.generatedmod.storage.network.BagFilterPacket.class, cn.blockforge.generated.generatedmod.storage.network.BagFilterPacket::encode, cn.blockforge.generated.generatedmod.storage.network.BagFilterPacket::new, cn.blockforge.generated.generatedmod.storage.network.BagFilterPacket::handle);
        s2c(91, cn.blockforge.generated.generatedmod.divinesurvey.SurveyPacket.class, cn.blockforge.generated.generatedmod.divinesurvey.SurveyPacket::encode, cn.blockforge.generated.generatedmod.divinesurvey.SurveyPacket::new, cn.blockforge.generated.generatedmod.divinesurvey.SurveyPacket::handle);
        s2c(92, cn.blockforge.generated.generatedmod.veincompass.VeinCompassStatePacket.class, cn.blockforge.generated.generatedmod.veincompass.VeinCompassStatePacket::encode, cn.blockforge.generated.generatedmod.veincompass.VeinCompassStatePacket::new, cn.blockforge.generated.generatedmod.veincompass.VeinCompassStatePacket::handle);
        c2s(93, cn.blockforge.generated.generatedmod.meditationqte.QteActionPacket.class, cn.blockforge.generated.generatedmod.meditationqte.QteActionPacket::encode, cn.blockforge.generated.generatedmod.meditationqte.QteActionPacket::new, cn.blockforge.generated.generatedmod.meditationqte.QteActionPacket::handle);
        s2c(94, cn.blockforge.generated.generatedmod.meditationqte.QteStatePacket.class, cn.blockforge.generated.generatedmod.meditationqte.QteStatePacket::encode, cn.blockforge.generated.generatedmod.meditationqte.QteStatePacket::new, cn.blockforge.generated.generatedmod.meditationqte.QteStatePacket::handle);
    }
    private static <T> void c2s(int id,Class<T> type,java.util.function.BiConsumer<T,net.minecraft.network.FriendlyByteBuf> encode,
            java.util.function.Function<net.minecraft.network.FriendlyByteBuf,T> decode,
            java.util.function.BiConsumer<T,java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context>> handle){
        register(id,type,encode,decode,handle,net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER);
    }
    private static <T> void s2c(int id,Class<T> type,java.util.function.BiConsumer<T,net.minecraft.network.FriendlyByteBuf> encode,
            java.util.function.Function<net.minecraft.network.FriendlyByteBuf,T> decode,
            java.util.function.BiConsumer<T,java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context>> handle){
        register(id,type,encode,decode,handle,net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
    }
    private static <T> void register(int id,Class<T> type,java.util.function.BiConsumer<T,net.minecraft.network.FriendlyByteBuf> encode,
            java.util.function.Function<net.minecraft.network.FriendlyByteBuf,T> decode,
            java.util.function.BiConsumer<T,java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context>> handle,
            net.minecraftforge.network.NetworkDirection direction){
        if(!IDS.add(id))throw new IllegalStateException("Duplicate packet ID: "+id);
        java.util.function.BiConsumer<T,java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context>> guarded=(packet,supplier)->{
            if(direction==net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER){var context=supplier.get();context.enqueueWork(()->{if(!ParalysisControl.blocked(context.getSender()))handle.accept(packet,supplier);});context.setPacketHandled(true);}
            else handle.accept(packet,supplier);
        };
        CHANNEL.registerMessage(id,type,encode,decode,guarded,java.util.Optional.of(direction));
    }
    private NetworkChannel() {}
}
