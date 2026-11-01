package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.*;
import cn.blockforge.generated.generatedmod.client.*;
import com.google.gson.GsonBuilder;
import net.minecraft.client.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Real divine activation and vanilla client flight input; never assigns a trajectory. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class JindouReproCapture {
    private static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.jindouRepro"),CANDIDATE=Boolean.getBoolean("wuxingxiantu.jindouReproCandidate"),SOLAS=Boolean.getBoolean("wuxingxiantu.jindouReproSolas");
    private static final Path OUT=Path.of(System.getProperty("wuxingxiantu.jindouReproOutput","../../reports/jindou-repro"));
    private static final String REUSE_WORLD=System.getProperty("wuxingxiantu.jindouReproWorld","");
    private static final boolean CHECKS_ONLY=Boolean.getBoolean("wuxingxiantu.jindouReproChecksOnly");
    private static final long START=System.nanoTime();
    private static final List<Map<String,Object>> FRAMES=new ArrayList<>(),SERVER=Collections.synchronizedList(new ArrayList<>());
    private static final List<String> ERRORS=Collections.synchronizedList(new ArrayList<>());
    private static final Map<String,Object> CHECKS=new LinkedHashMap<>();
    private static final ThreadPoolExecutor WRITER=new ThreadPoolExecutor(2,2,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(8),r->{var t=new Thread(r,"cloud-repro-png");t.setDaemon(true);return t;},new ThreadPoolExecutor.CallerRunsPolicy());
    private static volatile boolean started,done;
    private static boolean opened,prepared;
    private static CompletableFuture<?> preparation;
    private static UUID playerId;
    private static long epoch,videoStart;
    private static volatile long clock;
    private static int frameIndex;
    private static float yaw(long t){return t<140?0:t<220?(float)((t-140)*5):(float)(400+(t-220)*3);}
    private static boolean forward(){return clock>=40&&clock<300;}
    private static boolean sprint(){return clock>=100&&clock<300;}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void input(TickEvent.ClientTickEvent event){
        if(!ENABLED||done||!started||event.phase!=TickEvent.Phase.START)return;var mc=Minecraft.getInstance();if(mc.player==null)return;
        clock=mc.player.tickCount-epoch;mc.options.keyUp.setDown(forward());mc.options.keySprint.setDown(sprint());mc.options.keyDown.setDown(false);mc.options.keyLeft.setDown(false);mc.options.keyRight.setDown(false);mc.options.keyJump.setDown(false);mc.options.keyShift.setDown(false);
        mc.player.setYRot(yaw(clock));mc.player.setXRot(-2);mc.options.setCameraType(clock<230?CameraType.THIRD_PERSON_FRONT:CameraType.THIRD_PERSON_BACK);
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent event){
        if(!ENABLED||done||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        try{
            Files.createDirectories(OUT.resolve("frames"));GLFW.glfwHideWindow(mc.getWindow().getWindow());mc.setWindowActive(true);mc.options.pauseOnLostFocus=false;
            if((System.nanoTime()-START)/1e9>600)throw new AssertionError("Cloud reproduction watchdog");
            if(!opened&&mc.player!=null)throw new AssertionError("Refusing an existing user world");
            if(!opened&&mc.getOverlay()==null&&(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen||mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)){
                opened=true;mc.options.enableVsync().set(false);mc.getWindow().updateVsync(false);mc.getWindow().setWindowed(1280,720);mc.resizeDisplay();mc.options.framerateLimit().set(60);mc.options.fov().set(70);mc.options.renderDistance().set(8);mc.options.simulationDistance().set(6);mc.options.cloudStatus().set(CloudStatus.FANCY);mc.options.bobView().set(false);mc.options.hideGui=true;
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                if(!REUSE_WORLD.isBlank()){
                    Path saves=mc.gameDirectory.toPath().resolve("saves").toAbsolutePath().normalize(),save=saves.resolve(REUSE_WORLD).normalize();
                    if(!REUSE_WORLD.startsWith("cloud-real-input-")||REUSE_WORLD.contains("/")||REUSE_WORLD.contains("\\")||!save.getParent().equals(saves)||!Files.isRegularFile(save.resolve("level.dat")))throw new AssertionError("Refusing non-QA cloud world "+save);
                    mc.createWorldOpenFlows().loadLevel(new net.minecraft.client.gui.screens.TitleScreen(),REUSE_WORLD);return;
                }
                mc.createWorldOpenFlows().createFreshLevel("cloud-real-input-"+System.currentTimeMillis(),new LevelSettings("Cloud real input reproduction",GameType.SURVIVAL,false,net.minecraft.world.Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(7215,false,false),a->a.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions());return;
            }
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null||mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen)return;
            if(mc.screen!=null)mc.setScreen(null);
            if(!prepared){prepared=true;playerId=mc.player.getUUID();preparation=mc.getSingleplayerServer().submitAsync(()->prepare(mc.getSingleplayerServer().getPlayerList().getPlayer(playerId)));return;}
            if(preparation!=null){if(!preparation.isDone())return;preparation.join();preparation=null;}
            if(!started){
                if(!"jin_dou_yun".equals(GeneratedMod.data(mc.player).wuxingxiantu$getActiveDivineArt())||!mc.player.getAbilities().flying)return;
                PointerSafety.verify();runtimeEvidence(mc);if(CANDIDATE){CHECKS.put("dunxuAlphaBuffer",DunxuAlphaBufferQa.verify(mc));CHECKS.put("cloudMaterial",JindouCloudMaterialQa.verify(mc));CHECKS.put("pathWindow",Class.forName("cn.blockforge.generated.generatedmod.client.qa.myriad.HuahongPathWindowQa").getMethod("verify").invoke(null));}if(CHECKS_ONLY){finish(null);return;}epoch=mc.player.tickCount;started=true;
            }
            if(clock>=400)finish(null);
        }catch(Throwable error){finish(error);}
    }
    private static void prepare(ServerPlayer p){
        var l=p.serverLevel();for(int x=-24;x<=24;x++)for(int z=-16;z<=64;z++)l.getChunk(x,z);
        l.setDayTime(18000);l.setWeatherParameters(12000,0,false,false);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,l.getServer());
        p.removeAllEffects();var d=GeneratedMod.data(p);
        if(!REUSE_WORLD.isBlank()){DivineArtLogic.clear(p);p.getAbilities().setFlyingSpeed(.05F);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);d.wuxingxiantu$setDivineCooldown("jin_dou_yun",0);}
        p.teleportTo(l,8,120,-24,0,-2);d.wuxingxiantu$setRealm(99);d.wuxingxiantu$setMaxMana(1000000);d.wuxingxiantu$setAura(1000000);d.wuxingxiantu$learnDivineArt("jin_dou_yun");d.wuxingxiantu$setEquippedDivineArt("jin_dou_yun");GeneratedMod.sync(p);
        if(!DivineArtLogic.release(p,"jin_dou_yun"))throw new AssertionError("Real cloud divine activation failed");
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent event){
        if(!ENABLED||done||!started||event.phase!=TickEvent.Phase.END||event.getServer()!=Minecraft.getInstance().getSingleplayerServer())return;
        var p=event.getServer().getPlayerList().getPlayer(playerId);if(p==null)return;var row=new LinkedHashMap<String,Object>();row.put("nanoTime",System.nanoTime());row.put("clock",clock);row.put("tick",p.tickCount);row.put("position",List.of(p.getX(),p.getY(),p.getZ()));row.put("velocity",List.of(p.getDeltaMovement().x,p.getDeltaMovement().y,p.getDeltaMovement().z));row.put("flying",p.getAbilities().flying);row.put("flyingSpeed",p.getAbilities().getFlyingSpeed());row.put("activeArt",GeneratedMod.data(p).wuxingxiantu$getActiveDivineArt());SERVER.add(row);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void frame(TickEvent.RenderTickEvent event){
        if(!ENABLED||done||!started||event.phase!=TickEvent.Phase.END||clock<20||clock>=400)return;var mc=Minecraft.getInstance();if(mc.player==null)return;
        try{
            long before=System.nanoTime();if(videoStart==0)videoStart=before;var im=Screenshot.takeScreenshot(mc.getMainRenderTarget());long after=System.nanoTime();int index=frameIndex++;
            var p=mc.player;var row=new LinkedHashMap<String,Object>();row.put("frame",index);row.put("nanoTime",before);row.put("seconds",(before-videoStart)/1e9);row.put("clock",clock);row.put("partial",event.renderTickTime);row.put("file",String.format(Locale.ROOT,"frames/%06d.png",index));row.put("readbackMs",(after-before)/1e6);row.put("position",List.of(p.getX(),p.getY(),p.getZ()));row.put("velocity",List.of(p.getDeltaMovement().x,p.getDeltaMovement().y,p.getDeltaMovement().z));row.put("speed",p.getDeltaMovement().length());row.put("yaw",p.getYRot());row.put("camera",mc.options.getCameraType().name());row.put("forward",forward());row.put("sprintInput",sprint());row.put("vanillaInputForwardImpulse",p.input.forwardImpulse);row.put("vanillaInputUp",p.input.up);row.put("vanillaSprinting",p.isSprinting());row.put("flying",p.getAbilities().flying);row.put("flyingSpeed",p.getAbilities().getFlyingSpeed());row.put("activeArt",GeneratedMod.data(p).wuxingxiantu$getActiveDivineArt());row.put("cloudMotion",JindouCloudRenderer.motionSnapshot);row.put("renderedVolumes",JindouCloudRenderer.renderedVolumes);row.put("renderTrace",JindouCloudRenderer.renderTrace);FRAMES.add(row);
            WRITER.execute(()->{try(im){im.writeToFile(OUT.resolve((String)row.get("file")));}catch(Throwable error){ERRORS.add(error.toString());}});
            row.put("visibility",JindouVisibilityQa.snapshot());
            // Independent of the culling callback: omitted callbacks must not hide interpolation evidence.
            var rendered=p.getPosition(event.renderTickTime);row.put("poseInterpolation",Map.of("xo",List.of(p.xo,p.yo,p.zo),"xOld",List.of(p.xOld,p.yOld,p.zOld),"getPosition",List.of(rendered.x,rendered.y,rendered.z),"current",List.of(p.getX(),p.getY(),p.getZ())));
        }catch(Throwable error){finish(error);}
    }
    private static void runtimeEvidence(Minecraft mc)throws Exception{
        var classes=new LinkedHashMap<String,Object>();for(String shortName:List.of("DivineArtLogic","JindouCloudConfig","client.JindouCloudRenderer","client.JindouCloudShader","client.JindouCloudRenderState","client.JindouCloudTrailCache","client.JindouCloudPlayerPose","client.JindouCloudClock","client.JindouCloudTime","mixin.FlightRiderVisibilityMixin","mixin.FlightChunkVisibilityMixin","client.DunxuTransparency","client.HuahongFlightPath","client.HuahongSkinGeometry")){
            String name="cn.blockforge.generated.generatedmod."+shortName;
            // Mixin implementation classes cannot be loaded directly; hash their raw class resource.
            if(shortName.startsWith("mixin.")){String path="/"+name.replace('.','/')+".class";try(var in=GeneratedMod.class.getResourceAsStream(path)){if(in!=null)classes.put(name,Map.of("sha256",sha(in.readAllBytes()),"resource",path,"hashScope","raw class resource; never Class.forName"));}continue;}
            try{var c=Class.forName(name);try(var in=c.getResourceAsStream("/"+name.replace('.','/')+".class")){classes.put(name,Map.of("sha256",sha(in.readAllBytes()),"codeSource",c.getProtectionDomain().getCodeSource().getLocation().toString()));}}catch(ClassNotFoundException oldBaseline){}
        }
        var resources=new LinkedHashMap<String,String>();for(String path:List.of("shaders/core/jindou_cloud.fsh","shaders/core/jindou_cloud.vsh","shaders/core/jindou_cloud.json","textures/effect/jindou_worley.png"))try(var in=mc.getResourceManager().getResource(new net.minecraft.resources.ResourceLocation("wuxingxiantu",path)).orElseThrow().open()){resources.put(path,sha(in.readAllBytes()));}
        var iris=Class.forName("net.irisshaders.iris.Iris");String pack=String.valueOf(iris.getMethod("getCurrentPackName").invoke(null));boolean inUse=(Boolean)iris.getMethod("isPackInUseQuick").invoke(null);if(SOLAS?(!inUse||!pack.equals("Solas Shader V3.7b.zip")):inUse)throw new AssertionError("Unexpected cloud shader state "+pack+" / "+inUse);
        Files.writeString(OUT.resolve("runtime.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("classes",classes,"resources",resources,"shaderPack",pack,"shaderInUse",inUse,"candidate",CANDIDATE,"reuseWorld",REUSE_WORLD.isBlank()?"fresh QA flat world":REUSE_WORLD,"driver","Real DivineArtLogic.release then own-client vanilla KeyMapping forward/sprint and look input. No in-scenario teleport, setPos, velocity, xo/xOld or authoritative network replacement. Setup teleport and flat warmup are declared before epoch. Reused QA worlds reset old divine state, normal .05 flying speed and cooldown before actual activation; no recorded speed override.","scene","Night18000, cloudsFancy, FOV70, third-person front then rear; actual flyingSpeed granted by divine art; no exposure scaling.")));
    }
    private static String sha(byte[] b)throws Exception{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(b));}
    private static void finish(Throwable error){
        if(done)return;done=true;started=false;var mc=Minecraft.getInstance();mc.options.keyUp.setDown(false);mc.options.keySprint.setDown(false);if(error!=null){error.printStackTrace();ERRORS.add(error.toString());}
        try{WRITER.shutdown();if(!WRITER.awaitTermination(55,TimeUnit.SECONDS))ERRORS.add("PNG writer timeout");var report=new LinkedHashMap<String,Object>();report.put("checksOnly",CHECKS_ONLY);report.put("frames",FRAMES);synchronized(SERVER){report.put("server",List.copyOf(SERVER));}report.put("errors",ERRORS);report.put("pointer",PointerSafety.snapshot());report.put("setupChecks",CHECKS);report.put("scenario",CHECKS_ONLY?"Startup verification only; saved own QA world reused, actual divine activation and three startup checks, no recorded movement epoch or frames.":"0–40 idle active;40–100 vanilla forward;100–300 forward+sprint;140+look turns;300–400 release movement and keep looking/keep art active. Front camera before230, rear after. Setup only uses teleport; actual recorded movement remains vanilla client flight and network.");Files.writeString(OUT.resolve("capture.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));Files.writeString(OUT.resolve(ERRORS.isEmpty()?"complete.txt":"failure.txt"),ERRORS.isEmpty()?(CHECKS_ONLY?"STARTUP CHECKS PASSED — no flight or frames recorded":"RECORDED — continuous visual review required"):String.join("\n",ERRORS));}catch(Throwable write){write.printStackTrace();}mc.stop();
    }
    private JindouReproCapture(){}
}
