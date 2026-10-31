#version 150
uniform sampler2D Sampler0;
uniform vec2 OutputSize;

// U is cumulative arc reduced by a common 256-block mesh origin. Every
// channel repeats at 256 blocks / 128 seconds, so rebases never change phase.
uniform float EffectTime;
uniform sampler2D SmokeAtlas;
uniform float EmissionPass;
in vec2 trailCoord;
in vec4 vertexColor;
in vec3 viewPosition;
out vec4 fragColor;

float hash21(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
float periodicHash(vec2 p,float period){return hash21(vec2(mod(p.x,period),p.y));}
float periodicNoise(vec2 p,float period){
    vec2 cell=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);
    return mix(mix(periodicHash(cell,period),periodicHash(cell+vec2(1,0),period),f.x),
               mix(periodicHash(cell+vec2(0,1),period),periodicHash(cell+vec2(1,1),period),f.x),f.y);
}
float flowAngle(float arc,float spaceHarmonic,float timeHarmonic){
    return 6.28318530718*fract(arc*spaceHarmonic/256.0+EffectTime*timeHarmonic/128.0);
}
float layeredFlow(vec2 p,float period){
    float value=0.0,weight=.52;
    for(int i=0;i<5;i++){value+=weight*periodicNoise(p,period);p*=2.0;period*=2.0;weight*=.49;}
    return value;
}
vec2 warpFlow(vec2 p,float period){
    return vec2(layeredFlow(p,period),layeredFlow(p+vec2(period*.375,7.2),period))-.5;
}
float detailMask(vec2 uv,vec2 velocity,bool flow){
    float a=fract(EffectTime*.125),b=fract(a+.5);
    float wa=1.0-abs(1.0-a*2.0),wb=1.0-wa;
    vec2 p=clamp(fract(uv-velocity*a),vec2(.002),vec2(.998));
    vec2 q=clamp(fract(uv-velocity*b),vec2(.002),vec2(.998));
    vec2 offset=vec2(flow?.75:.5,0.0),scale=vec2(.25,.5);
    return texture(SmokeAtlas,offset+p*scale).a*wa+texture(SmokeAtlas,offset+q*scale).a*wb;
}
vec4 swordMaterial(){
    if(trailCoord.y>11.5){
        float band=floor((trailCoord.y+.001)*.5)*2.0;
        float v=trailCoord.y-band,across=v*2.0-1.0;
        float arc=trailCoord.x,edge=max(0.0,1.0-abs(across));
        float flow=arc+EffectTime*6.0;
        float n=periodicNoise(vec2(flow*2.0,across*3.5),512.0);
        n+=.5*periodicNoise(vec2(flow*4.0+5.0,across*8.0),1024.0);
        if(band==34.0){
            // Concentrated long rainbow V5: directional body and weak inner fibers.
            float axis=arc-EffectTime*2.0;
            float cloud=periodicNoise(vec2(axis*.5,across*2.7),128.0)*.67;
            cloud+=periodicNoise(vec2(axis+3.0,across*6.1),256.0)*.33;
            float fiber=pow(.5+.5*sin(across*21.0+cloud*3.0+flowAngle(arc,32.0,-64.0)),3.0);
            return vec4(vertexColor.rgb*(1.0+.18*fiber),pow(edge,1.75)*(.48+.42*cloud+.10*fiber)*vertexColor.a);
        }
        if(band==32.0){
            // Full local nucleus retained; this envelope extends over its nose.
            vec2 p=vec2(arc,v)*2.0-1.0;
            float radius2=dot(p,p);
            float rays=exp(-abs(p.x)*22.0-abs(p.y)*3.0)+exp(-abs(p.y)*22.0-abs(p.x)*3.0);
            float glow=(exp(-radius2*9.0)+.24*rays)*(1.0-smoothstep(.80,1.0,radius2));
            return vec4(vertexColor.rgb*1.8,glow*vertexColor.a);
        }
        if(band==30.0){
            // A stable world-arc birth cell drives each absorbing smoke parcel.
            // Transparent density lobes preserve holes between the short plumes.
            float seed=mod(floor(arc/4.0),64.0);
            vec2 p=vec2(fract(arc),v)*2.0-1.0;
            float phase=flowAngle(0.0,0.0,8.0);
            vec2 curl=vec2(sin(seed*2.17+phase),cos(seed*1.31-phase))*.13;
            vec2 q=p+curl*(1.0-dot(p,p)*.18);
            float smoke=periodicNoise(vec2(q.x*3.2+seed*4.0+EffectTime*.5,q.y*3.2+.35*sin(phase+seed)),64.0)*.58;
            smoke+=periodicNoise(vec2(q.x*7.1+seed*8.0-EffectTime,q.y*7.1+.25*cos(phase*.5+seed)),128.0)*.29;
            smoke+=periodicNoise(vec2(q.x*15.0+seed*16.0+EffectTime*2.0,q.y*15.0),256.0)*.13;
            float lobe=1.0-smoothstep(.20,1.22,dot(q,q));
            float density=smoothstep(.23,.76,smoke)*lobe;
            float variant=mod(seed,4.0);
            vec2 tile=vec2(mod(variant,2.0),floor(variant/2.0));
            vec2 spriteUV=clamp(p*.5+.5+curl*.08,vec2(.005),vec2(.995));
            float sprite=texture(SmokeAtlas,(spriteUV+tile)*vec2(.25,.5)).a;
            float alpha=1.0-exp(-density*sprite*vertexColor.a*3.0);
            vec3 soot=mix(vertexColor.rgb,vec3(.0035,.0012,.002),smoke*.36);
            return vec4(soot,alpha);
        }
        if(band==24.0){
            // Curved combustion sheets: broad flame, narrow hot folds and dark gaps.
            float axis=arc-EffectTime*4.0;
            float wave=sin(flowAngle(arc,48.0,-96.0));
            float flame=periodicNoise(vec2(axis*.5,across*2.4+wave*.35),128.0)*.57;
            flame+=periodicNoise(vec2(axis,across*6.0),256.0)*.29;
            flame+=periodicNoise(vec2(axis*2.0,across*12.0),512.0)*.14;
            float fold=exp(-pow(across-.16*wave-.12*(flame-.5),2.0)*45.0)*smoothstep(.35,.78,flame);
            float erosion=detailMask(vec2(across*.48+.5,arc*.125),vec2(.10,.24),false);
            float tongues=smoothstep(.15,.67,flame+edge*.23)*(.50+.75*erosion);
            vec3 rgb=mix(vertexColor.rgb*.80,vec3(1.0,.76,.31),fold*.65)+fold*vec3(.18,.15,.10);
            return vec4(rgb,pow(edge,.85)*tongues*(.42+.58*flame)*vertexColor.a);
        }
        if(band==26.0){
            // Open feather V4: warped broad density, stretched fibres and one fold.
            vec2 p=vec2(arc*.5-EffectTime,across*2.10);
            vec2 warp=warpFlow(p,128.0);
            float coarse=layeredFlow(p+warp*1.16,128.0);
            float fiber=layeredFlow(vec2(arc-EffectTime*2.0+warp.y*2.8,across*15.0),256.0);
            float torn=smoothstep(.16,.56,coarse*.70+fiber*.30+edge*.12);
            float detail=detailMask(vec2(across*.44+.5,arc*.125),warp*.18+vec2(0.0,.10),false);
            float fold=pow(max(0.0,1.0-abs(across+.17+warp.x*.30)*2.3),2.6);
            vec3 rgb=vertexColor.rgb*(.70+.27*coarse+.26*fold)+vec3(.035,.065,.090)*fold;
            return vec4(rgb,pow(edge,.70)*torn*(.49+.31*fiber+.20*detail)*vertexColor.a);
        }
        if(band==28.0){
            // V7 open silk: broad density patches and curled ragged edges.
            vec2 p=vec2(arc*.25+EffectTime*.5,across*2.2);
            vec2 warp=warpFlow(p,64.0);
            float n=layeredFlow(p+warp*.82,64.0);
            float large=layeredFlow(vec2(arc*.125+EffectTime*.25,across*.90)+warp*.35,32.0);
            float ragged=abs(across)+(.09+.14*(1.0-edge))*sin(flowAngle(arc,48.0,96.0)+n*6.0);
            float fringe=(1.0-smoothstep(.64,.99,ragged))*smoothstep(.22,.63,large+edge*.29);
            float fibers=pow(smoothstep(.30,.79,layeredFlow(vec2(arc*.0625+EffectTime*.125,across*8.0+n*.9)+warp*.35,16.0)),2.8);
            float ridges=pow(max(0.0,1.0-abs(n-.49)*7.0),4.0);
            float silk=detailMask(vec2(across*.48+.5,arc*.125),warp*.18,true);
            vec3 rgb=vertexColor.rgb*(.65+n*.25)+vec3(.20,.16,.07)*(fibers*.13+ridges*.12);
            return vec4(rgb,fringe*(.38+large*.50)*(.60+fibers*.22+ridges*.20)*(.82+.18*silk)*vertexColor.a);
        }
        if(band==22.0)return vec4(vertexColor.rgb*1.3,pow(edge,1.7)*vertexColor.a);
        if(band==14.0||band==20.0){
            vec2 p=vec2(fract(arc),v)*2.0-1.0;
            float smoke=periodicNoise(p*4.3+vec2(EffectTime*.5,floor(arc)*.13),64.0);
            smoke+=.5*periodicNoise(p*9.1-vec2(EffectTime,.6*sin(flowAngle(0.0,0.0,8.0))),128.0);
            float body=pow(max(0.0,1.0-dot(p,p)*.73),1.5);
            float alpha=body*smoothstep(.18,.85,smoke)*vertexColor.a;
            return vec4(vertexColor.rgb*(band==20.0?2.8:1.0),alpha);
        }
        if(band==12.0){
            float tongues=smoothstep(.16,.80,n-abs(across)*.24);
            float core=exp(-across*across*12.0)*smoothstep(.5,1.2,n);
            vec3 rgb=mix(vertexColor.rgb,vec3(1.0,.92,.53),core*.78)*(1.0+core*.4);
            return vec4(rgb,pow(edge,.7)*tongues*vertexColor.a);
        }
        if(band==16.0){
            float fiber=pow(.5+.5*sin(across*13.0+n*4.0-flowAngle(arc,81.0,244.0)),4.0);
            float torn=smoothstep(.1,.45,n+edge*.4);
            return vec4(vertexColor.rgb*(1.4+fiber*.9),pow(edge,1.1)*(.65+.35*fiber)*torn*vertexColor.a);
        }
        // Circular tube coordinates wrap continuously; no camera-facing density sheet.
        float angle=v*6.2831853;
        float cloud=periodicNoise(vec2(arc*.25+EffectTime*.5,cos(angle)*2.0+sin(angle)*1.2),64.0)*.67;
        cloud+=periodicNoise(vec2(arc*.5-EffectTime,cos(angle*2.0)*3.1+sin(angle*3.0)),128.0)*.33;
        float veins=pow(.5+.5*sin(angle*7.0+cloud*7.0-flowAngle(arc,64.0,-80.0)),12.0);
        float density=smoothstep(.25,.82,cloud+.10*sin(flowAngle(arc,32.0,-64.0)+angle*3.0));
        vec3 rainbow=.5+.5*cos(6.2831853*(arc*vec3(11.0,13.0,17.0)/256.0+cloud*.3+angle/6.2831853+vec3(.03,.29,.56)));
        vec3 rgb=mix(vertexColor.rgb,vec3(.37,.76,.93),cloud*.58)+veins*mix(vec3(.35,.60,.74),rainbow*.65,.36);
        return vec4(rgb,(.12+.55*density+.32*veins)*vertexColor.a);
    }

    if(trailCoord.y>5.5){
        bool soft=trailCoord.y>9.5,hot=!soft&&trailCoord.y>7.5;
        float across=(trailCoord.y-(soft?10.0:hot?8.0:6.0))*2.0-1.0;
        float edge=max(0.0,1.0-abs(across));
        if(soft)return vec4(vertexColor.rgb,pow(edge,1.7)*vertexColor.a);
        if(hot)return vec4(mix(vertexColor.rgb,vec3(.93,1.0,1.0),.75)*2.1,pow(edge,2.1)*vertexColor.a);
        // Independent V9 prism transport, using periodic noise for long flights.
        float axis=trailCoord.x+EffectTime*(512.0/128.0);
        float density=0.0,weight=.52,period=64.0;
        for(int i=0;i<5;i++){
            density+=weight*periodicNoise(vec2(axis*period/256.0,across*3.1*period/64.0),period);
            period*=2.0;weight*=.49;
        }
        float fibers=pow(.5+.5*sin(across*19.0+density*5.0-axis*6.2831853*(64.0/256.0)),2.0);
        vec3 spectrum=.5+.5*cos(6.2831853*(vec3(12.0,13.0,15.0)*axis/256.0+density*.27+vec3(.07,.31,.58)));
        vec3 color=mix(vertexColor.rgb,spectrum*1.25,.68);
        float alpha=pow(edge,1.8)*(.48+.52*fibers)*(.60+.40*density)*vertexColor.a;
        return vec4(color,alpha);
    }
    if(trailCoord.y>3.5){
        vec2 p=vec2(trailCoord.x,trailCoord.y-4.0)*2.0-1.0;
        float dotGlow=exp(-dot(p,p)*5.0);
        float facets=exp(-abs(p.x+p.y)*18.0)*exp(-abs(p.x-p.y)*2.3)
                    +exp(-abs(p.x-p.y)*18.0)*exp(-abs(p.x+p.y)*2.3);
        float a=min(.95,dotGlow*.70+facets*.26)*vertexColor.a;
        return vec4(mix(vertexColor.rgb,vec3(.94,1,1),dotGlow),a);
    }
    bool spiral=trailCoord.y>1.5;
    float across=(trailCoord.y-(spiral?2.0:0.0))*2.0-1.0;
    float arc=trailCoord.x;
    float coarse=periodicNoise(vec2(arc*(346.0/256.0)+EffectTime*(692.0/128.0),across*2.7),346.0);
    float edge=pow(max(0.0,1.0-abs(across)),1.4);
    // The white spine wanders very slightly; wider curl veins peel away and
    // reunite inside the same plume instead of becoming parallel laser lines.
    float spine=across-.024*sin(flowAngle(arc,72.0,153.0))-.015*sin(flowAngle(arc,148.0,312.0)+1.7);
    float coreWidth=max(spiral?.13:.12,min(.28,fwidth(across)*.65));
    float core=exp(-spine*spine/(coreWidth*coreWidth));
    float curlCenter=.31*sin(flowAngle(arc,34.0,72.0))+.12*sin(flowAngle(arc,72.0,153.0)+1.8);
    float curlDistance=across-curlCenter;
    float curl=exp(-curlDistance*curlDistance*42.0)
              *(.25+.75*pow(.5+.5*sin(flowAngle(arc,110.0,232.0)+across*2.1),2.0));
    float rolling=sin(flowAngle(arc,57.0,120.0)+across*6.5+coarse*1.8);
    float veil=exp(-across*across*4.6)*(.18+.29*coarse)
              *(.42+.58*pow(.5+.5*rolling,2.0));
    float ragged=smoothstep(.14,.63,coarse+(1.0-abs(across))*.43);
    float breaks=.74+.26*periodicNoise(vec2(arc*(614.0/256.0)+EffectTime*(614.0/128.0),across*4.0),614.0);
    float density=core*1.12+curl*.64+veil*.85;
    float alpha=clamp(density*edge*mix(ragged,1.0,core)*breaks*vertexColor.a,0.0,.96);
    vec3 blue=mix(vertexColor.rgb,vec3(.20,.60,.98),.30);
    vec3 icy=mix(blue,vec3(.70,.95,1.0),clamp(curl*.65+veil,0.0,1.0));
    vec3 color=mix(icy,vec3(.94,1.0,1.0),clamp(core*.92,0.0,1.0));
    return vec4(color,alpha);
}

void main(){
    vec2 screenUV=gl_FragCoord.xy/max(OutputSize,vec2(1.0));
    float sceneDepth=texture(Sampler0,screenUV).r;
    if(gl_FragCoord.z>sceneDepth+0.0000001)discard;
    vec4 material=swordMaterial();
    if(material.a<.003)discard;
    // Premultiplied light adds radiance; absorbing smoke keeps destination
    // attenuation. Ordinary sword bands retain their original alpha appearance.
    float materialBand=floor((trailCoord.y+.001)*.5)*2.0;
    bool luminous=materialBand>=6.0&&materialBand!=14.0&&materialBand!=30.0;
    fragColor=vec4(material.rgb*material.a,luminous?0.0:material.a);
    if(EmissionPass>0.5){
        float band=floor((trailCoord.y+.001)*.5)*2.0;
        float gain=(band==14.0||band==30.0)?0.0:band==12.0?.14:band==24.0?.12:band==26.0?.22:band==28.0?.18:band==34.0?.30:band==32.0?.60:band==18.0?.08:band==22.0?.22:band==16.0?.40:band==8.0?.52:band==6.0?.62:1.0;
        fragColor=vec4(material.rgb*material.a*gain,max(-viewPosition.z/128.0,0.0));
    }
}
