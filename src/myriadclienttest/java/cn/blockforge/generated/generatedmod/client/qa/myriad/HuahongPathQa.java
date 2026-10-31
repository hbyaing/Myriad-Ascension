package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.client.HuahongFlightPath;
import cn.blockforge.generated.generatedmod.client.SwordFlightTrailHistory;
import cn.blockforge.generated.generatedmod.yujian.FlightFrame;
import java.util.List;
import java.util.Map;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Exercises the actual production path through the old vertical-frame singularity. */
final class HuahongPathQa {
    static Map<String,Object> verify() {
        int checks=0;double maxJoinStep=0;
        for(float pitch=-90;pitch<=90;pitch+=.25F){
            var rotation=FlightFrame.rotation(37,pitch,29);
            Vec3 forward=new Vec3(rotation.transform(new Vector3f(0,0,1)));
            Vec3 side=new Vec3(rotation.transform(new Vector3f(1,0,0)));
            var path=new HuahongFlightPath(List.of(),Vec3.ZERO,forward,side);
            for(Vec3 direction:List.of(forward,side,forward.scale(-1),forward.add(side).normalize())){
                var frame=path.frame(Vec3.ZERO,direction);
                if(Math.abs(frame.side().lengthSqr()-1)>1e-5||Math.abs(frame.up().lengthSqr()-1)>1e-5
                        ||Math.abs(frame.forward().dot(frame.side()))>1e-5)
                    throw new AssertionError("Unstable huahong frame at pitch "+pitch);
                checks++;
            }
            if(path.behind(0).side().dot(side)<.99999)throw new AssertionError("Head rotated away from its carrier");
            checks++;
        }
        var history=new SwordFlightTrailHistory(.25,116,160,720);
        history.sample(new SwordFlightTrailHistory.Point(-2,0,0),0);
        history.sample(new SwordFlightTrailHistory.Point(0,0,0),1);
        history.sample(new SwordFlightTrailHistory.Point(2,0,0),2);
        Vec3[] previous=new Vec3[21];
        for(int frame=0;frame<=240;frame++){
            double t=frame/240.0;Vec3 center=new Vec3(t*2,.08*Math.sin(Math.PI*t),0);
            var nodes=history.renderNodes(1+t,new SwordFlightTrailHistory.Point(center.x,center.y,center.z));
            Vec3 forward=new Vec3(1,.08*Math.PI/2*Math.cos(Math.PI*t),0).normalize();
            var path=new HuahongFlightPath(nodes,center,forward,new Vec3(0,0,1));
            for(int i=0;i<previous.length;i++){
                var section=path.behind(i*.1);
                if(previous[i]!=null){
                    double step=section.p().distanceTo(previous[i]);maxJoinStep=Math.max(maxJoinStep,step);
                    if(step>.04)throw new AssertionError("Huahong head jumped at a history sample: "+step);
                }
                previous[i]=section.p();checks++;
            }
        }
        // Position alone missed the regression: an offset filament could jump
        // 0.16 blocks while the centerline moved less than 0.001 blocks.
        double maxOffsetStep=0,maxFrameAngle=0;
        int turnChecks=0;
        for(double speed:new double[]{.2,2,10}){
            var turning=new SwordFlightTrailHistory(.25,116,160,720);
            for(int tick=0;tick<=14;tick++)turning.sample(point(circle(tick,speed)),tick,20);
            Vec3[] oldOffsets=new Vec3[6],oldUps=new Vec3[6];
            double[] distances={.15,.8,1.5,2,3,5.5};
            for(int step=0;step<=30000;step++){
                double now=10+step/10000.0;int tick=(int)Math.floor(now);double partial=now-tick;
                Vec3 center=circle(tick,speed).lerp(circle(tick+1,speed),partial);
                double angle=now*.2;
                Vec3 forward=new Vec3(Math.cos(angle),0,Math.sin(angle));
                Vec3 side=new Vec3(-Math.sin(angle),0,Math.cos(angle));
                var nodes=turning.renderNodes(now,point(center));
                var path=new HuahongFlightPath(nodes,center,forward,side);
                for(int i=0;i<distances.length;i++){
                    var section=path.behind(distances[i]);
                    Vec3 offset=section.p().add(section.side().scale(.8)).add(section.up().scale(.4));
                    if(!Double.isFinite(offset.lengthSqr()))throw new AssertionError("Non-finite turning geometry");
                    if(oldOffsets[i]!=null){
                        double jump=offset.distanceTo(oldOffsets[i]);
                        double dot=Math.max(-1,Math.min(1,section.side().dot(oldUps[i])));
                        maxOffsetStep=Math.max(maxOffsetStep,jump);
                        maxFrameAngle=Math.max(maxFrameAngle,Math.toDegrees(Math.acos(dot)));
                        if(jump>.01||dot<.9999)throw new AssertionError("Huahong offset/frame jumped: speed="+speed+", now="+now+", behind="+distances[i]+", offset="+jump+", dot="+dot);
                    }
                    oldOffsets[i]=offset;oldUps[i]=section.side();turnChecks++;
                }
            }
        }
        int viewChecks=0;double maxViewStep=0;
        Vec3 forward=new Vec3(.3,.8,.5).normalize(),side=forward.cross(new Vec3(0,1,0)).normalize();
        var path=new HuahongFlightPath(List.of(),Vec3.ZERO,forward,side);
        Vec3 up=side.cross(forward).normalize();
        for(double distance:new double[]{1,10,1000})for(double sine:new double[]{0,.015,.025,.06,.1,.15,.2,.5,1}){
            Vec3 previousWidth=null;
            for(int step=0;step<=7200;step++){
                double angle=step*Math.PI/3600;
                Vec3 eye=forward.scale(-Math.sqrt(1-sine*sine)*distance)
                        .add(side.scale(Math.cos(angle)*sine*distance)).add(up.scale(Math.sin(angle)*sine*distance));
                Vec3 width=path.ribbonSide(Vec3.ZERO,forward.scale(-1),eye);
                if(Math.abs(width.lengthSqr()-1)>1e-6||Math.abs(width.dot(forward))>1e-6)
                    throw new AssertionError("Invalid local head width near an axial view");
                if(previousWidth!=null){
                    double jump=width.distanceTo(previousWidth);maxViewStep=Math.max(maxViewStep,jump);
                    if(jump>.01)throw new AssertionError("Local head width flipped around flight axis: "+jump+", sine="+sine);
                }
                previousWidth=width;viewChecks++;
            }
        }
        Vec3 previousWidth=null;
        for(int step=0;step<=1700;step++){
            double angle=Math.toRadians(step*.1);
            Vec3 axis=forward.scale(Math.cos(angle)).add(side.add(up).normalize().scale(Math.sin(angle)));
            Vec3 width=path.ribbonSide(Vec3.ZERO,axis.scale(-1),forward.scale(-10));
            if(previousWidth!=null&&width.dot(previousWidth)<.9999)
                throw new AssertionError("Head ribbon frame switched hemisphere at "+Math.toDegrees(angle));
            previousWidth=width;viewChecks++;
        }
        return Map.of("pathChecks",checks,"maxJoinStep",maxJoinStep,"turnChecks",turnChecks,
                "maxOffsetStep",maxOffsetStep,"maxFrameAngleDegrees",maxFrameAngle,"viewChecks",viewChecks,"maxViewStep",maxViewStep);
    }
    private static Vec3 circle(double tick,double speed){
        double radius=speed/.2,angle=tick*.2;
        return new Vec3(radius*Math.sin(angle),0,radius*(1-Math.cos(angle)));
    }
    private static SwordFlightTrailHistory.Point point(Vec3 p){return new SwordFlightTrailHistory.Point(p.x,p.y,p.z);}
    private HuahongPathQa() {}
}
