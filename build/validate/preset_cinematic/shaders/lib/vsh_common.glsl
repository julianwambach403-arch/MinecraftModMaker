// Vertex-Uniforms und Wind-/Wellen-Funktionen

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
