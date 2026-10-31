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
    private static final boolean UI_ONLY=Boolean.getBoolean("wuxingxiantu.huahongUiOnly");
    private static final boolean DYNAMICS=HuahongDynamicsQa.ENABLED&&!UI_ONLY;
    private static final boolean SKINS=Boolean.getBoolean("wuxingxiantu.huahongSkins")||Boolean.getBoolean("wuxingxiantu.huahongFullSpeed")||UI_ONLY;
    private static final boolean MOTION=Boolean.getBoolean("wuxingxiantu.huahongMotion");
    static final boolean FULL_SPEED=(Boolean.getBoolean("wuxingxiantu.huahongFullSpeed")||DYNAMICS)&&!UI_ONLY;
    private static final long START=System.nanoTime();
    private static final List<Object> ROWS=new ArrayList<>();
    private static final List<String> FAILURES=new ArrayList<>();
    private static final java.lang.reflect.Method CAMERA=ObfuscationReflectionHelper.findMethod(Camera.class,"m_90581_",Vec3.class);
    private static boolean opened,ready,done,materialChecked,pathChecked;private static int tick,motionFrame;private static String pending;
    private static CompletableFuture<?> work;
    private static net.minecraft.core.BlockPos occluder;
    private static int maxStage;private static double maxRatio;private static int maxVertices;
    private static long cameraNanos;private static double nearAxisMix;
    private static int lastToggleInputTick=-1;
    private static volatile Map<String,Object> lastServerFlight=Map.of("status","not sampled yet");
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!ENABLED||done||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try{
            Files.createDirectories(OUT);GLFW.glfwHideWindow(mc.getWindow().getWindow());mc.setWindowActive(true);mc.options.pauseOnLostFocus=false;
            if((System.nanoTime()-START)/1e9>(DYNAMICS?1200:420))throw new AssertionError("Huahong watchdog tick="+tick);
            if(!opened&&mc.player!=null)throw new AssertionError("Refusing an existing world");
            if(!opened&&mc.getOverlay()==null&&(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen||mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)){
                opened=true;mc.getWindow().setWindowed(1600,900);mc.resizeDisplay();mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);
                mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.framerateLimit().set(40);mc.options.fov().set(65);mc.options.hideGui=true;mc.options.bobView().set(false);mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("huahong-qa-"+System.currentTimeMillis(),new LevelSettings("Huahong isolated",GameType.SURVIVAL,false,net.minecraft.world.Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(7215,false,false),net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions);return;
            }
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null||mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen){
                if(ready&&(tick>0||DYNAMICS))input();return;
            }
            if(mc.screen!=null&&(!SKINS||tick<900))mc.setScreen(null);
            if(!ready){
                ready=true;var id=mc.player.getUUID();FlyingSwordConfig.TRAILS.set(true);FlyingSwordConfig.TRAIL_PATH.set(FlyingSwordConfig.TrailPath.NORMAL_TRAIL_RENDER_PATH);
                work=mc.getSingleplayerServer().submitAsync(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);var l=p.serverLevel();
                    for(int x=-2;x<=2;x++)for(int z=-3;z<=4;z++)l.getChunk(x,z);
                    l.setDayTime(9000);l.setWeatherParameters(12000,0,false,false);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,l.getServer());
                    p.setNoGravity(true);p.removeAllEffects();
                    p.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(WuXingItems.PURE_YANG_SWORD.get()));p.getInventory().selected=0;
                    var d=GeneratedMod.data(p);d.wuxingxiantu$setRealm(99);d.wuxingxiantu$learnArt(YujianController.FLIGHT);d.wuxingxiantu$setArtMastery(YujianController.FLIGHT,100);
                    d.wuxingxiantu$setMaxMana(1000000);d.wuxingxiantu$setAura(1000000);p.getPersistentData().putLong("YujianCooldown_DUNXU",Long.MAX_VALUE);
                    FlyingSwordConfig.SPEED_MULTIPLIER.set(FULL_SPEED?1.0:.1);GeneratedMod.sync(p);p.containerMenu.broadcastChanges();
                    if(DYNAMICS)HuahongDynamicsQa.prepareWorld(p);
                    p.teleportTo(l,8,DYNAMICS?300:FULL_SPEED?512:180,-24,0,0);
                });return;
            }
            if(work!=null){if(!work.isDone()){if(tick>0||DYNAMICS)input();return;}work.join();work=null;}
            PointerSafety.verify();
            if(DYNAMICS){if(!materialChecked)return;boolean finished=HuahongDynamicsQa.tick(mc,OUT);tick=HuahongDynamicsQa.tick();if(finished)finish(null);return;}
            if(UI_ONLY&&tick==0)tick=899;
            // A fixture state may span several real client ticks. Keep its cursor,
            // page, resolution and requested image together until a render consumes it.
            if(pending!=null||HuahongSkinQa.pending!=null){input();return;}
            tick++;input();
            if(mc.player.getVehicle() instanceof ControlledSwordEntity current)maxStage=Math.max(maxStage,current.huahongStage());
            if(tick==50)mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            if(!UI_ONLY&&tick%10==0){
                var id=mc.player.getUUID();work=mc.getSingleplayerServer().submitAsync(()->{
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                    if(p.getVehicle() instanceof ControlledSwordEntity s){
                        if(!FULL_SPEED&&s.getZ()>45&&(!SKINS||tick<315)){s.setPos(8,180,-24);s.positionRider(p);}
                        if(FULL_SPEED&&tick<880)warmFlightRoute(s,p.serverLevel());
                        var def=FlyingSwordRegistry.get(s.display());double normal=def.advanced().maxSpeed(s.advancedPhase(),def)*FlyingSwordConfig.CRUISE_FRACTION.get();
                        var row=new LinkedHashMap<String,Object>();row.put("tick",tick);row.put("stage",s.huahongStage());row.put("strength",s.huahongStrength());row.put("speed",s.flightSpeed());row.put("ratio",s.flightSpeed()/normal);
                        row.put("mana",GeneratedMod.data(p).wuxingxiantu$getAura());row.put("phase",s.advancedPhase().name());
                        row.put("serverEntityTick",s.tickCount);row.put("serverLevelTick",p.serverLevel().getGameTime());row.put("serverPosition",List.of(s.getX(),s.getY(),s.getZ()));row.put("serverYaw",s.getYRot());row.put("serverPitch",s.getXRot());
                        row.put("input",rideDiagnostics(p));lastServerFlight=Map.copyOf(row);
                        synchronized(ROWS){ROWS.add(row);}
                        maxStage=Math.max(maxStage,s.huahongStage());if(s.huahongStage()==2)maxRatio=Math.max(maxRatio,s.flightSpeed()/normal);
                    }
                });
            }
            if(FULL_SPEED&&tick==120)FlyingSwordConfig.TRAIL_PATH.set(FlyingSwordConfig.TrailPath.POST_SHADER_TRAIL_RENDER_PATH);
            if(!FULL_SPEED){
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
            }
            if(tick>=315){
                if(!UI_ONLY&&(maxStage<(FULL_SPEED?2:3)||maxRatio<1.95||maxVertices<40))throw new AssertionError("Missing actual stages/speed/geometry: "+maxStage+" / "+maxRatio+" / "+maxVertices);
                if(!SKINS||HuahongSkinQa.tick(mc,tick,OUT,ROWS))finish(null);
            }
        }catch(Throwable error){finish(error);}
    }
    private static void input(){
        if(UI_ONLY)return;
        if(DYNAMICS){HuahongDynamicsQa.input();return;}
        var mc=Minecraft.getInstance();if(mc.player==null||mc.getConnection()==null)return;
        boolean brake=FULL_SPEED?tick>=880:tick>=200&&tick<225;
        float yaw=tick>=90&&tick<120?(tick-90)*.25F:tick>=120?7.5F:0;
        if(SKINS&&tick>=320&&tick<880){int phase=(tick-320)%70;yaw=phase<=44?0:Math.min(16,phase-44)*1.5F;}
        float pitch=0;
        if(MOTION&&tick>=320&&tick<880){int phase=(tick-320)%70;pitch=(float)(-20*Math.sin(phase*Math.PI/70));}
        if(FULL_SPEED){yaw=Math.max(0,Math.min(tick,880)-20)*3F;pitch=tick>=880?0:(float)(12*Math.sin(Math.max(0,tick-20)*Math.PI/120));}
        // Waiting render/server work keeps held controls alive, never replays a toggle edge.
        boolean toggle=(tick==1||tick==20)&&lastToggleInputTick!=tick;if(toggle)lastToggleInputTick=tick;
        Network.CHANNEL.sendToServer(new SwordFlightInputPacket(tick<20,toggle,!brake&&tick>=20,brake,false,false,false,false,!brake&&tick>=20,false,1,0,yaw,pitch));
    }
    /** Test-only route preparation; keeps terrain generation out of the observed flight interval. */
    private static void warmFlightRoute(ControlledSwordEntity sword,net.minecraft.server.level.ServerLevel level){
        var definition=FlyingSwordRegistry.visual(sword.display());
        double normal=definition.advanced().maxSpeed(sword.advancedPhase(),definition)*FlyingSwordConfig.CRUISE_FRACTION.get();
        Vec3 p=sword.position();double speed=Math.max(normal*2,sword.flightSpeed());
        for(int ahead=0;ahead<=24;ahead+=4){
            if(ahead>0){double angle=Math.toRadians(sword.getYRot()+ahead*3);p=p.add(-Math.sin(angle)*speed*4,0,Math.cos(angle)*speed*4);}
            int cx=net.minecraft.util.Mth.floor(p.x)>>4,cz=net.minecraft.util.Mth.floor(p.z)>>4;
            for(int x=cx-1;x<=cx+1;x++)for(int z=cz-1;z<=cz+1;z++)level.getChunk(x,z);
        }
    }
    static Map<String,Object> flightDiagnostics(ControlledSwordEntity sword){
        return Map.of("qaTick",tick,"clientStage",sword.huahongStage(),"clientStrength",sword.huahongStrength(),"clientSpeed",sword.flightSpeed(),
                "clientPosition",List.of(sword.getX(),sword.getY(),sword.getZ()),"clientEntityTick",sword.tickCount,"lastServer",lastServerFlight);
    }
    static Map<String,Object> rideDiagnostics(net.minecraft.server.level.ServerPlayer player){
        try{
            var ridesField=YujianController.class.getDeclaredField("RIDES");ridesField.setAccessible(true);
            Object ride=((Map<?,?>)ridesField.get(null)).get(player.getUUID());if(ride==null)return Map.of("state","absent");
            var row=new LinkedHashMap<String,Object>();
            for(String name:List.of("state","forward","brake","sprint","desiredYaw","desiredPitch","collisionTicks","lastInputTick")){
                var field=ride.getClass().getDeclaredField(name);field.setAccessible(true);Object value=field.get(ride);row.put(name,value instanceof Enum<?> e?e.name():value);
            }
            row.put("age",player.tickCount-((Number)row.get("lastInputTick")).longValue());return row;
        }catch(ReflectiveOperationException error){throw new AssertionError("Unable to inspect fixture server input",error);}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void camera(ViewportEvent.ComputeCameraAngles e)throws Exception{
        if(!ENABLED||done||tick<25)return;var mc=Minecraft.getInstance();
        if(mc.player==null||!(mc.player.getVehicle() instanceof ControlledSwordEntity s)||mc.options.getCameraType().isFirstPerson())return;
        var transform=InterpolatedFlyingSwordTransform.get(s,(float)e.getPartialTick());
        var target=transform.position().add(0,.85,0);
        var eye=target.add(10,3.5,-14);
        if(DYNAMICS)eye=HuahongDynamicsQa.camera(transform);
        else if(FULL_SPEED){
            int phase=tick<320?0:(tick-320)%70;double desired=phase>=45&&tick<880?1:0;
            long nanos=System.nanoTime();double dt=cameraNanos==0?0:Math.min(.25,(nanos-cameraNanos)/1e9);cameraNanos=nanos;
            nearAxisMix+=(desired-nearAxisMix)*(1-Math.exp(-dt*12));
            Vec3 forward=transform.forward(),side=transform.local(1,0,0).subtract(transform.position()),up=side.cross(forward).normalize();
            double time=(FlyingSwordEffects.samplerTick()+e.getPartialTick())/20.0;
            eye=target.subtract(forward.scale(5+3*nearAxisMix))
                    .add(side.scale(5*(1-nearAxisMix)+.002*Math.sin(time*1.7)*nearAxisMix))
                    .add(up.scale(2*(1-nearAxisMix)+.002*Math.cos(time*1.3)*nearAxisMix));
        }
        var delta=target.subtract(eye);CAMERA.invoke(e.getCamera(),eye);
        e.setYaw((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));e.setPitch((float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));e.setRoll(0);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void frame(TickEvent.RenderTickEvent e){
        if(!ENABLED||done||e.phase!=TickEvent.Phase.END)return;
        maxVertices=Math.max(maxVertices,HuahongVisuals.vertices);
        if(DYNAMICS){try{
            if(ready&&work==null&&Minecraft.getInstance().player!=null&&!materialChecked){ROWS.add(HuahongMaterialQa.verify(Minecraft.getInstance()));ROWS.add(HuahongBlendQa.verify(Minecraft.getInstance()));materialChecked=true;}
            HuahongVolumeRenderer.flushPending();
            HuahongDynamicsQa.frame(Minecraft.getInstance(),e.renderTickTime);
        }catch(Throwable error){finish(error);}return;}
        if(!UI_ONLY&&!pathChecked&&tick>=50)try{pathChecked=true;ROWS.add(HuahongPathQa.verify());}catch(Throwable error){finish(error);return;}
        if(!UI_ONLY&&SKINS&&!materialChecked&&tick>=50)try{materialChecked=true;ROWS.add(HuahongMaterialQa.verify(Minecraft.getInstance()));}catch(Throwable error){finish(error);return;}
        if(SKINS)try{HuahongSkinQa.frame(Minecraft.getInstance(),OUT,ROWS);}catch(Throwable error){finish(error);}
        if(FULL_SPEED&&tick>=40&&tick<880&&work==null)try{HuahongMotionQa.frame(Minecraft.getInstance(),tick,e.renderTickTime);}catch(Throwable error){finish(error);return;}
        if(MOTION&&tick>=320&&tick<880){
            int phase=(tick-320)%70;
            if(phase>=35&&phase<=62&&(!FULL_SPEED||work==null))try{
                Files.createDirectories(OUT.resolve("motion"));
                try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){
                    image.writeToFile(OUT.resolve("motion").resolve(String.format(Locale.ROOT,"%d-%04d.png",(tick-320)/70,motionFrame++)));
                }
            }catch(Throwable error){finish(error);return;}
        }
        if(pending==null)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.screen!=null||mc.getOverlay()!=null)return;
        try{
            int gl=org.lwjgl.opengl.GL11.glGetError();if(gl!=0)throw new AssertionError("GL error "+gl);
            if(!FULL_SPEED&&tick>=286&&HuahongVisuals.hasTrails())throw new AssertionError("Disabled rainbow retained client history");
            if(tick>=120&&Boolean.getBoolean("wuxingxiantu.myriadExternalShaders")&&!TrailShaderBridge.shaderPackDetected())
                throw new AssertionError("Requested external shaderpack is not active");
            String name=pending;pending=null;
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(OUT.resolve(name));}
            ROWS.add(Map.of("image",name,"vertices",HuahongVisuals.vertices,"path",FlyingSwordEffects.activeTrailRenderPath,"pack",TrailShaderBridge.shaderPackDetected(),"glError",gl));
        }catch(Throwable ex){finish(ex);}
    }
    private static void finish(Throwable error){
        if(done)return;done=true;if(error!=null){error.printStackTrace();FAILURES.add(error.toString());}
        if(DYNAMICS)try{HuahongDynamicsQa.finish(OUT);}catch(Throwable verification){verification.printStackTrace();FAILURES.add(verification.toString());error=verification;}
        else if(FULL_SPEED)try{HuahongMotionQa.write(OUT);if(error==null)HuahongMotionQa.verify();}catch(Throwable verification){verification.printStackTrace();FAILURES.add(verification.toString());error=verification;}
        try{
            Files.writeString(OUT.resolve("verification.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("failures",FAILURES,"pointer",PointerSafety.snapshot(),"maxRatio",maxRatio,"maxVertices",maxVertices,"fullSpeed",FULL_SPEED,"uiOnly",UI_ONLY,"dynamics",DYNAMICS,"rows",ROWS)));
            Files.writeString(OUT.resolve(error==null?"complete.txt":"failure.txt"),error==null?"PASS":error.toString());
        }catch(Exception ignored){}Minecraft.getInstance().stop();
    }
    @SubscribeEvent public static void beginDynamicsFrame(TickEvent.RenderTickEvent e){if(ENABLED&&DYNAMICS&&e.phase==TickEvent.Phase.START)HuahongMotionQa.beginRender();}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void beforeDynamicsServerClock(TickEvent.LevelTickEvent e){if(ENABLED&&DYNAMICS&&e.phase==TickEvent.Phase.END&&e.level instanceof net.minecraft.server.level.ServerLevel level)HuahongDynamicsQa.beforeServerLevelTick(level);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void dynamicsOrderedInput(TickEvent.LevelTickEvent e){if(ENABLED&&DYNAMICS&&e.phase==TickEvent.Phase.START&&e.level instanceof net.minecraft.server.level.ServerLevel level)HuahongDynamicsQa.inputServerLevelTick(level);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void dynamicsServerClock(TickEvent.LevelTickEvent e){if(ENABLED&&DYNAMICS&&e.phase==TickEvent.Phase.END&&e.level instanceof net.minecraft.server.level.ServerLevel level)HuahongDynamicsQa.serverLevelTick(level);}
}
