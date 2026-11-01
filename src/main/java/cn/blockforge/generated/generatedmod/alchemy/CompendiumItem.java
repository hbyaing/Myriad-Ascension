package cn.blockforge.generated.generatedmod.alchemy;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CompendiumItem extends Item {
    private final AlchemyCompendium compendium;
    public CompendiumItem(AlchemyCompendium compendium, Properties properties) { super(properties); this.compendium = compendium; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (!(user instanceof ServerPlayer player)) return InteractionResultHolder.success(stack);
        if (compendium == AlchemyCompendium.COMPLETE && !player.isCreative() && !player.hasPermissions(2))
        {
            player.displayClientMessage(Component.translatable("message.wuxingxiantu.compendium_admin_only"), true);
            return InteractionResultHolder.fail(stack);
        }
        boolean changed = FormulaKnowledge.unlock(player);
        int learned=0;
        for (PillCatalog pill : compendium.formulas())
            if(FormulaKnowledge.learn(player, new ResourceLocation(GeneratedMod.MOD_ID, pill.id),false))learned++;
        changed |= learned>0;
        if (!changed) {
            player.displayClientMessage(Component.translatable("message.wuxingxiantu.compendium_already_learned"), true);
            return InteractionResultHolder.consume(stack);
        }
        Component bookName = stack.getHoverName();
        if (!player.isCreative()) stack.shrink(1);
        player.displayClientMessage(compendium == AlchemyCompendium.INTRODUCTION
                ? Component.translatable("message.wuxingxiantu.alchemy_introduction_unlocked")
                : Component.translatable("message.wuxingxiantu.compendium_learned",bookName,learned), true);
        player.displayClientMessage(Component.translatable("message.wuxingxiantu.compendium_research_next"), false);
        return InteractionResultHolder.consume(stack);
    }
    @Override public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack,level,lines,flag);
        lines.add(Component.translatable("tooltip.wuxingxiantu.compendium_use").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
