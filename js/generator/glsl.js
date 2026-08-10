// GLSL-Erzeugung für das Shaderpack (OptiFine-/Iris-Format, GLSL 120).
// Aufbau:
//   lib/settings.glsl  – alle Optionen (aus den UI-Einstellungen generiert)
//   lib/common.glsl    – Hilfsfunktionen (Tonemapping, Farbraum, Hash, ...)
//   lib/vsh_common.glsl– Vertex-Uniforms + Waving-Funktionen
//   lib/lighting.glsl  – Beleuchtung, Schatten, Nebel (Fragment)
//   gbuffers_*, shadow, composite*, final

import { hexToRgb01 } from '../state.js';

const fmt = (v, d = 2) => Number(v).toFixed(d);

// Erzeugt eine Werteliste für OptiFine-Slider: "0.00 0.05 ... 2.00",
// stellt sicher, dass der aktuelle Wert enthalten ist.
function sliderValues(min, max, step, current, decimals = 2) {
  const values = new Set();
  for (let v = min; v <= max + 1e-9; v += step) values.add(fmt(v, decimals));
  values.add(fmt(current, decimals));
  return [...values].sort((a, b) => Number(a) - Number(b)).join(' ');
}

function toggle(name, enabled) {
  return `${enabled ? '' : '//'}#define ${name}`;
}

function slider(name, value, min, max, step, decimals = 2) {
  return `#define ${name} ${fmt(value, decimals)} // [${sliderValues(min, max, step, value, decimals)}]`;
}

function colorDefines(name, hex) {
  const [r, g, b] = hexToRgb01(hex);
  return [
    slider(`${name}_R`, r, 0, 1, 0.01),
    slider(`${name}_G`, g, 0, 1, 0.01),
    slider(`${name}_B`, b, 0, 1, 0.01)
  ].join('\n');
}

// ---------------------------------------------------------------------------
// lib/settings.glsl (dynamisch)
// ---------------------------------------------------------------------------
export function buildSettingsGlsl(s) {
  const L = s.lighting;
  const tonemapId = { none: 0, aces: 1, reinhard: 2, uncharted2: 3 }[s.post.tonemap] ?? 1;

  const lines = [
    `/*`,
    `    ${s.meta.name} – erstellt mit ShaderCreator / created with ShaderCreator`,
    `    Alle Optionen sind auch im Spiel einstellbar (Videoeinstellungen -> Shader -> Shader-Optionen).`,
    `*/`,
    ``,
    `// ===== Beleuchtung / Lighting =====`,
    slider('SUNLIGHT_STRENGTH', L.sunlightStrength, 0, 2, 0.05),
    slider('AMBIENT_STRENGTH', L.ambientStrength, 0, 2, 0.05),
    slider('NIGHT_BRIGHTNESS', L.nightBrightness, 0, 1, 0.05),
    slider('TORCH_STRENGTH', L.torchStrength, 0, 2, 0.05),
    colorDefines('TORCH_COLOR', L.torchColor),
    `const float sunPathRotation = ${fmt(L.sunPathRotation, 1)}; // [${sliderValues(-60, 60, 5, L.sunPathRotation, 1)}]`,
    ``
  ];

  if (L.shadows) {
    lines.push(
      `// ===== Schatten / Shadows =====`,
      `#define SHADOWS`,
      `#define SHADOW_DISTORTION 0.85`,
      `const int shadowMapResolution = ${L.shadowResolution}; // [1024 2048 4096 8192]`,
      `const float shadowDistance = ${fmt(L.shadowDistance, 1)}; // [${sliderValues(64, 256, 32, L.shadowDistance, 1)}]`,
      toggle('SHADOW_SOFT', L.softShadows),
      `#define SHADOW_SAMPLES ${L.shadowSamples} // [1 2 3 4]`,
      slider('SHADOW_BIAS', L.shadowBias, 0.5, 3, 0.25),
      toggle('COLORED_SHADOWS', L.coloredShadows),
      ``
    );
  }

  lines.push(
    `// ===== Wasser / Water =====`,
    toggle('WATER_CUSTOM_COLOR', s.water.customColor),
    colorDefines('WATER_COLOR', s.water.color),
    slider('WATER_OPACITY', s.water.opacity, 0.1, 1, 0.05),
    toggle('WATER_WAVES', s.water.waves),
    slider('WATER_WAVE_HEIGHT', s.water.waveHeight, 0, 2, 0.1, 1),
    slider('WATER_WAVE_SPEED', s.water.waveSpeed, 0, 2, 0.1, 1),
    toggle('WATER_SPECULAR', s.water.specular),
    ``,
    `// ===== Himmel & Nebel / Sky & fog =====`,
    colorDefines('SKY_TINT_DAY', s.sky.tintDay),
    colorDefines('SKY_TINT_NIGHT', s.sky.tintNight),
    colorDefines('SUN_TINT', s.sky.sunTint),
    toggle('FOG_ENABLED', s.fog.enabled),
    slider('FOG_DENSITY', s.fog.density, 0, 3, 0.1, 1),
    slider('FOG_START', s.fog.start, 0, 0.9, 0.05),
    ``,
    `// ===== Bewegung / Waving =====`,
    toggle('WAVING_PLANTS', s.motion.wavingPlants),
    toggle('WAVING_LEAVES', s.motion.wavingLeaves),
    slider('WAVING_STRENGTH', s.motion.strength, 0, 2, 0.1, 1),
    slider('WAVING_SPEED', s.motion.speed, 0, 2, 0.1, 1),
    ``,
    `// ===== Farbe & Effekte / Post processing =====`,
    `#define TONEMAP ${tonemapId} // [0 1 2 3]`,
    slider('EXPOSURE', s.post.exposure, 0.5, 2, 0.05),
    slider('SATURATION', s.post.saturation, 0, 2, 0.05),
    slider('CONTRAST', s.post.contrast, 0.5, 1.5, 0.02),
    slider('BRIGHTNESS', s.post.brightness, -0.25, 0.25, 0.01),
    slider('GAMMA_ADJUST', s.post.gamma, 0.5, 2, 0.05)
  );

  if (s.post.bloom) {
    lines.push(
      slider('BLOOM_STRENGTH', s.post.bloomStrength, 0, 1, 0.05),
      slider('BLOOM_RADIUS', s.post.bloomRadius, 0.5, 2, 0.25),
      slider('BLOOM_THRESHOLD', 0.75, 0.3, 1.5, 0.05)
    );
  }

  lines.push(
    toggle('VIGNETTE', s.post.vignette),
    slider('VIGNETTE_STRENGTH', s.post.vignetteStrength, 0, 1, 0.05),
    toggle('CHROMATIC_ABERRATION', s.post.chromaticAberration),
    slider('CA_STRENGTH', s.post.caStrength, 0, 2, 0.1, 1),
    toggle('FILM_GRAIN', s.post.filmGrain),
    slider('GRAIN_STRENGTH', s.post.grainStrength, 0, 1, 0.05),
    ``
  );

  return lines.join('\n');
}

// ---------------------------------------------------------------------------
// Statische Bibliotheken
// ---------------------------------------------------------------------------
export const COMMON_GLSL = `// Gemeinsame Hilfsfunktionen

float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

vec3 srgbToLinear(vec3 c) {
    return pow(max(c, vec3(0.0)), vec3(2.2));
}

vec3 linearToSrgb(vec3 c) {
    return pow(max(c, vec3(0.0)), vec3(1.0 / 2.2));
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.x, p.y, p.x) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec3 tonemapACES(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

vec3 tonemapReinhard(vec3 x) {
    return x / (1.0 + luma(x));
}

vec3 uncharted2Curve(vec3 x) {
    const float A = 0.15; const float B = 0.50; const float C = 0.10;
    const float D = 0.20; const float E = 0.02; const float F = 0.30;
    return ((x * (A * x + C * B) + D * E) / (x * (A * x + B) + D * F)) - E / F;
}

vec3 tonemapUncharted2(vec3 c) {
    vec3 white = uncharted2Curve(vec3(11.2));
    return uncharted2Curve(c * 2.0) / white;
}

#ifdef SHADOWS
// Verzerrt die Schattenkarte, damit nahe Schatten mehr Auflösung bekommen.
vec3 distortShadowClip(vec3 clipPos) {
    float factor = length(clipPos.xy) * SHADOW_DISTORTION + (1.0 - SHADOW_DISTORTION);
    return vec3(clipPos.xy / factor, clipPos.z * 0.5);
}
#endif
`;

export const VSH_COMMON_GLSL = `// Vertex-Uniforms und Wind-/Wellen-Funktionen

uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;
uniform float frameTimeCounter;

// Windversatz für Pflanzen (nur obere Vertices)
vec2 plantSway(vec3 worldPos) {
    float t = frameTimeCounter * 1.3 * WAVING_SPEED;
    float phase = worldPos.x * 1.7 + worldPos.z * 1.1 + worldPos.y * 0.4;
    vec2 sway = vec2(sin(t + phase), cos(t * 0.66 + phase * 1.4)) * vec2(1.0, 0.6);
    float gust = 0.6 + 0.4 * sin(t * 0.27 + (worldPos.x + worldPos.z) * 0.08);
    return sway * gust * 0.045 * WAVING_STRENGTH;
}

// Sanftes Schwanken ganzer Blöcke (Blätter, Ranken)
vec3 leafSway(vec3 worldPos) {
    float t = frameTimeCounter * 1.1 * WAVING_SPEED;
    float phase = dot(worldPos, vec3(0.7, 0.5, 0.8));
    vec3 sway = vec3(sin(t + phase), sin(t * 0.7 + phase * 1.3) * 0.4, cos(t * 0.8 + phase));
    return sway * 0.016 * WAVING_STRENGTH;
}

// Vertikales Auf und Ab der Wasseroberfläche
float waterBob(vec3 worldPos) {
    float t = frameTimeCounter * 1.6 * WATER_WAVE_SPEED;
    float wave = sin(worldPos.x * 0.8 + worldPos.z * 0.7 + t)
               + sin(worldPos.x * 1.7 - worldPos.z * 1.3 + t * 1.4) * 0.5;
    return wave * 0.022 * WATER_WAVE_HEIGHT;
}
`;

export const LIGHTING_GLSL = `// Beleuchtung, Schatten und Nebel (Fragment-Shader)

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
`;

// ---------------------------------------------------------------------------
// Programm-Templates
// ---------------------------------------------------------------------------
const HEADER = `#version 120

#include "/lib/settings.glsl"
#include "/lib/common.glsl"
`;

const VARYINGS = `varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 viewPos;
varying vec3 playerPos;
varying vec3 normal;
`;

// Generischer Gbuffer-Vertex-Shader.
// options: { attributes: bool, waving: 'terrain'|'water'|null, blockId: bool }
function gbufferVsh({ attributes = false, waving = null, blockId = false } = {}) {
  let src = HEADER + `#include "/lib/vsh_common.glsl"

`;
  if (attributes) {
    src += `attribute vec4 mc_Entity;
attribute vec4 mc_midTexCoord;

`;
  }
  src += VARYINGS;
  if (blockId) src += `varying float blockId;\n`;
  src += `
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
    lmcoord  = (gl_TextureMatrix[1] * gl_MultiTexCoord1).st;
    glcolor  = gl_Color;
    normal   = normalize(gl_NormalMatrix * gl_Normal);
`;
  if (blockId) src += `    blockId  = mc_Entity.x;\n`;
  src += `
    vec4 vPos = gl_ModelViewMatrix * gl_Vertex;
    vec4 pPos = gbufferModelViewInverse * vPos;
`;
  if (waving === 'terrain') {
    src += `
#if defined WAVING_PLANTS || defined WAVING_LEAVES
    vec3 worldPos = pPos.xyz + cameraPosition;
#ifdef WAVING_PLANTS
    if (abs(mc_Entity.x - 10001.0) < 0.5 && gl_MultiTexCoord0.t < mc_midTexCoord.t) {
        pPos.xz += plantSway(worldPos);
        vPos = gbufferModelView * pPos;
    }
#endif
#ifdef WAVING_LEAVES
    if (abs(mc_Entity.x - 10002.0) < 0.5) {
        pPos.xyz += leafSway(worldPos);
        vPos = gbufferModelView * pPos;
    }
#endif
#endif
`;
  } else if (waving === 'water') {
    src += `
#ifdef WATER_WAVES
    if (abs(mc_Entity.x - 10008.0) < 0.5) {
        vec3 worldPos = pPos.xyz + cameraPosition;
        pPos.y += waterBob(worldPos);
        vPos = gbufferModelView * pPos;
    }
#endif
`;
  }
  src += `
    playerPos = pPos.xyz;
    viewPos   = vPos.xyz;
    gl_Position = gl_ProjectionMatrix * vPos;
}
`;
  return src;
}

const FSH_LIT_HEADER = HEADER + `#include "/lib/lighting.glsl"

uniform sampler2D texture;

` + VARYINGS;

function buildTerrainFsh() {
  return FSH_LIT_HEADER + `varying float blockId;

void main() {
    vec4 albedo = texture2D(texture, texcoord) * glcolor;
    if (albedo.a < 0.1) discard;
    albedo.rgb = srgbToLinear(albedo.rgb);

    vec2 lm = lmNormalize(lmcoord);
    vec3 N = normalize(normal);
    float NdotL = dot(N, normalize(shadowLightPosition));
    // Pflanzen (Kreuz-Modelle) gleichmäßig beleuchten, sonst flackern die Normalen
    if (abs(blockId - 10001.0) < 0.5) NdotL = 0.85;

    vec3 shadow = getShadow(playerPos, NdotL, lm.y);
    vec3 color = applyLighting(albedo.rgb, lm, NdotL, shadow);
    color = applyFog(color, playerPos);

/* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(color, albedo.a);
}
`;
}

function buildWaterFsh() {
  return FSH_LIT_HEADER + `varying float blockId;

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
`;
}

function buildEntitiesFsh({ entityColor = true } = {}) {
  return FSH_LIT_HEADER + (entityColor ? `uniform vec4 entityColor;\n\n` : `\n`) + `void main() {
    vec4 albedo = texture2D(texture, texcoord) * glcolor;
    if (albedo.a < 0.1) discard;
    albedo.rgb = srgbToLinear(albedo.rgb);
` + (entityColor ? `    albedo.rgb = mix(albedo.rgb, srgbToLinear(entityColor.rgb), entityColor.a);\n` : ``) + `
    vec2 lm = lmNormalize(lmcoord);
    vec3 N = normalize(normal);
    // Weiches N·L, damit Rückseiten von Mobs/Items nicht komplett schwarz sind
    float NdotL = max(dot(N, normalize(shadowLightPosition)), 0.0) * 0.6 + 0.4;

    vec3 shadow = getShadow(playerPos, 1.0, lm.y);
    vec3 color = applyLighting(albedo.rgb, lm, NdotL, shadow);
    color = applyFog(color, playerPos);

/* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(color, albedo.a);
}
`;
}

function buildTexturedLitFsh() {
  return FSH_LIT_HEADER + `
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
`;
}

function buildTexturedFsh() {
  // Glint, Spinnenaugen, Beacon-Strahl usw. – selbstleuchtend, keine Beleuchtung
  return HEADER + `
uniform sampler2D texture;

` + VARYINGS + `
void main() {
    vec4 albedo = texture2D(texture, texcoord) * glcolor;
    if (albedo.a < 0.01) discard;
    albedo.rgb = srgbToLinear(albedo.rgb);

/* DRAWBUFFERS:0 */
    gl_FragData[0] = albedo;
}
`;
}

function buildBasicFsh() {
  return HEADER + `
` + VARYINGS + `
void main() {
    vec4 color = glcolor;
    color.rgb = srgbToLinear(color.rgb);

/* DRAWBUFFERS:0 */
    gl_FragData[0] = color;
}
`;
}

function buildSkybasicFsh() {
  return HEADER + `#include "/lib/lighting.glsl"

` + VARYINGS + `
void main() {
    vec3 color = srgbToLinear(glcolor.rgb) * skyTint();

/* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(color, glcolor.a);
}
`;
}

function buildSkytexturedFsh() {
  return FSH_LIT_HEADER + `
void main() {
    vec4 albedo = texture2D(texture, texcoord) * glcolor;
    albedo.rgb = srgbToLinear(albedo.rgb);

    // Sonne & Mond leicht verstärken, damit Bloom sie aufnimmt
    vec3 color = albedo.rgb * srgbToLinear(vec3(SUN_TINT_R, SUN_TINT_G, SUN_TINT_B)) * 1.8;

/* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(color, albedo.a);
}
`;
}

function buildCloudsFsh() {
  return FSH_LIT_HEADER + `
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
`;
}

function buildWeatherFsh() {
  return FSH_LIT_HEADER + `
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
`;
}

function buildDamagedblockFsh() {
  // Risse werden multiplikativ geblendet – Textur unverändert durchreichen
  return HEADER + `
uniform sampler2D texture;

` + VARYINGS + `
void main() {
    vec4 albedo = texture2D(texture, texcoord) * glcolor;

/* DRAWBUFFERS:0 */
    gl_FragData[0] = albedo;
}
`;
}

function buildShadowVsh() {
  return HEADER + `#include "/lib/vsh_common.glsl"

varying vec2 texcoord;
varying vec4 glcolor;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
    glcolor = gl_Color;
    gl_Position = ftransform();
    gl_Position.xyz = distortShadowClip(gl_Position.xyz);
}
`;
}

function buildShadowFsh() {
  return HEADER + `
uniform sampler2D texture;

varying vec2 texcoord;
varying vec4 glcolor;

void main() {
    vec4 color = texture2D(texture, texcoord) * glcolor;
    if (color.a < 0.1) discard;

    // Farbe der Schattenwerfer für gefärbte Schatten (shadowcolor0)
    gl_FragData[0] = color;
}
`;
}

const FULLSCREEN_VSH = `#version 120

varying vec2 texcoord;

void main() {
    gl_Position = ftransform();
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).st;
}
`;

function buildCompositeFsh(bloom) {
  if (!bloom) {
    return HEADER + `
varying vec2 texcoord;

uniform sampler2D colortex0;

/*
const int colortex0Format = R11F_G11F_B10F;
*/

void main() {
/* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(texture2D(colortex0, texcoord).rgb, 1.0);
}
`;
  }
  return HEADER + `
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
`;
}

// Gauß-Weichzeichner (9 Taps), horizontal oder vertikal
function buildBlurFsh(horizontal) {
  const weights = [0.0162, 0.0540, 0.1216, 0.1945, 0.2270, 0.1945, 0.1216, 0.0540, 0.0162];
  const dir = horizontal ? 'vec2(1.0 / viewWidth, 0.0)' : 'vec2(0.0, 1.0 / viewHeight)';
  const taps = weights
    .map((w, i) => `    blur += texture2D(colortex1, texcoord + blurStep * ${fmt(i - 4, 1)}).rgb * ${w};`)
    .join('\n');
  return HEADER + `
varying vec2 texcoord;

uniform sampler2D colortex1;
uniform float viewWidth;
uniform float viewHeight;

void main() {
    vec2 blurStep = ${dir} * 2.0 * BLOOM_RADIUS;
    vec3 blur = vec3(0.0);
${taps}

/* DRAWBUFFERS:1 */
    gl_FragData[0] = vec4(blur, 1.0);
}
`;
}

function buildFinalFsh(bloom) {
  return HEADER + `
varying vec2 texcoord;

uniform sampler2D colortex0;
${bloom ? 'uniform sampler2D colortex1;' : ''}
uniform float viewWidth;
uniform float viewHeight;
uniform float frameTimeCounter;

vec3 fetchColor(vec2 uv) {
    vec3 c = texture2D(colortex0, uv).rgb;
${bloom ? '    c += texture2D(colortex1, uv).rgb * BLOOM_STRENGTH;' : ''}
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
`;
}

// ---------------------------------------------------------------------------
// Alle GLSL-Dateien des Packs erzeugen
// ---------------------------------------------------------------------------
export function buildShaderFiles(settings) {
  const files = {};
  const bloom = settings.post.bloom;

  files['shaders/lib/settings.glsl'] = buildSettingsGlsl(settings);
  files['shaders/lib/common.glsl'] = COMMON_GLSL;
  files['shaders/lib/vsh_common.glsl'] = VSH_COMMON_GLSL;
  files['shaders/lib/lighting.glsl'] = LIGHTING_GLSL;

  const simpleVsh = gbufferVsh();
  const terrainVsh = gbufferVsh({ attributes: true, waving: 'terrain', blockId: true });
  const waterVsh = gbufferVsh({ attributes: true, waving: 'water', blockId: true });

  // Gbuffer-Programme
  files['shaders/gbuffers_basic.vsh'] = simpleVsh;
  files['shaders/gbuffers_basic.fsh'] = buildBasicFsh();
  files['shaders/gbuffers_textured.vsh'] = simpleVsh;
  files['shaders/gbuffers_textured.fsh'] = buildTexturedFsh();
  files['shaders/gbuffers_textured_lit.vsh'] = simpleVsh;
  files['shaders/gbuffers_textured_lit.fsh'] = buildTexturedLitFsh();
  files['shaders/gbuffers_skybasic.vsh'] = simpleVsh;
  files['shaders/gbuffers_skybasic.fsh'] = buildSkybasicFsh();
  files['shaders/gbuffers_skytextured.vsh'] = simpleVsh;
  files['shaders/gbuffers_skytextured.fsh'] = buildSkytexturedFsh();
  files['shaders/gbuffers_clouds.vsh'] = simpleVsh;
  files['shaders/gbuffers_clouds.fsh'] = buildCloudsFsh();
  files['shaders/gbuffers_terrain.vsh'] = terrainVsh;
  files['shaders/gbuffers_terrain.fsh'] = buildTerrainFsh();
  files['shaders/gbuffers_damagedblock.vsh'] = simpleVsh;
  files['shaders/gbuffers_damagedblock.fsh'] = buildDamagedblockFsh();
  files['shaders/gbuffers_water.vsh'] = waterVsh;
  files['shaders/gbuffers_water.fsh'] = buildWaterFsh();
  files['shaders/gbuffers_entities.vsh'] = simpleVsh;
  files['shaders/gbuffers_entities.fsh'] = buildEntitiesFsh({ entityColor: true });
  files['shaders/gbuffers_hand.vsh'] = simpleVsh;
  files['shaders/gbuffers_hand.fsh'] = buildEntitiesFsh({ entityColor: false });
  files['shaders/gbuffers_weather.vsh'] = simpleVsh;
  files['shaders/gbuffers_weather.fsh'] = buildWeatherFsh();

  // Schattenkarte
  if (settings.lighting.shadows) {
    files['shaders/shadow.vsh'] = buildShadowVsh();
    files['shaders/shadow.fsh'] = buildShadowFsh();
  }

  // Post-Processing
  files['shaders/composite.vsh'] = FULLSCREEN_VSH;
  files['shaders/composite.fsh'] = buildCompositeFsh(bloom);
  if (bloom) {
    files['shaders/composite1.vsh'] = FULLSCREEN_VSH;
    files['shaders/composite1.fsh'] = buildBlurFsh(true);
    files['shaders/composite2.vsh'] = FULLSCREEN_VSH;
    files['shaders/composite2.fsh'] = buildBlurFsh(false);
  }
  files['shaders/final.vsh'] = FULLSCREEN_VSH;
  files['shaders/final.fsh'] = buildFinalFsh(bloom);

  return files;
}
