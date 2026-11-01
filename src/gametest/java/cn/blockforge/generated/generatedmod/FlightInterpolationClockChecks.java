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
    private static FlightSnapshot curve(long tick){
        double angle=Math.toRadians(4),radius=30/(2*Math.sin(angle*.5));
        return new FlightSnapshot(1,tick,new Vec3(radius*Math.sin(tick*angle),200,radius*(1-Math.cos(tick*angle))),
                (float)(tick*4),0,0,0,30,0,0);
    }
    private static Vec3 curvePosition(double tick){
        long first=(long)Math.floor(tick);
        return curve(first).position().lerp(curve(first+1).position(),(float)(tick-first));
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}

    public static String verify(){
        checks=0;
        visualStatePlayback();
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

        // More than 32 consecutive validated packets can arrive between render
        // samples after chunk/network work stalls. Evicting history must not
        // force the presented head forward by twelve ticks (360 blocks at 30/t).
        for(double speed:new double[]{15,30}){
            var overflow=new FlightInterpolation();
            for(int tick=0;tick<=10;tick++){overflow.accept(point(tick,tick*speed),tick);overflow.sample(tick);}
            double prior=overflow.sample(10).position().x;
            for(int tick=11;tick<=50;tick++)overflow.accept(point(tick,tick*speed),11);
            for(int client=11;client<=500;client++){
                long server=client+39;
                if(client>11)overflow.accept(point(server,server*speed),client);
                // Include three render samples per tick: the retained bridge
                // endpoints must stay fixed while the receive buffer advances.
                for(int frame=0;frame<3;frame++){
                    double x=overflow.sample(client+frame/3.0).position().x;
                    check(x>=prior-1e-5&&x<=server*speed+1e-5,"Overflow bridge rewound or extrapolated");
                    check(x-prior<=speed*1.201*(client==11&&frame==0?1:1.0/3)+.001,
                            "Evicted playback history forced a multi-tick presentation jump");
                    prior=x;
                }
            }
            check(overflow.playbackTick()>530,"Overflow bridge did not recover bounded normal playback");
        }

        // Straight endpoints cannot reveal a bridge that cuts across the real
        // curve. These are legal 30-block fixed steps with a 4-degree turn;
        // a 40-packet endpoint-only bridge departed from them by 36.75 blocks.
        for(int batch:new int[]{40,96}){
            var curved=new FlightInterpolation();
            for(int tick=0;tick<=10;tick++){curved.accept(curve(tick),tick);curved.sample(tick);}
            Vec3 prior=curved.sample(10).position(),expectedPrior=curvePosition(curved.playbackTick());
            double priorTime=curved.playbackTick();Vec3 priorStep=null,priorExpectedStep=null;
            for(int tick=11;tick<=10+batch;tick++)curved.accept(curve(tick),11);
            for(int client=11;client<=900;client++){
                long server=client+batch-1;
                if(client>11)curved.accept(curve(server),client);
                for(int frame=0;frame<3;frame++){
                    Vec3 pose=curved.sample(client+frame/3.0).position(),expected=curvePosition(curved.playbackTick());
                    check(curved.playbackTick()>=priorTime&&curved.playbackTick()<=server,"Retired curve playback rewound or extrapolated");
                    check(pose.distanceTo(expected)<.0001,"Evicted curved waypoints were replaced by a fictitious chord: batch="+batch);
                    Vec3 step=pose.subtract(prior),expectedStep=expected.subtract(expectedPrior);
                    check(step.distanceTo(expectedStep)<.0002,"Retired/current boundary changed actual world velocity");
                    if(priorStep!=null)check(step.subtract(priorStep).distanceTo(expectedStep.subtract(priorExpectedStep))<.0004,
                            "Retired/current boundary introduced a world acceleration impulse");
                    check(curved.retiredSamples()<=256,"Validated retired path exceeded its explicit memory budget");
                    prior=pose;expectedPrior=expected;priorTime=curved.playbackTick();priorStep=step;priorExpectedStep=expectedStep;
                }
            }
            check(curved.retiredSamples()==0,"Already presented overflow waypoints were never released");
        }
        // A genuine adjacent-tick teleport must retain its original one-tick
        // segment, even when that segment has left the ordinary receive queue.
        var retiredTeleport=new FlightInterpolation();
        for(int tick=0;tick<=10;tick++){retiredTeleport.accept(point(tick,tick*30),tick);retiredTeleport.sample(tick);}
        for(int tick=11;tick<=50;tick++)retiredTeleport.accept(point(tick,tick*30+(tick>=20?10000:0)),11);
        for(int client=11;client<=70;client++){
            if(client>11){long server=client+39;retiredTeleport.accept(point(server,server*30+10000),client);}
            double x=retiredTeleport.sample(client).position().x,time=retiredTeleport.playbackTick();
            long first=(long)Math.floor(time);float fraction=(float)(time-first);
            double start=first*30+(first>=20?10000:0),end=(first+1)*30+(first+1>=20?10000:0);
            check(Math.abs(x-(start+(end-start)*fraction))<.001,"Retired teleport was stretched across unrelated server ticks");
        }
        // A clock gap clears retired data as well. Over 256 unplayed retired
        // waypoints deliberately starts a new segment, without unbounded growth
        // or an invented interpolation across the discarded path.
        var bounded=new FlightInterpolation();
        for(int tick=0;tick<=10;tick++){bounded.accept(curve(tick),tick);bounded.sample(tick);}
        int highWater=0,resets=0,lastRetired=0;
        for(int tick=11;tick<=400;tick++){
            bounded.accept(curve(tick),11);int count=bounded.retiredSamples();
            check(count<=256,"An oversized backlog grew beyond its retired waypoint budget");
            highWater=Math.max(highWater,count);if(count==0&&lastRetired>200)resets++;lastRetired=count;
        }
        check(highWater==256&&resets==1,"An over-budget backlog did not explicitly start a new segment");
        Vec3 resetPose=bounded.sample(11).position();
        check(bounded.playbackTick()>100&&resetPose.distanceTo(curvePosition(bounded.playbackTick()))<.0001,
                "Budget reset drew a fictitious chord from its discarded segment");
        bounded.accept(point(1000,17),12);
        check(bounded.retiredSamples()==0&&bounded.sample(12).position().x==17,"A discontinuous server clock retained old overflow waypoints");

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
        return "Flight interpolation clock checks passed: "+checks+"; 15/30 blocks/server-tick, rate 1/1..1/4, stall/refill, straight/curved bounded overflow, jitter/loss/order, teleport";
    }
    public static void main(String[] args){System.out.println(verify());}
    @GameTest(template="empty") public static void interpolationTracksValidatedSlowServerClocks(GameTestHelper helper){
        com.mojang.logging.LogUtils.getLogger().info(verify());helper.succeed();
    }
    private FlightInterpolationClockChecks(){}
    private static float envelope(double tick){
        if(tick<20)return 0;if(tick<26)return (float)((tick-20)/6);
        if(tick<55)return 1;if(tick<63)return (float)((63-tick)/8);return 0;
    }
    private static FlightSnapshot visualPoint(long tick){
        return new FlightSnapshot(1,tick,new Vec3(tick*SPEED,200,0),0,0,0,0,15,0,0,
                tick<15||tick>=58?0:2,tick<20||tick>=63?0:tick<26?1:tick<55?2:3,envelope(tick));
    }
    private static void visualStatePlayback(){
        var wire=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{var source=visualPoint(37);FlightSnapshot.encode(source,wire);
            check(source.equals(FlightSnapshot.decode(wire))&&wire.readableBytes()==0,"Flight snapshot lost timestamped visual state on the wire");
        }finally{wire.release();}
        for(int batch:new int[]{1,4,12,40,96}){
            var path=new FlightInterpolation();int received=-1;
            for(int client=0;client<160;client++){
                if(client%batch==0)for(int tick=received+1;tick<=Math.min(100,client);tick++){path.accept(visualPoint(tick),client);received=tick;}
                if(!path.active())continue;
                var shown=path.sample(client);double time=shown.position().x/SPEED;
                check(Math.abs(shown.huahong()-envelope(time))<2e-5,"Metadata ran ahead of the validated pose in a "+batch+"-packet batch at "+time);
                int expected=time<15||time>=58?0:2;
                if(Math.abs(time-Math.rint(time))>1e-4)check(shown.phase()==expected,"Flight phase used the latest packet instead of the playback clock");
            }
        }
    }
}
