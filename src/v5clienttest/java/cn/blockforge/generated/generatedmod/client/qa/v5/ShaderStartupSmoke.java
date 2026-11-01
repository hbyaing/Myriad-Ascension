package cn.blockforge.generated.generatedmod.client.qa.v5;

import cn.blockforge.generated.generatedmod.client.HuahongVolumeRenderer;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.lwjgl.opengl.GL11;
import org.lwjgl.glfw.GLFW;

/** Own hidden client only: compile production shader on startup and again on resource reload. */
final class ShaderStartupSmoke {
    private static final long START=System.nanoTime();
    private static final Path OUT=Path.of(System.getProperty("wuxingxiantu.v5Output"));
    private static boolean done;private static int stage,ticks,violations;private static CompletableFuture<Void> reload;
    static void tick(TickEvent.ClientTickEvent event){
        if(done||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        try{
            Files.createDirectories(OUT);GLFW.glfwHideWindow(mc.getWindow().getWindow());
            if(mc.mouseHandler.isMouseGrabbed()||GLFW.glfwGetInputMode(mc.getWindow().getWindow(),GLFW.GLFW_CURSOR)!=GLFW.GLFW_CURSOR_NORMAL)violations++;
            if((System.nanoTime()-START)/1e9>120)throw new AssertionError("Startup/reload timeout");
            if(mc.player!=null)throw new AssertionError("Unexpected user world");
            if(mc.getOverlay()!=null)return;
            if(!(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen||mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen))return;
            if(++ticks<10)return;
            if(!HuahongVolumeRenderer.ready())throw new AssertionError(HuahongVolumeRenderer.status);
            if(GL11.glGetError()!=0)throw new AssertionError("GL error");
            if(stage==0){stage=1;ticks=0;reload=mc.reloadResourcePacks();return;}
            if(!reload.isDone())return;reload.join();
            if(violations!=0)throw new AssertionError("Mouse capture protection violated");
            finish(mc,null);
        }catch(Throwable e){finish(mc,e.toString());}
    }
    private static void finish(Minecraft mc,String error){if(done)return;done=true;
        try{Files.writeString(OUT.resolve("startup-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("passed",error==null,"resourceReload",stage==1&&reload!=null&&reload.isDone(),"shaderReady",HuahongVolumeRenderer.ready(),"shaderHash",HuahongVolumeRenderer.shaderHash,"gpu",GL11.glGetString(GL11.GL_RENDERER),"captureViolations",violations,"error",error==null?"":error)));}catch(Exception e){throw new RuntimeException(e);}mc.stop();
    }
}
