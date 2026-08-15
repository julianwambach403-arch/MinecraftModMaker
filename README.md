# Baritone Plus

Standalone Fabric mod for **Minecraft 26.2**. It bundles the uploaded `baritone-meteor-26.2` pathfinder (LGPL-3.0) and adds a survival AI layer on top of Baritone's public API. Meteor Client is **not** required.

Drop **one jar** into `mods/` together with Fabric Loader and Fabric API.

## Why this is standalone

`baritone-meteor-26.2.jar` is already a Fabric mod (mixins + `#` chat commands) and does not depend on Meteor. Re-implementing A* from scratch would be worse than using Baritone as the library it is. This project therefore:

1. Nests the original Baritone jar (`META-INF/jars/…`) so you only install one file
2. Uses **Fabric API** for HUD, keybinds and client ticks
3. Registers extra Baritone *processes* (the same plugin point Mine/Follow/Farm use)

Cloth Config / Mod Menu are optional and not required.

## Extra AI

| Layer | What it does |
| --- | --- |
| Survival brain | Auto-eat from the hotbar, flee creepers / low-HP hostiles, escape lava/fire, surface when drowning |
| Stuck recovery | Jump, sneak-wiggle, force-repath, then sidestep if the bot is frozen on a path |
| Loot assist | Briefly path to nearby valuables (diamonds, netherite, totems, shulkers, …) |
| Path tuner | Safer default costs: mob avoidance, magma/cactus/fire avoid lists, slightly better timeouts |

Toggle any of that in-game:

```
#plus
#plus brain off
#plus loot on
#plus save
```

Config file: `config/baritoneplus.properties`

## Commands (prefix `#`)

All original Baritone commands still work (`#goto`, `#mine`, `#follow`, `#elytra`, `#farm`, `#build`, `#stop`, …).

Added:

| Command | Action |
| --- | --- |
| `#plus` | Status + toggles for the AI extras |
| `#ore` | Mine diamond / debris / emerald / gold (or pass block ids) |
| `#wood` / `#holz` | Chop nearby logs |
| `#look` | Path to the block you are looking at |
| `#plusstatus` | Process, goal, ETA |
| `#gehe` `#stopp` `#folge` `#erz` | German aliases |

## Keys (Misc category)

- **R** stop
- **G** path to looked-at block
- **H** HUD on/off
- **B** survival brain on/off

## Build

Needs JDK 25 (Minecraft 26.2).

```bash
export JAVA_HOME=/path/to/jdk-25
./gradlew build
```

The playable jar is `build/libs/baritoneplus-1.0.0.jar` (also copied to `releases/`).

## License

- Baritone Plus overlay code: MIT (`LICENSE`)
- Nested Baritone: GNU LGPL v3 (`LICENSE-BARITONE`), https://github.com/cabaletta/baritone
