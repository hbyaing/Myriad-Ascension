package cn.blockforge.generated.generatedmod.client.qa.myriad;
import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.client.realmflight.MeteorBloomRenderer;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import java.util.*;

/** Freeze material and straight geometry; vary only subpixel translation through one pixel. */
final class HuahongSubpixelQa {
 static Map<String,Object> verify(Minecraft mc)throws Exception{
  var field=SwordFlightTrailRenderer.class.getDeclaredField("post");field.setAccessible(true);var shipping=(ShaderInstance)field.get(null);
  var widthMethod=HuahongVisuals.class.getDeclaredMethod("coreHalfWidth",Vec3.class,Vec3.class,Vec3.class,double.class);widthMethod.setAccessible(true);
  var pixelField=HuahongVisuals.class.getDeclaredField("corePixelScale");pixelField.setAccessible(true);double oldScale=pixelField.getDouble(null);boolean oldSmoke=HuahongVisuals.absorbingThisFrame;
  var main=mc.getMainRenderTarget();var projection=new Matrix4f().perspective((float)Math.toRadians(70),(float)main.viewWidth/main.viewHeight,.05F,512);var inverse=new Matrix4f(projection).invert();
  double physical=.0016,depth=10,baselineWidth=Math.max(physical,Math.min(physical*8,depth*.85/(projection.m11()*main.viewHeight)));
  var cases=new ArrayList<Object>();double clearDepth=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
  try(var saved=new MeteorBloomRenderer.State();var baseline=new ShaderInstance(mc.getResourceManager(),new ResourceLocation("wuxingxiantu","huahong_thin_baseline"),DefaultVertexFormat.POSITION_TEX_COLOR)){
   HuahongVisuals.absorbingThisFrame=false;HuahongVisuals.coreProjection(projection,main.viewHeight);
   double candidateWidth=((Number)widthMethod.invoke(null,new Vec3(0,0,-depth),new Vec3(0,1,0),Vec3.ZERO,physical)).doubleValue();
   for(int band:new int[]{8,22}){
    var old=measure(mc,field,baseline,projection,inverse,band,physical,baselineWidth);
    var now=measure(mc,field,shipping,projection,inverse,band,physical,candidateWidth);
    if(((Number)old.get("zeroFrames")).intValue()==0)throw new AssertionError("Thin-core baseline did not disappear between pixels");
    if(((Number)now.get("zeroFrames")).intValue()!=0||((Number)now.get("relativeEnergyRange")).doubleValue()>.20)throw new AssertionError("Thin core still aliases: "+now);
    cases.add(Map.of("band",band,"baseline",old,"candidate",now));
   }
  }finally{field.set(null,shipping);pixelField.setDouble(null,oldScale);HuahongVisuals.absorbingThisFrame=oldSmoke;GL11.glClearDepth(clearDepth);TrailShaderBridge.resetFrame();}
  return Map.of("passed",true,"cases",cases,"scope","Frozen previous shipping GLSL versus current shipping GLSL, production width helper, real post draw; 32 offsets through one screen pixel, fixed time/straight line/camera. Not whole-form visual acceptance.");
 }
 static Map<String,Object> measure(Minecraft mc,java.lang.reflect.Field field,ShaderInstance shader,Matrix4f projection,Matrix4f inverse,int band,double physical,double width)throws Exception{
  field.set(null,shader);var main=mc.getMainRenderTarget();var energies=new ArrayList<Integer>();int zero=0;
  for(int step=0;step<32;step++){
   main.bindWrite(true);RenderSystem.disableScissor();RenderSystem.colorMask(true,true,true,true);GL11.glClearColor(0,0,0,0);GL11.glDepthMask(true);GL11.glClearDepth(1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
   TrailShaderBridge.resetFrame();if(!TrailShaderBridge.captureDepth())throw new AssertionError("No subpixel fixture depth");
   double y=step/32.0*20/(projection.m11()*main.viewHeight);var b=new BufferBuilder(512);b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
   b.vertex(-1,y-width,-10).uv(0,band).color(1F,1F,1F,(float)(.8*physical/width)).endVertex();b.vertex(1,y-width,-10).uv(1,band).color(1F,1F,1F,(float)(.8*physical/width)).endVertex();b.vertex(1,y+width,-10).uv(1,band+1).color(1F,1F,1F,(float)(.8*physical/width)).endVertex();b.vertex(-1,y+width,-10).uv(0,band+1).color(1F,1F,1F,(float)(.8*physical/width)).endVertex();
   if(!SwordFlightTrailRenderer.drawPost(b.end(),projection,inverse,0))throw new AssertionError("Subpixel post draw failed");
   main.bindRead();var pixels=org.lwjgl.BufferUtils.createByteBuffer(12*4);GL11.glReadPixels(main.viewWidth/2,main.viewHeight/2-5,1,12,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);int total=0;for(int p=0;p<12;p++)total+=pixels.get(p*4)&255;energies.add(total);if(total==0)zero++;
  }
  int min=Collections.min(energies),max=Collections.max(energies);double mean=energies.stream().mapToInt(Integer::intValue).average().orElseThrow();
  return Map.of("widthWorld",width,"fullWidthPixels",projection.m11()*main.viewHeight*width/10,"energy",energies,"zeroFrames",zero,"minEnergy",min,"maxEnergy",max,"relativeEnergyRange",(max-min)/Math.max(1,mean));
 }
}
