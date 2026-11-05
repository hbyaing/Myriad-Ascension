package cn.blockforge.generated.generatedmod.network.packet.cultivation;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record FourArtsPacket(int playerId, Action action, int state, Vec3 position) {
    public enum Action { STATE, RENEWAL, PASSIVE_RENEWAL, GATHER_PULSE, BLOOD_HIT, PASSIVE_BLOOD_HIT }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(playerId);
        buf.writeEnum(action);
        buf.writeByte(state);
        buf.writeDouble(position.x);
        buf.writeDouble(position.y);
        buf.writeDouble(position.z);
    }

    public static FourArtsPacket decode(FriendlyByteBuf buf) {
        return new FourArtsPacket(buf.readVarInt(), buf.readEnum(Action.class), buf.readUnsignedByte(),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context=supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> cn.blockforge.generated.generatedmod.client.arts.FourArtsEffects.accept(this)));
        context.setPacketHandled(true);
    }
}
