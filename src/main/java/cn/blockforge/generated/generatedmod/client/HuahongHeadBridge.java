package cn.blockforge.generated.generatedmod.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Presentation filtering and luminous support; original history is never rewritten. */
public final class HuahongHeadBridge {
    public static final double FIXED_HEAD_LENGTH=6;
    public static final double WORLD_HISTORY_DISTANCE=12;
    private HuahongHeadBridge(){}
    /** Attachment settles below visible opacity; brightness keeps its own lifecycle. */
    public static float attachmentWeight(float strength){
        if(!Float.isFinite(strength)||strength<=0)return 0;
        float t=Math.min(1,strength/.015F);return t*t*(3-2*t);
    }
    /** Shared transverse enlargement; head length and historical path stay fixed. */
    public static final double WIDTH_MULTIPLIER=2;
    public static final double BODY_SCALE=1.8*WIDTH_MULTIPLIER;
    /** The shortest luminous head ends 1.965m behind its attachment. Its
     * history must already have full support there, rather than fading in
     * through 3.5m and leaving a thin unsupported neck between the two. */
    public static float luminousCoreJoin(double behind){
        double q=Math.max(0,Math.min(1,behind-.5));return (float)(q*q*(3-2*q));
    }
    /** One continuous luminous nose/body envelope, full width from 0.75m aft. */
    public static double luminousEnvelope(double behind){
        double q=Math.max(0,Math.min(1,(behind+.65)/1.4));return q*q*(3-2*q);
    }
    /** Rigid head support belongs to the active carrier, not an old stationary
     * history birth. Blend back to real age only as the head joins world history. */
    public static double luminousSupportAge(double age,double behind){
        double q=Math.max(0,Math.min(1,(behind-FIXED_HEAD_LENGTH)/(WORLD_HISTORY_DISTANCE-FIXED_HEAD_LENGTH)));
        return Math.max(0,age)*q*q*(3-2*q);
    }


    /** Preserve source order, time and material arc; filter only the drawing copy. */
    public static List<SwordFlightTrailHistory.Node> presentationNodes(
            List<SwordFlightTrailHistory.Node> source,Vec3 center,Vec3 forward){
        return presentationNodes(source,center,forward,1);
    }
    /** Weight follows the rendered lifecycle; zero retains the exact source. */
    public static List<SwordFlightTrailHistory.Node> presentationNodes(
            List<SwordFlightTrailHistory.Node> source,Vec3 center,Vec3 forward,float weight){
        if(!Float.isFinite(weight)||weight<=0||source.isEmpty()||!finite(center)||!finite(forward))return source;
        var curve=new FlightTrailCurve(source,center,forward);double arc=source.get(source.size()-1).arc();
        var result=new ArrayList<SwordFlightTrailHistory.Node>(source.size());
        for(var n:source){var p=n.point();Vec3 raw=new Vec3(p.x(),p.y(),p.z());
            Vec3 shown=raw.lerp(curve.at(arc-n.arc()).point(),Math.min(1,weight));
            result.add(new SwordFlightTrailHistory.Node(new SwordFlightTrailHistory.Point(shown.x,shown.y,shown.z),n.tick(),n.arc()));
        }
        return result;
    }
    /** Follow actual motion all the way to the pommel; do not impose a steering-axis hook. */
    public static List<SwordFlightTrailHistory.Node> swordRootNodes(
            List<SwordFlightTrailHistory.Node> source,Vec3 center,Vec3 forward){
        return presentationNodes(source,center,forward,1);
    }
    private static boolean finite(Vec3 p){return p!=null&&Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z);}
}
