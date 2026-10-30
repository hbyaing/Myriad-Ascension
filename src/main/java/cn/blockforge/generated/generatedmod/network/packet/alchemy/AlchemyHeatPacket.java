package cn.blockforge.generated.generatedmod.network.packet.alchemy;

import cn.blockforge.generated.generatedmod.AlchemyFurnaceMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Explicit heat selection, authorized against the currently open furnace menu. */
public record AlchemyHeatPacket(int containerId, int mode) {
    public static void encode(AlchemyHeatPacket p, FriendlyByteBuf b) { b.writeInt(p.containerId); b.writeInt(p.mode); }
    public static AlchemyHeatPacket decode(FriendlyByteBuf b) { return new AlchemyHeatPacket(b.readInt(), b.readInt()); }
    public static void handle(AlchemyHeatPacket p, Supplier<NetworkEvent.Context> supplier) {
        var c=supplier.get();
        c.enqueueWork(() -> {
            var player=c.getSender();
            if(player!=null && player.containerMenu instanceof AlchemyFurnaceMenu menu
                    && menu.containerId==p.containerId && menu.stillValid(player)
                    && (p.mode==0||p.mode==1||p.mode==4||p.mode==16)) {
                menu.furnace().v5Control(player,1000+p.mode);
                menu.broadcastChanges();
            }
        });
        c.setPacketHandled(true);
    }
}
