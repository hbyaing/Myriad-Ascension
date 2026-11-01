package cn.blockforge.generated.generatedmod.client.qa.myriad;
import java.util.*;
import java.nio.file.*;
import com.google.gson.GsonBuilder;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
/** Stage-to-stage wall time without GPU synchronization or framebuffer readback. */
@Mod.EventBusSubscriber(modid="wuxingxiantu",value=Dist.CLIENT)
public final class FlightWorldTimings {
    private static final boolean ON=Boolean.getBoolean("wuxingxiantu.flightColdProfile");
    private static final java.lang.management.ThreadMXBean CPU=java.lang.management.ManagementFactory.getThreadMXBean();
    private static long lastCpu;private static long last;private static String previous="initial";
    private static final List<Object> SLOW=new ArrayList<>();
    public static void begin(){if(ON){last=System.nanoTime();lastCpu=CPU.getCurrentThreadCpuTime();previous="runTick.begin";}}
    public static void mark(String label){
        if(!ON)return;long now=System.nanoTime(),cpu=CPU.getCurrentThreadCpuTime();double ms=(now-last)/1e6;
        if(last!=0&&ms>=3)SLOW.add(Map.of("from",previous,"to",label,"wallMs",ms,"cpuMs",lastCpu<0||cpu<0?-1:(cpu-lastCpu)/1e6,"clock",HuahongFrameTimings.sceneClock,"endWallClockMillis",System.currentTimeMillis()));
        last=now;lastCpu=cpu;previous=label;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void stage(RenderLevelStageEvent e){if(ON)mark(e.getStage().toString());}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void render(TickEvent.RenderTickEvent e){if(ON)mark(e.phase==TickEvent.Phase.START?"renderTick.start":"renderTick.end");}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void tick(TickEvent.ClientTickEvent e){if(ON)mark(e.phase==TickEvent.Phase.START?"clientTick.start":"clientTick.end");}
    public static void write(Path out)throws Exception{Files.writeString(out.resolve("flight-world-stage-timings.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("scope","Render-thread gaps between existing game/world stages, including prior GPU queue waits. No exclusive GPU timing inference.","slowSpans",SLOW)));}
    private FlightWorldTimings(){}
}
