package cn.blockforge.generated.generatedmod.network.packet.cultivation;

import cn.blockforge.generated.generatedmod.arts.IceSpikeVolley;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record IceSpikePacket(int caster, long started, int age, Vec3 base, Vec3 forward, List<Sample> samples) {
    public record Sample(int index, int flight, Vec3 tip, Vec3 axis, boolean impact) {}

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(caster);
        buffer.writeLong(started);
        buffer.writeByte(age);
        writePosition(buffer, base);
        writeDirection(buffer, forward);
        buffer.writeByte(samples.size());
        for (var shot : samples) {
            buffer.writeByte(shot.index);
            buffer.writeByte(shot.flight);
            Vec3 offset = shot.tip.subtract(base);
            buffer.writeFloat((float)offset.x);
            buffer.writeFloat((float)offset.y);
            buffer.writeFloat((float)offset.z);
            writeDirection(buffer, shot.axis);
            buffer.writeBoolean(shot.impact);
        }
    }

    public static IceSpikePacket decode(FriendlyByteBuf buffer) {
        int caster = buffer.readVarInt();
        long started = buffer.readLong();
        int age = buffer.readUnsignedByte();
        Vec3 base = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        Vec3 forward = readDirection(buffer);
        int count = buffer.readUnsignedByte();
        if (count > IceSpikeVolley.FORMATION.size()) throw new IllegalArgumentException("Invalid ice lance count");
        List<Sample> samples = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int index = buffer.readUnsignedByte();
            if (index >= IceSpikeVolley.FORMATION.size()) throw new IllegalArgumentException("Invalid ice lance index");
            samples.add(new Sample(index, buffer.readByte(), base.add(buffer.readFloat(), buffer.readFloat(), buffer.readFloat()),
                    readDirection(buffer), buffer.readBoolean()));
        }
        return new IceSpikePacket(caster, started, age, base, forward, samples);
    }

    private static void writePosition(FriendlyByteBuf buffer, Vec3 position) {
        buffer.writeDouble(position.x);
        buffer.writeDouble(position.y);
        buffer.writeDouble(position.z);
    }

    private static void writeDirection(FriendlyByteBuf buffer, Vec3 direction) {
        buffer.writeShort((int)Math.round(direction.x * 32767));
        buffer.writeShort((int)Math.round(direction.y * 32767));
        buffer.writeShort((int)Math.round(direction.z * 32767));
    }

    private static Vec3 readDirection(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readShort(), buffer.readShort(), buffer.readShort()).normalize();
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> cn.blockforge.generated.generatedmod.client.arts.FourArtsEffects.acceptIce(this)));
        context.setPacketHandled(true);
    }
}
