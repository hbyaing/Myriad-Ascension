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
        return Map.of("pathChecks",checks,"maxJoinStep",maxJoinStep);
    }
    private HuahongPathQa() {}
}
