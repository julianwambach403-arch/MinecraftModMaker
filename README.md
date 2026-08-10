# ShaderCreator ✨

**Shaderpack-Studio für Minecraft – wie MCreator, aber für Shader.**
*Visual shaderpack studio for Minecraft – like MCreator, but for shaders. [English below.](#english)*

ShaderCreator ist eine Web-App, mit der du **ohne Programmierkenntnisse** komplette
Minecraft-Shaderpacks für **OptiFine** und **Iris** zusammenstellst: Regler bewegen,
Farben wählen, live in einer 3D-Vorschau ansehen – und als fertige ZIP exportieren.
Die App erzeugt dabei echten GLSL-Shadercode (kein Baukasten-Fake).

## Schnellstart

Voraussetzung: [Node.js](https://nodejs.org) **oder** Python 3 (nur zum Ausliefern der Dateien).

```bash
# Variante 1 (Node.js)
npm start            # startet http://localhost:8123

# Variante 2 (ohne Node)
./start.sh           # Linux/macOS
start.bat            # Windows (Doppelklick)
```

Dann `http://localhost:8123` im Browser öffnen (Chrome, Firefox oder Edge).

## Funktionen

| Kategorie | Einstellungen |
|---|---|
| **Beleuchtung & Schatten** | Echtes Shadow-Mapping (Auflösung, Distanz, weiche/PCF- und gefärbte Schatten), Sonnen-/Umgebungslicht, Nachthelligkeit, Sonnenbahn-Neigung, Fackellicht-Farbe & -Stärke |
| **Wasser** | Eigene Wasserfarbe, Deckkraft, animierte Wellen (Höhe/Tempo), Sonnenreflexionen |
| **Himmel & Nebel** | Himmels-Tönung (Tag/Nacht), Sonnen-/Mond-Tönung, Wolken-Modus, Entfernungs- und Unterwassernebel |
| **Bewegung** | Wiegende Pflanzen und Blätter (Stärke/Tempo), anpassbare Blocklisten |
| **Farbe & Effekte** | Tonemapping (ACES, Reinhard, Uncharted 2), Belichtung, Sättigung, Kontrast, Helligkeit, Gamma, Bloom, Vignette, chromatische Aberration, Filmkorn |

Dazu:

- **Live-3D-Vorschau** (WebGL2): prozedurale Minecraft-artige Szene mit Tag-/Nacht-Zyklus,
  die dieselben Licht- und Effektformeln wie das exportierte Pack verwendet (Näherung).
- **5 Presets**: Ausgewogen, Cinematic, Lebendig, Gemütlich, Performance.
- **Datei-Vorschau**: der generierte GLSL-Code ist jederzeit einsehbar – ideal zum Lernen.
- **Projekt speichern/laden** (.json) + automatisches Speichern im Browser.
- **Zweisprachig**: Deutsch und Englisch.
- **In-Game-Optionen**: Das exportierte Pack enthält ein komplettes Shader-Optionsmenü
  (inkl. deutscher und englischer Übersetzung) – fast alle Werte lassen sich später im
  Spiel weiter anpassen.

## Das exportierte Shaderpack

Der Export erzeugt eine ZIP mit echtem, dokumentiertem GLSL (Version 120, maximale
Kompatibilität) im OptiFine-/Iris-Format:

```
Mein_Shader.zip
├── README.txt                    Installationsanleitung (DE/EN)
└── shaders/
    ├── shaders.properties        Optionsmenü, Wolken-Modus, ...
    ├── block.properties          Blocklisten für Waving-Effekte
    ├── lang/                     Übersetzungen der Optionen (DE/EN)
    ├── lib/                      settings/common/lighting/vsh_common
    ├── shadow.vsh/.fsh           Schattenkarte (verzerrte Shadow-Map)
    ├── gbuffers_terrain|water|entities|hand|clouds|weather|sky*…
    ├── composite*.vsh/.fsh       Bloom-Extraktion + Weichzeichnung
    └── final.vsh/.fsh            Tonemapping, Farbkorrektur, Kamera-Effekte
```

**Installation im Spiel:**

1. [OptiFine](https://optifine.net) oder [Iris](https://irisshaders.dev) installieren.
2. Exportierte ZIP in den Ordner `.minecraft/shaderpacks` legen.
3. Im Spiel unter *Optionen → Grafikeinstellungen → Shader* auswählen.

Unterstützt: Minecraft **1.13+** (empfohlen 1.16+), OptiFine HD U G5+ oder Iris 1.2+.

## Entwicklung & Tests

Keine Abhängigkeiten, reines HTML/CSS/JS (ES-Module).

```bash
npm test    # generiert alle Preset-Varianten und kompiliert jeden GLSL-Shader
            # mit glslangValidator (sudo apt install glslang-tools), prüft das
            # Optionsmenü auf Konsistenz und testet die ZIP-Integrität
```

Projektstruktur:

```
index.html, css/, js/main.js     Oberfläche (schema-basierte Panels, i18n)
js/preview.js                    WebGL2-Live-Vorschau (Raymarching-Szene)
js/generator/                    Herzstück: erzeugt GLSL + Properties + Lang
js/zip.js                        Abhängigkeitsfreier ZIP-Writer
tools/validate.mjs               Test-/Validierungswerkzeug (npm test)
tools/serve.mjs                  Mini-Dev-Server (npm start)
```

---

<a name="english"></a>

## English

ShaderCreator is a web app that lets you build complete Minecraft shaderpacks for
**OptiFine** and **Iris** **without any coding**: move sliders, pick colors, watch a
live 3D preview, export a ready-to-use ZIP. It generates real GLSL shader code.

**Quick start:** `npm start` (or `./start.sh` / `start.bat`), then open
`http://localhost:8123`.

**Features:** real shadow mapping (soft/colored shadows), custom water color & waves,
sky tints, fog, waving plants/leaves, tonemapping (ACES/Reinhard/Uncharted 2), bloom,
vignette, chromatic aberration, film grain – plus 5 presets, a live WebGL preview,
a generated-code viewer, project save/load and a full in-game options menu (DE/EN)
inside the exported pack.

**Install the exported pack:** drop the ZIP into `.minecraft/shaderpacks` and select it
under *Options → Video Settings → Shaders*. Supports Minecraft 1.13+ (1.16+ recommended),
OptiFine HD U G5+ or Iris 1.2+.

**Testing:** `npm test` regenerates every preset variant, compiles all GLSL with
`glslangValidator`, lints the in-game options menu and verifies ZIP integrity.
