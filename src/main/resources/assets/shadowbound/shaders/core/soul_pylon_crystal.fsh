#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 texCoord0;
in vec4 pylonData;
in vec4 lightColor;
in vec3 viewPos;
in vec3 viewNormal;
in float shade;
in float vertexDistance;

out vec4 fragColor;

const vec3 BLOOD = vec3(0.8, 0.04, 0.06);
const vec3 HOT = vec3(1.0, 0.55, 0.42);

void main() {
    float secs = GameTime * 1200.0;
    float charge = pylonData.r;

    vec3 n = normalize(viewNormal);
    vec3 v = normalize(-viewPos);
    float facing = abs(dot(n, v));
    float fres = pow(1.0 - facing, 2.0);

    vec2 a = vec2(fract(texCoord0.x * 0.8 + pylonData.g), fract(texCoord0.y * 0.6 - secs * 0.12));
    vec2 b = vec2(fract(texCoord0.x * 0.5 - secs * 0.05 + 0.37), fract(texCoord0.y * 0.9 - secs * 0.2));
    float veins = texture(Sampler0, vec2(a.x * 0.5, 0.75 + a.y * 0.25)).r;
    veins *= texture(Sampler0, vec2(b.x * 0.5, 0.75 + b.y * 0.25)).r * 1.6;

    vec3 col;
    float alpha;
    if (pylonData.b > 0.5) {
        float beat = 0.8 + 0.2 * sin(secs * 6.0);
        col = mix(BLOOD, HOT, facing) * (1.2 + charge) * beat;
        alpha = 0.75 * facing + 0.25;
    } else {
        col = BLOOD * (0.15 + veins * 0.9) * (0.6 + 0.6 * charge) + HOT * fres * (0.7 + 0.8 * charge);
        alpha = 0.55 + 0.45 * fres;
    }
    fragColor = vec4(col, alpha * pylonData.a) * ColorModulator;
}
