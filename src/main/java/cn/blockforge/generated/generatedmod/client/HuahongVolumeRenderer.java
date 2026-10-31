package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.client.realmflight.MeteorBloomRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Original world-path density renderer. No reference/source shader is embedded.
 * A forty-block art window bounds this form; it is not a natural speed-times-life tail. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class HuahongVolumeRenderer {
    private static final int POINTS=32,MAX_PACKETS=32;
    public static final float DEFAULT_WIDTH=.52F,DEFAULT_LIFE=2.2F,DEFAULT_SPREAD=1.25F,DEFAULT_SHELL=3.5F;
    private static final double PATH_LENGTH=40,RADIUS=4.8;
    private static final ArrayList<Packet> PENDING=new ArrayList<>();
    private static final ArrayList<Trace> TRACE=new ArrayList<>();
    private static List<Trace> previousTrace=List.of();
    private static ShaderInstance shader;
    private static int pathLocation=-1;
    private static long frameSequence;
    private static float exposureMultiplier=1;
    public static int queuedThisFrame,submittedThisFrame,totalDraws;
    public static String status="Awaiting huahong volume shader";
    public static String shaderHash="unregistered";

    public record Trace(int pathPoints,float pathLength,float strength,float exposure,int sceneDepth,
                        boolean shaderReady,boolean shaderPack,boolean submitted,List<List<Float>> path,
                        List<Float> pathAges,List<Float> worldToClip,List<Float> inverseWorldToClip,List<Float> clipBounds,
                        List<Float> boundsMin,List<Float> boundsMax,List<Float> noiseOrigin,List<Float> shapeProfile,float effectTime,double worldTime,long frame) {}
    private record Packet(float[] path,float[] ages,int count,float length,float strength,float exposure,
                          float time,double now,Vec3 eye,Vec3 min,Vec3 max,Matrix4f projection,Matrix4f inverse,float[] clip,int depth,long frame) {}
    private HuahongVolumeRenderer() {}
    public static boolean ready(){return shader!=null&&pathLocation>=0;}
    public static List<Trace> currentTrace(){return List.copyOf(TRACE);}
    public static List<Trace> lastTrace(){return previousTrace;}
    /** Submit a queued world layer before a foreground consumer or capture. */
    public static void flushPending(){flush();}
    public static void setExposure(float multiplier){exposureMultiplier=Float.isFinite(multiplier)?Math.max(.01F,multiplier):1;}
    public static Map<String,Object> diagnostics(){return Map.of("frame",frameSequence,"queued",queuedThisFrame,"submitted",submittedThisFrame,
            "totalDraws",totalDraws,"ready",ready(),"status",status,"shaderHash",shaderHash,"currentPackets",currentTrace(),"lastPackets",lastTrace());}

    @SubscribeEvent public static void register(RegisterShadersEvent event)throws IOException {
        PENDING.clear();TRACE.clear();previousTrace=List.of();queuedThisFrame=submittedThisFrame=0;pathLocation=-1;
        try{
            var digest=java.security.MessageDigest.getInstance("SHA-256");
            for(String extension:new String[]{"vsh","fsh","json"})try(var input=event.getResourceProvider().getResource(
                    ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID,"shaders/core/huahong_volume."+extension)).orElseThrow().open()){
                digest.update(input.readAllBytes());
            }
            shaderHash=java.util.HexFormat.of().formatHex(digest.digest());
        }catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID,"huahong_volume"),DefaultVertexFormat.POSITION_TEX),value->{
            shader=value;pathLocation=GL20.glGetUniformLocation(value.getId(),"PathPoints[0]");
            status=pathLocation>=0?"Huahong volume shader ready":"Huahong volume path uniform unavailable";
        });
    }

    /** Call during the world draw. Positions and this matrix both use camera-relative world space. */
    public static Trace queue(List<SwordFlightTrailHistory.Node> nodes,Vec3 eye,double now,float strength,
                              Matrix4f worldToClip,float exposure) {
        if(nodes.size()<2||TrailShaderBridge.shadowPass()||PENDING.size()>=MAX_PACKETS)return null;
        int depth=TrailShaderBridge.capturedDepthTexture();
        if(depth<0&&!TrailShaderBridge.captureDepth()){status="Awaiting volume world depth";return null;}
        depth=TrailShaderBridge.capturedDepthTexture();
        int last=nodes.size()-1;var newest=nodes.get(last);double headArc=newest.arc(),startArc=Math.max(nodes.get(0).arc(),headArc-PATH_LENGTH);
        double length=headArc-startArc;if(length<.01)return null;
        float[] path=new float[POINTS*4],ages=new float[POINTS];
        double origin=Math.floor(startArc/256)*256;
        double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
        int cursor=last;
        for(int i=0;i<POINTS;i++){
            double arc=headArc-length*i/(POINTS-1);
            while(cursor>0&&nodes.get(cursor-1).arc()>arc)cursor--;
            var b=nodes.get(cursor);var a=nodes.get(Math.max(0,cursor-1));
            double t=Mth.clamp((arc-a.arc())/Math.max(1e-10,b.arc()-a.arc()),0,1);
            var p=a.point().lerp(b.point(),t);double x=p.x()-eye.x,y=p.y()-eye.y,z=p.z()-eye.z;
            path[i*4]=(float)x;path[i*4+1]=(float)y;path[i*4+2]=(float)z;path[i*4+3]=(float)(arc-origin);
            ages[i]=(float)Math.max(0,(now-(a.tick()+(b.tick()-a.tick())*t))/20);
            minX=Math.min(minX,x-RADIUS);minY=Math.min(minY,y-RADIUS);minZ=Math.min(minZ,z-RADIUS);
            maxX=Math.max(maxX,x+RADIUS);maxY=Math.max(maxY,y+RADIUS);maxZ=Math.max(maxZ,z+RADIUS);
        }
        var min=new Vec3(minX,minY,minZ);var max=new Vec3(maxX,maxY,maxZ);
        float[] clip=clipBounds(worldToClip,min,max);if(clip==null)return null;
        Matrix4f inverse=new Matrix4f(worldToClip).invert();
        Packet packet=new Packet(path,ages,POINTS,(float)length,Mth.clamp(strength,0,1),Math.max(.01F,exposure)*exposureMultiplier,
                (float)((now%2560+2560)%2560/20),now,eye,min,max,new Matrix4f(worldToClip),inverse,clip,depth,frameSequence);
        PENDING.add(packet);queuedThisFrame++;
        Trace trace=trace(packet,false);TRACE.add(trace);return trace;
    }

    /** Convenience entry for callers whose positions are already in RenderSystem world space. */
    public static Trace queue(List<SwordFlightTrailHistory.Node> nodes,Vec3 eye,double now,float strength) {
        return queue(nodes,eye,now,strength,new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix()),1);
    }

    private static float[] clipBounds(Matrix4f projection,Vec3 min,Vec3 max) {
        float x0=1,y0=1,x1=-1,y1=-1;
        for(int i=0;i<8;i++){
            Vector4f p=projection.transform(new Vector4f((float)((i&1)==0?min.x:max.x),(float)((i&2)==0?min.y:max.y),(float)((i&4)==0?min.z:max.z),1));
            if(p.w<=.001)return new float[]{-1,-1,1,1};
            float x=p.x/p.w,y=p.y/p.w;x0=Math.min(x0,x);x1=Math.max(x1,x);y0=Math.min(y0,y);y1=Math.max(y1,y);
        }
        if(x1<-1||x0>1||y1<-1||y0>1)return null;
        return new float[]{Math.max(-1,x0-.005F),Math.max(-1,y0-.005F),Math.min(1,x1+.005F),Math.min(1,y1+.005F)};
    }

    private static Trace trace(Packet p,boolean submitted) {
        var points=new ArrayList<List<Float>>(p.count());
        for(int i=0;i<p.count();i++)points.add(List.of(p.path()[i*4],p.path()[i*4+1],p.path()[i*4+2],p.path()[i*4+3]));
        return new Trace(p.count(),p.length(),p.strength(),p.exposure(),p.depth(),ready(),TrailShaderBridge.shaderPackDetected(),submitted,
                List.copyOf(points),floats(p.ages()),matrix(p.projection()),matrix(p.inverse()),List.of(p.clip()[0],p.clip()[1],p.clip()[2],p.clip()[3]),
                vector(p.min()),vector(p.max()),List.of((float)mod(p.eye().x,256),(float)mod(p.eye().y,256),(float)mod(p.eye().z,256)),
                List.of(DEFAULT_WIDTH,DEFAULT_LIFE,DEFAULT_SPREAD,DEFAULT_SHELL),p.time(),p.now(),p.frame());
    }

    private static void flush() {
        if(PENDING.isEmpty())return;
        var packets=new ArrayList<>(PENDING);PENDING.clear();
        if(Minecraft.getInstance().level==null||!ready()){status="Volume shader or level unavailable";return;}
        packets.sort(Comparator.comparingDouble((Packet p)->p.min().add(p.max()).scale(.5).lengthSqr()).reversed());
        try(var saved=new MeteorBloomRenderer.State()){
            var target=Minecraft.getInstance().getMainRenderTarget();target.bindWrite(true);
            RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.disableScissor();
            RenderSystem.enableBlend();RenderSystem.blendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.colorMask(true,true,true,false);RenderSystem.setShaderColor(1,1,1,1);
            for(var p:packets){
                shader.safeGetUniform("InverseWorldToClip").set(p.inverse());
                shader.safeGetUniform("OutputSize").set((float)target.viewWidth,(float)target.viewHeight);
                shader.safeGetUniform("BoundsMin").set((float)p.min().x,(float)p.min().y,(float)p.min().z);
                shader.safeGetUniform("BoundsMax").set((float)p.max().x,(float)p.max().y,(float)p.max().z);
                shader.safeGetUniform("NoiseOrigin").set((float)mod(p.eye().x,256),(float)mod(p.eye().y,256),(float)mod(p.eye().z,256));
                shader.safeGetUniform("PathAges").set(p.ages());shader.safeGetUniform("PathCount").set(p.count());
                shader.safeGetUniform("PathLength").set(p.length());shader.safeGetUniform("Strength").set(p.strength());
                shader.safeGetUniform("ShapeProfile").set(DEFAULT_WIDTH,DEFAULT_LIFE,DEFAULT_SPREAD,DEFAULT_SHELL);
                shader.safeGetUniform("Exposure").set(p.exposure());shader.safeGetUniform("EffectTime").set(p.time());
                RenderSystem.setShaderTexture(0,p.depth());RenderSystem.setShader(()->shader);
                shader.apply();GL20.glUniform4fv(pathLocation,p.path());
                var b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_TEX);
                float[] c=p.clip();vertex(b,c[0],c[1]);vertex(b,c[2],c[1]);vertex(b,c[2],c[3]);
                vertex(b,c[0],c[1]);vertex(b,c[2],c[3]);vertex(b,c[0],c[3]);
                BufferUploader.drawWithShader(b.end());shader.clear();submittedThisFrame++;totalDraws++;
                TRACE.add(trace(p,true));
            }
            status="Original 3D path density / 128 steps / scene-depth clipped / direct exposure";
        }
    }
    private static double mod(double value,double period){return value-Math.floor(value/period)*period;}
    private static List<Float> floats(float[] values){var list=new ArrayList<Float>(values.length);for(float value:values)list.add(value);return List.copyOf(list);}
    private static List<Float> vector(Vec3 p){return List.of((float)p.x,(float)p.y,(float)p.z);}
    private static List<Float> matrix(Matrix4f matrix){float[] values=new float[16];matrix.get(values);return floats(values);}
    private static void vertex(BufferBuilder b,float x,float y){b.vertex(x,y,0).uv(x*.5F+.5F,y*.5F+.5F).endVertex();}

    @Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent(priority=EventPriority.LOWEST) public static void frame(TickEvent.RenderTickEvent event){
            if(event.phase==TickEvent.Phase.START){frameSequence++;previousTrace=List.copyOf(TRACE);TRACE.clear();PENDING.clear();queuedThisFrame=submittedThisFrame=0;}
            else flush();
        }
        @SubscribeEvent(priority=EventPriority.LOWEST) public static void gui(RenderGuiEvent.Pre event){
            if(!PENDING.isEmpty()){event.getGuiGraphics().flush();flush();}
        }
    }
}
