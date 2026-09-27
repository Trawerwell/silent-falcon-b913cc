#version 150

in vec3 Position;
in vec4 Color;
out vec2 Uv;

void main() {
    Uv = Position.xy * 0.5 + 0.5;
    gl_Position = vec4(Position.xy, 0.9999, 1.0);
}
