#version 150

uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float hash(float n) {
    return fract(sin(n) * 43758.5453);
}

float noise(float x) {
    float i = floor(x);
    float f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(hash(i), hash(i + 1.0), f);
}

void main() {
    float secs = GameTime * 1200.0;
    float across = abs(texCoord0.y * 2.0 - 1.0);
    float core = 1.0 - smoothstep(0.34, 0.6, across);
    float halo = 1.0 - across;
    halo *= halo;

    float flow = noise(texCoord0.x * 5.0 - secs * 3.0);
    float ember = smoothstep(0.72, 0.98, flow) * core;
    float ragged = 0.9 + 0.1 * noise(texCoord0.x * 9.0 + 7.0);

    vec3 col = vertexColor.rgb * (0.6 + 0.4 * core) + vec3(1.0, 0.62, 0.52) * ember * 0.7;
    float a = max(core * ragged, halo * 0.4) * vertexColor.a;
    if (a < 0.01) discard;
    fragColor = vec4(col, a) * ColorModulator;
}
