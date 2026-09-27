#version 150
uniform sampler2D Sampler0;
uniform vec2 TargetSize;
uniform vec2 Direction;
uniform float Radius;
out vec4 fragColor;
void main() {
    vec2 uv = gl_FragCoord.xy / TargetSize;
    float sigma = max(0.5, Radius / 3.0);
    int steps = int(clamp(ceil(Radius / 2.0), 2.0, 32.0));
    float sum = 0.0, total = 0.0;
    for (int i = -32; i <= 32; i++) {
        if (abs(i) > steps) continue;
        float offset = float(i) * Radius / float(steps);
        float weight = exp(-0.5 * offset * offset / (sigma * sigma));
        vec2 sampleUV = uv + Direction * offset;
        float alpha = 0.0;
        if (all(greaterThanEqual(sampleUV, vec2(0.0))) && all(lessThanEqual(sampleUV, vec2(1.0))))
            alpha = texture(Sampler0, sampleUV).a;
        sum += alpha * weight;
        total += weight;
    }
    fragColor = vec4(1.0, 1.0, 1.0, sum / total);
}
