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

float stroke(float distanceToLine,float width) {
    return 1.-smoothstep(width,width+max(fwidth(distanceToLine)*.80,.002),distanceToLine);
}

float petal(vec2 p,float height,float width) {
    float u=clamp(p.y/height,0.,1.);
    float edge=abs(p.x)-width*pow(max(0.,sin(u*3.141593)),.83);
    float gate=smoothstep(0.,.052,u)*(1.-smoothstep(.978,1.,u));
    return gate*(stroke(abs(edge),.011)+.14*(1.-smoothstep(-.014,.015,edge)));
}

float glyph(vec2 g,float variation) {
    float stem=stroke(abs(g.x+mix(-.26,.24,variation)),.052)*step(abs(g.y),.64);
    float rails=stroke(abs(abs(g.y)-.45),.045)*step(abs(g.x),.53);
    float cut=stroke(abs(g.y+.03),.045)*step(abs(g.x-.11),.36);
    float block=stroke(abs(max(abs(g.x-.21),abs(g.y+.12))-.22),.041);
    return clamp(rails+stem+mix(cut,block,variation)*.86,0.,1.);
}

void main() {
    vec2 p = shapeUV;
    float kind = params.x;
    float strength = params.y;
    float seed = params.z;
    if(kind>25.5) {
        vec2 v=vec2(p.y,p.x*2.-1.);
        float grain=noise(v*vec2(7.4,5.8)+vec2(seed,time*.58));
        float ripple=noise(v*12.6+vec2(-time*.49,seed*4.1));
        float field=gaussian(v.x*1.43)*gaussian(v.y*1.76);
        float alpha=strength*field*smoothstep(.22,.77,grain*.70+ripple*.30)*.48;
        frag=vec4(color*alpha,alpha);
        emissionLayer=vec4(color*alpha*.19,alpha);
        return;
    }
    if(kind>24.5) {
        vec2 v=vec2(p.y,p.x*2.-1.);
        float r=length(v), a=atan(v.y,v.x)+time*.41;
        float segment=smoothstep(.09,.19,abs(fract(a/6.283185*8.)-.5));
        float outer=stroke(abs(r-.77),.014)*segment;
        float inner=stroke(abs(r-.60),.008)*.62;
        float spoke=pow(.5+.5*cos(a*8.),19.)*smoothstep(.46,.60,r)*(1.-smoothstep(.83,.94,r));
        float center=gaussian(r*4.9)*.21;
        float alpha=strength*clamp(outer*.83+inner+spoke*.62+center,0.,.96);
        vec3 light=mix(color,vec3(.86,.99,1.),clamp(outer+spoke,0.,1.)*.83);
        frag=vec4(light*alpha,alpha);
        emissionLayer=vec4(light*alpha*.69,alpha);
        return;
    }
    if(kind>23.5) {
        vec2 v=vec2(p.x*2.-1.,p.y);
        float r=length(v), a=atan(v.y,v.x);
        float lobe=.72+.18*cos(a*6.);
        vec2 pixel=floor(v*vec2(35.,27.)+seed*13.);
        float flakes=smoothstep(.38,.87,hash(pixel));
        float edge=1.-smoothstep(lobe-.10,lobe+.06,r);
        float branch=pow(.5+.5*cos(a*6.),21.)*(1.-smoothstep(.70,.91,r));
        float twigs=stroke(abs(abs(sin(a*3.))-.75*fract(r*8.)),.028)*edge;
        float alpha=strength*(edge*(.19+.42*flakes)+.31*branch+.17*twigs);
        vec3 light=mix(color,vec3(.74,.95,1.),branch*.43+flakes*.28);
        frag=vec4(light*alpha,alpha);
        emissionLayer=vec4(light*alpha*.36,alpha);
        return;
    }
    if(kind>22.5) {
        vec2 v=vec2(p.x*2.-1.,p.y);
        float cell=max(abs(v.x),abs(v.y));
        float grain=hash(floor((v+1.)*4.)+seed*4.);
        float a=(1.-smoothstep(.87,.99,cell))*strength*(.48+.50*grain);
        frag=vec4(color*a,a);
        emissionLayer=vec4(0.);
        return;
    }
    if(kind>21.5) {
        float u=p.x;
        float spine=p.y-.24*sin(u*3.14159);
        float width=.62*pow(max(0.,sin(u*3.14159)),.91);
        float rim=stroke(abs(abs(spine)-width),.035);
        float vein=gaussian(spine*13.)*.39;
        float fill=(1.-smoothstep(-.013,.034,abs(spine)-width))*.30;
        float shape=(rim*.70+vein+fill)*smoothstep(.015,.11,u)*(1.-smoothstep(.91,1.,u));
        float alpha=clamp(shape*strength,0.,1.);
        vec3 light=mix(color,vec3(1.,.98,.85),rim*.73);
        frag=vec4(light*alpha,alpha);
        emissionLayer=vec4(light*alpha*.67,alpha);
        return;
    }
    if(kind>20.5) {
        float u=p.x;
        float side=abs(p.y);
        float envelope=smoothstep(0.,.050,u)*(1.-smoothstep(.91,1.,u));
        float trim=stroke(abs(side-.79),.045);
        float inner=stroke(abs(side-.65),.019)*.34;
        float tick=u*36.-time*.53+seed*.41;
        float runes=glyph(vec2((fract(tick)-.5)*2.,p.y),mod(floor(tick),2.));
        float foil=(1.-smoothstep(.74,.95,side))*(.12+.085*sin(u*15.-time*2.6));
        float tangent=abs(dot(normalize(world.xz),normalize(viewBack.xz)));
        float facing=.57+.43*smoothstep(0.,.43,tangent);
        float alpha=clamp(strength*envelope*facing*(trim*.91+inner+runes*.75+foil),0.,.94);
        vec3 light=mix(color,vec3(1.,.98,.83),max(trim,runes)*.84);
        frag=vec4(light*alpha,alpha);
        emissionLayer=vec4(light*alpha*(.27+.55*max(trim,runes)),alpha);
        return;
    }
    if(kind>19.5) {
        float h=p.x;
        float side=p.y;
        float weight=mix(.21,1.24,.5-.5*dot(vec2(cos(seed),sin(seed)),viewBack.xz));
        float fade=smoothstep(0.,.10,h)*(1.-smoothstep(.71,1.,h));
        float face=(1.-smoothstep(.42,1.,abs(side)))*.76+gaussian((side+.45)*16.)*.29;
        float detail=.71+.29*noise(vec2(floor(side*8.)+seed,h*1.5+time*.32));
        float alpha=fade*face*detail*strength*weight*.25;
        frag=vec4(color*alpha,alpha);
        emissionLayer=vec4(color*alpha*.52,alpha);
        return;
    }
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
        float r=length(v), a=atan(v.y,v.x);
        float a8=(fract(a/6.283185*8.+.5)-.5)*.785398;
        float b8=(fract(a/6.283185*8.)-.5)*.785398;
        float lotus=petal(vec2(r*sin(a8),r*cos(a8)-.061),.54,.104);
        float innerPetal=petal(vec2(r*sin(b8),r*cos(b8)-.046),.396,.091);
        float rings=stroke(abs(r-.805),.008)*.89+stroke(abs(r-.620),.005)*.56
            +stroke(abs(r-.903),.003)*.32+stroke(abs(r-.075),.006)*.52;
        float tile=a/6.283185*16.+.5;
        float mark=glyph(vec2((fract(tile)-.5)*2.36,(r-.715)*13.),mod(floor(tile),2.));
        float pin=stroke(abs(r*sin(b8)),.007)*smoothstep(.78,.82,r)*(1.-smoothstep(.925,.945,r));
        float cardinal=(fract(a/6.283185*4.+.5)-.5)*1.570796;
        vec2 seal=vec2(r*sin(cardinal),r*cos(cardinal)-.805);
        float knot=stroke(abs(abs(seal.x)+abs(seal.y)-.043),.006);
        knot+=stroke(abs(seal.x),.006)*step(abs(seal.y),.089);
        knot+=stroke(abs(seal.y),.006)*step(abs(seal.x),.064);
        float mist=gaussian((r-.81)*11.)*.13+gaussian(r*3.)*.11;
        float lines=max(max(lotus*.90,innerPetal*.63),max(rings,mark*.76+pin*.53+knot*.83));
        float alpha=clamp(strength*(lines+mist),0.,.95);
        vec3 light=mix(color,vec3(1.,.98,.83),clamp(lines,0.,1.)*.83);
        frag=vec4(light*alpha,alpha);
        emissionLayer=vec4(light*alpha*(.39+.28*lines),alpha);
        return;
    }
    if(kind>15.5) {
        float u=p.x;
        float curl=p.y+.24*sin(u*8.+seed);
        float chip=fract(noise(floor(vec2(u,curl)*vec2(30.,14.)))*5.7);
        float a=gaussian(curl*2.3)*smoothstep(0.,.14,u)*(1.-smoothstep(.72,1.,u));
        float alpha=strength*a*(.30+.66*smoothstep(.17,.54,chip));
        float seam=gaussian((curl+.17*sin(u*10.))*11.)*smoothstep(.30,.76,u);
        frag=vec4((mix(color*.61,vec3(.47,.23,.43),chip*.47)+vec3(.12,.056,.14)*seam)*alpha,alpha);
        emissionLayer=vec4(0.);
        return;
    }
    if(kind>14.5) {
        vec2 v=vec2(p.y,p.x*2.-.37);
        float bloom=0.;
        for(int i=-3;i<=3;i++) {
            float angle=float(i)*.329;
            vec2 q=vec2(dot(v,vec2(cos(angle),-sin(angle))),dot(v,vec2(sin(angle),cos(angle))));
            bloom=max(bloom,petal(q,1.15-abs(float(i))*.135,.184-abs(float(i))*.013));
        }
        float cup=gaussian((length(vec2(v.x,v.y*2.9))-.58)*26.)*step(v.y,0.);
        float alpha=clamp(strength*(bloom*.96+cup*.59+gaussian(length(v-vec2(0.,.35))*2.6)*.18),0.,.95);
        vec3 light=mix(color,vec3(1.,1.,.91),clamp(bloom,0.,1.)*.84);
        frag=vec4(light*alpha,alpha);
        emissionLayer=vec4(light*alpha*.77,alpha);
        return;
    }
    if(kind>13.5) {
        float width=abs(p.y);
        float envelope=smoothstep(0.,.51,p.x)*(1.-smoothstep(.92,1.02,p.x));
        float field=noise(vec2(floor(p.x*69.),floor((p.y+1.)*9.))+vec2(seed,time*.8));
        float pulse=.72+.28*sin(p.x*47.-time*6.+seed);
        float beam=gaussian(width*2.9)*(.39+.50*field);
        float blades=gaussian((width-.46-.10*sin(p.x*6.1+seed))*23.)*(.29+.54*field);
        float alpha=envelope*(beam*.39+blades*.44*pulse)*strength;
        vec3 light=mix(color,vec3(.76,.93,1.),blades*.36);
        frag=vec4(light*alpha,alpha);
        emissionLayer=vec4(light*alpha*.44,alpha);
        return;
    }
    if(kind>12.5) {
        vec3 n=normalize(cross(dFdx(world),dFdy(world)));
        n*=dot(n,viewBack)<0.?-1.:1.;
        vec2 faceCoord=vec2(p.x*5.7,p.y*1.13)+vec2(seed*.21,seed*.37);
        float gap=fracture(faceCoord);
        float microCrack=stroke(gap,.019)*smoothstep(.44,.72,noise(faceCoord*.79+seed*.29));
        float drift=abs(fract(p.x*2.32+p.y*.33+noise(faceCoord*1.27)*.28+seed*.163)-.5);
        float fault=stroke(drift,.013)*(1.-smoothstep(.77,.97,p.x));
        float crack=max(fault*.89,microCrack*.66);
        float frost=noise(floor(faceCoord*11.)*.66+seed);
        float edge=pow(clamp(abs(p.y),0.,1.),24.);
        float collar=(1.-smoothstep(.288,.340,p.x));
        float shoulder=gaussian((p.x-.265)*105.)+gaussian((p.x-.153)*118.)*.72
            +gaussian((p.x-.084)*153.)*.46;
        float point=smoothstep(.62,.998,p.x)*.74;
        vec3 ray=refract(-viewBack,n,1./1.309);
        vec2 bend=vec2(dot(ray,viewRight),dot(ray,viewUp))*(19.+23.*collar)*(1.+.12*frost);
        vec2 pixel=gl_FragCoord.xy;
        vec3 backdrop=vec3(sceneAt(pixel+bend*.96).r,sceneAt(pixel+bend).g,sceneAt(pixel+bend*1.05).b);
        float key=.58+.42*max(0.,dot(n,normalize(vec3(-.24,.86,.45))));
        float rim=pow(1.-clamp(dot(n,viewBack),0.,1.),3.)*.34+edge*.51;
        float front=gl_FrontFacing?1.:.32;
        float rib=gaussian((p.y+.08*sin(p.x*9.+seed))*6.)*smoothstep(.19,.93,p.x);
        vec3 ice=mix(backdrop*vec3(.49,.74,1.16),color*key,.47+.25*collar+.12*frost);
        ice=mix(ice,vec3(.57,.83,1.),collar*smoothstep(.32,.85,frost)*.58);
        vec3 glint=vec3(.61,.92,1.)*(.51*crack+rim*.89+.52*shoulder+point*.65+rib*.21)*front;
        float alpha=strength*(.68+.18*collar+.11*edge)*mix(.64,1.,front);
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
