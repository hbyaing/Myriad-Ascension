package cn.blockforge.generated.generatedmod.client.qa.myriad;
import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.yujian.HuahongSkin;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Observes actual submitted fan vertices; no duplicate geometry formula. */
final class HuahongFanVolumeQa {
 private static final class Collector implements VertexConsumer {
  double x,y,z;float v;int alpha;int vertices;long hash=17;double sumX,sumY,xx,xy,yy,weight;
  public VertexConsumer vertex(double x,double y,double z){if(!Double.isFinite(x+y+z))throw new AssertionError("Nonfinite flame/fan vertex");this.x=x;this.y=y;this.z=z;hash=hash*31+Double.doubleToLongBits(x);hash=hash*31+Double.doubleToLongBits(y);hash=hash*31+Double.doubleToLongBits(z);return this;}
  public VertexConsumer color(int r,int g,int b,int a){alpha=a;return this;}public VertexConsumer uv(float u,float v){this.v=v;return this;}public VertexConsumer overlayCoords(int a,int b){return this;}public VertexConsumer uv2(int a,int b){return this;}public VertexConsumer normal(float x,float y,float z){return this;}public void defaultColor(int r,int g,int b,int a){}public void unsetDefaultColor(){}
  public void endVertex(){vertices++;if(v>=28&&v<=29&&alpha>4){double w=alpha/255.;sumX+=x*w;sumY+=y*w;xx+=x*x*w;xy+=x*y*w;yy+=y*y*w;weight+=w;}}
  double ratio(){double a=xx/weight-Math.pow(sumX/weight,2),b=yy/weight-Math.pow(sumY/weight,2),c=xy/weight-sumX*sumY/(weight*weight);double d=Math.sqrt((a-b)*(a-b)+4*c*c);return(a+b-d)/(a+b+d);}
 }
 static Map<String,Object> verify()throws Exception{
  var draw=Class.forName("cn.blockforge.generated.generatedmod.client.HuahongSkinGeometry").getDeclaredMethod("draw",HuahongSkin.class,VertexConsumer.class,PoseStack.Pose.class,Vec3.class,Vec3.class,Vec3.class,Vec3.class,List.class,List.class,double.class,float.class);draw.setAccessible(true);
  var raw=new ArrayList<SwordFlightTrailHistory.Node>();for(int i=0;i<=300;i++){double d=(300-i)*.125;raw.add(new SwordFlightTrailHistory.Node(new SwordFlightTrailHistory.Point(0,0,-d),100-d/15,1000-d));}
  var nodes=SwordFlightTrailHistory.uniformHead(raw,.125,12);var pose=new PoseStack().last();Vec3 eye=new Vec3(22,8,10);double minRatio=1;int vertices=0;
  for(double time:new double[]{100,100.25,101}){
   var a=new Collector();draw.invoke(null,HuahongSkin.FAN,a,pose,eye,Vec3.ZERO,new Vec3(0,0,1),new Vec3(1,0,0),nodes,raw,time,1F);
   var b=new Collector();draw.invoke(null,HuahongSkin.FAN,b,pose,eye,Vec3.ZERO,new Vec3(1,0,0),new Vec3(0,0,-1),nodes,raw,time,1F);
   if(a.hash!=b.hash)throw new AssertionError("Current carrier banking moved already emitted fan");
   if(a.weight<=1||a.ratio()<.12)throw new AssertionError("Fan remains nearly planar across flight axis: "+a.ratio());minRatio=Math.min(minRatio,a.ratio());vertices=Math.max(vertices,a.vertices);
  }
  var flame=new Collector();draw.invoke(null,HuahongSkin.FLAME,flame,pose,eye,Vec3.ZERO,new Vec3(0,0,1),new Vec3(1,0,0),nodes,raw,100.25,1F);if(flame.vertices<1000)throw new AssertionError("Flame consumer empty");
  return Map.of("passed",true,"minimumWeightedTransverseEigenRatio",minRatio,"fanSubmittedVertices",vertices,"flameFiniteVertices",flame.vertices,"birthGeometryUnaffectedByCurrentBank",true,"scope","Actual production submitted coordinates, three ages, straight immutable world history; nonplanar transverse support plus stable births and finite flame geometry. Appearance verified separately from multiple cameras.");
 }
 private HuahongFanVolumeQa(){}
}
