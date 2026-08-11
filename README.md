# Choicer Voicer für Fabric

Minispiel für **Minecraft Java 26.1.2**. Die Mod spielt
Choicer-Voicer-Hörbeispiele ab, nimmt die Nachahmung über
[Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) auf und lässt
eine fünfköpfige Jury abstimmen.

## Voraussetzungen

- Minecraft Java 26.1.2
- Fabric Loader 0.19.2 oder neuer
- Fabric API 0.155.2+26.1.2
- Simple Voice Chat 2.6.21+26.1.2 auf Server **und Clients**
- Java 25
- Freigeschalteter UDP-Port 24454 für Simple Voice Chat
- Für Dub-Videos: FFmpeg im System-PATH jedes teilnehmenden Clients

Für normale Voice-Packs reicht die Choicer-Voicer-Mod auf dem Server. Für
Dub-Packs muss dieselbe JAR zusätzlich bei allen Mitspielern im Client installiert
sein, weil Minecraft Videos nicht serverseitig anzeigen kann.

## Installation

1. `choicer-voicer-1.0.0.jar`, Fabric API und Simple Voice Chat in den
   `mods`-Ordner des Servers legen.
2. Fabric API und Simple Voice Chat auch bei allen Mitspielern installieren.
   Für Dub-Packs zusätzlich `choicer-voicer-1.0.0.jar` clientseitig installieren.
3. UDP-Port 24454 am Server und in der Firewall freigeben.
4. Server einmal starten. Die Mod erzeugt
   `config/choicer_voicer/config.json` und die Pack-Verzeichnisse.

## GameBanana-Packs importieren

Unterstützt werden Choicer-Voicer-**Voice-Packs** und **Judge-Packs**, wie sie
beispielsweise auf GameBanana angeboten werden. Der Server lädt nichts
automatisch aus dem Internet:

1. Das Pack beim Urheber herunterladen.
2. Die ZIP-Datei unverändert nach
   `config/choicer_voicer/imports/` kopieren.
3. Im Spiel `/choicervoicer import` ausführen.
4. Mit `/choicervoicer packs` die erkannten IDs anzeigen.

Bereits entpackte Strukturen mit `packs_voice/<Packname>` und
`packs_judges/<Packname>` können ebenfalls direkt unter `imports` liegen.
ZIP-Pfade, Dateianzahl sowie komprimierte und entpackte Größe werden geprüft.
Beschädigte oder unsichere Archive werden nicht installiert.

### Unterstützte Pack-Inhalte

- Voice: WAV, MP3 und OGG (maximal 60 Sekunden pro Clip)
- Gleichnamige TXT-Dateien als Untertitel
- PNG, JPG/JPEG und WEBP als Clipbild, `_icon` oder `_pack_filler_image`
- JSON-Metadaten; bekannte Namen werden übernommen, unbekannte Werte erhalten
- Judges: `judge1` bis `judge5`, `judgeX_voice`, `scoreblip1` bis
  `scoreblip5`, `success` und `judgeX_success`
- Dub: `dub_video.ogv`, `_backing_track` und Zeitangaben am Ende des
  Clipnamens, beispielsweise `01_Satz_44-048.ogg` für 44,048 Sekunden

Die Bilder eines Judge-Packs können ohne zusätzliche Client-Ressourcen nicht
als eigene Minecraft-Oberfläche angezeigt werden. Namen und Stimmen werden
verwendet, die Abstimmung erscheint im Chat. Studio-, Menü-, Host- und
Figuren-Packs gehören nicht zum Umfang dieser Version.

## Dub-Videos

Beim ersten Spiel überträgt der Server `dub_video.ogv` in begrenzten Blöcken
an die Clients und speichert es unter `config/choicer_voicer/videos`. Die Lobby
wartet auf alle Downloads. Das passende Segment erscheint als **Vollbild-HUD-Overlay**
bei allen Mitspielern beim Anhören und erneut während der Aufnahme. Nach den Runden
läuft das Video vom Anfang und die temporären Spieleraufnahmen werden an ihren
Zeitpositionen über Simple Voice Chat abgespielt.

Auf jedem Spieler-PC muss `ffmpeg` über die Kommandozeile erreichbar sein. Wenn der
Minecraft-Launcher FFmpeg nicht findet, erscheint eine Chat-Meldung. Typische Installation:

```text
Windows: winget install Gyan.FFmpeg
macOS:   brew install ffmpeg
Linux:   sudo apt install ffmpeg
```

## Befehle

Operator:

- `/choicervoicer import` – neue ZIP-Dateien sicher importieren
- `/choicervoicer reload` – entpackte Packs neu einlesen
- `/choicervoicer start <pack-id>` – zehnsekündige Lobby öffnen
- `/choicervoicer stop` – aktives Spiel beenden

Alle Spieler:

- `/choicervoicer packs` – verfügbare Packs anzeigen
- `/choicervoicer join` – einer offenen Lobby beitreten
- `/choicervoicer leave` – Spiel verlassen
- `/choicervoicer status` – aktuellen Zustand anzeigen

Der Host tritt beim Start automatisch bei. In jeder Runde hört der aktive
Spieler privat einen zufälligen Clip, bekommt einen Countdown und spricht
diesen über das konfigurierte Simple-Voice-Chat-Mikrofon nach.

## Bewertung und Datenschutz

Die Ähnlichkeit wird lokal auf dem Server aus zeitlichem Lautstärkeverlauf,
Dauer, Nulldurchgangsrate und spektralem Schwerpunkt berechnet. Das ist eine
eigene, reproduzierbare Annäherung; der proprietäre Algorithmus des
Originalspiels ist nicht verfügbar.

Mikrofonpakete werden nur während des sichtbaren Aufnahmefensters für den
aktiven Spieler dekodiert. Aufnahmen werden nicht auf die Festplatte
geschrieben. Bei Dub-Packs bleiben sie bis zur Ergebniswiedergabe ausschließlich
im Arbeitsspeicher und werden anschließend entfernt.

## Entwicklung

```bash
./gradlew test
./gradlew build
./gradlew runServer
```

Minecraft 26.1 benötigt Java 25. Das Gradle-Projekt kann über Foojay
automatisch eine passende Toolchain beziehen.