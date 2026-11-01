package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightChunkWorkTimings;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightChunkWorkTimings.Kind;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets="net.minecraft.client.multiplayer.ClientChunkCache",remap=false)
public abstract class FlightChunkWorkInstallMixin {
    @Inject(method="replaceWithPacketData",at=@At("HEAD"),require=0,remap=false)
    private void begin(CallbackInfoReturnable<?> ci){FlightChunkWorkTimings.begin(Kind.CHUNK_INSTALL);}
    @Inject(method="replaceWithPacketData",at=@At("RETURN"),require=0,remap=false)
    private void end(CallbackInfoReturnable<?> ci){FlightChunkWorkTimings.end(Kind.CHUNK_INSTALL);}
}
