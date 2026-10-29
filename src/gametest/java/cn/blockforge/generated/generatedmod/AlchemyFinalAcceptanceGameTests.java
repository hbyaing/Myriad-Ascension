package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.alchemy.batch.*;
import cn.blockforge.generated.generatedmod.alchemy.inputs.*;
import cn.blockforge.generated.generatedmod.fixtures.GameTestPlayer;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.gametest.*;
import java.nio.file.*;
import java.util.*;

/** Two real server-player instances; these tests do not claim two networked client acceptance. */
@GameTestHolder("wuxingxiantu_entry") @PrefixGameTestTemplate(false)
public final class AlchemyFinalAcceptanceGameTests {
    @GameTest(template="empty",batch="v5_final_faults") public static void competingPlayersCannotControlOrClaimAnotherOwnersBatch(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h);var other=new GameTestPlayer(h.getLevel())){
            other.setPos(f.player().getX(),f.player().getY(),f.player().getZ());
            h.assertTrue(f.furnace().startV5Batch(f.player()),"Owner start failed");var before=f.furnace().v5ServerRecord();var id=before.getUUID("Id");
            h.assertTrue(!f.furnace().startV5Batch(other)&&!f.furnace().v5Control(other,260)&&before.equals(f.furnace().v5ServerRecord()),"Competing player changed escrow");
            h.assertTrue(f.furnace().sealV5Batch(),"Seal failed");
            for(int i=0;i<3;i++)h.assertTrue(f.furnace().collectResult(other)==0&&!BatchDelivery.claim(other,id),"Other owner claimed result");
            h.assertTrue(f.furnace().collectResult(f.player())==1,"Owner lost result");
            h.assertTrue(f.furnace().collectResult(other)==0&&f.furnace().collectResult(f.player())==0,"Claim replay minted result");
        }h.succeed();
    }
    @GameTest(template="empty",batch="v5_final_faults") public static void matchingCoordinatesInAnotherDimensionAreNotAValidFurnaceSession(GameTestHelper h){
        var nether=h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        try(var f=V5BatchGameTests.setup(h);var remote=new GameTestPlayer(nether)){
            remote.setPos(f.player().getX(),f.player().getY(),f.player().getZ());
            h.assertTrue(!f.furnace().stillValid(remote)&&!f.furnace().startV5Batch(remote),"Cross-dimension coordinates bypassed session validation");
            h.assertTrue(f.furnace().startV5Batch(f.player()),"Start failed");
            h.assertTrue(!f.furnace().v5Control(remote,260)&&f.furnace().collectResult(remote)==0,"Cross-dimension operation accepted");
        }h.succeed();
    }
    @GameTest(template="empty",batch="v5_final_faults") public static void spectatorAndDeadOwnerCannotClaimButPendingResultSurvives(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h)){
            h.assertTrue(f.furnace().startV5Batch(f.player())&&f.furnace().sealV5Batch(),"Start/seal failed");
            var before=f.furnace().v5ServerRecord();f.player().setGameMode(GameType.SPECTATOR);
            h.assertTrue(f.furnace().collectResult(f.player())==0,"Spectator claimed");f.player().setGameMode(GameType.SURVIVAL);f.player().setHealth(0);
            h.assertTrue(f.furnace().collectResult(f.player())==0&&f.furnace().v5ServerRecord().equals(before),"Death changed or granted pending result");
            f.player().setHealth(20);h.assertTrue(f.furnace().collectResult(f.player())==1,"Living owner cannot recover pending result");
        }h.succeed();
    }
    @GameTest(template="empty",batch="v5_final_faults") public static void truncatedAuthorityQuarantinesWithoutRefundingEscrowAndCanRecover(GameTestHelper h)throws Exception{
        try(var f=V5BatchGameTests.setup(h)){
            h.assertTrue(f.furnace().startV5Batch(f.player()),"Start failed");var chunk=f.furnace().saveWithoutMetadata();var doc=f.furnace().v5ServerRecord();
            var file=h.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/alchemy-v5/batches/"+doc.getUUID("Id")+".nbt");
            byte[] original=Files.readAllBytes(file);
            try{
                Files.write(file,new byte[]{10,0});f.furnace().load(chunk);f.furnace().restoreV5();
                h.assertTrue(f.furnace().busy()&&f.furnace().inventoryLocked()&&!f.furnace().startV5Batch(f.player())&&f.furnace().collectResult(f.player())==0,"Corruption did not fail closed");
                h.assertTrue(Arrays.equals(Files.readAllBytes(file),new byte[]{10,0}),"Fault handler rewrote evidence");
            }finally{Files.write(file,original);}
            f.furnace().restoreV5();h.assertTrue(f.furnace().v5ServerRecord().equals(doc)&&f.furnace().getItem(0).isEmpty(),"Recovery rerolled or refunded escrow");
        }h.succeed();
    }
    @GameTest(template="empty",batch="v5_final_faults") public static void allMoonPhasesUseRealWorldFactsWithoutGeneratingStock(GameTestHelper h){
        var level=h.getLevel();long time=level.getDayTime();var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,3,1)).atY(level.getMaxBuildHeight()-5);
        boolean rain=level.isRaining(),thunder=level.isThundering();
        try{level.setWeatherParameters(10000,0,false,false);for(int phase=0;phase<8;phase++){
            level.setDayTime(phase*24000L+18000);level.updateSkyBrightness();var sample=AlchemyEnvironment.sample(level,pos);
            h.assertTrue(sample.values().get("moon_phase")==phase&&sample.values().get("moon_input_rate").equals(InputDefinitions.current().moonStrength.get(phase)),"Moon response mismatch "+phase);
            h.assertTrue(sample.values().get("full_moon_night")== (phase==0?1L:0L)&&sample.values().get("new_moon_night")== (phase==4?1L:0L),"Phase facts double-counted or stale");
        }}finally{level.setDayTime(time);level.updateSkyBrightness();level.setWeatherParameters(10000,10000,rain,thunder);}h.succeed();
    }
    @GameTest(template="empty",batch="v5_final_faults") public static void moonBottleReloadDoesNotCatchUpSkippedTicks(GameTestHelper h){
        var level=h.getLevel();long time=level.getDayTime();var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,3,1)).atY(level.getMaxBuildHeight()-5);
        try{level.setDayTime(18000);level.updateSkyBrightness();var state=WuXingBlocks.MOON_BOTTLE.get().defaultBlockState();var bottle=new MoonBottleBlockEntity(pos,state);bottle.setLevel(level);
            MoonBottleBlockEntity.tick(level,pos,state,bottle);var first=bottle.saveWithoutMetadata();
            var reloaded=new MoonBottleBlockEntity(pos,state);reloaded.setLevel(level);reloaded.load(first);MoonBottleBlockEntity.tick(level,pos,state,reloaded);
            h.assertTrue(reloaded.contents().total()==0,"First loaded tick granted catch-up moon stock");
            var paid=new MoonBottleData();paid.collect(new AlchemyEnvironment.Sample("minecraft:overworld",0,18000,Map.of("night",1L,"open_sky",1L,"moon_phase",0L,"moon_input_rate",1000L)));
            var saved=first.copy();saved.put("MoonBottle",paid.save());reloaded.load(saved);MoonBottleBlockEntity.tick(level,pos,state,reloaded);
            h.assertTrue(reloaded.contents().total()==1000,"Reload duplicated or removed stored resource");
        }finally{level.setDayTime(time);level.updateSkyBrightness();}h.succeed();
    }
}
