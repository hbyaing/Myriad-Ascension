#version 150
uniform sampler2D Sampler0;
in vec2 texCoord;
out vec4 fragColor;
void main(){
    vec4 optical=texture(Sampler0,texCoord);
    float tau=max(optical.a,0.0);
    float opacity=1.0-exp(-min(tau,80.0));
    float mixture=tau>.0001?opacity/tau:1.0-tau*.5;
    fragColor=vec4(optical.rgb*mixture,opacity);
}
