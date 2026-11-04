package cn.blockforge.generated.generatedmod.client;

import java.util.*;
import net.minecraft.world.phys.Vec3;

/** Positive, head-anchored filtering of recorded world history. No steering-axis splice. */
public final class FlightTrailCurve {
    public record Sample(Vec3 point,Vec3 forward,Vec3 derivative){}
    private final Vec3 center,fallback;
    private final Vec3[] points,sums,slopes;
    private final double[] distances;
    private final double radius;
    public FlightTrailCurve(List<SwordFlightTrailHistory.Node> nodes,Vec3 center,Vec3 fallback){
        this.center=center;this.fallback=fallback.lengthSqr()>1e-12?fallback.normalize():new Vec3(0,0,1);
        Vec3[] ps=new Vec3[nodes.size()+1];double[] ds=new double[ps.length];int count=1;ps[0]=Vec3.ZERO;
        double head=nodes.isEmpty()?0:nodes.get(nodes.size()-1).arc();
        for(int i=nodes.size()-1;i>=0;i--){var n=nodes.get(i);double d=head-n.arc();
            if(d<=ds[count-1]+1e-9)continue;
            var p=n.point();ps[count]=new Vec3(p.x(),p.y(),p.z()).subtract(center);ds[count++]=d;
        }
        points=Arrays.copyOf(ps,count);distances=Arrays.copyOf(ds,count);
        // These slopes are invariant for the entire render submission. Keep
        // the exact arithmetic order; this changes no curve or derivative.
        slopes=new Vec3[Math.max(1,count-1)];
        if(count==1)slopes[0]=this.fallback.scale(-1);
        else for(int i=0;i<count-1;i++)slopes[i]=points[i+1].subtract(points[i]).scale(1/(distances[i+1]-distances[i]));
        sums=new Vec3[points.length];sums[0]=Vec3.ZERO;
        for(int i=1;i<points.length;i++)sums[i]=sums[i-1].add(points[i-1].add(points[i]).scale((distances[i]-distances[i-1])*.5));
        double recent=nodes.size()<2?0:(head-nodes.get(0).arc())/Math.max(1e-6,nodes.get(nodes.size()-1).tick()-nodes.get(0).tick());
        radius=Math.max(.35,Math.min(4,recent*.5));
    }
    private int segment(double d){int i=Arrays.binarySearch(distances,d);return i>=0?i:Math.max(0,-i-2);}
    private Vec3 slope(int i){return slopes[Math.min(i,slopes.length-1)];}
    private Vec3 point(double d){int i=segment(d);return points[i].add(slope(i).scale(d-distances[i]));}
    private Vec3 integral(double d){int i=segment(d);double x=d-distances[i];return sums[i].add(points[i].scale(x)).add(slope(i).scale(x*x*.5));}
    /** Position-only consumers need no derivative, normal or Sample carrier. */
    public Vec3 positionAt(double distance){
        double d=Double.isFinite(distance)?Math.max(0,distance):0;
        if(d<1e-5)return center.add(slope(0).scale(d));
        double den=Math.sqrt(radius*radius+d*d),r=radius*d/den;
        return center.add(integral(d+r).subtract(integral(d-r)).scale(.5/r));
    }
    public Sample at(double distance){
        double d=Double.isFinite(distance)?Math.max(0,distance):0;
        if(d<1e-5)return new Sample(center.add(slope(0).scale(d)),unit(slope(0).scale(-1)),slope(0));
        // r<=d keeps the entire positive averaging kernel inside real history.
        // Unlike blending an imposed straight segment, it cannot create a hook
        // outside the historical path's convex hull. Its derivative is analytic.
        double den=Math.sqrt(radius*radius+d*d),r=radius*d/den,dr=radius*radius*radius/(den*den*den);
        Vec3 p=integral(d+r).subtract(integral(d-r)).scale(.5/r);
        Vec3 derivative=point(d+r).scale(1+dr).subtract(point(d-r).scale(1-dr)).scale(.5/r).subtract(p.scale(dr/r));
        return new Sample(center.add(p),unit(derivative.scale(-1)),derivative);
    }
    private Vec3 unit(Vec3 p){return p.lengthSqr()>1e-16?p.normalize():fallback;}
}
