package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.client.realmflight.MeteorBloomRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL33;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Paired actual full integration shaders, GPU elapsed queries, identical float targets. */
final class HuahongVolumePerformanceQa {
 private static int compile(int type,String source){int s=GL33.glCreateShader(type);GL33.glShaderSource(s,source);GL33.glCompileShader(s);if(GL33.glGetShaderi(s,GL33.GL_COMPILE_STATUS)==0)throw new AssertionError(GL33.glGetShaderInfoLog(s));return s;}
 private static int program(String source){int v=compile(GL33.GL_VERTEX_SHADER,"#version 150\nout vec2 screenUv;void main(){vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);screenUv=p;gl_Position=vec4(p*2.-1.,0,1);}"),f=compile(GL33.GL_FRAGMENT_SHADER,source),p=GL33.glCreateProgram();GL33.glAttachShader(p,v);GL33.glAttachShader(p,f);GL33.glLinkProgram(p);GL33.glDeleteShader(v);GL33.glDeleteShader(f);if(GL33.glGetProgrami(p,GL33.GL_LINK_STATUS)==0)throw new AssertionError(GL33.glGetProgramInfoLog(p));return p;}
 private static String sha(String s)throws Exception{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}
 static Map<String,Object> verify(Minecraft mc)throws Exception{
  String source;try(var in=mc.getResourceManager().getResource(new ResourceLocation("wuxingxiantu","shaders/core/huahong_volume.fsh")).orElseThrow().open()){source=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
  String baseline;try(var in=HuahongVolumePerformanceQa.class.getResourceAsStream("/qa/huahong_volume_v13_before_cache.fsh")){baseline=new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8);}
  String directSource;try(var in=mc.getResourceManager().getResource(new ResourceLocation("wuxingxiantu","shaders/core/huahong_volume_direct.fsh")).orElseThrow().open()){directSource=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
  if(!sha(directSource).equals(sha(baseline)))throw new AssertionError("Front-view direct fallback drifted from baseline");
  HuahongVolumeNoiseCache.ensure();int cache=HuahongVolumeNoiseCache.texture();if(cache==0)throw new AssertionError(HuahongVolumeNoiseCache.status());
  String reconstruction;
  try(var in=mc.getResourceManager().getResource(new ResourceLocation("wuxingxiantu","shaders/core/huahong_volume_reconstruct.fsh")).orElseThrow().open()){
   reconstruction=new String(in.readAllBytes(),StandardCharsets.UTF_8);
  }
  int old=0,now=0,direct=0,compose=0,lowFbo=0,lowColor=0,vao=0,fbo=0,color=0,depth=0,query=0,vaoBefore=GL33.glGetInteger(GL33.GL_VERTEX_ARRAY_BINDING),programBefore=GL33.glGetInteger(GL33.GL_CURRENT_PROGRAM);
  boolean srgb=GL33.glIsEnabled(GL33.GL_FRAMEBUFFER_SRGB),dither=GL33.glIsEnabled(GL33.GL_DITHER);
  var cases=new ArrayList<Map<String,Object>>();double oldTotal=0,newTotal=0,maxError=0;
  try(var saved=new MeteorBloomRenderer.State()){
   old=program(baseline);now=program(source);direct=program(directSource);vao=GL33.glGenVertexArrays();GL33.glBindVertexArray(vao);
   RenderSystem.disableBlend();RenderSystem.disableDepthTest();RenderSystem.disableCull();RenderSystem.disableScissor();RenderSystem.colorMask(true,true,true,true);GL33.glDisable(GL33.GL_STENCIL_TEST);GL33.glDisable(GL33.GL_FRAMEBUFFER_SRGB);GL33.glDisable(GL33.GL_DITHER);
   color=GL33.glGenTextures();RenderSystem.bindTexture(color);GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_RGBA32F,1280,720,0,GL33.GL_RGBA,GL33.GL_FLOAT,0L);
   fbo=GL33.glGenFramebuffers();GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER,fbo);GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER,GL33.GL_COLOR_ATTACHMENT0,GL33.GL_TEXTURE_2D,color,0);if(GL33.glCheckFramebufferStatus(GL33.GL_FRAMEBUFFER)!=GL33.GL_FRAMEBUFFER_COMPLETE)throw new AssertionError("Perf FBO incomplete");
   depth=GL33.glGenTextures();RenderSystem.bindTexture(depth);GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_R32F,1,1,0,GL33.GL_RED,GL33.GL_FLOAT,new float[]{1});GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MIN_FILTER,GL33.GL_NEAREST);GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MAG_FILTER,GL33.GL_NEAREST);
   RenderSystem.activeTexture(GL33.GL_TEXTURE1);RenderSystem.bindTexture(cache);RenderSystem.activeTexture(GL33.GL_TEXTURE0);query=GL33.glGenQueries();
   String[] names={"side","oblique","axial","near","near640"};Vector3f[] eyes={new Vector3f(40,5,-25),new Vector3f(25,12,15),new Vector3f(.8F,2,18),new Vector3f(4,1,-3),new Vector3f(4,1,-3)};
   for(int c=0;c<eyes.length;c++){
    int optimizedProgram=eyes[c].z<=2?now:direct;
    int w=c==4?640:320,h=c==4?360:180;RenderSystem.viewport(0,0,w,h);
    set(old,w,h,eyes[c],false);draw(old);float[] a=new float[w*h*4];GL33.glReadPixels(0,0,w,h,GL33.GL_RGBA,GL33.GL_FLOAT,a);
    set(optimizedProgram,w,h,eyes[c],true);draw(optimizedProgram);float[] b=new float[a.length];GL33.glReadPixels(0,0,w,h,GL33.GL_RGBA,GL33.GL_FLOAT,b);
    double error=0,mean=0,energy=0;for(int i=0;i<a.length;i++){if(!Float.isFinite(a[i])||!Float.isFinite(b[i]))throw new AssertionError("Nonfinite cached volume");error=Math.max(error,Math.abs(a[i]-b[i]));mean+=Math.abs(a[i]-b[i]);energy+=Math.abs(a[i]);}mean/=a.length;
    if(energy<1||error>.005||mean>.00005)throw new AssertionError("Cache quality mismatch "+names[c]+": "+error+" / "+mean+" / "+energy);
    for(int i=0;i<2;i++){set(old,w,h,eyes[c],false);draw(old);set(optimizedProgram,w,h,eyes[c],true);draw(optimizedProgram);}GL33.glFinish();
    long[] before=new long[7],after=new long[7];
    for(int i=0;i<7;i++)for(int pass=0;pass<2;pass++){boolean optimized=(i%2==0?pass==1:pass==0);int p=optimized?optimizedProgram:old;set(p,w,h,eyes[c],optimized);GL33.glClearColor(0,0,0,0);GL33.glClear(GL33.GL_COLOR_BUFFER_BIT);GL33.glBeginQuery(GL33.GL_TIME_ELAPSED,query);GL33.glDrawArrays(GL33.GL_TRIANGLES,0,3);GL33.glEndQuery(GL33.GL_TIME_ELAPSED);long ns=GL33.glGetQueryObjectui64(query,GL33.GL_QUERY_RESULT);if(optimized)after[i]=ns;else before[i]=ns;}
    Arrays.sort(before);Arrays.sort(after);oldTotal+=before[3];newTotal+=after[3];maxError=Math.max(maxError,error);
    cases.add(Map.of("case",names[c],"accelerated",eyes[c].z<=2,"width",w,"height",h,"baselineMedianGpuMs",before[3]/1e6,"cachedMedianGpuMs",after[3]/1e6,"speedup",before[3]/(double)after[3],"maxChannelError",error,"meanChannelError",mean));
   }
   if(oldTotal/newTotal<1.05)throw new AssertionError("Cache/culling GPU cost: "+oldTotal/newTotal+" / "+cases);
   compose=program(reconstruction);
   lowColor=GL33.glGenTextures();RenderSystem.bindTexture(lowColor);
   GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_RGBA16F,640,360,0,GL33.GL_RGBA,GL33.GL_FLOAT,0L);
   GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MIN_FILTER,GL33.GL_NEAREST);
   GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MAG_FILTER,GL33.GL_NEAREST);
   lowFbo=GL33.glGenFramebuffers();GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER,lowFbo);
   GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER,GL33.GL_COLOR_ATTACHMENT0,GL33.GL_TEXTURE_2D,lowColor,0);
   if(GL33.glCheckFramebufferStatus(GL33.GL_FRAMEBUFFER)!=GL33.GL_FRAMEBUFFER_COMPLETE)throw new AssertionError("Reconstruction FBO incomplete");
   var reconstructed=new ArrayList<Map<String,Object>>();
   for(int index:new int[]{2,3}){
    Vector3f eye=eyes[index];int pass=index==2?direct:now;
    reconstructed.add(compareReconstruction(names[index],pass,compose,fbo,lowFbo,lowColor,depth,cache,query,eye,false));
   }
   bind(0,depth);
   GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_R32F,2,1,0,GL33.GL_RED,GL33.GL_FLOAT,new float[]{.4F,1});
   reconstructed.add(compareReconstruction("near-depth-edge",now,compose,fbo,lowFbo,lowColor,depth,cache,query,eyes[3],true));
   return Map.of("passed",true,"gpu",GL33.glGetString(GL33.GL_RENDERER),"baselineSha256",sha(baseline),"shippingSha256",sha(source),"cacheBytes",4194304,"maxChannelError",maxError,"aggregateSpeedup",oldTotal/newTotal,"cases",cases,"reconstructed1280x720",reconstructed,"scope","Existing full-resolution production integration compared with 640x360 integration plus the actual reconstruction shader at 1280x720. Fixed camera, depth and material; elapsed GPU draw queries, excludes world rendering.");
  }finally{
   if(query!=0)GL33.glDeleteQueries(query);if(fbo!=0)GL33.glDeleteFramebuffers(fbo);if(color!=0)GL33.glDeleteTextures(color);if(depth!=0)GL33.glDeleteTextures(depth);if(vao!=0)GL33.glDeleteVertexArrays(vao);GL33.glBindVertexArray(vaoBefore);if(old!=0)GL33.glDeleteProgram(old);if(now!=0)GL33.glDeleteProgram(now);if(direct!=0)GL33.glDeleteProgram(direct);
   if(compose!=0)GL33.glDeleteProgram(compose);if(lowFbo!=0)GL33.glDeleteFramebuffers(lowFbo);if(lowColor!=0)GL33.glDeleteTextures(lowColor);
   GL33.glUseProgram(programBefore);if(srgb)GL33.glEnable(GL33.GL_FRAMEBUFFER_SRGB);else GL33.glDisable(GL33.GL_FRAMEBUFFER_SRGB);if(dither)GL33.glEnable(GL33.GL_DITHER);else GL33.glDisable(GL33.GL_DITHER);
  }
 }
 private static Map<String,Object> compareReconstruction(String name,int pass,int compose,int fbo,int lowFbo,int lowColor,int depth,int cache,int query,Vector3f eye,boolean occluded){
  int w=1280,h=720;
  GL33.glUseProgram(compose);
  GL33.glUniformMatrix4fv(GL33.glGetUniformLocation(compose,"InverseWorldToClip"),false,inverse(w,h,eye));
  GL33.glUniform2f(GL33.glGetUniformLocation(compose,"OutputSize"),w,h);
  GL33.glUniform1i(GL33.glGetUniformLocation(compose,"Sampler0"),0);
  GL33.glUniform1i(GL33.glGetUniformLocation(compose,"Sampler1"),1);
  long[] nativeTimes=new long[5],reconstructedTimes=new long[5];
  float[][] image={new float[w*h*4],new float[w*h*4]};
  for(int repeat=-2;repeat<5;repeat++)for(int i=0;i<2;i++){
   boolean reduced=((repeat+i)&1)!=0;
   int iw=reduced?w/2:w,ih=reduced?h/2:h;
   GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER,reduced?lowFbo:fbo);
   RenderSystem.viewport(0,0,iw,ih);
   bind(0,depth);bind(1,cache);
   set(pass,iw,ih,eye,true);
   GL33.glBeginQuery(GL33.GL_TIME_ELAPSED,query);
   draw(pass);
   if(reduced){
    GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER,fbo);
    RenderSystem.viewport(0,0,w,h);
    bind(0,lowColor);bind(1,depth);
    draw(compose);
   }
   GL33.glEndQuery(GL33.GL_TIME_ELAPSED);
   long ns=GL33.glGetQueryObjectui64(query,GL33.GL_QUERY_RESULT);
   if(repeat>=0)(reduced?reconstructedTimes:nativeTimes)[repeat]=ns;
   if(repeat==-1)GL33.glReadPixels(0,0,w,h,GL33.GL_RGBA,GL33.GL_FLOAT,image[reduced?1:0]);
  }
  Arrays.sort(nativeTimes);Arrays.sort(reconstructedTimes);
  double old=0,now=0,error=0,foregroundLeak=0;
  for(int i=0;i<image[0].length;i++){
   if(!Float.isFinite(image[1][i]))throw new AssertionError("Nonfinite reconstructed radiance");
   old+=image[0][i];now+=image[1][i];error+=Math.abs(image[1][i]-image[0][i]);
   if(occluded&&i/4%w<w/2)foregroundLeak=Math.max(foregroundLeak,image[1][i]);
  }
  double speedup=nativeTimes[2]/(double)reconstructedTimes[2],ratio=now/old;
  if(!(speedup>1.3&&ratio>.93&&ratio<1.07))throw new AssertionError(name+" reconstruction: "+speedup+"x, energy "+ratio);
  if(foregroundLeak!=0)throw new AssertionError("Volume reconstruction leaked through foreground: "+foregroundLeak);
  return Map.of("view",name,"nativeMs",nativeTimes[2]/1e6,"reconstructedMs",reconstructedTimes[2]/1e6,
          "speedup",speedup,"energyRatio",ratio,"meanAbsoluteError",error/image[0].length,"foregroundLeak",foregroundLeak);
 }
 private static void bind(int unit,int texture){
  RenderSystem.activeTexture(GL33.GL_TEXTURE0+unit);RenderSystem.bindTexture(texture);
 }
 private static float[] inverse(int w,int h,Vector3f eye){
  return new Matrix4f().perspective((float)Math.toRadians(70),w/(float)h,.05F,500)
          .lookAt(eye,new Vector3f(0,0,-20),new Vector3f(0,1,0)).invert().get(new float[16]);
 }
 private static void draw(int p){GL33.glClearColor(0,0,0,0);GL33.glClear(GL33.GL_COLOR_BUFFER_BIT);GL33.glUseProgram(p);GL33.glDrawArrays(GL33.GL_TRIANGLES,0,3);}
 private static void set(int p,int w,int h,Vector3f eye,boolean cache){
  GL33.glUseProgram(p);GL33.glUniformMatrix4fv(GL33.glGetUniformLocation(p,"InverseWorldToClip"),false,inverse(w,h,eye));
  GL33.glUniformMatrix3fv(GL33.glGetUniformLocation(p,"WorldToMaterial"),false,new Matrix3f().get(new float[9]));GL33.glUniform2f(GL33.glGetUniformLocation(p,"OutputSize"),w,h);
  float length=(float)(8.2*2.2*HuahongHeadBridge.BODY_SCALE),radius=.025F+HuahongVolumeRenderer.DEFAULT_WIDTH*(.10F+HuahongVolumeRenderer.DEFAULT_SHELL*.012F+HuahongVolumeRenderer.DEFAULT_SPREAD*3.9F);
  GL33.glUniform3f(GL33.glGetUniformLocation(p,"BoundsMin"),-radius,-radius,-length-radius);GL33.glUniform3f(GL33.glGetUniformLocation(p,"BoundsMax"),radius,radius,radius);float[] path=new float[128],ages=new float[32];for(int i=0;i<32;i++){float d=i*length/31;path[i*4+2]=-d;path[i*4+3]=-d;ages[i]=d/300;}
  GL33.glUniform4fv(GL33.glGetUniformLocation(p,"SegmentBounds[0]"),HuahongVolumeRenderer.segmentBounds(path,ages,32,length));boolean accelerate=eye.z<=2;GL33.glUniform1i(GL33.glGetUniformLocation(p,"FieldCullEnabled"),cache&&accelerate?1:0);GL33.glUniform4fv(GL33.glGetUniformLocation(p,"PathPoints[0]"),path);GL33.glUniform1fv(GL33.glGetUniformLocation(p,"PathAges[0]"),ages);GL33.glUniform1i(GL33.glGetUniformLocation(p,"PathCount"),32);GL33.glUniform1f(GL33.glGetUniformLocation(p,"PathLength"),length);
  GL33.glUniform4f(GL33.glGetUniformLocation(p,"ShapeProfile"),HuahongVolumeRenderer.DEFAULT_WIDTH,HuahongVolumeRenderer.DEFAULT_LIFE,HuahongVolumeRenderer.DEFAULT_SPREAD,HuahongVolumeRenderer.DEFAULT_SHELL);GL33.glUniform1f(GL33.glGetUniformLocation(p,"EffectTime"),7.125F);GL33.glUniform1f(GL33.glGetUniformLocation(p,"Strength"),1);GL33.glUniform1f(GL33.glGetUniformLocation(p,"Exposure"),1);GL33.glUniform1i(GL33.glGetUniformLocation(p,"Sampler0"),0);GL33.glUniform1i(GL33.glGetUniformLocation(p,"GradientCache"),1);GL33.glUniform1i(GL33.glGetUniformLocation(p,"GradientCacheEnabled"),cache&&accelerate?1:0);
 }
}
