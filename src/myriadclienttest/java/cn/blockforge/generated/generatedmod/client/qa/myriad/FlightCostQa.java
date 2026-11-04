package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.yujian.HuahongSkin;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.world.phys.Vec3;
import java.lang.management.ManagementFactory;
import java.util.*;

/** Fixed geometry probe. CPU allocation/timing, not GPU or whole-game FPS. */
public final class FlightCostQa {
 private static volatile long sink;
 private static final class Collector implements VertexConsumer {
  long hash=0xcbf29ce484222325L;int vertices;
  void n(long x){hash=(hash^x)*0x100000001b3L;}
  public VertexConsumer vertex(double x,double y,double z){n(Double.doubleToLongBits(x));n(Double.doubleToLongBits(y));n(Double.doubleToLongBits(z));return this;}
  public VertexConsumer color(int r,int g,int b,int a){n(r);n(g);n(b);n(a);return this;}
  public VertexConsumer uv(float u,float v){n(Float.floatToIntBits(u));n(Float.floatToIntBits(v));return this;}
  public VertexConsumer overlayCoords(int a,int b){return this;}public VertexConsumer uv2(int a,int b){return this;}
  public VertexConsumer normal(float x,float y,float z){return this;}public void endVertex(){vertices++;}
  public void defaultColor(int r,int g,int b,int a){}public void unsetDefaultColor(){}
 }
 public static Map<String,Object> verify() throws Exception {
  var method=Class.forName("cn.blockforge.generated.generatedmod.client.HuahongSkinGeometry").getDeclaredMethod("draw",HuahongSkin.class,VertexConsumer.class,PoseStack.Pose.class,Vec3.class,Vec3.class,Vec3.class,Vec3.class,List.class,List.class,double.class,float.class);method.setAccessible(true);
  var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);long id=Thread.currentThread().getId();
  boolean baseline=Boolean.getBoolean("wuxingxiantu.flightPerfBaseline");var results=new LinkedHashMap<String,Object>();
  for(var skin:HuahongSkin.values()){
   var h=new SwordFlightTrailHistory(.25,116,160,720);
   for(int t=0;t<=120;t++){double a=t*.07;h.sample(new SwordFlightTrailHistory.Point(1000000+Math.sin(a)*180,11000+Math.sin(t*.03)*15,1000000+(1-Math.cos(a))*180),t,32);}
   var raw=h.nodes();var last=raw.get(raw.size()-1).point();Vec3 center=new Vec3(last.x(),last.y(),last.z()),forward=new Vec3(Math.cos(8.4),0,Math.sin(8.4)).normalize(),side=new Vec3(0,1,0).cross(forward).normalize();
   var nodes=SwordFlightTrailHistory.uniformHead(raw,.125,12);Vec3 eye=center.add(18,8,10);var pose=new PoseStack().last();
   java.util.function.Supplier<Collector> run=()->{try{var c=new Collector();
    if(skin==HuahongSkin.METEOR_PRISM){var path=new HuahongFlightPath(raw,center,forward,side,Double.POSITIVE_INFINITY,true,1);var sections=new HuahongFlightPath.Frame[nodes.size()];if(!baseline)for(int i=0;i<nodes.size();i++)sections[i]=path.behind(raw.get(raw.size()-1).arc()-nodes.get(i).arc());for(int k=0;k<5;k++)for(int index=0;index<sections.length;index++){var f=baseline?path.behind(raw.get(raw.size()-1).arc()-nodes.get(index).arc()):sections[index];c.vertex(f.p().x,f.p().y,f.p().z);c.vertex(f.side().x,f.side().y,f.side().z);c.vertex(f.up().x,f.up().y,f.up().z);} }
    else {method.invoke(null,skin,c,pose,eye,center,forward,side,nodes,raw,120.0,1F);if(skin==HuahongSkin.DENSE)for(var n:HuahongHeadBridge.presentationNodes(raw,center,forward,1)){var p=n.point();c.vertex(p.x(),p.y(),p.z());}}
    sink=c.hash;return c;
   }catch(Exception e){throw new RuntimeException(e);}};
   for(int i=0;i<120;i++)run.get();long start=bean.getThreadAllocatedBytes(id);long[] times=new long[40];Collector c=null;
   for(int i=0;i<40;i++){long t=System.nanoTime();c=run.get();times[i]=System.nanoTime()-t;}long bytes=bean.getThreadAllocatedBytes(id)-start;Arrays.sort(times);
   results.put(skin.name(),Map.of("geometryHash",Long.toUnsignedString(c.hash,16),"vertices",c.vertices,"allocatedBytesPerCall",bytes/40,"medianCpuNanos",times[20]));
  }
  return Map.of("passed",true,"styles",results,"scope","Seven actual geometry consumers plus full dense presentation path; prism shared transported frames queried by its five history layers. Fixed curved world history, warmed CPU timing and thread allocation; not GPU timings.");
 }
}
