package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.client.SwordFlightTrailHistory;
import cn.blockforge.generated.generatedmod.client.SwordTrailPhase;
import java.util.*;

/** Real history regression at the user's old-world tick magnitude, independent of GL. */
public final class FlightLargeTickQa {
    private static final long USER_WORLD_TICK=10_200_336L;
    private static final double WINDOW=16,TOLERANCE=1e-5;
    private record Measure(double ahead,double tailError,double visibleLength,double[] profile){}
    private static Measure measure(SwordFlightTrailHistory history,double now,SwordFlightTrailHistory.Point head){
        var nodes=history.renderNodes(now,head);
        if(nodes.size()<2)throw new AssertionError("History unexpectedly empty");
        double arc=nodes.get(nodes.size()-1).arc(),cut=arc-WINDOW,ahead=0;
        for(var node:nodes)ahead=Math.max(ahead,node.point().x()-head.x());
        int i=0;while(i<nodes.size()-2&&nodes.get(i+1).arc()<cut)i++;
        var a=nodes.get(i);var b=nodes.get(i+1);
        if(cut<a.arc()-1e-6)throw new AssertionError("Fixture has insufficient trailing support");
        double fraction=Math.max(0,Math.min(1,(cut-a.arc())/Math.max(1e-12,b.arc()-a.arc())));
        double tail=a.point().lerp(b.point(),fraction).x();
        double[] profile=new double[10];int station=0;
        for(double behind:new double[]{.5,1,4,8,16}){
            double target=arc-behind;int j=0;while(j<nodes.size()-2&&nodes.get(j+1).arc()<target)j++;
            var x=nodes.get(j);var y=nodes.get(j+1);double t=Math.max(0,Math.min(1,(target-x.arc())/Math.max(1e-12,y.arc()-x.arc())));
            profile[station++]=head.x()-x.point().lerp(y.point(),t).x();
            profile[station++]=now-(x.tick()+(y.tick()-x.tick())*t);
        }
        return new Measure(ahead,Math.abs(tail-(head.x()-WINDOW)),Math.max(0,head.x()-tail),profile);
    }
    public static Map<String,Object> verify(){
        var cases=new ArrayList<Object>();int checks=0;double maxCorrectedAhead=0,maxCorrectedTailError=0,userBaselineAhead=0,userBaselineSpan=0,maxAgeCovariance=0;
        for(long origin:new long[]{0,1000,1L<<20,1L<<22,USER_WORLD_TICK,1L<<24,1L<<26,50_000_000L})
            for(double speed:new double[]{.2,3,7,15,30})for(int fps:new int[]{60,120}){
                var history=new SwordFlightTrailHistory(.25,116,160,720);
                var reference=new SwordFlightTrailHistory(.25,116,160,720);
                for(int n=0;n<=80;n++){history.sample(new SwordFlightTrailHistory.Point(n*speed,0,0),origin+n,1000);reference.sample(new SwordFlightTrailHistory.Point(n*speed,0,0),1000+n,1000);}
                double oldAhead=0,oldMin=Double.POSITIVE_INFINITY,oldMax=0,fixedAhead=0,fixedTail=0,maxClockError=0;
                int phases=fps/20;
                for(int tickIndex=81;tickIndex<=84;tickIndex++){
                    history.sample(new SwordFlightTrailHistory.Point(tickIndex*speed,0,0),origin+tickIndex,1000);reference.sample(new SwordFlightTrailHistory.Point(tickIndex*speed,0,0),1000+tickIndex,1000);
                    for(int phase=0;phase<phases;phase++){
                        float partial=phase/(float)phases;
                        var head=new SwordFlightTrailHistory.Point((tickIndex-1)*speed,0,0).lerp(new SwordFlightTrailHistory.Point(tickIndex*speed,0,0),(double)partial);
                        long tick=origin+tickIndex;
                        // Freeze the defective production expression: long + float
                        // rounds BEFORE assignment to double.
                        double oldNow=tick-1+partial;
                        double now=SwordTrailPhase.renderTick(tick,partial);
                        var before=measure(history,oldNow,head);var after=measure(history,now,head);
                        var ref=measure(reference,SwordTrailPhase.renderTick(1000+tickIndex,partial),head);
                        for(int j=0;j<after.profile().length;j++)maxAgeCovariance=Math.max(maxAgeCovariance,Math.abs(after.profile()[j]-ref.profile()[j]));
                        oldAhead=Math.max(oldAhead,before.ahead());oldMin=Math.min(oldMin,before.visibleLength());oldMax=Math.max(oldMax,before.visibleLength());
                        fixedAhead=Math.max(fixedAhead,after.ahead());fixedTail=Math.max(fixedTail,after.tailError());maxClockError=Math.max(maxClockError,Math.abs(oldNow-now));checks++;
                    }
                }
                if(fixedAhead>TOLERANCE||fixedTail>TOLERANCE)throw new AssertionError("Corrected render time still deforms real trail: tick="+origin+" speed="+speed+" fps="+fps+" ahead="+fixedAhead+" tail="+fixedTail);
                maxCorrectedAhead=Math.max(maxCorrectedAhead,fixedAhead);maxCorrectedTailError=Math.max(maxCorrectedTailError,fixedTail);
                if(origin==USER_WORLD_TICK){userBaselineAhead=Math.max(userBaselineAhead,oldAhead);userBaselineSpan=Math.max(userBaselineSpan,oldMax-oldMin);}
                cases.add(Map.of("worldTickOrigin",origin,"blocksPerTick",speed,"fps",fps,"oldMaxClockErrorTicks",maxClockError,"oldMaxForwardOfAttachment",oldAhead,"oldVisibleLengthMinMax",List.of(oldMin,oldMax),"correctedMaxForwardOfAttachment",fixedAhead,"correctedMaxTailBoundaryError",fixedTail));
            }
        if(maxAgeCovariance>TOLERANCE)throw new AssertionError("Trail local geometry/age changes with world clock origin: "+maxAgeCovariance);
        if(userBaselineAhead<1||userBaselineSpan<2)throw new AssertionError("Old-world fixture did not reproduce front protrusion and tail-length oscillation");
        var result=new LinkedHashMap<String,Object>();result.put("passed",true);result.put("checks",checks);result.put("userWorldTick",USER_WORLD_TICK);result.put("userAgeBaselineMaxForward",userBaselineAhead);result.put("userAgeBaselineLengthRange",userBaselineSpan);result.put("maxCorrectedForward",maxCorrectedAhead);result.put("maxCorrectedTailBoundaryError",maxCorrectedTailError);result.put("maxWorldAgeCovariance",maxAgeCovariance);result.put("cases",cases);result.put("scope","Actual production history and renderTick helper. Same pose, speed, samples and partials; compare frozen long+float expression versus corrected clock. Bounds interpolate the real 16m history window; local geometry and ages are compared with a fresh-world origin1000. No brightness or screenshot thresholds.");return result;
    }
    public static void main(String[] args){System.out.println(verify());}
}