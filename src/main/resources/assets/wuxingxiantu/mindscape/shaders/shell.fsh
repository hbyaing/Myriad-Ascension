#version 150
// The finite color shell writes its real depth before world translucency.
// Its depth is deliberately separate from virtual membrane intersections.
in vec2 vUv;
out vec4 fragColor;
uniform sampler2D BackgroundSampler;
uniform mat4 ClipToLocal,LocalToClip;
uniform vec3 EyeLocal,ShellMin,ShellMax;
void main(){
 vec4 nearH=ClipToLocal*vec4(vUv*2.-1.,-1.,1.);
 vec3 ro=EyeLocal,rd=normalize(nearH.xyz/nearH.w-ro);
 vec3 safe=mix(vec3(-1.),vec3(1.),step(vec3(0.),rd))*max(abs(rd),vec3(.000001));
 vec3 va=(ShellMin-ro)/safe,vb=(ShellMax-ro)/safe;
 vec3 lo=min(va,vb),hi=max(va,vb);
 float entry=max(max(lo.x,lo.y),lo.z),exit=min(min(hi.x,hi.y),hi.z);
 if(exit<max(entry,0.))discard;
 float t=entry>.001?entry:exit;
 if(t<=.001)discard;
 vec4 clip=LocalToClip*vec4(ro+rd*t,1.);
 if(clip.w<=0.)discard;
 gl_FragDepth=clamp(clip.z/clip.w*.5+.5,0.,1.);
 fragColor=vec4(texture(BackgroundSampler,vUv).rgb,1.);
}
