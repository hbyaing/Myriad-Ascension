#version 150

in vec2 shapeUV;
in vec3 color;
flat in vec3 params;
uniform float time;
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
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
    if(gl_FragCoord.z>texture(Sampler0,sceneUv).r+.000045) discard;
    vec2 p = shapeUV;
    float kind = params.x;
    float strength = params.y;
    float seed = params.z;
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
        float height=p.x*2.-1.;
        vec2 v=vec2(p.y/(.82-.14*smoothstep(-.19,.87,height)),height/.89);
        float r=length(v);
        float aa=max(fwidth(r)*.70,.025);
        float drop=1.-smoothstep(1.-aa,1.+aa,r);
        float depth=sqrt(max(0.,1.-r*r));
        float fresnel=pow(1.-depth,2.3);
        float highlight=exp(-dot((v-vec2(-.37,.42))*vec2(7.,4.5),(v-vec2(-.37,.42))*vec2(7.,4.5)));
        float reflectedArc=gaussian((r-.86)*13.)*smoothstep(.05,.96, dot(v,vec2(.54,-.84)));
        vec2 pixel=gl_FragCoord.xy-v*mix(1.,4.2,depth);
        pixel=clamp(pixel,vec2(.5),OutputSize-.5);
        vec3 scene=texture(Sampler1,pixel/OutputSize).rgb;
        vec3 glass=mix(scene,scene*hue,.22);
        glass=mix(glass,hue,.18*fresnel+.58*reflectedArc);
        glass=mix(glass,vec3(.84,1.,.88),highlight*.88);
        glass+=hue*(.035+.08*fresnel+.055*reflectedArc);
        float coverage=drop*strength;
        frag=vec4(glass*coverage,coverage);
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
    } else if(kind < 8.5) {
        float u=p.x, v=p.y;
        float coverage=smoothstep(0.,.14,u)*(1.-smoothstep(.70,1.,u));
        float skirt=gaussian((v+.17)*2.8);
        float lip=gaussian((v-.34)*7.3);
        float fold=gaussian((v+.43)*4.9);
        float air=.77+.23*sin(u*7.4-time*3.8+seed);
        alpha=coverage*(.30*skirt+.25*lip+.19*fold)*(1.-smoothstep(.70,1.,abs(v)))*air;
        core=coverage*(lip*.46+fold*.27)*air;
    } else {
        vec2 v=vec2(p.x*2.-1.,p.y);
        float a=atan(v.y,v.x), r=length(v);
        float crest=.73+.021*cos(a*4.+seed);
        float rim=gaussian((r-crest)*36.);
        float inner=gaussian((r-crest+.074)*17.);
        float shiver=.57+.43*pow(.5+.5*cos(a*2.+seed),2.);
        core=rim*.47*shiver;
        alpha=(rim*.48+inner*.13)*shiver*(1.-smoothstep(.83,1.,r));
    }
    alpha *= strength;
    vec3 light = mix(hue,vec3(1.0,0.98,0.90),clamp(core*0.62,0.0,0.81));
    frag = vec4(light*alpha + hue*core*strength*0.25,alpha);
}
