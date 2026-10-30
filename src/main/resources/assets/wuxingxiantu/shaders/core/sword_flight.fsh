#version 150

// U is cumulative arc reduced by a common 256-block mesh origin. Every
// channel repeats at 256 blocks / 128 seconds, so rebases never change phase.
uniform float EffectTime;
in vec2 trailCoord;
in vec4 vertexColor;
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
    if(trailCoord.y>9.5){
        vec2 p=vec2(trailCoord.x,trailCoord.y-10.0)*2.0-1.0;
        float radius2=dot(p,p);
        float light=exp(-radius2*7.0)*(1.0-smoothstep(.25,1.0,radius2));
        return vec4(mix(vertexColor.rgb,vec3(1.0,.995,.96),light),light*vertexColor.a);
    }
    if(trailCoord.y>5.5){
        bool head=trailCoord.y>7.5;
        float across=(trailCoord.y-(head?8.0:6.0))*2.0-1.0;
        float width=max(.075,min(.22,fwidth(across)*.7));
        float core=exp(-across*across/(width*width));
        float veil=exp(-across*across*7.0)*.28;
        float flow=.78+.22*pow(.5+.5*sin(flowAngle(trailCoord.x,64.0,-256.0)+across*2.0),3.0);
        float edge=pow(max(0.0,1.0-abs(across)),1.5);
        // Head U=1 is the forward tip; the history is deliberately dimmer.
        float taper=head?(.10+.90*pow(clamp(trailCoord.x,0.0,1.0),4.0)):.65;
        float alpha=clamp((core+veil*flow)*edge*taper*vertexColor.a,0.0,.98);
        vec3 color=mix(vertexColor.rgb,vec3(1.0,.995,.94),core);
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
    vec4 material=swordMaterial();
    if(material.a<.003)discard;
    fragColor=material;
}
