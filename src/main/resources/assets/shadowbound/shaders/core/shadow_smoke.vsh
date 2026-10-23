#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 InvViewRot;
uniform vec3 CameraOffset;

out vec3 worldPos;
out vec3 viewPos;
out vec3 viewNormal;
out vec2 texCoord0;
out vec4 smokeData;
out vec4 lightColor;
out vec4 overlayColor;
out float vertexDistance;
out vec3 smokeP;

uniform float GameTime;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i), hash(i + vec3(1.0, 0.0, 0.0)), f.x),
                   mix(hash(i + vec3(0.0, 1.0, 0.0)), hash(i + vec3(1.0, 1.0, 0.0)), f.x), f.y),
               mix(mix(hash(i + vec3(0.0, 0.0, 1.0)), hash(i + vec3(1.0, 0.0, 1.0)), f.x),
                   mix(hash(i + vec3(0.0, 1.0, 1.0)), hash(i + vec3(1.0, 1.0, 1.0)), f.x), f.y), f.z);
}

void main() {
    vec4 vp = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * vp;

    viewPos = vp.xyz;
    vertexDistance = length(vp.xyz);
    worldPos = (InvViewRot * vec4(Position, 1.0)).xyz + CameraOffset;
    viewNormal = mat3(ModelViewMat) * Normal;

    texCoord0 = UV0;
    smokeData = Color;
    lightColor = texelFetch(Sampler2, UV2 / 16, 0);
    overlayColor = texelFetch(Sampler1, UV1, 0);

    float t = GameTime * 1200.0;
    float seed = Color.g * 41.0;
    vec3 p = worldPos * 2.1 + vec3(seed, -t * 0.85, seed * 0.6);
    vec3 warp = vec3(noise(p * 0.7 + vec3(0.0, 0.0, t * 0.25)),
                     0.0,
                     noise(p * 0.7 + vec3(5.2, 1.3, -t * 0.25))) - vec3(0.5, 0.0, 0.5);
    smokeP = p + warp * 1.6;
}
