package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.HeartDemonEntity;
import cn.blockforge.generated.generatedmod.client.MindscapeClient;
import cn.blockforge.generated.generatedmod.mindscape.*;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.*;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

/**
 * Opt-in real integrated-server/background client acceptance. No client state is
 * injected: entry, owner damage, completion and abort use production server code
 * and the normal S2C channel. Only this newly created QA save is ever touched.
 */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class MindscapeCapture {
    private static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.mindscapeCapture");
    private static final boolean EXTERNAL=Boolean.getBoolean("wuxingxiantu.myriadExternalShaders")&&!Boolean.getBoolean("wuxingxiantu.mindscapeNoShaders");
    private static final String EXPECTED_PACK=System.getProperty("wuxingxiantu.myriadExpectedShaderPack","");
    private static final Path OUT=Path.of(System.getProperty("wuxingxiantu.myriadCaptureOutput","captures")).resolve("mindscape");
    private static final int WIDTH=1600,HEIGHT=900,EXPECTED_CAPTURES=20;
    private static final double MIN_BACKGROUND_COLOR_RATIO=.02;
    private static final long START=System.nanoTime();
    private static final List<Map<String,Object>> ROWS=new ArrayList<>();
    private static final List<String> FAILURES=new ArrayList<>();
    private static final List<Map<String,Object>> SERVER_ACTIONS=new ArrayList<>();
    private static final Vec3 RETURN_POINT=new Vec3(.5,201,.5);
    private static final String FIRST="wuxingxiantu:six_thieves_1",FINAL="wuxingxiantu:heart_demon_final";
    private record View(Vec3 relative,float yaw,float pitch,int fov){}
    private static final View FRONT=new View(new Vec3(0,4,-23),0,-10,76);
    private static final View[] VIEWS={FRONT,new View(new Vec3(0,4,-23),55,-6,76),
            new View(new Vec3(0,4,-23),0,-10,100),new View(new Vec3(25.5,4,0),-90,-10,90),
            new View(new Vec3(0,3,-8),0,0,66)};
    private static final String[] VIEW_NAMES={"01-corrupted-front","02-corrupted-yaw","03-corrupted-wide-fov","04-corrupted-near-boundary","05-boss-entity-occlusion"};
    private enum Phase {BOOT,PREPARE,FIRST_ENTER,FIRST_VIEWS,RELOAD_START,RELOAD_WAIT,THIRD_SETUP,THIRD_VIEW,
        TRANSPARENT_SETUP,TRANSPARENT_VIEW,TRANSPARENT_CLEAR,FIRST_KILL,FIRST_VICTORY,FIRST_RETURN,
        FINAL_ENTER,FINAL_PHASE_ONE,FINAL_FIRST_LETHAL,FINAL_PHASE_TWO,FINAL_SECOND_LETHAL,FINAL_VICTORY,FINAL_RETURN,
        ABORT_ENTER,ABORT_ACTIVE,ABORT_DO,ABORT_RETURN,DEVELOPER_RETURN,DONE}
    private static Phase phase=Phase.BOOT;
    private static int ticks,viewIndex,victoryCapture,firstLethalTick,bossId=-1,occluderId=-1,heartbeatTicks,closedQaPauseScreens;
    private static boolean opened,finished,pointerExercised,shaderChecked,shaderActive;
    private static Boolean outsideExtendedVertexFormat;
    private static String pack="none",worldName="";
    private static View view;
    private static UUID playerId;
    private static Method cameraPosition;
    private static CompletableFuture<Map<String,Object>> pending;
    private static Runnable afterServer;
    private static CaptureRequest requested;
    private static Map<String,Object> lastServer=Map.of();
    private static CompletableFuture<Void> resourceReload;
    private static Map<String,Object> beforeReload=Map.of();
    private static UUID reloadSession;
    private static final List<Particle> probeParticles=new ArrayList<>();
    private static List<BlockPos> probeGlass=List.of();
    private static BlockPos probeOpaque;
    private static int translucentSubmissions;
    private static final Set<String> probeParticleTypes=new LinkedHashSet<>();
    private static final class CaptureRequest {
        final String name;final MindscapeState expected;final float low,high;final Runnable next;
        CompletableFuture<Map<String,Object>> evidence;
        CaptureRequest(String name,MindscapeState expected,float low,float high,Runnable next){this.name=name;this.expected=expected;this.low=low;this.high=high;this.next=next;}
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void phase(Phase next){phase=next;ticks=0;}
    private static void view(Minecraft mc,View next){view=next;mc.options.fov().set(next.fov());ticks=0;}
    private static boolean inside(Minecraft mc){return mc.level!=null&&mc.level.dimension()==MindscapeManager.DIMENSION;}
    private static boolean active(Minecraft mc,String challenge){return inside(mc)&&MindscapeClient.state()==MindscapeState.CHALLENGE_ACTIVE
            &&MindscapeClient.sessionId()!=null&&MindscapeClient.origin()!=null&&MindscapeClient.challenge().equals(challenge)
            &&mc.level.getEntity(bossId)!=null;}
    private static void request(String name,MindscapeState state,float low,float high,Runnable next){
        if(requested==null)requested=new CaptureRequest(name,state,low,high,next);
    }
    private static void server(Minecraft mc,Consumer<ServerPlayer> action,Runnable next){
        check(pending==null,"Overlapping QA server actions");
        pending=mc.getSingleplayerServer().submit(()->{
            ServerPlayer owner=mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
            check(owner!=null,"QA owner left integrated server");action.accept(owner);return snapshot(owner);
        });afterServer=next;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!ENABLED||finished||event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();
        try{
            Files.createDirectories(OUT);
            GLFW.glfwHideWindow(mc.getWindow().getWindow());mc.setWindowActive(true);mc.options.pauseOnLostFocus=false;
            if(++heartbeatTicks%100==0)writeStatus(mc);
            closeOwnedPauseScreen(mc);
            check((System.nanoTime()-START)/1_000_000_000L<420,"Mindscape QA timeout: "+phase);
            if(!opened&&mc.player!=null)throw new AssertionError("Refuses to adopt an existing player save");
            if(!opened&&mc.getOverlay()==null&&(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen
                    ||mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)){
                opened=true;worldName="mindscape-background-qa-"+System.currentTimeMillis();
                mc.getWindow().setWidth(WIDTH);mc.getWindow().setHeight(HEIGHT);mc.options.guiScale().set(2);
                mc.options.renderDistance().set(5);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(40);
                mc.options.fov().set(76);mc.options.bobView().set(false);mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.hideGui=false;mc.resizeDisplay();GLFW.glfwHideWindow(mc.getWindow().getWindow());
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel(worldName,
                        new LevelSettings("Mindscape isolated background QA",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),
                        new WorldOptions(7215,false,false),net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions);
            }
            if(!opened||mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;
            if(mc.getMainRenderTarget().width!=WIDTH||mc.getMainRenderTarget().height!=HEIGHT){mc.getWindow().setWidth(WIDTH);mc.getWindow().setHeight(HEIGHT);mc.resizeDisplay();}
            playerId=mc.player.getUUID();
            if(!pointerExercised){mc.mouseHandler.grabMouse();pointerExercised=true;PointerSafety.verify();}
            if(!shaderChecked){checkShaders();shaderChecked=true;}
            if(pending!=null){if(!pending.isDone())return;lastServer=pending.join();SERVER_ACTIONS.add(lastServer);pending=null;Runnable next=afterServer;afterServer=null;next.run();}
            ticks++;
            if(requested!=null)return;
            switch(phase){
                case BOOT -> {if(ticks>=30)server(mc,MindscapeCapture::prepare,()->phase(Phase.PREPARE));}
                case PREPARE -> {if(ticks>=15)request("00-overworld-before",null,0,0,()->phase(Phase.FIRST_ENTER));}
                case FIRST_ENTER -> server(mc,p->enter(p,FIRST,true),()->{viewIndex=0;view(mc,VIEWS[0]);phase(Phase.FIRST_VIEWS);});
                case FIRST_VIEWS -> {
                    if(!active(mc,FIRST)||ticks<12)return;
                    check(MindscapeClient.purification(0)==0,"Active challenge showed purified color");
                    if(viewIndex==4)check(mc.level.getEntity(occluderId)!=null,"Real foreground armor stand did not reach client");
                    request(VIEW_NAMES[viewIndex],MindscapeState.CHALLENGE_ACTIVE,0,0,()->{
                        if(++viewIndex==VIEWS.length){view(mc,FRONT);phase(Phase.RELOAD_START);}else view(mc,VIEWS[viewIndex]);
                    });
                }
                case RELOAD_START -> {
                    beforeReload=rendererDiagnostics();reloadSession=MindscapeClient.sessionId();
                    resourceReload=mc.reloadResourcePacks();phase(Phase.RELOAD_WAIT);
                }
                case RELOAD_WAIT -> {
                    if(!resourceReload.isDone())return;resourceReload.join();
                    if(!active(mc,FIRST)||ticks<12)return;
                    check(Objects.equals(reloadSession,MindscapeClient.sessionId()),"Resource reload changed authoritative session");
                    request("05a-resource-reload",MindscapeState.CHALLENGE_ACTIVE,0,0,()->phase(Phase.THIRD_SETUP));
                }
                case THIRD_SETUP -> server(mc,p->{
                    var center=MindscapeInstances.center(GeneratedMod.data(p).wuxingxiantu$getMindscapeData().getInt("slot"));
                    p.teleportTo(center.getX()+.5,65.1,center.getZ()+.5-25.5);p.setDeltaMovement(Vec3.ZERO);
                    check(MindscapeArenas.inside(center,p.position()),"Third-person owner is outside arena");
                },()->{mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);view(mc,new View(new Vec3(0,3.4,-31.5),0,8,70));phase(Phase.THIRD_VIEW);});
                case THIRD_VIEW -> {
                    if(active(mc,FIRST)&&ticks>=12)request("05b-third-person-camera-outside-arena",MindscapeState.CHALLENGE_ACTIVE,0,0,()->phase(Phase.TRANSPARENT_SETUP));
                }
                case TRANSPARENT_SETUP -> server(mc,MindscapeCapture::prepareTransparent,()->{
                    mc.options.setCameraType(CameraType.FIRST_PERSON);view(mc,new View(new Vec3(0,3,-8),0,0,76));phase(Phase.TRANSPARENT_VIEW);
                });
                case TRANSPARENT_VIEW -> {
                    if(!active(mc,FIRST))return;if(ticks%2==0)emitProbeParticles(mc);
                    if(ticks>=18)request("05c-glass-translucent-particles-depth",MindscapeState.CHALLENGE_ACTIVE,0,0,()->phase(Phase.TRANSPARENT_CLEAR));
                }
                case TRANSPARENT_CLEAR -> {
                    for(var particle:probeParticles)particle.remove();probeParticles.clear();
                    server(mc,p->{for(var pos:probeGlass)p.serverLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),2);
                        if(probeOpaque!=null)for(int y=0;y<4;y++)p.serverLevel().setBlock(probeOpaque.above(y),Blocks.AIR.defaultBlockState(),2);
                    },()->{view(mc,FRONT);phase(Phase.FIRST_KILL);});
                }
                case FIRST_KILL -> {if(ticks>=5)server(mc,p->{removeOccluder(p);lethal(p,false);},()->{victoryCapture=0;phase(Phase.FIRST_VICTORY);});}
                case FIRST_VICTORY -> victory(mc,"06-natural-victory-transition","07-natural-gold-hold",Phase.FIRST_RETURN);
                case FIRST_RETURN -> returned(mc,"08-natural-return",Phase.FINAL_ENTER);
                case FINAL_ENTER -> server(mc,p->enter(p,FINAL,false),()->{view(mc,FRONT);phase(Phase.FINAL_PHASE_ONE);});
                case FINAL_PHASE_ONE -> {if(active(mc,FINAL)&&ticks>=12)request("09-final-first-phase",MindscapeState.CHALLENGE_ACTIVE,0,0,()->phase(Phase.FINAL_FIRST_LETHAL));}
                case FINAL_FIRST_LETHAL -> server(mc,p->{
                    var heart=(HeartDemonEntity)boss(p);heart.hurt(p.damageSources().playerAttack(p),1_000_000);
                    check(heart.isAlive()&&heart.visualPhase()==HeartDemonEntity.VisualPhase.PHASE_2,"First lethal did not enter phase two");
                    check(MindscapeManager.state(p)==MindscapeState.CHALLENGE_ACTIVE,"First lethal prematurely completed final challenge");
                    check(!GeneratedMod.data(p).wuxingxiantu$getMindscapeData().getBoolean("final_completed"),"First lethal granted final reward");
                    firstLethalTick=heart.tickCount;
                },()->phase(Phase.FINAL_PHASE_TWO));
                case FINAL_PHASE_TWO -> {
                    if(!active(mc,FINAL)||ticks<24)return;
                    var entity=mc.level.getEntity(bossId);
                    if(!(entity instanceof HeartDemonEntity heart)||heart.visualPhase()!=HeartDemonEntity.VisualPhase.PHASE_2)return;
                    request("10-final-second-phase-still-corrupted",MindscapeState.CHALLENGE_ACTIVE,0,0,()->phase(Phase.FINAL_SECOND_LETHAL));
                }
                case FINAL_SECOND_LETHAL -> server(mc,p->{
                    var heart=(HeartDemonEntity)boss(p);check(heart.tickCount-firstLethalTick>=20,"Final transition entity ticks not elapsed");
                    lethal(p,true);
                },()->{victoryCapture=0;phase(Phase.FINAL_VICTORY);});
                case FINAL_VICTORY -> victory(mc,"11-final-victory-transition","12-final-gold-hold",Phase.FINAL_RETURN);
                case FINAL_RETURN -> returned(mc,"13-final-return",Phase.ABORT_ENTER);
                case ABORT_ENTER -> server(mc,p->enter(p,FIRST,false),()->{view(mc,FRONT);phase(Phase.ABORT_ACTIVE);});
                case ABORT_ACTIVE -> {if(active(mc,FIRST)&&ticks>=12)request("14-abort-before-exit",MindscapeState.CHALLENGE_ACTIVE,0,0,()->phase(Phase.ABORT_DO));}
                case ABORT_DO -> server(mc,p->{
                    int rewards=GeneratedMod.data(p).wuxingxiantu$getMindscapeData().getList("completed_challenges",8).size();
                    MindscapeManager.abort(p,true);check(!MindscapeExternalSnapshot.isSealed(p),"Abort retained sealed external snapshot");
                    check(GeneratedMod.data(p).wuxingxiantu$getMindscapeData().getList("completed_challenges",8).size()==rewards,"Abort granted an extra completion");
                },()->phase(Phase.ABORT_RETURN));
                case ABORT_RETURN -> returned(mc,"15-abort-return",Phase.DEVELOPER_RETURN);
                case DEVELOPER_RETURN -> server(mc,p->{
                    enter(p,FIRST,false);check(MindscapeManager.complete(p,true),"Developer completion rejected valid session");
                    check(p.level().dimension()!=MindscapeManager.DIMENSION,"Developer completion incorrectly retained gold display");
                    check(!MindscapeExternalSnapshot.isSealed(p),"Developer completion left snapshot sealed");
                },()->phase(Phase.DONE));
                case DONE -> {if(!inside(mc)&&MindscapeClient.sessionId()==null&&ticks>=10)request("16-developer-instant-return",null,0,0,()->finish(mc));}
            }
        }catch(Throwable error){fail(mc,error);}
    }
    private static void prepare(ServerPlayer p){
        GeneratedMod.data(p).wuxingxiantu$setRealm(8);p.setGameMode(GameType.CREATIVE);
        p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,7));p.getInventory().setChanged();
        for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++)p.serverLevel().setBlock(new BlockPos(x,200,z),Blocks.SMOOTH_STONE.defaultBlockState(),2);
        p.teleportTo(p.serverLevel(),RETURN_POINT.x,RETURN_POINT.y,RETURN_POINT.z,0,0);
        p.serverLevel().setDayTime(6000);p.serverLevel().setWeatherParameters(6000,0,false,false);GeneratedMod.sync(p);
    }
    private static void enter(ServerPlayer p,String challengeId,boolean occluder){
        check(p.level().dimension()!=MindscapeManager.DIMENSION,"Entry started before prior return");
        check(p.getInventory().items.stream().filter(s->s.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum()==7,"Prior return did not restore real inventory");
        MindscapeManager.markUnstableFor(p,challengeId);
        check(MindscapeManager.enter(p,MindscapeChallenges.get(challengeId),false),"Production natural entry rejected "+challengeId);
        var mob=boss(p);mob.setNoAi(true);bossId=mob.getId();
        check(MindscapeExternalSnapshot.isSealed(p),"Entry did not seal external snapshot");
        check(!MindscapeManager.complete(p,false),"Living boss incorrectly allowed natural completion");
        if(occluder){
            var center=MindscapeInstances.center(GeneratedMod.data(p).wuxingxiantu$getMindscapeData().getInt("slot"));
            var stand=new ArmorStand(p.serverLevel(),center.getX()+2.0,65.1,center.getZ()+1.5);
            stand.setNoGravity(true);stand.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.DIAMOND_HELMET));
            stand.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
            check(p.serverLevel().addFreshEntity(stand),"Foreground entity spawn failed");occluderId=stand.getId();
        }
    }
    private static net.minecraft.world.entity.Mob boss(ServerPlayer p){
        var tag=GeneratedMod.data(p).wuxingxiantu$getMindscapeData();
        var entity=tag.hasUUID("boss")?p.serverLevel().getEntity(tag.getUUID("boss")):null;
        check(entity instanceof net.minecraft.world.entity.Mob,"Bound production boss missing");return (net.minecraft.world.entity.Mob)entity;
    }
    private static void removeOccluder(ServerPlayer p){var stand=p.serverLevel().getEntity(occluderId);if(stand!=null)stand.discard();occluderId=-1;}
    private static void prepareTransparent(ServerPlayer p){
        var center=MindscapeInstances.center(GeneratedMod.data(p).wuxingxiantu$getMindscapeData().getInt("slot"));
        var spawn=MindscapeArenas.playerSpawn(center);p.teleportTo(spawn.x,spawn.y,spawn.z);p.setDeltaMovement(Vec3.ZERO);
        var positions=new ArrayList<BlockPos>();
        for(int side:new int[]{-1,1})for(int x=3;x<=5;x++)for(int y=2;y<=5;y++){
            var pos=new BlockPos(center.getX()+side*x,64+y,center.getZ()-1);
            p.serverLevel().setBlock(pos,(side<0?Blocks.GLASS:Blocks.BLUE_STAINED_GLASS).defaultBlockState(),2);positions.add(pos);
        }
        probeGlass=List.copyOf(positions);probeOpaque=new BlockPos(center.getX()+1,66,center.getZ()+2);
        for(int y=0;y<4;y++)p.serverLevel().setBlock(probeOpaque.above(y),Blocks.SMOOTH_STONE.defaultBlockState(),2);
    }
    private static void emitProbeParticles(Minecraft mc){
        Vec3 origin=MindscapeClient.origin();if(origin==null)return;
        // Real vanilla particle providers and ParticleEngine; front/behind glass,
        // plus matching front/behind an opaque column, for visual depth review.
        for(var relative:List.of(new Vec3(-3,3,-4),new Vec3(-3,3,1),new Vec3(1,3,0),new Vec3(1,3,3))){
            var point=origin.add(relative);var particle=mc.particleEngine.createParticle(ParticleTypes.CLOUD,point.x,point.y,point.z,0,0,0);
            if(particle==null)continue;particle.setColor(.12f,.70f,1f);particle.scale(.30f);probeParticles.add(particle);
            probeParticleTypes.add(particle.getClass().getName()+":"+particle.getRenderType());
            if(particle.getRenderType()==ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT)translucentSubmissions++;
        }
    }
    private static void lethal(ServerPlayer p,boolean heart){
        var entity=boss(p);if(!heart)entity.setHealth(1);
        entity.hurt(p.damageSources().playerAttack(p),1_000_000);
        check(MindscapeManager.state(p)==MindscapeState.COMPLETED,"Owner lethal did not naturally complete challenge");
        check(p.level().dimension()==MindscapeManager.DIMENSION,"Natural completion returned before visual window");
        check(MindscapeExternalSnapshot.isSealed(p),"Natural victory restored snapshot before gold hold");
    }
    private static void victory(Minecraft mc,String transitionName,String goldName,Phase next){
        check(inside(mc),"Missed natural victory display window");
        if(MindscapeClient.state()!=MindscapeState.COMPLETED)return;
        float progress=MindscapeClient.purification(0);
        if(victoryCapture==0&&progress>=.25f&&progress<.85f)request(transitionName,MindscapeState.COMPLETED,.20f,.95f,()->victoryCapture++);
        else if(victoryCapture==1&&progress>=.999f)request(goldName,MindscapeState.COMPLETED,.98f,1,()->{victoryCapture++;phase(next);});
        else if(victoryCapture==0&&progress>=.85f)throw new AssertionError("Missed actual transition capture: "+progress);
    }
    private static void returned(Minecraft mc,String name,Phase next){
        if(inside(mc)||MindscapeClient.sessionId()!=null||ticks<8)return;
        check(MindscapeClient.origin()==null&&MindscapeClient.purification(0)==0,"Returned client retained mindscape visuals");
        view=null;request(name,null,0,0,()->phase(next));
    }
    private static Map<String,Object> snapshot(ServerPlayer p){
        var data=GeneratedMod.data(p).wuxingxiantu$getMindscapeData();var row=new LinkedHashMap<String,Object>();
        row.put("state",MindscapeManager.state(p).name());row.put("dimension",p.level().dimension().location().toString());
        row.put("serverTick",p.serverLevel().getGameTime());row.put("challenge",data.getString("challenge"));
        row.put("session",data.hasUUID("session")?data.getUUID("session").toString():"");
        row.put("slot",data.hasUUID("session")?data.getInt("slot"):-1);row.put("snapshotSealed",MindscapeExternalSnapshot.isSealed(p));
        row.put("bossCompleted",data.getBoolean("boss_completed"));row.put("finalCompleted",data.getBoolean("final_completed"));
        row.put("victoryUntil",data.getLong("victory_display_until"));
        row.put("markerDiamonds",p.getInventory().items.stream().filter(s->s.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum());
        row.put("position",xyz(p.position()));
        boolean owns=p.level().dimension()==MindscapeManager.DIMENSION&&data.hasUUID("session")
                &&MindscapeInstances.get(p.serverLevel()).owns(p.getUUID(),data.getUUID("session"),data.getInt("slot"));row.put("leaseOwned",owns);
        if(data.hasUUID("boss")){var entity=p.serverLevel().getEntity(data.getUUID("boss"));if(entity instanceof LivingEntity living){row.put("bossHealth",living.getHealth());row.put("bossId",living.getId());if(living instanceof HeartDemonEntity demon)row.put("heartPhase",demon.visualPhase().name());}}
        return row;
    }
    private static List<Double> xyz(Vec3 point){return List.of(point.x,point.y,point.z);}
    private static void closeOwnedPauseScreen(Minecraft mc){
        // This fixture refuses existing saves at startup. Only its own ordinary
        // pause menu may be dismissed; loading, notices and all other screens wait.
        if(opened&&worldName.startsWith("mindscape-background-qa-")&&playerId!=null
                &&mc.player!=null&&playerId.equals(mc.player.getUUID())&&mc.getSingleplayerServer()!=null
                &&mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen){
            mc.setScreen(null);closedQaPauseScreens++;
        }
    }
    private static void writeStatus(Minecraft mc){
        try{
            var status=new LinkedHashMap<String,Object>();status.put("phase",phase.name());status.put("phaseTicks",ticks);
            status.put("clientTicks",heartbeatTicks);status.put("elapsedSeconds",(System.nanoTime()-START)/1_000_000_000L);
            status.put("screen",mc.screen==null?"none":mc.screen.getClass().getName());
            status.put("screenPauses",mc.screen!=null&&mc.screen.isPauseScreen());status.put("paused",mc.isPaused());
            status.put("overlay",mc.getOverlay()==null?"none":mc.getOverlay().getClass().getName());
            status.put("playerPresent",mc.player!=null);status.put("playerDimension",mc.level==null?"none":mc.level.dimension().location().toString());
            status.put("clientState",MindscapeClient.state().name());status.put("clientChallenge",MindscapeClient.challenge());
            status.put("clientSession",String.valueOf(MindscapeClient.sessionId()));
            status.put("pending",pending!=null);status.put("pendingDone",pending!=null&&pending.isDone());
            status.put("requested",requested==null?"none":requested.name);
            status.put("captureEvidencePending",requested!=null&&requested.evidence!=null&&!requested.evidence.isDone());
            status.put("completedCaptures",ROWS.size());status.put("expectedCaptures",EXPECTED_CAPTURES);
            status.put("lastCapture",ROWS.isEmpty()?"none":ROWS.get(ROWS.size()-1).get("case"));
            status.put("closedOwnedPauseScreens",closedQaPauseScreens);status.put("finished",finished);
            Files.writeString(OUT.resolve("status.json"),new GsonBuilder().setPrettyPrinting().create().toJson(status));
        }catch(Exception ignored){/* Diagnostics must not change the production session or fail a capture. */}
    }
    /**
     * Conservative final-output guard against shader-pack fog replacing the whole
     * background. Color presence is necessary, not proof of attractive/continuous
     * folds. The central upper ROI excludes the top HUD, sides and fighting floor.
     */
    private static Map<String,Object> backgroundColorProbe(NativeImage image,String name){
        boolean red=name.equals("01-corrupted-front");
        boolean gold=name.equals("07-natural-gold-hold")||name.equals("12-final-gold-hold");
        if(!red&&!gold)return Map.of();
        int left=(int)Math.floor(image.getWidth()*.15),top=(int)Math.floor(image.getHeight()*.14);
        int right=(int)Math.floor(image.getWidth()*.85),bottom=(int)Math.floor(image.getHeight()*.50);
        int samples=0,matching=0,stride=3;
        for(int y=top;y<bottom;y+=stride)for(int x=left;x<right;x+=stride){
            // NativeImage RGBA pixels are returned as packed ABGR on this API.
            int pixel=image.getPixelRGBA(x,y),r=pixel&255,g=(pixel>>>8)&255,b=(pixel>>>16)&255;
            boolean matches=red?r>=45&&r-g>=18&&r-b>=12&&2*r>=3*g
                    :r>=70&&g>=50&&r-b>=20&&g-b>=8&&10*r>=9*g;
            samples++;if(matches)matching++;
        }
        double ratio=samples==0?0:matching/(double)samples;
        var result=new LinkedHashMap<String,Object>();result.put("expected",red?"red":"gold");
        result.put("roiNormalized",List.of(.15,.14,.85,.50));result.put("roiPixels",List.of(left,top,right,bottom));
        result.put("sampleStride",stride);result.put("samples",samples);result.put("matchingPixels",matching);
        result.put("ratio",ratio);result.put("minimumRatio",MIN_BACKGROUND_COLOR_RATIO);result.put("passed",ratio>=MIN_BACKGROUND_COLOR_RATIO);
        result.put("predicate",red?"R>=45, R-G>=18, R-B>=12, 2R>=3G":"R>=70, G>=50, R-B>=20, G-B>=8, 10R>=9G");
        return result;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void camera(ViewportEvent.ComputeCameraAngles event){
        if(!ENABLED||finished||view==null||MindscapeClient.origin()==null)return;
        try{
            if(cameraPosition==null){cameraPosition=Camera.class.getDeclaredMethod("setPosition",Vec3.class);cameraPosition.setAccessible(true);}
            cameraPosition.invoke(event.getCamera(),MindscapeClient.origin().add(view.relative()));
            event.setYaw(view.yaw());event.setPitch(view.pitch());event.setRoll(0);
        }catch(Throwable error){fail(Minecraft.getInstance(),error);}
    }
    @SuppressWarnings("unchecked") private static Map<String,Object> rendererDiagnostics()throws Exception{
        return new LinkedHashMap<>((Map<String,Object>)Class.forName("cn.blockforge.generated.generatedmod.client.mindscape.MindscapeBackgroundRenderer").getMethod("diagnostics").invoke(null));
    }
    /** Read-only Oculus/Iris 1.8 API probe; no optional type enters the QA class linkage. */
    private static Map<String,Object> pipelineProbe()throws Exception{
        Class<?> iris=null;String prefix="";
        for(String candidate:List.of("net.irisshaders.iris","net.coderbot.iris"))try{
            iris=Class.forName(candidate+".Iris");prefix=candidate;break;
        }catch(ClassNotFoundException ignored){}
        var result=new LinkedHashMap<String,Object>();
        if(iris==null){result.put("irisPresent",false);result.put("class","none");result.put("loadedPackName","none");
            result.put("irisRenderingPipeline",false);result.put("vanillaRenderingPipeline",false);result.put("extendedVertexFormat","none");return result;}
        Object manager=iris.getMethod("getPipelineManager").invoke(null);
        Object pipeline=manager.getClass().getMethod("getPipelineNullable").invoke(manager);
        Object name=iris.getMethod("getCurrentPackName").invoke(null);
        String loadedName=name instanceof Optional<?> optional?String.valueOf(optional.orElse(null)):String.valueOf(name);
        Class<?> settingsClass=Class.forName(prefix+".shaderpack.materialmap.WorldRenderingSettings");
        Object settings=settingsClass.getField("INSTANCE").get(null);
        boolean extended=(boolean)settingsClass.getMethod("shouldUseExtendedVertexFormat").invoke(settings);
        result.put("irisPresent",true);result.put("class",pipeline==null?"none":pipeline.getClass().getName());result.put("loadedPackName",loadedName);
        result.put("irisRenderingPipeline",pipeline!=null&&Class.forName(prefix+".pipeline.IrisRenderingPipeline").isInstance(pipeline));
        result.put("vanillaRenderingPipeline",pipeline!=null&&Class.forName(prefix+".pipeline.VanillaRenderingPipeline").isInstance(pipeline));
        result.put("extendedVertexFormat",extended);return result;
    }
    private static void verifyPipeline(Map<String,Object> pipeline,boolean mindscape,String captureName){
        if(!EXTERNAL)return;
        check(pack.equals(pipeline.get("loadedPackName")),"Loaded external pack changed across dimensions: "+pipeline);
        if(mindscape){
            check(Boolean.TRUE.equals(pipeline.get("vanillaRenderingPipeline")),"Mindscape did not use isolated VanillaRenderingPipeline: "+pipeline);
            check(Boolean.FALSE.equals(pipeline.get("extendedVertexFormat")),"Mindscape retained external extended vertex format: "+pipeline);
        }else{
            check(Boolean.TRUE.equals(pipeline.get("irisRenderingPipeline")),"Outside shader-pack pipeline was not active/restored: "+pipeline);
            check(pipeline.get("extendedVertexFormat") instanceof Boolean,"Missing outside vertex-format evidence");
            boolean extended=(boolean)pipeline.get("extendedVertexFormat");
            if(captureName.equals("00-overworld-before"))outsideExtendedVertexFormat=extended;
            else check(outsideExtendedVertexFormat!=null&&outsideExtendedVertexFormat==extended,"Return did not restore outside vertex-format baseline");
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void frame(TickEvent.RenderTickEvent event){
        if(!ENABLED||finished||event.phase!=TickEvent.Phase.END||requested==null)return;
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.getOverlay()!=null||mc.screen!=null)return;
        try{
            var capture=requested;
            if(capture.evidence==null){capture.evidence=mc.getSingleplayerServer().submit(()->snapshot(mc.getSingleplayerServer().getPlayerList().getPlayer(playerId)));return;}
            if(!capture.evidence.isDone())return;
            Map<String,Object> server=capture.evidence.join();float progress=MindscapeClient.purification(0);
            check(progress>=capture.low&&progress<=capture.high,"Capture purification escaped expected range: "+capture.name+" = "+progress);
            if(capture.expected!=null){
                check(inside(mc)&&MindscapeClient.state()==capture.expected,"Capture missed authoritative client state "+capture.name);
                check(MindscapeClient.sessionId()!=null&&MindscapeClient.sessionId().toString().equals(server.get("session")),"Server/client session mismatch");
                check(Boolean.TRUE.equals(server.get("leaseOwned"))&&Boolean.TRUE.equals(server.get("snapshotSealed")),"Display lost lease or sealed snapshot");
                check(capture.expected.name().equals(server.get("state")),"Server/client lifecycle mismatch");
            }else{
                check(!inside(mc)&&MindscapeClient.sessionId()==null,"Return capture still has active visual session");
                check(!Boolean.TRUE.equals(server.get("snapshotSealed"))&&((Number)server.get("markerDiamonds")).intValue()==7,"Return did not restore external state");
            }
            PointerSafety.verify();check(GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),GLFW.GLFW_VISIBLE)==GLFW.GLFW_FALSE,"QA window became visible");
            int error=GL11.glGetError();check(error==GL11.GL_NO_ERROR,"Mindscape draw GL error "+error);
            var row=new LinkedHashMap<String,Object>();row.put("case",capture.name);row.put("file",capture.name+".png");row.put("server",server);
            row.put("clientState",MindscapeClient.state().name());row.put("clientChallenge",MindscapeClient.challenge());row.put("purification",progress);
            row.put("clientSession",String.valueOf(MindscapeClient.sessionId()));row.put("clientOrigin",MindscapeClient.origin()==null?List.of():xyz(MindscapeClient.origin()));
            row.put("camera",xyz(mc.gameRenderer.getMainCamera().getPosition()));row.put("fov",mc.options.fov().get());row.put("glError",error);
            row.put("cameraType",mc.options.getCameraType().name());
            var pipeline=pipelineProbe();row.put("pipeline",pipeline);row.put("shaderActive",Boolean.TRUE.equals(pipeline.get("irisRenderingPipeline")));
            row.put("shaderPack",pipeline.get("loadedPackName"));row.put("width",mc.getMainRenderTarget().width);row.put("height",mc.getMainRenderTarget().height);
            verifyPipeline(pipeline,capture.expected!=null,capture.name);
            if(capture.expected!=null){var diagnostics=rendererDiagnostics();check(((Number)diagnostics.getOrDefault("draws",0)).longValue()>0,"Production mindscape renderer did not draw");check(diagnostics.get("failure")==null||String.valueOf(diagnostics.get("failure")).isBlank(),"Production renderer recorded failure: "+diagnostics.get("failure"));row.put("renderer",diagnostics);
                if(capture.name.equals("05a-resource-reload")){
                    check(((Number)diagnostics.get("generation")).intValue()>((Number)beforeReload.get("generation")).intValue(),"Reload did not rebuild production GPU resources");
                    for(String key:List.of("corruptedPaletteSHA256","purifiedPaletteSHA256")){
                        Object before=beforeReload.get(key),after=diagnostics.get(key);
                        check(before instanceof String hash&&!hash.isBlank()&&after instanceof String nextHash&&!nextHash.isBlank()
                                &&before.equals(after),"Resource reload changed/missed accepted palette hash: "+key);
                    }
                    row.put("resourceReloadBefore",beforeReload);row.put("resourceReloadFutureCompleted",resourceReload.isDone()&&!resourceReload.isCompletedExceptionally());
                }
                if(capture.name.equals("05b-third-person-camera-outside-arena")){
                    check(mc.options.getCameraType()==CameraType.THIRD_PERSON_BACK,"Third-person fixture did not select actual camera mode");
                    var relative=mc.gameRenderer.getMainCamera().getPosition().subtract(MindscapeClient.origin());
                    check(Math.max(Math.abs(relative.x),Math.abs(relative.z))>MindscapeArenas.HALF_SIZE,"Third-person camera did not cross arena boundary");row.put("cameraOutsideArena",true);
                }
                if(capture.name.equals("05c-glass-translucent-particles-depth")){
                    check(probeGlass.size()==24&&probeGlass.stream().allMatch(pos->mc.level.getBlockState(pos).is(pos.getX()<MindscapeClient.origin().x?Blocks.GLASS:Blocks.BLUE_STAINED_GLASS)),"Real glass setup did not reach client");
                    check(mc.level.getBlockState(probeOpaque).is(Blocks.SMOOTH_STONE),"Opaque particle occluder missing on client");
                    check(translucentSubmissions>0&&!probeParticles.isEmpty(),"Real translucent particle layer was not submitted");
                    row.put("realGlassBlocks",probeGlass.size());row.put("translucentParticleSubmissions",translucentSubmissions);row.put("particleProviders",List.copyOf(probeParticleTypes));
                    row.put("particleDepthGroups",List.of("before glass","behind glass","before opaque column","behind opaque column"));
                }
            }
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
                int minimum=255,maximum=0;
                for(int y=0;y<image.getHeight();y+=13)for(int x=0;x<image.getWidth();x+=13){int pixel=image.getPixelRGBA(x,y);for(int shift:new int[]{0,8,16}){int channel=(pixel>>>shift)&255;minimum=Math.min(minimum,channel);maximum=Math.max(maximum,channel);}}
                check(maximum>8&&maximum-minimum>3,"Rejects blank/uniform capture "+capture.name);row.put("sampleChannelRange",List.of(minimum,maximum));
                var colorProbe=backgroundColorProbe(image,capture.name);if(!colorProbe.isEmpty())row.put("backgroundColorProbe",colorProbe);
                image.writeToFile(OUT.resolve(capture.name+".png"));
            }
            // Keep the failed final-output image and its measured ROI in the report.
            ROWS.add(row);requested=null;
            if(row.get("backgroundColorProbe") instanceof Map<?,?> probe)
                check(Boolean.TRUE.equals(probe.get("passed")),"Expected "+probe.get("expected")+" background absent in final output: ratio="+probe.get("ratio")+", minimum="+MIN_BACKGROUND_COLOR_RATIO+", ROI="+probe.get("roiPixels"));
            writeReport();capture.next.run();
        }catch(Throwable error){fail(mc,error);}
    }
    private static void checkShaders()throws Exception{
        for(String name:new String[]{"net.irisshaders.iris.api.v0.IrisApi","net.coderbot.iris.api.v0.IrisApi"})try{var api=Class.forName(name);shaderActive=(boolean)api.getMethod("isShaderPackInUse").invoke(api.getMethod("getInstance").invoke(null));break;}catch(ClassNotFoundException ignored){}
        if(shaderActive)for(String name:new String[]{"net.irisshaders.iris.Iris","net.coderbot.iris.Iris"})try{Object result=Class.forName(name).getMethod("getCurrentPackName").invoke(null);pack=result instanceof Optional<?> optional?String.valueOf(optional.orElse(null)):String.valueOf(result);break;}catch(ClassNotFoundException ignored){}
        check(shaderActive==EXTERNAL,"Requested native/external shader ownership was not actually active: "+pack);
        check(!EXTERNAL||EXPECTED_PACK.isBlank()||pack.contains(EXPECTED_PACK),"Unexpected actual shader pack "+pack);
    }
    private static void writeReport()throws Exception{
        var report=new LinkedHashMap<String,Object>();report.put("test","mindscape-real-server-client-background");report.put("world",worldName);
        report.put("newIsolatedSave",true);report.put("hiddenWindow",true);report.put("pointerSafety",PointerSafety.snapshot());
        report.put("expectedExternalShaders",EXTERNAL);report.put("outsideShaderPackActuallyActive",shaderActive);report.put("loadedShaderPack",pack);
        report.put("mindscapeCompatibilityStrategy",EXTERNAL?"isolated VanillaRenderingPipeline inside mindscape; outside IrisRenderingPipeline automatically restored":"native rendering without an active third-party pack");
        report.put("outsideExtendedVertexFormatBaseline",outsideExtendedVertexFormat);
        report.put("captures",ROWS);report.put("expectedCaptures",EXPECTED_CAPTURES);report.put("serverActions",SERVER_ACTIONS);report.put("failures",FAILURES);report.put("complete",finished&&FAILURES.isEmpty()&&ROWS.size()==EXPECTED_CAPTURES);
        report.put("scope","Real natural server entry, owner lethal damage, S2C session/timer, purification/gold hold, actual resource reload, third-person camera beyond arena, real glass and translucent ParticleEngine probes. External packs use an isolated VanillaRenderingPipeline only inside mindscape; captures verify actual pipeline class, loaded pack and outside vertex-format restoration. This is not simultaneous third-party-pack compositing inside mindscape. QA creative owner, controlled health/camera/foreground; transparent/particle depth appearance requires screenshot review. Abort exercises production abort API, not player-death/respawn. No fake client state or OS pointer input.");
        Files.writeString(OUT.resolve("verification.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }
    private static void fail(Minecraft mc,Throwable error){FAILURES.add(phase+": "+error);com.mojang.logging.LogUtils.getLogger().error("MINDSCAPE_QA",error);finish(mc);}
    private static void finish(Minecraft mc){
        if(finished)return;finished=true;
        if(FAILURES.isEmpty()&&ROWS.size()!=EXPECTED_CAPTURES)FAILURES.add("Expected "+EXPECTED_CAPTURES+" captures, received "+ROWS.size());
        writeStatus(mc);
        try{Files.createDirectories(OUT);writeReport();Files.writeString(OUT.resolve(FAILURES.isEmpty()&&ROWS.size()==EXPECTED_CAPTURES?"complete.txt":"failure.txt"),FAILURES.isEmpty()?EXPECTED_CAPTURES+" real-server background captures completed; inspect PNGs and verification.json.\n":String.join("\n",FAILURES));}
        catch(Exception error){com.mojang.logging.LogUtils.getLogger().error("MINDSCAPE_QA_REPORT",error);}mc.stop();
    }
    private MindscapeCapture(){}
}
