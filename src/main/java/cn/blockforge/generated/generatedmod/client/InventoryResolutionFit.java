package cn.blockforge.generated.generatedmod.client;

import net.minecraft.client.Minecraft;

/** Screen-local GUI density: leave room for the complete window and both JEI sidebars. */
public final class InventoryResolutionFit {
    static int scale(int pixelWidth,int pixelHeight,int configuredScale,boolean forceUnicode) {
        // At 1080p use a 960x540 GUI canvas (scale 2); 1440p uses scale 3.
        // A 480px window then has space for ingredients, bookmarks and navigation.
        int fit=Math.max(1,Math.min(pixelWidth/800,pixelHeight/360));
        if(fit==1&&pixelWidth>=1280&&pixelHeight>=720)fit=2;
        fit=Math.max(1,Math.min(configuredScale,fit));
        if(forceUnicode&&fit>1&&(fit&1)!=0)fit--;
        return fit;
    }

    public static void apply(Minecraft minecraft) {
        var window=minecraft.getWindow();
        int configured=window.calculateScale(minecraft.options.guiScale().get(),minecraft.isEnforceUnicode());
        // Restore the original backpack policy (f823139). Keep this independent of the
        // newer furnace/research-window fit policy and its JEI gutter requirements.
        // Each client uses its own framebuffer and preferred scale; no server/global preset.
        window.setGuiScale(scale(window.getWidth(),window.getHeight(),configured,minecraft.isEnforceUnicode()));
    }

    public static void restore(Minecraft minecraft) {
        if(minecraft==null)return;
        var window=minecraft.getWindow();
        window.setGuiScale(window.calculateScale(minecraft.options.guiScale().get(),minecraft.isEnforceUnicode()));
    }
    private InventoryResolutionFit() {}
}
