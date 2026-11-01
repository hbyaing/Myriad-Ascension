package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.qa.myriad.JindouVisibilityQa;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=EntityRenderDispatcher.class,priority=500,remap=false)
public abstract class JindouVisibilityProbeMixin {
    @Inject(method={"shouldRender","m_114397_"},at=@At("HEAD"),remap=false)
    private void observe(Entity e,Frustum f,double x,double y,double z,CallbackInfoReturnable<Boolean> ci){JindouVisibilityQa.cull(e,f,x,y,z);}
}
