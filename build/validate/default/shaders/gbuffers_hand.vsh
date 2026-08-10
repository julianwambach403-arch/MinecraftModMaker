#version 120

#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/vsh_common.glsl"

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 viewPos;
varying vec3 playerPos;
varying vec3 normal;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
    lmcoord  = (gl_TextureMatrix[1] * gl_MultiTexCoord1).st;
    glcolor  = gl_Color;
    normal   = normalize(gl_NormalMatrix * gl_Normal);

    vec4 vPos = gl_ModelViewMatrix * gl_Vertex;
    vec4 pPos = gbufferModelViewInverse * vPos;

    playerPos = pPos.xyz;
    viewPos   = vPos.xyz;
    gl_Position = gl_ProjectionMatrix * vPos;
}
