package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.Network;
import cn.blockforge.generated.generatedmod.yujian.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class HuahongSkinPreferences {
    private static Object player;
    private static HuahongSkin sent;
    public static void select(HuahongSkin skin){FlyingSwordConfig.HUAHONG_SKIN.set(skin);FlyingSwordConfig.HUAHONG_SKIN.save();send();}
    private static void send(){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.getConnection()==null)return;
        player=mc.player;sent=FlyingSwordConfig.HUAHONG_SKIN.get();
        Network.CHANNEL.sendToServer(new HuahongSkinPacket(sent.ordinal()));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.player==null){player=null;sent=null;return;}
        if(player!=mc.player||sent!=FlyingSwordConfig.HUAHONG_SKIN.get())send();
    }
    private HuahongSkinPreferences(){}
}
