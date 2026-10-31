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
public final class HuahongCapture {
    private static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.huahongCapture");
    private static final Path OUT=Path.of(System.getProperty("wuxingxiantu.huahongOutput","../../reports/huahong-client"));
    private static final boolean SKINS=Boolean.getBoolean("wuxingxiantu.huahongSkins");
    private static final boolean MOTION=Boolean.getBoolean("wuxingxiantu.huahongMotion");
    private static final long START=System.nanoTime();
    private static final List<Object> ROWS=new ArrayList<>();
    private static final List<String> FAILURES=new ArrayList<>();
    private static final java.lang.reflect.Method CAMERA=ObfuscationReflectionHelper.findMethod(Camera.class,"m_90581_",Vec3.class);
    private static boolean opened,ready,done,materialChecked,pathChecked;private static int tick,motionFrame;private static String pending;
    private static CompletableFuture<?> work;
    private static net.minecraft.core.BlockPos occluder;
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
            if(mc.screen!=null&&(!SKINS||tick<900))mc.setScreen(null);
            if(!ready){
                ready=true;var id=mc.player.getUUID();FlyingSwordConfig.TRAILS.set(true);FlyingSwordConfig.TRAIL_PATH.set(FlyingSwordConfig.TrailPath.NORMAL_TRAIL_RENDER_PATH);
                work=mc.getSingleplayerServer().submitAsync(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);var l=p.serverLevel();
                    for(int x=-2;x<=2;x++)for(int z=-3;z<=4;z++)l.getChunk(x,z);
                    l.setDayTime(9000);l.setWeatherParameters(12000,0,false,false);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,l.getServer());
                    p.teleportTo(l,8,180,-24,0,0);p.setNoGravity(true);p.removeAllEffects();
                    p.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(WuXingItems.PURE_YANG_SWORD.get()));p.getInventory().selected=0;
                    var d=GeneratedMod.data(p);d.wuxingxiantu$setRealm(99);d.wuxingxiantu$learnArt(YujianController.FLIGHT);d.wuxingxiantu$setArtMastery(YujianController.FLIGHT,100);
                    d.wuxingxiantu$setMaxMana(1000000);d.wuxingxiantu$setAura(1000000);p.getPersistentData().putLong("YujianCooldown_DUNXU",Long.MAX_VALUE);
                    FlyingSwordConfig.SPEED_MULTIPLIER.set(.1);GeneratedMod.sync(p);p.containerMenu.broadcastChanges();
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
                        if(s.getZ()>45&&(!SKINS||tick<315)){s.setPos(8,180,-24);s.positionRider(p);}
                        var def=FlyingSwordRegistry.get(s.display());double normal=def.advanced().maxSpeed(s.advancedPhase(),def)*FlyingSwordConfig.CRUISE_FRACTION.get();
                        synchronized(ROWS){ROWS.add(Map.of("tick",tick,"stage",s.huahongStage(),"speed",s.flightSpeed(),"ratio",s.flightSpeed()/normal,"mana",GeneratedMod.data(p).wuxingxiantu$getAura(),"phase",s.advancedPhase().name()));}
                        maxStage=Math.max(maxStage,s.huahongStage());if(s.huahongStage()==2)maxRatio=Math.max(maxRatio,s.flightSpeed()/normal);
                    }
                });
            }
            if(tick==70)pending="09-gathering.png";
            if(tick==85)pending="01-native-rainbow.png";
            if(tick==110)pending="02-gentle-turn.png";
            if(tick==120)FlyingSwordConfig.TRAIL_PATH.set(FlyingSwordConfig.TrailPath.POST_SHADER_TRAIL_RENDER_PATH);
            if(tick==150)pending="03-late-depth-rainbow.png";
            if(tick==152){
                var id=mc.player.getUUID();work=mc.getSingleplayerServer().submitAsync(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                    if(p.getVehicle() instanceof ControlledSwordEntity s){
                        var corner=net.minecraft.core.BlockPos.containing(s.getX()+1,s.getY()-4,s.getZ()-6);occluder=corner;
                        for(var pos:net.minecraft.core.BlockPos.betweenClosed(corner,corner.offset(11,18,0)))p.serverLevel().setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                    }
                });
            }
            if(tick==156)pending="08-world-depth-occlusion.png";
            if(tick==160)mc.options.setCameraType(CameraType.FIRST_PERSON);
            if(tick==180)pending="04-first-person.png";
            if(tick==190){mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                work=mc.getSingleplayerServer().submitAsync(()->{if(occluder!=null){
                    var l=mc.getSingleplayerServer().overworld();
                    for(var pos:net.minecraft.core.BlockPos.betweenClosed(occluder,occluder.offset(11,18,0)))l.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }});
            }
            if(tick==202)pending="05-exit.png";
            if(tick==215)pending="06-restored.png";
            if(tick==225){
                var id=mc.player.getUUID();work=mc.getSingleplayerServer().submitAsync(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                    GeneratedMod.data(p).wuxingxiantu$setYujianStack(new net.minecraft.world.item.ItemStack(WuXingItems.XUANYUAN_SWORD.get()));
                    GeneratedMod.sync(p);
                });
            }
            if(tick==275)pending="10-tianque-golden.png";
            if(tick==285)FlyingSwordConfig.TRAILS.set(false);
            if(tick==300)pending="07-effects-disabled.png";
            if(tick>=315){
                if(maxStage<3||maxRatio<1.95||maxVertices<40)throw new AssertionError("Missing actual stages/speed/geometry: "+maxStage+" / "+maxRatio+" / "+maxVertices);
                if(!SKINS||(MOTION&&tick>=880)||HuahongSkinQa.tick(mc,tick,OUT,ROWS))finish(null);
            }
        }catch(Throwable error){finish(error);}
    }
    private static void input(){
        var mc=Minecraft.getInstance();boolean brake=tick>=200&&tick<225;
        float yaw=tick>=90&&tick<120?(tick-90)*.25F:tick>=120?7.5F:0;
        if(SKINS&&tick>=320&&tick<880){int phase=(tick-320)%70;yaw=phase<=44?0:Math.min(16,phase-44)*1.5F;}
        float pitch=0;
        if(MOTION&&tick>=320&&tick<880){int phase=(tick-320)%70;pitch=(float)(-20*Math.sin(phase*Math.PI/70));}
        Network.CHANNEL.sendToServer(new SwordFlightInputPacket(tick<20,tick==1||tick==20,!brake&&tick>=20,brake,false,false,false,false,!brake&&tick>=20,false,1,0,yaw,pitch));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void camera(ViewportEvent.ComputeCameraAngles e)throws Exception{
        if(!ENABLED||done||tick<25)return;var mc=Minecraft.getInstance();
        if(mc.player==null||!(mc.player.getVehicle() instanceof ControlledSwordEntity s)||mc.options.getCameraType().isFirstPerson())return;
        var target=s.getPosition((float)e.getPartialTick()).add(0,.85,0);
        var eye=target.add(10,3.5,-14);var delta=target.subtract(eye);CAMERA.invoke(e.getCamera(),eye);
        e.setYaw((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));e.setPitch((float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));e.setRoll(0);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void frame(TickEvent.RenderTickEvent e){
        if(!ENABLED||done||e.phase!=TickEvent.Phase.END)return;
        maxVertices=Math.max(maxVertices,HuahongVisuals.vertices);
        if(!pathChecked&&tick>=50)try{pathChecked=true;ROWS.add(HuahongPathQa.verify());}catch(Throwable error){finish(error);return;}
        if(SKINS&&!materialChecked&&tick>=50)try{materialChecked=true;ROWS.add(HuahongMaterialQa.verify(Minecraft.getInstance()));}catch(Throwable error){finish(error);return;}
        if(SKINS)try{HuahongSkinQa.frame(Minecraft.getInstance(),OUT,ROWS);}catch(Throwable error){finish(error);}
        if(MOTION&&tick>=320&&tick<880){
            int phase=(tick-320)%70;
            if(phase>=35&&phase<=62)try{
                Files.createDirectories(OUT.resolve("motion"));
                try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){
                    image.writeToFile(OUT.resolve("motion").resolve(String.format(Locale.ROOT,"%d-%04d.png",(tick-320)/70,motionFrame++)));
                }
            }catch(Throwable error){finish(error);return;}
        }
        if(pending==null)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.screen!=null||mc.getOverlay()!=null)return;
        try{
            int gl=org.lwjgl.opengl.GL11.glGetError();if(gl!=0)throw new AssertionError("GL error "+gl);
            if(tick>=286&&HuahongVisuals.hasTrails())throw new AssertionError("Disabled rainbow retained client history");
            if(tick>=120&&Boolean.getBoolean("wuxingxiantu.myriadExternalShaders")&&!TrailShaderBridge.shaderPackDetected())
                throw new AssertionError("Requested external shaderpack is not active");
            String name=pending;pending=null;
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(OUT.resolve(name));}
            ROWS.add(Map.of("image",name,"vertices",HuahongVisuals.vertices,"path",FlyingSwordEffects.activeTrailRenderPath,"pack",TrailShaderBridge.shaderPackDetected(),"glError",gl));
        }catch(Throwable ex){finish(ex);}
    }
    private static void finish(Throwable error){
        if(done)return;done=true;if(error!=null){error.printStackTrace();FAILURES.add(error.toString());}
        try{Files.writeString(OUT.resolve("verification.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("failures",FAILURES,"pointer",PointerSafety.snapshot(),"maxRatio",maxRatio,"maxVertices",maxVertices,"rows",ROWS)));
            Files.writeString(OUT.resolve(error==null?"complete.txt":"failure.txt"),error==null?"PASS":error.toString());
        }catch(Exception ignored){}Minecraft.getInstance().stop();
    }
}
