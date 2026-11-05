#version 150

uniform sampler2D Sampler0;
uniform mat4 InverseClip;
uniform vec2 OutputSize;
uniform vec3 Origin;
uniform vec2 Forward;
uniform vec3 Wave;
uniform vec4 FloorLeft, FloorRight;
out vec4 frag;

const float halfAngle=.9424778;
const float range=6.5;
const int steps=78;

float hash(vec3 p) {
    p=fract(p*.1031);
    p+=dot(p,p.yzx+33.33);
    return fract((p.x+p.y)*p.z);
}

float noise(vec3 p) {
    vec3 cell=floor(p), t=fract(p);
    t=t*t*(3.-2.*t);
    vec2 lo=mix(vec2(hash(cell),hash(cell+vec3(0,1,0))),
                vec2(hash(cell+vec3(1,0,0)),hash(cell+vec3(1,1,0))),t.x);
    vec2 hi=mix(vec2(hash(cell+vec3(0,0,1)),hash(cell+vec3(0,1,1))),
                vec2(hash(cell+vec3(1,0,1)),hash(cell+vec3(1,1,1))),t.x);
    return mix(mix(lo.x,lo.y,t.y),mix(hi.x,hi.y,t.y),t.z);
}

float gaussian(float x) { return exp(-x*x); }

vec3 local(vec3 point) {
    return vec3(dot(point.xz,Forward),point.y,dot(point.xz,vec2(-Forward.y,Forward.x)));
}

vec3 unproject(vec2 ndc,float depth) {
    vec4 p=InverseClip*vec4(ndc,depth,1.);
    return p.xyz/p.w;
}

float floorHeight(float section) {
    float index=clamp(section*7.,0.,6.9999);
    int a=int(index), b=a+1;
    float left=a<4?FloorLeft[a]:FloorRight[a-4];
    float right=b<4?FloorLeft[b]:FloorRight[b-4];
    return mix(left,right,fract(index));
}

vec2 intersectBox(vec3 point,vec3 direction,vec3 lower,vec3 upper) {
    vec3 inv=1./mix(direction,vec3(1.e-6),lessThan(abs(direction),vec3(1.e-6)));
    vec3 a=(lower-point)*inv,b=(upper-point)*inv;
    vec3 near=min(a,b),far=max(a,b);
    return vec2(max(max(near.x,near.y),near.z),min(min(far.x,far.y),far.z));
}

vec3 fireColor(float heat) {
    vec3 red=mix(vec3(.36,.019,.008),vec3(.98,.18,.025),smoothstep(.10,.54,heat));
    vec3 amber=mix(red,vec3(1.,.59,.095),smoothstep(.43,.86,heat));
    return mix(amber,vec3(1.,.94,.65),smoothstep(.77,1.08,heat));
}

void main() {
    vec2 uv=gl_FragCoord.xy/OutputSize, ndc=uv*2.-1.;
    vec3 near=unproject(ndc,-1.),far=unproject(ndc,1.);
    vec3 ray=normalize(far-near),point=local(near-Origin),direction=local(ray);
    float depth=texture(Sampler0,uv).r;
    float stop=depth<.999999?dot(unproject(ndc,depth*2.-1.)-near,ray):length(far-near);
    float age=Wave.x, front=Wave.y, power=Wave.z;
    float flank=min(front+.74,range)*.82;
    vec2 box=intersectBox(point,direction,vec3(max(0.,front-1.58)*.57,-1.62,-flank),
                          vec3(min(range,front+.86),3.61,flank));
    float start=max(0.,box.x), end=min(stop,box.y);
    if(end<=start) discard;

    float stride=(end-start)/float(steps);
    float jitter=.20+.60*hash(vec3(gl_FragCoord.xy,0.));
    float transmission=1.;
    vec3 radiance=vec3(0.);
    for(int i=0;i<steps;i++) {
        vec3 p=point+direction*(start+(float(i)+jitter)*stride);
        float radius=length(p.xz);
        if(radius<max(.03,front-1.67) || radius>min(range,front+.84)) continue;
        float angle=atan(p.z,p.x+.00001);
        if(abs(angle)>=halfAngle) continue;
        float u=.5+.5*angle/halfAngle, y=p.y-floorHeight(u);
        float edge=smoothstep(0.,.13,u)*(1.-smoothstep(.87,1.,u));
        float lobe=.5+.5*sin(u*35.-age*7.9+sin(u*13.+age*4.)*.73);
        float crestHeight=(.74+1.36*pow(lobe,1.38))*pow(sin(u*3.1415927),.57)
            *smoothstep(.15,2.28,front);
        if(y<.006 || y>crestHeight+.31) continue;
        float h=y/max(.19,crestHeight);
        float ripple=.14*sin(u*39.+age*10.)+.065*sin(u*79.-age*9.);
        float settle=.34*smoothstep(4.3,range,front);
        float bodyCenter=front-.39-settle+ripple+.58*smoothstep(.43,.95,h);
        float bodyWidth=mix(.72,.27,smoothstep(.28,1.04,h));
        float radial=(radius-bodyCenter)/bodyWidth;
        float vertical=(h-.43)/.67;
        float body=sqrt(radial*radial+vertical*vertical);

        // An attached forward roll creates a round hook with its own front and back.
        float hookHeight=.72+.12*lobe;
        float hookRadius=min(range-.15,front+.33-settle+ripple);
        vec2 hook=vec2((radius-hookRadius)/(.31+.095*lobe),(h-hookHeight)/(.29+.07*lobe));
        float shape=min(body,length(hook)+.07);
        if(shape>1.32) continue;
        vec3 flow=vec3(u*20.+2.1*y,radius*3.-age*5.4,y*3.6-age*7.1);
        flow.x+=.70*sin(radial*3.1-h*4.5+u*19.+age*3.);
        float roll=noise(flow);
        float textureDetail=noise(flow*2.22+vec3(3.2,age*1.36,-2.8));
        float turbulent=roll*.69+textureDetail*.31;
        float fill=1.-smoothstep(.62,1.14,shape+(turbulent-.48)*.51);
        float foot=smoothstep(.005,.106,y);
        float strand=gaussian((turbulent-.57)*7.1);
        float base=gaussian((h-.14-.07*roll)*3.65);
        float inner=gaussian(radial*1.65)*(1.-smoothstep(.56,1.05,h));
        float rangeFade=1.-smoothstep(range-.31,range,radius);
        float density=fill*edge*foot*rangeFade*(.43+.61*roll+.26*strand);
        float slice=1.-exp(-density*stride*power*5.3);
        float heat=.20+.38*base+.21*strand+.19*textureDetail+.18*inner-.13*h;
        vec3 tint=fireColor(heat)*(1.02+.16*strand);
        radiance+=tint*slice*transmission;
        transmission*=1.-slice;
        if(transmission<.018) break;
    }
    float opacity=1.-transmission;
    if(opacity<.004) discard;
    frag=vec4(radiance,opacity);
}
