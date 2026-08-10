#version 120

#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/lighting.glsl"

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 viewPos;
varying vec3 playerPos;
varying vec3 normal;

void main() {
    vec3 color = srgbToLinear(glcolor.rgb) * skyTint();

/* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(color, glcolor.a);
}
