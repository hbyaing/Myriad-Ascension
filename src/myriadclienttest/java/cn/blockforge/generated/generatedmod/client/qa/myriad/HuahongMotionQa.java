package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import cn.blockforge.generated.generatedmod.yujian.FlyingSwordConfig;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.*;
import java.util.zip.GZIPOutputStream;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Per-frame samples of the real carrier and real production history; never in release artifacts. */
public final class HuahongMotionQa {
    private static long renderSerial;
    private static MeshFrame submittedMesh;
    private static Map<String,Vec3> previousCore=Map.of(),previousWorld=Map.of();
    private static String previousMeshSkin="";
    private static double previousMeshTime;
    private static List<SubmittedVertex> previousSubmitted=List.of();
    private static MeshFrame previousMesh;
    private static final Set<Object> WORLD_ROWS=Collections.newSetFromMap(new IdentityHashMap<>());
    private static int stripOrdinal,activeStrip=-1,activeVertex,activeSpace;
    static void beginRender(){renderSerial++;submittedMesh=null;WORLD_ROWS.clear();stripOrdinal=0;activeStrip=-1;activeVertex=0;activeSpace=0;}
    public static void tagWorldRows(Object rows){if(HuahongDynamicsQa.ENABLED)WORLD_ROWS.add(rows);}
    public static void tagDerivedRows(Object source,Object result){if(WORLD_ROWS.contains(source))WORLD_ROWS.add(result);}
    public static void beginGeometryStrip(Object rows){if(submittedMesh==null)return;activeStrip=stripOrdinal++;activeVertex=0;activeSpace=submittedMesh.skin.equals("FAN")||WORLD_ROWS.contains(rows)?2:1;}
    public static void beginPrismStrip(boolean local){if(submittedMesh==null)return;activeStrip=stripOrdinal++;activeVertex=0;activeSpace=local?1:2;}
    public static void endStrip(){activeStrip=-1;activeVertex=0;activeSpace=0;}
    record VertexTag(int space,int strip,int index){}
    record SubmittedVertex(float x,float y,float z,float u,float v,int rgba,float localX,float localY,float localZ){}
    static final class MeshFrame {
        final long serial=renderSerial;
        final double time;
        final float partial;
        final double exposure;
        final long scenarioClock=HuahongDynamicsQa.tick();
        final int entityId;
        final int stage;
        final float strength;
        final String skin;
        final Vec3 eye,center,forward,side,up;
        final Matrix4f inversePose;
        final float[] poseMatrix=new float[16],projectionMatrix=new float[16];
        final ArrayList<SubmittedVertex> vertices=new ArrayList<>();
        final ArrayList<VertexTag> tags=new ArrayList<>();
        int drawCalls=1;
        MeshFrame(PoseStack.Pose pose,Vec3 eye,double now,float partial,ControlledSwordEntity sword,double exposure){
            this.exposure=exposure;
            stage=sword.huahongStage();strength=sword.renderHuahong(partial);
            this.eye=eye;time=now;this.partial=partial;entityId=sword.getId();skin=sword.huahongSkin().name();
            var transform=InterpolatedFlyingSwordTransform.get(sword,partial);center=transform.position().add(0,.85,0);forward=transform.forward();
            side=transform.local(1,0,0).subtract(transform.position());up=side.cross(forward).normalize();inversePose=new Matrix4f(pose.pose()).invert();
            pose.pose().get(poseMatrix);
            try{((Matrix4f)field(FlyingSwordEffects.class,null,"projection")).get(projectionMatrix);}catch(Exception error){throw new AssertionError("Actual renderer projection is unavailable",error);}
        }
        void append(double x,double y,double z,float u,float v,int rgba){
            var relative=inversePose.transformPosition(new Vector3f((float)x,(float)y,(float)z));
            Vec3 local=new Vec3(relative).add(eye).subtract(center);
            vertices.add(new SubmittedVertex((float)x,(float)y,(float)z,u,v,rgba,(float)local.dot(side),(float)local.dot(up),(float)local.dot(forward)));
            if(activeSpace!=0)tags.add(new VertexTag(skin.equals("METEOR_PRISM")&&u>=0?2:activeSpace,activeStrip,activeVertex++));
            else if(skin.equals("METEOR_PRISM")&&v>=32&&v<=33)tags.add(new VertexTag(1,-32,(u>.5?1:0)+(v>32.5?2:0)));
            else tags.add(new VertexTag(skin.equals("DENSE")?1:2,-1,vertices.size()-1));
        }
        Map<String,Object> continuity(){
            Map<String,Vec3> core=new LinkedHashMap<>(),world=new LinkedHashMap<>();
            int worldVertices=0;
            for(int index=0;index<vertices.size();index++){var vertex=vertices.get(index);var tag=tags.get(index);
                Vec3 p=new Vec3(vertex.localX(),vertex.localY(),vertex.localZ());
                if(p.lengthSqr()>64)continue;if(tag.space()==2){
                    worldVertices++;
                    String worldKey=tag.strip()+":"+Integer.toHexString(Float.floatToIntBits(vertex.u()))+":"+Integer.toHexString(Float.floatToIntBits(vertex.v()))+":"+Integer.toHexString(vertex.rgba()&0xffffff);
                    Vec3 worldPosition=center.add(side.scale(p.x)).add(up.scale(p.y)).add(forward.scale(p.z));
                    world.put(worldKey,worldPosition);continue;
                }
                if(skin.equals("NEEDLE")&&vertex.u()>=0)continue;
                String key=tag.strip()+":"+tag.index()+":"+Integer.toHexString(Float.floatToIntBits(vertex.u()))+":"+Integer.toHexString(Float.floatToIntBits(vertex.v()))+":"+Integer.toHexString(vertex.rgba()&0xffffff);
                core.put(key,p);
            }
            ArrayList<Double> steps=new ArrayList<>();double maximum=0;
            boolean comparable=previousMesh!=null&&skin.equals(previousMeshSkin)&&entityId==previousMesh.entityId&&stage==2&&previousMesh.stage==2&&strength>=.995F&&previousMesh.strength>=.995F&&time>previousMeshTime&&time-previousMeshTime<=1;
            if(comparable)for(var entry:core.entrySet()){
                Vec3 old=previousCore.get(entry.getKey());if(old==null)continue;
                double step=old.distanceTo(entry.getValue());maximum=Math.max(maximum,step);steps.add(step);
            }
            ArrayList<Double> worldSteps=new ArrayList<>();double maxWorldStep=0;
            if(comparable)for(var entry:world.entrySet()){var old=previousWorld.get(entry.getKey());if(old!=null){double step=old.distanceTo(entry.getValue());worldSteps.add(step);maxWorldStep=Math.max(maxWorldStep,step);}}
            worldSteps.sort(Double::compare);
            steps.sort(Double::compare);double p99=steps.isEmpty()?0:steps.get(Math.min(steps.size()-1,(int)(steps.size()*.99)));
            int sameIndex=0;double sameIndexStep=0;
            if(comparable&&vertices.size()==previousSubmitted.size())for(int i=0;i<vertices.size();i++){
                var a=vertices.get(i);var b=previousSubmitted.get(i);
                if(tags.get(i).space()!=1||previousMesh.tags.get(i).space()!=1||!tags.get(i).equals(previousMesh.tags.get(i))||Float.floatToIntBits(a.u())!=Float.floatToIntBits(b.u())||Float.floatToIntBits(a.v())!=Float.floatToIntBits(b.v()))continue;
                Vec3 p=new Vec3(a.localX(),a.localY(),a.localZ());if(p.lengthSqr()>64)continue;
                sameIndex++;sameIndexStep=Math.max(sameIndexStep,p.distanceTo(new Vec3(b.localX(),b.localY(),b.localZ())));
            }
            var row=new LinkedHashMap<String,Object>();row.put("vertices",vertices.size());row.put("headVertices",core.size());row.put("matchedHeadVertices",steps.size());
            row.put("worldHistoryNearVertices",worldVertices);row.put("matchedWorldHistoryVertices",worldSteps.size());row.put("maxWorldHistoryVertexStep",maxWorldStep);row.put("p99WorldHistoryVertexStep",worldSteps.isEmpty()?0:worldSteps.get(Math.min(worldSteps.size()-1,(int)(worldSteps.size()*.99))));row.put("vertexSpaceEvidence","QA-only production history-list provenance + per-strip original vertex index; FAN world, PRISM local path/32, NEEDLE negative-u, full-strength local pairs only.");
            row.put("sameIndexHeadVertices",sameIndex);row.put("maxSameIndexFinalHeadStep",sameIndexStep);row.put("topologyStable",vertices.size()==previousSubmitted.size());
            row.put("maxFinalHeadVertexStep",maximum);row.put("p99FinalHeadVertexStep",p99);row.put("comparable",comparable);row.put("renderDelta",comparable?time-previousMeshTime:0);
            row.put("headMeshFile",String.format(Locale.ROOT,"mesh/head-%07d.bin",serial));row.put("drawCalls",drawCalls);
            previousCore=core;previousWorld=world;previousMeshSkin=skin;previousMeshTime=time;previousSubmitted=vertices;previousMesh=this;return row;
        }
        double[] projected(SubmittedVertex vertex){
            var clip=new Matrix4f().set(projectionMatrix).transform(new Vector4f(vertex.x(),vertex.y(),vertex.z(),1));
            if(!Float.isFinite(clip.w)||Math.abs(clip.w)<1e-8)return new double[]{Double.NaN,Double.NaN};
            var window=Minecraft.getInstance().getWindow();return new double[]{(clip.x/clip.w*.5+.5)*window.getWidth(),(.5-clip.y/clip.w*.5)*window.getHeight()};
        }
        List<Map<String,Object>> projectedHeadWidths(){
            List<Map<String,Object>> widths=new ArrayList<>();
            for(int quad=0;quad+3<vertices.size();quad+=4)for(int pair=0;pair<2;pair++){
                int ia=quad+(pair==0?0:3),ib=quad+(pair==0?1:2);var a=vertices.get(ia);var b=vertices.get(ib);
                if(a.u()<-.4||a.u()>.00001||a.localZ()<-1||a.localZ()>2||(a.rgba()>>>24)==0)continue;
                double[] pa=projected(a),pb=projected(b);if(!Double.isFinite(pa[0])||!Double.isFinite(pb[0]))continue;
                widths.add(Map.of("vertexIndex",ia,"u",a.u(),"band",Math.floor((Math.min(a.v(),b.v())+.001)*.5)*2,"pixelWidth",Math.hypot(pa[0]-pb[0],pa[1]-pb[1]),
                        "worldWidth",new Vec3(a.localX(),a.localY(),a.localZ()).distanceTo(new Vec3(b.localX(),b.localY(),b.localZ())),
                        "localZ",(a.localZ()+b.localZ())*.5,"pixelCenter",List.of((pa[0]+pb[0])*.5,(pa[1]+pb[1])*.5),"submittedAlpha",a.rgba()>>>24));
            }
            widths.sort(Comparator.comparingDouble((Map<String,Object> r)->((Number)r.get("localZ")).doubleValue()).reversed());
            return widths.subList(0,Math.min(24,widths.size()));
        }
        void write(Path root,boolean full)throws IOException{
            Path directory=root.resolve("mesh");Files.createDirectories(directory);
            int count=full?vertices.size():0;if(!full)for(var v:vertices)if(v.localX()*v.localX()+v.localY()*v.localY()+v.localZ()*v.localZ()<=64)count++;
            try(var stream=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(directory.resolve(String.format(Locale.ROOT,full?"full-%07d.bin":"head-%07d.bin",serial)))))){
                stream.writeInt(0x484d5133);stream.writeLong(serial);stream.writeDouble(time);stream.writeFloat(partial);stream.writeInt(entityId);stream.writeUTF(skin);stream.writeBoolean(full);stream.writeInt(vertices.size());stream.writeInt(count);
                for(int index=0;index<vertices.size();index++){var v=vertices.get(index);if(!full&&v.localX()*v.localX()+v.localY()*v.localY()+v.localZ()*v.localZ()>64)continue;
                    stream.writeInt(index);var tag=tags.get(index);stream.writeByte(tag.space());stream.writeInt(tag.strip());stream.writeInt(tag.index());stream.writeFloat(v.x());stream.writeFloat(v.y());stream.writeFloat(v.z());stream.writeFloat(v.u());stream.writeFloat(v.v());stream.writeInt(v.rgba());stream.writeFloat(v.localX());stream.writeFloat(v.localY());stream.writeFloat(v.localZ());}
            }
        }
    }
    static MeshFrame submittedMesh(){return submittedMesh;}
    static Map<String,Object> verifyNoseProjection(Minecraft mc)throws Exception{
        var method=HuahongVisuals.class.getDeclaredMethod("noseGlow",VertexConsumer.class,PoseStack.Pose.class,Vec3.class,Vec3.class,Vec3.class,float.class);method.setAccessible(true);
        var rotation=mc.gameRenderer.getMainCamera().rotation();
        Vec3 look=new Vec3(new Vector3f(0,0,-1).rotate(rotation)),right=new Vec3(new Vector3f(1,0,0).rotate(rotation)),up=new Vec3(new Vector3f(0,1,0).rotate(rotation));
        var points=new ArrayList<Vec3>();
        VertexConsumer collector=new VertexConsumer(){
            public VertexConsumer vertex(double x,double y,double z){points.add(new Vec3(x,y,z));return this;}
            public VertexConsumer color(int r,int g,int b,int a){return this;}
            public VertexConsumer uv(float u,float v){return this;}
            public VertexConsumer overlayCoords(int u,int v){return this;}
            public VertexConsumer uv2(int u,int v){return this;}
            public VertexConsumer normal(float x,float y,float z){return this;}
            public void endVertex(){}
            public void defaultColor(int r,int g,int b,int a){}
            public void unsetDefaultColor(){}
        };
        int oldVertices=HuahongVisuals.vertices,checks=0;double maxCornerStep=0;
        try{
            for(int axis=0;axis<3;axis++){
                List<Vec3> previous=null;
                for(int step=0;step<=2000;step++){
                    double theta=(step-1000)*1e-5;
                    Vec3 direction=axis<2?look.scale(Math.cos(theta)).add((axis==0?right:up).scale(Math.sin(theta))):
                            look.scale(Math.cos(1e-4)).add(right.scale(Math.cos(step*Math.PI/1000)*Math.sin(1e-4))).add(up.scale(Math.sin(step*Math.PI/1000)*Math.sin(1e-4)));
                    points.clear();method.invoke(null,collector,new PoseStack().last(),Vec3.ZERO,look.scale(10),direction,.8F);
                    if(points.size()!=4)throw new AssertionError("Projected nucleus did not submit four final corners");
                    for(int i=0;i<4;i++){
                        if(!Double.isFinite(points.get(i).lengthSqr()))throw new AssertionError("Nonfinite nucleus corner at frontal view");
                        if(previous!=null){double delta=points.get(i).distanceTo(previous.get(i));maxCornerStep=Math.max(maxCornerStep,delta);if(delta>.001)throw new AssertionError("Nucleus projected frame flipped facing camera: "+delta);}
                    }
                    previous=List.copyOf(points);checks++;
                }
            }
        }finally{HuahongVisuals.vertices=oldVertices;}
        return Map.of("passed",true,"checks",checks,"maxFinalCornerStep",maxCornerStep,"sample","Actual production noseGlow corners across frontal/axial directions");
    }
    public static VertexConsumer captureBuffer(VertexConsumer delegate,PoseStack.Pose pose,Vec3 eye,double now,float partial){
        if(!HuahongDynamicsQa.ENABLED||!HuahongDynamicsQa.started())return delegate;
        var mc=Minecraft.getInstance();if(mc.player==null||!(mc.player.getVehicle() instanceof ControlledSwordEntity sword))return delegate;
        boolean collect=submittedMesh==null;double exposure=HuahongDynamicsQa.exposure(renderSerial);
        HuahongVolumeRenderer.setExposure((float)exposure);
        if(collect)submittedMesh=new MeshFrame(pose,eye,now,partial,sword,exposure);else submittedMesh.drawCalls++;
        MeshFrame frame=submittedMesh;
        return new VertexConsumer(){
            double x,y,z;float u,v;int rgba;
            public VertexConsumer vertex(double a,double b,double c){x=a;y=b;z=c;delegate.vertex(a,b,c);return this;}
            public VertexConsumer color(int r,int g,int b,int alpha){int a=(int)Math.round(alpha*exposure);rgba=(a<<24)|(r<<16)|(g<<8)|b;delegate.color(r,g,b,a);return this;}
            public VertexConsumer uv(float a,float b){u=a;v=b;delegate.uv(a,b);return this;}
            public VertexConsumer overlayCoords(int a,int b){delegate.overlayCoords(a,b);return this;}
            public VertexConsumer uv2(int a,int b){delegate.uv2(a,b);return this;}
            public VertexConsumer normal(float a,float b,float c){delegate.normal(a,b,c);return this;}
            public void endVertex(){if(collect)frame.append(x,y,z,u,v,rgba);delegate.endVertex();}
            public void defaultColor(int r,int g,int b,int a){delegate.defaultColor(r,g,b,a);}
            public void unsetDefaultColor(){delegate.unsetDefaultColor();}
        };
    }
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
