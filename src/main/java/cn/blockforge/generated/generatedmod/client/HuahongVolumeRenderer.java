package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.client.realmflight.MeteorBloomRenderer;
import com.mojang.blaze3d.pipeline.TextureTarget;
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
import org.joml.Matrix3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Current V13 material adapter over a continuous world-history coordinate field.
 * A reference-proportioned art window bounds this form independently of gameplay speed. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class HuahongVolumeRenderer {
    private static final int POINTS=32,MAX_PACKETS=32;
    public static final float DEFAULT_WIDTH=(float)(.52*HuahongHeadBridge.BODY_SCALE),DEFAULT_LIFE=2.2F,DEFAULT_SPREAD=1.25F,DEFAULT_SHELL=3.5F;
    private static final double PATH_LENGTH=8.2*2.2*HuahongHeadBridge.BODY_SCALE,RADIUS=.025+DEFAULT_WIDTH*(.10+DEFAULT_SHELL*.012+DEFAULT_SPREAD*3.9);
    private static final ArrayList<Packet> PENDING=new ArrayList<>();
    private static final ArrayList<Trace> TRACE=new ArrayList<>();
    private static List<Trace> previousTrace=List.of();
    private static ShaderInstance shader,direct,reconstruct;
    private static TextureTarget integrationTarget;
    private static int pathLocation=-1,boundsLocation=-1,directPathLocation=-1;
    private static long frameSequence;
    private static float exposureMultiplier=1;
    public static int queuedThisFrame,submittedThisFrame,totalDraws;
    public static String status="Awaiting huahong volume shader";
    public static String shaderHash="unregistered";

    public record Trace(int pathPoints,float pathLength,float strength,float exposure,int sceneDepth,
                        boolean shaderReady,boolean shaderPack,boolean submitted,List<List<Float>> path,
                        List<Float> pathAges,List<Float> worldToClip,List<Float> inverseWorldToClip,List<Float> clipBounds,
                        List<Float> boundsMin,List<Float> boundsMax,List<Float> materialOrigin,List<Float> shapeProfile,float effectTime,double worldTime,long frame,
                        List<Float> worldToMaterial) {}
    private record Packet(float[] path,float[] ages,float[] segments,int count,float length,float strength,float exposure,
                          float time,double now,Vec3 min,Vec3 max,Matrix3f materialBasis,Matrix4f projection,Matrix4f inverse,float[] clip,int depth,long frame) {}
    private HuahongVolumeRenderer() {}
    public static boolean ready(){return shader!=null&&pathLocation>=0&&direct!=null&&directPathLocation>=0;}
    public static List<Trace> currentTrace(){return List.copyOf(TRACE);}
    public static List<Trace> lastTrace(){return previousTrace;}
    /** Submit a queued world layer before a foreground consumer or capture. */
    public static void flushPending(){flush();}
    public static void setExposure(float multiplier){exposureMultiplier=Float.isFinite(multiplier)?Math.max(.01F,multiplier):1;}
    public static Map<String,Object> diagnostics(){return Map.of("frame",frameSequence,"queued",queuedThisFrame,"submitted",submittedThisFrame,
            "totalDraws",totalDraws,"ready",ready(),"status",status,"shaderHash",shaderHash,"currentPackets",currentTrace(),"lastPackets",lastTrace(),"noiseCache",HuahongVolumeNoiseCache.status());}

    @SubscribeEvent public static void register(RegisterShadersEvent event)throws IOException {
        HuahongVolumeNoiseCache.release();
        releaseIntegrationTarget();
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
            boundsLocation=GL20.glGetUniformLocation(value.getId(),"SegmentBounds[0]");
            status=pathLocation>=0?"Huahong volume shader ready":"Huahong volume path uniform unavailable";
        });
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID,"huahong_volume_direct"),DefaultVertexFormat.POSITION_TEX),value->{direct=value;directPathLocation=GL20.glGetUniformLocation(value.getId(),"PathPoints[0]");});
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID,"huahong_volume_reconstruct"),DefaultVertexFormat.POSITION_TEX),value->reconstruct=value);
        try(var input=event.getResourceProvider().getResource(ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID,"shaders/core/huahong_volume.fsh")).orElseThrow().open()){
            HuahongVolumeNoiseCache.reload(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    /** Call during the world draw. Positions and this matrix both use camera-relative world space. */
    public static Trace queue(List<SwordFlightTrailHistory.Node> nodes,Vec3 eye,double now,float strength,
                              Matrix4f worldToClip,float exposure) {
        if(nodes.size()<2)return null;
        var a=nodes.get(nodes.size()-2).point();var b=nodes.get(nodes.size()-1).point();
        Vec3 forward=new Vec3(b.x()-a.x(),b.y()-a.y(),b.z()-a.z());
        if(forward.lengthSqr()<1e-12)forward=new Vec3(0,0,1);else forward=forward.normalize();
        Vec3 side=forward.cross(Math.abs(forward.y)>.95?new Vec3(0,0,1):new Vec3(0,1,0)).normalize();
        return queue(nodes,eye,now,strength,worldToClip,exposure,forward,side);
    }

    /** The carrier basis controls only material transport. Recorded positions,
     * spherical support and scene-depth clipping remain in world space. */
    public static Trace queue(List<SwordFlightTrailHistory.Node> nodes,Vec3 eye,double now,float strength,
                              Matrix4f worldToClip,float exposure,Vec3 forward,Vec3 side) {
        if(nodes.size()<2||TrailShaderBridge.shadowPass()||PENDING.size()>=MAX_PACKETS)return null;
        int depth=TrailShaderBridge.capturedDepthTexture();
        if(depth<0&&!TrailShaderBridge.captureDepth()){status="Awaiting volume world depth";return null;}
        depth=TrailShaderBridge.capturedDepthTexture();
        int last=nodes.size()-1;var newest=nodes.get(last);double headArc=newest.arc(),startArc=Math.max(nodes.get(0).arc(),headArc-PATH_LENGTH);
        double length=headArc-startArc;if(length<.01)return null;
        float[] path=new float[POINTS*4],ages=new float[POINTS];
        double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
        int cursor=last;
        for(int i=0;i<POINTS;i++){
            double arc=headArc-length*i/(POINTS-1);
            while(cursor>0&&nodes.get(cursor-1).arc()>arc)cursor--;
            var b=nodes.get(cursor);var a=nodes.get(Math.max(0,cursor-1));
            double t=Mth.clamp((arc-a.arc())/Math.max(1e-10,b.arc()-a.arc()),0,1);
            var p=a.point().lerp(b.point(),t);double x=p.x()-eye.x,y=p.y()-eye.y,z=p.z()-eye.z;
            path[i*4]=(float)x;path[i*4+1]=(float)y;path[i*4+2]=(float)z;path[i*4+3]=(float)(arc-headArc);
            ages[i]=(float)Math.max(0,(now-(a.tick()+(b.tick()-a.tick())*t))/20);
            minX=Math.min(minX,x-RADIUS);minY=Math.min(minY,y-RADIUS);minZ=Math.min(minZ,z-RADIUS);
            maxX=Math.max(maxX,x+RADIUS);maxY=Math.max(maxY,y+RADIUS);maxZ=Math.max(maxZ,z+RADIUS);
        }
        var min=new Vec3(minX,minY,minZ);var max=new Vec3(maxX,maxY,maxZ);
        float[] clip=clipBounds(worldToClip,min,max);if(clip==null)return null;
        Matrix4f inverse=new Matrix4f(worldToClip).invert();
        Matrix3f materialBasis=materialBasis(forward,side);
        Packet packet=new Packet(path,ages,segmentBounds(path,ages,POINTS,(float)length),POINTS,(float)length,Mth.clamp(strength,0,1),Math.max(.01F,exposure)*exposureMultiplier,
                (float)((now%2560+2560)%2560/20),now,min,max,materialBasis,new Matrix4f(worldToClip),inverse,clip,depth,frameSequence);
        PENDING.add(packet);queuedThisFrame++;
        Trace trace=trace(packet,false);TRACE.add(trace);return trace;
    }

    /** Conservative Gaussian support; discarded segments have sub-float contribution. */
    public static float[] segmentBounds(float[] path,float[] ages,int count,float length){
        float[] result=new float[31*4];
        for(int i=0;i<count-1;i++){
            int a=i*4,b=a+4;double dx=path[b]-path[a],dy=path[b+1]-path[a+1],dz=path[b+2]-path[a+2];
            double q=Math.max(Math.max(ages[i],ages[i+1])/DEFAULT_LIFE,Math.max(-path[a+3],-path[b+3])/length);q=Math.max(0,Math.min(1,q));
            double radius=.025+DEFAULT_WIDTH*(.10+DEFAULT_SHELL*.012+Math.pow(q,.9)*DEFAULT_SPREAD*3.9);
            double reach=Math.sqrt(dx*dx+dy*dy+dz*dz)*.5+radius*2.5+.001;
            result[a]=(path[a]+path[b])*.5F;result[a+1]=(path[a+1]+path[b+1])*.5F;result[a+2]=(path[a+2]+path[b+2])*.5F;result[a+3]=(float)(reach*reach);
        }
        return result;
    }
    public static boolean accelerate(float[] path,Matrix3f basis){
        // Front views see most path segments at once. Retain the original
        // program there, including its lower register pressure and no branches.
        double front=-(basis.m02()*path[0]+basis.m12()*path[1]+basis.m22()*path[2]);
        return front<=2;
    }
    /** One smooth rendered carrier frame, never a nearest-segment winner. */
    public static Matrix3f materialBasis(Vec3 forward,Vec3 side){
        Vec3 f=forward.lengthSqr()>1e-12?forward.normalize():new Vec3(0,0,1);
        Vec3 s=side.subtract(f.scale(side.dot(f)));
        if(s.lengthSqr()<1e-12)s=f.cross(Math.abs(f.y)>.95?new Vec3(0,0,1):new Vec3(0,1,0));
        s=s.normalize();Vec3 up=f.cross(s).normalize();
        // JOML constructor arguments are column-major: rows are dot products.
        return new Matrix3f((float)s.x,(float)up.x,(float)f.x,
                (float)s.y,(float)up.y,(float)f.y,(float)s.z,(float)up.z,(float)f.z);
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
                vector(p.min()),vector(p.max()),List.of(p.path()[0],p.path()[1],p.path()[2]),
                List.of(DEFAULT_WIDTH,DEFAULT_LIFE,DEFAULT_SPREAD,DEFAULT_SHELL),p.time(),p.now(),p.frame(),matrix(p.materialBasis()));
    }

    private static void flush() {
        if(PENDING.isEmpty())return;
        var packets=new ArrayList<>(PENDING);PENDING.clear();
        if(Minecraft.getInstance().level==null||!ready()){status="Volume shader or level unavailable";return;}
        HuahongVolumeNoiseCache.ensure();
        packets.sort(Comparator.comparingDouble((Packet p)->p.min().add(p.max()).scale(.5).lengthSqr()).reversed());
        try(var saved=new MeteorBloomRenderer.State()){
            SwordFlightTrailRenderer.clearPolygonOffset();
            var target=Minecraft.getInstance().getMainRenderTarget();target.bindWrite(true);
            RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.disableScissor();
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL20.glBlendEquationSeparate(org.lwjgl.opengl.GL14.GL_FUNC_ADD,org.lwjgl.opengl.GL14.GL_FUNC_ADD);
            RenderSystem.setShaderColor(1,1,1,1);
            int reducedDraws=0;
            long screenPixels=(long)target.viewWidth*target.viewHeight;
            // Keep the scratch allocation stable as the effect moves in and
            // out of view. High-resolution windows still have a finite ray budget.
            int divisor=Math.max(2,(int)Math.ceil(Math.sqrt(screenPixels/460800.0)));
            for(var p:packets){
                float[] c=p.clip();
                boolean reduced=reconstruct!=null&&(c[2]-c[0])*(c[3]-c[1])>=.16F
                        &&screenPixels>=640L*360;
                if(reduced){
                    var low=integrationTarget((target.viewWidth+divisor-1)/divisor,(target.viewHeight+divisor-1)/divisor);
                    RenderSystem.colorMask(true,true,true,true);
                    low.clear(Minecraft.ON_OSX);
                    low.bindWrite(true);
                }else{
                    RenderSystem.colorMask(true,true,true,false);
                }
                boolean accelerated=accelerate(p.path(),p.materialBasis());
                ShaderInstance pass=accelerated?shader:direct;
                int cache=HuahongVolumeNoiseCache.texture();
                pass.safeGetUniform("GradientCacheEnabled").set(cache!=0&&accelerated?1:0);
                pass.safeGetUniform("FieldCullEnabled").set(boundsLocation>=0&&accelerated?1:0);
                pass.setSampler("GradientCache",cache);RenderSystem.setShaderTexture(1,cache);
                pass.safeGetUniform("InverseWorldToClip").set(p.inverse());
                pass.safeGetUniform("OutputSize").set((float)(reduced?integrationTarget.viewWidth:target.viewWidth),
                        (float)(reduced?integrationTarget.viewHeight:target.viewHeight));
                pass.safeGetUniform("BoundsMin").set((float)p.min().x,(float)p.min().y,(float)p.min().z);
                pass.safeGetUniform("BoundsMax").set((float)p.max().x,(float)p.max().y,(float)p.max().z);
                pass.safeGetUniform("WorldToMaterial").set(p.materialBasis());
                pass.safeGetUniform("PathAges").set(p.ages());pass.safeGetUniform("PathCount").set(p.count());
                pass.safeGetUniform("PathLength").set(p.length());pass.safeGetUniform("Strength").set(p.strength());
                pass.safeGetUniform("ShapeProfile").set(DEFAULT_WIDTH,DEFAULT_LIFE,DEFAULT_SPREAD,DEFAULT_SHELL);
                pass.safeGetUniform("Exposure").set(p.exposure());pass.safeGetUniform("EffectTime").set(p.time());
                RenderSystem.setShaderTexture(0,p.depth());RenderSystem.setShader(()->pass);
                pass.apply();GL20.glUniform4fv(accelerated?pathLocation:directPathLocation,p.path());if(accelerated&&boundsLocation>=0)GL20.glUniform4fv(boundsLocation,p.segments());
                drawQuad(c[0],c[1],c[2],c[3]);
                pass.clear();
                if(reduced){
                    target.bindWrite(true);
                    RenderSystem.colorMask(true,true,true,false);
                    RenderSystem.setShaderTexture(0,integrationTarget.getColorTextureId());
                    RenderSystem.setShaderTexture(1,p.depth());
                    reconstruct.safeGetUniform("InverseWorldToClip").set(p.inverse());
                    reconstruct.safeGetUniform("OutputSize").set((float)target.viewWidth,(float)target.viewHeight);
                    RenderSystem.setShader(()->reconstruct);
                    // Cover the extra reconstruction tap beyond the integration proxy.
                    float dx=2F/integrationTarget.viewWidth,dy=2F/integrationTarget.viewHeight;
                    drawQuad(Math.max(-1,c[0]-dx),Math.max(-1,c[1]-dy),Math.min(1,c[2]+dx),Math.min(1,c[3]+dy));
                    reconstruct.clear();
                    reducedDraws++;
                }
                submittedThisFrame++;totalDraws++;
                TRACE.add(trace(p,true));
            }
            status="3D path density / 128 steps / scene-depth clipped / "+reducedDraws+" reconstructed draws";
        }
    }
    private static TextureTarget integrationTarget(int width,int height){
        if(integrationTarget!=null&&integrationTarget.viewWidth==width&&integrationTarget.viewHeight==height)return integrationTarget;
        releaseIntegrationTarget();
        var target=new TextureTarget(width,height,false,Minecraft.ON_OSX);
        RenderSystem.bindTexture(target.getColorTextureId());
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA16F,target.width,target.height,0,
                GL11.GL_RGBA,GL11.GL_FLOAT,(java.nio.ByteBuffer)null);
        target.setClearColor(0,0,0,0);
        integrationTarget=target;
        return target;
    }
    private static void releaseIntegrationTarget(){
        if(integrationTarget!=null){integrationTarget.destroyBuffers();integrationTarget=null;}
    }
    private static void drawQuad(float x0,float y0,float x1,float y1){
        var b=Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_TEX);
        vertex(b,x0,y0);vertex(b,x1,y0);vertex(b,x1,y1);
        vertex(b,x0,y0);vertex(b,x1,y1);vertex(b,x0,y1);
        BufferUploader.drawWithShader(b.end());
    }
    private static List<Float> floats(float[] values){var list=new ArrayList<Float>(values.length);for(float value:values)list.add(value);return List.copyOf(list);}
    private static List<Float> vector(Vec3 p){return List.of((float)p.x,(float)p.y,(float)p.z);}
    private static List<Float> matrix(Matrix4f matrix){float[] values=new float[16];matrix.get(values);return floats(values);}
    private static List<Float> matrix(Matrix3f matrix){float[] values=new float[9];matrix.get(values);return floats(values);}
    private static void vertex(BufferBuilder b,float x,float y){b.vertex(x,y,0).uv(x*.5F+.5F,y*.5F+.5F).endVertex();}

    @Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
    public static final class Events {
        private static boolean hadWorld;
        @SubscribeEvent public static void world(TickEvent.ClientTickEvent event){if(event.phase==TickEvent.Phase.END){boolean present=Minecraft.getInstance().level!=null;if(hadWorld&&!present){HuahongVolumeNoiseCache.release();releaseIntegrationTarget();}hadWorld=present;}}
        @SubscribeEvent(priority=EventPriority.LOWEST) public static void frame(TickEvent.RenderTickEvent event){
            if(event.phase==TickEvent.Phase.START){frameSequence++;previousTrace=List.copyOf(TRACE);TRACE.clear();PENDING.clear();queuedThisFrame=submittedThisFrame=0;}
            else flush();
        }
        @SubscribeEvent(priority=EventPriority.LOWEST) public static void gui(RenderGuiEvent.Pre event){
            if(!PENDING.isEmpty()){event.getGuiGraphics().flush();flush();}
        }
    }
}
