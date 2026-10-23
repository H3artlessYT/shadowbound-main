#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;
uniform float Progress;
uniform float Intensity;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

const float TAU = 6.2831853;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), f.x),
               mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), f.x), f.y);
}

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    if (tex.a < 0.003) discard;

    float t = GameTime * 1200.0;
    vec2 c = texCoord0 - 0.5;
    float r = length(c) * 2.0;
    float ang = atan(c.y, c.x) / TAU + 0.5;

    float edge = Progress * 1.08;
    float reveal = 1.0 - smoothstep(edge - 0.035, edge, ang);
    float front = (1.0 - smoothstep(0.0, 0.05, abs(ang - edge))) * (1.0 - step(1.0, Progress));

    float redness = clamp((tex.r - tex.g) * 1.6, 0.0, 1.0);
    float pulse = 0.55 + 0.45 * sin(t * 3.2 - r * 12.0);
    float flicker = 0.88 + 0.24 * noise(vec2(ang * 50.0, t * 4.0));
    float glow = mix(0.75, Intensity * (0.7 + 0.6 * pulse) * flicker, redness);

    vec3 col = tex.rgb * vertexColor.rgb * glow;
    col += vec3(1.0, 0.35, 0.3) * front * tex.a * 2.0;

    float a = tex.a * clamp(reveal + front, 0.0, 1.0) * vertexColor.a;
    fragColor = vec4(col, a) * ColorModulator;
}
