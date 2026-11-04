package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.client.realmflight.MeteorBloomRenderer;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import java.io.IOException;

/** Integrate overlapping smoke as a homogeneous optical mixture, without sorting cards.
 * This removes order exchanges; it does not claim exact depth-ordered volume transport. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class HuahongSmokeCompositor {
    private static ShaderInstance accumulation,composite;
    private static TextureTarget target;
    @SubscribeEvent public static void register(RegisterShadersEvent event)throws IOException{
        release();
        event.registerShader(new ShaderInstance(event.getResourceProvider(),new ResourceLocation(GeneratedMod.MOD_ID,"huahong_smoke_accum"),DefaultVertexFormat.POSITION_TEX_COLOR),s->accumulation=s);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),new ResourceLocation(GeneratedMod.MOD_ID,"huahong_smoke_composite"),DefaultVertexFormat.POSITION_TEX),s->composite=s);
    }
    public static void release(){if(target!=null){target.destroyBuffers();target=null;}}
    public static boolean ready(){return accumulation!=null&&composite!=null;}
    public static boolean draw(BufferBuilder.RenderedBuffer mesh,Matrix4f projection,Matrix4f inverseDepth,float time){
        int depth=TrailShaderBridge.capturedDepthTexture();
        if(mesh==null)return false;
        if(!ready()||depth<0){mesh.release();return false;}
        try(var saved=new MeteorBloomRenderer.State()){
            var mc=Minecraft.getInstance();var main=mc.getMainRenderTarget();
            if(target==null||target.viewWidth!=main.viewWidth||target.viewHeight!=main.viewHeight){
                release();target=new TextureTarget(main.viewWidth,main.viewHeight,false,Minecraft.ON_OSX);
                RenderSystem.bindTexture(target.getColorTextureId());
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA32F,target.width,target.height,0,GL11.GL_RGBA,GL11.GL_FLOAT,(java.nio.ByteBuffer)null);
                target.setFilterMode(GL11.GL_NEAREST);target.setClearColor(0,0,0,0);
            }
            SwordFlightTrailRenderer.clearPolygonOffset();RenderSystem.disableScissor();RenderSystem.colorMask(true,true,true,true);
            target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();
            GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(false);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glDisable(GL11.GL_STENCIL_TEST);
            GL11.glColorMask(true,true,true,true);RenderSystem.setShaderColor(1,1,1,1);
            RenderSystem.enableBlend();RenderSystem.blendFunc(GL11.GL_ONE,GL11.GL_ONE);GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD,GL14.GL_FUNC_ADD);
            GL11.glEnable(GL11.GL_BLEND);GL14.glBlendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE,GL11.GL_ONE,GL11.GL_ONE);
            accumulation.safeGetUniform("TrailProjection").set(projection);
            accumulation.safeGetUniform("InverseDepthProjection").set(inverseDepth);
            accumulation.safeGetUniform("DepthSoftness").set(.08F);
            accumulation.safeGetUniform("OutputSize").set((float)main.viewWidth,(float)main.viewHeight);
            accumulation.safeGetUniform("EffectTime").set(time);accumulation.safeGetUniform("EmissionPass").set(0F);accumulation.safeGetUniform("OpticalPass").set(1F);
            var atlas=mc.getTextureManager().getTexture(new ResourceLocation(GeneratedMod.MOD_ID,"textures/effect/huahong/smoke-atlas.png"));
            atlas.setFilter(true,false);accumulation.setSampler("SmokeAtlas",atlas.getId());
            RenderSystem.setShaderTexture(0,depth);RenderSystem.setShader(()->accumulation);
            try{BufferUploader.drawWithShader(mesh);}finally{accumulation.clear();}
            main.bindWrite(true);RenderSystem.colorMask(true,true,true,false);GL11.glColorMask(true,true,true,false);
            RenderSystem.blendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL14.glBlendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShaderTexture(0,target.getColorTextureId());RenderSystem.setShader(()->composite);
            var b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_TEX);
            b.vertex(-1,-1,0).uv(0,0).endVertex();b.vertex(3,-1,0).uv(2,0).endVertex();b.vertex(-1,3,0).uv(0,2).endVertex();
            try{BufferUploader.drawWithShader(b.end());}finally{composite.clear();}
            return true;
        }
    }
    private HuahongSmokeCompositor(){}
}
