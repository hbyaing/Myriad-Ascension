package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.client.SwordFlightTrailHistory;
import cn.blockforge.generated.generatedmod.client.SwordTrailPhase;

/** Exercise real bounded history and material phase across long-flight and time precision boundaries. */
public final class SwordTrailPhaseRegression {
    private static int checks;
    private static void attachmentContinuity(){
        var h=new SwordFlightTrailHistory();
        for(int tick=0;tick<5;tick++)h.sample(new SwordFlightTrailHistory.Point(tick*15,0,0),tick,16);
        double previous=-1;
        for(int frame=0;frame<=120;frame++){
            double now=3+frame/120.0;
            var expected=new SwordFlightTrailHistory.Point(now*15,.01*Math.sin(now),0);
            var render=h.renderNodes(now,expected);
            var head=render.get(render.size()-1);
            near(head.point().distance(expected),0,1e-10,"Fast trail detached from model tail");
            require(head.arc()>=previous,"Render arc rewound at tick boundary");previous=head.arc();
            for(var node:render)require(node.tick()<=now,"Future point entered trail");
        }
        require(h.nodes().get(h.nodes().size()-1).tick()==4,"Rendering mutated fixed history");
        var tiny=new SwordFlightTrailHistory.Point(60,.000002,0);
        var close=h.renderNodes(4,tiny);
        require(close.get(close.size()-1).point().equals(tiny),"Near-equal attachment must still use the exact model endpoint");
        require(h.nodes().get(h.nodes().size()-1).point().y()==0,"Near-equal attachment changed simulation history");
        // The rendered rear rotates on an arc, while fixed tick samples follow
        // its chord. A very short final segment previously made its tangent jump.
        var curved=new SwordFlightTrailHistory();
        curved.sample(new SwordFlightTrailHistory.Point(-2,0,0),0);
        curved.sample(new SwordFlightTrailHistory.Point(0,0,0),1);
        curved.sample(new SwordFlightTrailHistory.Point(2,0,0),2);
        SwordFlightTrailHistory.Point previousJoin=null;
        for(int frame=0;frame<=100;frame++){
            double now=1+frame/100.0;
            var anchor=new SwordFlightTrailHistory.Point((now-1)*2,.08*Math.sin(Math.PI*(now-1)),0);
            var rows=curved.renderNodes(now,anchor);
            require(rows.size()>=2,"Curved attachment lost history");
            var head=rows.get(rows.size()-1);
            require(head.point().equals(anchor),"Curved head detached");
            require(head.point().distance(rows.get(rows.size()-2).point())>=.18-1e-9,"Near-zero head segment destabilizes tangent");
            var join=rows.get(rows.size()-2).point();
            if(previousJoin!=null)require(join.distance(previousJoin)<.04,"Join jumped at a fixed sample boundary");
            previousJoin=join;
        }
    }
    private static void require(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private static void near(double a,double b,double tolerance,String message){require(Math.abs(a-b)<=tolerance,message+": "+a+" / "+b);}
    private static double fract(double v){return v-Math.floor(v);}
    private static double hash(double x,double y){return fract(Math.sin(x*127.1+y*311.7)*43758.5453);}
    private static double wrap(double x,double period){return x-Math.floor(x/period)*period;}
    private static double noise(double x,double y,int period){
        double ix=Math.floor(x),iy=Math.floor(y),fx=fract(x),fy=fract(y);
        fx=fx*fx*(3-2*fx);fy=fy*fy*(3-2*fy);
        double a=hash(wrap(ix,period),iy)*(1-fx)+hash(wrap(ix+1,period),iy)*fx;
        double b=hash(wrap(ix,period),iy+1)*(1-fx)+hash(wrap(ix+1,period),iy+1)*fx;
        return a*(1-fy)+b*fy;
    }
    private static double sin(double arc,double time,int x,int t){return Math.sin(SwordTrailPhase.angle(arc,time,x,t));}
    private static double alpha(double arc,double time,double across){
        double coarse=noise(SwordTrailPhase.noiseX(arc,time,346,692),across*2.7,346);
        double spine=across-.024*sin(arc,time,72,153)-.015*Math.sin(SwordTrailPhase.angle(arc,time,148,312)+1.7);
        double core=Math.exp(-spine*spine/(.12*.12));
        double center=.31*sin(arc,time,34,72)+.12*Math.sin(SwordTrailPhase.angle(arc,time,72,153)+1.8);
        double curl=Math.exp(-(across-center)*(across-center)*42)*(.25+.75*Math.pow(.5+.5*Math.sin(SwordTrailPhase.angle(arc,time,110,232)+across*2.1),2));
        double rolling=Math.sin(SwordTrailPhase.angle(arc,time,57,120)+across*6.5+coarse*1.8);
        double veil=Math.exp(-across*across*4.6)*(.18+.29*coarse)*(.42+.58*Math.pow(.5+.5*rolling,2));
        double edge=Math.pow(Math.max(0,1-Math.abs(across)),1.4);
        double r=Math.max(0,Math.min(1,(coarse+(1-Math.abs(across))*.43-.14)/(.63-.14)));
        double ragged=r*r*(3-2*r);
        double breaks=.74+.26*noise(SwordTrailPhase.noiseX(arc,time,614,614),across*4,614);
        return Math.min(.96,(core*1.12+curl*.64+veil*.85)*edge*(ragged*(1-core)+core)*breaks);
    }
    private static void longFlight(double speed,int ticks){
        var history=new SwordFlightTrailHistory();
        history.sample(new SwordFlightTrailHistory.Point(0,0,0),0,Math.max(8,speed*1.5+2));
        double lastArc=0;int oldBoundaries=0;
        for(int tick=1;tick<=ticks;tick++){
            history.sample(new SwordFlightTrailHistory.Point(speed*tick,0,0),tick,Math.max(8,speed*1.5+2));
            var nodes=history.nodes();double head=nodes.get(nodes.size()-1).arc(),first=nodes.get(0).arc();
            require(nodes.size()>1&&nodes.size()<=256,"Legitimate sustained high speed never clears or overflows trail history");
            near(head,speed*tick,1e-7,"Continuous sample history retains cumulative actual travel");
            require(head-lastArc>0&&head-first<=22+.000001,"The bounded long-flight wake keeps increasing without span inflation");
            if(Math.floor(head/1024)>Math.floor(lastArc/1024))oldBoundaries++;
            double origin=SwordTrailPhase.origin(head,37);
            float headU=SwordTrailPhase.coordinate(head,origin);
            near(headU,37*.37,1e-4,"Flight speed cannot sweep spatial noise through the rendered head");
            float previousU=SwordTrailPhase.coordinate(first,origin);
            for(var node:nodes){
                float u=SwordTrailPhase.coordinate(node.arc(),origin);
                require(u>=previousU&&u<=headU+.0001&&u>=headU-22.0001,"Continuous head-relative U is bounded without per-quad wraps");previousU=u;
                near(node.point().x(),node.arc(),1e-7,"Moving the material origin must not move historical world geometry");
            }
            // The same following-camera point at any flight speed has the
            // same material value; only explicit shader time advances it.
            for(double behind:new double[]{0,.25,1,3,8,16}){
                float u=SwordTrailPhase.coordinate(head-behind,origin);
                near(alpha(u,12.37,0),alpha(37*.37-behind,12.37,0),.0001,
                        "Material phase at a fixed distance behind the head is independent of travel distance");
            }
            lastArc=head;
        }
        require(oldBoundaries>=2,"Real continuously sampled flight crosses repeated former rebase boundaries");
        history.sample(new SwordFlightTrailHistory.Point(speed*ticks+200,0,0),ticks+1,Math.max(8,speed*1.5+2));
        require(history.nodes().size()==1&&history.nodes().get(0).arc()==0,"An impossible positional jump still cuts the old wake");
    }
    /** Server time packets can repeat, move backwards or jump without restarting the client simulation. */
    private static void correctedWorldTime(){
        Object level=new Object();long seed=5119;
        var clock=new SwordTrailPhase.TickClock();
        require(clock.setLevel(level),"Entering a real level resets the trail clock identity");
        clock.advance(seed,true);
        var corrected=new SwordFlightTrailHistory();
        var clean=new SwordFlightTrailHistory();
        var start=new SwordFlightTrailHistory.Point(0,0,0);
        corrected.sample(start,clock.tick(),20);clean.sample(start,seed,20);
        long raw=seed,previousRaw=raw;int backwards=0,repeats=0;
        for(int tick=1;tick<=2600;tick++){
            if(tick%60==0)raw-=35;
            else if(tick%37!=0)raw++;
            if(tick%401==0)raw+=100000;
            if(raw<previousRaw)backwards++;if(raw==previousRaw)repeats++;
            require(!clock.setLevel(level),"Time corrections in the same level do not replace its trail clock");
            clock.advance(raw,true);
            require(clock.tick()==seed+tick,"Each actual client tick advances exactly once despite repeated/backwards server time");
            if(tick%100==0){
                long paused=clock.tick();float phase=SwordTrailPhase.time(paused,.5F);
                for(int frame=0;frame<30;frame++)clock.advance(raw-1000-frame,false);
                require(clock.tick()==paused&&SwordTrailPhase.time(clock.tick(),.5F)==phase,
                        "Paused render frames cannot age the plume or alter its shared material phase");
            }
            var position=new SwordFlightTrailHistory.Point(tick*12,0,0);
            corrected.sample(position,clock.tick(),20);clean.sample(position,seed+tick,20);
            var nodes=corrected.nodes();var expected=clean.nodes();
            require(nodes.size()==expected.size()&&nodes.size()>20,
                    "World-time corrections cannot erase or skip any actual long-flight trail sample");
            var head=nodes.get(nodes.size()-1);var first=nodes.get(0);
            near(head.arc(),tick*12,1e-7,"Twelve-block-per-tick flight retains cumulative arc through time corrections");
            near(first.arc(),expected.get(0).arc(),1e-7,"Time corrections retain the same historical tail as uninterrupted flight");
            require(head.point().equals(position)&&head.tick()==seed+tick,
                    "A duplicate server tick still records the current sword position on its actual client tick");
            double renderCutoff=clock.tick()-1+.5;
            int visible=nodes.size();while(visible>0&&nodes.get(visible-1).tick()>renderCutoff)visible--;
            require(visible>20&&renderCutoff-nodes.get(0).tick()<22,
                    "Corrected world time cannot hide the entire plume behind a future-sample cutoff or premature age expiry");
            near(SwordTrailPhase.time(clock.tick(),.5F),SwordTrailPhase.time(seed+tick,.5F),0,
                    "Material time follows the same fixed clock as history and render-age interpolation");
            previousRaw=raw;
        }
        require(backwards>30&&repeats>30,"Sustained correction scenario actually includes many backwards and duplicate time packets");
        long next=clock.tick()+1;clock.advance(raw,true);
        corrected.sample(new SwordFlightTrailHistory.Point(2600*12+200,0,0),next,20);
        require(corrected.nodes().size()==1&&corrected.nodes().get(0).arc()==0,
                "A genuine teleport still disconnects the old plume after ignoring time corrections");
        require(clock.setLevel(new Object())&&!clock.initialized()&&clock.tick()==0,
                "Actual level replacement discards the old clock before any new-world sampling");
        clock.advance(17,false);require(!clock.initialized(),"A paused new level does not seed a simulated tick");
        clock.advance(17,true);require(clock.tick()==17&&clock.initialized(),"A replacement level seeds from its own first world tick");
        var replacement=new SwordFlightTrailHistory();replacement.sample(start,clock.tick(),20);
        require(replacement.nodes().size()==1&&replacement.nodes().get(0).arc()==0,
                "Fresh per-level history starts disconnected from the previous world's plume");
        require(clock.setLevel(null)&&!clock.initialized()&&clock.tick()==0,"Disconnect clears the level-local clock");
        clock.advance(500,true);require(!clock.initialized()&&clock.tick()==0,"Ticks without a level cannot seed or advance history time");
    }
    /** The real client samples the new pose, then renders the previous interval.
     * Check the WORLD tail, including its velocity and acceleration at 60 FPS;
     * a stable carrier-local nucleus cannot detect a bright 30-block tail cut. */
    private static void highSpeedWorldClipping(){
        for(boolean ordinary:new boolean[]{false,true})for(double speed:new double[]{15,30})for(boolean curved:new boolean[]{false,true}){
            double length=ordinary?22:160;int capacity=ordinary?256:720;
            var history=new SwordFlightTrailHistory(ordinary?.09:.25,ordinary?22:116,length,capacity);
            for(int tick=0;tick<=20;tick++)history.sample(flightPoint(tick,speed,curved),tick,speed*1.7+3);
            SwordFlightTrailHistory.Point previous=null,expectedPrevious=null,velocity=null,expectedVelocity=null;
            for(int tick=21;tick<=44;tick++){
                history.sample(flightPoint(tick,speed,curved),tick,speed*1.7+3);
                require(history.nodes().size()<=capacity,"Fixed history remains capacity bounded with the render guard");
                for(int frame=0;frame<3;frame++){
                    double now=tick-1+frame/3.0;
                    var head=flightPoint(now,speed,curved);
                    var rows=history.renderNodes(now,head);
                    require(rows.size()>2&&rows.size()<=capacity*2+2,"Render guard contains at most two bounded fixed histories");
                    var tail=rows.get(0);var expected=flightPoint(now-length/speed,speed,curved);
                    near(tail.point().distance(expected),0,2e-7,"High-speed world tail follows render time rather than the newer fixed sample [speed="+speed+", curved="+curved+", tick="+tick+", frame="+frame+", headArc="+rows.get(rows.size()-1).arc()+", tailTick="+tail.tick()+"]");
                    near(rows.get(rows.size()-1).arc()-tail.arc(),length,2e-7,"Visible world wake retains its exact spatial length across sample ticks");
                    near(tail.tick(),now-length/speed,2e-8,"Tail birth tick is interpolated with world position and arc");
                    require(rows.get(rows.size()-1).point().equals(head),"Tail clipping cannot move the actual model attachment");
                    for(var row:rows)require(row.tick()<=now+1e-9,"Render guard never exposes a future sample");
                    if(previous!=null){
                        var step=subtract(tail.point(),previous);
                        var expectedStep=subtract(expected,expectedPrevious);
                        near(step.distance(expectedStep),0,4e-7,"Adjacent-frame world tail velocity includes no sampler-size jump");
                        if(velocity!=null)near(subtract(step,velocity).distance(subtract(expectedStep,expectedVelocity)),0,8e-7,
                                "Adjacent-frame world acceleration follows the real path, without a fixed-tick impulse");
                        velocity=step;expectedVelocity=expectedStep;
                    }
                    previous=tail.point();expectedPrevious=expected;
                }
                // A nearly coincident pair straddles the actual fixed sample.
                double before=tick-1e-6,after=tick+1e-6;
                var a=history.renderNodes(before,flightPoint(before,speed,curved));
                var tailBefore=a.get(0).point();
                history.sample(flightPoint(tick+1,speed,curved),tick+1,speed*1.7+3);
                var b=history.renderNodes(after,flightPoint(after,speed,curved));
                require(b.get(0).point().distance(tailBefore)<speed*.000003,
                        "Tick crossing cannot evict one whole bright world-space displacement");
            }
        }
        // Lifetime eviction has the same one-tick look-ahead as length eviction.
        for(double speed:new double[]{15,30}){
            var history=new SwordFlightTrailHistory(.25,4,160,720);
            for(int tick=0;tick<=10;tick++)history.sample(flightPoint(tick,speed,false),tick,speed*1.7+3);
            for(int frame=0;frame<=30;frame++){
                double now=9+frame/30.0;
                var rows=history.renderNodes(now,flightPoint(now,speed,false));
                near(rows.get(0).point().x(),(now-4)*speed,2e-7,"Lifetime boundary uses interpolated render age");
                near(rows.get(0).tick(),now-4,1e-9,"Expired prefix ends exactly at the render lifetime boundary");
            }
            history.sample(new SwordFlightTrailHistory.Point(1000000,0,0),11,speed*1.7+3);
            var teleported=history.renderNodes(11,new SwordFlightTrailHistory.Point(1000000,0,0));
            require(teleported.size()==1&&teleported.get(0).arc()==0,"A real teleport also clears the render guard");
            history.sample(new SwordFlightTrailHistory.Point(0,0,0),1,speed*1.7+3);
            var reset=history.renderNodes(1,new SwordFlightTrailHistory.Point(0,0,0));
            require(reset.size()==1&&reset.get(0).arc()==0,"Clock rollback cannot reconnect the previous guarded segment");
        }
    }
    private static SwordFlightTrailHistory.Point subtract(SwordFlightTrailHistory.Point a,SwordFlightTrailHistory.Point b){
        return new SwordFlightTrailHistory.Point(a.x()-b.x(),a.y()-b.y(),a.z()-b.z());
    }
    /** Position seen by the real entity renderer: interpolation of fixed poses. */
    private static SwordFlightTrailHistory.Point flightPoint(double time,double speed,boolean curved){
        if(!curved)return new SwordFlightTrailHistory.Point(time*speed,0,0);
        double tick=Math.floor(time),fraction=time-tick,turn=Math.toRadians(4),radius=speed/(2*Math.sin(turn*.5));
        var a=new SwordFlightTrailHistory.Point(radius*Math.sin(tick*turn),0,radius*(1-Math.cos(tick*turn)));
        var b=new SwordFlightTrailHistory.Point(radius*Math.sin((tick+1)*turn),0,radius*(1-Math.cos((tick+1)*turn)));
        return a.lerp(b,fraction);
    }
    public static void run(){
        attachmentContinuity();
        longFlight(2.61,1400);longFlight(12,420);
        correctedWorldTime();
        highSpeedWorldClipping();
        for(double arc:new double[]{-21.17,0,.37,255.93,1024.77,1e8+.37}){
            double origin=SwordTrailPhase.origin(arc,42);
            float u=SwordTrailPhase.coordinate(arc,origin);
            for(double across:new double[]{-.75,-.4,0,.4,.75}){
                near(alpha(u,0,across),alpha(u,128,across),1e-8,"All material channels meet continuously at the exact time-period wrap");
                near(alpha(u,37.25,across),alpha((double)u+256,37.25,across),1e-8,"All channels, including both noise layers, share the exact arc period");
            }
        }
        for(long tick:new long[]{0,2559,2560,16777215,16777216,1L<<40,Long.MAX_VALUE-5}){
            float start=SwordTrailPhase.time(tick,0),middle=SwordTrailPhase.time(tick,.5F);
            require(start>=0&&middle<128,"Shader time remains bounded before float conversion in very old worlds");
            near(middle-start,.025,.00001,"Half-tick motion survives long-world float precision boundaries");
            float next=SwordTrailPhase.time(tick+1,0);
            near(wrap(next-start,128),.05,.00002,"Time advances continuously modulo the exact shared period");
        }
        float before=SwordTrailPhase.time(2559,.9999F),after=SwordTrailPhase.time(2560,0);
        for(double across:new double[]{-.6,-.25,0,.25,.6})
            near(alpha(137.41,before,across),alpha(137.41,after,across),.001,"An actual tick wrapping time cannot flash or narrow the plume");
        require(SwordTrailPhase.time(2560,Float.NaN)==0,"Invalid partial time uses a finite tick endpoint");
        System.out.println("Sword long-flight trail phase regression: "+checks+" checks passed");
    }
    private SwordTrailPhaseRegression(){}
}
