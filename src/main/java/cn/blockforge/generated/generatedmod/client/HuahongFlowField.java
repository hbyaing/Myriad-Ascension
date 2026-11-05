package cn.blockforge.generated.generatedmod.client;

/** Coherent material-domain motion, not a fluid simulation or a flight path. */
public final class HuahongFlowField {
    private static final long PERIOD_TICKS=2560;
    private static final double TAU=Math.PI*2;
    public record Smoke(double side,double up,double radius,double crimson){}
    public record Parcel(long id,double age,double behind,double side,double up,double radius,double alpha,double crimson){}
    public record WorldParcel(long id,double birthTick,double age,double behind,double x,double y,double z,double radius,double alpha,double crimson){}
    /** Old smoke keeps its emission position. Neither current yaw nor a moving
     * head-local curve is an input to its drift, so turns cannot rotate the wake. */
    public static java.util.List<WorldParcel> worldParcels(java.util.List<SwordFlightTrailHistory.Node> path,double now){
        var result=new java.util.ArrayList<WorldParcel>();if(path.size()<2)return result;
        double spacing=.55,head=path.get(path.size()-1).arc(),start=Math.max(path.get(0).arc(),head-84);int cursor=0;
        for(long id=(long)Math.ceil(start/spacing);id*spacing<=head;id++){
            double arc=id*spacing;while(cursor<path.size()-2&&path.get(cursor+1).arc()<arc)cursor++;
            var a=path.get(cursor);var b=path.get(cursor+1);double span=b.arc()-a.arc();if(span<1e-9)continue;
            double t=Math.max(0,Math.min(1,(arc-a.arc())/span)),born=a.tick()+(b.tick()-a.tick())*t,age=(now-born)/20,behind=head-arc;
            if(age<0||age>=1.8)continue;
            double join=smooth((behind-7)/9),end=1-smooth((behind-55)/29),life=1-smooth((age-1.2)/.6);
            double alpha=.18*join*end*life*Math.exp(-age/1.45);if(alpha<.0005)continue;
            double seed=hash(id),other=hash(id+419),phase=seed*TAU+age*(2.1+other*.9),scatter=.08+.20*smooth(age/.6);
            var p=a.point().lerp(b.point(),t);double dx=b.point().x()-a.point().x(),dy=b.point().y()-a.point().y(),dz=b.point().z()-a.point().z(),length=Math.sqrt(dx*dx+dy*dy+dz*dz);
            double drift=length>1e-8?-.8*age/length:0;
            double radius=(.42+.16*seed)*(1+.54*smooth(behind/60));
            result.add(new WorldParcel(id,born,age,behind,p.x()+dx*drift+Math.cos(phase)*scatter,p.y()+dy*drift+Math.sin(phase*1.31)*scatter+age*.12,p.z()+dz*drift+Math.sin(phase)*scatter,radius,alpha,
                    Math.exp(-age*.6)*(.2+.8*Math.pow(.5+.5*Math.sin(phase*1.3+other*TAU),2))));
        }
        return result;
    }
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
            double behind=.06+age*(3.2+4*smooth(age/.65));
            result.add(new Parcel(id,age,behind,Math.cos(angle)*spread,Math.sin(angle)*spread+age*.015,radius,fade,heat));
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
