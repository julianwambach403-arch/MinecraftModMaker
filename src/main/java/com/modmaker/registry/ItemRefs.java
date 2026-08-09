package com.modmaker.registry;

import com.modmaker.content.ContentManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Resolves user-facing item references. A reference is either the id of a ModMaker item or
 * block definition (e.g. {@code ruby}) or any registered item id (e.g. {@code minecraft:stick}).
 */
public final class ItemRefs {
	private ItemRefs() {
	}

	public static Item resolveItem(String ref) {
		if (ref == null || ref.isBlank()) return null;
		ContentManager content = ContentManager.get();

		Integer itemSlot = content.itemSlot(ref);
		if (itemSlot != null) return PlaceholderPool.ITEMS[itemSlot];

		Integer blockSlot = content.blockSlot(ref);
		if (blockSlot != null) return PlaceholderPool.BLOCK_ITEMS[blockSlot];

		Identifier id = Identifier.tryParse(ref);
		if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
			return BuiltInRegistries.ITEM.getValue(id);
		}
		return null;
	}

	/** Creates a ready-to-use stack (stamped when the reference is a ModMaker item). */
	public static ItemStack createStack(String ref, int count) {
		Item item = resolveItem(ref);
		if (item == null) return ItemStack.EMPTY;
		ItemStack stack = item.getDefaultInstance();
		stack.setCount(Math.max(1, count));
		return stack;
	}

	/** Registry id (e.g. {@code modmaker:item_5} or {@code minecraft:stick}) for recipe JSON, or null. */
	public static String registryId(String ref) {
		Item item = resolveItem(ref);
		if (item == null) return null;
		Identifier key = BuiltInRegistries.ITEM.getKey(item);
		return key != null ? key.toString() : null;
	}

	/** Human-readable label for GUI lists. */
	public static String describe(String ref) {
		ContentManager content = ContentManager.get();
		if (content.item(ref) != null) return content.item(ref).displayName;
		if (content.block(ref) != null) return content.block(ref).displayName;
		return ref;
	}
}
