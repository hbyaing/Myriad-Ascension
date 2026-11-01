package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.client.HuahongVolumeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL33;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Actual production volume field probes, complementary to continuous game video. */
final class HuahongVolumeMaterialQa {
    private static final int WIDTH=32,HEIGHT=16,FRAMES=240;
    private static String shipping(Minecraft mc)throws Exception{
        try(var in=mc.getResourceManager().getResource(new ResourceLocation("wuxingxiantu","shaders/core/huahong_volume.fsh")).orElseThrow().open()){
            return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static int shader(int type,String source){
        int id=GL33.glCreateShader(type);GL33.glShaderSource(id,source);GL33.glCompileShader(id);
        if(GL33.glGetShaderi(id,GL33.GL_COMPILE_STATUS)==0)throw new AssertionError(GL33.glGetShaderInfoLog(id));return id;
    }
    private static int program(String source){
        String wrapper="\nuniform mat3 ProbeToWorld;uniform vec3 ProbeHead;in vec2 probeUv;\nvoid main(){"
                +"vec3 p=ProbeHead+ProbeToWorld*vec3((probeUv.y-.5)*1.2,.173,-6.0-probeUv.x*26.0);"
                +"vec4 f=field(p,ProbeToWorld*vec3(0,1,0),.025,ProbeToWorld*vec3(.03,0,0),ProbeToWorld*vec3(0,0,.03));"
                +"fragColor=vec4(vec3(1.0)-exp(-f.rgb*f.a*.03),1.0-exp(-f.a*.03));}";
        int v=shader(GL33.GL_VERTEX_SHADER,"#version 150\nout vec2 probeUv;void main(){vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);probeUv=p;gl_Position=vec4(p*2.0-1.0,0,1);}"),
                f=shader(GL33.GL_FRAGMENT_SHADER,source.substring(0,source.indexOf("void main"))+wrapper),p=GL33.glCreateProgram();
        GL33.glAttachShader(p,v);GL33.glAttachShader(p,f);GL33.glLinkProgram(p);GL33.glDeleteShader(v);GL33.glDeleteShader(f);
        if(GL33.glGetProgrami(p,GL33.GL_LINK_STATUS)==0)throw new AssertionError(GL33.glGetProgramInfoLog(p));return p;
    }
    static Map<String,Object> verify(Minecraft mc)throws Exception{
        String current=shipping(mc),baseline;
        try(var in=HuahongVolumeMaterialQa.class.getResourceAsStream("/qa/huahong_volume_world_anchor.fsh")){
            if(in==null)throw new AssertionError("Missing frozen original volume shader");baseline=new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
        int programBefore=GL33.glGetInteger(GL33.GL_CURRENT_PROGRAM),vaoBefore=GL33.glGetInteger(GL33.GL_VERTEX_ARRAY_BINDING),
                drawBefore=GL33.glGetInteger(GL33.GL_DRAW_FRAMEBUFFER_BINDING),readBefore=GL33.glGetInteger(GL33.GL_READ_FRAMEBUFFER_BINDING),
                activeBefore=GL33.glGetInteger(GL33.GL_ACTIVE_TEXTURE);GL33.glActiveTexture(GL33.GL_TEXTURE0);
        int textureBefore=GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);int[] viewport=new int[4];GL33.glGetIntegerv(GL33.GL_VIEWPORT,viewport);
        int[] caps={GL33.GL_BLEND,GL33.GL_DEPTH_TEST,GL33.GL_CULL_FACE,GL33.GL_SCISSOR_TEST,GL33.GL_FRAMEBUFFER_SRGB};
        boolean[] enabled=new boolean[caps.length];for(int i=0;i<caps.length;i++){enabled[i]=GL33.glIsEnabled(caps[i]);GL33.glDisable(caps[i]);}
        int old=0,fixed=0,vao=0,texture=0,fbo=0;
        try{
            old=program(baseline);fixed=program(current);texture=GL33.glGenTextures();GL33.glBindTexture(GL33.GL_TEXTURE_2D,texture);
            GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_RGBA32F,WIDTH,HEIGHT,0,GL33.GL_RGBA,GL33.GL_FLOAT,0L);
            fbo=GL33.glGenFramebuffers();GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER,fbo);GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER,GL33.GL_COLOR_ATTACHMENT0,GL33.GL_TEXTURE_2D,texture,0);
            if(GL33.glCheckFramebufferStatus(GL33.GL_FRAMEBUFFER)!=GL33.GL_FRAMEBUFFER_COMPLETE)throw new AssertionError("Volume material FBO incomplete");
            vao=GL33.glGenVertexArrays();GL33.glBindVertexArray(vao);GL33.glViewport(0,0,WIDTH,HEIGHT);
            float[] reference=render(fixed,0,0,0,0,0,7.125F);double covariance=0,oldCovariance=0,timePeriod=0,timeWrap=0;
            for(float[] pose:new float[][]{{0,0,0,127,17},{45,20,13,90,31},{179,-45,89,233,-48},{270,75,-35,-127,63}}){
                covariance=Math.max(covariance,difference(reference,render(fixed,pose[0],pose[1],pose[2],pose[3],pose[4],7.125F)));
                oldCovariance=Math.max(oldCovariance,difference(render(old,0,0,0,0,0,7.125F),render(old,pose[0],pose[1],pose[2],pose[3],pose[4],7.125F)));
            }
            timePeriod=difference(reference,render(fixed,0,0,0,0,0,135.125F));
            timeWrap=difference(render(fixed,0,0,0,0,0,127.9999F),render(fixed,0,0,0,0,0,.0001F));
            Stats before=temporal(old),after=temporal(fixed);
            // Chosen before candidate results: rigid transport must be invariant,
            // and visible time animation must survive with >=3x lower frame delta.
            if(covariance>.02||oldCovariance<.05||timePeriod>.02||timeWrap>.02)
                throw new AssertionError("Volume material transform/period discontinuity "+covariance+" / old "+oldCovariance+" / period "+timePeriod+" / wrap "+timeWrap);
            if(before.stepRms<1e-4||after.totalPower<1e-9||after.stepRms<1e-6||after.stepRms>before.stepRms/3)
                throw new AssertionError("Volume flow empty, frozen or still aliases "+before+" / "+after);
            int error=GL33.glGetError();if(error!=0)throw new AssertionError("Volume material GL error "+error);
            var r=new LinkedHashMap<String,Object>();r.put("passed",true);r.put("check","actual-shipping-volume-field-transport");
            r.put("scope","Frozen previous production field vs actual shipping field, real renderer basis; synthetic local probes plus separate real continuous video. Geometry/depth not replaced by this fixture.");
            r.put("baselineSha256",sha(baseline));r.put("shippingSha256",sha(current));r.put("maxRigidTranslationRotationError",covariance);r.put("baselineRigidError",oldCovariance);
            r.put("timePeriod128Error",timePeriod);r.put("timeWrapError",timeWrap);r.put("before",before);r.put("after",after);r.put("frameDeltaReduction",before.stepRms/after.stepRms);r.put("frames",FRAMES);r.put("fps",120);r.put("speedWorldUnitsPerSecond",300);return r;
        }finally{
            GL33.glBindFramebuffer(GL33.GL_DRAW_FRAMEBUFFER,drawBefore);GL33.glBindFramebuffer(GL33.GL_READ_FRAMEBUFFER,readBefore);
            GL33.glActiveTexture(GL33.GL_TEXTURE0);GL33.glBindTexture(GL33.GL_TEXTURE_2D,textureBefore);GL33.glActiveTexture(activeBefore);GL33.glBindVertexArray(vaoBefore);GL33.glUseProgram(programBefore);GL33.glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            for(int i=0;i<caps.length;i++)if(enabled[i])GL33.glEnable(caps[i]);else GL33.glDisable(caps[i]);
            if(fbo!=0)GL33.glDeleteFramebuffers(fbo);if(texture!=0)GL33.glDeleteTextures(texture);if(vao!=0)GL33.glDeleteVertexArrays(vao);if(old!=0)GL33.glDeleteProgram(old);if(fixed!=0)GL33.glDeleteProgram(fixed);
        }
    }
    private static String sha(String source)throws Exception{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));}
    private static float[] render(int program,float yaw,float pitch,float roll,float worldX,float worldZ,float time)throws Exception{
        Matrix3f model=new Matrix3f().rotateY((float)Math.toRadians(yaw)).rotateX((float)Math.toRadians(pitch)).rotateZ((float)Math.toRadians(roll));
        Vector3f s=model.transform(new Vector3f(1,0,0)),f=model.transform(new Vector3f(0,0,1));
        // Reflection keeps this test-only class linkable during old-code baseline capture.
        Matrix3f basis=(Matrix3f)HuahongVolumeRenderer.class.getMethod("materialBasis",Vec3.class,Vec3.class)
                .invoke(null,new Vec3(f.x,f.y,f.z),new Vec3(s.x,s.y,s.z));
        GL33.glUseProgram(program);float[] matrix=new float[9];basis.get(matrix);GL33.glUniformMatrix3fv(GL33.glGetUniformLocation(program,"WorldToMaterial"),false,matrix);
        model.get(matrix);GL33.glUniformMatrix3fv(GL33.glGetUniformLocation(program,"ProbeToWorld"),false,matrix);
        // Both fields receive the same real path transformed by the same pose.
        float headX=worldX*.1F,headZ=worldZ*.1F;float[] path=new float[32*4],ages=new float[32];
        for(int i=0;i<32;i++){float behind=i*40F/31;Vector3f p=model.transform(new Vector3f(0,0,-behind));path[i*4]=p.x+headX;path[i*4+1]=p.y;path[i*4+2]=p.z+headZ;path[i*4+3]=-behind;ages[i]=behind/300;}
        GL33.glUniform3f(GL33.glGetUniformLocation(program,"ProbeHead"),headX,0,headZ);
        GL33.glUniform3f(GL33.glGetUniformLocation(program,"NoiseOrigin"),worldX-headX,0,worldZ-headZ);
        GL33.glUniform4fv(GL33.glGetUniformLocation(program,"PathPoints[0]"),path);GL33.glUniform1fv(GL33.glGetUniformLocation(program,"PathAges[0]"),ages);
        GL33.glUniform1i(GL33.glGetUniformLocation(program,"PathCount"),32);GL33.glUniform1f(GL33.glGetUniformLocation(program,"PathLength"),40);
        GL33.glUniform1f(GL33.glGetUniformLocation(program,"Strength"),1);GL33.glUniform4f(GL33.glGetUniformLocation(program,"ShapeProfile"),.52F,2.2F,1.25F,3.5F);
        GL33.glUniform1f(GL33.glGetUniformLocation(program,"EffectTime"),time);GL33.glDrawArrays(GL33.GL_TRIANGLES,0,3);
        float[] pixels=new float[WIDTH*HEIGHT*4];GL33.glReadPixels(0,0,WIDTH,HEIGHT,GL33.GL_RGBA,GL33.GL_FLOAT,pixels);return pixels;
    }
    private record Stats(double stepRms,double meanSignal,double totalPower,double powerAbove8HzFraction){}
    private static Stats temporal(int program)throws Exception{
        int points=48;double[][] traces=new double[points][FRAMES];double steps=0,signal=0;
        for(int frame=0;frame<FRAMES;frame++){
            float t=frame/120F;float[] pixels=render(program,t*37,t*19,t*13,t*300,0,7.125F+t);int n=0;
            for(int y:new int[]{2,6,10,14})for(int x=1;x<25;x+=2){int i=(y*WIDTH+x)*4;double value=.2126*pixels[i]+.7152*pixels[i+1]+.0722*pixels[i+2];
                if(!Double.isFinite(value))throw new AssertionError("Nonfinite volume material output");traces[n][frame]=value;signal+=value;if(frame>0)steps+=Math.pow(value-traces[n][frame-1],2);n++;}
        }
        double total=0,high=0;
        for(var trace:traces){double mean=Arrays.stream(trace).average().orElseThrow();
            for(int k=1;k<=FRAMES/2;k++){double real=0,imag=0;for(int i=0;i<FRAMES;i++){double value=(trace[i]-mean)*(.5-.5*Math.cos(2*Math.PI*i/(FRAMES-1))),angle=2*Math.PI*k*i/FRAMES;real+=value*Math.cos(angle);imag+=value*Math.sin(angle);}
                double power=(real*real+imag*imag)/(points*(double)FRAMES*FRAMES);total+=power;if(k*120.0/FRAMES>8)high+=power;}
        }
        return new Stats(Math.sqrt(steps/(points*(double)(FRAMES-1))),signal/(points*(double)FRAMES),total,high/total);
    }
    private static double difference(float[] a,float[] b){double d=0;for(int i=0;i<a.length;i++){if(!Float.isFinite(a[i])||!Float.isFinite(b[i]))throw new AssertionError("Nonfinite volume shader output");d=Math.max(d,Math.abs(a[i]-b[i]));}return d;}
    private HuahongVolumeMaterialQa(){}
}
