package cn.blockforge.generated.generatedmod.client;

/** Pure presentation time/flow, independent of server world-time correction. */
public final class JindouCloudTime {
    private JindouCloudTime(){}
    public static final class Clock {
        private Object level;
        private long tick;
        private double renderTime=-1;
        private boolean rendered;
        public boolean setLevel(Object next){
            if(level==next)return false;
            level=next;tick=0;renderTime=-1;rendered=false;return true;
        }
        public void advance(boolean actualTick){if(level!=null&&actualTick&&tick<Long.MAX_VALUE)tick++;}
        public long tick(){return tick;}
        public double time(float partial){return time(partial,false);}
        public double time(float partial,boolean paused){
            if(paused&&rendered)return renderTime;
            double p=Float.isFinite(partial)?Math.max(0,Math.min(1,partial)):0;
            renderTime=Math.max(renderTime,tick-1+p);rendered=true;return renderTime;
        }
    }
    /** Visible, bounded advection: at most0.32 atlas periods/second, independent of travel. */
    public static double flowStep(float motion){
        return Math.max(0,Math.min(1,Float.isFinite(motion)?motion:0))*.016;
    }
    /** Phase angles then periodic detail offsets, shared by renderer and GPU QA. */
    public static void material(float[] out,double tick,double flow,double seed){
        double time=tick/20;
        out[0]=(float)(fraction(time/5+seed)*Math.PI*2);
        out[1]=(float)(fraction(time/7+seed*.37)*Math.PI*2);
        out[2]=(float)(fraction(time/11+seed*.73)*Math.PI*2);
        // Closed orbits roll in place. Only measured movement advances the
        // longitudinal phase; idle time must not pump a rearward conveyor.
        out[3]=fraction(seed+.090*Math.sin(time*Math.PI*2/7));
        out[4]=fraction(seed*.37+.075*Math.cos(time*Math.PI*2/11));
        out[5]=fraction(seed*.73-flow+.060*Math.sin(time*Math.PI*2/5));
    }
    private static float fraction(double value){return (float)(value-Math.floor(value));}
}
