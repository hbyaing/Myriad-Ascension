package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.alchemy.*;
import cn.blockforge.generated.generatedmod.fixtures.GameTestPlayer;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.gametest.*;

@GameTestHolder("wuxingxiantu_entry") @PrefixGameTestTemplate(false)
public final class AlchemyCompendiumUseGameTests {
    @GameTest(template="empty") public static void introductionTeachesOnceAndRepeatedUseRetainsBook(GameTestHelper h){
        try(var p=new GameTestPlayer(h.getLevel())){
            var item=WuXingItems.ALCHEMY_BOOKS.get(AlchemyCompendium.INTRODUCTION.ordinal()).get();
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
            var first=item.use(h.getLevel(),p,InteractionHand.MAIN_HAND);
            h.assertTrue(first.getResult().consumesAction()&&FormulaKnowledge.unlocked(p)&&p.getMainHandItem().isEmpty(),"Fresh survival introduction did not unlock and consume exactly one book");
            for(var formula:PillCatalog.values())h.assertTrue(!FormulaKnowledge.knows(p,net.minecraft.resources.ResourceLocation.parse("wuxingxiantu:"+formula.id)),"Introduction still grants a formula");
            var knowledge=FormulaKnowledge.understanding(p).copy();
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
            var repeated=item.use(h.getLevel(),p,InteractionHand.MAIN_HAND);
            h.assertTrue(repeated.getResult().consumesAction()&&p.getMainHandItem().getCount()==1,"Already learned book still fails silently or consumes item");
            h.assertTrue(FormulaKnowledge.understanding(p).equals(knowledge),"Rereading granted research concepts or practice");
        }h.succeed();
    }
    @GameTest(template="empty") public static void creativeStudyRetainsBookAndCompleteVolumeKeepsPermissionGate(GameTestHelper h){
        try(var p=new GameTestPlayer(h.getLevel())){
            p.setGameMode(GameType.CREATIVE);
            var item=WuXingItems.ALCHEMY_BOOKS.get(0).get();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
            h.assertTrue(item.use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult().consumesAction()&&p.getMainHandItem().getCount()==1&&FormulaKnowledge.unlocked(p),"Creative study failed or consumed book");
            FormulaKnowledge.reset(p);p.setGameMode(GameType.SURVIVAL);
            var complete=WuXingItems.ALCHEMY_BOOKS.get(AlchemyCompendium.COMPLETE.ordinal()).get();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(complete));
            h.assertTrue(!complete.use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult().consumesAction()&&!FormulaKnowledge.unlocked(p)&&p.getMainHandItem().getCount()==1,"Complete book bypassed permission gate");
        }h.succeed();
    }
}
