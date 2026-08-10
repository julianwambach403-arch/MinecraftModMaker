// Zentrales Datenmodell: Standardwerte, Presets und Hilfsfunktionen.
// Alle Farben sind sRGB-Hexwerte; der Generator linearisiert im Shader.

export const DEFAULT_BLOCK_LISTS = {
  // Blöcke, die wie Pflanzen im Wind schwanken (nur obere Vertices)
  plants: [
    'minecraft:short_grass', 'minecraft:grass', 'minecraft:tall_grass',
    'minecraft:fern', 'minecraft:large_fern', 'minecraft:seagrass',
    'minecraft:tall_seagrass', 'minecraft:kelp', 'minecraft:kelp_plant',
    'minecraft:dandelion', 'minecraft:poppy', 'minecraft:blue_orchid',
    'minecraft:allium', 'minecraft:azure_bluet', 'minecraft:red_tulip',
    'minecraft:orange_tulip', 'minecraft:white_tulip', 'minecraft:pink_tulip',
    'minecraft:oxeye_daisy', 'minecraft:cornflower', 'minecraft:lily_of_the_valley',
    'minecraft:wither_rose', 'minecraft:torchflower', 'minecraft:sunflower',
    'minecraft:lilac', 'minecraft:rose_bush', 'minecraft:peony',
    'minecraft:wheat', 'minecraft:carrots', 'minecraft:potatoes',
    'minecraft:beetroots', 'minecraft:nether_wart', 'minecraft:sweet_berry_bush',
    'minecraft:crimson_roots', 'minecraft:warped_roots', 'minecraft:nether_sprouts',
    'minecraft:sugar_cane', 'minecraft:oak_sapling', 'minecraft:spruce_sapling',
    'minecraft:birch_sapling', 'minecraft:jungle_sapling', 'minecraft:acacia_sapling',
    'minecraft:dark_oak_sapling', 'minecraft:mangrove_propagule', 'minecraft:cherry_sapling'
  ].join(' '),
  // Blöcke, die als Ganzes leicht schwanken (Blätter, Ranken)
  leaves: [
    'minecraft:oak_leaves', 'minecraft:spruce_leaves', 'minecraft:birch_leaves',
    'minecraft:jungle_leaves', 'minecraft:acacia_leaves', 'minecraft:dark_oak_leaves',
    'minecraft:mangrove_leaves', 'minecraft:cherry_leaves', 'minecraft:azalea_leaves',
    'minecraft:flowering_azalea_leaves', 'minecraft:pale_oak_leaves',
    'minecraft:vine', 'minecraft:cave_vines', 'minecraft:cave_vines_plant',
    'minecraft:weeping_vines', 'minecraft:weeping_vines_plant',
    'minecraft:twisting_vines', 'minecraft:twisting_vines_plant'
  ].join(' '),
  water: ['minecraft:water', 'minecraft:flowing_water'].join(' ')
};

export function defaultSettings() {
  return {
    meta: {
      name: 'Mein Shader',
      author: '',
      description: 'Erstellt mit ShaderCreator'
    },
    lighting: {
      shadows: true,
      shadowResolution: 2048, // 1024 | 2048 | 4096 | 8192
      shadowDistance: 128, // 64..256
      softShadows: true,
      shadowSamples: 2, // 1..4 (PCF-Radius)
      coloredShadows: true,
      shadowBias: 1.0, // 0.5..3.0 (Multiplikator gegen Shadow-Acne)
      sunPathRotation: -30, // -60..60 Grad
      sunlightStrength: 1.0, // 0..2
      ambientStrength: 1.0, // 0..2
      nightBrightness: 0.3, // 0..1
      torchColor: '#ffb763',
      torchStrength: 1.0 // 0..2
    },
    water: {
      customColor: true,
      color: '#2f66c4',
      opacity: 0.75, // 0.1..1
      waves: true,
      waveHeight: 1.0, // 0..2
      waveSpeed: 1.0, // 0..2
      specular: true
    },
    sky: {
      tintDay: '#ffffff',
      tintNight: '#ffffff',
      sunTint: '#ffffff',
      clouds: 'fancy' // fancy | fast | off
    },
    fog: {
      enabled: true,
      density: 1.0, // 0..3
      start: 0.4 // 0..0.9 (Anteil der Sichtweite)
    },
    motion: {
      wavingPlants: true,
      wavingLeaves: true,
      strength: 1.0, // 0..2
      speed: 1.0, // 0..2
      plantBlocks: DEFAULT_BLOCK_LISTS.plants,
      leafBlocks: DEFAULT_BLOCK_LISTS.leaves
    },
    post: {
      tonemap: 'aces', // none | aces | reinhard | uncharted2
      exposure: 1.0, // 0.5..2
      saturation: 1.05, // 0..2
      contrast: 1.02, // 0.5..1.5
      brightness: 0.0, // -0.25..0.25
      gamma: 1.0, // 0.5..2
      bloom: true,
      bloomStrength: 0.35, // 0..1
      bloomRadius: 1.0, // 0.5..2
      vignette: true,
      vignetteStrength: 0.3, // 0..1
      chromaticAberration: false,
      caStrength: 0.5, // 0..2
      filmGrain: false,
      grainStrength: 0.3 // 0..1
    }
  };
}

// Presets: Teilobjekte, die über die Standardwerte gelegt werden.
export const PRESETS = {
  balanced: {},
  cinematic: {
    lighting: { shadowResolution: 4096, shadowSamples: 3, sunlightStrength: 1.1 },
    fog: { density: 1.3, start: 0.3 },
    post: {
      tonemap: 'aces', exposure: 1.05, saturation: 1.12, contrast: 1.08,
      bloom: true, bloomStrength: 0.55, bloomRadius: 1.3,
      vignette: true, vignetteStrength: 0.5,
      chromaticAberration: true, caStrength: 0.4,
      filmGrain: true, grainStrength: 0.2
    }
  },
  vibrant: {
    lighting: { sunlightStrength: 1.15, torchColor: '#ffa94d', torchStrength: 1.15 },
    water: { color: '#1fa9d6', opacity: 0.65 },
    sky: { tintDay: '#eaf4ff' },
    post: { saturation: 1.3, contrast: 1.06, bloom: true, bloomStrength: 0.45, vignette: false }
  },
  cozy: {
    lighting: {
      sunPathRotation: -40, sunlightStrength: 0.95, nightBrightness: 0.35,
      torchColor: '#ffc078', torchStrength: 1.3
    },
    sky: { tintDay: '#fff3e0', tintNight: '#cdd6ff', sunTint: '#ffe0b3' },
    fog: { density: 1.5, start: 0.25 },
    post: {
      tonemap: 'reinhard', saturation: 0.95, contrast: 1.0,
      bloom: true, bloomStrength: 0.5, bloomRadius: 1.5,
      vignette: true, vignetteStrength: 0.45,
      filmGrain: true, grainStrength: 0.35
    }
  },
  performance: {
    lighting: { shadowResolution: 1024, softShadows: false, coloredShadows: false, shadowDistance: 96 },
    water: { waves: true, waveHeight: 0.8 },
    post: {
      bloom: false, vignette: false, chromaticAberration: false, filmGrain: false,
      tonemap: 'aces'
    }
  }
};

export function deepMerge(base, patch) {
  const out = Array.isArray(base) ? [...base] : { ...base };
  for (const key of Object.keys(patch)) {
    const value = patch[key];
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      out[key] = deepMerge(base[key] ?? {}, value);
    } else {
      out[key] = value;
    }
  }
  return out;
}

export function applyPreset(presetId) {
  return deepMerge(defaultSettings(), PRESETS[presetId] ?? {});
}

// Hex "#rrggbb" -> [r,g,b] in 0..1 (sRGB)
export function hexToRgb01(hex) {
  const m = /^#?([0-9a-f]{6})$/i.exec(hex.trim());
  const v = m ? parseInt(m[1], 16) : 0xffffff;
  return [(v >> 16 & 255) / 255, (v >> 8 & 255) / 255, (v & 255) / 255];
}
