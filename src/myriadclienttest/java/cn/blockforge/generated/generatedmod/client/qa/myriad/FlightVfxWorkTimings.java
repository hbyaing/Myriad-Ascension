package cn.blockforge.generated.generatedmod.client.qa.myriad;
import java.util.*;
import java.nio.file.*;
import com.google.gson.GsonBuilder;
/** Observation-only CPU wall spans; native GL waits are included, no glFinish/readback. */
public final class FlightVfxWorkTimings {
 private static final boolean ON=Boolean.getBoolean("wuxingxiantu.flightColdProfile");
 private static final long[] START=new long[3];
 private static final List<Object> SLOW=new ArrayList<>();
 public static void span(int kind,boolean begin){if(!ON)return;if(begin)START[kind]=System.nanoTime();else {long end=System.nanoTime();double ms=(end-START[kind])/1e6;if(ms>=3)SLOW.add(Map.of("kind",kind,"wallMs",ms,"clock",HuahongFrameTimings.sceneClock,"endWallClockMillis",System.currentTimeMillis()));}}
 public static void write(Path out)throws Exception{Files.writeString(out.resolve("flight-vfx-work-timings.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("kinds",List.of("FlyingSwordEffects.render","FlyingSwordEffects.submitPending","SwordFlightTrailRenderer.drawPost"),"slowSpans",SLOW)));}
 private FlightVfxWorkTimings(){}
}
