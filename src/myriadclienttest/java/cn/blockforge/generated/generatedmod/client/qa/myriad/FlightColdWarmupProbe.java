package cn.blockforge.generated.generatedmod.client.qa.myriad;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

/** Read-only QA warmup observations. Never requests terrain, edits options,
 * changes queues or sleeps. A failed observation cannot count as stable. */
public final class FlightColdWarmupProbe {
    public static Map<String,Object> snapshot(Minecraft mc){
        var row=new LinkedHashMap<String,Object>();var errors=new ArrayList<String>();
        row.put("nanoTime",System.nanoTime());row.put("levelPresent",mc.level!=null);
        row.put("activity",FlightChunkWorkTimings.activitySnapshot());
        if(mc.level==null){row.put("error","No client level");return row;}
        row.put("levelIdentity",System.identityHashCode(mc.level));
        row.put("dimension",mc.level.dimension().location().toString());
        try{row.put("lightQueueEmpty",mc.level.isLightUpdateQueueEmpty());row.put("clientLoadedChunks",mc.level.getChunkSource().getLoadedChunksCount());}
        catch(Throwable error){errors.add("client chunks/light: "+message(error));}
        try{
            if(net.minecraftforge.fml.ModList.get().isLoaded("embeddium"))embeddium(mc,row);
            else vanilla(mc,row);
        }catch(Throwable error){errors.add("renderer: "+message(error));}
        if(!errors.isEmpty())row.put("error",String.join("; ",errors));
        return row;
    }
    private static void embeddium(Minecraft mc,Map<String,Object> row)throws Exception{
        row.put("rendererBackend","Embeddium");
        Class<?> type=Class.forName("me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer",false,mc.getClass().getClassLoader());
        Object renderer=type.getMethod("instanceNullable").invoke(null);
        if(renderer==null)throw new IllegalStateException("Embeddium renderer not initialized");
        row.put("terrainComplete",type.getMethod("isTerrainRenderComplete").invoke(renderer));
        row.put("rendererDebugStrings",strings(type.getMethod("getDebugStrings").invoke(renderer)));
        Field managerField=type.getDeclaredField("renderSectionManager");managerField.setAccessible(true);Object manager=managerField.get(renderer);
        if(manager==null)throw new IllegalStateException("Embeddium section manager not initialized");
        Object builder=manager.getClass().getMethod("getBuilder").invoke(manager);Class<?> b=builder.getClass();
        row.put("buildQueueEmpty",b.getMethod("isBuildQueueEmpty").invoke(builder));
        row.put("scheduledJobs",b.getMethod("getScheduledJobCount").invoke(builder));
        row.put("busyThreads",b.getMethod("getBusyThreadCount").invoke(builder));
        row.put("builderThreads",b.getMethod("getTotalThreadCount").invoke(builder));
        Field results=manager.getClass().getDeclaredField("buildResults");results.setAccessible(true);
        row.put("pendingUploadResults",((Collection<?>)results.get(manager)).size());
        Field rebuilds=manager.getClass().getDeclaredField("rebuildLists");rebuilds.setAccessible(true);int pending=0;
        for(Object queue:((Map<?,?>)rebuilds.get(manager)).values())pending+=((Collection<?>)queue).size();
        row.put("pendingRebuilds",pending);
    }
    private static void vanilla(Minecraft mc,Map<String,Object> row)throws Exception{
        row.put("rendererBackend","Vanilla");row.put("terrainComplete",mc.levelRenderer.hasRenderedAllChunks());
        row.put("rendererDebugStrings",List.of(mc.levelRenderer.getChunkStatistics()));
        ChunkRenderDispatcher dispatcher=null;
        for(Field f:mc.levelRenderer.getClass().getDeclaredFields())if(f.getType()==ChunkRenderDispatcher.class){f.setAccessible(true);dispatcher=(ChunkRenderDispatcher)f.get(mc.levelRenderer);break;}
        if(dispatcher==null)throw new IllegalStateException("Vanilla chunk dispatcher unavailable");
        row.put("buildQueueEmpty",dispatcher.isQueueEmpty());row.put("scheduledJobs",dispatcher.getToBatchCount());
        row.put("pendingRebuilds",dispatcher.getToBatchCount());row.put("pendingUploadResults",dispatcher.getToUpload());
        row.put("busyThreads",dispatcher.isQueueEmpty()?0:1);row.put("busyThreadsScope","Vanilla queue-empty proxy, not an exact worker count");
    }
    /** At least five seconds, low observed work and nearly drained/non-growing
     * queues. Empty LIGHT_DRAIN/UPLOAD method calls are expected every frame.
     * The caller owns the 120-second limit and any server rolling-window check. */
    public static Map<String,Object> stability(Map<String,Object> before,Map<String,Object> now,double lastServerTickWallMs){
        var result=new LinkedHashMap<String,Object>();var reasons=new ArrayList<String>();
        if(before.containsKey("error"))reasons.add("before snapshot error: "+before.get("error"));
        if(now.containsKey("error"))reasons.add("current snapshot error: "+now.get("error"));
        double seconds=(number(now,"nanoTime")-number(before,"nanoTime"))/1e9;result.put("windowSeconds",finite(seconds));
        if(!Double.isFinite(seconds)||seconds<5)reasons.add("Need at least five seconds between snapshots");
        if(!Boolean.TRUE.equals(before.get("levelPresent"))||!Boolean.TRUE.equals(now.get("levelPresent")))reasons.add("Client level unavailable");
        if(!java.util.Objects.equals(before.get("levelIdentity"),now.get("levelIdentity")))reasons.add("Client level changed");
        for(String key:List.of("lightQueueEmpty","terrainComplete","buildQueueEmpty"))if(!Boolean.TRUE.equals(now.get(key)))reasons.add(key+" is not true");
        var queueDeltas=new LinkedHashMap<String,Object>();
        for(String key:List.of("scheduledJobs","busyThreads","pendingUploadResults","pendingRebuilds")){
            double old=number(before,key),value=number(now,key),limit=key.equals("busyThreads")||key.equals("pendingUploadResults")?1:2;
            queueDeltas.put(key,Map.of("before",finite(old),"now",finite(value),"delta",finite(value-old),"limit",limit));
            if(!Double.isFinite(old)||!Double.isFinite(value)||value>limit||value-old>1)reasons.add(key+" not nearly drained/non-growing");
        }
        result.put("queues",queueDeltas);result.put("loadedChunkDelta",finite(number(now,"clientLoadedChunks")-number(before,"clientLoadedChunks")));
        try{
            Map<?,?> a=(Map<?,?>)before.get("activity"),b=(Map<?,?>)now.get("activity");
            if(!Boolean.TRUE.equals(b.get("enabled")))reasons.add("Chunk work timers are disabled");
            var deltas=new LinkedHashMap<String,Object>();
            var observedKinds=new ArrayList<>(List.of(FlightChunkWorkTimings.Kind.CHUNK_INSTALL,FlightChunkWorkTimings.Kind.LIGHT_DRAIN));
            if("Vanilla".equals(now.get("rendererBackend")))observedKinds.addAll(List.of(FlightChunkWorkTimings.Kind.VANILLA_COMPILE,FlightChunkWorkTimings.Kind.VANILLA_SYNC_COMPILE,FlightChunkWorkTimings.Kind.VANILLA_UPLOAD));
            else observedKinds.addAll(List.of(FlightChunkWorkTimings.Kind.EMBEDDIUM_MESH,FlightChunkWorkTimings.Kind.EMBEDDIUM_UPLOAD));
            for(var kind:observedKinds){
                int i=kind.ordinal();double calls=entry(b,"completedCalls",i)-entry(a,"completedCalls",i),ms=entry(b,"inclusiveWallMs",i)-entry(a,"inclusiveWallMs",i);
                deltas.put(kind.name(),Map.of("completedCalls",finite(calls),"inclusiveWallMs",finite(ms),"wallMsPerSecond",finite(ms/Math.max(.001,seconds))));
                if(!Double.isFinite(calls)||!Double.isFinite(ms))reasons.add("Non-finite chunk work counter");
                if(calls<0||ms<0)reasons.add("Chunk work counters reset");
                if(kind==FlightChunkWorkTimings.Kind.CHUNK_INSTALL&&calls>seconds*2)reasons.add("Chunk installs still arriving above 2/s");
                if(kind==FlightChunkWorkTimings.Kind.EMBEDDIUM_MESH&&calls>seconds*4)reasons.add("Chunk mesh work still above 4/s");
                double rateLimit=kind==FlightChunkWorkTimings.Kind.EMBEDDIUM_MESH?20:5;
                if(ms>seconds*rateLimit)reasons.add(kind+" warmup work still above "+rateLimit+"ms/s");
            }
            result.put("workDeltas",deltas);
        }catch(Throwable error){reasons.add("Work counters unavailable: "+message(error));}
        result.put("lastServerTickWallMs",finite(lastServerTickWallMs));
        if(!Double.isFinite(lastServerTickWallMs)||lastServerTickWallMs<0||lastServerTickWallMs>50)reasons.add("Latest server tick not ready (must be finite, 0..50ms)");
        result.put("scope","Five-second client idle heuristic plus latest server tick only; not proof of all terrain generated or a server p95 bound. No queue mutation or terrain loading.");
        result.put("stable",reasons.isEmpty());result.put("reasons",reasons);return result;
    }
    private static double number(Map<?,?> row,String key){Object v=row.get(key);return v instanceof Number n?n.doubleValue():Double.NaN;}
    private static Object finite(double value){return Double.isFinite(value)?value:"unavailable";}
    private static double entry(Map<?,?> row,String key,int i){return ((Number)((List<?>)row.get(key)).get(i)).doubleValue();}
    private static List<String> strings(Object value){var rows=new ArrayList<String>();for(Object v:(Collection<?>)value)rows.add(String.valueOf(v));return rows;}
    private static String message(Throwable error){Throwable cause=error.getCause()==null?error:error.getCause();return cause.getClass().getSimpleName()+": "+cause.getMessage();}
    private FlightColdWarmupProbe(){}
}
