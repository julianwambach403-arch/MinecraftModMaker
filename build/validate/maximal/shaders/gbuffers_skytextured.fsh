#version 120

#include "/lib/settings.glsl"
#include "/lib/common.glsl"
#include "/lib/lighting.glsl"

uniform sampler2D texture;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 viewPos;
varying vec3 playerPos;
varying vec3 normal;

void main() {
    vec4 albedo = texture2D(texture, texcoord) * glcolor;
    albedo.rgb = srgbToLinear(albedo.rgb);

    // Sonne & Mond leicht verstärken, damit Bloom sie aufnimmt
    vec3 color = albedo.rgb * srgbToLinear(vec3(SUN_TINT_R, SUN_TINT_G, SUN_TINT_B)) * 1.8;

/* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(color, albedo.a);
}
