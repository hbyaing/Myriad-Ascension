package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.client.HuahongFlightPath;
import cn.blockforge.generated.generatedmod.client.SwordFlightTrailHistory;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Final head-body frame cadence, measured at rendering intervals rather than
 * only checking head positions or infinitesimal C0 changes. Test source only. */
public final class FlightSectionCadenceQa {
    private static Vec3 circle(double t,double turn,double radius){return new Vec3(radius*Math.sin(t*turn),0,radius*(1-Math.cos(t*turn)));}
    private static SwordFlightTrailHistory.Point point(Vec3 p){return new SwordFlightTrailHistory.Point(p.x,p.y,p.z);}
    private static Vec3 vec(SwordFlightTrailHistory.Point p){return new Vec3(p.x(),p.y(),p.z());}
    private static double angle(Vec3 a,Vec3 b){return Math.toDegrees(Math.acos(Math.max(-1,Math.min(1,a.normalize().dot(b.normalize())))));}
    public static Map<String,Object> verify(){
        int checks=0;double maxBodyRatio=0,maxSectionRatio=0,maxLocalStep=0;var cases=new ArrayList<Map<String,Object>>();
        for(double speed:new double[]{5,15,30})for(double turnDegrees:new double[]{3,10}){
            double turn=Math.toRadians(turnDegrees),radius=speed/(2*Math.sin(turn*.5));
            var h=new SwordFlightTrailHistory(.25,116,160,720);
            var oldBody=new Vec3[6];var oldSide=new Vec3[6];var oldCross=new Vec3[6];var oldLocal=new Vec3[6];
            double[] behind={0,.25,.75,1.25,2.5,5};double previousTime=Double.NaN,bodyRatio=0,sectionRatio=0;
            for(int tick=0;tick<=10;tick++){
                h.sample(point(circle(tick,turn,radius)),tick,1000);if(tick<5)continue;
                for(int step=0;step<140;step++){
                    double fraction=step/140.0,now=tick-1+fraction;
                    Vec3 center=circle(tick-1,turn,radius).lerp(circle(tick,turn,radius),fraction);
                    double theta=now*turn;Vec3 forward=new Vec3(Math.cos(theta),0,Math.sin(theta));Vec3 side=new Vec3(-Math.sin(theta),0,Math.cos(theta));
                    var nodes=h.renderNodes(now,point(center));var path=new HuahongFlightPath(nodes,center,forward,side);
                    for(int i=0;i<behind.length;i++){
                        var body=path.behind(behind[i]);double target=nodes.get(nodes.size()-1).arc()-behind[i];int n=0;
                        while(n<nodes.size()-2&&nodes.get(n+1).arc()<target)n++;
                        var a=nodes.get(n);var b=nodes.get(n+1);double t=Math.max(0,Math.min(1,(target-a.arc())/Math.max(1e-9,b.arc()-a.arc())));
                        Vec3 world=vec(a.point()).lerp(vec(b.point()),t);
                        var section=path.frame(world,vec(b.point()).subtract(vec(a.point())));
                        if(section.p().distanceTo(world)>1e-10)throw new AssertionError("Historical section center moved");
                        // These are the two real constructed crossed-ribbon width
                        // vectors used by FlyingSwordEffects, not the model tip.
                        Vec3 right=world.add(section.side().scale(.1)).subtract(world.subtract(section.side().scale(.1)));
                        Vec3 cross=world.add(section.up().scale(.1)).subtract(world.subtract(section.up().scale(.1)));
                        Vec3 relative=body.p().subtract(center);Vec3 local=new Vec3(relative.dot(side),relative.y,relative.dot(forward));
                        if(oldBody[i]!=null){
                            double dt=now-previousTime,expected=turnDegrees*dt;
                            double r=angle(oldBody[i],body.forward())/expected;
                            double s=Math.max(angle(oldSide[i],right),angle(oldCross[i],cross))/expected;
                            bodyRatio=Math.max(bodyRatio,r);sectionRatio=Math.max(sectionRatio,s);
                            maxLocalStep=Math.max(maxLocalStep,local.distanceTo(oldLocal[i]));
                            if(r>3.2||s>3.2)throw new AssertionError("20Hz section slew: speed="+speed+", turn="+turnDegrees+", now="+now+", behind="+behind[i]+", body/model="+r+", constructed-width/model="+s);
                        }
                        oldBody[i]=body.forward();oldSide[i]=right;oldCross[i]=cross;oldLocal[i]=local;checks++;
                    }
                    previousTime=now;
                }
            }
            maxBodyRatio=Math.max(maxBodyRatio,bodyRatio);maxSectionRatio=Math.max(maxSectionRatio,sectionRatio);
            cases.add(Map.of("speed",speed,"turnDegreesPerTick",turnDegrees,"bodyAngularRateRatio",bodyRatio,"constructedSectionRateRatio",sectionRatio));
        }
        return Map.of("check","production-head-section-render-cadence","checks",checks,"maxBodyAngularRateRatio",maxBodyRatio,"maxConstructedSectionRateRatio",maxSectionRatio,"maxHeadLocalStep",maxLocalStep,"cases",cases,"passed",true);
    }
    public static void main(String[] args){System.out.println(verify());}
}
