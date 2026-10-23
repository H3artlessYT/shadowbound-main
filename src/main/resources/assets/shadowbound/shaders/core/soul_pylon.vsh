#version 150

#moj_import <light.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out vec2 texCoord0;
out vec4 pylonData;
out vec4 lightColor;
out vec3 viewPos;
out vec3 viewNormal;
out float shade;
out float vertexDistance;

void main() {
    vec4 vp = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * vp;

    viewPos = vp.xyz;
    vertexDistance = length(vp.xyz);
    viewNormal = mat3(ModelViewMat) * Normal;
    shade = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, vec4(1.0)).r;

    texCoord0 = UV0;
    pylonData = Color;
    lightColor = texelFetch(Sampler2, UV2 / 16, 0);
}
