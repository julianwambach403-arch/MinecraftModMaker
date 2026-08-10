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

    float df = dayFactor();
    vec3 light = mix(vec3(0.08, 0.10, 0.18), vec3(0.90, 0.95, 1.05), df);

    vec3 color = applyFog(albedo.rgb * light, playerPos);

/* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(color, albedo.a * 0.85);
}
