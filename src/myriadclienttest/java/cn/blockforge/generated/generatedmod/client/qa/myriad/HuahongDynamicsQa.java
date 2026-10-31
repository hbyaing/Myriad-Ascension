package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.*;
import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.network.packet.yujian.SwordFlightInputPacket;
import cn.blockforge.generated.generatedmod.yujian.*;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Real-time, one-times-speed maneuver fixture; all diagnostic code is test-only. */
final class HuahongDynamicsQa {
    static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.huahongDynamics");
    private static final HuahongSkin[] SKINS=skins();
    private static final int CYCLE=280;
    private static final List<Map<String,Object>> FRAMES=new ArrayList<>(),SERVER=new ArrayList<>();
    private static final List<String> FAILURES=Collections.synchronizedList(new ArrayList<>());
    private static final ExecutorService WRITER=Executors.newFixedThreadPool(4,r->{var t=new Thread(r,"huahong-qa-mesh");t.setDaemon(true);return t;});
    private static final ExecutorService IMAGE_WRITER=Executors.newFixedThreadPool(2,r->{var t=new Thread(r,"huahong-qa-images");t.setDaemon(true);return t;});
    private static final ScheduledExecutorService CONTROL_SENDER=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"huahong-qa-input-heartbeat");t.setDaemon(true);return t;});
    private static final AtomicInteger BACKLOG=new AtomicInteger();
    private static final AtomicInteger IMAGE_BACKLOG=new AtomicInteger(),SEND_ERRORS=new AtomicInteger();
    private static final AtomicLong SEND_COUNT=new AtomicLong();
    private static final AtomicBoolean summoned=new AtomicBoolean();
    private static final Map<String,Integer> UTURN_IMAGES=new HashMap<>();
    private static final Set<String> PREVIEWS=new HashSet<>();
    private static volatile boolean started;
    private static volatile long clock;
    private static long epoch=-1;
    private static long lastServerWorldTick=Long.MIN_VALUE;
    private static volatile UUID playerId;
    private static Path output;
    private static CompletableFuture<?> warm;
    private static long lastWarm=-20;
    private static int chosen=-1,geometryJumps,droppedImages,fullMeshFrames,maxMeshBacklog,maxImageBacklog;
    private static String previousArtifactSegment="";
    private static int supplementedWorldTicks;
    private static BeforeTick beforeTick;
    private record BeforeTick(long world,long lastServerTick,int entityTick,int playerTick,int entityId){}
    private static Vec3 cameraOffset=new Vec3(5,2,-5);
    private static long cameraNanos;

    record Control(int skin,int phase,float yaw,float pitch,boolean forward,boolean sprint,boolean brake,String segment){}
    private static HuahongSkin[] skins(){
        String value=System.getProperty("wuxingxiantu.huahongDynamicsSkin","METEOR_PRISM");
        if(value.equalsIgnoreCase("all"))return HuahongSkin.values();
        return Arrays.stream(value.split(",")).map(String::trim).map(s->HuahongSkin.valueOf(s.toUpperCase(Locale.ROOT))).distinct().toArray(HuahongSkin[]::new);
    }
    static boolean started(){return started;}
    static int tick(){return (int)clock;}
    static double exposure(long frame){
        String value=System.getProperty("wuxingxiantu.huahongQaExposure","cycle");
        int phase=control().phase();boolean normalSequence=SKINS[control().skin()]==HuahongSkin.METEOR_PRISM&&phase>=180&&phase<210&&UTURN_IMAGES.getOrDefault(SKINS[control().skin()].name(),0)<12||phase>=84&&phase<90||phase>=224&&phase<230||phase>=244&&phase<250;
        double exposure=value.equalsIgnoreCase("cycle")?(normalSequence?1:new double[]{1,.25,.0625}[(int)Math.floorMod(frame,3)]):Double.parseDouble(value);
        if(exposure<=0||exposure>1||!Double.isFinite(exposure))throw new IllegalArgumentException("Invalid QA exposure "+value);
        return exposure;
    }
    static Control control(){
        return control(clock);
    }
    private static Control control(long scenarioClock){
        int skin=Math.min(SKINS.length-1,(int)(scenarioClock/CYCLE)),phase=(int)(scenarioClock%CYCLE);float base=skin*180;
        float yaw=phase<110?base:phase<140?base-90:phase<170?base:base+180;
        boolean brake=scenarioClock>=SKINS.length*CYCLE||phase>=250;
        boolean coast=phase>=230&&phase<236;
        boolean releaseSprint=phase>=210&&phase<216;
        boolean forward=!brake&&!coast&&(scenarioClock>=20);
        String segment=phase<80?"accelerate":phase<110?"straight":phase<140?"left-90":phase<170?"right-90":phase<210?"uturn-180":phase<216?"release-sprint":phase<230?"recover-sprint":phase<236?"release-forward":phase<250?"recover-forward":"brake-exit";
        return new Control(skin,phase,yaw,0,forward,forward&&!releaseSprint,brake,segment);
    }
    static void input(){
        // Dynamics uses an explicitly ordered server-tick controller fixture.
        // Do not enqueue stale C2S plans behind world generation/catch-up ticks.
    }
    static boolean tick(Minecraft mc,Path out)throws Exception{
        if(!started){
            output=out;playerId=mc.player.getUUID();runtimeEvidence(out);started=true;
            mc.options.framerateLimit().set(30);HuahongSkinPreferences.select(SKINS[0]);chosen=0;
            FlyingSwordConfig.TRAIL_PATH.set(Boolean.getBoolean("wuxingxiantu.myriadExternalShaders")?FlyingSwordConfig.TrailPath.AUTO:FlyingSwordConfig.TrailPath.NORMAL_TRAIL_RENDER_PATH);
        }
        input();var c=control();
        if(chosen!=c.skin()){chosen=c.skin();HuahongSkinPreferences.select(SKINS[chosen]);}
        return clock>=SKINS.length*CYCLE;
    }
    static void beforeServerLevelTick(net.minecraft.server.level.ServerLevel level){
        if(!started||playerId==null)return;var player=level.getServer().getPlayerList().getPlayer(playerId);
        if(player!=null&&player.serverLevel()==level&&player.getVehicle() instanceof ControlledSwordEntity sword)
            beforeTick=new BeforeTick(level.getGameTime(),lastServerTick(sword),sword.tickCount,player.tickCount,sword.getId());
    }
    static void inputServerLevelTick(net.minecraft.server.level.ServerLevel level){
        if(!started||playerId==null)return;var player=level.getServer().getPlayerList().getPlayer(playerId);
        if(player==null||player.serverLevel()!=level)return;
        long nextWorldTick=level.getGameTime()+1;if(epoch<0)epoch=nextWorldTick;
        long nextClock=nextWorldTick-epoch;var c=control(nextClock);
        YujianController.handleInput(player,nextClock<20,!summoned.getAndSet(true),c.forward(),c.brake(),false,false,false,false,c.sprint(),false,1,0,c.yaw(),c.pitch());
        SEND_COUNT.incrementAndGet();
        if(player.getVehicle() instanceof ControlledSwordEntity sword)refreshNearCorridor(player,sword,c);
        if(nextClock-lastWarm>=10&&player.getVehicle() instanceof ControlledSwordEntity sword){
            lastWarm=nextClock;warmFlightRoute(player,sword,nextClock);
        }
    }
    static void prepareWorld(ServerPlayer player){
        var def=FlyingSwordRegistry.visual(player.getMainHandItem());
        double peak=def.advanced().maxSpeed(YujianPhase.JIYU,def)*FlyingSwordConfig.CRUISE_FRACTION.get()*2;
        int cx=8>>4,first=(-24>>4)-4,last=Mth.floor(-24+peak*110)>>4;
        // Before the scenario epoch: generate the first straight leg once.
        for(int z=last+3;z>=first;z--)for(int x=cx-3;x<=cx+3;x++)player.serverLevel().getChunk(x,z);
    }
    private static void refreshNearCorridor(ServerPlayer player,ControlledSwordEntity sword,Control input){
        var def=FlyingSwordRegistry.visual(sword.display());double steer=Mth.clamp(def.turnRate()*def.advanced().fastHandling()*.65,.03,.35);
        float yaw=sword.getYRot()+(float)(Mth.wrapDegrees(input.yaw()-sword.getYRot())*steer);
        double peak=def.advanced().maxSpeed(YujianPhase.JIYU,def)*FlyingSwordConfig.CRUISE_FRACTION.get()*2;
        double angle=Math.toRadians(yaw);Vec3 target=new Vec3(-Math.sin(angle)*peak,0,Math.cos(angle)*peak);
        Vec3 velocity=sword.getDeltaMovement().lerp(target,Math.max(def.advanced().jiyuAcceleration(),.55));
        var chunks=new HashSet<Long>();
        for(int step=0;step<=2;step++){
            Vec3 p=sword.position().add(velocity.scale(step));int cx=Mth.floor(p.x)>>4,cz=Mth.floor(p.z)>>4;
            for(int x=cx-1;x<=cx+1;x++)for(int z=cz-1;z<=cz+1;z++)chunks.add(net.minecraft.world.level.ChunkPos.asLong(x,z));
        }
        for(long chunk:chunks)player.serverLevel().getChunk(net.minecraft.world.level.ChunkPos.getX(chunk),net.minecraft.world.level.ChunkPos.getZ(chunk));
    }
    private static void warmFlightRoute(ServerPlayer player,ControlledSwordEntity sword,long scenarioClock){
        // Test-only synchronous generation before physics: the scenario never pauses or
        // skips inputs. Follow the shipping steering/velocity equations at <=1 tick
        // and <=16 blocks, including the scheduled next maneuver, not a .3 yaw guess.
        var def=FlyingSwordRegistry.visual(sword.display());
        double peak=def.advanced().maxSpeed(YujianPhase.JIYU,def)*FlyingSwordConfig.CRUISE_FRACTION.get()*2;
        double steer=Mth.clamp(def.turnRate()*def.advanced().fastHandling()*.65,.03,.35);
        double acceleration=Math.max(def.advanced().jiyuAcceleration(),.55);
        Vec3 p=sword.position(),velocity=sword.getDeltaMovement();float yaw=sword.getYRot();
        double dt=Math.min(1,16/Math.max(1,peak));var chunks=new HashSet<Long>();
        for(double ahead=0;ahead<=60+1e-8;ahead+=dt){
            var future=control(scenarioClock+(long)Math.floor(ahead));
            yaw+=Mth.wrapDegrees(future.yaw()-yaw)*(float)(1-Math.pow(1-steer,dt));
            double angle=Math.toRadians(yaw);Vec3 target=new Vec3(-Math.sin(angle)*peak,0,Math.cos(angle)*peak);
            velocity=velocity.lerp(target,1-Math.pow(1-acceleration,dt));
            if(ahead>0)p=p.add(velocity.scale(dt));
            int cx=Mth.floor(p.x)>>4,cz=Mth.floor(p.z)>>4;
            for(int x=cx-2;x<=cx+2;x++)for(int z=cz-2;z<=cz+2;z++)chunks.add(net.minecraft.world.level.ChunkPos.asLong(x,z));
        }
        // Generate far-to-near so the currently ridden corridor is retained last.
        var ordered=new ArrayList<>(chunks);ordered.sort(Comparator.comparingDouble((Long key)->{
            double x=net.minecraft.world.level.ChunkPos.getX(key)*16+8-sword.getX(),z=net.minecraft.world.level.ChunkPos.getZ(key)*16+8-sword.getZ();return -(x*x+z*z);
        }));
        for(long chunk:ordered)player.serverLevel().getChunk(net.minecraft.world.level.ChunkPos.getX(chunk),net.minecraft.world.level.ChunkPos.getZ(chunk));
    }
    static void serverLevelTick(net.minecraft.server.level.ServerLevel level){
        if(!started||playerId==null)return;var player=level.getServer().getPlayerList().getPlayer(playerId);
        if(player==null||player.serverLevel()!=level)return;
        long worldTick=player.serverLevel().getGameTime();
        if(worldTick==lastServerWorldTick)return;lastServerWorldTick=worldTick;
        if(epoch<0)epoch=worldTick;clock=worldTick-epoch;
        if(!(player.getVehicle() instanceof ControlledSwordEntity sword))return;
        var c=control();var row=new LinkedHashMap<String,Object>();
        row.put("clock",clock);row.put("skin",SKINS[c.skin()].name());row.put("phase",c.phase());row.put("segment",c.segment());
        row.put("entityTick",sword.tickCount);row.put("worldTick",worldTick);row.put("playerTick",player.tickCount);row.put("position",List.of(sword.getX(),sword.getY(),sword.getZ()));row.put("yaw",sword.getYRot());row.put("desiredYaw",c.yaw());
        row.put("stage",sword.huahongStage());row.put("strength",sword.huahongStrength());row.put("speed",sword.flightSpeed());
        row.put("inEntityTickingRange",level.getChunkSource().chunkMap.getDistanceManager().inEntityTickingRange(sword.chunkPosition().toLong()));
        row.put("positionEntityTicking",level.isPositionEntityTicking(sword.blockPosition()));row.put("alwaysTicking",sword.isAlwaysTicking());
        net.minecraft.world.level.entity.EntityTickList ticking=net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(net.minecraft.server.level.ServerLevel.class,level,"f_143243_");
        row.put("entityTickListContains",ticking!=null&&ticking.contains(sword));
        long last=lastServerTick(sword);row.put("lastServerTickAfter",last);
        if(beforeTick!=null&&beforeTick.world()==worldTick&&beforeTick.entityId()==sword.getId()){
            boolean supplemented=beforeTick.lastServerTick()!=worldTick&&last==worldTick;
            if(supplemented)supplementedWorldTicks++;
            row.put("lastServerTickBefore",beforeTick.lastServerTick());row.put("supplemented",supplemented);
            row.put("supplementRootTickGrowth",sword.tickCount-beforeTick.entityTick());row.put("supplementPlayerTickGrowth",player.tickCount-beforeTick.playerTick());
        }
        row.put("actualInput",HuahongCapture.rideDiagnostics(player));
        row.put("forward",c.forward());row.put("sprint",c.sprint());row.put("brake",c.brake());
        synchronized(SERVER){SERVER.add(row);}
    }
    private static long lastServerTick(ControlledSwordEntity sword){
        try{var method=ControlledSwordEntity.class.getDeclaredMethod("lastServerTick");method.setAccessible(true);return ((Number)method.invoke(sword)).longValue();}
        catch(ReflectiveOperationException error){throw new AssertionError("Unable to inspect production root ticking timestamp",error);}
    }
    static String view(){int p=control().phase();return p>=100&&p<110||p>=230?"far":p>=90&&p<100||p>=140&&p<230?"near-chase":"three-quarter-side";}
    private static Vec3 desiredCamera(){return switch(view()){case "far"->new Vec3(16,6,-23);case "near-chase"->new Vec3(.3,.75,-8);default->new Vec3(5,2,-5);};}
    static boolean cameraSettled(){return cameraOffset.distanceTo(desiredCamera())<.03;}
    static Vec3 camera(InterpolatedFlyingSwordTransform transform){
        long nanos=System.nanoTime();double dt=cameraNanos==0?0:Math.min(.2,(nanos-cameraNanos)/1e9);cameraNanos=nanos;
        cameraOffset=cameraOffset.lerp(desiredCamera(),1-Math.exp(-dt*20));
        Vec3 f=transform.forward(),s=transform.local(1,0,0).subtract(transform.position()),u=s.cross(f).normalize();
        return transform.position().add(0,.85,0).add(s.scale(cameraOffset.x)).add(u.scale(cameraOffset.y)).add(f.scale(cameraOffset.z));
    }
    static void frame(Minecraft mc,float partial)throws Exception{
        if(!started||mc.player==null||!(mc.player.getVehicle() instanceof ControlledSwordEntity sword))return;
        var mesh=HuahongMotionQa.submittedMesh();if(mesh==null||mesh.vertices.isEmpty())return;
        var c=control(mesh.scenarioClock);var row=new LinkedHashMap<String,Object>();row.put("frame",mesh.serial);row.put("clock",mesh.scenarioClock);row.put("skin",mesh.skin);row.put("phase",c.phase());row.put("segment",c.segment());
        row.put("samplerTick",FlyingSwordEffects.capturedTrailTick());row.put("partial",partial);row.put("entityTick",sword.tickCount);row.put("rawPosition",List.of(sword.getX(),sword.getY(),sword.getZ()));row.put("oldPosition",List.of(sword.xOld,sword.yOld,sword.zOld));
        var interpolationField=ControlledSwordEntity.class.getDeclaredField("flightInterpolation");interpolationField.setAccessible(true);var interpolation=(FlightInterpolation)interpolationField.get(sword);
        row.put("serverClockRate",interpolation.clockRate());row.put("serverBufferDelay",interpolation.bufferDelay());row.put("playbackTick",interpolation.playbackTick());row.put("interpolationBuffering",interpolation.buffering());
        row.put("entityId",mesh.entityId);row.put("center",List.of(mesh.center.x,mesh.center.y,mesh.center.z));row.put("rotation",List.of(sword.getYRot(),sword.getXRot(),sword.roll()));
        row.put("submittedPoseMatrix",mesh.poseMatrix);row.put("actualProjectionMatrix",mesh.projectionMatrix);
        row.put("stage",sword.huahongStage());row.put("strength",sword.renderHuahong(partial));row.put("speed",sword.flightSpeed());row.put("speedMultiplier",FlyingSwordConfig.SPEED_MULTIPLIER.get());
        row.put("hasChunkAt",mc.level.hasChunkAt(sword.blockPosition()));row.put("view",view());row.put("cameraSettled",cameraSettled());row.put("cameraPosition",List.of(mesh.eye.x,mesh.eye.y,mesh.eye.z));
        row.put("exposureAlphaMultiplier",mesh.exposure);row.put("shaderPack",TrailShaderBridge.shaderPackDetected());row.put("renderPath",FlyingSwordEffects.activeTrailRenderPath);
        if(mesh.skin.equals("DENSE"))row.put("volume",HuahongVolumeRenderer.diagnostics());
        var continuity=mesh.continuity();row.putAll(continuity);
        var widths=mesh.projectedHeadWidths();row.put("projectedHeadWidths",widths);
        boolean stable=(c.phase()>=84&&c.phase()<90)||(c.phase()>=224&&c.phase()<230)||(c.phase()>=244&&c.phase()<250);
        if(stable&&cameraSettled()&&Boolean.TRUE.equals(continuity.get("comparable"))&&((Number)continuity.get("matchedHeadVertices")).intValue()>=32&&((Number)continuity.get("p99FinalHeadVertexStep")).doubleValue()>.35)geometryJumps++;
        row.put("stableContinuityWindow",stable);FRAMES.add(row);
        int gl=org.lwjgl.opengl.GL11.glGetError();if(gl!=0)throw new AssertionError("Dynamics GL error "+gl);
        boolean imageWindow=c.phase()>=60;
        NativeImage image=null;boolean normalSequence=mesh.skin.equals("METEOR_PRISM")&&c.phase()>=180&&c.phase()<210&&cameraSettled()&&UTURN_IMAGES.getOrDefault(mesh.skin,0)<12;
        String preview=mesh.skin+"|"+c.segment()+"|"+view()+"|"+mesh.exposure;
        boolean mandatoryNormal=mesh.exposure==1&&(c.segment().equals("straight")&&view().equals("three-quarter-side")||c.segment().equals("uturn-180")&&view().equals("near-chase"));
        boolean realMoving=sword.flightSpeed()>=8;
        boolean submittedVolume=!mesh.skin.equals("DENSE")||((Number)HuahongVolumeRenderer.diagnostics().get("submitted")).intValue()>0;
        row.put("movingPhotoEligible",realMoving&&submittedVolume);
        normalSequence=normalSequence&&realMoving&&submittedVolume&&sword.huahongStage()==2&&sword.renderHuahong(partial)>=.995F;
        boolean previewDue=imageWindow&&!PREVIEWS.contains(preview)&&(!mandatoryNormal||realMoving&&submittedVolume&&cameraSettled()&&sword.huahongStage()==2&&sword.renderHuahong(partial)>=.995F);
        if(normalSequence||previewDue&&IMAGE_BACKLOG.get()<8){
            image=Screenshot.takeScreenshot(mc.getMainRenderTarget());PREVIEWS.add(preview);
            if(normalSequence)row.put("normalUturnSequenceIndex",UTURN_IMAGES.merge(mesh.skin,1,Integer::sum));
        }else if(previewDue)droppedImages++;
        if(image!=null&&!widths.isEmpty())row.put("tipRaster",tipRaster(image,widths.get(0)));
        String artifactSegment=mesh.skin+"|"+c.segment();boolean full=mesh.serial%10==0||!artifactSegment.equals(previousArtifactSegment);previousArtifactSegment=artifactSegment;
        if(full){row.put("fullMeshFile",String.format(Locale.ROOT,"mesh/full-%07d.bin",mesh.serial));fullMeshFrames++;}
        row.put("meshWriterBacklog",BACKLOG.get());row.put("imageWriterBacklog",IMAGE_BACKLOG.get());BACKLOG.incrementAndGet();maxMeshBacklog=Math.max(maxMeshBacklog,BACKLOG.get());
        WRITER.execute(()->{
            try{
                mesh.write(output,false);if(full)mesh.write(output,true);
            }catch(Throwable error){FAILURES.add(error.toString());}finally{BACKLOG.decrementAndGet();}
        });
        if(image!=null){NativeImage captured=image;IMAGE_BACKLOG.incrementAndGet();maxImageBacklog=Math.max(maxImageBacklog,IMAGE_BACKLOG.get());
            IMAGE_WRITER.execute(()->{try{Path directory=output.resolve("motion");Files.createDirectories(directory);String name=String.format(Locale.ROOT,"%s-%s-%s-e%.4f-%07d.png",mesh.skin,c.segment(),row.get("view"),mesh.exposure,mesh.serial);captured.writeToFile(directory.resolve(name));row.put("image","motion/"+name);}
                catch(Throwable error){FAILURES.add(error.toString());}finally{captured.close();IMAGE_BACKLOG.decrementAndGet();}});
        }
    }
    static void finish(Path out)throws Exception{
        started=false;
        CONTROL_SENDER.shutdownNow();
        List<Map<String,Object>> serverSnapshot;synchronized(SERVER){serverSnapshot=List.copyOf(SERVER);}
        WRITER.shutdown();IMAGE_WRITER.shutdown();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(55);
        while((!WRITER.isTerminated()||!IMAGE_WRITER.isTerminated())&&System.nanoTime()<deadline){WRITER.awaitTermination(250,TimeUnit.MILLISECONDS);IMAGE_WRITER.awaitTermination(250,TimeUnit.MILLISECONDS);}
        if(!WRITER.isTerminated()||!IMAGE_WRITER.isTerminated())FAILURES.add("Artifact writers did not finish in 55 seconds");
        var gson=new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(out.resolve("dynamics-frames.json"),gson.toJson(FRAMES));
        Files.writeString(out.resolve("dynamics-server.json"),gson.toJson(serverSnapshot));
        List<String> acceptance=new ArrayList<>(FAILURES);
        var totalCoverage=clientCoverage(FRAMES);verifyCoverage("all skins",totalCoverage,100,acceptance);
        var perSkinCoverage=new LinkedHashMap<String,Object>();
        if(geometryJumps>0)acceptance.add("Actual submitted local-head p99 jumps > .35 blocks in "+geometryJumps+" stable frames");
        for(var skin:SKINS){
            var rows=serverSnapshot.stream().filter(r->r.get("skin").equals(skin.name())).toList();
            var coverage=clientCoverage(FRAMES.stream().filter(r->r.get("skin").equals(skin.name())).toList());perSkinCoverage.put(skin.name(),coverage);verifyCoverage(skin.name(),coverage,80,acceptance);
            if(Boolean.getBoolean("wuxingxiantu.myriadExternalShaders")&&FRAMES.stream().noneMatch(r->r.get("skin").equals(skin.name())&&Boolean.TRUE.equals(r.get("shaderPack"))&&"POST_SHADER_TRAIL_RENDER_PATH".equals(r.get("renderPath"))&&((Number)r.get("speed")).doubleValue()>=8))acceptance.add("No actual enabled shader-pack POST moving frame: "+skin);
            if(rows.size()<80)acceptance.add("Missing server maneuver samples for "+skin);
            for(String segment:List.of("straight","left-90","right-90","uturn-180","release-sprint","recover-sprint","release-forward","recover-forward","brake-exit"))
                if(rows.stream().noneMatch(r->segment.equals(r.get("segment"))))acceptance.add("Missing "+skin+" / "+segment);
            // Sprint release is covered by the product's 12-tick recovery window.
            // Releasing forward is a stop request and must leave rainbow promptly.
            if(rows.stream().anyMatch(r->((Number)r.get("phase")).intValue()>=110&&((Number)r.get("phase")).intValue()<230&&((Number)r.get("stage")).intValue()!=2))acceptance.add("Rainbow interrupted during turns/sprint recovery window: "+skin);
            if(rows.stream().noneMatch(r->((Number)r.get("phase")).intValue()>=230&&((Number)r.get("phase")).intValue()<=234&&(((Number)r.get("stage")).intValue()==3||((Number)r.get("stage")).intValue()==0)))acceptance.add("Releasing forward did not exit rainbow by phase 234: "+skin);
            // Phase 236..249 is recorded without requiring reentry: the existing
            // 20-tick cooldown intentionally exceeds this short recovery sample.
            if(rows.stream().noneMatch(r->((Number)r.get("phase")).intValue()>=270&&((Number)r.get("stage")).intValue()==0))acceptance.add("Brake did not exit rainbow: "+skin);
            if(rows.stream().noneMatch(r->((Number)r.get("phase")).intValue()>=202&&((Number)r.get("phase")).intValue()<210&&Math.abs(Mth.wrapDegrees(((Number)r.get("yaw")).floatValue()-((Number)r.get("desiredYaw")).floatValue()))<20))acceptance.add("No completed 180-degree turn: "+skin);
            for(String required:List.of("straight|three-quarter-side","uturn-180|near-chase")){
                boolean captured=FRAMES.stream().anyMatch(r->r.get("skin").equals(skin.name())&&required.equals(r.get("segment")+"|"+r.get("view"))&&r.containsKey("image")&&((Number)r.get("exposureAlphaMultiplier")).doubleValue()==1&&Boolean.TRUE.equals(r.get("cameraSettled"))&&Boolean.TRUE.equals(r.get("movingPhotoEligible"))&&((Number)r.get("stage")).intValue()==2&&((Number)r.get("strength")).doubleValue()>=.995);
                if(!captured)acceptance.add("Missing real moving normal-exposure photograph: "+skin+" / "+required);
            }
            if(skin==HuahongSkin.DENSE){
                boolean submitted=false;
                for(var frame:FRAMES)if(frame.get("skin").equals("DENSE")&&frame.get("volume") instanceof Map<?,?> volume){
                    boolean ready=Boolean.TRUE.equals(volume.get("ready"));String hash=String.valueOf(volume.get("shaderHash"));
                    for(String key:List.of("currentPackets","lastPackets"))if(volume.get(key) instanceof List<?> packets)for(Object packet:packets)if(packet instanceof HuahongVolumeRenderer.Trace trace&&trace.submitted()){
                        if(ready&&hash.matches("[0-9a-f]{64}")&&trace.shaderReady()&&trace.sceneDepth()>0&&trace.pathPoints()==32&&trace.path().size()==32&&trace.pathAges().size()==32&&trace.worldToClip().size()==16&&trace.inverseWorldToClip().size()==16){
                            boolean finite=trace.path().stream().flatMap(List::stream).allMatch(Float::isFinite)&&trace.worldToClip().stream().allMatch(Float::isFinite)&&trace.inverseWorldToClip().stream().allMatch(Float::isFinite);
                            if(!finite)acceptance.add("Nonfinite DENSE volume packet");else submitted=true;
                        }
                    }
                }
                if(!submitted)acceptance.add("DENSE produced no real ready/compiled/submitted depth-tested volume packet");
            }
        }
        var summary=new LinkedHashMap<String,Object>();summary.put("failures",acceptance);summary.put("frames",FRAMES.size());summary.put("serverSamples",SERVER.size());summary.put("geometryJumpFrames",geometryJumps);summary.put("droppedImages",droppedImages);summary.put("skins",Arrays.stream(SKINS).map(Enum::name).toList());summary.put("simulatedSeconds",clock/20.0);
        summary.put("supplementedWorldTicks",supplementedWorldTicks);
        summary.put("headMeshFrames",FRAMES.size());summary.put("fullMeshFrames",fullMeshFrames);summary.put("fullMeshCadence","Every tenth render frame and first real frame of each skin/segment; all frames retain every actual vertex within 8 blocks of the carrier.");
        summary.put("maxMeshWriterBacklog",maxMeshBacklog);summary.put("maxImageWriterBacklog",maxImageBacklog);summary.put("normalUturnSequenceImages",UTURN_IMAGES);summary.put("orderedControllerInputTicks",SEND_COUNT.get());
        summary.put("warmupDriver","Before epoch generate first 110-tick straight corridor; refresh current and next 2 ticks of 3x3 chunks every server START; long route every 10 ticks: shipping turnRate*fastHandling*.65 and .55 minimum velocity interpolation; <=1 tick/16 block steps, future phase prediction, 60 ticks ahead with 2-chunk corridor; generation only, no forced tickets or physics changes.");
        summary.put("inputDriver","QA server-level START calls the real YujianController.handleInput in tick order; no fixture C2S control packets. Physics and snapshot S2C stay real. This run is not C2S network-timing acceptance.");
        summary.put("realClientCoverage",totalCoverage);summary.put("perSkinClientCoverage",perSkinCoverage);
        summary.put("vertexFormat","Raw big endian HMQ3: magic(int), frame(long), renderTime(double), partial(float), entityId(int), skin(UTF), full(boolean), originalCount(int), storedCount(int), then originalIndex(int), sourceSpace(byte: 1 local/2 world), strip(int), originalStripIndex(int), x/y/z/u/v(float), ARGB(int), carrier-local x/y/z(float). No compression; head every frame, full at declared cadence.");
        summary.put("exposureNote","QA-only alpha attenuation, normal/emission shared consumer; exposures are neighboring real frames, not a postprocessed PNG or changed production shader.");
        Files.writeString(out.resolve("dynamics-summary.json"),gson.toJson(summary));
        if(!acceptance.isEmpty())throw new AssertionError("Dynamics acceptance: "+acceptance);
    }
    private static Map<String,Object> clientCoverage(List<Map<String,Object>> frames){
        if(frames.isEmpty())return Map.of("frames",0,"entityTickGrowth",0,"samplerTickGrowth",0,"positionRange",0,"yawTravelDegrees",0);
        double[] min={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY},max={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        double yawTravel=0;Float oldYaw=null;
        for(var frame:frames){
            var position=(List<?>)frame.get("center");for(int axis=0;axis<3;axis++){double value=((Number)position.get(axis)).doubleValue();min[axis]=Math.min(min[axis],value);max[axis]=Math.max(max[axis],value);}
            float yaw=((Number)((List<?>)frame.get("rotation")).get(0)).floatValue();if(oldYaw!=null)yawTravel+=Math.abs(Mth.wrapDegrees(yaw-oldYaw));oldYaw=yaw;
        }
        var first=frames.get(0);var last=frames.get(frames.size()-1);double range=0;for(int axis=0;axis<3;axis++)range+=(max[axis]-min[axis])*(max[axis]-min[axis]);
        return Map.of("frames",frames.size(),"entityTickGrowth",((Number)last.get("entityTick")).longValue()-((Number)first.get("entityTick")).longValue(),
                "samplerTickGrowth",((Number)last.get("samplerTick")).longValue()-((Number)first.get("samplerTick")).longValue(),"positionRange",Math.sqrt(range),"yawTravelDegrees",yawTravel);
    }
    private static void verifyCoverage(String label,Map<String,Object> coverage,double minRange,List<String> failures){
        long entity=((Number)coverage.get("entityTickGrowth")).longValue(),sampler=((Number)coverage.get("samplerTickGrowth")).longValue();
        if(((Number)coverage.get("frames")).intValue()<20||sampler<80||entity<sampler*.8||((Number)coverage.get("positionRange")).doubleValue()<minRange||((Number)coverage.get("yawTravelDegrees")).doubleValue()<100)
            failures.add("Client did not actually complete dynamic movement ("+label+"): "+coverage);
    }
    private static Map<String,Object> tipRaster(NativeImage image,Map<String,Object> projected){
        var center=(List<?>)projected.get("pixelCenter");double px=((Number)center.get(0)).doubleValue(),py=((Number)center.get(1)).doubleValue();
        int x0=Math.max(0,(int)Math.floor(px)-12),x1=Math.min(image.getWidth()-1,(int)Math.ceil(px)+12),y0=Math.max(0,(int)Math.floor(py)-12),y1=Math.min(image.getHeight()-1,(int)Math.ceil(py)+12);
        if(x0>x1||y0>y1)return Map.of("visible",false);
        ArrayList<Double> luminance=new ArrayList<>();
        for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++){int rgb=image.getPixelRGBA(x,y);luminance.add((.2126*(rgb&255)+.7152*((rgb>>>8)&255)+.0722*((rgb>>>16)&255))/255);}
        var sorted=new ArrayList<>(luminance);sorted.sort(Double::compare);double background=sorted.get(sorted.size()/5),weight=0,sx=0,sy=0,peak=0;int bright=0,index=0;
        for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++){double value=luminance.get(index++),w=Math.max(0,value-background-.08);weight+=w;sx+=(x+.5)*w;sy+=(y+.5)*w;peak=Math.max(peak,value);if(value>.97)bright++;}
        return Map.of("visible",true,"roi",List.of(x0,y0,x1+1,y1+1),"predictedTip",List.of(px,py),"backgroundLuminance",background,"peakLuminance",peak,"signal",weight,"saturatedPixels",bright,
                "centroid",weight>1e-9?List.of(sx/weight,sy/weight):List.of(px,py));
    }
    private static void runtimeEvidence(Path out)throws Exception{
        List<Object> rows=new ArrayList<>();
        for(Class<?> type:List.of(GeneratedMod.class,HuahongVisuals.class,InterpolatedFlyingSwordTransform.class,ControlledSwordEntity.class,FlightInterpolation.class,HuahongFlightPath.class,HuahongVolumeRenderer.class,Class.forName("cn.blockforge.generated.generatedmod.client.HuahongSkinGeometry"))){
            var row=new LinkedHashMap<String,Object>();row.put("class",type.getName());var url=type.getProtectionDomain().getCodeSource().getLocation();row.put("codeSource",url.toString());
            try(var stream=type.getResourceAsStream("/"+type.getName().replace('.','/')+".class")){row.put("loadedClassSha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes())));}
            try{Path path=Path.of(url.toURI());row.put("loadMode",Files.isRegularFile(path)?"jar":"exploded development classes");
                if(Files.isRegularFile(path))row.put("loadedJarSha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
            }catch(RuntimeException virtualLocation){row.put("loadMode","loader virtual location; class bytes verified, JAR not asserted");}rows.add(row);
        }
        Map<String,Object> shaderRuntime=new LinkedHashMap<>();shaderRuntime.put("detected",TrailShaderBridge.shaderPackDetected());
        if(Boolean.getBoolean("wuxingxiantu.myriadExternalShaders")){
            Class<?> iris=Class.forName("net.irisshaders.iris.Iris");
            String name=String.valueOf(iris.getMethod("getCurrentPackName").invoke(null));
            boolean inUse=(Boolean)iris.getMethod("isPackInUseQuick").invoke(null);
            boolean present=((Optional<?>)iris.getMethod("getCurrentPack").invoke(null)).isPresent();
            String colorSpace=String.valueOf(Class.forName("net.irisshaders.iris.gui.option.IrisVideoSettings").getField("colorSpace").get(null));
            shaderRuntime.put("currentPackName",name);shaderRuntime.put("packPresent",present);shaderRuntime.put("inUse",inUse);shaderRuntime.put("colorSpace",colorSpace);
            if(!inUse||!present||!name.equals("Solas Shader V3.7b.zip")||!colorSpace.equals("DISPLAY_P3"))throw new AssertionError("Actual Solas runtime differs from the required pack/display space: "+shaderRuntime);
            Path pack=Minecraft.getInstance().gameDirectory.toPath().resolve("shaderpacks").resolve(name);shaderRuntime.put("packZipSha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(pack))));
        }
        Map<String,String> shaderHashes=new LinkedHashMap<>();
        for(String path:List.of("shaders/core/sword_flight.fsh","shaders/core/sword_flight_post.fsh","shaders/core/huahong_volume.fsh","shaders/core/huahong_volume.vsh","shaders/core/huahong_volume.json","shaders/core/sword_flight.json","shaders/core/sword_flight_post.json","textures/effect/huahong/smoke-atlas.png"))
            try(var stream=Minecraft.getInstance().getResourceManager().getResource(new net.minecraft.resources.ResourceLocation("wuxingxiantu",path)).orElseThrow().open()){
                shaderHashes.put(path,HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes())));
            }
        Files.writeString(out.resolve("runtime.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("classes",rows,"shaderRuntime",shaderRuntime,"runtimeResourceSha256",shaderHashes,"classHashScope","Runtime-resolved class resource bytes at capture startup; CodeSource identifies loader source. Post-Mixin JVM bytes are not instrumented.","speedMultiplier",FlyingSwordConfig.SPEED_MULTIPLIER.get(),"pointerProtection","QA source set only","plannedSimulatedSeconds",SKINS.length*CYCLE/20.0)));
    }
    private HuahongDynamicsQa(){}
}
