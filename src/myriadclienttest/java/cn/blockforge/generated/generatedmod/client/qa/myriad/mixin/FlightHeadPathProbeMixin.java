package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;
import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightHeadProbe;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
@Mixin(value=HuahongFlightPath.class,remap=false)
public abstract class FlightHeadPathProbeMixin {
    @Shadow @Final private Vec3 center,forward,side;
    @Shadow @Final private boolean attachedHead;
    @Shadow @Final private double attachmentWeight;
    @Shadow @Final private double[] distances;
    @Inject(method="<init>(Ljava/util/List;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;DZD)V",at=@At("TAIL"),remap=false)
    private void observe(CallbackInfo callback){FlightHeadProbe.path((HuahongFlightPath)(Object)this,center,forward,side,distances.length==0?0:distances[distances.length-1],attachedHead,attachmentWeight);}
}
