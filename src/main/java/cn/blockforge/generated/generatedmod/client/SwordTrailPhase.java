package cn.blockforge.generated.generatedmod.client;

/** Bounded material coordinates. Geometry retains world history; flow follows the rendered head. */
public final class SwordTrailPhase {
    public static final double ARC_PERIOD=256;
    public static final int TIME_PERIOD_TICKS=2560;
    public static final double TIME_PERIOD_SECONDS=128;
    /** A level-local simulation clock. Network world-time corrections cannot move trail age or phase. */
    public static final class TickClock {
        private Object level;
        private boolean initialized;
        private long tick;
        /** Return true only on a real level identity replacement, including disconnect. */
        public boolean setLevel(Object next){
            if(level==next)return false;
            level=next;initialized=false;tick=0;return true;
        }
        /** Called once at ClientTick.END; paused frames do not represent a level simulation tick. */
        public void advance(long initialWorldTick,boolean actualLevelTick){
            if(level==null||!actualLevelTick)return;
            if(!initialized){tick=initialWorldTick;initialized=true;}
            else if(tick<Long.MAX_VALUE)tick++;
        }
        public boolean initialized(){return initialized;}
        public long tick(){return tick;}
    }
    public static double origin(double headArc,int seed){
        if(!Double.isFinite(headArc))return 0;
        // World-fixed high-frequency U sweeps through a following camera at
        // flight speed (hundreds of material cycles per second). Keep its head
        // phase fixed and let EffectTime provide the deliberate material flow.
        return headArc-(seed&63)*.37;
    }
    /** Subtract in double precision before submitting small, continuous float coordinates. */
    public static float coordinate(double arc,double origin){return (float)(arc-origin);}
    /** Match the interpolated pose without promoting a long clock to float.
     * Assigning long+float to a double is too late: old worlds lose their
     * fractional tick first, making history overtake its smooth attachment. */
    public static double renderTick(long tick,float partial){
        double fraction=Float.isFinite(partial)?Math.max(0,Math.min(1,(double)partial)):0;
        return (tick-1)+fraction;
    }
    /** Reduce the integer tick before float conversion, including worlds older than 2^24 ticks. */
    public static float time(long tick,float partial){
        double fraction=Float.isFinite(partial)?Math.max(0,Math.min(1,partial)):0;
        return (float)((Math.floorMod(tick,TIME_PERIOD_TICKS)+fraction)/20.0);
    }
    public static double angle(double arc,double time,int spaceHarmonic,int timeHarmonic){
        return Math.PI*2*(arc*spaceHarmonic/ARC_PERIOD+time*timeHarmonic/TIME_PERIOD_SECONDS);
    }
    public static double noiseX(double arc,double time,int spaceCells,int timeCells){
        return arc*spaceCells/ARC_PERIOD+time*timeCells/TIME_PERIOD_SECONDS;
    }
    private SwordTrailPhase(){}
}
