package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import cn.blockforge.generated.generatedmod.yujian.FlyingSwordConfig;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Per-frame samples of the real carrier and real production history; never in release artifacts. */
final class HuahongMotionQa {
    private static final List<Object> FRAMES=new ArrayList<>();
    private static final Set<String> SKINS=new HashSet<>();
    private static final Map<String,Coverage> COVERAGE=new LinkedHashMap<>();
    private static final Coverage TOTAL=new Coverage();
    private static final double[] DISTANCES={0,.25,.75,1.5,3};
    private static Vec3 previousNose,previousForward;
    private static List<Vec3> previousLocal;
    private static String previousSkin;
    private static long previousTick=Long.MIN_VALUE;
    private static double previousTime=Double.NaN,maxNoseStep,maxHeadingStep,maxShapeStep,maxAnchorError;
    private static int tickBoundaries;

    private static final class Coverage {
        int frames,firstEntityTick,lastEntityTick;long firstSampler,lastSampler;
        double minX=Double.POSITIVE_INFINITY,minY=Double.POSITIVE_INFINITY,minZ=Double.POSITIVE_INFINITY;
        double maxX=Double.NEGATIVE_INFINITY,maxY=Double.NEGATIVE_INFINITY,maxZ=Double.NEGATIVE_INFINITY;
        float previousYaw;double yawTravel;
        void sample(Vec3 center,float yaw,int entityTick,long sampler){
            if(frames++==0){firstEntityTick=entityTick;firstSampler=sampler;}
            else yawTravel+=Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw-previousYaw));
            previousYaw=yaw;lastEntityTick=entityTick;lastSampler=sampler;
            minX=Math.min(minX,center.x);minY=Math.min(minY,center.y);minZ=Math.min(minZ,center.z);
            maxX=Math.max(maxX,center.x);maxY=Math.max(maxY,center.y);maxZ=Math.max(maxZ,center.z);
        }
        double positionRange(){return frames==0?0:Math.sqrt((maxX-minX)*(maxX-minX)+(maxY-minY)*(maxY-minY)+(maxZ-minZ)*(maxZ-minZ));}
        long samplerGrowth(){return lastSampler-firstSampler;}
        int entityGrowth(){return lastEntityTick-firstEntityTick;}
        Map<String,Object> summary(){return Map.of("frames",frames,"positionRange",positionRange(),"yawTravelDegrees",yawTravel,"entityTickGrowth",entityGrowth(),"samplerTickGrowth",samplerGrowth());}
        void verify(String label,double minRange,double minYaw){
            if(frames<8||samplerGrowth()<20||entityGrowth()<samplerGrowth()*.8||positionRange()<minRange||yawTravel<minYaw)
                throw new AssertionError("Flight did not really advance ("+label+"): "+summary());
        }
    }

    static void frame(Minecraft mc,int qaTick,float partial)throws Exception{
        if(mc.player==null||!(mc.player.getVehicle() instanceof ControlledSwordEntity sword)
                ||sword.huahongStage()!=2||sword.renderHuahong(partial)<.995F||HuahongVisuals.vertices==0)return;
        @SuppressWarnings("unchecked") var trails=(Map<ControlledSwordEntity,?>)field(HuahongVisuals.class,null,"TRAILS");
        Object trail=trails.get(sword);if(trail==null)throw new AssertionError("Rendered huahong has no production history");
        var history=(SwordFlightTrailHistory)field(trail.getClass(),trail,"history");
        var transform=InterpolatedFlyingSwordTransform.get(sword,partial);
        Vec3 center=transform.position().add(0,.85,0),forward=transform.forward();
        long sampler=FlyingSwordEffects.capturedTrailTick();double now=sampler-1+partial;
        var nodes=history.renderNodes(now,new SwordFlightTrailHistory.Point(center.x,center.y,center.z));
        Vec3 side=transform.local(1,0,0).subtract(transform.position());
        var path=new HuahongFlightPath(nodes,center,forward,side);
        Vec3 nose=path.behind(0).p().add(forward.scale(.26));
        double anchorError=path.behind(0).p().distanceTo(center);maxAnchorError=Math.max(maxAnchorError,anchorError);
        if(anchorError>1e-8||!Double.isFinite(nose.lengthSqr()))throw new AssertionError("Nonfinite/detached huahong nose");
        String skin=sword.huahongSkin().name();SKINS.add(skin);
        TOTAL.sample(center,transform.yaw(),sword.tickCount,sampler);
        COVERAGE.computeIfAbsent(skin,key->new Coverage()).sample(center,transform.yaw(),sword.tickCount,sampler);
        List<Object> sections=new ArrayList<>();List<Vec3> local=new ArrayList<>();
        Vec3 up=side.cross(forward).normalize();
        for(double distance:DISTANCES){
            var section=path.behind(distance);Vec3 offset=section.p().subtract(center);
            Vec3 relative=new Vec3(offset.dot(side),offset.dot(up),offset.dot(forward));local.add(relative);
            sections.add(Map.of("distance",distance,"world",vec(section.p()),"local",vec(relative),
                    "forward",vec(section.forward()),"side",vec(section.side())));
        }
        double noseStep=0,headingStep=0,shapeStep=0;
        boolean comparable=skin.equals(previousSkin)&&Double.isFinite(previousTime)&&now>previousTime&&now-previousTime<1.5;
        if(comparable){
            noseStep=nose.distanceTo(previousNose);
            headingStep=Math.toDegrees(Math.acos(Math.max(-1,Math.min(1,forward.dot(previousForward)))));
            for(int i=0;i<local.size();i++)shapeStep=Math.max(shapeStep,local.get(i).distanceTo(previousLocal.get(i)));
            maxNoseStep=Math.max(maxNoseStep,noseStep);maxHeadingStep=Math.max(maxHeadingStep,headingStep);maxShapeStep=Math.max(maxShapeStep,shapeStep);
            if(previousTick!=sampler)tickBoundaries++;
        }
        int gl=org.lwjgl.opengl.GL11.glGetError();if(gl!=0)throw new AssertionError("Motion GL error "+gl);
        var row=new LinkedHashMap<String,Object>();
        row.put("frame",FRAMES.size());row.put("nanoTime",System.nanoTime());row.put("qaTick",qaTick);row.put("samplerTick",sampler);row.put("partial",partial);row.put("time",now);
        row.put("skin",skin);row.put("speed",sword.flightSpeed());row.put("speedMultiplier",FlyingSwordConfig.SPEED_MULTIPLIER.get());
        row.put("entityTick",sword.tickCount);row.put("rawPosition",vec(sword.position()));row.put("oldPosition",List.of(sword.xOld,sword.yOld,sword.zOld));
        row.put("clientLevelTick",mc.level.getGameTime());row.put("hasChunkAt",mc.level.hasChunkAt(sword.blockPosition()));
        row.put("chunkSourceHasChunk",mc.level.getChunkSource().hasChunk(sword.getBlockX()>>4,sword.getBlockZ()>>4));
        row.put("center",vec(center));row.put("nose",vec(nose));row.put("forward",vec(forward));
        row.put("rotation",List.of(transform.yaw(),transform.pitch(),transform.roll()));row.put("sections",sections);
        row.put("comparable",comparable);row.put("noseStep",noseStep);row.put("headingStepDegrees",headingStep);row.put("localShapeStep",shapeStep);
        row.put("vertices",HuahongVisuals.vertices);row.put("shaderPack",TrailShaderBridge.shaderPackDetected());
        row.put("renderPath",FlyingSwordEffects.activeTrailRenderPath);row.put("glError",gl);FRAMES.add(row);
        previousNose=nose;previousForward=forward;previousLocal=local;previousSkin=skin;previousTick=sampler;previousTime=now;
    }
    static void verify(){
        if(FRAMES.size()<60||tickBoundaries<20||SKINS.size()<8)
            throw new AssertionError("Incomplete full-speed motion coverage: frames="+FRAMES.size()+", boundaries="+tickBoundaries+", skins="+SKINS);
        TOTAL.verify("all skins",120,100);
        for(var entry:COVERAGE.entrySet())entry.getValue().verify(entry.getKey(),40,30);
    }
    static void write(Path out)throws Exception{
        Files.writeString(out.resolve("motion-metrics.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of(
                "frames",FRAMES,"skins",SKINS,"tickBoundaries",tickBoundaries,"maxNoseStep",maxNoseStep,
                "maxHeadingStepDegrees",maxHeadingStep,"maxLocalShapeStep",maxShapeStep,"maxAnchorError",maxAnchorError,
                "realFlightCoverage",TOTAL.summary(),"perSkinCoverage",COVERAGE.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,e->e.getValue().summary())))));
    }
    private static Object field(Class<?> type,Object object,String name)throws Exception{var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(object);}
    private static List<Double> vec(Vec3 v){return List.of(v.x,v.y,v.z);}
    private HuahongMotionQa(){}
}
