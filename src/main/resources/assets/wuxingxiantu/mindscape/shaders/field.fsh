#version 150
// User-supplied full FBM shader (author/source not supplied), plus the reviewed
// continuous finite-membrane carrier. No sampled artwork or remote textures.
in vec2 vUv;
out vec4 fragColor;
uniform vec2 resolution;
uniform float seconds,purification;
uniform int stage;
uniform vec4 layers;
uniform mat4 ClipToLocal;
uniform vec3 EyeLocal;
uniform vec3 paletteGoldBase,paletteGoldHighlight,paletteRedBase,paletteSpiritBase,paletteSpiritEmission;
uniform float paletteSpiritStrength,paletteExposure;
#define iTime seconds
#define iResolution (vec3(resolution,1.))
const mat2 m = mat2(0.80, 0.60, -0.60, 0.80);

float noise(vec2 p) {
    return sin(p.x) * sin(p.y);
}

float fbm4(vec2 p) {
    float f = 0.0;
    f += 0.5000 * noise(p); p = m * p * 2.02;
    f += 0.2500 * noise(p); p = m * p * 2.03;
    f += 0.1250 * noise(p); p = m * p * 2.01;
    f += 0.0625 * noise(p);
    return f / 0.9375;
}

float fbm6(vec2 p) {
    float f = 0.0;
    f += 0.500000 * (0.5 + 0.5 * noise(p)); p = m * p * 2.02;
    f += 0.250000 * (0.5 + 0.5 * noise(p)); p = m * p * 2.03;
    f += 0.125000 * (0.5 + 0.5 * noise(p)); p = m * p * 2.01;
    f += 0.062500 * (0.5 + 0.5 * noise(p)); p = m * p * 2.04;
    f += 0.031250 * (0.5 + 0.5 * noise(p)); p = m * p * 2.01;
    f += 0.015625 * (0.5 + 0.5 * noise(p));
    return f / 0.96875;
}

vec2 fbm4_2(vec2 p) { return vec2(fbm4(p), fbm4(p + vec2(7.8))); }
vec2 fbm6_2(vec2 p) { return vec2(fbm6(p + vec2(16.8)), fbm6(p + vec2(11.5))); }

float func(vec2 q, out vec4 ron) {
    q += 0.03 * sin(vec2(0.27, 0.23) * iTime + length(q) * vec2(4.1, 4.3));
    vec2 o = fbm4_2(0.9 * q);
    o += 0.04 * sin(vec2(0.12, 0.14) * iTime + length(o));
    vec2 n = fbm6_2(3.0 * o);
    ron = vec4(o, n);
    float f = 0.5 + 0.5 * fbm4(1.8 * q + 6.0 * n);
    return mix(f, f*f*f*3.5, f * abs(n.x));
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 p = (2.0 * fragCoord - iResolution.xy) / iResolution.y;

    vec2 uv = p;
    for (float i = 1.0; i < 10.0; i++) {
        uv.x += 0.6 / i * cos(i * 2.5 * uv.y + iTime * 0.5);
        uv.y += 0.6 / i * cos(i * 1.5 * uv.x + iTime * 0.5);
    }

    vec4 on = vec4(0.0);
    float f = func(uv, on);

    // Black & White neon effect
    float col = f;              // Base brightness
    col = clamp(col * 3.0, 0.0, 1.0); // Boost contrast for glow

    // Lighting & shading
    float e = 2.0 / iResolution.y;
    vec4 kk;
    vec3 nor = normalize(vec3(func(uv + vec2(e,0.0), kk) - f,
                              2.0 * e,
                              func(uv + vec2(0.0,e), kk) - f));
    vec3 lig = normalize(vec3(0.9, 0.2, -0.4));
    float dif = clamp(0.3 + 0.7 * dot(nor, lig), 0.0, 1.0);
    float lin = (nor.y * 0.5 + 0.5) + 0.3 * dif; // Lighting for grayscale
    col *= lin;

    // Final contrast curve
    col = 1.0 - col;
    col = col * col * 1.2;

    fragColor = vec4(vec3(col), 1.0);
}

vec4 ellipsoidHit(vec3 ro,vec3 rd,vec3 center,vec3 axes){vec3 q=(ro-center)/axes,v=rd/axes;float a=dot(v,v),b=dot(q,v),c=dot(q,q)-1.,d=b*b-a*c;if(d<0.)return vec4(0.,0.,1.,1e5);float root=sqrt(max(d,0.)),nearT=(-b-root)/max(a,.000001),farT=(-b+root)/max(a,.000001);float t=nearT>.001?nearT:farT;if(t<=.001)return vec4(0.,0.,1.,1e5);vec3 p=ro+rd*t;vec3 normal=normalize((p-center)/(axes*axes));return vec4(normal,t);}
// Extended material exposes the SAME complete user's chain, plus its original
// ron and normal. Original mode above still calls the untouched mainImage.
struct UserMaterialState{float gray;vec4 ron;vec3 normal;float lin;float rawField;};
UserMaterialState completeUserMaterialState(vec2 domain){
 vec2 uv=domain;
 for(float i=1.0;i<10.0;i++){
  uv.x+=0.6/i*cos(i*2.5*uv.y+iTime*0.5);
  uv.y+=0.6/i*cos(i*1.5*uv.x+iTime*0.5);
 }
 vec4 on=vec4(0.0);float f=func(uv,on);
 float col=f;col=clamp(col*3.0,0.0,1.0);
 float e=2.0/iResolution.y;vec4 kk;
 vec3 nor=normalize(vec3(func(uv+vec2(e,0.0),kk)-f,2.0*e,func(uv+vec2(0.0,e),kk)-f));
 vec3 lig=normalize(vec3(0.9,0.2,-0.4));
 float dif=clamp(0.3+0.7*dot(nor,lig),0.0,1.0);
 float lin=(nor.y*0.5+0.5)+0.3*dif;col*=lin;
 col=1.0-col;col=col*col*1.2;
 UserMaterialState result;result.gray=col;result.ron=on;result.normal=nor;result.lin=lin;result.rawField=f;return result;
}
// One continuous material chart per finite membrane. A three-chart average of
// unrelated ron phases creates false grooves at chart boundaries; evaluate the
// complete user material only once in this chart instead. The rear pole lies
// outside finiteMask (all visible patches have 1+q.z > .09).
UserMaterialState projectedMaterialState(vec3 p,vec3 normal,float id,float scale){
 vec3 center=id<.5?vec3(3.,12.,17.):(id<1.5?vec3(-24.,22.,20.):vec3(40.,16.,30.));
 vec3 axes=id<.5?vec3(70.,43.,94.):(id<1.5?vec3(48.,25.,70.):vec3(29.,35.,73.));
 vec3 q=normalize((p-center)/axes);
 vec2 domain=q.xy/max(1.+q.z,.06)*axes.xy*1.4*scale;
 domain+=vec2(id*.43,-id*.61)+vec2(float(stage)*.013);
 return completeUserMaterialState(domain);
}
vec4 surfaceMaterial(vec3 p,vec3 normal,vec3 ray,float distance,int layerId){
 float id=float(layerId),scale=.018*(1.+float(stage)*.008);
 UserMaterialState sampled=projectedMaterialState(p,normal,id,scale);float value=clamp(sampled.gray,0.,1.2);float body=pow(clamp(value,0.,1.),.74);
 float finiteMask=smoothstep(-36.,-20.,p.z)*(1.-smoothstep(105.,126.,p.z));float localSupport=1.;
 if(layerId==1)localSupport=smoothstep(5.,15.,p.y)*(1.-smoothstep(6.,30.,p.x));
 if(layerId==2)localSupport=smoothstep(10.,22.,p.x)*smoothstep(-5.,3.,p.y)*(1.-smoothstep(45.,55.,p.y));
 float support=finiteMask*localSupport*smoothstep(.008,.10,abs(dot(normal,ray)));
 float tangent=.74+.26*abs(dot(normal,normalize(vec3(-.4,.65,-.36))));
 float layerWeight=layerId==0?1.:(layerId==1?.25:.22);float redOpacity=(.035+.55*body)*support*layerWeight;
 // Internal spiritual-energy grooves are coordinates of this very fold field,
 // not another projected FBM picture. The flow advances along the same ron.
 float ronPhase=sampled.ron.z-sampled.ron.w;
 float phaseDrift=.040*sin(iTime*.105+sampled.ron.x*1.15-sampled.ron.y*.75+id*.37);
 float width=.055+.006*sin(sampled.ron.x*1.7+sampled.ron.y*.9);
 float foot=fwidth(ronPhase-phaseDrift);float filteredWidth=sqrt(width*width+foot*foot/12.);
 float groove=exp(-.5*pow((ronPhase-phaseDrift)/max(filteredWidth,.0001),2.))*width/max(filteredWidth,.0001);
 float midEnvelope=smoothstep(.025,.10,body)*(1.-smoothstep(.72,.98,body));
 float flowWeight=.82+.18*sin(iTime*.17+sampled.ron.x*.9+sampled.ron.y*.4);
 // A deep-blue albedo and a narrower blue emissive core share this fold's
 // ron/normal/body and coverage. Neither strength nor blueFlow changes alpha.
 float flowSupport=groove*midEnvelope;
 float spirit=clamp(smoothstep(.12,.62,flowSupport)*.98*paletteSpiritStrength,0.,1.)*layers.y;
 float spiritCore=smoothstep(.50,.90,flowSupport)*paletteSpiritStrength*layers.y;
 float foldShade=clamp(.55+.35*sampled.lin+.10*sampled.normal.y,.45,1.10);
 vec3 redBase=mix(vec3(.075,.002,.020)*.16,paletteRedBase,body);
 vec3 blueBase=paletteSpiritBase*(.35+.65*body)*foldShade*flowWeight;
 vec3 blueLight=paletteSpiritEmission*spiritCore*foldShade*flowWeight;
 vec3 redColor=mix(redBase,blueBase,spirit)*tangent;
 redColor+=vec3(.95,.025,.15)*pow(body,3.)*.25*(1.-spirit);
 redColor+=vec3(1.,.15,.30)*pow(body,7.)*.12*(1.-spirit);
 redColor+=blueLight*(.14+.32*body);redColor*=support*layerWeight;
 // Gold occupies the broad folded surface without a central opening mask.
 // Body values modulate the gold, rather than leaving every midtone white.
 float goldPattern=smoothstep(.04,.34,body);
 float goldOpacity=clamp((.38+.62*goldPattern)*support*layerWeight*2.2,0.,.96);
 vec3 goldPigment=mix(paletteGoldBase,paletteGoldHighlight,goldPattern)+vec3(.35,.20,.04)*pow(body,3.);
 vec3 bluePigment=paletteSpiritBase*(.55+.65*goldPattern)*foldShade*flowWeight;
 vec3 goldColor=(mix(goldPigment,bluePigment,spirit)+blueLight*(.18+.40*goldPattern))*goldOpacity;
 float fade=exp(-max(distance-18.,0.)*.004);redColor*=fade;
 // The internal flow has no independent coverage: removing the fold carrier
 // removes its internal energy too. blueFlow never changes either alpha.
 if(layers.x<.5)return vec4(0.);
 vec3 color=mix(redColor,goldColor,purification);float opacity=mix(redOpacity,goldOpacity,purification);
 return vec4(color,clamp(opacity,0.,1.));
}

void main(){
 vec4 nearH=ClipToLocal*vec4(vUv*2.-1.,-1.,1.);
 vec3 ro=EyeLocal,rd=normalize(nearH.xyz/nearH.w-ro);
 vec3 color=mix(vec3(.010,.0015,.006),vec3(2.05,2.02,1.95),purification);
 vec4 a=ellipsoidHit(ro,rd,vec3(3.,12.,17.),vec3(70.,43.,94.));
 vec4 b=ellipsoidHit(ro,rd,vec3(-24.,22.,20.),vec3(48.,25.,70.));
 vec4 c=ellipsoidHit(ro,rd,vec3(40.,16.,30.),vec3(29.,35.,73.));
 int ai=0,bi=1,ci=2;
 if(a.w<b.w){vec4 h=a;a=b;b=h;int k=ai;ai=bi;bi=k;}
 if(b.w<c.w){vec4 h=b;b=c;c=h;int k=bi;bi=ci;ci=k;}
 if(a.w<b.w){vec4 h=a;a=b;b=h;int k=ai;ai=bi;bi=k;}
 if(a.w<300.){vec4 material=surfaceMaterial(ro+rd*a.w,a.xyz,rd,a.w,ai);color=color*(1.-material.a)+material.rgb;}
 if(b.w<300.){vec4 material=surfaceMaterial(ro+rd*b.w,b.xyz,rd,b.w,bi);color=color*(1.-material.a)+material.rgb;}
 if(c.w<300.){vec4 material=surfaceMaterial(ro+rd*c.w,c.xyz,rd,c.w,ci);color=color*(1.-material.a)+material.rgb;}
 float fog=(.5+.5*sin(rd.x*2.+rd.y*3.+seconds*.012))*.04;
 float side=smoothstep(.08,.48,abs(rd.x))*(1.-smoothstep(.6,.95,rd.y));
 color=mix(color,mix(vec3(.075,.002,.020),vec3(1.70,1.68,1.62),purification)*.40,fog*side);
 float exposure=mix(1.65,1.45,purification)*paletteExposure;
 color*=1.-.10*dot(vUv-.5,vUv-.5);
 color=1.-exp(-max(color,vec3(0.))*exposure);
 color=pow(color,vec3(.78));fragColor=vec4(color,1.);
}
