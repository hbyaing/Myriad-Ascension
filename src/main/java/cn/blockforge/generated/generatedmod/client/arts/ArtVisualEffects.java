package cn.blockforge.generated.generatedmod.client.arts;

import cn.blockforge.generated.generatedmod.ArtCatalog;
import cn.blockforge.generated.generatedmod.arts.ArtVfxCatalog;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.ArtVisualPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import static cn.blockforge.generated.generatedmod.network.packet.cultivation.ArtVisualPacket.*;

/** Range boundary, main form and hit debris use independent library materials. No full screen glow. */
public final class ArtVisualEffects {
    private static ClientLevel currentLevel;
    private static long budgetTick;
    private static int emitted;
    public static void accept(ArtVisualPacket p) {
        // Flight has dedicated continuous sword/huahong renderers. Also ignore
        // old flight packets without affecting any combat art's particle path.
        if (p.art().equals("art_spirit_shield") || p.art().equals("art_sword_flight")) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        if (currentLevel != level || budgetTick != level.getGameTime()) {
            currentLevel = level; budgetTick = level.getGameTime(); emitted = 0;
        }
        var r = ArtVfxCatalog.recipe(p.art());
        Vec3 direction = p.end().subtract(p.start()).normalize();
        switch (p.mode()) {
            case ARC -> {
                double angle = Math.toRadians(ArtCatalog.attackProfile(p.art()).angleDegrees());
                Vec3 flat = new Vec3(direction.x, 0, direction.z).normalize();
                if (flat.lengthSqr() < .01) flat = new Vec3(0,0,1);
                double heading = Math.atan2(flat.z, flat.x);
                int n = p.art().equals("art_sword_sweep") ? 11 : 7;
                for (int i = 0; i < n; i++) {
                    double a = heading - angle / 2 + angle * i / (n - 1);
                    double lift = p.art().equals("art_sword_sweep") ? 0.22D * Math.sin(i * 0.9D) : 0.0D;
                    Vec3 point = p.start().add(Math.cos(a) * p.size(), lift, Math.sin(a) * p.size());
                    spawn(level,r.body(),point,Vec3.ZERO,1.2,1.0,9,r.color(),false,Vec3.ZERO,(float)(a-heading));
                    if (p.art().equals("art_sword_sweep")) {
                        spawn(level,r.accent(),point.add(0,0.18,0),Vec3.ZERO,.38,.6,11,r.color(),false,Vec3.ZERO,(float)(a-heading+.35));
                    }
                }
            }
            case PROJECTILE -> {
                Vec3 center = p.start().lerp(p.end(), .5);
                spawn(level,r.body(),center,Vec3.ZERO,p.size(),Math.max(.8,p.start().distanceTo(p.end())+.4),6,r.color(),false,direction,0);
                spawn(level,r.accent(),p.start(),direction.scale(-.03),.4,.4,9,r.color(),false,Vec3.ZERO,0);
                if (p.art().equals("art_sword_pierce")) {
                    spawn(level,r.accent(),center,direction.scale(.02),.62,.62,12,0xFFFFFF,false,direction,0);
                    spawn(level,r.debris(),p.start().add(0,.25,0),direction.scale(.04),.42,.9,10,r.color(),false,direction,0);
                }
            }
            case IMPACT -> {
                spawn(level,r.accent(),p.start(),Vec3.ZERO,p.size(),p.size(),12,0xFFFFFF,false,Vec3.ZERO,0);
                for (int i=0;i<6;i++) {
                    double a=i*Math.PI/3;
                    spawn(level,r.debris(),p.start(),new Vec3(Math.cos(a)*.06,.04,Math.sin(a)*.06),.22,.35,14,r.color(),false,Vec3.ZERO,(float)a);
                }
            }
            case BOUNDARY -> {
                for (int i=0;i<32;i++) {
                    double a=i*Math.PI/16;
                    spawn(level,r.accent(),p.start().add(Math.cos(a)*p.size(),.06,Math.sin(a)*p.size()),Vec3.ZERO,.32,.32,14,r.color(),true,Vec3.ZERO,0);
                }
            }
            case AREA -> {
                if(p.art().equals("art_thunder")) {
                    for(int i=0;i<8;i++) {
                        double a=i*Math.PI/4;
                        Vec3 point=p.start().add(Math.cos(a)*p.size()*.6,2.6,Math.sin(a)*p.size()*.6);
                        spawn(level,r.body(),point,Vec3.ZERO,.8,5.2,10,r.color(),false,new Vec3(0,1,0),0);
                        spawn(level,r.accent(),point.add(0,-2.5,0),Vec3.ZERO,1.0,1.0,12,0xFFFFFF,true,Vec3.ZERO,0);
                    }
                } else if(p.art().equals("art_five_elements")) {
                    int element=p.phase()%5;
                    for(int i=0;i<10;i++) {
                        double a=i*Math.PI/5+element*.2;
                        spawn(level,ArtVfxCatalog.ELEMENTS.get(element),p.start().add(Math.cos(a)*p.size()*.7,.12,Math.sin(a)*p.size()*.7),Vec3.ZERO,
                                1.3,1.3,14,ArtVfxCatalog.ELEMENT_COLORS[element],true,Vec3.ZERO,(float)a);
                    }
                    spawn(level,r.accent(),p.start().add(0,.18,0),Vec3.ZERO,2,2,14,0xFFFFFF,true,Vec3.ZERO,0);
                } else if (p.art().equals("art_sword_array") || p.art().equals("art_myriad_swords")) {
                    int count = p.art().equals("art_myriad_swords") ? 12 : 6;
                    double radius = p.art().equals("art_myriad_swords") ? p.size() * .82D : p.size() * .68D;
                    double y = p.art().equals("art_myriad_swords") ? .24D : .18D;
                    for (int i = 0; i < count; i++) {
                        double a = i * Math.PI * 2D / count + p.phase() * .12D;
                        Vec3 point = p.start().add(Math.cos(a) * radius, y + (i % 2) * .12D, Math.sin(a) * radius);
                        spawn(level,r.body(),point,Vec3.ZERO,p.art().equals("art_myriad_swords") ? .72 : .9,1.6,16,r.color(),false,
                                new Vec3(0,1,0),(float)a);
                    }
                    spawn(level,r.accent(),p.start().add(0,y,0),Vec3.ZERO,p.size()*.55D,p.size()*.55D,14,0xFFFFFF,true,Vec3.ZERO,0);
                } else {
                    spawn(level,r.body(),p.start().add(0,.12,0),Vec3.ZERO,p.size()*1.4,p.size()*1.4,14,r.color(),true,Vec3.ZERO,0);
                }
            }
            case AURA -> {
                spawn(level,r.body(),p.start(),Vec3.ZERO,p.size(),p.size(),16,r.color(),false,Vec3.ZERO,0);
                int orbitCount = p.art().equals("art_sword_guard") ? 6 : 8;
                for(int i=0;i<orbitCount;i++) {
                    double a=i*Math.PI/4;
                    if (p.art().equals("art_sword_guard")) a=i*Math.PI/3 + .12D;
                    Vec3 point=p.start().add(Math.cos(a)*p.size()*.55,-.45,Math.sin(a)*p.size()*.55);
                    Vec3 velocity=new Vec3(0,.025,0);
                    spawn(level,r.accent(),point,velocity,.25,.4,16,r.color(),false,Vec3.ZERO,(float)a);
                    if (p.art().equals("art_sword_guard")) spawn(level,r.debris(),point.add(0,.22,0),Vec3.ZERO,.34,.75,14,r.color(),false,new Vec3(0,1,0),(float)a);
                }
            }
            case TRAIL -> {
                int n=Math.min(8,Math.max(1,(int)Math.ceil(p.start().distanceTo(p.end())*2)));
                for(int i=0;i<n;i++) spawn(level,r.debris(),p.start().lerp(p.end(),i/(double)n),Vec3.ZERO,p.size(),p.size(),8,r.color(),false,Vec3.ZERO,0);
            }
            default -> {}
        }
    }
    private static void spawn(ClientLevel level,String material,Vec3 position,Vec3 velocity,double w,double h,int life,int color,
                              boolean ground,Vec3 direction,float roll) {
        if(emitted++ >= 384) return;
        ArtMaterialParticle.spawn(level,material,position,velocity,w,h,life,color,ground,direction,roll);
    }
    private ArtVisualEffects() {}
}
