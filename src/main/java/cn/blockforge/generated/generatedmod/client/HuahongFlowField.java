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
        double radius=.56+.14*Math.sin(behind*.58-phase*40+.35*Math.sin(phase*22+behind*.7));
        double redPhase=behind*1.4-phase*112;
        double crimson=.20+.80*Math.pow(.5+.5*Math.sin(redPhase),2);
        return new Smoke(side,up,radius,crimson);
    }
    /** Animate individual head lobes without moving the attachment or rerolling slots. */
    public static Smoke smokeHead(int index,double tick){
        double whole=Math.floor(tick),phase=TAU*(Math.floorMod((long)whole,PERIOD_TICKS)+(tick-whole))/PERIOD_TICKS;
        double side=Math.sin(index*2.3)*.13+.055*Math.sin(index*1.7-phase*40);
        double up=Math.cos(index*2.3)*.12+.055*Math.cos(index*1.3-phase*48);
        double radius=.48+.11*Math.sin(index*1.7-phase*48);
        return new Smoke(side,up,radius,.2+.8*Math.pow(.5+.5*Math.sin(index*1.9-phase*112),2));
    }
}
