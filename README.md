# MinecraftModMaker

Minecraft-Mods **in Python** schreiben – direkt in **Cursor** oder **VS Code**.  
[fabricpy](https://github.com/danielkorkin/fabricpy) erzeugt daraus ein fertiges **Fabric**-Mod-Projekt, das du bauen und im Minecraft-Dev-Client testen kannst.

## Voraussetzungen

| Tool | Version | Zweck |
|------|---------|--------|
| Python | 3.10+ | Mod-Definition & Scripts |
| JDK | **25** (wird bei Bedarf nach `.jdk/` geladen) | Fabric/Gradle für Minecraft 26.2 |
| Git | 2+ | Fabric-Template klonen |

## Schnellstart

```bash
# 1. Abhängigkeiten
python3 -m pip install -r requirements.txt

# 2. Mod aus Python generieren & bauen
python3 compile_mod.py

# 3a. Testlauf (ohne Minecraft-Fenster)
python3 run_test.py

# 3b. Echter Ingame-Testlauf (Minecraft Dev-Client)
python3 run_client.py
```

### In Cursor / VS Code

1. Diesen Ordner öffnen  
2. Empfohlene Extensions installieren (Python)  
3. **Run and Debug** (`F5`) → eine Konfiguration wählen:
   - **Mod kompilieren**
   - **Testlauf (Unit-Tests)**
   - **Minecraft Testlauf (Dev-Client)**

Oder **Terminal → Run Task…** mit denselben Tasks.

## Mod bearbeiten

Alles Wichtige steckt in [`mod.py`](mod.py):

```python
import fabricpy

mod = fabricpy.ModConfig(
    mod_id="rubymod",
    name="Ruby Mod",
    version="1.0.0",
    description="Meine Mod",
    authors=["Du"],
    project_dir="generated-mod",
)

mod.registerItem(fabricpy.Item(
    id="rubymod:ruby",
    name="Ruby",
    texture_path="assets/textures/ruby.png",
))

mod.registerBlock(fabricpy.Block(
    id="rubymod:ruby_block",
    name="Ruby Block",
    block_texture_path="assets/textures/ruby_block.png",
    loot_table=fabricpy.LootTable.drops_self("rubymod:ruby_block"),
))
```

Texturen: 16×16 PNG unter `assets/textures/`.

Nach Änderungen an `mod.py` erneut `compile_mod.py` bzw. den Testlauf starten – das regeneriert `generated-mod/`.

## Projektstruktur

```
MinecraftModMaker/
├── mod.py                 ← deine Mod (Items, Blöcke, Rezepte)
├── compile_mod.py         ← Fabric-Projekt erzeugen + bauen
├── run_test.py            ← Testlauf (Python-Checks + Gradle-Build)
├── run_client.py          ← Minecraft Dev-Client starten
├── project_fix.py        ← Kompatibilität mit Minecraft 26.2 / JDK 25
├── assets/textures/       ← PNG-Texturen
├── generated-mod/         ← generiertes Fabric-Projekt (nach compile)
├── requirements.txt
└── .vscode/               ← Launch-Configs & Tasks für Cursor/VS Code
```

## Beispielinhalt (Ruby Mod)

- **Ruby** – Item  
- **Ruby Pickaxe** – Tool + Crafting-Rezept  
- **Ruby Apple** – Food + Rezept  
- **Ruby Block** / **Ruby Ore** – Blöcke mit Loot-Tabellen  
- Creative Tab: **Ruby Items**

## Tipps

- Der erste Build lädt Minecraft-Abhängigkeiten und kann länger dauern.  
- JDK 25 wird automatisch nach `.jdk/` heruntergeladen, falls nötig.  
- `run_client.py` braucht ein Display (lokal). In headless/CI besser `run_test.py`.  
- Generiertes Java-Projekt: `generated-mod/` – JAR unter `generated-mod/build/libs/`.
