package com.modmaker.registry;

import com.modmaker.content.BlockDefinition;
import com.modmaker.content.ContentManager;
import com.modmaker.content.ItemDefinition;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Registers a fixed pool of placeholder items and blocks while registries are still open.
 * User definitions are bound to pool slots at runtime, which is how ModMaker creates
 * "new" items and blocks without a game restart (registries freeze after startup).
 */
public final class PlaceholderPool {
	public static final int ITEM_SLOTS = 128;
	public static final int BLOCK_SLOTS = 64;

	public static final DynamicItem[] ITEMS = new DynamicItem[ITEM_SLOTS];
	public static final DynamicBlock[] BLOCKS = new DynamicBlock[BLOCK_SLOTS];
	public static final DynamicBlockItem[] BLOCK_ITEMS = new DynamicBlockItem[BLOCK_SLOTS];

	public static final ResourceKey<CreativeModeTab> TAB_KEY =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("modmaker", "modmaker"));

	private PlaceholderPool() {
	}

	public static void registerAll() {
		for (int i = 0; i < ITEM_SLOTS; i++) {
			Identifier id = Identifier.fromNamespaceAndPath("modmaker", "item_" + i);
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
			ITEMS[i] = Registry.register(BuiltInRegistries.ITEM, key,
					new DynamicItem(i, new Item.Properties().setId(key)));
		}

		for (int i = 0; i < BLOCK_SLOTS; i++) {
			Identifier id = Identifier.fromNamespaceAndPath("modmaker", "block_" + i);
			ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
			BLOCKS[i] = Registry.register(BuiltInRegistries.BLOCK, blockKey,
					new DynamicBlock(i, BlockBehaviour.Properties.of()
							.setId(blockKey)
							.strength(1.5f, 6.0f)
							.lightLevel(state -> state.getValue(DynamicBlock.LIGHT))
							.noLootTable()));

			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
			BLOCK_ITEMS[i] = Registry.register(BuiltInRegistries.ITEM, itemKey,
					new DynamicBlockItem(BLOCKS[i], new Item.Properties()
							.setId(itemKey)
							.useBlockDescriptionPrefix()));
		}

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY,
				CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
						.title(Component.translatable("itemGroup.modmaker"))
						.icon(PlaceholderPool::tabIcon)
						.displayItems((parameters, output) -> {
							ContentManager content = ContentManager.get();
							for (ItemDefinition def : content.allItems()) {
								Integer slot = content.itemSlot(def.id);
								if (slot != null) output.accept(ITEMS[slot].getDefaultInstance());
							}
							for (BlockDefinition def : content.allBlocks()) {
								Integer slot = content.blockSlot(def.id);
								if (slot != null) output.accept(new ItemStack(BLOCK_ITEMS[slot]));
							}
						})
						.build());
	}

	private static ItemStack tabIcon() {
		ContentManager content = ContentManager.get();
		for (ItemDefinition def : content.allItems()) {
			Integer slot = content.itemSlot(def.id);
			if (slot != null) return new ItemStack(ITEMS[slot]);
		}
		return new ItemStack(Items.CRAFTING_TABLE);
	}
}
