#version 150
in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform mat4 ClipMatrix;
out vec2 shapeUV;
out vec3 color;
flat out vec3 params;

void main() {
    float kind = floor(UV0.x * .5);
    float seed = floor((UV0.y + 1.01) * .25);
    shapeUV = vec2(UV0.x - kind * 2., UV0.y - seed * 4.);
    color = Color.rgb;
    params = vec3(kind,Color.a,seed);
    gl_Position = ClipMatrix * vec4(Position,1.);
}
