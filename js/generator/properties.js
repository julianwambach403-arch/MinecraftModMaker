// Erzeugt shaders.properties, block.properties, Sprachdateien und die
// README.txt im Shaderpack.

// Optionen für das In-Game-Menü (nur vorhandene Optionen aufnehmen!)
function optionLayout(s) {
  const lighting = [
    'SUNLIGHT_STRENGTH', 'AMBIENT_STRENGTH', 'NIGHT_BRIGHTNESS', '<empty>',
    'TORCH_STRENGTH', 'TORCH_COLOR_R', 'TORCH_COLOR_G', 'TORCH_COLOR_B'
  ];
  if (s.lighting.shadows) {
    lighting.push(
      '<empty>', 'shadowMapResolution', 'shadowDistance', 'SHADOW_SOFT',
      'SHADOW_SAMPLES', 'SHADOW_BIAS', 'COLORED_SHADOWS'
    );
  }

  const water = [
    'WATER_CUSTOM_COLOR', 'WATER_COLOR_R', 'WATER_COLOR_G', 'WATER_COLOR_B',
    'WATER_OPACITY', '<empty>',
    'WATER_WAVES', 'WATER_WAVE_HEIGHT', 'WATER_WAVE_SPEED', 'WATER_SPECULAR'
  ];

  const sky = [
    'SKY_TINT_DAY_R', 'SKY_TINT_DAY_G', 'SKY_TINT_DAY_B',
    'SKY_TINT_NIGHT_R', 'SKY_TINT_NIGHT_G', 'SKY_TINT_NIGHT_B',
    'SUN_TINT_R', 'SUN_TINT_G', 'SUN_TINT_B', '<empty>',
    'FOG_ENABLED', 'FOG_DENSITY', 'FOG_START'
  ];

  const motion = ['WAVING_PLANTS', 'WAVING_LEAVES', 'WAVING_STRENGTH', 'WAVING_SPEED'];

  const post = [
    'TONEMAP', 'EXPOSURE', 'SATURATION', 'CONTRAST', 'BRIGHTNESS', 'GAMMA_ADJUST', '<empty>'
  ];
  if (s.post.bloom) post.push('BLOOM_STRENGTH', 'BLOOM_RADIUS', 'BLOOM_THRESHOLD', '<empty>');
  post.push(
    'VIGNETTE', 'VIGNETTE_STRENGTH', 'CHROMATIC_ABERRATION', 'CA_STRENGTH',
    'FILM_GRAIN', 'GRAIN_STRENGTH'
  );

  return { lighting, water, sky, motion, post };
}

const SLIDER_OPTIONS = [
  'SUNLIGHT_STRENGTH', 'AMBIENT_STRENGTH', 'NIGHT_BRIGHTNESS', 'TORCH_STRENGTH',
  'TORCH_COLOR_R', 'TORCH_COLOR_G', 'TORCH_COLOR_B',
  'shadowDistance', 'SHADOW_BIAS', 'sunPathRotation',
  'WATER_COLOR_R', 'WATER_COLOR_G', 'WATER_COLOR_B', 'WATER_OPACITY',
  'WATER_WAVE_HEIGHT', 'WATER_WAVE_SPEED',
  'SKY_TINT_DAY_R', 'SKY_TINT_DAY_G', 'SKY_TINT_DAY_B',
  'SKY_TINT_NIGHT_R', 'SKY_TINT_NIGHT_G', 'SKY_TINT_NIGHT_B',
  'SUN_TINT_R', 'SUN_TINT_G', 'SUN_TINT_B',
  'FOG_DENSITY', 'FOG_START',
  'WAVING_STRENGTH', 'WAVING_SPEED',
  'EXPOSURE', 'SATURATION', 'CONTRAST', 'BRIGHTNESS', 'GAMMA_ADJUST',
  'BLOOM_STRENGTH', 'BLOOM_RADIUS', 'BLOOM_THRESHOLD',
  'VIGNETTE_STRENGTH', 'CA_STRENGTH', 'GRAIN_STRENGTH'
];

export function buildShadersProperties(s) {
  const layout = optionLayout(s);
  const clouds = s.sky.clouds === 'off' ? 'off' : s.sky.clouds === 'fast' ? 'fast' : 'fancy';

  // Nur Optionen aufführen, die im generierten Pack existieren
  const sliders = SLIDER_OPTIONS.filter((name) => {
    if (!s.lighting.shadows && ['shadowDistance', 'SHADOW_BIAS'].includes(name)) return false;
    if (!s.post.bloom && name.startsWith('BLOOM_')) return false;
    return true;
  });

  return `# ${s.meta.name}
# Erstellt mit ShaderCreator / created with ShaderCreator
${s.meta.author ? `# Autor / author: ${s.meta.author}` : ''}

clouds=${clouds}
vignette=false
underwaterOverlay=false
oldLighting=false
oldHandLight=false
sun=true
moon=true

sliders=${sliders.join(' ')}

screen=[LIGHTING_SCREEN] [WATER_SCREEN] [SKY_SCREEN] [MOTION_SCREEN] [POST_SCREEN] <empty> sunPathRotation
screen.LIGHTING_SCREEN=${layout.lighting.join(' ')}
screen.WATER_SCREEN=${layout.water.join(' ')}
screen.SKY_SCREEN=${layout.sky.join(' ')}
screen.MOTION_SCREEN=${layout.motion.join(' ')}
screen.POST_SCREEN=${layout.post.join(' ')}
`;
}

export function buildBlockProperties(s) {
  return `# Block-Zuordnung für Waving-Effekte (erstellt mit ShaderCreator)
# 10001 = Pflanzen (obere Vertices schwanken)
# 10002 = Blätter/Ranken (ganzer Block schwankt leicht)
# 10008 = Wasser

block.10001=${s.motion.plantBlocks.trim()}
block.10002=${s.motion.leafBlocks.trim()}
block.10008=minecraft:water minecraft:flowing_water
`;
}

const LANG = {
  en_us: {
    'screen.LIGHTING_SCREEN': 'Lighting & Shadows',
    'screen.WATER_SCREEN': 'Water',
    'screen.SKY_SCREEN': 'Sky & Fog',
    'screen.MOTION_SCREEN': 'Waving Effects',
    'screen.POST_SCREEN': 'Color & Effects',
    'option.SUNLIGHT_STRENGTH': 'Sunlight Strength',
    'option.AMBIENT_STRENGTH': 'Ambient Light',
    'option.NIGHT_BRIGHTNESS': 'Night Brightness',
    'option.TORCH_STRENGTH': 'Torch Light Strength',
    'option.TORCH_COLOR_R': 'Torch Color: Red',
    'option.TORCH_COLOR_G': 'Torch Color: Green',
    'option.TORCH_COLOR_B': 'Torch Color: Blue',
    'option.sunPathRotation': 'Sun Path Tilt',
    'option.shadowMapResolution': 'Shadow Resolution',
    'option.shadowDistance': 'Shadow Distance',
    'option.SHADOW_SOFT': 'Soft Shadows',
    'option.SHADOW_SAMPLES': 'Shadow Softness Samples',
    'option.SHADOW_BIAS': 'Shadow Bias',
    'option.SHADOW_BIAS.comment': 'Increase if you see shadow stripes (shadow acne).',
    'option.COLORED_SHADOWS': 'Colored Shadows',
    'option.COLORED_SHADOWS.comment': 'Stained glass etc. casts colored shadows.',
    'option.WATER_CUSTOM_COLOR': 'Custom Water Color',
    'option.WATER_COLOR_R': 'Water Color: Red',
    'option.WATER_COLOR_G': 'Water Color: Green',
    'option.WATER_COLOR_B': 'Water Color: Blue',
    'option.WATER_OPACITY': 'Water Opacity',
    'option.WATER_WAVES': 'Water Waves',
    'option.WATER_WAVE_HEIGHT': 'Wave Height',
    'option.WATER_WAVE_SPEED': 'Wave Speed',
    'option.WATER_SPECULAR': 'Sun Reflections',
    'option.SKY_TINT_DAY_R': 'Day Sky Tint: Red',
    'option.SKY_TINT_DAY_G': 'Day Sky Tint: Green',
    'option.SKY_TINT_DAY_B': 'Day Sky Tint: Blue',
    'option.SKY_TINT_NIGHT_R': 'Night Sky Tint: Red',
    'option.SKY_TINT_NIGHT_G': 'Night Sky Tint: Green',
    'option.SKY_TINT_NIGHT_B': 'Night Sky Tint: Blue',
    'option.SUN_TINT_R': 'Sun/Moon Tint: Red',
    'option.SUN_TINT_G': 'Sun/Moon Tint: Green',
    'option.SUN_TINT_B': 'Sun/Moon Tint: Blue',
    'option.FOG_ENABLED': 'Fog',
    'option.FOG_DENSITY': 'Fog Density',
    'option.FOG_START': 'Fog Start',
    'option.WAVING_PLANTS': 'Waving Plants',
    'option.WAVING_LEAVES': 'Waving Leaves',
    'option.WAVING_STRENGTH': 'Waving Strength',
    'option.WAVING_SPEED': 'Waving Speed',
    'option.TONEMAP': 'Tonemapping',
    'value.TONEMAP.0': 'Off',
    'value.TONEMAP.1': 'ACES (filmic)',
    'value.TONEMAP.2': 'Reinhard',
    'value.TONEMAP.3': 'Uncharted 2',
    'option.EXPOSURE': 'Exposure',
    'option.SATURATION': 'Saturation',
    'option.CONTRAST': 'Contrast',
    'option.BRIGHTNESS': 'Brightness',
    'option.GAMMA_ADJUST': 'Gamma',
    'option.BLOOM_STRENGTH': 'Bloom Strength',
    'option.BLOOM_RADIUS': 'Bloom Radius',
    'option.BLOOM_THRESHOLD': 'Bloom Threshold',
    'option.VIGNETTE': 'Vignette',
    'option.VIGNETTE_STRENGTH': 'Vignette Strength',
    'option.CHROMATIC_ABERRATION': 'Chromatic Aberration',
    'option.CA_STRENGTH': 'Aberration Strength',
    'option.FILM_GRAIN': 'Film Grain',
    'option.GRAIN_STRENGTH': 'Grain Strength'
  },
  de_de: {
    'screen.LIGHTING_SCREEN': 'Beleuchtung & Schatten',
    'screen.WATER_SCREEN': 'Wasser',
    'screen.SKY_SCREEN': 'Himmel & Nebel',
    'screen.MOTION_SCREEN': 'Bewegungseffekte',
    'screen.POST_SCREEN': 'Farbe & Effekte',
    'option.SUNLIGHT_STRENGTH': 'Sonnenlicht-Stärke',
    'option.AMBIENT_STRENGTH': 'Umgebungslicht',
    'option.NIGHT_BRIGHTNESS': 'Nachthelligkeit',
    'option.TORCH_STRENGTH': 'Fackellicht-Stärke',
    'option.TORCH_COLOR_R': 'Fackelfarbe: Rot',
    'option.TORCH_COLOR_G': 'Fackelfarbe: Grün',
    'option.TORCH_COLOR_B': 'Fackelfarbe: Blau',
    'option.sunPathRotation': 'Sonnenbahn-Neigung',
    'option.shadowMapResolution': 'Schatten-Auflösung',
    'option.shadowDistance': 'Schatten-Distanz',
    'option.SHADOW_SOFT': 'Weiche Schatten',
    'option.SHADOW_SAMPLES': 'Weichzeichnungs-Samples',
    'option.SHADOW_BIAS': 'Schatten-Bias',
    'option.SHADOW_BIAS.comment': 'Erhöhen, falls Streifenmuster (Shadow-Acne) sichtbar sind.',
    'option.COLORED_SHADOWS': 'Gefärbte Schatten',
    'option.COLORED_SHADOWS.comment': 'Buntglas usw. wirft gefärbte Schatten.',
    'option.WATER_CUSTOM_COLOR': 'Eigene Wasserfarbe',
    'option.WATER_COLOR_R': 'Wasserfarbe: Rot',
    'option.WATER_COLOR_G': 'Wasserfarbe: Grün',
    'option.WATER_COLOR_B': 'Wasserfarbe: Blau',
    'option.WATER_OPACITY': 'Wasser-Deckkraft',
    'option.WATER_WAVES': 'Wasserwellen',
    'option.WATER_WAVE_HEIGHT': 'Wellenhöhe',
    'option.WATER_WAVE_SPEED': 'Wellengeschwindigkeit',
    'option.WATER_SPECULAR': 'Sonnenreflexionen',
    'option.SKY_TINT_DAY_R': 'Himmel Tag: Rot',
    'option.SKY_TINT_DAY_G': 'Himmel Tag: Grün',
    'option.SKY_TINT_DAY_B': 'Himmel Tag: Blau',
    'option.SKY_TINT_NIGHT_R': 'Himmel Nacht: Rot',
    'option.SKY_TINT_NIGHT_G': 'Himmel Nacht: Grün',
    'option.SKY_TINT_NIGHT_B': 'Himmel Nacht: Blau',
    'option.SUN_TINT_R': 'Sonne/Mond: Rot',
    'option.SUN_TINT_G': 'Sonne/Mond: Grün',
    'option.SUN_TINT_B': 'Sonne/Mond: Blau',
    'option.FOG_ENABLED': 'Nebel',
    'option.FOG_DENSITY': 'Nebeldichte',
    'option.FOG_START': 'Nebelbeginn',
    'option.WAVING_PLANTS': 'Wiegende Pflanzen',
    'option.WAVING_LEAVES': 'Wiegende Blätter',
    'option.WAVING_STRENGTH': 'Wind-Stärke',
    'option.WAVING_SPEED': 'Wind-Geschwindigkeit',
    'option.TONEMAP': 'Tonemapping',
    'value.TONEMAP.0': 'Aus',
    'value.TONEMAP.1': 'ACES (filmisch)',
    'value.TONEMAP.2': 'Reinhard',
    'value.TONEMAP.3': 'Uncharted 2',
    'option.EXPOSURE': 'Belichtung',
    'option.SATURATION': 'Sättigung',
    'option.CONTRAST': 'Kontrast',
    'option.BRIGHTNESS': 'Helligkeit',
    'option.GAMMA_ADJUST': 'Gamma',
    'option.BLOOM_STRENGTH': 'Bloom-Stärke',
    'option.BLOOM_RADIUS': 'Bloom-Radius',
    'option.BLOOM_THRESHOLD': 'Bloom-Schwelle',
    'option.VIGNETTE': 'Vignette',
    'option.VIGNETTE_STRENGTH': 'Vignette-Stärke',
    'option.CHROMATIC_ABERRATION': 'Chromatische Aberration',
    'option.CA_STRENGTH': 'Aberrations-Stärke',
    'option.FILM_GRAIN': 'Filmkorn',
    'option.GRAIN_STRENGTH': 'Filmkorn-Stärke'
  }
};

export function buildLangFiles() {
  const files = {};
  for (const [locale, entries] of Object.entries(LANG)) {
    files[`shaders/lang/${locale}.lang`] = Object.entries(entries)
      .map(([k, v]) => `${k}=${v}`)
      .join('\n') + '\n';
  }
  return files;
}

export function buildPackReadme(s) {
  return `${s.meta.name}
${'='.repeat(Math.max(s.meta.name.length, 10))}

${s.meta.description || ''}
${s.meta.author ? `Autor / author: ${s.meta.author}` : ''}

Dieses Shaderpack wurde mit ShaderCreator erstellt.
This shaderpack was created with ShaderCreator.

INSTALLATION (Deutsch)
----------------------
1. Installiere OptiFine (optifine.net) oder Iris (irisshaders.dev).
2. Lege diese ZIP-Datei in den Ordner ".minecraft/shaderpacks".
   (Im Spiel: Optionen -> Grafikeinstellungen -> Shader -> Shader-Ordner)
3. Wähle das Pack im Shader-Menü aus.
4. Alle Einstellungen lassen sich im Spiel unter "Shader-Optionen" anpassen.

INSTALLATION (English)
----------------------
1. Install OptiFine (optifine.net) or Iris (irisshaders.dev).
2. Put this ZIP file into the ".minecraft/shaderpacks" folder.
   (In game: Options -> Video Settings -> Shaders -> Shaderpacks Folder)
3. Select the pack in the shaders menu.
4. All settings can be tweaked in game under "Shader Options".

Benötigt / requires: Minecraft 1.13+ (empfohlen 1.16+), OptiFine HD U G5+ oder Iris 1.2+.
`;
}
