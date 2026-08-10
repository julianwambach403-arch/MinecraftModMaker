// Live-Vorschau: prozedurale Minecraft-ähnliche Szene (WebGL2, Raymarching).
// Die Licht-, Wasser-, Nebel- und Post-Formeln spiegeln die des generierten
// Shaderpacks wider, damit die Vorschau ehrlich bleibt (Näherung!).

import { hexToRgb01 } from './state.js';

const VERT = `#version 300 es
precision highp float;
out vec2 vUv;
void main() {
  vec2 pos = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
  vUv = pos;
  gl_Position = vec4(pos * 2.0 - 1.0, 0.0, 1.0);
}`;

// ---------------------------------------------------------------------------
// Szene (Pass 1)
// ---------------------------------------------------------------------------
const SCENE_FRAG = `#version 300 es
precision highp float;

in vec2 vUv;
out vec4 fragColor;

uniform vec2 uRes;
uniform float uTime;
uniform vec3 uCamPos;
uniform vec3 uCamTarget;

uniform vec3 uSunDir;
uniform float uDayFactor;

uniform float uSunStrength;
uniform float uAmbientStrength;
uniform float uNightBrightness;
uniform vec3 uSunTint;        // linear
uniform vec3 uTorchColor;     // linear
uniform float uTorchStrength;

uniform int uShadowMode;      // 0 aus, 1 hart, 2 weich
uniform float uShadowSoftK;

uniform vec3 uWaterColor;     // linear
uniform float uWaterOpacity;
uniform int uWaterCustom;
uniform int uWaterWaves;
uniform float uWaveHeight;
uniform float uWaveSpeed;
uniform int uWaterSpecular;

uniform vec3 uSkyTintDay;     // linear
uniform vec3 uSkyTintNight;   // linear
uniform int uClouds;

uniform int uFog;
uniform float uFogDensity;
uniform float uFogStart;

uniform int uWavingLeaves;
uniform int uWavingPlants;
uniform float uWavingStrength;
uniform float uWavingSpeed;

#define FAR 60.0

// ------------------------------ Hilfen ------------------------------------
float hash12(vec2 p) {
  vec3 p3 = fract(vec3(p.xyx) * 0.1031);
  p3 += dot(p3, p3.yzx + 33.33);
  return fract((p3.x + p3.y) * p3.z);
}

float vnoise(vec2 p) {
  vec2 i = floor(p), f = fract(p);
  f = f * f * (3.0 - 2.0 * f);
  return mix(mix(hash12(i), hash12(i + vec2(1, 0)), f.x),
             mix(hash12(i + vec2(0, 1)), hash12(i + vec2(1, 1)), f.x), f.y);
}

float fbm(vec2 p) {
  float v = 0.0, a = 0.5;
  for (int i = 0; i < 4; i++) {
    v += a * vnoise(p);
    p = p * 2.13 + 17.7;
    a *= 0.5;
  }
  return v;
}

float sdBox(vec3 p, vec3 b) {
  vec3 q = abs(p) - b;
  return length(max(q, 0.0)) + min(max(q.x, max(q.y, q.z)), 0.0);
}

float sdTriPrism(vec3 p, vec2 h) {
  vec3 q = abs(p);
  return max(q.z - h.y, max(q.x * 0.866025 + p.y * 0.5, -p.y) - h.x * 0.5);
}

// ------------------------------ Szene -------------------------------------
const vec2 POND_CENTER = vec2(-3.4, 3.0);
const vec2 POND_HALF = vec2(2.3, 1.7);
const float WATER_LEVEL = -0.22;

vec3 leafSway(vec3 base) {
  if (uWavingLeaves == 0) return vec3(0.0);
  float t = uTime * 1.1 * uWavingSpeed;
  float phase = dot(base, vec3(0.7, 0.5, 0.8));
  return vec3(sin(t + phase), sin(t * 0.7 + phase * 1.3) * 0.4, cos(t * 0.8 + phase)) * 0.10 * uWavingStrength;
}

// dist, materialId
vec2 mapScene(vec3 p) {
  // Boden mit Teich-Vertiefung
  float ground = p.y;
  float pond = sdBox(p - vec3(POND_CENTER.x, 0.15, POND_CENTER.y), vec3(POND_HALF.x, 1.05, POND_HALF.y));
  ground = max(ground, -pond);
  vec2 res = vec2(ground, 1.0);

  // Haus
  vec3 hp = p - vec3(3.8, 0.0, -0.8);
  float walls = sdBox(hp - vec3(0.0, 1.05, 0.0), vec3(1.7, 1.05, 1.35));
  if (walls < res.x) res = vec2(walls, 2.0);
  float roof = sdTriPrism(hp - vec3(0.0, 2.1, 0.0), vec2(1.5, 1.55));
  roof = max(roof, -(hp.y - 2.05));
  if (roof < res.x) res = vec2(roof, 3.0);

  // Bäume
  for (int i = 0; i < 2; i++) {
    vec3 base = i == 0 ? vec3(-4.6, 0.0, -3.6) : vec3(1.4, 0.0, 5.4);
    vec3 tp = p - base;
    float trunk = sdBox(tp - vec3(0.0, 1.0, 0.0), vec3(0.22, 1.0, 0.22));
    if (trunk < res.x) res = vec2(trunk, 4.0);
    vec3 sway = leafSway(base);
    float canopy = sdBox(tp - vec3(0.0, 2.55, 0.0) - sway, vec3(1.25, 0.75, 1.25));
    canopy = min(canopy, sdBox(tp - vec3(0.0, 3.6, 0.0) - sway * 1.4, vec3(0.8, 0.5, 0.8)));
    if (canopy < res.x) res = vec2(canopy, 5.0);
  }

  // Steine
  float rocks = sdBox(p - vec3(-0.9, 0.32, -2.9), vec3(0.34, 0.32, 0.3));
  rocks = min(rocks, sdBox(p - vec3(-6.3, 0.26, 0.6), vec3(0.3, 0.26, 0.34)));
  if (rocks < res.x) res = vec2(rocks, 6.0);

  // Fackel
  vec3 fp = p - vec3(2.1, 0.0, 1.6);
  float pole = sdBox(fp - vec3(0.0, 0.42, 0.0), vec3(0.055, 0.42, 0.055));
  if (pole < res.x) res = vec2(pole, 7.0);
  float tip = sdBox(fp - vec3(0.0, 0.94, 0.0), vec3(0.085, 0.10, 0.085));
  if (tip < res.x) res = vec2(tip, 8.0);

  return res;
}

vec3 sceneNormal(vec3 p) {
  vec2 e = vec2(0.0015, 0.0);
  return normalize(vec3(
    mapScene(p + e.xyy).x - mapScene(p - e.xyy).x,
    mapScene(p + e.yxy).x - mapScene(p - e.yxy).x,
    mapScene(p + e.yyx).x - mapScene(p - e.yyx).x));
}

vec2 march(vec3 ro, vec3 rd) {
  float t = 0.02;
  float mat = 0.0;
  for (int i = 0; i < 110; i++) {
    vec2 h = mapScene(ro + rd * t);
    if (h.x < 0.0012 * t + 0.0006) { mat = h.y; break; }
    t += h.x * 0.92;
    if (t > FAR) break;
  }
  return vec2(t, t > FAR ? 0.0 : mat);
}

float softShadow(vec3 ro, vec3 rd) {
  if (uShadowMode == 0) return 1.0;
  float res = 1.0;
  float t = 0.04;
  float k = uShadowMode == 1 ? 64.0 : uShadowSoftK;
  for (int i = 0; i < 48; i++) {
    float h = mapScene(ro + rd * t).x;
    if (h < 0.001) return 0.0;
    res = min(res, k * h / t);
    t += clamp(h, 0.02, 0.6);
    if (t > 30.0) break;
  }
  return clamp(res, 0.0, 1.0);
}

float ambientOcclusion(vec3 p, vec3 n) {
  float occ = 0.0, w = 1.0;
  for (int i = 1; i <= 4; i++) {
    float d = 0.18 * float(i);
    occ += w * (d - mapScene(p + n * d).x);
    w *= 0.65;
  }
  return clamp(1.0 - occ * 1.1, 0.0, 1.0);
}

// ------------------------------ Himmel ------------------------------------
vec3 skyTintLinear() {
  return mix(uSkyTintNight, uSkyTintDay, uDayFactor);
}

vec3 sunColorLinear() {
  float h = uSunDir.y;
  vec3 warm = mix(vec3(1.0, 0.42, 0.18), vec3(1.0, 0.98, 0.94), smoothstep(0.02, 0.32, h));
  return warm * uSunTint;
}

vec3 skyColor(vec3 rd) {
  float df = uDayFactor;
  vec3 zenithDay = vec3(0.16, 0.34, 0.76), horizonDay = vec3(0.58, 0.72, 0.90);
  vec3 zenithNight = vec3(0.008, 0.012, 0.035), horizonNight = vec3(0.035, 0.05, 0.10);
  float horizonMix = pow(1.0 - clamp(rd.y, 0.0, 1.0), 2.6);
  vec3 day = mix(zenithDay, horizonDay, horizonMix);
  vec3 night = mix(zenithNight, horizonNight, horizonMix);
  vec3 sky = mix(night, day, df) * skyTintLinear();

  // Abendrot in Sonnennähe
  float sunAmount = pow(max(dot(rd, uSunDir), 0.0), 6.0);
  float duskband = smoothstep(0.35, 0.0, abs(uSunDir.y)) ;
  sky += vec3(1.0, 0.38, 0.12) * sunAmount * duskband * 0.55;

  // Sonne & Mond
  float sunDisc = smoothstep(0.9993, 0.9997, dot(rd, uSunDir));
  sky += uSunTint * sunDisc * 42.0 * smoothstep(-0.06, 0.02, uSunDir.y);
  sky += sunColorLinear() * pow(max(dot(rd, uSunDir), 0.0), 320.0) * 1.4 * smoothstep(-0.1, 0.05, uSunDir.y);
  vec3 moonDir = -uSunDir;
  float moonDisc = smoothstep(0.9995, 0.9998, dot(rd, moonDir));
  sky += vec3(0.75, 0.82, 0.95) * moonDisc * 2.2 * smoothstep(-0.06, 0.05, moonDir.y);

  // Sterne
  if (df < 0.5 && rd.y > 0.02) {
    vec2 sp = rd.xz / (rd.y + 0.25) * 34.0;
    float star = step(0.9962, hash12(floor(sp)));
    float tw = 0.6 + 0.4 * sin(uTime * 2.6 + hash12(floor(sp) + 7.7) * 40.0);
    sky += vec3(0.9) * star * tw * (1.0 - df * 2.0) * smoothstep(0.02, 0.24, rd.y);
  }

  // Wolken (2D-Schicht)
  if (uClouds == 1 && rd.y > 0.015) {
    vec2 cuv = rd.xz / rd.y * 0.55 + uTime * 0.008;
    float cov = fbm(cuv * 1.7);
    float clouds = smoothstep(0.52, 0.72, cov);
    vec3 cloudCol = mix(vec3(0.05, 0.06, 0.11), vec3(1.05, 1.03, 1.0), df) * skyTintLinear();
    float fade = smoothstep(0.015, 0.12, rd.y);
    sky = mix(sky, cloudCol, clouds * 0.72 * fade);
  }
  return sky;
}

// ------------------------------ Material ----------------------------------
vec3 materialAlbedo(float mat, vec3 p, vec3 n) {
  if (mat == 1.0) { // Boden
    float noise = vnoise(p.xz * 3.1);
    if (p.y < -0.28) return mix(vec3(0.30, 0.24, 0.16), vec3(0.36, 0.30, 0.20), noise); // Teichboden
    float pondDist = sdBox(vec3(p.x - POND_CENTER.x, 0.0, p.z - POND_CENTER.y), vec3(POND_HALF.x, 1.0, POND_HALF.y));
    if (pondDist < 0.55 && p.y < 0.05) return mix(vec3(0.72, 0.66, 0.47), vec3(0.66, 0.58, 0.42), noise); // Sand
    if (n.y < 0.55) return mix(vec3(0.32, 0.22, 0.13), vec3(0.38, 0.27, 0.16), noise); // Erde
    vec3 grass = mix(vec3(0.24, 0.46, 0.16), vec3(0.31, 0.55, 0.20), noise);
    if (uWavingPlants == 1) { // Wind-Wellen im Gras andeuten
      float ripple = sin(p.x * 1.4 + p.z * 0.9 + uTime * 1.8 * uWavingSpeed) * 0.5 + 0.5;
      grass *= 1.0 + (ripple - 0.5) * 0.14 * uWavingStrength;
    }
    return grass;
  }
  if (mat == 2.0) { // Bretter
    float plank = vnoise(vec2(p.y * 9.0, (p.x + p.z) * 1.4));
    return mix(vec3(0.55, 0.40, 0.24), vec3(0.62, 0.47, 0.29), plank);
  }
  if (mat == 3.0) return vec3(0.42, 0.20, 0.14); // Dach
  if (mat == 4.0) return vec3(0.30, 0.21, 0.12); // Stamm
  if (mat == 5.0) { // Blätter
    float noise = vnoise(p.xz * 4.5 + p.y * 3.0);
    return mix(vec3(0.13, 0.34, 0.10), vec3(0.20, 0.46, 0.14), noise);
  }
  if (mat == 6.0) return vec3(0.44, 0.46, 0.48); // Stein
  if (mat == 7.0) return vec3(0.34, 0.25, 0.14); // Fackelstab
  return vec3(1.0, 0.75, 0.35); // Fackelspitze (emissiv, s.u.)
}

// ------------------------------ Beleuchtung -------------------------------
const vec3 TORCH_POS = vec3(2.1, 1.02, 1.6);

vec3 lighting(vec3 albedo, vec3 p, vec3 n, float mat) {
  float df = uDayFactor;

  vec3 lightDir = uSunDir.y > -0.05 ? uSunDir : -uSunDir;
  float NdotL = max(dot(n, lightDir), 0.0);
  if (mat == 5.0) NdotL = NdotL * 0.5 + 0.45; // Blätter weicher

  float shadow = softShadow(p + n * 0.03, lightDir);
  float ao = ambientOcclusion(p, n);

  vec3 dayLight = sunColorLinear() * uSunStrength;
  vec3 nightLight = vec3(0.42, 0.52, 0.78) * uNightBrightness;
  vec3 direct = mix(nightLight, dayLight, df) * NdotL * shadow;

  vec3 daySky = vec3(0.52, 0.63, 0.82);
  vec3 nightSky = vec3(0.12, 0.16, 0.27) * (0.5 + uNightBrightness);
  vec3 ambient = mix(nightSky, daySky, df) * ao * uAmbientStrength + vec3(0.03);

  // Fackel-Punktlicht
  vec3 toTorch = TORCH_POS - p;
  float torchDist = length(toTorch);
  float atten = 1.0 / (1.0 + torchDist * torchDist * 0.55);
  float torchNdotL = clamp(dot(n, toTorch / max(torchDist, 0.001)), 0.0, 1.0);
  vec3 torch = uTorchColor * uTorchStrength * atten * torchNdotL * 1.6;

  vec3 color = albedo * (direct + ambient + torch);
  if (mat == 8.0) { // Fackelspitze glüht
    float flicker = 0.85 + 0.15 * sin(uTime * 9.0 + sin(uTime * 13.7));
    color += uTorchColor * uTorchStrength * 3.2 * flicker;
  }
  return color;
}

// ------------------------------ Wasser ------------------------------------
float waveHeightAt(vec2 p, float t) {
  float h = sin(p.x * 0.9 + t * 1.5) * 0.50
          + sin((p.x + p.y) * 0.6 + t * 1.1) * 0.35
          + sin(p.y * 1.3 - t * 0.9) * 0.40
          + sin((p.x - p.y) * 2.1 + t * 2.3) * 0.15;
  return h * 0.05 * uWaveHeight;
}

vec3 waterNormal(vec2 p) {
  if (uWaterWaves == 0) return vec3(0.0, 1.0, 0.0);
  float t = uTime * 1.6 * uWaveSpeed;
  float e = 0.06;
  float h0 = waveHeightAt(p, t);
  float hx = waveHeightAt(p + vec2(e, 0.0), t);
  float hz = waveHeightAt(p + vec2(0.0, e), t);
  return normalize(vec3(-(hx - h0) / e, 1.0, -(hz - h0) / e));
}

// ------------------------------ Nebel -------------------------------------
vec3 applyFog(vec3 color, float dist, vec3 rd) {
  if (uFog == 0) return color;
  vec3 fogCol = skyColor(normalize(vec3(rd.x, abs(rd.y) * 0.08 + 0.02, rd.z)));
  float start = FAR * uFogStart;
  float f = 1.0 - exp(-max(dist - start, 0.0) / FAR * 3.0 * uFogDensity);
  return mix(color, fogCol, clamp(f, 0.0, 1.0));
}

// ------------------------------ Haupt -------------------------------------
void main() {
  vec2 uv = (vUv * 2.0 - 1.0) * vec2(uRes.x / uRes.y, 1.0);

  vec3 forward = normalize(uCamTarget - uCamPos);
  vec3 right = normalize(cross(forward, vec3(0.0, 1.0, 0.0)));
  vec3 up = cross(right, forward);
  vec3 rd = normalize(forward * 1.65 + uv.x * right + uv.y * up);
  vec3 ro = uCamPos;

  vec2 hit = march(ro, rd);
  vec3 color;
  float dist = hit.x;

  if (hit.y < 0.5) {
    color = skyColor(rd);
    dist = FAR;
  } else {
    vec3 p = ro + rd * hit.x;
    vec3 n = sceneNormal(p);
    vec3 albedo = materialAlbedo(hit.y, p, n);
    color = lighting(albedo, p, n, hit.y);
    color = applyFog(color, hit.x, rd);
  }

  // Wasserfläche (analytisch, im Teich-Rechteck)
  if (rd.y < -0.001 || ro.y < WATER_LEVEL) {
    float tw = (WATER_LEVEL - ro.y) / rd.y;
    if (tw > 0.0 && tw < dist) {
      vec3 wp = ro + rd * tw;
      vec2 local = abs(wp.xz - POND_CENTER);
      if (local.x < POND_HALF.x && local.y < POND_HALF.y) {
        vec3 n = waterNormal(wp.xz);
        float df = uDayFactor;

        vec3 waterBase = uWaterCustom == 1 ? uWaterColor : vec3(0.05, 0.18, 0.35);
        float opacity = uWaterCustom == 1 ? uWaterOpacity : 0.75;

        // Durchsicht auf den Grund, getönt nach Tiefe
        float depth = clamp((dist - tw) * 0.8, 0.0, 1.0);
        vec3 transmitted = mix(color, waterBase * (0.25 + 0.55 * df), clamp(opacity + depth * 0.4, 0.0, 1.0));

        // Spiegelung des Himmels
        vec3 reflDir = reflect(rd, n);
        reflDir.y = abs(reflDir.y) * 0.9 + 0.05;
        vec3 reflection = skyColor(reflDir);

        float fresnel = pow(1.0 - clamp(dot(-rd, n), 0.0, 1.0), 3.0);
        vec3 waterCol = mix(transmitted, reflection, 0.18 + fresnel * 0.65);

        if (uWaterSpecular == 1) {
          vec3 lightDir = uSunDir.y > -0.05 ? uSunDir : -uSunDir;
          vec3 halfDir = normalize(lightDir - rd);
          float spec = pow(max(dot(n, halfDir), 0.0), 130.0);
          vec3 specColor = mix(vec3(0.42, 0.52, 0.78) * uNightBrightness, sunColorLinear() * uSunStrength, df);
          float shadow = softShadow(wp + vec3(0.0, 0.03, 0.0), lightDir);
          waterCol += specColor * spec * shadow * 2.2;
        }

        color = applyFog(waterCol, tw, rd);
      }
    }
  }

  fragColor = vec4(color, 1.0);
}`;

// ---------------------------------------------------------------------------
// Post-Processing (Pass 2) – gleiche Formeln wie final.fsh des Packs
// ---------------------------------------------------------------------------
const POST_FRAG = `#version 300 es
precision highp float;

in vec2 vUv;
out vec4 fragColor;

uniform sampler2D uScene;
uniform vec2 uRes;
uniform float uTime;

uniform int uTonemap;
uniform float uExposure;
uniform float uSaturation;
uniform float uContrast;
uniform float uBrightness;
uniform float uGamma;

uniform int uBloom;
uniform float uBloomStrength;
uniform float uBloomRadius;

uniform int uVignette;
uniform float uVignetteStrength;
uniform int uCA;
uniform float uCAStrength;
uniform int uGrain;
uniform float uGrainStrength;

float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }

float hash12(vec2 p) {
  vec3 p3 = fract(vec3(p.xyx) * 0.1031);
  p3 += dot(p3, p3.yzx + 33.33);
  return fract((p3.x + p3.y) * p3.z);
}

vec3 tonemapACES(vec3 x) {
  return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}
vec3 tonemapReinhard(vec3 x) { return x / (1.0 + luma(x)); }
vec3 uc2(vec3 x) {
  return ((x * (0.15 * x + 0.05) + 0.004) / (x * (0.15 * x + 0.5) + 0.06)) - 0.0667;
}
vec3 tonemapUncharted2(vec3 c) { return uc2(c * 2.0) / uc2(vec3(11.2)); }

vec3 sampleBloom(vec2 uv) {
  vec2 texel = uBloomRadius * 5.0 / uRes;
  vec3 acc = vec3(0.0);
  float wsum = 0.0;
  for (int x = -2; x <= 2; x++) {
    for (int y = -2; y <= 2; y++) {
      vec2 o = vec2(float(x), float(y));
      float w = exp(-dot(o, o) * 0.38);
      vec3 c = texture(uScene, uv + o * texel).rgb;
      float b = smoothstep(0.75, 1.35, luma(c));
      acc += c * b * w;
      wsum += w;
    }
  }
  return acc / wsum;
}

vec3 fetchColor(vec2 uv) {
  vec3 c = texture(uScene, uv).rgb;
  if (uBloom == 1) c += sampleBloom(uv) * uBloomStrength * 1.4;
  return c;
}

void main() {
  vec2 uv = vUv;
  vec3 color;

  if (uCA == 1) {
    vec2 shift = (uv - 0.5) * 0.006 * uCAStrength;
    color.r = fetchColor(uv + shift).r;
    color.g = fetchColor(uv).g;
    color.b = fetchColor(uv - shift).b;
  } else {
    color = fetchColor(uv);
  }

  color *= uExposure;

  if (uTonemap == 1) color = tonemapACES(color);
  else if (uTonemap == 2) color = tonemapReinhard(color);
  else if (uTonemap == 3) color = tonemapUncharted2(color);

  color = pow(clamp(color, 0.0, 1.0), vec3(1.0 / 2.2));

  color = (color - 0.5) * uContrast + 0.5 + uBrightness;
  color = mix(vec3(luma(color)), color, uSaturation);
  color = pow(clamp(color, 0.0, 1.0), vec3(1.0 / uGamma));

  if (uVignette == 1) {
    float d = distance(uv, vec2(0.5)) * 1.414;
    color *= 1.0 - uVignetteStrength * smoothstep(0.5, 1.05, d);
  }

  if (uGrain == 1) {
    float g = hash12(uv * uRes + fract(uTime) * 951.35);
    color += (g - 0.5) * 0.07 * uGrainStrength;
  }

  fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}`;

// ---------------------------------------------------------------------------
function compile(gl, type, source) {
  const shader = gl.createShader(type);
  gl.shaderSource(shader, source);
  gl.compileShader(shader);
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
    const log = gl.getShaderInfoLog(shader);
    gl.deleteShader(shader);
    throw new Error(`Shader-Fehler: ${log}`);
  }
  return shader;
}

function link(gl, vertSrc, fragSrc) {
  const program = gl.createProgram();
  gl.attachShader(program, compile(gl, gl.VERTEX_SHADER, vertSrc));
  gl.attachShader(program, compile(gl, gl.FRAGMENT_SHADER, fragSrc));
  gl.linkProgram(program);
  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
    throw new Error(`Link-Fehler: ${gl.getProgramInfoLog(program)}`);
  }
  return program;
}

const srgbToLinear = (c) => c.map((v) => Math.pow(v, 2.2));

export function createPreview(canvas) {
  let gl;
  try {
    gl = canvas.getContext('webgl2', { antialias: false, alpha: false });
  } catch { return null; }
  if (!gl) return null;

  let scene, post;
  try {
    scene = link(gl, VERT, SCENE_FRAG);
    post = link(gl, VERT, POST_FRAG);
  } catch (err) {
    console.error(err);
    return null;
  }

  const floatOk = Boolean(gl.getExtension('EXT_color_buffer_float'));

  const uniforms = (program) => {
    const map = {};
    const count = gl.getProgramParameter(program, gl.ACTIVE_UNIFORMS);
    for (let i = 0; i < count; i++) {
      const info = gl.getActiveUniform(program, i);
      map[info.name] = gl.getUniformLocation(program, info.name);
    }
    return map;
  };
  const sceneU = uniforms(scene);
  const postU = uniforms(post);

  const vao = gl.createVertexArray();

  // Framebuffer für HDR-Szene
  let fbo = null, fboTex = null, fboW = 0, fboH = 0;
  function ensureFbo(w, h) {
    if (fboW === w && fboH === h) return;
    if (fbo) { gl.deleteFramebuffer(fbo); gl.deleteTexture(fboTex); }
    fboTex = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, fboTex);
    gl.texImage2D(gl.TEXTURE_2D, 0, floatOk ? gl.RGBA16F : gl.RGBA8, w, h, 0, gl.RGBA, floatOk ? gl.HALF_FLOAT : gl.UNSIGNED_BYTE, null);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    fbo = gl.createFramebuffer();
    gl.bindFramebuffer(gl.FRAMEBUFFER, fbo);
    gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, fboTex, 0);
    gl.bindFramebuffer(gl.FRAMEBUFFER, null);
    fboW = w; fboH = h;
  }

  // ----------------------------------------------------------------- Zustand
  const state = {
    timeOfDay: 0.35,
    animateTime: false,
    autoRotate: true,
    yaw: 0.7,
    pitch: 0.34,
    dist: 12.5,
    settings: null,
    clock: 0,
    lastFrame: performance.now()
  };

  const api = {
    onTimeChanged: null,
    setSettings(s) { state.settings = s; },
    setTimeOfDay(v) { state.timeOfDay = ((v % 1) + 1) % 1; },
    setAnimateTime(v) { state.animateTime = v; },
    setAutoRotate(v) { state.autoRotate = v; }
  };

  // ------------------------------------------------------------ Interaktion
  let dragging = false, lastX = 0, lastY = 0;
  canvas.addEventListener('pointerdown', (e) => {
    dragging = true; lastX = e.clientX; lastY = e.clientY;
    canvas.setPointerCapture(e.pointerId);
  });
  canvas.addEventListener('pointermove', (e) => {
    if (!dragging) return;
    state.yaw -= (e.clientX - lastX) * 0.008;
    state.pitch = Math.min(1.25, Math.max(0.05, state.pitch + (e.clientY - lastY) * 0.006));
    lastX = e.clientX; lastY = e.clientY;
  });
  canvas.addEventListener('pointerup', () => { dragging = false; });
  canvas.addEventListener('wheel', (e) => {
    e.preventDefault();
    state.dist = Math.min(26, Math.max(5, state.dist * (e.deltaY > 0 ? 1.09 : 0.92)));
  }, { passive: false });

  // ---------------------------------------------------------------- Render
  function setUniformsFromSettings() {
    const s = state.settings;
    if (!s) return;

    // Sonnenbahn
    const angle = (state.timeOfDay - 0.25) * Math.PI * 2;
    const tilt = (s.lighting.sunPathRotation * Math.PI) / 180;
    const sunDir = [
      Math.cos(angle),
      Math.sin(angle) * Math.cos(tilt),
      Math.sin(angle) * Math.sin(tilt)
    ];
    const dayFactor = smoothstep(-0.04, 0.12, sunDir[1]);

    // Kamera
    const cy = Math.cos(state.yaw), sy = Math.sin(state.yaw);
    const cp = Math.cos(state.pitch), sp = Math.sin(state.pitch);
    const camPos = [sy * cp * state.dist, sp * state.dist + 0.9, cy * cp * state.dist];

    gl.uniform2f(sceneU.uRes, fboW, fboH);
    gl.uniform1f(sceneU.uTime, state.clock);
    gl.uniform3f(sceneU.uCamPos, camPos[0], camPos[1], camPos[2]);
    gl.uniform3f(sceneU.uCamTarget, 0, 0.9, 0);
    gl.uniform3f(sceneU.uSunDir, sunDir[0], sunDir[1], sunDir[2]);
    gl.uniform1f(sceneU.uDayFactor, dayFactor);

    const L = s.lighting;
    gl.uniform1f(sceneU.uSunStrength, L.sunlightStrength);
    gl.uniform1f(sceneU.uAmbientStrength, L.ambientStrength);
    gl.uniform1f(sceneU.uNightBrightness, L.nightBrightness);
    gl.uniform3fv(sceneU.uSunTint, srgbToLinear(hexToRgb01(s.sky.sunTint)));
    gl.uniform3fv(sceneU.uTorchColor, srgbToLinear(hexToRgb01(L.torchColor)));
    gl.uniform1f(sceneU.uTorchStrength, L.torchStrength);

    gl.uniform1i(sceneU.uShadowMode, !L.shadows ? 0 : L.softShadows ? 2 : 1);
    gl.uniform1f(sceneU.uShadowSoftK, 26 / Math.max(1, L.shadowSamples));

    const W = s.water;
    gl.uniform3fv(sceneU.uWaterColor, srgbToLinear(hexToRgb01(W.color)));
    gl.uniform1f(sceneU.uWaterOpacity, W.opacity);
    gl.uniform1i(sceneU.uWaterCustom, W.customColor ? 1 : 0);
    gl.uniform1i(sceneU.uWaterWaves, W.waves ? 1 : 0);
    gl.uniform1f(sceneU.uWaveHeight, W.waveHeight);
    gl.uniform1f(sceneU.uWaveSpeed, W.waveSpeed);
    gl.uniform1i(sceneU.uWaterSpecular, W.specular ? 1 : 0);

    gl.uniform3fv(sceneU.uSkyTintDay, srgbToLinear(hexToRgb01(s.sky.tintDay)));
    gl.uniform3fv(sceneU.uSkyTintNight, srgbToLinear(hexToRgb01(s.sky.tintNight)));
    gl.uniform1i(sceneU.uClouds, s.sky.clouds === 'off' ? 0 : 1);

    gl.uniform1i(sceneU.uFog, s.fog.enabled ? 1 : 0);
    gl.uniform1f(sceneU.uFogDensity, s.fog.density);
    gl.uniform1f(sceneU.uFogStart, s.fog.start);

    const M = s.motion;
    gl.uniform1i(sceneU.uWavingLeaves, M.wavingLeaves ? 1 : 0);
    gl.uniform1i(sceneU.uWavingPlants, M.wavingPlants ? 1 : 0);
    gl.uniform1f(sceneU.uWavingStrength, M.strength);
    gl.uniform1f(sceneU.uWavingSpeed, M.speed);
  }

  function setPostUniforms() {
    const s = state.settings;
    if (!s) return;
    const P = s.post;
    gl.uniform1i(postU.uScene, 0);
    gl.uniform2f(postU.uRes, fboW, fboH);
    gl.uniform1f(postU.uTime, state.clock);
    gl.uniform1i(postU.uTonemap, { none: 0, aces: 1, reinhard: 2, uncharted2: 3 }[P.tonemap] ?? 1);
    gl.uniform1f(postU.uExposure, P.exposure);
    gl.uniform1f(postU.uSaturation, P.saturation);
    gl.uniform1f(postU.uContrast, P.contrast);
    gl.uniform1f(postU.uBrightness, P.brightness);
    gl.uniform1f(postU.uGamma, P.gamma);
    gl.uniform1i(postU.uBloom, P.bloom ? 1 : 0);
    gl.uniform1f(postU.uBloomStrength, P.bloomStrength);
    gl.uniform1f(postU.uBloomRadius, P.bloomRadius);
    gl.uniform1i(postU.uVignette, P.vignette ? 1 : 0);
    gl.uniform1f(postU.uVignetteStrength, P.vignetteStrength);
    gl.uniform1i(postU.uCA, P.chromaticAberration ? 1 : 0);
    gl.uniform1f(postU.uCAStrength, P.caStrength);
    gl.uniform1i(postU.uGrain, P.filmGrain ? 1 : 0);
    gl.uniform1f(postU.uGrainStrength, P.grainStrength);
  }

  function smoothstep(a, b, x) {
    const t = Math.min(1, Math.max(0, (x - a) / (b - a)));
    return t * t * (3 - 2 * t);
  }

  let disposed = false;
  function frame(now) {
    if (disposed) return;
    const dt = Math.min(0.1, (now - state.lastFrame) / 1000);
    state.lastFrame = now;
    state.clock += dt;

    if (state.autoRotate && !dragging) state.yaw += dt * 0.07;
    if (state.animateTime) {
      state.timeOfDay = (state.timeOfDay + dt / 48) % 1;
      api.onTimeChanged?.(state.timeOfDay);
    }

    // Interne Auflösung begrenzen (Raymarching ist teuer)
    const rect = canvas.getBoundingClientRect();
    const scale = Math.min(1, 860 / Math.max(1, rect.width * devicePixelRatio));
    const w = Math.max(2, Math.round(rect.width * devicePixelRatio * scale));
    const h = Math.max(2, Math.round(rect.height * devicePixelRatio * scale));
    if (canvas.width !== w || canvas.height !== h) {
      canvas.width = w;
      canvas.height = h;
    }
    ensureFbo(w, h);

    gl.bindVertexArray(vao);

    gl.bindFramebuffer(gl.FRAMEBUFFER, fbo);
    gl.viewport(0, 0, w, h);
    gl.useProgram(scene);
    setUniformsFromSettings();
    gl.drawArrays(gl.TRIANGLES, 0, 3);

    gl.bindFramebuffer(gl.FRAMEBUFFER, null);
    gl.viewport(0, 0, w, h);
    gl.useProgram(post);
    gl.activeTexture(gl.TEXTURE0);
    gl.bindTexture(gl.TEXTURE_2D, fboTex);
    setPostUniforms();
    gl.drawArrays(gl.TRIANGLES, 0, 3);

    requestAnimationFrame(frame);
  }
  requestAnimationFrame(frame);

  api.dispose = () => { disposed = true; };
  return api;
}
