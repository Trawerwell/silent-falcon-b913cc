#version 150

in vec2 Uv;
uniform vec3 Forward;
uniform vec3 Right;
uniform vec3 Up;
uniform float Aspect;
uniform float TanHalfFov;
uniform float Time;
uniform float Style;
uniform float Brightness;
uniform float GlowStrength;
uniform vec3 NebulaColor1;
uniform vec3 NebulaColor2;
uniform vec3 NebulaColor3;
out vec4 fragColor;

float hash(vec2 p) {
    vec3 q = fract(vec3(p.x, p.y, p.x) * 0.1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float clouds(vec2 p) {
    float a = 0.0;
    float weight = 0.52;
    for (int i = 0; i < 4; ++i) {
        a += noise(p) * weight;
        p = p * 2.07 + vec2(4.2, 1.7);
        weight *= 0.5;
    }
    return a;
}

float stars(vec3 dir, float t) {
    vec2 sky = vec2(atan(dir.z, dir.x) / 6.2831853 + 0.5,
                    asin(clamp(dir.y, -1.0, 1.0)) / 3.1415927 + 0.5);
    vec2 cells = sky * vec2(360.0, 180.0);
    vec2 cell = floor(cells);
    float seed = hash(cell);
    if (seed < 0.982) return 0.0;
    vec2 center = vec2(hash(cell + 11.1), hash(cell + 71.7));
    float distanceToStar = length(fract(cells) - center);
    float core = 1.0 - smoothstep(0.015, 0.045, distanceToStar);
    float halo = 1.0 - smoothstep(0.02, 0.15, distanceToStar);
    float twinkle = 0.78 + 0.22 * sin(t * (0.6 + seed * 1.5) + seed * 34.0);
    return (core + halo * 0.27) * twinkle * (0.7 + seed * 0.5);
}

// A folded light curtain on the sky dome. Integer angular frequencies keep
// both ends of the azimuth seam identical as the camera turns around.
float auroraCurtain(float azimuth, float elevation, float time, float offset,
                    float baseHeight, float reach) {
    float broadWave = 0.105 * sin(2.0 * azimuth + offset + time * 0.032)
                    + 0.043 * sin(5.0 * azimuth - offset * 1.7 - time * 0.047)
                    + 0.022 * sin(9.0 * azimuth + offset * 2.3 + time * 0.021);
    float foot = baseHeight + broadWave;
    float height = elevation - foot;

    float upperEdge = reach * (0.77 + 0.15 * sin(3.0 * azimuth + offset * 1.5));
    float veil = smoothstep(-0.028, 0.032, height)
               * (1.0 - smoothstep(upperEdge * 0.54, upperEdge, height));

    float warp = 1.35 * sin(4.0 * azimuth + offset + time * 0.024)
               + 0.52 * sin(11.0 * azimuth - offset * 1.8 - time * 0.036);
    float pleats = 0.5 + 0.5 * sin(32.0 * azimuth + warp + time * 0.065);
    pleats = 0.24 + 0.76 * pow(pleats, 2.7);
    float widePatches = 0.72 + 0.19 * sin(3.0 * azimuth + offset + time * 0.018)
                              + 0.09 * sin(8.0 * azimuth - offset - time * 0.031);
    float lowerEdge = exp(-max(height, 0.0) * 13.0);
    return veil * pleats * widePatches * (0.48 + 0.52 * lowerEdge);
}

void main() {
    vec2 screen = Uv * 2.0 - 1.0;
    vec3 dir = normalize(Forward + Right * screen.x * Aspect * TanHalfFov
                        + Up * screen.y * TanHalfFov);
    float elevation = clamp(dir.y, -1.0, 1.0);
    float longitude = atan(dir.z, dir.x);
    float t = Time;
    vec3 color;

    if (Style < 0.5) {
        // Fine vertical filaments and two broad translucent curtain layers.
        color = mix(vec3(0.009, 0.019, 0.051), vec3(0.019, 0.052, 0.106),
                    smoothstep(-0.25, 0.75, elevation));
        vec2 skyPlane = dir.xz * 3.5 + elevation * vec2(0.42, -0.32);
        float warp = clouds(skyPlane * 0.9 + vec2(t * 0.012, 0.0));
        float phase = dir.x * 19.0 + dir.z * 11.0 + warp * 5.0 + t * 0.12;
        float filaments = pow(0.5 + 0.5 * sin(phase), 8.0);
        float sweep = 0.5 + 0.5 * sin(dir.x * 6.0 + dir.z * 4.0 + t * 0.07);
        float veil = (filaments * 0.72 + sweep * 0.32)
                     * smoothstep(-0.08, 0.16, elevation)
                     * (1.0 - smoothstep(0.65, 0.97, elevation));
        float edge = 1.0 - smoothstep(0.12, 0.78, elevation);
        color += vec3(0.04, 0.72, 0.47) * veil * edge;
        color += vec3(0.24, 0.43, 0.91) * veil * (1.0 - edge) * 0.85;
        color += vec3(0.45, 0.20, 0.60) * veil * warp * 0.34;
        color += vec3(stars(dir, t)) * 0.60;
    } else if (Style < 1.5) {
        // Soft clouds of color with sparse stars.
        vec2 p = dir.xz * 3.1 + elevation * vec2(0.65, 1.25);
        float n = clouds(p + vec2(t * 0.011, -t * 0.006));
        float n2 = clouds(p * 1.3 + vec2(5.2, 2.1) - vec2(t * 0.006, 0.0));
        float nebula = smoothstep(0.43, 0.77, n);
        color = mix(NebulaColor1 * 0.12, NebulaColor1 * 0.62, nebula);
        color += NebulaColor2 * 0.7 * smoothstep(0.52, 0.78, n2);
        color += NebulaColor3 * 0.62 * nebula * n2;
        color += vec3(stars(dir, t)) * 0.8;
    } else if (Style < 2.5) {
        // Warm horizon with a small sun and gently drifting cloud bands.
        float horizon = smoothstep(-0.40, 0.72, elevation);
        color = mix(vec3(0.66, 0.22, 0.23), vec3(0.13, 0.10, 0.30), horizon);
        color += vec3(0.46, 0.18, 0.10)
                 * (1.0 - smoothstep(-0.12, 0.40, abs(elevation - 0.02)));
        float sun = max(dot(dir, normalize(vec3(-0.24, 0.10, 0.96))), 0.0);
        color += vec3(1.0, 0.54, 0.23) * pow(sun, 80.0) * 0.65;
        color += vec3(1.0, 0.84, 0.55) * smoothstep(0.9994, 0.9998, sun);
        float cloud = clouds(dir.xz * 5.0 + elevation * vec2(1.2, 9.0)
                             - vec2(t * 0.013, 0.0));
        float bands = smoothstep(0.57, 0.71, cloud)
                      * (1.0 - smoothstep(0.35, 0.78, elevation));
        color = mix(color, color * vec3(0.60, 0.47, 0.62), bands * 0.47);
    } else if (Style < 3.5) {
        // Clear night with a faint moving milky way.
        color = mix(vec3(0.010, 0.014, 0.040), vec3(0.032, 0.045, 0.095),
                    smoothstep(-0.4, 0.75, elevation));
        float latitude = abs(elevation + 0.30 * sin(longitude * 2.0 + 0.4));
        float dust = clouds(dir.xz * 3.8 + elevation * vec2(1.5, 4.0)
                            + vec2(t * 0.002, 0.0));
        float galaxy = (1.0 - smoothstep(0.04, 0.39, latitude))
                       * smoothstep(0.28, 0.68, dust);
        color += vec3(0.16, 0.15, 0.29) * galaxy;
        color += vec3(stars(dir, t)) * 1.2;
    } else {
        // Layered polar-light curtains: bright feet, soft upward rays, no crossing lines.
        color = mix(vec3(0.006, 0.013, 0.035), vec3(0.010, 0.030, 0.063),
                    smoothstep(-0.2, 0.9, elevation));
        float nearVeil = auroraCurtain(longitude, elevation, t, 0.2, 0.01, 0.79);
        float farVeil = auroraCurtain(longitude, elevation, t * 0.78, 2.3, 0.26, 0.62);
        float violetVeil = auroraCurtain(longitude, elevation, t * 0.64, 4.8, -0.12, 0.72);
        vec3 glow = nearVeil * vec3(0.045, 0.77, 0.49);
        glow += farVeil * vec3(0.12, 0.36, 0.70) * 0.62;
        glow += violetVeil * vec3(0.35, 0.16, 0.55) * 0.39;
        // Diffuse light behind the folds gives the curtains a soft luminous body.
        glow += vec3(0.015, 0.11, 0.10) * (nearVeil + farVeil) * 0.65;
        color += glow * GlowStrength;
        color += vec3(stars(dir, t)) * 0.48;
    }

    color *= Brightness;
    color = color / (1.0 + color * 0.35);
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
