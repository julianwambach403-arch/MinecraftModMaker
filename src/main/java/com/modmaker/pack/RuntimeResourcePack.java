package com.modmaker.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.modmaker.ModMaker;
import com.modmaker.ModMakerPaths;
import com.modmaker.content.BlockDefinition;
import com.modmaker.content.ContentManager;
import com.modmaker.content.ItemDefinition;
import com.modmaker.registry.PlaceholderPool;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Writes a normal directory resource pack into {@code resourcepacks/ModMakerRuntime} that
 * gives every placeholder slot a model and the texture of its bound definition. The client
 * enables the pack automatically and re-triggers a resource reload when textures change,
 * so imported textures show up without restarting the game.
 */
public final class RuntimeResourcePack {
	public static final String PACK_ID = "file/ModMakerRuntime";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int RESOURCE_FORMAT = 88;

	private static byte[] defaultItemTexture;
	private static byte[] defaultBlockTexture;

	private RuntimeResourcePack() {
	}

	/**
	 * (Re)writes the pack. Static files (mcmeta, models, item definitions) are only written
	 * when missing; textures are compared byte-wise.
	 *
	 * @return true when any file changed, i.e. a client resource reload is needed
	 */
	public static synchronized boolean writeFull() {
		try {
			return writeInternal();
		} catch (IOException e) {
			ModMaker.LOGGER.error("ModMaker could not write the runtime resource pack", e);
			return false;
		}
	}

	private static boolean writeInternal() throws IOException {
		Path root = ModMakerPaths.runtimeResourcePack();
		Path assets = root.resolve("assets").resolve("modmaker");
		Files.createDirectories(assets);
		boolean changed = false;

		changed |= writeIfMissing(root.resolve("pack.mcmeta"), packMcmeta());

		Path itemDefs = assets.resolve("items");
		Path itemModels = assets.resolve("models").resolve("item");
		Path itemTextures = assets.resolve("textures").resolve("item");
		Path blockstates = assets.resolve("blockstates");
		Path blockModels = assets.resolve("models").resolve("block");
		Path blockTextures = assets.resolve("textures").resolve("block");
		Files.createDirectories(itemDefs);
		Files.createDirectories(itemModels);
		Files.createDirectories(itemTextures);
		Files.createDirectories(blockstates);
		Files.createDirectories(blockModels);
		Files.createDirectories(blockTextures);

		ContentManager content = ContentManager.get();

		for (int i = 0; i < PlaceholderPool.ITEM_SLOTS; i++) {
			String name = "item_" + i;
			changed |= writeIfMissing(itemDefs.resolve(name + ".json"),
					itemDefinitionJson("modmaker:item/" + name));
			changed |= writeIfMissing(itemModels.resolve(name + ".json"),
					itemModelJson("modmaker:item/" + name));

			ItemDefinition def = content.itemBySlot(i);
			byte[] texture = resolveTexture(def != null ? def.texture : null, true);
			changed |= writeIfDifferent(itemTextures.resolve(name + ".png"), texture);
		}

		for (int i = 0; i < PlaceholderPool.BLOCK_SLOTS; i++) {
			String name = "block_" + i;
			changed |= writeIfMissing(blockstates.resolve(name + ".json"), blockstateJson("modmaker:block/" + name));
			changed |= writeIfMissing(blockModels.resolve(name + ".json"), blockModelJson("modmaker:block/" + name));
			changed |= writeIfMissing(itemDefs.resolve(name + ".json"),
					itemDefinitionJson("modmaker:block/" + name));

			BlockDefinition def = content.blockBySlot(i);
			byte[] texture = resolveTexture(def != null ? def.texture : null, false);
			changed |= writeIfDifferent(blockTextures.resolve(name + ".png"), texture);
		}

		return changed;
	}

	private static String packMcmeta() {
		JsonObject pack = new JsonObject();
		pack.addProperty("description", "ModMaker runtime resources");
		pack.addProperty("min_format", RESOURCE_FORMAT);
		pack.addProperty("max_format", RESOURCE_FORMAT);
		JsonObject root = new JsonObject();
		root.add("pack", pack);
		return GSON.toJson(root);
	}

	private static String itemDefinitionJson(String model) {
		JsonObject inner = new JsonObject();
		inner.addProperty("type", "minecraft:model");
		inner.addProperty("model", model);
		JsonObject root = new JsonObject();
		root.add("model", inner);
		return GSON.toJson(root);
	}

	private static String itemModelJson(String texture) {
		JsonObject textures = new JsonObject();
		textures.addProperty("layer0", texture);
		JsonObject root = new JsonObject();
		root.addProperty("parent", "minecraft:item/generated");
		root.add("textures", textures);
		return GSON.toJson(root);
	}

	private static String blockstateJson(String model) {
		JsonObject variant = new JsonObject();
		variant.addProperty("model", model);
		JsonObject variants = new JsonObject();
		variants.add("", variant);
		JsonObject root = new JsonObject();
		root.add("variants", variants);
		return GSON.toJson(root);
	}

	private static String blockModelJson(String texture) {
		JsonObject textures = new JsonObject();
		textures.addProperty("all", texture);
		JsonObject root = new JsonObject();
		root.addProperty("parent", "minecraft:block/cube_all");
		root.add("textures", textures);
		return GSON.toJson(root);
	}

	private static byte[] resolveTexture(String textureFile, boolean item) {
		if (textureFile != null && !textureFile.isBlank()) {
			Path source = ModMakerPaths.textures().resolve(textureFile);
			if (Files.isRegularFile(source)) {
				try {
					return Files.readAllBytes(source);
				} catch (IOException e) {
					ModMaker.LOGGER.warn("ModMaker could not read texture {}", source);
				}
			}
		}
		return item ? defaultItemTexture() : defaultBlockTexture();
	}

	private static byte[] defaultItemTexture() {
		if (defaultItemTexture == null) defaultItemTexture = loadBundled("assets/modmaker/default_item.png");
		return defaultItemTexture;
	}

	private static byte[] defaultBlockTexture() {
		if (defaultBlockTexture == null) defaultBlockTexture = loadBundled("assets/modmaker/default_block.png");
		return defaultBlockTexture;
	}

	private static byte[] loadBundled(String resource) {
		try (InputStream in = RuntimeResourcePack.class.getClassLoader().getResourceAsStream(resource)) {
			if (in != null) return in.readAllBytes();
		} catch (IOException ignored) {
		}
		ModMaker.LOGGER.error("ModMaker bundled resource {} is missing", resource);
		return new byte[0];
	}

	private static boolean writeIfMissing(Path path, String contents) throws IOException {
		if (Files.exists(path)) return false;
		Files.createDirectories(path.getParent());
		Files.writeString(path, contents);
		return true;
	}

	private static boolean writeIfDifferent(Path path, byte[] contents) throws IOException {
		if (Files.isRegularFile(path)) {
			byte[] existing = Files.readAllBytes(path);
			if (Arrays.equals(existing, contents)) return false;
		}
		Files.createDirectories(path.getParent());
		Files.write(path, contents);
		return true;
	}
}
