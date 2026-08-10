// Validiert die generierten Shaderpacks:
//   1. Jeder GLSL-Shader (alle Varianten/Presets) wird mit glslangValidator kompiliert.
//   2. Options-Menü (shaders.properties) referenziert nur existierende Optionen.
//   3. Jeder Standardwert einer Option ist in der erlaubten Werteliste enthalten.
//   4. Das erzeugte ZIP besteht "unzip -t".
//
//   Aufruf: npm test   (bzw. node tools/validate.mjs)

import { spawnSync } from 'node:child_process';
import { mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

import { generatePack } from '../js/generator/index.js';
import { defaultSettings, PRESETS, applyPreset, deepMerge } from '../js/state.js';
import { createZip } from '../js/zip.js';

const ROOT = fileURLToPath(new URL('..', import.meta.url));
const OUT = join(ROOT, 'build', 'validate');

// ---------------------------------------------------------------------------
// Zu prüfende Varianten
// ---------------------------------------------------------------------------
const variants = {
  default: defaultSettings(),
  minimal: deepMerge(defaultSettings(), {
    lighting: { shadows: false },
    water: { customColor: false, waves: false, specular: false },
    fog: { enabled: false },
    motion: { wavingPlants: false, wavingLeaves: false },
    post: {
      tonemap: 'none', bloom: false, vignette: false,
      chromaticAberration: false, filmGrain: false
    }
  }),
  maximal: deepMerge(defaultSettings(), {
    lighting: { shadows: true, softShadows: true, coloredShadows: true, shadowResolution: 8192, shadowSamples: 4 },
    water: { customColor: true, waves: true, specular: true },
    fog: { enabled: true },
    motion: { wavingPlants: true, wavingLeaves: true },
    post: {
      tonemap: 'uncharted2', bloom: true, vignette: true,
      chromaticAberration: true, filmGrain: true
    }
  }),
  hardShadowsNoColor: deepMerge(defaultSettings(), {
    lighting: { softShadows: false, coloredShadows: false }
  })
};
for (const id of Object.keys(PRESETS)) {
  variants[`preset_${id}`] = applyPreset(id);
}

// ---------------------------------------------------------------------------
// #include auflösen (OptiFine-Pfade beginnen mit / relativ zum shaders-Ordner)
// ---------------------------------------------------------------------------
function resolveIncludes(fileMap, path, stack = []) {
  const source = fileMap.get(path);
  if (source == null) throw new Error(`Include nicht gefunden: ${path} (über ${stack.join(' -> ')})`);
  return source.replace(/^[ \t]*#include\s+"([^"]+)"\s*$/gm, (_, inc) => {
    const target = inc.startsWith('/') ? `shaders${inc}` : join(dirname(path), inc);
    return resolveIncludes(fileMap, target, [...stack, path]);
  });
}

// ---------------------------------------------------------------------------
// glslang ausführen
// ---------------------------------------------------------------------------
function compileGlsl(stage, source, label, failures) {
  const tmp = join(OUT, 'tmp_shader.glsl');
  writeFileSync(tmp, source);
  const res = spawnSync('glslangValidator', ['-S', stage, tmp], { encoding: 'utf8' });
  const output = (res.stdout || '') + (res.stderr || '');
  if (res.status !== 0) {
    failures.push(`FEHLER ${label}\n${output.trim()}\n`);
  } else if (/WARNING/i.test(output)) {
    // Warnungen nur anzeigen, nicht als Fehler werten
    console.log(`  Warnung in ${label}:\n${output.trim()}`);
  }
}

// ---------------------------------------------------------------------------
// Optionen aus GLSL extrahieren und shaders.properties dagegen prüfen
// ---------------------------------------------------------------------------
function lintOptions(fileMap, variant, failures) {
  const optionValues = new Map(); // name -> {default, allowed|null}
  const sourceFiles = [...fileMap.entries()].filter(([p]) => /\.(fsh|vsh|glsl)$/.test(p));

  for (const [, src] of sourceFiles) {
    for (const line of src.split('\n')) {
      let m = line.match(/^\s*(\/\/)?#define\s+(\w+)(?:\s+([-\d.]+))?\s*(?:\/\/\s*\[([^\]]+)\])?\s*$/);
      if (m) {
        const [, , name, value, allowed] = m;
        optionValues.set(name, { default: value ?? null, allowed: allowed?.trim().split(/\s+/) ?? null });
        continue;
      }
      m = line.match(/^\s*const\s+(?:int|float)\s+(\w+)\s*=\s*([-\d.]+)\s*;\s*(?:\/\/\s*\[([^\]]+)\])?/);
      if (m) {
        const [, name, value, allowed] = m;
        optionValues.set(name, { default: value, allowed: allowed?.trim().split(/\s+/) ?? null });
      }
    }
  }

  // Standardwert muss in der erlaubten Liste liegen
  for (const [name, info] of optionValues) {
    if (info.allowed && info.default != null) {
      const ok = info.allowed.some((v) => Number(v) === Number(info.default));
      if (!ok) {
        failures.push(`FEHLER [${variant}] Option ${name}: Standardwert ${info.default} fehlt in Werteliste [${info.allowed.join(' ')}]`);
      }
    }
  }

  // shaders.properties: screen-/slider-Einträge müssen existieren
  const props = fileMap.get('shaders/shaders.properties');
  const screens = new Set();
  for (const line of props.split('\n')) {
    const m = line.match(/^screen\.?(\w+)?=(.*)$/) || line.match(/^(sliders)=(.*)$/);
    if (!m) continue;
    for (const entry of m[2].trim().split(/\s+/)) {
      if (!entry || entry === '<empty>') continue;
      const sub = entry.match(/^\[(\w+)\]$/);
      if (sub) {
        screens.add(sub[1]);
        continue;
      }
      if (!optionValues.has(entry)) {
        failures.push(`FEHLER [${variant}] shaders.properties referenziert unbekannte Option: ${entry}`);
      }
    }
  }
  for (const screen of screens) {
    if (!props.includes(`screen.${screen}=`)) {
      failures.push(`FEHLER [${variant}] Unter-Menü [${screen}] ist nicht definiert`);
    }
  }
}

// ---------------------------------------------------------------------------
// Hauptablauf
// ---------------------------------------------------------------------------
rmSync(OUT, { recursive: true, force: true });
mkdirSync(OUT, { recursive: true });

const failures = [];
let compiled = 0;

for (const [variant, settings] of Object.entries(variants)) {
  const files = generatePack(settings);
  const fileMap = new Map(files.map((f) => [f.path, f.data]));

  // Pack auch auf die Platte schreiben (zur manuellen Inspektion)
  for (const { path, data } of files) {
    const target = join(OUT, variant, path);
    mkdirSync(dirname(target), { recursive: true });
    writeFileSync(target, data);
  }

  for (const [path] of fileMap) {
    const stage = path.endsWith('.vsh') ? 'vert' : path.endsWith('.fsh') ? 'frag' : null;
    if (!stage) continue;
    const resolved = resolveIncludes(fileMap, path);
    compileGlsl(stage, resolved, `[${variant}] ${path}`, failures);
    compiled++;
  }

  lintOptions(fileMap, variant, failures);
}

// ZIP-Test
const defaultFiles = generatePack(variants.default);
const zipBytes = createZip(defaultFiles);
const zipPath = join(OUT, 'test-pack.zip');
writeFileSync(zipPath, zipBytes);
const unzip = spawnSync('unzip', ['-t', zipPath], { encoding: 'utf8' });
if (unzip.status !== 0) {
  failures.push(`FEHLER ZIP-Integritätstest fehlgeschlagen:\n${unzip.stdout}${unzip.stderr}`);
} else {
  console.log(`ZIP-Test OK (${defaultFiles.length} Dateien, ${zipBytes.length} Bytes)`);
}

console.log(`\n${compiled} Shader kompiliert, ${Object.keys(variants).length} Varianten geprüft.`);
if (failures.length > 0) {
  console.error(`\n${failures.length} Fehler:\n`);
  for (const f of failures) console.error(f);
  process.exit(1);
}
console.log('Alle Prüfungen bestanden. / All checks passed.');
