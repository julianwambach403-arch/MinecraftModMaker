#version 120

#include "/lib/settings.glsl"
#include "/lib/common.glsl"

varying vec2 texcoord;

uniform sampler2D colortex1;
uniform float viewWidth;
uniform float viewHeight;

void main() {
    vec2 blurStep = vec2(0.0, 1.0 / viewHeight) * 2.0 * BLOOM_RADIUS;
    vec3 blur = vec3(0.0);
    blur += texture2D(colortex1, texcoord + blurStep * -4.0).rgb * 0.0162;
    blur += texture2D(colortex1, texcoord + blurStep * -3.0).rgb * 0.054;
    blur += texture2D(colortex1, texcoord + blurStep * -2.0).rgb * 0.1216;
    blur += texture2D(colortex1, texcoord + blurStep * -1.0).rgb * 0.1945;
    blur += texture2D(colortex1, texcoord + blurStep * 0.0).rgb * 0.227;
    blur += texture2D(colortex1, texcoord + blurStep * 1.0).rgb * 0.1945;
    blur += texture2D(colortex1, texcoord + blurStep * 2.0).rgb * 0.1216;
    blur += texture2D(colortex1, texcoord + blurStep * 3.0).rgb * 0.054;
    blur += texture2D(colortex1, texcoord + blurStep * 4.0).rgb * 0.0162;

/* DRAWBUFFERS:1 */
    gl_FragData[0] = vec4(blur, 1.0);
}
