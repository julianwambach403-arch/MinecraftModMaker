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
    if (albedo.a < 0.01) discard;
    albedo.rgb = srgbToLinear(albedo.rgb);

    vec2 lm = lmNormalize(lmcoord);
    vec3 shadow = getShadow(playerPos, 1.0, lm.y);
    vec3 color = applyLighting(albedo.rgb, lm, 0.8, shadow);
    color = applyFog(color, playerPos);

/* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(color, albedo.a);
}
