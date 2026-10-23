#version 150

uniform sampler2D DiffuseSampler;

uniform vec2 InSize;
uniform float Fade;
uniform float Clock;
uniform float Danger;
uniform float Speed;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(443.897, 441.423));
    p += dot(p, p.yx + 19.19);
    return fract((p.x + p.y) * p.x);
}

float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm2(vec2 p) {
    return (0.5 * noise(p) + 0.25 * noise(p * 2.03 + 7.1)) / 0.75 * 0.9375;
}

float fbm3(vec2 p) {
    float v = 0.5 * noise(p);
    p = p * 2.03 + 7.1;
    v += 0.25 * noise(p);
    p = p * 2.03 + 7.1;
    v += 0.125 * noise(p);
    return v / 0.875 * 0.9375;
}

vec3 lift(vec3 c) {
    return pow(c, vec3(0.55)) * 1.05;
}

float luma(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

float edgeLuma(vec2 at) {
    return pow(luma(texture(DiffuseSampler, at).rgb), 0.55);
}

void main() {
    vec2 uv = texCoord;
    vec2 c = uv - 0.5;
    float aspect = InSize.x / InSize.y;
    float dist = length(c * vec2(aspect, 1.0));

    uv += vec2(sin(Clock * 47.0), cos(Clock * 53.0)) * 0.0025 * Danger * Danger;

    vec2 w = vec2(fbm2(uv * 3.5 + vec2(Clock * 0.2, 0.0)), fbm2(uv * 3.5 - vec2(0.0, Clock * 0.17))) - 0.5;
    uv += w * 0.012;

    vec3 col;
    if (Speed > 0.02) {
        col = vec3(0.0);
        float total = 0.0;
        for (int i = 0; i < 6; i++) {
            float k = float(i) / 5.0;
            vec2 suv = mix(uv, vec2(0.5), k * 0.06 * Speed);
            float wgt = 1.0 - k * 0.6;
            col += texture(DiffuseSampler, suv).rgb * wgt;
            total += wgt;
        }
        col /= total;
    } else {
        col = texture(DiffuseSampler, uv).rgb;
    }
    vec3 orig = texture(DiffuseSampler, texCoord).rgb;

    vec3 lit = lift(col);
    float l = luma(lit);
    float redness = clamp((col.r - max(col.g, col.b)) * 3.0, 0.0, 1.0);
    vec3 graded = mix(vec3(l) * vec3(0.92, 0.88, 0.9), vec3(l * 1.5 + 0.1, l * 0.2, l * 0.22), redness);

    float lx = edgeLuma(uv + vec2(oneTexel.x, 0.0)) - edgeLuma(uv - vec2(oneTexel.x, 0.0));
    float ly = edgeLuma(uv + vec2(0.0, oneTexel.y)) - edgeLuma(uv - vec2(0.0, oneTexel.y));
    float edge = clamp(length(vec2(lx, ly)) * 3.8, 0.0, 1.0);
    graded = mix(graded, vec3(0.75, 0.06, 0.08), edge * 0.55);

    float glare = smoothstep(0.75, 1.0, luma(col));
    graded += vec3(1.0, 0.85, 0.8) * glare * (0.2 + 0.2 * sin(Clock * 30.0)) * (0.4 + Danger);

    float ang = atan(c.y, c.x);
    float tendril = fbm3(vec2(ang * 3.0 + 2.0, dist * 3.5 - Clock * 0.35));
    float inner = 0.42 - tendril * 0.2 - 0.08 * (1.0 - Danger);
    float vig = smoothstep(inner, 0.85, dist);

    float col40 = floor(texCoord.x * 48.0);
    float dripLen = 0.04 + 0.16 * pow(hash(vec2(col40, 3.0)), 3.0) * (0.7 + 0.3 * sin(Clock * 0.6 + col40));
    float drip = 1.0 - smoothstep(dripLen - 0.02, dripLen, 1.0 - texCoord.y);
    drip *= 1.0 - smoothstep(0.3, 0.5, abs(fract(texCoord.x * 48.0) - 0.5) * 2.0);
    float ink = clamp(vig + drip * 0.9, 0.0, 1.0);

    vec3 inkCol = mix(vec3(0.01, 0.005, 0.006), vec3(0.35, 0.05, 0.05), Danger);
    float burnEdge = smoothstep(inner - 0.02, inner + 0.08, dist) * (1.0 - smoothstep(inner + 0.08, inner + 0.25, dist));
    graded = mix(graded, inkCol, ink * 0.92);
    graded += vec3(0.7, 0.05, 0.05) * burnEdge * (0.35 + 0.8 * Danger);
    graded += vec3(1.0, 0.75, 0.7) * burnEdge * Danger * Danger * 0.2;

    graded += (hash(texCoord * InSize + fract(Clock) * 91.0) - 0.5) * 0.05;

    vec3 outCol = graded;
    if (Fade < 0.999) {
        float rise = Fade * 1.25 - 0.1 + (fbm2(vec2(texCoord.x * 6.0, Clock)) - 0.5) * 0.15;
        float under = 1.0 - smoothstep(rise - 0.05, rise, texCoord.y);
        outCol = mix(orig, graded, under);
        outCol += vec3(0.8, 0.05, 0.06) * (1.0 - smoothstep(0.0, 0.03, abs(texCoord.y - rise))) * (1.0 - Fade) * 2.0;
    }

    fragColor = vec4(outCol, 1.0);
}
