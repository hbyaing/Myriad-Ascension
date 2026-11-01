package cn.blockforge.generated.generatedmod.client.qa.myriad;
import java.util.*;
import java.nio.file.*;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
/** Native submission and upload wall spans; no GL finish/query synchronization. */
public final class FlightDrawTimings {
    private static final boolean ON=Boolean.getBoolean("wuxingxiantu.flightColdProfile");
    private static final long[] START=new long[11];private static final List<Object> SLOW=new ArrayList<>();
    public static void span(int kind,boolean start){
        if(!ON||!RenderSystem.isOnRenderThread())return;
        if(start){START[kind]=System.nanoTime();return;}
        double ms=(System.nanoTime()-START[kind])/1e6;if(ms<3)return;
        var shader=RenderSystem.getShader();SLOW.add(Map.of("kind",kind,"wallMs",ms,"clock",HuahongFrameTimings.sceneClock,
                "endWallClockMillis",System.currentTimeMillis(),"shader",shader==null?"none":shader.getName(),
                "program",org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM)));
    }
    public static void write(Path out)throws Exception{Files.writeString(out.resolve("flight-draw-timings.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("kinds",List.of("GlStateManager._drawElements","ShaderInstance.apply","ShaderInstance.clear","VertexBuffer.upload","BufferBuilder.end","ControlledSwordRenderer.render","PlayerRenderer.render","DunxuTransparency.wrap","DunxuTransparency.translucent","PackedBulk.bind","PackedBulk.write"),"slowSpans",SLOW)));}
    private FlightDrawTimings(){}
}
