package cn.blockforge.generated.generatedmod.client;
import cn.blockforge.generated.generatedmod.GeneratedMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
/** Only resources owned by this mod; ShaderInstances/textures registered with Minecraft retain its ownership. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class RenderResourceLifecycle {
    public static void release(){
        if(!RenderSystem.isOnRenderThread()){RenderSystem.recordRenderCall(RenderResourceLifecycle::release);return;}
        SpiritGuardArmorRenderer.release();SoulPlayerVisuals.clearNativeCaches();
        cn.blockforge.generated.generatedmod.client.firecoil.Client.reset();
        TrailShaderBridge.release();
        cn.blockforge.generated.generatedmod.client.arts.FourArtsEffects.release();
        HeavenlyFurnaceGpu.release();
        HeavenlyFurnaceVisuals.releaseMaterials();
        cn.blockforge.generated.generatedmod.client.realmflight.MeteorBloomRenderer.release();
        cn.blockforge.generated.generatedmod.client.realmflight.MeteorHeadRenderer.release();
    }
    @SubscribeEvent public static void shaders(net.minecraftforge.client.event.RegisterShadersEvent e){release();}
    @Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
    public static class Events{
        @SubscribeEvent public static void shutdown(net.minecraftforge.event.GameShuttingDownEvent e){release();}
    }
}
