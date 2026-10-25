#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    if (tex.a < 0.01) discard;
    float secs = GameTime * 1200.0;

    vec2 local = fract(texCoord0 * 2.0) - 0.5;
    float ang = atan(local.y, local.x);
    float r = length(local);
    float sweep = pow(0.5 + 0.5 * cos(ang - secs * 1.7), 10.0);
    float sweep2 = pow(0.5 + 0.5 * cos(ang * 2.0 + secs * 1.1), 14.0);
    float flicker = 0.85 + 0.15 * sin(secs * 9.0 + r * 40.0);

    float lum = tex.r;
    vec3 deep = vertexColor.rgb * 0.75;
    vec3 hot = vec3(1.0, 0.55, 0.42);
    vec3 col = mix(deep, hot, lum * lum * lum * 0.45);
    col += hot * (sweep * 0.8 + sweep2 * 0.4) * lum * lum;
    col *= flicker;

    float a = tex.a * (0.75 + 0.25 * lum) * vertexColor.a;
    fragColor = vec4(col, a) * ColorModulator;
}
