package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightChunkWorkTimings;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightChunkWorkTimings.Kind;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets="net.minecraft.client.Minecraft",remap=false)
public abstract class FlightChunkWorkMinecraftMixin {
    @Inject(method="runTick",at=@At("HEAD"),remap=false)
    private void begin(boolean render,CallbackInfo ci){FlightChunkWorkTimings.frame(true);}
    @Inject(method="runTick",at=@At("RETURN"),remap=false)
    private void end(boolean render,CallbackInfo ci){FlightChunkWorkTimings.frame(false);}
}
