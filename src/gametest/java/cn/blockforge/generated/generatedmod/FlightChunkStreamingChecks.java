package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.fixtures.GameTestPlayer;
import cn.blockforge.generated.generatedmod.yujian.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("wuxingxiantu")
@PrefixGameTestTemplate(false)
public final class FlightChunkStreamingChecks {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    public static String verify(){
        checks=0;
        for(double speed:new double[]{0,.1,4.4,15,48,120})for(double x:new double[]{.1,15.9,-.1,-16.1,29_999_000})for(float yaw:new float[]{0,45,90,179,-179}){
            var origin=new Vec3(x,300,-16.1);double angle=Math.toRadians(yaw);var velocity=new Vec3(-Math.sin(angle)*speed,0,Math.cos(angle)*speed);
            for(float turn:new float[]{0,90,180}){
                var path=FlightChunkStreaming.preview(origin,velocity,yaw,0,yaw+turn,0,.075,.55,speed,false);
                check(path.size()<=FlightChunkStreaming.MAX_RIDER_TICKETS,"Unbounded route ticket plan");
                check(new HashSet<>(path).size()==path.size(),"Duplicate route destinations");
                var start=new ChunkPos(BlockPos.containing(origin));
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)check(path.contains(new ChunkPos(start.x+dx,start.z+dz)),"Missing current swept-volume neighbour");
                for(var chunk:path)check(Math.hypot(chunk.getMiddleBlockX()-origin.x,chunk.getMiddleBlockZ()-origin.z)<=FlightChunkStreaming.MAX_ROUTE_DISTANCE+48,"Unbounded route distance");
            }
        }
        var straight=FlightChunkStreaming.preview(new Vec3(15.9,300,15.9),new Vec3(0,0,15),0,0,0,0,.075,.55,15,false);
        // A continuous corridor, not the old three 32/64/96-metre points. The
        // full 1m collision margin on both sides must stay represented.
        for(int distance=0;distance<=128;distance+=4){var point=new ChunkPos(BlockPos.containing(15.9,300,15.9+distance));for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)check(straight.contains(new ChunkPos(point.x+dx,point.z+dz)),"Hole in straight high-speed corridor at "+distance);}
        for(double speed:new double[]{.1,1,4.4,15,120})for(double acceleration:new double[]{.03,.14,.18,.55,1}){
            double v=speed,distance=0;for(int tick=0;tick<10000&&v>1e-10;tick++){v*=1-acceleration;distance+=v;}
            double envelope=FlightChunkStreaming.brakingDistance(speed,acceleration);
            check(envelope>=distance+2*speed+1.999999,"Braking/reaction envelope undershoots real geometric deceleration");
            check(Math.abs(FlightChunkStreaming.speedForReadyDistance(envelope,acceleration)-speed)<1e-8,"Ready-distance advisory changed an adequately loaded cruising speed");
            check(FlightChunkStreaming.speedForReadyDistance(envelope*.5,acceleration)<speed,"A short ready prefix failed to advise early slowdown");
        }
        var coast=FlightChunkStreaming.preview(Vec3.ZERO,new Vec3(0,0,15),0,0,0,0,.075,.14,7.5,true);
        var brake=FlightChunkStreaming.preview(Vec3.ZERO,new Vec3(0,0,15),0,0,0,0,.075,.18,0,true);
        check(coast.stream().mapToInt(ChunkPos::getMaxBlockZ).max().orElse(0)>brake.stream().mapToInt(ChunkPos::getMaxBlockZ).max().orElse(0)+32,"Released-forward momentum was mistaken for target-zero braking");
        check(FlightChunkStreaming.preview(Vec3.ZERO,new Vec3(Double.NaN,0,0),0,0,0,0,.075,.55,15,false).isEmpty(),"Nonfinite velocity produced terrain work");
        return "Flight chunk streaming planner checks passed: "+checks;
    }
    public static void main(String[] args){System.out.println(verify());}
    @GameTest(batch="flight_streaming",template="empty") public static void boundedContinuousRoutesAndBrakingEnvelope(GameTestHelper h){com.mojang.logging.LogUtils.getLogger().info(verify());h.succeed();}

    private static ChunkPos coldBoundary(GameTestHelper h){
        var cache=h.getLevel().getChunkSource();var base=new ChunkPos(h.absolutePos(BlockPos.ZERO));
        for(int x=base.x;x<base.x+1024;x++)if(cache.getChunkNow(x,base.z)!=null&&cache.getChunkNow(x+1,base.z)==null)return new ChunkPos(x+1,base.z);
        throw new AssertionError("No genuine ready/cold boundary in fixture");
    }
    private static ControlledSwordEntity mount(GameTestHelper h,GameTestPlayer player){
        var start=h.absolutePos(new BlockPos(4,0,4));player.setPos(start.getX(),h.getLevel().getMaxBuildHeight()+16,start.getZ());player.join();
        var data=GeneratedMod.data(player);data.wuxingxiantu$setRealm(99);data.wuxingxiantu$learnArt(YujianController.FLIGHT);data.wuxingxiantu$setArtMastery(YujianController.FLIGHT,100);data.wuxingxiantu$setMaxMana(100000);data.wuxingxiantu$setAura(100000);
        player.getInventory().setItem(0,new ItemStack(WuXingItems.PURE_YANG_SWORD.get()));YujianController.cast(player,YujianController.FLIGHT,100);
        var root=h.getLevel().getEntitiesOfClass(ControlledSwordEntity.class,player.getBoundingBox().inflate(8),e->e.flightRoot()&&player.getUUID().equals(e.ownerId)).get(0);
        for(int tick=0;tick<14;tick++)YujianController.tickFlightEntity(root,player);
        h.assertTrue(player.getVehicle()==root,"Streaming fixture did not mount the real flight root");root.positionRider(player);return root;
    }
    @GameTest(batch="flight_streaming",template="empty",timeoutTicks=100)
    public static void coldPrefixReadinessNeverLoadsAndWarnsBeforeTheBorder(GameTestHelper h){
        var boundary=coldBoundary(h);var level=h.getLevel();var cache=level.getChunkSource();
        try(var player=new GameTestPlayer(level)){
            player.setPos(boundary.getMinBlockX()-5,level.getMaxBuildHeight()+16,boundary.getMiddleBlockZ());
            int loaded=cache.getLoadedChunksCount();
            var result=FlightChunkStreaming.readiness(player,player.getBoundingBox(),new Vec3(15,0,0),.18);
            h.assertTrue(result.readyDistance()>=0&&result.readyDistance()<5&&!result.complete()&&result.suggestedSpeed()<15,"Cold terrain did not produce a bounded safe prefix: "+result);
            h.assertTrue(cache.getChunkNow(boundary.x,boundary.z)==null&&cache.getLoadedChunksCount()==loaded,"Readiness synchronously loaded the cold chunk");
        }
        h.succeed();
    }
    @GameTest(batch="flight_streaming",template="empty",timeoutTicks=100)
    public static void coldFullRequestsRemainPendingAndRapidReplansCannotEvadeBudgets(GameTestHelper h){
        var level=h.getLevel();var cache=level.getChunkSource();
        try(var player=new GameTestPlayer(level)){
            try{
                var root=mount(h,player);var boundary=coldBoundary(h);
                root.setPos(boundary.getMinBlockX()-5,level.getMaxBuildHeight()+16,boundary.getMiddleBlockZ());root.setYRot(-90);root.positionRider(player);
                FlightChunkStreaming.update(player,root,new Vec3(15,0,0),-90,0,.075,.55,15,false);
                var initial=FlightChunkStreaming.diagnostics(level);
                @SuppressWarnings("unchecked") var jobs=(List<Map<String,Object>>)initial.get("pendingJobs");
                var own=jobs.stream().filter(row->player.getUUID().toString().equals(row.get("owner"))).toList();
                h.assertTrue(!own.isEmpty()&&own.stream().noneMatch(row->Boolean.TRUE.equals(row.get("done"))),"FULL request waited on the server thread instead of returning a genuinely pending future");
                h.assertTrue(cache.getChunkNow(boundary.x,boundary.z)==null,"Async request completed cold terrain before returning to the server loop");
                for(int replan=0;replan<80;replan++)FlightChunkStreaming.update(player,root,new Vec3(15,0,0),-90+replan*13,0,.075,.55,15,false);
                var rider=FlightChunkStreaming.diagnostics(player);var all=FlightChunkStreaming.diagnostics(level);
                h.assertTrue(((Number)rider.get("directColdDestinations")).intValue()<=FlightChunkStreaming.MAX_RIDER_REQUESTS,"Route changes evaded per-rider pending-work budget");
                h.assertTrue(((Number)all.get("directColdDestinations")).intValue()<=FlightChunkStreaming.MAX_TOTAL_REQUESTS&&((Number)all.get("totalTickets")).intValue()<=FlightChunkStreaming.MAX_TOTAL_TICKETS,"Route changes exceeded total work/ticket budget");
                FlightChunkStreaming.release(player);var released=FlightChunkStreaming.diagnostics(player);
                h.assertTrue(((Number)released.get("tickets")).intValue()==0&&((Number)released.get("outstanding")).intValue()>0,"Release leaked tickets or hid still-pending vanilla work from the budget");
            }finally{YujianController.stop(player);}
        }
        h.succeed();
    }
    private FlightChunkStreamingChecks(){}
}
