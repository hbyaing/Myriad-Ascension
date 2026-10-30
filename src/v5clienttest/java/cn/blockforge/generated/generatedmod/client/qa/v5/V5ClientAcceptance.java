package cn.blockforge.generated.generatedmod.client.qa.v5;

import cn.blockforge.generated.generatedmod.*;
import cn.blockforge.generated.generatedmod.alchemy.batch.*;
import cn.blockforge.generated.generatedmod.alchemy.inputs.*;
import cn.blockforge.generated.generatedmod.alchemy.simulation.*;
import cn.blockforge.generated.generatedmod.client.AlchemyScreen;
import cn.blockforge.generated.generatedmod.plants.*;
import com.google.gson.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Explicit opt-in QA source set; never distributed in the production jar. No user input injection. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class V5ClientAcceptance {
    public static int blockedGrabs;
    private static int frames,violations,stage,ticks,worldTicks;
    private static boolean opened,done;
    private static final boolean RESUME=Boolean.getBoolean("wuxingxiantu.v5Resume");
    private static final long START=System.nanoTime();
    private static final Path OUT=Path.of(System.getProperty("wuxingxiantu.v5Output","captures/v5"));
    private static final Path CONTROL=Path.of("v5-qa-state.json");
    private static final BlockPos POS=new BlockPos(0,200,0);
    private static CompletableFuture<?> pending;
    private static JsonObject control;
    private static CompoundTag ready;
    private static final List<String> captures=new ArrayList<>();
    @SubscribeEvent public static void screen(net.minecraftforge.client.event.ScreenEvent.Render.Post event){if(Boolean.getBoolean("wuxingxiantu.v5Migration"))AlchemyMigrationAcceptance.decorate(event);}
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();frames++;
        GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if(Boolean.getBoolean("wuxingxiantu.v5Migration"))AlchemyMigrationAcceptance.render(mc);
        else if(Boolean.getBoolean("wuxingxiantu.v5Inference"))ResearchInferenceAcceptance.render(mc);
        else if(Boolean.getBoolean("wuxingxiantu.v5Research"))ResearchTableAcceptance.render(mc);
        else if(Boolean.getBoolean("wuxingxiantu.v5Entries"))V5EntryAcceptance.render(mc);
        else if(Boolean.getBoolean("wuxingxiantu.v5Effects"))V5EffectAcceptance.render(mc);
        else if(Boolean.getBoolean("wuxingxiantu.v5Incidents"))V5IncidentAcceptance.render(mc);
        if(mc.mouseHandler.isMouseGrabbed()||GLFW.glfwGetInputMode(mc.getWindow().getWindow(),GLFW.GLFW_CURSOR)!=GLFW.GLFW_CURSOR_NORMAL)violations++;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!System.getProperty("wuxingxiantu.v5NetworkRole","").isEmpty()){V5NetworkAcceptance.tick(event);return;}
        if(Boolean.getBoolean("wuxingxiantu.v5Migration")){AlchemyMigrationAcceptance.tick(event);return;}
        if(Boolean.getBoolean("wuxingxiantu.v5Inference")){ResearchInferenceAcceptance.tick(event);return;}
        if(Boolean.getBoolean("wuxingxiantu.v5Research")){ResearchTableAcceptance.tick(event);return;}
        if(Boolean.getBoolean("wuxingxiantu.v5Entries")){V5EntryAcceptance.tick(event);return;}
        if(Boolean.getBoolean("wuxingxiantu.v5Effects")){V5EffectAcceptance.tick(event);return;}
        if(Boolean.getBoolean("wuxingxiantu.v5Incidents")){V5IncidentAcceptance.tick(event);return;}
        if(done||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        try{
            Files.createDirectories(OUT);GLFW.glfwHideWindow(mc.getWindow().getWindow());mc.setWindowActive(true);mc.options.pauseOnLostFocus=false;
            if((System.nanoTime()-START)/1e9>(Boolean.getBoolean("wuxingxiantu.v5Heat")?180:300))throw new AssertionError("V5 independent client timeout stage="+stage);
            if(!opened&&mc.player!=null)throw new AssertionError("Refuses an unrelated existing world");
            if(!opened&&mc.getOverlay()==null&&(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen||mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)){
                opened=true;mc.getWindow().setWindowed(1280,900);mc.options.guiScale().set(2);mc.resizeDisplay();mc.options.renderDistance().set(2);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(40);
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                if(RESUME){control=JsonParser.parseString(Files.readString(CONTROL)).getAsJsonObject();String world=control.get("world").getAsString();check(world.startsWith("v5-batch-"),"Unrelated save refused");mc.createWorldOpenFlows().loadLevel(new net.minecraft.client.gui.screens.TitleScreen(),world);}
                else{control=new JsonObject();String world="v5-batch-"+System.currentTimeMillis();control.addProperty("world",world);var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
                    mc.createWorldOpenFlows().createFreshLevel(world,new LevelSettings("V5 independent background QA",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(7215,false,false),net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions);}
            }
            if(!opened||mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;ticks=0;}
            ticks++;
            if(stage==0){if(++worldTicks<30)return;mc.mouseHandler.grabMouse();check(blockedGrabs>0&&violations==0,"Mouse protection failed");
                pending=mc.getSingleplayerServer().submit(()->prepare(mc));stage=1;return;}
            if(stage==1&&ticks==30&&Boolean.getBoolean("wuxingxiantu.v5Heat")&&!(mc.screen instanceof AlchemyScreen)){
                pending=mc.getSingleplayerServer().submit(()->{var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var f=(AlchemyFurnaceBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(POS);net.minecraftforge.network.NetworkHooks.openScreen(player,f.menuProvider(),POS);});return;
            }
            if(Boolean.getBoolean("wuxingxiantu.v5Heat")&&ticks==100)System.out.println("HEAT_QA_STAGE "+stage+" pos="+mc.player.position()+" screen="+mc.screen+" menu="+mc.player.containerMenu.getClass().getSimpleName());
            if(!(mc.screen instanceof AlchemyScreen)||!(mc.player.containerMenu instanceof AlchemyFurnaceMenu menu))return;
            if(stage==1){if(ticks<15)return;mc.gameMode.handleInventoryButtonClick(menu.containerId,255);stage=2;return;}
            if(stage==2){if(!menu.freeMode()||ticks<25)return;
                if(RESUME){capture(mc,"restored-running");stage=4;return;}
                check(menu.state()==AlchemyFurnaceMenu.State.READY,"Free batch was not ready: "+menu.state());capture(mc,"ready");
                Network.CHANNEL.sendToServer(new cn.blockforge.generated.generatedmod.network.packet.alchemy.AlchemyPacket(POS,cn.blockforge.generated.generatedmod.alchemy.FreeAlchemy.ID));stage=3;ticks=0;return;}
            if(stage==3&&Boolean.getBoolean("wuxingxiantu.v5Heat")){if(!menu.isBrewing()||ticks<8)return;capture(mc,"heat-running-1x");stage=300;ticks=0;return;}
            if(stage>=300&&stage<=305){
                int[] requested={16,0,4,1,0,1};int index=stage-300;
                if(ticks==1){
                    int column=switch(requested[index]){case 0->0;case 1->1;case 4->2;default->3;};
                    var screen=(AlchemyScreen)mc.screen;
                    double x=(mc.getWindow().getGuiScaledWidth()-420)/2+298+column*28+13;
                    double y=(mc.getWindow().getGuiScaledHeight()-320)/2+304;
                    check(screen.mouseClicked(x,y,0),"Native heat button missed");return;
                }
                if(ticks<10)return;
                check(menu.v5HeatMode()==requested[index]&&menu.isBrewing(),"Explicit heat selection failed at "+stage);
                capture(mc,"heat-selected-"+stage+"-"+requested[index]);
                if(stage==305){mc.options.guiScale().set(3);mc.resizeDisplay();stage=306;}else stage++;
                ticks=0;return;
            }
            if(stage==306){if(ticks<10)return;capture(mc,"heat-gui3");mc.options.guiScale().set(4);mc.resizeDisplay();stage=307;ticks=0;return;}
            if(stage==307){if(ticks<10)return;capture(mc,"heat-gui4-fit");finish(mc,true,null);return;}
            if(stage==3){if(!menu.isBrewing()||ticks<110)return;capture(mc,"running-before-restart");
                pending=mc.getSingleplayerServer().submit(()->{
                    var f=(AlchemyFurnaceBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(POS);var d=f.v5ServerRecord();var core=V5Batch.core(d);check(core.tick()>0&&core.tick()<21,"Batch not active before restart");
                    control.addProperty("batch",d.getUUID("Id").toString());control.addProperty("seedHash",seedHash(core.seed()));control.addProperty("tickBeforeRestart",core.tick());
                    try{Files.writeString(CONTROL,new GsonBuilder().setPrettyPrinting().create().toJson(control));}catch(Exception e){throw new RuntimeException(e);}
                });stage=90;return;}
            if(stage==90){finish(mc,false,null);return;}
            if(stage==4){
                if(!menu.furnace().hasResult())return;capture(mc,"completed-after-restart");
                pending=mc.getSingleplayerServer().submit(()->{
                    var server=mc.getSingleplayerServer();var f=(AlchemyFurnaceBlockEntity)server.overworld().getBlockEntity(POS);var doc=f.v5ServerRecord();var core=V5Batch.core(doc);
                    check(doc.getUUID("Id").toString().equals(control.get("batch").getAsString()),"Restart changed batch identity");check(seedHash(core.seed()).equals(control.get("seedHash").getAsString()),"Restart rerolled official seed");
                    check(core.status()==AlchemySimulation.Status.FINISHED&&core.tick()==21&&core.ledger().initialMass()==41000,"Resumed core/accounting mismatch");
                    check((doc.contains("ReactionCore")?SimulationSnapshotCodec.decodeServerSnapshot(V5Batch.profile(doc.getString("Profile"),doc.getInt("Burn")).rules(),doc.getByteArray("ReactionCore")):core).stocks().keySet().stream().anyMatch(k->k.startsWith("extract_")),"Live core did not process actual material");ready=f.saveWithoutMetadata();
                    var player=server.getPlayerList().getPlayer(mc.player.getUUID());for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.DIAMOND,64));player.containerMenu.broadcastChanges();
                });stage=5;return;}
            if(stage==5){if(ticks<15)return;mc.gameMode.handleInventoryButtonClick(menu.containerId,249);stage=6;ticks=0;return;}
            if(stage==6){if(ticks<20)return;check(menu.furnace().hasResult(),"Full inventory lost result");capture(mc,"full-inventory-retains-result");
                pending=mc.getSingleplayerServer().submit(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());p.getInventory().setItem(0,ItemStack.EMPTY);p.containerMenu.broadcastChanges();});stage=7;return;}
            if(stage==7){if(ticks<15)return;mc.gameMode.handleInventoryButtonClick(menu.containerId,249);stage=8;ticks=0;return;}
            if(stage==8){if(ticks<25||menu.furnace().hasResult())return;capture(mc,"claimed");
                pending=mc.getSingleplayerServer().submit(()->{
                    var server=mc.getSingleplayerServer();var p=server.getPlayerList().getPlayer(mc.player.getUUID());var f=(AlchemyFurnaceBlockEntity)server.overworld().getBlockEntity(POS);var id=UUID.fromString(control.get("batch").getAsString());
                    check(receipts(p)==1,"Wrong receipt count");var journal=new BatchJournal(server);check(journal.read(id).getString("State").equals("CLAIMED"),"Claim was not durable");
                    f.load(ready);f.restoreV5();check(!f.hasResult()&&receipts(p)==1,"Stale chunk resurrected output");
                    var interrupted=journal.read(id);interrupted.putString("State","DELIVERING");interrupted.putUUID("Recipient",p.getUUID());journal.pending(p.getUUID(),id,true);journal.write(interrupted);BatchDelivery.recover(p);BatchDelivery.recover(p);
                    check(receipts(p)==1&&journal.pendingFor(p.getUUID()).isEmpty(),"Recovery minted a duplicate receipt");
                });stage=99;return;}
            if(stage==99)finish(mc,true,null);
        }catch(Throwable e){com.mojang.logging.LogUtils.getLogger().error("V5_CLIENT_ACCEPTANCE",e);finish(mc,false,e.toString());}
    }
    private static void prepare(Minecraft mc){
        var server=mc.getSingleplayerServer();var level=server.overworld();var player=server.getPlayerList().getPlayer(mc.player.getUUID());player.setNoGravity(true);player.teleportTo(level,POS.getX()+.5,POS.getY()+1,POS.getZ()-3,0,10);
        if(!RESUME){
            for(int x=-3;x<=3;x++)for(int z=-4;z<=3;z++)for(int y=-1;y<6;y++)level.setBlockAndUpdate(POS.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            if(Boolean.getBoolean("wuxingxiantu.v5Heat"))for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)level.setBlockAndUpdate(POS.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(POS,WuXingBlocks.ALCHEMY_FURNACE.get().defaultBlockState());CoreStructureBuilder.build(player,POS);AlchemyFurnaceStructure.refreshAround(level,POS);
            var f=(AlchemyFurnaceBlockEntity)level.getBlockEntity(POS);var tag=f.saveWithoutMetadata();tag.putInt("SpiritFuel",f.spiritCapacity());f.load(tag);
            var herb=new ItemStack(SpiritPlantRegistry.SPIRIT_SPROUT_GRASS_ITEM.get(),4);HerbAge.setYears(herb,1500);f.setItem(0,herb);f.setItem(8,cn.blockforge.generated.generatedmod.fire.FireBottleData.fresh(1));
            var moon=new MoonBottleData();for(int i=0;i<3;i++)moon.collect(new AlchemyEnvironment.Sample("minecraft:overworld",0,18000,Map.of("night",1L,"open_sky",1L,"moon_phase",0L,"moon_input_rate",1000L)));
            var bottle=new ItemStack(WuXingBlocks.MOON_BOTTLE_ITEM.get());var be=new CompoundTag();be.put("MoonBottle",moon.save());bottle.addTagElement("BlockEntityTag",be);f.setItem(1,bottle);
            GeneratedMod.data(player).wuxingxiantu$learnFire(1);cn.blockforge.generated.generatedmod.alchemy.FormulaKnowledge.unlock(player);
        }
        var furnace=(AlchemyFurnaceBlockEntity)level.getBlockEntity(POS);check(furnace!=null&&AlchemyFurnaceBlock.isFormed(furnace.getBlockState()),"Independent furnace not formed");
        net.minecraftforge.network.NetworkHooks.openScreen(player,furnace.menuProvider(),POS);
    }
    private static long receipts(net.minecraft.server.level.ServerPlayer player){return player.getInventory().items.stream().filter(i->i.hasTag()&&i.getTag().hasUUID(V5Batch.KEY)).count();}
    private static String seedHash(long seed){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Long.toString(seed).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new RuntimeException(e);}}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void capture(Minecraft mc,String name)throws Exception{check(GL11.glGetError()==0,"OpenGL error");try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(OUT.resolve(name+".png"));}captures.add(name);}
    private static void finish(Minecraft mc,boolean complete,String error){if(done)return;done=true;
        try{Files.createDirectories(OUT);Files.writeString(OUT.resolve(RESUME?"resume-result.json":"start-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("passed",error==null,"completed",complete,"resume",RESUME,"captures",captures,"checkedFrames",frames,"blockedGrabs",blockedGrabs,"captureViolations",violations,"error",error==null?"":error)));}
        catch(Exception e){throw new RuntimeException(e);}mc.stop();
    }
}
