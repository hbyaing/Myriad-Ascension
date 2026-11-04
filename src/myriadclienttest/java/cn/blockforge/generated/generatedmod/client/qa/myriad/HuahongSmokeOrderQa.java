package cn.blockforge.generated.generatedmod.client.qa.myriad;
import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.client.realmflight.MeteorBloomRenderer;
import cn.blockforge.generated.generatedmod.yujian.HuahongSkin;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import java.nio.*;
import java.util.*;

/** Identical shipping smoke vertices/material/depth; vary only quad submission order. */
final class HuahongSmokeOrderQa {
 static Map<String,Object> verify(Minecraft mc)throws Exception{
  var baseline=measure(mc,false);var candidate=measure(mc,true);
  if(((Number)baseline.get("maxRgbDifference")).intValue()<5)throw new AssertionError("Order fixture did not reproduce smoke discontinuity");
  if(((Number)candidate.get("maxRgbDifference")).intValue()>1)throw new AssertionError("Optical smoke still depends on slice order: "+candidate);
  double minimum=Double.POSITIVE_INFINITY,maximum=0,step=0;
  for(int k=0;k<8;k++){
   var before=HuahongFlowField.smokeHead(k,10200336);
   for(int i=1;i<=960;i++){
    var after=HuahongFlowField.smokeHead(k,10200336+i/6.0);minimum=Math.min(minimum,after.radius());maximum=Math.max(maximum,after.radius());
    step=Math.max(step,Math.max(Math.abs(after.radius()-before.radius()),Math.hypot(after.side()-before.side(),after.up()-before.up()))*HuahongHeadBridge.BODY_SCALE);before=after;
   }
  }
  if(maximum-minimum<.2||step>.02)throw new AssertionError("Smoke lobe animation frozen or stepping: "+(maximum-minimum)+" / "+step);
  return Map.of("baseline",baseline,"opticalMixture",candidate,"headLobeRadiusRange",List.of(minimum,maximum),"headLobeMaxStepAt120fps",step,"passed",true);
 }
 static Map<String,Object> measure(Minecraft mc,boolean optical)throws Exception{
  var type=Class.forName("cn.blockforge.generated.generatedmod.client.HuahongSkinGeometry");
  var draw=type.getDeclaredMethod("draw",HuahongSkin.class,VertexConsumer.class,PoseStack.Pose.class,Vec3.class,Vec3.class,Vec3.class,Vec3.class,List.class,List.class,double.class,float.class);draw.setAccessible(true);
  var h=new SwordFlightTrailHistory(.25,116,160,720);
  for(int tick=0;tick<=100;tick++)h.sample(new SwordFlightTrailHistory.Point(0,0,(tick-100)*15),tick,1000);
  var canonical=h.renderNodes(100,new SwordFlightTrailHistory.Point(0,0,0));var nodes=SwordFlightTrailHistory.uniformHead(canonical,.125,12);
  Vec3 eye=new Vec3(7,2,-12);var projection=new Matrix4f().perspective((float)Math.toRadians(70),(float)mc.getMainRenderTarget().viewWidth/mc.getMainRenderTarget().viewHeight,.05F,512);
  var view=new Matrix4f().lookAt(0,0,0,(float)-eye.x,(float)-eye.y,(float)-eye.z,0,1,0);var clip=new Matrix4f(projection).mul(view);var inverse=new Matrix4f(projection).invert();
  var b=new BufferBuilder(65536);b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
  draw.invoke(null,HuahongSkin.SMOKE,b,new PoseStack().last(),eye,Vec3.ZERO,new Vec3(0,0,1),new Vec3(1,0,0),nodes,canonical,100d,1f);
  var mesh=b.end();byte[] source;
  try{var bytes=mesh.vertexBuffer();source=new byte[bytes.remaining()];bytes.get(source);}finally{mesh.release();}
  var images=new ArrayList<byte[]>();double clearDepth=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
  try(var state=new MeteorBloomRenderer.State()){
   for(int variant=0;variant<(optical?19:3);variant++){
    boolean reverse=variant==1;
    var main=mc.getMainRenderTarget();main.bindWrite(true);RenderSystem.disableScissor();RenderSystem.colorMask(true,true,true,true);GL11.glClearColor(.6F,.6F,.6F,1);GL11.glDepthMask(true);GL11.glClearDepth(variant==2?0:1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
    TrailShaderBridge.resetFrame();if(!TrailShaderBridge.captureDepth())throw new AssertionError("Missing smoke order depth");
    var out=new BufferBuilder(source.length+256);out.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);var bytes=ByteBuffer.wrap(source).order(ByteOrder.nativeOrder());int stride=24,quads=source.length/(stride*4);
    for(int k=0;k<quads;k++){int q=reverse?quads-1-k:k;for(int j=0;j<4;j++){int p=(q*4+j)*stride;out.vertex(bytes.getFloat(p),bytes.getFloat(p+4),bytes.getFloat(p+8)).uv(bytes.getFloat(p+12),bytes.getFloat(p+16)).color(bytes.get(p+20)&255,bytes.get(p+21)&255,bytes.get(p+22)&255,bytes.get(p+23)&255).endVertex();}}
    float time=variant>=3?5+(variant-3)/30F:5;
    if(!(optical?HuahongSmokeCompositor.draw(out.end(),clip,inverse,time):SwordFlightTrailRenderer.drawPost(out.end(),clip,inverse,time)))throw new AssertionError("Smoke order production draw failed");
    main.bindRead();var pixels=org.lwjgl.BufferUtils.createByteBuffer(main.viewWidth*main.viewHeight*4);GL11.glReadPixels(0,0,main.viewWidth,main.viewHeight,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);byte[] image=new byte[pixels.remaining()];pixels.get(image);images.add(image);
   }
  }finally{GL11.glClearDepth(clearDepth);TrailShaderBridge.resetFrame();}
  int maximum=0,changed=0;double sum=0;
  for(int p=0;p<images.get(0).length;p++)if(p%4!=3){int difference=Math.abs((images.get(0)[p]&255)-(images.get(1)[p]&255));maximum=Math.max(maximum,difference);sum+=difference;if(difference>3)changed++;}
  int leak=0;for(int p=0;p<images.get(2).length;p++)if(p%4!=3)leak=Math.max(leak,Math.abs((images.get(2)[p]&255)-153));
  if(leak>1)throw new AssertionError("Smoke leaked through opaque depth: "+leak);
  var result=new LinkedHashMap<String,Object>();result.put("shippingVertices",source.length/24);result.put("maxRgbDifference",maximum);result.put("opaqueDepthMaxLeak",leak);result.put("channelsDifferingOver3",changed);result.put("meanChannelDifference",sum/(images.get(0).length*.75));
  if(optical){
   int motion=0,active=0,maxTimeDifference=0;double step=0;
   for(int p=0;p<images.get(3).length;p++)if(p%4!=3){
    int d=Math.abs((images.get(3)[p]&255)-(images.get(18)[p]&255));if(d>3)motion++;maxTimeDifference=Math.max(maxTimeDifference,d);
    if((images.get(3)[p]&255)!=153){active++;for(int f=4;f<=18;f++)step+=Math.pow((images.get(f)[p]&255)-(images.get(f-1)[p]&255),2);}
   }
   if(motion<100||maxTimeDifference<8)throw new AssertionError("Production optical smoke is visually frozen: "+motion+" / "+maxTimeDifference);
   result.put("temporalChangedChannelsOver3",motion);result.put("temporalMaxChannelDifference",maxTimeDifference);result.put("temporalActiveChannelStepRms",Math.sqrt(step/Math.max(1,active*15L)));result.put("temporalSeconds",.5);result.put("temporalScope","Fixed shipping mesh/camera/depth, vary only shader EffectTime over16 frames at30fps. Confirms visible animation survives optical compositing, not movement of the whole trail.");
  }
  result.put("scope","Same shipping smoke mesh/camera/depth; reverse order, test opaque depth, and optical-only fixed-mesh time animation. Separate real flight acceptance.");return result;
 }
}
