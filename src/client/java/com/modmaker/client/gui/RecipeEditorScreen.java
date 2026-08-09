package com.modmaker.client.gui;

import com.modmaker.ModMaker;
import com.modmaker.content.ContentManager;
import com.modmaker.content.RecipeDefinition;
import com.modmaker.pack.RuntimeDataPack;
import com.modmaker.registry.ItemRefs;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Visual 3x3 crafting recipe editor. Saving writes the recipe JSON and reloads the
 * runtime data pack, so the recipe works immediately in the running world.
 */
public class RecipeEditorScreen extends BaseScreen {
	private final RecipeDefinition original;
	private final RecipeDefinition def;
	private final boolean isNew;

	private EditBox idBox;
	private EditBox countBox;
	private Button resultButton;
	private final List<Button> gridButtons = new ArrayList<>();

	private int gridX;
	private int gridY;

	public RecipeEditorScreen(WorkspaceScreen parent, RecipeDefinition existing) {
		super(Component.translatable(existing == null ? "modmaker.gui.recipe.title_new" : "modmaker.gui.recipe.title_edit"), parent);
		this.original = existing;
		this.isNew = existing == null;
		this.def = existing == null ? new RecipeDefinition() : copy(existing);
		while (def.grid.size() < 9) def.grid.add("");
	}

	private static RecipeDefinition copy(RecipeDefinition source) {
		RecipeDefinition copy = new RecipeDefinition();
		copy.id = source.id;
		copy.type = source.type;
		copy.grid = new ArrayList<>(source.grid);
		copy.resultItem = source.resultItem;
		copy.resultCount = source.resultCount;
		copy.version = source.version;
		return copy;
	}

	@Override
	protected void init() {
		super.init();
		gridButtons.clear();
		int cx = width / 2;
		gridX = cx - 140;
		gridY = 62;

		label(Component.translatable("modmaker.gui.field.id"), cx - 140, 36 + 6);
		idBox = addEditBox(cx - 80, 36, 90, 20, def.id, Component.literal("id"));
		if (!isNew) idBox.setEditable(false);

		addCycleButton(cx + 20, 36, 120, 20, Component.translatable("modmaker.gui.field.recipe_type"),
				List.of("shaped", "shapeless"), def.type,
				value -> Component.translatable("modmaker.gui.recipe." + value),
				value -> def.type = value);

		for (int i = 0; i < 9; i++) {
			final int index = i;
			int x = gridX + (i % 3) * 26;
			int y = gridY + (i / 3) * 26;
			Button cell = addButton(x, y, 24, 24, Component.empty(), () ->
					open(new ItemPickerScreen(this, ref -> def.grid.set(index, ref == null ? "" : ref))));
			gridButtons.add(cell);
		}

		label(Component.literal("->"), cx - 44, gridY + 32);

		resultButton = addButton(cx - 20, gridY + 26, 24, 24, Component.empty(), () ->
				open(new ItemPickerScreen(this, ref -> def.resultItem = ref == null ? "" : ref)));

		label(Component.translatable("modmaker.gui.field.count"), cx + 14, gridY + 32);
		countBox = addEditBox(cx + 64, gridY + 26, 40, 20, String.valueOf(def.resultCount), Component.literal("1"));

		label(Component.translatable("modmaker.gui.recipe.hint"), gridX, gridY + 88);

		int by = height - 28;
		addButton(cx - 130, by, 80, 20, Component.translatable("modmaker.gui.save"), this::save);
		if (!isNew) {
			addButton(cx - 42, by, 80, 20, Component.translatable("modmaker.gui.delete"), () -> {
				ContentManager.get().deleteRecipe(original.id);
				refreshServerData();
				onClose();
			});
		}
		addButton(cx + 46, by, 80, 20, Component.translatable("gui.cancel"), this::onClose);
	}

	private void save() {
		def.id = isNew ? ContentManager.sanitizeId(idBox.getValue()) : original.id;
		def.resultCount = Math.max(1, Math.min(99, parseInt(countBox.getValue(), 1)));
		ContentManager.get().saveRecipe(def);
		refreshServerData();
		onClose();
	}

	private void refreshServerData() {
		MinecraftServer server = ModMaker.server();
		if (server != null) {
			server.execute(() -> RuntimeDataPack.rewriteAndReload(server));
		}
	}

	@Override
	protected void extractExtra(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		for (int i = 0; i < 9; i++) {
			String ref = def.grid.get(i);
			if (ref == null || ref.isBlank()) continue;
			ItemStack stack = ItemRefs.createStack(ref, 1);
			if (!stack.isEmpty()) {
				graphics.item(stack, gridX + (i % 3) * 26 + 4, gridY + (i / 3) * 26 + 4);
			}
		}
		if (def.resultItem != null && !def.resultItem.isBlank()) {
			ItemStack stack = ItemRefs.createStack(def.resultItem, 1);
			if (!stack.isEmpty()) {
				graphics.item(stack, width / 2 - 16, gridY + 30);
			}
		}
	}
}
