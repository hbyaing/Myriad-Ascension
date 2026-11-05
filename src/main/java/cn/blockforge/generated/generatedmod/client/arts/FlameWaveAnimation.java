package cn.blockforge.generated.generatedmod.client.arts;

import cn.blockforge.generated.generatedmod.arts.FlameWave;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static cn.blockforge.generated.generatedmod.arts.FlameWave.HALF_ANGLE;
import static cn.blockforge.generated.generatedmod.arts.FlameWave.RANGE;
import static cn.blockforge.generated.generatedmod.client.arts.FourArtsGeometry.*;

final class FlameWaveAnimation {
    static final int LIFETIME = 80;
    private static final int COPPER=0xFF6E18, GOLD=0xFFB03D, ASH_TINT=0x4D2E26;
    private static final int FLOOR_ANGLES=26, FLOOR_RINGS=14;
    final long tick;
    final Vec3 origin;
    private final Vec3 forward, side;
    private final double[][] floor = new double[FLOOR_ANGLES][FLOOR_RINGS];

    FlameWaveAnimation(ClientLevel level, AbstractClientPlayer caster, Vec3 origin, float yaw, long tick) {
        this.tick=tick;
        this.origin=origin;
        forward=Vec3.directionFromRotation(0,yaw);
        side=new Vec3(-forward.z,0,forward.x);
        // One world-space floor sample per cell. The shader clips each cell to actual upward scene depth.
        for(int a=0;a<FLOOR_ANGLES;a++) for(int r=0;r<FLOOR_RINGS;r++) {
            Vec3 point=point((r+.5)*RANGE/FLOOR_RINGS,0,angle((a+.5)/FLOOR_ANGLES));
            var hit=level.clip(new ClipContext(point.add(0,.93,0),point.add(0,-1.55,0),
                    ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,caster));
            floor[a][r]=hit.getType()==HitResult.Type.BLOCK && hit.getDirection()==Direction.UP
                    ?hit.getLocation().y-origin.y:Double.NaN;
        }
    }

    void render(FourArtsGeometry fx,Vec3 hand,double age) {
        if(hand!=null) ignition(fx,hand,age);
        if(age<=FlameWave.RELEASE || age>=3.98) return;
        double front=FlameWave.front(age);
        ground(fx,age,front);
        remnants(fx,age);
    }

    FlameWaveVolume.Sample volume(Vec3 camera,double age) {
        double strength=ease(.36,.57,age)*(1-ease(1.14,1.54,age));
        if(strength<=.004) return null;
        double front=FlameWave.front(age);
        float[] heights=new float[8];
        for(int i=0;i<heights.length;i++) heights[i]=(float)floorHeight(front-.26,angle(i/7.0));
        return new FlameWaveVolume.Sample(origin.subtract(camera),forward,(float)age,(float)front,(float)strength,heights);
    }

    private static void ignition(FourArtsGeometry fx,Vec3 hand,double age) {
        double strength=pulse(age,.02,.29,.52);
        if(strength<=.004) return;
        fx.stamp(hand,.48,.54,GOLD,CORE,strength*.94,7,age);
        fx.stamp(hand,.61,.32,COPPER,FLAME_TONGUE,strength*.86,13,.2);
        for(int i=0;i<4;i++) {
            List<Vec3> path=new ArrayList<>(21);
            for(int j=0;j<=20;j++) {
                double u=j/20.0;
                double bearing=i*Math.PI*.5+age*9.1+u*2.9;
                double radius=.62*Math.pow(1-u,1.38);
                path.add(hand.add(radius*Math.cos(bearing),-.43*(1-u)+Math.sin(u*5.1)*.09,radius*Math.sin(bearing)));
            }
            fx.ribbon(path,.047,i%2==0?COPPER:GOLD,ICE_RIBBON,strength*.68,i);
        }
        for(int i=0;i<18;i++) {
            double bearing=i*2.399963+age*6;
            double radius=(.13+hash(i)*.52)*(1-ease(.08,.37,age))+.10;
            Vec3 mote=hand.add(radius*Math.cos(bearing),(hash(i+37)-.55)*radius,radius*Math.sin(bearing));
            fx.stamp(mote,.065,.046,i%3==0?GOLD:COPPER,FLAME_SPARK,strength*.84,i,bearing);
        }
    }

    private void ground(FourArtsGeometry fx,double age,double front) {
        for(int a=0;a<FLOOR_ANGLES;a++) for(int r=0;r<FLOOR_RINGS;r++) {
            double floorY=floor[a][r];
            if(!Double.isFinite(floorY)) continue;
            double inner=r*RANGE/FLOOR_RINGS, outer=(r+1)*RANGE/FLOOR_RINGS;
            if(inner>front+1.35) continue;
            double u=a/(double)FLOOR_ANGLES,v=(a+1)/(double)FLOOR_ANGLES;
            double left=angle(u),right=angle(v),height=floorY+.025;
            fx.quad(point(inner,height,left),point(outer,height,left),point(outer,height,right),point(inner,height,right),
                    u,v,2*inner/RANGE-1,2*outer/RANGE-1,GOLD,FLAME_GROUND,age/4,0);
        }
    }

    private void remnants(FourArtsGeometry fx,double age) {
        for(int i=0;i<96;i++) {
            double a=angle(hash(i+37)), r=Mth.lerp(hash(i+90),.65,6.32);
            double elapsed=age-FlameWave.arrival(r)-hash(i+46)*.12;
            if(elapsed<0 || elapsed>1.47) continue;
            double seed=hash(i+132), drift=.40+seed*1.22;
            double fade=pulse(elapsed,0,.075,.59+seed*.78);
            double rise=.14+elapsed*(1.10+seed*1.9)-elapsed*elapsed*.74;
            double radius=r+elapsed*drift, bearing=a+elapsed*(hash(i+58)-.5)*.24;
            double height=floorHeight(r,a)+rise, size=.021+.098*seed*seed;
            Vec3 mote=point(radius,height,bearing);
            if(i%4==0 && elapsed<.56) {
                fx.ribbon(List.of(point(radius-.18,height-.10-.18*seed,bearing),mote),
                        size*.24,COPPER,ICE_RIBBON,fade*.79,i);
            }
            fx.stamp(mote,size*(1.28+seed),size,GOLD,FLAME_SPARK,fade*.93,i,a-.55);
        }
        for(int i=0;i<22;i++) {
            double r=Mth.lerp(hash(i+255),1.17,6.30), a=angle(hash(i+382))*.95;
            double elapsed=age-FlameWave.arrival(r)-.13;
            if(elapsed<0 || elapsed>1.5) continue;
            double grow=ease(0,1.5,elapsed);
            Vec3 smoke=point(r+.29*grow,floorHeight(r,a)+.15+grow*(.77+hash(i+173)*.48),a+grow*.07);
            fx.stamp(smoke,.36+grow*.87,.55+grow*.72,ASH_TINT,FLAME_SMOKE,pulse(elapsed,0,.35,1.5)*.36,i,a*.16);
        }
    }

    private double floorHeight(double radius,double bearing) {
        double a=Mth.clamp((bearing/HALF_ANGLE+1)*.5*FLOOR_ANGLES-.5,0,FLOOR_ANGLES-1.0001);
        double r=Mth.clamp(radius/RANGE*FLOOR_RINGS-.5,0,FLOOR_RINGS-1.0001);
        int left=(int)a,near=(int)r;
        return Mth.lerp(r-near,Mth.lerp(a-left,height(left,near),height(left+1,near)),
                Mth.lerp(a-left,height(left,near+1),height(left+1,near+1)));
    }

    private double height(int angle,int ring) {
        double value=floor[angle][ring];
        return Double.isFinite(value)?value:0;
    }

    private Vec3 point(double radius,double y,double a) {
        return origin.add(forward.scale(radius*Math.cos(a))).add(side.scale(radius*Math.sin(a))).add(0,y,0);
    }

    private static double angle(double fraction) {
        return (2*fraction-1)*HALF_ANGLE;
    }

    private static double hash(int seed) {
        double value=Math.sin(seed*127.1+21.7)*43758.5453;
        return value-Math.floor(value);
    }

    private static double ease(double start,double end,double t) {
        double value=Mth.clamp((t-start)/(end-start),0,1);
        return value*value*(3-2*value);
    }

    private static double pulse(double t,double start,double peak,double end) {
        return ease(start,peak,t)*(1-ease(peak,end,t));
    }
}
