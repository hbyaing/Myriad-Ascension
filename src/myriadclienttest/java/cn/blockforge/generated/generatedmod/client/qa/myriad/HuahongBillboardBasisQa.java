package cn.blockforge.generated.generatedmod.client.qa.myriad;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import cn.blockforge.generated.generatedmod.yujian.HuahongSkin;
import java.nio.*;
import java.util.*;
/** Exercise the actual private billboard emitter across the previous axis-switch boundary. */
final class HuahongBillboardBasisQa {
 static Map<String,Object> verify(Minecraft mc)throws Exception{
  var type=Class.forName("cn.blockforge.generated.generatedmod.client.HuahongSkinGeometry");
  var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
  var row=Class.forName(type.getName()+"$Row");var rc=row.getDeclaredConstructors()[0];rc.setAccessible(true);
  var emit=type.getDeclaredMethod("billboard",row,int.class);emit.setAccessible(true);
  var b=new BufferBuilder(1024);b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
  Vec3 eye=mc.gameRenderer.getMainCamera().getPosition();
  PoseStack pose=new PoseStack();
  pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(67));
  Object geometry=ctor.newInstance(HuahongSkin.SMOKE,b,pose.last(),eye,Vec3.ZERO,new Vec3(0,0,1),new Vec3(1,0,0),List.of(),List.of(),0d,1f);
  for(double y:new double[]{3,3.1})emit.invoke(geometry,rc.newInstance(eye.add(-1,-y,0),0d,.5d,1f,0xffffff,null),30);
  var mesh=b.end();double dot;
  try{
   var bytes=mesh.vertexBuffer().order(ByteOrder.nativeOrder());int stride=DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize();
   Vec3 a=edge(bytes,0,stride),c=edge(bytes,4*stride,stride);
   var modelView=com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix();
   Vec3 camera=new Vec3(modelView.m00(),modelView.m10(),modelView.m20()).normalize();
   dot=a.dot(c);
   if(dot<.99999||a.dot(camera)<.99999||c.dot(camera)<.99999)throw new AssertionError("Billboard basis snaps at old vertical threshold: "+dot);
  }finally{mesh.release();}
  return Map.of("productionQuads",2,"acrossOldThresholdBasisDot",dot,"scope","Actual production billboard vertices across |view.y|=.95, same camera; no shader or visual acceptance claim.");
 }
 private static Vec3 edge(ByteBuffer b,int offset,int stride){return new Vec3(b.getFloat(offset+stride)-b.getFloat(offset),b.getFloat(offset+stride+4)-b.getFloat(offset+4),b.getFloat(offset+stride+8)-b.getFloat(offset+8)).normalize();}
}
