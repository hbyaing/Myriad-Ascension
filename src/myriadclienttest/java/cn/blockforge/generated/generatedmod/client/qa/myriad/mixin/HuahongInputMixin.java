package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.YujianClientController;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only the opt-in automated fixture supplies input; never bundled with production. */
@Mixin(value=YujianClientController.class,remap=false)
public abstract class HuahongInputMixin {
    @Inject(method="clientTick",at=@At("HEAD"),cancellable=true,remap=false)
    private static void fixtureInput(TickEvent.ClientTickEvent event,CallbackInfo callback){
        if((Boolean.getBoolean("wuxingxiantu.huahongCapture")||Boolean.getBoolean("wuxingxiantu.swordTailCapture")))callback.cancel();
    }
}
