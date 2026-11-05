#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 OutputSize;
uniform float UseSceneDepth;
uniform float Travel;

in vec2 uv;
in float viewDistance;
flat in float phase;

out vec4 fragColor;

void main() {
    // RGB = inverse distance, distance travelled from the root, strand strength.
    float halfTexel = 0.5 / float(textureSize(Sampler0, 0).y);
    vec3 field = texture(Sampler0, vec2(uv.x, clamp(uv.y, halfTexel, 1.0 - halfTexel))).rgb;
    float d = (1.0 - field.r) * .064;
    float along = field.g * 4.8;
    float strength = field.b;
    float head = mod(Travel + phase * 5.78, 5.78) - .18;
    float age = head - along;
    float headOffset = (age - .018) / .092;
    float leading = exp(-headOffset * headOffset);
    float wake = smoothstep(-.04, .08, age) * exp(-max(age, 0.0) * 3.1);
    float flicker = .9 + .1 * sin(along * 96.0 + head * 37.0);
    float pulse = .075 + (.90 * leading + .61 * wake) * flicker;

    float halfWidth = mix(.0017, .0037, strength);
    float aa = clamp(fwidth(d) * .70, .00065, .004);
    float core = 1.0 - smoothstep(max(0.0, halfWidth - aa), halfWidth + aa, d);
    float nearGlow = exp(-pow(d / .013, 2.0));
    float spill = exp(-pow(d / .035, 2.0));
    vec3 emission = strength * pulse * (
        vec3(.63, .81, 1.0) * core
        + vec3(.13, .32, .77) * .48 * nearGlow
        + vec3(.07, .14, .33) * .22 * spill
    );
    float tolerance = max(.000002, min(.00003, fwidth(gl_FragCoord.z) * 2.0));
    if (UseSceneDepth > .5 && gl_FragCoord.z > texture(Sampler1, gl_FragCoord.xy / OutputSize).r + tolerance) discard;
    fragColor = vec4(emission * (1.0 - smoothstep(42.0, 64.0, viewDistance)), 0.0);
}
