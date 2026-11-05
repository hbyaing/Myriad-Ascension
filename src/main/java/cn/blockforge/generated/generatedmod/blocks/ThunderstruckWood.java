package cn.blockforge.generated.generatedmod.blocks;

import cn.blockforge.generated.generatedmod.WuXingBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;

import java.util.ArrayDeque;
import java.util.HashSet;

public final class ThunderstruckWood {
    // Vanilla leaves can be supported by a log up to six face-adjacent steps away.
    private static final int MAX_CANOPY_DISTANCE = 6;

    private ThunderstruckWood() {}

    public static void strike(ServerLevel level, BlockPos impact) {
        if (!level.hasChunkAt(impact)) return;
        // Natural lightning usually strikes at the surface boundary. Commands can place
        // the bolt anywhere inside the air block immediately above the surface.
        BlockPos surface = level.isEmptyBlock(impact) ? impact.below() : impact;
        BlockPos log = findPeachWood(level, surface);
        if (log == null) return;

        var state = level.getBlockState(log);
        level.setBlock(log, WuXingBlocks.THUNDERSTRUCK_WOOD.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS)), Block.UPDATE_ALL);
    }

    private static BlockPos findPeachWood(ServerLevel level, BlockPos surface) {
        var queue = new ArrayDeque<BlockPos>();
        var visited = new HashSet<BlockPos>();
        queue.add(surface);
        visited.add(surface);
        for (int distance = 0; distance <= MAX_CANOPY_DISTANCE && !queue.isEmpty(); distance++) {
            int layerSize = queue.size();
            for (int i = 0; i < layerSize; i++) {
                BlockPos pos = queue.remove();
                if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) continue;
                var state = level.getBlockState(pos);
                if (state.is(WuXingBlocks.PEACH_WOOD.get())) return pos;
                if (!state.is(WuXingBlocks.PEACH_LEAVES.get()) || distance == MAX_CANOPY_DISTANCE) continue;

                for (Direction face : Direction.values()) {
                    BlockPos neighbor = pos.relative(face);
                    if (visited.add(neighbor)) queue.add(neighbor);
                }
            }
        }
        return null;
    }
}
