package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Fit every owned screen before widgets, slots and JEI receive its dimensions. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class ScreenResolutionEvents {
    private static boolean fitted;
    private static boolean owned(Screen screen){
        return screen!=null&&screen.getClass().getName().startsWith("cn.blockforge.generated.generatedmod.");
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void initialize(ScreenEvent.Init.Pre event){
        // Embedded inventory pages receive their host's content viewport, not the window.
        // Re-fitting them changes global GUI scale after the host has laid out its frame,
        // and overwrites page.width/page.height with the full-screen canvas.
        if(event.getScreen() instanceof CultivationPage)return;
        var mc=Minecraft.getInstance();var screen=event.getScreen();var window=mc.getWindow();
        if(!owned(screen)){
            if(!fitted)return;
            InventoryResolutionFit.restore(mc);fitted=false;
        }else{
            int requiredWidth=480,requiredHeight=360;
            if(screen instanceof AbstractContainerScreen<?> container){
                requiredWidth=Math.max(320,container.getXSize()+32);
                requiredHeight=Math.max(240,container.getYSize()+32);
                // Leave both side gutters for JEI instead of fitting the panel so tightly that
                // the ingredient list disappears. No optional JEI types are loaded here.
                if(net.minecraftforge.fml.ModList.get().isLoaded("jei"))requiredWidth=Math.max(requiredWidth,container.getXSize()+216);
                // The anvil's template sidebar extends outside the native 176px inventory.
                if(screen instanceof XuanlingAnvilScreen)requiredWidth=Math.max(requiredWidth,334);
            }
            int configured=window.calculateScale(mc.options.guiScale().get(),mc.isEnforceUnicode());
            if(screen instanceof CultivationMainScreen || screen instanceof cn.blockforge.generated.generatedmod.formation.client.CarvingScreen)
                InventoryResolutionFit.apply(mc);
            else window.setGuiScale(ScreenFitPolicy.scale(window.getWidth(),window.getHeight(),configured,
                    requiredWidth,requiredHeight,mc.isEnforceUnicode()));
            fitted=true;
        }
        // Screen.init assigned the previous canvas before this event. Rebase before subclass init().
        screen.width=window.getGuiScaledWidth();screen.height=window.getGuiScaledHeight();
    }
    @SubscribeEvent public static void closed(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!fitted)return;
        var mc=Minecraft.getInstance();
        if(mc.screen==null){InventoryResolutionFit.restore(mc);fitted=false;}
    }
    private ScreenResolutionEvents(){}
}
