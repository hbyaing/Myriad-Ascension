#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform mat4 InverseWorldToClip;
uniform vec2 OutputSize;
out vec4 fragColor;

float reciprocalDepth(vec2 uv, float depth) {
    return (InverseWorldToClip * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0)).w;
}

void main() {
    vec2 uv = gl_FragCoord.xy / OutputSize;
    float depth = texture(Sampler1, uv).r;
    float inverseDepth = reciprocalDepth(uv, depth);
    ivec2 size = textureSize(Sampler0, 0);
    vec2 samplePos = uv * vec2(size) - 0.5;
    ivec2 base = ivec2(floor(samplePos));
    vec2 fraction = fract(samplePos);
    vec3 color = vec3(0.0);
    float total = 0.0;
    for (int y = 0; y <= 1; ++y) {
        for (int x = 0; x <= 1; ++x) {
            ivec2 pixel = clamp(base + ivec2(x, y), ivec2(0), size - 1);
            vec2 tapUv = (vec2(pixel) + 0.5) / vec2(size);
            float tapDepth = texture(Sampler1, tapUv).r;
            if ((depth >= 0.999999) != (tapDepth >= 0.999999)) continue;
            float difference = abs(inverseDepth - reciprocalDepth(tapUv, tapDepth));
            float tolerance = max(abs(inverseDepth) * 0.05, 1e-6);
            float weight = (x == 0 ? 1.0 - fraction.x : fraction.x)
                         * (y == 0 ? 1.0 - fraction.y : fraction.y)
                         * (1.0 - smoothstep(tolerance * 0.25, tolerance, difference));
            color += texelFetch(Sampler0, pixel, 0).rgb * weight;
            total += weight;
        }
    }
    // Integration already produces additive linear radiance.
    fragColor = vec4(color / max(total, 1e-5), 0.0);
}
