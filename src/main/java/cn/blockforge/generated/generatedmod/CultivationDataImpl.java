package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.acupoint.AcupointProgress;
import cn.blockforge.generated.generatedmod.items.CultivationLogic;
import net.minecraft.core.BlockPos;
import cn.blockforge.generated.generatedmod.balance.RealmBalance;
import cn.blockforge.generated.generatedmod.balance.ManaBalance;
import net.minecraft.nbt.CompoundTag;

public final class CultivationDataImpl implements CultivationData {
    private final AcupointProgress acupoints = new AcupointProgress();
    @Override public AcupointProgress acupoints() { return acupoints; }

    // Runtime invalidation only; never persisted or sent over protocol 63.
    private long stableSyncVersion,divineSyncVersion;
    public long stableSyncVersion(){return stableSyncVersion;}
    public long divineSyncVersion(){return divineSyncVersion;}
    private float currentMana = RealmBalance.mana(0);
    private float maxMana = RealmBalance.mana(0);
    private int realm;
    private boolean ghostImmortal;
    private int yinShenPurity;
    private long cultivation;
    /** Online ticks of meditation deviation; gain fractions survive saves instead of losing every one-point gain. */
    private int meditationPenaltyTicks,meditationPenaltyCarry;
    public int meditationPenaltyTicks(){return meditationPenaltyTicks;}
    public void setMeditationPenaltyTicks(int ticks){
        meditationPenaltyTicks=Math.max(0,Math.min(18000,ticks));
        if(meditationPenaltyTicks==0)meditationPenaltyCarry=0;
    }
    public void tickMeditationPenalty(){
        if(meditationPenaltyTicks>0&&--meditationPenaltyTicks==0)meditationPenaltyCarry=0;
    }
    private long merit;
    private int cultivationStage;
    private int rootMask;
    private final int[] rootQualities = new int[5];
    private int daoXin;
    private int daoHeartBeforeInvasion;
    private String heartDemonInvasionCause = "";
    private int daoHeartCultivation;
    /**
     * 主线“心魔入侵”（dao_heart_unstable）：渡劫成功后的境界锁定状态，
     * 与上面普通道心数值完全隔离。清心丹/镇心丹等普通净化只作用于 daoXin，
     * 绝不能清除此标记；心境实例、挑战顺序和外物快照由 mindscapeData 持久化。
     */
    private CompoundTag mindscapeData=new CompoundTag();
    private java.util.UUID heartForgeContract;
    private net.minecraft.world.item.ItemStack robeStack=net.minecraft.world.item.ItemStack.EMPTY;
    private net.minecraft.world.item.ItemStack yujianStack=net.minecraft.world.item.ItemStack.EMPTY;
    private boolean initialized;
    private int cultivationUses;
    private int auraBonus;
    private boolean insight;
    private int daysSinceDeath;
    private BlockPos cavePos;
    private int caveLevel = 1;
    private int tradeCount;
    private int biguRemainTime;
    private boolean meditating;
    private int meridianStatus;
    private BlockPos meditationPos;
    private int meditationTier;
    private int tribulationCount;
    private int tribulationFailCount;
    private boolean chaosRoot;
    private boolean creationBlessing;
    /** 天雷淬体次数与变异灵根状态：解锁后仍保留，形态可随时切换。 */
    private int thunderStrikes;
    private boolean thunderUnlocked;
    private boolean thunderActive;
    private boolean exoticUnlocked;
    private boolean exoticActive;
    /** -1 means off. A single value makes simultaneous domains impossible, including client sync. */
    private int activeDomain = -1;
    private long goldDomainCooldown;
    private long guandingCooldown;
    private long shieldCooldown;
    private long teleportCooldown;
    private int animalKills;
    private long animalKillWindow;
    private PhysiqueType physique = PhysiqueType.TAICHU;
    private boolean physiqueLocked;
    private String equippedHeartMethod = "yuan_hun";
    private final java.util.LinkedHashSet<String> learnedHeartMethods = new java.util.LinkedHashSet<>(java.util.List.of("yuan_hun"));
    private final java.util.LinkedHashSet<String> learnedArts = new java.util.LinkedHashSet<>();
    private final java.util.LinkedHashSet<String> knownSwordTechniques = new java.util.LinkedHashSet<>();
    private final String[] equippedArts = new String[8];
    private final String[] passiveArts = new String[3];
    private final long[] artCooldowns = new long[8];
    private final java.util.Map<String, Integer> artMastery = new java.util.HashMap<>();
    private final java.util.LinkedHashSet<String> learnedDivineArts = new java.util.LinkedHashSet<>();
    private final java.util.Map<String, Long> divineCooldowns = new java.util.HashMap<>();
    private final String[] equippedDivineArts = new String[3];
    private String activeDivineArt = "";
    private BlockPos divineStepOrigin;
    private BlockPos divineSoulOrigin;
    private int divineStepIndex;
    private float divineStepYaw;
    private final java.util.LinkedHashSet<String> possessedForms = new java.util.LinkedHashSet<>();
    private String activePossessedForm = "";
    private final java.util.LinkedHashSet<Integer> learnedFires = new java.util.LinkedHashSet<>();
    private int equippedFire;
    /** 玩家是否已明确做过装备选择：区分“从未选过”与“主动卸下（0）”，让卸下状态也能跨重启保留。 */
    private boolean fireChoiceMade;
    /** Server-authoritative friendly-fire preference for the wrapping enchantment. */
    private boolean exoticFireHurtsNonHostile;
    public boolean exoticFireHurtsNonHostile(){return exoticFireHurtsNonHostile;}
    public void setExoticFireHurtsNonHostile(boolean enabled){
        if(exoticFireHurtsNonHostile!=enabled){exoticFireHurtsNonHostile=enabled;stableSyncVersion++;}
    }

    @Override public float getCurrentMana() { return currentMana; }
    private final cn.blockforge.generated.generatedmod.balance.ManaLossLedger lossLedger=new cn.blockforge.generated.generatedmod.balance.ManaLossLedger();
    public net.minecraft.nbt.ListTag manaLossRows(){return lossLedger.save();}
    public boolean manaLossEnabled(){return lossLedger.enabled();}
    public void setManaLossEnabled(boolean enabled){lossLedger.enabled(enabled);}
    public long manaLossVersion(){return lossLedger.version();}
    public java.util.List<net.minecraft.nbt.CompoundTag> manaLossView(){return lossLedger.view();}
    public void receiveManaLossRows(net.minecraft.nbt.ListTag rows){lossLedger.load(rows);}
    @Override public void setCurrentMana(float value) {
        float next=clampMana(value,getMaxMana());
        lossLedger.debit(currentMana-next);
        currentMana = next;
    }
    @Override public float getMaxMana() { return maxMana; }
    @Override public void setMaxMana(float value) {
        maxMana = ManaBalance.sanitizeMaximum(realm, value);
        float next=clampMana(currentMana,maxMana);
        lossLedger.record("灵力上限降低（境界、体质或心法变化）",currentMana-next);
        currentMana = next;
    }
    @Override public int wuxingxiantu$getRealm() { return realm; }
    @Override public void wuxingxiantu$setRealm(int value) { stableSyncVersion++;
        realm = Math.max(0, Math.min(8, value));
        if (realm < 8) ghostImmortal = false;
        setMaxMana(CultivationLogic.calculateMaxMana(this));
        cultivation = Math.min(cultivation, CultivationLogic.cultivationCap(realm, cultivationStage));
    }
    @Override public boolean wuxingxiantu$isGhostImmortal() { return ghostImmortal; }
    @Override public void wuxingxiantu$setGhostImmortal(boolean value) { stableSyncVersion++; ghostImmortal = value && realm >= 8; }
    @Override public int wuxingxiantu$getYinShenPurity() { return yinShenPurity; }
    @Override public void wuxingxiantu$setYinShenPurity(int value) { stableSyncVersion++; yinShenPurity = Math.max(0, Math.min(100, value)); }
    @Override public long wuxingxiantu$getCultivation() { return cultivation; }
    @Override public void wuxingxiantu$setCultivation(long value) {
        if(daoXin == 4 && value>cultivation)return;
        if(value>cultivation&&meditationPenaltyTicks>0){
            long gain=value-cultivation;
            int halves=(int)(gain%2)+meditationPenaltyCarry;
            value=cultivation+gain/2+halves/2;
            meditationPenaltyCarry=halves%2;
        }
        wuxingxiantu$restoreCultivation(value);
    }
    @Override public void wuxingxiantu$restoreCultivation(long value) {
        cultivation = Math.max(0L, Math.min(CultivationLogic.cultivationCap(realm, cultivationStage), value));
    }
    @Override public long wuxingxiantu$getMerit() { return merit; }
    @Override public void wuxingxiantu$setMerit(long value) { merit = value; }
    @Override public int wuxingxiantu$getCultivationStage() { return cultivationStage; }
    @Override public void wuxingxiantu$setCultivationStage(int value) { stableSyncVersion++;
        cultivationStage = Math.max(0, Math.min(3, value));
        cultivation = Math.min(cultivation, CultivationLogic.cultivationCap(realm, cultivationStage));
    }

    private static float clampMana(float value, float maximum) {
        return ManaBalance.clamp(value, maximum);
    }
    @Override public int wuxingxiantu$getRootMask() { return rootMask; }
    @Override public void wuxingxiantu$setRootMask(int value) { stableSyncVersion++;
        rootMask = value & 31;
        // 失去基础灵根时，依附其上的变异形态立即变回本相（解锁记录保留）。
        if ((rootMask & (1 << 1)) == 0) thunderActive = false;
        if ((rootMask & (1 << 3)) == 0) exoticActive = false;
        // 反向补判：先收服异火、后觉醒火灵根时，异火灵根同样视为已解锁。
        if ((rootMask & (1 << 3)) != 0 && !learnedFires.isEmpty()) exoticUnlocked = true;
    }
    @Override public int wuxingxiantu$getRootQuality(int index) { return index >= 0 && index < 5 ? rootQualities[index] : 0; }
    @Override public void wuxingxiantu$setRootQuality(int index, int value) { stableSyncVersion++; if (index >= 0 && index < 5) rootQualities[index] = Math.max(0, Math.min(4, value)); }
    @Override public int wuxingxiantu$getDaoXin() { return daoXin; }
    @Override public void wuxingxiantu$setDaoXin(int value) {
        if (daoXin == 4 && value != 4) return;
        if (value == 4 && daoXin != 4) daoHeartBeforeInvasion = daoXin;
        wuxingxiantu$restoreDaoXin(value);
    }
    @Override public void wuxingxiantu$restoreDaoXin(int value) { stableSyncVersion++; daoXin = Math.max(0, Math.min(8, value)); }
    @Override public int wuxingxiantu$getDaoHeartBeforeInvasion() { return daoHeartBeforeInvasion; }
    @Override public void wuxingxiantu$setDaoHeartBeforeInvasion(int value) { daoHeartBeforeInvasion = Math.max(0, Math.min(8, value)); }
    @Override public String wuxingxiantu$getHeartDemonInvasionCause() { return heartDemonInvasionCause; }
    @Override public void wuxingxiantu$setHeartDemonInvasionCause(String value) { heartDemonInvasionCause = value == null ? "" : value; }
    @Override public int wuxingxiantu$getDaoHeartCultivation() { return daoHeartCultivation; }
    @Override public void wuxingxiantu$setDaoHeartCultivation(int value) { daoHeartCultivation = Math.max(0, Math.min(10000, value)); }
    @Override public CompoundTag wuxingxiantu$getMindscapeData(){return mindscapeData;}
    @Override public java.util.UUID wuxingxiantu$getHeartForgeContract(){return heartForgeContract;}
    @Override public void wuxingxiantu$setHeartForgeContract(java.util.UUID value){ stableSyncVersion++;heartForgeContract=value;}
    @Override public net.minecraft.world.item.ItemStack wuxingxiantu$getRobeStack(){return robeStack;}
    @Override public net.minecraft.world.item.ItemStack wuxingxiantu$getYujianStack(){return yujianStack;}
    // Ownership is transferred by the server container; keep the original stack and all its capabilities.
    @Override public void wuxingxiantu$setYujianStack(net.minecraft.world.item.ItemStack stack){yujianStack=stack;}
    @Override public void wuxingxiantu$setRobeStack(net.minecraft.world.item.ItemStack stack){
        robeStack=stack.isEmpty()?net.minecraft.world.item.ItemStack.EMPTY:stack.copyWithCount(1);
    }
    @Override public boolean wuxingxiantu$isInitialized() { return initialized; }
    @Override public void wuxingxiantu$setInitialized(boolean value) { initialized = value; }
    @Override public int wuxingxiantu$getCultivationUses() { return cultivationUses; }
    @Override public void wuxingxiantu$setCultivationUses(int value) { cultivationUses = Math.max(0, Math.min(100, value)); }
    @Override public int wuxingxiantu$getAuraBonus() { return auraBonus; }
    @Override public void wuxingxiantu$setAuraBonus(int value) {
        auraBonus = Math.max(-5, Math.min(25, value));
        setMaxMana(CultivationLogic.calculateMaxMana(this));
    }
    @Override public boolean wuxingxiantu$hasInsight() { return insight; }
    @Override public void wuxingxiantu$setInsight(boolean value) { insight = value; }
    @Override public int wuxingxiantu$getDaysSinceDeath() { return daysSinceDeath; }
    @Override public void wuxingxiantu$setDaysSinceDeath(int value) { daysSinceDeath = Math.max(0, value); }
    @Override public BlockPos wuxingxiantu$getCavePos() { return cavePos; }
    @Override public void wuxingxiantu$setCavePos(BlockPos value) { cavePos = value; }
    @Override public int wuxingxiantu$getCaveLevel() { return caveLevel; }
    @Override public void wuxingxiantu$setCaveLevel(int value) { caveLevel = Math.max(1, Math.min(4, value)); }
    @Override public int wuxingxiantu$getTradeCount() { return tradeCount; }
    @Override public void wuxingxiantu$setTradeCount(int value) { tradeCount = Math.max(0, value); }
    @Override public int wuxingxiantu$getBiguRemainTime() { return biguRemainTime; }
    @Override public void wuxingxiantu$setBiguRemainTime(int value) { biguRemainTime = Math.max(0, value); }
    @Override public boolean wuxingxiantu$isMeditating() { return meditating; }
    @Override public void wuxingxiantu$setMeditating(boolean value) { stableSyncVersion++; meditating = value; }
    @Override public int wuxingxiantu$getMeridianStatus() { return meridianStatus; }
    @Override public void wuxingxiantu$setMeridianStatus(int value) { stableSyncVersion++; meridianStatus = Math.max(0, Math.min(2, value)); }
    @Override public BlockPos wuxingxiantu$getMeditationPos() { return meditationPos; }
    @Override public void wuxingxiantu$setMeditationPos(BlockPos value) { meditationPos = value; }
    @Override public int wuxingxiantu$getMeditationTier() { return meditationTier; }
    @Override public void wuxingxiantu$setMeditationTier(int value) { meditationTier = Math.max(0, Math.min(2, value)); }
    @Override public int wuxingxiantu$getTribulationCount() { return tribulationCount; }
    @Override public void wuxingxiantu$setTribulationCount(int value) { tribulationCount = Math.max(0, value); }
    @Override public int wuxingxiantu$getTribulationFailCount() { return tribulationFailCount; }
    @Override public void wuxingxiantu$setTribulationFailCount(int value) { tribulationFailCount = Math.max(0, value); }
    @Override public boolean wuxingxiantu$hasChaosRoot() { return chaosRoot; }
    @Override public void wuxingxiantu$setChaosRoot(boolean value) { stableSyncVersion++; chaosRoot = value; }
    @Override public boolean wuxingxiantu$hasCreationBlessing() { return creationBlessing; }
    @Override public void wuxingxiantu$setCreationBlessing(boolean value) { creationBlessing = value; }
    @Override public int wuxingxiantu$getThunderStrikes() { return thunderStrikes; }
    @Override public void wuxingxiantu$setThunderStrikes(int value) { stableSyncVersion++; thunderStrikes = Math.max(0, value); }
    @Override public boolean wuxingxiantu$hasThunderRoot() { return thunderUnlocked; }
    @Override public void wuxingxiantu$setThunderRoot(boolean value) { stableSyncVersion++; thunderUnlocked = value; }
    @Override public boolean wuxingxiantu$isThunderRootActive() { return thunderActive; }
    @Override public void wuxingxiantu$setThunderRootActive(boolean value) { stableSyncVersion++; thunderActive = value; }
    @Override public boolean wuxingxiantu$hasExoticRoot() { return exoticUnlocked; }
    @Override public void wuxingxiantu$setExoticRoot(boolean value) { stableSyncVersion++; exoticUnlocked = value; }
    @Override public boolean wuxingxiantu$isExoticRootActive() { return exoticActive; }
    @Override public void wuxingxiantu$setExoticRootActive(boolean value) { stableSyncVersion++; exoticActive = value; }
    private void setDomainEnabled(int root, boolean enabled) {
        int next = enabled ? root : activeDomain == root ? -1 : activeDomain;
        if (activeDomain != next) { activeDomain = next; stableSyncVersion++; }
    }
    @Override public boolean wuxingxiantu$isWoodDomainEnabled() { return activeDomain == 1; }
    @Override public void wuxingxiantu$setWoodDomainEnabled(boolean value) { setDomainEnabled(1, value); }
    @Override public boolean wuxingxiantu$isFireDomainEnabled() { return activeDomain == 3; }
    @Override public void wuxingxiantu$setFireDomainEnabled(boolean value) { setDomainEnabled(3, value); }
    @Override public boolean wuxingxiantu$isGoldDomainEnabled() { return activeDomain == 0; }
    @Override public void wuxingxiantu$setGoldDomainEnabled(boolean value) { setDomainEnabled(0, value); }
    @Override public boolean wuxingxiantu$isWaterDomainEnabled() { return activeDomain == 2; }
    @Override public void wuxingxiantu$setWaterDomainEnabled(boolean value) { setDomainEnabled(2, value); }
    @Override public boolean wuxingxiantu$isEarthDomainEnabled() { return activeDomain == 4; }
    @Override public void wuxingxiantu$setEarthDomainEnabled(boolean value) { setDomainEnabled(4, value); }
    @Override public long wuxingxiantu$getGoldDomainCooldown() { return goldDomainCooldown; }
    @Override public void wuxingxiantu$setGoldDomainCooldown(long value) { goldDomainCooldown = Math.max(0L, value); }
    @Override public long wuxingxiantu$getGuandingCooldown() { return guandingCooldown; }
    @Override public void wuxingxiantu$setGuandingCooldown(long value) { guandingCooldown = Math.max(0L, value); }
    @Override public long wuxingxiantu$getShieldCooldown() { return shieldCooldown; }
    @Override public void wuxingxiantu$setShieldCooldown(long value) { shieldCooldown = Math.max(0L, value); }
    @Override public long wuxingxiantu$getTeleportCooldown() { return teleportCooldown; }
    @Override public void wuxingxiantu$setTeleportCooldown(long value) { teleportCooldown = Math.max(0L, value); }
    @Override public int wuxingxiantu$getAnimalKills() { return animalKills; }
    @Override public void wuxingxiantu$setAnimalKills(int value) { animalKills = Math.max(0, value); }
    @Override public long wuxingxiantu$getAnimalKillWindow() { return animalKillWindow; }
    @Override public void wuxingxiantu$setAnimalKillWindow(long value) { animalKillWindow = Math.max(0L, value); }
    @Override public PhysiqueType wuxingxiantu$getPhysique() { return physique; }
    @Override public void wuxingxiantu$setPhysique(PhysiqueType value) { stableSyncVersion++;
        physique = value == null ? PhysiqueType.TAICHU : value;
        setMaxMana(CultivationLogic.calculateMaxMana(this));
    }
    /** 兼容旧存档字段；体质现在始终不受境界锁定。 */
    @Override public boolean wuxingxiantu$isPhysiqueLocked() { return false; }
    @Override public void wuxingxiantu$setPhysiqueLocked(boolean value) { physiqueLocked = false; }
    @Override public String wuxingxiantu$getEquippedHeartMethod() { return equippedHeartMethod; }
    @Override public void wuxingxiantu$setEquippedHeartMethod(String value) { stableSyncVersion++;
        equippedHeartMethod = HeartMethodCatalog.isValid(value) ? value : "yuan_hun";
        setMaxMana(CultivationLogic.calculateMaxMana(this));
    }
    @Override public boolean wuxingxiantu$hasLearnedHeartMethod(String value) {
        return HeartMethodCatalog.isValid(value) && learnedHeartMethods.contains(value);
    }
    @Override public boolean wuxingxiantu$learnHeartMethod(String value) { stableSyncVersion++;
        return HeartMethodCatalog.isValid(value) && learnedHeartMethods.size() < HeartMethodCatalog.METHOD_IDS.size()
                && learnedHeartMethods.add(value);
    }
    @Override public String[] wuxingxiantu$getLearnedHeartMethods() {
        return learnedHeartMethods.toArray(String[]::new);
    }
    @Override public void wuxingxiantu$setLearnedHeartMethods(String[] values) { stableSyncVersion++;
        learnedHeartMethods.clear();
        if (values != null) for (String value : values) {
            if (HeartMethodCatalog.isValid(value)) learnedHeartMethods.add(value);
            if (learnedHeartMethods.size() >= HeartMethodCatalog.METHOD_IDS.size()) break;
        }
        if (learnedHeartMethods.isEmpty()) learnedHeartMethods.add("yuan_hun");
    }
    @Override public boolean wuxingxiantu$hasLearnedArt(String value) { return learnedArts.contains(value); }
    @Override public boolean wuxingxiantu$learnArt(String value) { stableSyncVersion++;
        if (cn.blockforge.generated.generatedmod.yujian.SwordTechniqueRegistry.isTechnique(value))
            return wuxingxiantu$learnSwordTechnique(value);
        return ArtCatalog.isValid(value) && learnedArts.size() < ArtCatalog.ENTRIES.size() && learnedArts.add(value);
    }
    @Override public boolean wuxingxiantu$knowsSwordTechnique(String id) { return knownSwordTechniques.contains(id); }
    @Override public boolean wuxingxiantu$learnSwordTechnique(String id) { stableSyncVersion++;
        return cn.blockforge.generated.generatedmod.yujian.SwordTechniqueRegistry.isTechnique(id) && knownSwordTechniques.add(id);
    }
    @Override public String[] wuxingxiantu$getKnownSwordTechniques() { return knownSwordTechniques.toArray(String[]::new); }
    @Override public void wuxingxiantu$setKnownSwordTechniques(String[] values) { stableSyncVersion++;
        knownSwordTechniques.clear();
        if (values != null) for (String value : values) wuxingxiantu$learnSwordTechnique(value);
    }
    @Override public String[] wuxingxiantu$getLearnedArts() { return learnedArts.toArray(String[]::new); }
    @Override public void wuxingxiantu$setLearnedArts(String[] values) { stableSyncVersion++;
        learnedArts.clear();
        if (values != null) for (String value : values) wuxingxiantu$learnArt(value);
        for (int i = 0; i < equippedArts.length; i++) {
            if (!ArtCatalog.isValid(equippedArts[i]) || !learnedArts.contains(equippedArts[i])) {
                equippedArts[i] = null;
                artCooldowns[i] = 0;
            }
        }
        for (int i = 0; i < passiveArts.length; i++) {
            if (!ArtCatalog.isPassiveCompatible(passiveArts[i]) || !learnedArts.contains(passiveArts[i])) passiveArts[i] = null;
        }
    }
    @Override public String[] wuxingxiantu$getEquippedArts() { return equippedArts.clone(); }
    @Override public String[] wuxingxiantu$getPassiveArts() { return passiveArts.clone(); }
    @Override public boolean wuxingxiantu$equipPassiveArt(int slot, String value) { stableSyncVersion++;
        if (slot < 0 || slot >= passiveArts.length || !ArtCatalog.isPassiveCompatible(value) || !learnedArts.contains(value)) return false;
        passiveArts[slot] = value; return true;
    }
    @Override public void wuxingxiantu$clearPassiveArt(int slot) { stableSyncVersion++;
        if (slot >= 0 && slot < passiveArts.length) passiveArts[slot] = null;
    }
    @Override public boolean wuxingxiantu$equipArt(int slot, String value) { stableSyncVersion++;
        if (slot < 0 || slot >= equippedArts.length || value == null || !learnedArts.contains(value)) return false;
        equippedArts[slot] = value;
        return true;
    }
    @Override public void wuxingxiantu$clearEquippedArt(int slot) { stableSyncVersion++;
        if (slot >= 0 && slot < equippedArts.length) equippedArts[slot] = null;
    }
    @Override public long wuxingxiantu$getArtCooldown(int slot) { return slot >= 0 && slot < 8 ? artCooldowns[slot] : 0L; }
    @Override public void wuxingxiantu$setArtCooldown(int slot, long value) { if (slot >= 0 && slot < 8) artCooldowns[slot] = Math.max(0L, value); }
    @Override public int wuxingxiantu$getArtMastery(String artId) { return artMastery.getOrDefault(artId, 0); }
    @Override public void wuxingxiantu$setArtMastery(String artId, int value) { stableSyncVersion++;
        if (ArtCatalog.isValid(artId)) artMastery.put(artId, Math.max(0, Math.min(100, value)));
    }
    @Override public void wuxingxiantu$addArtMastery(String artId, int value) {
        if (ArtCatalog.isValid(artId) && value > 0) wuxingxiantu$setArtMastery(artId, wuxingxiantu$getArtMastery(artId) + value);
    }
    @Override public boolean wuxingxiantu$hasLearnedDivineArt(String value) {
        return DivineArtCatalog.isValid(value) && learnedDivineArts.contains(value);
    }
    @Override public boolean wuxingxiantu$learnDivineArt(String value) { divineSyncVersion++;
        return DivineArtCatalog.isValid(value) && learnedDivineArts.add(value);
    }
    @Override public String[] wuxingxiantu$getLearnedDivineArts() { return learnedDivineArts.toArray(String[]::new); }
    @Override public void wuxingxiantu$setLearnedDivineArts(String[] values) { divineSyncVersion++;
        learnedDivineArts.clear();
        if (values != null) for (String value : values) if (DivineArtCatalog.isValid(value)) learnedDivineArts.add(value);
        for (int i = 0; i < equippedDivineArts.length; i++) {
            if (!DivineArtCatalog.isValid(equippedDivineArts[i]) || !learnedDivineArts.contains(equippedDivineArts[i])) equippedDivineArts[i] = null;
        }
        if (!DivineArtCatalog.isValid(activeDivineArt) || !learnedDivineArts.contains(activeDivineArt)) activeDivineArt = "";
    }
    @Override public String[] wuxingxiantu$getEquippedDivineArts() { return equippedDivineArts.clone(); }
    @Override public String wuxingxiantu$getEquippedDivineArt(int slot) {
        return slot >= 0 && slot < equippedDivineArts.length && equippedDivineArts[slot] != null ? equippedDivineArts[slot] : "";
    }
    @Override public void wuxingxiantu$setEquippedDivineArt(int slot, String value) { divineSyncVersion++;
        if (slot < 0 || slot >= equippedDivineArts.length) return;
        equippedDivineArts[slot] = DivineArtCatalog.isValid(value) && learnedDivineArts.contains(value) ? value : null;
    }
    @Override public String wuxingxiantu$getActiveDivineArt() { return activeDivineArt; }
    @Override public void wuxingxiantu$setActiveDivineArt(String value) { divineSyncVersion++; activeDivineArt = DivineArtCatalog.isValid(value) ? value : ""; }
    @Override public long wuxingxiantu$getDivineCooldown(String value) { return divineCooldowns.getOrDefault(value, 0L); }
    @Override public void wuxingxiantu$setDivineCooldown(String value, long tick) {
        if (DivineArtCatalog.isValid(value)) divineCooldowns.put(value, Math.max(0L, tick));
    }
    @Override public BlockPos wuxingxiantu$getDivineStepOrigin() { return divineStepOrigin; }
    @Override public void wuxingxiantu$setDivineStepOrigin(BlockPos value) { divineStepOrigin = value; }
    @Override public BlockPos wuxingxiantu$getDivineSoulOrigin() { return divineSoulOrigin; }
    @Override public void wuxingxiantu$setDivineSoulOrigin(BlockPos value) { divineSoulOrigin = value; }
    @Override public int wuxingxiantu$getDivineStepIndex() { return divineStepIndex; }
    @Override public void wuxingxiantu$setDivineStepIndex(int value) { divineSyncVersion++; divineStepIndex = Math.max(0, Math.min(7, value)); }
    @Override public float wuxingxiantu$getDivineStepYaw() { return divineStepYaw; }
    @Override public void wuxingxiantu$setDivineStepYaw(float value) {
        divineStepYaw = Float.isFinite(value) ? value : 0.0F;
    }
    @Override public String[] wuxingxiantu$getPossessedForms() { return possessedForms.toArray(String[]::new); }
    @Override public void wuxingxiantu$setPossessedForms(String[] values) { divineSyncVersion++;
        possessedForms.clear();
        if (values != null) for (String value : values) if (isValidPossessedForm(value)) possessedForms.add(value);
        if (!possessedForms.contains(activePossessedForm)) activePossessedForm = "";
    }
    @Override public boolean wuxingxiantu$addPossessedForm(String value) { divineSyncVersion++;
        return possessedForms.size() < 256 && isValidPossessedForm(value) && possessedForms.add(value);
    }
    @Override public String wuxingxiantu$getActivePossessedForm() { return activePossessedForm; }
    @Override public void wuxingxiantu$setActivePossessedForm(String value) { divineSyncVersion++;
        activePossessedForm = isValidPossessedForm(value) && possessedForms.contains(value) ? value : "";
    }
    private static boolean isValidPossessedForm(String value) {
        return value != null && value.length() <= 128 && TransformAbilityRegistry.isValidFormId(value);
    }
    @Override public boolean wuxingxiantu$hasLearnedFire(int tier) {
        return tier >= 1 && tier <= 9 && learnedFires.contains(tier);
    }
    @Override public boolean wuxingxiantu$learnFire(int tier) { stableSyncVersion++;divineSyncVersion++;
        if (tier < 1 || tier > 9) return false;
        boolean added = learnedFires.add(tier);
        // 异火灵根解锁条件：拥有火灵根的状态下收服过任意异火。
        if (added && (rootMask & (1 << 3)) != 0) exoticUnlocked = true;
        // 只在玩家从未做过任何装备/卸下决定时自动装备首朵习得的异火；
        // 主动卸下（0）是已记录的装备状态，学新火不得把它顶掉。
        if (added && equippedFire == 0 && !fireChoiceMade) {
            equippedFire = tier;
            fireChoiceMade = true;
        }
        return added;
    }
    @Override public int[] wuxingxiantu$getLearnedFires() {
        return learnedFires.stream().mapToInt(Integer::intValue).toArray();
    }
    @Override public void wuxingxiantu$setLearnedFires(int[] tiers) { divineSyncVersion++;
        learnedFires.clear();
        if (tiers != null) for (int tier : tiers) if (tier >= 1 && tier <= 9) learnedFires.add(tier);
        // equippedFire 为 0 表示“未装备”本身就是一种合法状态，不再默认顶成第一朵异火。
        if (equippedFire != 0 && !learnedFires.contains(equippedFire))
            equippedFire = learnedFires.stream().findFirst().orElse(0);
    }
    @Override public int wuxingxiantu$getEquippedFire() { return equippedFire; }
    @Override public void wuxingxiantu$setEquippedFire(int tier) { divineSyncVersion++;
        if (tier == 0 || wuxingxiantu$hasLearnedFire(tier)) {
            equippedFire = tier;
            fireChoiceMade = true;
        }
    }

    private void addLegacyDivineArt(CompoundTag tag, String key) {
        if (tag.contains(key, net.minecraft.nbt.Tag.TAG_STRING)) {
            String value = tag.getString(key);
            if (DivineArtCatalog.isValid(value)) learnedDivineArts.add(value);
            return;
        }
        net.minecraft.nbt.Tag raw = tag.get(key);
        if (raw instanceof net.minecraft.nbt.ListTag values) {
            for (net.minecraft.nbt.Tag valueTag : values) {
                String value = valueTag instanceof CompoundTag compound ? compound.getString("Id") : valueTag.getAsString();
                if (DivineArtCatalog.isValid(value)) learnedDivineArts.add(value);
            }
        }
    }

    @Override public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("CurrentMana", currentMana);
        tag.put("ManaLossLedger",lossLedger.save());
        tag.putBoolean("ManaLossTracking",lossLedger.enabled());
        tag.putBoolean("ExoticFireHurtsNonHostile",exoticFireHurtsNonHostile);
        tag.putFloat("MaxMana", maxMana);
        tag.putFloat("Aura", currentMana); // 兼容旧版本存档。
        tag.putInt("Realm", realm);
        tag.putBoolean("GhostImmortal", ghostImmortal);
        tag.putInt("YinShenPurity", yinShenPurity);
        tag.putLong("Cultivation", cultivation);
        tag.putInt("MeditationPenaltyTicks",meditationPenaltyTicks);
        tag.putInt("MeditationPenaltyCarry",meditationPenaltyCarry);
        tag.putLong("Merit", merit);
        tag.putByte("CultivationStage", (byte) cultivationStage);
        tag.putInt("RootMask", rootMask);
        for (int i = 0; i < 5; i++) tag.putInt("Root" + i, rootQualities[i]);
        tag.putInt("DaoXin", daoXin);
        tag.putInt("DaoHeartBeforeInvasion", daoHeartBeforeInvasion);
        tag.putString("HeartDemonInvasionCause", heartDemonInvasionCause);
        tag.putInt("DaoHeartCultivation", daoHeartCultivation);
        tag.put("mindscape",mindscapeData.copy());
        tag.put("Baiqiao", acupoints.save());
        if(heartForgeContract!=null)tag.putUUID("HeartForgeContract",heartForgeContract);
        tag.put("robe_stack",robeStack.save(new CompoundTag()));
        tag.put("yujian_stack",yujianStack.save(new CompoundTag()));
        tag.putBoolean("Initialized", initialized); tag.putInt("CultivationUses", cultivationUses);
        tag.putInt("AuraBonus", auraBonus); tag.putBoolean("Insight", insight);
        tag.putInt("DaysSinceDeath", daysSinceDeath);
        if (cavePos != null) tag.putLong("CavePos", cavePos.asLong());
        tag.putBoolean("HasCave", cavePos != null); tag.putInt("CaveLevel", caveLevel);
        tag.putInt("TradeCount", tradeCount); tag.putInt("BiguRemainTime", biguRemainTime);
        tag.putBoolean("Meditating", meditating);
        tag.putByte("MeridianStatus", (byte) meridianStatus);
        if (meditationPos != null) tag.putLong("MeditationPos", meditationPos.asLong());
        tag.putInt("MeditationTier", meditationTier);
        tag.putInt("TribulationCount", tribulationCount);
        tag.putInt("TribulationFailCount", tribulationFailCount); tag.putBoolean("ChaosRoot", chaosRoot);
        tag.putBoolean("CreationBlessing", creationBlessing);
        tag.putInt("ThunderStrikes", thunderStrikes);
        tag.putBoolean("ThunderUnlocked", thunderUnlocked);
        tag.putBoolean("ThunderActive", thunderActive);
        tag.putBoolean("ExoticUnlocked", exoticUnlocked);
        tag.putBoolean("ExoticActive", exoticActive);
        tag.putBoolean("WoodDomainEnabled", wuxingxiantu$isWoodDomainEnabled());
        tag.putBoolean("FireDomainEnabled", wuxingxiantu$isFireDomainEnabled());
        tag.putBoolean("GoldDomainEnabled", wuxingxiantu$isGoldDomainEnabled());
        tag.putBoolean("WaterDomainEnabled", wuxingxiantu$isWaterDomainEnabled());
        tag.putBoolean("EarthDomainEnabled", wuxingxiantu$isEarthDomainEnabled());
        tag.putLong("GoldDomainCooldown", goldDomainCooldown);
        tag.putLong("GuandingCooldown", guandingCooldown);
        tag.putLong("ShieldCooldown", shieldCooldown); tag.putLong("TeleportCooldown", teleportCooldown);
        tag.putInt("AnimalKills", animalKills); tag.putLong("AnimalKillWindow", animalKillWindow);
        tag.putString("Physique", physique.id());
        tag.putBoolean("PhysiqueLocked", false); // 兼容旧键，但不再按境界锁定体质。
        tag.putString("HeartMethod", equippedHeartMethod);
        net.minecraft.nbt.ListTag heartMethods = new net.minecraft.nbt.ListTag();
        for (String method : learnedHeartMethods) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", method);
            heartMethods.add(entry);
        }
        tag.put("LearnedHeartMethods", heartMethods);
        net.minecraft.nbt.ListTag arts = new net.minecraft.nbt.ListTag();
        for (String art : learnedArts) { CompoundTag entry = new CompoundTag(); entry.putString("Id", art); arts.add(entry); }
        tag.put("LearnedArts", arts);
        net.minecraft.nbt.ListTag techniques = new net.minecraft.nbt.ListTag();
        for (String technique : knownSwordTechniques) techniques.add(net.minecraft.nbt.StringTag.valueOf(technique));
        tag.put("known_sword_techniques", techniques);
        for (int i = 0; i < 8; i++) {
            if (equippedArts[i] != null) tag.putString("EquippedArt" + i, equippedArts[i]);
            tag.putLong("ArtCooldown" + i, artCooldowns[i]);
        }
        for (int i = 0; i < passiveArts.length; i++) if (passiveArts[i] != null) tag.putString("PassiveArt" + i, passiveArts[i]);
        CompoundTag mastery = new CompoundTag();
        for (java.util.Map.Entry<String, Integer> entry : artMastery.entrySet()) mastery.putInt(entry.getKey(), entry.getValue());
        tag.put("ArtMastery", mastery);
        net.minecraft.nbt.ListTag divineArts = new net.minecraft.nbt.ListTag();
        for (String art : learnedDivineArts) { CompoundTag entry = new CompoundTag(); entry.putString("Id", art); divineArts.add(entry); }
        tag.put("LearnedDivineArts", divineArts);
        for (int i = 0; i < equippedDivineArts.length; i++) {
            if (equippedDivineArts[i] != null) tag.putString("EquippedDivineArt" + i, equippedDivineArts[i]);
        }
        // 保留旧字段，旧版本读取时仍能看到第一个槽位。
        tag.putString("EquippedDivineArt", wuxingxiantu$getEquippedDivineArt(0));
        tag.putString("ActiveDivineArt", activeDivineArt);
        CompoundTag divineCooldown = new CompoundTag();
        for (java.util.Map.Entry<String, Long> entry : divineCooldowns.entrySet()) divineCooldown.putLong(entry.getKey(), entry.getValue());
        tag.put("DivineCooldowns", divineCooldown);
        if (divineStepOrigin != null) tag.putLong("DivineStepOrigin", divineStepOrigin.asLong());
        tag.putBoolean("HasDivineStepOrigin", divineStepOrigin != null);
        if (divineSoulOrigin != null) tag.putLong("DivineSoulOrigin", divineSoulOrigin.asLong());
        tag.putBoolean("HasDivineSoulOrigin", divineSoulOrigin != null);
        tag.putInt("DivineStepIndex", divineStepIndex);
        tag.putFloat("DivineStepYaw", divineStepYaw);
        net.minecraft.nbt.ListTag possessed = new net.minecraft.nbt.ListTag();
        for (String form : possessedForms) possessed.add(net.minecraft.nbt.StringTag.valueOf(form));
        tag.put("PossessedForms", possessed);
        tag.putString("ActivePossessedForm", activePossessedForm);
        int[] fires = wuxingxiantu$getLearnedFires();
        tag.putIntArray("LearnedFires", fires);
        tag.putInt("EquippedFire", equippedFire);
        tag.putBoolean("FireChoiceMade", fireChoiceMade);
        return tag;
    }

    @Override public void deserializeNBT(CompoundTag savedTag) {
        lossLedger.load(savedTag.getList("ManaLossLedger",10));
        lossLedger.enabled(!savedTag.contains("ManaLossTracking")||savedTag.getBoolean("ManaLossTracking"));
        exoticFireHurtsNonHostile=savedTag.getBoolean("ExoticFireHurtsNonHostile");
        stableSyncVersion++;divineSyncVersion++;
        final CompoundTag tag=LegacyDivineMigration.migrate(savedTag);
        realm = Math.max(0, Math.min(8, tag.getInt("Realm")));
        ghostImmortal = realm >= 8 && tag.getBoolean("GhostImmortal");
        yinShenPurity = Math.max(0, Math.min(100, tag.getInt("YinShenPurity")));
        cultivationStage = Math.max(0, Math.min(3, tag.getByte("CultivationStage")));
        cultivation = Math.max(0L, Math.min(CultivationLogic.cultivationCap(realm, cultivationStage), tag.getLong("Cultivation")));
        setMeditationPenaltyTicks(tag.getInt("MeditationPenaltyTicks"));
        meditationPenaltyCarry=meditationPenaltyTicks>0?Math.max(0,Math.min(1,tag.getInt("MeditationPenaltyCarry"))):0;
        merit = tag.getLong("Merit");
        auraBonus = Math.max(-5, Math.min(25, tag.getInt("AuraBonus")));
        float savedCurrentMana = tag.contains("CurrentMana") ? tag.getFloat("CurrentMana") : tag.getFloat("Aura");
        currentMana = savedCurrentMana;
        rootMask = tag.getInt("RootMask") & 31;
        for (int i = 0; i < 5; i++) rootQualities[i] = Math.max(0, Math.min(4, tag.getInt("Root" + i)));
        daoXin = Math.max(0, Math.min(8, tag.getInt("DaoXin")));
        boolean legacyUnstable = tag.contains("dao_heart_unstable") ? tag.getBoolean("dao_heart_unstable") : tag.getBoolean("DaoHeartUnstable");
        daoHeartBeforeInvasion = Math.max(0, Math.min(8, tag.getInt("DaoHeartBeforeInvasion")));
        heartDemonInvasionCause = tag.getString("HeartDemonInvasionCause");
        daoHeartCultivation = Math.max(0, Math.min(10000, tag.getInt("DaoHeartCultivation")));
        if (legacyUnstable && daoXin != 4) { daoHeartBeforeInvasion = daoXin; daoXin = 4; }
        mindscapeData=tag.getCompound("mindscape").copy();
        acupoints.load(tag.getCompound("Baiqiao"));
        heartForgeContract=tag.hasUUID("HeartForgeContract")?tag.getUUID("HeartForgeContract"):null;
        robeStack=net.minecraft.world.item.ItemStack.of(tag.getCompound("robe_stack"));
        yujianStack=net.minecraft.world.item.ItemStack.of(tag.getCompound("yujian_stack"));
        if(!robeStack.isEmpty())robeStack.setCount(1);
        initialized = tag.getBoolean("Initialized"); cultivationUses = Math.max(0, tag.getInt("CultivationUses"));
        insight = tag.getBoolean("Insight"); daysSinceDeath = Math.max(0, tag.getInt("DaysSinceDeath"));
        cavePos = tag.getBoolean("HasCave") ? BlockPos.of(tag.getLong("CavePos")) : null;
        caveLevel = Math.max(1, Math.min(4, tag.getInt("CaveLevel"))); tradeCount = Math.max(0, tag.getInt("TradeCount"));
        biguRemainTime = Math.max(0, tag.getInt("BiguRemainTime")); meditating = tag.getBoolean("Meditating");
        meridianStatus = Math.max(0, Math.min(2, tag.getByte("MeridianStatus")));
        meditationPos = tag.contains("MeditationPos") ? BlockPos.of(tag.getLong("MeditationPos")) : null;
        meditationTier = Math.max(0, Math.min(2, tag.getInt("MeditationTier")));
        tribulationCount = Math.max(0, tag.getInt("TribulationCount")); tribulationFailCount = Math.max(0, tag.getInt("TribulationFailCount"));
        chaosRoot = tag.getBoolean("ChaosRoot");
         creationBlessing = tag.getBoolean("CreationBlessing");
        boolean woodDomainEnabled = tag.contains("WoodDomainEnabled")
                ? tag.getBoolean("WoodDomainEnabled")
                : !tag.contains("WoodAuraEnabled") || tag.getBoolean("WoodAuraEnabled");
        boolean fireDomainEnabled = tag.contains("FireDomainEnabled")
                ? tag.getBoolean("FireDomainEnabled")
                : !tag.contains("FireAuraEnabled") || tag.getBoolean("FireAuraEnabled");
        boolean goldDomainEnabled = !tag.contains("GoldDomainEnabled") || tag.getBoolean("GoldDomainEnabled");
        boolean waterDomainEnabled = !tag.contains("WaterDomainEnabled") || tag.getBoolean("WaterDomainEnabled");
        boolean earthDomainEnabled = !tag.contains("EarthDomainEnabled") || tag.getBoolean("EarthDomainEnabled");
        // Legacy all-on saves retain the first unlocked domain in 金木水火土 order.
        boolean[] oldDomains = {goldDomainEnabled, woodDomainEnabled, waterDomainEnabled, fireDomainEnabled, earthDomainEnabled};
        activeDomain = -1;
        for (int root = 0; root < oldDomains.length; root++) {
            if (oldDomains[root] && (rootMask & (1 << root)) != 0 && rootQualities[root] >= 2) {
                activeDomain = root;
                break;
            }
        }
        goldDomainCooldown = Math.max(0L, tag.getLong("GoldDomainCooldown"));
        guandingCooldown = Math.max(0L, tag.getLong("GuandingCooldown"));
        shieldCooldown = Math.max(0L, tag.getLong("ShieldCooldown"));
        teleportCooldown = Math.max(0L, tag.getLong("TeleportCooldown"));
        animalKills = Math.max(0, tag.getInt("AnimalKills")); animalKillWindow = Math.max(0L, tag.getLong("AnimalKillWindow"));
        physique = PhysiqueType.fromId(tag.getString("Physique")); physiqueLocked = tag.getBoolean("PhysiqueLocked");
        wuxingxiantu$setEquippedHeartMethod(tag.contains("HeartMethod") ? tag.getString("HeartMethod") : "yuan_hun");
        learnedHeartMethods.clear();
        net.minecraft.nbt.ListTag heartMethods = tag.getList("LearnedHeartMethods", 10);
        for (int i = 0; i < heartMethods.size() && learnedHeartMethods.size() < HeartMethodCatalog.METHOD_IDS.size(); i++) {
            String method = heartMethods.getCompound(i).getString("Id");
            if (HeartMethodCatalog.isValid(method)) learnedHeartMethods.add(method);
        }
        // 兼容旧存档：旧版本只保存当前心法，没有单独的已掌握列表。
        if (learnedHeartMethods.isEmpty()) learnedHeartMethods.add(equippedHeartMethod);
        learnedArts.clear();
        knownSwordTechniques.clear();
        for (var technique : tag.getList("known_sword_techniques", 8)) wuxingxiantu$learnSwordTechnique(technique.getAsString());
        net.minecraft.nbt.ListTag arts = tag.getList("LearnedArts", 10);
        for (int i = 0; i < arts.size() && learnedArts.size() < ArtCatalog.ENTRIES.size(); i++) {
            String id = arts.getCompound(i).getString("Id");
            wuxingxiantu$learnArt(id);
        }
        for (int i = 0; i < 8; i++) {
            String id = tag.getString("EquippedArt" + i);
            equippedArts[i] = ArtCatalog.isValid(id) && learnedArts.contains(id) ? id : null;
            artCooldowns[i] = equippedArts[i] == null ? 0 : Math.max(0L, tag.getLong("ArtCooldown" + i));
        }
        for (int i = 0; i < passiveArts.length; i++) {
            String id = tag.contains("PassiveArt" + i) ? tag.getString("PassiveArt" + i) : null;
            passiveArts[i] = ArtCatalog.isPassiveCompatible(id) && learnedArts.contains(id) ? id : null;
        }
        artMastery.clear();
        CompoundTag mastery = tag.getCompound("ArtMastery");
        for (String key : mastery.getAllKeys()) if (ArtCatalog.isValid(key)) wuxingxiantu$setArtMastery(key, mastery.getInt(key));
        learnedDivineArts.clear();
        net.minecraft.nbt.ListTag divineArts = tag.getList("LearnedDivineArts", 10);
        for (net.minecraft.nbt.Tag raw : divineArts) {
            String value = raw instanceof CompoundTag compound ? compound.getString("Id")
                    : raw.getAsString();
            if (DivineArtCatalog.isValid(value)) learnedDivineArts.add(value);
        }
        // 兼容旧存档：早期版本可能把已学神通保存为单个字符串或字符串列表。
        addLegacyDivineArt(tag, "LearnedDivineArt");
        addLegacyDivineArt(tag, "DivineArt");
        addLegacyDivineArt(tag, "DivineArts");
        addLegacyDivineArt(tag, "learnedDivineArts");
        if (tag.contains("GangdouUnlocked") && tag.getBoolean("GangdouUnlocked")) learnedDivineArts.add("ta_gang_bu_dou");
        if (tag.contains("TaGangBuDouUnlocked") && tag.getBoolean("TaGangBuDouUnlocked")) learnedDivineArts.add("ta_gang_bu_dou");
        for (int i = 0; i < equippedDivineArts.length; i++) {
            String key = "EquippedDivineArt" + i;
            equippedDivineArts[i] = tag.contains(key) ? tag.getString(key) : null;
        }
        // 旧存档只有 EquippedDivineArt，迁移到第一个槽位；同时补回旧版本未单独保存的已学列表。
        if (equippedDivineArts[0] == null && tag.contains("EquippedDivineArt")) {
            equippedDivineArts[0] = tag.getString("EquippedDivineArt");
        }
        for (String equipped : equippedDivineArts) {
            if (DivineArtCatalog.isValid(equipped)) learnedDivineArts.add(equipped);
        }
        boolean hasSavedDivineSlot = tag.contains("EquippedDivineArt")
                || java.util.stream.IntStream.range(0, equippedDivineArts.length)
                .anyMatch(i -> tag.contains("EquippedDivineArt" + i));
        for (int i = 0; i < equippedDivineArts.length; i++) {
            if (!learnedDivineArts.contains(equippedDivineArts[i])) equippedDivineArts[i] = null;
        }
        // 旧存档可能只保存了已学列表，没有神通槽位；迁移第一项到首槽，保证轮盘可触发。
        if (!hasSavedDivineSlot && !learnedDivineArts.isEmpty()) equippedDivineArts[0] = learnedDivineArts.iterator().next();
        activeDivineArt = tag.contains("ActiveDivineArt") ? tag.getString("ActiveDivineArt") : "";
        if (!learnedDivineArts.contains(activeDivineArt)) activeDivineArt = "";
        divineCooldowns.clear();
        CompoundTag cooldowns = tag.getCompound("DivineCooldowns");
        for (String key : cooldowns.getAllKeys()) if (DivineArtCatalog.isValid(key)) divineCooldowns.put(key, Math.max(0L, cooldowns.getLong(key)));
        boolean hasStepOrigin = tag.getBoolean("HasDivineStepOrigin") || tag.contains("DivineStepOrigin");
        divineStepOrigin = hasStepOrigin ? BlockPos.of(tag.getLong("DivineStepOrigin")) : null;
        boolean hasSoulOrigin = tag.getBoolean("HasDivineSoulOrigin") || tag.contains("DivineSoulOrigin");
        divineSoulOrigin = hasSoulOrigin ? BlockPos.of(tag.getLong("DivineSoulOrigin")) : null;
        divineStepIndex = Math.max(0, Math.min(7, tag.getInt("DivineStepIndex")));
        if (tag.contains("StepIndex")) divineStepIndex = Math.max(0, Math.min(7, tag.getInt("StepIndex")));
        divineStepYaw = tag.contains("DivineStepYaw") ? tag.getFloat("DivineStepYaw") : 0.0F;
        possessedForms.clear();
        net.minecraft.nbt.ListTag possessed = tag.getList("PossessedForms", net.minecraft.nbt.Tag.TAG_STRING);
        for (net.minecraft.nbt.Tag raw : possessed) {
            String value = raw.getAsString();
            if (isValidPossessedForm(value)) possessedForms.add(value);
        }
        activePossessedForm = tag.contains("ActivePossessedForm") ? tag.getString("ActivePossessedForm") : "";
        if (!possessedForms.contains(activePossessedForm)) activePossessedForm = "";
        learnedFires.clear();
        for (int tier : tag.getIntArray("LearnedFires")) if (tier >= 1 && tier <= 9) learnedFires.add(tier);
        fireChoiceMade = tag.getBoolean("FireChoiceMade");
        equippedFire = Math.max(0, Math.min(9, tag.getInt("EquippedFire")));
        // equippedFire 为 0 是合法的“已卸下”状态，必须原样恢复；仅当存档里的品阶确实失学时才换火。
        if (equippedFire != 0 && !learnedFires.contains(equippedFire))
            equippedFire = learnedFires.stream().findFirst().orElse(0);
        // 只连 EquippedFire 字段都没有的远古存档，才按旧版习惯迁移成第一朵习得的异火。
        if (equippedFire == 0 && !fireChoiceMade && !tag.contains("EquippedFire") && !learnedFires.isEmpty()) {
            equippedFire = learnedFires.stream().findFirst().orElse(0);
            fireChoiceMade = true;
        }
        thunderStrikes = Math.max(0, tag.getInt("ThunderStrikes"));
        // Completed legacy tempering records are sufficient evidence of an earned unlock.
        thunderUnlocked = tag.getBoolean("ThunderUnlocked") || thunderStrikes >= CultivationLogic.THUNDER_STRIKES_REQUIRED || creationBlessing;
        if (creationBlessing) thunderStrikes = Math.max(thunderStrikes, CultivationLogic.THUNDER_STRIKES_REQUIRED);
        // 兼容旧存档：没有解锁字段时，按“有火灵根且已收服过异火”即时判定异火灵根。
        exoticUnlocked = tag.contains("ExoticUnlocked") ? tag.getBoolean("ExoticUnlocked")
                : (rootMask & (1 << 3)) != 0 && !learnedFires.isEmpty();
        if (creationBlessing) exoticUnlocked = true;
        // 变异形态依附于基础灵根与解锁状态，缺任何一项都回退到本相。
        thunderActive = tag.getBoolean("ThunderActive") && thunderUnlocked && (rootMask & (1 << 1)) != 0;
        exoticActive = tag.getBoolean("ExoticActive") && exoticUnlocked && (rootMask & (1 << 3)) != 0;
        maxMana = CultivationLogic.calculateMaxMana(this);
        currentMana = clampMana(savedCurrentMana, maxMana);
    }
}
