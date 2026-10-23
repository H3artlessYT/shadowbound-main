#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float GameTime;
uniform float FogStart;
uniform float FogEnd;
uniform float Quality;

in vec3 worldPos;
in vec3 viewPos;
in vec3 viewNormal;
in vec2 texCoord0;
in vec4 smokeData;
in vec4 lightColor;
in vec4 overlayColor;
in float vertexDistance;
in vec3 smokeP;

out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i + vec3(0, 0, 0)), hash(i + vec3(1, 0, 0)), f.x),
                   mix(hash(i + vec3(0, 1, 0)), hash(i + vec3(1, 1, 0)), f.x), f.y),
               mix(mix(hash(i + vec3(0, 0, 1)), hash(i + vec3(1, 0, 1)), f.x),
                   mix(hash(i + vec3(0, 1, 1)), hash(i + vec3(1, 1, 1)), f.x), f.y), f.z);
}

float fbm(vec3 p, int octaves) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        if (i >= octaves) break;
        v += a * noise(p);
        p = p * 2.03 + vec3(3.1, 1.7, 5.3);
        a *= 0.5;
    }
    return v / (1.0 - a * 2.0 + 1e-4) * (1.0 - 0.0625);
}

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    if (tex.a < 0.1) discard;

    float phase = smokeData.r;
    float seed = smokeData.g * 41.0;
    float clone = smokeData.b;
    float fade = smokeData.a;
    float t = GameTime * 1200.0;

    int octaves = Quality < 0.5 ? 2 : (Quality < 1.5 ? 3 : 4);
    float n = fbm(smokeP, octaves);

    vec3 N = normalize(viewNormal);
    vec3 V = normalize(-viewPos);
    float rim = pow(1.0 - abs(dot(N, V)), 2.2);

    vec3 col = vec3(0.035, 0.03, 0.035);
    float alpha;

    if (phase < 0.005) {
        float clumps = smoothstep(0.30, 0.55, n);
        alpha = mix(0.32, 0.62, clumps);
        alpha = max(alpha, rim * 0.72);

        if (clone > 0.5) {
            float l = dot(tex.rgb, vec3(0.299, 0.587, 0.114));
            vec3 skin = mix(tex.rgb, vec3(l), 0.3) * 0.34;
            col = mix(col, skin, 0.65 + 0.3 * clumps);
            alpha = mix(0.6, 0.86, clumps);
            alpha = max(alpha, rim * 0.8);
        }

        col = mix(overlayColor.rgb, col, overlayColor.a);
    } else {
        float cut = 0.40 + 0.3 * phase;
        float d = smoothstep(cut, cut + 0.16, n);
        float rim = smoothstep(cut - 0.06, cut, n) * (1.0 - smoothstep(cut, cut + 0.05, n));
        float life = smoothstep(0.0, 0.15, phase) * (1.0 - phase);
        float heat = clamp(rim * 1.6 * (1.0 - phase * 0.5), 0.0, 1.0);

        col = mix(col, vec3(0.92, 0.09, 0.1), heat);
        col += vec3(1.0, 0.55, 0.45) * pow(heat, 4.0) * 0.5;
        alpha = max(d * 0.5, heat * 0.85) * life;
    }

    if (phase < 0.005) {
        float vein = 1.0 - smoothstep(0.0, 0.035, abs(n - 0.62));
        col = mix(col, vec3(0.8, 0.07, 0.09), vein * 0.7);
        alpha = max(alpha, vein * 0.55);
    }

    alpha *= fade;

    float fog = vertexDistance <= FogStart ? 0.0
              : (vertexDistance < FogEnd ? smoothstep(FogStart, FogEnd, vertexDistance) : 1.0);
    alpha *= 1.0 - fog;

    if (alpha < 0.01) discard;
    fragColor = vec4(col, alpha) * ColorModulator;
}
