package cn.blockforge.generated.generatedmod.client.qa.myriad;
import cn.blockforge.generated.generatedmod.client.*;
import java.util.*;

/** Actual world-parcel consumer under divergent future headings and subframe aging. */
final class HuahongSmokeWakeQa {
 static Map<String,Object> verify(){
  var common=new ArrayList<SwordFlightTrailHistory.Node>();
  for(int i=0;i<=640;i++){double arc=i*.25;common.add(new SwordFlightTrailHistory.Node(new SwordFlightTrailHistory.Point(0,0,arc),100-(160-arc)/15,arc));}
  var straight=new ArrayList<>(common);var turned=new ArrayList<>(common);
  for(int i=1;i<=60;i++){double d=i*.25;straight.add(new SwordFlightTrailHistory.Node(new SwordFlightTrailHistory.Point(0,0,160+d),100+d/15,160+d));turned.add(new SwordFlightTrailHistory.Node(new SwordFlightTrailHistory.Point(d,0,160),100+d/15,160+d));}
  var a=HuahongFlowField.worldParcels(straight,101.25);var b=new HashMap<Long,HuahongFlowField.WorldParcel>();for(var p:HuahongFlowField.worldParcels(turned,101.25))b.put(p.id(),p);
  int matches=0;double error=0;for(var p:a){var q=b.get(p.id());if(q==null||p.birthTick()>=99.9)continue;error=Math.max(error,Math.sqrt(Math.pow(p.x()-q.x(),2)+Math.pow(p.y()-q.y(),2)+Math.pow(p.z()-q.z(),2)));matches++;}
  if(matches<40||error>1e-10)throw new AssertionError("Old smoke rotates with future head: "+matches+" / "+error);
  var next=new HashMap<Long,HuahongFlowField.WorldParcel>();for(var p:HuahongFlowField.worldParcels(turned,101.25+20./120))next.put(p.id(),p);double step=0,motion=0;
  for(var p:b.values()){var q=next.get(p.id());if(q==null)continue;double d=Math.sqrt(Math.pow(p.x()-q.x(),2)+Math.pow(p.y()-q.y(),2)+Math.pow(p.z()-q.z(),2));step=Math.max(step,d);motion+=d;}
  if(step>.03||motion<1e-4)throw new AssertionError("World smoke drift frozen/discontinuous: "+step+" / "+motion);
  double farthest=a.stream().mapToDouble(HuahongFlowField.WorldParcel::behind).max().orElse(0);
  if(farthest<26||farthest>44||a.get(0).alpha()>.005)throw new AssertionError("Smoke tail has no short, faded world-space ending: "+farthest);
  double crimsonStep=0,radiusStep=0;
  for(int i=1;i<a.size();i++){
   var p=a.get(i-1);var q=a.get(i);
   crimsonStep=Math.max(crimsonStep,Math.abs(p.crimson()-q.crimson()));
   radiusStep=Math.max(radiusStep,Math.abs(p.radius()-q.radius()));
  }
  if(crimsonStep>.01||radiusStep>.01)throw new AssertionError("Adjacent smoke coverage parcels still produce high-frequency pulses");
  return Map.of("passed",true,"matchedPastBirths",matches,"maxPastBirthPositionErrorAfterTurn",error,"max120FpsWorldDrift",step,"nonzeroDrift",motion,"maxAdjacentCrimsonStep",crimsonStep,"maxAdjacentRadiusStep",radiusStep,"farthestWorldParcel",farthest,"scope","Actual world parcels on straight and turning flight history. Old world positions stay fixed through a future turn, adjacent fast-flight coverage samples blend coherently, and the shortened endpoint fades.");
 }
 private HuahongSmokeWakeQa(){}
}
