package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.HuahongFlightPath;
import cn.blockforge.generated.generatedmod.client.HuahongVisuals;
import cn.blockforge.generated.generatedmod.client.qa.myriad.HuahongMotionQa;
import com.mojang.blaze3d.vertex.*;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=HuahongVisuals.class,remap=false)
public abstract class HuahongPrismCaptureMixin {
    @Inject(method="strip(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/world/phys/Vec3;Ljava/util/List;DFFLcn/blockforge/generated/generatedmod/client/HuahongFlightPath;Z)V",at=@At("HEAD"),remap=false)
    private static void begin(VertexConsumer buffer,PoseStack.Pose pose,Vec3 eye,List<?> rows,double width,float opacity,float mode,HuahongFlightPath path,boolean cross,CallbackInfo callback){HuahongMotionQa.beginPrismStrip(path!=null);}
    @Inject(method="strip(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/world/phys/Vec3;Ljava/util/List;DFFLcn/blockforge/generated/generatedmod/client/HuahongFlightPath;Z)V",at=@At("RETURN"),remap=false)
    private static void end(VertexConsumer buffer,PoseStack.Pose pose,Vec3 eye,List<?> rows,double width,float opacity,float mode,HuahongFlightPath path,boolean cross,CallbackInfo callback){HuahongMotionQa.endStrip();}
}
