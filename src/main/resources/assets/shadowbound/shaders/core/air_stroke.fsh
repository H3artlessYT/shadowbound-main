#version 150

uniform sampler2D Sampler0;
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
    float along = texCoord0.x;
    float across = abs(texCoord0.y * 2.0 - 1.0);

    vec2 bandUv = vec2(fract(along * 0.22 - secs * 0.05), 0.75 + clamp(texCoord0.y, 0.0, 1.0) * 0.125);
    vec4 vein = texture(Sampler0, bandUv);

    float n1 = noise(along * 2.6 - secs * 3.5);
    float n2 = noise(along * 6.5 + secs * 5.0 + 3.7);
    float n3 = noise(along * 13.0 - secs * 9.0 + 11.0);

    float width = 0.5 + 0.3 * n1 + 0.1 * n3;
    float flame = 1.0 - smoothstep(width * 0.4, width, across);
    float tongues = smoothstep(0.6, 0.95, n2) * (1.0 - smoothstep(0.3, 1.0, across));
    float pulse = pow(0.5 + 0.5 * sin(along * 1.3 - secs * 6.0), 6.0);

    vec3 tint = vertexColor.rgb;
    vec3 hot = vec3(1.0, 0.7, 0.58);
    vec3 col = tint * flame * 0.55
             + tint * tongues * 0.6
             + mix(tint, hot, vein.r * vein.r * 0.55) * vein.a * (0.8 + 0.6 * pulse);
    float a = max(max(flame * 0.55, vein.a), tongues * 0.6) * vertexColor.a;
    if (a < 0.004) discard;
    fragColor = vec4(col, a) * ColorModulator;
}
