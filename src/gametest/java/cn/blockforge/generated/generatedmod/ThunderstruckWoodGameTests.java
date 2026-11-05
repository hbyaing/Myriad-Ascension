package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.blocks.ThunderstruckWoodBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.event.level.ChunkEvent;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ThunderstruckWoodGameTests {
    @GameTest(template = "empty")
    public static void lightningCharsNearestPeachWoodThroughCanopy(GameTestHelper helper) {
        BlockPos canopy = new BlockPos(2, 3, 2);
        BlockPos nearest = canopy.below();
        BlockPos farther = nearest.below();
        var peach = WuXingBlocks.PEACH_WOOD.get();
        var horizontal = peach.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        helper.setBlock(canopy, WuXingBlocks.PEACH_LEAVES.get());
        helper.setBlock(nearest, horizontal);
        helper.setBlock(farther, peach);
        strike(helper, canopy, false);

        helper.runAfterDelay(12, () -> {
            helper.assertBlockPresent(WuXingBlocks.THUNDERSTRUCK_WOOD.get(), nearest);
            helper.assertBlockProperty(nearest, RotatedPillarBlock.AXIS, Direction.Axis.X);
            helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(nearest))
                    instanceof ThunderstruckWoodBlock.RuneAnchor, "Converted wood has no rune anchor");
            helper.assertBlockPresent(peach, farther);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void existingThunderstruckWoodGetsRuneAnchor(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 2, 2);
        BlockPos pos = helper.absolutePos(relative);
        helper.setBlock(relative, WuXingBlocks.THUNDERSTRUCK_WOOD.get());
        var chunk = helper.getLevel().getChunkAt(pos);
        chunk.removeBlockEntity(pos);
        helper.assertTrue(chunk.getBlockEntity(pos) == null, "Legacy fixture unexpectedly has anchor data");

        ThunderstruckWoodBlock.restoreRuneAnchors(new ChunkEvent.Load(chunk, false));

        helper.assertTrue(chunk.getBlockEntity(pos) instanceof ThunderstruckWoodBlock.RuneAnchor,
                "Chunk load did not restore the rune anchor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lightningOnlyCharsPeachWoodOnRealImpact(GameTestHelper helper) {
        BlockPos peach = new BlockPos(1, 2, 1);
        BlockPos visual = new BlockPos(3, 2, 1);
        BlockPos oak = new BlockPos(3, 2, 3);
        BlockPos underRod = new BlockPos(1, 2, 3);
        var peachWood = WuXingBlocks.PEACH_WOOD.get();
        helper.setBlock(peach, peachWood);
        helper.setBlock(visual, peachWood);
        helper.setBlock(oak, Blocks.OAK_LOG);
        helper.setBlock(underRod, peachWood);
        helper.setBlock(underRod.above(), Blocks.LIGHTNING_ROD);

        strike(helper, peach, false);
        strike(helper, visual, true);
        strike(helper, oak, false);
        strike(helper, underRod.above(), false);

        helper.runAfterDelay(8, () -> {
            helper.assertBlockPresent(WuXingBlocks.THUNDERSTRUCK_WOOD.get(), peach);
            helper.assertBlockPresent(peachWood, visual);
            helper.assertBlockPresent(Blocks.OAK_LOG, oak);
            helper.assertBlockPresent(peachWood, underRod);
            helper.succeed();
        });
    }

    private static void strike(GameTestHelper helper, BlockPos hit, boolean visualOnly) {
        var bolt = EntityType.LIGHTNING_BOLT.create(helper.getLevel());
        bolt.setVisualOnly(visualOnly);
        bolt.moveTo(Vec3.atBottomCenterOf(helper.absolutePos(hit.above())));
        helper.getLevel().addFreshEntity(bolt);
    }
}
