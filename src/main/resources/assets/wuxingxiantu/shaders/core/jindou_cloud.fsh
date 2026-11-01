#version 150

uniform sampler2D Sampler0;
uniform sampler2D SceneDepth;
uniform sampler2D TrailData;
uniform mat4 ClipToRelativeWorld;
uniform mat4 RelativeWorldToClip;
uniform mat3 WorldToCloud;
uniform vec4 Viewport;
// Both positions are world axes relative to the same 256-block render origin.
uniform vec3 CameraPos;
uniform vec3 CloudCenter;
uniform vec3 CloudRadii;
uniform vec3 NoiseOffset;
uniform vec3 DetailOffset;
uniform vec3 FlowPhase;
uniform vec3 HistoryNoiseOrigin;
uniform vec3 HistoryDetailOffset;
uniform vec3 LightDirection;
uniform float SkyLight;
uniform float DayLight;
uniform float Opacity;
uniform float DensityScale;
uniform float Extinction;
uniform float AmbientStrength;
uniform float SoftIntersection;
uniform float MotionAmount;
uniform float TrailLifetime;
uniform int TrailCount;
uniform int Steps;
uniform int DebugVolume;
out vec4 fragColor;

vec3 unproject(vec2 ndc, float depth) {
    vec4 p = ClipToRelativeWorld * vec4(ndc, depth, 1.0);
    return p.xyz / p.w;
}

vec2 intersectBox(vec3 origin, vec3 direction, vec3 lower, vec3 upper) {
    vec3 safeDir = mix(vec3(-1.0), vec3(1.0), greaterThanEqual(direction, vec3(0.0))) * max(abs(direction), vec3(0.000001));
    vec3 a = (lower - origin) / safeDir;
    vec3 b = (upper - origin) / safeDir;
    vec3 lo = min(a, b), hi = max(a, b);
    return vec2(max(max(lo.x, lo.y), lo.z), min(min(hi.x, hi.y), hi.z));
}

vec2 worley(vec3 p) {
    vec3 coord = fract(p) * 64.0 - 0.5;
    float slice = floor(coord.z);
    float z0 = mod(slice, 64.0), z1 = mod(slice + 1.0, 64.0);
    vec2 tile0 = vec2(mod(z0, 8.0), floor(z0 / 8.0));
    vec2 tile1 = vec2(mod(z1, 8.0), floor(z1 / 8.0));
    return mix(texture(Sampler0, (tile0 * 66.0 + coord.xy + 1.5) / 528.0).rg,
            texture(Sampler0, (tile1 * 66.0 + coord.xy + 1.5) / 528.0).rg, fract(coord.z));
}

float opticalScale(vec3 direction) {
    // Keep the first version's optical thickness when enlarging its world-space volume.
    return 2.0 * length(direction * vec3(1.35, 0.46, 1.05) / CloudRadii);
}

float lobe(vec3 q, vec3 center, vec3 radius) {
    vec3 d = (q - center) / radius;
    return 1.0 - dot(d, d);
}

float softUnion(float a, float b) {
    float h = max(0.16 - abs(a - b), 0.0) / 0.16;
    return max(a, b) + h * h * 0.04;
}

float cloudShape(vec3 q) {
    // Polar rolling has no four corner lobes. Its contour itself changes continuously.
    q.x *= 1.13;
    q.z *= 0.90;
    float angle = atan(q.z, q.x + 0.000001);
    float radius = 0.94 + 0.035 * sin(3.0 * angle + FlowPhase.x)
            + 0.025 * sin(5.0 * angle - FlowPhase.z) + 0.018 * sin(angle + FlowPhase.y);
    q.y += 0.045 * sin(q.x * 3.0 + FlowPhase.y) * sin(q.z * 3.0 + FlowPhase.z);
    float shape = lobe(q, vec3(0.0, -0.10, 0.0), vec3(radius, 0.78, radius));
    return softUnion(shape, lobe(q, vec3(0.0, 0.09 + 0.025 * sin(FlowPhase.y), 0.0), vec3(0.60, 0.66, 0.60)));
}

vec3 stretchRear(vec3 q, float motion) {
    // This inverse map is monotone for every speed, so acceleration cannot fold the density field.
    float rear = 1.0 - exp(-2.0 * max(0.0, -q.z));
    q.z /= 1.0 + 0.85 * motion * rear;
    float dilution = smoothstep(0.15, 0.95, -q.z) * pow(motion, 0.35);
    q.x /= 1.0 - 0.32 * dilution;
    q.y /= 1.0 - 0.12 * dilution;
    return q;
}

vec3 curvePoint(vec3 a, vec3 c, vec3 b, float t) {
    return mix(mix(a, c, t), mix(c, b, t), t);
}

float curveParameter(vec3 p, vec3 a, vec3 c, vec3 b) {
    vec3 acceleration = 2.0 * (a - 2.0 * c + b);
    if (dot(acceleration, acceleration) < 0.0000001) {
        vec3 delta = b - a;
        return clamp(dot(p - a, delta) / max(dot(delta, delta), 0.0000001), 0.0, 1.0);
    }
    float best = dot(p - a, p - a), result = 0.0;
    float endDistance = dot(p - b, p - b);
    if (endDistance < best) { best = endDistance; result = 1.0; }
    // Two seeds retain both sides of tight reversals; clamp every closest-point iteration.
    for (int seed = 0; seed < 2; ++seed) {
        float t = seed == 0 ? 0.25 : 0.75;
        for (int j = 0; j < 4; ++j) {
            vec3 offset = curvePoint(a, c, b, t) - p;
            vec3 tangent = 2.0 * mix(c - a, b - c, t);
            float denominator = dot(tangent, tangent) + dot(offset, acceleration);
            t = clamp(t - dot(offset, tangent) / max(denominator, 0.00001), 0.0, 1.0);
        }
        vec3 offset = curvePoint(a, c, b, t) - p;
        float distance = dot(offset, offset);
        if (distance < best) { best = distance; result = t; }
    }
    return result;
}

float erodedDensity(float boundary, vec2 noise, float curl, float detail, float fine, float wake, float age) {
    if (boundary <= 0.0) return 0.0;
    float base = boundary - 0.38 * noise.r - 0.12 * noise.g;
    if (base <= 0.0) return 0.0;
    float edgeWeight = 1.0 - smoothstep(0.05, 0.65, base);
    base -= edgeWeight * ((0.55 + 0.14 * wake + 0.10 * age) * detail * detail
            + (0.14 + 0.09 * wake + 0.06 * age) * fine * fine);
    float shaped = smoothstep(0.0, 0.22, base);
    float breakup = 1.0 - smoothstep(0.45, 0.80, detail + 0.20 * fine + 0.15 * age);
    return shaped * smoothstep(0.0, 0.22, boundary)
            * mix(1.0, breakup, max(wake * wake, age * age)) * (0.90 + 0.10 * curl);
}

float densityAt(vec3 p) {
    vec3 q = p / CloudRadii;
    bool inBody = abs(q.x) < 1.15 && abs(q.y) < 1.15 && q.z > -2.4 && q.z < 1.25;
    float boundary = inBody ? cloudShape(stretchRear(q, MotionAmount)) : 0.0;
    boundary += 0.28 * (1.0 - smoothstep(0.02, 0.22, dot(q.xz, q.xz)))
            * smoothstep(0.10, 0.35, q.y) * (1.0 - smoothstep(0.62, 0.80, q.y));
    if (boundary <= 0.0 && TrailCount == 0) return 0.0;
    float density = 0.0;
    if (boundary > 0.0) {
        vec3 domain = p * 0.38;
        vec2 curl = worley(domain * 1.7 + DetailOffset);
        vec3 flowing = domain + 0.080 * vec3(curl.y - 0.5, curl.x - 0.5, 0.5 - curl.y);
        vec2 noise = worley(domain + NoiseOffset);
        float wake = smoothstep(0.85, 2.5, length(q));
        float detail = worley(flowing * 1.8 + DetailOffset).r;
        float fine = worley(flowing * 3.2 - DetailOffset * 2.0).r;
        density = erodedDensity(boundary, noise, curl.x, detail, fine, wake, 0.0) * DensityScale;
    }
    // History appearance is anchored in world axes; turning the emitter cannot rotate old noise.
    vec2 historyNoise = vec2(0.0);
    vec3 historyDetail = vec3(0.0);
    bool historyNoiseReady = false;
    vec3 metric = vec3(1.0, CloudRadii.x / CloudRadii.y, 1.0);
    // Inverse-advection: sample the SAME source cloud at coordinates carried down the measured path.
    // Birth geometry is independent of current speed. Age fades density, never the stretch length.
    for (int i = 0; i < 32; ++i) {
        if (i >= TrailCount) break;
        vec4 a = texelFetch(TrailData, ivec2(0, i), 0);
        vec4 b = texelFetch(TrailData, ivec2(1, i), 0);
        vec2 lives = min(vec2(TrailLifetime), texelFetch(TrailData, ivec2(2, i), 0).zw);
        if (a.w >= lives.x && b.w >= lives.y) continue;
        vec4 birth = texelFetch(TrailData, ivec2(3, i), 0);
        vec3 control = texelFetch(TrailData, ivec2(4, i), 0).xyz;
        vec3 support = CloudRadii.xyx * (max(birth.x, birth.y) / CloudRadii.x) * 1.12;
        if (any(lessThan(p, min(min(a.xyz, b.xyz), control) - support))
                || any(greaterThan(p, max(max(a.xyz, b.xyz), control) + support))) continue;
        float t = curveParameter(p * metric, a.xyz * metric, control * metric, b.xyz * metric);
        vec3 offset = p - curvePoint(a.xyz, control, b.xyz, t);
        vec3 radial = offset * metric / (mix(birth.x, birth.y, t) * 1.12);
        if (dot(radial, radial) >= 1.0) continue;
        float age = mix(a.w, b.w, t);
        float life = max(0.01, mix(lives.x, lives.y, t));
        float ageFraction = clamp(age / life, 0.0, 1.0);
        float fade = 1.0 - smoothstep(0.0, life, age);
        if (fade <= 0.0) continue;
        // The whole central diameter leaves volume behind, not a narrow emitter at the tail tip.
        // Birth width stays fixed; only density erosion/fade and outward dispersion evolve with age.
        vec3 material = vec3(length(offset.xz) / CloudRadii.x, offset.y / CloudRadii.y, 0.0);
        material.xy /= 1.08;
        material.xy /= (mix(birth.x, birth.y, t) / CloudRadii.x) * (1.0 + 0.12 * ageFraction);
        float historyBoundary = cloudShape(material);
        if (historyBoundary <= 0.0) continue;
        if (!historyNoiseReady) {
            vec3 historyDomain = transpose(WorldToCloud) * p * 0.38 + HistoryNoiseOrigin;
            vec2 historyCurl = worley(historyDomain * 1.7 + HistoryDetailOffset);
            vec3 historyFlow = historyDomain + 0.080 * vec3(historyCurl.y - 0.5, historyCurl.x - 0.5, 0.5 - historyCurl.y);
            historyNoise = worley(historyDomain + NoiseOffset);
            historyDetail = vec3(historyCurl.x, worley(historyFlow * 1.8 + HistoryDetailOffset).r,
                    worley(historyFlow * 3.2 - HistoryDetailOffset * 2.0).r);
            historyNoiseReady = true;
        }
        float historyDensity = erodedDensity(historyBoundary,
                historyNoise, historyDetail.x, historyDetail.y, historyDetail.z, 0.45, ageFraction);
        density = max(density, historyDensity * fade * mix(birth.z, birth.w, t));
    }
    return density;
}

float phaseHG(float cosine, float g) {
    return (1.0 - g * g) / (12.5663706 * pow(max(0.001, 1.0 + g * g - 2.0 * g * cosine), 1.5));
}

vec3 lightingAt(vec3 p, vec3 light, float phase, float density) {
    float height = clamp(p.y / CloudRadii.y * 0.5 + 0.5, 0.0, 1.0);
    // Local slab scattering avoids retracing the entire curved wake for every lighting sample.
    float opticalDepth = density * CloudRadii.y * (1.0 - height * 0.65) / max(light.y, 0.35);
    // Softer self-shadow and a multiple-scattering floor keep dense interiors pale rather than dirty.
    float sunVisibility = 0.16 + 0.84 * exp(-opticalDepth * Extinction * opticalScale(light) * 0.55);
    float powder = 1.0 - exp(-density * 2.0);
    float ambient = mix(0.70, 0.82, height) + 0.035 * SkyLight * DayLight;
    float direct = sunVisibility * (0.17 + 0.30 * phase) * (0.8 + 0.2 * powder) * SkyLight * DayLight;
    float edgeGlow = 0.055 * pow(clamp(1.0 - density, 0.0, 1.0), 1.5) * (0.4 + 0.6 * DayLight);
    return vec3(1.0, 0.985, 0.96) * ambient * (AmbientStrength / 0.66)
            + vec3(1.0, 0.99, 0.97) * (direct + edgeGlow);
}

void main() {
    vec2 uv = (gl_FragCoord.xy - Viewport.xy) / Viewport.zw;
    vec2 ndc = uv * 2.0 - 1.0;
    vec3 nearPoint = unproject(ndc, -1.0);
    vec3 worldDirection = normalize(unproject(ndc, 0.0) - nearPoint);
    vec3 origin = WorldToCloud * (nearPoint + CameraPos - CloudCenter);
    vec3 direction = WorldToCloud * worldDirection;
    float surface = 1e6;
    float depth = texture(SceneDepth, uv).r;
    if (depth < 1.0) {
        surface = dot(unproject(ndc, depth * 2.0 - 1.0) - nearPoint, worldDirection);
    }
    // Intersect, sort and merge occupied ray intervals so long/curved trails do not waste steps in gaps.
    vec2 intervals[33];
    int count = 0;
    float reach = 1.0 + 0.85 * MotionAmount;
    vec3 bodySupport = CloudRadii * vec3(1.15, 1.15, 1.25);
    vec2 hit = intersectBox(origin, direction, -bodySupport * vec3(1.0, 1.0, reach), bodySupport);
    hit = vec2(max(0.0, hit.x), min(surface, hit.y));
    if (hit.y > hit.x) intervals[count++] = hit;
    for (int i = 0; i < 32; ++i) {
        if (i >= TrailCount) break;
        vec4 a = texelFetch(TrailData, ivec2(0, i), 0);
        vec4 b = texelFetch(TrailData, ivec2(1, i), 0);
        vec2 lives = min(vec2(TrailLifetime), texelFetch(TrailData, ivec2(2, i), 0).zw);
        if (a.w >= lives.x && b.w >= lives.y) continue;
        vec3 control = texelFetch(TrailData, ivec2(4, i), 0).xyz;
        vec2 widths = texelFetch(TrailData, ivec2(3, i), 0).xy;
        vec3 radius = CloudRadii.xyx * (max(widths.x, widths.y) / CloudRadii.x) * 1.12;
        hit = intersectBox(origin, direction, min(min(a.xyz, b.xyz), control) - radius,
                max(max(a.xyz, b.xyz), control) + radius);
        hit = vec2(max(0.0, hit.x), min(surface, hit.y));
        if (hit.y <= hit.x) continue;
        int at = count;
        for (int j = 32; j > 0; --j) {
            if (j > at) continue;
            if (intervals[j - 1].x <= hit.x) break;
            intervals[j] = intervals[j - 1];
            at = j - 1;
        }
        intervals[at] = hit;
        count++;
    }
    if (count == 0) discard;
    int merged = 1;
    for (int i = 1; i < 33; ++i) {
        if (i >= count) break;
        if (intervals[i].x <= intervals[merged - 1].y) {
            intervals[merged - 1].y = max(intervals[merged - 1].y, intervals[i].y);
        } else intervals[merged++] = intervals[i];
    }
    float occupied = 0.0;
    for (int i = 0; i < 33; ++i) {
        if (i >= merged) break;
        occupied += intervals[i].y - intervals[i].x;
    }
    float begin = intervals[0].x;
    // Hardware LEQUAL tests the volume entry; scene depth also truncates the integral.
    vec4 entryClip = RelativeWorldToClip * vec4(nearPoint + worldDirection * begin, 1.0);
    gl_FragDepth = clamp(entryClip.z / entryClip.w * 0.5 + 0.5, 0.0, 1.0);
    if (DebugVolume != 0) {
        fragColor = vec4(0.92, 0.95, 1.0, (1.0 - exp(-occupied)) * Opacity);
        return;
    }
    float stepLength = max(2.0 / (length(direction / CloudRadii) * float(Steps)), occupied / 64.0);
    float transmittance = 1.0;
    vec3 radiance = vec3(0.0);
    vec3 light = normalize(WorldToCloud * LightDirection);
    float cosine = dot(direction, light);
    float phase = 0.85 * phaseHG(cosine, 0.45) + 0.15 * phaseHG(cosine, -0.2);
    float extinction = Extinction * opticalScale(direction);
    int span = 0;
    float cursor = begin;
    for (int i = 0; i < 104; ++i) {
        if (span >= merged || transmittance < 0.015) break;
        float ds = min(stepLength, intervals[span].y - cursor);
        float distance = cursor + 0.5 * ds;
        cursor += ds;
        if (cursor >= intervals[span].y - 0.00001) {
            span++;
            if (span < merged) cursor = intervals[span].x;
        }
        vec3 p = origin + direction * distance;
        float density = densityAt(p) * smoothstep(0.0, SoftIntersection, surface - distance);
        if (density < 0.001) continue;
        float stepAlpha = 1.0 - exp(-density * extinction * ds);
        radiance += transmittance * stepAlpha * lightingAt(p, light, phase, density);
        transmittance *= 1.0 - stepAlpha;
    }
    float alpha = (1.0 - transmittance) * Opacity;
    if (alpha < 0.002) discard;
    fragColor = vec4(radiance / max(1.0 - transmittance, 0.0001), alpha);
}
