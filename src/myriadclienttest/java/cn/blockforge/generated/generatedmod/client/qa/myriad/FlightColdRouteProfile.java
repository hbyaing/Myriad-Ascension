package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.*;
import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.yujian.*;
import com.google.gson.GsonBuilder;
import jdk.jfr.Recording;
import jdk.jfr.Configuration;
import net.minecraft.client.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** Independent normal-terrain cold route. No screenshot/readback/mesh capture or future-route warming. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class FlightColdRouteProfile {
    private static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.flightColdProfile");
    private static final boolean PREPARE=Boolean.getBoolean("wuxingxiantu.flightColdPrepare");
    private static final boolean SOLAS=Boolean.getBoolean("wuxingxiantu.flightColdSolas");
    private static final Path OUT=Path.of(System.getProperty("wuxingxiantu.flightColdOutput","../../reports/cold-flight-profile"));
    private static final String WORLD=System.getProperty("wuxingxiantu.flightColdWorld","cold-spawn-f214");
    private static final long STARTED=System.nanoTime();
    private static final List<Map<String,Object>> WARMUP=new ArrayList<>(),SERVER=Collections.synchronizedList(new ArrayList<>()),CLIENT=new ArrayList<>();
    private static final List<String> ERRORS=new ArrayList<>();
    private static final ArrayDeque<Map<String,Object>> WINDOW=new ArrayDeque<>();
    private static final Map<String,Object> SUMMARY=new ConcurrentHashMap<>();
    private static volatile boolean flight,ready;
    private static volatile boolean done;
    private static boolean opened,summoned,runtimeWritten,lateWarmupReported;
    private static long warmupBegan,lastProbe,flightBegan,serverBegan;
    private static volatile long epoch=-1,clock=-1,readyServerTicks;
    private static volatile double lastServerWallMs;
    private static UUID playerId;
    private static CompletableFuture<?> preparation;
    private static CompletableFuture<?> snapshotCleanup;
    private static Recording recording;
    private static String termination="";

    @SubscribeEvent public static void client(TickEvent.ClientTickEvent event){
        if(!ENABLED||done||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            Files.createDirectories(OUT);GLFW.glfwHideWindow(mc.getWindow().getWindow());mc.setWindowActive(true);mc.options.pauseOnLostFocus=false;
            if((System.nanoTime()-STARTED)/1e9>1800)throw new AssertionError("Cold profile total watchdog; no stable/complete result asserted");
            if(!opened&&mc.player!=null)throw new AssertionError("Refusing an existing user world");
            if(!opened&&mc.getOverlay()==null&&(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen||mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)){
                opened=true;mc.options.renderDistance().set(32);mc.options.simulationDistance().set(Integer.getInteger("wuxingxiantu.flightColdSimulation",32));mc.options.framerateLimit().set(260);mc.options.enableVsync().set(true);mc.getWindow().updateVsync(true);
                mc.getWindow().setWindowed(2560,1440);mc.resizeDisplay();mc.options.fov().set(70);mc.options.cloudStatus().set(CloudStatus.FANCY);mc.options.bobView().set(false);mc.options.hideGui=true;mc.options.save();
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);startJfr();
                Path save=mc.gameDirectory.toPath().resolve("saves").resolve(WORLD);
                if(PREPARE){
                    if(Files.exists(save.resolve("level.dat")))throw new AssertionError("Refusing to overwrite prep snapshot "+save);
                    var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                    rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);
                    mc.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings("Cold32 stable spawn",GameType.SURVIVAL,false,net.minecraft.world.Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(7215,true,false),net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions);
                }else{
                    if(!Files.isRegularFile(save.resolve("level.dat")))throw new AssertionError("Missing copied stable spawn snapshot "+save);
                    mc.createWorldOpenFlows().loadLevel(new net.minecraft.client.gui.screens.TitleScreen(),WORLD);
                }return;
            }
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null||mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen)return;
            if(mc.screen!=null)mc.setScreen(null);
            if(snapshotCleanup!=null){if(!snapshotCleanup.isDone())return;snapshotCleanup.join();termination="Stable stationary spawn cleaned of QA ride and saved for cloning; no flight performed";finish(null);return;}
            if(!ready&&preparation==null){playerId=mc.player.getUUID();FlyingSwordConfig.TRAILS.set(true);FlyingSwordConfig.TRAIL_PATH.set(FlyingSwordConfig.TrailPath.AUTO);HuahongSkinPreferences.select(HuahongSkin.SMOKE);preparation=mc.getSingleplayerServer().submitAsync(()->preparePlayer(mc.getSingleplayerServer().getPlayerList().getPlayer(playerId)));return;}
            if(preparation!=null){if(!preparation.isDone())return;preparation.join();preparation=null;ready=true;warmupBegan=System.nanoTime();}
            if(!runtimeWritten){runtimeEvidence(mc);runtimeWritten=true;}
            mc.player.setYRot(0);mc.player.setXRot(-2);mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            if(mc.player.getVehicle() instanceof ControlledSwordEntity&&!YujianClientController.active())YujianClientController.setActive(true);
            HuahongFrameTimings.sceneClock=flight?clock:-1;
            long now=System.nanoTime();
            if(!flight&&now-lastProbe>=1_000_000_000L){
                lastProbe=now;Map<String,Object> probe=FlightColdWarmupProbe.snapshot(mc);WARMUP.add(probe);WINDOW.addLast(probe);
                while(WINDOW.size()>2&&((Number)((Map<?,?>)WINDOW.toArray()[1]).get("nanoTime")).longValue()<=now-5_000_000_000L)WINDOW.removeFirst();
                var stability=FlightColdWarmupProbe.stability(WINDOW.getFirst(),probe,lastServerWallMs);SUMMARY.put("latestStability",stability);SUMMARY.put("warmupElapsedSeconds",(now-warmupBegan)/1e9);
                if((now-warmupBegan)>120_000_000_000L&&!lateWarmupReported){lateWarmupReported=true;System.out.println("[Cold32] Initial resident loading still busy after120s; continuing stationary, not a flight result.");}
                progress("warmup",probe);
                if(Boolean.TRUE.equals(stability.get("stable"))&&mc.player.getVehicle() instanceof ControlledSwordEntity){
                    SUMMARY.put("stableWarmup",true);SUMMARY.put("stableWarmupAtNanoTime",now);SUMMARY.put("warmupDecision",stability);
                    if(PREPARE){ready=false;snapshotCleanup=mc.getSingleplayerServer().submitAsync(()->cleanSnapshot(mc.getSingleplayerServer().getPlayerList().getPlayer(playerId)));return;}
                    PointerSafety.verify();flight=true;flightBegan=now;System.out.println("[Cold32] Stable resident start reached; starting real input into ungenerated route.");
                }
            }
            if(flight){
                if(now-lastProbe>=1_000_000_000L){lastProbe=now;progress("flight",FlightColdWarmupProbe.snapshot(mc));}
                if(clock>=640){termination="Completed600 forward ticks plus40 brake ticks";finish(null);}
                else if((now-flightBegan)>120_000_000_000L){termination="Flight wall budget120s; actual completed ticks recorded, not a completion pass";finish(null);}
            }
        }catch(Throwable error){finish(error);}
    }
    private static void preparePlayer(ServerPlayer p){
        var l=p.serverLevel();l.setDayTime(18000);l.setWeatherParameters(12000,0,false,false);
        p.setNoGravity(true);p.removeAllEffects();p.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(WuXingItems.PURE_YANG_SWORD.get()));p.getInventory().selected=0;
        var d=GeneratedMod.data(p);d.wuxingxiantu$setRealm(99);d.wuxingxiantu$learnArt(YujianController.FLIGHT);d.wuxingxiantu$setArtMastery(YujianController.FLIGHT,100);d.wuxingxiantu$setMaxMana(1000000);d.wuxingxiantu$setAura(1000000);
        p.getPersistentData().putLong("YujianCooldown_DUNXU",Long.MAX_VALUE);FlyingSwordConfig.SPEED_MULTIPLIER.set(1.0);GeneratedMod.sync(p);
        // Spawn-only read: never loads/generates a future route corridor.
        double y=Math.max(220,l.getHeight(Heightmap.Types.MOTION_BLOCKING,8,-24)+64);p.teleportTo(l,8,y,-24,0,-2);
    }
    private static void cleanSnapshot(ServerPlayer p){
        YujianController.stop(p);p.stopRiding();int removed=0;
        for(var entity:p.serverLevel().getAllEntities())if(entity instanceof ControlledSwordEntity s&&p.getUUID().equals(s.ownerId)){s.discard();removed++;}
        p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.serverLevel().save(null,true,false);SUMMARY.put("snapshotOwnedSwordEntitiesRemovedAfterNormalStop",removed);SUMMARY.put("snapshotPlayerMounted",p.isPassenger());
    }
    @SubscribeEvent public static void input(TickEvent.LevelTickEvent event){
        if(!ENABLED||done||!ready||event.phase!=TickEvent.Phase.START||!(event.level instanceof ServerLevel l))return;
        var p=l.getServer().getPlayerList().getPlayer(playerId);if(p==null||p.serverLevel()!=l)return;
        readyServerTicks++;if(flight){if(epoch<0)epoch=l.getGameTime();clock=l.getGameTime()-epoch;}
        boolean forward=flight&&clock<600,brake=flight&&clock>=600;
        YujianController.handleInput(p,readyServerTicks<=10,!summoned,forward,brake,false,false,false,false,forward&&clock>=20,false,1,0,0,-2);summoned=true;
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event){
        if(!ENABLED||done||event.getServer()!=Minecraft.getInstance().getSingleplayerServer())return;
        if(event.phase==TickEvent.Phase.START){serverBegan=System.nanoTime();return;}
        if(serverBegan!=0)lastServerWallMs=(System.nanoTime()-serverBegan)/1e6;
        if(!ready)return;var p=event.getServer().getPlayerList().getPlayer(playerId);if(p==null)return;
        var row=new LinkedHashMap<String,Object>();row.put("nanoTime",System.nanoTime());row.put("wallClockMillis",System.currentTimeMillis());row.put("phase",flight?"flight":"warmup");row.put("clock",clock);row.put("serverTickWallMs",lastServerWallMs);row.put("position",List.of(p.getX(),p.getY(),p.getZ()));
        if(p.getVehicle() instanceof ControlledSwordEntity s){row.put("speed",s.flightSpeed());row.put("targetSpeed",s.targetSpeed());row.put("huahongStage",s.huahongStage());row.put("huahongStrength",s.huahongStrength());row.put("aheadLoaded",s.aheadLoaded());row.put("entityTick",s.tickCount);}
        int roots=0;for(var entity:p.serverLevel().getAllEntities())if(entity instanceof ControlledSwordEntity s&&s.flightRoot()&&p.getUUID().equals(s.ownerId))roots++;row.put("ownedRootCount",roots);
        row.put("streamRider",FlightChunkStreaming.diagnostics(p));row.put("streamLevel",FlightChunkStreaming.diagnostics(p.serverLevel()));SERVER.add(row);
    }
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event){
        if(!ENABLED||done||!ready||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.player==null)return;
        var row=new LinkedHashMap<String,Object>();row.put("nanoTime",System.nanoTime());row.put("clock",flight?clock:-1);row.put("phase",flight?"flight":"warmup");row.put("position",List.of(mc.player.getX(),mc.player.getY(),mc.player.getZ()));
        if(mc.player.getVehicle() instanceof ControlledSwordEntity s){row.put("entityTick",s.tickCount);row.put("speed",s.renderSpeed(mc.getFrameTime()));row.put("huahongStage",s.renderHuahongStage());row.put("strength",s.renderHuahong(mc.getFrameTime()));}
        CLIENT.add(row);
    }
    private static void startJfr()throws Exception{
        recording=new Recording(Configuration.getConfiguration("profile"));recording.enable("jdk.NativeMethodSample").withPeriod(Duration.ofMillis(10));recording.enable("jdk.JavaMonitorEnter").withThreshold(Duration.ofMillis(10));recording.enable("jdk.ThreadPark").withThreshold(Duration.ofMillis(10));recording.setName("Cold32-"+(PREPARE?"prepare":"flight"));recording.setMaxSize(512L*1024*1024);recording.setMaxAge(Duration.ofMinutes(25));recording.start();
    }
    private static void runtimeEvidence(Minecraft mc)throws Exception{
        var classes=new LinkedHashMap<String,Object>();
        for(String name:List.of("yujian.ControlledSwordEntity","yujian.YujianController","yujian.FlightChunkStreaming","yujian.FlightTerrainGovernor","yujian.FlightRootTicking","client.FlyingSwordEffects","client.HuahongSkinGeometry","client.HuahongVolumeRenderer","client.JindouCloudRenderer")){
            var c=Class.forName("cn.blockforge.generated.generatedmod."+name);try(var in=c.getResourceAsStream("/"+c.getName().replace('.','/')+".class")){classes.put(c.getName(),Map.of("sha256",sha(in.readAllBytes()),"codeSource",c.getProtectionDomain().getCodeSource().getLocation().toString()));}
        }
        var iris=Class.forName("net.irisshaders.iris.Iris");String pack=String.valueOf(iris.getMethod("getCurrentPackName").invoke(null));boolean inUse=(Boolean)iris.getMethod("isPackInUseQuick").invoke(null);
        if(SOLAS?(!inUse||!pack.equals("Solas Shader V3.7b.zip")):inUse)throw new AssertionError("Unexpected shader state "+pack+" / "+inUse);
        var report=new LinkedHashMap<String,Object>();report.put("classes",classes);report.put("shaderInUse",inUse);report.put("shaderPack",pack);report.put("renderDistance",mc.options.renderDistance().get());report.put("simulationDistance",mc.options.simulationDistance().get());report.put("maxFps",mc.options.framerateLimit().get());report.put("vsyncSetting",mc.options.enableVsync().get());report.put("renderTarget",List.of(mc.getMainRenderTarget().viewWidth,mc.getMainRenderTarget().viewHeight));report.put("availableProcessors",Runtime.getRuntime().availableProcessors());report.put("maxHeapBytes",Runtime.getRuntime().maxMemory());report.put("world",WORLD);report.put("prepareOnly",PREPARE);report.put("screenshotsOrGpuReadback",false);report.put("futureRoutePreGeneration",false);report.put("pointer",PointerSafety.snapshot());report.put("runtimeClassScope","Runtime-resolved class resources, not post-Mixin JVM instrumentation.");
        Path cfg=mc.gameDirectory.toPath().resolve("config/embeddium-options.json");if(Files.isRegularFile(cfg)){report.put("embeddiumConfig",Files.readString(cfg));report.put("embeddiumConfigSha256",sha(Files.readAllBytes(cfg)));}
        Files.writeString(OUT.resolve("runtime.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }
    private static String sha(byte[] data)throws Exception{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data));}
    private static void progress(String phase,Map<String,Object> probe)throws Exception{
        var p=new LinkedHashMap<String,Object>();p.put("phase",phase);p.put("clock",clock);p.put("elapsedSeconds",(System.nanoTime()-STARTED)/1e9);p.put("warmupElapsedSeconds",(System.nanoTime()-warmupBegan)/1e9);p.put("latestStability",SUMMARY.getOrDefault("latestStability",Map.of()));p.put("probe",probe);p.put("lastServerTickWallMs",lastServerWallMs);Files.writeString(OUT.resolve("progress.json"),new GsonBuilder().setPrettyPrinting().create().toJson(p));
    }
    private static void finish(Throwable error){
        if(done)return;done=true;if(error!=null){error.printStackTrace();ERRORS.add(error.toString());}
        try{
            if(recording!=null){recording.stop();recording.dump(OUT.resolve("profile.jfr"));recording.close();}
            FlightChunkWorkTimings.write(OUT);FlightVfxWorkTimings.write(OUT);HuahongFrameTimings.write(OUT);SUMMARY.put("termination",termination);SUMMARY.put("errors",ERRORS);SUMMARY.put("pointer",PointerSafety.snapshot());SUMMARY.put("world",WORLD);SUMMARY.put("prepareOnly",PREPARE);SUMMARY.put("warmup",WARMUP);synchronized(SERVER){SUMMARY.put("server",List.copyOf(SERVER));}SUMMARY.put("client",CLIENT);SUMMARY.put("flightTicksCompleted",clock);SUMMARY.put("wallSeconds",(System.nanoTime()-STARTED)/1e9);SUMMARY.put("scene","Normal terrain seed7215 with structures; render32 with recorded simulation distance, VSync setting on, maxFps260, unmodified sword speed and production SMOKE; stationary warmup then straight cold route. No future route getChunk calls, no screenshot or mesh readback.");
            Files.writeString(OUT.resolve("profile-summary.json"),new GsonBuilder().setPrettyPrinting().create().toJson(SUMMARY));
            Files.writeString(OUT.resolve(ERRORS.isEmpty()?"complete.txt":"failure.txt"),ERRORS.isEmpty()?"RECORDED — performance analysis required":String.join("\n",ERRORS));
        }catch(Throwable write){write.printStackTrace();}Minecraft.getInstance().stop();
    }
    private FlightColdRouteProfile(){}
}
