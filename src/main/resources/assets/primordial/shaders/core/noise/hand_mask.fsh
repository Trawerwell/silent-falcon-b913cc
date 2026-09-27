#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;
uniform vec2 Resolution;
in vec2 TexCoord;
out vec4 OutColor;

float rawMask(vec2 uv) {
    vec3 before = texture(Sampler0, uv).rgb;
    vec3 after = texture(Sampler1, uv).rgb;
    vec3 difference = abs(before - after);
    float colorMask = smoothstep(0.012, 0.055, max(max(difference.r, difference.g), difference.b));
    float beforeDepth = texture(Sampler2, uv).r;
    float afterDepth = texture(Sampler3, uv).r;
    float depthMask = smoothstep(0.00004, 0.00024, beforeDepth - afterDepth);
    return max(colorMask, depthMask);
}

void main() {
    float mask = rawMask(TexCoord);
    // Fill tiny holes where a held model's pixels match the world color/depth.
    // Requiring several surrounding mask samples avoids spreading the mask outside its silhouette.
    vec2 texel = 1.0 / max(Resolution, vec2(1.0));
    float neighbours = 0.0;
    neighbours += rawMask(TexCoord + vec2(texel.x, 0.0));
    neighbours += rawMask(TexCoord + vec2(-texel.x, 0.0));
    neighbours += rawMask(TexCoord + vec2(0.0, texel.y));
    neighbours += rawMask(TexCoord + vec2(0.0, -texel.y));
    neighbours += rawMask(TexCoord + vec2(texel.x, texel.y));
    neighbours += rawMask(TexCoord + vec2(-texel.x, texel.y));
    neighbours += rawMask(TexCoord + vec2(texel.x, -texel.y));
    neighbours += rawMask(TexCoord + vec2(-texel.x, -texel.y));
    if (mask < 0.12 && neighbours >= 5.0) mask = 0.82;
    OutColor = vec4(1.0, 1.0, 1.0, mask);
}
