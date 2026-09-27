#version 150

in vec2 FragCoord;
in vec4 FragColor;
uniform float Style;
out vec4 fragColor;

float segment(vec2 p, vec2 a, vec2 b) {
    vec2 v = b - a;
    return length(p - a - v * clamp(dot(p - a, v) / dot(v, v), 0.0, 1.0));
}

float cross2(vec2 a, vec2 b) { return a.x * b.y - a.y * b.x; }

float triangle(vec2 p, vec2 a, vec2 b, vec2 c) {
    float d = min(segment(p, a, b), min(segment(p, b, c), segment(p, c, a)));
    float s = sign(cross2(b - a, c - a));
    bool inside = s * cross2(b - a, p - a) >= 0.0
               && s * cross2(c - b, p - b) >= 0.0
               && s * cross2(a - c, p - c) >= 0.0;
    return inside ? -d : d;
}

float chevron(vec2 p) {
    return min(segment(p, vec2(-0.46, 0.24), vec2(0.0, -0.36)),
               segment(p, vec2(0.0, -0.36), vec2(0.46, 0.24)));
}

void main() {
    vec2 p = FragCoord * 2.0 - 1.0;
    float d;
    float opacity = 1.0;
    if (Style < 0.5 || Style > 2.5) {
        vec2 tip = vec2(0.0, -0.76);
        vec2 left = vec2(-0.38, 0.50);
        vec2 notch = vec2(0.0, 0.23);
        vec2 right = vec2(0.38, 0.50);
        if (Style > 2.5) {
            d = min(min(segment(p, tip, left), segment(p, left, notch)),
                    min(segment(p, notch, right), segment(p, right, tip))) - 0.052;
        } else {
            float boundary = min(min(segment(p, tip, left), segment(p, left, notch)),
                                 min(segment(p, notch, right), segment(p, right, tip)));
            bool inside = min(triangle(p, tip, left, notch), triangle(p, tip, notch, right)) <= 0.0;
            d = inside ? -boundary : boundary;
        }
    } else if (Style < 1.5) {
        d = chevron(p) - 0.065;
    } else {
        float front = chevron(p + vec2(0.0, 0.30)) - 0.052;
        float back = chevron(p - vec2(0.0, 0.30)) - 0.052;
        d = min(front, back);
        opacity = front <= back ? 1.0 : 0.52;
    }
    float aa = max(fwidth(d), 0.001);
    float alpha = 1.0 - smoothstep(-aa * 0.5, aa * 0.5, d);
    fragColor = vec4(FragColor.rgb, FragColor.a * alpha * opacity);
}
