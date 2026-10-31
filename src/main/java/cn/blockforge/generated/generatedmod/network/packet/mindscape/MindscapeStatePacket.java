package cn.blockforge.generated.generatedmod.network.packet.mindscape;

import cn.blockforge.generated.generatedmod.*;
import static cn.blockforge.generated.generatedmod.Network.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import java.util.UUID;

public record MindscapeStatePacket(boolean unstable,int state,int remaining,String challenge,UUID session,int slot){
    public MindscapeStatePacket(boolean unstable,int state,int remaining,String challenge){this(unstable,state,remaining,challenge,null,-1);}
    public static void encode(MindscapeStatePacket p,FriendlyByteBuf b){
        b.writeBoolean(p.unstable);b.writeVarInt(p.state);b.writeVarInt(p.remaining);b.writeUtf(p.challenge,256);
        b.writeBoolean(p.session!=null);if(p.session!=null)b.writeUUID(p.session);b.writeVarInt(p.slot);
    }
    public static MindscapeStatePacket decode(FriendlyByteBuf b){
        boolean unstable=b.readBoolean();int state=b.readVarInt(),remaining=b.readVarInt();String challenge=b.readUtf(256);
        UUID session=b.readBoolean()?b.readUUID():null;return new MindscapeStatePacket(unstable,state,remaining,challenge,session,b.readVarInt());
    }
    public static void handle(MindscapeStatePacket p,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->ClientPacketSink.safe(ClientPacketSink.mindscapeState,p));c.setPacketHandled(true);}
}
