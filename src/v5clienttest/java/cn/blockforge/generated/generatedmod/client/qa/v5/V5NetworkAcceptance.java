package cn.blockforge.generated.generatedmod.client.qa.v5;

import cn.blockforge.generated.generatedmod.*;
import cn.blockforge.generated.generatedmod.alchemy.*;
import cn.blockforge.generated.generatedmod.alchemy.batch.*;
import cn.blockforge.generated.generatedmod.plants.*;
import com.google.gson.*;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraftforge.event.TickEvent;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Two separate Minecraft JVMs and a real loopback LAN/Forge connection. Test-only own worlds. */
final class V5NetworkAcceptance {
    private static final boolean HOST=System.getProperty("wuxingxiantu.v5NetworkRole").equals("host");
    private static final Path OUT=Path.of(System.getProperty("wuxingxiantu.v5NetworkOutput"));
    private static final BlockPos POS=new BlockPos(0,200,0);
    private static final long START=System.nanoTime();private static boolean opened,done;private static int stage,ticks,violations,port;private static UUID id,owner,peer;
    private static CompletableFuture<?> pending;
    private static boolean has(String name){return Files.exists(OUT.resolve(name+".json"));}
    private static void note(String name,Object value){try{Files.createDirectories(OUT);Files.writeString(OUT.resolve(name+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(value));}catch(Exception e){throw new RuntimeException(e);}}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static net.minecraft.server.level.ServerPlayer host(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static AlchemyFurnaceBlockEntity furnace(Minecraft mc){return (AlchemyFurnaceBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(POS);}
    static void tick(TickEvent.ClientTickEvent event){if(done||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();try{
        org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());mc.setWindowActive(true);mc.options.pauseOnLostFocus=false;
        if(mc.mouseHandler.isMouseGrabbed()||org.lwjgl.glfw.GLFW.glfwGetInputMode(mc.getWindow().getWindow(),org.lwjgl.glfw.GLFW.GLFW_CURSOR)!=org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL)violations++;
        check((System.nanoTime()-START)/1e9<360,"Network acceptance timeout stage="+stage);
        if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;ticks=0;}
        ticks++;
        if(!opened&&mc.getOverlay()==null&&(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen||mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)){
            mc.options.renderDistance().set(2);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);mc.options.guiScale().set(2);mc.getWindow().setWindowed(1280,900);mc.resizeDisplay();mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            if(HOST){check(!has("port"),"Refuse old network fixture output");opened=true;var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_KEEPINVENTORY).set(true,null);
                mc.createWorldOpenFlows().createFreshLevel("v5-network-"+System.currentTimeMillis(),new LevelSettings("V5 isolated network QA",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new net.minecraft.world.level.levelgen.WorldOptions(1329,false,false),net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions);
            }else if(has("port")){opened=true;connect(mc);}
        }
        if(!HOST&&has("host-result")){var result=JsonParser.parseString(Files.readString(OUT.resolve("host-result.json"))).getAsJsonObject();if(!result.get("passed").getAsBoolean())throw new AssertionError("Owned host failed; stop peer");}if(!opened||mc.getOverlay()!=null)return;
        if(HOST)hostTick(mc);else peerTick(mc);
    }catch(Throwable error){com.mojang.logging.LogUtils.getLogger().error("V5_NETWORK_ACCEPTANCE",error);finish(mc,error.toString());}}
    private static void connect(Minecraft mc)throws Exception{
        port=JsonParser.parseString(Files.readString(OUT.resolve("port.json"))).getAsJsonObject().get("port").getAsInt();String address="127.0.0.1:"+port;
        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(new net.minecraft.client.gui.screens.TitleScreen(),mc,net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(address),new net.minecraft.client.multiplayer.ServerData("V5 own loopback QA",address,false),false);
    }
    private static void hostTick(Minecraft mc)throws Exception{
        if(mc.player==null||mc.getSingleplayerServer()==null)return;
        if(stage==0){if(ticks<40)return;pending=mc.getSingleplayerServer().submit(()->{
            var p=host(mc);owner=p.getUUID();p.setNoGravity(true);p.teleportTo(p.serverLevel(),.5,201,-2.5,0,10);
            p.serverLevel().setBlockAndUpdate(POS,WuXingBlocks.ALCHEMY_FURNACE.get().defaultBlockState());CoreStructureBuilder.build(p,POS);AlchemyFurnaceStructure.refreshAround(p.serverLevel(),POS);check(AlchemyFurnaceBlock.isFormed(furnace(mc).getBlockState()),"Network furnace not formed");
            var herb=new ItemStack(SpiritPlantRegistry.SPIRIT_SPROUT_GRASS_ITEM.get(),4);HerbAge.setYears(herb,1500);furnace(mc).setItem(0,herb);furnace(mc).setItem(8,cn.blockforge.generated.generatedmod.fire.FireBottleData.fresh(1));var n=furnace(mc).saveWithoutMetadata();n.putInt("SpiritFuel",furnace(mc).spiritCapacity());furnace(mc).load(n);GeneratedMod.data(p).wuxingxiantu$learnFire(1);FormulaKnowledge.unlock(p);p.setGameMode(GameType.SURVIVAL);p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();
            try(var socket=new java.net.ServerSocket(0)){port=socket.getLocalPort();}catch(Exception e){throw new RuntimeException(e);}mc.getSingleplayerServer().setUsesAuthentication(false);check(mc.getSingleplayerServer().publishServer(GameType.SURVIVAL,false,port),"LAN publication failed");note("port",Map.of("port",port));
        });stage=1;return;}
        if(stage==1){if(ticks<20)return;pending=mc.getSingleplayerServer().submit(()->{var players=mc.getSingleplayerServer().getPlayerList().getPlayers();for(var p:players)if(!p.getUUID().equals(owner)){check(p.getGameProfile().getName().equals("V5Peer"),"Unrelated player refused");peer=p.getUUID();GeneratedMod.data(p).wuxingxiantu$learnFire(1);FormulaKnowledge.unlock(p);p.teleportTo(p.serverLevel(),2.5,201,.5,90,10);p.setNoGravity(true);p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();} });stage=2;return;}
        if(stage==2){if(peer==null){stage=1;return;}pending=mc.getSingleplayerServer().submit(()->{var p=host(mc);p.teleportTo(p.serverLevel(),.5,201,-2.5,0,10);AlchemyFurnaceBlock.openAlchemy(p.serverLevel(),POS,p);check(p.containerMenu instanceof AlchemyFurnaceMenu,"Host server menu did not open");});stage=3;ticks=0;return;}
        if(stage==3){if(ticks<20||!(mc.player.containerMenu instanceof AlchemyFurnaceMenu menu))return;mc.gameMode.handleInventoryButtonClick(menu.containerId,255);Network.CHANNEL.sendToServer(new cn.blockforge.generated.generatedmod.network.packet.alchemy.AlchemyPacket(POS,FreeAlchemy.ID));stage=4;ticks=0;return;}
        if(stage==4){if(ticks<20)return;pending=mc.getSingleplayerServer().submit(()->{var doc=furnace(mc).v5ServerRecord();check(doc.hasUUID("Id"),"Real start packet not accepted");id=doc.getUUID("Id");check(doc.getUUID("Owner").equals(owner),"Wrong network owner");note("batch",Map.of("ready",true));});stage=5;return;}
        if(stage==5){if(!has("attempted"))return;pending=mc.getSingleplayerServer().submit(()->{var doc=furnace(mc).v5ServerRecord();check(doc.getUUID("Id").equals(id)&&doc.getUUID("Owner").equals(owner)&&doc.getString("State").equals("ACTIVE")&&doc.getInt("HeatMode")==1,"Peer changed owner batch");});stage=6;return;}
        if(stage==6){var menu=(AlchemyFurnaceMenu)mc.player.containerMenu;mc.gameMode.handleInventoryButtonClick(menu.containerId,248);stage=7;ticks=0;return;}
        if(stage==7){if(ticks<20)return;pending=mc.getSingleplayerServer().submit(()->{check(furnace(mc).hasResult(),"Owner seal packet failed");note("claim-ready",Map.of("ready",true));});stage=8;return;}
        if(stage==8){if(!has("claim-attempted"))return;pending=mc.getSingleplayerServer().submit(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(peer);check(p!=null&&count(p)==0&&furnace(mc).hasResult(),"Peer stole result");});stage=9;return;}
        if(stage==9){var menu=(AlchemyFurnaceMenu)mc.player.containerMenu;mc.gameMode.handleInventoryButtonClick(menu.containerId,249);mc.gameMode.handleInventoryButtonClick(menu.containerId,249);stage=10;ticks=0;return;}
        if(stage==10){if(ticks<20)return;pending=mc.getSingleplayerServer().submit(()->{check(count(host(mc))==1&&new BatchJournal(host(mc).server).read(id).getString("State").equals("CLAIMED"),"Owner claim packet duplicated/lost result");note("reconnect",Map.of("ready",true));});stage=11;return;}
        if(stage==11){if(!has("rejoined"))return;pending=mc.getSingleplayerServer().submit(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(peer);check(p!=null&&count(p)==0&&count(host(mc))==1,"Reconnect altered receipt ownership");p.setHealth(0);note("respawn",Map.of("ready",true));});stage=12;return;}
        if(stage==12){if(!has("respawned"))return;pending=mc.getSingleplayerServer().submit(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(peer);check(p.isAlive()&&count(p)==0,"Peer respawn altered receipt");p.teleportTo(p.server.getLevel(Level.NETHER),.5,120,.5,0,0);note("dimension",Map.of("ready",true));});stage=13;return;}
        if(stage==13){if(!has("peer-passed"))return;pending=mc.getSingleplayerServer().submit(()->{check(count(host(mc))==1&&new BatchJournal(host(mc).server).read(id).getString("State").equals("CLAIMED"),"Cross-dimension replay changed receipt");});stage=14;return;}
        if(stage==14)finish(mc,null);
    }
    private static long count(net.minecraft.server.level.ServerPlayer p){return p.getInventory().items.stream().filter(s->s.hasTag()&&s.getTag().hasUUID(V5Batch.KEY)).mapToLong(ItemStack::getCount).sum();}
    private static void peerTick(Minecraft mc)throws Exception{
        if(stage==5){if(ticks<20)return;connect(mc);stage=6;ticks=0;return;}
        if(mc.player==null||mc.level==null)return;
        if(stage==0){if(!has("batch"))return;mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(POS.getCenter(),Direction.UP,POS,false));stage=1;ticks=0;return;}
        if(stage==1){if(ticks<20)return;if(!(mc.player.containerMenu instanceof AlchemyFurnaceMenu menu)){if(ticks%20==0)mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(POS.getCenter(),Direction.UP,POS,false));return;}mc.gameMode.handleInventoryButtonClick(menu.containerId,245);mc.gameMode.handleInventoryButtonClick(menu.containerId,248);Network.CHANNEL.sendToServer(new cn.blockforge.generated.generatedmod.network.packet.alchemy.AlchemyPacket(POS,FreeAlchemy.ID));stage=2;ticks=0;return;}
        if(stage==2){if(ticks<20)return;note("attempted",Map.of("ready",true));stage=3;return;}
        if(stage==3){if(!has("claim-ready"))return;var menu=(AlchemyFurnaceMenu)mc.player.containerMenu;for(int i=0;i<3;i++)mc.gameMode.handleInventoryButtonClick(menu.containerId,249);stage=4;ticks=0;return;}
        if(stage==4){if(ticks<20)return;note("claim-attempted",Map.of("ready",true));if(!has("reconnect"))return;mc.level.disconnect();mc.clearLevel(new net.minecraft.client.gui.screens.TitleScreen());stage=5;ticks=0;return;}
        if(stage==6){if(ticks<40)return;note("rejoined",Map.of("ready",true));stage=7;return;}
        if(stage==7){if(!has("respawn")||mc.player.isAlive())return;mc.player.respawn();stage=8;ticks=0;return;}
        if(stage==8){if(ticks<40||!mc.player.isAlive())return;note("respawned",Map.of("ready",true));stage=9;return;}
        if(stage==9){if(!has("dimension")||mc.level.dimension()!=Level.NETHER)return;Network.CHANNEL.sendToServer(new cn.blockforge.generated.generatedmod.network.packet.alchemy.AlchemyPacket(POS,FreeAlchemy.ID));stage=10;ticks=0;return;}
        if(stage==10){if(ticks<20)return;note("peer-passed",Map.of("passed",true));finish(mc,null);}
    }
    private static void finish(Minecraft mc,String error){if(done)return;done=true;note(HOST?"host-result":"peer-result",Map.of("passed",error==null&&violations==0,"error",error==null?"":error,"captureViolations",violations,"blockedGrabs",V5ClientAcceptance.blockedGrabs,"transport","two JVM loopback TCP Forge LAN","stage",stage));mc.stop();}
}
