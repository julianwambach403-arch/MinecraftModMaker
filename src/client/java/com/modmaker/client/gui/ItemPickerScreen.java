package com.modmaker.client.gui;

import com.modmaker.content.BlockDefinition;
import com.modmaker.content.ContentManager;
import com.modmaker.content.ItemDefinition;
import com.modmaker.registry.ItemRefs;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Searchable item selector. Lists ModMaker elements first, then every registered item.
 * The callback receives a reference string (definition id or registry id) or null for "none".
 */
public class ItemPickerScreen extends BaseScreen {
	private static final int PAGE_SIZE = 7;

	private record Entry(String ref, Component label, ItemStack icon) {
	}

	private final Consumer<String> callback;
	private final List<Entry> allEntries = new ArrayList<>();
	private final List<Entry> filtered = new ArrayList<>();
	private EditBox searchBox;
	private String query = "";
	private int page;

	public ItemPickerScreen(Screen parent, Consumer<String> callback) {
		super(Component.translatable("modmaker.gui.picker.title"), parent);
		this.callback = callback;
	}

	@Override
	protected void init() {
		super.init();
		if (allEntries.isEmpty()) {
			buildEntries();
		}
		int cx = width / 2;

		searchBox = addEditBox(cx - 120, 30, 200, 18, query, Component.translatable("modmaker.gui.picker.search"));
		searchBox.setResponder(value -> {
			query = value;
			page = 0;
			applyFilter();
			rebuildWidgets();
		});

		addButton(cx + 86, 29, 60, 20, Component.translatable("modmaker.gui.picker.none"), () -> {
			callback.accept(null);
			onClose();
		});

		applyFilter();
		int listY = 56;
		for (int i = 0; i < PAGE_SIZE; i++) {
			int index = page * PAGE_SIZE + i;
			if (index >= filtered.size()) break;
			Entry entry = filtered.get(index);
			addButton(cx - 96, listY + i * 24, 216, 20, entry.label(), () -> {
				callback.accept(entry.ref());
				onClose();
			});
		}

		if (page > 0) {
			addButton(cx + 126, listY, 20, 20, Component.literal("^"), () -> {
				page--;
				rebuildWidgets();
			});
		}
		if ((page + 1) * PAGE_SIZE < filtered.size()) {
			addButton(cx + 126, listY + (PAGE_SIZE - 1) * 24, 20, 20, Component.literal("v"), () -> {
				page++;
				rebuildWidgets();
			});
		}

		addButton(cx - 40, height - 26, 80, 20, Component.translatable("gui.cancel"), this::onClose);
	}

	private void buildEntries() {
		ContentManager content = ContentManager.get();
		for (ItemDefinition def : content.allItems()) {
			ItemStack icon = ItemRefs.createStack(def.id, 1);
			allEntries.add(new Entry(def.id,
					Component.literal(def.displayName + " (" + def.id + ")"), icon));
		}
		for (BlockDefinition def : content.allBlocks()) {
			ItemStack icon = ItemRefs.createStack(def.id, 1);
			allEntries.add(new Entry(def.id,
					Component.literal(def.displayName + " (" + def.id + ")"), icon));
		}
		for (Item item : BuiltInRegistries.ITEM) {
			if (item == Items.AIR) continue;
			Identifier key = BuiltInRegistries.ITEM.getKey(item);
			if (key == null || "modmaker".equals(key.getNamespace())) continue;
			ItemStack stack = new ItemStack(item);
			allEntries.add(new Entry(key.toString(),
					Component.literal(stack.getHoverName().getString() + " (" + key + ")"), stack));
		}
	}

	private void applyFilter() {
		filtered.clear();
		String needle = query.toLowerCase(Locale.ROOT).trim();
		for (Entry entry : allEntries) {
			if (needle.isEmpty()
					|| entry.ref().toLowerCase(Locale.ROOT).contains(needle)
					|| entry.label().getString().toLowerCase(Locale.ROOT).contains(needle)) {
				filtered.add(entry);
			}
		}
	}

	@Override
	protected void extractExtra(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int cx = width / 2;
		int listY = 56;
		for (int i = 0; i < PAGE_SIZE; i++) {
			int index = page * PAGE_SIZE + i;
			if (index >= filtered.size()) break;
			Entry entry = filtered.get(index);
			if (!entry.icon().isEmpty()) {
				graphics.item(entry.icon(), cx - 118, listY + i * 24 + 2);
			}
		}
	}
}
