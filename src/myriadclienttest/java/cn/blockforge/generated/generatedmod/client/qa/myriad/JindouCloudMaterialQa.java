package cn.blockforge.generated.generatedmod.client.qa.myriad;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.platform.NativeImage;
import org.joml.Matrix3f;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Shipping density functions and atlas; complements the real cloud flight clip. */
final class JindouCloudMaterialQa {
    private static final int WIDTH=64,HEIGHT=32,FRAMES=240;
    private static double candidateFlowRate;
    private static java.lang.reflect.Method materialField;
    private static String resource(Minecraft mc,String path)throws Exception{
        try(var in=mc.getResourceManager().getResource(new ResourceLocation("wuxingxiantu",path)).orElseThrow().open()){
            return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static int shader(int type,String source){
        int id=GL33.glCreateShader(type);GL33.glShaderSource(id,source);GL33.glCompileShader(id);
        if(GL33.glGetShaderi(id,GL33.GL_COMPILE_STATUS)==0)throw new AssertionError(GL33.glGetShaderInfoLog(id));return id;
    }
    private static int program(String source){
        String wrapper="\nuniform int ProbeMode;in vec2 probeUv;void main(){vec3 p=ProbeMode==0?vec3((probeUv.x-.5)*3.4,(probeUv.y-.5)*1.2,0.0):vec3((probeUv.y-.5)*1.6,.08,-3.9-probeUv.x*2.0);float d=densityAt(p);fragColor=vec4(d,d,d,1);}";
        int v=shader(GL33.GL_VERTEX_SHADER,"#version 150\nout vec2 probeUv;void main(){vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);probeUv=p;gl_Position=vec4(p*2.0-1,0,1);}"),
                f=shader(GL33.GL_FRAGMENT_SHADER,source.substring(0,source.indexOf("void main"))+wrapper),p=GL33.glCreateProgram();
        GL33.glAttachShader(p,v);GL33.glAttachShader(p,f);GL33.glLinkProgram(p);GL33.glDeleteShader(v);GL33.glDeleteShader(f);
        if(GL33.glGetProgrami(p,GL33.GL_LINK_STATUS)==0)throw new AssertionError(GL33.glGetProgramInfoLog(p));return p;
    }
    static Map<String,Object> verify(Minecraft mc)throws Exception{
        var field=Class.forName("cn.blockforge.generated.generatedmod.client.JindouCloudTime");
        candidateFlowRate=(double)field.getMethod("flowStep",float.class).invoke(null,5F/5.7F)*20;
        materialField=field.getMethod("material",float[].class,double.class,double.class,double.class);
        String current=resource(mc,"shaders/core/jindou_cloud.fsh"),old;
        try(var in=JindouCloudMaterialQa.class.getResourceAsStream("/qa/jindou_cloud_f214.fsh")){
            if(in==null)throw new AssertionError("Missing frozen f214 cloud shader");old=new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
        byte[] atlas;try(var in=mc.getResourceManager().getResource(new ResourceLocation("wuxingxiantu","textures/effect/jindou_worley.png")).orElseThrow().open()){atlas=in.readAllBytes();}
        int previousProgram=GL33.glGetInteger(GL33.GL_CURRENT_PROGRAM),previousVao=GL33.glGetInteger(GL33.GL_VERTEX_ARRAY_BINDING),
                previousDraw=GL33.glGetInteger(GL33.GL_DRAW_FRAMEBUFFER_BINDING),previousRead=GL33.glGetInteger(GL33.GL_READ_FRAMEBUFFER_BINDING),
                previousActive=GL33.glGetInteger(GL33.GL_ACTIVE_TEXTURE);int[] textures=new int[3],viewport=new int[4];GL33.glGetIntegerv(GL33.GL_VIEWPORT,viewport);
        for(int i=0;i<3;i++){GL33.glActiveTexture(GL33.GL_TEXTURE0+i);textures[i]=GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);}
        int[] caps={GL33.GL_BLEND,GL33.GL_DEPTH_TEST,GL33.GL_CULL_FACE,GL33.GL_SCISSOR_TEST,GL33.GL_FRAMEBUFFER_SRGB};boolean[] enabled=new boolean[caps.length];
        for(int i=0;i<caps.length;i++){enabled[i]=GL33.glIsEnabled(caps[i]);GL33.glDisable(caps[i]);}
        int before=0,after=0,vao=0,color=0,fbo=0,noise=0,trail=0;
        try{
            before=program(old);after=program(current);GL33.glActiveTexture(GL33.GL_TEXTURE0);
            color=GL33.glGenTextures();GL33.glBindTexture(GL33.GL_TEXTURE_2D,color);GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_RGBA32F,WIDTH,HEIGHT,0,GL33.GL_RGBA,GL33.GL_FLOAT,0L);
            fbo=GL33.glGenFramebuffers();GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER,fbo);GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER,GL33.GL_COLOR_ATTACHMENT0,GL33.GL_TEXTURE_2D,color,0);
            if(GL33.glCheckFramebufferStatus(GL33.GL_FRAMEBUFFER)!=GL33.GL_FRAMEBUFFER_COMPLETE)throw new AssertionError("Cloud density FBO incomplete");
            noise=GL33.glGenTextures();GL33.glBindTexture(GL33.GL_TEXTURE_2D,noise);
            try(var image=NativeImage.read(new java.io.ByteArrayInputStream(atlas))){
                ByteBuffer pixels=MemoryUtil.memAlloc(image.getWidth()*image.getHeight()*4);
                try{for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){int rgba=image.getPixelRGBA(x,y);pixels.put((byte)rgba).put((byte)(rgba>>>8)).put((byte)(rgba>>>16)).put((byte)(rgba>>>24));}pixels.flip();GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_RGBA8,image.getWidth(),image.getHeight(),0,GL33.GL_RGBA,GL33.GL_UNSIGNED_BYTE,pixels);}finally{MemoryUtil.memFree(pixels);}
            }
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MIN_FILTER,GL33.GL_LINEAR);GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MAG_FILTER,GL33.GL_LINEAR);
            GL33.glActiveTexture(GL33.GL_TEXTURE2);trail=GL33.glGenTextures();GL33.glBindTexture(GL33.GL_TEXTURE_2D,trail);
            float[] data=new float[32*20];
            for(int i=0;i<32;i++){int k=i*20;float center=-i*.55F;
                data[k+2]=center-.275F;data[k+3]=(i*.55F+.275F)/5;
                data[k+6]=i==0?0:center+.275F;data[k+7]=Math.max(0,(i*.55F-.275F)/5);
                data[k+8]=data[k+9]=5;data[k+10]=data[k+11]=1;data[k+12]=data[k+13]=2.025F;data[k+14]=data[k+15]=1;data[k+18]=center;
            }
            GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_RGBA32F,5,32,0,GL33.GL_RGBA,GL33.GL_FLOAT,data);
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MIN_FILTER,GL33.GL_NEAREST);GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MAG_FILTER,GL33.GL_NEAREST);
            vao=GL33.glGenVertexArrays();GL33.glBindVertexArray(vao);GL33.glViewport(0,0,WIDTH,HEIGHT);
            var results=new LinkedHashMap<String,Object>();
            for(int mode=0;mode<2;mode++){
                Stats baseline=temporal(before,mode,true),candidate=temporal(after,mode,false);
                if(baseline.stepRms<1e-4||candidate.stepRms<1e-6||candidate.totalPower<1e-8||candidate.meanDensity<1e-4||candidate.stepRms>baseline.stepRms/3)
                    throw new AssertionError("Cloud density empty/frozen/still aliases mode"+mode+": "+baseline+" / "+candidate);
                // The previous low-RMS candidate passed but looked frozen to
                // the user. Retain visible temporal energy in this fixed probe.
                if(candidate.totalPower<(mode==0?.0005:.00008))
                    throw new AssertionError("Cloud flow has been over-stabilized again: "+candidate);
                results.put(mode==0?"sourceBody":"historicalWake",Map.of("baseline",baseline,"candidate",candidate,"frameDeltaReduction",baseline.stepRms/candidate.stepRms));
            }
            float[] reference=render(after,1,false,0,0,0);double rigid=0,oldRigid=0;
            for(float yaw:new float[]{45,90,179,270}){
                rigid=Math.max(rigid,difference(reference,render(after,1,false,0,137,yaw)));
                oldRigid=Math.max(oldRigid,difference(render(before,1,true,0,0,0),render(before,1,true,0,137,yaw)));
            }
            if(rigid>1e-5||oldRigid<.02)throw new AssertionError("Cloud source material rigid-domain check "+rigid+" / old "+oldRigid);
            Map<String,Object> track=trackLifecycle();int error=GL33.glGetError();if(error!=0)throw new AssertionError("Cloud density GL error "+error);
            results.put("maxRigidWakeDensityDifference",rigid);results.put("baselineRigidWakeDifference",oldRigid);results.put("trackLifecycle",track);
            results.put("passed",true);results.put("fps",120);results.put("frames",FRAMES);results.put("speedWorldUnitsPerSecond",100);results.put("atlasSha256",sha(atlas));results.put("baselineShaderSha256",sha(old.getBytes(StandardCharsets.UTF_8)));results.put("shaderSha256",sha(current.getBytes(StandardCharsets.UTF_8)));
            results.put("scope","Actual shipping density functions and actual atlas; body travel-flow and wake world-scan isolated separately. Synthetic fields complement real native/Solas flight; not final visual acceptance.");return results;
        }finally{
            GL33.glBindFramebuffer(GL33.GL_DRAW_FRAMEBUFFER,previousDraw);GL33.glBindFramebuffer(GL33.GL_READ_FRAMEBUFFER,previousRead);
            for(int i=0;i<3;i++){GL33.glActiveTexture(GL33.GL_TEXTURE0+i);GL33.glBindTexture(GL33.GL_TEXTURE_2D,textures[i]);}GL33.glActiveTexture(previousActive);
            GL33.glBindVertexArray(previousVao);GL33.glUseProgram(previousProgram);GL33.glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            for(int i=0;i<caps.length;i++)if(enabled[i])GL33.glEnable(caps[i]);else GL33.glDisable(caps[i]);
            if(fbo!=0)GL33.glDeleteFramebuffers(fbo);for(int t:new int[]{color,noise,trail})if(t!=0)GL33.glDeleteTextures(t);if(vao!=0)GL33.glDeleteVertexArrays(vao);if(before!=0)GL33.glDeleteProgram(before);if(after!=0)GL33.glDeleteProgram(after);
        }
    }
    private static String sha(byte[] bytes)throws Exception{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static float fract(double n){return (float)(n-Math.floor(n));}
    private static float[] render(int program,int mode,boolean legacy,double seconds,float world,float yaw){
        GL33.glUseProgram(program);GL33.glUniform1i(GL33.glGetUniformLocation(program,"Sampler0"),0);GL33.glUniform1i(GL33.glGetUniformLocation(program,"TrailData"),2);
        GL33.glUniform1i(GL33.glGetUniformLocation(program,"ProbeMode"),mode);GL33.glUniform3f(GL33.glGetUniformLocation(program,"CloudRadii"),2.025F,.9522F,1.575F);
        GL33.glUniform1f(GL33.glGetUniformLocation(program,"DensityScale"),1);GL33.glUniform1f(GL33.glGetUniformLocation(program,"MotionAmount"),5F/5.7F);
        GL33.glUniform1i(GL33.glGetUniformLocation(program,"TrailCount"),mode==0?0:32);GL33.glUniform1f(GL33.glGetUniformLocation(program,"TrailLifetime"),20);
        double t=7.125+seconds,seed=.25,flow=seconds*(legacy?100*.035:candidateFlowRate);
        GL33.glUniform3f(GL33.glGetUniformLocation(program,"NoiseOffset"),(float)seed,fract(seed*.37),fract(seed*.73));
        if(!legacy)GL33.glUniform3f(GL33.glGetUniformLocation(program,"NoiseOffset"),(float)seed,fract(seed*.37),fract(seed*.73-flow));
        float[] phases=new float[6];
        if(legacy){phases[0]=(float)(fract(t/11+seed)*Math.PI*2);phases[1]=(float)(fract(t/17+seed*.37)*Math.PI*2);phases[2]=(float)(fract(t/23+seed*.73)*Math.PI*2);phases[3]=fract(t*.025+seed);phases[4]=fract(t*.016+seed*.37);phases[5]=fract(-t*.020-flow+seed*.73);}
        else try{materialField.invoke(null,phases,t*20,flow,seed);}catch(Exception failure){throw new AssertionError("Production cloud material fields unavailable",failure);}
        GL33.glUniform3f(GL33.glGetUniformLocation(program,"FlowPhase"),phases[0],phases[1],phases[2]);
        GL33.glUniform3f(GL33.glGetUniformLocation(program,"DetailOffset"),phases[3],phases[4],phases[5]);
        GL33.glUniform3f(GL33.glGetUniformLocation(program,"HistoryNoiseOrigin"),0,0,(float)((world+seconds*100)*.38%10));
        GL33.glUniform3f(GL33.glGetUniformLocation(program,"HistoryDetailOffset"),fract(t*.013+seed),fract(t*.007+seed*.37),fract(-t*.010+seed*.73));
        float[] matrix=new float[9];new Matrix3f().rotateY((float)Math.toRadians(-yaw)).get(matrix);GL33.glUniformMatrix3fv(GL33.glGetUniformLocation(program,"WorldToCloud"),false,matrix);
        GL33.glDrawArrays(GL33.GL_TRIANGLES,0,3);float[] pixels=new float[WIDTH*HEIGHT*4];GL33.glReadPixels(0,0,WIDTH,HEIGHT,GL33.GL_RGBA,GL33.GL_FLOAT,pixels);return pixels;
    }
    private record Stats(double stepRms,double meanDensity,double totalPower,double powerAbove8HzFraction){}
    private static Stats temporal(int program,int mode,boolean legacy){
        int points=48;double[][] traces=new double[points][FRAMES];double steps=0,signal=0;
        for(int frame=0;frame<FRAMES;frame++){
            float[] pixels=render(program,mode,legacy,frame/120.0,0,0);int n=0;
            for(int y:new int[]{6,12,19,25})for(int x=3;x<60;x+=5){double value=pixels[(y*WIDTH+x)*4];if(!Double.isFinite(value))throw new AssertionError("Nonfinite cloud field");traces[n][frame]=value;signal+=value;if(frame>0)steps+=Math.pow(value-traces[n][frame-1],2);n++;}
        }
        double total=0,high=0;for(var trace:traces){double mean=Arrays.stream(trace).average().orElseThrow();
            for(int k=1;k<=FRAMES/2;k++){double re=0,im=0;for(int i=0;i<FRAMES;i++){double value=(trace[i]-mean)*(.5-.5*Math.cos(2*Math.PI*i/(FRAMES-1))),a=2*Math.PI*k*i/FRAMES;re+=value*Math.cos(a);im+=value*Math.sin(a);}double power=(re*re+im*im)/(points*(double)FRAMES*FRAMES);total+=power;if(k*120.0/FRAMES>8)high+=power;}
        }return new Stats(Math.sqrt(steps/(points*(double)(FRAMES-1))),signal/(points*(double)FRAMES),total,high/total);
    }
    private static double difference(float[] a,float[] b){double d=0;for(int i=0;i<a.length;i+=4){if(!Float.isFinite(a[i])||!Float.isFinite(b[i]))throw new AssertionError("Nonfinite cloud density");d=Math.max(d,Math.abs(a[i]-b[i]));}return d;}
    private static Map<String,Object> trackLifecycle()throws Exception{
        Class<?> type=Class.forName("cn.blockforge.generated.generatedmod.client.JindouCloudTrailCache$Track");var constructor=type.getDeclaredConstructor();constructor.setAccessible(true);Object track=constructor.newInstance();
        var sample=type.getDeclaredMethod("sample",Vec3.class,long.class,Vec3.class,Vec3.class);sample.setAccessible(true);
        var heading=type.getDeclaredField("headingYaw");heading.setAccessible(true);var reset=type.getDeclaredField("discontinuityTick");reset.setAccessible(true);var flow=type.getDeclaredField("flow");flow.setAccessible(true);
        Class<?> clockType=Class.forName("cn.blockforge.generated.generatedmod.client.JindouCloudTime$Clock");Object clock=clockType.getDeclaredConstructor().newInstance();var level=clockType.getMethod("setLevel",Object.class);var advance=clockType.getMethod("advance",boolean.class);var tick=clockType.getMethod("tick");level.invoke(clock,new Object());
        Vec3 p=Vec3.ZERO;long first=0;int checks=0;double maxFlow=0;
        for(int i=0;i<30;i++){advance.invoke(clock,true);long now=(long)tick.invoke(clock);Vec3 v=new Vec3(Math.sin(i*.12)*5,0,Math.cos(i*.12)*5),next=p.add(v);double previous=flow.getDouble(track);sample.invoke(track,next,now,v,p);if(i==0)first=reset.getLong(track);else if(reset.getLong(track)!=first)throw new AssertionError("Continuous cloud path reset");maxFlow=Math.max(maxFlow,flow.getDouble(track)-previous);p=next;checks++;}
        float last=heading.getFloat(track);for(int i=0;i<10;i++){advance.invoke(clock,true);sample.invoke(track,p,(long)tick.invoke(clock),Vec3.ZERO,p);if(heading.getFloat(track)!=last)throw new AssertionError("Stopped cloud continues rotating old field");checks++;}
        long paused=(long)tick.invoke(clock);for(int i=0;i<20;i++){advance.invoke(clock,false);sample.invoke(track,p,(long)tick.invoke(clock),Vec3.ZERO,p);if((long)tick.invoke(clock)!=paused||reset.getLong(track)!=first)throw new AssertionError("Paused cloud clock/history changed");checks++;}
        advance.invoke(clock,true);Vec3 fast=new Vec3(40,0,0);sample.invoke(track,p.add(fast),(long)tick.invoke(clock),fast,p);p=p.add(fast);if(reset.getLong(track)!=first)throw new AssertionError("Fast legitimate cloud path reset");checks++;
        advance.invoke(clock,true);Vec3 teleport=p.add(100,0,0);long now=(long)tick.invoke(clock);sample.invoke(track,teleport,now,Vec3.ZERO,teleport);if(reset.getLong(track)!=now)throw new AssertionError("Genuine cloud teleport did not reset");checks++;
        if(maxFlow>.01000001)throw new AssertionError("Cloud travel flow still unbounded: "+maxFlow);
        return Map.of("passed",true,"actualTrackReflectionChecks",checks,"maxFlowStep",maxFlow,"stoppedHeadingDifference",0,"pauseKeptHistory",true,"fast40BlocksPerTickKeptHistory",true,"trueTeleportReset",true,"scope","Actual production Track plus pure production clock; real active-stop/cloud video validates integration separately.");
    }
    private JindouCloudMaterialQa(){}
}
