#version 120

#include "/lib/settings.glsl"
#include "/lib/common.glsl"

varying vec2 texcoord;

uniform sampler2D colortex0;

/*
const int colortex0Format = R11F_G11F_B10F;
*/

void main() {
/* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(texture2D(colortex0, texcoord).rgb, 1.0);
}
