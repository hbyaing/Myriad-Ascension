package cn.blockforge.generated.generatedmod.network.packet.cultivation;

import cn.blockforge.generated.generatedmod.ArtCatalog;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** One event per visible cast phase; positions and timing come from authoritative server simulation. */
public record ArtVisualPacket(String art, int mode, Vec3 start, Vec3 end, float size, int phase) {
    public static final int ARC = 0, PROJECTILE = 1, IMPACT = 2, AREA = 3, AURA = 4, BOUNDARY = 5, TRAIL = 6;
    public static void encode(ArtVisualPacket p, FriendlyByteBuf b) {
        b.writeUtf(p.art, 48); b.writeByte(p.mode); vector(b, p.start); vector(b, p.end);
        b.writeFloat(p.size); b.writeByte(p.phase);
    }
    public static ArtVisualPacket decode(FriendlyByteBuf b) {
        return new ArtVisualPacket(b.readUtf(48), b.readUnsignedByte(), vector(b), vector(b), b.readFloat(), b.readUnsignedByte());
    }
    private static void vector(FriendlyByteBuf b, Vec3 v) { b.writeDouble(v.x); b.writeDouble(v.y); b.writeDouble(v.z); }
    private static Vec3 vector(FriendlyByteBuf b) { return new Vec3(b.readDouble(), b.readDouble(), b.readDouble()); }
    private static boolean finite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z)
                && Math.abs(v.x) <= 30_000_000 && Math.abs(v.z) <= 30_000_000 && Math.abs(v.y) <= 4096;
    }
    public static void handle(ArtVisualPacket p, Supplier<NetworkEvent.Context> supplier) {
        var ctx = supplier.get();
        if (ArtCatalog.isValid(p.art) && !cn.blockforge.generated.generatedmod.arts.FourArtsVisuals.replaces(p.art) && p.mode <= TRAIL && Float.isFinite(p.size) && p.size > 0 && p.size <= 32
                && finite(p.start) && finite(p.end) && p.start.distanceToSqr(p.end) <= 4096) {
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> cn.blockforge.generated.generatedmod.client.arts.ArtVisualEffects.accept(p)));
        }
        ctx.setPacketHandled(true);
    }
    public int color() { return cn.blockforge.generated.generatedmod.arts.ArtVfxCatalog.recipe(art).color(); }
}
