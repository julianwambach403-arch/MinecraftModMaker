#version 120

#include "/lib/settings.glsl"
#include "/lib/common.glsl"

varying vec2 texcoord;

uniform sampler2D colortex0;

uniform float viewWidth;
uniform float viewHeight;
uniform float frameTimeCounter;

vec3 fetchColor(vec2 uv) {
    vec3 c = texture2D(colortex0, uv).rgb;

    return c;
}

void main() {
    vec2 uv = texcoord;
    vec3 color;

#ifdef CHROMATIC_ABERRATION
    vec2 shift = (uv - 0.5) * 0.006 * CA_STRENGTH;
    color.r = fetchColor(uv + shift).r;
    color.g = fetchColor(uv).g;
    color.b = fetchColor(uv - shift).b;
#else
    color = fetchColor(uv);
#endif

    color *= EXPOSURE;

#if TONEMAP == 1
    color = tonemapACES(color);
#elif TONEMAP == 2
    color = tonemapReinhard(color);
#elif TONEMAP == 3
    color = tonemapUncharted2(color);
#endif

    color = linearToSrgb(clamp(color, 0.0, 1.0));

    // Farbkorrektur
    color = (color - 0.5) * CONTRAST + 0.5 + BRIGHTNESS;
    color = mix(vec3(luma(color)), color, SATURATION);
    color = pow(clamp(color, 0.0, 1.0), vec3(1.0 / GAMMA_ADJUST));

#ifdef VIGNETTE
    float vDist = distance(uv, vec2(0.5)) * 1.414;
    color *= 1.0 - VIGNETTE_STRENGTH * smoothstep(0.5, 1.05, vDist);
#endif

#ifdef FILM_GRAIN
    float grain = hash12(uv * vec2(viewWidth, viewHeight) + fract(frameTimeCounter) * 951.35);
    color += (grain - 0.5) * 0.07 * GRAIN_STRENGTH;
#endif

    gl_FragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
