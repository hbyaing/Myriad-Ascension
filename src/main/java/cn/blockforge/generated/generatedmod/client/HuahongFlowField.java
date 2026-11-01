package cn.blockforge.generated.generatedmod.client;

/** Coherent material-domain motion, not a fluid simulation or a flight path. */
public final class HuahongFlowField {
    private static final long PERIOD_TICKS=2560;
    private static final double TAU=Math.PI*2;
    public record Smoke(double side,double up,double radius,double crimson){}
    private HuahongFlowField(){}
    public static Smoke smoke(double behind,double tick){
        double whole=Math.floor(tick);
        double seconds=(Math.floorMod((long)whole,PERIOD_TICKS)+(tick-whole))/20;
        double phase=TAU*seconds/128,scatter=.11+behind*.0015;
        double side=Math.cos(behind*.72-phase*22)*scatter;
        double up=Math.sin(behind*.5976-phase*15)*scatter+behind*.0007;
        double radius=.47+.24*(.5+.5*Math.sin(behind*.58+.30*Math.sin(phase*14)));
        double redPhase=behind*1.4-phase*40;
        double crimson=.20+.80*Math.pow(.5+.5*Math.sin(redPhase),2);
        return new Smoke(side,up,radius,crimson);
    }
}
