package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.plants.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import java.lang.reflect.*;
import java.util.*;

/** Pre-flight equality against the previous live-height-accessor algorithm; no terrain requests. */
@Mod.EventBusSubscriber(modid="wuxingxiantu",value=Dist.CLIENT)
public final class RegionProjectionQa {
    private static final boolean ON=Boolean.getBoolean("wuxingxiantu.flightProjectionCheck");
    private static final List<Long> POINTS=List.of(ConnectedBiomeRegion.pack(8,-24),ConnectedBiomeRegion.pack(-33,65),
            ConnectedBiomeRegion.pack(8000,8000),ConnectedBiomeRegion.pack(-8000,8000),ConnectedBiomeRegion.pack(8000,-8000),
            ConnectedBiomeRegion.pack(-8000,-8000),ConnectedBiomeRegion.pack(2000,2000),ConnectedBiomeRegion.pack(-2000,-2000),
            ConnectedBiomeRegion.pack(31,31),ConnectedBiomeRegion.pack(-31,-31),ConnectedBiomeRegion.pack(20000,4000),
            ConnectedBiomeRegion.pack(-20000,4000),ConnectedBiomeRegion.pack(4000,20000),ConnectedBiomeRegion.pack(4000,-20000),
            ConnectedBiomeRegion.pack(300,700),ConnectedBiomeRegion.pack(-300,-700));
    private static ServerLevel level;private static SpiritPlantRegions state;private static Method sample;
    private static final Map<Long,String> EXPECTED=new LinkedHashMap<>();private static int cursor,ticks;private static long maxPollNanos;private static volatile boolean verified;
    public static void start(ServerLevel l)throws Exception{
        if(!ON)return;level=l;state=new SpiritPlantRegions();sample=SpiritPlantRegions.class.getDeclaredMethod("surfaceBiome",ServerLevel.class,long.class);sample.setAccessible(true);
        var source=l.getChunkSource();var generator=source.getGenerator();var random=source.randomState();
        var biomes=new net.minecraft.world.level.biome.BiomeManager((x,y,z)->generator.getBiomeSource().getNoiseBiome(x,y,z,random.sampler()),net.minecraft.world.level.biome.BiomeManager.obfuscateSeed(l.getSeed()));
        int before=source.getLoadedChunksCount();
        for(long p:POINTS){int x=ConnectedBiomeRegion.x(p),z=ConnectedBiomeRegion.z(p),height=generator.getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,l,random);EXPECTED.put(p,biomes.getBiome(new BlockPos(x,height,z)).unwrapKey().orElseThrow().location().toString());}
        if(source.getLoadedChunksCount()!=before)throw new AssertionError("Reference projection requested terrain chunks");
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event)throws Exception{
        if(!ON||level==null||event.level!=level||event.phase!=TickEvent.Phase.END||complete())return;
        if(++ticks>400)throw new AssertionError("Deferred biome sampling starved");
        while(cursor<POINTS.size()){
            long cell=POINTS.get(cursor),start=System.nanoTime();String value=(String)sample.invoke(state,level,cell);maxPollNanos=Math.max(maxPollNanos,System.nanoTime()-start);
            if(value==null)return;
            if(!value.equals(EXPECTED.get(cell)))throw new AssertionError("Deferred surface biome differs from original at "+cell);
            cursor++;
        }
        var flag=SpiritPlantRegions.class.getDeclaredField("synchronousSamples");flag.setAccessible(true);if(flag.getBoolean(state))throw new AssertionError("Normal noise generator did not use the deferred path");
        var field=SpiritPlantRegions.class.getDeclaredField("surfaceProjection");field.setAccessible(true);Object projection=field.get(state);var shapeMethod=projection.getClass().getDeclaredMethod("shape");shapeMethod.setAccessible(true);var shape=(LevelHeightAccessor)shapeMethod.invoke(projection);
        if(shape instanceof Level||shape.getHeight()!=level.getHeight()||shape.getMinBuildHeight()!=level.getMinBuildHeight())throw new AssertionError("Projection retains a live world or changed height bounds");
        var source=level.getChunkSource();for(long p:POINTS){int x=ConnectedBiomeRegion.x(p),z=ConnectedBiomeRegion.z(p);if(source.getGenerator().getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,shape,source.randomState())!=source.getGenerator().getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,level,source.randomState()))throw new AssertionError("Snapshot height differs from live bounds");}
        verified=true;
    }
    public static boolean complete(){return !ON||verified;}
    public static Map<String,Object> result(){return Map.of("enabled",ON,"complete",complete(),"equalBiomeAndHeightColumns",cursor,"ticks",ticks,"maxPollWallMs",maxPollNanos/1e6,"scope","Pre-flight real NoiseBasedChunkGenerator and production private surfaceBiome; original live accessor versus immutable shape and deferred results. No chunk generation or GPU readback.");}
    private RegionProjectionQa(){}
}
