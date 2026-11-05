package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.economy.SpiritStoneGrade;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.*;

@GameTestHolder("wuxingxiantu_entry") @PrefixGameTestTemplate(false)
public final class UnifiedSpiritOreGameTests {
    @GameTest(template="empty", timeoutTicks=200)
    public static void randomGradesAndLegacySilk(GameTestHelper h) {
        var ores=new Block[]{WuXingBlocks.SPIRIT_VEIN_ORE.get()};
        var pick=new ItemStack(Items.DIAMOND_PICKAXE);
        var silk=pick.copy();silk.enchant(Enchantments.SILK_TOUCH,1);
        var fortune=pick.copy();fortune.enchant(Enchantments.BLOCK_FORTUNE,3);
        for(var grade:SpiritStoneGrade.values()) h.assertTrue(grade.ore()==ores[0],"Every grade generates the same physical ore");
        for(var ore:ores){
            var pos=h.absolutePos(new BlockPos(1,2,1));h.getLevel().setBlockAndUpdate(pos,ore.defaultBlockState());
            int[] counts=new int[4];boolean multiplied=false;
            for(int i=0;i<2000;i++){
                var drops=Block.getDrops(ore.defaultBlockState(),h.getLevel(),pos,null,null,pick);
                h.assertTrue(drops.size()==1&&drops.get(0).getCount()==1,"Ordinary mining must select exactly one stone grade");
                var grade=SpiritStoneGrade.of(drops.get(0).getItem());h.assertTrue(grade!=null,"Only spirit stones may drop");counts[grade.ordinal()]++;
                var bonus=Block.getDrops(ore.defaultBlockState(),h.getLevel(),pos,null,null,fortune);
                h.assertTrue(bonus.size()==1&&SpiritStoneGrade.of(bonus.get(0).getItem())!=null,"Fortune must keep one grade");
                multiplied|=bonus.get(0).getCount()>1;
            }
            int[] weights={60,25,10,5};
            for(int i=0;i<4;i++)h.assertTrue(Math.abs(counts[i]/20.0-weights[i])<7,"Random grade weights outside tolerance");
            h.assertTrue(multiplied,"Fortune did not increase quantity");
            var drops=Block.getDrops(ore.defaultBlockState(),h.getLevel(),pos,null,null,silk);
            h.assertTrue(drops.size()==1&&drops.get(0).is(WuXingBlocks.SPIRIT_VEIN_ORE_ITEM.get()),"Silk Touch must normalize legacy ore to the unified item");
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void gradedOresDropTheirOwnStoneAndSilkPreservesGrade(GameTestHelper h) {
        var ores = new Block[] { WuXingBlocks.SPIRIT_VEIN_ORE_LOW.get(), WuXingBlocks.SPIRIT_VEIN_ORE_MIDDLE.get(),
                WuXingBlocks.SPIRIT_VEIN_ORE_SUPERIOR.get(), WuXingBlocks.SPIRIT_VEIN_ORE_EXQUISITE.get() };
        var pick = new ItemStack(Items.IRON_PICKAXE);
        var silk = pick.copy();
        silk.enchant(Enchantments.SILK_TOUCH, 1);
        var fortune = pick.copy();
        fortune.enchant(Enchantments.BLOCK_FORTUNE, 3);
        var pos = h.absolutePos(new BlockPos(1, 2, 1));
        for (int i = 0; i < ores.length; i++) {
            var state = ores[i].defaultBlockState();
            var grade = SpiritStoneGrade.values()[i];
            for (var tool : new ItemStack[] { pick, fortune }) {
                var drops = Block.getDrops(state, h.getLevel(), pos, null, null, tool);
                h.assertTrue(drops.size() == 1 && SpiritStoneGrade.of(drops.get(0).getItem()) == grade,
                        "Graded ore must drop only its matching stone grade");
                if (tool == pick) h.assertTrue(drops.get(0).getCount() == 1, "Plain pick must drop one stone");
            }
            var preserved = Block.getDrops(state, h.getLevel(), pos, null, null, silk);
            h.assertTrue(preserved.size() == 1 && preserved.get(0).is(ores[i].asItem()),
                    "Silk Touch must preserve the graded ore block");
        }
        h.succeed();
    }
}
