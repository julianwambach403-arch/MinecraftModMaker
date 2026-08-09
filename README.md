# ModMaker — Mods im Spiel bauen (Minecraft 26.2, Fabric)

ModMaker ist eine Fabric-Mod für **Minecraft Java 26.2 („Chaos Cubed")**, mit der du eigene
Items, Blöcke, Rezepte und JavaScript-Events **live im Spiel** erstellst und sofort benutzt —
wie MCreator, aber direkt in Minecraft, ohne Neustart und ohne externes Programm.

*ModMaker is a Fabric mod for Minecraft Java 26.2 that lets you create custom items, blocks,
recipes and JavaScript events live in-game — like MCreator, but inside Minecraft. GUI is
available in English and German.*

## Features

- **Items erstellen**: Name, Textur, Tooltip, Stapelgröße, Seltenheit, Glitzern, Essen
  (Nahrung/Sättigung), Werkzeuge (Spitzhacke/Axt/Schaufel/Hacke/Schwert mit Abbautempo,
  Schaden und Haltbarkeit) — sofort benutzbar
- **Blöcke erstellen**: Textur, Härte, Lichtlevel, Geräusche, Drops, Werkzeugpflicht —
  sofort platzierbar
- **Rezepte bauen**: visueller 3x3-Crafting-Editor (geformt und formlos) — sofort craftbar
- **Live-Scripting** mit JavaScript (Rhino): Events wie Rechtsklick, Block abbauen, Tick,
  Chat, Spieler-Beitritt; Hot-Reload per Knopfdruck, Fehler crashen nie das Spiel
- **Dateien importieren**: PNG-Texturen, `.js`-Skripte, `.json`-Elemente und komplette
  `.mmpack`-Pakete über einen In-Game-Dateibrowser (Desktop, Downloads, ...)
- **Teilen**: ein Klick exportiert deine ganze Werkstatt als `.mmpack`-Datei

## Installation

1. **Minecraft Java 26.2** mit dem [Fabric Loader](https://fabricmc.net/use/) (0.19.3 oder neuer) installieren
2. [Fabric API](https://modrinth.com/mod/fabric-api) (`0.156.0+26.2` oder neuer) in den `mods/`-Ordner legen
3. Die ModMaker-Jar (`build/libs/modmaker-1.0.0.jar`) in den `mods/`-Ordner legen
4. Spiel starten — fertig! (Java 25 wird von Minecraft 26.2 ohnehin vorausgesetzt)

## Selbst bauen

```bash
./gradlew build      # benötigt JDK 25
```

Die fertige Mod liegt danach unter `build/libs/modmaker-1.0.0.jar`
(die Rhino-Script-Engine ist bereits in der Jar gebündelt).

## Benutzung

- Taste **K** (umbelegbar) oder `/modmakergui` öffnet die **ModMaker-Werkstatt**
- Elemente anklicken zum Bearbeiten, unten neue Items/Blöcke/Rezepte anlegen
- **Import...** öffnet den Dateibrowser (PNG/JS/JSON/MMPACK)
- **Exportieren** schreibt ein `.mmpack` nach `<Spielordner>/modmaker/exports/`
- Eigene Items findest du im Kreativ-Inventar im Tab **ModMaker**

### Befehle

| Befehl | Wirkung |
|---|---|
| `/modmaker give <id> [anzahl]` | Gibt dir ein ModMaker-Element |
| `/modmaker list` | Listet alle Elemente |
| `/modmaker reload` | Lädt Definitionen, Skripte und Rezepte neu (auch nach externem Bearbeiten der Dateien) |
| `/modmakergui` | Öffnet die Werkstatt (Client) |

### Scripting (JavaScript)

Skripte liegen in `<Spielordner>/modmaker/scripts/*.js` und lassen sich im Spiel im
**Skript-Editor** schreiben und per **Speichern + Neu laden** sofort aktivieren.

```javascript
// Rechtsklick mit dem Item "zauberstab" (ID deines Items):
mm.onUse("zauberstab", function(player, x, y, z) {
    player.tell("Zap!");
    player.playSound("minecraft:entity.lightning_bolt.thunder");
    mm.runCommand("summon lightning_bolt " + x + " " + y + " " + z);
});

// Wenn dein Block "geheimkiste" abgebaut wird:
mm.onBlockBreak("geheimkiste", function(player, x, y, z) {
    player.give("minecraft:diamond", 3);
});

mm.onTick(200, function() {          // alle 10 Sekunden
    mm.broadcast("Denkt an eure Hausaufgaben!");
});

mm.onChat(function(player, message) {
    if (message == "heilung") player.heal(20);
});
```

**`mm`-API**: `onUse(id, fn)`, `onBlockBreak(id, fn)`, `onBlockUse(id, fn)`,
`onTick(ticks, fn)`, `onChat(fn)`, `onJoin(fn)`, `broadcast(text)`, `runCommand(cmd)`,
`log(text)`, `random(min, max)`, `randomInt(min, max)` — bei Events kann statt der ID auch
`"*"` (alle) stehen.

**`player`-API**: `name()`, `tell(text)`, `give(idOderVanilla, anzahl)`, `x()`, `y()`, `z()`,
`health()`, `heal(n)`, `teleport(x, y, z)`, `isSneaking()`, `playSound(soundId)`,
`runCommand(cmd)`, `setBlock(x, y, z, id)`, `getBlock(x, y, z)`

### Dateien und Ordner

```
<Spielordner>/modmaker/
├── items/      *.json   Item-Definitionen
├── blocks/     *.json   Block-Definitionen
├── recipes/    *.json   Rezept-Definitionen
├── scripts/    *.js     JavaScript-Dateien
├── textures/   *.png    importierte Texturen (16x16 empfohlen)
├── exports/    *.mmpack exportierte Pakete
└── bindings.json        interne Slot-Zuordnung (nicht löschen)
```

Alle Dateien sind normale JSON/JS/PNG-Dateien — du kannst sie auch außerhalb des Spiels
bearbeiten und danach `/modmaker reload` ausführen. Eine `.mmpack`-Datei ist ein ZIP mit
genau diesen Ordnern plus `manifest.json` — ideal zum Teilen mit Freunden (die einfach
über **Import...** dein Paket laden).

## Wie es funktioniert (Technik)

Minecraft friert Item-/Block-Registrierungen beim Spielstart ein. ModMaker registriert
deshalb beim Start einen Pool von **128 Platzhalter-Items und 64 Platzhalter-Blöcken**
(`modmaker:item_0` …). Deine Definitionen werden zur Laufzeit an freie Slots gebunden;
Namen, Werte und Verhalten werden bei jedem Zugriff live aus der Definition gelesen,
Eigenschaften wie Essen/Werkzeug werden als Data-Components auf die Item-Stacks gestempelt
(und bei Änderungen automatisch aktualisiert). Aussehen kommt aus einem automatisch
generierten Ressourcenpaket (`resourcepacks/ModMakerRuntime`), Rezepte aus einem
generierten Datenpaket in der Welt — beides wird bei Änderungen programmatisch neu geladen.

## Grenzen

- Maximal **128 Items und 64 Blöcke** gleichzeitig (Platzhalter-Pool)
- Ausgelegt auf **Einzelspieler / LAN-Host**; auf dedizierten Servern funktionieren
  Definitionen, Rezepte und Skripte, aber Mitspieler brauchen die Mod und für Texturen
  die gleichen Dateien
- Neue Elemente erscheinen im Kreativ-Tab sofort; in seltenen Fällen hilft einmal
  Welt neu betreten
- Keine eigenen Mobs/Dimensionen (bewusste Ausbaustufe)

## Lizenz

MIT
