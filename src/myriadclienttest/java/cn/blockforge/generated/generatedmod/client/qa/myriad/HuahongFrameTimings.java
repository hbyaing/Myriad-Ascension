package cn.blockforge.generated.generatedmod.client.qa.myriad;

import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;

/** QA-only whole-loop and GLFW swap timing; contains no rendering changes. */
public final class HuahongFrameTimings {
    private static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.huahongDynamics")||Boolean.getBoolean("wuxingxiantu.flightColdProfile");
    private static final List<Map<String,Object>> ROWS=new ArrayList<>();
    public static volatile long sceneClock=-1;
    private static long began,swapBegan,serial;
    private static double swapMs;
    public static void run(boolean start){
        if(!ENABLED)return;
        if(start){began=System.nanoTime();swapMs=0;serial++;}
        else if(began!=0)ROWS.add(Map.of("serial",serial,"clock",sceneClock,"nanoTime",System.nanoTime(),"wallClockMillis",System.currentTimeMillis(),"runTickWallMs",(System.nanoTime()-began)/1e6,"windowUpdateDisplayWallMs",swapMs));
    }
    public static void swap(boolean start){if(!ENABLED)return;if(start)swapBegan=System.nanoTime();else if(swapBegan!=0)swapMs+=(System.nanoTime()-swapBegan)/1e6;}
    static void write(Path out)throws Exception{Files.writeString(out.resolve("minecraft-loop-timings.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("scope","Minecraft.runTick HEAD/RETURN and Window.updateDisplay HEAD/RETURN; includes swap/event polling and frame limiter. Trailing unfinished runTick excluded. clock<0 is startup.","rows",ROWS)));}
    private HuahongFrameTimings(){}
}
