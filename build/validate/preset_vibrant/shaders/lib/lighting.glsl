// Beleuchtung, Schatten und Nebel (Fragment-Shader)

uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;
uniform vec3 sunPosition;
uniform vec3 moonPosition;
uniform vec3 shadowLightPosition;
uniform vec3 upPosition;
uniform float rainStrength;
uniform float frameTimeCounter;
uniform int isEyeInWater;
uniform vec3 fogColor;
uniform float far;

#ifdef SHADOWS
uniform mat4 shadowModelView;
uniform mat4 shadowProjection;
uniform sampler2D shadowtex0;
uniform sampler2D shadowtex1;
uniform sampler2D shadowcolor0;
#endif

// 0 = Nacht, 1 = Tag
float dayFactor() {
    return smoothstep(-0.04, 0.12, dot(normalize(sunPosition), normalize(upPosition)));
}

// Sonnenfarbe abhängig vom Sonnenstand (Morgen-/Abendrot)
vec3 sunColor() {
    float h = dot(normalize(sunPosition), normalize(upPosition));
    vec3 warm = mix(vec3(1.0, 0.42, 0.18), vec3(1.0, 0.98, 0.94), smoothstep(0.02, 0.32, h));
    return warm * srgbToLinear(vec3(SUN_TINT_R, SUN_TINT_G, SUN_TINT_B));
}

// Himmels-Tönung (linear), Tag/Nacht-Mischung
vec3 skyTint() {
    vec3 day = srgbToLinear(vec3(SKY_TINT_DAY_R, SKY_TINT_DAY_G, SKY_TINT_DAY_B));
    vec3 night = srgbToLinear(vec3(SKY_TINT_NIGHT_R, SKY_TINT_NIGHT_G, SKY_TINT_NIGHT_B));
    return mix(night, day, dayFactor());
}

// Lightmap-Koordinaten von Vanilla (0.03..0.97) auf 0..1 normalisieren
vec2 lmNormalize(vec2 lm) {
    return clamp((lm - 0.03125) * 1.06667, 0.0, 1.0);
}

#ifdef SHADOWS
float shadowTest(sampler2D tex, vec3 sp, float bias) {
    return step(sp.z - bias, texture2D(tex, sp.xy).r);
}

// Einzelne Schattenprobe, optional mit gefärbten Schatten (Glas usw.)
vec3 shadowSampleOnce(vec3 sp, float bias) {
#ifdef COLORED_SHADOWS
    float visAll = shadowTest(shadowtex0, sp, bias);
    float visSolid = shadowTest(shadowtex1, sp, bias);
    vec4 casterColor = texture2D(shadowcolor0, sp.xy);
    vec3 tinted = srgbToLinear(casterColor.rgb) * (1.0 - casterColor.a * 0.8);
    return mix(tinted * visSolid, vec3(1.0), visAll);
#else
    return vec3(shadowTest(shadowtex1, sp, bias));
#endif
}
#endif

// Sichtbarkeit des direkten Lichts (0..1 je Farbkanal).
// playerPos: Position relativ zur Kamera, skyLight: normalisierter Lightmap-Himmelsanteil
vec3 getShadow(vec3 playerPos, float NdotL, float skyLight) {
    float skyFallback = smoothstep(0.6, 0.95, skyLight);
#ifndef SHADOWS
    return vec3(skyFallback);
#else
    if (NdotL <= 0.0) return vec3(0.0);

    float dist = length(playerPos);
    float fade = smoothstep(shadowDistance * 0.75, shadowDistance * 0.95, dist);
    if (fade >= 1.0) return vec3(skyFallback);

    vec4 shadowClip = shadowProjection * (shadowModelView * vec4(playerPos, 1.0));
    float distortFactor = length(shadowClip.xy) * SHADOW_DISTORTION + (1.0 - SHADOW_DISTORTION);
    vec3 sp = vec3(shadowClip.xy / distortFactor, shadowClip.z * 0.5) * 0.5 + 0.5;
    if (sp.x < 0.0 || sp.x > 1.0 || sp.y < 0.0 || sp.y > 1.0) return vec3(skyFallback);

    float bias = (0.0012 + 0.0040 * (1.0 - NdotL)) * distortFactor * distortFactor;
    bias *= SHADOW_BIAS * 2048.0 / float(shadowMapResolution);

    vec3 shadow = vec3(0.0);
#ifdef SHADOW_SOFT
    float texel = 1.0 / float(shadowMapResolution);
    for (int x = -SHADOW_SAMPLES; x <= SHADOW_SAMPLES; x++) {
        for (int y = -SHADOW_SAMPLES; y <= SHADOW_SAMPLES; y++) {
            vec2 offset = vec2(float(x), float(y)) * texel;
            shadow += shadowSampleOnce(vec3(sp.xy + offset, sp.z), bias);
        }
    }
    float n = float(2 * SHADOW_SAMPLES + 1);
    shadow /= n * n;
#else
    shadow = shadowSampleOnce(sp, bias);
#endif
    return mix(shadow, vec3(skyFallback), fade);
#endif
}

// Komplette Beleuchtung: direktes Licht + Himmelslicht + Blocklicht.
// lm ist bereits mit lmNormalize() normalisiert.
vec3 applyLighting(vec3 albedo, vec2 lm, float NdotL, vec3 shadow) {
    float df = dayFactor();
    float rainDim = 1.0 - 0.8 * rainStrength;

    vec3 dayLight = sunColor() * SUNLIGHT_STRENGTH;
    vec3 nightLight = vec3(0.42, 0.52, 0.78) * NIGHT_BRIGHTNESS;
    vec3 direct = mix(nightLight, dayLight, df) * (max(NdotL, 0.0) * rainDim) * shadow;

    vec3 daySky = vec3(0.52, 0.63, 0.82);
    vec3 nightSky = vec3(0.12, 0.16, 0.27) * (0.5 + NIGHT_BRIGHTNESS);
    vec3 skyAmbient = mix(nightSky, daySky, df) * (1.0 - 0.35 * rainStrength);
    vec3 ambient = skyAmbient * (lm.y * lm.y) * AMBIENT_STRENGTH + vec3(0.03);

    vec3 torchColor = srgbToLinear(vec3(TORCH_COLOR_R, TORCH_COLOR_G, TORCH_COLOR_B));
    vec3 blockLight = torchColor * TORCH_STRENGTH * (pow(lm.x, 2.2) * 1.4 + pow(lm.x, 8.0) * 0.6);

    return albedo * (direct + ambient + blockLight);
}

// Entfernungs- und Unterwassernebel
vec3 applyFog(vec3 color, vec3 playerPos) {
    float dist = length(playerPos);

    if (isEyeInWater == 1) {
        vec3 waterFog = srgbToLinear(vec3(WATER_COLOR_R, WATER_COLOR_G, WATER_COLOR_B));
        waterFog *= 0.08 + 0.35 * dayFactor();
        float f = 1.0 - exp(-dist * 0.10);
        return mix(color, waterFog, f);
    }

#ifdef FOG_ENABLED
    vec3 fogCol = srgbToLinear(fogColor) * skyTint();
    float start = far * FOG_START;
    float f = 1.0 - exp(-max(dist - start, 0.0) / far * 3.0 * FOG_DENSITY);
    f = mix(f, clamp(f * 1.6 + 0.08, 0.0, 1.0), rainStrength * 0.7);
    return mix(color, fogCol, clamp(f, 0.0, 1.0));
#else
    return color;
#endif
}

// Sonnen-/Mondglanz (Blinn-Phong) für Wasser
float getSpecular(vec3 viewPos, vec3 viewNormal) {
    vec3 lightDir = normalize(shadowLightPosition);
    vec3 viewDir = normalize(-viewPos);
    vec3 halfDir = normalize(lightDir + viewDir);
    return pow(max(dot(viewNormal, halfDir), 0.0), 120.0);
}
