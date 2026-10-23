#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    float a = tex.a * vertexColor.a;
    if (a < 0.003) discard;
    fragColor = vec4(tex.rgb * vertexColor.rgb, a) * ColorModulator;
}
