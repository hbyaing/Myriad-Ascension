package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.yujian.HuahongSkin;
import cn.blockforge.generated.generatedmod.client.HuahongFlightPath.Frame;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Independent Forge geometry for the active huahong form families.
 * World history drives every tail; local sheets bend along that same path.
 * No reference-only V13 shader, carrier model or browser code is embedded. */
final class HuahongSkinGeometry {
    private record Row(Vec3 p,double u,double width,float alpha,int color,Vec3 side){}
    private record Birth(Vec3 p,double tick,double arc,Vec3 direction,long cell){}
    private record Cloud(Vec3 p,double seed,double radius,float alpha,int color,int mode){}
    private final HuahongFlightPath path;
    private final VertexConsumer out;
    private final PoseStack.Pose pose;
    private final Vec3 eye,center,forward;
    private final List<SwordFlightTrailHistory.Node> nodes;
    private final double now,origin;
    private final float strength;
    private int vertices;
    private HuahongSkinGeometry(VertexConsumer out,PoseStack.Pose pose,Vec3 eye,Vec3 center,Vec3 forward,Vec3 headSide,
            List<SwordFlightTrailHistory.Node> nodes,double now,float strength){
        this.out=out;this.pose=pose;this.eye=eye;this.center=center;this.forward=forward;this.nodes=nodes;this.now=now;this.strength=strength;
        path=new HuahongFlightPath(nodes,center,forward,headSide);
        origin=nodes.isEmpty()?0:Math.floor(nodes.get(0).arc()/256)*256;
    }
    static int draw(HuahongSkin skin,VertexConsumer out,PoseStack.Pose pose,Vec3 eye,Vec3 center,Vec3 forward,Vec3 headSide,
            List<SwordFlightTrailHistory.Node> nodes,double now,float strength){
        var g=new HuahongSkinGeometry(out,pose,eye,center,forward,headSide,nodes,now,strength);
        switch(skin){
            case NEEDLE -> g.needle();
            case METEOR_FLAME -> g.fire(false);
            case FLAME -> g.fire(true);
            case FEATHER -> g.feather();
            case FAN -> g.fan();
            case SMOKE -> g.smoke();
            case DENSE -> g.dense();
            default -> throw new IllegalArgumentException("Prism has its own consumer");
        }
        return g.vertices;
    }
    private static double fade(double age,double life){return Math.pow(Mth.clamp(1-age/life,0,1),1.6);}
    private static double rnd(double x){double v=Math.sin(x*71.371+19.713)*15317.317;return v-Math.floor(v);}
    private static Vec3 vec(SwordFlightTrailHistory.Point p){return new Vec3(p.x(),p.y(),p.z());}
    private static Vec3 side(Vec3 f){return f.cross(Math.abs(f.y)>.95?new Vec3(0,0,1):new Vec3(0,1,0)).normalize();}
    private Frame frame(Vec3 p,Vec3 f){return path.frame(p,f);}
    private HuahongFlightPath.Frame at(int i){return path.frame(vec(nodes.get(i).point()),vec(nodes.get(Math.min(nodes.size()-1,i+1)).point()).subtract(vec(nodes.get(Math.max(0,i-1)).point())));}
    /** Walk the recorded path backwards; turn local flames with the trail, not a rigid spine. */
    private HuahongFlightPath.Frame back(double distance){return path.behind(distance);}
    private Frame axial(double x){
        if(x<=0)return back(-x);
        Frame head=back(0);return new Frame(head.p().add(head.forward().scale(x)),head.forward(),head.side(),head.up());
    }
    private Vec3 offset(HuahongFlightPath.Frame f,double x,double y){return f.p().add(f.side().scale(x)).add(f.up().scale(y));}
    private static double smooth(double a,double b,double x){double q=Mth.clamp((x-a)/(b-a),0,1);return q*q*(3-2*q);}
    private static double curve(double a,double b,double c,double q){return a*(1-q)*(1-q)+2*b*q*(1-q)+c*q*q;}
    private double headArc(){return nodes.isEmpty()?0:nodes.get(nodes.size()-1).arc();}
    private static int mix(int a,int b,double t){t=Mth.clamp(t,0,1);return ((int)(((a>>16)&255)*(1-t)+((b>>16)&255)*t)<<16)|((int)(((a>>8)&255)*(1-t)+((b>>8)&255)*t)<<8)|(int)((a&255)*(1-t)+(b&255)*t);}
    /** Canonical world-arc births survive render-time head clipping and inserted nodes.
     * Position and birth time are interpolated together; the cell is the identity. */
    private List<Birth> births(double spacing,double length,double life){
        var out=new ArrayList<Birth>();if(nodes.size()<2)return out;
        double start=Math.max(nodes.get(0).arc(),headArc()-length);int i=0;
        for(long cell=(long)Math.ceil(start/spacing);cell*spacing<=headArc()+1e-8;cell++){
            double arc=cell*spacing;while(i<nodes.size()-2&&nodes.get(i+1).arc()<arc)i++;
            var a=nodes.get(i);var b=nodes.get(i+1);double span=b.arc()-a.arc();if(span<1e-8)continue;
            double t=Mth.clamp((arc-a.arc())/span,0,1),born=a.tick()+(b.tick()-a.tick())*t;
            if(now-born<0||now-born>life*20)continue;
            Vec3 direction=vec(b.point()).subtract(vec(a.point()));if(direction.lengthSqr()<1e-10)direction=forward;
            out.add(new Birth(vec(a.point()).lerp(vec(b.point()),t),born,arc,direction.normalize(),cell));
        }
        return out;
    }
    private List<Row> history(double life,double width,int color,int strand,double spread,boolean oriented){
        return history(life,width,color,strand,spread,oriented,0);
    }
    private List<Row> history(double life,double width,int color,int strand,double spread,boolean oriented,double visualSpeed){
        List<Row> rows=new ArrayList<>();
        for(int i=0;i<nodes.size();i++){
            var n=nodes.get(i);double age=(now-n.tick())/20;
            if(age<0)continue;
            if(visualSpeed>0)age=Math.max(age,(headArc()-n.arc())/visualSpeed);
            if(age<0||age>=life)continue;
            Frame f=at(i);double phase=strand*2.399+Math.sin(n.arc()*.47+strand)*.28;
            double open=(1-Math.exp(-age*5))*spread;
            Vec3 normal=f.side().scale(Math.cos(phase)).add(f.up().scale(Math.sin(phase)));
            rows.add(new Row(f.p().add(normal.scale(open)),n.arc()-origin,width*(.18+.82*fade(age,life)),(float)fade(age,life),color,oriented?normal:null));
        }
        return rows;
    }
    private static List<Row> opacity(List<Row> rows,float opacity){
        var scaled=new ArrayList<Row>(rows.size());
        for(var r:rows)scaled.add(new Row(r.p(),r.u(),r.width(),r.alpha()*opacity,r.color(),r.side()));
        return scaled;
    }
    private void nucleus(int color,double length,double width,float opacity){
        List<Row> rows=new ArrayList<>();
        for(int i=0;i<=36;i++){
            double q=i/36.0;Frame f=back(q*length);
            // The white maximum belongs to a short pointed ridge, not a second
            // broad spindle pasted over every form family.
            rows.add(new Row(f.p().add(f.forward().scale(.18*(1-q))),-q*length,width*Math.pow(Math.max(0,Math.sin(Math.PI*q)),1.1)*Math.exp(-q*1.8),strength*opacity*(float)Math.pow(1-q,1.2),color,f.side()));
        }
        strip(rows,22,true);
    }
    private void needle(){
        // The active Skill selects flow-lookdev V5: a concentrated LONG head,
        // not the older single thin-needle consumer and not a reduced white line.
        double half=.026+.14*.20,length=1+3.4*.76+9.6*.025;
        strip(opacity(history(5.8,half*4.8,0xffca74,0,0,false),.05F),10);
        strip(opacity(history(5.8,half*1.35,0xffd69b,0,0,false),.40F),34);
        strip(opacity(history(5.8,half*.23,0xfff3d6,0,0,false),.63F),22);
        for(int k=0;k<3;k++)strip(opacity(history(5.8,half*(.08+.10*rnd(k+21)),0xffdfaa,k,.08,false),.32F),22);
        var veil=new ArrayList<Row>();var body=new ArrayList<Row>();var core=new ArrayList<Row>();
        for(int i=0;i<=96;i++){
            double q=i/96.0;Frame f=back(q*length);
            double shape=Math.pow(smooth(0,.15,q),.8)*Math.pow(1-q,1.8);
            Vec3 p=f.p().add(f.forward().scale(.68));
            int color=mix(0xffc45c,0xfff5cf,.20+.80*Math.pow(1-q,1.7));
            float alpha=strength*(float)(1-q*.7);
            veil.add(new Row(p,-q*length,shape*(.12+.14*.48)*1.9,alpha*.08F,0xffca73,f.side()));
            body.add(new Row(p,-q*length,shape*(.12+.14*.48),alpha*.50F,color,f.side()));
            core.add(new Row(p,-q*length,shape*(.018+.14*.07),alpha*.72F,0xfff4d4,f.side()));
        }
        strip(veil,10,true);strip(body,34,true);strip(core,22,true);
    }
    private void fire(boolean torn){
        // Unequal open combustion tongues retain separate dark gaps. A few
        // broad tongues carry the silhouette; the others are narrow hot folds.
        int count=torn?29:8;double length=torn?4.2:5.35;
        for(int k=0;k<count;k++){
            List<Row> rows=new ArrayList<>(),vein=new ArrayList<>();
            double phase=k*2.399,span=torn?(.55+length*(.18+.87*rnd(k+8)))*1.1392:length*(.55+.45*rnd(k+8));
            double root=torn?0:.04+(k%4)*.065,rootRadius=(.10+.58*rnd(k+5))*.95;
            double breadth=torn?.27*(.48+.96*rnd(k+41)):.24;
            boolean hero=k%5==0;
            for(int i=0;i<=64;i++){
                double q=i/64.0,envelope=Math.max(0,Math.sin(Math.PI*q));Frame f=back(root+q*span);
                double angle=phase+q*(torn?.24:.65)+Math.sin(q*7-now*.04+k)*q*.20;
                double open=smooth(0,.4,q),reach=torn?curve(rootRadius*.72,rootRadius*.72+.17,rootRadius*.72+.95*(.24+.75*rnd(k+19)),q):open*.2625*Math.sin(Math.PI*q);
                double curl=Math.sin(q*8-now*.065+k)*envelope*q*(torn?.14:.075);
                Vec3 transverse=f.side().scale(Math.cos(angle)).add(f.up().scale(Math.sin(angle)));
                Vec3 p=offset(f,Math.cos(angle)*reach,Math.sin(angle)*reach+curl).add(f.forward().scale(torn?.48+.16*rnd(k+4):.17));
                double width=breadth*Math.pow(envelope,torn?.60:.65)*Math.pow(1-q,torn?.24:.34)*(.60+.40*rnd(k+7))*(1+.18*Math.sin(q*12-now*.06+k));
                float a=strength*(torn?.245F:hero?.42F:.28F)*(float)(torn?.96-.81*q:1-q);
                rows.add(new Row(p,q*span+k*1.37,width,a,torn?mix(0xff502c,0xffa35c,.28*(1-q)):mix(0xff743e,0xffc17d,.35*(1-q)),transverse));
                if(k%3==0)vein.add(new Row(p.add(transverse.scale(width*.24)),q*span+k*1.37,.009*(1-q),strength*.21F*(float)Math.pow(1-q,1.4),0xffdab0,transverse));
            }
            if(torn){
                var red=new ArrayList<Row>();for(var r:rows)red.add(new Row(r.p(),r.u(),r.width()*2.1,r.alpha()*.38F,0xef492c,r.side()));strip(red,24);
                var gold=new ArrayList<Row>();for(int i=0;i<Math.min(25,rows.size());i++){var r=rows.get(i);gold.add(new Row(r.p(),r.u(),r.width()*.20,r.alpha(),0xffd17b,r.side()));}strip(gold,24);
            }
            strip(rows,24);if(!vein.isEmpty())strip(vein,22);
        }
        for(int k=0;k<(torn?7:4);k++){
            var tail=history(torn?2.1:2.8,torn?.075:.055,torn?0xf56037:0xff9b56,k,torn?.33:.13,true);
            strip(tail,24);
        }
        if(torn){
            var red=new ArrayList<Row>();
            for(int i=0;i<=36;i++){
                double q=i/36.0;Frame f=back(q*1.7);
                red.add(new Row(f.p().add(f.forward().scale(1.19)),-q*1.7,.243*Math.sin(Math.PI*q),strength*.11F,0xf2522f,f.side()));
            }
            strip(red,10,true);nucleus(0xffedc2,.64,.043,.28F);
        }else{
            var soft=new ArrayList<Row>();var hot=new ArrayList<Row>();
            for(int i=0;i<=68;i++){
                double q=i/68.0,w=Math.pow(Math.max(0,Math.sin(Math.PI*q)),.65)*Math.exp(-q*2.7);Frame f=back(q*5.35);
                Vec3 p=f.p().add(f.forward().scale(.26));float alpha=strength*(float)(1-q);
                soft.add(new Row(p,-q*5.35,w*.32*2.1,alpha*.06F,0xff8750,f.side()));
                hot.add(new Row(p,-q*5.35,w*.32*.19,alpha*.68F,0xfff1c8,f.side()));
            }
            strip(soft,10,true);strip(hot,22,true);
        }
        // Detached remnants are tied to canonical world births, not current pose.
        if(torn)for(var b:births(1.10,26,1.1)){
            double age=(now-b.tick())/20,behind=headArc()-b.arc();if(age<.04)continue;
            Frame f=frame(b.p(),b.direction());double phase=b.cell()*2.399;
            Vec3 start=offset(f,Math.cos(phase)*(.10+age*.18),Math.sin(phase)*.22+age*.12);
            List<Row> remnant=new ArrayList<>();
            for(int i=0;i<=16;i++){
                double q=i/16.0;Vec3 p=start.subtract(f.forward().scale(q*(.36+.8*fade(age,1.1)))).add(f.up().scale(Math.sin(q*5+phase)*q*.12));
                remnant.add(new Row(p,q*2+(b.cell()%64)*3,.075*Math.sin(Math.PI*q),(float)(fade(age,1.1)*smooth(26,20,behind)*.32),0xff6a38,f.side()));
            }
            strip(remnant,24);
        }
        embers(torn?0xff662b:0xffbd69,torn?.09:.05);
    }
    private void feather(){
        // Open V4 membrane family: irregular three-dimensional rear endpoints,
        // offset birth roots and a few middle-born forks. No removed V3 form.
        float formFade=(float)Math.pow(strength,2.1);
        double[][] lobes={{1,-.84,.3,1.18},{.68,.91,.4,1.38},{1.15,.38,-.16,.8},{.51,-.55,-.70,1.12},{.84,.21,.88,1.05},{.61,-1.03,.05,.84},{.96,.93,-.33,.73},{.44,.43,-.58,1.24},{1.24,-.31,.03,.58},{.72,-.35,.72,.95},{.57,.73,.12,.87},{1.07,.10,-.65,.62}};
        for(int k=0;k<lobes.length;k++){
            List<Row> rows=new ArrayList<>(),rib=new ArrayList<>();var l=lobes[k];
            double span=4.146*l[0],reach=1.1*(.55+.24*rnd(k+8));
            boolean middleBorn=k==3||k==6||k==9;
            double rootX=middleBorn?-.10-span*.17:.64-.055*(k%4),controlX=.58-span*(.30+.14*rnd(k+94)),tipX=.45-span;
            double rootReach=middleBorn?reach*.28:.023;
            double controlReach=reach*1.12,tipReach=reach*(1.02+.46*rnd(k+36));
            for(int i=0;i<=64;i++){
                double q=i/64.0,envelope=Math.max(0,Math.sin(Math.PI*q));Frame f=axial(curve(rootX,controlX,tipX,q));
                double spread=curve(rootReach,controlReach,tipReach,q);
                double x=l[1]*spread,y=l[2]*curve(rootReach,reach*.82,reach*(1.12+.40*rnd(k+91)),q)*2.05;
                double flutter=Math.sin(q*7-now*.022+k*.83)*envelope*q*.045;
                double hook=smooth(.55,1,q)*Math.sin(q*4.4+k*1.61)*(k%3==1?.20:.07);
                Vec3 p=offset(f,x,y+flutter+hook);
                Vec3 s=f.side().scale(l[1]).add(f.up().scale(l[2]+Math.sin(q*3+k)*.20)).normalize();
                double width=(.055+.14*.83)*l[3]*Math.pow(envelope,.67)*Math.pow(1-q,.34);
                int color=k==2||k==8||k==11?0x8063d8:mix(0x55b3db,0x2884d0,q*.6);
                rows.add(new Row(p,q*span+k*2.37,width,formFade*(k==1||k==4||k==8?.3864F:.14835F)*(float)((.86-.46*q)*smooth(0,.1,q)),color,s));
                if(i>=3+k%4&&i<=36+k%12)rib.add(new Row(p.add(s.scale(width*(k%2==0?-.28:.35))),q*span+k*2.37,.006*(1-q),strength*(k%4==0?.24F:.12F)*(float)(1-q),0xa5d6ed,s));
            }
            strip(rows,26);strip(rib,22);
            if(k%2==0){
                var fork=new ArrayList<Row>();double start=.36;
                for(int i=0;i<=30;i++){
                    double q=i/30.0;Frame f=axial(curve(rootX,controlX,tipX,start)-q*span*.42);
                    double spread=curve(rootReach,controlReach,tipReach,start);
                    Vec3 p=offset(f,l[1]*(spread+q*.38),l[2]*spread*2.05+Math.sin(q*Math.PI)*.16);
                    Vec3 s=f.side().scale(l[1]).add(f.up().scale(l[2])).normalize();
                    fork.add(new Row(p,q*span+k*1.71,.075*Math.pow(Math.sin(Math.PI*q),.8),formFade*(k==4||k==8?.2184F:.1014F)*(float)(1-q),0x6e8dd1,s));
                }
                strip(fork,26);
            }
        }
        strip(history(3.2,.045,0x4598cf,0,0,false),10);
        for(int k=0;k<3;k++)strip(history(3.2,.008,0x6ea7da,k,.07,false),26);
        var haze=new ArrayList<Row>();var hot=new ArrayList<Row>();
        for(int i=0;i<=24;i++){
            double q=i/24.0,w=Math.pow(Math.max(0,Math.sin(Math.PI*q)),1.45);Frame f=axial(.38+q*.48);
            float a=(float)(Math.pow(strength,2.1)*(.3+.7*Math.sin(Math.PI*q)));
            haze.add(new Row(f.p(),q,w*(.045+.14*.32),a*.1105F,0x6ec9ff,f.side()));
            hot.add(new Row(f.p(),q,w*(.007+.14*.058),a*.72F,0xecffff,f.side()));
        }
        strip(haze,10,true);strip(hot,22,true);
    }
    private void fan(){
        // Gauze follows stored world rows and opens with birth age. Broad soft
        // streams and sparse long wisps replace the old evenly spaced comb.
        // Preserve V7's spatial silhouette at the game's much higher flight
        // speed: its source consumer travels 7.8 units/s. This limits only the
        // artistic wake; actual movement and collision are unaffected.
        final double visualSpeed=7.8;
        for(int sign:new int[]{-1,1})for(int k=0;k<3;k++){
            var rows=new ArrayList<Row>();var ridge=new ArrayList<Row>();double rank=(k+.55)/3.15,phase=k*1.79+(sign<0?.94:0);
            double life=2.4*(.43+.43*(1-rank))*(sign>0?1:.92);
            for(int i=0;i<nodes.size();i++){
                var n=nodes.get(i);double realAge=(now-n.tick())/20;if(realAge<0)continue;
                double behind=headArc()-n.arc(),age=Math.max(realAge,behind/visualSpeed);if(age>=life)continue;
                Frame f=at(i);double open=1-Math.exp(-age*2.208),left=fade(age,life);
                double lateral=sign*(.014+1.6*(.26+rank*1.45)*open)*(1+.09*Math.sin(n.arc()*.46+phase-age*.8)*open);
                double wave=(Math.sin(n.arc()*.38+phase-age*.90)*.22+Math.sin(n.arc()*.87+phase*1.7-age*1.25)*.07)*open;
                double width=(.045+.36*(.50+.60*(1-rank)))*smooth(0,.21,age)*Math.pow(left,.45)*(1+.24*Math.sin(n.arc()*.55+phase));
                double roll=.65+sign*.20+Math.sin(n.arc()*.29+phase-age*.62)*.24;
                Vec3 across=f.side().scale(Math.cos(roll)).add(f.up().scale(Math.sin(roll)));
                Vec3 p=offset(f,lateral*.79+wave*.48,lateral*.61+wave-age*.06);
                float a=(float)(Math.pow(Mth.clamp(1-age/life,0,1),1.8)*(1-.22*rank)*smooth(0,.28,behind));
                rows.add(new Row(p,n.arc()-origin+phase*2.2,width,a*.72F,mix(0xefb75e,0xfff5d9,.16+.20*left),across));
                ridge.add(new Row(p.add(across.scale(width*.30)),n.arc()-origin+phase*2.2,.0045+.003*left,a*.18F,0xedd6a1,across));
            }
            strip(rows,28);strip(ridge,22);
        }
        for(int k=0;k<4;k++)strip(history(3.1,.0075,0xc7ad7e,k,.29,true,visualSpeed),28);
        // V7's concentrated root belongs to the gauze streams themselves; it
        // does not add the generic white nucleus used by the meteor families.
    }
    private void smoke(){
        // Dense, short, irregular packets instead of one tiny card every two
        // ticks (almost nine blocks apart at actual top speed).
        var clouds=new ArrayList<Cloud>();
        final double bodyLife=.18+3.2*.07,smokeLife=bodyLife*1.8;
        for(var b:births(.55,24,smokeLife)){
            double age=(now-b.tick())/20,behind=headArc()-b.arc(),seed=b.cell();
            double dissolve=smooth(24,18,behind)*Math.pow(Mth.clamp(1-age/smokeLife,0,1),2.1);
            double core=Math.exp(-Math.pow(age/bodyLife,2)*1.9);
            double scatter=.11+age*.30;double phi=seed*2.399;
            // An isotropic world offset does not make old smoke orbit the
            // carrier when it banks. Canonical cell controls every random value.
            Vec3 p=b.p().add(Math.cos(phi)*scatter,Math.sin(phi*1.31)*scatter+age*.16,Math.sin(phi)*scatter);
            double radius=(.47+.24*rnd(seed+7))*(1+age*.35);
            float a=(float)(dissolve*(.20+.12*core));
            clouds.add(new Cloud(p,Math.floorMod(b.cell(),64),radius,a,mix(0x010001,0x0a0101,core*(.2+.5*rnd(seed+9))),30));
            if(rnd(seed+23)>.64&&core>.15)
                clouds.add(new Cloud(p.add(.025,.02,-.015),Math.floorMod(b.cell()+19,64),radius*.48,(float)(dissolve*core*.10),0xce252f,20));
        }
        // The head is broad dark smoke with a small red fracture, never a white
        // nucleus. These layers disappear with the local transformation fade.
        if(strength>.001){
            for(int k=0;k<8;k++){
                Frame f=back(.15+k*.34);Vec3 p=offset(f,Math.sin(k*2.3)*.13,Math.cos(k*2.3)*.12);
                clouds.add(new Cloud(p,k*5,.48+.11*Math.sin(k*1.7),strength*.23F,0x080001,30));
                if(k==1||k==4)clouds.add(new Cloud(p,k*5+3,.22,strength*.13F,0xb92235,20));
            }
        }
        // Several depth slices give packets actual spatial thickness and
        // intersect world depth independently; this is layered smoke, not a
        // claim of ray-marched density or a fluid solver.
        var slices=new ArrayList<Cloud>();
        for(var c:clouds){
            Vec3 view=eye.subtract(c.p());view=view.lengthSqr()<1e-10?forward:view.normalize();
            for(int layer=0;layer<6;layer++){
                double z=(layer-2.5)*.28,radius=c.radius()*Math.sqrt(1-z*z);
                slices.add(new Cloud(c.p().add(view.scale(z*c.radius()*.72)),c.seed(),radius,c.alpha()*.34F,c.color(),c.mode()));
            }
        }
        slices.sort(Comparator.comparingDouble((Cloud c)->c.p().distanceToSqr(eye)).reversed());
        for(var c:slices)billboard(new Row(c.p(),c.seed(),c.radius(),c.alpha(),c.color(),null),c.mode());
        embers(0xff341b,.026);
    }
    private void dense(){
        // Only the compact source nucleus uses a sprite. The surrounding body
        // is the independent ray-integrated world field, queued by HuahongVisuals.
        if(strength>.001F)billboard(new Row(center,0,.12+3.5*.045+.52*.10,strength*.74F,0xe6fbff,null),32);
    }
    private void embers(int color,double size){
        for(var b:births(1.65,34,1.2)){
            if(rnd(b.cell()+31)>.32)continue;
            double age=(now-b.tick())/20;if(age<.035)continue;
            double phase=b.cell()*2.399;Frame f=frame(b.p(),b.direction());
            Vec3 p=b.p().add(Math.sin(phase)*age*.42,Math.cos(phase)*age*.31+age*age*.17,Math.sin(phase*1.7)*age*.31);
            float a=(float)(fade(age,1.2)*smooth(34,27,headArc()-b.arc())*.34);
            strip(List.of(new Row(p,b.arc()-origin,size,a,color,f.side()),new Row(p.subtract(f.forward().scale(.14+age*.1)),b.arc()-origin-.2,0,0,color,f.side())),24);
        }
    }
    private void billboard(Row r,int mode){
        Vec3 normal=eye.subtract(r.p()).normalize(),right=side(normal).scale(r.width()),up=normal.cross(right).normalize().scale(r.width());
        vertex(r.p().subtract(right).subtract(up),Math.floor(r.u())*4,mode,r.alpha(),r.color());
        vertex(r.p().add(right).subtract(up),Math.floor(r.u())*4+1,mode,r.alpha(),r.color());
        vertex(r.p().add(right).add(up),Math.floor(r.u())*4+1,mode+1,r.alpha(),r.color());
        vertex(r.p().subtract(right).add(up),Math.floor(r.u())*4,mode+1,r.alpha(),r.color());
    }
    private void strip(List<Row> rows,int mode){
        strip(rows,mode,false);
    }
    private void strip(List<Row> rows,int mode,boolean localHead){
        if(!localHead){strip(rows,mode,false,false);return;}
        strip(rows,mode,true,false);strip(rows,mode,true,true);
    }
    private void strip(List<Row> rows,int mode,boolean localHead,boolean crossPlane){
        if(rows.size()<2)return;
        double[] lengths=new double[rows.size()];for(int i=1;i<rows.size();i++)lengths[i]=lengths[i-1]+rows.get(i).p().distanceTo(rows.get(i-1).p());
        double total=lengths[lengths.length-1];if(total<1e-8)return;
        double cap=Math.min(total*.12,.25);
        int columns=mode==24||mode==26||mode==28||mode==34?6:1;
        Vec3[] previous=null;Vec3 previousSide=null;Row old=null;float previousAlpha=0;
        for(int i=0;i<rows.size();i++){
            Row row=rows.get(i);Vec3 f=rows.get(Math.min(rows.size()-1,i+1)).p().subtract(rows.get(Math.max(0,i-1)).p());
            if(f.lengthSqr()<1e-12)continue;f=f.normalize();
            // Local nuclei and membrane ridges carry the exact section's side
            // through every position offset. Never infer a nearest path point
            // from that offset: a U-turn can put another arm closer to it.
            Vec3 supplied=row.side();
            if(supplied==null&&localHead)supplied=path.behind(0).side();
            Vec3 s=supplied==null?f.cross(eye.subtract(row.p())):supplied.subtract(f.scale(supplied.dot(f)));
            if(crossPlane)s=f.cross(s);
            if(s.lengthSqr()<1e-10&&previousSide!=null)s=previousSide.subtract(f.scale(previousSide.dot(f)));
            if(s.lengthSqr()<1e-10)s=side(f);else s=s.normalize();
            if(previousSide!=null&&s.dot(previousSide)<0)s=s.scale(-1);
            Vec3 normal=f.cross(s).normalize();
            double endFade=rows.size()==2?1:smooth(0,cap,lengths[i])*smooth(0,cap,total-lengths[i]);
            float alpha=row.alpha()*(float)endFade*(localHead?.70710677F:1);
            double width=row.width();
            if(mode==22&&width>0){
                double covered=HuahongVisuals.coreHalfWidth(row.p(),s,eye,width);
                if(covered>width){alpha*=width/covered;width=covered;}
            }
            Vec3[] section=new Vec3[columns+1];
            for(int j=0;j<=columns;j++){
                double across=j*2.0/columns-1;
                double camber=columns==1||mode==34?0:width*(1-across*across)*(.24+.08*Math.sin(row.u()*(Math.PI*2*33/256)+now*.013));
                section[j]=row.p().add(s.scale(width*across)).add(normal.scale(camber));
            }
            if(previous!=null){
                for(int j=0;j<columns;j++){
                    double a=mode+j/(double)columns,b=mode+(j+1)/(double)columns;
                    vertex(previous[j],old.u(),a,previousAlpha,old.color());
                    vertex(previous[j+1],old.u(),b,previousAlpha,old.color());
                    vertex(section[j+1],row.u(),b,alpha,row.color());
                    vertex(section[j],row.u(),a,alpha,row.color());
                }
            }
            previous=section;previousSide=s;old=row;previousAlpha=alpha;
        }
    }
    private void vertex(Vec3 p,double u,double v,float alpha,int color){
        Vec3 r=p.subtract(eye);out.vertex(pose.pose(),(float)r.x,(float)r.y,(float)r.z).uv((float)u,(float)v)
                .color((color>>16)&255,(color>>8)&255,color&255,Mth.clamp(Math.round(alpha*255),0,255)).endVertex();vertices++;
    }
}
