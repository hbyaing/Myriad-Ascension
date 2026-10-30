package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.alchemy.batch.*;
import cn.blockforge.generated.generatedmod.alchemy.simulation.*;
import cn.blockforge.generated.generatedmod.alchemy.inputs.*;
import cn.blockforge.generated.generatedmod.fixtures.GameTestPlayer;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import java.util.*;
import static cn.blockforge.generated.generatedmod.alchemy.simulation.AlchemySimulation.*;

@GameTestHolder("wuxingxiantu_entry") @PrefixGameTestTemplate(false)
public final class V5IncidentGameTests {
    @GameTest(template="empty") public static void directHeatSelectionStopsWithoutPassingThroughStrongFire(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h)){
            var initial=V5Batch.prepare(f.furnace(),f.player());
            long time=h.getLevel().getGameTime();
            var stopped=BatchIncidents.control(initial,1000,time);
            h.assertTrue(stopped.getInt("HeatMode")==0&&stopped.getString("State").equals("ACTIVE"),"Stopping heat aborted batch or increased fire");
            h.assertTrue(V5Batch.core(stopped).equals(V5Batch.core(initial)),"Selecting heat advanced the reaction");
            var strong=BatchIncidents.control(stopped,1016,time);
            var stoppedAgain=BatchIncidents.control(strong,1000,time);
            h.assertTrue(strong.getInt("HeatMode")==16&&stoppedAgain.getInt("HeatMode")==0,"Emergency stop blocked by cooldown");
            var low=BatchIncidents.control(strong,1001,time);
            h.assertTrue(low.getInt("HeatMode")==1,"Direct reduction did not reach requested mode");
            h.assertTrue(BatchIncidents.control(low,1003,time)==low,"Invalid heat accepted");
            var sealed=V5Batch.seal(low);
            h.assertTrue(BatchIncidents.control(sealed,1001,time+20)==sealed,"Terminal batch changed");
        }h.succeed();
    }
    private static CompoundTag doc(V5BatchGameTests.Fixture f,long burst,long thermal){
        var d=V5Batch.prepare(f.furnace(),f.player());var json=SimulationRuleLoader.document(d.getString("Profile"));
        json.getAsJsonObject("rules").getAsJsonObject("limits").addProperty("burstPressure",burst);
        json.getAsJsonObject("rules").getAsJsonObject("limits").addProperty("maximumDamage",thermal);
        d.putString("Profile",json.toString());var rules=V5Batch.profile(d.getString("Profile"),d.getInt("Burn")).rules();var old=V5Batch.core(f.furnace().v5Batch()?f.furnace().v5ServerRecord():V5Batch.prepare(f.furnace(),f.player()));
        var core=AlchemySimulation.begin(rules,d.getUUID("Id").toString(),old.stocks(),0,old.referenceMass(),old.seed(),old.environment());
        d.putByteArray("Core",SimulationSnapshotCodec.encodeServerSnapshot(rules,core));V5Batch.validate(d);return d;
    }
    private static CompoundTag step(CompoundTag d,V5BatchGameTests.Fixture f){
        d=d.copy();d.putLong("LastWorldTick",f.player().level().getGameTime()-1);
        return V5Batch.advance(d,(net.minecraft.server.level.ServerLevel)f.player().level(),f.pos());
    }
    private static void conserve(GameTestHelper h,CompoundTag d){var p=V5Batch.profile(d.getString("Profile"),d.getInt("Burn"));var s=V5Batch.core(d);AlchemySimulation.validate(p.rules(),s);h.assertTrue(s.ledger().initialMass()==40000,"Unexpected fixture mass");V5Batch.validate(d);}
    @GameTest(template="empty") public static void paidHeatRaisesDamageAndAshUsesOneConservedTerminal(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h)){
            var d=doc(f,1000000,100);d.putInt("HeatMode",16);d=step(d,f);h.assertTrue((d.getInt("Warning")&1)!=0,"Missing thermal warning");
            d=step(d,f);conserve(h,d);h.assertTrue(V5Batch.core(d).status()==Status.ASHED&&V5Batch.result(d).is(WuXingItems.PILL_ASH.get()),"Overheat did not ash");
            h.assertTrue(V5Batch.core(d).stocks().isEmpty()&&V5Batch.core(d).ledger().ashMass()==40000,"Ash awarded besides retained raw stocks");
            h.assertTrue(V5Batch.advance(d,h.getLevel(),f.pos())==d,"Terminal advanced twice");
            h.assertTrue(d.getInt("FuelSpent")>12,"Extra heat was free");
        }h.succeed();
    }
    @GameTest(template="empty") public static void pressureWinsOverAshAndDeviceDamageCannotRepeat(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h)){
            var d=doc(f,500,1);d.putInt("HeatMode",16);d=step(d,f);conserve(h,d);var c=V5Batch.core(d);
            h.assertTrue(c.status()==Status.EXPLODED&&c.ledger().ashMass()==30000&&c.ledger().ventedMass()==10000,"Explosion priority/allocation wrong");
            h.assertTrue(d.getInt("DeviceWear")==500&&step(d,f).getInt("DeviceWear")==500,"Repeated equipment damage");
            var device=doc(f,1000000,1);device.putInt("StructureDamage",1000);device.putInt("HeatMode",16);var beforeDevice=V5Batch.core(device);device=step(device,f);h.assertTrue(V5Batch.core(device).status()==Status.EXPLODED&&V5Batch.core(device).reason().equals("DEVICE_FAILURE")&&V5Batch.core(device).ledger().ashMass()==30000,"Thermal ash incorrectly outranked hard device failure");
            h.assertTrue(V5Batch.core(device).operations().stream().anyMatch(op->op.action()==Action.DEVICE_FAILURE),"Device accident missing from replay history");
            h.assertTrue(AlchemySimulation.step(V5Batch.profile(device.getString("Profile"),1).rules(),beforeDevice,V5Batch.core(device).operations()).snapshot().equals(V5Batch.core(device)),"Recorded device failure does not replay deterministically");
        }h.succeed();
    }
    @GameTest(template="empty") public static void pressureWarningPrecedesBurstAndVentingPaysRealMass(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h)){
            var normal=doc(f,150,1000000);var warned=step(step(normal,f),f);h.assertTrue((warned.getInt("Warning")&2)!=0&&V5Batch.core(warned).status()==Status.ACTIVE,"No reaction interval");
            var vent=normal.copy();var vr=V5Batch.profile(vent.getString("Profile"),1).rules();var vc=V5Batch.core(vent);
            vent.putByteArray("Core",SimulationSnapshotCodec.encodeServerSnapshot(vr,AlchemySimulation.begin(vr,vc.batchId(),Map.of("herb_mu",40000L),0,40000,vc.seed(),vc.environment())));vent.putBoolean("Vent",true);vent=step(vent,f);conserve(h,vent);
            h.assertTrue(V5Batch.core(vent).ledger().ventedMass()==10000&&V5Batch.core(vent).ledger().ventedPotential()>0,"Venting did not sacrifice actual resources");
            h.assertTrue(V5Batch.core(vent).stocks().values().stream().mapToLong(Long::longValue).sum()==30000,"Venting duplicated stocks");
        }h.succeed();
    }
    @GameTest(template="empty") public static void emergencyStopIsResidueAndCannotReverseCommittedAccident(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h)){
            var d=doc(f,500,1);var stopped=V5Batch.seal(d);h.assertTrue(V5Batch.result(stopped).is(WuXingItems.ALCHEMY_RESIDUE.get()),"Stop returned successful pill");conserve(h,stopped);
            var condensed=doc(f,1000000,1000000);for(int i=0;i<16;i++)condensed=step(condensed,f);h.assertTrue(V5Batch.result(V5Batch.seal(condensed)).is(WuXingItems.WASTE_PILL.get()),"Early condensed harvest mislabeled uncondensed residue");
            d.putInt("HeatMode",16);var exploded=step(d,f);var after=V5Batch.seal(exploded);h.assertTrue(V5Batch.core(after).status()==Status.EXPLODED&&V5Batch.result(after).is(WuXingItems.PILL_ASH.get()),"Stop reversed explosion");
        }h.succeed();
    }
    @GameTest(template="empty") public static void structureDamageAtCondensationCreatesWasteWithoutFakeGrade(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h)){
            var d=doc(f,1000000,1000000);d.putInt("StructureDamage",300);for(int i=0;i<21;i++)d=step(d,f);conserve(h,d);
            h.assertTrue(V5Batch.core(d).status()==Status.FINISHED&&V5Batch.result(d).is(WuXingItems.WASTE_PILL.get()),"Unstable condensate mislabelled successful pill");
            h.assertTrue(!V5Batch.result(d).getTag().contains("PillGrade"),"Waste given a fake grade");
        }h.succeed();
    }
    @GameTest(template="empty") public static void brokenStructureContinuesToFiniteDeviceFailure(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h)){
            var d=doc(f,1000000,1000000);h.getLevel().removeBlock(f.pos().offset(2,0,2),false);AlchemyFurnaceStructure.refreshAround(h.getLevel(),f.pos());h.assertTrue(!AlchemyFurnaceBlock.isFormed(f.furnace().getBlockState()),"Broken fixture reformed");
            for(int i=0;i<5;i++)d=step(d,f);conserve(h,d);
            h.assertTrue(V5Batch.core(d).status()==Status.EXPLODED&&V5Batch.core(d).reason().equals("DEVICE_FAILURE"),"Broken furnace paused for free forever");
        }h.succeed();
    }
    private static CompoundTag moon(CompoundTag d){return moon(d,100);}
    private static CompoundTag moon(CompoundTag d,int amount){
        var p=V5Batch.profile(d.getString("Profile"),d.getInt("Burn"));var s=V5Batch.core(d);var stocks=new TreeMap<>(s.stocks());stocks.put("moon_essence",(long)amount);
        var c=AlchemySimulation.begin(p.rules(),s.batchId(),stocks,0,s.referenceMass()+amount,s.seed(),s.environment());d.putByteArray("Core",SimulationSnapshotCodec.encodeServerSnapshot(p.rules(),c));d.putBoolean("MoonDependent",true);return d;
    }
    @GameTest(template="empty") public static void lunarDiscontinuityWarnsAndWarmHoldCostsMoonAndFuel(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h)){
            var d=moon(doc(f,1000000,1000000));d.putBoolean("Warming",true);var held=step(d,f);
            h.assertTrue(held.getInt("ProcessStep")==0&&V5Batch.core(held).tick()==1,"Warm hold advanced recipe or stopped time");
            h.assertTrue(V5Batch.core(held).stocks().get("moon_essence")==99&&held.getInt("FuelSpent")==11&&held.getInt("FuelReserved")==18,"Warm hold resource debit wrong");
            h.assertTrue(held.getInt("StructureDamage")==0&&(held.getInt("Warning")&32)!=0,"Paid warm hold not applied");
            d.putBoolean("Warming",false);d=step(d,f);
            if(V5Batch.core(d).environment().getOrDefault("moon_input_rate",0L)==0)h.assertTrue((d.getInt("Warning")&4)!=0&&d.getInt("StructureDamage")>0,"Day/occlusion lost lunar warning");
            AlchemySimulation.validate(V5Batch.profile(held.getString("Profile"),1).rules(),V5Batch.core(held));
        }h.succeed();
    }
    @GameTest(template="empty") public static void warmSupplyExhaustionEndsAsFiniteResidue(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h)){
            var d=moon(doc(f,1000000,1000000));d.putBoolean("Warming",true);d.putInt("FuelBalance",0);d.putInt("FuelInitial",28);d=step(d,f);
            h.assertTrue(V5Batch.core(d).status()==Status.ABORTED&&V5Batch.result(d).is(WuXingItems.ALCHEMY_RESIDUE.get())&&d.getString("TerminalCause").equals("WARM_SUPPLY_EXHAUSTED"),"Empty hold gave free pause");V5Batch.validate(d);
        }h.succeed();
    }
    @GameTest(template="empty") public static void ownerOnlyControlsRateLimitAndOldBatchesStayBound(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h);var other=new GameTestPlayer(h.getLevel())){
            h.assertTrue(f.furnace().startV5Batch(f.player()),"Start failed");other.setPos(f.player().position());
            h.assertTrue(!f.furnace().v5Control(other,246)&&f.furnace().v5Control(f.player(),246)&&!f.furnace().v5Control(f.player(),246),"Owner/control flood gate broken");
            h.assertTrue(f.furnace().v5Control(f.player(),245),"Different emergency control incorrectly shares cooldown");
            var d=f.furnace().v5ServerRecord();var copy=d.copy();f.furnace().load(f.furnace().saveWithoutMetadata());f.furnace().restoreV5();h.assertTrue(f.furnace().v5ServerRecord().equals(copy),"Restart lost paid controls");
            d.remove("IncidentVersion");h.assertTrue(BatchIncidents.control(d,246,h.getLevel().getGameTime())==d,"Legacy batch received new unpinned controls");
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200) public static void actualBlastClaimReplayAndPaidRepair(GameTestHelper h){
        var f=V5BatchGameTests.setup(h);f.player().cleanupAfter(h,f::close);
        var herb=f.furnace().getItem(0).copy();herb.setCount(8);f.furnace().setItem(0,herb);
        h.assertTrue(f.furnace().startV5Batch(f.player()),"Start failed");var d=f.furnace().v5ServerRecord();d.putInt("HeatMode",16);new BatchJournal(h.getLevel().getServer()).write(d);f.furnace().restoreV5();
        var stale=f.furnace().saveWithoutMetadata();
        h.runAfterDelay(100,()->{
            var terminal=f.furnace().v5ServerRecord();h.assertTrue(V5Batch.core(terminal).status()==Status.EXPLODED&&f.furnace().v5DeviceWear()==500,"Actual furnace did not commit explosion");
            var menu=new AlchemyFurnaceMenu(1,f.player().getInventory(),f.furnace());h.assertTrue(menu.stillValid(f.player())&&menu.clickMenuButton(f.player(),249),"Damaged/unformed menu closed or ash claim failed");f.furnace().load(stale);f.furnace().restoreV5();
            h.assertTrue(!f.furnace().hasResult()&&f.furnace().v5DeviceWear()==500,"Old active chunk reversed committed accident");
            h.assertTrue(!f.furnace().upgradeCore(f.player()),"Repair was free");f.player().getInventory().setItem(4,new ItemStack(Items.IRON_INGOT,4));
            h.assertTrue(f.furnace().upgradeCore(f.player())&&f.furnace().v5DeviceWear()==0&&f.player().getInventory().getItem(4).isEmpty(),"Paid repair failed");
            h.assertTrue(new BatchJournal(h.getLevel().getServer()).read(terminal.getUUID("Id")).getInt("DeviceWear")==0,"Repair not persisted to authority");h.succeed();
        });
    }

    @GameTest(template="empty") public static void unloadedWorldGapDamagesContinuityWithoutOfflineFuelOrReroll(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h)){
            var d=doc(f,1000000,1000000);long seed=V5Batch.core(d).seed();d.putLong("LastWorldTick",h.getLevel().getGameTime()-200);
            var resumed=V5Batch.advance(d,h.getLevel(),f.pos());conserve(h,resumed);
            h.assertTrue(resumed.getInt("StructureDamage")==400&&resumed.getLong("InterruptedWorldTicks")==200,"Unloaded gap was a free continuity hold");
            h.assertTrue(V5Batch.core(resumed).tick()==1&&resumed.getInt("FuelSpent")==11&&V5Batch.core(resumed).seed()==seed,"Offline gap simulated extra stock/fuel or rerolled seed");
        }h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=200) public static void fundedLongWarmRouteHasBoundedReplayableHistory(GameTestHelper h){
        try(var f=V5BatchGameTests.setup(h)){
            var d=moon(doc(f,1000000,1000000),1000);d.putInt("FuelBalance",1000);d.putInt("FuelInitial",1028);d.putBoolean("Warming",true);
            for(int i=0;i<600;i++)d=step(d,f);
            h.assertTrue(V5Batch.core(d).status()==Status.ACTIVE&&V5Batch.core(d).tick()==600&&d.getInt("ProcessStep")==0,"Long warm route exhausted short prototype budget");
            h.assertTrue(V5Batch.core(d).stocks().get("moon_essence")==400&&d.getInt("FuelSpent")==610&&d.getInt("FuelReserved")==18,"Long hold violated finite supply");
            V5Batch.validate(d);var journal=new BatchJournal(h.getLevel().getServer());journal.write(d);h.assertTrue(journal.read(d.getUUID("Id")).equals(d),"Long hold checkpoint did not roundtrip");
        }h.succeed();
    }

}
