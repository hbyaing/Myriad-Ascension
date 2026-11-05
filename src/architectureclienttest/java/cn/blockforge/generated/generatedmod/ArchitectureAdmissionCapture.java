package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.*;
import cn.blockforge.generated.generatedmod.alchemy.FormulaKnowledge;
import cn.blockforge.generated.generatedmod.client.qa.myriad.PointerSafety;
import cn.blockforge.generated.generatedmod.network.NetworkChannel;
import cn.blockforge.generated.generatedmod.network.packet.alchemy.AlchemyPacket;
import cn.blockforge.generated.generatedmod.plants.HerbAge;
import cn.blockforge.generated.generatedmod.plants.SpiritPlantRegistry;
import com.google.gson.GsonBuilder;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Isolated real client/packet acceptance. No OS input and no production source inclusion. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class ArchitectureAdmissionCapture {
    private static final Path OUT=Path.of("architecture-acceptance").toAbsolutePath();
    private static final BlockPos POS=new BlockPos(0,180,0);
    private static final long START=System.nanoTime();
    private static final List<String> checks=new ArrayList<>(),messages=new ArrayList<>(),failures=new ArrayList<>();
    private static boolean opened,finished,pointerExercised;
    private static int phase,ticks;
    private static CompletableFuture<?> pending;
    private static volatile UUID batch;
    private static volatile net.minecraft.nbt.CompoundTag initial;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(finished||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        try{
            Files.createDirectories(OUT);GLFW.glfwHideWindow(mc.getWindow().getWindow());mc.options.pauseOnLostFocus=false;
            if((System.nanoTime()-START)/1e9>600)throw new AssertionError("Architecture client timeout phase="+phase);
            if(!opened&&mc.player!=null)throw new AssertionError("Refuses an existing player save");
            if(!opened&&mc.getOverlay()==null&&(mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen
                    ||mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)){
                opened=true;mc.getWindow().setWindowed(1280,900);mc.options.guiScale().set(2);mc.resizeDisplay();
                mc.options.languageCode="zh_cn";mc.getLanguageManager().setSelected("zh_cn");mc.reloadResourcePacks();
                mc.options.renderDistance().set(2);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(40);
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                mc.createWorldOpenFlows().createFreshLevel("architecture-admission-"+System.currentTimeMillis(),
                        new LevelSettings("Architecture isolated network acceptance",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),
                        new WorldOptions(7215,false,false),net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions);
            }
            if(!opened||mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.getOverlay()!=null)return;
            if(!pointerExercised){mc.mouseHandler.grabMouse();PointerSafety.verify();pointerExercised=true;}
            var field=net.minecraft.client.gui.Gui.class.getDeclaredField("overlayMessageString");field.setAccessible(true);
            var overlay=(Component)field.get(mc.gui);if(overlay!=null&&!messages.contains(overlay.getString()))messages.add(overlay.getString());
            if(pending!=null){if(!pending.isDone())return;pending.join();pending=null;ticks=0;}
            ticks++;
            switch(phase){
                case 0 -> {
                    var owner=mc.player.getUUID();pending=mc.getSingleplayerServer().submit(()->{
                        var p=Objects.requireNonNull(mc.getSingleplayerServer().getPlayerList().getPlayer(owner));var level=p.serverLevel();build(level);
                        p.teleportTo(POS.getX()+.5,POS.getY()+1,POS.getZ()+.5);p.setInvulnerable(true);
                        GeneratedMod.data(p).wuxingxiantu$learnFire(1);GeneratedMod.data(p).wuxingxiantu$setAura(100);
                        FormulaKnowledge.unlock(p);var furnace=(AlchemyFurnaceBlockEntity)level.getBlockEntity(POS);
                        var n=furnace.saveWithoutMetadata();n.putInt("SpiritFuel",furnace.spiritCapacity());furnace.load(n);
                        var herb=new ItemStack(SpiritPlantRegistry.SPIRIT_SPROUT_GRASS_ITEM.get(),4);HerbAge.setYears(herb,1500);
                        furnace.setItem(0,herb);furnace.setItem(8,cn.blockforge.generated.generatedmod.fire.FireBottleData.fresh(1));
                        initial=furnace.saveWithoutMetadata();
                    });phase=1;
                }
                case 1 -> {if(ticks>=20){send();phase=2;ticks=0;}}
                case 2 -> {if(ticks>=20){
                    var owner=mc.player.getUUID();pending=mc.getSingleplayerServer().submit(()->{
                        var p=mc.getSingleplayerServer().getPlayerList().getPlayer(owner);var f=(AlchemyFurnaceBlockEntity)p.level().getBlockEntity(POS);
                        require(!f.v5Batch()&&initial.getList("Items",10).equals(f.saveWithoutMetadata().getList("Items",10)),"packet without menu rejected without consuming inputs");
                        AlchemyFurnaceBlock.openAlchemy(p.level(),POS,p);
                        require(p.containerMenu instanceof AlchemyFurnaceMenu,"production server menu opened");
                        p.containerMenu.clickMenuButton(p,255);
                    });phase=3;
                }}
                case 3 -> {if(ticks>=20&&mc.player.containerMenu instanceof AlchemyFurnaceMenu menu&&menu.freeMode()){
                    require(mc.screen instanceof cn.blockforge.generated.generatedmod.client.AlchemyScreen,"production client screen received");
                    send();phase=4;ticks=0;
                }}
                case 4 -> {if(ticks>=30){
                    var owner=mc.player.getUUID();pending=mc.getSingleplayerServer().submit(()->{
                        var p=mc.getSingleplayerServer().getPlayerList().getPlayer(owner);var f=(AlchemyFurnaceBlockEntity)p.level().getBlockEntity(POS);
                        var doc=f.v5ServerRecord();require(doc.hasUUID("Id")&&f.getItem(0).isEmpty(),"encoded client packet reached production admission and escrow");batch=doc.getUUID("Id");
                    });phase=5;
                }}
                case 5 -> {if(ticks>=10){
                    String success=Component.translatable("message.wuxingxiantu.alchemy_start_started").getString();
                    require(messages.contains(success),"translated success feedback delivered over real client connection");
                    var clientFurnace=(AlchemyFurnaceBlockEntity)mc.level.getBlockEntity(POS);
                    require(clientFurnace!=null&&!clientFurnace.saveWithoutMetadata().hasUUID("V5Ref"),"private batch identity absent from client block update");
                    capture(mc,"success.png");mc.player.closeContainer();phase=6;ticks=0;
                }}
                case 6 -> {if(ticks>=2){
                    capture(mc,"success-hud.png");var owner=mc.player.getUUID();
                    pending=mc.getSingleplayerServer().submit(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(owner);AlchemyFurnaceBlock.openAlchemy(p.level(),POS,p);});phase=7;
                }}
                case 7 -> {if(ticks>=15&&mc.player.containerMenu instanceof AlchemyFurnaceMenu){send();phase=8;ticks=0;}}
                case 8 -> {if(ticks>=25){
                    String busy=Component.translatable("message.wuxingxiantu.alchemy_busy").getString();
                    require(messages.contains(busy),"duplicate packet produced busy feedback");capture(mc,"busy.png");
                    mc.player.closeContainer();phase=9;ticks=0;
                }}
                case 9 -> {if(ticks>=2){
                    capture(mc,"busy-hud.png");
                    var owner=mc.player.getUUID();pending=mc.getSingleplayerServer().submit(()->{
                        var p=mc.getSingleplayerServer().getPlayerList().getPlayer(owner);var f=(AlchemyFurnaceBlockEntity)p.level().getBlockEntity(POS);
                        require(f.v5ServerRecord().getUUID("Id").equals(batch),"duplicate client request retained original identity");
                        p.closeContainer();
                    });phase=10;
                }}
                case 10 -> finish(mc);
                default -> throw new AssertionError("Unexpected phase");
            }
        }catch(Throwable error){failures.add(error.toString());com.mojang.logging.LogUtils.getLogger().error("Architecture client acceptance failed",error);finish(mc);}
    }
    private static void send(){NetworkChannel.CHANNEL.sendToServer(new AlchemyPacket(POS,cn.blockforge.generated.generatedmod.alchemy.FreeAlchemy.ID));}
    private static synchronized void require(boolean value,String label){if(!value)throw new AssertionError(label);checks.add(label);}
    private static void capture(Minecraft mc,String name)throws Exception{try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(OUT.resolve(name));}}
    private static void finish(Minecraft mc){
        if(finished)return;finished=true;
        try{
            PointerSafety.verify();var report=new LinkedHashMap<String,Object>();report.put("complete",failures.isEmpty()&&phase==10);
            report.put("checks",checks);report.put("messages",messages);report.put("failures",failures);report.put("pointer",PointerSafety.snapshot());
            report.put("batch",String.valueOf(batch));report.put("language",mc.getLanguageManager().getSelected());
            report.put("scope","Isolated Forge 47.4.10 client, actual AlchemyPacket encode/channel/decode/production handler/menu/block-update and feedback; no external shader or full UI redesign claim.");
            Files.writeString(OUT.resolve("result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }catch(Throwable error){com.mojang.logging.LogUtils.getLogger().error("Architecture acceptance report failed",error);}
        mc.stop();
    }
    private static void build(net.minecraft.server.level.ServerLevel level){
        String[][] plan={{"GGGGG","GCCCG","GCKCG","GCCCG","GGGGG"},{"GRFRG","R...R","F...F","R...R","GRFRG"},
                {"GRGRG","R...R","G...G","R...R","GRGRG"},{"GGGGG","GGGGG","GG.GG","GGGGG","GGGGG"}};
        AlchemyFurnaceStructure.mutating=true;
        try{for(int y=0;y<4;y++)for(int z=0;z<5;z++)for(int x=0;x<5;x++){
            var state=switch(plan[y][z].charAt(x)){
                case 'G'->Blocks.GOLD_BLOCK.defaultBlockState();case 'C'->WuXingBlocks.ETERNAL_SPIRIT_COAL_BLOCK.get().defaultBlockState();
                case 'K'->WuXingBlocks.ALCHEMY_FURNACE.get().defaultBlockState();case 'R'->WuXingBlocks.RUNE_STONE.get().defaultBlockState();
                case 'F'->Blocks.BLAST_FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,x==0?Direction.WEST:x==4?Direction.EAST:z==0?Direction.NORTH:Direction.SOUTH);
                default->Blocks.AIR.defaultBlockState();};level.setBlock(POS.offset(x-2,y,z-2),state,3);
        }}finally{AlchemyFurnaceStructure.mutating=false;}
        AlchemyFurnaceStructure.refreshAround(level,POS);
    }
}
