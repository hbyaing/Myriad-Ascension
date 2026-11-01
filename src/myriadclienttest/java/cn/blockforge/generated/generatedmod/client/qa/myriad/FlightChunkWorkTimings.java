package cn.blockforge.generated.generatedmod.client.qa.myriad;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/** QA only. Observes real client chunk work without delaying or reordering it.
 * Inclusive spans and concurrent worker times must not be summed as frame CPU. */
public final class FlightChunkWorkTimings {
    public enum Kind {
        CHUNK_PACKET, CHUNK_INSTALL, LIGHT_DRAIN, EMBEDDIUM_UPDATE,
        EMBEDDIUM_AWAIT, EMBEDDIUM_MESH, EMBEDDIUM_UPLOAD,
        VANILLA_COMPILE, VANILLA_SYNC_COMPILE, VANILLA_UPLOAD
    }
    private static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.flightChunkWorkTiming");
    private static final int KINDS=Kind.values().length,MAX_SLOW_SPANS=20000;
    private static final long SLOW_NANOS=2_000_000;
    private static final AtomicLongArray STARTS=new AtomicLongArray(KINDS),FINISHES=new AtomicLongArray(KINDS),NANOS=new AtomicLongArray(KINDS),LAST_END=new AtomicLongArray(KINDS);
    private static final ConcurrentHashMap<String,AtomicLongArray> THREAD_NANOS=new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<SlowSpan> SLOW=new ConcurrentLinkedQueue<>();
    private static final AtomicInteger slowCount=new AtomicInteger(),droppedSlow=new AtomicInteger();
    private static final AtomicLong awaitJobs=new AtomicLong(),uploadResults=new AtomicLong();
    private static final List<Frame> FRAMES=new ArrayList<>();
    private static volatile Frame current;
    private static long sequence;
    private static final ThreadLocal<Spans> SPANS=ThreadLocal.withInitial(Spans::new);
    private static final class Spans {
        final int[] depth=new int[KINDS];
        final long[] began=new long[KINDS],clock=new long[KINDS],frame=new long[KINDS];
    }
    private static final class Frame {
        final long serial,began,clock,wallClockMillis;
        final boolean levelPresent;
        final AtomicLongArray calls=new AtomicLongArray(KINDS),nanos=new AtomicLongArray(KINDS);
        final AtomicInteger immediateUpdates=new AtomicInteger();
        final AtomicInteger awaitJobs=new AtomicInteger(),uploadResults=new AtomicInteger();
        long ended;
        Frame(long serial,long began,long clock){this.serial=serial;this.began=began;this.clock=clock;wallClockMillis=System.currentTimeMillis();levelPresent=net.minecraft.client.Minecraft.getInstance().level!=null;}
    }
    private record SlowSpan(String kind,String thread,long startNanoTime,long endNanoTime,long startClock,long endClock,long startFrame,long endFrame,double wallMs){}

    public static void frame(boolean start){
        if(!ENABLED)return;
        if(start){
            current=new Frame(++sequence,System.nanoTime(),HuahongFrameTimings.sceneClock);
        }else{
            Frame f=current;current=null;
            if(f!=null){f.ended=System.nanoTime();FRAMES.add(f);}
        }
    }
    public static void begin(Kind kind){
        if(!ENABLED)return;
        int k=kind.ordinal();Spans s=SPANS.get();
        if(s.depth[k]++!=0)return;
        Frame f=current;s.began[k]=System.nanoTime();s.clock[k]=HuahongFrameTimings.sceneClock;s.frame[k]=f==null?-1:f.serial;
        STARTS.incrementAndGet(k);
    }
    public static void end(Kind kind){
        if(!ENABLED)return;
        int k=kind.ordinal();Spans s=SPANS.get();
        if(s.depth[k]==0||--s.depth[k]!=0)return;
        long ended=System.nanoTime(),elapsed=ended-s.began[k];
        FINISHES.incrementAndGet(k);NANOS.addAndGet(k,elapsed);LAST_END.set(k,ended);
        String thread=Thread.currentThread().getName();THREAD_NANOS.computeIfAbsent(thread,key->new AtomicLongArray(KINDS)).addAndGet(k,elapsed);
        Frame f=current;
        if(f!=null){f.calls.incrementAndGet(k);f.nanos.addAndGet(k,elapsed);}
        if(elapsed>=SLOW_NANOS){
            if(slowCount.getAndIncrement()<MAX_SLOW_SPANS)SLOW.add(new SlowSpan(kind.name(),thread,s.began[k],ended,s.clock[k],HuahongFrameTimings.sceneClock,s.frame[k],f==null?-1:f.serial,elapsed/1e6));
            else droppedSlow.incrementAndGet();
        }
    }
    public static void immediateUpdate(boolean immediate){
        Frame f=current;if(ENABLED&&immediate&&f!=null)f.immediateUpdates.incrementAndGet();
    }
    public static void awaitJobs(int count){
        if(!ENABLED)return;
        awaitJobs.addAndGet(count);Frame f=current;if(f!=null)f.awaitJobs.addAndGet(count);
    }
    public static void uploadResults(int count){
        if(!ENABLED)return;
        uploadResults.addAndGet(count);Frame f=current;if(f!=null)f.uploadResults.addAndGet(count);
    }
    /** Cumulative, read-only counters for bounded warmup stability windows.
     * Compare snapshots; LIGHT_DRAIN/UPLOAD calls alone need not become zero. */
    public static Map<String,Object> activitySnapshot(){
        var result=new LinkedHashMap<String,Object>();result.put("enabled",ENABLED);result.put("nanoTime",System.nanoTime());result.put("clock",HuahongFrameTimings.sceneClock);
        result.put("completedCalls",values(FINISHES,false));result.put("inclusiveWallMs",values(NANOS,true));result.put("lastEndNanoTime",values(LAST_END,false));
        result.put("embeddiumAwaitJobs",awaitJobs.get());result.put("embeddiumUploadResults",uploadResults.get());return result;
    }
    /** Call once after the scenario, alongside existing QA timing output. */
    public static void write(Path out)throws Exception{
        if(!ENABLED)return;
        var kinds=new ArrayList<String>();for(Kind k:Kind.values())kinds.add(k.name());
        var rows=new ArrayList<Map<String,Object>>();
        for(Frame f:FRAMES){
            var row=new LinkedHashMap<String,Object>();row.put("serial",f.serial);row.put("clock",f.clock);row.put("phase",f.clock<0?"warmup":"flight");row.put("levelPresent",f.levelPresent);row.put("wallClockMillis",f.wallClockMillis);
            row.put("beginNanoTime",f.began);row.put("endNanoTime",f.ended);row.put("runTickWallMs",(f.ended-f.began)/1e6);
            row.put("completedCalls",values(f.calls,false));row.put("inclusiveWallMs",values(f.nanos,true));row.put("embeddiumImmediateUpdateCalls",f.immediateUpdates.get());
            row.put("embeddiumAwaitJobs",f.awaitJobs.get());row.put("embeddiumUploadResults",f.uploadResults.get());rows.add(row);
        }
        var threads=new LinkedHashMap<String,Object>();THREAD_NANOS.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->threads.put(e.getKey(),values(e.getValue(),true)));
        var result=new LinkedHashMap<String,Object>();
        result.put("scope","QA-only observers, including clock<0 warmup/startup (levelPresent distinguishes menu). CHUNK_PACKET begins AFTER PacketUtils thread dispatch. Intervals are inclusive; nested spans overlap. Worker mesh wall spans overlap each other and frames; frame rows attribute completed spans to their completion frame. They are not exclusive CPU/GPU timings. No screenshots, packet buffering or chunk scheduling changes are performed by these observers.");
        result.put("kinds",kinds);result.put("starts",values(STARTS,false));result.put("finishes",values(FINISHES,false));result.put("inclusiveWallMs",values(NANOS,true));
        result.put("embeddiumAwaitJobs",awaitJobs.get());result.put("embeddiumUploadResults",uploadResults.get());
        result.put("threadInclusiveWallMs",threads);result.put("frames",rows);result.put("slowThresholdMs",SLOW_NANOS/1e6);result.put("slowSpans",List.copyOf(SLOW));result.put("droppedSlowSpans",droppedSlow.get());
        Files.writeString(out.resolve("flight-chunk-work-timings.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));
    }
    private static List<Number> values(AtomicLongArray a,boolean millis){
        var values=new ArrayList<Number>();for(int i=0;i<a.length();i++){if(millis)values.add(a.get(i)/1e6);else values.add(a.get(i));}return values;
    }
    private FlightChunkWorkTimings(){}
}
