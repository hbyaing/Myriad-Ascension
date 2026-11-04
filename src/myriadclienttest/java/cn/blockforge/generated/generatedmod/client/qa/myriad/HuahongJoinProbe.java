package cn.blockforge.generated.generatedmod.client.qa.myriad;
import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.lang.reflect.*;
import java.util.*;

/** Observe the submitted unified primary strip by material arc, including bent paths. */
@Mod.EventBusSubscriber(modid="wuxingxiantu",value=Dist.CLIENT)
public final class HuahongJoinProbe {
 private static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.flightJoinStress");
 private static final double[] DISTANCES={.75,1,1.25,1.5,1.75,2,2.25,2.5,3,4,5,6,8,10,12};
 private static final double[] coverage=new double[DISTANCES.length],widths=new double[DISTANCES.length];
 private static int strips;private static double curveError;
 private record Access(Method point,Method width,Method alpha,Method arc){}
 private static final Map<Class<?>,Access> ACCESS=new HashMap<>();
 @SubscribeEvent public static void begin(TickEvent.RenderTickEvent e){if(ENABLED&&e.phase==TickEvent.Phase.START){Arrays.fill(coverage,0);Arrays.fill(widths,0);strips=0;curveError=0;}}
 private static Access access(Object row){return ACCESS.computeIfAbsent(row.getClass(),c->{try{
  Method p;try{p=c.getDeclaredMethod("p");}catch(NoSuchMethodException ex){p=c.getDeclaredMethod("point");}
  Method u;try{u=c.getDeclaredMethod("u");}catch(NoSuchMethodException ex){u=c.getDeclaredMethod("arc");}
  var w=c.getDeclaredMethod("width");var a=c.getDeclaredMethod("alpha");for(Method m:List.of(p,u,w,a))m.setAccessible(true);return new Access(p,w,a,u);
 }catch(Exception ex){throw new AssertionError(ex);}});}
 @SuppressWarnings("unchecked") public static void observe(List<?> rows,double baseWidth,float opacity,int mode){
  if(!ENABLED||rows.size()<2||!(mode==34||mode==6&&Math.abs(baseWidth-.0798)<1e-6))return;
  var mc=Minecraft.getInstance();if(mc.player==null||!(mc.player.getVehicle() instanceof ControlledSwordEntity sword))return;
  var f=InterpolatedFlyingSwordTransform.get(sword,mc.getFrameTime());if(f.huahong()<.99999F)return;
  try{
   Access a=access(rows.get(0));double headU=((Number)a.arc.invoke(rows.get(rows.size()-1))).doubleValue()-.65;
   var shared=FlyingSwordEffects.class.getDeclaredMethod("sharedHistory",ControlledSwordEntity.class);shared.setAccessible(true);
   var h=(SwordFlightTrailHistory)shared.invoke(null,sword);var field=SwordFlightTrailHistory.class.getDeclaredField("presentation");field.setAccessible(true);
   var source=(List<SwordFlightTrailHistory.Node>)field.get(h);var hp=source.get(source.size()-1).point();Vec3 center=new Vec3(hp.x(),hp.y(),hp.z());var curve=new FlightTrailCurve(source,center,f.forward());
   double[] ds=new double[rows.size()],ws=new double[rows.size()],cs=new double[rows.size()];
   for(int i=0;i<rows.size();i++){Object r=rows.get(i);ds[i]=headU-((Number)a.arc.invoke(r)).doubleValue();ws[i]=((Number)a.width.invoke(r)).doubleValue()*baseWidth*HuahongHeadBridge.BODY_SCALE;cs[i]=ws[i]*((Number)a.alpha.invoke(r)).doubleValue()*opacity;
    if(ds[i]>=0&&ds[i]<=26)curveError=Math.max(curveError,((Vec3)a.point.invoke(r)).distanceTo(curve.at(ds[i]).point()));
   }
   for(int k=0;k<DISTANCES.length;k++)for(int i=1;i<rows.size();i++){
    double x=ds[i-1],y=ds[i],d=DISTANCES[k];if((d-x)*(d-y)>0||Math.abs(y-x)<1e-9)continue;double t=(d-x)/(y-x);
    coverage[k]+=cs[i-1]+(cs[i]-cs[i-1])*t;widths[k]+=ws[i-1]+(ws[i]-ws[i-1])*t;break;
   }
   strips++;
  }catch(Exception ex){throw new AssertionError("Primary arc observation failed",ex);}
 }
 public static Map<String,Object> snapshot(){return Map.of("version",2,"distances",DISTANCES.clone(),"mainCoverage",coverage.clone(),"physicalHalfWidths",widths.clone(),"strips",strips,"mainCurveError",curveError,"scope","Actual submitted primary mode34 or prism mode6 width .0798, sampled by material arc along a curved centreline. Width is geometric; coverage includes vertex alpha, not final shader brightness. Curve error compares actual row positions with the production curve on the current read-only canonical presentation.");}
 private HuahongJoinProbe(){}
}
