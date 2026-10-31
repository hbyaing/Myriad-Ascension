package cn.blockforge.generated.generatedmod.yujian;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Sender-owned cosmetic preference; never grants flight or changes its rules. */
public record HuahongSkinPacket(int skin) {
    public static void encode(HuahongSkinPacket p,FriendlyByteBuf b){b.writeVarInt(p.skin);}
    public static HuahongSkinPacket decode(FriendlyByteBuf b){return new HuahongSkinPacket(b.readVarInt());}
    public static void apply(ServerPlayer player,int id){
        var skin=HuahongSkin.fromId(id);player.getPersistentData().putInt(HuahongSkin.KEY,skin.ordinal());
        if(player.getVehicle() instanceof ControlledSwordEntity sword)sword.huahongSkin(skin);
    }
    public static void handle(HuahongSkinPacket p,Supplier<NetworkEvent.Context> supplier){
        var context=supplier.get();context.enqueueWork(()->{if(context.getSender()!=null)apply(context.getSender(),p.skin);});context.setPacketHandled(true);
    }
}
