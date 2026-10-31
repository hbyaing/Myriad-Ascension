package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.fixtures.GameTestPlayer;
import cn.blockforge.generated.generatedmod.yujian.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("wuxingxiantu")
@PrefixGameTestTemplate(false)
public final class FlightSweepPerformanceGameTests {
    private static FlightCollision.Sweep reference(GameTestPlayer p,AABB box,Vec3 velocity,boolean phase){
        if(FlightCollision.clear(p,box.minmax(box.move(velocity)),phase))return new FlightCollision.Sweep(velocity,false,true);
        int steps=Math.min(YujianTuning.MAX_SWEEP_STEPS,Math.max(1,(int)Math.ceil(velocity.length()/.2)));
        Vec3 accepted=Vec3.ZERO;
        for(int i=1;i<=steps;i++){
            Vec3 next=velocity.scale(i/(double)steps);AABB swept=box.move(accepted).minmax(box.move(next));
            if(!FlightCollision.loaded(p,swept.inflate(1)))return new FlightCollision.Sweep(accepted,true,false);
            if(!FlightCollision.clear(p,swept,phase))return new FlightCollision.Sweep(accepted,true,true);
            accepted=next;
        }
        return new FlightCollision.Sweep(accepted,false,true);
    }
    @GameTest(template="furnace_empty",timeoutTicks=100)
    public static void coarseEmptySectionsKeepExactCollisionFallback(GameTestHelper h){
        try(var p=new GameTestPlayer(h.getLevel())){
            var start=h.absolutePos(new BlockPos(0,0,0));double y=h.getLevel().getMaxBuildHeight()-20;
            for(int x=-1;x<=4;x++)for(int z=-1;z<=4;z++)h.getLevel().getChunk((start.getX()>>4)+x,(start.getZ()>>4)+z);
            p.setPos(start.getX()+4,y,start.getZ()+4);var box=p.getBoundingBox();
            Vec3 fast=new Vec3(48,0,0);
            reference(p,box,fast,false);FlightCollision.sweep(p,box,fast,false);
            long oldStart=System.nanoTime();for(int i=0;i<20;i++)reference(p,box,fast,false);long oldNanos=System.nanoTime()-oldStart;
            long newStart=System.nanoTime();for(int i=0;i<20;i++)FlightCollision.sweep(p,box,fast,false);long newNanos=System.nanoTime()-newStart;
            com.mojang.logging.LogUtils.getLogger().info("FLIGHT_SWEEP_PROFILE reference_ms={} optimized_ms={} samples=20",oldNanos/1e6,newNanos/1e6);
            var wall=BlockPos.containing(p.position().add(20,0,0));var saved=h.getLevel().getBlockState(wall);
            try{
                h.getLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
                for(var v:java.util.List.of(fast,new Vec3(35,0,1),new Vec3(12,0,8)))for(boolean phase:new boolean[]{false,true}){
                    var a=reference(p,box,v,phase);var b=FlightCollision.sweep(p,box,v,phase);
                    h.assertTrue(a.blocked()==b.blocked()&&a.loaded()==b.loaded(),"Coarse scan changed collision semantics");
                    h.assertTrue(a.movement().distanceTo(b.movement())<=.21,"Fine collision prefix exceeded one original step");
                }
            }finally{h.getLevel().setBlockAndUpdate(wall,saved);}
        }
        h.succeed();
    }
}
