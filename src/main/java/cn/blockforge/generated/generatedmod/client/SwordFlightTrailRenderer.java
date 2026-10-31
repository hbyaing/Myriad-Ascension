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
    private static abstract class Types extends RenderType {
        private Types(){super("unused",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
        static final TransparencyStateShard LIGHT_AND_SMOKE=new TransparencyStateShard("huahong_light_and_smoke",
                ()->{RenderSystem.enableBlend();RenderSystem.blendFuncSeparate(org.lwjgl.opengl.GL11.GL_ONE,org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA,
                        org.lwjgl.opengl.GL11.GL_ONE,org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);},
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
        int depth=TrailShaderBridge.capturedDepthTexture();
        if(mesh==null)return false;
        if(post==null||depth<0){mesh.release();status="Awaiting sword world depth";return false;}
        try(var saved=new MeteorBloomRenderer.State()){
            var main=Minecraft.getInstance().getMainRenderTarget();main.bindWrite(true);
            RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.disableScissor();
            RenderSystem.enableBlend();RenderSystem.blendFuncSeparate(org.lwjgl.opengl.GL11.GL_ONE,org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA,
                    org.lwjgl.opengl.GL11.GL_ONE,org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);RenderSystem.colorMask(true,true,true,false);
            RenderSystem.setShaderColor(1,1,1,1);
            post.safeGetUniform("TrailProjection").set(projection);
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
