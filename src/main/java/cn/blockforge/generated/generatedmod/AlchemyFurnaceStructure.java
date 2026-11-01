package cn.blockforge.generated.generatedmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** 5 x 5 x 4 blueprint, relative to the bottom-center core. */
public final class AlchemyFurnaceStructure {
    private static boolean refreshing;
    static boolean mutating;
    private AlchemyFurnaceStructure() {}
    public static final Direction[] INWARDS = {Direction.SOUTH};
    public static Direction perp(Direction inward) { return inward.getCounterClockWise(); }
    public static Direction inwardOf(BlockState state) { return state.hasProperty(AlchemyFurnaceBlock.FACING)
            ? state.getValue(AlchemyFurnaceBlock.FACING).getOpposite() : Direction.SOUTH; }
    public static BlockPos offset(BlockPos core, Direction inward, int x, int z, int y) { return core.offset(x,y,z); }

    public static BlockState expected(int x, int z, int y) {
        boolean rim = Math.abs(x)==2 || Math.abs(z)==2;
        boolean center = x==0 && z==0;
        if (y==0) {
            if (center) return null;
            return (rim ? Blocks.GOLD_BLOCK : WuXingBlocks.ETERNAL_SPIRIT_COAL_BLOCK.get()).defaultBlockState();
        }
        if (y==3) return (center ? Blocks.AIR : Blocks.GOLD_BLOCK).defaultBlockState();
        if (!rim) return Blocks.AIR.defaultBlockState();
        boolean midpoint = x==0 || z==0;
        if (y==1 && midpoint) {
            Direction facing = x==-2 ? Direction.WEST : x==2 ? Direction.EAST : z==-2 ? Direction.NORTH : Direction.SOUTH;
            return Blocks.BLAST_FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,facing);
        }
        return ((Math.abs(x)==2 && Math.abs(z)==2) || midpoint
                ? Blocks.GOLD_BLOCK : WuXingBlocks.RUNE_STONE.get()).defaultBlockState();
    }
    public static boolean accepts(int x, int z, int y, BlockState actual) {
        BlockState want=expected(x,z,y);
        if (want==null) return AlchemyFurnaceBlock.isCore(actual);
        if (want.isAir()) return actual.isAir();
        if (!actual.is(want.getBlock())) return false;
        return !want.is(Blocks.BLAST_FURNACE) || actual.getValue(BlockStateProperties.HORIZONTAL_FACING)
                ==want.getValue(BlockStateProperties.HORIZONTAL_FACING);
    }
    private static BlockState material(Level level, BlockPos pos, BlockPos core) {
        if (level.getBlockEntity(pos) instanceof AlchemyFurnacePartEntity part && part.core.equals(core)) return part.original;
        return level.getBlockState(pos);
    }
    public static boolean isComponent(BlockState state) {
        return AlchemyFurnaceBlock.isCore(state) || state.is(WuXingBlocks.ALCHEMY_FURNACE_PART.get())
                || state.is(Blocks.GOLD_BLOCK) || state.is(Blocks.BLAST_FURNACE)
                || state.is(WuXingBlocks.RUNE_STONE.get()) || state.is(WuXingBlocks.ETERNAL_SPIRIT_COAL_BLOCK.get());
    }
    public static Vec3 bodyCenter(Level level, BlockPos core) { return Vec3.atCenterOf(core); }
    private static boolean loaded(Level level, BlockPos core) {
        return level.hasChunkAt(core.offset(-2,0,-2)) && level.hasChunkAt(core.offset(2,0,2))
                && level.hasChunkAt(core.offset(-2,0,2)) && level.hasChunkAt(core.offset(2,0,-2));
    }
    public static boolean matches(Level level, BlockPos core, Direction inward) { return loaded(level,core) && missingCount(level,core)==0; }
    public static Direction findInward(Level level, BlockPos core) {
        return AlchemyFurnaceBlock.isCore(level.getBlockState(core)) && matches(level,core,Direction.SOUTH) ? Direction.SOUTH : null;
    }
    public static Direction bestInward(Level level, BlockPos core) { return Direction.SOUTH; }
    public static int missingCount(Level level, BlockPos core) {
        int missing=0;
        for (int x=-2;x<=2;x++) for (int z=-2;z<=2;z++) for (int y=0;y<4;y++)
            if (!accepts(x,z,y,material(level,core.offset(x,y,z),core))) missing++;
        return missing;
    }
    public static void markDirtyAround(Level level,BlockPos changed) {
        if(!(level instanceof net.minecraft.server.level.ServerLevel server)||refreshing||mutating)return;
        var core=new BlockPos.MutableBlockPos();
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-3;y<=0;y++) {
            core.set(changed.getX()+x,changed.getY()+y,changed.getZ()+z);
            var chunk=server.getChunkSource().getChunkNow(core.getX()>>4,core.getZ()>>4);
            if(chunk!=null&&chunk.getBlockEntity(core) instanceof AlchemyFurnaceBlockEntity furnace)
                furnace.markStructureDirty();
        }
    }
    public static void refreshAround(Level level, BlockPos changed) {
        if (level.isClientSide || refreshing || mutating) return;
        refreshing=true;
        try {
            for (int x=-2;x<=2;x++) for (int z=-2;z<=2;z++) for (int y=-3;y<=0;y++) {
                BlockPos core=changed.offset(x,y,z);
                if (level.hasChunkAt(core) && AlchemyFurnaceBlock.isCore(level.getBlockState(core)) && loaded(level,core)) refresh(level,core);
            }
        } finally { refreshing=false; }
    }
    public static boolean hasComponentNear(Level level, BlockPos pos) {
        for (int x=-2;x<=2;x++) for (int z=-2;z<=2;z++) for (int y=-3;y<=0;y++) {
            BlockPos core=pos.offset(x,y,z);
            if (level.hasChunkAt(core) && AlchemyFurnaceBlock.isCore(level.getBlockState(core))) return true;
        }
        return false;
    }
    public static BlockPos findCore(Level level, BlockPos part) {
        for (int x=-2;x<=2;x++) for (int z=-2;z<=2;z++) for (int y=-3;y<=0;y++) {
            BlockPos core=part.offset(x,y,z);
            BlockState want=expected(-x,-z,-y);
            if ((want==null || !want.isAir()) && level.hasChunkAt(core) && AlchemyFurnaceBlock.isFormed(level.getBlockState(core))) return core;
        }
        return null;
    }
    public static boolean isComplete(Level level, BlockPos core) { return findInward(level,core)!=null; }

    private static void refresh(Level level, BlockPos core) {
        BlockState state=level.getBlockState(core);
        if (!matches(level,core,Direction.SOUTH)) {
            if (AlchemyFurnaceBlock.isFormed(state)) dismantle(level,core,null);
            return;
        }
        mutating=true;
        try {
            for (int x=-2;x<=2;x++) for (int z=-2;z<=2;z++) for (int y=0;y<4;y++) {
                BlockState wanted=expected(x,z,y);
                if (wanted==null) continue;
                BlockPos pos=core.offset(x,y,z);
                if (level.getBlockEntity(pos) instanceof AlchemyFurnacePartEntity) continue;
                BlockState original=level.getBlockState(pos);
                BlockEntity originalEntity=level.getBlockEntity(pos);
                var data=originalEntity==null ? null : originalEntity.saveWithFullMetadata();
                level.removeBlockEntity(pos); // Do not spill a furnace inventory when hiding it.
                // Air placeholders carry only model-derived collision, never a solid seal.
                level.setBlock(pos,WuXingBlocks.ALCHEMY_FURNACE_PART.get().defaultBlockState()
                        .setValue(AlchemyFurnacePartBlock.SEAL,wanted.isAir()),Block.UPDATE_ALL);
                if (!(level.getBlockEntity(pos) instanceof AlchemyFurnacePartEntity part)) throw new IllegalStateException("Missing furnace part entity");
                part.core=core.immutable(); part.coreTurn=AlchemyFurnaceCollision.turn(level.getBlockState(core)); part.original=original; part.originalData=data; part.setChanged();
                level.sendBlockUpdated(pos,part.getBlockState(),part.getBlockState(),Block.UPDATE_CLIENTS);
            }
            if (!AlchemyFurnaceBlock.isFormed(state)) level.setBlock(core,state.setValue(AlchemyFurnaceBlock.FORMED,true),Block.UPDATE_ALL);
        } finally { mutating=false; }
    }
    public static void restore(Level level, AlchemyFurnacePartEntity part) {
        boolean previous=mutating; mutating=true;
        try {
            BlockPos pos=part.getBlockPos();
            level.setBlock(pos,part.original,Block.UPDATE_ALL);
            BlockEntity restored=level.getBlockEntity(pos);
            if (part.originalData!=null && restored!=null) { restored.load(part.originalData.copy()); restored.setChanged(); }
        } finally { mutating=previous; }
    }
    public static void dismantle(Level level, BlockPos core, BlockPos broken) {
        if (level.isClientSide || mutating) return;
        mutating=true;
        try {
            BlockState state=level.getBlockState(core);
            if (AlchemyFurnaceBlock.isFormed(state)) {
                level.setBlock(core,state.setValue(AlchemyFurnaceBlock.FORMED,false),Block.UPDATE_ALL);
                if (level instanceof net.minecraft.server.level.ServerLevel server
                        && (!(level.getBlockEntity(core) instanceof AlchemyFurnaceBlockEntity active)||!active.v5Batch())) Network.cancelAlchemyAt(server,core);
            }
            for (int x=-2;x<=2;x++) for (int z=-2;z<=2;z++) for (int y=0;y<4;y++) {
                BlockPos pos=core.offset(x,y,z);
                if (pos.equals(broken) || !level.hasChunkAt(pos)) continue;
                if (level.getBlockEntity(pos) instanceof AlchemyFurnacePartEntity part && part.core.equals(core)) restore(level,part);
            }
        } finally { mutating=false; }
    }
}
