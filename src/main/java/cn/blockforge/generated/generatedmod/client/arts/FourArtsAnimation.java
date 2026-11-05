package cn.blockforge.generated.generatedmod.client.arts;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;
import static cn.blockforge.generated.generatedmod.client.arts.FourArtsGeometry.*;

final class FourArtsAnimation {
    private static final double TURN=Math.PI*2;
    private static final Vec3 UP=new Vec3(0,1,0);
    private static final int WIND=0x87EDFF, PALE_WIND=0xD1FFF7, RECOVERY_CYAN=0x56EEDB;
    private static final int SPIRIT_BLUE=0x217AFF, SPIRIT_ICE=0x7DD4FF, CARMINE=0xFF1A38, RUBY=0xB00B2B;
    private static final int IVORY=0xFFEBB0, GOLD=0xFFB347, SUN=0xFFCF78, STAIN=0x47164D;
    private static final int[] LEAF_COLORS={0x499D36,0x78C44D,0x9ED56B,0x398B4B};

    private FourArtsAnimation() {}

    static void wind(FourArtsGeometry fx,Vec3 base,Vec3 front,List<FourArtsEffects.WindPoint> history,
                     boolean passive,boolean descending,double time,double age) {
        var right=new Vec3(front.z,0,-front.x);
        double strength=passive?.53:.91;
        boolean moving=history.size()>2 && !(passive&&descending)
                && history.get(history.size()-1).point().subtract(history.get(0).point()).horizontalDistanceSqr()>=.016;
        if(moving && age>=.09) {
            for(int side:new int[]{-1,1}) for(int strand=0;strand<(passive?2:4);strand++) {
                List<Vec3> points=new ArrayList<>();
                for(int i=Math.max(0,history.size()-(passive?9:17));i<history.size();i++) {
                    var sample=history.get(i);
                    double past=(history.size()-1-i)/20.0;
                    double drift=past*(passive?.42:.70);
                    Vec3 lateral=new Vec3(sample.forward().z,0,-sample.forward().x);
                    points.add(sample.point().subtract(sample.forward().scale(.19+drift))
                            .add(lateral.scale(side*(.15+past*.19+strand*.054)))
                            .add(0,.13+strand*.087+past*.30+.060*Math.sin(sample.time()*.49+side+strand),0));
                }
                boolean gauze=strand<(passive?1:2);
                fx.ribbon(points,gauze?(passive?.067:.14)/(1+strand*.20):.025,
                        strand==0?PALE_WIND:WIND,gauze?GAUZE:LINE,strength/(1+strand*.16),side+strand*3);
            }
        }
        if(!passive && (!moving || age<.54)) {
            double lift=age<.54?pulse(age,0,.18,.64):.17;
            for(int i=0;i<2;i++) {
                final int strand=i;
                fx.ribbon(curve(34,u->base.add(polar(.46+strand*.08,.05+u*.15,
                                (u*.92+time*.85+strand*.53)*TURN))),
                        .040,WIND,LINE,.19+lift*.72,i);
            }
            fx.plane(base.add(0,.035,0),front.scale(.72),right.scale(.72),WIND,SIGIL,lift*.58,2);
        }
        if(descending) {
            for(int i=0;i<3;i++) {
                final int layer=i;
                double depth=.07+layer*.115;
                double radius=.45+layer*.107;
                double angle=.35+time*1.15+layer*2.15;
                fx.ribbon(curve(36,u->{
                    double swell=Math.sin(u*Math.PI);
                    return base.add(polar(radius*(.81+.19*swell),-depth+.087*swell-.03*u,
                            angle+u*(TURN*.71-layer*.15)));
                }),.127-layer*.025,layer==0?PALE_WIND:WIND,AIR,.82-layer*.14,layer);
            }
        }
    }

    static void renewal(FourArtsGeometry fx,Vec3 base,double height,double time,boolean passive) {
        double lifetime=passive?.93:1.56;
        if(time<0 || time>=lifetime) return;
        int leaves=passive?9:21;
        double span=passive?.77:1;
        for(int i=0;i<leaves;i++) {
            double delay=.014*(i%7);
            double age=(time-delay)/lifetime;
            if(age<=0 || age>=1) continue;
            double alpha=ease(0,.16,age)*(1-ease(.68,1,age))*(passive?.77:.97);
            double angle=i*2.399963+age*(3.15+(i%4)*.26);
            double radius=(.48+(i%6)*.075+.24*Math.sin(age*Math.PI))*span;
            double y=(.12+(i%8)*.086+.30*age+.047*Math.sin(angle*2+i))*height;
            Vec3 position=base.add(polar(radius,y,angle));
            double heading=angle+.69+Math.sin(age*8+i)*.48;
            double tilt=.76*Math.sin(age*9.4+i*1.75);
            Vec3 along=UP.scale(Math.cos(tilt)).add(polar(Math.sin(tilt),0,heading));
            Vec3 cross=polar(1,0,heading+Math.PI*.5);
            double roll=.91*Math.sin(age*13.7+i*.83);
            Vec3 across=cross.scale(Math.cos(roll)).add(along.cross(cross).scale(Math.sin(roll)));
            double size=(.099+(i%5)*.016)*span;
            fx.plane(position,along.scale(size),across.scale(size*.60),LEAF_COLORS[i%LEAF_COLORS.length],LEAF,alpha,i);
        }
    }

    static void renewalSurface(FourArtsGeometry fx,List<Vec3> surface,double time,boolean passive) {
        double age=time/.78;
        if(age<0 || age>=(passive?1:2)) return;
        double pass=fract(age);
        double pulse=ease(0,.08,pass)*(1-ease(.84,1,pass));
        fx.surface(surface,-.09+pass*1.25,RISE,RECOVERY_CYAN,pulse*(passive?.73:age<1?.91:.68));
    }

    static void purification(FourArtsGeometry fx,List<Vec3> surface,Vec3 base,Vec3 chest,double height,
                             double time,boolean passive,boolean cleared) {
        if(time<0 || time>=2.04) return;
        double scale=Mth.clamp(height/2.075,.73,1.16);
        if(passive) {
            double strength=pulse(time,.025,.19,.88);
            if(strength<=.004) return;
            double rise=ease(.02,.70,time);
            fx.surface(surface,0,LOTUS_LIGHT,IVORY,strength*.50);
            lotusField(fx,base,.91+.25*rise,.22+rise*.13,strength*.58);
            if(chest!=null) fx.stamp(chest.add(0,.12*rise*scale,0),.44,.42,IVORY,LOTUS,strength*.88,0,0);
            for(int i=0;i<5;i++) {
                var point=base.add(polar(.41+.24*rise,(.48+i*.21+.52*rise)*scale,i*2.399963+time*1.2));
                fx.stamp(point,.087,.055,SUN,GOLD_PETAL,strength*.69,i,i*1.17);
            }
            return;
        }

        double brightness=cleared?1:.86;
        double cast=pulse(time,.025,.59,1.97)*brightness;
        double ward=pulse(time,.17,.58,1.57);
        double bloom=pulse(time,.25,.66,1.40)*brightness;
        fx.surface(surface,0,LOTUS_LIGHT,IVORY,ward*.68);
        lotusField(fx,base,.56+1.52*ease(.02,.49,time),.07+time*.17,cast);
        purificationPillars(fx,base,scale,time,ward*brightness);
        purificationSpiral(fx,base,scale,time,pulse(time,.09,.61,1.79)*brightness);
        if(chest!=null) fx.stamp(chest,.61,.56,IVORY,LOTUS,bloom,0,0);
        for(int i=0;i<11;i++) {
            double age=(time-.17-(i%5)*.088)/(1.12+(i%3)*.12);
            if(age<=0 || age>=1) continue;
            double phase=i*2.399963+age*1.86;
            double size=.14+(i%5)*.023;
            var point=base.add(polar(1.08+age*.54+(i%3)*.12,(.30+(i%5)*.36+age*1.68)*scale,phase));
            fx.stamp(point,size*1.48,size*.78,i%3==0?IVORY:SUN,GOLD_PETAL,pulse(age,0,.29,1)*.95,i,
                    phase+.38*Math.sin(age*9+i));
        }
        for(int i=0;i<36;i++) {
            double age=(time-.11-(i%12)*.061)/1.24;
            if(age<=0 || age>=1) continue;
            double angle=i*2.399963+age*.91;
            var point=base.add(polar(.34+(i%7)*.18+age*.24,(.17+(i%11)*.179+age*1.13)*scale,angle));
            double size=.021+(i%6)*.009;
            fx.stamp(point,size,size,i%4==0?GOLD:IVORY,GOLD_MOTE,pulse(age,0,.26,1)*.94,i,Math.sin(i)*.40);
        }
        if(cleared) purifiedStains(fx,base,scale,time);
    }

    private static void lotusField(FourArtsGeometry fx,Vec3 base,double radius,double angle,double strength) {
        fx.plane(base.add(0,.029,0),polar(radius,0,angle),polar(radius,0,angle+Math.PI*.5),
                SUN,LOTUS_FIELD,strength,0);
    }

    private static void purificationPillars(FourArtsGeometry fx,Vec3 base,double scale,double time,double strength) {
        if(strength<=.004) return;
        double height=3.85*scale*(.39+.61*ease(.05,.54,time));
        for(int i=0;i<24;i++) {
            double angle=i*2.399963;
            double radius=.22+.90*fract(i*.381966);
            Vec3 center=base.add(polar(radius,height*.5,angle));
            double width=.105+.091*fract(i*.627);
            int bearing=(int)Math.round(angle*32/TURN);
            fx.plane(center,UP.scale(height*(.46+.05*Math.sin(i))),polar(width,0,angle+Math.PI*.5),
                    i%3==0?GOLD:SUN,LIGHT_SHAFT,strength*(i%4==0?.76:.52),bearing);
        }
    }

    private static void purificationSpiral(FourArtsGeometry fx,Vec3 base,double scale,double time,double strength) {
        if(strength<=.004) return;
        double end=.13+.87*ease(.18,.78,time);
        double phase=-1.37+time*1.06;
        Vec3 previous=base.add(polar(1.11,.13*scale,phase));
        double previousHalf=.086*scale;
        for(int i=1;i<=108;i++) {
            double u=i/108.0;
            double p=u*end;
            double radius=1.11+.25*Math.sin(Math.PI*p)-.16*p;
            var point=base.add(polar(radius,(.13+p*3.40)*scale,phase+p*1.83*TURN));
            double half=(.086+.022*Math.sin(Math.PI*u))*scale;
            fx.quad(previous.add(0,-previousHalf,0),previous.add(0,previousHalf,0),
                    point.add(0,half,0),point.add(0,-half,0),(i-1)/108.0,u,IVORY,SCRIPT_BAND,strength,0);
            previous=point;
            previousHalf=half;
        }
    }

    private static void purifiedStains(FourArtsGeometry fx,Vec3 base,double scale,double time) {
        for(int i=0;i<6;i++) {
            double age=(time-.29-i*.075)/.95;
            if(age<=0 || age>=1) continue;
            double alpha=pulse(age,0,.31,.87);
            double peel=ease(0,.74,age);
            double angle=i*TURN/6+age*.56;
            double startHeight=.25+(i%3)*.36;
            DoubleFunction<Vec3> path=u->{
                double curl=Math.sin(Math.PI*u);
                double radius=.25+(.68+.26*curl)*peel*u;
                double y=(startHeight+.79*u+.59*peel+.15*curl)*scale;
                return base.add(polar(radius,y,angle+u*1.10));
            };
            fx.ribbon(curve(27,path),.15+(i%3)*.034,STAIN,ASH,alpha*.90,i);
            for(int j=0;j<10;j++) {
                double u=(j+.5)/10;
                Vec3 point=path.apply(u).add(polar(.042+.16*age,.13*Math.sin(i*12.7+j*2.1),i*2.2+j*2.399963));
                double size=.024+.039*(.5+.5*Math.sin(j*2.36+i));
                fx.stamp(point,size,size,0x5C2657,ASH_CHIP,alpha*(.71+.23*Math.sin(j+i)),j+i*10,j*.6);
            }
        }
    }

    static void gathering(FourArtsGeometry fx,Vec3 base,double t,boolean passive) {
        double strength=passive?pulse(t,0,.28,.94):ease(0,.62,t);
        if(strength<.004) return;
        double close=passive?ease(.27,.94,t):0;
        int strands=passive?2:7;
        if(!passive) fx.plane(base.add(0,.036,0),new Vec3(1.37,0,0),new Vec3(0,0,1.37),SPIRIT_BLUE,SIGIL,strength*.62,4);
        for(int i=0;i<strands;i++) {
            final int strand=i;
            double seed=i*TURN/strands;
            DoubleFunction<Vec3> path=u->{
                double r=Mth.lerp(u,passive?.89:1.60,.10)*(1-.87*close);
                double h=.29+(strand%3)*.41+Math.sin(seed+t*.82)*.16;
                double y=Mth.lerp(u,h,.91)+Math.sin(Math.PI*u)*(.22+(strand%2)*.19);
                return base.add(polar(r,y,seed+(passive?.49:.79)*t+u*(1.47+(strand%3)*.22)));
            };
            boolean gauze=!passive&&i%2==0;
            fx.ribbon(curve(27,path),passive?.038:gauze?.084:.043,i%3==0?SPIRIT_ICE:SPIRIT_BLUE,
                    gauze?GAUZE:LINE,strength*(passive?.59:.69),i);
            double phase=fract((t-i*.11)/(passive?1.3:1.65));
            double u=phase*phase*.40+phase*.60;
            for(int k=0;k<(passive?3:6);k++) {
                double v=u-k*.051;
                if(v>=0) fx.stamp(path.apply(v),.082+u*.045,.079+u*.035,SPIRIT_ICE,SPARK,strength*(1-k*.13),i,0);
            }
        }
        double breath=strength*(passive?.48:.72+.23*Math.sin(t*4.9));
        fx.stamp(base.add(0,.91,0),.48,.50,SPIRIT_BLUE,CORE,breath,0,0);
    }

    static void bloodCharge(FourArtsGeometry fx,Vec3 anchor,double t) {
        double charge=ease(0,.30,t)*(.81+.18*Math.sin(t*7.3));
        for(int i=0;i<4;i++) {
            double a=t*2.35+i*TURN/4;
            fx.stamp(anchor.add(polar(.11+(i%2)*.027,(i-1.5)*.054,a)),.28+(i%2)*.06,.22,RUBY,INK,charge*.70,i+2,a);
        }
        for(int i=0;i<12;i++) {
            double age=fract((t-i*.10)/1.19), a=i*2.39996+age*2.14;
            double r=(.31+(i%5)*.069)*(1-age);
            Vec3 p=anchor.add(polar(r,-.36*(1-age)+.04*Math.cos(a),a));
            fx.stamp(p,.072+(i%3)*.030,.034+(i%2)*.027,CARMINE,SHARD,charge*Math.sin(age*Math.PI)*.81,i,a);
        }
    }

    static void bloodHit(FourArtsGeometry fx,Vec3 hit,Vec3 aim,double t,boolean passive) {
        double intensity=pulse(t,-.04,.094,.64);
        if(intensity<.004) return;
        Vec3 side=aim.cross(UP).normalize();
        if(side.lengthSqr()<.01) side=new Vec3(1,0,0);
        Vec3 up=side.cross(aim).normalize();
        double push=ease(0,.41,t),size=passive?.46:1;
        Vec3 center=hit.subtract(aim.scale(.12*push*size));
        fx.stamp(center,(.27+.57*push)*size,(.33+.68*push)*size,RUBY,INK,intensity,10,-.27);
        for(int i=0;i<(passive?2:5);i++) {
            double angle=i*2.39996+.40;
            double spread=size*push*(.25+(i%3)*.14);
            Vec3 offset=side.scale(Math.cos(angle)*spread).add(up.scale(Math.sin(angle)*spread));
            Vec3 p=center.add(offset).subtract(aim.scale(push*(.05+i*.03)*size));
            double cloud=(.18+.36*push)*size;
            fx.stamp(p,cloud*1.25,cloud*.84,i%2==0?CARMINE:RUBY,INK,intensity*.76,12+i,angle);
        }
        for(int i=0;i<(passive?5:11);i++) {
            double angle=i*2.39996-.71,spread=push*size*(.38+(i%7)*.091);
            Vec3 radial=side.scale(Math.cos(angle)*spread).add(up.scale(Math.sin(angle)*spread));
            Vec3 p=center.add(radial).subtract(aim.scale(push*size*(.06+(i%5)*.08)));
            double chip=(.073+(i%4)*.026)*size;
            fx.stamp(p,chip*1.6,chip,CARMINE,SHARD,intensity*(1-push*.34),i,angle+push*.75);
        }
        fx.stamp(hit,.29*size,.28*size,CARMINE,SPARK,pulse(t,-.024,.038,.19),0,0);
    }

    private static List<Vec3> curve(int samples,DoubleFunction<Vec3> function) {
        List<Vec3> points=new ArrayList<>(samples);
        for(int i=0;i<samples;i++) points.add(function.apply(i/(double)(samples-1)));
        return points;
    }

    private static Vec3 polar(double r,double y,double angle) { return new Vec3(r*Math.cos(angle),y,r*Math.sin(angle)); }
    private static double fract(double x) { return x-Math.floor(x); }
    private static double ease(double a,double b,double x) {
        double u=Mth.clamp((x-a)/(b-a),0,1);
        return u*u*(3-2*u);
    }
    private static double pulse(double x,double a,double b,double c) { return ease(a,b,x)*(1-ease(b,c,x)); }
}
