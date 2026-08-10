#version 120

#include "/lib/settings.glsl"
#include "/lib/common.glsl"

uniform sampler2D texture;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 viewPos;
varying vec3 playerPos;
varying vec3 normal;

void main() {
    vec4 albedo = texture2D(texture, texcoord) * glcolor;

/* DRAWBUFFERS:0 */
    gl_FragData[0] = albedo;
}
