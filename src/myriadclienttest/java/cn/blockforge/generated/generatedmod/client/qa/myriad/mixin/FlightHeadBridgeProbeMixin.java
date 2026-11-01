package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;
import cn.blockforge.generated.generatedmod.client.SwordFlightTrailHistory;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightHeadProbe;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
@Pseudo @Mixin(targets="cn.blockforge.generated.generatedmod.client.HuahongHeadBridge",remap=false)
public abstract class FlightHeadBridgeProbeMixin {
    @Inject(method="presentationNodes(Ljava/util/List;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;F)Ljava/util/List;",at=@At("RETURN"),remap=false,require=0)
    private static void observe(List<SwordFlightTrailHistory.Node> raw,Vec3 center,Vec3 forward,float weight,CallbackInfoReturnable<List<SwordFlightTrailHistory.Node>> result){FlightHeadProbe.bridge(raw,result.getReturnValue(),center,forward,weight);}
}
