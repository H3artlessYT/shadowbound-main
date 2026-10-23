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
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
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
    float seed = vertexColor.r * 40.0;
    float age = vertexColor.g;
    vec2 uv = texCoord0;

    float across = abs(uv.x - 0.5) * 2.0;
    vec2 p = vec2(uv.x * 1.6, uv.y * 1.4) + seed;
    float n = fbm(p);
    float n2 = fbm(vec2(uv.x * 4.0, uv.y * 3.5) + vec2(t * 0.05, -t * 0.07) + seed);

    float edge = 0.95 - 0.35 * age;
    float body = 1.0 - smoothstep(edge - 0.3, edge, across + (n - 0.5) * 0.7);

    float breakup = smoothstep(age * 0.85 - 0.05, age * 0.85 + 0.2, n * 0.7 + n2 * 0.45);
    float dens = body * breakup;
    if (dens < 0.01) discard;

    vec3 col = vec3(0.012, 0.009, 0.011) + vec3(0.06, 0.05, 0.055) * smoothstep(0.6, 0.9, n2);

    float rimBand = smoothstep(0.05, 0.25, dens) * (1.0 - smoothstep(0.25, 0.55, dens));
    col += vec3(0.65, 0.04, 0.06) * rimBand * pow(1.0 - age, 2.0);

    float alpha = smoothstep(0.0, 0.35, dens) * 0.9 * pow(1.0 - age, 0.7) * vertexColor.a;
    fragColor = vec4(col, alpha) * ColorModulator;
}
