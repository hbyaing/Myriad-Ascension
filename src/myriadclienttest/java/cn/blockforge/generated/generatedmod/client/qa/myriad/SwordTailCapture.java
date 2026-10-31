package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.*;
import cn.blockforge.generated.generatedmod.client.*;
import cn.blockforge.generated.generatedmod.network.packet.yujian.SwordFlightInputPacket;
import cn.blockforge.generated.generatedmod.yujian.*;
import com.google.gson.GsonBuilder;
import net.minecraft.client.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Isolated real client + server flight acceptance. Pointer guard is mandatory and test-only. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class SwordTailCapture {
    private static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.swordTailCapture");
    private static final Path OUT=Path.of(System.getProperty("wuxingxiantu.swordTailOutput","../../reports/sword-tail-20261031"));
    private static final long START=System.nanoTime();
    private static final List<Object> ROWS=new ArrayList<>();
    private static final List<String> FAILURES=new ArrayList<>();
    private static final java.lang.reflect.Method CAMERA=ObfuscationReflectionHelper.findMethod(Camera.class,"m_90581_",Vec3.class);
    private static boolean opened,ready,done;private static int tick;private static String pending;
    private static CompletableFuture<?> work;
    private static int attachmentFrames;private static double maxAttachmentError;
    private static int maxStage;private static double maxRatio;private static int maxVertices;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!ENABLED||done||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            Files.createDirectories(OUT);GLFW.glfwHideWindow(mc.getWindow().getWindow());mc.setWindowActive(true);mc.options.pauseOnLostFocus=false;
            if((System.nanoTime()-START)/1e9>420)throw new AssertionError("Huahong watchdog tick="+tick);
            if(!opened&&mc.player!=null)throw new AssertionError("Refusing an existing world");
            if(!opened&&mc.getOverlay()==null&&(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen||mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)){
                opened=true;mc.getWindow().setWindowed(1600,900);mc.resizeDisplay();mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);
                mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.framerateLimit().set(40);mc.options.fov().set(65);mc.options.hideGui=true;mc.options.bobView().set(false);mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("huahong-qa-"+System.currentTimeMillis(),new LevelSettings("Huahong isolated",GameType.SURVIVAL,false,net.minecraft.world.Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(7215,false,false),net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions);return;
            }
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null||mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen)return;
            if(mc.screen!=null)mc.setScreen(null);
            if(!ready){
                ready=true;var id=mc.player.getUUID();FlyingSwordConfig.TRAILS.set(true);FlyingSwordConfig.TRAIL_PATH.set(FlyingSwordConfig.TrailPath.NORMAL_TRAIL_RENDER_PATH);
                work=mc.getSingleplayerServer().submitAsync(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);var l=p.serverLevel();
                    for(int x=-2;x<=2;x++)for(int z=-3;z<=4;z++)l.getChunk(x,z);
                    l.setDayTime(9000);l.setWeatherParameters(12000,0,false,false);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,l.getServer());
                    p.teleportTo(l,8,180,-24,0,0);p.setNoGravity(true);p.removeAllEffects();
                    p.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(WuXingItems.DARK_IRON_SWORD.get()));p.getInventory().selected=0;
                    var d=GeneratedMod.data(p);d.wuxingxiantu$setRealm(99);d.wuxingxiantu$learnArt(YujianController.FLIGHT);d.wuxingxiantu$setArtMastery(YujianController.FLIGHT,100);
                    d.wuxingxiantu$setMaxMana(1000000);d.wuxingxiantu$setAura(1000000);p.getPersistentData().putLong("YujianCooldown_DUNXU",Long.MAX_VALUE);
                    FlyingSwordConfig.SPEED_MULTIPLIER.set(1.0);GeneratedMod.sync(p);p.containerMenu.broadcastChanges();
                });return;
            }
            if(work!=null){if(!work.isDone())return;work.join();work=null;}
            PointerSafety.verify();
            if(pending!=null){input();return;}
            tick++;input();
            if(mc.player.getVehicle() instanceof ControlledSwordEntity current)maxStage=Math.max(maxStage,current.huahongStage());
            if(tick==50)mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            if(tick%10==0){
                var id=mc.player.getUUID();work=mc.getSingleplayerServer().submitAsync(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                    if(p.getVehicle() instanceof ControlledSwordEntity s){
                        if(s.getZ()>45){s.setPos(8,180,-24);s.positionRider(p);}
                        var def=FlyingSwordRegistry.get(s.display());double normal=def.advanced().maxSpeed(s.advancedPhase(),def)*FlyingSwordConfig.CRUISE_FRACTION.get();
                        synchronized(ROWS){ROWS.add(Map.of("tick",tick,"stage",s.huahongStage(),"speed",s.flightSpeed(),"ratio",s.flightSpeed()/normal,"mana",GeneratedMod.data(p).wuxingxiantu$getAura(),"phase",s.advancedPhase().name()));}
                        maxStage=Math.max(maxStage,s.huahongStage());if(s.huahongStage()==2)maxRatio=Math.max(maxRatio,s.flightSpeed()/normal);
                    }
                });
            }
            if(tick==80)pending="01-dark-iron-straight.png";
            if(tick==115||tick==180||tick==245){
                var id=mc.player.getUUID();var item=tick==115?WuXingItems.AZURE_STEEL_SWORD.get():tick==180?WuXingItems.TIANGANG_SWORD.get():WuXingItems.BIG_DIPPER_SWORD.get();
                work=mc.getSingleplayerServer().submitAsync(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);GeneratedMod.data(p).wuxingxiantu$setYujianStack(new net.minecraft.world.item.ItemStack(item));GeneratedMod.sync(p);});
            }
            if(tick==150)pending="02-mingjue-turn.png";
            if(tick==210)pending="03-qingming-roll.png";
            if(tick==235)FlyingSwordConfig.TRAIL_PATH.set(FlyingSwordConfig.TrailPath.POST_SHADER_TRAIL_RENDER_PATH);
            if(tick==275)pending="04-jixiao-late-path.png";
            if(tick==340)pending="05-braking.png";
            if(tick>=370){if(maxVertices<40||attachmentFrames<200)throw new AssertionError("Missing actual tail frames");finish(null);}
        }catch(Throwable error){finish(error);}
    }
    private static void input(){
        boolean brake=tick>=320;
        float yaw=tick<100?0:tick<280?(tick-100)*.7F:126;
        Network.CHANNEL.sendToServer(new SwordFlightInputPacket(tick<20,tick==1||tick==20,!brake&&tick>=20,brake,false,false,tick>=190&&tick<220,false,!brake&&tick>=20,false,1,0,yaw,0));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void camera(ViewportEvent.ComputeCameraAngles e)throws Exception{
        if(!ENABLED||done||tick<25)return;var mc=Minecraft.getInstance();
        if(mc.player==null||!(mc.player.getVehicle() instanceof ControlledSwordEntity s)||mc.options.getCameraType().isFirstPerson())return;
        var target=s.getPosition((float)e.getPartialTick()).add(0,.85,0);
        var eye=target.add(4,1.4,-5);var delta=target.subtract(eye);CAMERA.invoke(e.getCamera(),eye);
        e.setYaw((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));e.setPitch((float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));e.setRoll(0);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void frame(TickEvent.RenderTickEvent e){
        if(!ENABLED||done||e.phase!=TickEvent.Phase.END)return;
        maxVertices=Math.max(maxVertices,FlyingSwordEffects.normalTrailVertexCount);
        for(var d:FlyingSwordEffects.trailDiagnostics()){
            double error=d.attachment().distanceTo(d.renderedHead());maxAttachmentError=Math.max(maxAttachmentError,error);attachmentFrames++;
            if(error>1e-6){finish(new AssertionError("Trail detached: "+error));return;}
        }
        if(pending==null)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.screen!=null||mc.getOverlay()!=null)return;
        try{
            int gl=org.lwjgl.opengl.GL11.glGetError();if(gl!=0)throw new AssertionError("GL error "+gl);

            if(tick>=120&&Boolean.getBoolean("wuxingxiantu.myriadExternalShaders")&&!TrailShaderBridge.shaderPackDetected())
                throw new AssertionError("Requested external shaderpack is not active");
            String name=pending;pending=null;
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(OUT.resolve(name));}
            ROWS.add(Map.of("image",name,"vertices",HuahongVisuals.vertices,"path",FlyingSwordEffects.activeTrailRenderPath,"pack",TrailShaderBridge.shaderPackDetected(),"glError",gl));
        }catch(Throwable ex){finish(ex);}
    }
    private static void finish(Throwable error){
        if(done)return;done=true;if(error!=null){error.printStackTrace();FAILURES.add(error.toString());}
        try{Files.writeString(OUT.resolve("verification.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("failures",FAILURES,"pointer",PointerSafety.snapshot(),"maxRatio",maxRatio,"maxVertices",maxVertices,"attachmentFrames",attachmentFrames,"maxAttachmentError",maxAttachmentError,"rows",ROWS)));
            Files.writeString(OUT.resolve(error==null?"complete.txt":"failure.txt"),error==null?"PASS":error.toString());
        }catch(Exception ignored){}Minecraft.getInstance().stop();
    }
}
