package cn.blockforge.generated.generatedmod.client.qa.myriad;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL33;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
        int programBefore=GL33.glGetInteger(GL33.GL_CURRENT_PROGRAM),vaoBefore=GL33.glGetInteger(GL33.GL_VERTEX_ARRAY_BINDING);
        int drawBefore=GL33.glGetInteger(GL33.GL_DRAW_FRAMEBUFFER_BINDING),readBefore=GL33.glGetInteger(GL33.GL_READ_FRAMEBUFFER_BINDING);
        int textureBefore=GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);int[] viewport=new int[4];GL33.glGetIntegerv(GL33.GL_VIEWPORT,viewport);
        int[] capabilities={GL33.GL_BLEND,GL33.GL_DEPTH_TEST,GL33.GL_CULL_FACE,GL33.GL_SCISSOR_TEST,GL33.GL_FRAMEBUFFER_SRGB};
        boolean[] enabled=new boolean[capabilities.length];for(int i=0;i<capabilities.length;i++){enabled[i]=GL33.glIsEnabled(capabilities[i]);GL33.glDisable(capabilities[i]);}
        int vertex=0,fragment=0,program=0,vao=0,texture=0,fbo=0;
        try{
            vertex=shader(GL33.GL_VERTEX_SHADER,"#version 150\nout vec2 uv;void main(){vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);uv=p;gl_Position=vec4(p*2.0-1.0,0.0,1.0);}");
            fragment=shader(GL33.GL_FRAGMENT_SHADER,"#version 150\nuniform float EffectTime,ArcOffset,Band;in vec2 uv;out vec4 fragColor;vec2 trailCoord;vec4 vertexColor;\n"+source+"\nvoid main(){trailCoord=vec2(ArcOffset+uv.x*15.25,Band+.02+uv.y*.96);vertexColor=vec4(.35,.65,.95,.8);fragColor=swordMaterial();}");
            program=GL33.glCreateProgram();GL33.glAttachShader(program,vertex);GL33.glAttachShader(program,fragment);GL33.glLinkProgram(program);
            if(GL33.glGetProgrami(program,GL33.GL_LINK_STATUS)==0)throw new AssertionError(GL33.glGetProgramInfoLog(program));
            texture=GL33.glGenTextures();GL33.glBindTexture(GL33.GL_TEXTURE_2D,texture);
            GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_RGBA32F,64,32,0,GL33.GL_RGBA,GL33.GL_FLOAT,0L);
            fbo=GL33.glGenFramebuffers();GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER,fbo);GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER,GL33.GL_COLOR_ATTACHMENT0,GL33.GL_TEXTURE_2D,texture,0);
            if(GL33.glCheckFramebufferStatus(GL33.GL_FRAMEBUFFER)!=GL33.GL_FRAMEBUFFER_COMPLETE)throw new AssertionError("Material fixture FBO incomplete");
            vao=GL33.glGenVertexArrays();GL33.glBindVertexArray(vao);GL33.glUseProgram(program);GL33.glViewport(0,0,64,32);
            double error=0;int comparisons=0;
            for(int band:new int[]{6,8,10,12,14,16,18,20,22}){
                float[] reference=render(program,band,47.25F,7.125F);
                float low=Float.POSITIVE_INFINITY,high=0;for(float v:reference){low=Math.min(low,v);high=Math.max(high,v);}
                if(high<.1||high-low<.01)throw new AssertionError("Material fixture produced no varying signal: "+band);
                error=Math.max(error,difference(reference,render(program,band,47.25F,135.125F)));comparisons++;
                // Smoke UVs encode stable birth identity, not a rebased path arc.
                if(band!=14&&band!=20){error=Math.max(error,difference(reference,render(program,band,303.25F,7.125F)));comparisons++;}
            }
            if(error>.025)throw new AssertionError("Material periodic seam error: "+error);
            int gl=GL33.glGetError();if(gl!=0)throw new AssertionError("Material fixture GL error "+gl);
            return Map.of("check","shipping-glsl-periodicity","comparisons",comparisons,"maxChannelError",error,"passed",true);
        }finally{
            GL33.glBindFramebuffer(GL33.GL_DRAW_FRAMEBUFFER,drawBefore);GL33.glBindFramebuffer(GL33.GL_READ_FRAMEBUFFER,readBefore);
            GL33.glBindTexture(GL33.GL_TEXTURE_2D,textureBefore);GL33.glBindVertexArray(vaoBefore);GL33.glUseProgram(programBefore);GL33.glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            for(int i=0;i<capabilities.length;i++)if(enabled[i])GL33.glEnable(capabilities[i]);else GL33.glDisable(capabilities[i]);
            if(fbo!=0)GL33.glDeleteFramebuffers(fbo);if(texture!=0)GL33.glDeleteTextures(texture);if(vao!=0)GL33.glDeleteVertexArrays(vao);
            if(program!=0)GL33.glDeleteProgram(program);if(vertex!=0)GL33.glDeleteShader(vertex);if(fragment!=0)GL33.glDeleteShader(fragment);
        }
    }
    private static float[] render(int program,int band,float arc,float time){
        GL33.glUniform1f(GL33.glGetUniformLocation(program,"Band"),band);GL33.glUniform1f(GL33.glGetUniformLocation(program,"ArcOffset"),arc);GL33.glUniform1f(GL33.glGetUniformLocation(program,"EffectTime"),time);
        GL33.glDrawArrays(GL33.GL_TRIANGLES,0,3);float[] pixels=new float[64*32*4];GL33.glReadPixels(0,0,64,32,GL33.GL_RGBA,GL33.GL_FLOAT,pixels);return pixels;
    }
    private static double difference(float[] a,float[] b){double max=0;for(int i=0;i<a.length;i++){if(!Float.isFinite(a[i])||!Float.isFinite(b[i]))throw new AssertionError("Nonfinite material output");max=Math.max(max,Math.abs(a[i]-b[i]));}return max;}
    private HuahongMaterialQa(){}
}
