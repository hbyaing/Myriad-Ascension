package cn.blockforge.generated.generatedmod.client;

/** Coherent material-domain motion, not a fluid simulation or a flight path. */
public final class HuahongFlowField {
    private static final long PERIOD_TICKS=2560;
    private static final double TAU=Math.PI*2;
    public record Smoke(double side,double up,double radius,double crimson){}
    public record Parcel(long id,double age,double behind,double side,double up,double radius,double alpha,double crimson){}
    /** Time births advect down the attached wake; travel speed never rerolls their identities. */
    public static java.util.List<Parcel> parcels(double tick){
        double seconds=tick/20,interval=.08,life=3.2;long last=(long)Math.floor(seconds/interval);
        var result=new java.util.ArrayList<Parcel>(42);
        for(long id=last-40;id<=last;id++){
            double age=seconds-id*interval;if(age<0||age>=life)continue;
            double seed=hash(id),other=hash(id+419),fade=smooth(age/.14)*smooth((life-age)/.65);
            double angle=seed*TAU+age*(2.1+other*.9)+.35*age*age;
            double spread=.045+.15*(1-Math.exp(-age*2.8));
            double radius=(.36+.17*seed)*(.72+.45*smooth(age/.75));
            double heat=Math.exp(-age*.35)*(.25+.75*Math.pow(.5+.5*Math.sin(angle*1.3+other*TAU),2));
            result.add(new Parcel(id,age,.12+age*7.2,Math.cos(angle)*spread,Math.sin(angle)*spread+age*.015,radius,fade,heat));
        }
        return result;
    }
    private static double smooth(double x){double t=Math.max(0,Math.min(1,x));return t*t*(3-2*t);}
    private static double hash(long id){long v=id*0x9e3779b97f4a7c15L;v=(v^(v>>>30))*0xbf58476d1ce4e5b9L;v=(v^(v>>>27))*0x94d049bb133111ebL;return ((v^(v>>>31))>>>11)*0x1.0p-53;}
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
