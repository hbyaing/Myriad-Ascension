package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightChunkWorkTimings;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightChunkWorkTimings.Kind;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets="net.minecraft.client.multiplayer.ClientLevel",remap=false)
public abstract class FlightChunkWorkLightMixin {
    @Inject(method="pollLightUpdates",at=@At("HEAD"),require=0,remap=false)
    private void begin(CallbackInfo ci){FlightChunkWorkTimings.begin(Kind.LIGHT_DRAIN);}
    @Inject(method="pollLightUpdates",at=@At("RETURN"),require=0,remap=false)
    private void end(CallbackInfo ci){FlightChunkWorkTimings.end(Kind.LIGHT_DRAIN);}
}
