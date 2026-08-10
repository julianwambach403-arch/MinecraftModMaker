#version 120

#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/vsh_common.glsl"

varying vec2 texcoord;
varying vec4 glcolor;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
    glcolor = gl_Color;
    gl_Position = ftransform();
    gl_Position.xyz = distortShadowClip(gl_Position.xyz);
}
