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

#ifdef WATER_WAVES
    if (abs(mc_Entity.x - 10008.0) < 0.5) {
        vec3 worldPos = pPos.xyz + cameraPosition;
        pPos.y += waterBob(worldPos);
        vPos = gbufferModelView * pPos;
    }
#endif

    playerPos = pPos.xyz;
    viewPos   = vPos.xyz;
    gl_Position = gl_ProjectionMatrix * vPos;
}
