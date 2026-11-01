package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.client.HuahongFlightPath;
import cn.blockforge.generated.generatedmod.client.HuahongHeadBridge;
import cn.blockforge.generated.generatedmod.client.SwordFlightTrailHistory;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Geometry invariants on production history/path; no shaders or game client required. */
public final class HuahongHeadBridgeQa {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static SwordFlightTrailHistory.Point point(Vec3 p){return new SwordFlightTrailHistory.Point(p.x,p.y,p.z);}
    private static Vec3 vec(SwordFlightTrailHistory.Point p){return new Vec3(p.x(),p.y(),p.z());}
    private static Vec3 position(double t,double speed,double turn){return turn==0?new Vec3(t*speed,0,0):new Vec3(speed/turn*Math.sin(t*turn),0,speed/turn*(1-Math.cos(t*turn)));}
    private static double error(HuahongFlightPath.Frame a,HuahongFlightPath.Frame b){return Math.max(Math.max(a.p().distanceTo(b.p()),a.forward().distanceTo(b.forward())),Math.max(a.side().distanceTo(b.side()),a.up().distanceTo(b.up())));}
    private static double range(double[] values){return Arrays.stream(values).max().orElse(0)-Arrays.stream(values).min().orElse(0);}
    private static void derivative(HuahongFlightPath path,double d){
        double e=1e-6;var before=path.behind(d-e);var after=path.behind(d+e);var frame=path.behind(d);
        Vec3 difference=before.p().subtract(after.p());Vec3 tangent=difference.scale(1/difference.length());
        check(tangent.distanceTo(frame.forward())<1e-6,"Attached analytic tangent differs from curve derivative at "+d+": "+tangent.distanceTo(frame.forward()));
        check(before.forward().distanceTo(after.forward())<.001,"Attached join has a tangent discontinuity at "+d);
    }
    public static Map<String,Object> verify(){
        checks=0;
        for(float strength:new float[]{.015F,.02F,.05F,.1F,.25F,.5F,1F})check(HuahongHeadBridge.attachmentWeight(strength)==1,"Visible entry did not retain rigid attachment");
        check(HuahongHeadBridge.attachmentWeight(0)==0,"Invisible endpoint changed");
        double oldHeadPulse=0,newHeadPulse=0,windowError=0,defaultError=0;
        for(double speed:new double[]{.2,7,15,30})for(double degrees:new double[]{0,10,35})for(double bank:new double[]{0,20}){
            double turn=Math.toRadians(degrees);var history=new SwordFlightTrailHistory(.25,116,160,720);
            for(int n=0;n<=24;n++)history.sample(point(position(n,speed,turn)),n,1000);
            double[] oldLocal=new double[48],newLocal=new double[48];
            for(int k=0;k<48;k++){
                double partial=k/48.0,t=23+partial,a=t*turn,b=t*Math.toRadians(bank);
                Vec3 center=position(23,speed,turn).lerp(position(24,speed,turn),partial);
                Vec3 forward=new Vec3(Math.cos(a),0,Math.sin(a)),base=new Vec3(-Math.sin(a),0,Math.cos(a));
                Vec3 side=base.scale(Math.cos(b)).add(new Vec3(0,1,0).scale(Math.sin(b))),up=side.cross(forward);
                var source=history.renderNodes(t,point(center));var snapshot=List.copyOf(source);double arc=source.get(source.size()-1).arc();
                var presentation=HuahongHeadBridge.presentationNodes(source,center,forward);
                check(source.equals(snapshot),"Bridge mutated source history");check(presentation.size()==source.size(),"Bridge changed sample identity/order");
                check(HuahongHeadBridge.presentationNodes(source,center,forward,0)==source,"Zero presentation weight did not return exact source");
                for(int i=0;i<source.size();i++){
                    var raw=source.get(i);var shown=presentation.get(i);double d=arc-raw.arc();
                    check(raw.tick()==shown.tick()&&raw.arc()==shown.arc(),"Bridge changed material arc or age");
                    if(d>=8)check(raw==shown,"Distant world history node was replaced");
                    if(d<=2.5)check(vec(shown.point()).distanceTo(center.subtract(forward.scale(d)))<1e-10,"Rigid head presentation moved off carrier");
                }
                var old=new HuahongFlightPath(source,center,forward,side);
                var disabled=new HuahongFlightPath(source,center,forward,side,Double.POSITIVE_INFINITY,false);
                var zero=new HuahongFlightPath(source,center,forward,side,Double.POSITIVE_INFINITY,true,0);
                var full=new HuahongFlightPath(source,center,forward,side,Double.POSITIVE_INFINITY,true);
                var bounded=new HuahongFlightPath(source,center,forward,side,32,true);
                for(double d:new double[]{0,.15,.5,1,2,2.5,3,5,8,12,24,26}){
                    double de=Math.max(error(old.behind(d),disabled.behind(d)),error(old.behind(d),zero.behind(d)));
                    defaultError=Math.max(defaultError,de);check(de==0,"Default/zero-weight path changed");
                    double we=error(full.behind(d),bounded.behind(d));windowError=Math.max(windowError,we);check(we<1e-9,"Bounded attached window changed a visible frame");
                    var frame=full.behind(d);
                    if(d<=2.5){Vec3 relative=frame.p().subtract(center);check(Math.abs(relative.dot(side))<1e-10&&Math.abs(relative.dot(up))<1e-10&&Math.abs(relative.dot(forward)+d)<1e-10,"Attached body-local rigid head position oscillates");check(frame.forward().distanceTo(forward)<1e-12&&frame.side().distanceTo(side)<1e-12,"Attached rigid head basis changed");}
                    if(d>=full.headTransitionLength()){check(frame.p().equals(old.behind(d).p()),"Attached path displaced original far curve");check(frame.forward().equals(old.behind(d).forward()),"Attached path changed original far tangent");}
                }
                oldLocal[k]=old.behind(2).p().subtract(center).dot(base);newLocal[k]=full.behind(2).p().subtract(center).dot(base);
                if(k==17){
                    derivative(full,2.5);derivative(full,full.headTransitionLength());
                    for(double weight:new double[]{.001,.25,.5,.75,.999}){
                        var fade=new HuahongFlightPath(source,center,forward,side,32,true,weight);derivative(fade,2.5);derivative(fade,fade.headTransitionLength());
                        var faded=HuahongHeadBridge.presentationNodes(source,center,forward,(float)weight);
                        for(int i=0;i<source.size();i++){var raw=source.get(i);var shown=faded.get(i);check(raw.arc()==shown.arc()&&raw.tick()==shown.tick(),"Fade changed source clocks");if(arc-raw.arc()>=8)check(raw==shown,"Fade replaced distant node");else check(vec(shown.point()).distanceTo(vec(raw.point()).lerp(vec(presentation.get(i).point()),(float)weight))<1e-10,"Presentation fade is discontinuous between endpoints");}
                    }
                }
            }
            oldHeadPulse=Math.max(oldHeadPulse,range(oldLocal));newHeadPulse=Math.max(newHeadPulse,range(newLocal));
        }
        check(oldHeadPulse>.05,"Cadence fixture did not reproduce original head deformation");check(newHeadPulse<1e-10,"Rigid head retained tick-phase body-local deformation");
        return Map.of("checks",checks,"oldHead2mPeakToPeak",oldHeadPulse,"attachedHead2mPeakToPeak",newHeadPulse,"maxVisibleWindowError",windowError,"maxDefaultPathError",defaultError,"passed",true,"scope","Production path/history/bridge mathematics, including linear entity interpolation; no in-game or shader acceptance.");
    }
    public static void main(String[] args){System.out.println(verify());}
    private HuahongHeadBridgeQa(){}
}
