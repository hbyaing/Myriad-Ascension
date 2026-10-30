package cn.blockforge.generated.generatedmod.alchemy.batch;

import cn.blockforge.generated.generatedmod.*;
import cn.blockforge.generated.generatedmod.alchemy.inputs.*;
import cn.blockforge.generated.generatedmod.alchemy.simulation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import java.util.*;
import static cn.blockforge.generated.generatedmod.alchemy.simulation.AlchemySimulation.*;

/** Pinned incident policy, paid controls and finite byproduct classification. No client-authored outcomes. */
public final class BatchIncidents {
    private static final Set<String> KEYS=Set.of("schema","warningPercent","structureFailure","structureLoss","moonLoss","warmFuel","warmMoon","ventDivisor","minimumCondensedPotential");
    private static com.google.gson.JsonObject policy(CompoundTag doc){return SimulationRuleLoader.document(doc.getString("Profile")).getAsJsonObject("incidents");}
    private static int value(CompoundTag doc,String key){return policy(doc).get(key).getAsBigDecimal().intValueExact();}
    public static void initialize(CompoundTag doc){
        if(!SimulationRuleLoader.document(doc.getString("Profile")).has("incidents"))return;
        doc.putInt("IncidentVersion",1);doc.putInt("HeatMode",1);doc.putLong("LastControlTick",-100);doc.putInt("ProcessStep",0);
        doc.putBoolean("MoonDependent",V5Batch.core(doc).stocks().getOrDefault("moon_essence",0L)>0&&V5Batch.core(doc).environment().getOrDefault("moon_input_rate",0L)>0);
    }
    public static void validate(CompoundTag doc){
        if(!doc.contains("IncidentVersion"))return;
        var p=policy(doc);if(doc.getInt("IncidentVersion")!=1||p==null||!p.keySet().equals(KEYS)||value(doc,"schema")!=1)throw new IllegalArgumentException("Unknown incident policy");
        for(String key:KEYS){int n=value(doc,key);if(n<1||n>1000000)throw new IllegalArgumentException("Incident limit invalid");}
        if(value(doc,"warningPercent")>90||value(doc,"ventDivisor")<2||value(doc,"structureFailure")>1000)throw new IllegalArgumentException("Unsafe incident limits");
        if(!Set.of(0,1,4,16).contains(doc.getInt("HeatMode"))||doc.getInt("StructureDamage")<0||doc.getInt("StructureDamage")>1000||doc.getInt("ProcessStep")<0||doc.getInt("ProcessStep")>60)throw new IllegalArgumentException("Invalid controls/damage");
    }
    /** One control of each kind per five world ticks, serialized before application by the furnace. */
    public static CompoundTag control(CompoundTag original,int button,long time){
        V5Batch.validate(original);if(!original.contains("IncidentVersion")||!original.getString("State").equals("ACTIVE"))return original;
        String cooldown="Control"+button;if(button!=1000&&original.contains(cooldown)&&time-original.getLong(cooldown)<5)return original;
        var doc=original.copy();
        switch(button){
            case 1000,1001,1004,1016 -> {
                int mode=button-1000;
                if(doc.getInt("HeatMode")==mode)return original;
                doc.putInt("HeatMode",mode);
            }
            case 245 -> doc.putBoolean("Vent",true);
            case 246 -> doc.putInt("HeatMode",switch(doc.getInt("HeatMode")){case 0->1;case 1->4;case 4->16;default->0;});
            case 247 -> doc.putBoolean("Warming",!doc.getBoolean("Warming"));
            default -> {return original;}
        }
        if(doc.contains("ResearchStart")){var history=doc.getList("ResearchControls",net.minecraft.nbt.Tag.TAG_COMPOUND);if(history.size()<512){var e=new CompoundTag();e.putInt("Button",button);e.putLong("Step",V5Batch.core(original).tick());e.putInt("Mode",doc.getInt("HeatMode"));e.putBoolean("Warm",doc.getBoolean("Warming"));history.add(e);doc.put("ResearchControls",history);}else doc.putBoolean("ResearchControlsIncomplete",true);}
        doc.putLong("LastControlTick",time);doc.putLong(cooldown,time);validate(doc);return doc;
    }
    public static CompoundTag advance(CompoundTag original,ServerLevel level,BlockPos pos){
        V5Batch.validate(original);if(!original.getString("State").equals("ACTIVE")||original.getLong("LastWorldTick")>=level.getGameTime())return original;
        var doc=original.copy();var profile=V5Batch.profile(doc.getString("Profile"),doc.getInt("Burn"));var rules=profile.rules();var s=V5Batch.core(doc);
        if(s.status()!=Status.ACTIVE)return original;
        var trajectory=new InputTrajectory(new InputDefinitions(doc.getString("Inputs")),AlchemyInputs.fire(V5Batch.inventory(doc).get(AlchemyFurnaceBlockEntity.IGNITION_SLOT)));
        var sample=trajectory.sample(level,pos);boolean formed=doc.getString("Entry").equals("heavenly")?level.hasChunkAt(pos):AlchemyFurnaceBlock.isFormed(level.getBlockState(pos));
        int process=doc.getInt("ProcessStep"),desired=0,total=0;for(int duration:profile.phases()){total+=duration;if(process<total)break;desired++;}
        boolean warm=doc.getBoolean("Warming");long moon=s.stocks().getOrDefault("moon_essence",0L);
        boolean moonMissing=doc.getBoolean("MoonDependent")&&sample.values().getOrDefault("moon_input_rate",0L)==0;
        int damage=doc.getInt("StructureDamage");
        long gap=Math.max(0,level.getGameTime()-doc.getLong("LastWorldTick"));
        if(gap>40){
            damage+=(int)Math.min(400,(gap/20-1)*50);doc.putLong("InterruptedWorldTicks",doc.getLong("InterruptedWorldTicks")+gap);
            s=new Snapshot(s.batchId(),s.rulesHash(),s.tick(),s.phase(),s.status(),s.reason(),s.stocks(),s.heat(),s.damage(),s.referenceMass(),s.seed(),s.environment(),s.draws(),Map.of(),s.lastSequence(),s.operations(),s.ledger());
        }
        if(!formed)damage+=value(doc,"structureLoss")*(s.phase()==SimulationRules.Phase.CONDENSE?2:1);
        if(moonMissing&&!warm)damage+=value(doc,"moonLoss");
        doc.putInt("StructureDamage",Math.min(1000,damage));
        var controls=new ArrayList<InputTrajectory.Control>();
        if(!warm&&s.phase().ordinal()<desired)controls.add(new InputTrajectory.Control(Action.ADVANCE,"",0));
        int base=desired<5?profile.burn():0,heatMode=formed?doc.getInt("HeatMode"):0;
        int extra=warm?value(doc,"warmFuel"):Math.multiplyExact(base,Math.max(0,heatMode-1));
        boolean empty=doc.getInt("FuelBalance")<extra||warm&&moon<value(doc,"warmMoon");
        if(empty){extra=0;if(damage<value(doc,"structureFailure"))controls.add(new InputTrajectory.Control(Action.ABORT,"",0));doc.putString("StopReason","WARM_SUPPLY_EXHAUSTED");}
        else {
            if(warm)controls.add(new InputTrajectory.Control(Action.VENT,"moon_essence",value(doc,"warmMoon")));
            long heat=warm?0:Math.min(new InputDefinitions(doc.getString("Inputs")).fireMaximumRate(trajectoryFire(doc)),(long)base*10*heatMode);
            if(heat>0)controls.add(new InputTrajectory.Control(Action.HEAT,"",heat));
            if(doc.getBoolean("Vent"))for(var e:s.stocks().entrySet())if(rules.resources.get(e.getKey()).gas()>0&&e.getValue()>0){
                long amount=Math.max(1,e.getValue()/value(doc,"ventDivisor"));if(warm&&e.getKey().equals("moon_essence"))amount=Math.min(amount,Math.max(0,e.getValue()-value(doc,"warmMoon")));
                if(amount>0&&controls.size()<Math.min(14,30-AlchemyEnvironment.changes(s,sample).size()))controls.add(new InputTrajectory.Control(Action.VENT,e.getKey(),amount));
            }
            if(!warm&&damage<value(doc,"structureFailure")&&process==profile.steps()-1)controls.add(new InputTrajectory.Control(Action.FINISH,"",0));
        }
        var step=trajectory.step(rules,s,level,pos,controls,damage>=value(doc,"structureFailure"));var next=step.snapshot();
        doc.putByteArray("Core",SimulationSnapshotCodec.encodeServerSnapshot(rules,next));doc.putBoolean("Vent",false);
        doc.putInt("FuelReserved",doc.getInt("FuelReserved")-base);doc.putInt("FuelSpent",doc.getInt("FuelSpent")+base+extra);doc.putInt("FuelBalance",doc.getInt("FuelBalance")-extra);
        doc.putInt("ProcessStep",process+(warm?0:1));doc.putLong("LastWorldTick",level.getGameTime());doc.putLong("SampleGameTime",level.getGameTime());doc.putLong("SampleDayTime",level.getDayTime());
        // Warm operation is an extension: pay from balance, retain the base reservation for later process steps.
        if(warm){doc.putInt("FuelReserved",doc.getInt("FuelReserved")+base);doc.putInt("FuelSpent",doc.getInt("FuelSpent")-base);}
        int warning=0,percent=value(doc,"warningPercent");
        if(next.damage()*100>=rules.limits.maximumDamage()*percent)warning|=1;
        if(step.peakPressure().multiply(java.math.BigInteger.valueOf(100)).compareTo(java.math.BigInteger.valueOf(rules.limits.burstPressure()*percent))>=0)warning|=2;
        if(moonMissing)warning|=4;if(damage>0||!formed)warning|=8;if(empty||doc.getInt("FuelBalance")<10)warning|=16;if(warm)warning|=32;
        doc.putInt("Warning",warning);doc.putLong("PeakTemperature",step.peakTemperature());
        if(next.status()==Status.EXPLODED)doc.putInt("DeviceWear",Math.min(1000,doc.getInt("DeviceWear")+500));
        if(next.status()!=Status.ACTIVE){V5Batch.ready(doc);doc.putString("TerminalCause",next.reason().isEmpty()?doc.getString("StopReason"):next.reason());}
        V5Batch.validate(doc);return doc;
    }
    private static String trajectoryFire(CompoundTag doc){return AlchemyInputs.fire(V5Batch.inventory(doc).get(AlchemyFurnaceBlockEntity.IGNITION_SLOT)).id();}
    public static Item resultItem(CompoundTag doc){
        if(!doc.contains("IncidentVersion"))return WuXingItems.SEALED_ALCHEMY_BATCH.get();
        var s=V5Batch.core(doc);
        if(s.status()==Status.EXPLODED||s.status()==Status.ASHED)return WuXingItems.PILL_ASH.get();
        if(s.status()==Status.ABORTED||s.status()==Status.PAUSED)return s.phase().ordinal()>=SimulationRules.Phase.CONDENSE.ordinal()?WuXingItems.WASTE_PILL.get():WuXingItems.ALCHEMY_RESIDUE.get();
        if(doc.getInt("StructureDamage")>200||V5Batch.profile(doc.getString("Profile"),doc.getInt("Burn")).rules().weight(s.stocks(),false)<value(doc,"minimumCondensedPotential"))return WuXingItems.WASTE_PILL.get();
        if(doc.contains("PillResult")&&cn.blockforge.generated.generatedmod.alchemy.effects.PillSnapshot.read(doc.getCompound("PillResult")).grade()==0)return WuXingItems.WASTE_PILL.get();
        return WuXingItems.SEALED_ALCHEMY_BATCH.get();
    }
    private BatchIncidents(){}
}
