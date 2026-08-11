# Choicer Voicer für Fabric

Serverseitiges Minispiel für **Minecraft Java 26.1.2**. Die Mod spielt
Choicer-Voicer-Hörbeispiele ab, nimmt die Nachahmung über
[Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) auf und lässt
eine fünfköpfige Jury abstimmen.

Dub-Videos und das Endergebnis erscheinen auf einer **vom Server gehosteten
Webseite** im Browser – nicht als Minecraft-Client-Overlay.

## Voraussetzungen

- Minecraft Java 26.1.2
- Fabric Loader 0.19.2 oder neuer
- Fabric API 0.155.2+26.1.2
- Simple Voice Chat 2.6.21+26.1.2 auf Server **und Clients**
- Java 25
- Freigeschalteter UDP-Port 24454 für Simple Voice Chat
- Für Dub-Packs: freigeschalteter HTTP-Port (Standard `8765`) und idealerweise
  `ffmpeg` auf dem Server (wandelt `dub_video.ogv` nach WebM um)

Die Choicer-Voicer-Mod selbst muss **nur auf dem Fabric-Server** installiert
werden. Clients benötigen Fabric, Fabric API und Simple Voice Chat.

## Installation

1. `choicer-voicer-1.0.0.jar`, Fabric API und Simple Voice Chat in den
   `mods`-Ordner des Servers legen.
2. Fabric API und Simple Voice Chat auch bei allen Mitspielern installieren.
3. UDP-Port 24454 und für Dub-Packs TCP-Port 8765 freigeben.
4. Server einmal starten. Die Mod erzeugt
   `config/choicer_voicer/config.json` und die Pack-Verzeichnisse.
5. Optional in der Config `webPublicBaseUrl` auf die öffentliche Server-URL setzen,
   z. B. `http://dein-server.de:8765`.

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

## Dub-Videos im Browser

Beim Start eines Dub-Packs bereitet der Server das Video für den Browser vor und
öffnet ein kleines Webportal:

- Startseite: `http://<Server>:8765/`
- Live während des Spiels: `/watch`
- Endergebnis mit Takes: `/result`

Die Chat-Nachricht enthält die Links. Mit `/choicervoicer web` kannst du sie
jederzeit erneut anzeigen. Auf `/watch` springt das Video zur aktuellen
Spielstelle. Auf `/result` lässt sich das fertige Dub mit den aufgenommenen
Takes abspielen.

Relevante Config-Werte:

```json
{
  "webEnabled": true,
  "webPort": 8765,
  "webPublicBaseUrl": "http://dein-server.de:8765"
}
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
- `/choicervoicer web` – Webportal-Links anzeigen

Der Host tritt beim Start automatisch bei. In jeder Runde hört der aktive
Spieler privat einen zufälligen Clip, bekommt einen Countdown und spricht
diesen über das konfigurierte Simple-Voice-Chat-Mikrofon nach.

## Bewertung und Datenschutz

Die Ähnlichkeit wird lokal auf dem Server aus zeitlichem Lautstärkeverlauf,
Dauer, Nulldurchgangsrate und spektralem Schwerpunkt berechnet. Das ist eine
eigene, reproduzierbare Annäherung; der proprietäre Algorithmus des
Originalspiels ist nicht verfügbar.

Mikrofonpakete werden nur während des sichtbaren Aufnahmefensters für den
aktiven Spieler dekodiert. Aufnahmen werden nicht dauerhaft als Rohdateien im
Spielordner belassen; für Dub-Ergebnisse werden temporäre WAV-Takes nur für die
Web-Ergebnisseite unter `config/choicer_voicer/web/` bereitgestellt.

## Entwicklung

```bash
./gradlew test
./gradlew build
./gradlew runServer
```

Minecraft 26.1 benötigt Java 25. Das Gradle-Projekt kann über Foojay
automatisch eine passende Toolchain beziehen.
