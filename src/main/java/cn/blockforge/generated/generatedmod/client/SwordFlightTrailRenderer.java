package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.client.realmflight.MeteorBloomRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import java.io.IOException;

/** Independent sword material: arc-length UVs, flow, and a depth-tested late path. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class SwordFlightTrailRenderer {
    private static ShaderInstance normal,post;
    private static final ResourceLocation SMOKE_ATLAS=new ResourceLocation(GeneratedMod.MOD_ID,"textures/effect/huahong/smoke-atlas.png");
    private static void smokeAtlas(ShaderInstance shader){shader.setSampler("SmokeAtlas",Minecraft.getInstance().getTextureManager().getTexture(SMOKE_ATLAS).getId());}
    public static String status="Awaiting sword shaders";
    public static int generation,normalDraws,postDraws;

    @SubscribeEvent public static void register(RegisterShadersEvent event)throws IOException{
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID,"sword_flight"),DefaultVertexFormat.POSITION_TEX_COLOR),s->normal=s);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID,"sword_flight_post"),DefaultVertexFormat.POSITION_TEX_COLOR),s->post=s);
        generation++;
    }
    private static boolean warnedPolygonOffset;
    public static void clearPolygonOffset(){
        if(!warnedPolygonOffset&&org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_POLYGON_OFFSET_FILL)){
            warnedPolygonOffset=true;
            com.mojang.logging.LogUtils.getLogger().warn("Flight VFX isolated inherited polygon depth offset (factor={}, units={})",
                    org.lwjgl.opengl.GL11.glGetFloat(org.lwjgl.opengl.GL11.GL_POLYGON_OFFSET_FACTOR),
                    org.lwjgl.opengl.GL11.glGetFloat(org.lwjgl.opengl.GL11.GL_POLYGON_OFFSET_UNITS));
        }
        RenderSystem.disablePolygonOffset();org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_POLYGON_OFFSET_FILL);
    }
    private static void lightBlend(){
        clearPolygonOffset();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(org.lwjgl.opengl.GL11.GL_ONE,org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA,
                org.lwjgl.opengl.GL11.GL_ONE,org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL14.glBlendFuncSeparate(org.lwjgl.opengl.GL11.GL_ONE,org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA,
                org.lwjgl.opengl.GL11.GL_ONE,org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
        org.lwjgl.opengl.GL20.glBlendEquationSeparate(org.lwjgl.opengl.GL14.GL_FUNC_ADD,org.lwjgl.opengl.GL14.GL_FUNC_ADD);
    }
    private static abstract class Types extends RenderType {
        private Types(){super("unused",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
        static final TransparencyStateShard LIGHT_AND_SMOKE=new TransparencyStateShard("huahong_light_and_smoke",
                SwordFlightTrailRenderer::lightBlend,
                ()->{RenderSystem.disableBlend();RenderSystem.defaultBlendFunc();});
        static final RenderType TRAIL=create("wuxing_sword_flow",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,65536,false,true,
                CompositeState.builder().setShaderState(new ShaderStateShard(()->normal))
                .setTransparencyState(LIGHT_AND_SMOKE).setCullState(NO_CULL)
                .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    }
    public static boolean ready(){return normal!=null&&post!=null;}
    public static ShaderInstance emissionShader(){if(post!=null)smokeAtlas(post);return post;}
    public static RenderType renderType(float time){
        normal.safeGetUniform("EffectTime").set(time);
        smokeAtlas(normal);
        return Types.TRAIL;
    }
    public static boolean drawPost(BufferBuilder.RenderedBuffer mesh,Matrix4f projection,float time){
        return drawPost(mesh,projection,null,time);
    }
    public static boolean drawPost(BufferBuilder.RenderedBuffer mesh,Matrix4f projection,Matrix4f inverseDepth,float time){
        int depth=TrailShaderBridge.capturedDepthTexture();
        if(mesh==null)return false;
        if(post==null||depth<0){mesh.release();status="Awaiting sword world depth";return false;}
        if(HuahongVisuals.absorbingThisFrame&&inverseDepth!=null){
            boolean submitted=HuahongSmokeCompositor.draw(mesh,projection,inverseDepth,time);
            if(submitted){postDraws++;status="Sword smoke / order-independent optical mixture";}
            return submitted;
        }
        try(var saved=new MeteorBloomRenderer.State()){
            var main=Minecraft.getInstance().getMainRenderTarget();main.bindWrite(true);
            RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.disableScissor();
            // This late draw owns its blend state. Native calls also repair a
            // wrapper cache left out of sync by an external renderer.
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
            org.lwjgl.opengl.GL11.glDepthMask(false);
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_STENCIL_TEST);
            lightBlend();RenderSystem.colorMask(true,true,true,false);
            org.lwjgl.opengl.GL11.glColorMask(true,true,true,false);
            RenderSystem.setShaderColor(1,1,1,1);
            post.safeGetUniform("EmissionPass").set(0F);
            post.safeGetUniform("OpticalPass").set(0F);
            post.safeGetUniform("TrailProjection").set(projection);
            post.safeGetUniform("DepthSoftness").set(inverseDepth==null?0F:.08F);
            if(inverseDepth!=null)post.safeGetUniform("InverseDepthProjection").set(inverseDepth);
            post.safeGetUniform("OutputSize").set((float)main.viewWidth,(float)main.viewHeight);
            post.safeGetUniform("EffectTime").set(time);
            smokeAtlas(post);
            RenderSystem.setShaderTexture(0,depth);RenderSystem.setShader(()->post);
            try{BufferUploader.drawWithShader(mesh);}finally{post.clear();}
            postDraws++;status="Sword flow / late depth shader";
            return true;
        }
    }
    private SwordFlightTrailRenderer(){}
}
