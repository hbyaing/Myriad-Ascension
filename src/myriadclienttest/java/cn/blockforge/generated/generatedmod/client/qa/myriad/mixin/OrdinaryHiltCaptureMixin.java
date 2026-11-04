package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.client.qa.myriad.OrdinaryHiltQa;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=FlyingSwordEffects.class,remap=false)
public abstract class OrdinaryHiltCaptureMixin {
    @Shadow private static boolean spiralPass;
    @Inject(method="drawHistory",at=@At("HEAD"),remap=false)
    private static void root(VertexConsumer buffer,PoseStack.Pose pose,java.util.List<SwordFlightTrailHistory.Node> nodes,int start,Vec3 eye,double headArc,double length,double now,double width,float alpha,double origin,Vec3 forward,Vec3 side,CallbackInfo ci){OrdinaryHiltQa.history(nodes,forward,headArc);}
    @Inject(method="vertex",at=@At("HEAD"),remap=false)
    private static void capture(VertexConsumer buffer,PoseStack.Pose pose,Vec3 p,float u,float v,float alpha,CallbackInfo ci){OrdinaryHiltQa.vertex(pose,p,u,v,alpha,spiralPass);}
}
