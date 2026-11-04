package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.lang.reflect.Method;
import java.util.*;

/** Observation-only frame/geometry probes. Never bundled into production. */
@Mod.EventBusSubscriber(modid="wuxingxiantu",value=Dist.CLIENT)
public final class FlightHeadProbe {
    private static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.huahongHeadFollowup");
    private static final int MAX_VERTICES=512;
    private static final List<Object> VERTICES=new ArrayList<>(),BRIDGES=new ArrayList<>(),PATHS=new ArrayList<>(),BILLBOARDS=new ArrayList<>();
    private static final Set<HuahongFlightPath> SEEN_PATHS=Collections.newSetFromMap(new IdentityHashMap<>());
    private static Frame frame;private static int ordinal,nearVertices,billboardOrdinal,activeBillboard=-1,billboardEligible,untaggedSmokeVertices,pathCalls,pathAttached;private static long serial;
    private static Matrix4f rawProjection;
    private static float pairX,pairY,pairZ,pairV;
    private static int corePairs,subpixelCorePairs;
    private static double minCorePixels,maxCorePixels;
    private record Frame(Vec3 center,Vec3 forward,Vec3 side,Vec3 up,Vec3 eye,Matrix4f pose,Matrix4f inversePose,Matrix4f clip,double now,float partial,String skin){}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void begin(TickEvent.RenderTickEvent e){if(ENABLED&&e.phase==TickEvent.Phase.START){serial++;frame=null;rawProjection=null;ordinal=nearVertices=billboardOrdinal=billboardEligible=untaggedSmokeVertices=pathCalls=pathAttached=0;activeBillboard=-1;VERTICES.clear();BRIDGES.clear();PATHS.clear();SEEN_PATHS.clear();BILLBOARDS.clear();}}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void world(RenderLevelStageEvent e){if(ENABLED&&e.getStage()==RenderLevelStageEvent.Stage.AFTER_PARTICLES&&!TrailShaderBridge.shadowPass())rawProjection=new Matrix4f(e.getProjectionMatrix());}
    public static VertexConsumer capture(VertexConsumer target,PoseStack.Pose pose,Vec3 eye,double now,float partial){
        if(!ENABLED||TrailShaderBridge.shadowPass())return target;var mc=Minecraft.getInstance();if(mc.player==null||!(mc.player.getVehicle() instanceof ControlledSwordEntity sword))return target;
        if(frame==null){var f=InterpolatedFlyingSwordTransform.get(sword,partial);Vec3 side=f.local(1,0,0).subtract(f.position()).normalize(),forward=f.forward();Matrix4f clip;
            try{var field=FlyingSwordEffects.class.getDeclaredField("projection");field.setAccessible(true);clip=new Matrix4f((Matrix4f)field.get(null));}catch(ReflectiveOperationException ex){throw new AssertionError(ex);}
            frame=new Frame(f.position().add(0,.85,0),forward,side,side.cross(forward).normalize(),eye,new Matrix4f(pose.pose()),new Matrix4f(pose.pose()).invert(),clip,now,partial,sword.huahongSkin().name());}
        return new VertexConsumer(){double x,y,z;float u,v;int rgba;
            public VertexConsumer vertex(double a,double b,double c){x=a;y=b;z=c;target.vertex(a,b,c);return this;}
            public VertexConsumer color(int r,int g,int b,int a){rgba=(r&255)|((g&255)<<8)|((b&255)<<16)|((a&255)<<24);target.color(r,g,b,a);return this;}
            public VertexConsumer uv(float a,float b){u=a;v=b;target.uv(a,b);return this;}
            public VertexConsumer overlayCoords(int a,int b){target.overlayCoords(a,b);return this;}
            public VertexConsumer uv2(int a,int b){target.uv2(a,b);return this;}
            public VertexConsumer normal(float a,float b,float c){target.normal(a,b,c);return this;}
            public void endVertex(){observeVertex((float)x,(float)y,(float)z,u,v,rgba);target.endVertex();}
            public void defaultColor(int r,int g,int b,int a){target.defaultColor(r,g,b,a);}
            public void unsetDefaultColor(){target.unsetDefaultColor();}
        };
    }
    private static void observeVertex(float x,float y,float z,float u,float v,int rgba){
        if(ordinal==0){corePairs=subpixelCorePairs=0;minCorePixels=Double.POSITIVE_INFINITY;maxCorePixels=0;}
        int index=ordinal++;if(frame==null)return;var relative=frame.inversePose.transformPosition(new Vector3f(x,y,z));Vec3 world=new Vec3(relative).add(frame.eye),local=world.subtract(frame.center);double lx=local.dot(frame.side),ly=local.dot(frame.up),lz=local.dot(frame.forward);
        if(index%4==0||index%4==2){pairX=x;pairY=y;pairZ=z;pairV=v;}
        else if((rgba>>>24)>=4&&lz>=-2.6&&lz<=4){
            int band=(int)Math.floor((Math.min(v,pairV)+.001)*.5)*2;
            if((band==8||band==22)&&Math.abs(v-pairV)>.01){
                var a=pixel(pairX,pairY,pairZ);var b=pixel(x,y,z);
                double pixels=Math.hypot(a.get(0)-b.get(0),a.get(1)-b.get(1))/Math.abs(v-pairV);
                if(Double.isFinite(pixels)){corePairs++;minCorePixels=Math.min(minCorePixels,pixels);maxCorePixels=Math.max(maxCorePixels,pixels);if(pixels<1)subpixelCorePairs++;}
            }
        }
        if(lz< -2.6||lz>4||Math.abs(lx)>4||Math.abs(ly)>4)return;nearVertices++;if(frame.skin.equals("SMOKE")&&activeBillboard<0)untaggedSmokeVertices++;if(VERTICES.size()>=MAX_VERTICES)return;
        var pixel=pixel(x,y,z);VERTICES.add(List.of(index,world.x,world.y,world.z,lx,ly,lz,pixel.get(0),pixel.get(1),pixel.get(2),u,v,Integer.toUnsignedLong(rgba),activeBillboard));
    }
    private static List<Double> pixel(float x,float y,float z){var c=frame.clip.transform(new Vector4f(x,y,z,1));if(!Float.isFinite(c.w)||Math.abs(c.w)<1e-8)return List.of(-99999D,-99999D,0D);var w=Minecraft.getInstance().getWindow();return List.of((c.x/c.w*.5+.5)*w.getWidth(),(.5-c.y/c.w*.5)*w.getHeight(),(double)c.w);}
    private static List<Double> vec(Vec3 p){return List.of(p.x,p.y,p.z);}
    private static List<Object> nodes(List<SwordFlightTrailHistory.Node> list){var result=new ArrayList<Object>();if(list.isEmpty())return result;double arc=list.get(list.size()-1).arc();for(int i=list.size()-1;i>=0&&result.size()<20;i--){var n=list.get(i);if(arc-n.arc()>9)break;result.add(List.of(arc-n.arc(),n.tick(),n.arc(),n.point().x(),n.point().y(),n.point().z()));}return result;}
    public static void bridge(List<SwordFlightTrailHistory.Node> raw,List<SwordFlightTrailHistory.Node> presented,Vec3 center,Vec3 forward,float weight){if(!ENABLED||TrailShaderBridge.shadowPass()||BRIDGES.size()>=6)return;BRIDGES.add(Map.of("raw",nodes(raw),"presentation",nodes(presented),"center",vec(center),"forward",vec(forward),"weight",weight,"sourceCount",raw.size(),"presentationCount",presented.size(),"sameList",raw==presented));}
    public static void path(HuahongFlightPath path,Vec3 center,Vec3 forward,Vec3 side,double cachedDistance,boolean attached,double weight){if(!ENABLED||TrailShaderBridge.shadowPass())return;pathCalls++;if(attached)pathAttached++;if(!attached||PATHS.size()>=8||!SEEN_PATHS.add(path))return;var sections=new ArrayList<Object>();for(double d:new double[]{0,1,2,2.5,5,8}){var f=path.behind(d);sections.add(Map.of("distance",d,"p",vec(f.p()),"forward",vec(f.forward()),"side",vec(f.side()),"up",vec(f.up())));}PATHS.add(Map.of("center",vec(center),"forward",vec(forward),"side",vec(side),"cachedHistoryDistance",cachedDistance,"weight",weight,"sections",sections));}
    private static Object value(Object row,String field)throws ReflectiveOperationException{Method m=row.getClass().getDeclaredMethod(field);m.setAccessible(true);return m.invoke(row);}
    public static void billboard(Object row,int mode){activeBillboard=-1;if(!ENABLED||frame==null||TrailShaderBridge.shadowPass())return;int index=billboardOrdinal++;try{Vec3 p=(Vec3)value(row,"p"),local=p.subtract(frame.center);double radius=((Number)value(row,"width")).doubleValue(),bound=radius*Math.sqrt(2);if(local.dot(frame.forward)+bound< -2.6||local.dot(frame.forward)-bound>4||Math.abs(local.dot(frame.side))-bound>4||Math.abs(local.dot(frame.up))-bound>4)return;billboardEligible++;if(BILLBOARDS.size()>=512)return;activeBillboard=index;BILLBOARDS.add(Map.of("submissionOrder",index,"world",vec(p),"seed",value(row,"u"),"radius",radius,"alpha",value(row,"alpha"),"color",value(row,"color"),"mode",mode,"eyeDistanceSquared",p.distanceToSqr(frame.eye)));}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    public static void endBillboard(){activeBillboard=-1;}
    public static Map<String,Object> snapshot(){var out=new LinkedHashMap<String,Object>();out.put("frameSerial",serial);out.put("observed",frame!=null);out.put("actualVertexCount",ordinal);out.put("nearHeadVertexCount",nearVertices);out.put("recordedVertexCount",VERTICES.size());out.put("vertexLimit",MAX_VERTICES);out.put("vertices",List.copyOf(VERTICES));out.put("vertexColumns",List.of("submissionIndex","worldX","worldY","worldZ","carrierLocalX","carrierLocalY","carrierLocalZ","screenX","screenY","clipW","u","v","rgba","billboardSubmissionOrder"));out.put("bridges",List.copyOf(BRIDGES));out.put("attachedPaths",List.copyOf(PATHS));out.put("billboards",List.copyOf(BILLBOARDS));out.put("capturedSourceEffectTime",FlyingSwordEffects.capturedEffectTime());
        out.put("completeHeadCoreRasterSupport",Map.of("quadEdges",corePairs,"subpixelEdges",subpixelCorePairs,"minPixels",corePairs==0?0:minCorePixels,"maxPixels",maxCorePixels,"scope","All submitted near-head band8/22 cross-sections with alpha>=4, before the 512 vertex detail limit; projected full strip width, not a visual brightness claim."));
        if(rawProjection!=null){out.put("rawProjectionM20",rawProjection.m20());out.put("rawProjectionM21",rawProjection.m21());out.put("rawProjection",rawProjection.get(new float[16]));}
        if(frame!=null){out.put("geometryNow",frame.now);out.put("geometryPartial",frame.partial);out.put("skin",frame.skin);out.put("center",vec(frame.center));out.put("eye",vec(frame.eye));out.put("eyeToHead",vec(frame.center.subtract(frame.eye)));out.put("forward",vec(frame.forward));out.put("side",vec(frame.side));out.put("up",vec(frame.up));out.put("worldToClip",frame.clip.get(new float[16]));var p=frame.center.subtract(frame.eye);var v=frame.pose.transformPosition(new Vector3f((float)p.x,(float)p.y,(float)p.z));out.put("headScreen",pixel(v.x,v.y,v.z));}
        out.put("pathConstructorCalls",pathCalls);out.put("pathConstructorAttachedTrue",pathAttached);out.put("billboardMetadataEligible",billboardEligible);out.put("billboardMetadataLimit",512);out.put("billboardMetadataTruncated",billboardEligible>BILLBOARDS.size());out.put("untaggedNearSmokeVertices",untaggedSmokeVertices);out.put("scope","Only actual production helper returns/constructor instances and submitted float vertices; no altered return, exposure, camera, positions, time or render forcing. Rows are in actual submit order; billboard seeds/order identify transparency changes.");return out;
    }
    private FlightHeadProbe(){}
}
