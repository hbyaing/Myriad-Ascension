package cn.blockforge.generated.generatedmod.yujian;

import java.util.ArrayDeque;
import net.minecraft.util.Mth;

/** Bounded presentation buffer. Never extrapolates past a server-validated position. */
public final class FlightInterpolation {
    private static final double BUFFER_TICKS=3,MAX_BUFFER_TICKS=16,MIN_RATE=.05,MAX_RATE=1;
    private record Arrival(long serverTick,int clientTick) {}
    private final ArrayDeque<FlightSnapshot> samples=new ArrayDeque<>();
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
            samples.clear();arrivals.clear();clockRate=playbackRate=1;bufferDelay=BUFFER_TICKS;
            lastPlayback=Double.NEGATIVE_INFINITY;lastClientTime=Double.NaN;buffering=true;
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
        samples.addLast(snapshot);while(samples.size()>32)samples.removeFirst();
    }
    public FlightSnapshot sample(double clientTick){
        if(samples.isEmpty())return null;
        FlightSnapshot first=samples.getFirst(),latest=samples.getLast();
        double elapsed=Double.isFinite(lastClientTime)?Math.max(0,clientTick-lastClientTime):0;
        lastClientTime=clientTick;
        if(!Double.isFinite(lastPlayback))lastPlayback=first.tick();
        // The retained history is bounded, as before. A client that did not
        // sample for more than that history must resume from its first sample.
        lastPlayback=Math.max(lastPlayback,first.tick());
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
        FlightSnapshot previous=samples.getFirst();
        if(time<=previous.tick())return previous;
        for(FlightSnapshot next:samples){
            if(next.tick()<=previous.tick())continue;
            if(time<=next.tick()){
                float t=(float)Mth.clamp((time-previous.tick())/(next.tick()-previous.tick()),0,1);
                return new FlightSnapshot(next.entityId(),(long)time,previous.position().lerp(next.position(),t),
                        Mth.rotLerp(t,previous.yaw(),next.yaw()),Mth.lerp(t,previous.pitch(),next.pitch()),
                        Mth.lerp(t,previous.roll(),next.roll()),Mth.lerp(t,previous.bank(),next.bank()),
                        Mth.lerp(t,previous.speed(),next.speed()),Mth.lerp(t,previous.angular(),next.angular()),
                        Mth.lerp(t,previous.jinghong(),next.jinghong()));
            }
            previous=next;
        }
        return samples.getLast();
    }
    public boolean active(){return !samples.isEmpty();}
    public double clockRate(){return clockRate;}
    public double playbackTick(){return lastPlayback;}
    public double bufferDelay(){return bufferDelay;}
    public boolean buffering(){return buffering;}
}
