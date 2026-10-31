package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.fixtures.GameTestPlayer;
import cn.blockforge.generated.generatedmod.yujian.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("wuxingxiantu_entry")
@PrefixGameTestTemplate(false)
public final class FlightTerrainLoadingGameTests {
    private static final TicketType<UUID> TICKET=TicketType.create("sword_loading_qa",Comparator.<UUID>naturalOrder());
    private static void queue(net.minecraft.server.level.ServerChunkCache cache,ChunkPos chunk,UUID owner) {
        cache.addRegionTicket(TICKET,chunk,0,owner);
        try {
            var update=cache.getClass().getDeclaredMethod("runDistanceManagerUpdates");
            update.setAccessible(true);update.invoke(cache);
        } catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }

    @GameTest(template="empty",timeoutTicks=400)
    public static void queuedFullChunkNeverCountsAsReadyAndFlightResumes(GameTestHelper h) throws Exception {
        var level=h.getLevel();var cache=level.getChunkSource();
        var origin=h.absolutePos(new BlockPos(8192,0,8192));
        var chunk=new ChunkPos(origin);var owner=UUID.randomUUID();
        var player=new GameTestPlayer(level);
        var saved=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        player.cleanupAfter(h,()->{saved.forEach(level::setBlockAndUpdate);cache.removeRegionTicket(TICKET,chunk,0,owner);});
        h.assertTrue(cache.getChunkNow(chunk.x,chunk.z)==null,"Fixture chunk was already available");
        // getChunkFuture itself managed-blocks on the server thread in 1.20.1.
        // Advance tickets without polling the executor so the FULL request stays pending.
        queue(cache,chunk,owner);
        h.assertTrue(level.hasChunk(chunk.x,chunk.z),"Fixture did not enter requested FULL state");
        h.assertTrue(cache.getChunkNow(chunk.x,chunk.z)==null,"Fixture did not exercise pending generation");
        double y=level.getMaxBuildHeight()-8;
        player.setPos(chunk.getMinBlockX()+4,y,chunk.getMinBlockZ()+8);
        AABB box=player.getBoundingBox();Vec3 requested=new Vec3(3,0,0);
        h.assertTrue(!FlightCollision.loaded(player,box),"Requested FULL holder was mistaken for usable terrain");
        h.assertTrue(!FlightCollision.clear(player,box,false),"Collision read pending terrain");
        for(boolean phasing:new boolean[]{false,true}){
            var sweep=FlightCollision.sweep(player,box,requested,phasing);
            h.assertTrue(sweep.blocked()&&!sweep.loaded()&&sweep.movement().equals(Vec3.ZERO),"Flight entered pending terrain");
        }
        h.assertTrue(FlightCollision.predict(player,box,requested)==FlightCollision.Prediction.UNLOADED,"Prediction read pending terrain");
        h.assertTrue(FlightCollision.clipLoaded(player,player,player.position(),player.position().add(requested)).isEmpty(),"Sword-tip ray waited for terrain");
        h.assertTrue(cache.getChunkNow(chunk.x,chunk.z)==null,"A flight query synchronously completed the load");
        h.assertTrue(!cn.blockforge.generated.generatedmod.flight.FlightTerrainStreaming.available(player,box),"Realm flight accepted queued terrain");
        h.assertTrue(cn.blockforge.generated.generatedmod.flight.FlightTerrainStreaming.limit(player,box,requested).equals(Vec3.ZERO),"Realm flight entered queued terrain");
        var data=GeneratedMod.data(player);data.wuxingxiantu$setRealm(7);data.wuxingxiantu$setMaxMana(10000);data.wuxingxiantu$setAura(10000);
        player.getAbilities().mayfly=true;player.getAbilities().flying=true;
        cn.blockforge.generated.generatedmod.realmflight.RealmFlightControl.input(player,new Vec3(1,0,0),true);
        cn.blockforge.generated.generatedmod.realmflight.RealmFlightControl.tick(player);
        h.assertTrue(cache.getChunkNow(chunk.x,chunk.z)==null,"Realm prediction synchronously waited for generation");
        h.assertTrue(player.getDeltaMovement().equals(Vec3.ZERO),"Realm flight failed to clip at queued terrain");
        cn.blockforge.generated.generatedmod.realmflight.RealmFlightControl.release(player);
        h.startSequence().thenWaitUntil(()->h.assertTrue(cache.getChunkNow(chunk.x,chunk.z)!=null,"Waiting for asynchronous FULL readiness"))
            .thenExecute(()->{
                var resumed=FlightCollision.sweep(player,box,requested,false);
                h.assertTrue(resumed.loaded()&&!resumed.blocked()&&resumed.movement().equals(requested),"Flight remained stuck after terrain became available");
                var ray=FlightCollision.clipLoaded(player,player,player.position(),player.position().add(requested));
                h.assertTrue(ray.isPresent()&&ray.get().getType()==HitResult.Type.MISS,"Ready sky ray did not resume");
                var wall=BlockPos.containing(player.position().add(2,0,0));
                saved.put(wall,level.getBlockState(wall));
                level.setBlockAndUpdate(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                var blocked=FlightCollision.sweep(player,box,requested,false);
                h.assertTrue(blocked.loaded()&&blocked.blocked()&&blocked.movement().x<2,"Ready terrain lost its wall collision");
                h.assertTrue(FlightCollision.sweep(player,box,requested,true).movement().equals(requested),"Phasing stopped passing ordinary walls");
                h.assertTrue(FlightCollision.clipLoaded(player,player,player.position(),player.position().add(requested)).orElseThrow().getType()==HitResult.Type.BLOCK,"Ready sword-tip ray missed wall");
            }).thenSucceed();
    }

    @GameTest(template="empty",timeoutTicks=400)
    public static void mountedSwordWaitsAtQueuedBorderWithoutImpactAndResumes(GameTestHelper h) {
        var level=h.getLevel();var cache=level.getChunkSource();
        var chunk=new ChunkPos(h.absolutePos(new BlockPos(16384,0,16384)));
        final ChunkPos[] ahead={null};var owner=UUID.randomUUID();
        var player=new GameTestPlayer(level);
        player.cleanupAfter(h,()->{
            YujianController.stop(player);
            cache.removeRegionTicket(TICKET,chunk,2,owner);if(ahead[0]!=null)cache.removeRegionTicket(TICKET,ahead[0],0,owner);
        });
        cache.addRegionTicket(TICKET,chunk,2,owner);
        final ControlledSwordEntity[] root={null};final int[] impacts={0};
        Runnable input=()->YujianController.handleInput(player,false,false,true,false,false,false,false,false,true,false,1,0,-90,0);
        h.onEachTick(()->{if(root[0]!=null&&!root[0].isRemoved())input.run();});
        h.startSequence().thenWaitUntil(()->h.assertTrue(cache.getChunkNow(chunk.x,chunk.z)!=null
                &&level.isPositionEntityTicking(chunk.getWorldPosition()),"Waiting for ticking starting terrain"))
            .thenExecute(()->{
                player.setPos(chunk.getMinBlockX()+12.75,level.getMaxBuildHeight()-8,chunk.getMinBlockZ()+8);
                player.setYRot(-90);player.join();
                var data=GeneratedMod.data(player);data.wuxingxiantu$setRealm(5);data.wuxingxiantu$learnArt(YujianController.FLIGHT);data.wuxingxiantu$setAura(10000);
                data.wuxingxiantu$addArtMastery(YujianController.FLIGHT,100);
                player.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(WuXingItems.DARK_IRON_SWORD.get()));
                h.assertTrue(YujianController.canCast(player,YujianController.FLIGHT),"Summon denied: "+YujianController.flightDenialReason(player));
                YujianController.cast(player,YujianController.FLIGHT,0);
                for(int i=0;i<12;i++){
                    var swords=level.getEntitiesOfClass(ControlledSwordEntity.class,player.getBoundingBox().inflate(10),s->s.flightRoot()&&player.getUUID().equals(s.ownerId));
                    swords.forEach(s->YujianController.tickFlightEntity(s,player));
                }
                h.assertTrue(player.getVehicle() instanceof ControlledSwordEntity,"Real sword summon did not mount");
                root[0]=(ControlledSwordEntity)player.getVehicle();root[0].positionRider(player);
                for(int x=chunk.x;x<chunk.x+40;x++)if(cache.getChunkNow(x,chunk.z)!=null&&cache.getChunkNow(x+1,chunk.z)==null){
                    ahead[0]=new ChunkPos(x+1,chunk.z);break;
                }
                h.assertTrue(ahead[0]!=null,"No ready/queued boundary for mounted fixture");
                root[0].setPos(ahead[0].getMinBlockX()-3.25,level.getMaxBuildHeight()-8,chunk.getMinBlockZ()+8);
                root[0].positionRider(player);queue(cache,ahead[0],owner);
                h.assertTrue(level.hasChunk(ahead[0].x,ahead[0].z)&&cache.getChunkNow(ahead[0].x,ahead[0].z)==null,"Ahead chunk must be queued, not ready");
                impacts[0]=root[0].impact();input.run();root[0].setDeltaMovement(new Vec3(8,0,0));
                YujianController.tickFlightEntity(root[0],player);root[0].positionRider(player);
                h.assertTrue(cache.getChunkNow(ahead[0].x,ahead[0].z)==null,"Mounted sword synchronously loaded the queued chunk");
                h.assertTrue(root[0].impact()==impacts[0]&&root[0].advancedPhase()==YujianPhase.JIYU,"Loading triggered a wall impact or phase downgrade");
                h.assertTrue(root[0].getBoundingBox().maxX+1<(ahead[0].getMinBlockX()+.01),"Sword crossed the unready collision halo");
            }).thenWaitUntil(()->h.assertTrue(cache.getChunkNow(ahead[0].x,ahead[0].z)!=null,"Waiting for forward terrain"))
            .thenExecute(()->{
                var before=root[0].position();input.run();YujianController.tickFlightEntity(root[0],player);root[0].positionRider(player);
                h.assertTrue(root[0].position().x>before.x&&player.getVehicle()==root[0],"Mounted sword did not resume when terrain became ready");
                h.assertTrue(root[0].impact()==impacts[0]&&root[0].advancedPhase()==YujianPhase.JIYU,"Resumed flight retained a false collision penalty");
            }).thenSucceed();
    }
}
