"""
Minecraft-Mod in Python definieren.

Hier registrierst du Items, Blöcke, Tools und Food.
Anschließend mit compile_mod.py bauen oder run_test.py / run_client.py starten.
"""

from __future__ import annotations

from pathlib import Path

import fabricpy

ROOT = Path(__file__).resolve().parent
TEXTURES = ROOT / "assets" / "textures"

# ---------------------------------------------------------------------------
# Mod-Konfiguration
# ---------------------------------------------------------------------------
mod = fabricpy.ModConfig(
    mod_id="rubymod",
    name="Ruby Mod",
    version="1.0.0",
    description="Beispiel-Mod: Ruby-Items und -Blöcke – geschrieben in Python mit fabricpy",
    authors=["MinecraftModMaker"],
    project_dir=str(ROOT / "generated-mod"),
    # fabricpy-Unit-Tests sind für Minecraft 26.2 noch nicht kompatibel;
    # der Testlauf nutzt Python-Checks + Gradle-Compile (siehe run_test.py).
    enable_testing=False,
    generate_unit_tests=False,
    generate_game_tests=False,
)

# ---------------------------------------------------------------------------
# Custom Creative Tab
# ---------------------------------------------------------------------------
ruby_tab = fabricpy.ItemGroup(
    id="ruby_items",
    name="Ruby Items",
)

# ---------------------------------------------------------------------------
# Items
# ---------------------------------------------------------------------------
ruby = fabricpy.Item(
    id="rubymod:ruby",
    name="Ruby",
    max_stack_size=64,
    texture_path=str(TEXTURES / "ruby.png"),
    item_group=ruby_tab,
)
mod.registerItem(ruby)

ruby_pickaxe = fabricpy.ToolItem(
    id="rubymod:ruby_pickaxe",
    name="Ruby Pickaxe",
    texture_path=str(TEXTURES / "ruby_pickaxe.png"),
    durability=800,
    mining_speed_multiplier=9.0,
    attack_damage=4.0,
    mining_level=2,
    enchantability=22,
    repair_ingredient="rubymod:ruby",
    item_group=ruby_tab,
    recipe=fabricpy.RecipeJson(
        {
            "type": "minecraft:crafting_shaped",
            "pattern": ["RRR", " S ", " S "],
            "key": {
                "R": "rubymod:ruby",
                "S": "minecraft:stick",
            },
            "result": {"id": "rubymod:ruby_pickaxe", "count": 1},
        }
    ),
)
mod.registerItem(ruby_pickaxe)

ruby_apple = fabricpy.FoodItem(
    id="rubymod:ruby_apple",
    name="Ruby Apple",
    texture_path=str(TEXTURES / "ruby_apple.png"),
    nutrition=6,
    saturation=10.0,
    always_edible=True,
    item_group=ruby_tab,
    recipe=fabricpy.RecipeJson(
        {
            "type": "minecraft:crafting_shaped",
            "pattern": ["RRR", "RAR", "RRR"],
            "key": {
                "R": "rubymod:ruby",
                "A": "minecraft:apple",
            },
            "result": {"id": "rubymod:ruby_apple", "count": 1},
        }
    ),
)
mod.registerFoodItem(ruby_apple)

# ---------------------------------------------------------------------------
# Blocks
# ---------------------------------------------------------------------------
ruby_block = fabricpy.Block(
    id="rubymod:ruby_block",
    name="Ruby Block",
    block_texture_path=str(TEXTURES / "ruby_block.png"),
    item_group=ruby_tab,
    hardness=5.0,
    resistance=6.0,
    tool_type="pickaxe",
    mining_level="iron",
    loot_table=fabricpy.LootTable.drops_self("rubymod:ruby_block"),
    recipe=fabricpy.RecipeJson(
        {
            "type": "minecraft:crafting_shaped",
            "pattern": ["RRR", "RRR", "RRR"],
            "key": {"R": "rubymod:ruby"},
            "result": {"id": "rubymod:ruby_block", "count": 1},
        }
    ),
)
mod.registerBlock(ruby_block)

ruby_ore = fabricpy.Block(
    id="rubymod:ruby_ore",
    name="Ruby Ore",
    block_texture_path=str(TEXTURES / "ruby_block.png"),
    item_group=ruby_tab,
    hardness=3.0,
    resistance=3.0,
    tool_type="pickaxe",
    mining_level="iron",
    loot_table=fabricpy.LootTable.drops_with_fortune(
        "rubymod:ruby_ore",
        "rubymod:ruby",
        min_count=1,
        max_count=3,
    ),
)
mod.registerBlock(ruby_ore)


def get_mod() -> fabricpy.ModConfig:
    """Gibt die konfigurierte Mod zurück (für Scripts und Tests)."""
    return mod


if __name__ == "__main__":
    print(f"Mod '{mod.name}' ({mod.mod_id}) v{mod.version}")
    print(f"  Items:  {len(mod.registered_items)}")
    print(f"  Blöcke: {len(mod.registered_blocks)}")
    print("Zum Bauen:     python compile_mod.py")
    print("Zum Testen:    python run_test.py")
    print("Minecraft:     python run_client.py")
