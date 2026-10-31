package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;
import cn.blockforge.generated.generatedmod.client.qa.myriad.HuahongFrameTimings;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=Window.class,remap=false)
public abstract class HuahongWindowTimingMixin {
    @Inject(method="updateDisplay",at=@At("HEAD"),remap=false) private void begin(CallbackInfo callback){HuahongFrameTimings.swap(true);}
    @Inject(method="updateDisplay",at=@At("RETURN"),remap=false) private void end(CallbackInfo callback){HuahongFrameTimings.swap(false);}
}
