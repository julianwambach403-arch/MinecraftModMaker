#version 120

#include "/lib/settings.glsl"
#include "/lib/common.glsl"

uniform sampler2D texture;

varying vec2 texcoord;
varying vec4 glcolor;

void main() {
    vec4 color = texture2D(texture, texcoord) * glcolor;
    if (color.a < 0.1) discard;

    // Farbe der Schattenwerfer für gefärbte Schatten (shadowcolor0)
    gl_FragData[0] = color;
}
