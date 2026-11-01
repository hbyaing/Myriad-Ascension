package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One level-local clock shared by cloud history, material and remote visibility. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class JindouCloudClock {
    private static final JindouCloudTime.Clock CLOCK=new JindouCloudTime.Clock();
    private JindouCloudClock(){}
    private static void sync(){CLOCK.setLevel(Minecraft.getInstance().level);}
    public static long tick(){sync();return CLOCK.tick();}
    public static double time(float partial){sync();return CLOCK.time(partial,Minecraft.getInstance().isPaused());}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        sync();CLOCK.advance(!Minecraft.getInstance().isPaused());
    }
}
