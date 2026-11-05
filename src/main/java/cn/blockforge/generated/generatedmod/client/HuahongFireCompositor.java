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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import java.io.IOException;

/** Fire owns its HDR transfer. Other flight forms retain their optical composite. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class HuahongFireCompositor {
    private static ShaderInstance accumulation,ordinaryComposite,fireComposite;
    private static TextureTarget ordinary,fire;
    private static VertexBuffer geometry;
    @SubscribeEvent public static void register(RegisterShadersEvent event)throws IOException{
        release();
        event.registerShader(new ShaderInstance(event.getResourceProvider(),id("huahong_fire_accum"),DefaultVertexFormat.POSITION_TEX_COLOR),s->accumulation=s);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),id("huahong_smoke_composite"),DefaultVertexFormat.POSITION_TEX),s->ordinaryComposite=s);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),id("huahong_fire_composite"),DefaultVertexFormat.POSITION_TEX),s->fireComposite=s);
    }
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID,path);}
    public static void release(){
        if(ordinary!=null)ordinary.destroyBuffers();if(fire!=null)fire.destroyBuffers();ordinary=fire=null;
        if(geometry!=null){geometry.close();geometry=null;}
    }
    private static TextureTarget target(int w,int h){
        var t=new TextureTarget(w,h,false,Minecraft.ON_OSX);RenderSystem.bindTexture(t.getColorTextureId());
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA32F,t.width,t.height,0,GL11.GL_RGBA,GL11.GL_FLOAT,(java.nio.ByteBuffer)null);
        t.setFilterMode(GL11.GL_LINEAR);t.setClearColor(0,0,0,0);return t;
    }
    public static boolean draw(BufferBuilder.RenderedBuffer mesh,Matrix4f projection,Matrix4f inverseDepth,float time){
        if(mesh==null)return false;
        int depth=TrailShaderBridge.capturedDepthTexture();
        if(accumulation==null||ordinaryComposite==null||fireComposite==null||depth<0){mesh.release();return false;}
        try(var saved=new MeteorBloomRenderer.State()){
            var mc=Minecraft.getInstance();var main=mc.getMainRenderTarget();
            if(ordinary==null||ordinary.viewWidth!=main.viewWidth||ordinary.viewHeight!=main.viewHeight){release();ordinary=target(main.viewWidth,main.viewHeight);fire=target(main.viewWidth,main.viewHeight);}
            if(geometry==null)geometry=new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            geometry.bind();geometry.upload(mesh);VertexBuffer.unbind();
            SwordFlightTrailRenderer.clearPolygonOffset();RenderSystem.disableScissor();RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();
            GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(false);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glDisable(GL11.GL_STENCIL_TEST);
            RenderSystem.setShaderColor(1,1,1,1);
            for(int pass=1;pass<=2;pass++){
                var output=pass==1?ordinary:fire;RenderSystem.colorMask(true,true,true,true);GL11.glColorMask(true,true,true,true);
                output.clear(Minecraft.ON_OSX);output.bindWrite(true);
                RenderSystem.enableBlend();RenderSystem.blendFunc(GL11.GL_ONE,GL11.GL_ONE);GL11.glEnable(GL11.GL_BLEND);GL14.glBlendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE,GL11.GL_ONE,GL11.GL_ONE);GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD,GL14.GL_FUNC_ADD);
                accumulation.safeGetUniform("FirePass").set((float)pass);accumulation.safeGetUniform("TrailProjection").set(projection);
                accumulation.safeGetUniform("InverseDepthProjection").set(inverseDepth);accumulation.safeGetUniform("DepthSoftness").set(.08F);
                accumulation.safeGetUniform("OutputSize").set((float)main.viewWidth,(float)main.viewHeight);accumulation.safeGetUniform("EffectTime").set(time);
                accumulation.safeGetUniform("EmissionPass").set(0F);accumulation.safeGetUniform("OpticalPass").set(1F);
                var atlas=mc.getTextureManager().getTexture(id("textures/effect/huahong/smoke-atlas.png"));atlas.setFilter(true,false);accumulation.setSampler("SmokeAtlas",atlas.getId());
                RenderSystem.setShaderTexture(0,depth);geometry.bind();geometry.drawWithShader(new Matrix4f(),new Matrix4f(),accumulation);VertexBuffer.unbind();
            }
            main.bindWrite(true);RenderSystem.colorMask(true,true,true,false);GL11.glColorMask(true,true,true,false);
            RenderSystem.blendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);GL14.glBlendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
            for(int pass=1;pass<=2;pass++){
                var shader=pass==1?ordinaryComposite:fireComposite;
                RenderSystem.setShaderTexture(0,(pass==1?ordinary:fire).getColorTextureId());RenderSystem.setShader(()->shader);
                var b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_TEX);
                b.vertex(-1,-1,0).uv(0,0).endVertex();b.vertex(3,-1,0).uv(2,0).endVertex();b.vertex(-1,3,0).uv(0,2).endVertex();
                try{BufferUploader.drawWithShader(b.end());}finally{shader.clear();}
            }
            return true;
        }
    }
    @Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
    public static final class Lifecycle {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase==TickEvent.Phase.END&&Minecraft.getInstance().level==null&&ordinary!=null)release();}
    }
    private HuahongFireCompositor(){}
}
