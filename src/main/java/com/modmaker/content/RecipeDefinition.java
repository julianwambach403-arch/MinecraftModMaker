package com.modmaker.content;

import java.util.ArrayList;
import java.util.List;

/**
 * A user-created crafting recipe, stored as JSON in {@code modmaker/recipes/<id>.json}
 * and exported into the runtime data pack as a vanilla recipe.
 *
 * <p>The 3x3 grid is stored as 9 item references (row-major); empty string = empty slot.
 * Item references are either ModMaker definition ids (e.g. "ruby") or vanilla ids
 * (e.g. "minecraft:stick").
 */
public class RecipeDefinition {
	public String id = "";
	/** shaped | shapeless */
	public String type = "shaped";
	public List<String> grid = new ArrayList<>(List.of("", "", "", "", "", "", "", "", ""));
	public String resultItem = "";
	public int resultCount = 1;

	public int version = 1;
}
