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
    private static final boolean ORDINARY=Boolean.getBoolean("wuxingxiantu.huahongReproOrdinary");
    private static final boolean HILT_UTURN=Boolean.getBoolean("wuxingxiantu.huahongReproHiltUTurn");
    private static final HuahongSkin SKIN=HuahongSkin.valueOf(System.getProperty("wuxingxiantu.huahongReproSkin","SMOKE").toUpperCase(Locale.ROOT));
    private static final int END_TICK=Integer.getInteger("wuxingxiantu.huahongReproEndTick",420);
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
    private static float baseYaw(long t){return t<70?0:t<160?(float)((t-70)*3):(float)(270+(t-160)*5);}
    private static float yaw(long t){if(HILT_UTURN){if(t>=120&&t<150)return baseYaw(119)+180;if(t>=300&&t<330)return baseYaw(299)+180;}return baseYaw(t);}
    private static float pitch(long t){return (float)(-5+7*Math.sin(Math.max(0,t-100)*Math.PI/100));}
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent event){
        if(!ENABLED||done||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            Files.createDirectories(OUT.resolve("frames"));GLFW.glfwHideWindow(mc.getWindow().getWindow());mc.setWindowActive(true);mc.options.pauseOnLostFocus=false;
            if((System.nanoTime()-START)/1e9>600)throw new AssertionError("Reproduction watchdog");
            if(!opened&&mc.player!=null)throw new AssertionError("Refusing existing world");
            if(!opened&&mc.getOverlay()==null&&(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen||mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)){
                opened=true;mc.options.enableVsync().set(false);mc.getWindow().updateVsync(false);mc.getWindow().setWindowed(1280,720);mc.resizeDisplay();mc.options.framerateLimit().set(60);mc.options.fov().set(70);mc.options.renderDistance().set(8);mc.options.simulationDistance().set(6);mc.options.cloudStatus().set(CloudStatus.FANCY);mc.options.bobView().set(false);mc.options.hideGui=false;
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("flight-continuous-"+System.currentTimeMillis(),new LevelSettings("Flight continuous reproduction",GameType.SURVIVAL,false,net.minecraft.world.Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(7215,false,false),a->a.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions());return;
            }
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null||mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen)return;
            if(mc.screen!=null)mc.setScreen(null);
            if(!ready){
                ready=true;playerId=mc.player.getUUID();FlyingSwordConfig.TRAILS.set(true);FlyingSwordConfig.TRAIL_PATH.set(FlyingSwordConfig.TrailPath.AUTO);HuahongSkinPreferences.select(SKIN);
                preparation=mc.getSingleplayerServer().submitAsync(()->prepare(mc.getSingleplayerServer().getPlayerList().getPlayer(playerId)));return;
            }
            if(preparation!=null){if(!preparation.isDone())return;preparation.join();preparation=null;PointerSafety.verify();runtimeEvidence(mc);if(OrdinaryHiltQa.LAYERS)SETUP_CHECKS.put("diagnosticOnly","Old frozen production outputs: layered observations; setup GPU checks not repeated against newly edited QA sources.");else{SETUP_CHECKS.put("sectionCadence",FlightSectionCadenceQa.verify());SETUP_CHECKS.put("material",HuahongMaterialQa.verify(mc));SETUP_CHECKS.put("path",HuahongPathQa.verify());if(SKIN==HuahongSkin.DENSE)SETUP_CHECKS.put("volumeMaterial",HuahongVolumeMaterialQa.verify(mc));}started=true;}
            if(mc.player.getVehicle() instanceof ControlledSwordEntity){if(!YujianClientController.active())YujianClientController.setActive(true);mc.options.setCameraType(clock<260?CameraType.THIRD_PERSON_FRONT:CameraType.THIRD_PERSON_BACK);}
            mc.player.setYRot(yaw(clock));mc.player.setXRot(pitch(clock));
            if(clock>=END_TICK)finish(null);
        }catch(Throwable error){finish(error);}
    }
    private static void prepare(ServerPlayer p){
        var l=p.serverLevel();
        // Flat, explicitly loaded visual route: this recording is not cold-generation performance evidence.
        for(int x=-20;x<=32;x++)for(int z=-20;z<=32;z++)l.getChunk(x,z);
        l.setDayTime(18000);l.setWeatherParameters(12000,0,false,false);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,l.getServer());
        p.setNoGravity(true);p.removeAllEffects();p.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(WuXingItems.PURE_YANG_SWORD.get()));p.getInventory().selected=0;
        var d=GeneratedMod.data(p);d.wuxingxiantu$setRealm(99);d.wuxingxiantu$learnArt(YujianController.FLIGHT);d.wuxingxiantu$setArtMastery(YujianController.FLIGHT,100);d.wuxingxiantu$setMaxMana(1000000);d.wuxingxiantu$setAura(1000000);p.getPersistentData().putLong("YujianCooldown_DUNXU",Long.MAX_VALUE);
        FlyingSwordConfig.SPEED_MULTIPLIER.set(1.0);GeneratedMod.sync(p);p.teleportTo(l,8,113,-24,0,0);
    }
    @SubscribeEvent public static void input(TickEvent.LevelTickEvent event){
        if(!ENABLED||!started||event.phase!=TickEvent.Phase.START||!(event.level instanceof net.minecraft.server.level.ServerLevel l))return;
        var p=l.getServer().getPlayerList().getPlayer(playerId);if(p==null||p.serverLevel()!=l)return;
        if(epoch<0)epoch=l.getGameTime();clock=l.getGameTime()-epoch;
        boolean forward=clock>=10&&clock<390,sprint=(ORDINARY?clock>=20:clock>=160)&&forward,roll=clock>=190&&clock<245;
        if(ORDINARY)try{
            var rides=YujianController.class.getDeclaredField("RIDES");rides.setAccessible(true);Object ride=((Map<?,?>)rides.get(null)).get(p.getUUID());
            if(ride!=null){var flight=ride.getClass().getDeclaredField("huahong");flight.setAccessible(true);Object state=flight.get(ride);var cooldown=state.getClass().getDeclaredField("cooldown");cooldown.setAccessible(true);cooldown.setInt(state,1000000);}
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
        YujianController.handleInput(p,clock<10,!summoned,forward,clock>=390,false,false,false,roll,sprint,false,1,0,yaw(clock),pitch(clock));summoned=true;
    }
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event){
        if(!ENABLED||!started||done||event.phase!=TickEvent.Phase.END||clock<40||clock>=END_TICK)return;
        var mc=Minecraft.getInstance();if(mc.player==null||!(mc.player.getVehicle() instanceof ControlledSwordEntity s))return;
        try{
            long before=System.nanoTime();if(videoStart==0)videoStart=before;
            var image=Screenshot.takeScreenshot(mc.getMainRenderTarget());long after=System.nanoTime();int index=frames++;
            var f=InterpolatedFlyingSwordTransform.get(s,event.renderTickTime);var row=new LinkedHashMap<String,Object>();
            row.put("frame",index);row.put("nanoTime",before);row.put("seconds",(before-videoStart)/1e9);row.put("clock",clock);row.put("partial",event.renderTickTime);row.put("speed",s.renderSpeed(1));row.put("strength",f.huahong());row.put("stage",s.renderHuahongStage());row.put("position",List.of(f.position().x,f.position().y,f.position().z));row.put("roll",f.roll());row.put("camera",mc.options.getCameraType().name());row.put("readbackMs",(after-before)/1e6);row.put("shaderPack",TrailShaderBridge.shaderPackDetected());row.put("path",FlyingSwordEffects.activeTrailRenderPath);row.put("normalVertices",FlyingSwordEffects.normalTrailVertexCount-HuahongVisuals.vertices);row.put("rainbowVertices",HuahongVisuals.vertices);row.put("file",String.format(Locale.ROOT,"frames/%06d.png",index));FRAMES.add(row);
            if(SKIN==HuahongSkin.DENSE)row.put("volume",HuahongVolumeRenderer.diagnostics());
            if(OrdinaryHiltQa.ENABLED)row.put("hilt",OrdinaryHiltQa.snapshot());
            if(HILT_UTURN){row.put("inputYaw",yaw(clock));row.put("actualYaw",f.yaw());row.put("actualPitch",f.pitch());}
            WRITER.execute(()->{try(image){image.writeToFile(OUT.resolve((String)row.get("file")));}catch(Throwable error){ERRORS.add(error.toString());}});
        }catch(Throwable error){finish(error);}
    }
    private static void runtimeEvidence(Minecraft mc)throws Exception{
        var classes=new LinkedHashMap<String,Object>();
        for(String name:List.of("GeneratedMod","yujian.ControlledSwordEntity","yujian.YujianController","yujian.FlightInterpolation","yujian.FlightCollision","yujian.FlightRootTicking","yujian.FlightChunkStreaming","yujian.FlightTerrainGovernor","client.FlyingSwordEffects","client.HuahongVisuals","client.HuahongFlightPath","client.HuahongSkinGeometry","client.HuahongFlowField","client.HuahongVolumeRenderer","client.InterpolatedFlyingSwordTransform","client.SwordTrailPhase","client.ControlledSwordRenderer","client.SwordFlightAttachment")){
            String full="cn.blockforge.generated.generatedmod."+name;Class<?> type=Class.forName(full);
            try(var stream=type.getResourceAsStream("/"+full.replace('.','/')+".class")){
                classes.put(full,Map.of("sha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes())),"codeSource",type.getProtectionDomain().getCodeSource().getLocation().toString()));
            }
        }
        try{Class<?> type=Class.forName("cn.blockforge.generated.generatedmod.client.SwordTailClipper");try(var stream=type.getResourceAsStream("/"+type.getName().replace('.','/')+".class")){classes.put(type.getName(),Map.of("sha256",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes())),"codeSource",type.getProtectionDomain().getCodeSource().getLocation().toString()));}}catch(ClassNotFoundException oldBaseline){}
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
    private static void finish(Throwable error){
        if(done)return;done=true;started=false;if(error!=null){error.printStackTrace();ERRORS.add(error.toString());}
        try{
            WRITER.shutdown();if(!WRITER.awaitTermination(55,TimeUnit.SECONDS))ERRORS.add("PNG writer timeout");
            var report=new LinkedHashMap<String,Object>();report.put("frames",FRAMES);report.put("errors",ERRORS);report.put("pointer",PointerSafety.snapshot());report.put("cameraDriver","Shipping Yujian camera; QA changes only player look angles and F5 mode. No custom position/rotation hook.");report.put("scene","Flat preloaded route at Y113, time18000, vanilla clouds on, FOV70, actual speedMultiplier1; render-frame PNGs without exposure scaling.");report.put("skin",SKIN.name());report.put("ordinaryOnly",ORDINARY);report.put("stateFixture",ORDINARY?"Huahong entry cooldown held at1000000 only in test ride; real JIYU input/physics/speed remain intact.":"Natural ordinary then "+SKIN.name()+" entry.");
            report.put("configuredScenarioEndTick",END_TICK);if(!FRAMES.isEmpty())report.put("actualLastCapturedTick",FRAMES.get(FRAMES.size()-1).get("clock"));
            report.put("hiltUTurnInput",HILT_UTURN);if(HILT_UTURN)report.put("uTurnSchedule","At tick120 target yaw steps exactly180deg from tick119 then holds30ticks; same at300 from299, holds30ticks. Outside those windows original yaw curve resumes. Real steering/velocity and shipping camera remain active.");
            if(SKIN==HuahongSkin.DENSE){var check=denseExitBasis();report.put("exitVolumeBasis",check);if(!Boolean.TRUE.equals(check.get("passed")))ERRORS.add("DENSE exit basis check failed: "+check);}
            report.put("setupChecks",SETUP_CHECKS);Files.writeString(OUT.resolve("capture.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));Files.writeString(OUT.resolve(ERRORS.isEmpty()?"complete.txt":"failure.txt"),ERRORS.isEmpty()?"RECORDED — visual review required":String.join("\n",ERRORS));
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
