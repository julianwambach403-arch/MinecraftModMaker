#version 120

#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/vsh_common.glsl"

attribute vec4 mc_Entity;
attribute vec4 mc_midTexCoord;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 viewPos;
varying vec3 playerPos;
varying vec3 normal;
varying float blockId;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
    lmcoord  = (gl_TextureMatrix[1] * gl_MultiTexCoord1).st;
    glcolor  = gl_Color;
    normal   = normalize(gl_NormalMatrix * gl_Normal);
    blockId  = mc_Entity.x;

    vec4 vPos = gl_ModelViewMatrix * gl_Vertex;
    vec4 pPos = gbufferModelViewInverse * vPos;

#if defined WAVING_PLANTS || defined WAVING_LEAVES
    vec3 worldPos = pPos.xyz + cameraPosition;
#ifdef WAVING_PLANTS
    if (abs(mc_Entity.x - 10001.0) < 0.5 && gl_MultiTexCoord0.t < mc_midTexCoord.t) {
        pPos.xz += plantSway(worldPos);
        vPos = gbufferModelView * pPos;
    }
#endif
#ifdef WAVING_LEAVES
    if (abs(mc_Entity.x - 10002.0) < 0.5) {
        pPos.xyz += leafSway(worldPos);
        vPos = gbufferModelView * pPos;
    }
#endif
#endif

    playerPos = pPos.xyz;
    viewPos   = vPos.xyz;
    gl_Position = gl_ProjectionMatrix * vPos;
}
