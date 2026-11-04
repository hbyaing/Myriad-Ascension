package cn.blockforge.generated.generatedmod.client.qa.myriad;
import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.lang.reflect.Method;
import java.util.*;

/** Observe actual emitter rows, before pixel widening; never changes production output. */
@Mod.EventBusSubscriber(modid="wuxingxiantu",value=Dist.CLIENT)
public final class HuahongJoinProbe {
 private static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.flightJoinStress");
 private static final double[] DISTANCES={.75,1,1.25,1.5,1.75,2,2.25,2.5,3,4,5};
 private static final double[] coverage=new double[DISTANCES.length];
 private static final double[] mainCoverage=new double[DISTANCES.length];
 private static int strips;private static double axisError;
 private record Access(Method point,Method width,Method alpha){}
 private static final Map<Class<?>,Access> ACCESS=new HashMap<>();
 @SubscribeEvent public static void begin(TickEvent.RenderTickEvent e){if(ENABLED&&e.phase==TickEvent.Phase.START){Arrays.fill(coverage,0);Arrays.fill(mainCoverage,0);strips=0;axisError=0;}}
 private static Access access(Object row){return ACCESS.computeIfAbsent(row.getClass(),c->{try{Method p;try{p=c.getDeclaredMethod("p");}catch(NoSuchMethodException ex){p=c.getDeclaredMethod("point");}var w=c.getDeclaredMethod("width");var a=c.getDeclaredMethod("alpha");p.setAccessible(true);w.setAccessible(true);a.setAccessible(true);return new Access(p,w,a);}catch(Exception ex){throw new AssertionError(ex);}});}
 public static void observe(List<?> rows,double baseWidth,float opacity,int mode){
  if(!ENABLED||rows.size()<2||!(mode==6||mode==8||mode==10||mode==22||mode==34))return;
  var mc=Minecraft.getInstance();if(mc.player==null||!(mc.player.getVehicle() instanceof ControlledSwordEntity sword))return;
  var f=InterpolatedFlyingSwordTransform.get(sword,mc.getFrameTime());if(f.huahong()<.99999F)return;
  Vec3 center=f.position().add(0,.85,0),forward=f.forward();
  try{
   Access a=access(rows.get(0));double[] distances=new double[rows.size()],energies=new double[rows.size()];Vec3[] points=new Vec3[rows.size()];
   for(int i=0;i<rows.size();i++){Object r=rows.get(i);points[i]=(Vec3)a.point.invoke(r);distances[i]=-points[i].subtract(center).dot(forward);energies[i]=Math.max(0,((Number)a.width.invoke(r)).doubleValue()*baseWidth*HuahongHeadBridge.BODY_SCALE*((Number)a.alpha.invoke(r)).doubleValue()*opacity);}
   for(int k=0;k<DISTANCES.length;k++)for(int i=1;i<rows.size();i++){
    double x=distances[i-1],y=distances[i],d=DISTANCES[k];if((d-x)*(d-y)>0||Math.abs(y-x)<1e-9)continue;
    double t=(d-x)/(y-x);Vec3 p=points[i-1].lerp(points[i],t);double offset=p.distanceTo(center.subtract(forward.scale(d)));
    // During a U-turn a distant arm can cross the same infinite plane. It is
    // not the head junction: measure only the local 25cm axial neighbourhood.
    if(offset>.25)continue;
    coverage[k]+=energies[i-1]+(energies[i]-energies[i-1])*t;
    if(mode==34||mode==6&&Math.abs(baseWidth-.0798)<1e-6){mainCoverage[k]+=energies[i-1]+(energies[i]-energies[i-1])*t;axisError=Math.max(axisError,offset);}
    break;
   }
   strips++;
  }catch(Exception ex){throw new AssertionError("Join row observation failed",ex);}
 }
 public static Map<String,Object> snapshot(){return Map.of("distances",DISTANCES.clone(),"coverage",coverage.clone(),"mainCoverage",mainCoverage.clone(),"strips",strips,"mainAxisError",axisError,"scope","Actual submitted emitter rows: sum physical half-width times vertex alpha/strip opacity at head-relative sections. mainCoverage isolates mode34 or prism main mode6 width .0798; coverage includes age-varying filaments. Integrated geometric support, not shader luminance, projected pixel width, or optical occlusion.");}
 private HuahongJoinProbe(){}
}
