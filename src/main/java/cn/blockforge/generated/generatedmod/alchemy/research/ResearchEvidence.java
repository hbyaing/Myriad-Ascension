package cn.blockforge.generated.generatedmod.alchemy.research;

import cn.blockforge.generated.generatedmod.alchemy.batch.*;
import cn.blockforge.generated.generatedmod.alchemy.effects.*;
import cn.blockforge.generated.generatedmod.alchemy.simulation.*;
import cn.blockforge.generated.generatedmod.alchemy.inputs.*;
import cn.blockforge.generated.generatedmod.plants.HerbAge;
import cn.blockforge.generated.generatedmod.HerbPreparation;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.*;

/** Owner-authorized measurements. No seeds, resource weights or private reaction rules in a Point. */
public final class ResearchEvidence {
    public record Point(UUID batch,CompoundTag plan,CompoundTag result,CompoundTag operation,String context,String rules) {
        public Point {plan=plan.copy();result=result.copy();operation=operation.copy();}
        @Override public CompoundTag plan(){return plan.copy();}@Override public CompoundTag result(){return result.copy();}@Override public CompoundTag operation(){return operation.copy();}
    }
    public record History(List<Point> points,int unavailable){public History{points=List.copyOf(points);}}
    public static CompoundTag start(Map<String,Long> environment,long day){var n=new CompoundTag();n.putInt("Schema",1);var env=new CompoundTag();environment.forEach(env::putLong);n.put("Environment",env);n.putLong("Day",day);return n;}
    public static String canonical(Tag tag){
        if(tag instanceof CompoundTag n){var keys=new TreeSet<>(n.getAllKeys());var s=new StringBuilder("{");for(String k:keys)s.append(k.length()).append(':').append(k).append(canonical(n.get(k)));return s.append('}').toString();}
        if(tag instanceof ListTag list){var s=new StringBuilder("[");for(var v:list)s.append(canonical(v));return s.append(']').toString();}return tag.getId()+":"+tag.toString()+";";
    }
    public static String hash(CompoundTag n){return AbilityCompiler.hash(canonical(n));}
    public static void validatePlan(CompoundTag p){
        if(!p.getAllKeys().equals(Set.of("Numbers","Choices"))||p.getCompound("Numbers").size()>128||p.getCompound("Choices").size()>64||canonical(p).length()>32768)throw new IllegalArgumentException("Research point budget/schema");
        for(String k:p.getCompound("Numbers").getAllKeys())if(!p.getCompound("Numbers").contains(k,Tag.TAG_LONG)||p.getCompound("Numbers").getLong(k)< -1000000000000L||p.getCompound("Numbers").getLong(k)>1000000000000L)throw new IllegalArgumentException("Point number bounds");
        for(String k:p.getCompound("Choices").getAllKeys())if(!p.getCompound("Choices").contains(k,Tag.TAG_STRING)||p.getCompound("Choices").getString(k).length()>8192)throw new IllegalArgumentException("Point choice bounds");
    }
    /** Dynamic device heat capacity/burst pressure are point coordinates, not a rules-version change. */
    public static String context(CompoundTag doc){var json=SimulationRuleLoader.document(doc.getString("Profile"));var limits=json.getAsJsonObject("rules").getAsJsonObject("limits");limits.remove("heatCapacity");limits.remove("burstPressure");return AbilityCompiler.hash(SimulationRules.ENGINE+"/"+json+"/"+doc.getString("InputsHash")+"/"+AbilityCompiler.hash(doc.getString("EffectPolicy")));}
    public static String currentContext(ServerPlayer p){var n=new CompoundTag();n.putString("Profile",V5Batch.currentProfile(p.serverLevel()));n.putString("InputsHash",InputDefinitions.current().hash);n.putString("EffectPolicy",PillSnapshot.current(p.serverLevel()));return context(n);}
    public static History history(ServerPlayer p,Set<String> publicRules){
        var points=new ArrayList<Point>();int missing=0;var journal=new BatchJournal(p.server);
        for(var t:ResearchPractice.records(p))try{var n=(CompoundTag)t;var d=journal.read(UUID.fromString(n.getString("Batch")));points.add(read(p,d,publicRules));}catch(RuntimeException e){missing++;}
        return new History(points,missing);
    }
    public static Point read(ServerPlayer p,CompoundTag doc,Set<String> publicRules){
        V5Batch.validate(doc);if(!doc.getUUID("Owner").equals(p.getUUID())||doc.getString("State").equals("ACTIVE")||!doc.contains("ResearchStart",Tag.TAG_COMPOUND)||!doc.contains("PillResult"))throw new IllegalArgumentException("No authorized complete measurement");
        var start=doc.getCompound("ResearchStart");if(start.getInt("Schema")!=1||doc.getBoolean("ResearchControlsIncomplete"))throw new IllegalArgumentException("Unknown/incomplete measurement schema");
        var core=V5Batch.core(doc);var initial=start.getCompound("Environment");var replay=initial.copy();
        for(var op:core.operations())if(op.action()==AlchemySimulation.Action.ENVIRONMENT)replay.putLong(op.resource(),op.amount());
        if(!replay.getAllKeys().equals(core.environment().keySet()))throw new IllegalArgumentException("Environment provenance mismatch");
        for(var e:core.environment().entrySet())if(replay.getLong(e.getKey())!=e.getValue())throw new IllegalArgumentException("Environment history mismatch");
        var numbers=new CompoundTag();var choices=new CompoundTag();choices.putString("dimension",doc.getString("Dimension"));
        numbers.putLong("clock",Math.floorMod(start.getLong("Day"),24000));numbers.putLong("fuel",doc.getInt("FuelInitial"));numbers.putLong("interruption",doc.getLong("InterruptedWorldTicks"));
        for(String key:initial.getAllKeys())if(!key.startsWith("fire_"))numbers.putLong("environment/"+key,initial.getLong(key));
        var defs=new InputDefinitions(doc.getString("Inputs"));var materials=new ArrayList<ItemStack>();for(var t:doc.getList("Materials",Tag.TAG_COMPOUND))materials.add(ItemStack.of((CompoundTag)t));
        materials.sort(Comparator.comparing((ItemStack s)->BuiltInRegistries.ITEM.getKey(s.getItem()).toString()).thenComparingInt(ResearchEvidence::materialYears).thenComparing(s->HerbPreparation.prepared(s)?1:0).thenComparingInt(s->defs.schema>=2?materialPotency(s):1));
        var prototypes=new LinkedHashMap<String,ItemStack>();var totals=new LinkedHashMap<String,Long>();
        for(var item:materials){String key=BuiltInRegistries.ITEM.getKey(item.getItem())+"/"+materialYears(item)+"/"+HerbPreparation.prepared(item)+"/"+(defs.schema>=2?materialPotency(item):1);prototypes.putIfAbsent(key,item);totals.merge(key,(long)item.getCount(),Math::addExact);}
        int index=0;for(var entry:prototypes.entrySet()){var item=entry.getValue();choices.putString("material/"+index,BuiltInRegistries.ITEM.getKey(item.getItem())+"/"+HerbPreparation.prepared(item));numbers.putLong("count/"+index,totals.get(entry.getKey()));numbers.putLong("age/"+index,materialYears(item));numbers.putLong("materialPotency/"+index,defs.schema>=2?materialPotency(item):1);index++;}
        long moon=0;for(String k:doc.getCompound("MoonSources").getAllKeys())moon=Math.addExact(moon,doc.getCompound("MoonSources").getLong(k));numbers.putLong("moon",moon);
        var profile=V5Batch.profile(doc.getString("Profile"),doc.getInt("Burn"));numbers.putLong("device/heatCapacity",profile.rules().limits.heatCapacity());numbers.putLong("device/noise",profile.rules().limits.controlNoise());numbers.putLong("device/pressure",profile.rules().limits.burstPressure());
        var fire=AlchemyInputs.fire(V5Batch.inventory(doc).get(cn.blockforge.generated.generatedmod.AlchemyFurnaceBlockEntity.IGNITION_SLOT));choices.putString("fire",AbilityCompiler.hash(fire.toString()));numbers.putLong("fireTier",fire.tier());
        var programs=new TreeMap<String,StringBuilder>();choices.putString("stageProgram",Arrays.toString(profile.phases()));choices.putString("stopRequested",Boolean.toString(doc.getBoolean("ResearchStopRequested")));
        // Compare interventions, not endogenous heat/vent amounts or an accident-shortened phase trace.
        for(var t:doc.getList("ResearchControls",Tag.TAG_COMPOUND)){var op=(CompoundTag)t;String kind=switch(op.getInt("Button")){case 245->"ventProgram";case 246,1000,1001,1004,1016->"heatProgram";case 247->"warmProgram";default->throw new IllegalArgumentException("Unknown intervention");};programs.computeIfAbsent(kind,k->new StringBuilder()).append(op.getLong("Step")).append(':').append(op.getInt("Mode")).append(':').append(op.getBoolean("Warm")).append(';');}
        for(var op:core.operations())if(op.action()==AlchemySimulation.Action.ENVIRONMENT&&!op.resource().startsWith("fire_"))programs.computeIfAbsent("environmentProgram/"+op.resource(),k->new StringBuilder()).append(op.tick()).append(':').append(op.amount()).append(';');
        programs.forEach((k,v)->choices.putString(k,v.toString()));var plan=new CompoundTag();plan.put("Numbers",numbers);plan.put("Choices",choices);validatePlan(plan);
        var taught=p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG).getCompound("alchemyUnderstanding").getCompound("ResearchMethodDefinitions");var known=new HashSet<String>();for(var r:profile.rules().reactions)if(publicRules.contains(r.id())&&taught.getString(r.id()).equals(AbilityCompiler.hash(r.toString())))known.add(r.id());
        var reachable=new HashSet<>(AlchemyInputs.batchWithInert(materials,defs).stocks().keySet());var applicable=new HashSet<String>();boolean changed;
        do{changed=false;for(var r:profile.rules().reactions)if(known.contains(r.id())&&reachable.containsAll(r.consume().keySet())){applicable.add(r.id());if(reachable.addAll(r.produce().keySet()))changed=true;}}while(changed);
        var projectedRules=new SimulationRules(profile.rules().version,new ArrayList<>(profile.rules().resources.values()),profile.rules().reactions.stream().filter(r->!known.contains(r.id())||applicable.contains(r.id())).toList(),profile.rules().limits);
        var operation=ResearchDifficulty.operation(projectedRules,known,ResearchDifficulty.stageBudgets(profile.phases(),core.operations(),core.tick()),Math.max(1,Math.min(defs.fireMaximumRate(fire.id()),doc.getInt("Burn")*10L)),profile.rules().limits.heatCapacity());
        return new Point(doc.getUUID("Id"),plan,doc.getCompound("PillResult"),operation,context(doc),profile.rules().hash);
    }
    private static int materialYears(ItemStack item){return HerbAge.definition(item)==null?0:HerbAge.years(item);}
    private static int materialPotency(ItemStack item){return cn.blockforge.generated.generatedmod.importedcreatures.BeastMaterials.blood(item)?cn.blockforge.generated.generatedmod.importedcreatures.BeastMaterials.potency(item):1;}
    public static List<String> differences(CompoundTag a,CompoundTag b){
        validatePlan(a);validatePlan(b);var result=new ArrayList<String>();for(String kind:List.of("Numbers","Choices")){
            var keys=new TreeSet<>(a.getCompound(kind).getAllKeys());keys.addAll(b.getCompound(kind).getAllKeys());for(String key:keys)if(!Objects.equals(a.getCompound(kind).get(key),b.getCompound(kind).get(key)))result.add(key);
        }return List.copyOf(result);
    }
    public static String compare(Point a,Point b){if(a.batch.equals(b.batch))return "同一批次不能自作对照";if(!a.context.equals(b.context))return "规则或输入定义不同，需复验";var diff=differences(a.plan,b.plan);return diff.isEmpty()?"同条件复现；不代表独立认证":diff.size()==1?"单因素差异："+label(diff.get(0))+"；不能仅凭两批断言因果":"同时变化"+diff.size()+"项，不能作单因素对照";}
    public static String label(String key){if(key.startsWith("age/"))return "药龄";if(key.startsWith("count/"))return "用量";if(key.startsWith("materialPotency/"))return "材料药性强度";if(key.startsWith("material/"))return "材料身份/炮制";if(key.startsWith("environment"))return "环境";if(key.startsWith("device/"))return "设备";return switch(key){case "clock"->"开炉时刻";case "heatProgram"->"控火轨迹";case "stageProgram"->"工序轨迹";case "ventProgram"->"泄压轨迹";case "fire","fireTier"->"火种";case "fuel"->"储备灵力";default->"其它条件";};}
    private ResearchEvidence(){}
}
