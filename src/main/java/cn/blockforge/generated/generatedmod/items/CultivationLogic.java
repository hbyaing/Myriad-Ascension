package cn.blockforge.generated.generatedmod.items;

import cn.blockforge.generated.generatedmod.CultivationCapability;
import cn.blockforge.generated.generatedmod.balance.RealmBalance;
import cn.blockforge.generated.generatedmod.balance.ManaBalance;
import cn.blockforge.generated.generatedmod.balance.RootBalance;
import cn.blockforge.generated.generatedmod.balance.DamageDomainBalance;
import cn.blockforge.generated.generatedmod.balance.DamageDomainBalance.Domain;
import cn.blockforge.generated.generatedmod.CultivationData;
import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.acupoint.AcupointAttributes;
import cn.blockforge.generated.generatedmod.acupoint.AcupointCatalog.Stat;
import cn.blockforge.generated.generatedmod.ModEffects;
import cn.blockforge.generated.generatedmod.WuXingBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/** 五行仙途的数值规则与灵根被动效果。 */
public final class CultivationLogic {
    public static final int CRYSTALLIZE_AURA_COST = 20;
    public static final long INSIGHT_CULTIVATION = 1000L;
    public static final String CULTIVATION_REWARD_TAG = "CultivationReward";
    /** 各大境界初期修为上限；四阶段总上限依次为100至一亿。 */
    
    /** 每个境界的基础灵力回复比例，以境界基准灵力结算。 */
    private static final String[] REALMS = {"练气", "筑基", "金丹", "元婴", "化神", "炼虚", "合体", "大乘", "地仙"};
    private static final String[] STAGES = {"初期", "中期", "后期", "巅峰"};
    /** 练气至地仙的基础总生命；原版玩家的20点生命会被属性加成补齐。 */
    private static final UUID REALM_HEALTH_UUID = UUID.fromString("7d62d743-7b8a-4f74-9f40-b1a3a1d6a001");
    private static final UUID REALM_ATTACK_UUID = UUID.fromString("7d62d743-7b8a-4f74-9f40-b1a3a1d6a003");
    private static final UUID FIVE_ROOT_HEALTH_UUID = UUID.fromString("7d62d743-7b8a-4f74-9f40-b1a3a1d6a002");
    private CultivationLogic() {}

    public static int baseAura(int realm) { return RealmBalance.mana(realm); }
    public static float rootMultiplier(int count) { return RootBalance.capacity(count); }
    public static float stageMultiplier(int stage) { return RealmBalance.stage(stage); }
    public static long cultivationCap(int realm, int stage) {
        int safeRealm = Math.max(0, Math.min(8, realm));
        int safeStage = Math.max(0, Math.min(3, stage));
        return RealmBalance.cultivation(safeRealm, safeStage);
    }
    /** 突破成功后消耗上一大境界灵气上限的50%。 */
    public static long breakthroughAuraCost(CultivationData data) {
        return Math.max(1L, Math.round(maxAura(data) * cn.blockforge.generated.generatedmod.balance.HeartMethodBalance.breakthroughFraction(
                cn.blockforge.generated.generatedmod.ProgressionGate.effectiveHeartMethod(null,data))));
    }
    public static String stageName(int stage) { return STAGES[Math.max(0, Math.min(3, stage))]; }
    /** Minor stages have no pill, mana, chance or location gate; major realms still require trials. */
    public static boolean advanceMinorStage(Player player,CultivationData data) {
        if(player==null||player.level().isClientSide||player.isSpectator()||data==null
                ||data.wuxingxiantu$getCultivationStage()>=3||data.wuxingxiantu$getCultivation()<breakthroughRequirement(data))return false;
        data.wuxingxiantu$setCultivationStage(data.wuxingxiantu$getCultivationStage()+1);
        data.wuxingxiantu$setCultivation(0L);
        refreshMaxMana(data);applyHealthBonus(player,data);
        player.displayClientMessage(Component.literal("小境界突破："+realmStageName(player,data)),true);
        return true;
    }
    /** 灵力上限由境界基准与灌顶造成的永久经脉变化共同决定。 */
    public static float maxAura(CultivationData data) { return data == null ? 0.0F : data.wuxingxiantu$getMaxMana(); }
    /** 唯一的最大灵力计算入口：境界基础、灌顶经脉和体质倍率在此统一合并。 */
    public static float calculateMaxMana(CultivationData data) {
        if (data == null) return 0.0F;
        return ManaBalance.maximum(data.wuxingxiantu$getRealm(), data.wuxingxiantu$getAuraBonus(),
                RootBalance.capacity(data) * PhysiqueLogic.manaMultiplier(data) * cn.blockforge.generated.generatedmod.balance.HeartMethodBalance.manaMultiplier(
                        cn.blockforge.generated.generatedmod.ProgressionGate.effectiveHeartMethod(null,data)))
                + (float) data.acupoints().bonuses().get(Stat.MAX_MANA);
    }
    public static float refreshMaxMana(CultivationData data) {
        float target = calculateMaxMana(data);
        if (data != null && Math.abs(data.wuxingxiantu$getMaxMana() - target) > 0.01F)
            data.wuxingxiantu$setMaxMana(target);
        return target;
    }
    public static long breakthroughRequirement(CultivationData data) {
        return cultivationCap(data.wuxingxiantu$getRealm(), data.wuxingxiantu$getCultivationStage());
    }
    public static String realmName(int realm) { return REALMS[Math.max(0, Math.min(8, realm))]; }
    /** 境界名称不再按所在维度动态变化；鬼仙身份由玩家修炼数据中的突破记录决定。 */
    public static String realmName(Player player, int realm) {
        int safeRealm = Math.max(0, Math.min(8, realm));
        CultivationData data = player == null ? null : GeneratedMod.data(player);
        return safeRealm == 8 && data != null && data.wuxingxiantu$isGhostImmortal() ? "鬼仙" : realmName(safeRealm);
    }
    public static String realmStageName(CultivationData data) {
        if (data == null) return "";
        String realm = data.wuxingxiantu$isGhostImmortal() && data.wuxingxiantu$getRealm() == 8
                ? "鬼仙" : realmName(data.wuxingxiantu$getRealm());
        return realm + "·" + stageName(data.wuxingxiantu$getCultivationStage());
    }
    public static String realmStageName(Player player, CultivationData data) {
        return realmStageName(data);
    }

    /**
     * 按当前灵力上限恢复灵力，并返回实际恢复量。
     *
     * <p>灵气是旧版存档和接口中的兼容名称，实际玩家资源是当前灵力；所有恢复
     * 入口统一走这里，避免不同入口使用不同上限或绕过浮点值钳制。</p>
     */
    public static float restoreMana(CultivationData data, float amount) {
        if (data == null || !Float.isFinite(amount) || amount <= 0.0F) return 0.0F;
        float maximum = Math.max(0.0F, maxAura(data));
        float before = Math.max(0.0F, Math.min(maximum, data.wuxingxiantu$getCurrentMana()));
        if (before >= maximum) return 0.0F;
        float after = Math.min(maximum, before + amount);
        data.wuxingxiantu$setCurrentMana(after);
        return Math.max(0.0F, after - before);
    }

    /** 旧版“灵气”命名的兼容入口，实际恢复的是玩家灵力。 */
    @Deprecated
    public static float restoreAura(CultivationData data, float amount) {
        return restoreMana(data, amount);
    }

    public static void gainCultivation(Player player, CultivationData data, long cultivation) {
        if (data == null || player == null || player.isCreative() || player.isSpectator()) return;
        long cap = breakthroughRequirement(data);
        if(cn.blockforge.generated.generatedmod.mindscape.MindscapeManager.canGain(player,data,false)) {
            long current=Math.max(0L,Math.min(cap,data.wuxingxiantu$getCultivation()));
            if(current>=cap)return;
            String remainderKey="WuxingCultivationRewardRemainder";
            var reward=cn.blockforge.generated.generatedmod.balance.HeartMethodBalance.splitCultivationReward(
                    cn.blockforge.generated.generatedmod.ProgressionGate.effectiveHeartMethod(player,data),
                    data.wuxingxiantu$isMeditating(),cultivation,player.getPersistentData().getDouble(remainderKey));
            long gain=Math.min(reward.amount(),cap-current);
            player.getPersistentData().putDouble(remainderKey,gain<reward.amount()?0.0D:reward.remainder());
            data.wuxingxiantu$setCultivation(current+gain);
        }
    }

    /** Training only: direct rewards and instant pills deliberately do not call this method. */
    public static void gainTrainingCultivation(Player player,CultivationData data,double base) {
        if(data==null||player==null||player.isCreative()||player.isSpectator()
                ||!cn.blockforge.generated.generatedmod.mindscape.MindscapeManager.canGain(player,data,false))return;
        long cap=breakthroughRequirement(data),current=Math.max(0,Math.min(cap,data.wuxingxiantu$getCultivation()));
        if(current>=cap)return;
        String key="WuxingRootTrainingRemainder";
        double multiplier=RootBalance.training(data)*cn.blockforge.generated.generatedmod.balance.HeartMethodBalance.cultivationMultiplier(
                cn.blockforge.generated.generatedmod.ProgressionGate.effectiveHeartMethod(player,data),data.wuxingxiantu$isMeditating());
        multiplier *= data.acupoints().bonuses().multiplier(Stat.TRAINING_PCT);
        var reward=RootBalance.split(base,multiplier,player.getPersistentData().getDouble(key));
        long gain=Math.min(reward.amount(),cap-current);
        player.getPersistentData().putDouble(key,current+gain>=cap?0:reward.remainder());
        data.wuxingxiantu$setCultivation(current+gain);
    }

    public static void gainCultivationAndAura(Player player, CultivationData data, long cultivation, float aura) {
        gainCultivation(player, data, cultivation);
        if (data != null && player != null && !player.isSpectator() && !player.isCreative()) restoreMana(data, aura);
    }

    /** 实体自身修为奖励：优先读取持久化 CultivationReward，普通动物始终为零。 */
    public static long cultivationReward(LivingEntity entity) {
        if (entity == null || entity instanceof Animal) return 0L;
        if (entity.getPersistentData().contains(CULTIVATION_REWARD_TAG))
            return Math.max(0L, entity.getPersistentData().getLong(CULTIVATION_REWARD_TAG));
        if (entity instanceof Enemy) return Math.max(1L, Math.min(5L, Math.round(entity.getMaxHealth() / 4.0D)));
        return 0L;
    }

    public static long killCultivationReward(CultivationData data, LivingEntity entity) {
        return cultivationReward(entity);
    }

    public static float killManaReward(CultivationData data, LivingEntity entity) {
        if (data == null || entity == null) return 0.0F;
        return Math.max(1.0F, Math.min(maxAura(data) * 0.05F, entity.getMaxHealth() * 0.5F));
    }

    /** 旧版奖励方法名兼容；击杀恢复的是灵力，不是独立的第二条资源。 */
    @Deprecated
    public static float killAuraReward(CultivationData data, LivingEntity entity) {
        return killManaReward(data, entity);
    }

    public static long meditationCultivationReward(CultivationData data) {
        if (data == null) return 0L;
        double base = RealmBalance.meditation(data.wuxingxiantu$getRealm());
        double tierMultiplier = switch (data.wuxingxiantu$getMeditationTier()) {
            case 1 -> 1.30D;
            case 2 -> 1.60D;
            default -> 1.00D;
        };
        return Math.max(0L, Math.round(base * tierMultiplier));
    }

    public static float meditationAuraReward(CultivationData data) {
        return 0.0F;
    }

    /** 功法槽位按境界解锁：练气、筑基2，金丹4，元婴6，化神及以上8。 */
    public static int unlockedArtSlots(int realm) {
        return switch (Math.max(0, Math.min(8, realm))) {
            case 0, 1 -> 2;
            case 2 -> 4;
            case 3 -> 6;
            default -> 8;
        };
    }

    /** 道心对每秒恢复量的倍率。 */
    public static float daoXinMultiplier(int daoXin) {
        return switch (Math.max(0, Math.min(8, daoXin))) {
            case 1 -> 1.15F;
            case 2 -> 0.85F;
            case 3 -> 0.65F;
            case 4 -> 0.50F;
            case 5 -> 0.25F;
            case 6 -> 1.25F;
            case 7 -> 1.40F;
            case 8 -> 1.60F;
            default -> 1.0F;
        };
    }

    public static float daoXinDamageTakenMultiplier(int daoXin, boolean mindscape) {
        if (!mindscape) return 1.0F;
        return switch (Math.max(0, Math.min(8, daoXin))) {
            case 1 -> .95F; case 2 -> 1.05F; case 3 -> 1.10F; case 5 -> 1.15F;
            case 7 -> .85F; case 8 -> .80F; default -> 1.0F;
        };
    }
    public static float daoXinLightningMultiplier(int daoXin) {
        return switch (Math.max(0, Math.min(8, daoXin))) {
            case 1 -> .95F; case 2 -> 1.05F; case 3 -> 1.10F; case 6 -> .90F;
            case 7 -> .85F; case 8 -> .80F; default -> 1.0F;
        };
    }
    public static float daoXinHeartDemonDamageMultiplier(int daoXin) { return daoXin == 6 ? 1.10F : 1.0F; }

    /** 境界基础灵力 × 境界比例 × 平均灵根品质及各项倍率，单位为每秒恢复量。 */
    public static float manaRegenPerSecond(CultivationData data) {
        int realm = Math.max(0, Math.min(8, data.wuxingxiantu$getRealm()));
        float realmBase = Math.max(0.0F, baseAura(realm) * RealmBalance.regeneration(realm));
        float heartMultiplier = cn.blockforge.generated.generatedmod.balance.HeartMethodBalance.regenerationMultiplier(
                cn.blockforge.generated.generatedmod.ProgressionGate.effectiveHeartMethod(null,data),data.wuxingxiantu$isMeditating());
        return realmBase * RootBalance.recovery(data) * daoXinMultiplier(data.wuxingxiantu$getDaoXin())
                * PhysiqueLogic.regenMultiplier(data) * heartMultiplier;
    }

    public static float regeneration(CultivationData data, boolean meditating) {
        return baseRegeneration(data, meditating) * data.acupoints().bonuses().multiplier(Stat.MANA_REGEN_PCT);
    }

    private static float baseRegeneration(CultivationData data, boolean meditating) {
        if (!meditating) return manaRegenPerSecond(data);
        int tier = data.wuxingxiantu$getMeditationTier();
        float recoveryMultiplier = switch (tier) {
            case 1 -> 3.0F;
            case 2 -> 4.0F;
            default -> 2.0F;
        };
        return manaRegenPerSecond(data) * recoveryMultiplier;
    }

    /**
     * 每个服务端 tick 恢复一部分灵力，合计为每秒恢复量。
     *
     * <p>该方法由服务端统一玩家循环调用，而不是依赖玩家实体 tick 的阶段顺序；这样即使
     * 持续技能提前返回、玩家刚换维度或其他事件修改了能力，也不会跳过自然恢复。</p>
     */
    public static void tickManaRegen(Player player) {
        if (player == null || player.level().isClientSide) return;
        player.getCapability(CultivationCapability.INSTANCE).ifPresent(data -> {
            // 境界、灌顶或旧存档迁移后，先把最大灵力校正到当前规则，再计算本 tick 恢复量。
            refreshMaxMana(data);

            float maxMana = Math.max(0.0F, data.wuxingxiantu$getMaxMana());
            if (maxMana <= 0.0F) return;
            float currentMana = data.wuxingxiantu$getCurrentMana();
            if (!Float.isFinite(currentMana)) {
                currentMana = 0.0F;
                data.wuxingxiantu$setCurrentMana(currentMana);
            }
            currentMana = Math.max(0.0F, Math.min(maxMana, currentMana));
            if (currentMana >= maxMana) return;

            boolean meditating = data.wuxingxiantu$isMeditating();
            float calculatedPerSecond = baseRegeneration(data, meditating);
            float perSecond = calculatedPerSecond
                    * environmentMultiplier(player, data)
                    * cn.blockforge.generated.generatedmod.SpiritEnchantmentLogic.surgeRegenMultiplier(player);
            float capPerSecond = ManaBalance.regenerationCap(baseAura(data.wuxingxiantu$getRealm()), meditating) * RootBalance.recovery(data);
            perSecond = Math.min(Math.max(0.0F, capPerSecond), Math.max(0.0F, perSecond));
            float amount = perSecond * data.acupoints().bonuses().multiplier(Stat.MANA_REGEN_PCT) / 20.0F;
            if (!Float.isFinite(amount) || amount <= 0.0F) return;
            float nextMana = Math.min(maxMana, currentMana + amount);
            if (nextMana > currentMana) data.wuxingxiantu$setCurrentMana(nextMana);
        });
    }

    /** 按需求表维护境界生命上限，使用属性修饰符避免临时 Buff 累加。 */
    public static void applyHealthBonus(Player player, CultivationData data) {
        refreshMaxMana(data);
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        double previousMaxHealth = player.getMaxHealth();
        float previousHealth = player.getHealth();
        int realm = Math.max(0, Math.min(8, data.wuxingxiantu$getRealm()));
        float stage = stageMultiplier(data.wuxingxiantu$getCultivationStage());
        double realmAmount = Math.max(0.0D, RealmBalance.health(realm) * stage - 20.0D);
        AttributeModifier realmModifier = health.getModifier(REALM_HEALTH_UUID);
        if (realmModifier == null || realmModifier.getOperation() != AttributeModifier.Operation.ADDITION
                || Math.abs(realmModifier.getAmount() - realmAmount) > 0.001D) {
            health.removeModifier(REALM_HEALTH_UUID);
            health.addPermanentModifier(new AttributeModifier(REALM_HEALTH_UUID, "wuxingxiantu.realm_health",
                    realmAmount, AttributeModifier.Operation.ADDITION));
        }
        boolean fiveRoots = Integer.bitCount(data.wuxingxiantu$getRootMask()) == 5;
        AttributeModifier rootModifier = health.getModifier(FIVE_ROOT_HEALTH_UUID);
        if (fiveRoots && (rootModifier == null || rootModifier.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL
                || Math.abs(rootModifier.getAmount() - 0.10D) > 0.001D)) {
            health.removeModifier(FIVE_ROOT_HEALTH_UUID);
            health.addPermanentModifier(new AttributeModifier(FIVE_ROOT_HEALTH_UUID, "wuxingxiantu.five_root_health",
                    0.10D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (!fiveRoots && rootModifier != null) {
            health.removeModifier(FIVE_ROOT_HEALTH_UUID);
        }
        cn.blockforge.generated.generatedmod.HeartMethodEffects.updateAttributes(player,data);
        if (!player.level().isClientSide) AcupointAttributes.apply(player, data);
        double newMaxHealth = player.getMaxHealth();
        if (previousMaxHealth > 0.0D && Math.abs(newMaxHealth - previousMaxHealth) > 0.001D
                && previousHealth > 0.0F) {
            player.setHealth((float) Math.min(newMaxHealth, previousHealth / previousMaxHealth * newMaxHealth));
        } else if (player.getHealth() > newMaxHealth) player.setHealth((float) newMaxHealth);

        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            double attackAmount = RealmBalance.attack(realm) * stage;
            AttributeModifier attackModifier = attack.getModifier(REALM_ATTACK_UUID);
            if (attackModifier == null || attackModifier.getOperation() != AttributeModifier.Operation.ADDITION
                    || Math.abs(attackModifier.getAmount() - attackAmount) > 0.001D) {
                attack.removeModifier(REALM_ATTACK_UUID);
                if (attackAmount > 0.0D) {
                    attack.addPermanentModifier(new AttributeModifier(REALM_ATTACK_UUID, "wuxingxiantu.realm_attack",
                            attackAmount, AttributeModifier.Operation.ADDITION));
                }
            }
        }
    }

    public static boolean isInCave(Player player, CultivationData data) {
        return isWithinCave(player.blockPosition(), data);
    }

    public static boolean isWithinCave(BlockPos position, CultivationData data) {
        BlockPos cave = data.wuxingxiantu$getCavePos();
        if (cave == null) return false;
        int radius = data.wuxingxiantu$getCaveLevel() * 8;
        return Math.abs(position.getX() - cave.getX()) <= radius
                && Math.abs(position.getY() - cave.getY()) <= radius
                && Math.abs(position.getZ() - cave.getZ()) <= radius;
    }

    public static boolean onArray(Player player) {
        BlockPos below = player.blockPosition().below();
        return player.level().getBlockState(below).is(WuXingBlocks.GATHERING_ARRAY.get())
                ||cn.blockforge.generated.generatedmod.formation.carving.CarvingEye.gatheringAt(player);
    }

    public static float environmentMultiplier(Player player, CultivationData data) {
        float multiplier = 1.0F;
        if (player.level().dimension() == net.minecraft.world.level.Level.NETHER || player.level().dimension() == net.minecraft.world.level.Level.END) multiplier *= 0.5F;
        if (player.isUnderWater() || player.getY() > 100 && player.level().getBiome(player.blockPosition()).is(net.minecraft.tags.BiomeTags.IS_MOUNTAIN)) multiplier *= 1.5F;
        if(player.level() instanceof net.minecraft.server.level.ServerLevel level)
            multiplier *= 1.0F + cn.blockforge.generated.generatedmod.world.SpiritVeinData.get(level).auraAt(player.blockPosition()) / 100.0F;
        if (onArray(player)) multiplier *= 2.0F;
        if (isInCave(player, data)) multiplier *= 1.3F;
        if (data.wuxingxiantu$getBiguRemainTime() > 0 && data.wuxingxiantu$getRealm() <= 1 && player.isUnderWater()) multiplier *= 1.5F;
        if (GeneratedMod.isSpiritTide(player.level().getGameTime())) multiplier *= 2.0F;
        if (GeneratedMod.isSpiritStorm(player.level().getGameTime())) multiplier *= 0.5F;
        return Math.max(0.5F, Math.min(2.0F, multiplier));
    }

    /** 五行灵根是否全部觉醒且皆达极品（品质 4）。 */
    public static boolean hasAllPerfectRoots(CultivationData data) {
        if (data == null || data.wuxingxiantu$getRootMask() != 31) return false;
        for (int i = 0; i < 5; i++) {
            if (data.wuxingxiantu$getRootQuality(i) < 4) return false;
        }
        return true;
    }

    /**
     * 按五行极品状态刷新混沌灵根：凑齐自动凝聚并提示，失去条件自动消散。
     * 返回本次是否发生变化，调用方可据此决定是否同步客户端。
     */
    public static boolean refreshChaosRoot(Player player, CultivationData data) {
        boolean complete = hasAllPerfectRoots(data);
        if (data.wuxingxiantu$hasChaosRoot() == complete) return false;
        data.wuxingxiantu$setChaosRoot(complete);
        if (complete && !player.level().isClientSide) {
            player.displayClientMessage(Component.translatable("message.wuxingxiantu.chaos_root"), false);
        }
        return true;
    }

    public static void applyRootEffects(Player player, CultivationData data) {
        if (player.level() instanceof net.minecraft.server.level.ServerLevel level) clearDomainEffects(level, player);
        refreshDomainBuffs(player, data);
        // 变异形态依附于基础灵根与解锁记录，任何一项失效都立即变回本相。
        if (data.wuxingxiantu$isThunderRootActive() && !thunderRootAvailable(data)) data.wuxingxiantu$setThunderRootActive(false);
        if (data.wuxingxiantu$isExoticRootActive() && !exoticRootAvailable(data)) data.wuxingxiantu$setExoticRootActive(false);
        // 混沌灵根是五行极品的衍生状态：每秒兜底刷新一次，任何途径凑齐五行极品都会当场凝聚。
        if (refreshChaosRoot(player, data) && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            GeneratedMod.sync(serverPlayer);
        }
        boolean chaos = data.wuxingxiantu$hasChaosRoot();
        for (int root = 0; root < 5; root++) {
            if ((data.wuxingxiantu$getRootMask() & (1 << root)) == 0) continue;
            int quality = data.wuxingxiantu$getRootQuality(root);
            if (root == 0) {
                int luckAmp = quality >= 4 ? 4 : quality >= 2 ? 2 : 0;
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 45, luckAmp, true, false, true));
            } else if (root == 1) {
                if (quality >= 3) {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 45, chaos ? 1 : 0, true, false, true));
                }
                // 天雷灵根显化后，木灵领域的催生被上位领域“万雷领域”取代：
                // 范围内对敌对生物降下天雷，玩家自身无视闪电伤害。
                if (isThunderForm(data) && data.wuxingxiantu$isWoodDomainEnabled() && quality >= 2) {
                    thunderDomain(player, data, quality);
                }
            } else if (root == 2) {
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 45, 0, true, false, true));
                if (quality >= 2) player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 45, 0, true, false, true));
                if (data.wuxingxiantu$isWaterDomainEnabled() && quality >= 2) tideDomain(player, quality);
            } else if (root == 3) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 45, 0, true, false, true));
                if (data.wuxingxiantu$isFireDomainEnabled() && quality >= 2) {
                    // 异火灵根显化后，业火领域的凡火点燃换成与异火系统对接的异火灼烧。
                    if (isExoticForm(data)) exoticFireDomain(player, data, quality);
                    else fireAura(player, data, quality);
                }
            } else if (root == 4) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 45, Math.max(0, quality - 1), true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 45, Math.max(0, quality - 1), true, false, true));
                if (quality >= 3 && player.getHealth() < player.getMaxHealth() * 0.5F) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 45, 3, true, false, true));
                if (data.wuxingxiantu$isEarthDomainEnabled() && quality >= 2) gravityDomain(player, quality);
            }
        }
    }

    /** 将已勾选的领域同步成物品栏可查看的状态效果。 */
    public static void refreshDomainBuffs(Player player, CultivationData data) {
        if (data == null) return;
        var effects = new net.minecraft.world.effect.MobEffect[]{ModEffects.GOLD_DOMAIN.get(), ModEffects.WOOD_DOMAIN.get(),
                ModEffects.WATER_DOMAIN.get(), ModEffects.FIRE_DOMAIN.get(), ModEffects.EARTH_DOMAIN.get(),
                ModEffects.THUNDER_DOMAIN.get(), ModEffects.EXOTIC_DOMAIN.get()};
        boolean[] enabled = {data.wuxingxiantu$isGoldDomainEnabled(), data.wuxingxiantu$isWoodDomainEnabled(),
                data.wuxingxiantu$isWaterDomainEnabled(), data.wuxingxiantu$isFireDomainEnabled(), data.wuxingxiantu$isEarthDomainEnabled()};
        for (int i=0; i<effects.length; i++) {
            int root = i==5 ? 1 : i==6 ? 3 : i;
            boolean selected = i==1 ? !isThunderForm(data) : i==5 ? isThunderForm(data)
                    : i==3 ? !isExoticForm(data) : i==6 ? isExoticForm(data) : true;
            if (selected && enabled[root] && hasDomain(data,root,2)) addDomainBuff(player,effects[i],data.wuxingxiantu$getRootQuality(root));
            else if (player.hasEffect(effects[i])) player.removeEffect(effects[i]);
        }
    }

    private static boolean hasDomain(CultivationData data, int root, int minimumQuality) {
        return (data.wuxingxiantu$getRootMask() & (1 << root)) != 0
                && data.wuxingxiantu$getRootQuality(root) >= minimumQuality;
    }

    private static void addDomainBuff(Player player, net.minecraft.world.effect.MobEffect effect, int quality) {
        int amplifier = Math.max(0, quality - 2);
        var current = player.getEffect(effect);
        if (current == null || current.getAmplifier() != amplifier || current.getDuration() < 40) {
            if (current != null && current.getAmplifier() != amplifier) player.removeEffect(effect);
            player.addEffect(new MobEffectInstance(effect, 100, amplifier, true, true, true));
        }
    }

    private static void removeDomainBuffs(Player player) {
        player.removeEffect(ModEffects.WOOD_DOMAIN.get());
        player.removeEffect(ModEffects.FIRE_DOMAIN.get());
        player.removeEffect(ModEffects.THUNDER_DOMAIN.get());
        player.removeEffect(ModEffects.EXOTIC_DOMAIN.get());
        player.removeEffect(ModEffects.GOLD_DOMAIN.get());
        player.removeEffect(ModEffects.WATER_DOMAIN.get());
        player.removeEffect(ModEffects.EARTH_DOMAIN.get());
    }

    /** 领域的攻击、减速和压制效果只作用于原版定义的敌对生物。 */
    private static boolean isHostileDomainTarget(LivingEntity target) {
        return target instanceof Enemy && target.isAlive()
                && !HumanEmperorBannerItem.isServant(target);
    }

    private static boolean consumeDomainAura(Player player, CultivationData data, int cost) {
        if (player.isCreative()) return true;
        if (data == null || cost<0 || cost==Integer.MAX_VALUE
                || !Float.isFinite(data.wuxingxiantu$getAura()) || data.wuxingxiantu$getAura() < cost) return false;
        data.wuxingxiantu$setAura(data.wuxingxiantu$getAura() - cost);
        return true;
    }

    private static List<LivingEntity> domainTargets(Player player,Domain domain,int quality){
        double radius=DamageDomainBalance.radius(domain,quality);
        return new java.util.ArrayList<>(player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius),target->isHostileDomainTarget(target)
                        &&!target.isAlliedTo(player)&&target.distanceToSqr(player)<=radius*radius)
                .stream().sorted(java.util.Comparator.comparingDouble(player::distanceToSqr))
                .limit(DamageDomainBalance.targetLimit(domain,quality)).toList());
    }
    public static float domainDamage(Player player,Domain domain,int quality){
        return DamageDomainBalance.damage(domain,quality,player.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }
    public static int domainManaCost(Player player,CultivationData data,Domain domain,int quality,
                                     int targets,boolean damagingPulse,boolean waterWalking){
        if(data==null)return Integer.MAX_VALUE;
        return DamageDomainBalance.manaCost(domain,quality,player.getAttributeValue(Attributes.ATTACK_DAMAGE),
                data.wuxingxiantu$getRealm(),maxAura(data),targets,damagingPulse,waterWalking);
    }
    private static net.minecraft.world.damagesource.DamageSource domainSource(Player player){
        return new cn.blockforge.generated.generatedmod.combat.DomainDamageSource(player,false);
    }

    private static void fireAura(Player player, CultivationData data, int quality) {
        List<LivingEntity> targets=domainTargets(player,Domain.FIRE,quality);
        // 先确认领域有实际目标，避免空转扣费抵消低境界的自然恢复。
        if (targets.isEmpty() || !consumeDomainAura(player,data,domainManaCost(player,data,Domain.FIRE,quality,targets.size(),true,false))) return;
        float damage=domainDamage(player,Domain.FIRE,quality);
        for (LivingEntity target : targets) {
            target.hurt(domainSource(player),damage);
            target.setSecondsOnFire(2);
        }
    }

    /** 解锁天雷灵根所需的雷击次数。 */
    public static final int THUNDER_STRIKES_REQUIRED = 99;

    /** 天雷灵根是否可显化：已解锁、当前持有木灵根，并通过统一进度门槛（境界+品质）。 */
    public static boolean thunderRootAvailable(CultivationData data) {
        return data != null && data.wuxingxiantu$hasThunderRoot()
                && (data.wuxingxiantu$getRootMask() & (1 << 1)) != 0
                && cn.blockforge.generated.generatedmod.ProgressionGate.canManifestThunderRoot(data);
    }

    /** 当前是否以天雷灵根形态生效。 */
    public static boolean isThunderForm(CultivationData data) {
        return thunderRootAvailable(data) && data.wuxingxiantu$isThunderRootActive();
    }

    /** 异火灵根是否可显化：已解锁、持有火灵根，并通过统一进度门槛（境界+品质+异火阶位）。 */
    public static boolean exoticRootAvailable(CultivationData data) {
        return data != null && data.wuxingxiantu$hasExoticRoot()
                && (data.wuxingxiantu$getRootMask() & (1 << 3)) != 0
                && cn.blockforge.generated.generatedmod.ProgressionGate.canManifestExoticRoot(data);
    }

    /** 当前是否以异火灵根形态生效。 */
    public static boolean isExoticForm(CultivationData data) {
        return exoticRootAvailable(data) && data.wuxingxiantu$isExoticRootActive();
    }

    /** 万雷领域是否生效（天雷形态 + 领域开关 + 品质达标），用于闪电免疫判定。 */
    public static boolean isThunderDomainActive(CultivationData data) {
        return isThunderForm(data) && data.wuxingxiantu$isWoodDomainEnabled()
                && data.wuxingxiantu$getRootQuality(1) >= 2;
    }

    /**
     * 拥有木灵根期间被天雷淬体的计数入口；满 99 次解锁天雷灵根。
     *
     * <p>进度门槛（ProgressionGate）统一裁决：只有有效雷击来源（玩家自行召唤的雷
     * 不算）、达到最低境界且木灵根品质达标才计入淬体次数，防止练气期靠人为刷雷
     * 提前获得终局级特殊灵根。计数不足时不报错也不计数，境界不足给出明确提示。</p>
     */
    public static void addThunderStrike(net.minecraft.server.level.ServerPlayer player, CultivationData data,
                                        boolean validSource) {
        if (data == null || data.wuxingxiantu$hasThunderRoot()) return;
        if ((data.wuxingxiantu$getRootMask() & (1 << 1)) == 0) return;
        if (!validSource) return;
        if (!cn.blockforge.generated.generatedmod.ProgressionGate.canUnlockThunderRoot(player, data, true)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    cn.blockforge.generated.generatedmod.ProgressionGate.MSG_THUNDER_GATE,
                    realmName(player, cn.blockforge.generated.generatedmod.ProgressionGate.THUNDER_ROOT_REALM)), true);
            return;
        }
        int strikes = data.wuxingxiantu$getThunderStrikes() + 1;
        data.wuxingxiantu$setThunderStrikes(strikes);
        if (strikes >= THUNDER_STRIKES_REQUIRED) {
            data.wuxingxiantu$setThunderRoot(true);
            data.wuxingxiantu$setThunderStrikes(THUNDER_STRIKES_REQUIRED);
            player.displayClientMessage(Component.translatable("message.wuxingxiantu.thunder_root_unlocked"), false);
        } else {
            player.displayClientMessage(Component.translatable("message.wuxingxiantu.thunder_progress", strikes), true);
        }
        GeneratedMod.sync(player);
    }

    /**
     * 万雷领域：木灵领域的变异上位。数值参照业火领域的半径/费用曲线，
     * 改为对范围内敌对生物降下天雷（品阶越高，半径越大、落雷越多、单雷越重），
     * 闪电伤害由施雷者结算，击杀计入玩家。
     */
    private static void thunderDomain(Player player, CultivationData data, int quality) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        List<LivingEntity> targets=domainTargets(player,Domain.THUNDER,quality);
        if (targets.isEmpty() || !consumeDomainAura(player,data,domainManaCost(player,data,Domain.THUNDER,quality,targets.size(),true,false))) return;
        float damage=domainDamage(player,Domain.THUNDER,quality);
        for (LivingEntity target : targets) {
            cn.blockforge.generated.generatedmod.tribulation.ThunderField.emit(level,player,target.position(),data.wuxingxiantu$getRealm(),false,cn.blockforge.generated.generatedmod.tribulation.ThunderField.grounded(target));
            var source=new cn.blockforge.generated.generatedmod.combat.DomainDamageSource(player,true);
            if(target.hurt(source,damage))cn.blockforge.generated.generatedmod.ParalysisControl.apply(target,DamageDomainBalance.paralysisTicks(quality));
            if (target.isAlive()) target.setSecondsOnFire(quality >= 3 ? 3 : 2);
        }
        // 落雷本身会同步给客户端渲染并播放雷声，无需再手动播放音效。
    }

    /**
     * 异火领域：业火领域的变异上位。半径、直接伤害与费用沿用业火领域曲线，
     * 点燃改为调用异火系统的品阶灼烧（装备异火决定火焰颜色与灼烧伤害）；
     * 已燃烧的目标只续燃不叠层，防止每秒脉冲把灼烧层数刷爆。
     */
    private static void exoticFireDomain(Player player, CultivationData data, int quality) {
        List<LivingEntity> targets=domainTargets(player,Domain.EXOTIC,quality);
        if (targets.isEmpty() || !consumeDomainAura(player,data,domainManaCost(player,data,Domain.EXOTIC,quality,targets.size(),true,false))) return;
        float damage=domainDamage(player,Domain.EXOTIC,quality);
        // 驭火资格由统一门槛复核：旧数据里“已装备”的高阶异火在境界不足时按未装备结算。
        int equipped = cn.blockforge.generated.generatedmod.ProgressionGate.effectiveFire(
                data, data.wuxingxiantu$getEquippedFire());
        for (LivingEntity target : targets) {
            if (equipped > 0 && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                cn.blockforge.generated.generatedmod.SpiritEnchantmentLogic
                        .igniteByDomain(serverPlayer, target, equipped);
            }
            target.hurt(domainSource(player),damage);
        }
    }

    private static void tideDomain(Player player, int quality) {
        WATER_WALKING_PLAYERS.remove(player.getUUID());
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        clearDomainTargets(level, player, "tide", domainModifierId(player, "tide_speed"), domainModifierId(player, "tide_jump"));
        CultivationData data = GeneratedMod.data(player);
        List<LivingEntity> affected=domainTargets(player,Domain.TIDE,quality);
        boolean waterWalking = quality >= 4 && isAtWaterSurface(player);
        boolean pulse=quality>=3&&(quality>=4||player.tickCount%DamageDomainBalance.damageIntervalTicks(Domain.TIDE,quality)==0);
        int cost=domainManaCost(player,data,Domain.TIDE,quality,affected.size(),pulse,waterWalking);
        if ((!affected.isEmpty() || waterWalking) && !consumeDomainAura(player, data, cost)) return;
        // 陆地上只准备水上行走能力，不收费；进入水面后仍按原每秒费用结算。
        if (quality >= 4 && (player.isCreative() || data!=null && data.wuxingxiantu$getAura()>=
                domainManaCost(player,data,Domain.TIDE,quality,0,false,true))) WATER_WALKING_PLAYERS.add(player.getUUID());
        if (affected.isEmpty()) return;
        double slow = quality >= 4 ? 0.40D : quality >= 3 ? 0.30D : 0.20D;
        java.util.Set<UUID> targets = new java.util.HashSet<>();
        for (LivingEntity target : affected) {
            UUID speedId = domainModifierId(player, "tide_speed");
            UUID jumpId = domainModifierId(player, "tide_jump");
            addModifier(target, Attributes.MOVEMENT_SPEED, speedId,
                    "wuxingxiantu.tide_speed", -slow, AttributeModifier.Operation.MULTIPLY_TOTAL);
            if (quality >= 2) {
                addModifier(target, Attributes.JUMP_STRENGTH, domainModifierId(player, "tide_jump"),
                        "wuxingxiantu.tide_jump", quality >= 4 ? -0.50D : -0.25D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL);
            }
            if (pulse) {
                target.hurt(domainSource(player),domainDamage(player,Domain.TIDE,quality));
                int freezeChance = quality >= 4 ? 20 : 10;
                if (target.getRandom().nextInt(100) < freezeChance) freeze(target);
            }
            targets.add(target.getUUID());
        }
        DOMAIN_TARGETS.put(domainTargetsKey(player, "tide"), targets);
    }

    private static void gravityDomain(Player player, int quality) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        clearDomainTargets(level, player, "gravity", domainModifierId(player, "gravity_speed"),
                domainModifierId(player, "gravity_knockback"), domainModifierId(player, "gravity_jump"));
        List<LivingEntity> affected=domainTargets(player,Domain.GRAVITY,quality);
        var data=GeneratedMod.data(player);
        if (affected.isEmpty() || !consumeDomainAura(player,data,domainManaCost(player,data,Domain.GRAVITY,quality,affected.size(),quality>=3,false))) return;
        double slow = quality >= 4 ? 0.35D : quality >= 3 ? 0.25D : 0.15D;
        java.util.Set<UUID> targets = new java.util.HashSet<>();
        for (LivingEntity target : affected) {
            addModifier(target, Attributes.MOVEMENT_SPEED, domainModifierId(player, "gravity_speed"),
                    "wuxingxiantu.gravity_speed", -slow, AttributeModifier.Operation.MULTIPLY_TOTAL);
            addModifier(target, Attributes.KNOCKBACK_RESISTANCE, domainModifierId(player, "gravity_knockback"),
                    "wuxingxiantu.gravity_knockback", -0.50D, AttributeModifier.Operation.MULTIPLY_TOTAL);
            if (quality >= 4) addModifier(target, Attributes.JUMP_STRENGTH, domainModifierId(player, "gravity_jump"),
                    "wuxingxiantu.gravity_jump", -0.50D, AttributeModifier.Operation.MULTIPLY_TOTAL);
            if (quality >= 3) {
                target.hurt(domainSource(player),domainDamage(player,Domain.GRAVITY,quality));
                int petrifyChance = quality >= 4 ? 15 : 10;
                if (target.getRandom().nextInt(100) < petrifyChance) freeze(target);
            }
            targets.add(target.getUUID());
        }
        DOMAIN_TARGETS.put(domainTargetsKey(player, "gravity"), targets);
    }

    private static final java.util.Map<String, java.util.Set<UUID>> DOMAIN_TARGETS = new java.util.HashMap<>();
    private static final java.util.Set<UUID> WATER_WALKING_PLAYERS = new java.util.HashSet<>();

    private static String domainTargetsKey(Player player, String domain) {
        return player.getUUID() + ":" + domain;
    }

    private static UUID domainModifierId(Player player, String name) {
        return UUID.nameUUIDFromBytes(("wuxingxiantu:" + player.getUUID() + ":" + name)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static void clearDomainTargets(net.minecraft.server.level.ServerLevel level, Player player, String domain, UUID... modifierIds) {
        java.util.Set<UUID> previous = DOMAIN_TARGETS.remove(domainTargetsKey(player, domain));
        if (previous == null) return;
        for (UUID targetId : previous) {
            if (level.getEntity(targetId) instanceof LivingEntity target) {
                for (UUID modifierId : modifierIds) {
                    removeModifier(target, modifierId);
                }
            }
        }
    }

    private static void addModifier(LivingEntity target, net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                    UUID id, String name, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = target.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, name, amount, operation));
    }

    private static void removeModifier(LivingEntity target, UUID id) {
        for (net.minecraft.world.entity.ai.attributes.Attribute attribute : new net.minecraft.world.entity.ai.attributes.Attribute[]{
                Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH, Attributes.KNOCKBACK_RESISTANCE}) {
            AttributeInstance instance = target.getAttribute(attribute);
            if (instance != null) instance.removeModifier(id);
        }
    }

    private static void freeze(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 10, true, false, true));
        target.setDeltaMovement(0.0D, target.getDeltaMovement().y, 0.0D);
    }

    public static boolean isFrozen(LivingEntity entity) {
        MobEffectInstance effect = entity.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        return effect != null && effect.getAmplifier() >= 10;
    }

    public static boolean canGoldDomain(CultivationData data) {
        return (data.wuxingxiantu$getRootMask() & 1) != 0 && data.wuxingxiantu$getRootQuality(0) >= 2;
    }

    public static int goldDomainCost(CultivationData data) {
        int quality = data.wuxingxiantu$getRootQuality(0);
        return quality >= 4 ? 40 : quality >= 3 ? 30 : 20;
    }

    public static int goldDomainCooldown(CultivationData data) {
        return data.wuxingxiantu$getRootQuality(0) >= 3 ? 20 : 0;
    }

    public static boolean isGoldOre(BlockState state) {
        return state.is(BlockTags.GOLD_ORES);
    }

    public static boolean isSupportedGoldOre(BlockState state) {
        return state.is(WuXingBlocks.SPIRIT_ORE.get()) || state.is(WuXingBlocks.SPIRIT_COAL_ORE.get())
                || state.is(net.minecraft.world.level.block.Blocks.NETHER_QUARTZ_ORE)
                || state.is(BlockTags.IRON_ORES) || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.COPPER_ORES)
                || state.is(BlockTags.COAL_ORES) || state.is(BlockTags.REDSTONE_ORES)
                || state.is(BlockTags.LAPIS_ORES) || state.is(BlockTags.EMERALD_ORES);
    }

    public static BlockState goldDomainResult(BlockState state, int quality) {
        if (quality == 2) return state.is(BlockTags.IRON_ORES) ? Blocks.GOLD_ORE.defaultBlockState() : null;
        // Third-grade conversion is item-producing rather than block-producing;
        // returning the original state is the authoritative "supported" marker
        // consumed by the domain handler before it extracts the mineral drop.
        if (quality == 3) return isSupportedGoldOre(state) ? state : null;
        return state.is(Blocks.NETHER_QUARTZ_ORE) ? Blocks.QUARTZ_BLOCK.defaultBlockState()
                : state.is(WuXingBlocks.SPIRIT_ORE.get()) ? WuXingBlocks.DARK_IRON_BLOCK.get().defaultBlockState()
                : state.is(WuXingBlocks.SPIRIT_COAL_ORE.get()) ? WuXingBlocks.SPIRIT_COAL_BLOCK.get().defaultBlockState()
                : state.is(BlockTags.IRON_ORES) ? Blocks.IRON_BLOCK.defaultBlockState()
                : state.is(BlockTags.GOLD_ORES) ? Blocks.GOLD_BLOCK.defaultBlockState()
                : state.is(BlockTags.COPPER_ORES) ? Blocks.COPPER_BLOCK.defaultBlockState()
                : state.is(BlockTags.DIAMOND_ORES) ? Blocks.DIAMOND_BLOCK.defaultBlockState()
                : state.is(BlockTags.EMERALD_ORES) ? Blocks.EMERALD_BLOCK.defaultBlockState()
                : state.is(BlockTags.COAL_ORES) ? Blocks.COAL_BLOCK.defaultBlockState()
                : state.is(BlockTags.REDSTONE_ORES) ? Blocks.REDSTONE_BLOCK.defaultBlockState()
                : state.is(BlockTags.LAPIS_ORES) ? Blocks.LAPIS_BLOCK.defaultBlockState() : null;
    }

    public static void clearDomainEffects(net.minecraft.server.level.ServerLevel level, Player player) {
        WATER_WALKING_PLAYERS.remove(player.getUUID());
        removeDomainBuffs(player);
        clearDomainTargets(level, player, "tide", domainModifierId(player, "tide_speed"), domainModifierId(player, "tide_jump"));
        clearDomainTargets(level, player, "gravity", domainModifierId(player, "gravity_speed"),
                domainModifierId(player, "gravity_knockback"), domainModifierId(player, "gravity_jump"));
    }

    private static boolean isAtWaterSurface(Player player) {
        if (player.isUnderWater()) return false;
        BlockPos below = player.blockPosition().below();
        return player.level().getFluidState(below).is(FluidTags.WATER)
                && player.getY() <= below.getY() + 1.20D;
    }

    public static void tickWaterWalking(Player player, CultivationData data) {
        if (!WATER_WALKING_PLAYERS.contains(player.getUUID()) || !isAtWaterSurface(player)) return;
        BlockPos below = player.blockPosition().below();
        player.setPos(player.getX(), below.getY() + 1.001D, player.getZ());
        player.setOnGround(true);
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, Math.max(0.0D, velocity.y), velocity.z);
    }
}
