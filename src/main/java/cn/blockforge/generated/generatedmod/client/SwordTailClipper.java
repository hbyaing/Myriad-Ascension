package cn.blockforge.generated.generatedmod.client;

import java.util.function.Consumer;

/** Clips ordinary plume triangles behind the measured pommel, preserving UVs.
 * History itself remains unchanged. Scratch storage is reused for every quad. */
public final class SwordTailClipper {
    public record Vertex(double x,double y,double z,float u,float v,float alpha) {
        Vertex lerp(Vertex b,double t){return new Vertex(x+(b.x-x)*t,y+(b.y-y)*t,z+(b.z-z)*t,
                (float)(u+(b.u-u)*t),(float)(v+(b.v-v)*t),(float)(alpha+(b.alpha-alpha)*t));}
    }
    private final Vertex[] quad=new Vertex[4],polygon=new Vertex[4];
    private double ox,oy,oz,nx,ny,nz,gap,fade;
    private int count;
    public void begin(double x,double y,double z,double dx,double dy,double dz,double setback,double fadeLength){
        double norm=Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(!Double.isFinite(norm)||norm<1e-9)throw new IllegalArgumentException("Invalid pommel direction");
        ox=x;oy=y;oz=z;nx=dx/norm;ny=dy/norm;nz=dz/norm;
        gap=Math.max(0,setback);fade=Math.max(.0001,fadeLength);count=0;
    }
    public void vertex(double x,double y,double z,float u,float v,float alpha,Consumer<Vertex> sink){
        quad[count++]=new Vertex(x,y,z,u,v,alpha);
        if(count<4)return;
        count=0;
        boolean all=true,any=false;
        for(var p:quad){boolean inside=distance(p)<=0;all&=inside;any|=inside;}
        if(!any)return;
        if(all){for(var p:quad)emit(p,sink);return;}
        // Use the same diagonal as the original QUADS index buffer. Clamping
        // individual corners would fold the light into a bright boundary line.
        triangle(quad[0],quad[1],quad[2],sink);
        triangle(quad[0],quad[2],quad[3],sink);
    }
    private double distance(Vertex p){return (p.x-ox)*nx+(p.y-oy)*ny+(p.z-oz)*nz+gap;}
    private void triangle(Vertex a,Vertex b,Vertex c,Consumer<Vertex> sink){
        int size=0;Vertex previous=c;double pd=distance(previous);
        for(int i=0;i<3;i++){
            Vertex current=i==0?a:i==1?b:c;double d=distance(current);
            if((pd<=0)!=(d<=0))polygon[size++]=previous.lerp(current,pd/(pd-d));
            if(d<=0)polygon[size++]=current;
            previous=current;pd=d;
        }
        for(int i=1;i+1<size;i++){
            emit(polygon[0],sink);emit(polygon[i],sink);emit(polygon[i+1],sink);
            emit(polygon[i+1],sink); // one zero-area triangle in the QUADS carrier
        }
    }
    private void emit(Vertex p,Consumer<Vertex> sink){
        double dx=p.x-ox,dy=p.y-oy,dz=p.z-oz;
        // A fixed centimetre fade crosses a distant old segment in milliseconds
        // during a U-turn. Fade its last 30 degrees before the plane as well;
        // the measured near-root join keeps its short spatial opening.
        double window=Math.max(fade,Math.sqrt(dx*dx+dy*dy+dz*dz)*.5);
        double q=Math.max(0,Math.min(1,-distance(p)/window)),weight=q*q*(3-2*q);
        if(q>=1){sink.accept(p);return;}
        sink.accept(new Vertex(p.x,p.y,p.z,p.u,p.v,(float)(p.alpha*weight)));
    }
    public void end(){if(count!=0)throw new IllegalStateException("Incomplete sword-tail quad");}
}
