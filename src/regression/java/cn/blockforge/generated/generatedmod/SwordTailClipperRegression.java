package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.client.SwordTailClipper;
import java.util.ArrayList;

/** Analytic clipped area, affine UVs and rear-plane coverage of the real clipper. */
public final class SwordTailClipperRegression {
    private static int checks;
    public static void run(){
        for(double world:new double[]{0,12345.67,29_999_900})for(int angle=0;angle<16;angle++)for(double cut:new double[]{-1.2,-1,-.73,-.1,0,.37,.99,1,1.2}){
            double yaw=angle*Math.PI/8,pitch=Math.sin(yaw)*1.3;
            double[] x={Math.cos(yaw)*Math.cos(pitch),Math.sin(pitch),Math.sin(yaw)*Math.cos(pitch)};
            double[] y={-Math.sin(yaw),0,Math.cos(yaw)};
            double[] center={world,-world*.37,world*.71};
            var out=new ArrayList<SwordTailClipper.Vertex>();var clip=new SwordTailClipper();
            clip.begin(center[0]+x[0]*cut,center[1]+x[1]*cut,center[2]+x[2]*cut,x[0],x[1],x[2],.01,.22);
            for(double[] p:new double[][]{{-1,-1},{1,-1},{1,1},{-1,1}})
                clip.vertex(center[0]+x[0]*p[0]+y[0]*p[1],center[1]+x[1]*p[0]+y[1]*p[1],center[2]+x[2]*p[0]+y[2]*p[1],(float)((p[0]+1)/2),(float)((p[1]+1)/2),.8F,out::add);
            clip.end();require(out.size()%4==0,"Incomplete output primitive");
            double area=0;
            for(int i=0;i<out.size();i+=4)area+=area(out.get(i),out.get(i+1),out.get(i+2))+area(out.get(i),out.get(i+2),out.get(i+3));
            near(area,2*Math.max(0,Math.min(2,cut-.01+1)),3e-7,"Clipping changed area or duplicated a lit triangle");
            for(var p:out){
                double px=(p.x()-center[0])*x[0]+(p.y()-center[1])*x[1]+(p.z()-center[2])*x[2];
                double py=(p.x()-center[0])*y[0]+(p.y()-center[1])*y[1]+(p.z()-center[2])*y[2];
                require(px<=cut-.01+1e-7,"Submitted geometry crossed the measured pommel plane");
                near(p.u(),(px+1)/2,1e-6,"Clipped U lost affine interpolation");near(p.v(),(py+1)/2,1e-6,"Clipped V lost affine interpolation");
                require(p.alpha()>=0&&p.alpha()<=.800001,"Invalid root opacity");
                if(Math.abs(px-cut+.01)<1e-7)near(p.alpha(),0,1e-8,"Cut boundary remains a bright line");
            }
        }
        double previous=.8,maxStep=0;
        for(int degrees=0;degrees<=95;degrees+=5){
            double angle=Math.toRadians(degrees);var out=new ArrayList<SwordTailClipper.Vertex>();var clip=new SwordTailClipper();
            clip.begin(0,0,0,Math.sin(angle),0,Math.cos(angle),.01,.22);
            for(int i=0;i<4;i++)clip.vertex(0,0,-20,0,0,.8F,out::add);
            clip.end();double alpha=out.isEmpty()?0:out.get(0).alpha();
            maxStep=Math.max(maxStep,Math.abs(alpha-previous));previous=alpha;
            if(degrees<=55)near(alpha,.8,1e-7,"Aligned distant trail was dimmed");
            if(degrees>=90)near(alpha,0,1e-8,"Front-side distant tail survived the turn");
        }
        require(maxStep<.23,"U-turn erased the distant trail in one angular sample: "+maxStep);
        System.out.println("Sword pommel clip regression: "+checks+" analytic area/UV/rear-plane checks passed");
    }
    private static double area(SwordTailClipper.Vertex a,SwordTailClipper.Vertex b,SwordTailClipper.Vertex c){
        double x=b.x()-a.x(),y=b.y()-a.y(),z=b.z()-a.z(),u=c.x()-a.x(),v=c.y()-a.y(),w=c.z()-a.z();
        double cx=y*w-z*v,cy=z*u-x*w,cz=x*v-y*u;return Math.sqrt(cx*cx+cy*cy+cz*cz)*.5;
    }
    private static void near(double a,double b,double tolerance,String message){require(Double.isFinite(a)&&Math.abs(a-b)<=tolerance,message+": "+a+" / "+b);}
    private static void require(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){run();}
    private SwordTailClipperRegression(){}
}
