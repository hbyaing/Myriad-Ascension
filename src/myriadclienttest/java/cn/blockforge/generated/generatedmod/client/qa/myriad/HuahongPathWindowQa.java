package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.client.HuahongFlightPath;
import cn.blockforge.generated.generatedmod.client.SwordFlightTrailHistory;
import net.minecraft.world.phys.Vec3;
import java.lang.management.ManagementFactory;
import java.util.*;

/** Compare the production32m smoke transport window with the full historical path. */
public final class HuahongPathWindowQa {
    private static volatile double sink;
    public static Map<String,Object> verify(){
        int checks=0;double error=0;
        for(double speed:new double[]{.2,5,15,30})for(int shape=0;shape<4;shape++){
            var history=new SwordFlightTrailHistory(.25,116,160,720);
            for(int t=0;t<=120;t++)history.sample(point(position(t,speed,shape)),t,speed*2+3);
            for(double partial:new double[]{0,.13,.5,.999}){
                Vec3 center=position(119,speed,shape).lerp(position(120,speed,shape),partial);
                var nodes=history.renderNodes(119+partial,point(center));
                Vec3 forward=position(120,speed,shape).subtract(position(119,speed,shape)).normalize();
                Vec3 side=forward.cross(Math.abs(forward.y)>.95?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();
                var full=new HuahongFlightPath(nodes,center,forward,side);var limited=new HuahongFlightPath(nodes,center,forward,side,32);
                for(double d=0;d<=26;d+=.13){
                    var a=full.behind(d);var b=limited.behind(d);
                    double e=Math.max(Math.max(a.p().distanceTo(b.p()),a.side().distanceTo(b.side())),Math.max(a.forward().distanceTo(b.forward()),a.up().distanceTo(b.up())));
                    error=Math.max(error,e);if(e>1e-9)throw new AssertionError("Window changed visible smoke frame: "+shape+"/"+speed+"/"+d+" error "+e);checks++;
                }
                for(var n:nodes){
                    if(nodes.get(nodes.size()-1).arc()-n.arc()>24)continue;
                    Vec3 p=new Vec3(n.point().x(),n.point().y(),n.point().z());
                    var a=full.frame(p,forward);var b=limited.frame(p,forward);
                    double e=Math.max(a.side().distanceTo(b.side()),a.up().distanceTo(b.up()));
                    error=Math.max(error,e);if(e>1e-9)throw new AssertionError("Window changed smoke birth frame "+e);checks++;
                }
            }
        }
        var history=new SwordFlightTrailHistory(.25,116,160,720);
        for(int t=0;t<=120;t++)history.sample(point(new Vec3(t*15,0,0)),t,30);
        var nodes=history.nodes();Vec3 center=new Vec3(1800,0,0),forward=new Vec3(1,0,0),side=new Vec3(0,0,1);
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);
        for(int i=0;i<20;i++){sink+=new HuahongFlightPath(nodes,center,forward,side).behind(17).p().x;sink+=new HuahongFlightPath(nodes,center,forward,side,32).behind(17).p().x;}
        int count=80;long start=bean.getThreadAllocatedBytes(Thread.currentThread().getId());
        for(int i=0;i<count;i++)sink+=new HuahongFlightPath(nodes,center,forward,side).behind(17).p().x;
        long full=bean.getThreadAllocatedBytes(Thread.currentThread().getId())-start;start=bean.getThreadAllocatedBytes(Thread.currentThread().getId());
        for(int i=0;i<count;i++)sink+=new HuahongFlightPath(nodes,center,forward,side,32).behind(17).p().x;
        long limited=bean.getThreadAllocatedBytes(Thread.currentThread().getId())-start;
        if(limited>=full*.6)throw new AssertionError("Smoke path allocation did not fall "+full+" / "+limited);
        return Map.of("checks",checks,"maxVisibleFrameDifference",error,"constructorsPerProbe",count,"fullAllocatedBytes",full,"windowAllocatedBytes",limited,"allocationReduction",(double)full/limited,"passed",true,
                "scope","Production path equivalence over visible smoke32m window; ThreadMXBean constructor allocation probe, not total game frame-time acceptance.");
    }
    private static Vec3 position(double t,double speed,int shape){
        return switch(shape){
            case 0->new Vec3(t*speed,0,0);
            case 1->new Vec3(Math.sin(t*.05)*speed/.05,Math.sin(t*.02)*speed*.3,(1-Math.cos(t*.05))*speed/.05);
            case 2->{double a=t<110?t*.02:2.2+(t-110)*.2;yield new Vec3(Math.sin(a)*speed/.2,t*.01,(1-Math.cos(a))*speed/.2);}
            default->new Vec3(29_999_000+Math.sin(t*.03)*speed,Math.sin(t*.013)*speed/.013,29_999_000+t*speed*.1);
        };
    }
    private static SwordFlightTrailHistory.Point point(Vec3 p){return new SwordFlightTrailHistory.Point(p.x,p.y,p.z);}
    private HuahongPathWindowQa(){}
}
