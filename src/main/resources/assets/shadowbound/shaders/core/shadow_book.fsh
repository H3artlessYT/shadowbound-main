#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float GameTime;

in vec2 texCoord0;
in vec4 pylonData;
in vec4 lightColor;
in vec3 viewPos;
in vec3 viewNormal;
in float shade;
in float vertexDistance;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    if (tex.a < 0.1) discard;

    float secs = GameTime * 1200.0;
    float glow = pylonData.r;
    float form = pylonData.g;
    float seed = pylonData.b * 17.0;

    vec2 cell = floor(texCoord0 * vec2(textureSize(Sampler0, 0)));
    float h = hash(cell + seed);
    if (h > form) discard;
    float front = (1.0 - smoothstep(0.0, 0.14, form - h)) * step(form, 0.999);

    vec3 n = normalize(viewNormal);
    vec3 v = normalize(-viewPos);
    float rim = 1.0 - clamp(abs(dot(n, v)), 0.0, 1.0);
    rim = rim * rim * rim;

    float red = clamp((tex.r - max(tex.g, tex.b)) * 2.5, 0.0, 1.0);
    float wave = 0.5 + 0.5 * sin(cell.y * 0.8 + cell.x * 0.35 - secs * 3.2);

    vec3 lit = tex.rgb * max(shade, 0.35) * max(lightColor.rgb, vec3(0.22));
    vec3 emit = vec3(1.0, 0.1, 0.12) * red * (0.45 + 0.55 * wave) * (0.5 + glow);
    vec3 col = lit + emit + vec3(0.75, 0.04, 0.06) * rim * (0.35 + glow);
    col += vec3(1.0, 0.45, 0.32) * front * 1.6;
    col = mix(col, vec3(1.0, 0.45, 0.38), smoothstep(0.75, 1.0, glow) * 0.35);

    fragColor = linear_fog(vec4(col, 1.0) * ColorModulator, vertexDistance, FogStart, FogEnd, FogColor);
}
