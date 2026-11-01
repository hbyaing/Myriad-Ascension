package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.fixtures.GameTestPlayer;
import cn.blockforge.generated.generatedmod.yujian.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Real FULL chunks and controller steps with deliberately limited supply.
 * This is a deterministic loading-supply fixture, not a natural cold-route benchmark. */
@GameTestHolder("wuxingxiantu")
@PrefixGameTestTemplate(false)
public final class FlightSingleChunkRecoveryGameTests {
    private static final TicketType<UUID> TICKET=TicketType.create("flight_single_full_fixture",Comparator.<UUID>naturalOrder());
    private FlightSingleChunkRecoveryGameTests(){}

    /** Call only on the server thread; advance once per real server tick. No
     * real player registration, view distance, readiness or global budget is modified. */
    public static Probe start(ServerLevel level,ChunkPos location){return start(level,location,level.getSharedSpawnPos());}
    public static Probe start(ServerLevel level,ChunkPos location,BlockPos loadedMountPosition){return new Probe(level,location,loadedMountPosition);}

    public static final class Probe implements AutoCloseable {
        private final ServerLevel level;
        private final ServerChunkCache cache;
        private final EntityTickList entityTicks;
        private final ChunkPos base,ahead,next;
        private final BlockPos loadedMountPosition;
        private final UUID ticketOwner=UUID.randomUUID();
        private final GameTestPlayer player;
        private final List<Map<String,Object>> rows=new ArrayList<>();
        private ControlledSwordEntity sword;
        private Object rider;
        private FlightTerrainGovernor governor;
        private String phase="WARMUP";
        private int ticks,stable,recoveryTicks,initialImpact,initialRootTicks,tickListRemovals;
        private long lastWorldTick=Long.MIN_VALUE;
        private double stoppedX;
        private boolean closed,done;
        private CompletableFuture<?> requested;

        private Probe(ServerLevel level,ChunkPos location,BlockPos loadedMountPosition){
            this.level=level;cache=level.getChunkSource();entityTicks=tickList(level);base=location;ahead=new ChunkPos(base.x+1,base.z);next=new ChunkPos(base.x+2,base.z);
            this.loadedMountPosition=loadedMountPosition.immutable();
            require(level.getServer().isSameThread(),"Fixture started off server thread");
            require(cache.getChunkNow(ahead.x,ahead.z)==null&&cache.getChunkNow(next.x,next.z)==null,"Fixture forward chunks were already FULL");
            player=new GameTestPlayer(level);request(base);
        }
        private void request(ChunkPos pos){
            cache.addRegionTicket(TICKET,pos,0,ticketOwner);
            // Same public thread-safe path as production: never managedBlock
            // this test/controller tick while the requested FULL is pending.
            requested=CompletableFuture.supplyAsync(()->cache.getChunkFuture(pos.x,pos.z,ChunkStatus.FULL,true)).thenCompose(f->f);
        }
        private static Field field(Class<?> type,String name){try{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
        private static EntityTickList tickList(ServerLevel level){
            EntityTickList result=null;
            try{
                for(Class<?> type=level.getClass();type!=null;type=type.getSuperclass())for(Field f:type.getDeclaredFields())if(f.getType()==EntityTickList.class){
                    require(result==null,"Multiple server EntityTickList fields");f.setAccessible(true);result=(EntityTickList)f.get(level);
                }
            }catch(IllegalAccessException e){throw new IllegalStateException(e);}
            require(result!=null,"No server EntityTickList field");return result;
        }
        private void isolateManualRoot(){
            // Mounted flight roots are isAlwaysTicking, even in FULL-only
            // chunks. Section callbacks can therefore re-add this entity after
            // a manual move. Remove only the fixture root, never other entities.
            if(sword==null)return;
            if(entityTicks.contains(sword))tickListRemovals++;
            entityTicks.remove(sword);
            require(!entityTicks.contains(sword),"Fixture root remains naturally ticking");
        }
        @SuppressWarnings("unchecked") private void installOwnerHold(){
            try{
                Class<?> stateClass=Class.forName(FlightChunkStreaming.class.getName()+"$LevelState"),riderClass=Class.forName(FlightChunkStreaming.class.getName()+"$Rider");
                Map<ServerLevel,Object> levels=(Map<ServerLevel,Object>)field(FlightChunkStreaming.class,"LEVELS").get(null);
                Object state=levels.get(level);
                if(state==null){Constructor<?> c=stateClass.getDeclaredConstructor(ServerLevel.class);c.setAccessible(true);state=c.newInstance(level);levels.put(level,state);}
                Constructor<?> c=riderClass.getDeclaredConstructor(UUID.class,stateClass);c.setAccessible(true);rider=c.newInstance(player.getUUID(),state);
                Map<UUID,Object> riders=(Map<UUID,Object>)field(FlightChunkStreaming.class,"RIDERS").get(null);
                require(!riders.containsKey(player.getUUID()),"Fixture UUID already owns streaming state");riders.put(player.getUUID(),rider);holdOwner();
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        @SuppressWarnings("unchecked") private void holdOwner(){
            try{
                // The normal lifecycle reaps unregistered owners at level END.
                // Re-admit this empty, held fixture state before each manual
                // update instead of registering a player/view-distance source.
                Map<UUID,Object> riders=(Map<UUID,Object>)field(FlightChunkStreaming.class,"RIDERS").get(null);
                Object current=riders.get(player.getUUID());require(current==null||current==rider,"Fixture owner acquired unexpected streaming state");
                if(current==null)riders.put(player.getUUID(),rider);
                // Hold only this rider's admission budget BEFORE its first real
                // update. A FULL ticket itself can promote terrain, so delaying
                // only a future would not make this a single-FULL experiment.
                field(rider.getClass(),"lastIssueTick").setLong(rider,level.getServer().getTickCount());
                field(rider.getClass(),"issued").setInt(rider,FlightChunkStreaming.MAX_NEW_RIDER_TICKETS_PER_TICK);
                field(rider.getClass(),"updated").setLong(rider,level.getGameTime());
            }catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
        @SuppressWarnings("unchecked") private void mount(){
            require(!level.players().contains(player)&&level.getServer().getPlayerList().getPlayer(player.getUUID())==null,"Fixture fake player was registered");
            require(cache.getChunkNow(loadedMountPosition.getX()>>4,loadedMountPosition.getZ()>>4)!=null,"Mount fixture is not in existing loaded terrain");
            player.setPos(loadedMountPosition.getX()+.5,level.getMaxBuildHeight()+16,loadedMountPosition.getZ()+.5);player.setYRot(-90);
            var data=GeneratedMod.data(player);data.wuxingxiantu$setRealm(99);data.wuxingxiantu$learnArt(YujianController.FLIGHT);data.wuxingxiantu$setArtMastery(YujianController.FLIGHT,100);data.wuxingxiantu$setMaxMana(1_000_000);data.wuxingxiantu$setAura(1_000_000);
            player.getInventory().setItem(0,new ItemStack(WuXingItems.PURE_YANG_SWORD.get()));
            installOwnerHold(); // Must precede cast/mount and the first FLYING update.
            require(YujianController.canCast(player,YujianController.FLIGHT),"Fixture summon denied");YujianController.cast(player,YujianController.FLIGHT,100);
            try{
                Map<UUID,Object> rides=(Map<UUID,Object>)field(YujianController.class,"RIDES").get(null);
                require(rides.containsKey(player.getUUID()),"Real cast did not install fixture ride state");
            }catch(IllegalAccessException e){throw new IllegalStateException(e);}
            for(int i=0;i<24&&player.getVehicle()==null;i++){
                var roots=level.getEntitiesOfClass(ControlledSwordEntity.class,player.getBoundingBox().inflate(20),s->s.flightRoot()&&player.getUUID().equals(s.ownerId));
                require(roots.size()==1,"Fixture summon did not create one real root");sword=roots.get(0);holdOwner();YujianController.tickFlightEntity(sword,player);isolateManualRoot();
            }
            require(player.getVehicle()==sword&&sword!=null,"Fixture did not mount");sword.positionRider(player);
            try{
                Map<UUID,Object> rides=(Map<UUID,Object>)field(YujianController.class,"RIDES").get(null);Object ride=rides.get(player.getUUID());
                governor=(FlightTerrainGovernor)field(ride.getClass(),"terrainGovernor").get(ride);
                var huahong=(HuahongFlight)field(ride.getClass(),"huahong").get(ride);
                // Seed an already-funded active form through its real lifecycle;
                // entry look-ahead is intentionally impossible at this cold edge.
                for(int i=0;i<HuahongFlight.STABLE_TICKS+HuahongFlight.GATHER_TICKS;i++)huahong.tick(true,true,true,true);
                require(huahong.stage()==2,"Fixture lifecycle did not reach Huahong");sword.huahong(huahong.stage(),huahong.strength());
            }catch(IllegalAccessException e){throw new IllegalStateException(e);}
            // FULL readiness does not imply that a newly remote chunk has
            // finished publishing its visible entity index. Mount in the
            // existing fixture area first, then move only these test actors.
            sword.setPos(base.getMinBlockX()+8,level.getMaxBuildHeight()+16,base.getMinBlockZ()+8);
            sword.setYRot(-90);sword.setXRot(0);sword.setDeltaMovement(new Vec3(7,0,0));sword.flightSpeed(7);sword.positionRider(player);isolateManualRoot();initialRootTicks=sword.tickCount;initialImpact=sword.impact();phase="STOPPING";
        }
        private void controllerStep(){
            require(sword.tickCount==initialRootTicks,"Fixture root received a natural entity tick between manual controller steps");
            isolateManualRoot();
            require(!level.isPositionEntityTicking(sword.blockPosition()),"Fixture root became naturally entity-ticking; manual steps would duplicate it");
            holdOwner();player.tickCount++;
            YujianController.handleInput(player,false,false,true,false,false,false,false,false,true,false,1,0,-90,0);
            YujianController.tickFlightEntity(sword,player);sword.positionRider(player);isolateManualRoot();
            require(!sword.isRemoved()&&player.getVehicle()==sword,"Fixture ride ended");
            require(sword.huahongStage()==2&&sword.impact()==initialImpact,"Loading exited Huahong or counted as a wall impact");
            require(cache.getChunkNow(next.x,next.z)==null,"A second forward FULL chunk invalidated the fixture");
            var volume=FlightFrame.riderBounds(sword.position(),sword.getYRot(),sword.getXRot(),sword.roll(),sword.riderOffset()).minmax(player.getBoundingBox()).minmax(sword.getBoundingBox());
            double border=cache.getChunkNow(ahead.x,ahead.z)==null?ahead.getMinBlockX():next.getMinBlockX();
            require(volume.maxX+1<=border+.01,"Controller crossed the unknown collision margin");
            var streaming=FlightChunkStreaming.diagnostics(player);
            require(((Number)streaming.get("tickets")).intValue()==0&&((Number)streaming.get("outstanding")).intValue()==0,"Owner admission hold leaked FULL requests");
            rows.add(Map.ofEntries(Map.entry("worldTick",level.getGameTime()),Map.entry("phase",phase),Map.entry("positionX",sword.getX()),Map.entry("speed",sword.getDeltaMovement().length()),Map.entry("ceiling",governor.ceiling()),Map.entry("huahongStage",sword.huahongStage()),Map.entry("advancedPhase",sword.advancedPhase().name()),Map.entry("baseFull",cache.getChunkNow(base.x,base.z)!=null),Map.entry("oneAheadFull",cache.getChunkNow(ahead.x,ahead.z)!=null),Map.entry("twoAheadFull",cache.getChunkNow(next.x,next.z)!=null),Map.entry("rootNaturalTicks",sword.tickCount-initialRootTicks),Map.entry("rootInEntityTickList",entityTicks.contains(sword)),Map.entry("tickListRemovals",tickListRemovals)));
        }
        /** Returns true only after both restart and the second safe stop. */
        public boolean advance(){
            require(!closed,"Fixture already closed");long now=level.getGameTime();require(now!=lastWorldTick,"Fixture must advance at most once per real world tick");lastWorldTick=now;
            if(++ticks>1000)throw new AssertionError("Single-FULL fixture timeout: "+phase);
            if(requested.isCompletedExceptionally())throw new AssertionError("Fixture FULL request failed");
            require(cache.getChunkNow(next.x,next.z)==null,"Second forward FULL was loaded independently");
            if(phase.equals("WARMUP")){if(cache.getChunkNow(base.x,base.z)==null)return false;require(cache.getChunkNow(ahead.x,ahead.z)==null,"Warmup promoted ahead FULL");mount();return false;}
            if(phase.equals("WAITING_FULL")&&cache.getChunkNow(ahead.x,ahead.z)!=null){phase="RECOVERING";recoveryTicks=0;}
            controllerStep();double speed=sword.getDeltaMovement().length();
            if(phase.equals("STOPPING")){
                require(cache.getChunkNow(ahead.x,ahead.z)==null,"Ahead FULL completed before the stop");
                stable=speed<1e-5?stable+1:0;
                if(stable>=8){stoppedX=sword.getX();phase="WAITING_FULL";stable=0;request(ahead);}
            }else if(phase.equals("RECOVERING")){
                if(++recoveryTicks==8){require(sword.getX()-stoppedX>1,"One genuine16m FULL chunk did not restart stopped flight within8ticks");phase="RESTOPPING";}
            }else if(phase.equals("RESTOPPING")){
                stable=speed<1e-5?stable+1:0;if(stable>=8){done=true;phase="COMPLETE";return true;}
            }
            return false;
        }
        public Map<String,Object> snapshot(){return Map.of("done",done,"phase",phase,"worldTicks",ticks,"controllerRows",List.copyOf(rows),"scope","Owner-scoped admission hold with real FULL terrain. Only the fixture root is removed from EntityTickList for one manual controller step per world tick; not a natural loading benchmark.");}
        @Override public void close(){if(closed)return;closed=true;try{isolateManualRoot();YujianController.stop(player);}finally{isolateManualRoot();cache.removeRegionTicket(TICKET,base,0,ticketOwner);cache.removeRegionTicket(TICKET,ahead,0,ticketOwner);player.close();}}
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    @GameTest(batch="flight_streaming",template="empty",timeoutTicks=1100)
    public static void oneFullChunkRestartsAnUnregisteredMountedHuahong(GameTestHelper h){
        var probe=start(h.getLevel(),new ChunkPos(h.absolutePos(new BlockPos(524288,0,524288))),h.absolutePos(new BlockPos(4,0,4)));
        probe.player.cleanupAfter(h,probe::close);
        h.onEachTick(()->{if(probe.done)return;try{if(probe.advance()){com.mojang.logging.LogUtils.getLogger().info("Single FULL restart fixture: {}",probe.snapshot());h.succeed();}}catch(Throwable error){probe.close();com.mojang.logging.LogUtils.getLogger().error("Single FULL fixture failed",error);h.fail("Single FULL fixture: "+error);}});
    }
}
