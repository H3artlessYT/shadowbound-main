#version 150

uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec2 p = texCoord0 * 2.0 - 1.0;
    float d = length(p);
    if (d > 1.0) discard;
    float core = 1.0 - smoothstep(0.22, 0.38, d);
    float halo = 1.0 - d;
    halo *= halo;
    float a = max(core, halo * 0.5) * vertexColor.a;
    if (a < 0.01) discard;
    fragColor = vec4(vertexColor.rgb * (0.7 + 0.3 * core), a) * ColorModulator;
}
