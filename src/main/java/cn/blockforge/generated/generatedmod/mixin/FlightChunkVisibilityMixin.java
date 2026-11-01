package cn.blockforge.generated.generatedmod.mixin;

import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A local mounted model does not depend on the terrain mesh below it compiling. */
@Mixin(value=LevelRenderer.class,remap=false)
public abstract class FlightChunkVisibilityMixin {
    @Inject(method={"isChunkCompiled","m_202430_"},at=@At("HEAD"),cancellable=true,remap=false)
    private void wuxing$localFlightModels(BlockPos position,CallbackInfoReturnable<Boolean> callback){
        var player=Minecraft.getInstance().player;
        if(player!=null&&player.getVehicle() instanceof ControlledSwordEntity sword&&sword.flightRoot()
                &&(position.equals(player.blockPosition())||position.equals(sword.blockPosition())))callback.setReturnValue(true);
    }
}
