package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.yujian.FlightInterpolation;
import cn.blockforge.generated.generatedmod.yujian.FlightSnapshot;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;

/** Numeric checks run against the production interpolation class, also usable without a game process. */
@GameTestHolder("wuxingxiantu")
@PrefixGameTestTemplate(false)
public final class FlightInterpolationClockChecks {
    private static int checks;
    private static final double SPEED=15;
    private record Delivery(int client,long server) {}
    private static FlightSnapshot point(long tick){return point(tick,tick*SPEED);}
    private static FlightSnapshot point(long tick,double x){return new FlightSnapshot(1,tick,new Vec3(x,200,0),0,0,0,0,15,0,0);}
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}

    public static String verify(){
        checks=0;
        for(int interval:new int[]{1,2,3,4}){
            var buffer=new FlightInterpolation();double previous=0;int holds=0;
            for(int client=0;client<=480;client++){
                long server=client/interval;if(client%interval==0)buffer.accept(point(server),client);
                double x=buffer.sample(client).position().x;
                check(Double.isFinite(x)&&x>=previous-1e-6&&x<=server*SPEED+1e-6,"Slow-clock playback rewound or extrapolated");
                if(client>120){
                    double step=x-previous;if(step<1e-6)holds++;
                    check(Math.abs(step-SPEED/interval)<1e-4,"Steady slow server still advances in full-tick bursts: interval="+interval+", step="+step);
                    check(server-buffer.playbackTick()<=3.001,"Original 20/20 Hz latency or slow-server buffer grew without bound");
                }
                previous=x;
            }
            check(holds==0&&Math.abs(buffer.clockRate()-1.0/interval)<1e-5,"Steady server clock slope did not converge");
        }
        // Preserve the existing consecutive-delivery-delay regression verbatim:
        // server ticks stay consecutive while the receiving clock shifts by ten.
        var delayed=new FlightInterpolation();
        for(int tick=0;tick<=10;tick++){delayed.accept(point(tick,tick),tick);delayed.sample(tick);}
        double last=delayed.sample(20).position().x;
        for(int tick=11;tick<=22;tick++){
            delayed.accept(point(tick,tick),tick+10);double x=delayed.sample(tick+10).position().x;
            check(x>=last&&x<=tick,"Existing delayed-delivery regression rewound or extrapolated");last=x;
        }
        check(last<=20&&last>=18,"Existing three-tick refill cushion changed");
        double a=delayed.sample(33).position().x,b=delayed.sample(34).position().x;
        check(b>a&&b<=22,"Existing refill no longer bridges a short packet gap");
        var stalled=new FlightInterpolation();double previous=0;long latest=0;int recoveredHolds=0;
        for(int client=0;client<=260;client++){
            if(client<=100){latest=client;stalled.accept(point(latest),client);}
            else if(client>=126){latest=client-25;stalled.accept(point(latest),client);}
            double x=stalled.sample(client).position().x;
            check(x>=previous-1e-6&&x<=latest*SPEED+1e-6,"A stopped/resumed server caused rewind or future prediction");
            if(client>0)check(x-previous<=SPEED*1.201,"Server recovery skipped validated playback ticks");
            if(client>=110&&client<=125)check(Math.abs(x-100*SPEED)<1e-6,"A stalled server did not hold safely at its last validated sample");
            if(client>145&&x-previous<1e-6)recoveredHolds++;
            previous=x;
        }
        check(recoveredHolds==0,"Resumed server never restored continuous buffered playback");

        // A main-thread stall can deliver consecutive validated server ticks in
        // several short batches, even though the long-run server clock is slow.
        // native-final FEATHER advanced 3.699..3.856 server ticks per client tick,
        // then exhausted its history and held while the server kept flying.
        for(int interval:new int[]{2,3,4}){
            var batched=new FlightInterpolation();previous=0;latest=-1;int batchHolds=0;
            for(int client=0;client<=700;client++){
                int count=client<=80?1:client==101||client==106?12:client>=126&&(client-126)%(2*interval)==0?2:0;
                for(int n=0;n<count;n++)batched.accept(point(++latest),client);
                double x=batched.sample(client).position().x;
                check(x>=previous-1e-6&&x<=latest*SPEED+1e-6,"Batch recovery rewound or exceeded the last validated position");
                check(batched.clockRate()<=1.000001,"A catch-up packet batch was mistaken for a faster-than-normal simulation clock");
                check(x-previous<=SPEED*1.201,"Batch recovery advanced several server ticks in one client tick");
                if(client>300&&x-previous<1e-6)batchHolds++;
                previous=x;
            }
            check(batchHolds==0,"A continuous slow server delivered in pairs still produces recurring starvation holds: interval="+interval);
            check(Math.abs(batched.clockRate()-1.0/interval)<.03,"Batch arrival timing did not converge to the actual slow server clock");
        }
        // Eight validated ticks arrive together every sixteen client ticks.
        // A three-tick latency cannot bridge that gap without extrapolation;
        // bounded added latency must replace the old fast-advance/hold cycle.
        var coarseBatches=new FlightInterpolation();previous=0;latest=-1;int coarseHolds=0;
        for(int client=0;client<=1400;client++){
            int count=client<=80?1:client<=900&&(client-96)%16==0?8:client>940?1:0;
            for(int n=0;n<count;n++)coarseBatches.accept(point(++latest),client);
            double x=coarseBatches.sample(client).position().x;
            check(x>=previous-1e-6&&x<=latest*SPEED+1e-6,"Coarse batch playback rewound or extrapolated into unvalidated space");
            check(x-previous<=SPEED*1.201,"Coarse batch recovery skipped multiple server ticks");
            check(coarseBatches.bufferDelay()>=3&&coarseBatches.bufferDelay()<=16,"Adaptive delay escaped the bounded retained history");
            if(client>500&&client<880&&x-previous<1e-6)coarseHolds++;
            if(client>=930&&client<=940)check(Math.abs(x-latest*SPEED)<1e-6,"An actual packet stop did not hold at the last validated sample");
            if(client>1250){
                check(coarseBatches.bufferDelay()<3.001,"Normal delivery did not restore the low-latency buffer");
                check(latest-coarseBatches.playbackTick()<3.01,"Recovered normal delivery retained excessive playback latency");
            }
            previous=x;
        }
        check(coarseHolds==0,"Steady eight-tick packet batches still alternate fast playback and starvation holds");

        // Half-rate server, bounded network jitter, dropped samples, a batch
        // arriving in reverse order, and rejected same-tick corrections.
        List<Delivery> deliveries=new ArrayList<>();
        for(int tick=0;tick<200;tick++)if(tick==0||tick%13!=0)deliveries.add(new Delivery(tick*2+(tick%7==0?2:0),tick));
        var jitter=new FlightInterpolation();previous=0;latest=-1;
        for(int client=0;client<=410;client++){
            for(int i=deliveries.size()-1;i>=0;i--){var d=deliveries.get(i);if(d.client()!=client)continue;
                jitter.accept(point(d.server()),client);latest=Math.max(latest,d.server());
                jitter.accept(point(d.server(),1_000_000),client);
                if(d.server()>1)jitter.accept(point(d.server()-2,-1_000_000),client);
            }
            if(!jitter.active())continue;
            double x=jitter.sample(client).position().x;
            check(x>=previous-1e-6&&x<=latest*SPEED+1e-6,"Dropped/out-of-order/same-tick packets corrupted the validated path");
            if(client>80)check(x-previous<=SPEED*1.201,"Jitter recovery jumped by multiple server ticks");
            previous=x;
        }
        var teleport=new FlightInterpolation();
        for(int tick=0;tick<=60;tick++){teleport.accept(point(tick,tick*SPEED+(tick>=40?10000:0)),tick);teleport.sample(tick);}
        check(teleport.sample(60).position().x>10000,"An adjacent-tick teleport was capped or mistaken for a slow-clock reset");
        teleport.accept(point(1000,17),61);
        check(teleport.sample(61).position().x==17,"A discontinuous server tick no longer starts a fresh validated segment");
        return "Flight interpolation clock checks passed: "+checks+"; 15 blocks/server-tick, rate 1/1..1/4, stall/refill, catch-up batches, jitter/loss/order, teleport";
    }
    public static void main(String[] args){System.out.println(verify());}
    @GameTest(template="empty") public static void interpolationTracksValidatedSlowServerClocks(GameTestHelper helper){
        com.mojang.logging.LogUtils.getLogger().info(verify());helper.succeed();
    }
    private FlightInterpolationClockChecks(){}
}
