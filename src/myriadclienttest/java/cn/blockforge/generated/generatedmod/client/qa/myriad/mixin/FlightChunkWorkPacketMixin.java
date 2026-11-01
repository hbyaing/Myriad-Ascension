package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightChunkWorkTimings;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightChunkWorkTimings.Kind;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets="net.minecraft.client.multiplayer.ClientPacketListener",remap=false)
public abstract class FlightChunkWorkPacketMixin {
    // Netty's first invocation exits via thread dispatch. Start only once the
    // packet is actually being installed on the client/render thread.
    @Inject(method="handleLevelChunkWithLight",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V",shift=At.Shift.AFTER,remap=false),remap=false)
    private void begin(CallbackInfo ci){FlightChunkWorkTimings.begin(Kind.CHUNK_PACKET);}
    @Inject(method="handleLevelChunkWithLight",at=@At("RETURN"),remap=false)
    private void end(CallbackInfo ci){FlightChunkWorkTimings.end(Kind.CHUNK_PACKET);}
}
