#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;

uniform mat4 WorldProjection;

out vec2 uv;
out float viewDistance;
flat out float phase;

void main() {
    gl_Position = WorldProjection * vec4(Position, 1.0);
    uv = UV0;
    viewDistance = length(Position);
    phase = Color.r;
}
