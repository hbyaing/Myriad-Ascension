package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.yujian.FlightTerrainGovernor;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Deterministic frontier replays against the actual production governor. */
@GameTestHolder("wuxingxiantu")
@PrefixGameTestTemplate(false)
public final class FlightTerrainGovernorChecks {
    private static int checks;
    // native-cold server clocks 80..319. Each digit is the number of new 16m
    // ready-frontier planes since the preceding world tick, not a warm call.
    // Reconstructed fronts differ from recorded post-move samples by <.019m;
    // the initial prefix below is rounded DOWN by >.05m for a conservative replay.
    private static final String NATIVE_ADVANCES=
            "000100101000100001100001010000101001000100101010010010010101000010101001010000011000001000010001000010001010001001010010010010000101000001000010000100101000001001001001010000010010100100011000010010101000100101000100101010001001010010000100";
    private record Stats(double mean,double minimum,double maximum,double largestRise,double largestFall,
                         double largestJerk,int risesOverThree,int fallsBelowMinusTwo,double distance,double remaining){}
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private static Stats stats(double[] speeds,double distance,double remaining){
        double sum=0,min=Double.POSITIVE_INFINITY,max=0,rise=0,fall=0,jerk=0,previousDelta=0;
        int rises=0,falls=0;
        for(int i=0;i<speeds.length;i++){
            double speed=speeds[i];sum+=speed;min=Math.min(min,speed);max=Math.max(max,speed);
            if(i==0)continue;double delta=speed-speeds[i-1];rise=Math.max(rise,delta);fall=Math.min(fall,delta);
            if(delta>3)rises++;if(delta< -2)falls++;if(i>1)jerk=Math.max(jerk,Math.abs(delta-previousDelta));previousDelta=delta;
        }
        return new Stats(sum/speeds.length,min,max,rise,fall,jerk,rises,falls,distance,remaining);
    }
    private static double drive(FlightTerrainGovernor governor,double speed,double top,double normalAcceleration,double ready,boolean complete){
        double planned=speed+(top-speed)*normalAcceleration;
        double next=governor.limit(speed,planned,top,ready,complete);
        check(Double.isFinite(next)&&next>=0&&next<=planned+1e-8,"Governor exceeded normal physics or returned an invalid speed");
        return next;
    }
    private static Stats nativeFrontier(){
        var governor=new FlightTerrainGovernor();double position=0,speed=14.974481,frontier=91.625,minRemaining=Double.POSITIVE_INFINITY;
        double[] speeds=new double[NATIVE_ADVANCES.length()];
        for(int tick=0;tick<speeds.length;tick++){
            frontier+=(NATIVE_ADVANCES.charAt(tick)-'0')*16;
            speed=drive(governor,speed,14.974875,.55,frontier-position,false);position+=speed;speeds[tick]=speed;
            minRemaining=Math.min(minRemaining,frontier-position);
            check(position<=frontier+1e-7,"Native-frontier replay entered unavailable terrain");
            check(speed>.01,"A continuously advancing native frontier produced a hard stop");
        }
        Stats s=stats(speeds,position,minRemaining);
        // The observed old controller had 51 rises >3, 42 falls <-2, jerk 5.724,
        // mean 5.218 and travelled 1252.217m in these same 240 server ticks.
        check(s.risesOverThree()==0&&s.fallsBelowMinusTwo()<=1,"Native chunk pulses did not substantially decrease");
        check(s.largestJerk()<2&&s.minimum()>3,"Native replay retained rapid braking/recovery oscillation");
        check(s.distance()>1252.217*.94,"Pulse reduction was obtained by unnecessarily suppressing cold traversal");
        return s;
    }
    private static void periodicFrontiers(){
        for(double top:new double[]{3.32775,5.54625,14.974875,30,120})for(int interval:new int[]{2,3,4,8,16,32}){
            var governor=new FlightTerrainGovernor();double position=0,speed=top,initial=FlightTerrainGovernor.brakingDistance(top)+32;
            double normal=top<6?.13:.55;double[] measured=new double[600];
            for(int tick=0;tick<1200;tick++){
                double frontier=initial+16*(tick/interval);
                speed=drive(governor,speed,top,normal,frontier-position,false);position+=speed;
                check(position<=frontier+1e-6,"A periodic ready frontier was crossed: top="+top+", interval="+interval);
                check(speed>.0001,"A progressing frontier locked the governor at zero");
                if(tick>=600)measured[tick-600]=speed;
            }
            Stats s=stats(measured,position,0);double throughput=Math.min(top,16.0/interval);
            check(s.mean()>throughput*.90&&s.mean()<throughput*1.08,"Governor did not track available cold throughput");
            check(s.risesOverThree()==0&&s.fallsBelowMinusTwo()==0,"Chunk promotion still causes high-amplitude speed pulses");
            for(int tick=0;tick<128;tick++){
                speed=drive(governor,speed,top,normal,10000,true);
                if(tick==63)check(speed>top*.995,"Loaded corridor failed its 64-tick full-cruise recovery deadline");
            }
            check(!governor.active()&&speed>top*.9999,"A generated corridor retained a stale loading limit");
        }
    }
    private static void stallsTurnsAndControls(){
        var stopped=new FlightTerrainGovernor();double speed=15,position=0,frontier=160;
        for(int tick=0;tick<240;tick++){
            speed=drive(stopped,speed,15,.55,frontier-position,false);position+=speed;
            check(position<=frontier+1e-6,"An actually stopped frontier caused future extrapolation");
        }
        check(speed<.00001&&frontier-position>=1.99,"A stopped loader did not hold a safe prefix");
        for(int tick=0;tick<80;tick++)speed=drive(stopped,speed,15,.55,10000,true);
        check(speed>14.99&&!stopped.active(),"Restarted loading left a permanent low-speed lock");

        // Abrupt invalidation needs the collision sweep: the smooth governor
        // deliberately cannot make an already-moving body stop instantaneously.
        var turn=new FlightTerrainGovernor();double suggested=turn.limit(15,15,15,0,false);
        check(suggested>=15*.82-1e-8&&suggested<=15,"Abrupt loss changed the promised loading deceleration");
        double signed=0; // exact sweep clips the zero ready prefix, as production does
        for(int tick=0;tick<80;tick++){
            double normal=signed+(-15-signed)*.55;
            double magnitude=turn.limit(Math.abs(signed),Math.abs(normal),15,10000,true);
            check(magnitude<=Math.abs(normal)+1e-8,"Turning governor accelerated beyond normal steering");
            signed=Math.copySign(magnitude,normal);
            if(tick==63)check(signed< -14.925,"Turning into a loaded opposite corridor retained the old cold-direction cap");
        }
        check(signed< -14.99&&!turn.active(),"Loaded 180-degree recovery remained locked");

        for(double planned:new double[]{0,.01,.4,2,8}){
            var braking=new FlightTerrainGovernor();braking.limit(10,10,15,0,false);
            check(braking.limit(8,planned,0,10000,true)<=planned,"User braking/hovering was overridden by the governor");
        }
        for(double top:new double[]{3.32775,14.974875,30,120}){
            var hot=new FlightTerrainGovernor();double v=0;
            for(int tick=0;tick<120;tick++){
                double normal=v+(top-v)*.55,next=hot.limit(v,normal,top,10000,true);
                check(next==normal&&!hot.active(),"Already-loaded normal flight was globally slowed");v=next;
            }
        }
        var invalid=new FlightTerrainGovernor();
        check(Double.isFinite(invalid.limit(Double.NaN,Double.POSITIVE_INFINITY,-3,Double.NaN,false)),"Invalid diagnostic inputs escaped as NaN");
    }
    private static void oneChunkRestartsStoppedFlight(){
        for(double top:new double[]{7,15}){
        var governor=new FlightTerrainGovernor();double speed=top,position=0,frontier=160;
        for(int tick=0;tick<240;tick++){
            speed=drive(governor,speed,top,.55,frontier-position,false);position+=speed;
        }
        check(speed<.00001,"Single-chunk restart fixture did not first stop");
        double before=position;frontier+=16;
        for(int tick=0;tick<8;tick++){
            speed=drive(governor,speed,top,.55,frontier-position,false);position+=speed;
            check(position<=frontier+1e-7,"Single-chunk restart crossed unknown terrain");
        }
        check(position-before>1,"A newly ready 16m chunk left flight locked at zero awaiting a second chunk");
        for(int tick=0;tick<240;tick++){
            speed=drive(governor,speed,top,.55,frontier-position,false);position+=speed;
            check(position<=frontier+1e-7,"Restart exhausted a ready prefix by entering unknown terrain");
        }
        check(speed<.00001&&frontier-position>=1.99,"Restart did not stop safely if loading stopped again");
        }
    }
    public static String verify(){
        checks=0;Stats nativeStats=nativeFrontier();periodicFrontiers();stallsTurnsAndControls();oneChunkRestartsStoppedFlight();
        return "Flight terrain governor checks passed: "+checks+"; native frontier="+nativeStats
                +"; periodic 16m chunks/2..32 ticks, 3.3..120 blocks/tick, hot/turn recovery, stopped loader, normal flight unchanged";
    }
    public static void main(String[] args){System.out.println(verify());}
    @GameTest(template="empty") public static void governorSmoothsColdFrontiersAndRestoresCruise(GameTestHelper helper){
        com.mojang.logging.LogUtils.getLogger().info(verify());helper.succeed();
    }
}
