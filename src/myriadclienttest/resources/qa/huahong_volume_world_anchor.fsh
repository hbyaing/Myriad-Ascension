#version 150
// Original three-dimensional path field, independent of the local reference shader.
uniform sampler2D Sampler0;
uniform mat4 InverseWorldToClip;
uniform vec2 OutputSize;
uniform vec3 BoundsMin;
uniform vec3 BoundsMax;
uniform vec3 NoiseOrigin;
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

float hashCell(vec3 cell){
    cell=mod(cell,64.0);
    return fract(sin(dot(cell,vec3(19.173,73.719,41.137)))*15731.743);
}
float noise3(vec3 p){
    vec3 cell=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);
    float a=mix(hashCell(cell),hashCell(cell+vec3(1,0,0)),f.x);
    float b=mix(hashCell(cell+vec3(0,1,0)),hashCell(cell+vec3(1,1,0)),f.x);
    float c=mix(hashCell(cell+vec3(0,0,1)),hashCell(cell+vec3(1,0,1)),f.x);
    float d=mix(hashCell(cell+vec3(0,1,1)),hashCell(cell+vec3(1,1,1)),f.x);
    return mix(mix(a,b,f.y),mix(c,d,f.y),f.z);
}
vec4 noiseGradient(vec3 p){
    vec3 cell=floor(p),fraction=fract(p),f=fraction*fraction*(3.0-2.0*fraction);
    vec3 derivative=6.0*fraction*(1.0-fraction);
    float a=hashCell(cell),b=hashCell(cell+vec3(1,0,0));
    float c=hashCell(cell+vec3(0,1,0)),d=hashCell(cell+vec3(1,1,0));
    float e=hashCell(cell+vec3(0,0,1)),g=hashCell(cell+vec3(0,1,1));
    float h=hashCell(cell+vec3(1,1,1)),j=hashCell(cell+vec3(1,0,1));
    float low=mix(mix(a,b,f.x),mix(c,d,f.x),f.y),high=mix(mix(e,j,f.x),mix(g,h,f.x),f.y);
    float dx=mix(mix(b-a,d-c,f.y),mix(j-e,h-g,f.y),f.z)*derivative.x;
    float dy=mix(mix(c-a,d-b,f.x),mix(g-e,h-j,f.x),f.z)*derivative.y;
    return vec4(mix(low,high,f.z),dx,dy,(high-low)*derivative.z);
}
float filteredNoise(vec3 p,float footprint){
    return mix(noise3(p),.5,smoothstep(.25,.85,footprint));
}
float clouds(vec3 p,float footprint){
    return filteredNoise(p,footprint)*.57
        +filteredNoise(p*2.0+vec3(13,7,21),footprint*2.0)*.28
        +filteredNoise(p*4.0+vec3(5,29,11),footprint*4.0)*.15;
}
vec3 reconstruct(vec2 uv,float depth){
    vec4 p=InverseWorldToClip*vec4(uv*2.0-1.0,depth*2.0-1.0,1.0);
    return p.xyz/p.w;
}
vec2 rayBox(vec3 origin,vec3 direction){
    vec3 safe=sign(direction)*max(abs(direction),vec3(.000001));
    safe+=vec3(equal(safe,vec3(0)))*.000001;
    vec3 a=(BoundsMin-origin)/safe,b=(BoundsMax-origin)/safe;
    vec3 low=min(a,b),high=max(a,b);
    return vec2(max(low.x,max(low.y,low.z)),min(high.x,min(high.y,high.z)));
}
vec4 field(vec3 p,vec3 ray,float stepLength,vec3 pixelX,vec3 pixelY){
    float footprint=max(stepLength,max(length(pixelX),length(pixelY)));
    float envelope=0.0,headCore=0.0;
    for(int i=0;i<31;i++){
        if(i>=PathCount-1)break;
        vec3 a=PathPoints[i].xyz,b=PathPoints[i+1].xyz,delta=b-a;
        float t=clamp(dot(p-a,delta)/max(dot(delta,delta),.000001),0.0,1.0);
        vec3 q=a+delta*t;float squared=dot(p-q,p-q);
        float candidateArc=mix(PathPoints[i].w,PathPoints[i+1].w,t);
        float candidateAge=mix(PathAges[i],PathAges[i+1],t);
        float candidateBehind=max(0.0,PathPoints[0].w-candidateArc);
        float candidateTail=1.0-smoothstep(PathLength*.72,PathLength,candidateBehind);
        float candidateLife=pow(max(0.0,1.0-candidateAge/ShapeProfile.y),1.4);
        if(candidateTail*candidateLife<.001)continue;
        float growth=clamp(max(candidateAge/ShapeProfile.y,candidateBehind/40.0),0.0,1.0);
        float candidateRadius=.025+ShapeProfile.x*(.10+ShapeProfile.w*.012+pow(growth,.9)*ShapeProfile.z*3.9);
        // Every segment contributes a continuous spherical support. The union
        // stays continuous where nearest-segment ownership changes, including
        // folded history. Never carry a winning segment's arc into the noise.
        float support=exp(-squared/(candidateRadius*candidateRadius)*3.2)*candidateTail*candidateLife;
        envelope=max(envelope,support);
        float coreRadius=.085+.018*min(candidateBehind,3.0);
        float core=exp(-squared/(coreRadius*coreRadius))
            *exp(-candidateBehind*.65)*(1.0-smoothstep(3.0,6.0,candidateBehind));
        headCore=max(headCore,core*candidateLife*candidateTail);
    }
    if(envelope<.0001&&headCore<.0001)return vec4(0);
    vec3 advection=vec3(0,EffectTime*.5,0);
    vec3 world=(p+NoiseOrigin)*.5;
    vec4 warpX=noiseGradient(world+advection),warpY=noiseGradient(world.yzx+advection+17.0),warpZ=noiseGradient(world.zxy+advection+31.0);
    vec3 displacement=vec3(warpX.x,warpY.x,warpZ.x)-.5;
    vec3 gradientX=warpX.yzw*.5,gradientY=warpY.yzw.zxy*.5,gradientZ=warpZ.yzw.yzx*.5;
    float cloud=clouds(world+displacement*1.8-advection,footprint*.5);
    float density=envelope*pow(smoothstep(.39,.67,cloud),2.2)*(.18+.82*Strength);
    // Both constraints live in absolute world space. Arc rebasing and nearest
    // path-segment changes cannot reset or duplicate a vein at a row boundary.
    vec3 veinDomain=world*.5+displacement*.18-advection;
    vec4 first=noiseGradient(veinDomain),second=noiseGradient(world.yzx*.5+displacement*.2+vec3(7,19,29));
    // Convolve narrow isosurfaces with the ray/pixel footprint. Preserve their
    // integrated energy while widening sub-step features instead of selecting
    // a single maximum hit, which creates bright rectangular sampling bands.
    vec3 firstGradient=first.yzw*.25+.18*(first.y*gradientX+first.z*gradientY+first.w*gradientZ);
    vec3 secondGradient=second.yzw.zxy*.25+.2*(second.y*gradientX+second.z*gradientY+second.w*gradientZ);
    // Convert scalar offsets to orthogonal world distances. Fixed noise-value
    // widths otherwise expand into luminous blobs near a noise stationary point.
    float firstSlope=max(length(firstGradient),.0001),secondSlope=max(length(secondGradient),.0001);
    vec3 normalX=firstGradient/firstSlope,secondNormal=secondGradient/secondSlope;
    float correlation=clamp(dot(normalX,secondNormal),-.999,.999);
    float sine=sqrt(max(.001,1.0-correlation*correlation));
    vec3 normalY=(secondNormal-correlation*normalX)/sine;
    vec2 rayChange=vec2(dot(normalX,ray),dot(normalY,ray))*stepLength;
    vec2 xChange=vec2(dot(normalX,pixelX),dot(normalY,pixelX));
    vec2 yChange=vec2(dot(normalX,pixelY),dot(normalY,pixelY));
    const float veinRadius=.035;
    // Keep the cross covariance: the ray footprint is a line, not a square.
    // det compensation preserves energy in both transverse dimensions.
    vec2 variance=vec2(veinRadius*veinRadius)+(rayChange*rayChange+xChange*xChange+yChange*yChange)/12.0;
    float covariance=(rayChange.x*rayChange.y+xChange.x*xChange.y+yChange.x*yChange.y)/12.0;
    float determinant=max(veinRadius*veinRadius*veinRadius*veinRadius,variance.x*variance.y-covariance*covariance);
    float hue=noise3(world*.5+vec3(23,3,41));
    float vein=0.0;vec3 veinLight=vec3(0);
    for(int j=0;j<3;j++)for(int k=0;k<3;k++){
        vec2 levels=vec2(.38)+vec2(float(j),float(k))*.12;
        vec2 distanceToLine=vec2((first.x-levels.x)/firstSlope,(second.x-levels.y)/secondSlope);
        distanceToLine.y=(distanceToLine.y-correlation*distanceToLine.x)/sine;
        float squaredDistance=(variance.y*distanceToLine.x*distanceToLine.x
            +variance.x*distanceToLine.y*distanceToLine.y-2.0*covariance*distanceToLine.x*distanceToLine.y)/determinant;
        float line=exp(-.5*max(0.0,squaredDistance))*(veinRadius*veinRadius)/sqrt(determinant)*envelope
            *smoothstep(.08,.30,sine);
        float pink=mod(float(j+k),2.0)*(.75+.25*smoothstep(.40,.68,hue));
        vec3 color=mix(vec3(.12,1.8,3.4),vec3(3.4,.15,1.9),pink);
        vein+=line;veinLight+=color*line;
    }
    float sigma=density*.85+vein*2.0+headCore*.8;
    float brightCloud=pow(smoothstep(.45,.72,cloud),3.0);
    vec3 cloudColor=vec3(.18,.80,1.8)+vec3(.90,1.20,1.4)*brightCloud;
    vec3 emission=cloudColor*density*1.6+veinLight*32.0+vec3(1.4,2.8,4.0)*headCore*5.0;
    return vec4(emission/max(sigma,.00001),sigma);
}
void main(){
    vec2 uv=gl_FragCoord.xy/OutputSize;
    vec3 nearPoint=reconstruct(uv,0.0),farPoint=reconstruct(uv,1.0);
    vec3 ray=normalize(farPoint-nearPoint);
    vec3 rayPixelX=dFdx(ray),rayPixelY=dFdy(ray);
    vec2 limits=rayBox(nearPoint,ray);
    float first=max(0.0,limits.x),last=limits.y;
    float sceneDepth=texture(Sampler0,uv).r;
    if(sceneDepth<.999999){vec3 scene=reconstruct(uv,sceneDepth);last=min(last,dot(scene-nearPoint,ray)-.015);}
    if(last<=first)discard;
    float stepLength=(last-first)/128.0,transmission=1.0;vec3 radiance=vec3(0);
    for(int i=0;i<128;i++){
        float distance=first+(float(i)+.5)*stepLength;
        vec3 p=nearPoint+ray*distance;
        vec4 sampleField=field(p,ray,stepLength,distance*rayPixelX,distance*rayPixelY);
        float alpha=1.0-exp(-sampleField.a*stepLength);
        radiance+=transmission*alpha*sampleField.rgb;
        transmission*=1.0-alpha;
        if(transmission<.015)break;
    }
    float alpha=clamp(1.0-transmission,0.0,.99);
    if(alpha<.002)discard;
    // This option owns its direct display conversion; it never enters the
    // sword/realm bloom buffer or a shared filmic tone mapper.
    vec3 display=vec3(1.0)-exp(-radiance*Exposure);
    // The accumulated emission already includes density and transmittance;
    // multiplying it by opacity again would erase thin luminous filaments.
    fragColor=vec4(display,alpha);
}
