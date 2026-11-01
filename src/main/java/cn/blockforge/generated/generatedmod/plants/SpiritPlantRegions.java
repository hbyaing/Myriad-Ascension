package cn.blockforge.generated.generatedmod.plants;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.*;

/** Persistent quotas over tile-bounded connected surface-biome projections, shared with caves.
 * No terrain chunks are requested by the boundary search. Incomplete regions cannot place plants.
 * All public mutations are synchronized because decoration can run on worldgen worker threads.
 */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpiritPlantRegions extends SavedData {
    private static final String NAME = "wuxingxiantu_spirit_plant_regions_v1";
    private final List<Region> regions = new ArrayList<>();
    private final Map<Long, Region> cells = new HashMap<>();
    private final ArrayDeque<Candidate> pending = new ArrayDeque<>();
    private record CandidateKey(int chunkX,int chunkZ,String species) {}
    private final Set<CandidateKey> pendingKeys = new HashSet<>();
    private final Map<Integer,Region> byId = new HashMap<>();
    private final Map<Long,List<BlockPos>> placementIndex = new HashMap<>();
    private final Map<String,Map<String,WildRarity>> eligibleCache = new HashMap<>();
    private final Set<String> restrictedBiomes = new HashSet<>();
    private Region activeSearch;
    private final ArrayDeque<Region> ready = new ArrayDeque<>();
    private final Set<Integer> readyIds = new HashSet<>();
    private final Map<Long, Integer> wild = new HashMap<>();
    private final Set<Long> cultivated = new HashSet<>();
    private int nextRegion;
    private static final ThreadPoolExecutor HEIGHT_SAMPLES=new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(8),r->{var t=new Thread(r,"xiantu-biome-height");t.setDaemon(true);return t;},
            new ThreadPoolExecutor.AbortPolicy());
    private final DeferredBiomeSampler heightSamples=new DeferredBiomeSampler(HEIGHT_SAMPLES,256);
    private SurfaceProjection surfaceProjection;
    private boolean synchronousSamples;

    private static final class Candidate {
        final BlockPos pos;
        final SpiritPlantFeature.Config config;
        Candidate(BlockPos pos, SpiritPlantFeature.Config config) {
            this.pos = pos.immutable(); this.config = config;
        }
        String species() { return ((SpiritPlantBlock) config.place.state.getBlock()).definition().id; }
        WildRarity rarity() { return ((SpiritPlantBlock) config.place.state.getBlock()).definition().rarity; }
    }

    private static final class Region {
        int id, target;
        long seed;
        long tile;
        long canonical=Long.MAX_VALUE;
        String biome;
        boolean complete, decided;
        final Set<Long> members = new HashSet<>();
        final Set<Long> examined = new HashSet<>();
        final ArrayDeque<Long> frontier = new ArrayDeque<>();
        final List<Candidate> candidates = new ArrayList<>();
        final Map<String, Integer> limits = new HashMap<>();
        final Map<String, Integer> counts = new HashMap<>();
        final List<BlockPos> placements = new ArrayList<>();
    }

    public static SpiritPlantRegions get(ServerLevel level) {
        synchronized (level.getDataStorage()) {
            return level.getDataStorage().computeIfAbsent(SpiritPlantRegions::load, SpiritPlantRegions::new, NAME);
        }
    }

    /** Only decoration calls this. Existing chunks and manual planting never enqueue candidates. */
    public synchronized void offer(BlockPos pos, SpiritPlantFeature.Config config) {
        if (!(config.place.state.getBlock() instanceof SpiritPlantBlock)) return;
        // Bound storage per chunk/species; no patch is placed at this stage.
        Candidate c=new Candidate(pos,config);
        if(pendingKeys.add(key(c))){pending.addLast(c);setDirty();}
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) get(level).advance(level);
    }

    private synchronized void advance(ServerLevel level) {
        long deadline = System.nanoTime() + 2_000_000L;
        Region active = activeSearch;
        // Serial complete-component searches prevent independently allocated regions from later merging.
        if (active == null && !pending.isEmpty()) {
            Candidate first = pending.peekFirst();
            long cell = cell(first.pos);
            active = cells.get(cell);
            if (active == null) {
                String biome=surfaceBiome(level,cell);
                if(biome==null)return;
                active = new Region(); active.id = nextRegion++;
                active.biome = biome;
                active.tile = ConnectedBiomeRegion.tile(cell);
                active.seed = mix(level.getSeed() ^ cell ^ active.biome.hashCode());
                active.frontier.add(cell); active.examined.add(cell);
                regions.add(active); byId.put(active.id,active);activeSearch=active; setDirty();
            }
        }
        if (active != null && !active.complete) {
            Region scanning = active;
            int visited = ConnectedBiomeRegion.step(active.biome,active.members,active.examined,active.frontier,
                    at -> {
                        String biome=surfaceBiome(level,at);
                        if(biome!=null&&biome.equals(scanning.biome)){
                            cells.put(at,scanning);scanning.canonical=Math.min(scanning.canonical,at);
                        }
                        return biome;
                    },128,Math.min(deadline,System.nanoTime()+1_000_000L),active.tile);
            if (visited > 0) setDirty();
            if (active.frontier.isEmpty()) {
                active.complete = true; active.examined.clear(); setDirty();
                active.seed=mix(level.getSeed()^active.canonical^active.biome.hashCode());
                activeSearch=null;
                queueReady(active);
            }
        }
        // Attach candidates only after their XZ cell is assigned; underground shares the surface region.
        int pendingBudget=Math.min(128,pending.size());
        for(int i=0;i<pendingBudget&&System.nanoTime()<deadline;i++) {
            Candidate c = pending.removeFirst(); Region r = cells.get(cell(c.pos));
            if (r == null) {pending.addLast(c);continue;}
            if ((!r.decided || r.limits.containsKey(c.species()))
                    && r.candidates.stream().filter(p -> p.species().equals(c.species())).count() < 64) {
                r.candidates.add(c);if(r.complete)queueReady(r);
            }
            pendingKeys.remove(key(c)); setDirty();
        }
        // Rotate ready regions so unloaded candidates cannot starve another region.
        if (!ready.isEmpty()) {
            Region r = ready.removeFirst();readyIds.remove(r.id);
            if (r.complete) {
                if (!r.decided) { decide(r, level); setDirty(); }
                int placementBudget=32;
                for (Iterator<Candidate> it = r.candidates.iterator(); it.hasNext()&&placementBudget-->0&&System.nanoTime()<deadline;) {
                    Candidate c = it.next();
                    if (!allowed(r, c) || !spaced(c.pos)) { it.remove(); setDirty(); continue; }
                    // Never load chunks for delayed placement. Revalidate habitat after player edits.
                    if (!neighborhoodLoaded(level,c.pos)) continue;
                    if (SpiritPlantFeature.placeRecorded(level, c.pos, c.config,
                            new Random(mix(r.seed ^ c.pos.asLong())).nextInt(8))) {
                        r.placements.add(c.pos);
                        indexPlacement(c.pos);
                        r.counts.merge(c.species(), 1, Integer::sum);
                        wild.put(c.pos.asLong(), r.id);
                        cultivated.remove(c.pos.asLong());
                    }
                    it.remove(); setDirty();
                }
                if(!r.candidates.isEmpty())queueReady(r);
            }
        }
    }

    private void decide(Region r, ServerLevel level) {
        r.decided = true;
        var eligible=eligibleCache.computeIfAbsent(r.biome,biomeId->{
            Map<String,WildRarity> species=new TreeMap<>();
            boolean[] regularSurface={false};
            var biome=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                    .get(new net.minecraft.resources.ResourceLocation(biomeId));
            if(biome!=null)biome.getGenerationSettings().features().forEach(set->set.forEach(placed->{
                var config=placed.value().feature().value().config();
                if(config instanceof SpiritPlantFeature.Config c && c.place.state.getBlock() instanceof SpiritPlantBlock plant){
                    species.put(plant.definition().id,plant.definition().rarity);
                    if(c.rule!=SpiritPlantFeature.Rule.SURFACE_HIGHLAND&&c.rule!=SpiritPlantFeature.Rule.CAVE_FLOOR
                            &&c.rule!=SpiritPlantFeature.Rule.CAVE_WATER)regularSurface[0]=true;
                    if(c.rule==SpiritPlantFeature.Rule.SURFACE_HIGHLAND||c.rule==SpiritPlantFeature.Rule.CAVE_FLOOR
                            ||c.rule==SpiritPlantFeature.Rule.CAVE_WATER)restrictedBiomes.add(biomeId);
                }
            }));
            // Cave features are injected broadly and then filtered at the actual cave point.
            // Their presence must not classify every ordinary surface habitat as restricted.
            if(regularSurface[0])restrictedBiomes.remove(biomeId);
            return species;
        });
        SpiritPlantQuota.Plan plan=SpiritPlantQuota.choose(r.seed,level.dimension().equals(Level.END),restrictedBiomes.contains(r.biome),eligible);
        r.target=Math.max(r.placements.size(),plan.target());r.limits.clear();r.limits.putAll(plan.limits());
        r.counts.forEach((species,count)->r.limits.merge(species,count,Math::max));
    }

    private boolean allowed(Region r, Candidate c) {
        return r.placements.size() < r.target && r.placements.size() < 3
                && r.counts.getOrDefault(c.species(), 0) < r.limits.getOrDefault(c.species(), 0);
    }

    private boolean spaced(BlockPos pos) {
        int gridX=Math.floorDiv(pos.getX(),48),gridZ=Math.floorDiv(pos.getZ(),48);
        for(int x=gridX-1;x<=gridX+1;x++)for(int z=gridZ-1;z<=gridZ+1;z++)
        for (BlockPos other : placementIndex.getOrDefault(pack(x,z),List.of())) {
            long dx = (long)pos.getX()-other.getX(), dz = (long)pos.getZ()-other.getZ();
            if (dx*dx+dz*dz < 48L*48L) return false;
        }
        return true;
    }
    private static CandidateKey key(Candidate c){return new CandidateKey(c.pos.getX()>>4,c.pos.getZ()>>4,c.species());}
    private void queueReady(Region r){if(!r.candidates.isEmpty()&&readyIds.add(r.id))ready.addLast(r);}
    private void indexPlacement(BlockPos p){placementIndex.computeIfAbsent(pack(Math.floorDiv(p.getX(),48),Math.floorDiv(p.getZ(),48)),k->new ArrayList<>()).add(p);}

    private static boolean neighborhoodLoaded(ServerLevel level, BlockPos pos) {
        for(int dx=-4;dx<=4;dx+=4)for(int dz=-4;dz<=4;dz+=4)
            if(!level.hasChunk((pos.getX()+dx)>>4,(pos.getZ()+dz)>>4))return false;
        return true;
    }

    /** Unknown legacy mushrooms are conservatively non-spreading; explicitly planted ones can spread. */
    public synchronized boolean isCultivated(BlockPos pos) { return cultivated.contains(pos.asLong()); }
    public synchronized void markCultivated(BlockPos pos) {
        wild.remove(pos.asLong()); cultivated.add(pos.asLong()); setDirty();
    }
    public synchronized void removed(BlockPos pos) {
        if (wild.remove(pos.asLong()) != null | cultivated.remove(pos.asLong())) setDirty();
        // Historical placements and counts deliberately remain: harvesting never refunds a quota.
    }
    public synchronized boolean spreadWild(ServerLevel level, BlockPos source, BlockPos target) {
        Integer id = wild.get(source.asLong());
        if (id == null) return false;
        Region r = byId.get(id);
        if (r == null || cells.get(cell(target)) != r || !spaced(target)) return false;
        String species = SpiritPlantCatalog.GLOWING_SHROOM.id;
        if (r.placements.size() >= r.target || r.placements.size() >= 3
                || r.counts.getOrDefault(species,0) >= r.limits.getOrDefault(species,0)) return false;
        if (!level.setBlock(target, level.getBlockState(source), 2)) return false;
        r.placements.add(target.immutable()); r.counts.merge(species,1,Integer::sum);
        indexPlacement(target.immutable());
        wild.put(target.asLong(),id); setDirty(); return true;
    }

    private String surfaceBiome(ServerLevel level, long cell) {
        if(surfaceProjection==null){
            var source=level.getChunkSource();var generator=source.getGenerator();var random=source.randomState();
            var shape=net.minecraft.world.level.LevelHeightAccessor.create(level.getMinBuildHeight(),level.getHeight());
            var biomes=new net.minecraft.world.level.biome.BiomeManager(
                    (qx,qy,qz)->generator.getBiomeSource().getNoiseBiome(qx,qy,qz,random.sampler()),
                    net.minecraft.world.level.biome.BiomeManager.obfuscateSeed(level.getSeed()));
            surfaceProjection=new SurfaceProjection(generator,random,shape,biomes);
            // Other generators may rely on thread-affine state. Keep their old path.
            Class<?> biomeSource=generator.getBiomeSource().getClass();
            boolean vanillaSource=biomeSource==net.minecraft.world.level.biome.MultiNoiseBiomeSource.class
                    ||biomeSource==net.minecraft.world.level.biome.TheEndBiomeSource.class
                    ||biomeSource==net.minecraft.world.level.biome.FixedBiomeSource.class
                    ||biomeSource==net.minecraft.world.level.biome.CheckerboardColumnBiomeSource.class;
            synchronousSamples=generator.getClass()!=net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator.class||!vanillaSource;
        }
        if(synchronousSamples)return surfaceProjection.sample(cell,level);
        try{return heightSamples.poll(cell,()->sampleBatch(cell),surfaceProjection::sample);}
        catch(CompletionException failed){
            synchronousSamples=true;
            com.mojang.logging.LogUtils.getLogger().warn("Surface biome background sampling failed; retaining synchronous compatibility",failed.getCause());
            return surfaceProjection.sample(cell,level);
        }
    }
    private List<Long> sampleBatch(long cell){
        var batch=new LinkedHashSet<Long>();batch.add(cell);
        if(activeSearch!=null)for(long at:activeSearch.frontier){if(batch.size()>=8)break;batch.add(at);}
        for(long at:List.copyOf(batch)){
            int x=x(at),z=z(at);
            for(long next:new long[]{pack(x-1,z),pack(x+1,z),pack(x,z-1),pack(x,z+1)}){
                if(batch.size()>=16)break;batch.add(next);
            }
        }
        return List.copyOf(batch);
    }
    /** No ServerLevel, ChunkSource, live heightmap or mutable world random is retained. */
    private record SurfaceProjection(net.minecraft.world.level.chunk.ChunkGenerator generator,
            net.minecraft.world.level.levelgen.RandomState random,net.minecraft.world.level.LevelHeightAccessor shape,
            net.minecraft.world.level.biome.BiomeManager biomes){
        String sample(long cell){return sample(cell,shape);}
        String sample(long cell,net.minecraft.world.level.LevelHeightAccessor heightAccess){
            int height=generator.getBaseHeight(x(cell),z(cell),Heightmap.Types.WORLD_SURFACE_WG,heightAccess,random);
            return biomes.getBiome(new BlockPos(x(cell),height,z(cell))).unwrapKey().orElseThrow().location().toString();
        }
    }
    private static long cell(BlockPos pos) { return pack(pos.getX(),pos.getZ()); }
    private static long pack(int x,int z) { return ((long)x << 32) | (z & 0xffffffffL); }
    private static int x(long p) { return (int)(p >> 32); }
    private static int z(long p) { return (int)p; }
    private static long mix(long x) {
        x = (x ^ (x >>> 30))*0xbf58476d1ce4e5b9L;
        x = (x ^ (x >>> 27))*0x94d049bb133111ebL; return x ^ (x >>> 31);
    }

    private static CompoundTag writeCandidate(Candidate c) {
        CompoundTag tag = new CompoundTag(); tag.putLong("Pos", c.pos.asLong());
        tag.put("Config", SpiritPlantFeature.Config.CODEC.encodeStart(NbtOps.INSTANCE,c.config).result().orElseThrow());
        return tag;
    }
    private static Candidate readCandidate(CompoundTag t) {
        var config = SpiritPlantFeature.Config.CODEC.parse(NbtOps.INSTANCE,t.get("Config")).result();
        return config.filter(c -> c.place.state.getBlock() instanceof SpiritPlantBlock)
                .map(c -> new Candidate(BlockPos.of(t.getLong("Pos")),c)).orElse(null);
    }
    private static ListTag writeCandidates(Collection<Candidate> candidates) {
        ListTag list = new ListTag(); for (Candidate c : candidates) list.add(writeCandidate(c)); return list;
    }
    private static void readCandidates(ListTag list, Collection<Candidate> into) {
        for (int i=0;i<list.size();i++) { Candidate c = readCandidate(list.getCompound(i)); if(c!=null)into.add(c); }
    }
    private static CompoundTag writeCounts(Map<String,Integer> values) {
        CompoundTag t = new CompoundTag(); values.forEach(t::putInt); return t;
    }
    private static void readCounts(CompoundTag t, Map<String,Integer> into) {
        for(String key:t.getAllKeys())into.put(key,t.getInt(key));
    }

    @Override public synchronized CompoundTag save(CompoundTag tag) {
        tag.putInt("Policy",2);tag.putInt("Next",nextRegion); ListTag list = new ListTag();
        for (Region r : regions) {
            CompoundTag t = new CompoundTag(); t.putInt("Id",r.id); t.putLong("Seed",r.seed);
            t.putString("Biome",r.biome); t.putBoolean("Complete",r.complete); t.putBoolean("Decided",r.decided);
            t.putLong("Tile",r.tile);
            t.putInt("Target",r.target); t.putLongArray("Cells",r.members.stream().mapToLong(Long::longValue).toArray());
            t.putLongArray("Examined",r.examined.stream().mapToLong(Long::longValue).toArray());
            t.putLongArray("Frontier",r.frontier.stream().mapToLong(Long::longValue).toArray());
            t.put("Candidates",writeCandidates(r.candidates)); t.put("Limits",writeCounts(r.limits));
            t.put("Counts",writeCounts(r.counts));
            t.putLongArray("Placements",r.placements.stream().mapToLong(BlockPos::asLong).toArray()); list.add(t);
        }
        tag.put("Regions",list); tag.put("Pending",writeCandidates(pending));
        ListTag sources = new ListTag(); wild.forEach((pos,id) -> {
            CompoundTag t = new CompoundTag(); t.putLong("Pos",pos); t.putInt("Region",id); sources.add(t);
        }); tag.put("Wild",sources);
        tag.putLongArray("Cultivated",cultivated.stream().mapToLong(Long::longValue).toArray()); return tag;
    }

    public static SpiritPlantRegions load(CompoundTag tag) {
        SpiritPlantRegions data = new SpiritPlantRegions(); data.nextRegion = tag.getInt("Next");
        ListTag list = tag.getList("Regions",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++) {
            CompoundTag t = list.getCompound(i); Region r = new Region(); r.id=t.getInt("Id"); r.seed=t.getLong("Seed");
            r.biome=t.getString("Biome"); r.complete=t.getBoolean("Complete"); r.decided=t.getBoolean("Decided");
            r.target=t.getInt("Target");
            for(long p:t.getLongArray("Cells")){r.members.add(p);data.cells.put(p,r);r.canonical=Math.min(r.canonical,p);}
            for(long p:t.getLongArray("Examined"))r.examined.add(p);
            for(long p:t.getLongArray("Frontier"))r.frontier.addLast(p);
            readCandidates(t.getList("Candidates",Tag.TAG_COMPOUND),r.candidates);
            readCounts(t.getCompound("Limits"),r.limits); readCounts(t.getCompound("Counts"),r.counts);
            for(long p:t.getLongArray("Placements")){BlockPos pos=BlockPos.of(p);r.placements.add(pos);data.indexPlacement(pos);}
            r.tile=t.getLong("Tile");
            if(tag.getInt("Policy")<2){
                r.decided=false;
                if(!r.complete&&r.placements.isEmpty()){
                    for(long p:r.members)data.cells.remove(p);
                    data.pending.addAll(r.candidates);continue;
                }
                r.complete=true;r.examined.clear();r.frontier.clear();
            }
            data.regions.add(r);data.byId.put(r.id,r);
            if(!r.complete)data.activeSearch=r;else data.queueReady(r);
        }
        readCandidates(tag.getList("Pending",Tag.TAG_COMPOUND),data.pending);
        data.pending.removeIf(c->!data.pendingKeys.add(key(c)));
        if(tag.getInt("Policy")<2)data.setDirty();
        ListTag sources = tag.getList("Wild",Tag.TAG_COMPOUND);
        for(int i=0;i<sources.size();i++){CompoundTag t=sources.getCompound(i);data.wild.put(t.getLong("Pos"),t.getInt("Region"));}
        for(long p:tag.getLongArray("Cultivated"))data.cultivated.add(p); return data;
    }
}
