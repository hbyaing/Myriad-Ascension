package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.items.CultivationLogic;
import cn.blockforge.generated.generatedmod.items.HumanEmperorBannerItem;
import cn.blockforge.generated.generatedmod.items.SpiritSwordItem;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.UUID;

/** 灵力附魔的服务端效果实现。 */
public final class SpiritEnchantmentLogic {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final EquipmentSlot[] REPAIR_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final EquipmentSlot[] CORROSION_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final float[] TIER_DAMAGE_MULTIPLIERS = {
            0.0F, 1.05F, 1.10F, 1.15F, 1.20F, 1.25F, 1.30F, 1.35F, 1.40F, 1.50F
    };
    private static final float[] LEVEL_DAMAGE_MULTIPLIERS = {
            0.0F, 1.0F, 1.04F, 1.08F, 1.12F, 1.20F
    };
    private static final String RED_BURN_BOOST_TAG = "wuxingxiantu_red_burn_boost";
    private static final String NETHER_HEAL_REDUCTION_TAG = "wuxingxiantu_nether_heal_reduction";
    private static final String COLD_BONE_EXPIRES_TAG = "wuxingxiantu_cold_bone_expires";
    private static final String SPECIAL_COOLDOWN_PREFIX = "wuxingxiantu_exotic_fire_cooldown_";
    private static final UUID COLD_BONE_ATTACK_SPEED_ID = UUID.fromString("4e66d44f-cf5f-45ef-b5c6-a13b619f7791");
    private static final UUID NETHER_ARMOR_CORROSION_ID = UUID.fromString("c2c5cc7c-7613-4b2a-8e1a-f7a7bf63f5a9");
    private record WeaponHit(DamageSource source,ServerPlayer owner,net.minecraft.resources.ResourceLocation fire) {}
    private static final java.util.Map<LivingEntity,WeaponHit> WEAPON_HITS=new java.util.WeakHashMap<>();

    private SpiritEnchantmentLogic() {}

    public static void tick(ServerPlayer player, long gameTime) {
        CultivationData data = GeneratedMod.data(player);
        if (data == null) return;
        updateManaCap(player, data);
        if (gameTime % 20L == 0L) repairOneItem(player, data, gameTime);
        if (gameTime % 2L == 0L) exoticFireAura(player, data, gameTime);
    }

    /** 维护由境界和灌顶经脉加成共同决定的灵力上限；附魔只影响恢复效率。 */
    public static void updateManaCap(Player player, CultivationData data) {
        CultivationLogic.refreshMaxMana(data);
    }

    /** 按灵能涌动总等级提高每秒恢复量，四件盔甲的效果可以叠加。 */
    public static float surgeRegenMultiplier(Player player) {
        int total = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            total += EnchantmentHelper.getItemEnchantmentLevel(SpiritEnchantment.SPIRIT_SURGE.get(), player.getItemBySlot(slot));
        }
        return 1.0F + total * 0.05F;
    }

    /** 轻灵每级降低武器灵力消耗，最多保留 45% 的基础消耗。 */
    public static float lightnessCostMultiplier(ItemStack weapon) {
        int level = EnchantmentHelper.getItemEnchantmentLevel(SpiritEnchantment.LIGHTNESS.get(), weapon);
        return Math.max(0.45F, 1.0F - Math.max(0, level) * 0.18F);
    }

    public static int weaponAuraCost(int baseCost, ItemStack weapon) {
        return Math.max(1, Math.round(Math.max(baseCost*.45F,cn.blockforge.generated.generatedmod.crafting.EquipmentCrafting.upkeep(weapon,baseCost * lightnessCostMultiplier(weapon)))));
    }

    private static int highestSurgeLevel(Player player) {
        int highest = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            highest = Math.max(highest, EnchantmentHelper.getItemEnchantmentLevel(
                    SpiritEnchantment.SPIRIT_SURGE.get(), player.getItemBySlot(slot)));
        }
        return highest;
    }

    private static void repairOneItem(ServerPlayer player, CultivationData data, long gameTime) {
        ItemStack selected = null;
        int selectedLevel = 0;
        int selectedDamage = -1;
        for (EquipmentSlot slot : REPAIR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            int level = EnchantmentHelper.getItemEnchantmentLevel(SpiritEnchantment.SPIRIT_MENDING.get(), stack);
            if (level <= 0 || !stack.isDamageableItem()) continue;
            int missing = stack.getItem() instanceof HumanEmperorBannerItem
                    ? HumanEmperorBannerItem.MAX_SOUL_POWER - HumanEmperorBannerItem.soulPower(stack)
                    : stack.getDamageValue();
            if (missing <= 0) continue;
            if (selected == null || missing > selectedDamage) {
                selected = stack;
                selectedLevel = level;
                selectedDamage = missing;
            }
        }
        if (selected == null) return;
        int interval = Math.max(4, 20 - (selectedLevel - 1) * 4);
        if (gameTime % interval != 0L) return;
        float cost = 2.0F - (selectedLevel - 1) * 0.20F;
        if (data.wuxingxiantu$getAura() < cost) return;
        data.wuxingxiantu$setAura(data.wuxingxiantu$getAura() - cost);
        if (selected.getItem() instanceof HumanEmperorBannerItem) {
            HumanEmperorBannerItem.pourSoulPower(selected, 10);
        } else {
            selected.setDamageValue(Math.max(0, selected.getDamageValue() - 1));
        }
        GeneratedMod.sync(player);
    }

    public static void applySpiritGuard(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getEntity().level().isClientSide) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || event.getSource().is(DamageTypeTags.IS_FALL)) return;
        CultivationData data = GeneratedMod.data(player);
        if (data == null || event.getAmount() <= 0.0F) return;
        double totalLevel = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            var stack=player.getItemBySlot(slot);
            totalLevel += cn.blockforge.generated.generatedmod.crafting.EquipmentCrafting.guardLevel(stack)
                    *cn.blockforge.generated.generatedmod.crafting.EquipmentCrafting.potency(stack,"guard");
        }
        if (totalLevel <= 0) return;
        totalLevel=Math.min(16,totalLevel);
        float requiredAura = (float)(event.getAmount() * totalLevel * 2.0F);
        float availableAura = data.wuxingxiantu$getAura();
        float reduction = (float)(totalLevel * 0.05F);
        if (availableAura < requiredAura) reduction *= 0.5F;
        float spent = Math.min(availableAura, requiredAura);
        if (spent <= 0.0F) return;
        data.wuxingxiantu$setAura(availableAura - spent);
        Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->player),
            new cn.blockforge.generated.generatedmod.network.packet.cultivation.ShieldImpactPacket(player.getId(),
                cn.blockforge.generated.generatedmod.event.SpiritShieldImpactEvents.incomingDirection(player,event.getSource().getDirectEntity(),event.getSource())));

        event.setAmount(event.getAmount() * Math.max(0.0F, 1.0F - Math.min(0.80F, reduction)));
        GeneratedMod.sync(player);
    }

    public static void applySpiritDrain(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        ServerPlayer player = findKiller(event.getEntity(), event.getSource().getDirectEntity());
        if (player == null) return;
        ItemStack weapon = player.getMainHandItem();
        int level = EnchantmentHelper.getItemEnchantmentLevel(SpiritEnchantment.SPIRIT_DRAIN.get(), weapon);
        if (level <= 0) {
            weapon = player.getOffhandItem();
            level = EnchantmentHelper.getItemEnchantmentLevel(SpiritEnchantment.SPIRIT_DRAIN.get(), weapon);
        }
        if (level <= 0) return;
        CultivationData data = GeneratedMod.data(player);
        if (data == null) return;
        float[] ratios = {0.0F, 0.01F, 0.02F, 0.03F, 0.05F, 0.08F};
        int restoreBase = weapon.getItem() instanceof SpiritSwordItem sword
                ? sword.maxSpirit()
                : Math.round(CultivationLogic.maxAura(data));
        int restore = Math.max(1, Math.round(restoreBase * ratios[Math.min(5, level)]));
        int overflow = SpiritSwordItem.addSpirit(weapon, restore);
        if (overflow > 0) {
            data.wuxingxiantu$setAura(Math.min(CultivationLogic.maxAura(data), data.wuxingxiantu$getAura() + overflow));
        }
        if (level >= 2) {
            int duration = level >= 5 ? 160 : level >= 3 ? 100 : 60;
            int amplifier = level >= 4 ? 1 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, amplifier, true, false, true));
        }
        if (level >= 5) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0, true, false, true));
        GeneratedMod.sync(player);
    }

    private static ServerPlayer findKiller(LivingEntity victim, Entity directEntity) {
        if (directEntity instanceof ServerPlayer player) return player;
        if (victim.getKillCredit() instanceof ServerPlayer player) return player;
        return null;
    }

    /** 异火缠绕可影响普通生物，但不影响施术者本人或人皇幡魂仆。 */
    private static boolean canReceiveExoticFire(LivingEntity target, ServerPlayer owner) {
        return target != null && target.isAlive() && target != owner
                && !(target instanceof Player) && !HumanEmperorBannerItem.isServant(target)
                && !(target instanceof SoulBodyEntity body && owner!=null && body.owner().filter(owner.getUUID()::equals).isPresent());
    }

    /** Only the wrapping enchantment observes this per-player setting; domains retain their own rules. */
    private static boolean canReceiveWrapFire(LivingEntity target,ServerPlayer owner){
        if(owner==null||!canReceiveExoticFire(target,owner))return false;
        if(target instanceof Mob mob){
            if(mob.getTarget()==owner)return true;
            var attackTarget=net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET;
            if(mob.getBrain().hasMemoryValue(attackTarget)&&mob.getBrain().getMemory(attackTarget).orElse(null)==owner)return true;
        }
        if(target instanceof net.minecraft.world.entity.NeutralMob neutral){
            if(neutral.isAngryAt(owner))return true;
        }else if(target instanceof Enemy&&!(target instanceof net.minecraft.world.entity.monster.piglin.Piglin))return true;
        return GeneratedMod.data(owner) instanceof CultivationDataImpl data&&data.exoticFireHurtsNonHostile();
    }

    /** 防具四槽的异火缠绕总等级，封顶 V。 */
    private static int armorWrapLevel(LivingEntity entity) {
        int total = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            total += EnchantmentHelper.getItemEnchantmentLevel(
                    SpiritEnchantment.EXOTIC_FIRE_WRAP.get(), entity.getItemBySlot(slot));
        }
        return Math.min(5, total);
    }

    /**
     * 异火缠绕（防具）：周期性点燃附近普通生物。
     * 未装备异火时整个身上效果关闭，不再按默认一阶异火运行。
     */
    private static void exoticFireAura(ServerPlayer player, CultivationData data, long gameTime) {
        int level = armorWrapLevel(player);
        if (level <= 0) return;
        // 驭火资格由 ProgressionGate 复核：境界不足的高阶异火按未装备结算。
        int equipped = data == null ? 0 : ProgressionGate.effectiveFire(data, data.wuxingxiantu$getEquippedFire());
        if (equipped <= 0) return;
        Level world = player.level();
        int tier = ExoticFireColorRegistry.clampTier(equipped);
        long interval = Math.max(20L, 44L - level * 6L);
        if (gameTime % interval != 0L) return;
        double radius = 2.0D + level * 0.25D;
        for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius, 1.25D, radius),
                entity -> canReceiveWrapFire(entity, player))) {
            igniteExoticFire(target, tier, level, player);
        }
    }

    /** 每秒把防具缠绕的品阶与等级广播给追踪该玩家的客户端，驱动环绕火焰特效；由统一玩家循环调用，御剑时也不中断。 */
    public static void broadcastWrapVisual(ServerPlayer player, CultivationData data) {
        int level = armorWrapLevel(player);
        int equipped = data == null ? 0 : ProgressionGate.effectiveFire(data, data.wuxingxiantu$getEquippedFire());
        int tier = level > 0 && equipped > 0 ? ExoticFireColorRegistry.clampTier(equipped) : 0;
        Network.sendWrapVisual(player, tier, level);
    }

    /**
     * 异火缠绕（武器）：在护甲结算前严格应用“基础命中伤害 × 品阶倍率 × 附魔倍率”。
     * 读取实际近战/御剑攻击物品；自定义灼烧作用于除攻击者和魂仆外的生物。
     * 额外元素伤害不会递归触发本方法。
     */
    public static void applyExoticFireStrike(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || !target.isAlive() || event.getAmount() <= 0.0F
                || !canReceiveExoticFire(target, null)) return;
        WEAPON_HITS.remove(target);

        DamageSource source = event.getSource();
        if ((!source.is(DamageTypes.PLAYER_ATTACK)&&!cn.blockforge.generated.generatedmod.combat.HeartSwordCombatProfile.isHeartTrue(source))
                || !(source.getEntity() instanceof ServerPlayer player)
                || source.getDirectEntity() != player) return;
        if(!canReceiveWrapFire(target,player))return;

        var weapon=cn.blockforge.generated.generatedmod.fire.WeaponFlameState.attackingStack(player);
        int level=cn.blockforge.generated.generatedmod.fire.WeaponFlameState.level(weapon);
        if (level <= 0) return;
        var fireId=cn.blockforge.generated.generatedmod.fire.WeaponFlameState.fireId(weapon,player);
        if(fireId==null)return;
        int tier=cn.blockforge.generated.generatedmod.fire.FireCatalog.id(fireId.toString()).tier();
        float baseDamage = event.getAmount();
        float multipliedDamage = baseDamage * TIER_DAMAGE_MULTIPLIERS[tier] * LEVEL_DAMAGE_MULTIPLIERS[level];
        if (tier == 2 && player.getRandom().nextFloat() < 0.20F) {
            multipliedDamage = compensateForArmorPenetration(target, multipliedDamage, 0.10F);
        }

        cn.blockforge.generated.generatedmod.fire.ExoticBurnManager.ignite(target,fireId,level,player,true);
        WEAPON_HITS.put(target,new WeaponHit(source,player,fireId));
        event.setAmount(multipliedDamage + applyTierSpecial(player, target, tier, baseDamage));
    }

    public static void finishWeaponHit(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        var hit=WEAPON_HITS.remove(event.getEntity());
        if(hit==null||hit.source()!=event.getSource()||event.isCanceled()||event.getAmount()<=0)return;
        var target=event.getEntity();
        var bounds=target.getBoundingBox();var eye=hit.owner().getEyePosition();
        var point=new net.minecraft.world.phys.Vec3(net.minecraft.util.Mth.clamp(eye.x,bounds.minX,bounds.maxX),
                net.minecraft.util.Mth.clamp(eye.y,bounds.minY,bounds.maxY),net.minecraft.util.Mth.clamp(eye.z,bounds.minZ,bounds.maxZ));
        Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->target),
                new cn.blockforge.generated.generatedmod.network.packet.cultivation.WeaponFlameHitPacket(
                        target.level().dimension().location(),target.getId(),target.getUUID(),hit.fire(),point));
    }

    /** 熔金淬火按原版护甲公式折算，使本次攻击等效无视目标 10% 护甲。 */
    private static float compensateForArmorPenetration(LivingEntity target, float damage, float penetration) {
        float armor = cn.blockforge.generated.generatedmod.acupoint.AcupointAttributes.armorForDamage(target, target.getArmorValue());
        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        if (armor <= 0.0F) return damage;
        float normalAfterArmor = CombatRules.getDamageAfterAbsorb(damage, armor, toughness);
        float piercedAfterArmor = CombatRules.getDamageAfterAbsorb(damage, armor * (1.0F - penetration), toughness);
        if (normalAfterArmor <= 0.0F) return damage;
        return damage * piercedAfterArmor / normalAfterArmor;
    }

    private static float applyTierSpecial(ServerPlayer player, LivingEntity target, int tier, float baseDamage) {
        long now = player.level().getGameTime();
        float extraDamage = 0.0F;
        switch (tier) {
            case 1 -> {
                if (claimCooldown(player, tier, now, 60L)) {
                    player.getPersistentData().putLong(RED_BURN_BOOST_TAG, now + 60L);
                }
            }
            case 3 -> applyColdBone(target, now);
            case 4 -> {
                if (claimCooldown(player, tier, now, 40L)) player.heal(Math.max(1.0F, player.getMaxHealth() * 0.005F));
            }
            case 5 -> {
                if (claimCooldown(player, tier, now, 100L)) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 10, true, false, true));
                    target.setDeltaMovement(0.0D, target.getDeltaMovement().y, 0.0D);
                }
            }
            case 6 -> {
                if (player.getRandom().nextFloat() < 0.10F && claimCooldown(player, tier, now, 60L)) {
                    arcLightning(player, target, baseDamage * 0.50F);
                }
            }
            case 7 -> {
                if (target.getMobType() == MobType.UNDEAD && player.getRandom().nextFloat() < 0.05F
                        && claimCooldown(player, tier, now, 60L)) {
                    extraDamage += baseDamage * 0.35F;
                }
            }
            case 8 -> {
                if (claimCooldown(player, tier, now, 160L)) corrodeTarget(target, now);
            }
            case 9 -> extraDamage += baseDamage * 0.20F;
            default -> {
            }
        }
        return extraDamage;
    }

    private static boolean claimCooldown(ServerPlayer player, int tier, long now, long duration) {
        CompoundTag tag = player.getPersistentData();
        String key = SPECIAL_COOLDOWN_PREFIX + tier;
        if (tag.getLong(key) > now) return false;
        tag.putLong(key, now + duration);
        return true;
    }

    private static void applyColdBone(LivingEntity target, long now) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false, true));
        AttributeInstance attackSpeed = target.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(COLD_BONE_ATTACK_SPEED_ID);
            attackSpeed.addTransientModifier(new AttributeModifier(COLD_BONE_ATTACK_SPEED_ID,
                    "Cold bone attack speed", -0.15D, AttributeModifier.Operation.MULTIPLY_TOTAL));
            target.getPersistentData().putLong(COLD_BONE_EXPIRES_TAG, now + 60L);
        }
    }

    private static void arcLightning(ServerPlayer player, LivingEntity struck, float damage) {
        ServerLevel level = player.serverLevel();
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class,
                struck.getBoundingBox().inflate(4.0D),
                entity -> entity != struck && entity instanceof Enemy && canReceiveWrapFire(entity,player))) {
            nearby.hurt(elementalSource(player), damage);
        }
    }

    private static void corrodeTarget(LivingEntity target, long now) {
        for (EquipmentSlot slot : CORROSION_SLOTS) {
            ItemStack stack = target.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                stack.hurtAndBreak(1, target, broken -> target.broadcastBreakEvent(slot));
            }
        }
        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.removeModifier(NETHER_ARMOR_CORROSION_ID);
            armor.addTransientModifier(new AttributeModifier(NETHER_ARMOR_CORROSION_ID,
                    "Nether fire armor corrosion", -0.10D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        target.getPersistentData().putLong(NETHER_HEAL_REDUCTION_TAG, now + 100L);
    }

    private static DamageSource elementalSource(ServerPlayer player) {
        return new DamageSource(player.level().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.MAGIC), player, player);
    }

    /** All elemental ignition and stacking now belong to the independent manager. */
    private static void igniteExoticFire(LivingEntity target, int tier, int level, ServerPlayer owner) {
        var fire=cn.blockforge.generated.generatedmod.fire.FireCatalog.tier(tier);
        if(fire==null)return;
        cn.blockforge.generated.generatedmod.fire.ExoticBurnManager.ignite(target,fire.key(),level,owner,true);
        var burn=cn.blockforge.generated.generatedmod.fire.ExoticBurnManager.state(target);
        if(burn!=null)sendBurst(target,tier,14+(int)burn.intensity());
    }

    public static void igniteByDomain(ServerPlayer owner, LivingEntity target, int tier) {
        if(owner==null||!canReceiveExoticFire(target,owner))return;
        var fire=cn.blockforge.generated.generatedmod.fire.FireCatalog.tier(tier);
        if(fire!=null)cn.blockforge.generated.generatedmod.fire.ExoticBurnManager.ignite(target,fire.key(),1,owner,false);
    }

    /** Existing particles and temporary debuffs only; the manager owns duration and damage. */
    public static void tickBurningExoticFire(LivingEntity entity) {
        if(entity.level().isClientSide)return;
        var burn=cn.blockforge.generated.generatedmod.fire.ExoticBurnManager.state(entity);
        var fire=burn==null?null:cn.blockforge.generated.generatedmod.fire.FireCatalog.id(burn.fireId().toString());
        if(fire!=null&&burn.elemental()&&(entity.tickCount&1)==0)sendBurnParticles(entity,fire.tier());
        clearTemporaryModifiers(entity,entity.getPersistentData());
    }

    private static void clearTemporaryModifiers(LivingEntity entity, CompoundTag tag) {
        long now = entity.level().getGameTime();
        long coldExpires = tag.getLong(COLD_BONE_EXPIRES_TAG);
        if (coldExpires > 0L && coldExpires <= now) {
            AttributeInstance attackSpeed = entity.getAttribute(Attributes.ATTACK_SPEED);
            if (attackSpeed != null) attackSpeed.removeModifier(COLD_BONE_ATTACK_SPEED_ID);
            tag.remove(COLD_BONE_EXPIRES_TAG);
        }
        long corrosionExpires = tag.getLong(NETHER_HEAL_REDUCTION_TAG);
        if (corrosionExpires > 0L && corrosionExpires <= now) {
            AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
            if (armor != null) armor.removeModifier(NETHER_ARMOR_CORROSION_ID);
            tag.remove(NETHER_HEAL_REDUCTION_TAG);
        }
    }

    private static void sendBurnParticles(LivingEntity entity, int tier) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        long time = level.getGameTime();
        DustColorTransitionOptions dust = new DustColorTransitionOptions(
                ExoticFireColorRegistry.vector(ExoticFireColorRegistry.color(tier, time)),
                ExoticFireColorRegistry.vector(ExoticFireColorRegistry.nextColor(tier, time)),
                tier == 8 ? 0.70F : 0.95F);
        level.sendParticles(dust, entity.getX(), entity.getY() + entity.getBbHeight() * 0.52D, entity.getZ(),
                tier == 9 ? 7 : 4, entity.getBbWidth() * 0.48D, entity.getBbHeight() * 0.42D,
                entity.getBbWidth() * 0.48D, 0.025D);
    }

    private static void sendBurst(LivingEntity entity, int tier, int count) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        long time = level.getGameTime();
        DustColorTransitionOptions dust = new DustColorTransitionOptions(
                ExoticFireColorRegistry.vector(ExoticFireColorRegistry.color(tier, time)),
                ExoticFireColorRegistry.vector(ExoticFireColorRegistry.nextColor(tier, time)), 1.15F);
        level.sendParticles(dust, entity.getX(), entity.getY() + entity.getBbHeight() * 0.52D, entity.getZ(),
                count, entity.getBbWidth() * 0.62D, entity.getBbHeight() * 0.50D,
                entity.getBbWidth() * 0.62D, 0.10D);
    }

    public static void reduceNetherHealing(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.getPersistentData().getLong(NETHER_HEAL_REDUCTION_TAG) > entity.level().getGameTime()) {
            event.setAmount(event.getAmount() * 0.50F);
        }
    }

    /** 涅槃异火标记的目标死亡后，为最后施加异火的玩家回复 10% 最大生命。 */
    public static void applyNirvanaKillHeal(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();
        var burn=cn.blockforge.generated.generatedmod.fire.ExoticBurnManager.state(target);
        if(burn==null||!burn.fireId().equals(cn.blockforge.generated.generatedmod.fire.FireCatalog.tier(9).key()))return;
        ServerPlayer owner=cn.blockforge.generated.generatedmod.fire.ExoticBurnManager.owner(target);
        ServerPlayer killer = findKiller(target, event.getSource().getDirectEntity());
        if (owner != null && owner == killer && owner.isAlive()) owner.heal(owner.getMaxHealth() * 0.10F);
    }
}
