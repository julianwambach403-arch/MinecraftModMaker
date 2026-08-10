// Baut aus den Einstellungen die komplette Dateiliste des Shaderpacks.

import { buildShaderFiles } from './glsl.js';
import {
  buildShadersProperties,
  buildBlockProperties,
  buildLangFiles,
  buildPackReadme
} from './properties.js';

/**
 * @param {object} settings Einstellungen (siehe js/state.js)
 * @returns {Array<{path: string, data: string}>} Dateien des Shaderpacks
 */
export function generatePack(settings) {
  const files = {
    'README.txt': buildPackReadme(settings),
    'shaders/shaders.properties': buildShadersProperties(settings),
    'shaders/block.properties': buildBlockProperties(settings),
    ...buildLangFiles(),
    ...buildShaderFiles(settings)
  };

  return Object.entries(files)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([path, data]) => ({ path, data }));
}

/** Dateiname für das exportierte Pack ("Mein Shader" -> "Mein_Shader.zip") */
export function packFileName(settings) {
  const base = (settings.meta.name || 'Shaderpack')
    .trim()
    .replace(/[^\p{L}\p{N} _.-]/gu, '')
    .replace(/\s+/g, '_') || 'Shaderpack';
  return `${base}.zip`;
}
