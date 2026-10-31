package cn.blockforge.generated.generatedmod.client;

import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Local light follows the rendered path and the same smooth frame as its carrier. */
public final class HuahongFlightPath {
    public record Frame(Vec3 p,Vec3 forward,Vec3 side,Vec3 up) {}
    private final List<SwordFlightTrailHistory.Node> nodes;
    private final Vec3 center,forward,side;

    public HuahongFlightPath(List<SwordFlightTrailHistory.Node> nodes,Vec3 center,Vec3 forward,Vec3 side) {
        this.nodes=nodes;this.center=center;this.forward=forward.normalize();
        this.side=side.subtract(this.forward.scale(side.dot(this.forward))).normalize();
    }

    public Frame frame(Vec3 p,Vec3 direction) {
        Vec3 f=direction.lengthSqr()<1e-10?forward:direction.normalize();
        // Shortest-arc transport preserves the model's roll through vertical
        // flight. A world-up threshold or projecting side alone can flip 90/180 degrees.
        double dot=Mth.clamp(forward.dot(f),-1,1);
        Vec3 axis=forward.cross(f),s=side;
        if(dot>-.999999)s=side.add(axis.cross(side)).add(axis.cross(axis.cross(side)).scale(1/(1+dot)));
        s=s.subtract(f.scale(s.dot(f))).normalize();
        return new Frame(p,f,s,s.cross(f).normalize());
    }

    public Frame behind(double distance) {
        if(distance<=1e-8)return frame(center,forward);
        double original=distance;
        Vec3 p=center,direction=forward;
        for(int i=nodes.size()-1;i>=0;i--){
            var node=nodes.get(i).point();Vec3 next=new Vec3(node.x(),node.y(),node.z());
            double length=p.distanceTo(next);if(length<1e-8)continue;
            direction=p.subtract(next).normalize();
            if(distance<=length)return blend(original,p.lerp(next,distance/length),direction);
            distance-=length;p=next;
        }
        return blend(original,p.subtract(direction.scale(distance)),direction);
    }

    private Frame blend(double distance,Vec3 historical,Vec3 tangent) {
        double t=Mth.clamp(distance/1.25,0,1);t=t*t*(3-2*t);
        Vec3 p=center.subtract(forward.scale(distance)).lerp(historical,t);
        return frame(p,forward.lerp(tangent,t));
    }
}
