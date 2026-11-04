package cn.blockforge.generated.generatedmod.client.realmflight;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.client.TrailShaderBridge;
import cn.blockforge.generated.generatedmod.client.SwordFlightTrailRenderer;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.BlendMode;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import java.io.IOException;

/** Only realm flight geometry enters this floating-point emission buffer. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class MeteorBloomRenderer {
    private static ShaderInstance blur,composite;
    private static TextureTarget emission,ping,pong;
    private static VertexBuffer geometry;
    private static int failures;
    public static int draws;
    public static String status="Not drawn";
    @SubscribeEvent public static void register(RegisterShadersEvent event)throws IOException{
        release();
        event.registerShader(new ShaderInstance(event.getResourceProvider(),new ResourceLocation(GeneratedMod.MOD_ID,"energy_blur"),DefaultVertexFormat.POSITION_TEX),s->blur=s);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),new ResourceLocation(GeneratedMod.MOD_ID,"realm_meteor_bloom"),DefaultVertexFormat.POSITION_TEX),s->composite=s);
    }
    public static void release(){
        for(var target:new TextureTarget[]{emission,ping,pong})if(target!=null)target.destroyBuffers();
        emission=ping=pong=null;
        if(geometry!=null){geometry.close();geometry=null;}
    }
    @Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
    public static final class Lifecycle {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
            if(event.phase==TickEvent.Phase.END&&Minecraft.getInstance().level==null){
                if(emission!=null)release();MeteorHeadRenderer.release();
            }
        }
    }
    private static TextureTarget target(int width,int height){
        var target=new TextureTarget(Math.max(1,width),Math.max(1,height),false,Minecraft.ON_OSX);
        RenderSystem.bindTexture(target.getColorTextureId());
        // Alpha is linear view depth metadata; an 8-bit attachment is not sufficient.
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA32F,target.width,target.height,0,GL11.GL_RGBA,GL11.GL_FLOAT,(java.nio.ByteBuffer)null);
        target.setFilterMode(GL11.GL_LINEAR);target.setClearColor(0,0,0,1000000F);
        return target;
    }
    private static void clear(TextureTarget target){
        RenderSystem.disableScissor();RenderSystem.colorMask(true,true,true,true);
        target.clear(Minecraft.ON_OSX);target.bindWrite(true);
    }
    public static void draw(BufferBuilder.RenderedBuffer mesh,Matrix4f projection,Matrix4f view,Matrix4f inverseProjection,java.util.List<MeteorHeadRenderer.Head> heads){
        draw(mesh,projection,view,inverseProjection,heads,false);
    }
    public static void drawSword(BufferBuilder.RenderedBuffer mesh,Matrix4f projection,Matrix4f view,Matrix4f inverseProjection){
        draw(mesh,projection,view,inverseProjection,java.util.List.of(),true);
    }
    private static void draw(BufferBuilder.RenderedBuffer mesh,Matrix4f projection,Matrix4f view,Matrix4f inverseProjection,java.util.List<MeteorHeadRenderer.Head> heads,boolean sword){
        int sceneDepth=TrailShaderBridge.capturedDepthTexture();
        if(blur==null||composite==null||sceneDepth<0){if(mesh!=null)mesh.release();status="Waiting for shaders or world depth";return;}
        boolean uploaded=false;
        try(State saved=new State()){
            if(sword)SwordFlightTrailRenderer.clearPolygonOffset();
            var main=Minecraft.getInstance().getMainRenderTarget();int w=Math.max(1,main.viewWidth/2),h=Math.max(1,main.viewHeight/2);
            if(emission==null||emission.width!=w||emission.height!=h){
                release();emission=target(w,h);ping=target(w,h);pong=target(w,h);
                geometry=new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            }
            clear(emission);
            RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();
            var shader=sword?SwordFlightTrailRenderer.emissionShader():TrailShaderBridge.meteorPostShader();
            shader.safeGetUniform("TrailProjection").set(projection);
            if(sword){shader.safeGetUniform("InverseDepthProjection").set(inverseProjection);shader.safeGetUniform("DepthSoftness").set(.08F);shader.safeGetUniform("OpticalPass").set(0F);}
            shader.safeGetUniform("TrailView").set(view);
            shader.safeGetUniform("EffectTime").set(sword?cn.blockforge.generated.generatedmod.client.FlyingSwordEffects.capturedEffectTime():
                    (Minecraft.getInstance().level.getGameTime()+Minecraft.getInstance().getFrameTime())/20F);
            shader.safeGetUniform("OutputSize").set((float)w,(float)h);
            shader.safeGetUniform("EmissionPass").set(1F);shader.setSampler("Sampler0",sceneDepth);
            if(mesh!=null){geometry.bind();geometry.upload(mesh);uploaded=true;shader.apply();
            // Add selected radiance; retain the nearest emitting surface in the alpha metadata.
            RenderSystem.enableBlend();RenderSystem.blendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE,GL11.GL_ONE,GL11.GL_ONE);
            GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD,GL14.GL_MIN);
            geometry.draw();shader.clear();VertexBuffer.unbind();BufferUploader.invalidate();}
            shader.safeGetUniform("EmissionPass").set(0F);
            GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD,GL14.GL_FUNC_ADD);
            MeteorHeadRenderer.drawEmission(heads,w,h,inverseProjection);
            blur.safeGetUniform("InverseProjection").set(inverseProjection);
            blur.safeGetUniform("OutputSize").set((float)w,(float)h);
            int source=emission.getColorTextureId();
            for(int pass=0;pass<2;pass++){
                clear(ping);blur.safeGetUniform("Direction").set(1.55F,0F);fullscreen(blur,source,sceneDepth,false);
                clear(pong);blur.safeGetUniform("Direction").set(0F,1.55F);fullscreen(blur,ping.getColorTextureId(),sceneDepth,false);
                source=pong.getColorTextureId();
            }
            main.bindWrite(true);
            composite.safeGetUniform("InverseProjection").set(inverseProjection);
            composite.safeGetUniform("OutputSize").set((float)main.viewWidth,(float)main.viewHeight);
            composite.safeGetUniform("Intensity").set(sword?.85F:.62F);
            RenderSystem.colorMask(true,true,true,false);
            fullscreen(composite,source,sceneDepth,true);
            draws++;status="Depth constrained floating-point bloom "+w+"x"+h;
        }catch(RuntimeException failure){
            if(!uploaded&&mesh!=null)mesh.release();
            TrailShaderBridge.meteorPostShader().safeGetUniform("EmissionPass").set(0F);
            if(sword&&SwordFlightTrailRenderer.emissionShader()!=null)SwordFlightTrailRenderer.emissionShader().safeGetUniform("EmissionPass").set(0F);
            status="Bloom failed: "+failure.getMessage();
            if(failures++<3)com.mojang.logging.LogUtils.getLogger().warn("Realm meteor bloom",failure);
        }
    }
    private static void fullscreen(ShaderInstance shader,int source,int depth,boolean additive){
        RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();
        if(additive){RenderSystem.enableBlend();RenderSystem.blendFunc(GL11.GL_ONE,GL11.GL_ONE);}else RenderSystem.disableBlend();
        RenderSystem.setShader(()->shader);RenderSystem.setShaderTexture(0,source);RenderSystem.setShaderTexture(1,depth);
        var buffer=Tesselator.getInstance().getBuilder();buffer.begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(-1,-1,0).uv(0,0).endVertex();buffer.vertex(3,-1,0).uv(2,0).endVertex();buffer.vertex(-1,3,0).uv(0,2).endVertex();
        BufferUploader.drawWithShader(buffer.end());
    }
    public static final class State implements AutoCloseable {
        final int draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        final int program=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        final int[] viewport=new int[4],bound=new int[3],shaderTextures=new int[3];
        final int[] scissorBox=new int[4];
        final boolean[] mask=cn.blockforge.generated.generatedmod.client.NativeGlState.colorMask();
        final float[] color=RenderSystem.getShaderColor().clone();
        final float[] clearColor=new float[4];
        final boolean polygonOffset=GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
        final boolean stencil=GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        final boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),write=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),blend=GL11.glIsEnabled(GL11.GL_BLEND),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        final int srcRGB=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dstRGB=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        final int rgbEquation=GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB),alphaEquation=GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        final ShaderInstance previous=RenderSystem.getShader();
        final BlendMode previousBlend=BlendMode.lastApplied;
        public State(){
            GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
            GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE,clearColor);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX,scissorBox);
            for(int i=0;i<3;i++){shaderTextures[i]=RenderSystem.getShaderTexture(i);GlStateManager._activeTexture(GL13.GL_TEXTURE0+i);bound[i]=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);}
            // Scratch target allocation must not replace an unsaved lightmap
            // binding when the caller left texture unit 2 (or higher) active.
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        }
        public void close(){
            VertexBuffer.unbind();BufferUploader.invalidate();
            // Also invalidate ShaderInstance's program cache if an owned draw
            // threw before clear(). This shader touches only the saved units 0/1.
            if(composite!=null)composite.clear();
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);
            RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            for(int i=0;i<3;i++){RenderSystem.setShaderTexture(i,shaderTextures[i]);GlStateManager._activeTexture(GL13.GL_TEXTURE0+i);GlStateManager._bindTexture(bound[i]);}
            GlStateManager._activeTexture(active);RenderSystem.setShader(()->previous);RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);
            if(polygonOffset){RenderSystem.enablePolygonOffset();GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);}else{RenderSystem.disablePolygonOffset();GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);}
            if(stencil)GL11.glEnable(GL11.GL_STENCIL_TEST);else GL11.glDisable(GL11.GL_STENCIL_TEST);
            RenderSystem.depthMask(write);if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
            if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(srcRGB,dstRGB,srcAlpha,dstAlpha);GL20.glBlendEquationSeparate(rgbEquation,alphaEquation);
            if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
            RenderSystem.enableScissor(scissorBox[0],scissorBox[1],scissorBox[2],scissorBox[3]);
            if(!scissor)RenderSystem.disableScissor();
            RenderSystem.colorMask(mask[0],mask[1],mask[2],mask[3]);
            RenderSystem.clearColor(clearColor[0],clearColor[1],clearColor[2],clearColor[3]);
            GlStateManager._glUseProgram(program);
            // GL restoration alone is insufficient: a stale transparent shader
            // cache makes the HUD shader overwrite vignette's special factors.
            BlendMode.lastApplied=previousBlend;
        }
    }
    private MeteorBloomRenderer(){}
}
