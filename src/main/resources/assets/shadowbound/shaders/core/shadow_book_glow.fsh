#version 150

uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec2 p = texCoord0 * 2.0 - 1.0;
    float r = length(p);
    if (r > 1.0) discard;
    float secs = GameTime * 1200.0;

    float glow = 1.0 - r;
    glow *= glow * glow;

    float ang = atan(p.y, p.x) + secs * 0.35;
    float seg = mod(ang, 1.0471976) - 0.5235988;
    float hexR = 0.8660254 / cos(seg);
    float hex = 1.0 - smoothstep(0.0, 0.03, abs(r - 0.55 * hexR));

    float ring = 1.0 - smoothstep(0.0, 0.025, abs(r - 0.82));
    float ticks = step(0.7, fract((ang - secs * 0.7) * 3.8197));
    float outer = (1.0 - smoothstep(0.0, 0.05, abs(r - 0.9))) * ticks;

    float a = (glow * 0.55 + hex * 0.75 + ring * 0.6 + outer * 0.5) * vertexColor.a;
    vec3 col = mix(vertexColor.rgb, vec3(1.0, 0.55, 0.45), hex * 0.3);
    fragColor = vec4(col, a) * ColorModulator;
}
