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
    /** Visible, bounded advection: at most0.20 atlas periods/second, independent of travel. */
    public static double flowStep(float motion){
        return Math.max(0,Math.min(1,Float.isFinite(motion)?motion:0))*.01;
    }
    /** Phase angles then periodic detail offsets, shared by renderer and GPU QA. */
    public static void material(float[] out,double tick,double flow,double seed){
        double time=tick/20;
        out[0]=(float)(fraction(time/5+seed)*Math.PI*2);
        out[1]=(float)(fraction(time/7+seed*.37)*Math.PI*2);
        out[2]=(float)(fraction(time/11+seed*.73)*Math.PI*2);
        out[3]=fraction(time*.025+seed);out[4]=fraction(time*.016+seed*.37);
        out[5]=fraction(-time*.020-flow+seed*.73);
    }
    private static float fraction(double value){return (float)(value-Math.floor(value));}
}
