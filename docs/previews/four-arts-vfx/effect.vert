#version 300 es
precision highp float;

in vec3 position;
in vec2 uv;
in vec3 tint;
in vec3 style;
uniform mat4 camera;
out vec2 shapeUV;
out vec3 color;
flat out vec3 params;

void main() {
    shapeUV = uv;
    color = tint;
    params = style;
    gl_Position = camera * vec4(position, 1.0);
}
