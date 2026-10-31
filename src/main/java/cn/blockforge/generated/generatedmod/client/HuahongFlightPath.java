package cn.blockforge.generated.generatedmod.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Local light follows the rendered path and the same smooth frame as its carrier. */
public final class HuahongFlightPath {
    public record Frame(Vec3 p,Vec3 forward,Vec3 side,Vec3 up) {}
    private static final double SMOOTH_RADIUS=.25,HEAD_JOIN=1.25;
    private final Vec3 center,forward,side;
    private final Vec3[] points,integrals;
    private final double[] distances;
    private final Vec3 tailDirection;

    public HuahongFlightPath(List<SwordFlightTrailHistory.Node> nodes,Vec3 center,Vec3 forward,Vec3 side) {
        this.center=center;this.forward=unit(forward,new Vec3(0,0,1));
        Vec3 transverse=side.subtract(this.forward.scale(side.dot(this.forward)));
        this.side=unit(transverse,this.forward.cross(Math.abs(this.forward.y)>.95?new Vec3(0,0,1):new Vec3(0,1,0)).normalize());
        // Cache relative coordinates and the exact integral of the recorded
        // polyline. Relative coordinates retain precision far from world zero.
        var path=new ArrayList<Vec3>();path.add(Vec3.ZERO);
        for(int i=nodes.size()-1;i>=0;i--){
            var p=nodes.get(i).point();Vec3 next=new Vec3(p.x(),p.y(),p.z()).subtract(center);
            if(finite(next)&&next.distanceToSqr(path.get(path.size()-1))>1e-16)path.add(next);
        }
        points=path.toArray(Vec3[]::new);distances=new double[points.length];integrals=new Vec3[points.length];integrals[0]=Vec3.ZERO;
        for(int i=1;i<points.length;i++){
            double length=points[i].distanceTo(points[i-1]);distances[i]=distances[i-1]+length;
            integrals[i]=integrals[i-1].add(points[i-1].add(points[i]).scale(length*.5));
        }
        tailDirection=points.length>1?points[points.length-2].subtract(points[points.length-1]).normalize():this.forward;
    }

    public Frame frame(Vec3 p,Vec3 direction) {
        Vec3 f=unit(direction,forward);
        // Shortest-arc transport preserves the model's roll through vertical
        // flight. A world-up threshold or projecting side alone can flip 90/180 degrees.
        double dot=Mth.clamp(forward.dot(f),-1,1);
        Vec3 axis=forward.cross(f),s=side;
        if(dot>-.999999)s=side.add(axis.cross(side)).add(axis.cross(axis.cross(side)).scale(1/(1+dot)));
        s=s.subtract(f.scale(s.dot(f))).normalize();
        return new Frame(p,f,s,s.cross(f).normalize());
    }

    /** One of two crossed local-head ribbons, independent of the chase view.
     * A normalized camera cross product has no continuous limit on the axis. */
    public Vec3 ribbonSide(Vec3 p,Vec3 direction,Vec3 eye){
        // Local rows run from head to tail, opposite the flight direction.
        // Reverse that convention once; a dot-product hemisphere switch would
        // introduce a fresh discontinuity when a tightly curved tail passes 90°.
        Vec3 axis=unit(direction.scale(-1),forward);
        return frame(p,axis).side();
    }

    public Frame behind(double distance) {
        if(distance<=1e-8)return frame(center,forward);
        double a=distance-SMOOTH_RADIUS,b=distance+SMOOTH_RADIUS;
        // A box integral of a continuous polyline is C1. Its exact derivative
        // stays continuous when the moving head crosses a recorded corner;
        // using the current segment's direction made offset sheets snap there.
        Vec3 smooth=integral(b).subtract(integral(a)).scale(1/(2*SMOOTH_RADIUS));
        Vec3 derivative=point(b).subtract(point(a)).scale(1/(2*SMOOTH_RADIUS));
        double q=Mth.clamp(distance/HEAD_JOIN,0,1),blend=q*q*(3-2*q);
        double rate=q>=1?0:6*q*(1-q)/HEAD_JOIN;
        Vec3 straight=forward.scale(-distance);
        Vec3 p=straight.lerp(smooth,blend);
        Vec3 tangent=forward.scale(1-blend).subtract(derivative.scale(blend)).subtract(smooth.subtract(straight).scale(rate));
        return frame(center.add(p),tangent);
    }

    private int segment(double distance){
        int i=Arrays.binarySearch(distances,distance);
        return i>=0?Math.min(i,points.length-1):-i-2;
    }
    private Vec3 point(double distance){
        if(distance<=0)return forward.scale(-distance);
        int i=segment(distance);
        if(i>=points.length-1)return points[i].subtract(tailDirection.scale(distance-distances[i]));
        return points[i].lerp(points[i+1],(distance-distances[i])/(distances[i+1]-distances[i]));
    }
    private Vec3 integral(double distance){
        if(distance<=0)return forward.scale(-distance*distance*.5);
        int i=segment(distance);double length=distance-distances[i];
        if(i>=points.length-1)return integrals[i].add(points[i].scale(length)).subtract(tailDirection.scale(length*length*.5));
        Vec3 slope=points[i+1].subtract(points[i]).scale(1/(distances[i+1]-distances[i]));
        return integrals[i].add(points[i].scale(length)).add(slope.scale(length*length*.5));
    }
    private static boolean finite(Vec3 p){return Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z);}
    private static Vec3 unit(Vec3 p,Vec3 fallback){return finite(p)&&p.lengthSqr()>1e-10?p.normalize():fallback;}
}
