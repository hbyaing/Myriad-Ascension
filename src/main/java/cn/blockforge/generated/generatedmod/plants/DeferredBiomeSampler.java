package cn.blockforge.generated.generatedmod.plants;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Caller-thread cache with at most one outstanding, immutable sampling batch.
 * The supplied operation must not access a live world. Null means retry later. */
public final class DeferredBiomeSampler {
    private final Executor executor;
    private final int capacity;
    private final LinkedHashMap<Long,String> cache=new LinkedHashMap<>(256,.75F,true);
    private CompletableFuture<Map<Long,String>> pending;

    public DeferredBiomeSampler(Executor executor,int capacity){
        if(capacity<1)throw new IllegalArgumentException("Empty biome cache");
        this.executor=Objects.requireNonNull(executor);this.capacity=capacity;
    }

    public String poll(long key,Supplier<? extends Collection<Long>> batch,LongFunction<String> sample){
        if(pending!=null&&pending.isDone()){
            var completed=pending;pending=null;
            cache.putAll(completed.getNow(Map.of()));
            while(cache.size()>capacity)cache.remove(cache.keySet().iterator().next());
        }
        String ready=cache.get(key);
        if(ready!=null)return ready;
        if(pending!=null)return null;
        var keys=new LinkedHashSet<Long>();keys.add(key);
        for(long cell:batch.get()){
            if(keys.size()>=Math.min(16,capacity))break;
            if(!cache.containsKey(cell))keys.add(cell);
        }
        var snapshot=List.copyOf(keys);
        try{
            pending=CompletableFuture.supplyAsync(()->{
                Map<Long,String> result=new LinkedHashMap<>();
                for(long cell:snapshot)result.put(cell,Objects.requireNonNull(sample.apply(cell)));
                return Collections.unmodifiableMap(result);
            },executor);
        }catch(RejectedExecutionException busy){
            // Never run rejected work on the tick thread, and never wait for it.
            return null;
        }
        return null;
    }
}
