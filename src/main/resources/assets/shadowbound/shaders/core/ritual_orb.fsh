#version 150

uniform vec4 ColorModulator;
uniform float GameTime;

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
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = p * 2.03 + vec2(4.7, 1.3);
        a *= 0.5;
    }
    return v;
}

void main() {
    float t = GameTime * 1200.0;
    float glow = vertexColor.r;
    vec2 c = texCoord0 * 2.0 - 1.0;
    float r = length(c);
    if (r > 1.0) discard;

    float z = sqrt(max(1.0 - r * r, 0.0));
    float rim = pow(1.0 - z, 2.2);

    float ang = atan(c.y, c.x) + t * 0.7 + (1.0 - r) * 3.5;
    vec2 q = vec2(cos(ang), sin(ang)) * r / (0.35 + z);
    float n = fbm(q * 2.6 + vec2(t * 0.15, -t * 0.1));
    float cracks = 1.0 - smoothstep(0.0, 0.05, abs(n - 0.5));
    float cracks2 = 1.0 - smoothstep(0.0, 0.03, abs(fbm(q * 5.0 - t * 0.2) - 0.5));

    vec3 col = vec3(0.025, 0.004, 0.008) * (0.6 + n);
    col += vec3(0.95, 0.06, 0.09) * (cracks * 0.9 + cracks2 * 0.4) * (0.35 + 0.65 * glow) * (0.3 + 0.7 * z);
    col += vec3(1.0, 0.1, 0.1) * rim * 1.3 * glow;
    col += vec3(1.0, 0.8, 0.75) * pow(rim, 7.0) * glow;

    float alpha = 1.0 - smoothstep(0.93, 1.0, r);
    fragColor = vec4(col, alpha) * ColorModulator;
}
