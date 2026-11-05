package cn.blockforge.generated.generatedmod.client.arts;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class FourArtsGeometry {
    static final int LINE=0, DEW=1, INK=2, SPARK=3, SIGIL=4, GAUZE=5, CORE=6, SHARD=7, AIR=8, RIPPLE=9;
    private final Vec3 camera, right, up;
    private final List<Quad> quads=new ArrayList<>();
    boolean refracts;
    int materials;

    FourArtsGeometry(Vec3 camera,Vec3 right,Vec3 up) {
        this.camera=camera;
        this.right=right;
        this.up=up;
    }

    void stamp(Vec3 center,double height,double width,int rgb,int kind,double strength,int seed,double angle) {
        double c=Math.cos(angle),s=Math.sin(angle);
        plane(center,up.scale(c).add(right.scale(s)).scale(height*.5),
                right.scale(c).subtract(up.scale(s)).scale(width*.5),rgb,kind,strength,seed);
    }

    void plane(Vec3 center,Vec3 along,Vec3 across,int rgb,int kind,double strength,int seed) {
        quad(center.subtract(along).subtract(across),center.subtract(along).add(across),
                center.add(along).add(across),center.add(along).subtract(across),0,1,rgb,kind,strength,seed);
    }

    void ribbon(List<Vec3> points,double width,int rgb,int kind,double strength,int seed) {
        if(strength<=.004 || points.size()<2) return;
        double[] distance=new double[points.size()];
        for(int i=1;i<points.size();i++) distance[i]=distance[i-1]+points.get(i).distanceTo(points.get(i-1));
        double length=distance[distance.length-1];
        if(length<.002) return;
        Vec3 previousA=null,previousB=null;
        for(int i=0;i<points.size();i++) {
            Vec3 point=points.get(i);
            Vec3 tangent=points.get(Math.min(i+1,points.size()-1)).subtract(points.get(Math.max(0,i-1))).normalize();
            Vec3 side=tangent.cross(camera.subtract(point).normalize());
            side=(side.lengthSqr()>.0001?side.normalize():right)
                    .scale(width*(.18+.82*Math.pow(Math.sin(Math.PI*distance[i]/length),.49)));
            Vec3 a=point.subtract(side),b=point.add(side);
            if(i>0) quad(previousA,previousB,b,a,distance[i-1]/length,distance[i]/length,rgb,kind,strength,seed);
            previousA=a;
            previousB=b;
        }
    }

    private void quad(Vec3 a,Vec3 b,Vec3 c,Vec3 d,double u,double v,int rgb,int kind,double strength,int seed) {
        if(strength<=.004) return;
        if(kind==DEW) refracts=true;
        materials|=1<<kind;
        quads.add(new Quad(a,b,c,d,(float)u,(float)v,rgb,kind,
                Mth.clamp((int)(strength*255),1,255),seed&31,a.add(c).scale(.5).distanceToSqr(camera)));
    }

    BufferBuilder.RenderedBuffer bake(BufferBuilder buffer) {
        if(quads.isEmpty()) return null;
        quads.sort(Comparator.comparingDouble(Quad::depth).reversed());
        buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        for(var q:quads) {
            vertex(buffer,q,q.a,q.u,-1);
            vertex(buffer,q,q.b,q.u,1);
            vertex(buffer,q,q.c,q.v,1);
            vertex(buffer,q,q.d,q.v,-1);
        }
        return buffer.end();
    }

    private void vertex(BufferBuilder buffer,Quad q,Vec3 point,float u,float v) {
        Vec3 p=point.subtract(camera);
        buffer.vertex(p.x,p.y,p.z).uv(q.kind*2+u,q.seed*4+v)
                .color(q.rgb>>16&255,q.rgb>>8&255,q.rgb&255,q.alpha).endVertex();
    }

    private record Quad(Vec3 a,Vec3 b,Vec3 c,Vec3 d,float u,float v,int rgb,int kind,int alpha,int seed,double depth) {}
}
