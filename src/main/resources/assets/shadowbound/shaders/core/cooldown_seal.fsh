#version 150

uniform sampler2D Sampler0;
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

    float progress = vertexColor.r;
    float flash = vertexColor.g;
    float ready = step(0.999, progress);
    float a01 = fract(atan(p.x, -p.y) / 6.2831853 + 1.0);

    float r0 = 0.56, r1 = 0.82;
    float band = step(r0, r) * step(r, r1);
    float across = clamp((r - r0) / (r1 - r0), 0.0, 0.999);
    vec2 fillUv = vec2((floor(across * 8.0) + 0.5) / 8.0, fract(a01 * 3.0 - secs * 0.08));
    vec3 fill = texture(Sampler0, fillUv).rgb;

    float filled = step(a01, progress);
    float lum = dot(fill, vec3(0.299, 0.587, 0.114));
    vec3 empty = vec3(lum * 0.35, lum * 0.1, lum * 0.12);
    float breathe = 0.85 + 0.15 * sin(secs * 4.0);
    vec3 ring = mix(empty, fill * (ready > 0.5 ? breathe * 1.15 : 1.0), filled);

    float lead = abs(a01 - progress);
    lead = min(lead, 1.0 - lead);
    float front = (1.0 - ready) * band * (1.0 - step(0.02, lead));

    float edgeIn = step(r0 - 0.06, r) * step(r, r0);
    float edgeOut = step(r1, r) * step(r, r1 + 0.06);
    float inner = step(r, r0 - 0.06);
    float halo = ready * (1.0 - smoothstep(r1 + 0.06, 1.0, r)) * step(r1 + 0.06, r) * (0.5 + 0.5 * sin(secs * 4.0));
    float wave = flash * (1.0 - smoothstep(0.0, 0.06, abs(r - (1.0 - flash * 0.45))));

    vec3 frame = vec3(0.16, 0.05, 0.07);
    vec3 col = vec3(0.05, 0.015, 0.02) * inner
             + ring * band
             + vec3(1.0, 0.35, 0.3) * front
             + frame * (edgeIn + edgeOut)
             + vec3(0.8, 0.05, 0.08) * halo * 0.7
             + vec3(1.0, 0.62, 0.5) * wave * 1.4
             + vec3(1.0, 0.5, 0.4) * flash * inner * 0.3;

    float alpha = max(max(inner * 0.82, band), max(edgeIn + edgeOut, max(halo * 0.5, wave)));
    fragColor = vec4(col, alpha * vertexColor.a) * ColorModulator;
}
