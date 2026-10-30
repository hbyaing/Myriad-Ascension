package cn.blockforge.generated.generatedmod.items;

import cn.blockforge.generated.generatedmod.SpiritEnchantmentLogic;
import cn.blockforge.generated.generatedmod.yujian.NurtureData;
import cn.blockforge.generated.generatedmod.yujian.SwordProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** 本命灵器剑：保留近战、品阶、剑灵力与养成记录，不提供脱手投射。 */
public class SpiritSwordItem extends SwordItem {
    private static final String TIER_TAG = "tier";
    private static final String SPIRIT_TAG = "swordSpirit";
    private static final String MAX_SPIRIT_TAG = "maxSpirit";
    private static final String KILLS_TAG = "kills";
    private static final String REFINEMENTS_TAG = "refinements";
    private static final String BONDED_TAG = "isSpiritSword";
    private static final String HEART_FORGED_TAG = "HeartForged";
    private static final String HEART_OWNER_TAG = "HeartOwner";

    private final String tierId;
    private final float baseDamage;
    private final int maxSpirit;
    private final int spiritCost;
    private final int techniquePierceCount;

    public SpiritSwordItem(Tier tier, float damage, Properties properties, String tierId,
                           int maxSpirit, int ignoredRange, int spiritCost, int pierceCount) {
        super(tier, Math.max(0, Math.round(damage - 1.0F - tier.getAttackDamageBonus())), -2.4F, properties);
        this.tierId = tierId;
        this.baseDamage = damage;
        this.maxSpirit = maxSpirit;
        this.spiritCost = spiritCost;
        this.techniquePierceCount = pierceCount;
    }

    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new cn.blockforge.generated.generatedmod.client.FlyingSwordItemRenderer();
                return renderer;
            }
        });
    }

    public int maxSpirit() { return maxSpirit; }
    public int spiritCost() { return spiritCost; }
    public int techniquePierceCount() { return techniquePierceCount; }
    public String tierId() { return tierId; }
    public SwordProfile profile() { return SwordProfile.of(tierId); }
    public static NurtureData nurture(ItemStack stack) { return NurtureData.read(stack); }
    public static void saveNurture(ItemStack stack, NurtureData data) {
        if (stack.getItem() instanceof SpiritSwordItem sword) data.write(stack, sword.profile());
    }
    public int effectiveSpiritCost(ItemStack stack) {
        return SpiritEnchantmentLogic.weaponAuraCost(spiritCost, stack);
    }

    public static boolean isSword(ItemStack stack) { return stack.getItem() instanceof SpiritSwordItem; }
    public static boolean isHeartForged(ItemStack stack) {
        return isSword(stack) && stack.hasTag() && stack.getTag().getBoolean(HEART_FORGED_TAG)
                && stack.getTag().hasUUID(HEART_OWNER_TAG);
    }
    public static java.util.UUID heartOwner(ItemStack stack) {
        return isHeartForged(stack) ? stack.getTag().getUUID(HEART_OWNER_TAG) : null;
    }
    public static void setHeartForged(ItemStack stack, java.util.UUID owner) {
        if (!isSword(stack) || owner == null) return;
        stack.getOrCreateTag().putBoolean(HEART_FORGED_TAG, true);
        stack.getOrCreateTag().putUUID(HEART_OWNER_TAG, owner);
    }
    public static void clearHeartForge(ItemStack stack) {
        if (!stack.hasTag()) return;
        stack.getTag().remove(HEART_FORGED_TAG);
        stack.getTag().remove(HEART_OWNER_TAG);
        stack.getTag().remove(cn.blockforge.generated.generatedmod.combat.HeartForgeContracts.TAG);
    }
    @Override public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        return cn.blockforge.generated.generatedmod.combat.HeartForgeContracts.visible(stack) ? Component.translatable("item.wuxingxiantu.heart_forged_prefix").withStyle(net.minecraft.ChatFormatting.AQUA).append(base) : base;
    }
    public static int getKills(ItemStack stack) { return stack.getOrCreateTag().getInt(KILLS_TAG); }
    public static int getRefinements(ItemStack stack) { return stack.getOrCreateTag().getInt(REFINEMENTS_TAG); }
    public static int getSpirit(ItemStack stack) { return stack.getOrCreateTag().getInt(SPIRIT_TAG); }
    public static int getMaxSpirit(ItemStack stack) {
        return stack.getOrCreateTag().contains(MAX_SPIRIT_TAG)
                ? stack.getOrCreateTag().getInt(MAX_SPIRIT_TAG)
                : stack.getItem() instanceof SpiritSwordItem sword ? sword.maxSpirit : 0;
    }

    public static void setSpirit(ItemStack stack, int value) {
        if (!(stack.getItem() instanceof SpiritSwordItem sword)) return;
        stack.getOrCreateTag().putInt(SPIRIT_TAG, Math.max(0, Math.min(sword.maxSpirit, value)));
        stack.getOrCreateTag().putInt(MAX_SPIRIT_TAG, sword.maxSpirit);
    }

    public static int addSpirit(ItemStack stack, int amount) {
        if (!(stack.getItem() instanceof SpiritSwordItem sword) || amount <= 0) return amount;
        int current = getSpirit(stack);
        int accepted = Math.min(amount, Math.max(0, sword.maxSpirit - current));
        setSpirit(stack, current + accepted);
        return amount - accepted;
    }

    public static void addKill(ItemStack stack) {
        if (isSword(stack)) {
            migrateKillCount(stack);
            stack.getOrCreateTag().putInt(KILLS_TAG, getKills(stack) + 1);
        }
    }
    private static void migrateKillCount(ItemStack stack) {
        var tag=stack.getOrCreateTag();
        if(tag.getInt("KillCountVersion")>=1)return;
        tag.putInt("LegacyHitCount",tag.getInt(KILLS_TAG));
        tag.putInt(KILLS_TAG,0);tag.putInt("KillCountVersion",1);
    }

    public void initialize(ItemStack stack) {
        migrateKillCount(stack);
        var tag = stack.getOrCreateTag();
        cn.blockforge.generated.generatedmod.alchemy.LegacyStackMigration.removeLegacyEnchantment(tag);
        tag.remove("Damage");
        if (!tag.contains(TIER_TAG)) tag.putString(TIER_TAG, tierId);
        if (!tag.contains(BONDED_TAG)) tag.putBoolean(BONDED_TAG, true);
        if (!tag.contains(MAX_SPIRIT_TAG)) tag.putInt(MAX_SPIRIT_TAG, maxSpirit);
        if (!tag.contains(SPIRIT_TAG)) tag.putInt(SPIRIT_TAG, maxSpirit);
        if (!tag.contains(KILLS_TAG)) tag.putInt(KILLS_TAG, 0);
        if (!tag.contains(REFINEMENTS_TAG)) tag.putInt(REFINEMENTS_TAG, 0);
    }

    @Override public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity,
                                        int slot, boolean selected) {
        if (!level.isClientSide) {
            initialize(stack);
            if (!stack.getTag().contains(NurtureData.TAG)) saveNurture(stack, nurture(stack));
            if (entity instanceof net.minecraft.server.level.ServerPlayer player
                    && (!stack.getTag().getCompound(NurtureData.TAG).getBoolean("SwordKnowledgeMigrated")
                        || player.tickCount % 20 == 0 && player.getPersistentData().contains("SwordLegacyTechniques")))
                cn.blockforge.generated.generatedmod.yujian.SwordNurture.migrate(player, stack);
        }
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    @Override public int getEnchantmentValue() { return 20; }

    @Override public boolean isEnchantable(ItemStack stack) {
        // This item-level capability also lets infusers edit existing enchants.
        // Vanilla ItemStack.isEnchantable separately rejects already enchanted stacks.
        return stack.getCount() == 1;
    }
    @Override public boolean isFoil(ItemStack stack) { return cn.blockforge.generated.generatedmod.combat.HeartForgeContracts.visible(stack) || super.isFoil(stack); }

    @Override public boolean canApplyAtEnchantingTable(ItemStack stack,
            net.minecraft.world.item.enchantment.Enchantment enchantment) {
        return enchantment != net.minecraft.world.item.enchantment.Enchantments.UNBREAKING
                && enchantment != net.minecraft.world.item.enchantment.Enchantments.MENDING
                && super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(book).keySet().stream()
                .anyMatch(enchantment -> canApplyAtEnchantingTable(stack, enchantment));
    }

    // TieredItem supplies tier durability even without Properties.durability().
    @Override public int getMaxDamage(ItemStack stack) { return 0; }
    @Override public boolean isDamageable(ItemStack stack) { return false; }
    @Override public void setDamage(ItemStack stack, int damage) {
        if (stack.hasTag()) stack.getTag().remove("Damage");
    }
    @Override public boolean mineBlock(ItemStack stack, Level level,
            net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos, LivingEntity miner) {
        initialize(stack);
        return true;
    }

    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        initialize(stack);
        if(!target.isAlive())addKill(stack);
        return true;
    }

    public float attackDamage(LivingEntity target) {
        return target.getMobType() == MobType.UNDEAD ? baseDamage * 1.5F : baseDamage;
    }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        initialize(stack);
        tooltip.add(Component.translatable("tooltip.wuxingxiantu.sword_spirit", getSpirit(stack), maxSpirit));
        tooltip.add(Component.translatable("tooltip.wuxingxiantu.sword_technique", effectiveSpiritCost(stack)));
        tooltip.add(Component.translatable("tooltip.wuxingxiantu.sword_pierce",
                techniquePierceCount >= 99 ? "∞" : techniquePierceCount));
        tooltip.add(Component.translatable("tooltip.wuxingxiantu.sword_undead"));
        var flight=cn.blockforge.generated.generatedmod.yujian.FlyingSwordRegistry.get(stack);
        if(flight!=null){
            var lingkongLevel=profile().lingkongLevel();
            if(cn.blockforge.generated.generatedmod.yujian.HuahongFlight.supported(profile().tier()))
                tooltip.add(Component.translatable("tooltip.wuxingxiantu.huahong"));
            flight.advanced().capabilities().stream().sorted().forEach(phase -> {
                if(phase==cn.blockforge.generated.generatedmod.yujian.YujianPhase.BASIC
                        ||phase==cn.blockforge.generated.generatedmod.yujian.YujianPhase.ACCELERATING)return;
                if(phase==cn.blockforge.generated.generatedmod.yujian.YujianPhase.LINGKONG)
                    tooltip.add(Component.translatable("tooltip.wuxingxiantu.yujian_phase_lingkong",roman(lingkongLevel),lingkongLevel*25));
                else tooltip.add(Component.translatable(phase.translationKey()+".detail"));
                if(phase==cn.blockforge.generated.generatedmod.yujian.YujianPhase.LINGKONG && flight.advanced().jinghong())
                    tooltip.add(Component.translatable("yujian.wuxingxiantu.passive.jinghong.detail"));
            });
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    private static String roman(int value){
        return switch(value){case 1->"I";case 2->"II";case 3->"III";case 4->"IV";case 5->"V";case 6->"VI";default->"-";};
    }

    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getSpirit(stack) / Math.max(1, getMaxSpirit(stack)));
    }
    @Override public int getBarColor(ItemStack stack) { return 0xE7B8FF; }
}
