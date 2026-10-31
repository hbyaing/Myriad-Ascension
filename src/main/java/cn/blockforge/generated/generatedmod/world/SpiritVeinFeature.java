// Adapted from hbyaing/xiantu r115 (ff253fa), retaining the original vein algorithm.
package cn.blockforge.generated.generatedmod.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 灵脉地物：仅替换地下矿洞侧壁上的岩石，不生成埋藏矿团。
 * <p>是否成脉、脉心在哪、多粗，全部由 {@link SpiritVein} 按种子算出来，
 * 成功生成后登记，供灵气场和探测器共用。</p>
 */
public class SpiritVeinFeature extends Feature<NoneFeatureConfiguration> {

    public SpiritVeinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        ChunkPos candidate = new ChunkPos(origin);
        SpiritVeinData.get(level.getLevel()).markDecorationChunk(candidate);
        if (!SpiritVein.hasVein(level.getSeed(), candidate.x, candidate.z)) return false;
        SpiritVeinData.get(level.getLevel()).queue(candidate);
        return true;
    }
    public static boolean generate(WorldGenLevel level, BlockPos origin) {
        var scan=new Scan(level,origin);
        while(!scan.advance(Integer.MAX_VALUE))if(!scan.ready())return false;
        return scan.result();
    }

    /** Same deterministic vein selection, with a bounded resumable cave scan. */
    public static final class Scan {
        private final WorldGenLevel level;
        private final SpiritVeinData data;
        private final ChunkPos chunk;
        private final int requested,minX,minZ,minY,height,total;
        private final java.util.NavigableSet<BlockPos> walls=new java.util.TreeSet<>(java.util.Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
        private final java.util.Set<BlockPos> remaining=new java.util.HashSet<>();
        private final java.util.ArrayDeque<BlockPos> frontier=new java.util.ArrayDeque<>();
        private java.util.List<BlockPos> component=new java.util.ArrayList<>(),largest=new java.util.ArrayList<>();
        private int cursor,lastBatchWork;private boolean done,result;
        private final BlockPos.MutableBlockPos position=new BlockPos.MutableBlockPos();
        public Scan(WorldGenLevel level,BlockPos origin){
            this.level=level;chunk=new ChunkPos(origin);data=SpiritVeinData.get(level.getLevel());
            requested=SpiritVein.richness(level.getSeed(),chunk.x,chunk.z);
            minX=chunk.getMinBlockX()-15;minZ=chunk.getMinBlockZ()-15;
            minY=Math.max(SpiritVein.MIN_Y,level.getMinBuildHeight()+1);height=Math.max(0,SpiritVein.MAX_Y-minY+1);total=46*46*height;
            done=!SpiritVein.hasVein(level.getSeed(),chunk.x,chunk.z)||!data.canDecorate(chunk)||height==0;
        }
        public ChunkPos chunk(){return chunk;}
        public boolean result(){return result;}
        public int lastBatchWork(){return lastBatchWork;}
        public boolean ready(){
            if(!(level instanceof net.minecraft.server.level.ServerLevel server))return true;
            for(int x=chunk.x-1;x<=chunk.x+1;x++)for(int z=chunk.z-1;z<=chunk.z+1;z++)
                if(server.getChunkSource().getChunkNow(x,z)==null)return false;
            return true;
        }
        public boolean advance(int budget){
            lastBatchWork=0;if(done)return true;if(!ready())return false;
            long deadline=System.nanoTime()+2_000_000;
            while(cursor<total&&lastBatchWork<Math.max(1,budget)){
                if((lastBatchWork&63)==0&&lastBatchWork>0&&System.nanoTime()>=deadline)break;
                int x=minX+cursor/(46*height),z=minZ+(cursor/height)%46,y=minY+cursor%height;
                lastBatchWork++;
                if(!data.canDecorate(new ChunkPos(x>>4,z>>4))){cursor+=height-cursor%height;continue;}
                cursor++;position.set(x,y,z);BlockState state=level.getBlockState(position);
                if((state.is(BlockTags.STONE_ORE_REPLACEABLES)||state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES))&&isCaveWall(level,position)){
                    BlockPos wall=position.immutable();walls.add(wall);remaining.add(wall);
                }
            }
            if(cursor<total)return false;
            // Connectivity is also resumable; a dense cave cannot move the entire BFS into one tick.
            while(lastBatchWork<Math.max(1,budget)){
                if((lastBatchWork&63)==0&&lastBatchWork>0&&System.nanoTime()>=deadline)return false;
                if(frontier.isEmpty()){
                    if(component.size()>largest.size())largest=component;
                    component=new java.util.ArrayList<>();
                    if(walls.isEmpty()){result=finish();done=true;remaining.clear();return true;}
                    BlockPos first=walls.pollFirst();lastBatchWork++;
                    if(remaining.remove(first))frontier.add(first);
                }else{
                    BlockPos p=frontier.removeFirst();component.add(p);lastBatchWork++;
                    for(Direction d:Direction.values()){BlockPos n=p.relative(d);if(remaining.remove(n))frontier.addLast(n);}
                }
            }
            return false;
        }
        private boolean finish(){
        // 只选六面相邻的最大连续洞壁；BFS前缀本身也保持连通。
        int minimum=requested==5?256:64;
        if(largest.size()<minimum)return false;
        int target=new int[]{96,144,192,255,512}[requested-1];
        java.util.List<BlockPos> ores=largest.subList(0,Math.min(target,largest.size()));
        int actual=requested==5?5:ores.size()>=192?4:ores.size()>=144?3:ores.size()>=96?2:1;
        // A player may edit terrain between scan batches. Preflight every target before any write.
        for(BlockPos p:ores){
            BlockState state=level.getBlockState(p);
            if(!(state.is(BlockTags.STONE_ORE_REPLACEABLES)||state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES))||!isCaveWall(level,p))return false;
        }
        for(int i=0;i<ores.size();i++) {
            level.setBlock(ores.get(i),cn.blockforge.generated.generatedmod.WuXingBlocks.SPIRIT_VEIN_ORE.get().defaultBlockState(),2);
        }
        // 脉心取实际矿带内、靠近质心的一块矿，不再指向随机岩层坐标。
        double ax=ores.stream().mapToInt(BlockPos::getX).average().orElseThrow(),ay=ores.stream().mapToInt(BlockPos::getY).average().orElseThrow(),az=ores.stream().mapToInt(BlockPos::getZ).average().orElseThrow();
        BlockPos center=ores.stream().min(java.util.Comparator.comparingDouble(p->(p.getX()-ax)*(p.getX()-ax)+(p.getY()-ay)*(p.getY()-ay)+(p.getZ()-az)*(p.getZ()-az))).orElseThrow();
        SpiritVeinData.get(level.getLevel()).register(chunk.x,chunk.z,actual,center,ores.size());
        return true;
        }
    }

    /** 必须是地表下的侧壁，水平邻接至少两格高的干燥洞内空气。 */
    private static boolean isCaveWall(WorldGenLevel level, BlockPos pos) {
        if (level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ()) - pos.getY() < 8) return false;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos air = pos.relative(side);
            if (level.getBlockState(air).isAir() && level.getBlockState(air.above()).isAir()) return true;
        }
        return false;
    }

}
