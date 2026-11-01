package cn.blockforge.generated.generatedmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * 炼器台多方块结构的组装管理：玩家用普通方块摆出框架，
 * 结构完整的那一刻整体切换为“成型”外观（符文石点亮纹面、四角磨制黑石砖台阶
 * 替换为角座），拆毁任意部件即恢复为玩家摆放的原始方块。
 *
 * <p>成型只校验方块种类，不校验任何组件的朝向：3x3 平面四面对称，核心所在的那一面即正面，
 * 符文石与角座无论朝哪一面都能成型。
 */
public final class RefiningForgeStructure {
    private static boolean refreshing;
    static boolean mutating;

    private RefiningForgeStructure() {}

    /** 判断某个方块是否属于炼器台结构的可能组件。 */
    public static boolean isComponent(BlockState state) {
        return RefiningForgeBlock.isCore(state)
                || state.is(WuXingBlocks.RUNE_STONE.get())
                || state.is(WuXingBlocks.REFINING_FORGE_CORNER.get())
                || state.is(WuXingBlocks.ETERNAL_SPIRIT_COAL_BLOCK.get())
                || ExoticFireState.isFire(state)
                || isRawCornerSlab(state);
    }

    /** 玩家摆放的角部原始方块：磨制黑石砖台阶，且必须放在下半格。 */
    public static boolean isRawCornerSlab(BlockState state) {
        return state.is(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB)
                && state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM;
    }

    /** 变更点附近搜索核心方块；结构完整则成型，残缺则回退。 */
    public static void refreshAround(Level level, BlockPos changedPos) {
        if (level.isClientSide || refreshing || mutating) return;
        refreshing = true;
        try {
            for (int dy = -2; dy <= 0; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos candidate = changedPos.offset(dx, dy, dz);
                        BlockState state = level.getBlockState(candidate);
                        if (!RefiningForgeBlock.isCore(state)) continue;
                        cn.blockforge.generated.generatedmod.network.service.AlchemyService.wakeAt(
                                (net.minecraft.server.level.ServerLevel)level,candidate);
                        if (RefiningForgeBlock.isStructureComplete(level, candidate))
                            transform((net.minecraft.server.level.ServerLevel) level, candidate);
                        else revert((net.minecraft.server.level.ServerLevel) level, candidate);
                    }
                }
            }
        } finally {
            refreshing = false;
        }
    }

    /** 判断变更点邻域内是否存在炼器台组件，用于过滤无关的邻居更新事件。 */
    public static boolean hasComponentNear(Level level, BlockPos pos) {
        if(!(level instanceof net.minecraft.server.level.ServerLevel server))return false;
        var candidate=new BlockPos.MutableBlockPos();
        for (int dy = -2; dy <= 1; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    candidate.set(pos.getX()+dx,pos.getY()+dy,pos.getZ()+dz);
                    var chunk=server.getChunkSource().getChunkNow(candidate.getX()>>4,candidate.getZ()>>4);
                    if(chunk!=null&&isComponent(chunk.getBlockState(candidate)))return true;
                }
            }
        }
        return false;
    }

    /** 从结构部件位置定位可用的完整结构核心；找不到返回 null。 */
    public static BlockPos findCore(Level level, BlockPos partPos) {
        for (int dy = -2; dy <= 0; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos candidate = partPos.offset(dx, dy, dz);
                    if (RefiningForgeBlock.isCore(level.getBlockState(candidate))
                            && RefiningForgeBlock.isStructureComplete(level, candidate)) return candidate;
                }
            }
        }
        return null;
    }

    private static void transform(net.minecraft.server.level.ServerLevel level, BlockPos corePos) {
        boolean changed = false;
        BlockState core = level.getBlockState(corePos);
        if (!core.getValue(RefiningForgeBlock.FORGED)) {
            level.setBlock(corePos, core.setValue(RefiningForgeBlock.FORGED, true), Block.UPDATE_ALL);
            changed = true;
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos lower = corePos.offset(x, 0, z);
                BlockPos upper = corePos.offset(x, 1, z);
                if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                    BlockState state = level.getBlockState(lower);
                    if (isRawCornerSlab(state)) {
                        level.setBlock(lower, WuXingBlocks.REFINING_FORGE_CORNER.get().defaultBlockState()
                                        .setValue(RefiningForgeCornerBlock.FACING, cornerFacingOf(x, z)),
                                Block.UPDATE_ALL);
                        changed = true;
                    }
                    changed |= forgeRune(level, upper, RuneStoneBlock.ForgePart.UPPER_CORNER,
                            cornerFacing(x, z));
                } else {
                    changed |= forgeRune(level, lower, RuneStoneBlock.ForgePart.LOWER, edgeFacing(x, z));
                    changed |= forgeRune(level, upper, RuneStoneBlock.ForgePart.UPPER, edgeFacing(x, z));
                }
            }
        }
        if (!changed) return;
        level.playSound(null, corePos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.sendParticles(ParticleTypes.END_ROD, corePos.getX() + 0.5D, corePos.getY() + 1.4D,
                corePos.getZ() + 0.5D, 60, 1.4D, 0.5D, 1.4D, 0.02D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, corePos.getX() + 0.5D, corePos.getY() + 1.1D,
                corePos.getZ() + 0.5D, 30, 1.3D, 0.4D, 1.3D, 0.01D);
    }

    /** 边石的外向：-z 北、+x 东、+z 南、-x 西。 */
    private static RuneStoneBlock.ForgeFace edgeFacing(int x, int z) {
        if (z < 0) return RuneStoneBlock.ForgeFace.NORTH;
        if (x > 0) return RuneStoneBlock.ForgeFace.EAST;
        if (z > 0) return RuneStoneBlock.ForgeFace.SOUTH;
        return RuneStoneBlock.ForgeFace.WEST;
    }

    /** 角石的外向（斜向）。 */
    private static RuneStoneBlock.ForgeFace cornerFacing(int x, int z) {
        if (z < 0) return x < 0 ? RuneStoneBlock.ForgeFace.NORTH_WEST
                                : RuneStoneBlock.ForgeFace.NORTH_EAST;
        return x < 0 ? RuneStoneBlock.ForgeFace.SOUTH_WEST : RuneStoneBlock.ForgeFace.SOUTH_EAST;
    }

    /** 角座火盆的外向（斜向），与角石同一套方位语义。 */
    private static RefiningForgeCornerBlock.CornerFacing cornerFacingOf(int x, int z) {
        if (z < 0) return x < 0 ? RefiningForgeCornerBlock.CornerFacing.NORTH_WEST
                                : RefiningForgeCornerBlock.CornerFacing.NORTH_EAST;
        return x < 0 ? RefiningForgeCornerBlock.CornerFacing.SOUTH_WEST
                     : RefiningForgeCornerBlock.CornerFacing.SOUTH_EAST;
    }

    private static boolean forgeRune(net.minecraft.server.level.ServerLevel level, BlockPos pos,
                                     RuneStoneBlock.ForgePart part, RuneStoneBlock.ForgeFace facing) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(WuXingBlocks.RUNE_STONE.get())) return false;
        BlockState target = state.setValue(RuneStoneBlock.FORGED, true)
                .setValue(RuneStoneBlock.PART, part)
                .setValue(RuneStoneBlock.FACING, facing);
        if (state == target) return false;
        level.setBlock(pos, target, Block.UPDATE_ALL);
        return true;
    }

    private static void revert(net.minecraft.server.level.ServerLevel level, BlockPos corePos) {
        boolean changed = false;
        BlockState core = level.getBlockState(corePos);
        if (core.getValue(RefiningForgeBlock.FORGED) || core.getValue(RefiningForgeBlock.LIT)) {
            level.setBlock(corePos, core.setValue(RefiningForgeBlock.FORGED, false)
                    .setValue(RefiningForgeBlock.LIT, false), Block.UPDATE_ALL);
            changed = true;
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                changed |= unforgeRune(level, corePos.offset(x, 0, z));
                changed |= unforgeRune(level, corePos.offset(x, 1, z));
                if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                    BlockPos lower = corePos.offset(x, 0, z);
                    if (level.getBlockState(lower).is(WuXingBlocks.REFINING_FORGE_CORNER.get())) {
                        level.setBlock(lower, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState(),
                                Block.UPDATE_ALL);
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            level.playSound(null, corePos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 0.6F, 1.4F);
        }
    }

    /** 核心被拆毁后无从定位结构：把原核心周围的成形符文与角座手动还原为原始方块。 */
    public static void collapseAround(Level level, BlockPos formerCore) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel) || refreshing) return;
        boolean changed = false;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                changed |= unforgeRune(serverLevel, formerCore.offset(x, 0, z));
                changed |= unforgeRune(serverLevel, formerCore.offset(x, 1, z));
                BlockPos lower = formerCore.offset(x, 0, z);
                if (serverLevel.getBlockState(lower).is(WuXingBlocks.REFINING_FORGE_CORNER.get())) {
                    serverLevel.setBlock(lower, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState(),
                            Block.UPDATE_ALL);
                    changed = true;
                }
            }
        }
        if (changed) {
            level.playSound(null, formerCore, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 0.6F, 1.4F);
        }
    }

    private static boolean unforgeRune(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(WuXingBlocks.RUNE_STONE.get())) return false;
        boolean forged = state.getValue(RuneStoneBlock.FORGED);
        boolean lit = state.getValue(RuneStoneBlock.LIT);
        boolean placed = state.getValue(RuneStoneBlock.PART) == RuneStoneBlock.ForgePart.LOWER
                && state.getValue(RuneStoneBlock.FACING) == RuneStoneBlock.ForgeFace.NORTH;
        if (!forged && !lit && placed) return false;
        level.setBlock(pos, state.setValue(RuneStoneBlock.FORGED, false)
                .setValue(RuneStoneBlock.LIT, false)
                .setValue(RuneStoneBlock.PART, RuneStoneBlock.ForgePart.LOWER)
                .setValue(RuneStoneBlock.FACING, RuneStoneBlock.ForgeFace.NORTH), Block.UPDATE_ALL);
        return true;
    }
}
