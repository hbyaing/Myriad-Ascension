package cn.blockforge.generated.generatedmod.client.qa.myriad;
import cn.blockforge.generated.generatedmod.client.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Regression of the user's hook, hard corners, and diverging local/history curves. */
public final class HuahongHeadBridgeQa {
 private static int checks;
 private static void check(boolean v,String text){checks++;if(!v)throw new AssertionError(text);}
 private static Vec3 vec(SwordFlightTrailHistory.Node n){var p=n.point();return new Vec3(p.x(),p.y(),p.z());}
 private static List<SwordFlightTrailHistory.Node> fixture(Vec3 origin,boolean corner){
  var rows=new ArrayList<SwordFlightTrailHistory.Node>();
  for(int i=400;i>=0;i--){double d=i*.1;Vec3 p=origin.add(corner?-Math.max(0,Math.min(8,d-4)):0,0,-(corner?Math.min(d,4)+Math.max(0,d-12):d));rows.add(new SwordFlightTrailHistory.Node(new SwordFlightTrailHistory.Point(p.x,p.y,p.z),10200336-d/8,400-d));}
  return rows;
 }
 public static Map<String,Object> verify(){
  checks=0;double oldHook=0,newHook=0,maxSharedError=0,maxDerivativeError=0;
  for(Vec3 origin:List.of(Vec3.ZERO,new Vec3(29999000,11000,29999000)))for(boolean corner:new boolean[]{false,true})for(Vec3 forward:List.of(new Vec3(0,0,1),new Vec3(1,0,1).normalize(),new Vec3(0,1,0),new Vec3(0,0,-1))){
   var source=fixture(origin,corner);var saved=List.copyOf(source);var curve=new FlightTrailCurve(source,origin,forward);
   var shown=HuahongHeadBridge.presentationNodes(source,origin,forward);var path=new HuahongFlightPath(source,origin,forward,new Vec3(1,0,0),Double.POSITIVE_INFINITY,true,1);
   check(source.equals(saved),"Drawing filter mutated canonical world history");check(shown.size()==source.size(),"Filter changed material station count");
   check(HuahongHeadBridge.presentationNodes(source,origin,forward,0)==source,"Zero filter did not retain source");
   for(int i=0;i<source.size();i++){
    var raw=source.get(i);double d=400-raw.arc();check(raw.tick()==shown.get(i).tick()&&raw.arc()==shown.get(i).arc(),"Filter changed sample age/arc");
    double error=vec(shown.get(i)).distanceTo(path.behind(d).p());maxSharedError=Math.max(maxSharedError,error);check(error<1e-7,"Local head and historical ribbon have different centreline positions");
   }
   check(path.behind(0).p().equals(origin),"Rendered path detached from true model attachment");
   for(double d=.01;d<=26;d+=.037){var s=curve.at(d);Vec3 p=s.point().subtract(origin);
    if(!corner){newHook=Math.max(newHook,Math.hypot(p.x,p.y));check(Math.hypot(p.x,p.y)<1e-7,"Steering yaw created a hook in straight actual travel");}
    else{check(p.x<=1e-7&&p.x>=-8-1e-7&&Math.abs(p.y)<1e-7&&p.z<=1e-7,"Smoothing escaped historical convex hull");}
   }
   for(double d:new double[]{.2,.5,1,1.5,2,3.9,4,4.1,6,11.9,12,12.1,18,25}){
    double e=.001;Vec3 numeric=curve.at(d-e).point().subtract(curve.at(d+e).point()).normalize();
    double error=numeric.distanceTo(curve.at(d).forward());maxDerivativeError=Math.max(maxDerivativeError,error);check(error<2e-5,"Curve orientation differs from continuous centreline derivative: "+error);
    check(curve.at(d-e).forward().distanceTo(curve.at(d+e).forward())<.01,"Rendered historical corner is still a hard angle");
   }
   if(!corner)for(double d=.01;d<1.5;d+=.01){double q=Math.max(0,Math.min(1,(d-.5)));double blend=q*q*(3-2*q);Vec3 old=forward.scale(-d).lerp(new Vec3(0,0,-d),blend);oldHook=Math.max(oldHook,Math.hypot(old.x,old.y));}
  }
  check(oldHook>.5,"Fixture failed to reproduce the old forced-axis hook");check(newHook<1e-7,"Filtered path retains the forced-axis hook");
  for(double d=.75;d<=26;d+=.07)check(HuahongHeadBridge.luminousEnvelope(d)==1,"Unified luminous spine has an internal width dip");
  return Map.of("passed",true,"checks",checks,"oldForcedAxisHook",oldHook,"newStraightTravelHook",newHook,"maxLocalHistoryCentrelineError",maxSharedError,"maxCurveDerivativeError",maxDerivativeError,"scope","Production positive-kernel history curve; mismatched steering versus true motion, L-shaped history corners, far-world precision, shared local/history centreline and no internal primary width dip. Visual/video acceptance is separate.");
 }
 private HuahongHeadBridgeQa(){}
}
