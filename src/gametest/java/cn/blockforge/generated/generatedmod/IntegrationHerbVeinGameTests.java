package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.plants.*;
import cn.blockforge.generated.generatedmod.world.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraftforge.gametest.*;
import java.lang.reflect.Proxy;
import java.util.*;

@GameTestHolder("wuxingxiantu_entry")
@PrefixGameTestTemplate(false)
public final class IntegrationHerbVeinGameTests {
    @GameTest(template="empty")
    public static void oldVeinSaveDoesNotAuthorizeNeighborDecoration(GameTestHelper h) {
        var old=new SpiritVeinData();var candidate=new ChunkPos(-7,9);var neighbor=new ChunkPos(-6,9);
        old.queue(candidate);old.register(-6,9,2,new BlockPos(-90,0,150),96);
        var restored=SpiritVeinData.load(old.save(new CompoundTag()));
        h.assertTrue(!restored.canDecorate(candidate)&&!restored.canDecorate(neighbor),"Old queued/detected chunks acquired write permission");
        restored.markDecorationChunk(candidate);
        var reloaded=SpiritVeinData.load(restored.save(new CompoundTag()));
        h.assertTrue(reloaded.canDecorate(candidate)&&!reloaded.canDecorate(neighbor),"Decoration eligibility lost or spread to old neighbor");
        h.assertTrue(reloaded.grade(-6,9)==2,"Migration erased existing vein record");h.succeed();
    }

    @GameTest(template="empty")
    public static void realVeinAlgorithmNeverWritesIntoUnmarkedOldNeighbor(GameTestHelper h) {
        var chunk=new ChunkPos(100000,-99998);long trial=0;
        while(trial<10000&&!SpiritVein.hasVein(trial,chunk.x,chunk.z))trial++;
        h.assertTrue(trial<10000,"Fixture could not select a valid vein seed");
        final long seed=trial;var written=new ArrayList<BlockPos>();
        var data=SpiritVeinData.get(h.getLevel());data.markDecorationChunk(chunk);
        // A controlled continuous cave wall crosses both sides of the new/old chunk boundary.
        // Run the production algorithm, keeping terrain reads/writes in this synthetic fixture.
        var world=(WorldGenLevel)Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),new Class<?>[]{WorldGenLevel.class},(proxy,method,args)->switch(method.getName()) {
            case "getSeed" -> seed;
            case "getLevel" -> h.getLevel();
            case "getMinBuildHeight" -> -64;
            case "getHeight" -> 100;
            case "getBlockState" -> {
                var p=(BlockPos)args[0];yield p.getY()>=0&&p.getY()<=16&&(p.getZ()&1)==1?Blocks.AIR.defaultBlockState():Blocks.STONE.defaultBlockState();
            }
            case "setBlock" -> {written.add(((BlockPos)args[0]).immutable());yield true;}
            default -> throw new UnsupportedOperationException(method.toString());
        });
        var origin=new BlockPos(chunk.getMinBlockX()+8,0,chunk.getMinBlockZ()+8);
        var scan=new SpiritVeinFeature.Scan(world,origin);int batches=0;
        while(!scan.advance(512)){
            batches++;h.assertTrue(scan.lastBatchWork()<=512,"Vein scan exceeded its per-tick work budget");
            h.assertTrue(written.isEmpty(),"Incremental scan placed a partial vein before selection completed");
        }
        h.assertTrue(batches>1&&scan.result()&&written.size()>=64,"Controlled incremental cave produced no vein");
        var incremental=new java.util.HashSet<>(written);written.clear();
        h.assertTrue(SpiritVeinFeature.generate(world,origin)&&incremental.equals(new java.util.HashSet<>(written)),"Budgeted scan changed the vein layout");
        h.assertTrue(written.stream().allMatch(p->new ChunkPos(p).equals(chunk)),"New vein overwrote old neighboring terrain");
        h.assertTrue(data.nearest(origin,64).orElseThrow().count()==written.size(),"Recorded ore count differs from writes");h.succeed();
    }

    @GameTest(template="empty")
    public static void matureHarvestHasOneAgedSpecimenAndFreshPropagules(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,2,1));var level=h.getLevel();
        level.setBlockAndUpdate(pos.below(),Blocks.DIRT.defaultBlockState());
        var plant=(SpiritPlantBlock)SpiritPlantRegistry.GALE_GRASS.get();var mature=plant.defaultBlockState().setValue(SpiritPlantBlock.AGE,plant.definition().matureAge);
        level.setBlockAndUpdate(pos,mature);var specimen=new ItemStack(plant.asItem());HerbAge.setYears(specimen,2500);
        specimen.getOrCreateTag().putInt(HerbAge.ASH_USES,3);HerbCultivationData.get(level).plant(level,pos,plant.definition(),specimen);
        var tool=new ItemStack(Items.DIAMOND_PICKAXE);tool.enchant(Enchantments.BLOCK_FORTUNE,3);boolean sawExtra=false;
        for(int trial=0;trial<32;trial++) {
            var drops=Block.getDrops(mature,level,pos,null,null,tool).stream().filter(s->s.is(plant.asItem())).toList();
            h.assertTrue(drops.stream().filter(s->HerbAge.years(s)==2500).mapToInt(ItemStack::getCount).sum()==1,"Harvest duplicated/lost aged specimen");
            for(int i=1;i<drops.size();i++)h.assertTrue(HerbAge.years(drops.get(i))==0&&!drops.get(i).getOrCreateTag().contains(HerbAge.ASH_USES),"Propagule inherited years or ash uses");
            h.assertTrue(drops.get(0).getOrCreateTag().getInt(HerbAge.ASH_USES)==3,"Harvest reset ash limit");sawExtra|=drops.size()>1;
        }
        h.assertTrue(sawExtra,"Fixture never exercised propagules");h.succeed();
    }

    @GameTest(template="empty")
    public static void replantInRemovalTickRetainsAgeAndAshLimit(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var level=h.getLevel();var plant=(SpiritPlantBlock)SpiritPlantRegistry.GALE_GRASS.get();
        level.setBlockAndUpdate(pos.below(),Blocks.DIRT.defaultBlockState());level.setBlockAndUpdate(pos,plant.defaultBlockState());
        var specimen=new ItemStack(plant.asItem());HerbAge.setYears(specimen,2500);specimen.getOrCreateTag().putInt(HerbAge.ASH_USES,3);
        var data=HerbCultivationData.get(level);data.plant(level,pos,plant.definition(),specimen);
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,plant.defaultBlockState());plant.setPlacedBy(level,pos,plant.defaultBlockState(),null,specimen);
        h.runAfterDelay(2,()-> {
            h.assertTrue(data.years(pos)==2500&&data.ashUses(pos)==3&&!data.nourish(pos),"Delayed cleanup erased replant or reset ash limit");
            var loaded=HerbCultivationData.load(data.save(new CompoundTag()));
            h.assertTrue(loaded.years(pos)==2500&&loaded.ashUses(pos)==3,"Reload lost medicinal age or ash limit");h.succeed();
        });
    }
}
