package cn.blockforge.generated.generatedmod.plants;

import java.util.*;
import java.util.function.LongFunction;

/** Complete four-neighbour connected-component traversal; callers own and persist its state. */
public final class ConnectedBiomeRegion {
    public static long pack(int x,int z){return ((long)x<<32)|(z&0xffffffffL);}
    public static int x(long cell){return (int)(cell>>32);}
    public static int z(long cell){return (int)cell;}
    public static int step(String biome, Set<Long> members, Set<Long> examined, ArrayDeque<Long> frontier,
                           LongFunction<String> sample, int limit, long deadline) {
        return step(biome,members,examined,frontier,sample,limit,deadline,null);
    }
    /** A fixed 256-block tile makes even single-biome worlds terminate. */
    public static int step(String biome, Set<Long> members, Set<Long> examined, ArrayDeque<Long> frontier,
                           LongFunction<String> sample, int limit, long deadline, Long tile) {
        int visited=0;
        while(visited<limit&&!frontier.isEmpty()&&System.nanoTime()<deadline) {
            long at=frontier.peekFirst();
            String found=sample.apply(at);
            if(found==null)break; // Deferred sample: retain the complete frontier.
            frontier.removeFirst();visited++;
            if(!found.equals(biome))continue;
            members.add(at);int x=x(at),z=z(at);
            for(long adjacent:new long[]{pack(x-1,z),pack(x+1,z),pack(x,z-1),pack(x,z+1)}) {
                if(tile!=null && tile(adjacent)!=tile.longValue())continue;
                if(Math.abs((long)x(adjacent))>29_999_984||Math.abs((long)z(adjacent))>29_999_984)continue;
                if(examined.add(adjacent))frontier.addLast(adjacent);
            }
        }
        return visited;
    }
    public static long tile(long cell){return pack(Math.floorDiv(x(cell),256),Math.floorDiv(z(cell),256));}
    private ConnectedBiomeRegion() {}
}
