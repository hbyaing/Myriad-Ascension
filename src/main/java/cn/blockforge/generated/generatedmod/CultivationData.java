package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.acupoint.AcupointProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** 玩家修炼数据；所有修改均在服务端执行，并通过网络同步给客户端。 */
public interface CultivationData {
    AcupointProgress acupoints();
    float getCurrentMana();
    void setCurrentMana(float value);
    float getMaxMana();
    void setMaxMana(float value);

    default float wuxingxiantu$getCurrentMana() { return getCurrentMana(); }
    default void wuxingxiantu$setCurrentMana(float value) { setCurrentMana(value); }
    default float wuxingxiantu$getMaxMana() { return getMaxMana(); }
    default void wuxingxiantu$setMaxMana(float value) { setMaxMana(value); }

    /** 兼容旧版灵力接口；灵力与 mana 使用同一份存储。 */
    default float wuxingxiantu$getAura() { return getCurrentMana(); }
    default void wuxingxiantu$setAura(float value) { setCurrentMana(value); }
    int wuxingxiantu$getRealm();
    void wuxingxiantu$setRealm(int value);
    /** 是否在下界完成从大乘到地仙的突破；只有该情况下才称为鬼仙。 */
    boolean wuxingxiantu$isGhostImmortal();
    void wuxingxiantu$setGhostImmortal(boolean value);
    /** 阳神纯度显示值，范围 0 至 100，每次击杀心魔增加 10；存档字段保持旧接口名以兼容旧数据。 */
    int wuxingxiantu$getYinShenPurity();
    void wuxingxiantu$setYinShenPurity(int value);
    long wuxingxiantu$getCultivation();
    void wuxingxiantu$setCultivation(long value);
    /** Apply authoritative saved/network values without treating them as cultivation gains. */
    void wuxingxiantu$restoreCultivation(long value);
    java.util.UUID wuxingxiantu$getHeartForgeContract();
    void wuxingxiantu$setHeartForgeContract(java.util.UUID value);
    long wuxingxiantu$getMerit();
    void wuxingxiantu$setMerit(long value);
    int wuxingxiantu$getCultivationStage();
    void wuxingxiantu$setCultivationStage(int value);
    int wuxingxiantu$getRootMask();
    void wuxingxiantu$setRootMask(int value);
    int wuxingxiantu$getRootQuality(int index);
    void wuxingxiantu$setRootQuality(int index, int value);
    int wuxingxiantu$getDaoXin();
    /** Ordinary changes must not resolve an active invasion. */
    void wuxingxiantu$setDaoXin(int value);
    /** Authoritative state replacement for server synchronization / explicit resolution. */
    void wuxingxiantu$restoreDaoXin(int value);
    default void wuxingxiantu$resolveHeartDemonInvasion() {
        if (wuxingxiantu$getDaoXin() != 4) return;
        int previous = wuxingxiantu$getDaoHeartBeforeInvasion();
        wuxingxiantu$restoreDaoXin(previous == 4 ? 6 : previous);
        wuxingxiantu$setHeartDemonInvasionCause("");
    }
    int wuxingxiantu$getDaoHeartBeforeInvasion();
    void wuxingxiantu$setDaoHeartBeforeInvasion(int value);
    String wuxingxiantu$getHeartDemonInvasionCause();
    void wuxingxiantu$setHeartDemonInvasionCause(String value);
    int wuxingxiantu$getDaoHeartCultivation();
    void wuxingxiantu$setDaoHeartCultivation(int value);
    /**
     * 主线“心魔入侵”（dao_heart_unstable）：渡劫成功后境界提升、修为锁定、
     * 须打坐入心境击败 Boss 才解除。与普通道心数值（daoXin）严格隔离，
     * 清心丹/镇心丹等普通净化效果不得清除本标记。
     */
    default boolean wuxingxiantu$isDaoHeartUnstable() { return wuxingxiantu$getDaoXin() == 4; }
    default void wuxingxiantu$setDaoHeartUnstable(boolean value) {
        if (value) {
            if (wuxingxiantu$getDaoXin() != 4) wuxingxiantu$setDaoHeartBeforeInvasion(wuxingxiantu$getDaoXin());
            wuxingxiantu$setDaoXin(4);
        } else wuxingxiantu$resolveHeartDemonInvasion();
    }
    CompoundTag wuxingxiantu$getMindscapeData();
    net.minecraft.world.item.ItemStack wuxingxiantu$getRobeStack();
    void wuxingxiantu$setRobeStack(net.minecraft.world.item.ItemStack stack);
    net.minecraft.world.item.ItemStack wuxingxiantu$getYujianStack();
    void wuxingxiantu$setYujianStack(net.minecraft.world.item.ItemStack stack);
    boolean wuxingxiantu$isInitialized();
    void wuxingxiantu$setInitialized(boolean value);
    int wuxingxiantu$getCultivationUses();
    void wuxingxiantu$setCultivationUses(int value);
    int wuxingxiantu$getAuraBonus();
    void wuxingxiantu$setAuraBonus(int value);
    boolean wuxingxiantu$hasInsight();
    void wuxingxiantu$setInsight(boolean value);
    int wuxingxiantu$getDaysSinceDeath();
    void wuxingxiantu$setDaysSinceDeath(int value);
    BlockPos wuxingxiantu$getCavePos();
    void wuxingxiantu$setCavePos(BlockPos value);
    int wuxingxiantu$getCaveLevel();
    void wuxingxiantu$setCaveLevel(int value);
    int wuxingxiantu$getTradeCount();
    void wuxingxiantu$setTradeCount(int value);
    int wuxingxiantu$getBiguRemainTime();
    void wuxingxiantu$setBiguRemainTime(int value);
    boolean wuxingxiantu$isMeditating();
    void wuxingxiantu$setMeditating(boolean value);
    /** 经脉状态：0 完整，1 受损，2 寸断。 */
    int wuxingxiantu$getMeridianStatus();
    void wuxingxiantu$setMeridianStatus(int value);
    BlockPos wuxingxiantu$getMeditationPos();
    void wuxingxiantu$setMeditationPos(BlockPos value);
    int wuxingxiantu$getMeditationTier();
    void wuxingxiantu$setMeditationTier(int value);
    int wuxingxiantu$getTribulationCount();
    void wuxingxiantu$setTribulationCount(int value);
    int wuxingxiantu$getTribulationFailCount();
    void wuxingxiantu$setTribulationFailCount(int value);
    boolean wuxingxiantu$hasChaosRoot();
    void wuxingxiantu$setChaosRoot(boolean value);
    /** 创造丹赐予的万法无冷却状态，持久化到玩家修炼数据。 */
    boolean wuxingxiantu$hasCreationBlessing();
    void wuxingxiantu$setCreationBlessing(boolean value);
    /** 拥有木灵根期间被天雷淬体的累计次数，达到 99 次解锁天雷灵根。 */
    int wuxingxiantu$getThunderStrikes();
    void wuxingxiantu$setThunderStrikes(int value);
    /** 天雷灵根是否已解锁（木灵根状态下被雷劈满 99 次）。 */
    boolean wuxingxiantu$hasThunderRoot();
    void wuxingxiantu$setThunderRoot(boolean value);
    /** 当前是否显化为天雷灵根形态；天雷灵根是木灵根的变异上位，两者共用品质。 */
    boolean wuxingxiantu$isThunderRootActive();
    void wuxingxiantu$setThunderRootActive(boolean value);
    /** 异火灵根是否已解锁（拥有火灵根状态下收服过任意异火）。 */
    boolean wuxingxiantu$hasExoticRoot();
    void wuxingxiantu$setExoticRoot(boolean value);
    /** 当前是否显化为异火灵根形态；异火灵根是火灵根的变异上位，两者共用品质。 */
    boolean wuxingxiantu$isExoticRootActive();
    void wuxingxiantu$setExoticRootActive(boolean value);
    /** 木灵领域是否启用；领域仅在拥有对应木灵根时生效。 */
    boolean wuxingxiantu$isWoodDomainEnabled();
    void wuxingxiantu$setWoodDomainEnabled(boolean value);
    /** 火焰领域是否启用；领域仅在拥有对应火灵根时生效。 */
    boolean wuxingxiantu$isFireDomainEnabled();
    void wuxingxiantu$setFireDomainEnabled(boolean value);
    /** 金光领域是否启用；领域仅在拥有对应金灵根时生效。 */
    boolean wuxingxiantu$isGoldDomainEnabled();
    void wuxingxiantu$setGoldDomainEnabled(boolean value);
    /** 潮汐领域是否启用；领域仅在拥有对应水灵根时生效。 */
    boolean wuxingxiantu$isWaterDomainEnabled();
    void wuxingxiantu$setWaterDomainEnabled(boolean value);
    /** 重力领域是否启用；领域仅在拥有对应土灵根时生效。 */
    boolean wuxingxiantu$isEarthDomainEnabled();
    void wuxingxiantu$setEarthDomainEnabled(boolean value);
    long wuxingxiantu$getGoldDomainCooldown();
    void wuxingxiantu$setGoldDomainCooldown(long value);
    long wuxingxiantu$getGuandingCooldown();
    void wuxingxiantu$setGuandingCooldown(long value);

    /** 旧版调用兼容：存档和外部扩展仍可通过旧名称访问领域开关。 */
    @Deprecated default boolean wuxingxiantu$isWoodAuraEnabled() { return wuxingxiantu$isWoodDomainEnabled(); }
    @Deprecated default void wuxingxiantu$setWoodAuraEnabled(boolean value) { wuxingxiantu$setWoodDomainEnabled(value); }
    @Deprecated default boolean wuxingxiantu$isFireAuraEnabled() { return wuxingxiantu$isFireDomainEnabled(); }
    @Deprecated default void wuxingxiantu$setFireAuraEnabled(boolean value) { wuxingxiantu$setFireDomainEnabled(value); }
    long wuxingxiantu$getShieldCooldown();
    void wuxingxiantu$setShieldCooldown(long value);
    long wuxingxiantu$getTeleportCooldown();
    void wuxingxiantu$setTeleportCooldown(long value);
    int wuxingxiantu$getAnimalKills();
    void wuxingxiantu$setAnimalKills(int value);
    long wuxingxiantu$getAnimalKillWindow();
    void wuxingxiantu$setAnimalKillWindow(long value);
    PhysiqueType wuxingxiantu$getPhysique();
    void wuxingxiantu$setPhysique(PhysiqueType value);
    boolean wuxingxiantu$isPhysiqueLocked();
    void wuxingxiantu$setPhysiqueLocked(boolean value);
    String wuxingxiantu$getEquippedHeartMethod();
    void wuxingxiantu$setEquippedHeartMethod(String value);
    boolean wuxingxiantu$hasLearnedHeartMethod(String value);
    boolean wuxingxiantu$learnHeartMethod(String value);
    String[] wuxingxiantu$getLearnedHeartMethods();
    void wuxingxiantu$setLearnedHeartMethods(String[] values);
    boolean wuxingxiantu$hasLearnedArt(String value);
    boolean wuxingxiantu$learnArt(String value);
    String[] wuxingxiantu$getLearnedArts();
    void wuxingxiantu$setLearnedArts(String[] values);
    boolean wuxingxiantu$knowsSwordTechnique(String id);
    boolean wuxingxiantu$learnSwordTechnique(String id);
    String[] wuxingxiantu$getKnownSwordTechniques();
    void wuxingxiantu$setKnownSwordTechniques(String[] values);
    String[] wuxingxiantu$getEquippedArts();
    String[] wuxingxiantu$getPassiveArts();
    boolean wuxingxiantu$equipPassiveArt(int slot, String value);
    void wuxingxiantu$clearPassiveArt(int slot);
    boolean wuxingxiantu$equipArt(int slot, String value);
    void wuxingxiantu$clearEquippedArt(int slot);
    long wuxingxiantu$getArtCooldown(int slot);
    void wuxingxiantu$setArtCooldown(int slot, long value);
    default void wuxingxiantu$clearArtCooldowns() {
        for (int slot = 0; slot < 8; slot++) wuxingxiantu$setArtCooldown(slot, 0L);
    }
    int wuxingxiantu$getArtMastery(String artId);
    void wuxingxiantu$setArtMastery(String artId, int value);
    void wuxingxiantu$addArtMastery(String artId, int value);
    boolean wuxingxiantu$hasLearnedDivineArt(String value);
    boolean wuxingxiantu$learnDivineArt(String value);
    String[] wuxingxiantu$getLearnedDivineArts();
    void wuxingxiantu$setLearnedDivineArts(String[] values);
    String[] wuxingxiantu$getEquippedDivineArts();
    void wuxingxiantu$setEquippedDivineArt(int slot, String value);
    String wuxingxiantu$getEquippedDivineArt(int slot);
    /** 兼容旧版单神通接口，返回第一个装备槽位。 */
    default String wuxingxiantu$getEquippedDivineArt() { return wuxingxiantu$getEquippedDivineArt(0); }
    default void wuxingxiantu$setEquippedDivineArt(String value) { wuxingxiantu$setEquippedDivineArt(0, value); }
    String wuxingxiantu$getActiveDivineArt();
    void wuxingxiantu$setActiveDivineArt(String value);
    long wuxingxiantu$getDivineCooldown(String value);
    void wuxingxiantu$setDivineCooldown(String value, long tick);
    default void wuxingxiantu$clearDivineCooldowns() {
        for (String id : DivineArtCatalog.ids()) wuxingxiantu$setDivineCooldown(id, 0L);
    }
    default void wuxingxiantu$clearTechniqueCooldowns() {
        wuxingxiantu$clearArtCooldowns();
        wuxingxiantu$clearDivineCooldowns();
    }
    BlockPos wuxingxiantu$getDivineStepOrigin();
    void wuxingxiantu$setDivineStepOrigin(BlockPos value);
    BlockPos wuxingxiantu$getDivineSoulOrigin();
    void wuxingxiantu$setDivineSoulOrigin(BlockPos value);
    int wuxingxiantu$getDivineStepIndex();
    void wuxingxiantu$setDivineStepIndex(int value);
    float wuxingxiantu$getDivineStepYaw();
    void wuxingxiantu$setDivineStepYaw(float value);
    String[] wuxingxiantu$getPossessedForms();
    void wuxingxiantu$setPossessedForms(String[] values);
    boolean wuxingxiantu$addPossessedForm(String value);
    String wuxingxiantu$getActivePossessedForm();
    void wuxingxiantu$setActivePossessedForm(String value);
    boolean wuxingxiantu$hasLearnedFire(int tier);
    boolean wuxingxiantu$learnFire(int tier);
    int[] wuxingxiantu$getLearnedFires();
    void wuxingxiantu$setLearnedFires(int[] tiers);
    int wuxingxiantu$getEquippedFire();
    void wuxingxiantu$setEquippedFire(int tier);
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag tag);
}
