package cn.blockforge.generated.generatedmod.blocks;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.WuXingBlocks;
import cn.blockforge.generated.generatedmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class ThunderstruckWoodBlock extends RotatedPillarBlock implements EntityBlock {
    public ThunderstruckWoodBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RuneAnchor(pos, state);
    }

    /** Logs placed before the rune renderer was added have no block-entity NBT. */
    @SubscribeEvent
    public static void restoreRuneAnchors(ChunkEvent.Load event) {
        if (event.isNewChunk() || !(event.getChunk() instanceof LevelChunk chunk) || chunk.getLevel().isClientSide) return;
        var block = WuXingBlocks.THUNDERSTRUCK_WOOD.get();
        var sections = chunk.getSections();
        var pos = new BlockPos.MutableBlockPos();
        for (int index = 0; index < sections.length; index++) {
            var section = sections[index];
            if (!section.maybeHas(state -> state.is(block))) continue;
            int baseY = chunk.getSectionYFromSectionIndex(index) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (!section.getBlockState(x, y, z).is(block)) continue;
                        pos.set(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
                        if (chunk.getBlockEntity(pos) == null) {
                            chunk.getBlockEntity(pos.immutable(), LevelChunk.EntityCreationType.IMMEDIATE);
                            chunk.setUnsaved(true);
                        }
                    }
                }
            }
        }
    }

    public static final class RuneAnchor extends BlockEntity {
        public RuneAnchor(BlockPos pos, BlockState state) {
            super(ModBlockEntities.THUNDERSTRUCK_WOOD.get(), pos, state);
        }
    }
}
