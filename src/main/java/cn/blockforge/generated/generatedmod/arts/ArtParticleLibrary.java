package cn.blockforge.generated.generatedmod.arts;

import cn.blockforge.generated.generatedmod.Network;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.ArtVisualPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Server sends compact phase events; client resolves V2 textures and performs bounded local emission. */
public final class ArtParticleLibrary {
    public static void emit(ServerLevel level, String art, int mode, Vec3 start, Vec3 end, double size, int phase) {
        if (FourArtsVisuals.replaces(art)) return;
        var packet = new ArtVisualPacket(art, mode, start, end, (float) size, phase);
        for (var observer : level.players()) {
            if (Math.min(observer.position().distanceToSqr(start), observer.position().distanceToSqr(end)) < 64 * 64)
                Network.sendToPlayer(observer, packet);
        }
    }
    private ArtParticleLibrary() {}
}
