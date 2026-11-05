#version 150
// LOCAL REFERENCE ADAPTATION: current minecraft-huahong meteor-volume.js V13.
// User-supplied material provenance is retained in docs/testing/THREE_FORM_FIDELITY_20261105.md.
// Local acceptance build only; do not include this adaptation in a public MIT release.
uniform sampler2D Sampler0;
uniform mat4 InverseWorldToClip;
uniform vec2 OutputSize;
uniform vec3 BoundsMin;
uniform vec3 BoundsMax;
uniform mat3 WorldToMaterial;
uniform vec4 PathPoints[32];
uniform float PathAges[32];
uniform int PathCount;
uniform float PathLength;
uniform float Strength;
uniform float Exposure;
uniform float EffectTime;
// Width, lifetime seconds, transverse spread, head/shell scale.
uniform vec4 ShapeProfile;
in vec2 screenUv;
out vec4 fragColor;



// LOCAL USER-SUPPLIED Meteor Test111 adaptation; not for public MIT release.
float hhSmooth(float a,float b,float x){float q=clamp((x-a)/(b-a),0.,1.);return q*q*(3.-2.*q);}
// colors
const vec3 C_BLACK   = vec3(0.);
const vec3 C_WHITE   = vec3(1.);
const vec3 C_BLUE    = vec3(.1, .6, 1.);
const vec3 C_CYAN    = vec3(.4, .8, 1.);
const vec3 C_PURPLE  = vec3(.6, .1, .9);
const vec3 C_YELLOW  = vec3(1., .82, .2);
const vec3 C_ORANGE  = vec3(1., .8, .4);
const vec3 C_BG      = vec3(.2, .23, .33);
const vec3 C_GRAY    = vec3(.5);

// hh_palette constants
const vec3 P_A = vec3(.5);
const vec3 P_B = vec3(.5);
const vec3 P_C = vec3(1.);
const vec3 P_D = vec3(0., .33, .67);

// ---------- noise ----------
vec3 hh_hash33(vec3 p) {
    p=mod(p,64.0);
    p = vec3(
        dot(p, vec3(127.1, 311.7,  74.7)),
        dot(p, vec3(269.5, 183.3, 246.1)),
        dot(p, vec3(113.5, 271.9, 124.6))
    );
    return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
}

float hh_hash12(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * 0.1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

float hh_perlin3D(vec3 p) {
    vec3 pi = floor(p), pf = p - pi,
    w = pf*pf*(3. - 2.*pf);
    vec2 e = vec2(1,0);

    return mix(
        mix(
            mix(dot(hh_hash33(pi), pf),
                dot(hh_hash33(pi + e.xyy), pf - e.xyy), w.x),
            mix(dot(hh_hash33(pi + e.yxy), pf - e.yxy),
                dot(hh_hash33(pi + e.xxy), pf - e.xxy), w.x),
            w.y
        ),
        mix(
            mix(dot(hh_hash33(pi + e.yyx), pf - e.yyx),
                dot(hh_hash33(pi + e.xyx), pf - e.xyx), w.x),
            mix(dot(hh_hash33(pi + e.yxx), pf - e.yxx),
                dot(hh_hash33(pi + e.xxx), pf - e.xxx), w.x),
            w.y
        ),
        w.z
    );
}

float hh_fbm3D(vec3 p) {
    float v = 0., a = .5;
    for (int i = 0; i < 3; i++) {
        v += a * hh_perlin3D(p);
        p *= 2.;
        a *= .5;
    }
    return v*.5 + .5;
}

float hh_renderMeteorLayer(vec2 uv, float timeOffset, float warpIntensity) {
    uv *= vec2(8., 1.);
    uv.y -= EffectTime*3. + timeOffset;

    vec3 p = vec3(uv, EffectTime*2.5);
    vec2 w = vec2(hh_fbm3D(p + 1.), hh_fbm3D(p - 1.)) - .5;
    float n = hh_fbm3D(p + vec3(w * warpIntensity, 0.));

    return clamp((n - .5)*3. + .35, 0., 1.);
}

// https://iquilezles.org/articles/palettes/
vec3 hh_palette(float t) {
    return P_A + P_B * cos(2. * 3.1415926535 * (P_C * t + P_D));
}

vec3 hh_renderLightning(vec2 uv, float timeOffset, vec3 glowColor) {
    float turbulence = hh_fbm3D(vec3(uv.y * 2., 0., EffectTime * 5. + timeOffset));
    float d = abs(uv.x + (turbulence - .5)*2.);

    vec3 c = C_GRAY * (.002 / (d + 1e-3)) + glowColor * (.02 / (d + 1e-3));
    return c * hhSmooth(1., .3, abs(uv.y));
}




vec3 reconstruct(vec2 uv,float depth){vec4 p=InverseWorldToClip*vec4(uv*2.-1.,depth*2.-1.,1.);return p.xyz/p.w;}
vec2 rayBox(vec3 origin,vec3 direction){
 vec3 safe=mix(vec3(-1.),vec3(1.),greaterThanEqual(direction,vec3(0.)))*max(abs(direction),vec3(.000001));
 vec3 a=(BoundsMin-origin)/safe,b=(BoundsMax-origin)/safe,lo=min(a,b),hi=max(a,b);
 return vec2(max(lo.x,max(lo.y,lo.z)),min(hi.x,min(hi.y,hi.z)));
}
vec4 field(vec3 p,vec3 ray,float stepLength,vec3 pixelX,vec3 pixelY){
 float best=1e6,weightSum=0.,ageSum=0.,radiusSum=0.;vec3 deltaSum=vec3(0.);
 for(int i=0;i<31;i++){
  if(i>=PathCount-1)break;
  vec3 a=PathPoints[i].xyz,b=PathPoints[i+1].xyz,d=b-a;
  float len2=dot(d,d);if(len2<1e-8)continue;
  float t=clamp(dot(p-a,d)/len2,0.,1.),behind=-mix(PathPoints[i].w,PathPoints[i+1].w,t);
  float realAge=mix(PathAges[i],PathAges[i+1],t)/ShapeProfile.y;
  float q=clamp(max(realAge,behind/PathLength),0.,1.);
  float radius=.025+ShapeProfile.x*(.10+ShapeProfile.w*.012+pow(q,.9)*ShapeProfile.z*3.9);
  vec3 delta=p-mix(a,b,t);float dist=length(delta)/radius;
  best=min(best,dist);float w=exp(-dist*dist*8.)*sqrt(len2);
  weightSum+=w;ageSum+=q*w;radiusSum+=radius*w;deltaSum+=delta*w;
 }
 if(best>1.08||weightSum<1e-10)return vec4(0.);
 float age=ageSum/weightSum,radius=radiusSum/weightSum;
 vec3 local=WorldToMaterial*(deltaSum/weightSum)/radius;
 vec2 section=local.xy;
 float angle=atan(section.y,section.x+1.e-7);
 float radial=length(section);
 vec2 uv=vec2(radial*.235,sqrt(max(.0001,age))*.94);
 // Retain the supplied material's scale relationships. Angular offsets make the
 // field vary through depth, rather than extruding an identical 2D image.
 float phase=sin(angle*2.0+age*3.)*.16;
 uv.x+=sin(3.14159265*(hh_perlin3D(vec3(uv*vec2(3.,2.)-vec2(0.,EffectTime*5.),1.3+phase))-.5-uv.y))*.025;
 float maskNoise=hh_fbm3D(vec3(uv*vec2(30.,10.)-vec2(0.,EffectTime*4.),EffectTime*5.+phase));
 float mask=hhSmooth(.25,0.,uv.x+(maskNoise-.5)*pow(uv.y,5.)*.1);
 vec3 base=vec3(.4,.8,1.)*hh_renderMeteorLayer(uv,phase*3.,1.5);
 vec2 colorUV=uv*vec2(15.,5.);colorUV.y-=EffectTime*4.;
 float raw=hh_fbm3D(vec3(colorUV,EffectTime*3.+phase));
 float sharp=clamp((raw-.5)*50.+.5,0.,1.);
 vec3 neon=mix(vec3(.4,.8,1.),hh_palette(raw*2.-EffectTime),hhSmooth(.25,.55,sharp));
 vec2 coreUV=uv*vec2(30.,10.);coreUV.y-=EffectTime*5.;
 coreUV.x+=(maskNoise-.5)*pow(uv.y,10.)*5.;
 vec3 pp=vec3(coreUV,EffectTime*4.+phase);
 float rawCore=hh_fbm3D(pp+vec3(hh_fbm3D(pp)*2.5,0,0));
 vec3 bright=vec3(2.)*hhSmooth(.55,.75,rawCore)+vec3(.4,.8,1.)*.5*hhSmooth(.3,.65,rawCore);
 bright+=.5*hhSmooth(0.,.3,rawCore)*hhSmooth(.1,0.,uv.x)*hhSmooth(1.,0.,uv.y);
 base*=clamp(vec3(.224,.557,1.)/vec3(.224,.557,1.),vec3(0.),vec3(3.));
 bright*=clamp(vec3(.902,.984,1.)/vec3(.902,.984,1.),vec3(0.),vec3(3.));
 vec3 emission=base*.8+neon*.2+bright;
 // Color accents come from the same advected material field as the body.
 // No independent sinusoidal paths or constant-width neon filaments.
 float crease=pow(max(0.,1.-abs(rawCore-.53)*17.),2.);
 float breakup=hhSmooth(.46,.64,raw)*hhSmooth(.35,.61,maskNoise);
 vec3 creaseColor=mix(vec3(.18,.63,1.),hh_palette(raw*2.-EffectTime),.65);
 emission+=creaseColor*crease*breakup*(.15+8.0*.025);
 float edge=1.-smoothstep(.68,1.02,best+(maskNoise-.5)*age*.20);
 float end=1.-smoothstep(.78,1.,age);
 float intensity=max(emission.r,max(emission.g,emission.b));
 float density=mask*edge*end*smoothstep(.12,.65,intensity)*(.28+.72*pow(radial,.7));
 emission*=mask*edge*end*(.60+.65*.62);
 return vec4(emission,density);

}

void main(){
 vec2 uv=gl_FragCoord.xy/OutputSize;
 vec3 nearPoint=reconstruct(uv,0.),farPoint=reconstruct(uv,1.),ray=normalize(farPoint-nearPoint);
 vec2 limits=rayBox(nearPoint,ray);float first=max(0.,limits.x),last=limits.y;
 float sceneDepth=texture(Sampler0,uv).r;
 if(sceneDepth<.999999){vec3 scene=reconstruct(uv,sceneDepth);last=min(last,dot(scene-nearPoint,ray)-.015);}
 // Tighten the proxy to conservative segment spheres. Keep all128 samples
 // for occupied support instead of spending them across the whole box.
 float occupiedNear=1e20,occupiedFar=-1e20;
 for(int k=0;k<31;k++){
  if(k>=PathCount-1)break;
  vec3 a=PathPoints[k].xyz,b=PathPoints[k+1].xyz;
  float qa=clamp(max(PathAges[k]/ShapeProfile.y,-PathPoints[k].w/PathLength),0.,1.);
  float qb=clamp(max(PathAges[k+1]/ShapeProfile.y,-PathPoints[k+1].w/PathLength),0.,1.);
  float radius=(.025+ShapeProfile.x*(.10+ShapeProfile.w*.012+pow(max(qa,qb),.9)*ShapeProfile.z*3.9))*1.08+length(b-a)*.5;
  vec3 c=(a+b)*.5-nearPoint;float t=dot(c,ray),r2=radius*radius-dot(c,c)+t*t;
  if(r2>=0.){float reach=sqrt(r2);occupiedNear=min(occupiedNear,t-reach);occupiedFar=max(occupiedFar,t+reach);}
 }
 first=max(first,occupiedNear);last=min(last,occupiedFar);
 if(last<=first)discard;
 float dt=(last-first)/128.,transmission=1.,peakWeight=0.;vec3 sum=vec3(0.),peakSum=vec3(0.);
 for(int i=0;i<128;i++){
  vec4 f=field(nearPoint+ray*(first+(float(i)+.5)*dt),ray,dt,vec3(0.),vec3(0.));
  float opacity=1.-exp(-f.a*dt*.65),score=dot(f.rgb,vec3(.2126,.7152,.0722));
  // Continuous bright-sample weighting retains V13 luminous ridges without a winner swap.
  float w=pow(max(score,0.),8.);peakWeight+=w;peakSum+=w*f.rgb;
  sum+=transmission*opacity*f.rgb;transmission*=1.-opacity;
  if(transmission<.01)break;
 }
 float opacity=1.-transmission;if(opacity<.0003)discard;
 vec3 mean=sum/max(.0001,opacity);
 vec3 color=mix(mean,peakWeight>1e-12?peakSum/peakWeight:mean,.78);
 // Source V13 direct conversion: no shared filmic map, gamma or extra Bloom.
 // This form is luminous: do not turn a long axial optical path into a dark disk.
 // Thin support fades continuously; dividing by an almost-zero opacity must
 // not turn individual ray hits into full-strength concentric plates.
 fragColor=vec4(color*Exposure*Strength*smoothstep(0.,.06,opacity),0.);
}
