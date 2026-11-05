package cn.blockforge.generated.generatedmod.network.packet.cultivation;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record FourArtsPacket(int playerId, Action action, int state, Vec3 position, float yaw) {
    public enum Action { STATE, RENEWAL, PASSIVE_RENEWAL, GATHER_PULSE, BLOOD_HIT, PASSIVE_BLOOD_HIT, PURIFICATION, PASSIVE_PURIFICATION, FLAME_WAVE }

    public FourArtsPacket(int playerId, Action action, int state, Vec3 position) {
        this(playerId,action,state,position,0);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(playerId);
        buf.writeEnum(action);
        buf.writeByte(state);
        buf.writeDouble(position.x);
        buf.writeDouble(position.y);
        buf.writeDouble(position.z);
        if(action==Action.FLAME_WAVE) buf.writeFloat(yaw);
    }

    public static FourArtsPacket decode(FriendlyByteBuf buf) {
        int playerId=buf.readVarInt();
        Action action=buf.readEnum(Action.class);
        int state=buf.readUnsignedByte();
        Vec3 position=new Vec3(buf.readDouble(),buf.readDouble(),buf.readDouble());
        return new FourArtsPacket(playerId,action,state,position,action==Action.FLAME_WAVE?buf.readFloat():0);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context=supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> cn.blockforge.generated.generatedmod.client.arts.FourArtsEffects.accept(this)));
        context.setPacketHandled(true);
    }
}
