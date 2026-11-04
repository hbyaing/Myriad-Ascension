package cn.blockforge.generated.generatedmod.client.qa.myriad;
import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.client.realmflight.MeteorBloomRenderer;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.BlendMode;
import org.lwjgl.opengl.*;
import org.joml.Matrix4f;
import java.util.*;
/** Real production submission under intentionally dirty external GL state; separate from visual acceptance. */
final class SwordTrailStateQa {
 static int red(Minecraft mc){var main=mc.getMainRenderTarget();main.bindRead();var pixel=org.lwjgl.BufferUtils.createByteBuffer(4);GL11.glReadPixels(main.viewWidth/2,main.viewHeight/2,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixel);return pixel.get(0)&255;}
 static Map<String,Object> verify(Minecraft mc,boolean requireStable)throws Exception{
  var shader=SwordFlightTrailRenderer.emissionShader();BlendMode blend=null;
  for(var f:net.minecraft.client.renderer.ShaderInstance.class.getDeclaredFields())if(f.getType()==BlendMode.class){f.setAccessible(true);blend=(BlendMode)f.get(shader);break;}
  if(blend==null)throw new AssertionError("No actual shader blend state");
  var overlaps=new ArrayList<Object>();var results=new ArrayList<Object>();int[] reference=null;int maximum=0;double clearDepth=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
  try(var saved=new MeteorBloomRenderer.State()){
   for(String mode:List.of("reference","reverseEquation","staleBlendFactors","emissionUniform")){
    var main=mc.getMainRenderTarget();main.bindWrite(true);RenderSystem.disableScissor();RenderSystem.colorMask(true,true,true,true);
    GL11.glClearColor(0,0,0,0);GL11.glDepthMask(true);GL11.glClearDepth(1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
    TrailShaderBridge.resetFrame();if(!TrailShaderBridge.captureDepth())throw new AssertionError("No diagnostic scene depth");
    RenderSystem.enableBlend();GL11.glEnable(GL11.GL_BLEND);RenderSystem.blendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL14.glBlendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD,GL14.GL_FUNC_ADD);BlendMode.lastApplied=blend;
    shader.safeGetUniform("EmissionPass").set(mode.equals("emissionUniform")?1F:0F);
    if(mode.equals("reverseEquation"))GL20.glBlendEquationSeparate(GL14.GL_FUNC_REVERSE_SUBTRACT,GL14.GL_FUNC_REVERSE_SUBTRACT);
    if(mode.equals("staleBlendFactors"))GL14.glBlendFuncSeparate(GL11.GL_ZERO,GL11.GL_ONE,GL11.GL_ZERO,GL11.GL_ONE);
    var b=new BufferBuilder(256);b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
    for(float[] p:new float[][]{{-.5F,-.5F},{.5F,-.5F},{.5F,.5F},{-.5F,.5F}})b.vertex(p[0],p[1],0).uv(0,36.5F).color(.28F,.82F,1F,.5F).endVertex();
    if(!SwordFlightTrailRenderer.drawPost(b.end(),new Matrix4f(),0))throw new AssertionError("Real post draw failed");
    main.bindRead();var pixel=org.lwjgl.BufferUtils.createByteBuffer(4);GL11.glReadPixels(main.viewWidth/2,main.viewHeight/2,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixel);
    int[] rgb={pixel.get(0)&255,pixel.get(1)&255,pixel.get(2)&255};if(reference==null){reference=rgb;if(rgb[2]<20)throw new AssertionError("Diagnostic reference disappeared");}
    int delta=0;for(int i=0;i<3;i++)delta=Math.max(delta,Math.abs(reference[i]-rgb[i]));maximum=Math.max(maximum,delta);
    results.add(Map.of("state",mode,"rgb",rgb,"differenceFromReference",delta));
   }
   for(int band:new int[]{0,2}){
    var main=mc.getMainRenderTarget();main.bindWrite(true);GL11.glClearColor(.65F,.65F,.65F,1);GL11.glDepthMask(true);GL11.glClearDepth(1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
    TrailShaderBridge.resetFrame();if(!TrailShaderBridge.captureDepth())throw new AssertionError("No overlap scene depth");
    var levels=new ArrayList<Integer>();int prior=red(mc);levels.add(prior);
    for(int layer=0;layer<3;layer++){
     var b=new BufferBuilder(256);b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
     for(float[] p:new float[][]{{-.5F,-.5F},{.5F,-.5F},{.5F,.5F},{-.5F,.5F}})b.vertex(p[0],p[1],0).uv(0,band+.68F).color(.28F,.82F,1F,.9F).endVertex();
     if(!SwordFlightTrailRenderer.drawPost(b.end(),new Matrix4f(),0))throw new AssertionError("Overlap post draw failed");
     int value=red(mc);if(value<prior-1)throw new AssertionError("Ordinary light absorbs its own overlapping layers: band="+band+" "+levels+" -> "+value);levels.add(value);prior=value;
    }
    overlaps.add(Map.of("band",band,"redAfterEachLayer",levels));
   }
  }finally{shader.safeGetUniform("EmissionPass").set(0F);GL11.glClearDepth(clearDepth);TrailShaderBridge.resetFrame();}
  if(requireStable&&maximum>1)throw new AssertionError("Production post depends on incoming GL state: "+results);
  return Map.of("scope","Real SwordFlightTrailRenderer.drawPost and shipping post shader, actual main target; one diagnostic quad, not real flight/visual acceptance. GL readback only here, never cold route.","stableRequired",requireStable,"maxRgbDifference",maximum,"states",results,"ordinaryOverlap",overlaps);
 }
}
