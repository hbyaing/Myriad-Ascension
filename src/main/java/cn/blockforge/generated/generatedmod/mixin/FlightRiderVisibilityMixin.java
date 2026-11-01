package cn.blockforge.generated.generatedmod.mixin;

import cn.blockforge.generated.generatedmod.client.InterpolatedFlyingSwordTransform;
import cn.blockforge.generated.generatedmod.client.JindouCloudRenderState;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import cn.blockforge.generated.generatedmod.yujian.FlightFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cull the same interpolated flight-player pose that the renderer actually submits. */
@Mixin(value=EntityRenderDispatcher.class,remap=false)
public abstract class FlightRiderVisibilityMixin {
    @Inject(method={"shouldRender","m_114397_"},at=@At("HEAD"),cancellable=true,remap=false)
    private void wuxing$flightPoseVisibility(Entity entity,Frustum frustum,double x,double y,double z,CallbackInfoReturnable<Boolean> callback){
        var client=Minecraft.getInstance();
        if(entity==client.player && entity instanceof Player player && JindouCloudRenderState.isActive(player)){
            var rendered=entity.getPosition(client.getFrameTime());
            // Vanilla tests the current-tick box. At cloud speed that box can
            // already be behind a front-facing camera while the rendered rider
            // is still in front of it. Retain vanilla padding/range policies.
            var box=entity.getBoundingBoxForCulling().move(rendered.subtract(entity.position())).inflate(.5);
            if(box.hasNaN() || box.getSize()==0)box=new AABB(rendered.x-2,rendered.y-2,rendered.z-2,rendered.x+2,rendered.y+2,rendered.z+2);
            boolean inRange=entity.shouldRender(x+entity.getX()-rendered.x,y+entity.getY()-rendered.y,z+entity.getZ()-rendered.z);
            callback.setReturnValue(inRange&&(entity.noCulling||frustum.isVisible(box)));
            return;
        }
        if(!(entity instanceof Player)||!(entity.getVehicle() instanceof ControlledSwordEntity sword)||!sword.flightRoot())return;
        var f=InterpolatedFlyingSwordTransform.get(sword,Minecraft.getInstance().getFrameTime());
        // Shift the distance query rather than impose a new render distance;
        // retain vanilla/entity-distance options and other visibility policies.
        boolean inRange=entity.shouldRender(x+entity.getX()-f.position().x,y+entity.getY()-f.position().y,z+entity.getZ()-f.position().z);
        callback.setReturnValue(inRange&&(entity.noCulling||frustum.isVisible(
                FlightFrame.riderBounds(f.position(),f.yaw(),f.pitch(),f.roll(),sword.riderOffset()))));
    }
}
