package cn.blockforge.generated.generatedmod.mixin;

import cn.blockforge.generated.generatedmod.client.InterpolatedFlyingSwordTransform;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import cn.blockforge.generated.generatedmod.yujian.FlightFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cull the same mounted-player pose that YujianRidingPose actually renders. */
@Mixin(value=EntityRenderDispatcher.class,remap=false)
public abstract class FlightRiderVisibilityMixin {
    @Inject(method={"shouldRender","m_114397_"},at=@At("HEAD"),cancellable=true,remap=false)
    private void wuxing$flightPoseVisibility(Entity entity,Frustum frustum,double x,double y,double z,CallbackInfoReturnable<Boolean> callback){
        if(!(entity instanceof Player)||!(entity.getVehicle() instanceof ControlledSwordEntity sword)||!sword.flightRoot())return;
        var f=InterpolatedFlyingSwordTransform.get(sword,Minecraft.getInstance().getFrameTime());
        // Shift the distance query rather than impose a new render distance;
        // retain vanilla/entity-distance options and other visibility policies.
        boolean inRange=entity.shouldRender(x+entity.getX()-f.position().x,y+entity.getY()-f.position().y,z+entity.getZ()-f.position().z);
        callback.setReturnValue(inRange&&(entity.noCulling||frustum.isVisible(
                FlightFrame.riderBounds(f.position(),f.yaw(),f.pitch(),f.roll(),sword.riderOffset()))));
    }
}
