package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.*;
import cn.blockforge.generated.generatedmod.mindscape.MindscapeState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class MindscapeClient {
    private static final ResourceLocation DIMENSION=new ResourceLocation(GeneratedMod.MOD_ID,"mindscape");
    private static MindscapeState state=MindscapeState.NONE;
    private static int remaining;
    private static UUID owner,session;
    private static int slot=-1;
    private static String challenge="";
    private static long receivedAt;
    private static boolean cancelSent;
    public static MindscapeState state(){return state;}
    public static String challenge(){return challenge;}
    public static UUID sessionId(){return visualSession()?session:null;}
    /** Pure client coordinates: no server manager or SavedData class is loaded. */
    public static Vec3 origin(){
        return visualSession()?new Vec3((slot%8)*128+32.5,64,(slot/8)*128+32.5):null;
    }
    /** Only the server's successful COMPLETED state may reveal the purified bank. */
    public static float purification(float partialTick){
        if(!visualSession()||state!=MindscapeState.COMPLETED)return 0;
        float elapsed=MindscapeState.VICTORY_DISPLAY_TICKS-remainingAt(partialTick);
        return net.minecraft.util.Mth.clamp(elapsed/MindscapeState.PURIFICATION_TICKS,0,1);
    }
    private static boolean visualState(){
        return state==MindscapeState.ENTERING||state==MindscapeState.IN_MINDSCAPE
                ||state==MindscapeState.CHALLENGE_ACTIVE||state==MindscapeState.COMPLETED;
    }
    private static boolean visualSession(){
        var client=Minecraft.getInstance();
        return visualState()&&session!=null&&slot>=0&&slot<64&&client.player!=null
                &&client.player.getUUID().equals(owner)&&client.level!=null&&client.level.dimension().location().equals(DIMENSION);
    }
    private static float remainingAt(float partialTick){
        var level=Minecraft.getInstance().level;
        float elapsed=level==null?0:Math.max(0,level.getGameTime()-receivedAt+net.minecraft.util.Mth.clamp(partialTick,0,1));
        return Math.max(0,remaining-elapsed);
    }
    private static void clear(){
        state=MindscapeState.NONE;remaining=0;owner=null;session=null;slot=-1;challenge="";receivedAt=0;cancelSent=false;
    }
    public static void accept(cn.blockforge.generated.generatedmod.network.packet.mindscape.MindscapeStatePacket packet){
        var client=Minecraft.getInstance();var player=client.player;if(player==null||client.level==null)return;
        var data=GeneratedMod.data(player);if(data!=null)data.wuxingxiantu$setDaoHeartUnstable(packet.unstable());
        owner=player.getUUID();state=packet.state()>=0&&packet.state()<MindscapeState.values().length?MindscapeState.values()[packet.state()]:MindscapeState.NONE;
        int maximum=state==MindscapeState.COMPLETED?MindscapeState.VICTORY_DISPLAY_TICKS:state==MindscapeState.MEDITATING?300:0;
        remaining=Math.max(0,Math.min(maximum,packet.remaining()));receivedAt=client.level.getGameTime();
        challenge=java.util.Objects.requireNonNullElse(packet.challenge(),"");
        session=visualState()&&packet.slot()>=0&&packet.slot()<64?packet.session():null;slot=session==null?-1:packet.slot();
        if(state!=MindscapeState.MEDITATING)cancelSent=false;
    }
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){clear();}
    @SubscribeEvent public static void unload(net.minecraftforge.event.level.LevelEvent.Unload event){
        if(event.getLevel() instanceof net.minecraft.world.level.Level level&&level.isClientSide()&&level.dimension().location().equals(DIMENSION))clear();
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var client=Minecraft.getInstance();if(client.player==null||!client.player.getUUID().equals(owner)){clear();return;}
        if(state==MindscapeState.MEDITATING&&!cancelSent&&(client.options.keyUp.isDown()||client.options.keyDown.isDown()
                ||client.options.keyLeft.isDown()||client.options.keyRight.isDown()||client.options.keyJump.isDown()||client.options.keyShift.isDown())){
            cancelSent=true;Network.CHANNEL.sendToServer(new cn.blockforge.generated.generatedmod.network.packet.mindscape.MindscapeCancelPacket());
        }
    }
    @SubscribeEvent public static void render(net.minecraftforge.client.event.RenderGuiEvent.Post event){
        var client=Minecraft.getInstance();if(client.player==null||state==MindscapeState.NONE||client.options.hideGui)return;
        Component label=state==MindscapeState.MEDITATING?Component.translatable("hud.wuxingxiantu.mindscape_meditation",((int)Math.ceil(remainingAt(0))+19)/20)
                :Component.translatable("mindscape.wuxingxiantu.state."+state.name().toLowerCase(java.util.Locale.ROOT));
        int x=Math.max(4,(client.getWindow().getGuiScaledWidth()-client.font.width(label))/2);
        event.getGuiGraphics().drawString(client.font,label,x,client.getWindow().getGuiScaledHeight()-92,(state==MindscapeState.UNSTABLE||state==MindscapeState.MEDITATING)?0xDC143C:0xE5CB87,true);
    }
    private MindscapeClient(){}
}
