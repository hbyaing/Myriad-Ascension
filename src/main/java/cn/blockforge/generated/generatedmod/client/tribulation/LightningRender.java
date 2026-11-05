package cn.blockforge.generated.generatedmod.client.tribulation;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.resources.ResourceLocation;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

/** Bounded depth-tested emissive ribbons with smooth radial profiles. No scene lighting implied. */
@Mod.EventBusSubscriber(modid=cn.blockforge.generated.generatedmod.GeneratedMod.MOD_ID, value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.MOD)
public final class LightningRender extends RenderType {
    private LightningRender(){super("unused",DefaultVertexFormat.POSITION_COLOR_TEX,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
    private static ShaderInstance shader, vividShader;
    private static final MultiBufferSource.BufferSource BUFFER = MultiBufferSource.immediate(new BufferBuilder(262144));
    private static RenderType core, glow, vivid;
    private static RenderType lateCore,lateVivid;
    private static final TransparencyStateShard VIVID_TRANSPARENCY = new TransparencyStateShard("lightning_vivid",
        () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        }, () -> { RenderSystem.disableBlend(); RenderSystem.defaultBlendFunc(); });
    @SubscribeEvent public static void shaders(RegisterShadersEvent e)throws IOException {
        e.registerShader(new ShaderInstance(e.getResourceProvider(), new ResourceLocation(cn.blockforge.generated.generatedmod.GeneratedMod.MOD_ID,"bolt"), DefaultVertexFormat.POSITION_COLOR_TEX),
            s->{shader=s;core=null;glow=null;lateCore=null;LightningShaderBridge.release();System.out.println("[VFXWORKBENCH] lightning shader compiled");});
        e.registerShader(new ShaderInstance(e.getResourceProvider(), new ResourceLocation(cn.blockforge.generated.generatedmod.GeneratedMod.MOD_ID,"bolt_vivid"), DefaultVertexFormat.POSITION_COLOR_TEX),
            s->{vividShader=s;vivid=null;lateVivid=null;System.out.println("[VFXWORKBENCH] lightning vivid shader compiled");});
    }
    private static RenderType type(boolean halo, LightningRecipe recipe) {
        boolean useVivid=!halo && LightningAppearance.vividCore(recipe);
        RenderType out=LightningShaderBridge.late?(useVivid?lateVivid:lateCore):halo?glow:useVivid?vivid:core;
        if(out!=null)return out;
        out=RenderType.create(halo?"lightninglab_glow":useVivid?"lightninglab_vivid":"lightninglab_core",DefaultVertexFormat.POSITION_COLOR_TEX,VertexFormat.Mode.QUADS,262144,false,false,
            CompositeState.builder().setShaderState(new ShaderStateShard(()->useVivid?vividShader:shader)).setTransparencyState(useVivid?VIVID_TRANSPARENCY:ADDITIVE_TRANSPARENCY)
                .setCullState(NO_CULL).setDepthTestState(LightningShaderBridge.late?NO_DEPTH_TEST:LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        if(LightningShaderBridge.late){if(useVivid)lateVivid=out;else lateCore=out;}else if(halo)glow=out;else if(useVivid)vivid=out;else core=out;
        return out;
    }
    private static void material(LightningRecipe r, boolean halo) {
        boolean useVivid=!halo && LightningAppearance.vividCore(r);
        ShaderInstance selected=useVivid?vividShader:shader;
        LightningShaderBridge.uniforms(selected);
        selected.safeGetUniform("Tint").set(1f,1f,1f);
        selected.safeGetUniform("Gain").set((float)(halo?r.glowGain:r.coreGain));
        selected.safeGetUniform("Profile").set(halo?1f:0f);
        selected.safeGetUniform("Vivid").set(useVivid?1f:0f);
    }
    private record Node(Vec3 p,double width,double along,double alpha) {}
    private record CoverageView(Matrix4f clip,int width,int height,HaloGeometry.Projection projection) {}
    private static CoverageView coverageView(PoseStack pose) {
        try(var stack=MemoryStack.stackPush()) {
            var viewport=stack.mallocInt(4);GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
            // Matches ProjMat * ModelViewMat * the CPU vertex pose in bolt.vsh.
            Matrix4f clip=LightningShaderBridge.clip(pose.last().pose());
            int width=viewport.get(2),height=viewport.get(3);
            return new CoverageView(clip,width,height,HaloGeometry.prepare(clip,width,height));
        }
    }
    static void drawOrbit(List<OrbitField.Segment> list,LightningRecipe r,PoseStack pose,Vec3 camera) {
        drawOrbit(list,r,pose,camera,null);
    }
    static void drawOrbit(List<OrbitField.Segment> list,LightningRecipe r,PoseStack pose,Vec3 camera,List<Runnable> deferredCores) {
        if(shader==null||vividShader==null||list.isEmpty())return;
        CoverageView view=coverageView(pose);
        Map<Long,List<Node>> paths=new LinkedHashMap<>();
        for(OrbitField.Segment seg:list) {
            var nodes=paths.computeIfAbsent((long)seg.strand(),key->new ArrayList<>());
            if(nodes.isEmpty())nodes.add(new Node(seg.a(),seg.width(),seg.alongA(),seg.alpha()));
            if(seg.a().distanceToSqr(seg.b())>1e-12)nodes.add(new Node(seg.b(),seg.width(),seg.alongB(),seg.alpha()));
        }
        double baseWidth=r.mode.equals("orbit")?r.currentWidth:r.strikeWidth;
        drawLayers(paths.values(),r,baseWidth,pose,camera,view,deferredCores);
    }
    /** Wide soft sky illumination shares the stroke halo's depth and shader-pack path. */
    static void drawSkyGlow(Vec3 start,Vec3 end,double brightness,PoseStack pose,Vec3 camera) {
        if(brightness<=.001)return;
        var r=new LightningRecipe();r.gradientEnabled=false;r.glowColor="#D3E1FF";r.glowGain=.9;
        CoverageView view=coverageView(pose);
        double radius=Math.max(28,Math.min(48,start.distanceTo(end)*.35));
        var path=List.of(new HaloGeometry.Point(start.lerp(end,.1).subtract(camera),radius,0,brightness*.32),
                new HaloGeometry.Point(start.lerp(end,.9).subtract(camera),radius,1,brightness*.32));
        LightningHalo.draw(List.of(path),r,view.projection(),pose.last().pose(),view.width(),view.height());
    }
    static void drawBolt(BoltGeometry bolt,LightningRecipe r,double ticks,PoseStack pose,Vec3 camera) {
        drawBolt(bolt,r,ticks,pose,camera,null);
    }
    static void drawBolt(BoltGeometry bolt,LightningRecipe r,double ticks,PoseStack pose,Vec3 camera,List<Runnable> deferredCores) {
        if(shader==null||vividShader==null)return;
        LightningEnvelope env=new LightningEnvelope(r);
        double power=env.brightness(ticks),progress=env.propagation(ticks);
        if(power<=0)return;
        CoverageView view=coverageView(pose);
        Map<Long,List<Node>> paths=new LinkedHashMap<>();
        for(BoltGeometry.Segment seg:bolt.segments()) {
            if(seg.alongA()>=progress)continue;
            double f=LightningMath.clamp((progress-seg.alongA())/Math.max(1e-9,seg.alongB()-seg.alongA()),0,1);
            Vec3 end=seg.a().lerp(seg.b(),f);
            if(seg.a().distanceToSqr(end)<1e-12)continue;
            var nodes=paths.computeIfAbsent(seg.mask(),key->new ArrayList<>());
            if(nodes.isEmpty())nodes.add(new Node(seg.a(),seg.widthA(),seg.alongA(),power*seg.intensity()));
            nodes.add(new Node(end,seg.widthA()+(seg.widthB()-seg.widthA())*f,
                seg.alongA()+(seg.alongB()-seg.alongA())*f,power*seg.intensity()));
        }
        drawLayers(paths.values(),r,Math.max(.004,r.strikeWidth),pose,camera,view,deferredCores);
    }
    private static void drawLayers(java.util.Collection<List<Node>> paths,LightningRecipe r,double baseWidth,
                                   PoseStack pose,Vec3 camera,CoverageView view,List<Runnable> deferredCores) {
        if(paths.isEmpty())return;
        boolean translucentHalo=deferredCores!=null;
        halos(paths,r,baseWidth,pose,camera,view,translucentHalo);
        if(translucentHalo)deferredCores.add(()->drawCores(paths,r,pose,camera,view));
        else drawCores(paths,r,pose,camera,view);
    }
    private static void drawCores(java.util.Collection<List<Node>> paths,LightningRecipe r,PoseStack pose,Vec3 camera,CoverageView view) {
        if(cores(paths,r,pose,camera,view))return;
        material(r,false);
        for(var path:paths)ribbon(path,2,pose,camera,false,r,view);
        BUFFER.endBatch(type(false,r));
    }
    private static boolean cores(java.util.Collection<List<Node>> paths,LightningRecipe r,PoseStack pose,Vec3 camera,CoverageView view){
        // Union body and round joints before compositing so a joint cannot
        // accumulate a second or third core contribution into a bright bead.
        List<List<HaloGeometry.Point>> strokes=new ArrayList<>();
        for(var path:paths)strokes.add(path.stream().map(node->new HaloGeometry.Point(node.p().subtract(camera),node.width()*2,node.along(),node.alpha())).toList());
        return LightningHalo.drawCore(strokes,r,view.projection(),pose.last().pose(),view.width(),view.height());
    }
    private static void halos(java.util.Collection<List<Node>> paths,LightningRecipe r,double baseWidth,PoseStack pose,Vec3 camera,CoverageView view,boolean translucent){
        if(r.glowGain<=0)return;
        double scale=LightningAppearance.ribbonScale(r,true,baseWidth);
        List<List<HaloGeometry.Point>> strokes=new ArrayList<>();
        for(var path:paths)strokes.add(path.stream().map(node->new HaloGeometry.Point(node.p().subtract(camera),node.width()*scale,node.along(),node.alpha())).toList());
        LightningHalo.draw(strokes,r,view.projection(),pose.last().pose(),view.width(),view.height(),translucent);
    }
    private static void ribbon(List<Node> path,double scale,PoseStack pose,Vec3 cam,boolean halo,LightningRecipe r,CoverageView view) {
        if(path.size()<2)return;
        // Project independent convex strips in the actual camera transform.
        // Shared 3D miters can turn into self-intersecting sheets when looking
        // along the channel; widening their almost tangential projection made
        // the old subpixel correction amplify those sheets into bright spikes.
        var points=path.stream().map(n->new HaloGeometry.Point(n.p().subtract(cam),n.width()*scale,n.along(),n.alpha())).toList();
        var mesh=HaloGeometry.build(points,view.projection(),r,halo);
        ShaderInstance selected=LightningAppearance.vividCore(r)?vividShader:shader;
        coreVertices(mesh.body(),false,pose,selected,r);
        coreVertices(mesh.round(),true,pose,selected,r);
    }
    private static void coreVertices(List<HaloGeometry.Vertex> vertices,boolean round,PoseStack pose,ShaderInstance selected,LightningRecipe r){
        if(vertices.isEmpty())return;
        selected.safeGetUniform("RoundJoint").set(round?1f:0f);
        VertexConsumer out=BUFFER.getBuffer(type(false,r));
        Matrix4f matrix=pose.last().pose();
        for(var v:vertices)RibbonCoverage.emit(out,matrix,v.p(),v.alpha(),v.u(),v.v(),v.rgb());
        // Uniform belongs to this batch, not the following joint/body batch.
        BUFFER.endBatch(type(false,r));
    }
}

