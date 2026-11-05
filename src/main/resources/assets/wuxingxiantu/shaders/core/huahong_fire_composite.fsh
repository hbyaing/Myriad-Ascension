#version 150
uniform sampler2D Sampler0;

in vec2 texCoord;out vec4 fragColor;
vec3 filmic(vec3 c){return clamp((c*(2.51*c+.03))/(c*(2.43*c+.59)+.14),0.,1.);}
void main(){
 vec4 field=texture(Sampler0,texCoord);float opacity=1.-exp(-max(field.a,0.));
 if(max(field.r,max(field.g,field.b))<.000001)discard;
 bool torn=field.a>.000001;
 vec2 pixel=1./vec2(textureSize(Sampler0,0));vec3 halo=vec3(0.);
 for(int i=0;i<8;i++){float a=float(i)*.78539816339;halo+=texture(Sampler0,texCoord+vec2(cos(a),sin(a))*pixel*4.).rgb*.125;}
 // Subtract a mapped pedestal: faint emission remains smooth at zero instead
 // of amplifying quad corners through an unbounded gamma derivative.
 vec3 pedestal=vec3(.04);
 vec3 color=(pow(filmic((field.rgb+halo*.45)*.85+pedestal),vec3(1./2.2))-pow(filmic(pedestal),vec3(1./2.2)))*(torn?1.35:1.5);
 float coverage=torn?opacity:0.;
 fragColor=vec4(max(color,vec3(0.)),coverage);
}
