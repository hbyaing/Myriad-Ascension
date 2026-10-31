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
            return source.substring(source.indexOf("float hash21"),source.indexOf("void main"));
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
            fragment=shader(GL33.GL_FRAGMENT_SHADER,"#version 150\nuniform sampler2D SmokeAtlas;uniform float EffectTime,ArcOffset,Band;uniform vec2 ProbeUV;uniform int ProbeMode;in vec2 uv;out vec4 fragColor;vec2 trailCoord;vec4 vertexColor;\n"+source+"\nvoid main(){trailCoord=ProbeMode==1?vec2(ProbeUV.x,Band+ProbeUV.y):vec2(ArcOffset+uv.x*15.25,Band+.02+uv.y*.96);vertexColor=vec4(.35,.65,.95,.8);fragColor=swordMaterial();}");
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
            int gl=GL33.glGetError();if(gl!=0)throw new AssertionError("Material fixture GL error "+gl);
            var result=new LinkedHashMap<String,Object>();result.put("check","shipping-glsl-periodicity");result.put("comparisons",comparisons);result.put("maxChannelError",error);result.put("passed",true);
            result.put("noseBand",32);result.put("noseProbes",noseProbes);result.put("noseCenterAlpha",noseCenter[3]);result.put("noseMaxBoundaryAlpha",noseEdge);result.put("noseMaxTimeError",noseTimeError);result.put("noseProjection",noseProjection);
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
    private static double difference(float[] a,float[] b){double max=0;for(int i=0;i<a.length;i++){if(!Float.isFinite(a[i])||!Float.isFinite(b[i]))throw new AssertionError("Nonfinite material output");max=Math.max(max,Math.abs(a[i]-b[i]));}return max;}
    private HuahongMaterialQa(){}
}
