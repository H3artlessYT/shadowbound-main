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

const vec3 BLOOD = vec3(0.72, 0.03, 0.05);
const vec3 HOT = vec3(1.0, 0.42, 0.3);

void main() {
    vec2 uv = texCoord0;
    vec3 albedo = texture(Sampler0, uv).rgb;
    float mask = texture(Sampler0, uv + vec2(0.5, 0.0)).r;

    float secs = GameTime * 1200.0;
    float glow = pylonData.r;
    float h = pylonData.g;
    float seed = pylonData.b;

    float flow = 0.5 + 0.5 * sin(h * 19.0 - secs * 4.5 + seed * 6.2831);
    flow = flow * flow * flow;
    float flicker = 0.9 + 0.1 * sin(secs * 23.0 + seed * 40.0);
    float e = mask * (0.15 + glow) * (0.35 + 0.65 * flow) * flicker;

    vec3 emit = mix(BLOOD, HOT, clamp(e * e * 1.6, 0.0, 1.0)) * e * 2.6;

    vec3 n = normalize(viewNormal);
    vec3 v = normalize(-viewPos);
    float rim = 1.0 - clamp(dot(n, v), 0.0, 1.0);
    rim = rim * rim * rim;

    vec3 lit = albedo * shade * lightColor.rgb;
    lit += albedo * BLOOD * glow * 0.6;

    vec3 col = lit + emit + BLOOD * rim * (0.2 + 0.8 * glow);
    fragColor = linear_fog(vec4(col, 1.0) * ColorModulator, vertexDistance, FogStart, FogEnd, FogColor);
}
