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
    static final int LINE=0, LEAF=1, INK=2, SPARK=3, SIGIL=4, GAUZE=5, CORE=6, SHARD=7, AIR=8, RECOVERY=9, ASH=10;
    static final int SILVER=11;
    static final int RISE=0, WASH=1, CHEST=2;
    private final Vec3 camera, right, up;
    private final List<Quad> quads=new ArrayList<>();
    private final List<Surface> surfaces=new ArrayList<>();
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

    void surface(List<Vec3> points,double crest,int mode,int rgb,double strength) {
        if(points.isEmpty() || strength<=.004) return;
        double bottom=Double.POSITIVE_INFINITY,top=Double.NEGATIVE_INFINITY;
        for(var point:points) {
            bottom=Math.min(bottom,point.y);
            top=Math.max(top,point.y);
        }
        double height=top-bottom;
        if(height<.01) return;
        materials|=1<<RECOVERY;
        surfaces.add(new Surface(points,bottom,height,(float)crest,mode,rgb,(int)(strength*255)));
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
        materials|=1<<kind;
        quads.add(new Quad(a,b,c,d,(float)u,(float)v,rgb,kind,
                Mth.clamp((int)(strength*255),1,255),seed&31,a.add(c).scale(.5).distanceToSqr(camera)));
    }

    BufferBuilder.RenderedBuffer bake(BufferBuilder buffer) {
        if(quads.isEmpty() && surfaces.isEmpty()) return null;
        quads.sort(Comparator.comparingDouble(Quad::depth).reversed());
        buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        for(var surface:surfaces) {
            for(var point:surface.points) {
                float y=(float)((point.y-surface.bottom)/surface.height);
                vertex(buffer,point,RECOVERY*2+y,surface.mode*4+surface.crest,surface.rgb,surface.alpha);
            }
        }
        for(var q:quads) {
            vertex(buffer,q,q.a,q.u,-1);
            vertex(buffer,q,q.b,q.u,1);
            vertex(buffer,q,q.c,q.v,1);
            vertex(buffer,q,q.d,q.v,-1);
        }
        return buffer.end();
    }

    private void vertex(BufferBuilder buffer,Quad q,Vec3 point,float u,float v) {
        vertex(buffer,point,q.kind*2+u,q.seed*4+v,q.rgb,q.alpha);
    }

    private void vertex(BufferBuilder buffer,Vec3 point,float u,float v,int rgb,int alpha) {
        Vec3 p=point.subtract(camera);
        buffer.vertex(p.x,p.y,p.z).uv(u,v).color(rgb>>16&255,rgb>>8&255,rgb&255,alpha).endVertex();
    }

    private record Surface(List<Vec3> points,double bottom,double height,float crest,int mode,int rgb,int alpha) {}
    private record Quad(Vec3 a,Vec3 b,Vec3 c,Vec3 d,float u,float v,int rgb,int kind,int alpha,int seed,double depth) {}
}
