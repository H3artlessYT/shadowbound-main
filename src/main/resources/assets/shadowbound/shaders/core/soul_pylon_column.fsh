#version 150

uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p, float per) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(vec2(mod(i.x, per), i.y));
    float b = hash(vec2(mod(i.x + 1.0, per), i.y));
    float c = hash(vec2(mod(i.x, per), i.y + 1.0));
    float d = hash(vec2(mod(i.x + 1.0, per), i.y + 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

void main() {
    float secs = GameTime * 1200.0;
    float u = texCoord0.x;
    float v = texCoord0.y;

    float n1 = noise(vec2(u * 16.0, v * 3.0 - secs * 1.6), 16.0);
    float n2 = noise(vec2(u * 32.0 + 7.0, v * 6.0 - secs * 2.7), 32.0);
    float streak = smoothstep(0.55, 0.95, n1 * 0.65 + n2 * 0.45);

    float base = 1.0 - smoothstep(0.0, 0.45, v);
    float top = 1.0 - smoothstep(0.55, 1.0, v);
    float bottom = smoothstep(0.0, 0.06, v);

    float a = (streak * top * 0.8 + base * 0.15) * bottom;
    vec3 col = mix(vertexColor.rgb, vec3(1.0, 0.55, 0.45), streak * 0.5);
    fragColor = vec4(col, a * vertexColor.a) * ColorModulator;
}
