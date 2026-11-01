package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.realmflight.MeteorBloomRenderer;
import cn.blockforge.generated.generatedmod.client.qa.myriad.OrdinaryHiltQa;
import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=MeteorBloomRenderer.class,remap=false)
public abstract class OrdinaryHiltBloomLayerMixin {
    @Inject(method="drawSword",at=@At("HEAD"),cancellable=true,remap=false)
    private static void layer(BufferBuilder.RenderedBuffer mesh,Matrix4f projection,Matrix4f view,Matrix4f inverseProjection,CallbackInfo ci){
        OrdinaryHiltQa.bloomAttempt(mesh);
        if(OrdinaryHiltQa.ENABLED&&OrdinaryHiltQa.suppressBloom()){if(mesh!=null)mesh.release();ci.cancel();}
    }
}
