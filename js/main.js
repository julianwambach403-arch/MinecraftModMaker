// ShaderCreator – Hauptlogik der Oberfläche

import { defaultSettings, PRESETS, applyPreset, deepMerge } from './state.js';
import { generatePack, packFileName } from './generator/index.js';
import { createZip } from './zip.js';
import { t, setLanguage, getLanguage } from './i18n.js';
import { createPreview } from './preview.js';

const STORAGE_KEY = 'shadercreator.project';

// ---------------------------------------------------------------------------
// Zustand
// ---------------------------------------------------------------------------
let settings = loadStoredSettings();
let activePanel = 'overview';
let activeFile = 'shaders/lib/settings.glsl';
let preview = null;

function loadStoredSettings() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return deepMerge(defaultSettings(), JSON.parse(raw));
  } catch { /* defekte Daten ignorieren */ }
  return defaultSettings();
}

let saveTimer = null;
function persist() {
  clearTimeout(saveTimer);
  saveTimer = setTimeout(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
  }, 250);
}

function getPath(path) {
  return path.split('.').reduce((o, k) => o?.[k], settings);
}

function setPath(path, value) {
  const keys = path.split('.');
  const last = keys.pop();
  const target = keys.reduce((o, k) => o[k], settings);
  target[last] = value;
  onSettingsChanged();
}

let filesTimer = null;
function onSettingsChanged() {
  persist();
  preview?.setSettings(settings);
  updateEnabledStates();
  updateExportSummary();
  if (activePanel === 'files') {
    clearTimeout(filesTimer);
    filesTimer = setTimeout(renderFilesPanelContent, 200);
  }
}

// ---------------------------------------------------------------------------
// Schema der Einstellungs-Panels
// ---------------------------------------------------------------------------
const SCHEMA = [
  {
    id: 'lighting',
    icon: '☀️',
    sections: [
      {
        titleKey: 'lighting.section.sun',
        rows: [
          { key: 'lighting.sunlightStrength', type: 'slider', i18n: 'lighting.sunlightStrength', min: 0, max: 2, step: 0.05 },
          { key: 'lighting.ambientStrength', type: 'slider', i18n: 'lighting.ambientStrength', min: 0, max: 2, step: 0.05 },
          { key: 'lighting.nightBrightness', type: 'slider', i18n: 'lighting.nightBrightness', min: 0, max: 1, step: 0.05 },
          { key: 'lighting.sunPathRotation', type: 'slider', i18n: 'lighting.sunPathRotation', min: -60, max: 60, step: 5, decimals: 0, hint: 'lighting.sunPathRotation.hint' }
        ]
      },
      {
        titleKey: 'lighting.section.shadows',
        rows: [
          { key: 'lighting.shadows', type: 'toggle', i18n: 'lighting.shadows', hint: 'lighting.shadows.hint' },
          { key: 'lighting.shadowResolution', type: 'select', i18n: 'lighting.shadowResolution', options: [1024, 2048, 4096, 8192].map(v => ({ value: v, label: `${v} px` })), numeric: true, enabledIf: 'lighting.shadows' },
          { key: 'lighting.shadowDistance', type: 'slider', i18n: 'lighting.shadowDistance', min: 64, max: 256, step: 16, decimals: 0, enabledIf: 'lighting.shadows' },
          { key: 'lighting.softShadows', type: 'toggle', i18n: 'lighting.softShadows', enabledIf: 'lighting.shadows' },
          { key: 'lighting.shadowSamples', type: 'slider', i18n: 'lighting.shadowSamples', min: 1, max: 4, step: 1, decimals: 0, enabledIf: 'lighting.softShadows' },
          { key: 'lighting.coloredShadows', type: 'toggle', i18n: 'lighting.coloredShadows', enabledIf: 'lighting.shadows' },
          { key: 'lighting.shadowBias', type: 'slider', i18n: 'lighting.shadowBias', min: 0.5, max: 3, step: 0.25, hint: 'lighting.shadowBias.hint', enabledIf: 'lighting.shadows' }
        ]
      },
      {
        titleKey: 'lighting.section.torch',
        rows: [
          { key: 'lighting.torchColor', type: 'color', i18n: 'lighting.torchColor' },
          { key: 'lighting.torchStrength', type: 'slider', i18n: 'lighting.torchStrength', min: 0, max: 2, step: 0.05 }
        ]
      }
    ]
  },
  {
    id: 'water',
    icon: '🌊',
    sections: [
      {
        titleKey: 'water.section.color',
        rows: [
          { key: 'water.customColor', type: 'toggle', i18n: 'water.customColor' },
          { key: 'water.color', type: 'color', i18n: 'water.color', enabledIf: 'water.customColor' },
          { key: 'water.opacity', type: 'slider', i18n: 'water.opacity', min: 0.1, max: 1, step: 0.05, enabledIf: 'water.customColor' }
        ]
      },
      {
        titleKey: 'water.section.waves',
        rows: [
          { key: 'water.waves', type: 'toggle', i18n: 'water.waves' },
          { key: 'water.waveHeight', type: 'slider', i18n: 'water.waveHeight', min: 0, max: 2, step: 0.1, enabledIf: 'water.waves' },
          { key: 'water.waveSpeed', type: 'slider', i18n: 'water.waveSpeed', min: 0, max: 2, step: 0.1, enabledIf: 'water.waves' },
          { key: 'water.specular', type: 'toggle', i18n: 'water.specular' }
        ]
      }
    ]
  },
  {
    id: 'sky',
    icon: '⛅',
    sections: [
      {
        titleKey: 'sky.section.tint',
        rows: [
          { key: 'sky.tintDay', type: 'color', i18n: 'sky.tintDay' },
          { key: 'sky.tintNight', type: 'color', i18n: 'sky.tintNight' },
          { key: 'sky.sunTint', type: 'color', i18n: 'sky.sunTint' },
          { key: 'sky.clouds', type: 'select', i18n: 'sky.clouds', options: [
            { value: 'fancy', labelKey: 'sky.clouds.fancy' },
            { value: 'fast', labelKey: 'sky.clouds.fast' },
            { value: 'off', labelKey: 'sky.clouds.off' }
          ] }
        ]
      },
      {
        titleKey: 'sky.section.fog',
        rows: [
          { key: 'fog.enabled', type: 'toggle', i18n: 'fog.enabled' },
          { key: 'fog.density', type: 'slider', i18n: 'fog.density', min: 0, max: 3, step: 0.1, enabledIf: 'fog.enabled' },
          { key: 'fog.start', type: 'slider', i18n: 'fog.start', min: 0, max: 0.9, step: 0.05, enabledIf: 'fog.enabled' }
        ]
      }
    ]
  },
  {
    id: 'motion',
    icon: '🍃',
    sections: [
      {
        titleKey: 'motion.section.wind',
        rows: [
          { key: 'motion.wavingPlants', type: 'toggle', i18n: 'motion.wavingPlants' },
          { key: 'motion.wavingLeaves', type: 'toggle', i18n: 'motion.wavingLeaves' },
          { key: 'motion.strength', type: 'slider', i18n: 'motion.strength', min: 0, max: 2, step: 0.1 },
          { key: 'motion.speed', type: 'slider', i18n: 'motion.speed', min: 0, max: 2, step: 0.1 }
        ]
      },
      {
        titleKey: 'motion.section.blocks',
        hint: 'motion.blocks.hint',
        rows: [
          { key: 'motion.plantBlocks', type: 'textarea', i18n: 'motion.plantBlocks' },
          { key: 'motion.leafBlocks', type: 'textarea', i18n: 'motion.leafBlocks' }
        ]
      }
    ]
  },
  {
    id: 'post',
    icon: '🎨',
    sections: [
      {
        titleKey: 'post.section.tone',
        rows: [
          { key: 'post.tonemap', type: 'select', i18n: 'post.tonemap', options: [
            { value: 'none', labelKey: 'post.tonemap.none' },
            { value: 'aces', labelKey: 'post.tonemap.aces' },
            { value: 'reinhard', labelKey: 'post.tonemap.reinhard' },
            { value: 'uncharted2', labelKey: 'post.tonemap.uncharted2' }
          ] },
          { key: 'post.exposure', type: 'slider', i18n: 'post.exposure', min: 0.5, max: 2, step: 0.05 },
          { key: 'post.saturation', type: 'slider', i18n: 'post.saturation', min: 0, max: 2, step: 0.05 },
          { key: 'post.contrast', type: 'slider', i18n: 'post.contrast', min: 0.5, max: 1.5, step: 0.02 },
          { key: 'post.brightness', type: 'slider', i18n: 'post.brightness', min: -0.25, max: 0.25, step: 0.01 },
          { key: 'post.gamma', type: 'slider', i18n: 'post.gamma', min: 0.5, max: 2, step: 0.05 }
        ]
      },
      {
        titleKey: 'post.section.bloom',
        rows: [
          { key: 'post.bloom', type: 'toggle', i18n: 'post.bloom', hint: 'post.bloom.hint' },
          { key: 'post.bloomStrength', type: 'slider', i18n: 'post.bloomStrength', min: 0, max: 1, step: 0.05, enabledIf: 'post.bloom' },
          { key: 'post.bloomRadius', type: 'slider', i18n: 'post.bloomRadius', min: 0.5, max: 2, step: 0.25, enabledIf: 'post.bloom' }
        ]
      },
      {
        titleKey: 'post.section.camera',
        rows: [
          { key: 'post.vignette', type: 'toggle', i18n: 'post.vignette' },
          { key: 'post.vignetteStrength', type: 'slider', i18n: 'post.vignetteStrength', min: 0, max: 1, step: 0.05, enabledIf: 'post.vignette' },
          { key: 'post.chromaticAberration', type: 'toggle', i18n: 'post.chromaticAberration' },
          { key: 'post.caStrength', type: 'slider', i18n: 'post.caStrength', min: 0, max: 2, step: 0.1, enabledIf: 'post.chromaticAberration' },
          { key: 'post.filmGrain', type: 'toggle', i18n: 'post.filmGrain' },
          { key: 'post.grainStrength', type: 'slider', i18n: 'post.grainStrength', min: 0, max: 1, step: 0.05, enabledIf: 'post.filmGrain' }
        ]
      }
    ]
  }
];

const NAV_ITEMS = [
  { id: 'overview', icon: '🧭' },
  ...SCHEMA.map(p => ({ id: p.id, icon: p.icon })),
  { id: 'files', icon: '📄' },
  { id: 'export', icon: '📦' }
];

const PRESET_SWATCHES = {
  balanced: ['#7ec8ff', '#2f66c4', '#51c76d'],
  cinematic: ['#ffb45e', '#20365e', '#101018'],
  vibrant: ['#41d6ff', '#1fa9d6', '#7bff8e'],
  cozy: ['#ffd9a0', '#c98f4e', '#7a5a3a'],
  performance: ['#c8d2e0', '#8b98ab', '#4d5a6e']
};

// ---------------------------------------------------------------------------
// Rendering-Helfer
// ---------------------------------------------------------------------------
function el(tag, attrs = {}, ...children) {
  const node = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (k === 'class') node.className = v;
    else if (k.startsWith('on')) node.addEventListener(k.slice(2), v);
    else if (v !== false && v != null) node.setAttribute(k, v === true ? '' : v);
  }
  for (const child of children.flat()) {
    if (child == null) continue;
    node.append(child.nodeType ? child : document.createTextNode(child));
  }
  return node;
}

function fmtValue(value, decimals = 2) {
  return Number(value).toFixed(decimals);
}

function buildControl(row) {
  const value = getPath(row.key);
  const widget = el('div', { class: 'control-widget' });

  switch (row.type) {
    case 'toggle': {
      const input = el('input', {
        type: 'checkbox',
        onchange: (e) => setPath(row.key, e.target.checked)
      });
      input.checked = Boolean(value);
      widget.append(el('label', { class: 'toggle' }, input, el('span', { class: 'track' })));
      break;
    }
    case 'slider': {
      const decimals = row.decimals ?? 2;
      const valueLabel = el('span', { class: 'range-value' }, fmtValue(value, decimals));
      const input = el('input', {
        type: 'range', min: row.min, max: row.max, step: row.step, value,
        oninput: (e) => {
          const v = Number(e.target.value);
          valueLabel.textContent = fmtValue(v, decimals);
          setPath(row.key, v);
        }
      });
      widget.append(input, valueLabel);
      break;
    }
    case 'color': {
      const hexLabel = el('span', { class: 'color-hex' }, String(value).toLowerCase());
      const input = el('input', {
        type: 'color', value,
        oninput: (e) => {
          hexLabel.textContent = e.target.value.toLowerCase();
          setPath(row.key, e.target.value);
        }
      });
      widget.append(input, hexLabel);
      break;
    }
    case 'select': {
      const select = el('select', {
        onchange: (e) => setPath(row.key, row.numeric ? Number(e.target.value) : e.target.value)
      });
      for (const option of row.options) {
        const label = option.labelKey ? t(option.labelKey) : option.label;
        const opt = el('option', { value: option.value }, label);
        if (String(option.value) === String(value)) opt.selected = true;
        select.append(opt);
      }
      widget.append(select);
      break;
    }
    case 'textarea': {
      const input = el('textarea', {
        rows: 4,
        onchange: (e) => setPath(row.key, e.target.value)
      });
      input.value = value;
      widget.append(input);
      break;
    }
  }
  return widget;
}

function buildRow(row) {
  const label = el('label', {}, t(row.i18n));
  if (row.hint) label.append(el('span', { class: 'sub-hint' }, t(row.hint)));
  const rowEl = el('div', { class: 'control-row', 'data-key': row.key }, label, buildControl(row));
  if (row.enabledIf) rowEl.dataset.enabledIf = row.enabledIf;
  return rowEl;
}

function updateEnabledStates() {
  for (const rowEl of document.querySelectorAll('[data-enabled-if]')) {
    const enabled = Boolean(getPath(rowEl.dataset.enabledIf));
    rowEl.classList.toggle('disabled', !enabled);
  }
}

// ---------------------------------------------------------------------------
// Panels
// ---------------------------------------------------------------------------
function renderNav() {
  const nav = document.getElementById('nav');
  nav.replaceChildren(
    ...NAV_ITEMS.map(item =>
      el('button', {
        class: item.id === activePanel ? 'active' : '',
        onclick: () => switchPanel(item.id)
      }, el('span', { class: 'icon' }, item.icon), t(`nav.${item.id}`))
    )
  );
}

function switchPanel(id) {
  activePanel = id;
  renderNav();
  for (const panel of document.querySelectorAll('.panel')) {
    panel.classList.toggle('active', panel.dataset.panel === id);
  }
  if (id === 'files') renderFilesPanelContent();
  if (id === 'export') updateExportSummary();
}

function renderOverviewPanel() {
  const metaCard = el('div', { class: 'card' },
    el('h3', {}, t('overview.project')),
    el('div', { class: 'control-row' },
      el('label', {}, t('overview.name')),
      el('div', { class: 'control-widget' },
        el('input', {
          type: 'text', value: settings.meta.name, maxlength: 60,
          onchange: (e) => setPath('meta.name', e.target.value.trim() || 'Mein Shader')
        }))
    ),
    el('div', { class: 'control-row' },
      el('label', {}, t('overview.author')),
      el('div', { class: 'control-widget' },
        el('input', {
          type: 'text', value: settings.meta.author, maxlength: 60,
          onchange: (e) => setPath('meta.author', e.target.value.trim())
        }))
    ),
    el('div', { class: 'control-row' },
      el('label', {}, t('overview.description')),
      el('div', { class: 'control-widget' },
        el('input', {
          type: 'text', value: settings.meta.description, maxlength: 140,
          onchange: (e) => setPath('meta.description', e.target.value.trim())
        }))
    )
  );

  const presetCards = Object.keys(PRESETS).map(id =>
    el('div', { class: 'preset-card' },
      el('div', { class: 'swatches' },
        ...(PRESET_SWATCHES[id] ?? []).map(c => el('span', { style: `background:${c}` }))),
      el('h4', {}, t(`preset.${id}`)),
      el('p', {}, t(`preset.${id}.desc`)),
      el('button', { class: 'btn small', onclick: () => {
        const meta = { ...settings.meta };
        settings = applyPreset(id);
        settings.meta = meta;
        onSettingsChanged();
        renderAllPanels();
        toast(`${t('overview.applied')}: ${t(`preset.${id}`)}`);
      } }, t('overview.apply'))
    )
  );

  const presetCard = el('div', { class: 'card' },
    el('h3', {}, t('overview.presets')),
    el('p', { class: 'hint' }, t('overview.presets.hint')),
    el('div', { class: 'preset-grid' }, presetCards)
  );

  const howtoCard = el('div', { class: 'card' },
    el('h3', {}, t('overview.howto')),
    el('p', { style: 'margin:0;color:var(--text-dim);font-size:13.5px' }, t('overview.howto.text'))
  );

  return el('div', { class: 'panel', 'data-panel': 'overview' },
    el('h2', {}, t('nav.overview')), metaCard, presetCard, howtoCard);
}

function renderSchemaPanel(panelDef) {
  const cards = panelDef.sections.map(section => {
    const card = el('div', { class: 'card' }, el('h3', {}, t(section.titleKey)));
    if (section.hint) card.append(el('p', { class: 'hint' }, t(section.hint)));
    card.append(...section.rows.map(buildRow));
    return card;
  });
  return el('div', { class: 'panel', 'data-panel': panelDef.id },
    el('h2', {}, t(`nav.${panelDef.id}`)), ...cards);
}

function renderFilesPanel() {
  return el('div', { class: 'panel', 'data-panel': 'files' },
    el('h2', {}, t('files.title')),
    el('p', { class: 'hint' }, t('files.hint')),
    el('div', { class: 'files-layout' },
      el('div', { class: 'file-list', id: 'file-list' }),
      el('div', { class: 'file-view' }, el('pre', { id: 'file-view' }))
    )
  );
}

function renderFilesPanelContent() {
  const files = generatePack(settings);
  const list = document.getElementById('file-list');
  const view = document.getElementById('file-view');
  if (!list || !view) return;
  if (!files.some(f => f.path === activeFile)) activeFile = files[0].path;

  list.replaceChildren(...files.map(f =>
    el('button', {
      class: f.path === activeFile ? 'active' : '',
      onclick: () => {
        activeFile = f.path;
        renderFilesPanelContent();
      }
    }, f.path)
  ));
  view.textContent = files.find(f => f.path === activeFile)?.data ?? '';
}

function renderExportPanel() {
  return el('div', { class: 'panel', 'data-panel': 'export' },
    el('h2', {}, t('export.title')),
    el('div', { class: 'card' },
      el('h3', {}, t('export.title')),
      el('p', { class: 'hint' }, t('export.hint')),
      el('div', { class: 'action-row' },
        el('button', { class: 'btn primary', onclick: exportPack }, '⬇ ' + t('export.download'))
      ),
      el('div', { class: 'export-summary', id: 'export-summary' })
    ),
    el('div', { class: 'card' },
      el('h3', {}, t('export.install')),
      el('ol', { class: 'install-steps' },
        el('li', {}, t('export.install.1')),
        el('li', {}, t('export.install.2')),
        el('li', {}, t('export.install.3'))
      )
    ),
    el('div', { class: 'card' },
      el('h3', {}, t('export.project')),
      el('p', { class: 'hint' }, t('export.project.hint')),
      el('div', { class: 'action-row' },
        el('button', { class: 'btn', onclick: saveProject }, '💾 ' + t('export.save')),
        el('button', { class: 'btn', onclick: () => document.getElementById('project-file-input').click() }, '📂 ' + t('export.load')),
        el('button', { class: 'btn danger', onclick: resetAll }, '♻ ' + t('export.reset'))
      )
    )
  );
}

function updateExportSummary() {
  const summary = document.getElementById('export-summary');
  if (!summary) return;
  const files = generatePack(settings);
  const bytes = files.reduce((s, f) => s + f.data.length, 0);
  summary.textContent = `${packFileName(settings)} · ${files.length} ${getLanguage() === 'de' ? 'Dateien' : 'files'} · ${(bytes / 1024).toFixed(1)} KiB`;
}

function renderAllPanels() {
  const main = document.getElementById('panels');
  main.replaceChildren(
    renderOverviewPanel(),
    ...SCHEMA.map(renderSchemaPanel),
    renderFilesPanel(),
    renderExportPanel()
  );
  switchPanel(activePanel);
  updateEnabledStates();
  updateExportSummary();
}

// ---------------------------------------------------------------------------
// Aktionen
// ---------------------------------------------------------------------------
function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const a = el('a', { href: url, download: filename });
  document.body.append(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 3000);
}

function exportPack() {
  const files = generatePack(settings);
  const zip = createZip(files);
  const name = packFileName(settings);
  downloadBlob(new Blob([zip], { type: 'application/zip' }), name);
  toast(`${t('toast.exported')} ${name}`);
}

function saveProject() {
  const data = JSON.stringify({ app: 'ShaderCreator', version: 1, settings }, null, 2);
  const name = packFileName(settings).replace(/\.zip$/, '') + '.shadercreator.json';
  downloadBlob(new Blob([data], { type: 'application/json' }), name);
  toast(t('toast.saved'));
}

function loadProjectFile(file) {
  const reader = new FileReader();
  reader.onload = () => {
    try {
      const parsed = JSON.parse(reader.result);
      settings = deepMerge(defaultSettings(), parsed.settings ?? parsed);
      onSettingsChanged();
      renderAllPanels();
      toast(t('toast.loaded'));
    } catch {
      toast(t('toast.loadError'));
    }
  };
  reader.readAsText(file);
}

function resetAll() {
  if (!confirm(t('export.reset.confirm'))) return;
  settings = defaultSettings();
  onSettingsChanged();
  renderAllPanels();
  toast(t('toast.reset'));
}

function toast(message) {
  const container = document.getElementById('toasts');
  const node = el('div', { class: 'toast' }, message);
  container.append(node);
  setTimeout(() => node.remove(), 3900);
}

// ---------------------------------------------------------------------------
// Vorschau-Steuerung
// ---------------------------------------------------------------------------
function updateTimeLabel(timeValue) {
  const label = document.getElementById('time-label');
  const totalMinutes = Math.round(timeValue * 24 * 60);
  const hh = String(Math.floor(totalMinutes / 60) % 24).padStart(2, '0');
  const mm = String(totalMinutes % 60).padStart(2, '0');
  const names = [t('time.night'), t('time.morning'), t('time.noon'), t('time.evening')];
  const phase = names[Math.round(timeValue * 4) % 4];
  label.textContent = `${hh}:${mm} · ${phase}`;
}

function initPreviewUI() {
  const canvas = document.getElementById('preview-canvas');
  preview = createPreview(canvas);
  if (!preview) {
    const frame = canvas.parentElement;
    frame.append(el('div', { class: 'fallback' }, t('preview.unsupported')));
    return;
  }
  preview.setSettings(settings);

  const slider = document.getElementById('time-slider');
  const animate = document.getElementById('animate-time');
  const rotate = document.getElementById('auto-rotate');

  preview.setTimeOfDay(Number(slider.value));
  updateTimeLabel(Number(slider.value));

  slider.addEventListener('input', () => {
    animate.checked = false;
    preview.setAnimateTime(false);
    preview.setTimeOfDay(Number(slider.value));
    updateTimeLabel(Number(slider.value));
  });

  animate.addEventListener('change', () => preview.setAnimateTime(animate.checked));
  rotate.addEventListener('change', () => preview.setAutoRotate(rotate.checked));

  preview.onTimeChanged = (timeValue) => {
    slider.value = String(timeValue);
    updateTimeLabel(timeValue);
  };
}

// ---------------------------------------------------------------------------
// Start
// ---------------------------------------------------------------------------
function applyStaticI18n() {
  for (const node of document.querySelectorAll('[data-i18n]')) {
    node.textContent = t(node.dataset.i18n);
  }
  document.title = `${t('app.title')} – ${t('app.subtitle')}`;
}

function init() {
  setLanguage(getLanguage());

  const langSelect = document.getElementById('language-select');
  langSelect.value = getLanguage();
  langSelect.addEventListener('change', () => {
    setLanguage(langSelect.value);
    applyStaticI18n();
    renderNav();
    renderAllPanels();
    updateTimeLabel(Number(document.getElementById('time-slider').value));
  });

  document.getElementById('export-button-header').addEventListener('click', exportPack);
  document.getElementById('project-file-input').addEventListener('change', (e) => {
    if (e.target.files[0]) loadProjectFile(e.target.files[0]);
    e.target.value = '';
  });

  applyStaticI18n();
  renderNav();
  renderAllPanels();
  initPreviewUI();
}

init();
