package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import cn.blockforge.generated.generatedmod.client.qa.myriad.HuahongMotionQa;
import cn.blockforge.generated.generatedmod.client.qa.myriad.FlightHeadProbe;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Carries the production row's actual world/local provenance into submitted vertices. */
@Mixin(targets="cn.blockforge.generated.generatedmod.client.HuahongSkinGeometry",remap=false)
public abstract class HuahongGeometryCaptureMixin {
    @Inject(method="billboard",at=@At("HEAD"),remap=false)
    private void beginBillboard(@Coerce Object row,int mode,CallbackInfo callback){FlightHeadProbe.billboard(row,mode);}
    @Inject(method="billboard",at=@At("RETURN"),remap=false)
    private void endBillboard(@Coerce Object row,int mode,CallbackInfo callback){FlightHeadProbe.endBillboard();}
    @Inject(method="history(DDIIDZ)Ljava/util/List;",at=@At("RETURN"),remap=false)
    private void historyRows(double life,double width,int color,int strand,double spread,boolean oriented,CallbackInfoReturnable<List<?>> callback){HuahongMotionQa.tagWorldRows(callback.getReturnValue());}
    @Inject(method="opacity",at=@At("RETURN"),remap=false)
    private static void derivedRows(List<?> rows,float opacity,CallbackInfoReturnable<List<?>> callback){HuahongMotionQa.tagDerivedRows(rows,callback.getReturnValue());}
    @Inject(method="strip(Ljava/util/List;IZZ)V",at=@At("HEAD"),remap=false)
    private void beginStrip(List<?> rows,int mode,boolean localHead,boolean crossPlane,CallbackInfo callback){HuahongMotionQa.beginGeometryStrip(rows);cn.blockforge.generated.generatedmod.client.qa.myriad.HuahongJoinProbe.observe(rows,1,localHead?.70710677F:1F,mode);}
    @Inject(method="strip(Ljava/util/List;IZZ)V",at=@At("RETURN"),remap=false)
    private void endStrip(List<?> rows,int mode,boolean localHead,boolean crossPlane,CallbackInfo callback){HuahongMotionQa.endStrip();}
}
