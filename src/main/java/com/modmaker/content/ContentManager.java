package com.modmaker.content;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.modmaker.ModMaker;
import com.modmaker.ModMakerPaths;
import com.modmaker.registry.PlaceholderPool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * Loads, stores and indexes all user-created content (items, blocks, recipes) and
 * maintains the stable mapping between definition ids and placeholder slots.
 *
 * <p>Definitions are plain JSON files so they can also be edited or shared outside the game.
 */
public final class ContentManager {
	private static final ContentManager INSTANCE = new ContentManager();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private final Map<String, ItemDefinition> items = new LinkedHashMap<>();
	private final Map<String, BlockDefinition> blocks = new LinkedHashMap<>();
	private final Map<String, RecipeDefinition> recipes = new LinkedHashMap<>();

	/** definition id -> placeholder slot */
	private final Map<String, Integer> itemSlots = new LinkedHashMap<>();
	private final Map<String, Integer> blockSlots = new LinkedHashMap<>();

	/** slot -> definition id (rebuilt from the maps above) */
	private ItemDefinition[] itemBySlot = new ItemDefinition[PlaceholderPool.ITEM_SLOTS];
	private BlockDefinition[] blockBySlot = new BlockDefinition[PlaceholderPool.BLOCK_SLOTS];

	private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

	private ContentManager() {
	}

	public static ContentManager get() {
		return INSTANCE;
	}

	// ------------------------------------------------------------------ loading

	public synchronized void loadAll() {
		items.clear();
		blocks.clear();
		recipes.clear();
		itemSlots.clear();
		blockSlots.clear();

		loadBindings();
		loadDirectory(ModMakerPaths.items(), ItemDefinition.class, def -> {
			if (def.id != null && !def.id.isBlank()) items.put(def.id, def);
		});
		loadDirectory(ModMakerPaths.blocks(), BlockDefinition.class, def -> {
			if (def.id != null && !def.id.isBlank()) blocks.put(def.id, def);
		});
		loadDirectory(ModMakerPaths.recipes(), RecipeDefinition.class, def -> {
			if (def.id != null && !def.id.isBlank()) recipes.put(def.id, def);
		});

		// Drop bindings whose definitions vanished, then assign slots to unbound definitions.
		itemSlots.keySet().removeIf(id -> !items.containsKey(id));
		blockSlots.keySet().removeIf(id -> !blocks.containsKey(id));
		items.keySet().forEach(this::ensureItemSlot);
		blocks.keySet().forEach(this::ensureBlockSlot);

		rebuildSlotIndex();
		saveBindings();
		ModMaker.LOGGER.info("ModMaker loaded {} items, {} blocks, {} recipes", items.size(), blocks.size(), recipes.size());
	}

	private <T> void loadDirectory(Path dir, Class<T> type, java.util.function.Consumer<T> sink) {
		if (!Files.isDirectory(dir)) return;
		try (Stream<Path> stream = Files.list(dir)) {
			stream.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().forEach(path -> {
				try {
					T def = GSON.fromJson(Files.readString(path), type);
					if (def != null) sink.accept(def);
				} catch (Exception e) {
					ModMaker.LOGGER.error("ModMaker could not read {}: {}", path, e.toString());
				}
			});
		} catch (IOException e) {
			ModMaker.LOGGER.error("ModMaker could not list {}", dir, e);
		}
	}

	private void loadBindings() {
		Path file = ModMakerPaths.bindingsFile();
		if (!Files.isRegularFile(file)) return;
		try {
			JsonObject root = GSON.fromJson(Files.readString(file), JsonObject.class);
			if (root == null) return;
			if (root.has("items")) {
				root.getAsJsonObject("items").entrySet().forEach(e -> itemSlots.put(e.getKey(), e.getValue().getAsInt()));
			}
			if (root.has("blocks")) {
				root.getAsJsonObject("blocks").entrySet().forEach(e -> blockSlots.put(e.getKey(), e.getValue().getAsInt()));
			}
		} catch (Exception e) {
			ModMaker.LOGGER.error("ModMaker could not read bindings.json", e);
		}
	}

	private void saveBindings() {
		JsonObject root = new JsonObject();
		JsonObject itemsJson = new JsonObject();
		itemSlots.forEach(itemsJson::addProperty);
		JsonObject blocksJson = new JsonObject();
		blockSlots.forEach(blocksJson::addProperty);
		root.add("items", itemsJson);
		root.add("blocks", blocksJson);
		try {
			Files.createDirectories(ModMakerPaths.root());
			Files.writeString(ModMakerPaths.bindingsFile(), GSON.toJson(root));
		} catch (IOException e) {
			ModMaker.LOGGER.error("ModMaker could not write bindings.json", e);
		}
	}

	private void ensureItemSlot(String defId) {
		if (itemSlots.containsKey(defId)) return;
		int slot = firstFreeSlot(itemSlots.values(), PlaceholderPool.ITEM_SLOTS);
		if (slot < 0) {
			ModMaker.LOGGER.error("ModMaker is out of item slots ({}), '{}' will not be usable", PlaceholderPool.ITEM_SLOTS, defId);
			return;
		}
		itemSlots.put(defId, slot);
	}

	private void ensureBlockSlot(String defId) {
		if (blockSlots.containsKey(defId)) return;
		int slot = firstFreeSlot(blockSlots.values(), PlaceholderPool.BLOCK_SLOTS);
		if (slot < 0) {
			ModMaker.LOGGER.error("ModMaker is out of block slots ({}), '{}' will not be usable", PlaceholderPool.BLOCK_SLOTS, defId);
			return;
		}
		blockSlots.put(defId, slot);
	}

	private static int firstFreeSlot(java.util.Collection<Integer> used, int max) {
		for (int i = 0; i < max; i++) {
			if (!used.contains(i)) return i;
		}
		return -1;
	}

	private void rebuildSlotIndex() {
		ItemDefinition[] newItems = new ItemDefinition[PlaceholderPool.ITEM_SLOTS];
		itemSlots.forEach((id, slot) -> {
			ItemDefinition def = items.get(id);
			if (def != null && slot >= 0 && slot < newItems.length) newItems[slot] = def;
		});
		BlockDefinition[] newBlocks = new BlockDefinition[PlaceholderPool.BLOCK_SLOTS];
		blockSlots.forEach((id, slot) -> {
			BlockDefinition def = blocks.get(id);
			if (def != null && slot >= 0 && slot < newBlocks.length) newBlocks[slot] = def;
		});
		itemBySlot = newItems;
		blockBySlot = newBlocks;
	}

	// ------------------------------------------------------------------ mutation (used by GUI / import)

	public synchronized void saveItem(ItemDefinition def) {
		def.id = sanitizeId(def.id);
		ItemDefinition existing = items.get(def.id);
		def.version = existing != null ? existing.version + 1 : 1;
		items.put(def.id, def);
		ensureItemSlot(def.id);
		writeJson(ModMakerPaths.items().resolve(def.id + ".json"), def);
		rebuildSlotIndex();
		saveBindings();
		fireChanged();
	}

	public synchronized void saveBlock(BlockDefinition def) {
		def.id = sanitizeId(def.id);
		BlockDefinition existing = blocks.get(def.id);
		def.version = existing != null ? existing.version + 1 : 1;
		blocks.put(def.id, def);
		ensureBlockSlot(def.id);
		writeJson(ModMakerPaths.blocks().resolve(def.id + ".json"), def);
		rebuildSlotIndex();
		saveBindings();
		fireChanged();
	}

	public synchronized void saveRecipe(RecipeDefinition def) {
		def.id = sanitizeId(def.id);
		RecipeDefinition existing = recipes.get(def.id);
		def.version = existing != null ? existing.version + 1 : 1;
		recipes.put(def.id, def);
		writeJson(ModMakerPaths.recipes().resolve(def.id + ".json"), def);
		fireChanged();
	}

	public synchronized void deleteItem(String id) {
		if (items.remove(id) == null) return;
		itemSlots.remove(id);
		deleteQuietly(ModMakerPaths.items().resolve(id + ".json"));
		rebuildSlotIndex();
		saveBindings();
		fireChanged();
	}

	public synchronized void deleteBlock(String id) {
		if (blocks.remove(id) == null) return;
		blockSlots.remove(id);
		deleteQuietly(ModMakerPaths.blocks().resolve(id + ".json"));
		rebuildSlotIndex();
		saveBindings();
		fireChanged();
	}

	public synchronized void deleteRecipe(String id) {
		if (recipes.remove(id) == null) return;
		deleteQuietly(ModMakerPaths.recipes().resolve(id + ".json"));
		fireChanged();
	}

	private void writeJson(Path path, Object def) {
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(def));
		} catch (IOException e) {
			ModMaker.LOGGER.error("ModMaker could not write {}", path, e);
		}
	}

	private static void deleteQuietly(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException ignored) {
		}
	}

	/** Lower-cases and strips characters that are not valid in identifiers. */
	public static String sanitizeId(String raw) {
		String id = raw == null ? "" : raw.toLowerCase(Locale.ROOT).trim()
				.replace(' ', '_').replaceAll("[^a-z0-9_.-]", "");
		return id.isBlank() ? "element_" + System.currentTimeMillis() % 100000 : id;
	}

	// ------------------------------------------------------------------ queries

	public ItemDefinition itemBySlot(int slot) {
		ItemDefinition[] index = itemBySlot;
		return slot >= 0 && slot < index.length ? index[slot] : null;
	}

	public BlockDefinition blockBySlot(int slot) {
		BlockDefinition[] index = blockBySlot;
		return slot >= 0 && slot < index.length ? index[slot] : null;
	}

	public ItemDefinition item(String id) {
		return items.get(id);
	}

	public BlockDefinition block(String id) {
		return blocks.get(id);
	}

	public RecipeDefinition recipe(String id) {
		return recipes.get(id);
	}

	public Integer itemSlot(String defId) {
		return itemSlots.get(defId);
	}

	public Integer blockSlot(String defId) {
		return blockSlots.get(defId);
	}

	public List<ItemDefinition> allItems() {
		return new ArrayList<>(items.values());
	}

	public List<BlockDefinition> allBlocks() {
		return new ArrayList<>(blocks.values());
	}

	public List<RecipeDefinition> allRecipes() {
		return new ArrayList<>(recipes.values());
	}

	// ------------------------------------------------------------------ change notification

	public void addChangeListener(Runnable listener) {
		changeListeners.add(listener);
	}

	public void fireChanged() {
		for (Runnable listener : changeListeners) {
			try {
				listener.run();
			} catch (Exception e) {
				ModMaker.LOGGER.error("ModMaker change listener failed", e);
			}
		}
	}
}
