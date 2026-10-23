#version 150

uniform sampler2D DiffuseSampler;

uniform vec2 InSize;
uniform float Fade;
uniform float Clock;

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
    return mix(mix(hash(i), hash(i + vec2(1, 0)), f.x),
               mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), f.x), f.y);
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

void main() {
    vec2 uv = texCoord;
    vec2 c = uv - 0.5;
    float aspect = InSize.x / InSize.y;
    float dist = length(c * vec2(aspect, 1.0));

    float warpAmt = smoothstep(0.15, 0.8, dist);
    if (warpAmt > 0.0) {
        vec2 w = vec2(fbm2(uv * 3.0 + vec2(Clock * 0.15, 0.0)), fbm2(uv * 3.0 - vec2(0.0, Clock * 0.12))) - 0.5;
        uv += w * 0.018 * Fade * warpAmt;
    }

    vec2 split = c * 0.006 * Fade * dist;
    vec3 orig = texture(DiffuseSampler, texCoord).rgb;
    vec3 col = vec3(texture(DiffuseSampler, uv + split).r,
                    texture(DiffuseSampler, uv).g,
                    texture(DiffuseSampler, uv - split).b);

    float luma = dot(col, vec3(0.299, 0.587, 0.114));
    vec3 mono = vec3(pow(luma, 1.15) * 1.12);
    float redness = clamp((col.r - max(col.g, col.b)) * 2.5, 0.0, 1.0);
    vec3 graded = mix(mono, vec3(mono.r * 1.6 + 0.08, mono.g * 0.25, mono.b * 0.28), redness);

    float ang = atan(c.y, c.x);
    float tendril = fbm3(vec2(ang * 2.5 + 3.0, dist * 3.0 - Clock * 0.5));
    float beat = pow(max(0.0, sin(Clock * 4.6)), 16.0) + 0.5 * pow(max(0.0, sin(Clock * 4.6 - 0.6)), 16.0);
    float inner = 0.30 - tendril * 0.22 - beat * 0.05;
    float vig = smoothstep(inner, 0.78, dist);
    graded = mix(graded, vec3(0.02, 0.02, 0.02), vig * 0.94);
    float edge = smoothstep(inner - 0.04, inner + 0.10, dist) * (1.0 - smoothstep(inner + 0.10, inner + 0.30, dist));
    graded += vec3(0.55, 0.03, 0.06) * edge * (0.35 + 0.65 * beat);
    graded += vec3(0.35, 0.0, 0.03) * beat * vig * 0.6;

    graded *= 0.95 + 0.05 * sin(texCoord.y * InSize.y * 1.4 + Clock * 6.0);
    graded += (hash(texCoord * InSize + fract(Clock) * 91.0) - 0.5) * 0.06;

    fragColor = vec4(mix(orig, graded, clamp(Fade, 0.0, 1.0)), 1.0);
}
