package cn.blockforge.generated.generatedmod.client.qa.myriad;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL33;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;
import com.mojang.blaze3d.platform.NativeImage;

/** Runs the actual shipping GLSL material at both sides of its arc/time periods. */
final class HuahongMaterialQa {
    private static String material(Minecraft mc,String file)throws Exception{
        try(var stream=mc.getResourceManager().getResource(new ResourceLocation("wuxingxiantu","shaders/core/"+file+".fsh")).orElseThrow().open()){
            String source=new String(stream.readAllBytes(),StandardCharsets.UTF_8);
            int start=source.indexOf("float filteredSpine");
            return source.substring(start>=0?start:source.indexOf("float hash21"),source.indexOf("void main"));
        }
    }
    private static int shader(int type,String source){
        int shader=GL33.glCreateShader(type);GL33.glShaderSource(shader,source);GL33.glCompileShader(shader);
        if(GL33.glGetShaderi(shader,GL33.GL_COMPILE_STATUS)==0)throw new AssertionError(GL33.glGetShaderInfoLog(shader));
        return shader;
    }
    static Map<String,Object> verify(Minecraft mc)throws Exception{
        String source=material(mc,"sword_flight");
        if(!source.equals(material(mc,"sword_flight_post")))throw new AssertionError("Native/post material differs");
        byte[] smokeBytes;try(var stream=mc.getResourceManager().getResource(new ResourceLocation("wuxingxiantu","textures/effect/huahong/smoke-atlas.png")).orElseThrow().open()){smokeBytes=stream.readAllBytes();}
        int programBefore=GL33.glGetInteger(GL33.GL_CURRENT_PROGRAM),vaoBefore=GL33.glGetInteger(GL33.GL_VERTEX_ARRAY_BINDING);
        int drawBefore=GL33.glGetInteger(GL33.GL_DRAW_FRAMEBUFFER_BINDING),readBefore=GL33.glGetInteger(GL33.GL_READ_FRAMEBUFFER_BINDING);
        int activeTextureBefore=GL33.glGetInteger(GL33.GL_ACTIVE_TEXTURE);GL33.glActiveTexture(GL33.GL_TEXTURE0);
        int textureBefore=GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);int[] viewport=new int[4];GL33.glGetIntegerv(GL33.GL_VIEWPORT,viewport);
        int[] capabilities={GL33.GL_BLEND,GL33.GL_DEPTH_TEST,GL33.GL_CULL_FACE,GL33.GL_SCISSOR_TEST,GL33.GL_FRAMEBUFFER_SRGB};
        boolean[] enabled=new boolean[capabilities.length];for(int i=0;i<capabilities.length;i++){enabled[i]=GL33.glIsEnabled(capabilities[i]);GL33.glDisable(capabilities[i]);}
        int vertex=0,fragment=0,program=0,vao=0,texture=0,fbo=0,smokeTexture=0,smokeWidth=0,smokeHeight=0;
        try{
            vertex=shader(GL33.GL_VERTEX_SHADER,"#version 150\nout vec2 uv;void main(){vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);uv=p;gl_Position=vec4(p*2.0-1.0,0.0,1.0);}");
            fragment=shader(GL33.GL_FRAGMENT_SHADER,"#version 150\nuniform sampler2D SmokeAtlas;uniform float EffectTime,ArcOffset,Band;uniform vec2 ProbeUV;uniform int ProbeMode;in vec2 uv;out vec4 fragColor;vec2 trailCoord;vec4 vertexColor;\n"+source+"\nvoid main(){if(ProbeMode==2){float spine=(uv.y-.5)*ProbeUV.x+ProbeUV.y;fragColor=vec4(filteredSpine(spine,.12));return;}trailCoord=ProbeMode==1?vec2(ProbeUV.x,Band+ProbeUV.y):vec2(ArcOffset+uv.x*15.25,Band+.02+uv.y*.96);vertexColor=vec4(.35,.65,.95,.8);fragColor=swordMaterial();}");
            program=GL33.glCreateProgram();GL33.glAttachShader(program,vertex);GL33.glAttachShader(program,fragment);GL33.glLinkProgram(program);
            if(GL33.glGetProgrami(program,GL33.GL_LINK_STATUS)==0)throw new AssertionError(GL33.glGetProgramInfoLog(program));
            texture=GL33.glGenTextures();GL33.glBindTexture(GL33.GL_TEXTURE_2D,texture);
            GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_RGBA32F,64,32,0,GL33.GL_RGBA,GL33.GL_FLOAT,0L);
            fbo=GL33.glGenFramebuffers();GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER,fbo);GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER,GL33.GL_COLOR_ATTACHMENT0,GL33.GL_TEXTURE_2D,texture,0);
            if(GL33.glCheckFramebufferStatus(GL33.GL_FRAMEBUFFER)!=GL33.GL_FRAMEBUFFER_COMPLETE)throw new AssertionError("Material fixture FBO incomplete");
            vao=GL33.glGenVertexArrays();GL33.glBindVertexArray(vao);GL33.glUseProgram(program);GL33.glViewport(0,0,64,32);
            smokeTexture=GL33.glGenTextures();GL33.glBindTexture(GL33.GL_TEXTURE_2D,smokeTexture);
            try(var image=NativeImage.read(new java.io.ByteArrayInputStream(smokeBytes))){
                smokeWidth=image.getWidth();smokeHeight=image.getHeight();
                ByteBuffer pixels=MemoryUtil.memAlloc(image.getWidth()*image.getHeight()*4);
                try{
                    for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){int rgba=image.getPixelRGBA(x,y);pixels.put((byte)rgba).put((byte)(rgba>>>8)).put((byte)(rgba>>>16)).put((byte)(rgba>>>24));}pixels.flip();
                    GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_RGBA8,image.getWidth(),image.getHeight(),0,GL33.GL_RGBA,GL33.GL_UNSIGNED_BYTE,pixels);
                }finally{MemoryUtil.memFree(pixels);}
            }
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MIN_FILTER,GL33.GL_LINEAR);GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MAG_FILTER,GL33.GL_LINEAR);
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_WRAP_S,GL33.GL_CLAMP_TO_EDGE);GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_WRAP_T,GL33.GL_CLAMP_TO_EDGE);
            GL33.glUniform1i(GL33.glGetUniformLocation(program,"SmokeAtlas"),0);
            double error=0;int comparisons=0;
            for(int band:new int[]{6,8,10,12,14,16,18,20,22,24,26,28,30,34}){
                float[] reference=render(program,band,47.25F,7.125F);
                float low=Float.POSITIVE_INFINITY,high=0;for(float v:reference){low=Math.min(low,v);high=Math.max(high,v);}
                if(high<.1||high-low<.01)throw new AssertionError("Material fixture produced no varying signal: "+band);
                error=Math.max(error,difference(reference,render(program,band,47.25F,135.125F)));comparisons++;
                // Smoke UVs encode stable birth identity, not a rebased path arc.
                if(band!=14&&band!=20){error=Math.max(error,difference(reference,render(program,band,303.25F,7.125F)));comparisons++;}
            }
            if(error>.025)throw new AssertionError("Material periodic seam error: "+error);
            float[] noseCenter=probe(program,.5F,.5F,7.125F);difference(noseCenter,noseCenter);
            if(noseCenter[3]<.1F)throw new AssertionError("Band32 nucleus has no center coverage");
            double noseEdge=0,noseTimeError=difference(noseCenter,probe(program,.5F,.5F,135.125F));int noseProbes=2;
            for(float[] uv:new float[][]{{0,.5F},{1,.5F},{.5F,0},{.5F,1},{0,0},{0,1},{1,0},{1,1}}){
                float[] edge=probe(program,uv[0],uv[1],7.125F);difference(edge,edge);noseEdge=Math.max(noseEdge,Math.abs(edge[3]));noseProbes++;
            }
            if(noseEdge>1e-6||noseTimeError>1e-6)throw new AssertionError("Band32 local UV envelope boundary/time error "+noseEdge+" / "+noseTimeError);
            Map<String,Object> noseProjection=HuahongMotionQa.verifyNoseProjection(mc);
            var spineCoverage=new ArrayList<Map<String,Object>>();
            for(float scale:new float[]{8,16,32}){
                double low=Double.POSITIVE_INFINITY,high=0;
                for(int phase=0;phase<32;phase++){
                    GL33.glUniform1i(GL33.glGetUniformLocation(program,"ProbeMode"),2);
                    GL33.glUniform2f(GL33.glGetUniformLocation(program,"ProbeUV"),scale,phase/32F*scale/32);
                    GL33.glDrawArrays(GL33.GL_TRIANGLES,0,3);float[] pixels=new float[64*32*4];GL33.glReadPixels(0,0,64,32,GL33.GL_RGBA,GL33.GL_FLOAT,pixels);
                    double energy=0;for(int i=3;i<pixels.length;i+=4)energy+=pixels[i];
                    low=Math.min(low,energy);high=Math.max(high,energy);
                }
                double variation=(high-low)/high;
                if(!Double.isFinite(variation)||low<=0||variation>.08)throw new AssertionError("Subpixel spine blinks: scale="+scale+" variation="+variation);
                spineCoverage.add(Map.of("acrossScale",scale,"pixelPhases",32,"minEnergy",low,"maxEnergy",high,"relativeVariation",variation));
            }
            Map<String,Object> temporalTransport=temporalTransport(program);
            Map<String,Object> smokeField=smokeField();
            int gl=GL33.glGetError();if(gl!=0)throw new AssertionError("Material fixture GL error "+gl);
            var result=new LinkedHashMap<String,Object>();result.put("check","shipping-glsl-periodicity");result.put("comparisons",comparisons);result.put("maxChannelError",error);result.put("passed",true);
            result.put("noseBand",32);result.put("noseProbes",noseProbes);result.put("noseCenterAlpha",noseCenter[3]);result.put("noseMaxBoundaryAlpha",noseEdge);result.put("noseMaxTimeError",noseTimeError);result.put("noseProjection",noseProjection);
            result.put("subpixelSpineCoverage",spineCoverage);
            result.put("temporalTransport",temporalTransport);result.put("smokeField",smokeField);
            result.put("smokeAtlasSha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(smokeBytes)));result.put("smokeAtlasDimensions",List.of(smokeWidth,smokeHeight));result.put("materialBands",List.of(6,8,10,12,14,16,18,20,22,24,26,28,30,32,34));return result;
        }finally{
            GL33.glBindFramebuffer(GL33.GL_DRAW_FRAMEBUFFER,drawBefore);GL33.glBindFramebuffer(GL33.GL_READ_FRAMEBUFFER,readBefore);
            GL33.glActiveTexture(GL33.GL_TEXTURE0);GL33.glBindTexture(GL33.GL_TEXTURE_2D,textureBefore);GL33.glActiveTexture(activeTextureBefore);GL33.glBindVertexArray(vaoBefore);GL33.glUseProgram(programBefore);GL33.glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            for(int i=0;i<capabilities.length;i++)if(enabled[i])GL33.glEnable(capabilities[i]);else GL33.glDisable(capabilities[i]);
            if(fbo!=0)GL33.glDeleteFramebuffers(fbo);if(texture!=0)GL33.glDeleteTextures(texture);if(smokeTexture!=0)GL33.glDeleteTextures(smokeTexture);if(vao!=0)GL33.glDeleteVertexArrays(vao);
            if(program!=0)GL33.glDeleteProgram(program);if(vertex!=0)GL33.glDeleteShader(vertex);if(fragment!=0)GL33.glDeleteShader(fragment);
        }
    }
    private static float[] render(int program,int band,float arc,float time){
        GL33.glUniform1i(GL33.glGetUniformLocation(program,"ProbeMode"),0);
        GL33.glUniform1f(GL33.glGetUniformLocation(program,"Band"),band);GL33.glUniform1f(GL33.glGetUniformLocation(program,"ArcOffset"),arc);GL33.glUniform1f(GL33.glGetUniformLocation(program,"EffectTime"),time);
        GL33.glDrawArrays(GL33.GL_TRIANGLES,0,3);float[] pixels=new float[64*32*4];GL33.glReadPixels(0,0,64,32,GL33.GL_RGBA,GL33.GL_FLOAT,pixels);return pixels;
    }
    private static float[] probe(int program,float u,float v,float time){
        GL33.glUniform1i(GL33.glGetUniformLocation(program,"ProbeMode"),1);GL33.glUniform2f(GL33.glGetUniformLocation(program,"ProbeUV"),u,v);
        GL33.glUniform1f(GL33.glGetUniformLocation(program,"Band"),32);GL33.glUniform1f(GL33.glGetUniformLocation(program,"EffectTime"),time);
        GL33.glDrawArrays(GL33.GL_TRIANGLES,0,3);float[] pixels=new float[64*32*4];GL33.glReadPixels(0,0,64,32,GL33.GL_RGBA,GL33.GL_FLOAT,pixels);return pixels;
    }
    private static Map<String,Object> temporalTransport(int program){
        final int frames=240;final double fps=120,speed=300;
        var world=temporalSignal(program,frames,fps,speed);var local=temporalSignal(program,frames,fps,0);
        // The new transport must retain visible animation, while moving most
        // energy below 8Hz and reducing frame replacement by at least 3x.
        // These are preselected flow-continuity criteria, not pixel acceptance.
        if(world.stepRms()<1e-4||local.totalPower()<1e-7||local.stepRms()<1e-5)
            throw new AssertionError("Temporal material probe is empty or frozen: "+world+" / "+local);
        if(local.stepRms()>world.stepRms()/3||local.highFraction()>.25||local.highFraction()>world.highFraction()*.35)
            throw new AssertionError("Head material still aliases at flight speed: "+world+" / "+local);
        return Map.of("scope","Same shipping GLSL, same patch, explicit time advances in both; world arc sweeps at300m/s vs fixed distance behind head. GPU material test, not whole-world acceptance.",
                "frames",frames,"fps",fps,"worldSpeed",speed,"highFrequencyCutHz",8,
                "worldArc",world,"headRelative",local,"frameDeltaReduction",world.stepRms()/local.stepRms(),"passed",true);
    }
    private record TemporalStats(double stepRms,double meanSignal,double totalPower,double highFraction,List<Map<String,Double>> peakFrequencies){}
    private static TemporalStats temporalSignal(int program,int frames,double fps,double speed){
        int[] xs={2,10,18,26,34,42,50,58},ys={6,10,14,16,18,22,26};int count=xs.length*ys.length;
        double[][] trace=new double[count][frames];double steps=0,signal=0;
        for(int frame=0;frame<frames;frame++){
            double seconds=frame/fps;float[] pixels=render(program,0,(float)(-16+speed*seconds),(float)(7.125+seconds));int probe=0;
            for(int y:ys)for(int x:xs){int i=(y*64+x)*4;double value=(pixels[i]*.2126+pixels[i+1]*.7152+pixels[i+2]*.0722)*pixels[i+3];
                if(!Double.isFinite(value))throw new AssertionError("Nonfinite temporal shader signal");
                trace[probe][frame]=value;signal+=value;if(frame>0){double d=value-trace[probe][frame-1];steps+=d*d;}probe++;
            }
        }
        double[] powers=new double[frames/2+1];
        for(var point:trace){double mean=0;for(double v:point)mean+=v;mean/=frames;
            for(int frequency=1;frequency<powers.length;frequency++){
                double real=0,imaginary=0;
                for(int i=0;i<frames;i++){
                    double sample=(point[i]-mean)*(.5-.5*Math.cos(2*Math.PI*i/(frames-1)));
                    double angle=2*Math.PI*frequency*i/frames;real+=sample*Math.cos(angle);imaginary-=sample*Math.sin(angle);
                }
                powers[frequency]+=(real*real+imaginary*imaginary)/(frames*(double)frames*count);
            }
        }
        double total=0,high=0;var peaks=new ArrayList<Map<String,Double>>();
        for(int k=1;k<powers.length;k++){double hz=k*fps/frames;total+=powers[k];if(hz>8)high+=powers[k];peaks.add(Map.of("hz",hz,"power",powers[k]));}
        peaks.sort(Comparator.comparingDouble((Map<String,Double> p)->p.get("power")).reversed());
        return new TemporalStats(Math.sqrt(steps/(count*(double)(frames-1))),signal/(count*(double)frames),total,total>0?high/total:0,List.copyOf(peaks.subList(0,8)));
    }
    private static Map<String,Object> smokeField(){
        int checks=0;double seam=0,maxPositionStep=0,maxRadiusStep=0,maxCrimsonStep=0;
        for(double behind=0;behind<=24;behind+=.25){
            for(int tick=0;tick<2560;tick+=16){
                var a=cn.blockforge.generated.generatedmod.client.HuahongFlowField.smoke(behind,tick+.25);
                for(long epoch:new long[]{2560,16_775_680,999_997_440}){
                    var b=cn.blockforge.generated.generatedmod.client.HuahongFlowField.smoke(behind,epoch+tick+.25);
                    double d=Math.max(Math.max(Math.abs(a.side()-b.side()),Math.abs(a.up()-b.up())),Math.max(Math.abs(a.radius()-b.radius()),Math.abs(a.crimson()-b.crimson())));
                    seam=Math.max(seam,d);checks++;
                }
                var b=cn.blockforge.generated.generatedmod.client.HuahongFlowField.smoke(behind,tick+.25+20.0/120);
                maxPositionStep=Math.max(maxPositionStep,Math.hypot(a.side()-b.side(),a.up()-b.up()));
                maxRadiusStep=Math.max(maxRadiusStep,Math.abs(a.radius()-b.radius()));maxCrimsonStep=Math.max(maxCrimsonStep,Math.abs(a.crimson()-b.crimson())*.075);
            }
            var a=cn.blockforge.generated.generatedmod.client.HuahongFlowField.smoke(behind,2560-.00001);
            var b=cn.blockforge.generated.generatedmod.client.HuahongFlowField.smoke(behind,2560+.00001);
            seam=Math.max(seam,Math.max(Math.abs(a.side()-b.side()),Math.max(Math.abs(a.up()-b.up()),Math.max(Math.abs(a.radius()-b.radius()),Math.abs(a.crimson()-b.crimson())))));
        }
        if(seam>2e-6||maxPositionStep>.002||maxRadiusStep>.001||maxCrimsonStep>.001)
            throw new AssertionError("Smoke field discontinuity: "+seam+" / "+maxPositionStep+" / "+maxRadiusStep+" / "+maxCrimsonStep);
        return Map.of("productionFieldChecks",checks,"maxPeriodOrBoundaryDifference",seam,"max120FpsPositionStep",maxPositionStep,"max120FpsRadiusStep",maxRadiusStep,
                "max120FpsCrimsonAlphaStep",maxCrimsonStep,"periodSeconds",128,"angularTimeHarmonics",List.of(22,15,14,40),"crimsonAdvectionWorldUnitsPerSecond",Math.PI*2*40/128/1.4,"passed",true);
    }
    private static double difference(float[] a,float[] b){double max=0;for(int i=0;i<a.length;i++){if(!Float.isFinite(a[i])||!Float.isFinite(b[i]))throw new AssertionError("Nonfinite material output");max=Math.max(max,Math.abs(a[i]-b[i]));}return max;}
    private HuahongMaterialQa(){}
}
