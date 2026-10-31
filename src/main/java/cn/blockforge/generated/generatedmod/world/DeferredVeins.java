package cn.blockforge.generated.generatedmod.world;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Decoration is deferred until neighboring full chunks exist; never force-load terrain. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID)
public final class DeferredVeins {
    private static final java.util.Map<ServerLevel,SpiritVeinFeature.Scan> JOBS=new java.util.HashMap<>();
    private static final java.util.Map<ServerLevel,java.util.ArrayDeque<Long>> CANDIDATES=new java.util.HashMap<>();
    public static boolean tryGenerate(ServerLevel level,ChunkPos pos) {
        var data=SpiritVeinData.get(level);
        if(!data.isPending(pos))return false;
        for(int x=pos.x-1;x<=pos.x+1;x++)for(int z=pos.z-1;z<=pos.z+1;z++)
            if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        if(data.grade(pos.x,pos.z)==0)SpiritVeinFeature.generate(level,new BlockPos(pos.x*16+8,0,pos.z*16+8));
        data.finish(pos);return true;
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel level))return;
        var data=SpiritVeinData.get(level);var scan=JOBS.get(level);
        if(scan!=null&&!scan.ready()){JOBS.remove(level);scan=null;}
        if(scan==null){
            var queue=CANDIDATES.computeIfAbsent(level,k->new java.util.ArrayDeque<>());
            if(level.getGameTime()%20==0){
                var queued=new java.util.HashSet<>(queue);
                for(long key:data.pending())if(queued.add(key))queue.addLast(key);
            }
            int attempts=Math.min(16,queue.size());
            while(attempts-->0){
                var pos=new ChunkPos(queue.removeFirst());if(!data.isPending(pos))continue;
                var candidate=new SpiritVeinFeature.Scan(level,new BlockPos(pos.x*16+8,0,pos.z*16+8));
                if(!candidate.ready()){queue.addLast(pos.toLong());continue;}
                if(data.grade(pos.x,pos.z)>0){data.finish(pos);continue;}
                scan=candidate;JOBS.put(level,scan);break;
            }
        }
        if(scan!=null&&scan.advance(1024)){data.finish(scan.chunk());JOBS.remove(level);}
    }
    @SubscribeEvent public static void unload(net.minecraftforge.event.level.LevelEvent.Unload event){
        if(event.getLevel() instanceof ServerLevel level){JOBS.remove(level);CANDIDATES.remove(level);}
    }
    private DeferredVeins(){}
}
