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
    float lum = tex.r;
    float flicker = 0.82 + 0.18 * sin(secs * 11.0 + texCoord0.x * 90.0 + texCoord0.y * 60.0);
    vec3 col = mix(vertexColor.rgb * 0.8, vec3(1.0, 0.6, 0.5), lum * lum * 0.6) * flicker;
    fragColor = vec4(col, tex.a * vertexColor.a) * ColorModulator;
}
