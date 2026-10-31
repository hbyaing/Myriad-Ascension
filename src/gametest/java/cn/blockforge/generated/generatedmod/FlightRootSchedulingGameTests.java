package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.fixtures.GameTestPlayer;
import cn.blockforge.generated.generatedmod.yujian.*;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder("wuxingxiantu_entry")
@PrefixGameTestTemplate(false)
public final class FlightRootSchedulingGameTests {
    private static final TicketType<UUID> QUEUED=TicketType.create("flight_schedule_qa",Comparator.<UUID>naturalOrder());

    private static ControlledSwordEntity mount(GameTestHelper h,GameTestPlayer player){
        var base=h.absolutePos(new BlockPos(8,0,8));
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)h.getLevel().getChunk((base.getX()>>4)+x,(base.getZ()>>4)+z);
        player.setPos(base.getX(),h.getLevel().getMaxBuildHeight()+16,base.getZ());player.join();
        var data=GeneratedMod.data(player);data.wuxingxiantu$setRealm(99);data.wuxingxiantu$learnArt(YujianController.FLIGHT);
        data.wuxingxiantu$setArtMastery(YujianController.FLIGHT,100);data.wuxingxiantu$setMaxMana(100000);data.wuxingxiantu$setAura(100000);
        player.getInventory().setItem(0,new ItemStack(WuXingItems.PURE_YANG_SWORD.get()));
        YujianController.cast(player,YujianController.FLIGHT,100);
        var root=h.getLevel().getEntitiesOfClass(ControlledSwordEntity.class,player.getBoundingBox().inflate(8),e->e.flightRoot()&&player.getUUID().equals(e.ownerId)).get(0);
        for(int i=0;i<14;i++)YujianController.tickFlightEntity(root,player);
        h.assertTrue(player.getVehicle()==root&&root.isAddedToWorld(),"Actual flight carrier did not mount/join");
        root.positionRider(player);return root;
    }
    private static EntityTickList ticking(ServerLevel level)throws ReflectiveOperationException{
        var field=ServerLevel.class.getDeclaredField("entityTickList");field.setAccessible(true);return (EntityTickList)field.get(level);
    }
    private static Visibility effective(ControlledSwordEntity root,Visibility section)throws ReflectiveOperationException{
        var method=PersistentEntitySectionManager.class.getDeclaredMethod("getEffectiveStatus",EntityAccess.class,Visibility.class);
        method.setAccessible(true);return (Visibility)method.invoke(null,root,section);
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void skippedCarrierGetsOneRealTickAndItsPassenger(GameTestHelper h)throws Exception{
        try(var player=new GameTestPlayer(h.getLevel())){
            try{
                var root=mount(h,player);var level=h.getLevel();
                h.assertTrue(root.isAlwaysTicking()&&effective(root,Visibility.TRACKED)==Visibility.TICKING
                        &&effective(root,Visibility.HIDDEN)==Visibility.TICKING,"A mounted owner lost the real manager's ticking exception");
                // Simulate an engine scheduling miss without manually invoking
                // root.tick or changing the world clock/physics implementation.
                ticking(level).remove(root);
                h.assertTrue(!ticking(level).contains(root),"Fixture remained in the ordinary entity tick list");
                int before=root.tickCount,riderBefore=player.tickCount,loaded=level.getChunkSource().getLoadedChunksCount();
                FlightRootTicking.tickMissing(level);
                h.assertTrue(root.tickCount==before+1&&player.tickCount==riderBefore+1,"Supplement did not run vanilla carrier and passenger ticks once");
                FlightRootTicking.tickMissing(level);
                h.assertTrue(root.tickCount==before+1&&player.tickCount==riderBefore+1,"Repeated same-world-tick supplement double-ticked carrier/passenger");
                h.assertTrue(level.getChunkSource().getLoadedChunksCount()==loaded,"Supplement loaded additional terrain");
            }finally{YujianController.stop(player);}
        }
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void normallyScheduledCarrierIsNeverSupplementedAgain(GameTestHelper h){
        try(var player=new GameTestPlayer(h.getLevel())){
            try{
                var root=mount(h,player);var level=h.getLevel();
                level.guardEntityTick(level::tickNonPassenger,root);
                int before=root.tickCount,riderBefore=player.tickCount;
                FlightRootTicking.tickMissing(level);
                h.assertTrue(root.tickCount==before&&player.tickCount==riderBefore,"Normal carrier/passenger ticks were repeated at level END");
            }finally{YujianController.stop(player);}
        }
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void supplementExcludesWrongOwnerOrdinaryAndUnriddenSwords(GameTestHelper h)throws Exception{
        try(var player=new GameTestPlayer(h.getLevel())){
            try{
                var root=mount(h,player);int before=root.tickCount;UUID owner=root.ownerId;
                root.ownerId=UUID.randomUUID();FlightRootTicking.tickMissing(h.getLevel());
                h.assertTrue(root.tickCount==before&&!root.isAlwaysTicking(),"Wrong-owner carrier received ticking authority");
                root.ownerId=owner;root.flightRoot(false);FlightRootTicking.tickMissing(h.getLevel());
                h.assertTrue(root.tickCount==before&&!root.isAlwaysTicking()
                        &&effective(root,Visibility.TRACKED)==Visibility.TRACKED,"Ordinary sword received a flight ticking exception");
                root.flightRoot(true);player.stopRiding();FlightRootTicking.tickMissing(h.getLevel());
                h.assertTrue(root.tickCount==before&&!root.isAlwaysTicking(),"Unridden flight root received supplemental ticks");
            }finally{YujianController.stop(player);}
        }
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void supplementalMovementStillWaitsAtQueuedTerrain(GameTestHelper h)throws Exception{
        var level=h.getLevel();var cache=level.getChunkSource();ChunkPos queued=null;var ticket=UUID.randomUUID();
        try(var player=new GameTestPlayer(level)){
            try{
                var root=mount(h,player);var start=root.chunkPosition();
                for(int x=start.x;x<start.x+256;x++)if(cache.getChunkNow(x,start.z)!=null&&cache.getChunkNow(x+1,start.z)==null){queued=new ChunkPos(x+1,start.z);break;}
                h.assertTrue(queued!=null,"No ready/unready boundary for scheduling fixture");
                root.setPos(queued.getMinBlockX()-3.25,level.getMaxBuildHeight()+16,start.getMiddleBlockZ());root.setYRot(-90);root.positionRider(player);
                cache.addRegionTicket(QUEUED,queued,0,ticket);
                var update=cache.getClass().getDeclaredMethod("runDistanceManagerUpdates");update.setAccessible(true);update.invoke(cache);
                h.assertTrue(cache.getChunkNow(queued.x,queued.z)==null,"Fixture did not retain queued terrain");
                ticking(level).remove(root);int before=root.tickCount,impacts=root.impact();
                root.setDeltaMovement(new Vec3(8,0,0));
                YujianController.handleInput(player,false,false,true,false,false,false,false,false,true,false,1,0,-90,0);
                FlightRootTicking.tickMissing(level);
                h.assertTrue(root.tickCount==before+1&&player.getVehicle()==root,"Queued border skipped actual supplemental simulation");
                h.assertTrue(cache.getChunkNow(queued.x,queued.z)==null,"Supplement synchronously completed queued terrain");
                h.assertTrue(root.getBoundingBox().maxX+1<queued.getMinBlockX()+.01&&root.impact()==impacts,"Supplement bypassed loaded sweep or treated unready terrain as a wall");
            }finally{YujianController.stop(player);if(queued!=null)cache.removeRegionTicket(QUEUED,queued,0,ticket);}
        }
        h.succeed();
    }
}
