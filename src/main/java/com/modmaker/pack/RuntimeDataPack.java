package com.modmaker.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.modmaker.ModMaker;
import com.modmaker.content.ContentManager;
import com.modmaker.content.RecipeDefinition;
import com.modmaker.registry.ItemRefs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Generates a vanilla data pack inside the world's {@code datapacks/} folder containing all
 * user-created recipes, and reloads server resources so new recipes work immediately.
 */
public final class RuntimeDataPack {
	public static final String PACK_DIR_NAME = "modmaker_runtime";
	/** Pack id vanilla assigns to world data pack folders. */
	public static final String PACK_ID = "file/" + PACK_DIR_NAME;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int DATA_FORMAT = 107;

	private RuntimeDataPack() {
	}

	/** Writes/refreshes the pack on disk. Safe to call before world load and while running. */
	public static void writeToWorld(MinecraftServer server) {
		Path dir = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK_DIR_NAME);
		try {
			write(dir);
		} catch (IOException e) {
			ModMaker.LOGGER.error("ModMaker could not write runtime data pack", e);
		}
	}

	/** Rewrites the pack, makes sure it is selected and reloads server resources. */
	public static void rewriteAndReload(MinecraftServer server) {
		writeToWorld(server);
		PackRepository repository = server.getPackRepository();
		repository.reload();
		List<String> selected = new ArrayList<>(repository.getSelectedIds());
		if (!selected.contains(PACK_ID) && repository.getAvailableIds().contains(PACK_ID)) {
			selected.add(PACK_ID);
		}
		server.reloadResources(selected).exceptionally(throwable -> {
			ModMaker.LOGGER.error("ModMaker data pack reload failed", throwable);
			return null;
		});
	}

	private static void write(Path root) throws IOException {
		Path recipeDir = root.resolve("data").resolve("modmaker").resolve("recipe");
		deleteRecursively(root.resolve("data"));
		Files.createDirectories(recipeDir);

		JsonObject mcmeta = new JsonObject();
		JsonObject pack = new JsonObject();
		pack.addProperty("description", "ModMaker runtime recipes");
		pack.addProperty("min_format", DATA_FORMAT);
		pack.addProperty("max_format", DATA_FORMAT);
		mcmeta.add("pack", pack);
		Files.writeString(root.resolve("pack.mcmeta"), GSON.toJson(mcmeta));

		int written = 0;
		for (RecipeDefinition def : ContentManager.get().allRecipes()) {
			JsonObject json = buildRecipeJson(def);
			if (json == null) {
				ModMaker.LOGGER.warn("ModMaker recipe '{}' has unresolved items and was skipped", def.id);
				continue;
			}
			Files.writeString(recipeDir.resolve(def.id + ".json"), GSON.toJson(json));
			written++;
		}
		ModMaker.LOGGER.info("ModMaker wrote {} recipe(s) into the runtime data pack", written);
	}

	/** Builds vanilla recipe JSON from a definition; returns null when an item reference is unknown. */
	public static JsonObject buildRecipeJson(RecipeDefinition def) {
		String resultId = ItemRefs.registryId(def.resultItem);
		if (resultId == null) return null;

		JsonObject result = new JsonObject();
		result.addProperty("id", resultId);
		result.addProperty("count", Math.max(1, def.resultCount));

		JsonObject json = new JsonObject();
		if ("shapeless".equals(def.type)) {
			JsonArray ingredients = new JsonArray();
			for (String ref : def.grid) {
				if (ref == null || ref.isBlank()) continue;
				String id = ItemRefs.registryId(ref);
				if (id == null) return null;
				ingredients.add(id);
			}
			if (ingredients.isEmpty()) return null;
			json.addProperty("type", "minecraft:crafting_shapeless");
			json.addProperty("category", "misc");
			json.add("ingredients", ingredients);
		} else {
			// Assign a letter to each distinct ingredient and build a trimmed pattern.
			Map<String, Character> letters = new LinkedHashMap<>();
			char next = 'A';
			char[][] cells = new char[3][3];
			boolean any = false;
			for (int i = 0; i < 9; i++) {
				String ref = i < def.grid.size() ? def.grid.get(i) : "";
				if (ref == null || ref.isBlank()) {
					cells[i / 3][i % 3] = ' ';
					continue;
				}
				if (ItemRefs.registryId(ref) == null) return null;
				Character letter = letters.get(ref);
				if (letter == null) {
					letter = next++;
					letters.put(ref, letter);
				}
				cells[i / 3][i % 3] = letter;
				any = true;
			}
			if (!any) return null;

			int minRow = 3, maxRow = -1, minCol = 3, maxCol = -1;
			for (int r = 0; r < 3; r++) {
				for (int c = 0; c < 3; c++) {
					if (cells[r][c] != ' ') {
						minRow = Math.min(minRow, r);
						maxRow = Math.max(maxRow, r);
						minCol = Math.min(minCol, c);
						maxCol = Math.max(maxCol, c);
					}
				}
			}
			JsonArray pattern = new JsonArray();
			for (int r = minRow; r <= maxRow; r++) {
				StringBuilder row = new StringBuilder();
				for (int c = minCol; c <= maxCol; c++) {
					row.append(cells[r][c]);
				}
				pattern.add(row.toString());
			}
			JsonObject key = new JsonObject();
			letters.forEach((ref, letter) -> key.addProperty(String.valueOf(letter), ItemRefs.registryId(ref)));

			json.addProperty("type", "minecraft:crafting_shaped");
			json.addProperty("category", "misc");
			json.add("pattern", pattern);
			json.add("key", key);
		}
		json.add("result", result);
		return json;
	}

	private static void deleteRecursively(Path path) throws IOException {
		if (!Files.exists(path)) return;
		try (Stream<Path> stream = Files.walk(path)) {
			stream.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (IOException ignored) {
				}
			});
		}
	}
}
