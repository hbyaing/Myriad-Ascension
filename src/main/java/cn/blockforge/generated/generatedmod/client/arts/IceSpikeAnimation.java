package cn.blockforge.generated.generatedmod.client.arts;

import cn.blockforge.generated.generatedmod.arts.IceSpikeVolley;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.IceSpikePacket;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.blockforge.generated.generatedmod.client.arts.FourArtsGeometry.*;

final class IceSpikeAnimation {
    private static final int BLUE = 0x529FFF, ICE = 0x76C8FF, FROST = 0xB5E7FF;
    private static final double GOLDEN_ANGLE = 2.399963;
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final double[][] OUTLINE = {{1.04,.12},{.56,.94},{-.39,1.02},{-.86,.48},{-.72,-.66},{-.10,-.98},{.73,-.68}};
    private static final int[] FACES = {0x579EEB,0x80C4FF,0x336ECC,0x1C51A1,0x2E70C9,0x549EE6,0x9CD4FF};
    private static final double[][] LANCE = {{0,.46},{.086,.83},{.217,1.04},{.337,.89},{.58,.54},{.82,.22},{1,0}};
    private static final double[][] PLATE = {{0,.53},{.16,.96},{.43,.90},{1,0}};
    private static final double[][] SHARD = {{0,.08},{.15,.68},{.33,1},{.58,.69},{1,0}};

    private final Wake[] shots = new Wake[IceSpikeVolley.FORMATION.size()];
    private final Map<BlockPos, Floor> ground = new LinkedHashMap<>();
    private Vec3 base, forward;
    private int age;
    private long received;

    IceSpikeAnimation() {
        for (int i = 0; i < shots.length; i++) shots[i] = new Wake();
    }

    boolean expired(long now) {
        return now - received > 38;
    }

    void accept(ClientLevel level, IceSpikePacket packet, long tick) {
        if (base != null && packet.age() <= age) return;
        base = packet.base();
        forward = packet.forward();
        age = packet.age();
        received = tick;
        for (var shot : packet.samples()) {
            Wake wake = shots[shot.index()];
            wake.previous = wake.current;
            wake.current = shot;
            wake.tick = tick;
            if (shot.flight() <= 0) continue;
            if (wake.history.size() == 16) wake.history.removeLast();
            if (wake.previous != null) wake.history.addFirst(wake.previous.tip());
            if (shot.flight() < 3) continue;
            // Each swept flight segment seeds stable ground cells under its actual curved path.
            Vec3 path = shot.tip().subtract(wake.previous == null ? base : wake.previous.tip());
            Vec3 side = UP.cross(path).normalize();
            double strength = shot.index() == 0 ? 1 : .63;
            frost(level, shot.tip(), tick, strength);
            frost(level, shot.tip().add(side.scale(.72)), tick, strength * .61);
            frost(level, shot.tip().subtract(side.scale(.72)), tick, strength * .61);
        }
    }

    private void frost(ClientLevel level, Vec3 point, long tick, double strength) {
        var entity = net.minecraft.client.Minecraft.getInstance().player;
        if (entity == null) return;
        var hit = level.clip(new ClipContext(point.add(0,.28,0), point.add(0,-4.7,0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != net.minecraft.core.Direction.UP) return;
        ground.merge(hit.getBlockPos(), new Floor(hit.getLocation(), tick, strength),
                (a,b) -> b.strength > a.strength ? new Floor(b.point, a.tick, b.strength) : a);
    }

    void render(FourArtsGeometry fx, Vec3 camera, double now) {
        if (base.distanceToSqr(camera) > 56 * 56) return;
        double castAge = (age + clamp(now - received)) / 20;
        gather(fx, castAge);
        for (int i = 0; i < shots.length; i++) {
            Wake wake = shots[i];
            if (wake.current == null) continue;
            double since = now - wake.tick;
            if (since > 21) continue;
            double blend = clamp(since);
            var shot = wake.current;
            Vec3 tip = wake.previous == null ? shot.tip() : wake.previous.tip().lerp(shot.tip(), blend);
            Vec3 axis = wake.previous == null ? shot.axis() : wake.previous.axis().lerp(shot.axis(), blend).normalize();
            double flight = shot.flight() - 1 + blend;
            var form = IceSpikeVolley.FORMATION.get(i);
            double visible = smooth(-12, -3, flight) * (1 - smooth(15, 20, flight)) * (1 - smooth(2, 11, since));
            double growth = smooth(1, 8, flight);
            double length = form.length() * (.30 + .70 * growth);
            double radius = form.width() * (.43 + .57 * smooth(0, 6, flight));
            if (!shot.impact() || since < 1) {
                double clearance = smooth(.48,.88,tip.distanceTo(camera));
                crystal(fx, tip, axis, length, radius, visible * clearance, i, false);
            }
            if (flight > 0 && visible > .004) {
                wake(fx, wake, tip, axis, i, length, radius, flight, castAge, visible);
            } else {
                fx.stamp(tip, .48, .72, ICE, ICE_FOG, pulse(flight, -11, -5, 1) * .42, i + 91, form.bearing() * .12);
            }
            if (shot.impact()) impact(fx, shot.tip(), shot.axis(), (since - 1) / 20, i);
        }
        for (var entry : ground.entrySet()) {
            Floor floor = entry.getValue();
            double elapsed = (now - floor.tick) / 20;
            double opacity = pulse(elapsed, -.025, .16, 1.30) * floor.strength;
            if (opacity <= .004) continue;
            fx.plane(floor.point.add(0,.028,0), new Vec3(.85,0,0), new Vec3(0,0,.85),
                    ICE, ICE_GROUND, opacity, entry.getKey().hashCode());
        }
    }

    private void gather(FourArtsGeometry fx, double time) {
        double strength = pulse(time,.025,.33,.74);
        if (strength <= .004) return;
        Vec3 side = new Vec3(-forward.z,0,forward.x);
        for (int i = 0; i < 7; i++) {
            double angle = i * GOLDEN_ANGLE + time * 1.6;
            double radius = 1.47 - .20 * smooth(.05,.59,time);
            List<Vec3> arc = new ArrayList<>();
            for (int j = 0; j <= 20; j++) {
                double u = j / 20.0, bearing = angle + u * 1.21;
                double r = radius - .22 * u;
                arc.add(base.add(forward.scale(r*Math.cos(bearing))).add(side.scale(r*Math.sin(bearing)))
                        .add(0,.37+(i%5)*.32+.38*u,0));
            }
            fx.ribbon(arc,.062,BLUE,ICE_RIBBON,strength*.57,i);
            Vec3 flake = base.add(polar(radius,.40+(i%4)*.35+time*.30,angle+.41));
            fx.stamp(flake,.11+(i%3)*.021,.11+(i%3)*.021,FROST,ICE_SNOW,strength*.74,i,angle*.48);
        }
    }

    private static void wake(FourArtsGeometry fx, Wake wake, Vec3 tip, Vec3 axis, int i, double length,
                             double width, double flight, double time, double visibility) {
        double strength = visibility * smooth(.4, 6.5, flight);
        double travel = clamp(flight / 16);
        List<Vec3> path = new ArrayList<>(wake.history.size() + 1);
        path.add(tip);
        path.addAll(wake.history);
        double available = 0;
        for (int j = 1; j < path.size(); j++) available += path.get(j-1).distanceTo(path.get(j));
        double heel = Math.min(length*.78, available*.73);
        double wakeLength = Math.min((2.85 + length*.53)*smooth(0,10,flight), available-heel);
        if (wakeLength < .10) return;
        Vec3 rear = tip.subtract(axis.scale(heel));
        Vec3 side = UP.cross(axis).normalize();
        Vec3 vertical = axis.cross(side).normalize();
        List<Vec3> trail = new ArrayList<>(25);
        for (int j = 0; j <= 24; j++) {
            double u = j / 24.0;
            Vec3 point = backtrack(path, heel + (1-u)*wakeLength);
            trail.add(point.lerp(rear.add(axis.scale(.14)), smooth(.69,1,u)*.65));
        }
        // One continuous volume follows the actual curved path, with a thin colder core.
        double fogWidth = (.26 + width*.70 + travel*.09) * (i==0?1.16:1);
        fx.ribbon(trail,fogWidth,BLUE,ICE_WAKE,strength*.86,i*3);
        for (int j = 0; j < 2; j++) {
            List<Vec3> strand = new ArrayList<>(25);
            double shift = (j-.5)*width*.78;
            for (int k = 0; k <= 24; k++) {
                double u = k / 24.0;
                strand.add(trail.get(k).add(vertical.scale(shift * (.22+.78*u)+.034*Math.sin(u*6.3+i+j)))
                        .add(side.scale(shift*.34)));
            }
            fx.ribbon(strand,width*.39,j==1?FROST:ICE,ICE_WAKE,strength*.37,i*3+j+1);
        }
        for (int j = 0; j < 9; j++) {
            double u = (j+.43)/9, radius = .08+.26*u;
            int seed = i*13+j;
            Vec3 point = trail.get((int)((1-u)*24)).add(vertical.scale(Math.sin(seed*2.1)*radius))
                    .add(side.scale(Math.cos(seed*2.3)*radius));
            double size = .026+.083*Math.pow(.5+.5*Math.cos(seed*2.29),3);
            fx.stamp(point,size,size,FROST,ICE_GRAIN,strength*(1-u*.54),seed,seed*.27+time*.9);
        }
        for (int j = 0; j < 14; j++) {
            double u = (j+.45)/14;
            int key = i*17+j;
            double swirl = key*GOLDEN_ANGLE+time*(2+(j%4)*.41), spread = .19+.49*(1-u)+.17*travel;
            Vec3 center = trail.get((int)(u*24)).add(axis.scale(Math.sin(key*1.67)*.31))
                    .add(vertical.scale(Math.cos(swirl)*spread)).add(side.scale(Math.sin(swirl)*spread));
            double size = .055+.178*Math.pow(.5+.5*Math.sin(key*3.7),3);
            fx.stamp(center,size,size,FROST,ICE_SNOW,strength*(.76+.24*Math.sin(key*.71+time*8.7)),key,swirl*.56);
        }
    }

    private static Vec3 backtrack(List<Vec3> path, double distance) {
        for (int i = 1; i < path.size(); i++) {
            Vec3 a = path.get(i-1), b = path.get(i);
            double step = a.distanceTo(b);
            if (step > distance) return a.lerp(b, distance / step);
            distance -= step;
        }
        return path.get(path.size()-1);
    }

    private static void impact(FourArtsGeometry fx, Vec3 point, Vec3 direction, double age, int lance) {
        double strength = pulse(age,0,.075,.83) * (lance == 0 ? 1 : .62);
        if (strength <= .004) return;
        double grow = smooth(0,.25,age);
        double scatter = 1-Math.pow(1-clamp(age/.64),3);
        double scale = lance == 0 ? 1 : .70;
        fx.stamp(point,1.14*scale,1.22*scale,FROST,CORE,pulse(age,0,.043,.27)*.74*scale,lance,0);
        for (int i = 0; i < 9; i++) {
            double angle = i*GOLDEN_ANGLE + lance*.81;
            Vec3 gas = point.add(polar((.24+(i%3)*.19)*grow*scale,(-.42+(i%5)*.18+.30*grow)*scale,angle));
            fx.stamp(gas,(.60+.59*grow)*scale,(1.02+.53*grow)*scale,ICE,ICE_FOG,strength*.59,i+54,angle*.13);
        }
        for (int i = 0; i < 21; i++) {
            double angle = i*GOLDEN_ANGLE+lance*.81;
            double radius = (.30+(i%7)*.109)*scatter*scale;
            double y = ((i%5)*.083+Math.sin(Math.PI*scatter)*(.26+(i%6)*.105)-.68*scatter*scatter)*scale;
            Vec3 axis = polar(.76,.53*Math.cos(scatter*2.7+i*.41),angle+scatter*.86).normalize();
            Vec3 tip = point.add(direction.scale(.47*scatter*scale)).add(polar(radius,y,angle));
            crystal(fx,tip,axis,(.11+(i%4)*.083)*scale,(.029+(i%3)*.021)*scale,strength*.89,i+lance,true);
            Vec3 flake = point.add(polar(radius*1.53,y*.71,angle+.41));
            double size = (.066+(i%6)*.022)*scale;
            fx.stamp(flake,size,size,FROST,ICE_SNOW,strength*.87,i+121,angle+scatter*.88);
        }
    }

    private static void crystal(FourArtsGeometry fx, Vec3 tip, Vec3 axis, double length, double radius,
                                double strength, int seed, boolean fragment) {
        if (strength <= .004 || axis.lengthSqr() < .1) return;
        Vec3 side = axis.cross(Math.abs(axis.y) > .95 ? new Vec3(0,0,1) : UP).normalize();
        Vec3 up = side.cross(axis);
        double angle = .12+Math.sin(seed*8.13)*(fragment?.71:.23);
        prism(fx,tip,axis,up,side,length,radius,angle,fragment?SHARD:LANCE,strength,seed);
        if (fragment) return;
        for (int plate = 0; plate < 3; plate++) {
            double a = angle+plate*2.02+.51;
            Vec3 edge = up.scale(Math.cos(a)).add(side.scale(Math.sin(a)));
            Vec3 end = tip.subtract(axis.scale(length*(.40+plate*.076))).add(edge.scale(radius*(.82+plate*.087)));
            prism(fx,end,axis,up,side,length*(.49-.053*plate),radius*(.39-.033*plate),a,PLATE,strength*.97,seed*3+plate+9);
        }
    }

    private static void prism(FourArtsGeometry fx, Vec3 tip, Vec3 axis, Vec3 up, Vec3 side, double length,
                              double radius, double angle, double[][] profile, double strength, int seed) {
        int sides = OUTLINE.length;
        Vec3[][] rows = new Vec3[profile.length][sides];
        double c = Math.cos(angle), s = Math.sin(angle);
        for (int row = 0; row < rows.length; row++) {
            for (int i = 0; i < sides; i++) {
                double facet = i*2.7+seed;
                double r = radius*profile[row][1]*(1+.075*Math.sin(facet+row*.76));
                double cut = row==0||row==rows.length-1 ? 0 : length*.032*Math.sin(facet+row*1.71);
                double y = OUTLINE[i][0], z = OUTLINE[i][1];
                rows[row][i] = tip.add(axis.scale(-length*(1-profile[row][0])+cut))
                        .add(up.scale(r*(y*c-z*s))).add(side.scale(r*(z*c+y*s)));
            }
        }
        for (int row = 1; row < rows.length; row++) {
            for (int i = 0; i < sides; i++) {
                int j = (i+1)%sides;
                fx.quad(rows[row-1][i],rows[row-1][j],rows[row][j],rows[row][i],
                        profile[row-1][0],profile[row][0],FACES[i],ICE_CRYSTAL,strength,seed*7+i);
            }
        }
        Vec3 heel = tip.subtract(axis.scale(length));
        for (int i = 0; i < sides; i++) {
            fx.quad(heel,heel,rows[0][(i+1)%sides],rows[0][i],0,.022,FACES[i],ICE_CRYSTAL,strength*.85,seed*7+i);
        }
    }

    private static Vec3 polar(double radius, double y, double angle) {
        return new Vec3(radius*Math.cos(angle),y,radius*Math.sin(angle));
    }

    private static double clamp(double x) {
        return Math.max(0, Math.min(1, x));
    }

    private static double smooth(double start, double end, double x) {
        double t = clamp((x-start)/(end-start));
        return t*t*(3-2*t);
    }

    private static double pulse(double x, double start, double crest, double end) {
        return smooth(start,crest,x)*(1-smooth(crest,end,x));
    }

    private record Floor(Vec3 point, long tick, double strength) {}

    private static final class Wake {
        final ArrayDeque<Vec3> history = new ArrayDeque<>();
        IceSpikePacket.Sample previous, current;
        long tick;
    }
}
