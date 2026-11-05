package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.alchemy.batch.*;
import cn.blockforge.generated.generatedmod.alchemy.inputs.*;
import cn.blockforge.generated.generatedmod.plants.*;
import cn.blockforge.generated.generatedmod.fixtures.GameTestPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("wuxingxiantu_entry") @PrefixGameTestTemplate(false)
public final class V5BatchGameTests {
    record Fixture(GameTestPlayer player,AlchemyFurnaceBlockEntity furnace,BlockPos pos) implements AutoCloseable {
        @Override public void close(){try{if(furnace.getLevel().getBlockEntity(pos)==furnace)furnace.getLevel().removeBlock(pos,false);}finally{player.close();}}
    }
    static Fixture setup(GameTestHelper h){
        var pos=FurnaceGameTests.build(h);AlchemyFurnaceStructure.refreshAround(h.getLevel(),pos);
        var furnace=(AlchemyFurnaceBlockEntity)h.getLevel().getBlockEntity(pos);
        var player=new GameTestPlayer(h.getLevel());player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
        GeneratedMod.data(player).wuxingxiantu$learnFire(1);GeneratedMod.data(player).wuxingxiantu$setAura(100);cn.blockforge.generated.generatedmod.alchemy.FormulaKnowledge.unlock(player);
        var saved=furnace.saveWithoutMetadata();saved.putInt("SpiritFuel",furnace.spiritCapacity());furnace.load(saved);
        var herb=new ItemStack(SpiritPlantRegistry.SPIRIT_SPROUT_GRASS_ITEM.get(),4);HerbAge.setYears(herb,1500);herb.getOrCreateTag().putString("ThirdParty","keep");furnace.setItem(0,herb);
        furnace.setItem(8,cn.blockforge.generated.generatedmod.fire.FireBottleData.fresh(1));return new Fixture(player,furnace,pos);
    }
    private static long receipts(GameTestPlayer player){return player.getInventory().items.stream().filter(s->s.hasTag()&&s.getTag().hasUUID(V5Batch.KEY)).mapToInt(ItemStack::getCount).sum();}
    @GameTest(template="empty") public static void rejectBeforeCommitDoesNotMutateItemsFuelMoonOrAura(GameTestHelper h){
        try(var f=setup(h)){
            f.furnace.setItem(1,new ItemStack(Items.APPLE));var before=f.furnace.saveWithoutMetadata();float aura=GeneratedMod.data(f.player).wuxingxiantu$getAura();
            h.assertTrue(!f.furnace.startV5Batch(f.player)&&before.equals(f.furnace.saveWithoutMetadata()),"Rejected batch changed the furnace");
            h.assertTrue(GeneratedMod.data(f.player).wuxingxiantu$getAura()==aura,"Rejected batch charged player");
        }h.succeed();
    }
    @GameTest(template="empty") public static void commitEscrowsOnceLocksMutableAccessAndSanitizesClientSnapshot(GameTestHelper h){
        try(var f=setup(h)){
            int fuel=f.furnace.spiritFuel();float aura=GeneratedMod.data(f.player).wuxingxiantu$getAura();h.assertTrue(f.furnace.startV5Batch(f.player),"Batch failed to start");
            var doc=f.furnace.v5ServerRecord();int after=f.furnace.spiritFuel();h.assertTrue(f.furnace.getItem(0).isEmpty()&&after<fuel,"Real materials/fuel not escrowed");
            h.assertTrue(!f.furnace.startV5Batch(f.player)&&f.furnace.spiritFuel()==after&&doc.equals(f.furnace.v5ServerRecord()),"Replayed start changed batch");
            h.assertTrue(GeneratedMod.data(f.player).wuxingxiantu$getAura()==aura,"Prepaid batch charged player again");
            h.assertTrue(f.furnace.removeItem(8,1).isEmpty()&&f.furnace.removeItemNoUpdate(8).isEmpty()&&!f.furnace.canPlaceItem(0,new ItemStack(Items.DIRT)),"Automation bypassed lock");
            f.furnace.getItem(8).setCount(0);h.assertTrue(f.furnace.getItem(8).getCount()==1,"Mutable item alias escaped escrow lock");
            var update=f.furnace.getUpdateTag();h.assertTrue(!update.contains("V5Ref")&&!update.contains("Core")&&!update.contains("Inputs")&&!update.contains("Materials"),"Private checkpoint/seed source leaked to clients");
            h.assertTrue(doc.getCompound("Original").getList("Items",Tag.TAG_COMPOUND).stream().map(t->(CompoundTag)t).anyMatch(t->t.getCompound("tag").getString("ThirdParty").equals("keep")),"Source NBT lost");
        }h.succeed();
    }
    @GameTest(template="empty") public static void stalePreStartChunkCannotResurrectMaterialsOrRerollSeed(GameTestHelper h){
        try(var f=setup(h)){
            var stale=f.furnace.saveWithoutMetadata();h.assertTrue(f.furnace.startV5Batch(f.player),"Start failed");var expected=f.furnace.v5ServerRecord();
            f.furnace.load(stale);f.furnace.restoreV5();var restored=f.furnace.v5ServerRecord();
            h.assertTrue(restored.equals(expected)&&f.furnace.getItem(0).isEmpty(),"Journal did not override stale pre-start chunk");
            h.assertTrue(V5Batch.core(restored).seed()==V5Batch.core(expected).seed(),"Recovery rerolled seed");
        }h.succeed();
    }
    @GameTest(template="empty") public static void invalidOrReboundCheckpointCannotOverwriteCommittedJournal(GameTestHelper h){
        try(var f=setup(h)){
            h.assertTrue(f.furnace.startV5Batch(f.player),"Start failed");var original=f.furnace.v5ServerRecord();var journal=new BatchJournal(h.getLevel().getServer());var id=original.getUUID("Id");
            var invalid=original.copy();invalid.putString("InputsHash","modified");boolean rejected=false;try{journal.write(invalid);}catch(RuntimeException expected){rejected=true;}
            h.assertTrue(rejected&&journal.read(id).equals(original),"Invalid input binding overwrote durable state");
            invalid=original.copy();invalid.putString("Profile",invalid.getString("Profile").replace("furnace-catalog-5","rebound"));rejected=false;
            try{journal.write(invalid);}catch(RuntimeException expected){rejected=true;}
            h.assertTrue(rejected&&journal.read(id).equals(original),"Rules drift rewrote checkpoint");
            invalid=original.copy();invalid.putInt("Burn",-1);rejected=false;try{journal.write(invalid);}catch(RuntimeException expected){rejected=true;}
            h.assertTrue(rejected&&journal.read(id).equals(original),"Negative saved fuel rate accepted");
            h.assertTrue(new BatchJournal(h.getLevel().getServer()).activeCount(f.player.getUUID())==1,"Durable V5 task missing from concurrency gate");
        }h.succeed();
    }
    @GameTest(template="empty") public static void moonBottleTransferIsPaidOnceAndCarriesSourceWithoutForgingNight(GameTestHelper h){
        try(var f=setup(h)){
            var moon=new MoonBottleData();for(int i=0;i<5;i++)moon.collect(new AlchemyEnvironment.Sample("minecraft:overworld",0,18000,Map.of("night",1L,"open_sky",1L,"moon_phase",0L,"moon_input_rate",1000L)));
            var bottle=new ItemStack(WuXingBlocks.MOON_BOTTLE_ITEM.get());var tag=new CompoundTag();tag.put("MoonBottle",moon.save());bottle.addTagElement("BlockEntityTag",tag);f.furnace.setItem(1,bottle);
            h.assertTrue(f.furnace.startV5Batch(f.player),"Moon batch failed");var doc=f.furnace.v5ServerRecord();
            h.assertTrue(V5Batch.core(doc).stocks().get("moon_essence")==1000,"Unpaid/incorrect lunar input");
            var remaining=MoonBottleData.load(BlockItem.getBlockEntityData(f.furnace.getItem(1)).getCompound("MoonBottle"));h.assertTrue(remaining.total()==4000,"Bottle not charged exactly once");
            h.assertTrue(doc.getCompound("MoonSources").getLong("minecraft:overworld/moon/0")==1000,"Lunar origin lost");
            h.assertTrue(AlchemyMaterialTransfer.move(f.furnace,f.player.getInventory(),null,true)==AlchemyMaterialTransfer.Result.BUSY,"Clear bypassed active escrow");
            h.assertTrue(f.furnace.sealV5Batch(),"Cannot seal lunar batch");
            h.assertTrue(AlchemyMaterialTransfer.move(f.furnace,f.player.getInventory(),null,true)==AlchemyMaterialTransfer.Result.BUSY,"Clear duplicated locked pending bottle");
            h.assertTrue(!f.furnace.startV5Batch(f.player)&&MoonBottleData.load(BlockItem.getBlockEntityData(f.furnace.getItem(1)).getCompound("MoonBottle")).total()==4000,"Replay drained the bottle again");
        }h.succeed();
    }
    @GameTest(template="empty") public static void fullInventoryKeepsResultAndClaimReplayCannotMintAnotherReceipt(GameTestHelper h){
        try(var f=setup(h)){
            h.assertTrue(f.furnace.startV5Batch(f.player)&&f.furnace.sealV5Batch(),"Cannot seal batch");var saved=f.furnace.saveWithoutMetadata();var id=f.furnace.v5ServerRecord().getUUID("Id");
            for(int i=0;i<36;i++)f.player.getInventory().setItem(i,new ItemStack(Items.DIAMOND,64));
            h.assertTrue(f.furnace.collectResult(f.player)==0&&f.furnace.hasResult(),"Full inventory lost the output");
            f.player.getInventory().setItem(5,ItemStack.EMPTY);h.assertTrue(f.furnace.collectResult(f.player)==1&&receipts(f.player)==1,"First claim failed");
            f.furnace.load(saved);f.furnace.restoreV5();h.assertTrue(!f.furnace.hasResult()&&receipts(f.player)==1,"Stale ready chunk duplicated claim");
            h.assertTrue(new BatchJournal(h.getLevel().getServer()).read(id).getString("State").equals("CLAIMED"),"Claim receipt not durable");
        }h.succeed();
    }
    @GameTest(template="empty") public static void deliveryIntentRecoveryFindsAlreadySavedPlayerReceipt(GameTestHelper h){
        try(var f=setup(h)){
            h.assertTrue(f.furnace.startV5Batch(f.player)&&f.furnace.sealV5Batch(),"Seal failed");var id=f.furnace.v5ServerRecord().getUUID("Id");h.assertTrue(f.furnace.collectResult(f.player)==1,"Claim failed");
            var journal=new BatchJournal(h.getLevel().getServer());var pending=journal.read(id);pending.putString("State","DELIVERING");pending.putUUID("Recipient",f.player.getUUID());journal.pending(f.player.getUUID(),id,true);journal.write(pending);
            BatchDelivery.recover(f.player);BatchDelivery.recover(f.player);
            h.assertTrue(receipts(f.player)==1&&journal.pendingFor(f.player.getUUID()).isEmpty()&&journal.read(id).getString("State").equals("CLAIMED"),"Interrupted acknowledgement duplicated delivery");
        }h.succeed();
    }
    @GameTest(template="empty") public static void dismantleSealsActiveBatchAndDoesNotRestoreItsEscrowAtOldPosition(GameTestHelper h){
        try(var f=setup(h)){
            h.assertTrue(f.furnace.startV5Batch(f.player),"Start failed");var id=f.furnace.v5ServerRecord().getUUID("Id");
            var drops=net.minecraft.world.level.block.Block.getDrops(f.furnace.getBlockState(),h.getLevel(),f.pos,f.furnace);
            h.assertTrue(drops.size()==1&&BlockItem.getBlockEntityData(drops.get(0)).getInt("SpiritFuel")==110,"Dismantle lost unspent prepaid fuel");
            h.getLevel().removeBlock(f.pos,false);var doc=new BatchJournal(h.getLevel().getServer()).read(id);
            h.assertTrue(doc.getBoolean("Detached")&&doc.getString("State").equals("READY"),"Dismantle lost pending material");
            var items=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(f.pos).inflate(3));
            h.assertTrue(items.stream().filter(e->e.getItem().hasTag()&&e.getItem().getTag().hasUUID(V5Batch.KEY)&&e.getItem().getTag().getUUID(V5Batch.KEY).equals(id)).mapToInt(e->e.getItem().getCount()).sum()==1,"Dismantle lost/duplicated sealed batch");
        }h.succeed();
    }
    @GameTest(template="empty",batch="v5_loaded_furnace",timeoutTicks=650) public static void actualLoadedFurnaceTicksV5ToCompletionWithoutLegacyJobOrPreRolledPill(GameTestHelper h){
        var f=setup(h);f.player.cleanupAfter(h,f::close);
        // Run this long-lived multiblock separately from adjacent fixture mutation.
        // The furnace extends beyond the 4x4 template. Keep every corner loaded for
        // this live-tick test; a fixture chunk unload is not a gameplay device failure.
        for(int x:new int[]{-2,2})for(int z:new int[]{-2,2})
            f.player.keepTicking(h.getLevel(),f.pos.offset(x,0,z));
        h.assertTrue(f.furnace.startV5Batch(f.player),"Start failed");var id=f.furnace.v5ServerRecord().getUUID("Id");
        h.runAfterDelay(470,()->{
            var doc=f.furnace.v5ServerRecord();var core=V5Batch.core(doc);
            h.assertTrue(doc.getString("State").equals("READY")&&core.status()==cn.blockforge.generated.generatedmod.alchemy.simulation.AlchemySimulation.Status.FINISHED,"Real furnace did not finish V5: "+core.status()+" tick="+core.tick()+" reason="+core.reason()+" structureDamage="+doc.getInt("StructureDamage")+" missing="+AlchemyFurnaceStructure.missingCount(h.getLevel(),f.pos));
            h.assertTrue(core.ledger().initialMass()==40000&&doc.contains("ReactionCore")&&core.stocks().containsKey(cn.blockforge.generated.generatedmod.alchemy.effects.AbilityCompiler.BOUND),"Live core did not react/conserve actual herbs");
            h.assertTrue(f.furnace.completedResult().is(WuXingItems.V5_PILL.get())&&!Network.hasAlchemyJob(h.getLevel(),f.pos),"Batch used a legacy planned result");
            h.assertTrue(core.tick()==21&&doc.getInt("FuelReserved")==0&&doc.getInt("FuelSpent")==28,"Loaded timing/fuel accounting changed");
            h.assertTrue(f.furnace.collectResult(f.player)==1&&new BatchJournal(h.getLevel().getServer()).read(id).getString("State").equals("CLAIMED"),"Actual completed batch not claimable");h.succeed();
        });
    }
}
