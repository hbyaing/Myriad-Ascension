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
    static Map<String,Object> verify(Minecraft mc)throws Exception{return verify(mc,false);}
    static Map<String,Object> verifyLoops(Minecraft mc)throws Exception{return verify(mc,true);}
    private static Map<String,Object> verify(Minecraft mc,boolean loopsOnly)throws Exception{
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
            for(int band:new int[]{6,8,10,12,14,16,18,20,22,24,26,28,30,34,36,38}){
                float[] reference=render(program,band,47.25F,7.125F);
                float low=Float.POSITIVE_INFINITY,high=0;for(float v:reference){low=Math.min(low,v);high=Math.max(high,v);}
                if(high<.1||high-low<.01)throw new AssertionError("Material fixture produced no varying signal: "+band);
                error=Math.max(error,difference(reference,render(program,band,47.25F,135.125F)));comparisons++;
                // Smoke UVs encode stable birth identity, not a rebased path arc.
                if(band!=14&&band!=20){error=Math.max(error,difference(reference,render(program,band,303.25F,7.125F)));comparisons++;}
            }
            if(error>.025)throw new AssertionError("Material periodic seam error: "+error);
            if(loopsOnly){
                float[] old=render(program,38,47.25F,7.125F),first=old;double motion=0,maxStep=0;
                for(int i=1;i<=240;i++){float[] next=render(program,38,47.25F,7.125F+i/120F);maxStep=Math.max(maxStep,difference(old,next));motion=Math.max(motion,difference(first,next));old=next;}
                double wrap=difference(render(program,38,47.25F,127.9999F),render(program,38,47.25F,.0001F));
                if(motion<.001||maxStep>.04||wrap>.001)throw new AssertionError("Fan ridge loop frozen/discontinuous: "+motion+" / "+maxStep+" / "+wrap);
                return Map.of("passed",true,"check","shipping-material-periodicity-and-new-fan-ridge-animation","comparisons",comparisons,"maxChannelPeriodError",error,"fanRidge120FpsMaxStep",maxStep,"fanRidgeTwoSecondMotion",motion,"fanRidge128SecondWrapDifference",wrap,"scope","Native/post shared shipping functions, all existing bands plus38; tests periodicity and new fan ridge motion only. Legacy smoke-field probes are separate.");
            }
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
            Map<String,Object> boundedHalo=boundedHalo(program);
            Map<String,Object> boundedHaloPost=boundedHaloPost(mc);
            int gl=GL33.glGetError();if(gl!=0)throw new AssertionError("Material fixture GL error "+gl);
            var result=new LinkedHashMap<String,Object>();result.put("check","shipping-glsl-periodicity");result.put("comparisons",comparisons);result.put("maxChannelError",error);result.put("passed",true);
            result.put("noseBand",32);result.put("noseProbes",noseProbes);result.put("noseCenterAlpha",noseCenter[3]);result.put("noseMaxBoundaryAlpha",noseEdge);result.put("noseMaxTimeError",noseTimeError);result.put("noseProjection",noseProjection);
            result.put("subpixelSpineCoverage",spineCoverage);
            result.put("temporalTransport",temporalTransport);result.put("smokeField",smokeField);
            result.put("boundedOrdinaryHalo",boundedHalo);result.put("boundedOrdinaryHaloPost",boundedHaloPost);
            result.put("smokeAtlasSha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(smokeBytes)));result.put("smokeAtlasDimensions",List.of(smokeWidth,smokeHeight));result.put("materialBands",List.of(6,8,10,12,14,16,18,20,22,24,26,28,30,32,34,36,38));return result;
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
        return probe(program,32,u,v,time);
    }
    private static float[] probe(int program,int band,float u,float v,float time){
        GL33.glUniform1i(GL33.glGetUniformLocation(program,"ProbeMode"),1);GL33.glUniform2f(GL33.glGetUniformLocation(program,"ProbeUV"),u,v);
        GL33.glUniform1f(GL33.glGetUniformLocation(program,"Band"),band);GL33.glUniform1f(GL33.glGetUniformLocation(program,"EffectTime"),time);
        GL33.glDrawArrays(GL33.GL_TRIANGLES,0,3);float[] pixels=new float[64*32*4];GL33.glReadPixels(0,0,64,32,GL33.GL_RGBA,GL33.GL_FLOAT,pixels);return pixels;
    }
    private static Map<String,Object> boundedHalo(int program){
        double boundary=0,period=0,wrap=0,step=0,low=Double.POSITIVE_INFINITY,high=0;
        for(float arc:new float[]{-71.5F,0,1,47.25F,255.75F}){
            for(float across:new float[]{0,1}){
                float[] p=probe(program,36,arc,across,7.125F);boundary=Math.max(boundary,Math.abs(p[3]));
            }
            float[] center=probe(program,36,arc,.5F,7.125F);
            period=Math.max(period,difference(center,probe(program,36,arc,.5F,135.125F)));
            period=Math.max(period,difference(center,probe(program,36,arc+256,.5F,7.125F)));
            wrap=Math.max(wrap,difference(probe(program,36,arc,.5F,127.999F),probe(program,36,arc,.5F,.001F)));
        }
        for(int i=0;i<64;i++){
            float t=i*2F;float[] a=probe(program,36,47.25F,.5F,t),b=probe(program,36,47.25F,.5F,t+1F/120);
            low=Math.min(low,a[3]);high=Math.max(high,a[3]);step=Math.max(step,Math.abs(a[3]-b[3]));
        }
        float[] image=render(program,36,47.25F,7.125F);double energy=0,maxAlpha=0;
        for(int i=0;i<image.length;i+=4){double alpha=image[i+3];energy+=(image[i]*.2126+image[i+1]*.7152+image[i+2]*.0722)*alpha;maxAlpha=Math.max(maxAlpha,alpha);}
        double mean=energy/(64*32),variation=(high-low)/high;
        // Finite transverse support and low-amplitude, low-frequency time flow.
        // Longitudinal root support belongs to the real geometry/vertex alpha.
        if(boundary>1e-7||period>1e-5||wrap>.0002||low<.799||high>.80001||variation>1e-6||step>1e-6||mean<.01||maxAlpha>.80001)
            throw new AssertionError("Bounded halo energy/boundary/time failure "+boundary+" / "+period+" / "+wrap+" / "+low+".."+high+" / "+step+" / "+mean);
        return Map.of("passed",true,"band",36,"meanPremultipliedRadiance",mean,"maxAlpha",maxAlpha,"maxTransverseBoundaryAlpha",boundary,
                "maxArc256Time128Error",period,"time128WrapDifference",wrap,"centerTimeVariation",variation,"max120FpsAlphaStep",step,
                "scope","Actual shipping material in finite strip; transverse boundaries only. Actual pommel/root geometry and final image checked separately.");
    }
    private static Map<String,Object> boundedHaloPost(Minecraft mc)throws Exception{
        String source;try(var in=mc.getResourceManager().getResource(new ResourceLocation("wuxingxiantu","shaders/core/sword_flight_post.fsh")).orElseThrow().open()){source=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
        int beforeProgram=GL33.glGetInteger(GL33.GL_CURRENT_PROGRAM),beforeTexture=GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
        float[] clearColor=new float[4];GL33.glGetFloatv(GL33.GL_COLOR_CLEAR_VALUE,clearColor);
        int v=0,f=0,p=0,depth=0;
        try{
            v=shader(GL33.GL_VERTEX_SHADER,"#version 150\nout vec2 trailCoord;out vec4 vertexColor;out vec3 viewPosition;void main(){vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);trailCoord=vec2(47.25+p.x*15.25,36.02+p.y*.96);vertexColor=vec4(.35,.65,.95,.8);viewPosition=vec3(0,0,-4);gl_Position=vec4(p*2.0-1.0,0,1);}");
            f=shader(GL33.GL_FRAGMENT_SHADER,source);p=GL33.glCreateProgram();GL33.glAttachShader(p,v);GL33.glAttachShader(p,f);GL33.glLinkProgram(p);
            if(GL33.glGetProgrami(p,GL33.GL_LINK_STATUS)==0)throw new AssertionError(GL33.glGetProgramInfoLog(p));
            depth=GL33.glGenTextures();GL33.glBindTexture(GL33.GL_TEXTURE_2D,depth);GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_R32F,1,1,0,GL33.GL_RED,GL33.GL_FLOAT,new float[]{1});
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MIN_FILTER,GL33.GL_NEAREST);GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MAG_FILTER,GL33.GL_NEAREST);
            GL33.glUseProgram(p);GL33.glUniform1i(GL33.glGetUniformLocation(p,"Sampler0"),0);GL33.glUniform1i(GL33.glGetUniformLocation(p,"SmokeAtlas"),0);GL33.glUniform2f(GL33.glGetUniformLocation(p,"OutputSize"),64,32);GL33.glUniform1f(GL33.glGetUniformLocation(p,"EffectTime"),7.125F);
            GL33.glClearColor(0,0,0,0);GL33.glClear(GL33.GL_COLOR_BUFFER_BIT);
            GL33.glUniform1f(GL33.glGetUniformLocation(p,"EmissionPass"),0);GL33.glDrawArrays(GL33.GL_TRIANGLES,0,3);float[] direct=new float[64*32*4];GL33.glReadPixels(0,0,64,32,GL33.GL_RGBA,GL33.GL_FLOAT,direct);
            GL33.glClear(GL33.GL_COLOR_BUFFER_BIT);
            GL33.glUniform1f(GL33.glGetUniformLocation(p,"EmissionPass"),1);GL33.glDrawArrays(GL33.GL_TRIANGLES,0,3);float[] emission=new float[64*32*4];GL33.glReadPixels(0,0,64,32,GL33.GL_RGBA,GL33.GL_FLOAT,emission);
            double light=0,maxDirectAlpha=0,maxEmission=0;
            for(int i=0;i<direct.length;i+=4){for(int c=0;c<3;c++){light+=direct[i+c];maxEmission=Math.max(maxEmission,Math.abs(emission[i+c]));}maxDirectAlpha=Math.max(maxDirectAlpha,Math.abs(direct[i+3]));}
            if(!Double.isFinite(light)||light<.01||maxDirectAlpha>1e-7||maxEmission>1e-7)throw new AssertionError("Halo actual post pass is empty/nonadditive/enters bloom: "+light+" / "+maxDirectAlpha+" / "+maxEmission);
            return Map.of("passed",true,"band",36,"directRgbEnergy",light,"maxDirectAlpha",maxDirectAlpha,"maxIndependentBloomRgb",maxEmission,"scope","Actual complete shipping post shader: additive direct output and zero RGB in EmissionPass.");
        }finally{
            GL33.glClearColor(clearColor[0],clearColor[1],clearColor[2],clearColor[3]);GL33.glUseProgram(beforeProgram);GL33.glBindTexture(GL33.GL_TEXTURE_2D,beforeTexture);if(depth!=0)GL33.glDeleteTextures(depth);if(p!=0)GL33.glDeleteProgram(p);if(v!=0)GL33.glDeleteShader(v);if(f!=0)GL33.glDeleteShader(f);
        }
    }
    private static Map<String,Object> temporalTransport(int program){
        final int frames=240;final double fps=120,speed=300;
        var world=temporalSignal(program,frames,fps,speed);var local=temporalSignal(program,frames,fps,0);
        // Ordinary flight was explicitly changed to constant emission. Testing
        // it for mandatory animation would reject the user's accepted behavior.
        // Rainbow/cloud animation has separate nonzero-energy GPU probes.
        if(world.meanSignal()<.01||local.meanSignal()<.01||world.stepRms()>1e-6||local.stepRms()>1e-6||local.totalPower()>1e-10)
            throw new AssertionError("Ordinary constant material flickers or is empty: "+world+" / "+local);
        return Map.of("scope","Shipping ordinary band0 remains lit and invariant under time advancement and a300m/s arc sweep; rainbow animation is checked separately.",
                "frames",frames,"fps",fps,"worldSpeed",speed,"worldArc",world,"headRelative",local,"passed",true);
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
        // The renderer consumes time-born parcels, not the obsolete smoke()
        // fixed-slot API. Follow the SAME birth identities between frames.
        int checks=0,birthChecks=0;double maxSide=0,maxRadius=0,maxAlpha=0,maxTravelError=0;
        for(double epoch:new double[]{0,10200336,999997440})for(int fps:new int[]{60,120,240})for(int k=0;k<160;k++){
            double tick=epoch+100+k*.71,dt=1.0/fps;
            var a=cn.blockforge.generated.generatedmod.client.HuahongFlowField.parcels(tick);
            var b=cn.blockforge.generated.generatedmod.client.HuahongFlowField.parcels(tick+20*dt);
            var previous=new HashMap<Long,cn.blockforge.generated.generatedmod.client.HuahongFlowField.Parcel>();for(var q:a)previous.put(q.id(),q);
            for(var q:b){var old=previous.get(q.id());
                if(old==null){if(q.alpha()>.05)throw new AssertionError("New smoke birth pops at full opacity");birthChecks++;continue;}
                maxSide=Math.max(maxSide,Math.hypot(q.side()-old.side(),q.up()-old.up()));
                maxRadius=Math.max(maxRadius,Math.abs(q.radius()-old.radius()));maxAlpha=Math.max(maxAlpha,Math.abs(q.alpha()-old.alpha()));
                maxTravelError=Math.max(maxTravelError,Math.abs(q.behind()-old.behind()-7.2*dt));checks++;
            }
        }
        if(checks<10000||birthChecks<10||maxSide>.03||maxRadius>.02||maxAlpha>.20||maxTravelError>1e-6)
            throw new AssertionError("Actual smoke parcel discontinuity: "+checks+" / "+maxSide+" / "+maxRadius+" / "+maxAlpha+" / "+maxTravelError);
        return Map.of("passed",true,"productionParcelChecks",checks,"newBirthChecks",birthChecks,"maxTransverseStep",maxSide,"maxRadiusStep",maxRadius,"maxAlphaStep",maxAlpha,"maxAdvectionError",maxTravelError,"fps",List.of(60,120,240),"scope","Actual rendered parcels; same birth id, rearward travel, transverse roll, radius growth, soft newborn alpha and old world clocks. Not obsolete fixed-slot smoke().");
    }
    private static double difference(float[] a,float[] b){double max=0;for(int i=0;i<a.length;i++){if(!Float.isFinite(a[i])||!Float.isFinite(b[i]))throw new AssertionError("Nonfinite material output");max=Math.max(max,Math.abs(a[i]-b[i]));}return max;}
    private HuahongMaterialQa(){}
}
