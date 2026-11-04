package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/** Observe the actual last submitted canonical history; never samples or rebuilds it. */
public final class FlightTickJoinProbe {
    private static Field trails,presentation,lastTick,previousNodes,interpolation,framePartial,frameSequence,rendering;
    private static Method anchor;
    private static Field field(Class<?> owner,String name)throws ReflectiveOperationException{
        var result=owner.getDeclaredField(name);result.setAccessible(true);return result;
    }
    private static void link()throws ReflectiveOperationException{
        if(trails!=null)return;
        trails=field(FlyingSwordEffects.class,"TRAILS");
        presentation=field(SwordFlightTrailHistory.class,"presentation");
        previousNodes=field(SwordFlightTrailHistory.class,"previousNodes");
        lastTick=field(SwordFlightTrailHistory.class,"lastTick");
        interpolation=field(ControlledSwordEntity.class,"flightInterpolation");
        framePartial=field(InterpolatedFlyingSwordTransform.class,"framePartial");frameSequence=field(InterpolatedFlyingSwordTransform.class,"frameSequence");rendering=field(InterpolatedFlyingSwordTransform.class,"rendering");
        anchor=FlyingSwordEffects.class.getDeclaredMethod("morphAnchor",ControlledSwordEntity.class,Vec3.class,float.class,float.class,float.class,float.class);anchor.setAccessible(true);
    }
    private static List<Double> vec(Vec3 v){return List.of(v.x,v.y,v.z);}
    private static Vec3 vec(SwordFlightTrailHistory.Point p){return new Vec3(p.x(),p.y(),p.z());}
    private static Map<String,Object> node(SwordFlightTrailHistory.Node n,Vec3 origin,Vec3 forward){
        return Map.of("point",vec(vec(n.point())),"tick",n.tick(),"arc",n.arc(),"forwardFromAttachment",vec(n.point()).subtract(origin).dot(forward),"distanceFromAttachment",vec(n.point()).distanceTo(origin));
    }
    private static Map<String,Object> list(List<SwordFlightTrailHistory.Node> nodes,Vec3 origin,Vec3 forward){
        if(nodes.isEmpty())return Map.of("count",0);
        var head=nodes.get(nodes.size()-1);var near=new ArrayList<Object>();
        int inversions=0;double maxNearForward=-Double.MAX_VALUE;
        for(int i=0;i<nodes.size();i++){
            var n=nodes.get(i);if(i>0&&n.arc()<nodes.get(i-1).arc()-1e-9)inversions++;
            if(head.arc()-n.arc()<=2){maxNearForward=Math.max(maxNearForward,vec(n.point()).subtract(origin).dot(forward));if(near.size()<20)near.add(node(n,origin,forward));}
        }
        var row=new LinkedHashMap<String,Object>();row.put("count",nodes.size());row.put("first",node(nodes.get(0),origin,forward));row.put("head",node(head,origin,forward));
        if(nodes.size()>1)row.put("penultimate",node(nodes.get(nodes.size()-2),origin,forward));
        row.put("arcSpan",head.arc()-nodes.get(0).arc());row.put("near2mMaxForward",maxNearForward);row.put("arcInversions",inversions);row.put("near2m",near);return row;
    }
    @SuppressWarnings("unchecked") public static Map<String,Object> snapshot(ControlledSwordEntity sword,float partial){
        try{
            link();var frame=InterpolatedFlyingSwordTransform.get(sword,partial);
            Vec3 attachment=(Vec3)anchor.invoke(null,sword,frame.position(),frame.yaw(),frame.pitch(),frame.roll(),frame.huahong());
            Vec3 current=(Vec3)anchor.invoke(null,sword,sword.position(),sword.getYRot(),sword.getXRot(),sword.renderRoll(1),InterpolatedFlyingSwordTransform.morphProgress(sword));
            Vec3 old=new Vec3(sword.xOld,sword.yOld,sword.zOld);
            var row=new LinkedHashMap<String,Object>();row.put("partialArgument",partial);row.put("sharedPartial",InterpolatedFlyingSwordTransform.partial(partial));row.put("clientFrameTime",Minecraft.getInstance().getFrameTime());row.put("entityTick",sword.tickCount);
            row.put("samplerTick",FlyingSwordEffects.samplerTick());row.put("capturedSamplerTick",FlyingSwordEffects.capturedTrailTick());row.put("legacyPromotedNow",FlyingSwordEffects.capturedTrailTick()-1+InterpolatedFlyingSwordTransform.partial(partial));row.put("expectedDoubleNow",(double)FlyingSwordEffects.capturedTrailTick()-1.0+(double)framePartial.getFloat(null));
            row.put("capturedFramePartial",framePartial.getFloat(null));row.put("frameSequence",frameSequence.getLong(null));row.put("renderingAtObservation",rendering.getBoolean(null));
            Double observed=FlightHeadProbe.observedGeometryNow();row.put("actualObservedNow",observed);
            if(Boolean.getBoolean("wuxingxiantu.flightTickJoinAssert")){
                double expected=(double)FlyingSwordEffects.capturedTrailTick()-1.0+(double)framePartial.getFloat(null);
                if(observed==null||Math.abs(observed-expected)>1e-6)throw new AssertionError("Tick join render-time mismatch: actual="+observed+" expected="+expected+" tick="+FlyingSwordEffects.capturedTrailTick()+" partial="+framePartial.getFloat(null));
            }
            row.put("entityOld",vec(old));row.put("entityCurrent",vec(sword.position()));row.put("renderPosition",vec(frame.position()));row.put("attachment",vec(attachment));row.put("currentTickAttachment",vec(current));row.put("currentMinusRender",sword.position().distanceTo(frame.position()));
            row.put("yawOld",sword.yRotO);row.put("yawCurrent",sword.getYRot());row.put("renderYaw",frame.yaw());row.put("renderPitch",frame.pitch());row.put("renderRoll",frame.roll());row.put("forward",vec(frame.forward()));
            row.put("camera",vec(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()));row.put("phase",sword.renderPhase().name());row.put("speed",frame.speed());row.put("sprintFactor",frame.sprintFactor());row.put("spiralFactor",frame.spiralFactor());row.put("morph",frame.huahong());row.put("morphSampler",InterpolatedFlyingSwordTransform.morphProgress(sword));
            Object playback=interpolation.get(sword);var type=playback.getClass();
            row.put("playbackTick",type.getMethod("playbackTick").invoke(playback));row.put("playbackRate",type.getMethod("clockRate").invoke(playback));row.put("playbackBuffering",type.getMethod("buffering").invoke(playback));
            var history=((Map<ControlledSwordEntity,SwordFlightTrailHistory>)trails.get(null)).get(sword);
            if(history!=null){
                var submitted=(List<SwordFlightTrailHistory.Node>)presentation.get(history);
                boolean emitted=FlyingSwordEffects.normalTrailVertexCount>0||HuahongVisuals.vertices>0;
                row.put("headAttachmentAssertionApplicable",emitted&&!submitted.isEmpty());
                if(!submitted.isEmpty())row.put("cachedHeadAttachmentError",vec(submitted.get(submitted.size()-1).point()).distanceTo(attachment));
                // A pre-flight frame can have no generated strip while keeping
                // the previous presentation list. Do not label that cache as a
                // detached submitted head; every nonempty visible draw is checked.
                if(Boolean.getBoolean("wuxingxiantu.flightTickJoinAssert")&&emitted&&!submitted.isEmpty()){
                    double error=vec(submitted.get(submitted.size()-1).point()).distanceTo(attachment);
                    if(error>1e-6)throw new AssertionError("Tick join head detached from shared attachment: "+error);
                }
                row.put("lastHistorySampleTick",lastTick.getDouble(history));row.put("fixedHistory",list(history.nodes(),attachment,frame.forward()));row.put("lastSubmittedHistory",list(submitted,attachment,frame.forward()));row.put("previousHistory",list((List<SwordFlightTrailHistory.Node>)previousNodes.get(history),attachment,frame.forward()));}
            row.put("ordinaryVertices",FlyingSwordEffects.normalTrailVertexCount-HuahongVisuals.vertices);row.put("rainbowVertices",HuahongVisuals.vertices);row.put("renderedSegments",FlyingSwordEffects.renderedSegments);
            row.put("scope","Read-only actual entity/shared frame/fixed sample/last submitted canonical nodes. No renderNodes/sample call, pose change, clock change or extra geometry draw.");return row;
        }catch(ReflectiveOperationException error){throw new AssertionError("Tick join telemetry failed",error);}
    }
    private FlightTickJoinProbe(){}
}
