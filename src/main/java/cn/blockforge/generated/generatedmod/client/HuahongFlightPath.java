package cn.blockforge.generated.generatedmod.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/** Local light follows the rendered path and the same smooth frame as its carrier. */
public final class HuahongFlightPath {
    public record Frame(Vec3 p,Vec3 forward,Vec3 side,Vec3 up) {}
    private static final double SMOOTH_RADIUS=.25,HEAD_JOIN=1.25;
    private static final double FRAME_STEP=.35,MIN_FRAME_STEP=.35/32,MAX_TURN_COS=Math.cos(Math.toRadians(18));
    private static final int MAX_BASE_STATIONS=1536,MAX_ADAPTIVE_STATIONS=4096;
    private record Pose(Vec3 p,Vec3 tangent) {}
    private record Station(double distance,Vec3 p,Vec3 forward,Vec3 side,Quaterniond rotation) {}
    private final Vec3 center,forward,side;
    private final Vec3[] points,integrals;
    private final double[] distances;
    private final Vec3 tailDirection;
    private final Map<Vec3,Double> nodeDistances=new HashMap<>();
    private final Map<Vec3,Frame> nodeFrames=new HashMap<>();
    private final Station[] stations;
    private final double[] stationDistances;

    public HuahongFlightPath(List<SwordFlightTrailHistory.Node> nodes,Vec3 center,Vec3 forward,Vec3 side) {
        this.center=finite(center)?center:Vec3.ZERO;this.forward=unit(forward,new Vec3(0,0,1));
        Vec3 transverse=side.subtract(this.forward.scale(side.dot(this.forward)));
        this.side=unit(transverse,this.forward.cross(Math.abs(this.forward.y)>.95?new Vec3(0,0,1):new Vec3(0,1,0)).normalize());
        // Cache relative coordinates and the exact integral of the recorded
        // polyline. Relative coordinates retain precision far from world zero.
        var path=new ArrayList<Vec3>();path.add(Vec3.ZERO);nodeDistances.put(this.center,0.0);
        double arc=0;
        for(int i=nodes.size()-1;i>=0;i--){
            var p=nodes.get(i).point();Vec3 world=new Vec3(p.x(),p.y(),p.z()),next=world.subtract(this.center);
            double length=next.distanceTo(path.get(path.size()-1));
            if(finite(next)&&Double.isFinite(length)&&length>1e-8){path.add(next);arc+=length;}
            if(finite(world))nodeDistances.putIfAbsent(world,arc);
        }
        points=path.toArray(Vec3[]::new);distances=new double[points.length];integrals=new Vec3[points.length];integrals[0]=Vec3.ZERO;
        for(int i=1;i<points.length;i++){
            double length=points[i].distanceTo(points[i-1]);distances[i]=distances[i-1]+length;
            integrals[i]=integrals[i-1].add(points[i-1].add(points[i]).scale(length*.5));
        }
        tailDirection=points.length>1?points[points.length-2].subtract(points[points.length-1]).normalize():this.forward;
        // Transport over successive curve sections, never directly from the head
        // to a distant, potentially opposite tangent. Double reflection supplies
        // the rotation-minimizing frame without an antipodal-angle branch.
        var frames=new ArrayList<Station>();
        frames.add(new Station(0,Vec3.ZERO,this.forward,this.side,rotation(this.forward,this.side)));
        double end=Math.max(8,distances[distances.length-1]+SMOOTH_RADIUS);
        // Keep the grid anchored to the head. Rescaling every interval when the
        // oldest node expires would move all near-head transport samples at once.
        double step=Math.max(FRAME_STEP,end/MAX_BASE_STATIONS);
        int intervals=Math.min(MAX_BASE_STATIONS,(int)Math.floor(end/step));
        for(int i=1;i<=intervals;i++)appendStation(frames,i*step,0);
        if(end-frames.get(frames.size()-1).distance()>1e-8)appendStation(frames,end,0);
        stations=frames.toArray(Station[]::new);stationDistances=new double[stations.length];
        for(int i=0;i<stations.length;i++)stationDistances[i]=stations[i].distance();
    }

    public Frame frame(Vec3 p,Vec3 direction) {
        Vec3 position=finite(p)?p:center;
        if(points.length==1){
            Vec3 f=unit(direction,forward),s=orthogonal(side,f,forward.cross(side));
            return new Frame(position,f,s,s.cross(f).normalize());
        }
        // Historical callers provide exact node positions. Their smooth curve
        // frame owns the orientation; recomputing from a raw corner tangent would
        // reintroduce the independent 180-degree swing this cache removes.
        Double exact=nodeDistances.get(position);
        if(exact!=null)return nodeFrames.computeIfAbsent(position,key->atPosition(key,exact));
        return atPosition(position,nearestDistance(position,Double.POSITIVE_INFINITY));
    }

    private Frame atPosition(Vec3 position,double distance) {
        Frame basis=orientation(distance);
        return new Frame(position,basis.forward(),basis.side(),basis.up());
    }

    /** One of two crossed local-head ribbons, independent of the chase view.
     * A normalized camera cross product has no continuous limit on the axis. */
    public Vec3 ribbonSide(Vec3 p,Vec3 direction,Vec3 eye){
        // Local rows run from head to tail, opposite the flight direction.
        // Reverse that convention once; a dot-product hemisphere switch would
        // introduce a fresh discontinuity when a tightly curved tail passes 90°.
        Vec3 axis=unit(direction.scale(-1),forward);
        if(points.length==1)return frame(p,axis).side();
        Frame basis=orientation(nearestDistance(finite(p)?p:center,8));
        return orthogonal(basis.side(),axis,basis.up());
    }

    public Frame behind(double distance) {
        double d=Double.isFinite(distance)?Math.max(0,distance):0;
        if(d<=1e-8)return new Frame(center,forward,side,side.cross(forward).normalize());
        Pose pose=curve(d);Frame basis=orientation(d,pose.tangent());
        return new Frame(center.add(pose.p()),basis.forward(),basis.side(),basis.up());
    }

    private Pose curve(double distance) {
        double a=distance-SMOOTH_RADIUS,b=distance+SMOOTH_RADIUS;
        // A box integral of a continuous polyline is C1. Its exact derivative
        // stays continuous when the moving head crosses a recorded corner;
        // using the current segment's direction made offset sheets snap there.
        Vec3 smooth=integral(b).subtract(integral(a)).scale(1/(2*SMOOTH_RADIUS));
        Vec3 derivative=point(b).subtract(point(a)).scale(1/(2*SMOOTH_RADIUS));
        if(!finite(smooth))smooth=point(distance);
        double q=Mth.clamp(distance/HEAD_JOIN,0,1),blend=q*q*(3-2*q);
        double rate=q>=1?0:6*q*(1-q)/HEAD_JOIN;
        Vec3 straight=forward.scale(-distance);
        Vec3 p=straight.lerp(smooth,blend);
        Vec3 tangent=forward.scale(1-blend).subtract(derivative.scale(blend)).subtract(smooth.subtract(straight).scale(rate));
        return new Pose(finite(p)?p:straight,unit(tangent,forward));
    }

    private void appendStation(ArrayList<Station> frames,double distance,int depth) {
        Station previous=frames.get(frames.size()-1);Pose next=curve(distance);
        if(depth<5&&frames.size()<MAX_ADAPTIVE_STATIONS&&distance-previous.distance()>MIN_FRAME_STEP
                &&previous.forward().dot(next.tangent())<MAX_TURN_COS){
            appendStation(frames,(previous.distance()+distance)*.5,depth+1);
            appendStation(frames,distance,depth+1);return;
        }
        Vec3 displacement=next.p().subtract(previous.p()),s=previous.side(),f=previous.forward();
        double length=displacement.lengthSqr();
        if(Double.isFinite(length)&&length>1e-18){
            s=s.subtract(displacement.scale(2*displacement.dot(s)/length));
            f=f.subtract(displacement.scale(2*displacement.dot(f)/length));
        }
        Vec3 reflection=next.tangent().subtract(f);length=reflection.lengthSqr();
        if(Double.isFinite(length)&&length>1e-18)s=s.subtract(reflection.scale(2*reflection.dot(s)/length));
        s=orthogonal(s,next.tangent(),previous.side());
        Quaterniond rotation=rotation(next.tangent(),s);
        frames.add(new Station(distance,next.p(),next.tangent(),s,rotation));
    }

    private Frame orientation(double distance) {
        return orientation(distance,curve(distance).tangent());
    }

    private Frame orientation(double distance,Vec3 tangent) {
        int i=Arrays.binarySearch(stationDistances,distance);
        if(i<0)i=Math.max(0,-i-2);
        Station a=stations[i];
        if(i==stations.length-1||distance<=a.distance()+1e-12)
            return align(a.forward(),a.side(),tangent);
        Station b=stations[i+1];double t=Mth.clamp((distance-a.distance())/(b.distance()-a.distance()),0,1);
        Quaterniond q=new Quaterniond(a.rotation()).slerp(b.rotation(),t).normalize();
        Vector3d x=q.transform(new Vector3d(1,0,0)),z=q.transform(new Vector3d(0,0,1));
        Vec3 f=unit(new Vec3(z.x,z.y,z.z),a.forward()),s=orthogonal(new Vec3(x.x,x.y,x.z),f,a.side());
        return align(f,s,tangent);
    }

    private static Frame align(Vec3 from,Vec3 side,Vec3 tangent) {
        // Interpolate roll, then match the exact C1 tangent. Otherwise adding an
        // adaptive station could change interpolated tilt at a distant query.
        // This correction is between neighbouring sections, never head-to-tail.
        Vec3 axis=from.cross(tangent),s=side;double dot=Mth.clamp(from.dot(tangent),-1,1);
        if(dot>-.99)s=s.add(axis.cross(s)).add(axis.cross(axis.cross(s)).scale(1/(1+dot)));
        s=orthogonal(s,tangent,side);
        return new Frame(Vec3.ZERO,tangent,s,s.cross(tangent).normalize());
    }

    private double nearestDistance(Vec3 p,double limit) {
        Double exact=nodeDistances.get(p);
        if(exact!=null)return Math.min(exact,limit);
        Vec3 relative=p.subtract(center);double best=Double.POSITIVE_INFINITY,found=0;
        for(int i=0;i<points.length-1&&distances[i]<limit;i++){
            Vec3 delta=points[i+1].subtract(points[i]);double length=distances[i+1]-distances[i];
            double t=Mth.clamp(relative.subtract(points[i]).dot(delta)/(length*length),0,Math.min(1,(limit-distances[i])/length));
            double error=points[i].add(delta.scale(t)).distanceToSqr(relative);
            if(error<best){best=error;found=distances[i]+length*t;}
        }
        return found;
    }

    private static Quaterniond rotation(Vec3 f,Vec3 s) {
        Vec3 up=f.cross(s).normalize();
        return new Quaterniond().setFromNormalized(new Matrix3d(s.x,s.y,s.z,up.x,up.y,up.z,f.x,f.y,f.z)).normalize();
    }

    private static Vec3 orthogonal(Vec3 candidate,Vec3 f,Vec3 fallback) {
        Vec3 s=candidate.subtract(f.scale(candidate.dot(f)));
        if(finite(s)&&Double.isFinite(s.lengthSqr())&&s.lengthSqr()>1e-12)return s.normalize();
        s=fallback.subtract(f.scale(fallback.dot(f)));
        if(finite(s)&&Double.isFinite(s.lengthSqr())&&s.lengthSqr()>1e-12)return s.normalize();
        Vec3 axis=Math.abs(f.x)<.8?new Vec3(1,0,0):new Vec3(0,1,0);
        return unit(f.cross(axis),new Vec3(0,0,1));
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
    private static Vec3 unit(Vec3 p,Vec3 fallback){return finite(p)&&Double.isFinite(p.lengthSqr())&&p.lengthSqr()>1e-10?p.normalize():fallback;}
}
