#version 150
uniform sampler2D Sampler0;
uniform vec2 OutputSize;

// U is cumulative arc reduced by a common 256-block mesh origin. Every
// channel repeats at 256 blocks / 128 seconds, so rebases never change phase.
uniform float EffectTime;
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
vec4 swordMaterial(){
    if(trailCoord.y>11.5){
        float band=floor((trailCoord.y+.001)*.5)*2.0;
        float v=trailCoord.y-band,across=v*2.0-1.0;
        float arc=trailCoord.x,edge=max(0.0,1.0-abs(across));
        float flow=arc+EffectTime*6.0;
        float n=periodicNoise(vec2(flow*2.0,across*3.5),512.0);
        n+=.5*periodicNoise(vec2(flow*4.0+5.0,across*8.0),1024.0);
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
        float cloud=periodicNoise(vec2(arc*(154.0/256.0)+EffectTime*(462.0/128.0),cos(angle)*2.0+sin(angle)*1.2),154.0);
        float veins=pow(.5+.5*sin(angle*5.0+cloud*8.0-flowAngle(arc,65.0,-81.0)),8.0);
        vec3 rgb=mix(vertexColor.rgb,vec3(.9,1.0,1.0),cloud*.7)+veins*vec3(.65,.9,1.0);
        return vec4(rgb,(.30+.45*cloud+.5*veins)*vertexColor.a);
    }

    if(trailCoord.y>5.5){
        bool soft=trailCoord.y>9.5,hot=!soft&&trailCoord.y>7.5;
        float across=(trailCoord.y-(soft?10.0:hot?8.0:6.0))*2.0-1.0;
        float edge=max(0.0,1.0-abs(across));
        if(soft)return vec4(vertexColor.rgb,pow(edge,1.7)*vertexColor.a);
        if(hot)return vec4(mix(vertexColor.rgb,vec3(.96,1.0,1.0),.9)*2.2,pow(edge,2.1)*vertexColor.a);
        // Independent V9 prism transport, using periodic noise for long flights.
        float axis=trailCoord.x+EffectTime*(512.0/128.0);
        float density=0.0,weight=.52,period=64.0;
        for(int i=0;i<5;i++){
            density+=weight*periodicNoise(vec2(axis*period/256.0,across*3.1*period/64.0),period);
            period*=2.0;weight*=.49;
        }
        float fibers=pow(.5+.5*sin(across*19.0+density*5.0-axis*6.2831853*(64.0/256.0)),2.0);
        vec3 spectrum=.5+.5*cos(6.2831853*(vec3(12.0,13.0,15.0)*axis/256.0+density*.27+vec3(.07,.31,.58)));
        vec3 color=mix(vertexColor.rgb,spectrum*1.6,.52);
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
    fragColor=material;
    if(EmissionPass>0.5){
        float band=floor((trailCoord.y+.001)*.5)*2.0;
        float gain=band==14.0?0.0:band==12.0?.14:band==18.0?.08:band==22.0?.22:band==16.0?.40:1.0;
        fragColor=vec4(material.rgb*material.a*gain,max(-viewPosition.z/128.0,0.0));
    }
}
