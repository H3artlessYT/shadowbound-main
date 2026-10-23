#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float secs = GameTime * 1200.0;
    vec2 uv = vec2(fract(texCoord0.x), 0.5 + clamp(texCoord0.y, 0.0, 1.0) * 0.25);
    vec4 tex = texture(Sampler0, uv);

    float sweep = 0.5 + 0.5 * sin(texCoord0.x * 12.566 + secs * 3.0);
    sweep = pow(sweep, 6.0);

    vec3 hot = vec3(1.0, 0.6, 0.5);
    vec3 col = vertexColor.rgb * tex.r * (0.8 + 1.4 * sweep) + hot * sweep * tex.r * tex.a * 0.6;
    fragColor = vec4(col, tex.a * vertexColor.a) * ColorModulator;
}
