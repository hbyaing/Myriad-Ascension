package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.yujian.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.util.*;

/** Observation-only QA of actual entity pose, baked model rear, and submitted ordinary vertices. */
@Mod.EventBusSubscriber(modid="wuxingxiantu",value=Dist.CLIENT)
public final class OrdinaryHiltQa {
    public static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.huahongReproHilt");
    public static final boolean LAYERS=Boolean.getBoolean("wuxingxiantu.huahongReproHiltLayers");
    private static long renderFrame;
    private static int bloomCalls,bloomVertices;
    private static final ArrayList<Sample> VERTICES=new ArrayList<>();
    private static ModelFrame model;
    private record Sample(Vec3 world,Matrix4f pose,float u,float v,float alpha,boolean spiral){}
    private record ModelFrame(Vec3 hilt,Vec3 attachment,Vec3 center,Vec3 forward,Vec3 eye,Vec3 actualHiltView,Matrix4f actualEntityPose,List<Vec3> rearView,String item,int modelVertices){}
    @SubscribeEvent public static void reset(TickEvent.RenderTickEvent event){if(ENABLED&&event.phase==TickEvent.Phase.START){renderFrame++;VERTICES.clear();model=null;bloomCalls=bloomVertices=0;}}
    public static void bloomAttempt(com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer mesh){if(ENABLED){bloomCalls++;if(mesh!=null)bloomVertices+=mesh.drawState().vertexCount();}}
    public static String layer(){return LAYERS?new String[]{"FULL","DIRECT","BLOOM","MODEL"}[(int)(renderFrame%4)]:"FULL";}
    public static boolean suppressDirect(){return LAYERS&&(layer().equals("MODEL")||layer().equals("BLOOM"));}
    public static boolean suppressBloom(){return LAYERS&&(layer().equals("MODEL")||layer().equals("DIRECT"));}
    public static void model(ControlledSwordEntity sword,float partial,PoseStack source){
        if(!ENABLED||TrailShaderBridge.shadowPass()||Minecraft.getInstance().player==null||Minecraft.getInstance().player.getVehicle()!=sword)return;
        var mc=Minecraft.getInstance();var f=InterpolatedFlyingSwordTransform.get(sword,partial);var d=FlyingSwordRegistry.visual(sword.display());
        if(f.huahong()>=.995F)return;
        BakedModel baked=d.baked3d()?mc.getModelManager().getModel(FlyingSwordItemModels.worldModel(d)):mc.getItemRenderer().getModel(sword.display(),sword.level(),null,sword.getId());
        // Measure in carrier space first. The actual entity entry pose already
        // contains the view rotation and must not be interpreted as world axes.
        var pose=new PoseStack();SwordFlightAttachment.rotateModel(pose,d.modelYaw(),d.modelPitch(),d.modelRoll());float scale=2.35F*sword.visualScale();pose.scale(scale,scale,scale);
        baked=baked.applyTransform(d.baked3d()?ItemDisplayContext.NONE:ItemDisplayContext.FIXED,pose,false);pose.translate(-.5,-.5,-.5);
        Vec3 eye=mc.gameRenderer.getMainCamera().getPosition(),forward=f.forward();var points=new ArrayList<Vec3>();var random=net.minecraft.util.RandomSource.create(42);
        for(int face=-1;face<6;face++){random.setSeed(42);for(var quad:baked.getQuads(null,face<0?null:net.minecraft.core.Direction.values()[face],random)){
            int[] data=quad.getVertices();int stride=data.length/4;for(int i=0;i<4;i++)points.add(new Vec3(pose.last().pose().transformPosition(new Vector3f(Float.intBitsToFloat(data[i*stride]),Float.intBitsToFloat(data[i*stride+1]),Float.intBitsToFloat(data[i*stride+2])))));
        }}
        if(points.isEmpty())return;double rear=points.stream().mapToDouble(p->p.z).min().orElseThrow(),front=points.stream().mapToDouble(p->p.z).max().orElseThrow();
        double band=Math.max(1e-5,(front-rear)*1e-4);var rearPoints=points.stream().filter(p->p.z<=rear+band).toList();
        double sx0=Double.POSITIVE_INFINITY,sx1=Double.NEGATIVE_INFINITY,uy0=sx0,uy1=sx1;
        for(var p:rearPoints){sx0=Math.min(sx0,p.x);sx1=Math.max(sx1,p.x);uy0=Math.min(uy0,p.y);uy1=Math.max(uy1,p.y);}
        var localHilt=new Vector3f((float)((sx0+sx1)*.5),(float)((uy0+uy1)*.5),(float)rear);
        Vec3 hilt=f.position().add(new Vec3(f.rotation().transform(new Vector3f(localHilt))));
        var actual=new Matrix4f(source.last().pose()).rotate(f.rotation());
        var rearView=rearPoints.stream().map(p->new Vec3(actual.transformPosition(new Vector3f((float)p.x,(float)p.y,(float)p.z)))).toList();
        model=new ModelFrame(hilt,SwordFlightAttachment.tail(sword,f.position(),f.yaw(),f.pitch(),f.roll()),f.position(),forward,eye,new Vec3(actual.transformPosition(new Vector3f(localHilt))),new Matrix4f(source.last().pose()),rearView,String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(sword.display().getItem())),points.size());
    }
    public static void vertex(PoseStack.Pose pose,Vec3 world,float u,float v,float alpha,boolean spiral){if(ENABLED&&!TrailShaderBridge.shadowPass())VERTICES.add(new Sample(world,new Matrix4f(pose.pose()),u,v,alpha,spiral));}
    private static List<Double> vec(Vec3 p){return List.of(p.x,p.y,p.z);}
    private static List<Double> project(Vec3 world,Matrix4f pose,Matrix4f projection,Vec3 eye){
        var p=world.subtract(eye);var clip=projection.transform(pose.transform(new Vector4f((float)p.x,(float)p.y,(float)p.z,1)));
        if(!Float.isFinite(clip.w)||Math.abs(clip.w)<1e-8)return List.of(-99999D,-99999D,-1D);
        var w=Minecraft.getInstance().getWindow();return List.of((clip.x/clip.w*.5+.5)*w.getWidth(),(.5-clip.y/clip.w*.5)*w.getHeight(),(double)clip.w);
    }
    private static List<Double> projectView(Vec3 point,Matrix4f projection){return project(point,new Matrix4f(),projection,Vec3.ZERO);}
    public static Map<String,Object> snapshot(){
        if(!ENABLED||model==null)return Map.of("observed",false,"reason","No actual local sword entity model this render frame");
        try{
            var field=FlyingSwordEffects.class.getDeclaredField("projection");field.setAccessible(true);var projection=new Matrix4f((Matrix4f)field.get(null));
            var ordinary=VERTICES.stream().filter(v->!v.spiral&&v.alpha>.002).toList();var spirals=VERTICES.stream().filter(v->v.spiral&&v.alpha>.002).toList();
            var out=new LinkedHashMap<String,Object>();out.put("observed",true);out.put("probeVersion",2);out.put("layer",layer());out.put("layerScope",LAYERS?"Diagnostic adjacent real frames: model always visible; FULL adds direct+bloom, DIRECT adds direct only, BLOOM adds bloom only, MODEL adds neither. Never primary continuous visual acceptance.":"Unmodified full rendering");out.put("item",model.item);out.put("modelVerticesMeasured",model.modelVertices);out.put("worldHilt",vec(model.hilt));out.put("worldAttachment",vec(model.attachment));out.put("hiltAttachmentDistance",model.hilt.distanceTo(model.attachment));out.put("worldForward",vec(model.forward));out.put("eye",vec(model.eye));
            Matrix4f trailPose=VERTICES.isEmpty()?new Matrix4f():VERTICES.get(0).pose;
            out.put("ordinaryProjectionAvailable",!VERTICES.isEmpty());
            out.put("actualModelRearPixels",model.rearView.stream().map(p->projectView(p,projection)).toList());out.put("hiltPixel",projectView(model.actualHiltView,projection));out.put("canonicalHiltPixel",project(model.hilt,trailPose,projection,model.eye));out.put("attachmentPixel",project(model.attachment,trailPose,projection,model.eye));
            out.put("viewAngleFromForwardDegrees",Math.toDegrees(Math.acos(Math.max(-1,Math.min(1,model.eye.subtract(model.hilt).normalize().dot(model.forward))))));
            out.put("ordinary",vertices(ordinary,projection));out.put("spiral",vertices(spirals,projection));out.put("ordinaryNearHilt2_5m",vertices(ordinary.stream().filter(v->v.world.distanceToSqr(model.hilt)<=6.25).toList(),projection));out.put("spiralNearHilt2_5m",vertices(spirals.stream().filter(v->v.world.distanceToSqr(model.hilt)<=6.25).toList(),projection));out.put("allOrdinaryBounds",bounds(VERTICES.stream().filter(v->!v.spiral).toList()));out.put("allSpiralBounds",bounds(VERTICES.stream().filter(v->v.spiral).toList()));out.put("bloomSubmissionAttemptCalls",bloomCalls);out.put("bloomSubmissionAttemptVertices",bloomVertices);out.put("productionTrailDiagnostics",FlyingSwordEffects.trailDiagnostics());
            out.put("scope","Observation only: actual ControlledSwordRenderer input pose plus same baked-model transform/display context; true ordinary vertex input with actual pose/projection. No render forcing, camera hook, exposure change or acceptance threshold.");return out;
        }catch(Exception e){throw new AssertionError("Ordinary hilt observation failed",e);}
    }
    private static Map<String,Object> vertices(List<Sample> samples,Matrix4f projection){
        var sorted=samples.stream().sorted(Comparator.comparingDouble((Sample s)->s.world.subtract(model.hilt).dot(model.forward)).reversed()).toList();
        List<Object> head=new ArrayList<>();for(var s:sorted.subList(0,Math.min(48,sorted.size())))head.add(Map.of("world",vec(s.world),"pixel",project(s.world,s.pose,projection,model.eye),"forwardOfHilt",s.world.subtract(model.hilt).dot(model.forward),"u",s.u,"v",s.v,"alpha",s.alpha));
        return Map.of("vertices",samples.size(),"maximumForwardOfHilt",sorted.isEmpty()?0:sorted.get(0).world.subtract(model.hilt).dot(model.forward),"frontMostVertices",head);
    }
    private static Map<String,Object> bounds(List<Sample> samples){var bands=new TreeMap<Integer,Integer>();double maximum=-Double.MAX_VALUE;for(var s:samples){maximum=Math.max(maximum,s.world.subtract(model.hilt).dot(model.forward));bands.merge((int)Math.floor(s.v/2)*2,1,Integer::sum);}return Map.of("vertices",samples.size(),"maximumForwardOfHilt",samples.isEmpty()?0:maximum,"bandCounts",bands,"scope","All actual vertex inputs including alpha zero, after production clipping; not just visible raster pixels.");}
    private OrdinaryHiltQa(){}
}
