package cn.blockforge.generated.generatedmod.yujian;

import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.util.Mth;

/** Bounded presentation buffer. Never extrapolates past a server-validated position. */
public final class FlightInterpolation {
    private static final double BUFFER_TICKS=3,MAX_BUFFER_TICKS=16,MIN_RATE=.05,MAX_RATE=1;
    private static final int MAX_RETIRED_SAMPLES=256;
    private record Arrival(long serverTick,int clientTick) {}
    private final ArrayDeque<FlightSnapshot> samples=new ArrayDeque<>();
    // Already received waypoints evicted from the normal 32-sample queue but
    // not yet presented. Keep the curve, rather than drawing a chord across it.
    private final ArrayDeque<FlightSnapshot> retired=new ArrayDeque<>();
    private final ArrayDeque<Arrival> arrivals=new ArrayDeque<>();
    private double clockRate=1,playbackRate=1,lastClientTime=Double.NaN;
    private double bufferDelay=BUFFER_TICKS;
    private double lastPlayback=Double.NEGATIVE_INFINITY;
    private boolean buffering=true;
    public void accept(FlightSnapshot snapshot,int clientTick){
        if(!samples.isEmpty()&&snapshot.tick()<=samples.getLast().tick())return;
        if(!samples.isEmpty()&&snapshot.tick()-samples.getLast().tick()>20){
            // A discontinuous server clock retains the existing new-segment
            // semantics. Position changes on consecutive ticks are not capped.
            resetSegment();
        }
        // Keep clock observations across a short refill. Clearing them whenever
        // playback reaches the newest sample makes periodic batches continually
        // restart the estimator before their quiet intervals can be measured.
        // A batch has one arrival time. Counting every packet separately would
        // evict the quiet part of the observation window before it can be used.
        if(!arrivals.isEmpty()&&arrivals.getLast().clientTick()==clientTick)arrivals.removeLast();
        arrivals.addLast(new Arrival(snapshot.tick(),clientTick));while(arrivals.size()>16)arrivals.removeFirst();
        Arrival oldest=arrivals.getFirst();int elapsed=clientTick-oldest.clientTick();
        if(elapsed>=4){
            // Estimate clock slope across several arrivals, rather than treating
            // each client tick as a full server tick. A catch-up batch is already
            // validated history, not evidence that normal playback should run
            // faster than 20 Hz; only the bounded buffer correction may catch up.
            double serverSpan=snapshot.tick()-oldest.serverTick(),clientSpan=elapsed;
            if(arrivals.size()>=6){
                // A single delivery pause followed by catch-up is jitter, not
                // a new TPS. Trim the fastest and slowest inter-arrival spans;
                // sustained slow clocks still occupy the rest of this window.
                Arrival previous=null;double fastest=Double.POSITIVE_INFINITY,slowest=0;
                double fastServer=0,fastClient=0,slowServer=0,slowClient=0;
                for(Arrival next:arrivals){
                    if(previous!=null){
                        double ds=next.serverTick()-previous.serverTick(),dc=next.clientTick()-previous.clientTick(),ratio=dc/ds;
                        if(ratio<fastest){fastest=ratio;fastServer=ds;fastClient=dc;}
                        if(ratio>slowest){slowest=ratio;slowServer=ds;slowClient=dc;}
                    }
                    previous=next;
                }
                if(slowest>fastest+1e-9){serverSpan-=fastServer+slowServer;clientSpan-=fastClient+slowClient;}
            }
            double measured=Mth.clamp(serverSpan/clientSpan,MIN_RATE,MAX_RATE);
            clockRate=buffering?measured:clockRate+(measured-clockRate)*.18;
        }
        samples.addLast(snapshot);
        while(samples.size()>32){
            retired.addLast(samples.removeFirst());
            pruneRetired();
            if(retired.size()>MAX_RETIRED_SAMPLES){
                // A bounded buffer cannot losslessly recover an arbitrarily
                // long unplayed route. Start a declared new segment, as for a
                // discontinuous server clock, instead of inventing its curve.
                resetSegment();
                arrivals.addLast(new Arrival(snapshot.tick(),clientTick));
                samples.addLast(snapshot);return;
            }
        }
    }
    public FlightSnapshot sample(double clientTick){
        if(samples.isEmpty())return null;
        FlightSnapshot first=retired.isEmpty()?samples.getFirst():retired.getFirst(),latest=samples.getLast();
        double elapsed=Double.isFinite(lastClientTime)?Math.max(0,clientTick-lastClientTime):0;
        lastClientTime=clientTick;
        if(!Double.isFinite(lastPlayback))lastPlayback=first.tick();
        Arrival newestArrival=arrivals.getLast();
        double neededDelay=BUFFER_TICKS;Arrival previousArrival=null;
        for(Arrival arrival:arrivals){
            if(previousArrival!=null)neededDelay=Math.max(neededDelay,arrival.serverTick()-previousArrival.serverTick());
            previousArrival=arrival;
        }
        // Normal delivery keeps its three-tick latency. Repeated coalesced
        // packets need enough validated history to bridge their observed gap.
        // Cap this at half the existing 32-sample history; larger gaps hold.
        neededDelay=Math.min(MAX_BUFFER_TICKS,neededDelay);
        if(neededDelay>bufferDelay)bufferDelay=neededDelay;
        else bufferDelay+=(neededDelay-bufferDelay)*(1-Math.exp(-elapsed*.05));
        double target=latest.tick()+Math.max(0,clientTick-newestArrival.clientTick())*clockRate-bufferDelay;
        if(buffering){
            if(latest.tick()-lastPlayback>=bufferDelay){
                buffering=false;playbackRate=clockRate;
                lastPlayback=Math.min(latest.tick(),Math.max(lastPlayback,
                        Math.min(target,lastPlayback+elapsed*clockRate)));
            }
        }else if(elapsed>0){
            double predicted=lastPlayback+elapsed*clockRate;
            double correction=Mth.clamp((target-predicted)*.10,-clockRate*.20,clockRate*.20);
            double wanted=clockRate+correction;
            playbackRate+=(wanted-playbackRate)*(1-Math.exp(-elapsed*.35));
            lastPlayback=Math.min(latest.tick(),lastPlayback+elapsed*Math.max(0,playbackRate));
            if(lastPlayback>=latest.tick()-1e-8){
                buffering=true;
            }
        }
        double time=lastPlayback;
        FlightSnapshot previous=first;
        if(time<=previous.tick())return finishSample(previous);
        for(var source:List.of(retired,samples))for(FlightSnapshot next:source){
            if(next.tick()<=previous.tick())continue;
            if(time<=next.tick()){
                return finishSample(interpolate(previous,next,
                        (time-previous.tick())/(next.tick()-previous.tick()),time));
            }
            previous=next;
        }
        return finishSample(samples.getLast());
    }
    private FlightSnapshot finishSample(FlightSnapshot pose){
        pruneRetired();return pose;
    }
    private void pruneRetired(){
        if(retired.isEmpty())return;
        if(!samples.isEmpty()&&samples.getFirst().tick()<=lastPlayback){retired.clear();return;}
        // Retain the last played predecessor until its successor is reached.
        // Removing it early would discard the endpoints of a fractional pose.
        while(retired.size()>1){
            var iterator=retired.iterator();iterator.next();
            if(iterator.next().tick()>lastPlayback)break;
            retired.removeFirst();
        }
    }
    private void resetSegment(){
        samples.clear();retired.clear();arrivals.clear();clockRate=playbackRate=1;bufferDelay=BUFFER_TICKS;
        lastPlayback=Double.NEGATIVE_INFINITY;lastClientTime=Double.NaN;buffering=true;
    }
    private static FlightSnapshot interpolate(FlightSnapshot from,FlightSnapshot to,double fraction,double time){
        float t=(float)Mth.clamp(fraction,0,1);
        return new FlightSnapshot(to.entityId(),(long)time,from.position().lerp(to.position(),t),
                Mth.rotLerp(t,from.yaw(),to.yaw()),Mth.lerp(t,from.pitch(),to.pitch()),
                Mth.lerp(t,from.roll(),to.roll()),Mth.lerp(t,from.bank(),to.bank()),
                Mth.lerp(t,from.speed(),to.speed()),Mth.lerp(t,from.angular(),to.angular()),
                Mth.lerp(t,from.jinghong(),to.jinghong()),t>=1?to.phase():from.phase(),
                t>=1?to.huahongStage():from.huahongStage(),Mth.lerp(t,from.huahong(),to.huahong()));
    }
    public boolean active(){return !samples.isEmpty();}
    public double clockRate(){return clockRate;}
    public double playbackTick(){return lastPlayback;}
    public double bufferDelay(){return bufferDelay;}
    public boolean buffering(){return buffering;}
    public int retiredSamples(){return retired.size();}
}
