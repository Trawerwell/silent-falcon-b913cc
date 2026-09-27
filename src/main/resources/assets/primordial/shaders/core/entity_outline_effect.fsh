#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 ScreenSize;
uniform float OutlineSize;
uniform float GlowRadius;
uniform float GlowStrength;
uniform vec4 OutlineColor;
out vec4 fragColor;
const float TAU = 6.28318530718;
float coverage(vec2 uv) {
    if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) return 0.0;
    return texture(Sampler0, uv).a;
}
void main() {
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    float center = texture(Sampler0, uv).a;
    if (center >= 0.999) discard;
    float line = 0.0;
    // Bilinear coverage and two rings soften fractional-pixel outlines.
    // Cost is fixed and independent of glow radius.
    for (int i = 0; i < 24; i++) {
        float angle = TAU * float(i) / 24.0;
        vec2 direction = vec2(cos(angle), sin(angle)) / ScreenSize;
        line = max(line, coverage(uv + direction * OutlineSize));
        line = max(line, coverage(uv + direction * OutlineSize * 0.5));
    }
    float blurredGlow = texture(Sampler1, uv).a;
    // Previous build's soft halo response.
    float glow = GlowRadius > 0.0 ? pow(clamp(blurredGlow, 0.0, 1.0), 0.45) * GlowStrength : 0.0;
    float alpha = max(line, glow) * (1.0 - center) * OutlineColor.a;
    if (alpha <= 0.001) discard;
    fragColor = vec4(OutlineColor.rgb, min(alpha, 1.0));
}
