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
varying float blockId;

#ifdef WATER_WAVES
float waveHeightAt(vec2 p, float t) {
    float h = sin(p.x * 0.9 + t * 1.5) * 0.50
            + sin((p.x + p.y) * 0.6 + t * 1.1) * 0.35
            + sin(p.y * 1.3 - t * 0.9) * 0.40
            + sin((p.x - p.y) * 2.1 + t * 2.3) * 0.15;
    return h * 0.06 * WATER_WAVE_HEIGHT;
}

vec3 waterWaveNormal(vec3 worldPos) {
    float t = frameTimeCounter * 1.6 * WATER_WAVE_SPEED;
    vec2 p = worldPos.xz;
    float e = 0.08;
    float h0 = waveHeightAt(p, t);
    float hx = waveHeightAt(p + vec2(e, 0.0), t);
    float hz = waveHeightAt(p + vec2(0.0, e), t);
    return normalize(vec3(-(hx - h0) / e, 1.0, -(hz - h0) / e));
}
#endif

void main() {
    vec4 albedo = texture2D(texture, texcoord) * glcolor;
    albedo.rgb = srgbToLinear(albedo.rgb);

    vec2 lm = lmNormalize(lmcoord);
    vec3 N = normalize(normal);
    bool isWater = abs(blockId - 10008.0) < 0.5;

    if (isWater) {
#ifdef WATER_CUSTOM_COLOR
        float texLum = luma(albedo.rgb);
        albedo.rgb = srgbToLinear(vec3(WATER_COLOR_R, WATER_COLOR_G, WATER_COLOR_B)) * (0.4 + 1.8 * texLum);
        albedo.a = WATER_OPACITY;
#endif
#ifdef WATER_WAVES
        vec3 worldNormal = mat3(gbufferModelViewInverse) * N;
        if (worldNormal.y > 0.9) {
            vec3 wn = waterWaveNormal(playerPos + cameraPosition);
            N = normalize(mat3(gbufferModelView) * wn);
        }
#endif
    }

    float NdotL = dot(N, normalize(shadowLightPosition));
    vec3 shadow = getShadow(playerPos, max(NdotL, 0.1), lm.y);
    vec3 color = applyLighting(albedo.rgb, lm, NdotL, shadow);
    float alpha = albedo.a;

    if (isWater) {
        vec3 viewDir = normalize(-viewPos);
        float fresnel = pow(1.0 - clamp(dot(viewDir, N), 0.0, 1.0), 3.0);
        alpha = clamp(alpha + fresnel * (1.0 - alpha) * 0.9, 0.0, 1.0);
#ifdef WATER_SPECULAR
        float df = dayFactor();
        vec3 specColor = mix(vec3(0.42, 0.52, 0.78) * NIGHT_BRIGHTNESS, sunColor() * SUNLIGHT_STRENGTH, df);
        color += specColor * shadow * getSpecular(viewPos, N) * (0.4 + fresnel * 2.0);
#endif
    }

    color = applyFog(color, playerPos);

/* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(color, alpha);
}
