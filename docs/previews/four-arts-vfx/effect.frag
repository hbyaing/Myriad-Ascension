#version 300 es
precision highp float;

in vec2 shapeUV;
in vec3 color;
in vec3 world;
flat in vec3 params;
uniform float time;
uniform sampler2D opaqueScene;
uniform vec4 sampleBounds;
uniform vec3 viewRight, viewUp, viewBack;
layout(location=0) out vec4 frag;
layout(location=1) out vec4 emissionLayer;

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

float fracture(vec2 p) {
    vec2 cell=floor(p), f=fract(p);
    vec2 best=vec2(8.);
    for(int y=-1;y<=1;y++) for(int x=-1;x<=1;x++) {
        vec2 d=vec2(x,y), key=cell+d;
        vec2 jitter=vec2(hash(key),hash(key+vec2(18.3,-3.1)));
        float r=length(d+.16+.68*jitter-f);
        if(r<best.x) best=vec2(r,best.x);
        else best.y=min(best.y,r);
    }
    return best.y-best.x;
}

vec3 sceneAt(vec2 pixel) {
    return texture(opaqueScene,clamp(pixel,sampleBounds.xy,sampleBounds.zw)
        /vec2(textureSize(opaqueScene,0))).rgb;
}

void main() {
    vec2 p = shapeUV;
    float kind = params.x;
    float strength = params.y;
    float seed = params.z;
    if(kind>18.5) {
        vec2 v=vec2(p.x*2.-1.,p.y);
        vec2 pixel=floor(v*vec2(45.,14.)+seed*13.);
        float fleck=smoothstep(.54,.87,hash(pixel));
        float mist=gaussian(v.x*1.43)*gaussian(v.y*2.42);
        float alpha=mist*strength*(.19+fleck*.67);
        frag=vec4(mix(color,vec3(.68,.89,1.),fleck*.42)*alpha,alpha);
        emissionLayer=vec4(color*fleck*alpha*.25,alpha);
        return;
    }
    if(kind>17.5) {
        vec2 v=vec2(p.x*2.-1.,p.y);
        float radius=max(abs(v.x),abs(v.y));
        float fill=1.-smoothstep(.81,.99,radius);
        float bevel=gaussian((radius-.79)*16.);
        float gleam=smoothstep(.02,.73,v.y-v.x*.34);
        float alpha=strength*fill*(.30+.46*bevel+.23*gleam);
        vec3 glint=mix(color,vec3(.89,.98,1.),bevel*.56+gleam*.20);
        frag=vec4(glint*alpha,alpha);
        emissionLayer=vec4(glint*alpha*.39,alpha);
        return;
    }
    if(kind>16.5) {
        vec2 v=vec2(p.x*2.-1.,p.y);
        float box=max(abs(v.x),abs(v.y));
        float outer=gaussian((box-.79)*48.);
        float inner=gaussian((box-.67)*60.);
        float corner=step(.54,min(abs(v.x),abs(v.y)));
        float inlay=gaussian((abs(v.x)+abs(v.y)-.46)*46.);
        float alpha=strength*(outer*.48+inner*.21+inlay*.25+outer*corner*.38);
        frag=vec4(color*alpha,alpha);
        emissionLayer=vec4(color*alpha*.41,alpha);
        return;
    }
    if(kind>15.5) {
        float u=p.x;
        float curl=p.y+.24*sin(u*8.+seed);
        float chip=fract(noise(floor(vec2(u,curl)*vec2(30.,14.)))*5.7);
        float a=gaussian(curl*2.3)*smoothstep(0.,.14,u)*(1.-smoothstep(.72,1.,u));
        float alpha=strength*a*(.33+.64*smoothstep(.17,.54,chip));
        float seam=gaussian((curl+.17*sin(u*10.))*11.)*smoothstep(.30,.76,u);
        frag=vec4((mix(color*.63,vec3(.40,.28,.31),chip*.44)+vec3(.13,.06,.09)*seam)*alpha,alpha);
        emissionLayer=vec4(0.);
        return;
    }
    if(kind>14.5) {
        vec2 v=vec2(p.y,p.x*2.-1.);
        float shape=max(abs(v.x),abs(v.y));
        float corner=max(0.,abs(v.x)+abs(v.y)-1.58);
        float paper=1.-smoothstep(.96,1.,shape+corner);
        float frame=gaussian((shape+corner-.84)*38.);
        float stem=gaussian((v.x+.09)*26.)*(1.-smoothstep(.52,.68,abs(v.y)));
        float fork=gaussian((abs(v.x)-.33)*30.)*(1.-smoothstep(.19,.32,abs(v.y+.08)));
        float teeth=gaussian((abs(v.y+.06)-.36)*42.)*(1.-smoothstep(.32,.45,abs(v.x)));
        float seedMark=gaussian((v.x-.20*sin(seed*2.3))*29.)*gaussian((v.y-.47)*38.);
        float glyph=clamp(stem+fork+teeth+seedMark,0.,1.);
        float cap=gaussian((abs(v.y)-.72)*46.)*gaussian(v.x*4.);
        float alpha=paper*strength*(.19+.46*frame+.75*glyph+.37*cap);
        vec3 jade=mix(color,vec3(1.,.78,.44),cap*.80+frame*.14);
        frag=vec4(jade*alpha,alpha);
        emissionLayer=vec4(jade*alpha*(.20+.43*glyph),alpha);
        return;
    }
    if(kind>13.5) {
        float width=abs(p.y);
        float envelope=smoothstep(0.,.44,p.x)*(1.-smoothstep(.94,1.02,p.x));
        float field=noise(vec2(floor(p.x*85.),floor((p.y+1.)*9.))+vec2(seed,time*.8));
        float beam=gaussian(width*3.4)*(.40+.60*field);
        float blades=gaussian((width-.51)*21.)*(.42+.52*field);
        float alpha=envelope*(beam*.47+blades*.42)*strength;
        frag=vec4(color*alpha,alpha);
        emissionLayer=vec4(color*alpha*.37,alpha);
        return;
    }
    if(kind>12.5) {
        vec3 n=normalize(cross(dFdx(world),dFdy(world)));
        n*=dot(n,viewBack)<0.?-1.:1.;
        vec2 faceCoord=vec2(p.x*6.1,p.y*1.37)+vec2(seed*.21,seed*.37);
        float gap=fracture(faceCoord);
        float crack=1.-smoothstep(.012,.036+fwidth(gap)*.58,gap);
        crack*=.52+.48*noise(faceCoord*.72);
        float frost=noise(floor(faceCoord*9.)*.58+seed);
        float edge=pow(clamp(abs(p.y),0.,1.),24.);
        float collar=(1.-smoothstep(.288,.340,p.x));
        float shoulder=gaussian((p.x-.265)*123.)+gaussian((p.x-.174)*155.)*.64;
        float point=smoothstep(.69,.998,p.x)*.55;
        vec3 ray=refract(-viewBack,n,1./1.309);
        vec2 bend=vec2(dot(ray,viewRight),dot(ray,viewUp))*(16.+19.*collar);
        vec2 pixel=gl_FragCoord.xy;
        vec3 backdrop=vec3(sceneAt(pixel+bend*.96).r,sceneAt(pixel+bend).g,sceneAt(pixel+bend*1.05).b);
        float key=.58+.42*max(0.,dot(n,normalize(vec3(-.24,.86,.45))));
        float rim=pow(1.-clamp(dot(n,viewBack),0.,1.),3.)*.35+edge*.43;
        float front=gl_FrontFacing?1.:.32;
        vec3 ice=mix(backdrop*vec3(.53,.77,1.13),color*key,.49+.19*collar+.14*frost);
        ice=mix(ice,vec3(.54,.80,.98),collar*smoothstep(.37,.84,frost)*.53);
        vec3 glint=vec3(.59,.90,1.)*(.30*crack+rim*.87+.46*shoulder+point*.58)*front;
        float alpha=strength*(.70+.15*collar+.11*edge)*mix(.66,1.,front);
        frag=vec4((ice+glint)*alpha,alpha);
        emissionLayer=vec4(glint*strength*.44,alpha);
        return;
    }
    if(kind>11.5) {
        float y=p.y*.5+.5;
        float end=smoothstep(0.,.06,p.x)*(1.-smoothstep(.94,1.,p.x));
        float flicker=noise(vec2(p.x*48.+seed,y*4.-time*6.));
        float crest=.43+.48*noise(vec2(p.x*38.+time*2.8,seed+y*1.4));
        float body=(1.-smoothstep(crest-.17,crest+.04,y))*smoothstep(0.,.13,y);
        float fold=gaussian((y-(.22+.12*flicker))*9.);
        float hot=clamp(fold*(.55+.45*flicker)+.24*flicker,0.,1.);
        float alpha=end*strength*body*(.43+.49*flicker+.21*fold);
        vec3 flame=mix(color,vec3(1.,.85,.37),hot);
        frag=vec4(flame*alpha+flame*alpha*hot*.14,alpha);
        emissionLayer=vec4(flame*alpha*(.34+.33*hot),alpha);
        return;
    }
    if(kind>10.5 && kind<11.5) {
        float along=smoothstep(0.,.13,p.x)*(1.-smoothstep(.83,1.,p.x));
        float fold=p.y-.21*sin(p.x*6.3+seed*.8);
        float ridge=gaussian(fold*10.);
        float silk=gaussian(fold*2.8)*(.24+.21*sin(p.x*15.+seed));
        float filament=gaussian((fold-.34)*7.)*.20;
        float alpha=along*(ridge*.72+silk+filament)*strength;
        vec3 silver=mix(color*.71,vec3(.92,.99,1.),ridge*.83);
        frag=vec4(silver*alpha+color*ridge*along*strength*.19,alpha);
        emissionLayer=vec4(color*ridge*along*strength*.29,alpha);
        return;
    }
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
        emissionLayer=vec4(glow*.55,alpha);
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
    frag = vec4(light*alpha + hue*core*strength*0.12,alpha);
    emissionLayer=vec4(light*core*strength*.72,alpha);
}
