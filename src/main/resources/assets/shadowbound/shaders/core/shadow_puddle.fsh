#version 150

uniform vec4 ColorModulator;
uniform float GameTime;
uniform float Quality;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

const float TAU = 6.2831853;

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
        p = p * 2.05 + vec2(3.7, 8.1);
        a *= 0.5;
    }
    return v / (1.0 - 2.0 * a) * 0.9375;
}

float ringNoise(float ang, float freq, float t, float seed) {
    vec2 p = vec2(cos(ang), sin(ang)) * freq;
    return fbm(p + vec2(t, seed));
}

void main() {
    float t = GameTime * 1200.0;
    float seed = vertexColor.r * 50.0;
    float speed = vertexColor.g;

    vec2 c = texCoord0 * 2.0 - 1.0;
    float r = length(c);
    float ang = atan(c.y, c.x);

    float edge = 0.55 + 0.16 * ringNoise(ang, 2.0, t * 0.5, seed);
    float reach = pow(ringNoise(ang, 4.0, t * 0.35, seed + 9.0), 3.0);
    edge += 0.45 * reach * (0.6 + 0.4 * sin(t * 1.3 + ang * 3.0));
    float body = 1.0 - smoothstep(edge - 0.1, edge, r);
    if (body < 0.004) discard;

    float sheen = fbm(c * 3.0 + vec2(t * 0.25, -t * 0.18) + seed);
    vec3 col = vec3(0.012, 0.008, 0.01) + vec3(0.07, 0.06, 0.07) * smoothstep(0.55, 0.85, sheen);

    float ripple = sin(r * 22.0 - t * (3.0 + 6.0 * speed));
    col += vec3(0.05, 0.03, 0.035) * smoothstep(0.7, 1.0, ripple) * (1.0 - r);

    float rim = smoothstep(edge - 0.18, edge - 0.04, r) * (1.0 - smoothstep(edge - 0.04, edge, r));
    col += vec3(0.6, 0.04, 0.06) * rim * (0.6 + 0.4 * sin(t * 2.0 + ang * 2.0));

    float blinkCycle = fract(t * 0.23 + seed * 0.1);
    float open = 1.0 - smoothstep(0.0, 0.02, blinkCycle) * (1.0 - smoothstep(0.04, 0.06, blinkCycle));
    vec2 look = vec2(sin(t * 0.4 + seed), 0.0) * 0.03;
    for (int i = 0; i < 2; i++) {
        vec2 e = vec2(i == 0 ? -0.17 : 0.17, -0.08) + look;
        vec2 d = (c - e) * vec2(1.0, 1.0 / max(open, 0.08));
        float eye = 1.0 - smoothstep(0.035, 0.06, length(d * vec2(1.0, 1.6)));
        float glow = 1.0 - smoothstep(0.0, 0.2, length(c - e));
        col = mix(col, vec3(1.0, 0.15, 0.12), eye * open);
        col += vec3(0.5, 0.02, 0.03) * glow * 0.6 * open;
    }

    float alpha = body * 0.93 * vertexColor.a;
    fragColor = vec4(col, alpha) * ColorModulator;
}
