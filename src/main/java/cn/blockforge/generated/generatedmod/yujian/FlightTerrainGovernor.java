package cn.blockforge.generated.generatedmod.yujian;

/** Per-ride speed envelope for an incompletely loaded route; no terrain reads or global speed cap. */
public final class FlightTerrainGovernor {
    public static final double BRAKING=.18;
    private static final double BRAKING_TICKS=2+(1-BRAKING)/BRAKING;
    private static final int RECOVERY_TICKS=4,CLEAR_TICKS=8;
    private boolean active;
    private double ceiling;
    private int recoveryTicks,clearTicks;

    /** Normal planning stays authoritative. This can only reduce its next speed. */
    public double limit(double current,double planned,double desired,double readyDistance,boolean complete){
        current=finiteSpeed(current);planned=finiteSpeed(planned);desired=finiteSpeed(desired);
        double ready=Double.isFinite(readyDistance)?Math.max(0,readyDistance):0;
        double safe=complete?Double.POSITIVE_INFINITY:Math.max(0,(ready-2)/BRAKING_TICKS);
        if(!active){
            if(planned<=safe)return planned;
            active=true;ceiling=Math.min(current,planned);recoveryTicks=clearTicks=0;
        }
        ceiling=Math.min(ceiling,planned);
        double step=Math.min(.2,Math.max(.05,desired*.0125));
        if(complete)step=Math.max(step,desired/40);
        if(safe<ceiling){
            ceiling=safe;recoveryTicks=0;
        }else{
            // One newly completed 16m chunk must not immediately restore the
            // ordinary .55 rainbow acceleration. Accumulate a modest reserve,
            // then recover at a bounded slope while that reserve stays ready.
            double reserve=Math.max(16,ceiling*4);
            boolean room=complete||ready>=brakingDistance(ceiling+step)+reserve;
            recoveryTicks=room?Math.min(RECOVERY_TICKS,recoveryTicks+1):0;
            if(recoveryTicks>=RECOVERY_TICKS)ceiling=Math.min(planned,Math.min(safe,ceiling+step));
        }
        // If previously loaded terrain suddenly disappears, this smooth floor
        // can exceed the remaining prefix. The existing exact sweep still has
        // to clip movement; this governor never grants movement permission.
        double next=Math.min(planned,Math.max(current*(1-BRAKING),ceiling));
        clearTicks=complete?Math.min(CLEAR_TICKS,clearTicks+1):0;
        if(clearTicks>=CLEAR_TICKS&&ceiling>=desired-Math.max(.00001,desired*.00001))reset();
        return next;
    }
    public static double brakingDistance(double speed){return finiteSpeed(speed)*BRAKING_TICKS+2;}
    private static double finiteSpeed(double value){return Double.isFinite(value)?Math.max(0,value):0;}
    public boolean active(){return active;}
    public double ceiling(){return ceiling;}
    public void reset(){active=false;ceiling=0;recoveryTicks=clearTicks=0;}
}
