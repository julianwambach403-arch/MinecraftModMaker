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
    if (albedo.a < 0.1) discard;
    albedo.rgb = srgbToLinear(albedo.rgb);

    float df = dayFactor();
    vec3 light = mix(vec3(0.10, 0.13, 0.22) * (0.5 + NIGHT_BRIGHTNESS), vec3(1.05, 1.00, 0.95), df);
    light *= 1.0 - 0.5 * rainStrength;

    vec3 color = applyFog(albedo.rgb * light, playerPos);

/* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(color, albedo.a * 0.9);
}
