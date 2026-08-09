package com.modmaker.content;

/**
 * A user-created item, stored as JSON in {@code modmaker/items/<id>.json}.
 * Bound to a placeholder item slot at runtime so it can be used without restarting the game.
 */
public class ItemDefinition {
	public String id = "";
	public String displayName = "New Item";
	/** File name of a PNG inside {@code modmaker/textures/}, empty for the default texture. */
	public String texture = "";
	public String tooltip = "";
	public int stackSize = 64;
	/** common | uncommon | rare | epic */
	public String rarity = "common";
	public boolean glint = false;

	public boolean foodEnabled = false;
	public int foodNutrition = 4;
	public float foodSaturation = 0.3f;
	public boolean foodAlwaysEdible = false;

	/** none | pickaxe | axe | shovel | hoe | sword */
	public String toolType = "none";
	public float miningSpeed = 4.0f;
	public float attackDamage = 3.0f;
	public float attackSpeed = -2.4f;
	public int durability = 0;

	/** Incremented on every save; used to re-stamp item stacks that carry an older version. */
	public int version = 1;

	public boolean isTool() {
		return toolType != null && !toolType.isBlank() && !"none".equals(toolType);
	}
}
