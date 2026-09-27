#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;
uniform sampler2D Sampler4;
uniform sampler2D Sampler5;
uniform vec4 TintColor;
uniform float Time;
uniform float EffectMode;
uniform float WaveSpeed;
uniform float WaveScale;
uniform float OutlineWidth;
uniform float GlowStrength;
uniform float GlowRadius;
uniform float FillAmount;
uniform float BlurRadius;
uniform vec2 Resolution;

in vec2 TexCoord;
in vec4 FragColor;
out vec4 OutColor;

float noiseMask(vec2 uv) {
    return texture(Sampler4, uv).a;
}

vec3 worldBlur(vec2 uv, vec2 px) {
    float sigma = max(0.2, BlurRadius * 0.45);
    vec3 sum = vec3(0.0);
    float weightSum = 0.0;
    for (int y = -4; y <= 4; y++) {
        for (int x = -4; x <= 4; x++) {
            vec2 offset = vec2(float(x), float(y)) * px * BlurRadius * 0.5;
            float weight = exp(-dot(vec2(x,y),vec2(x,y)) / 10.0);
            sum += texture(Sampler3, clamp(uv + offset, vec2(0.0), vec2(1.0))).rgb * weight;
            weightSum += weight;
        }
    }
    return sum / weightSum;
}

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), f.x),
               mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += noise(p) * a;
        p = p * 2.02 + vec2(8.4, 5.7);
        a *= 0.5;
    }
    return v;
}

float ridged(vec2 p) {
    float v = 0.0;
    float a = 0.55;
    for (int i = 0; i < 4; i++) {
        float r = 1.0 - abs(noise(p) * 2.0 - 1.0);
        v += r * a;
        p = p * 2.18 + vec2(3.1, 9.2);
        a *= 0.52;
    }
    return v;
}

void main() {
    vec2 uv = TexCoord;
    float mask = noiseMask(uv);

    if (EffectMode > 0.5) {
        vec2 px = max(vec2(1.0) / max(Resolution, vec2(1.0)), vec2(0.0001));
        float blurred = texture(Sampler5, uv).a;
        float halo = pow(max(blurred, 0.0), 0.72) * (1.0 - mask) * GlowStrength * 0.85;
        if (mask < 0.01 && halo < 0.002) discard;
        float outside = 0.0;
        for (int i = 0; i < 24; i++) {
            float angle = float(i) * 6.2831853 / 24.0;
            vec2 direction = vec2(cos(angle), sin(angle));
            outside = max(outside, noiseMask(uv + direction * px * OutlineWidth * 2.0));
        }
        float edge = outside * (1.0 - mask);
        vec3 scene = texture(Sampler0, uv).rgb;
        vec3 result = scene;
        if (EffectMode < 1.5) {
            float wave = sin(uv.y * WaveScale * 48.0 - Time * WaveSpeed * 3.0
                           + fbm(uv * 5.0 + Time * 0.2) * 4.0) * 0.5 + 0.5;
            result = mix(scene, TintColor.rgb * (0.45 + wave * 0.95), FillAmount);
            result += TintColor.rgb * pow(wave, 3.0) * 0.22 * GlowStrength;
        } else if (EffectMode < 2.5) {
            result = scene;
        } else if (EffectMode < 3.5) {
            result = mix(scene, TintColor.rgb, FillAmount);
        } else {
            vec3 glass = mix(worldBlur(uv, px), TintColor.rgb, FillAmount * 0.28);
            result = glass;
        }
        result = mix(result, TintColor.rgb, clamp(halo, 0.0, 1.0));
        float border = clamp(edge * max(TintColor.a, 0.75), 0.0, 1.0);
        if (EffectMode > 3.5) {
            float alpha = mask > 0.01 ? 1.0 : clamp(max(border, halo * 0.5), 0.0, 1.0);
            OutColor = vec4(mix(result, TintColor.rgb, border * 0.35), alpha);
        } else {
            result = mix(result, TintColor.rgb, border * 0.6);
            OutColor = vec4(result, clamp(max(max(mask * TintColor.a, border), halo * 0.6), 0.0, 1.0));
        }
        return;
    }

    if (mask < 0.01) discard;

    float t = Time;
    vec2 flow = uv * 2.5;
    vec2 drift = vec2(t * 0.20, -t * 0.15);

    vec2 warp = vec2(
        fbm(flow * 0.90 + drift * 0.75 + vec2(0.0, 4.1)),
        fbm(flow * 0.78 - drift * 0.48 + vec2(3.7, 1.8))
    );
    vec2 q = flow + (warp - 0.5) * 1.8;

    float mist = fbm(q * 0.72 - drift * 0.24 + vec2(4.2, 8.1));
    float veins = pow(clamp(ridged(q * 1.85 + vec2(mist * 2.5, mist * 1.6) - drift * 0.55), 0.0, 1.0), 2.4);
    float sA = pow(clamp(1.0 - abs(sin((q.x * 1.08 + q.y * 0.42) * 1.7 + t * 0.85 + mist * 4.3)), 0.0, 1.0), 4.8);
    float sB = pow(clamp(1.0 - abs(sin((q.x * -0.58 + q.y * 1.12) * 1.45 - t * 0.65 - mist * 2.9)), 0.0, 1.0), 5.4);

    float energy = clamp(mist * 0.22 + veins * 0.88 + sA * 0.55 + sB * 0.32, 0.0, 1.0);
    float core = smoothstep(0.18, 0.98, energy);
    float accent = pow(clamp(max(veins, sA), 0.0, 1.0), 1.25);

    vec3 col = mix(TintColor.rgb, mix(TintColor.rgb, vec3(1.0), 0.4), clamp(core * 0.75 + sB * 0.25, 0.0, 1.0));
    float fill = mask * (0.26 + core * 0.82 + accent * 0.28);
    float outA = clamp(TintColor.a * fill * 0.92 * mask, 0.0, 1.0);

    if (outA <= 0.001) discard;

    OutColor = vec4(col * fill, outA);
}
