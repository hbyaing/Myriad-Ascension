package cn.blockforge.generated.generatedmod.client.qa.myriad;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL33;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Actual old/new ordinary GLSL; local flicker, minima and motion are separate checks. */
final class OrdinaryTrailFlickerQa {
    private static final int WIDTH=96,HEIGHT=16,FRAMES=512;
    private static final double FPS=64,HIGH_HZ=2;
    private static final int[] XS={4,16,28,40,52,64,76,88},CORE_YS={7,8},EDGE_YS={2,5,10,13};
    private static final double[] WINDOW=new double[FRAMES];
    private static final double[][] COS=new double[FRAMES/2+1][FRAMES],SIN=new double[FRAMES/2+1][FRAMES];
    static{for(int i=0;i<FRAMES;i++)WINDOW[i]=.5-.5*Math.cos(2*Math.PI*i/(FRAMES-1));for(int k=1;k<COS.length;k++)for(int i=0;i<FRAMES;i++){double a=2*Math.PI*k*i/FRAMES;COS[k][i]=Math.cos(a);SIN[k][i]=Math.sin(a);}}
    private static String resource(Minecraft mc,String name)throws Exception{
        try(var in=mc.getResourceManager().getResource(new ResourceLocation("wuxingxiantu","shaders/core/"+name+".fsh")).orElseThrow().open()){
            return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String body(String source){return source.substring(source.indexOf("float filteredSpine"),source.indexOf("void main"));}
    private static int shader(int type,String source){int id=GL33.glCreateShader(type);GL33.glShaderSource(id,source);GL33.glCompileShader(id);if(GL33.glGetShaderi(id,GL33.GL_COMPILE_STATUS)==0)throw new AssertionError(GL33.glGetShaderInfoLog(id));return id;}
    private static int program(String source){
        int v=shader(GL33.GL_VERTEX_SHADER,"#version 150\nout vec2 uv;void main(){vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);uv=p;gl_Position=vec4(p*2.-1.,0,1);}"),
            f=shader(GL33.GL_FRAGMENT_SHADER,"#version 150\nuniform sampler2D SmokeAtlas;uniform float EffectTime,Band,ArcOffset,AcrossSpan,VertexAlpha;in vec2 uv;out vec4 fragColor;vec2 trailCoord;vec4 vertexColor;\n"+body(source)+"\nvoid main(){trailCoord=vec2(ArcOffset+uv.x*14.25,Band+.5+(uv.y-.5)*AcrossSpan*.5);vertexColor=vec4(.28,.82,1.,VertexAlpha);vec4 m=swordMaterial();fragColor=vec4(m.rgb*m.a,m.a);}"),
            p=GL33.glCreateProgram();GL33.glAttachShader(p,v);GL33.glAttachShader(p,f);GL33.glLinkProgram(p);GL33.glDeleteShader(v);GL33.glDeleteShader(f);if(GL33.glGetProgrami(p,GL33.GL_LINK_STATUS)==0)throw new AssertionError(GL33.glGetProgramInfoLog(p));return p;
    }
    static Map<String,Object> verify(Minecraft mc)throws Exception{
        String current=resource(mc,"sword_flight"),post=resource(mc,"sword_flight_post"),baseline;
        try(var in=OrdinaryTrailFlickerQa.class.getResourceAsStream("/qa/sword_flight_94001de.fsh")){
            if(in==null)throw new AssertionError("Missing frozen ordinary shader");baseline=new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
        if(!body(current).equals(body(post)))throw new AssertionError("Actual native/post material differs");
        String marker="    bool spiral=trailCoord.y>1.5;";
        String unchanged=body(baseline).substring(0,body(baseline).indexOf(marker));
        if(!unchanged.equals(body(current).substring(0,body(current).indexOf(marker))))throw new AssertionError("Nonordinary material/halo branch changed");
        int previousProgram=GL33.glGetInteger(GL33.GL_CURRENT_PROGRAM),previousVao=GL33.glGetInteger(GL33.GL_VERTEX_ARRAY_BINDING),
            previousDraw=GL33.glGetInteger(GL33.GL_DRAW_FRAMEBUFFER_BINDING),previousRead=GL33.glGetInteger(GL33.GL_READ_FRAMEBUFFER_BINDING),
            previousActive=GL33.glGetInteger(GL33.GL_ACTIVE_TEXTURE);GL33.glActiveTexture(GL33.GL_TEXTURE0);int previousTexture=GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
        int[] viewport=new int[4];GL33.glGetIntegerv(GL33.GL_VIEWPORT,viewport);int[] caps={GL33.GL_BLEND,GL33.GL_DEPTH_TEST,GL33.GL_CULL_FACE,GL33.GL_SCISSOR_TEST,GL33.GL_FRAMEBUFFER_SRGB};boolean[] enabled=new boolean[caps.length];
        for(int i=0;i<caps.length;i++){enabled[i]=GL33.glIsEnabled(caps[i]);GL33.glDisable(caps[i]);}
        int old=0,now=0,color=0,dummy=0,fbo=0,vao=0;
        try{
            old=program(baseline);now=program(current);color=GL33.glGenTextures();GL33.glBindTexture(GL33.GL_TEXTURE_2D,color);GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_RGBA32F,WIDTH,HEIGHT,0,GL33.GL_RGBA,GL33.GL_FLOAT,0L);
            fbo=GL33.glGenFramebuffers();GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER,fbo);GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER,GL33.GL_COLOR_ATTACHMENT0,GL33.GL_TEXTURE_2D,color,0);
            if(GL33.glCheckFramebufferStatus(GL33.GL_FRAMEBUFFER)!=GL33.GL_FRAMEBUFFER_COMPLETE)throw new AssertionError("Ordinary flicker FBO incomplete");
            dummy=GL33.glGenTextures();GL33.glBindTexture(GL33.GL_TEXTURE_2D,dummy);GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_RGBA32F,1,1,0,GL33.GL_RGBA,GL33.GL_FLOAT,new float[]{1,1,1,1});
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MIN_FILTER,GL33.GL_LINEAR);GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MAG_FILTER,GL33.GL_LINEAR);
            vao=GL33.glGenVertexArrays();GL33.glBindVertexArray(vao);GL33.glViewport(0,0,WIDTH,HEIGHT);
            var cases=new ArrayList<Map<String,Object>>();
            for(int band:new int[]{0,2})for(float seed:new float[]{0,11.47F,23.31F})for(float alpha:new float[]{.15F,.8F}){
                Stats before=temporal(old,band,seed,alpha,true),after=temporal(now,band,seed,alpha,true);
                // Preselected local stability limits: keep a bright core, no
                // half-peak off frames, reduce contrast and absolute fast power.
                // Mean intensity is only an anti-dimming guard, never the pass criterion.
                if(after.worstMinMedian<.84||after.halfPeakDropouts!=0||after.normalizedContrastRms>before.normalizedContrastRms*.70
                    ||after.stepRms>before.stepRms*.65||after.highPower>before.highPower*.25||after.mean<before.mean*.65)
                    throw new AssertionError("Ordinary local flicker still excessive band="+band+" seed="+seed+" alpha="+alpha+": "+before+" / "+after);
                cases.add(Map.of("band",band,"headSeed",seed,"vertexAlpha",alpha,"baseline",before,"candidate",after));
            }
            var edges=new ArrayList<Map<String,Object>>();for(int band:new int[]{0,2}){
                Stats before=temporal(old,band,11.47F,.8F,false),after=temporal(now,band,11.47F,.8F,false);
                if(after.totalPower<1e-8||after.mean<.002)throw new AssertionError("Ordinary peripheral flow removed/frozen: "+after);
                edges.add(Map.of("band",band,"baseline",before,"candidate",after));
            }
            double period=0,wrap=0;for(int band:new int[]{0,2})for(float arc:new float[]{-71.5F,0,23.31F,255.75F}){
                float[] a=render(now,band,arc,1.7F,.8F,7.125F);
                period=Math.max(period,difference(a,render(now,band,arc+256,1.7F,.8F,7.125F)));
                period=Math.max(period,difference(a,render(now,band,arc,1.7F,.8F,135.125F)));
                wrap=Math.max(wrap,difference(render(now,band,arc,1.7F,.8F,127.999F),render(now,band,arc,1.7F,.8F,.001F)));
            }
            if(period>.025||wrap>.005)throw new AssertionError("Ordinary phase period/wrap failure "+period+" / "+wrap);
            int error=GL33.glGetError();if(error!=0)throw new AssertionError("Ordinary GPU GL error "+error);
            var result=new LinkedHashMap<String,Object>();result.put("passed",true);result.put("baselineShaderSha256",sha(baseline));result.put("nativeShaderSha256",sha(current));result.put("postShaderSha256",sha(post));
            result.put("actualMaterialNativePostEqual",true);result.put("allNonordinaryMaterialAndHaloUnchanged",true);result.put("frames",FRAMES);result.put("fps",FPS);result.put("coreCases",cases);result.put("peripheralCases",edges);result.put("max128s256mPeriodDifference",period);result.put("timeWrapTwoMillisecondDifference",wrap);
            result.put("scope","Actual old/new shipping GLSL on the same head-relative local patches. Per-point minima/dropouts and absolute PSD complement real mesh/video; no claim of projected triangle subpixel coverage or pommel bounds from a shader fixture.");return result;
        }finally{
            GL33.glBindFramebuffer(GL33.GL_DRAW_FRAMEBUFFER,previousDraw);GL33.glBindFramebuffer(GL33.GL_READ_FRAMEBUFFER,previousRead);GL33.glActiveTexture(GL33.GL_TEXTURE0);GL33.glBindTexture(GL33.GL_TEXTURE_2D,previousTexture);GL33.glActiveTexture(previousActive);GL33.glUseProgram(previousProgram);GL33.glBindVertexArray(previousVao);GL33.glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            for(int i=0;i<caps.length;i++)if(enabled[i])GL33.glEnable(caps[i]);else GL33.glDisable(caps[i]);
            if(fbo!=0)GL33.glDeleteFramebuffers(fbo);for(int t:new int[]{color,dummy})if(t!=0)GL33.glDeleteTextures(t);if(vao!=0)GL33.glDeleteVertexArrays(vao);if(old!=0)GL33.glDeleteProgram(old);if(now!=0)GL33.glDeleteProgram(now);
        }
    }
    private static float[] render(int program,int band,float arc,float span,float alpha,float time){
        GL33.glUseProgram(program);GL33.glUniform1i(GL33.glGetUniformLocation(program,"SmokeAtlas"),0);GL33.glUniform1f(GL33.glGetUniformLocation(program,"Band"),band);GL33.glUniform1f(GL33.glGetUniformLocation(program,"ArcOffset"),arc);GL33.glUniform1f(GL33.glGetUniformLocation(program,"AcrossSpan"),span);GL33.glUniform1f(GL33.glGetUniformLocation(program,"VertexAlpha"),alpha);GL33.glUniform1f(GL33.glGetUniformLocation(program,"EffectTime"),time);
        GL33.glDrawArrays(GL33.GL_TRIANGLES,0,3);float[] pixels=new float[WIDTH*HEIGHT*4];GL33.glReadPixels(0,0,WIDTH,HEIGHT,GL33.GL_RGBA,GL33.GL_FLOAT,pixels);return pixels;
    }
    private record Stats(double mean,double stepRms,double normalizedContrastRms,double worstMinMedian,long halfPeakDropouts,double totalPower,double highPower,double highFraction,List<Map<String,Object>> points){}
    private static Stats temporal(int program,int band,float seed,float alpha,boolean core){
        int[] ys=core?CORE_YS:EDGE_YS;int count=XS.length*ys.length;double[][] traces=new double[count][FRAMES];double mean=0,steps=0;
        for(int f=0;f<FRAMES;f++){
            float[] pixels=render(program,band,seed-14.25F,core?.22F:1.7F,alpha,(float)(7.125+f/FPS));int n=0;
            for(int y:ys)for(int x:XS){int k=(y*WIDTH+x)*4;double value=pixels[k]*.2126+pixels[k+1]*.7152+pixels[k+2]*.0722;if(!Double.isFinite(value))throw new AssertionError("Nonfinite ordinary radiance");traces[n][f]=value;mean+=value;if(f>0)steps+=Math.pow(value-traces[n][f-1],2);n++;}
        }
        double variance=0,worst=Double.POSITIVE_INFINITY,total=0,high=0;long drops=0;var points=new ArrayList<Map<String,Object>>();
        for(var trace:traces){double[] sorted=trace.clone();Arrays.sort(sorted);double median=(sorted[FRAMES/2-1]+sorted[FRAMES/2])*.5,min=sorted[0],max=sorted[FRAMES-1],average=Arrays.stream(trace).average().orElseThrow(),v=0;long off=0;
            for(double x:trace){v+=(x-average)*(x-average);if(x<max*.5)off++;}double normalized=Math.sqrt(v/FRAMES)/Math.max(1e-9,median);variance+=normalized*normalized;worst=Math.min(worst,min/Math.max(1e-9,median));drops+=off;
            points.add(Map.of("min",min,"p05",sorted[(int)(FRAMES*.05)],"median",median,"p95",sorted[(int)(FRAMES*.95)],"max",max,"minMedian",min/Math.max(1e-9,median),"normalizedRms",normalized,"halfPeakDropouts",off));
            for(int k=1;k<COS.length;k++){double re=0,im=0;for(int i=0;i<FRAMES;i++){double value=(trace[i]-average)*WINDOW[i];re+=value*COS[k][i];im+=value*SIN[k][i];}double power=(re*re+im*im)/(count*(double)FRAMES*FRAMES);total+=power;if(k*FPS/FRAMES>HIGH_HZ)high+=power;}
        }
        return new Stats(mean/(count*(double)FRAMES),Math.sqrt(steps/(count*(double)(FRAMES-1))),Math.sqrt(variance/count),worst,drops,total,high,total>0?high/total:0,List.copyOf(points));
    }
    private static double difference(float[] a,float[] b){double out=0;for(int i=0;i<a.length;i++){if(!Float.isFinite(a[i])||!Float.isFinite(b[i]))throw new AssertionError("Nonfinite ordinary shader");out=Math.max(out,Math.abs(a[i]-b[i]));}return out;}
    private static String sha(String text)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}
    private OrdinaryTrailFlickerQa(){}
}
