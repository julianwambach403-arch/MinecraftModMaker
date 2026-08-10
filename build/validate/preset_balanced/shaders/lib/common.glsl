// Gemeinsame Hilfsfunktionen

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
