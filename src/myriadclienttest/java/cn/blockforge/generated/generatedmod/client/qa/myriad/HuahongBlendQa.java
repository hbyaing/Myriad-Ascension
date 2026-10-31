package cn.blockforge.generated.generatedmod.client.qa.myriad;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Full shipping fragment-main regressions against a real floating-point sky FBO. QA only. */
final class HuahongBlendQa {
    private static final int W=64,H=32;
    private static final float[] SKY={.55F,.70F,.90F,1F};
    private static final int[] LIGHT={6,8,10,12,16,18,20,22,24,26,28,32,34};
    private static final String VERTEX="""
            #version 150
            uniform float ProbeBand;
            uniform vec4 ProbeColor;
            out vec2 trailCoord;
            out vec4 vertexColor;
            out vec3 viewPosition;
            void main(){
                vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);
                float u=(ProbeBand==32.0||ProbeBand==4.0)?p.x:47.25+p.x*15.25;
                float v=(ProbeBand==32.0||ProbeBand==4.0)?p.y:.02+p.y*.96;
                trailCoord=vec2(u,ProbeBand+v);
                vertexColor=ProbeColor;
                viewPosition=vec3(0.0,0.0,-8.0);
                gl_Position=vec4(p*2.0-1.0,0.0,1.0);
            }
            """;

    private static byte[] resource(Minecraft mc,String path)throws Exception{
        try(var stream=mc.getResourceManager().getResource(new ResourceLocation("wuxingxiantu",path)).orElseThrow().open()){
            return stream.readAllBytes();
        }
    }
    private static String shaderSource(Minecraft mc,String name)throws Exception{
        return new String(resource(mc,"shaders/core/"+name+".fsh"),StandardCharsets.UTF_8);
    }
    private static void blendJson(Minecraft mc,String name)throws Exception{
        var blend=JsonParser.parseString(new String(resource(mc,"shaders/core/"+name+".json"),StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("blend");
        for(var entry:Map.of("func","add","srcrgb","1","dstrgb","1-srcalpha","srcalpha","1","dstalpha","1-srcalpha").entrySet())
            if(!blend.has(entry.getKey())||!entry.getValue().equals(blend.get(entry.getKey()).getAsString()))
                throw new AssertionError(name+" shipping blend differs at "+entry.getKey()+": "+blend);
    }
    private static int compile(int type,String source){
        int id=GL33.glCreateShader(type);GL33.glShaderSource(id,source);GL33.glCompileShader(id);
        if(GL33.glGetShaderi(id,GL33.GL_COMPILE_STATUS)==0){String log=GL33.glGetShaderInfoLog(id);GL33.glDeleteShader(id);throw new AssertionError(log);}
        return id;
    }
    private static int program(String fragment){
        int vertex=0,pixel=0,program=0;boolean linked=false;
        try{
            vertex=compile(GL33.GL_VERTEX_SHADER,VERTEX);pixel=compile(GL33.GL_FRAGMENT_SHADER,fragment);
            program=GL33.glCreateProgram();GL33.glAttachShader(program,vertex);GL33.glAttachShader(program,pixel);GL33.glLinkProgram(program);
            if(GL33.glGetProgrami(program,GL33.GL_LINK_STATUS)==0)throw new AssertionError(GL33.glGetProgramInfoLog(program));
            linked=true;return program;
        }finally{
            if(vertex!=0)GL33.glDeleteShader(vertex);if(pixel!=0)GL33.glDeleteShader(pixel);if(!linked&&program!=0)GL33.glDeleteProgram(program);
        }
    }
    private static int uniform(int program,String name){return GL33.glGetUniformLocation(program,name);}
    private static float[] render(int program,int band,boolean blend,boolean legacy,float emission){
        GL33.glUseProgram(program);
        GL33.glUniform1f(uniform(program,"ProbeBand"),band);
        if(band==14||band==30)GL33.glUniform4f(uniform(program,"ProbeColor"),.018F,.005F,.011F,.8F);
        else GL33.glUniform4f(uniform(program,"ProbeColor"),.35F,.65F,.95F,.8F);
        GL33.glUniform1f(uniform(program,"EffectTime"),7.125F);
        GL33.glUniform1f(uniform(program,"EmissionPass"),emission);
        GL33.glUniform2f(uniform(program,"OutputSize"),W,H);
        GL33.glUniform1i(uniform(program,"SmokeAtlas"),0);
        GL33.glUniform1i(uniform(program,"Sampler0"),1);
        GL33.glClearColor(SKY[0],SKY[1],SKY[2],SKY[3]);GL33.glClear(GL33.GL_COLOR_BUFFER_BIT);
        if(blend){
            GL33.glEnable(GL33.GL_BLEND);
            GL33.glBlendFuncSeparate(legacy?GL33.GL_SRC_ALPHA:GL33.GL_ONE,GL33.GL_ONE_MINUS_SRC_ALPHA,GL33.GL_ONE,GL33.GL_ONE_MINUS_SRC_ALPHA);
            GL33.glBlendEquationSeparate(GL33.GL_FUNC_ADD,GL33.GL_FUNC_ADD);
        }else GL33.glDisable(GL33.GL_BLEND);
        GL33.glDrawArrays(GL33.GL_TRIANGLES,0,3);
        float[] pixels=new float[W*H*4];GL33.glReadPixels(0,0,W,H,GL33.GL_RGBA,GL33.GL_FLOAT,pixels);
        for(float value:pixels)if(!Float.isFinite(value))throw new AssertionError("Nonfinite shipping main output band "+band);
        return pixels;
    }
    private static double difference(float[] a,float[] b){double error=0;for(int i=0;i<a.length;i++)error=Math.max(error,Math.abs(a[i]-b[i]));return error;}
    private static double minimumSkyDelta(float[] pixels){double min=Double.POSITIVE_INFINITY;for(int i=0;i<pixels.length;i++)if((i&3)<3)min=Math.min(min,pixels[i]-SKY[i&3]);return min;}
    private static double maximumSkyDelta(float[] pixels){double max=Double.NEGATIVE_INFINITY;for(int i=0;i<pixels.length;i++)if((i&3)<3)max=Math.max(max,pixels[i]-SKY[i&3]);return max;}
    private static double emissionGain(int band){return switch(band){
        case 14,30->0;case 12->.14;case 24->.12;case 26,22->.22;case 28->.18;case 34->.30;case 32->.60;case 18->.08;case 16->.40;case 8->.52;case 6->.62;default->1;
    };}

    static Map<String,Object> verify(Minecraft mc)throws Exception{
        blendJson(mc,"sword_flight");blendJson(mc,"sword_flight_post");
        String nativeSource=shaderSource(mc,"sword_flight"),postSource=shaderSource(mc,"sword_flight_post");
        byte[] atlasBytes=resource(mc,"textures/effect/huahong/smoke-atlas.png");
        int oldProgram=GL33.glGetInteger(GL33.GL_CURRENT_PROGRAM),oldVao=GL33.glGetInteger(GL33.GL_VERTEX_ARRAY_BINDING);
        int oldDraw=GL33.glGetInteger(GL33.GL_DRAW_FRAMEBUFFER_BINDING),oldRead=GL33.glGetInteger(GL33.GL_READ_FRAMEBUFFER_BINDING);
        int oldActive=GL33.glGetInteger(GL33.GL_ACTIVE_TEXTURE);
        int[] oldTextures=new int[2];for(int i=0;i<2;i++){GL33.glActiveTexture(GL33.GL_TEXTURE0+i);oldTextures[i]=GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);}
        GL33.glActiveTexture(oldActive);
        int[] viewport=new int[4];GL33.glGetIntegerv(GL33.GL_VIEWPORT,viewport);
        float[] clear=new float[4];GL33.glGetFloatv(GL33.GL_COLOR_CLEAR_VALUE,clear);
        ByteBuffer colorMask=MemoryUtil.memAlloc(4);GL33.glGetBooleanv(GL33.GL_COLOR_WRITEMASK,colorMask);
        boolean depthWrite=GL33.glGetBoolean(GL33.GL_DEPTH_WRITEMASK);
        int[] blendFactors={GL33.glGetInteger(GL33.GL_BLEND_SRC_RGB),GL33.glGetInteger(GL33.GL_BLEND_DST_RGB),GL33.glGetInteger(GL33.GL_BLEND_SRC_ALPHA),GL33.glGetInteger(GL33.GL_BLEND_DST_ALPHA)};
        int rgbEquation=GL33.glGetInteger(GL33.GL_BLEND_EQUATION_RGB),alphaEquation=GL33.glGetInteger(GL33.GL_BLEND_EQUATION_ALPHA);
        int[] capabilities={GL33.GL_BLEND,GL33.GL_DEPTH_TEST,GL33.GL_CULL_FACE,GL33.GL_SCISSOR_TEST,GL33.GL_FRAMEBUFFER_SRGB,GL33.GL_DITHER,GL33.GL_COLOR_LOGIC_OP,GL33.GL_RASTERIZER_DISCARD};
        boolean[] enabled=new boolean[capabilities.length];for(int i=0;i<capabilities.length;i++){enabled[i]=GL33.glIsEnabled(capabilities[i]);GL33.glDisable(capabilities[i]);}
        List<Integer> programs=new ArrayList<>();int vao=0,target=0,fbo=0,atlas=0,depth=0;
        try{
            GL33.glColorMask(true,true,true,true);GL33.glDepthMask(false);
            int nativeProgram=program(nativeSource);programs.add(nativeProgram);
            int postProgram=program(postSource);programs.add(postProgram);
            // The historical ordinary-sword main differs only in straight RGB
            // output. Render that full main using its actual old blend equation.
            String premult="fragColor=vec4(material.rgb*material.a,luminous?0.0:material.a);";
            if(!nativeSource.contains(premult))throw new AssertionError("Cannot identify ordinary sword straight-output control");
            int ordinaryControl=program(nativeSource.replace(premult,"fragColor=material;"));programs.add(ordinaryControl);
            vao=GL33.glGenVertexArrays();GL33.glBindVertexArray(vao);
            GL33.glActiveTexture(GL33.GL_TEXTURE0);
            target=GL33.glGenTextures();GL33.glBindTexture(GL33.GL_TEXTURE_2D,target);
            GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_RGBA32F,W,H,0,GL33.GL_RGBA,GL33.GL_FLOAT,0L);
            fbo=GL33.glGenFramebuffers();GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER,fbo);
            GL33.glFramebufferTexture2D(GL33.GL_FRAMEBUFFER,GL33.GL_COLOR_ATTACHMENT0,GL33.GL_TEXTURE_2D,target,0);
            if(GL33.glCheckFramebufferStatus(GL33.GL_FRAMEBUFFER)!=GL33.GL_FRAMEBUFFER_COMPLETE)throw new AssertionError("Blend fixture FBO incomplete");
            GL33.glViewport(0,0,W,H);
            atlas=GL33.glGenTextures();GL33.glBindTexture(GL33.GL_TEXTURE_2D,atlas);
            try(var image=NativeImage.read(new java.io.ByteArrayInputStream(atlasBytes))){
                if(image.getWidth()!=2048||image.getHeight()!=1024)throw new AssertionError("Unexpected shipping smoke atlas dimensions "+image.getWidth()+"x"+image.getHeight());
                ByteBuffer pixels=MemoryUtil.memAlloc(image.getWidth()*image.getHeight()*4);
                try{
                    for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
                        int rgba=image.getPixelRGBA(x,y);pixels.put((byte)rgba).put((byte)(rgba>>>8)).put((byte)(rgba>>>16)).put((byte)(rgba>>>24));
                    }
                    pixels.flip();GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_RGBA8,image.getWidth(),image.getHeight(),0,GL33.GL_RGBA,GL33.GL_UNSIGNED_BYTE,pixels);
                }finally{MemoryUtil.memFree(pixels);}
            }
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MIN_FILTER,GL33.GL_LINEAR);GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MAG_FILTER,GL33.GL_LINEAR);
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_WRAP_S,GL33.GL_CLAMP_TO_EDGE);GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_WRAP_T,GL33.GL_CLAMP_TO_EDGE);
            GL33.glActiveTexture(GL33.GL_TEXTURE1);depth=GL33.glGenTextures();GL33.glBindTexture(GL33.GL_TEXTURE_2D,depth);
            GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_R32F,1,1,0,GL33.GL_RED,GL33.GL_FLOAT,new float[]{1});
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MIN_FILTER,GL33.GL_NEAREST);GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MAG_FILTER,GL33.GL_NEAREST);

            int checks=2;double minDelta=Double.POSITIVE_INFINITY,nativePostError=0,ordinaryError=0,emissionError=0;
            Map<String,Object> byBand=new LinkedHashMap<>();
            for(int band:LIGHT){
                float[] nativePixels=render(nativeProgram,band,true,false,0),postPixels=render(postProgram,band,true,false,0);
                double min=Math.min(minimumSkyDelta(nativePixels),minimumSkyDelta(postPixels));
                double signal=Math.min(maximumSkyDelta(nativePixels),maximumSkyDelta(postPixels));
                if(min<-.000002||signal<.0001)throw new AssertionError("Shipping luminous band "+band+" darkened sky or has no signal: "+min+" / "+signal);
                minDelta=Math.min(minDelta,min);nativePostError=Math.max(nativePostError,difference(nativePixels,postPixels));checks+=3;
                byBand.put(Integer.toString(band),Map.of("minDelta",min,"signal",signal));
            }
            for(int band:new int[]{14,30}){
                float[] a=render(nativeProgram,band,true,false,0),b=render(postProgram,band,true,false,0);
                double absorption=Math.min(minimumSkyDelta(a),minimumSkyDelta(b));
                if(absorption>-.005)throw new AssertionError("Shipping smoke band "+band+" has no sky absorption: "+absorption);
                nativePostError=Math.max(nativePostError,difference(a,b));checks+=2;
                byBand.put(Integer.toString(band),Map.of("minDelta",absorption,"absorption",true));
            }
            for(int band:new int[]{0,2,4}){
                float[] current=render(nativeProgram,band,true,false,0),old=render(ordinaryControl,band,true,true,0),post=render(postProgram,band,true,false,0);
                ordinaryError=Math.max(ordinaryError,difference(current,old));nativePostError=Math.max(nativePostError,difference(current,post));checks+=2;
            }
            for(int band:new int[]{6,8,10,12,14,16,18,20,22,24,26,28,30,32,34}){
                float[] current=render(nativeProgram,band,false,false,0),emission=render(postProgram,band,false,false,1);
                for(int i=0;i<current.length;i++)if((i&3)<3){
                    // Discarded pixels keep the clear sky in both captures.
                    if(current[i]==SKY[i&3]&&emission[i]==SKY[i&3])continue;
                    emissionError=Math.max(emissionError,Math.abs(emission[i]-current[i]*emissionGain(band)));
                }
                checks++;
            }
            if(nativePostError>2e-6||ordinaryError>2e-6||emissionError>2e-6)throw new AssertionError("Shipping main mismatch native/post="+nativePostError+", ordinary="+ordinaryError+", emission="+emissionError);
            int error=GL33.glGetError();if(error!=0)throw new AssertionError("Blend fixture GL error "+error);
            Map<String,Object> result=new LinkedHashMap<>();result.put("check","shipping-full-main-sky-blend");result.put("checks",checks);result.put("minDelta",minDelta);
            result.put("nativePostError",nativePostError);result.put("ordinaryAlphaAppearanceError",ordinaryError);result.put("emissionSingleAlphaError",emissionError);
            result.put("atlasSHA",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(atlasBytes)));result.put("bands",byBand);result.put("passed",true);return result;
        }finally{
            GL33.glBindFramebuffer(GL33.GL_DRAW_FRAMEBUFFER,oldDraw);GL33.glBindFramebuffer(GL33.GL_READ_FRAMEBUFFER,oldRead);
            for(int i=0;i<2;i++){GL33.glActiveTexture(GL33.GL_TEXTURE0+i);GL33.glBindTexture(GL33.GL_TEXTURE_2D,oldTextures[i]);}
            GL33.glActiveTexture(oldActive);GL33.glBindVertexArray(oldVao);GL33.glUseProgram(oldProgram);GL33.glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            GL33.glBlendFuncSeparate(blendFactors[0],blendFactors[1],blendFactors[2],blendFactors[3]);GL33.glBlendEquationSeparate(rgbEquation,alphaEquation);
            GL33.glColorMask(colorMask.get(0)!=0,colorMask.get(1)!=0,colorMask.get(2)!=0,colorMask.get(3)!=0);GL33.glDepthMask(depthWrite);GL33.glClearColor(clear[0],clear[1],clear[2],clear[3]);
            for(int i=0;i<capabilities.length;i++)if(enabled[i])GL33.glEnable(capabilities[i]);else GL33.glDisable(capabilities[i]);
            MemoryUtil.memFree(colorMask);if(fbo!=0)GL33.glDeleteFramebuffers(fbo);for(int id:new int[]{target,atlas,depth})if(id!=0)GL33.glDeleteTextures(id);if(vao!=0)GL33.glDeleteVertexArrays(vao);for(int id:programs)GL33.glDeleteProgram(id);
        }
    }
    private HuahongBlendQa(){}
}
