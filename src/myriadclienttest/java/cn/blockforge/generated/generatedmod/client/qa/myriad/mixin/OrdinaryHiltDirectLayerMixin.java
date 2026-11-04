package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.SwordFlightTrailRenderer;
import cn.blockforge.generated.generatedmod.client.qa.myriad.OrdinaryHiltQa;
import cn.blockforge.generated.generatedmod.client.qa.myriad.HuahongReproCapture;
import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=SwordFlightTrailRenderer.class,remap=false)
public abstract class OrdinaryHiltDirectLayerMixin {
    @Inject(method="drawPost(Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;F)Z",at=@At("HEAD"),cancellable=true,remap=false)
    private static void layer(BufferBuilder.RenderedBuffer mesh,Matrix4f projection,Matrix4f inverse,float time,CallbackInfoReturnable<Boolean> ci){
        HuahongReproCapture.rootLayer("before-direct");
        if(OrdinaryHiltQa.ENABLED&&OrdinaryHiltQa.suppressDirect()){if(mesh!=null)mesh.release();ci.setReturnValue(false);}
    }
    @Inject(method="drawPost(Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;F)Z",at=@At("RETURN"),remap=false)
    private static void after(BufferBuilder.RenderedBuffer mesh,Matrix4f projection,Matrix4f inverse,float time,CallbackInfoReturnable<Boolean> ci){HuahongReproCapture.rootLayer("after-direct");}
}
