# MinClient

**MinClient by Minju26** is a Minecraft Fabric utility client for **26.2**, based on [Meteor Client](https://github.com/MeteorDevelopment/meteor-client).

The title screen credit reads **MinClient by Minju26**. Chat prefix, GUI accent, Discord presence, HUD watermark and splashes are all rebranded.

## MinClient extras

| Feature | Category | What it does |
| --- | --- | --- |
| **China Hat** | Render | Spinning rainbow cone above your head |
| **Jump Circles** | Render | Expanding rainbow rings when you jump |
| **Hit Markers** | Render | Crosshair marker + sound on hit |
| **Kill Effects** | Render | Lightning / totem / explosion / confetti on kills |
| **Min Notifications** | Misc | Toast popups for toggles, kills and totem pops |
| **Min Watermark** | HUD | Gradient `MinClient` title with `by Minju26` |
| **Session Stats** | HUD | Playtime, K/D, streak and totem pops |

On first launch those visual modules are enabled automatically.

## Build

Java **25** is required (Minecraft 26.2).

```bash
./gradlew build
```

The playable Fabric mod JAR is:

```
jars/min-client-26.2-minju.jar
```

Copy it into your Minecraft `mods` folder. Fabric Loader for 26.2 is required. Do **not** install vanilla Meteor Client next to MinClient — they share the same mod id (`meteor-client`) so addons still work.

To rebuild from source (Java 25):

```bash
./gradlew build
```

## License

GNU GPL v3.0 — same as Meteor Client.

This is a fork. Original copyright belongs to Meteor Development. See `LICENSE` and `NOTICE`.
