package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;
import cn.blockforge.generated.generatedmod.client.qa.myriad.HuahongFrameTimings;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=Minecraft.class,remap=false)
public abstract class HuahongMinecraftTimingMixin {
    @Inject(method="runTick",at=@At("HEAD"),remap=false) private void begin(boolean render,CallbackInfo callback){HuahongFrameTimings.run(true);cn.blockforge.generated.generatedmod.client.qa.myriad.FlightWorldTimings.begin();}
    @Inject(method="runTick",at=@At("RETURN"),remap=false) private void end(boolean render,CallbackInfo callback){HuahongFrameTimings.run(false);}
}
