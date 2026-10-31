package cn.blockforge.generated.generatedmod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Per-dimension successful veins and pending decoration; no seed-only phantom discoveries. */
public final class SpiritVeinData extends SavedData {
    public record Entry(int grade, BlockPos center, int count) {}
    private final Map<Long,Entry> veins=new ConcurrentHashMap<>();
    private final Set<Long> pending=ConcurrentHashMap.newKeySet();
    private final Set<Long> attempted=ConcurrentHashMap.newKeySet();
    private final Set<Long> decorationChunks=ConcurrentHashMap.newKeySet();
    public static SpiritVeinData get(ServerLevel level) {
        synchronized(level.getDataStorage()) {
            return level.getDataStorage().computeIfAbsent(SpiritVeinData::load,SpiritVeinData::new,"wuxingxiantu_spirit_veins");
        }
    }
    /** Only a real feature decoration callback can authorize terrain writes. Old saves fail closed. */
    public void markDecorationChunk(ChunkPos pos) {if(decorationChunks.add(pos.toLong()))setDirty();}
    public boolean canDecorate(ChunkPos pos) {return decorationChunks.contains(pos.toLong());}
    public void queue(ChunkPos pos) {long key=pos.toLong();if(!attempted.contains(key)&&pending.add(key))setDirty();}
    public boolean isPending(ChunkPos pos){return pending.contains(pos.toLong());}
    public Set<Long> pending() {return Set.copyOf(pending);}
    public int grade(int x,int z) {var e=veins.get(ChunkPos.asLong(x,z));return e==null?0:e.grade();}
    public void finish(ChunkPos pos) {pending.remove(pos.toLong());attempted.add(pos.toLong());setDirty();}
    public void register(int x,int z,int grade,BlockPos center,int count) {
        if(grade<1||grade>5||center==null||count<1)throw new IllegalArgumentException("Invalid vein");
        veins.put(ChunkPos.asLong(x,z),new Entry(grade,center.immutable(),count));setDirty();
    }
    public Optional<Entry> nearest(BlockPos pos,int radius) {
        return veins.values().stream().filter(v->v.center().distSqr(pos)<=(double)radius*radius)
            .min(Comparator.comparingDouble(v->v.center().distSqr(pos)));
    }
    public int auraAt(BlockPos pos) {
        int best=0;
        for(int x=(pos.getX()>>4)-3;x<=(pos.getX()>>4)+3;x++)for(int z=(pos.getZ()>>4)-3;z<=(pos.getZ()>>4)+3;z++) {
            var e=veins.get(ChunkPos.asLong(x,z));
            if(e!=null)best=Math.max(best,SpiritVein.auraAt(e.grade(),Math.sqrt(e.center().distSqr(pos))));
        }
        return best;
    }
    public static SpiritVeinData load(CompoundTag tag) {
        var data=new SpiritVeinData();
        for(Tag value:tag.getList("Veins",Tag.TAG_COMPOUND)) {
            var n=(CompoundTag)value;int grade=n.getInt("Grade"),count=n.getInt("Count");
            if(grade>=1&&grade<=5&&count>0&&n.contains("Center"))
                data.veins.put(n.getLong("Chunk"),new Entry(grade,BlockPos.of(n.getLong("Center")),count));
        }
        for(long key:tag.getLongArray("DecorationChunks"))data.decorationChunks.add(key);
        for(long key:tag.getLongArray("Attempted"))data.attempted.add(key);
        for(long key:tag.getLongArray("Pending"))if(!data.attempted.contains(key))data.pending.add(key);
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var list=new ListTag();
        veins.forEach((key,e)->{var n=new CompoundTag();n.putLong("Chunk",key);n.putInt("Grade",e.grade());
            n.putLong("Center",e.center().asLong());n.putInt("Count",e.count());list.add(n);});
        tag.put("Veins",list);tag.putLongArray("Pending",pending.stream().mapToLong(Long::longValue).toArray());
        tag.putLongArray("DecorationChunks",decorationChunks.stream().mapToLong(Long::longValue).toArray());
        tag.putLongArray("Attempted",attempted.stream().mapToLong(Long::longValue).toArray());return tag;
    }
}
