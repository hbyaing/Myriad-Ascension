package cn.blockforge.generated.generatedmod.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Presentation-only bridge from a rigid carrier head back to world history. */
public final class HuahongHeadBridge {
    public static final double FIXED_HEAD_LENGTH=2.5;
    public static final double WORLD_HISTORY_DISTANCE=8;
    private HuahongHeadBridge(){}
    /** Attachment settles below visible opacity; brightness keeps its own lifecycle. */
    public static float attachmentWeight(float strength){
        if(!Float.isFinite(strength)||strength<=0)return 0;
        float t=Math.min(1,strength/.015F);return t*t*(3-2*t);
    }
    public static final double BODY_SCALE=1.8;


    /** Preserve source order, time and material arc. Distant nodes retain their
     * exact instances; neither source history nor its recorded positions change. */
    public static List<SwordFlightTrailHistory.Node> presentationNodes(
            List<SwordFlightTrailHistory.Node> source,Vec3 center,Vec3 forward){
        return presentationNodes(source,center,forward,1);
    }
    /** Weight follows the rendered lifecycle; zero retains the exact source. */
    public static List<SwordFlightTrailHistory.Node> presentationNodes(
            List<SwordFlightTrailHistory.Node> source,Vec3 center,Vec3 forward,float weight){
        if(!Float.isFinite(weight)||weight<=0)return source;
        weight=Math.min(1,weight);
        if(source.isEmpty()||!finite(center)||!finite(forward)||forward.lengthSqr()<1e-10)return source;
        Vec3 direction=forward.normalize();double headArc=source.get(source.size()-1).arc();
        var result=new ArrayList<SwordFlightTrailHistory.Node>(source.size());
        for(var node:source){
            double behind=Math.max(0,headArc-node.arc());
            if(behind>=WORLD_HISTORY_DISTANCE){result.add(node);continue;}
            Vec3 straight=center.subtract(direction.scale(behind));
            double q=Math.max(0,Math.min(1,(behind-FIXED_HEAD_LENGTH)/(WORLD_HISTORY_DISTANCE-FIXED_HEAD_LENGTH)));
            double blend=q*q*(3-2*q);
            var p=node.point();Vec3 world=new Vec3(p.x(),p.y(),p.z()),position=straight.lerp(world,blend);
            if(weight<1)position=world.lerp(position,weight);
            result.add(new SwordFlightTrailHistory.Node(new SwordFlightTrailHistory.Point(position.x,position.y,position.z),node.tick(),node.arc()));
        }
        return result;
    }
    private static boolean finite(Vec3 p){return p!=null&&Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z);}
}
