#version 120

#include "/lib/settings.glsl"
#include "/lib/common.glsl"

varying vec2 texcoord;

uniform sampler2D colortex0;

/*
const int colortex0Format = R11F_G11F_B10F;
const int colortex1Format = R11F_G11F_B10F;
*/

void main() {
    vec3 color = texture2D(colortex0, texcoord).rgb;

    // Helle Bereiche für Bloom extrahieren
    float brightness = luma(color);
    vec3 bright = color * smoothstep(BLOOM_THRESHOLD, BLOOM_THRESHOLD + 0.6, brightness);

/* DRAWBUFFERS:01 */
    gl_FragData[0] = vec4(color, 1.0);
    gl_FragData[1] = vec4(bright, 1.0);
}
