#version 150

in vec2 shapeUV;
in vec3 color;
flat in vec3 params;
uniform float time;
uniform sampler2D Sampler0;
uniform mat4 InverseClip;
in vec3 relativePosition;
uniform vec2 OutputSize;
out vec4 frag;

float gaussian(float x) { return exp(-x*x); }

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f*f*(3.0-2.0*f);
    return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),
               mix(hash(i+vec2(0,1)),hash(i+1.0),f.x),f.y);
}

void main() {
    vec2 sceneUv=gl_FragCoord.xy/OutputSize;
    float worldDepth=texture(Sampler0,sceneUv).r;
    vec2 p=shapeUV;
    float kind=params.x;
    float strength=params.y;
    float seed=params.z;
    if(kind>8.5) {
        if(worldDepth>=.999999) discard;
        vec4 position=InverseClip*vec4(sceneUv*2.-1.,worldDepth*2.-1.,1.);
        if(abs(position.w)<.00001) discard;
        vec3 delta=position.xyz/position.w-relativePosition;
        float contact=1.-smoothstep(.023,.145,length(delta));
        vec2 patch=vec2(p.x*2.-1.,p.y);
        float fade=1.-smoothstep(.18,1.,length(patch));
        float opacity=contact*fade*strength;
        if(opacity<.004) discard;
        frag=vec4(color*opacity*.95,opacity*.75);
        return;
    }
    if(gl_FragCoord.z>worldDepth+.000045) discard;
    float alpha;
    float core;
    vec3 hue = color;
    if (kind < 0.5) {
        float lateral = abs(p.y);
        float flow = 0.74 + 0.26*sin(p.x*31.0 - time*10.0 + seed);
        float along = smoothstep(0.0,0.15,p.x)*(1.0-smoothstep(0.79,1.0,p.x));
        core = exp(-lateral*lateral*22.0)*along*flow;
        alpha = (0.37*core + 0.25*exp(-lateral*lateral*3.8))*along*flow;
    } else if (kind < 1.5) {
        float u=p.x;
        float spine=.17*sin(u*3.14159)-.07*u;
        float side=p.y-spine;
        float width=pow(max(0.,sin(u*3.14159)),.84)*(.83-.13*u);
        width*=1.-.055*cos(u*55.);
        float distanceToEdge=abs(side)-width;
        float edgeAA=max(fwidth(distanceToEdge),.022);
        float silhouette=(1.-smoothstep(-edgeAA,edgeAA,distanceToEdge))
                *smoothstep(.015,.065,u)*(1.-smoothstep(.962,1.,u));
        float centerVein=gaussian(side*23.)*smoothstep(.035,.23,u)*(1.-smoothstep(.80,.99,u));
        float fork=abs(fract(u*5.6-abs(side)*.83)-.5);
        float lateralVeins=gaussian(fork*18.)*smoothstep(.06,.19,abs(side))*(1.-smoothstep(.38,.83,abs(side)));
        float fold=.70+.21*tanh(-side*7.)+.11*sin(u*5.1+seed);
        float rim=gaussian((distanceToEdge+.045)*18.);
        float detail=.52*centerVein+.24*lateralVeins+.32*rim;
        alpha=silhouette*(.31+.25*detail)*strength;
        vec3 blade=mix(hue*fold,vec3(.56,1.,.46),detail);
        vec3 glow=vec3(.35,.96,.28)*silhouette*(.16+.84*detail)*strength;
        frag=vec4(blade*alpha+glow*.32,alpha);
        return;
    } else if (kind < 2.5) {
        vec2 v=vec2(p.x*2.-1.,p.y);
        float r=length(v), a=atan(v.y,v.x);
        vec2 q=v*4.+vec2(seed*2.31,-time*.48);
        float n=noise(q)*.66+noise(q*2.18)*.34;
        float lobe=.69+.11*cos(a*5.+seed)+.067*cos(a*9.-seed*.8);
        float ink=lobe-r+(n-.55)*.25;
        float body=smoothstep(-.033,.045,ink)*(1.-smoothstep(.87,1.,r));
        float hem=gaussian((ink-.036)*25.)*smoothstep(.32,.74,n);
        float vein=gaussian((noise(q*1.77+vec2(8.3))-.48)*23.)*smoothstep(.21,.74,r);
        core=body*(hem*.59+vein*.21);
        alpha=body*(.67+.22*n)+hem*.18;
        hue=mix(vec3(.074,.014,.045),hue,clamp(hem*.77+vein*.22+n*.18,0.,1.));
    } else if (kind < 3.5) {
        vec2 v = vec2(p.x*2.0-1.0,p.y);
        float r = length(v);
        core = exp(-r*r*19.0);
        alpha = (0.64*core+0.27*exp(-r*r*4.8))*(1.0-smoothstep(0.76,1.0,r));
    } else if (kind < 4.5) {
        vec2 v=vec2(p.x*2.-1.,p.y);
        float r=length(v), a=atan(v.y,v.x)+time*.22;
        float hatch=pow(.5+.5*cos(a*36.),10.);
        float sigil=gaussian((r-.70)*86.)*.29;
        sigil+=gaussian((r-.62)*78.)*(.26+.59*hatch);
        sigil+=gaussian((r-.31-.042*cos(6.*a))*82.)*.46;
        if(seed<2.5) {
            float curl=.59+.19*sin(a+time*1.8);
            sigil=gaussian((r-curl)*40.)*(.47+.32*cos(a*2.-time*6.));
        }
        float ripple=fract(time*.78+seed*.17);
        sigil+=gaussian((r-.1-ripple*.74)*31.)*(1.-ripple)*.44;
        core=sigil*.42;
        alpha=sigil*.51+.22*exp(-r*r*5.7)*(1.-smoothstep(.73,1.,r));
    } else if (kind < 5.5) {
        float u=p.x;
        float envelope=smoothstep(0.,.22,u)*(1.-smoothstep(.80,1.,u));
        float flex=.21*sin(u*19.-time*4.6+seed)+.12*sin(u*33.+time*2.4+seed);
        float x=p.y+flex*sin(u*3.14159);
        float swath=exp(-x*x*4.6);
        float ridge=gaussian((x+.24+.09*sin(u*37.+time*7.))*11.);
        float fold=gaussian((x-.30+.12*sin(u*16.-time*4.))*6.);
        float grain=noise(vec2(u*37.-time*8.,x*4.+seed));
        alpha=envelope*(swath*(.19+.26*grain)+.33*ridge+.21*fold)*(1.-smoothstep(.67,1.,abs(p.y)));
        core=envelope*(.60*ridge+.30*fold)*(.58+.42*grain);
    } else if(kind < 6.5) {
        vec2 v=vec2(p.x*2.-1.,p.y);
        float a=atan(v.y,v.x)-time*1.5;
        float r=length(v);
        float lens=exp(-r*r*12.);
        float petal=gaussian((r-.33-.13*cos(4.*a))*27.);
        float ray=exp(-abs(v.x)*25.)*exp(-abs(v.y)*4.)+exp(-abs(v.y)*25.)*exp(-abs(v.x)*4.);
        core=lens*.89+petal*.35+ray*.22;
        alpha=(core*.76+exp(-r*r*6.)*.22)*(1.-smoothstep(.71,1.,r));
    } else if(kind < 7.5) {
        vec2 v=vec2(p.x*2.-1.,p.y);
        float ridge=abs(v.x+.26*v.y)*.77+abs(v.y+.21*v.x)*.93;
        float chip=1.-smoothstep(.70,.78,ridge);
        float facet=smoothstep(-.09,.07,v.x-v.y*.26);
        float edge=gaussian((ridge-.66)*27.);
        alpha=chip*(.78+.21*facet);
        core=chip*edge*(.28+.62*facet);
        hue=mix(vec3(.057,.013,.037),hue,clamp(facet*.39+edge*.46,0.,.94));
    } else {
        float u=p.x, v=p.y;
        float coverage=smoothstep(0.,.14,u)*(1.-smoothstep(.70,1.,u));
        float skirt=gaussian((v+.17)*2.8);
        float lip=gaussian((v-.34)*7.3);
        float fold=gaussian((v+.43)*4.9);
        float air=.77+.23*sin(u*7.4-time*3.8+seed);
        alpha=coverage*(.30*skirt+.25*lip+.19*fold)*(1.-smoothstep(.70,1.,abs(v)))*air;
        core=coverage*(lip*.46+fold*.27)*air;
    }
    alpha *= strength;
    vec3 light = mix(hue,vec3(1.0,0.98,0.90),clamp(core*0.62,0.0,0.81));
    frag = vec4(light*alpha + hue*core*strength*0.25,alpha);
}
