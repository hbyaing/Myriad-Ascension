package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.*;
import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.yujian.*;
import com.google.gson.GsonBuilder;
import net.minecraft.client.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Continuous-frame reproduction using the shipping camera, no exposure changes or mesh fixtures. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class HuahongReproCapture {
    private static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.huahongRepro");
    private static final boolean USER_VIEW=Boolean.getBoolean("wuxingxiantu.huahongUserView");
    private static final boolean ORDINARY=Boolean.getBoolean("wuxingxiantu.huahongReproOrdinary");
    private static final boolean HILT_UTURN=Boolean.getBoolean("wuxingxiantu.huahongReproHiltUTurn");
    private static final boolean FOLLOWUP=Boolean.getBoolean("wuxingxiantu.huahongHeadFollowup"),CANDIDATE=Boolean.getBoolean("wuxingxiantu.huahongHeadFollowupCandidate");
    private static final String REUSE_WORLD=System.getProperty("wuxingxiantu.huahongReproWorld","");
    private static final String SINGLE_SKIN=System.getProperty("wuxingxiantu.huahongFollowupSingleSkin","");
    private static final int CYCLE_START=Integer.getInteger("wuxingxiantu.huahongFollowupCycleStart",160);
    private static final HuahongSkin SKIN=HuahongSkin.valueOf(System.getProperty("wuxingxiantu.huahongReproSkin","SMOKE").toUpperCase(Locale.ROOT));
    private static final int CAPTURE_START=Integer.getInteger("wuxingxiantu.huahongReproCaptureStart",FOLLOWUP&&!ORDINARY?CYCLE_START-10:40);
    private static final int END_TICK=Integer.getInteger("wuxingxiantu.huahongReproEndTick",FOLLOWUP&&!ORDINARY?CYCLE_START+540:420);
    private static final Path OUT=Path.of(System.getProperty("wuxingxiantu.huahongReproOutput","../../reports/flight-repro"));
    private static final long START=System.nanoTime();
    private static final List<Map<String,Object>> FRAMES=new ArrayList<>();
    private static final List<String> ERRORS=Collections.synchronizedList(new ArrayList<>());
    private static final Map<String,Object> SETUP_CHECKS=new LinkedHashMap<>();
    private static final ThreadPoolExecutor WRITER=new ThreadPoolExecutor(2,2,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(8),r->{var t=new Thread(r,"repro-png");t.setDaemon(true);return t;},new ThreadPoolExecutor.CallerRunsPolicy());
    private static boolean opened,ready,done,summoned;
    private static volatile boolean started;
    private static CompletableFuture<?> preparation;
    private static volatile long clock;
    private static long epoch=-1;
    private static UUID playerId;
    private static int frames;
    private static long videoStart;
    private static boolean capturePending,backbufferSaved;
    private static float capturePartial;
    private static HuahongSkin selected;
    public static long scenarioClock(){return clock;}
    public static String segment(){if(!FOLLOWUP||ORDINARY)return "ordinary";if(clock<CYCLE_START)return "entry";if(clock>=CYCLE_START+480)return "exit";int p=(int)((clock-CYCLE_START)%60);return p<10?"switch-warm":p<26?"straight":p<36?"turn-settle":"steady-turn";}
    private static HuahongSkin chosenSkin(){return !SINGLE_SKIN.isBlank()?HuahongSkin.valueOf(SINGLE_SKIN):FOLLOWUP&&!ORDINARY?HuahongSkin.values()[(int)Math.max(0,Math.min(7,(clock-CYCLE_START)/60))]:SKIN;}
    private static float baseYaw(long t){return t<70?0:t<160?(float)((t-70)*3):(float)(270+(t-160)*5);}
    private static float yaw(long t){if(Boolean.getBoolean("wuxingxiantu.smokeFlowReview"))return 0;if(Boolean.getBoolean("wuxingxiantu.flightJoinStress")){if(t<220)return baseYaw(t);if(t<270)return baseYaw(219)+180;return (float)(baseYaw(219)+180+(t-270)*4);}if(FOLLOWUP&&!ORDINARY&&t>=CYCLE_START){long k=(t-CYCLE_START)/60,p=(t-CYCLE_START)%60;return (float)(baseYaw(CYCLE_START)+k*220+(p<10?p*5:p<26?50:50+(p-26)*5));}if(HILT_UTURN){if(t>=120&&t<150)return baseYaw(119)+180;if(t>=300&&t<330)return baseYaw(299)+180;}return baseYaw(t);}
    private static float pitch(long t){if(Boolean.getBoolean("wuxingxiantu.smokeFlowReview"))return -2;if(Boolean.getBoolean("wuxingxiantu.flightJoinStress")&&t>=220)return (float)(40*Math.sin((t-220)*Math.PI/70));if(Boolean.getBoolean("wuxingxiantu.flightSteepLook")&&t>=160)return (float)(68*Math.sin((t-160)*Math.PI/45));return FOLLOWUP&&!ORDINARY?-2:(float)(-5+7*Math.sin(Math.max(0,t-100)*Math.PI/100));}
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent event){
        if(!ENABLED||done||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            Files.createDirectories(OUT.resolve("frames"));GLFW.glfwHideWindow(mc.getWindow().getWindow());mc.setWindowActive(true);mc.options.pauseOnLostFocus=false;
            if((System.nanoTime()-START)/1e9>600)throw new AssertionError("Reproduction watchdog");
            if(!opened&&mc.player!=null)throw new AssertionError("Refusing existing world");
            if(!opened&&mc.getOverlay()==null&&(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen||mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)){
                opened=true;mc.options.enableVsync().set(false);mc.getWindow().updateVsync(false);mc.getWindow().setWindowed(1280,720);mc.resizeDisplay();mc.options.framerateLimit().set(Integer.getInteger("wuxingxiantu.flightReproFps",60));mc.options.fov().set(70);mc.options.renderDistance().set(8);mc.options.simulationDistance().set(6);mc.options.cloudStatus().set(CloudStatus.FANCY);mc.options.bobView().set(USER_VIEW);mc.options.hideGui=false;
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);if(FOLLOWUP)mc.options.hideGui=!USER_VIEW;
                var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                if(!REUSE_WORLD.isBlank()){Path saves=mc.gameDirectory.toPath().resolve("saves").toAbsolutePath().normalize(),save=saves.resolve(REUSE_WORLD).normalize();if(!REUSE_WORLD.startsWith("flight-continuous-")||REUSE_WORLD.contains("/")||REUSE_WORLD.contains("\\")||!save.getParent().equals(saves)||!Files.isRegularFile(save.resolve("level.dat")))throw new AssertionError("Refusing non-QA flight world "+save);mc.createWorldOpenFlows().loadLevel(new net.minecraft.client.gui.screens.TitleScreen(),REUSE_WORLD);return;}
                mc.createWorldOpenFlows().createFreshLevel("flight-continuous-"+System.currentTimeMillis(),new LevelSettings("Flight continuous reproduction",GameType.SURVIVAL,false,net.minecraft.world.Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(7215,false,false),a->a.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions());return;
            }
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null||mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen)return;
            if(mc.screen!=null)mc.setScreen(null);
            if(!ready){
                ready=true;playerId=mc.player.getUUID();FlyingSwordConfig.TRAILS.set(true);FlyingSwordConfig.TRAIL_PATH.set(FlyingSwordConfig.TrailPath.AUTO);HuahongSkinPreferences.select(chosenSkin());selected=chosenSkin();
                preparation=mc.getSingleplayerServer().submitAsync(()->prepare(mc.getSingleplayerServer().getPlayerList().getPlayer(playerId)));return;
            }
            if(preparation!=null){if(!preparation.isDone())return;preparation.join();preparation=null;seedTrailClock();PointerSafety.verify();runtimeEvidence(mc);if(Boolean.getBoolean("wuxingxiantu.flightTickJoinProbe"))SETUP_CHECKS.put("largeTickHistory",FlightLargeTickQa.verify());if(FOLLOWUP){if(CANDIDATE){if(ORDINARY)SETUP_CHECKS.put("ordinaryFlicker",OrdinaryTrailFlickerQa.verify(mc));else{SETUP_CHECKS.put("headBridge",Class.forName("cn.blockforge.generated.generatedmod.client.qa.myriad.HuahongHeadBridgeQa").getMethod("verify").invoke(null));SETUP_CHECKS.put("pathWindow",Class.forName("cn.blockforge.generated.generatedmod.client.qa.myriad.HuahongPathWindowQa").getMethod("verify").invoke(null));}}else SETUP_CHECKS.put("baseline","Frozen production outputs selected by huahongFrozenMain; actual runtime hashes recorded, no candidate-only synthetic checks.");}else if(OrdinaryHiltQa.LAYERS)SETUP_CHECKS.put("diagnosticOnly","Old frozen production outputs: layered observations; setup GPU checks not repeated against newly edited QA sources.");else{SETUP_CHECKS.put("sectionCadence",FlightSectionCadenceQa.verify());SETUP_CHECKS.put("material",HuahongMaterialQa.verify(mc));SETUP_CHECKS.put("path",HuahongPathQa.verify());if(SKIN==HuahongSkin.DENSE)SETUP_CHECKS.put("volumeMaterial",HuahongVolumeMaterialQa.verify(mc));}if(Boolean.getBoolean("wuxingxiantu.flightBlendStateProbe")){SETUP_CHECKS.put("billboardBasis",HuahongBillboardBasisQa.verify(mc));SETUP_CHECKS.put("postState",SwordTrailStateQa.verify(mc,CANDIDATE));SETUP_CHECKS.put("dunxuAlphaBuffer",DunxuAlphaBufferQa.verify(mc));}if(Boolean.getBoolean("wuxingxiantu.huahongRootLayers")||Boolean.getBoolean("wuxingxiantu.huahongRootChecks")){SETUP_CHECKS.put("smokeOrder",HuahongSmokeOrderQa.verify(mc));SETUP_CHECKS.put("subpixelCore",HuahongSubpixelQa.verify(mc));}started=true;}
            if(FOLLOWUP&&selected!=chosenSkin()){selected=chosenSkin();HuahongSkinPreferences.select(selected);}
            if(mc.player.getVehicle() instanceof ControlledSwordEntity){if(!YujianClientController.active())YujianClientController.setActive(true);mc.options.setCameraType(USER_VIEW?CameraType.THIRD_PERSON_BACK:FOLLOWUP&&!ORDINARY||clock<260?CameraType.THIRD_PERSON_FRONT:CameraType.THIRD_PERSON_BACK);}
            mc.player.setYRot(Boolean.getBoolean("wuxingxiantu.smokeFlowReview")?90:yaw(clock));mc.player.setXRot(pitch(clock));
            if(clock>=END_TICK)finish(null);
        }catch(Throwable error){finish(error);}
    }
    private static void prepare(ServerPlayer p){
        var l=p.serverLevel();
        // Flat, explicitly loaded visual route: this recording is not cold-generation performance evidence.
        if(FOLLOWUP){YujianController.stop(p);p.stopRiding();for(var entity:l.getAllEntities())if(entity instanceof ControlledSwordEntity s&&p.getUUID().equals(s.ownerId))s.discard();p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);warmCorridor(l);}else for(int x=-20;x<=32;x++)for(int z=-20;z<=32;z++)l.getChunk(x,z);
        l.setDayTime(18000);l.setWeatherParameters(12000,0,false,false);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,l.getServer());
        p.getPersistentData().remove("SwordFlightCooldown");p.setNoGravity(true);p.removeAllEffects();p.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(WuXingItems.PURE_YANG_SWORD.get()));p.getInventory().selected=0;
        var d=GeneratedMod.data(p);d.wuxingxiantu$setRealm(99);d.wuxingxiantu$learnArt(YujianController.FLIGHT);d.wuxingxiantu$setArtMastery(YujianController.FLIGHT,100);d.wuxingxiantu$setMaxMana(1000000);d.wuxingxiantu$setAura(1000000);p.getPersistentData().putLong("YujianCooldown_DUNXU",Long.MAX_VALUE);
        FlyingSwordConfig.SPEED_MULTIPLIER.set(1.0);GeneratedMod.sync(p);p.teleportTo(l,8,Double.parseDouble(System.getProperty("wuxingxiantu.flightReproAltitude",Boolean.getBoolean("wuxingxiantu.flightSteepLook")?"2000":"113")),-24,0,0);
    }
    @SubscribeEvent public static void input(TickEvent.LevelTickEvent event){
        if(!ENABLED||!started||event.phase!=TickEvent.Phase.START||!(event.level instanceof net.minecraft.server.level.ServerLevel l))return;
        var p=l.getServer().getPlayerList().getPlayer(playerId);if(p==null||p.serverLevel()!=l)return;
        if(epoch<0)epoch=l.getGameTime();clock=l.getGameTime()-epoch;
        int brakeAt=FOLLOWUP&&!ORDINARY?CYCLE_START+480:390;
        boolean forward=clock>=10&&clock<brakeAt,sprint=(ORDINARY?clock>=20:clock>=160)&&forward,roll=(!FOLLOWUP||ORDINARY)&&clock>=190&&clock<245;
        if(Boolean.getBoolean("wuxingxiantu.flightJoinStress")&&(clock>=230&&clock<255||clock>=330&&clock<355))sprint=false;
        if(ORDINARY)try{
            var rides=YujianController.class.getDeclaredField("RIDES");rides.setAccessible(true);Object ride=((Map<?,?>)rides.get(null)).get(p.getUUID());
            if(ride!=null){var flight=ride.getClass().getDeclaredField("huahong");flight.setAccessible(true);Object state=flight.get(ride);var cooldown=state.getClass().getDeclaredField("cooldown");cooldown.setAccessible(true);cooldown.setInt(state,1000000);}
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
        YujianController.handleInput(p,clock<10,!summoned,forward,clock>=brakeAt,false,false,false,roll,sprint,false,1,0,yaw(clock),pitch(clock));summoned=true;
    }
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event){
        if(!ENABLED||!started||done||event.phase!=TickEvent.Phase.END||clock<CAPTURE_START||clock>=END_TICK)return;
        capturePartial=event.renderTickTime;capturePending=true;
    }
    /** After all END listeners and blitToScreen, before the real buffer swap. */
    public static void captureAtPresent(){
        if(!capturePending||!ENABLED||!started||done)return;capturePending=false;float partial=capturePartial;
        var mc=Minecraft.getInstance();if(mc.player==null||!(mc.player.getVehicle() instanceof ControlledSwordEntity s))return;
        try{
            long before=System.nanoTime();if(videoStart==0)videoStart=before;
            int index=frames++;boolean recordImage=index%Math.max(1,Integer.getInteger("wuxingxiantu.flightSparsePng",1))==0;var image=recordImage?Screenshot.takeScreenshot(mc.getMainRenderTarget()):null;long after=System.nanoTime();
            var f=InterpolatedFlyingSwordTransform.get(s,partial);var row=new LinkedHashMap<String,Object>();
            if(f.huahong()>.001F){
                var ordinary=FlyingSwordEffects.class.getDeclaredField("TRAILS");ordinary.setAccessible(true);
                var rainbow=HuahongVisuals.class.getDeclaredField("TRAILS");rainbow.setAccessible(true);
                Object trail=((Map<?,?>)rainbow.get(null)).get(s);boolean shared=false;
                if(trail!=null){var history=trail.getClass().getDeclaredField("history");history.setAccessible(true);shared=history.get(trail)==((Map<?,?>)ordinary.get(null)).get(s);}
                row.put("sharedMorphHistory",shared);if(!shared)throw new AssertionError("Rainbow emitted from a second history");
                if(f.huahong()>=.99999F&&FlyingSwordEffects.normalTrailVertexCount-HuahongVisuals.vertices!=0)throw new AssertionError("Ordinary trail remains after complete transformation");
            }
            if(Boolean.getBoolean("wuxingxiantu.flightTickJoinProbe"))row.put("tickJoin",FlightTickJoinProbe.snapshot(s,partial));if(Boolean.getBoolean("wuxingxiantu.flightJoinStress"))row.put("joinProfile",HuahongJoinProbe.snapshot());row.put("imageRecorded",recordImage);row.put("frame",index);row.put("nanoTime",before);row.put("seconds",(before-videoStart)/1e9);row.put("clock",clock);row.put("partial",partial);row.put("speed",s.renderSpeed(1));row.put("strength",f.huahong());row.put("stage",s.renderHuahongStage());row.put("position",List.of(f.position().x,f.position().y,f.position().z));row.put("roll",f.roll());row.put("camera",mc.options.getCameraType().name());row.put("readbackMs",(after-before)/1e6);row.put("shaderPack",TrailShaderBridge.shaderPackDetected());row.put("path",FlyingSwordEffects.activeTrailRenderPath);row.put("normalVertices",FlyingSwordEffects.normalTrailVertexCount-HuahongVisuals.vertices);row.put("rainbowVertices",HuahongVisuals.vertices);row.put("file",String.format(Locale.ROOT,"frames/%06d.png",index));row.put("captureTiming","Window.updateDisplay HEAD: after all RenderTick.END submits and blitToScreen, before swap");row.put("presentation",Map.of("renderStage",FlyingSwordEffects.renderStage,"normalSubmitted",FlyingSwordEffects.normalTrailSubmitted,"postDraws",SwordFlightTrailRenderer.postDraws,"postStatus",SwordFlightTrailRenderer.status,"volumeSubmitted",HuahongVolumeRenderer.submittedThisFrame,"guiHidden",mc.options.hideGui));FRAMES.add(row);
            if(SKIN==HuahongSkin.DENSE)row.put("volume",HuahongVolumeRenderer.diagnostics());
            if(OrdinaryHiltQa.ENABLED)row.put("hilt",OrdinaryHiltQa.snapshot());
            if(HILT_UTURN){row.put("inputYaw",yaw(clock));row.put("actualYaw",f.yaw());row.put("actualPitch",f.pitch());}
            if(FOLLOWUP){row.put("actualPitch",f.pitch());row.put("requestedSkin",chosenSkin().name());row.put("actualSkin",s.huahongSkin().name());row.put("segment",segment());row.put("skinWarmTicks",clock<CYCLE_START?0:(clock-CYCLE_START)%60);row.put("head",FlightHeadProbe.snapshot());row.put("sourceEffectTime",FlyingSwordEffects.capturedEffectTime());row.put("sprintFactor",f.sprintFactor());row.put("spiralFactor",f.spiralFactor());row.put("jinghong",s.renderJinghong(partial));row.put("aheadLoaded",s.aheadLoaded());if(s.huahongSkin()==HuahongSkin.DENSE)row.put("volume",HuahongVolumeRenderer.diagnostics());}
            if(FOLLOWUP&&!backbufferSaved&&f.huahong()>=.995F){saveBackbuffer(mc);backbufferSaved=true;row.put("defaultBackbufferDiagnostic","default-backbuffer-first-active.png");}
            if(image!=null)WRITER.execute(()->{try(image){image.writeToFile(OUT.resolve((String)row.get("file")));}catch(Throwable error){ERRORS.add(error.toString());}});
        }catch(Throwable error){finish(error);}
    }
    private static void saveBackbuffer(Minecraft mc)throws Exception{
        int w=mc.getWindow().getWidth(),h=mc.getWindow().getHeight(),oldFbo=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER_BINDING),oldBuffer=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_READ_BUFFER);var bytes=org.lwjgl.BufferUtils.createByteBuffer(w*h*4);
        org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER,0);org.lwjgl.opengl.GL11.glReadBuffer(org.lwjgl.opengl.GL11.GL_BACK);org.lwjgl.opengl.GL11.glReadPixels(0,0,w,h,org.lwjgl.opengl.GL11.GL_RGBA,org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE,bytes);org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER,oldFbo);org.lwjgl.opengl.GL11.glReadBuffer(oldBuffer);
        var im=new com.mojang.blaze3d.platform.NativeImage(w,h,false);for(int y=0;y<h;y++)for(int x=0;x<w;x++){int p=(y*w+x)*4;im.setPixelRGBA(x,h-1-y,(bytes.get(p)&255)|((bytes.get(p+1)&255)<<8)|((bytes.get(p+2)&255)<<16)|((bytes.get(p+3)&255)<<24));}WRITER.execute(()->{try(im){im.writeToFile(OUT.resolve("default-backbuffer-first-active.png"));}catch(Exception e){ERRORS.add(e.toString());}});
    }
    private static void warmCorridor(net.minecraft.server.level.ServerLevel l){
        // Loaded visual fixture only: warm a narrow predicted corridor, never a cold-route result.
        Set<Long> chunks=new HashSet<>();for(double top:new double[]{5,15,30}){double x=8,z=-24,heading=0,speed=0;for(int t=0;t<END_TICK;t++){heading+=net.minecraft.util.Mth.wrapDegrees(yaw(t)-(float)heading)*.18;double target=t<10?0:!ORDINARY&&t<160?2:top;speed+=(target-speed)*.2;double angle=Math.toRadians(heading);x-=Math.sin(angle)*speed;z+=Math.cos(angle)*speed;int cx=net.minecraft.util.Mth.floor(x)>>4,cz=net.minecraft.util.Mth.floor(z)>>4;for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)chunks.add(net.minecraft.world.level.ChunkPos.asLong(cx+dx,cz+dz));}}for(long key:chunks){var p=new net.minecraft.world.level.ChunkPos(key);l.getChunk(p.x,p.z);}
    }
    private static void runtimeEvidence(Minecraft mc)throws Exception{
        var classes=new LinkedHashMap<String,Object>();
        for(String name:List.of("GeneratedMod","yujian.ControlledSwordEntity","yujian.YujianController","yujian.FlightInterpolation","yujian.FlightCollision","yujian.FlightRootTicking","yujian.FlightChunkStreaming","yujian.FlightTerrainGovernor","client.FlyingSwordEffects","client.DunxuTransparency","client.HuahongVisuals","client.HuahongFlightPath","client.HuahongSkinGeometry","client.HuahongFlowField","client.HuahongVolumeRenderer","client.InterpolatedFlyingSwordTransform","client.SwordTrailPhase","client.ControlledSwordRenderer","client.SwordFlightAttachment")){
            String full="cn.blockforge.generated.generatedmod."+name;Class<?> type=Class.forName(full);
            try(var stream=type.getResourceAsStream("/"+full.replace('.','/')+".class")){
                classes.put(full,Map.of("sha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes())),"codeSource",type.getProtectionDomain().getCodeSource().getLocation().toString()));
            }
        }
        for(String optional:List.of("SwordTailClipper","HuahongHeadBridge","FlightTrailCurve"))try{Class<?> type=Class.forName("cn.blockforge.generated.generatedmod.client."+optional);try(var stream=type.getResourceAsStream("/"+type.getName().replace('.','/')+".class")){classes.put(type.getName(),Map.of("sha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes())),"codeSource",type.getProtectionDomain().getCodeSource().getLocation().toString()));}}catch(ClassNotFoundException oldBaseline){}
        var resourceHashes=new LinkedHashMap<String,String>();
        for(String path:List.of("shaders/core/sword_flight.fsh","shaders/core/sword_flight_post.fsh","shaders/core/huahong_volume.fsh","shaders/core/huahong_volume.vsh","shaders/core/huahong_volume.json","shaders/core/sword_flight.json","shaders/core/sword_flight_post.json","textures/effect/huahong/smoke-atlas.png"))
            try(var stream=mc.getResourceManager().getResource(new net.minecraft.resources.ResourceLocation("wuxingxiantu",path)).orElseThrow().open()){
                resourceHashes.put(path,HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes())));
            }
        var shader=new LinkedHashMap<String,Object>();shader.put("bridgeDetected",TrailShaderBridge.shaderPackDetected());
        Class<?> iris=Class.forName("net.irisshaders.iris.Iris");
        String pack=String.valueOf(iris.getMethod("getCurrentPackName").invoke(null));
        boolean inUse=(Boolean)iris.getMethod("isPackInUseQuick").invoke(null),present=((Optional<?>)iris.getMethod("getCurrentPack").invoke(null)).isPresent();
        shader.put("currentPackName",pack);shader.put("inUse",inUse);shader.put("packPresent",present);shader.put("colorSpace",String.valueOf(Class.forName("net.irisshaders.iris.gui.option.IrisVideoSettings").getField("colorSpace").get(null)));
        boolean expected=Boolean.getBoolean("wuxingxiantu.huahongReproSolas");
        if(expected?(!inUse||!present||!pack.equals("Solas Shader V3.7b.zip")):inUse)throw new AssertionError("Unexpected shader state "+shader);
        Path packPath=mc.gameDirectory.toPath().resolve("shaderpacks").resolve(pack);if(Files.isRegularFile(packPath))shader.put("packZipSha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(packPath))));
        Files.writeString(OUT.resolve("runtime.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("classes",classes,"resources",resourceHashes,"shader",shader,"scope","Runtime-resolved resource class bytes; not post-Mixin instrumentation.","configuredScenarioEndTick",END_TICK,"configuredLastCaptureTick",END_TICK-1,"hiltUTurnInput",HILT_UTURN)));
    }
    private static final List<Object> ROOT_LAYERS=new ArrayList<>();
    /** Observe identical production frames before/after direct and bloom. Never changes rendering. */
    public static void rootLayer(String stage){
        if(!Boolean.getBoolean("wuxingxiantu.huahongRootLayers")||!started||done||clock<CAPTURE_START||clock>=END_TICK||frames%3!=0)return;
        var mc=Minecraft.getInstance();if(mc.player==null||!(mc.player.getVehicle() instanceof ControlledSwordEntity sword))return;
        var f=InterpolatedFlyingSwordTransform.get(sword,mc.getFrameTime());if(f.huahong()<.1F)return;
        try{
            var probe=FlightHeadProbe.snapshot();String file=String.format(Locale.ROOT,"layers/%s/%06d.png",stage,frames);
            var image=Screenshot.takeScreenshot(mc.getMainRenderTarget());
            var row=new LinkedHashMap<String,Object>();row.put("frame",frames);row.put("clock",clock);row.put("stage",stage);row.put("file",file);row.put("strength",f.huahong());row.put("headScreen",probe.get("headScreen"));row.put("headForward",probe.get("forward"));row.put("skin",sword.huahongSkin().name());row.put("depthStatus",TrailShaderBridge.depthStatus);row.put("depthSource",TrailShaderBridge.sourceFramebuffer);row.put("projectionM20",probe.get("rawProjectionM20"));row.put("projectionM21",probe.get("rawProjectionM21"));
            float[] range=new float[2];org.lwjgl.opengl.GL11.glGetFloatv(org.lwjgl.opengl.GL11.GL_DEPTH_RANGE,range);row.put("depthRange",range);ROOT_LAYERS.add(row);
            WRITER.execute(()->{try(image){Files.createDirectories(OUT.resolve(file).getParent());image.writeToFile(OUT.resolve(file));}catch(Throwable error){ERRORS.add(error.toString());}});
        }catch(Throwable error){throw new AssertionError("Root layer capture failed",error);}
    }
    private static void seedTrailClock()throws Exception{long origin=Long.getLong("wuxingxiantu.flightReproClockOrigin",0);if(origin==0)return;var f=FlyingSwordEffects.class.getDeclaredField("TRAIL_CLOCK");f.setAccessible(true);Object clock=f.get(null);var tick=clock.getClass().getDeclaredField("tick");tick.setAccessible(true);tick.setLong(clock,origin);SETUP_CHECKS.put("clockFixture",Map.of("origin",origin,"scope","Only seed empty QA trail clock age before summoning. No pose, velocity, partial or movement override; production clock then advances normally."));}
    private static void finish(Throwable error){
        if(done)return;done=true;started=false;if(error!=null){error.printStackTrace();ERRORS.add(error.toString());}
        try{
            WRITER.shutdown();if(!WRITER.awaitTermination(55,TimeUnit.SECONDS))ERRORS.add("PNG writer timeout");
            var report=new LinkedHashMap<String,Object>();report.put("frames",FRAMES);report.put("errors",ERRORS);report.put("pointer",PointerSafety.snapshot());report.put("cameraDriver","Shipping Yujian camera; QA changes only player look angles and F5 mode. No custom position/rotation hook.");report.put("scene","Flat preloaded route at Y113, time18000, vanilla clouds on, FOV70, actual speedMultiplier1; render-frame PNGs without exposure scaling.");report.put("skin",SKIN.name());report.put("ordinaryOnly",ORDINARY);report.put("stateFixture",ORDINARY?"Huahong entry cooldown held at1000000 only in test ride; real JIYU input/physics/speed remain intact.":"Natural ordinary then "+SKIN.name()+" entry.");
            if(FOLLOWUP){report.put("steepLook",Boolean.getBoolean("wuxingxiantu.flightSteepLook"));report.put("headFollowup",true);report.put("candidate",CANDIDATE);report.put("skin",ORDINARY?"ORDINARY":!SINGLE_SKIN.isBlank()?SINGLE_SKIN:"ALL8");report.put("stateFixture",ORDINARY?"Only Huahong entry cooldown is held; actual JIYU/roll input and all physics remain production.":"Natural Huahong entry, then real client cosmetic selection every60ticks; no velocity override. Switch0..9warm,10..25straight,26..35turn-settle,36..59steady-turn; skin0 starts before first transformation, so actual full-strength rows decide eligibility.");report.put("reuseWorld",REUSE_WORLD);report.put("scene","Saved own flat visual world with setup-only narrow predicted corridor warmup; never cold-streaming performance evidence. Shipping camera/FOV70, clouds,1280x720, no exposure scaling.");}
            report.put("clockOriginFixture",Long.getLong("wuxingxiantu.flightReproClockOrigin",0));report.put("configuredAltitude",System.getProperty("wuxingxiantu.flightReproAltitude","113"));report.put("configuredFps",Integer.getInteger("wuxingxiantu.flightReproFps",60));report.put("pngEveryFrames",Integer.getInteger("wuxingxiantu.flightSparsePng",1));report.put("configuredScenarioEndTick",END_TICK);if(!FRAMES.isEmpty())report.put("actualLastCapturedTick",FRAMES.get(FRAMES.size()-1).get("clock"));
            report.put("hiltUTurnInput",HILT_UTURN);if(HILT_UTURN)report.put("uTurnSchedule","At tick120 target yaw steps exactly180deg from tick119 then holds30ticks; same at300 from299, holds30ticks. Outside those windows original yaw curve resumes. Real steering/velocity and shipping camera remain active.");
            if(SKIN==HuahongSkin.DENSE){var check=denseExitBasis();report.put("exitVolumeBasis",check);if(!Boolean.TRUE.equals(check.get("passed")))ERRORS.add("DENSE exit basis check failed: "+check);}
            if(FRAMES.isEmpty())ERRORS.add("No actual mounted flight frames recorded");
            report.put("flowReviewSideLook",Boolean.getBoolean("wuxingxiantu.smokeFlowReview"));report.put("joinStress",Boolean.getBoolean("wuxingxiantu.flightJoinStress"));report.put("joinStressInputs","Yaw +180 at220, resumes4deg/tick at270; pitch sine40deg; sprint released230..254 and330..354, then restored. Shipping input/physics; no pose or velocity override.");report.put("rootLayers",ROOT_LAYERS);report.put("setupChecks",SETUP_CHECKS);Files.writeString(OUT.resolve("capture.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));Files.writeString(OUT.resolve(ERRORS.isEmpty()?"complete.txt":"failure.txt"),ERRORS.isEmpty()?"RECORDED — visual review required":String.join("\n",ERRORS));
        }catch(Throwable write){write.printStackTrace();}Minecraft.getInstance().stop();
    }
    private static Map<String,Object> denseExitBasis(){
        var packets=new TreeMap<Long,HuahongVolumeRenderer.Trace>();
        for(var row:FRAMES)if(row.get("volume") instanceof Map<?,?> volume)for(String key:List.of("lastPackets","currentPackets"))
            if(volume.get(key) instanceof List<?> traces)for(Object value:traces)if(value instanceof HuahongVolumeRenderer.Trace trace&&trace.submitted())packets.put(trace.frame(),trace);
        HuahongVolumeRenderer.Trace lastActive=null;var inactive=new ArrayList<HuahongVolumeRenderer.Trace>();
        for(var trace:packets.values())if(trace.strength()>.001F){lastActive=trace;inactive.clear();}else if(lastActive!=null)inactive.add(trace);
        var result=new LinkedHashMap<String,Object>();double within=0,boundary=0,time=0,age=0;boolean adjacent=false;
        if(!inactive.isEmpty()){
            var first=inactive.get(0);adjacent=lastActive!=null&&first.frame()==lastActive.frame()+1;
            if(adjacent)boundary=basisDifference(lastActive.worldToMaterial(),first.worldToMaterial());
            for(int i=0;i<inactive.size();i++){
                var trace=inactive.get(i);within=Math.max(within,basisDifference(first.worldToMaterial(),trace.worldToMaterial()));
                if(i>0)time+=(trace.effectTime()-inactive.get(i-1).effectTime()+128)%128;
            }
            age=inactive.get(inactive.size()-1).pathAges().get(0)-first.pathAges().get(0);
            result.put("lastActivePacketFrame",lastActive.frame());result.put("firstInactivePacketFrame",first.frame());result.put("lastInactivePacketFrame",inactive.get(inactive.size()-1).frame());
        }
        result.put("passed",inactive.size()>=5&&within<=1e-6&&(!adjacent||boundary<=1e-6)&&time>.1&&age>.1);
        result.put("actualSubmittedPackets",packets.size());result.put("inactivePackets",inactive.size());result.put("maxInactiveBasisDifference",within);result.put("activeInactiveAdjacent",adjacent);
        if(adjacent)result.put("lastActiveToFirstInactiveBasisDifference",boundary);
        result.put("inactiveEffectTimeAdvancedSeconds",time);result.put("inactiveHeadAgeAdvancedSeconds",age);result.put("scope","Actual submitted Trace packets deduplicated/sorted by renderer frame. Boundary comparison only asserted for adjacent actual packets; inactive basis compared throughout. Separate source frames remain visual evidence.");return result;
    }
    private static double basisDifference(List<Float> a,List<Float> b){double delta=0;for(int i=0;i<9;i++)delta=Math.max(delta,Math.abs(a.get(i)-b.get(i)));return delta;}
}
