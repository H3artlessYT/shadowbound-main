#version 150

uniform vec4 ColorModulator;
uniform float GameTime;
uniform float Quality;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), f.x),
               mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    int octaves = 2 + int(Quality + 0.5);
    for (int i = 0; i < 4; i++) {
        if (i >= octaves) break;
        v += a * noise(p);
        p = p * 2.07 + vec2(5.3, 1.9);
        a *= 0.5;
    }
    return v / (1.0 - 2.0 * a) * 0.9375;
}

void main() {
    float t = GameTime * 1200.0;
    float seed = vertexColor.r * 37.0;
    float phase = vertexColor.g;
    vec2 uv = texCoord0;

    float across = abs(uv.x - 0.5) * 2.0;
    float n = fbm(vec2(uv.x * 2.2 + seed, uv.y * 2.4 - t * 1.3 + seed * 0.7));
    float n2 = noise(vec2(uv.x * 7.0 - seed, uv.y * 6.0 - t * 2.0));

    float shape = 1.0 - smoothstep(0.15, 1.0, across + (n - 0.5) * 0.9);
    float ends = smoothstep(0.0, 0.3, uv.y) * (1.0 - smoothstep(0.6, 1.0, uv.y));
    float life = smoothstep(0.0, 0.12, phase) * (1.0 - smoothstep(0.55, 1.0, phase));
    float dens = shape * (0.55 + 0.6 * n) * ends;

    float body = smoothstep(0.32, 0.7, dens);

    float rim = smoothstep(0.2, 0.32, dens) * (1.0 - smoothstep(0.32, 0.46, dens));
    float heat = (1.0 - phase) * (0.6 + 0.6 * n2);

    vec3 smoke = vec3(0.055, 0.05, 0.055) * (0.7 + 0.6 * n);
    vec3 ember = vec3(0.95, 0.1, 0.1);
    vec3 col = mix(smoke, ember, clamp(rim * heat * 1.6, 0.0, 1.0));
    col += vec3(1.0, 0.6, 0.5) * pow(clamp(rim * heat, 0.0, 1.0), 4.0) * 0.6;

    float alpha = max(body * 0.6, rim * heat * 0.9) * life * vertexColor.a;
    if (alpha < 0.01) discard;
    fragColor = vec4(col, alpha) * ColorModulator;
}
