package cn.blockforge.generated.generatedmod.client.qa.myriad;
import java.util.*;
import java.nio.file.*;
import com.google.gson.GsonBuilder;
/** Server-thread inclusive wall spans; warmup is excluded from aggregates. */
public final class FlightEcologyTimings {
    private static final boolean ON=Boolean.getBoolean("wuxingxiantu.flightColdProfile");
    private static final long[] START=new long[2],COUNT=new long[2];
    private static final double[] SUM=new double[2],MAX=new double[2];
    private static final List<Object> SLOW=new ArrayList<>();
    public static void span(int kind,boolean start){
        if(!ON)return;
        if(start){START[kind]=System.nanoTime();return;}
        long clock=HuahongFrameTimings.sceneClock;if(clock<0)return;
        double ms=(System.nanoTime()-START[kind])/1e6;COUNT[kind]++;SUM[kind]+=ms;MAX[kind]=Math.max(MAX[kind],ms);
        if(ms>=2)SLOW.add(Map.of("kind",kind,"clock",clock,"wallMs",ms,"endWallClockMillis",System.currentTimeMillis()));
    }
    public static void write(Path out)throws Exception{
        Files.writeString(out.resolve("flight-ecology-work-timings.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("kinds",List.of("SpiritPlantRegions.advance","SpiritPlantRegions.surfaceBiome (caller)"),"calls",COUNT,"inclusiveWallMs",SUM,"maxWallMs",MAX,"slowSpans",SLOW)));
    }
    private FlightEcologyTimings(){}
}
