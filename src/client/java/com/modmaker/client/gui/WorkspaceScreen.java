package com.modmaker.client.gui;

import com.modmaker.ModMaker;
import com.modmaker.content.BlockDefinition;
import com.modmaker.content.ContentManager;
import com.modmaker.content.ItemDefinition;
import com.modmaker.content.RecipeDefinition;
import com.modmaker.io.MmpackIO;
import com.modmaker.pack.RuntimeDataPack;
import com.modmaker.registry.ItemRefs;
import com.modmaker.registry.PlaceholderPool;
import com.modmaker.script.ScriptManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The ModMaker workshop: lists every user-created element and is the hub for
 * creating, editing, importing, exporting and reloading content.
 */
public class WorkspaceScreen extends BaseScreen {
	private static final int PAGE_SIZE = 6;

	private record Entry(String kind, String id, Component label, ItemStack icon, Runnable openAction, Runnable deleteAction) {
	}

	private final List<Entry> entries = new ArrayList<>();
	private int page;
	private String pendingDelete;
	private Component status = Component.empty();
	private int statusColor = GREEN;

	public WorkspaceScreen(Screen parent) {
		super(Component.translatable("modmaker.gui.workspace.title"), parent);
	}

	@Override
	protected void init() {
		super.init();
		rebuildEntries();

		int cx = width / 2;
		int listX = cx - 150;
		int listY = 36;

		int maxPage = Math.max(0, (entries.size() - 1) / PAGE_SIZE);
		page = Math.min(page, maxPage);

		if (entries.isEmpty()) {
			label(Component.translatable("modmaker.gui.workspace.empty"), listX + 24, listY + 30);
		}

		for (int i = 0; i < PAGE_SIZE; i++) {
			int index = page * PAGE_SIZE + i;
			if (index >= entries.size()) break;
			Entry entry = entries.get(index);
			int y = listY + i * 24;
			addButton(listX + 24, y, 226, 20, entry.label(), entry.openAction());
			boolean confirming = entry.id().equals(pendingDelete);
			addButton(listX + 254, y, 42, 20,
					Component.translatable(confirming ? "modmaker.gui.confirm_delete" : "modmaker.gui.delete"),
					() -> handleDelete(entry));
		}

		if (page > 0) {
			addButton(listX + 300, listY, 20, 20, Component.literal("^"), () -> {
				page--;
				rebuildWidgets();
			});
		}
		if ((page + 1) * PAGE_SIZE < entries.size()) {
			addButton(listX + 300, listY + (PAGE_SIZE - 1) * 24, 20, 20, Component.literal("v"), () -> {
				page++;
				rebuildWidgets();
			});
		}

		int rowY1 = height - 52;
		int rowY2 = height - 28;
		int bw = 82;
		int bx = cx - 2 * bw - 9;

		addButton(bx, rowY1, bw, 20, Component.translatable("modmaker.gui.new_item"),
				() -> open(new ItemEditorScreen(this, null)));
		addButton(bx + bw + 6, rowY1, bw, 20, Component.translatable("modmaker.gui.new_block"),
				() -> open(new BlockEditorScreen(this, null)));
		addButton(bx + 2 * (bw + 6), rowY1, bw, 20, Component.translatable("modmaker.gui.new_recipe"),
				() -> open(new RecipeEditorScreen(this, null)));
		addButton(bx + 3 * (bw + 6), rowY1, bw, 20, Component.translatable("modmaker.gui.scripts"),
				() -> open(new ScriptEditorScreen(this)));

		addButton(bx, rowY2, bw, 20, Component.translatable("modmaker.gui.import"), this::startImport);
		addButton(bx + bw + 6, rowY2, bw, 20, Component.translatable("modmaker.gui.export"), this::exportAll);
		addButton(bx + 2 * (bw + 6), rowY2, bw, 20, Component.translatable("modmaker.gui.reload"), this::reloadAll);
		addButton(bx + 3 * (bw + 6), rowY2, bw, 20, Component.translatable("gui.done"), this::onClose);

		label(status, listX + 24, height - 66, statusColor);
	}

	private void rebuildEntries() {
		entries.clear();
		ContentManager content = ContentManager.get();
		for (ItemDefinition def : content.allItems()) {
			Integer slot = content.itemSlot(def.id);
			ItemStack icon = slot != null ? new ItemStack(PlaceholderPool.ITEMS[slot]) : ItemStack.EMPTY;
			entries.add(new Entry("item", def.id,
					Component.translatable("modmaker.gui.entry.item", def.displayName),
					icon,
					() -> open(new ItemEditorScreen(this, def)),
					() -> content.deleteItem(def.id)));
		}
		for (BlockDefinition def : content.allBlocks()) {
			Integer slot = content.blockSlot(def.id);
			ItemStack icon = slot != null ? new ItemStack(PlaceholderPool.BLOCK_ITEMS[slot]) : ItemStack.EMPTY;
			entries.add(new Entry("block", def.id,
					Component.translatable("modmaker.gui.entry.block", def.displayName),
					icon,
					() -> open(new BlockEditorScreen(this, def)),
					() -> content.deleteBlock(def.id)));
		}
		for (RecipeDefinition def : content.allRecipes()) {
			ItemStack icon = ItemRefs.createStack(def.resultItem, 1);
			entries.add(new Entry("recipe", "recipe/" + def.id,
					Component.translatable("modmaker.gui.entry.recipe", def.id),
					icon,
					() -> open(new RecipeEditorScreen(this, def)),
					() -> {
						content.deleteRecipe(def.id);
						refreshServerData();
					}));
		}
	}

	private void handleDelete(Entry entry) {
		if (entry.id().equals(pendingDelete)) {
			pendingDelete = null;
			entry.deleteAction().run();
			setStatus(Component.translatable("modmaker.gui.deleted"), GREEN);
		} else {
			pendingDelete = entry.id();
		}
		rebuildWidgets();
	}

	private void startImport() {
		open(new FileBrowserScreen(this, List.of("png", "js", "json", "mmpack", "zip"), path -> {
			Component result = com.modmaker.io.ImportHelper.importFile(path);
			refreshServerData();
			setStatus(result, GREEN);
			rebuildWidgets();
		}));
	}

	private void exportAll() {
		try {
			Path file = MmpackIO.exportAll();
			setStatus(Component.translatable("modmaker.gui.exported", file.toString()), GREEN);
		} catch (Exception e) {
			ModMaker.LOGGER.error("ModMaker export failed", e);
			setStatus(Component.translatable("modmaker.gui.export_failed", e.getMessage()), RED);
		}
		rebuildWidgets();
	}

	private void reloadAll() {
		ContentManager.get().loadAll();
		ScriptManager.get().reload();
		refreshServerData();
		ContentManager.get().fireChanged();
		int errors = ScriptManager.get().errors().size();
		if (errors > 0) {
			setStatus(Component.translatable("modmaker.gui.reloaded_errors", errors), RED);
		} else {
			setStatus(Component.translatable("modmaker.gui.reloaded"), GREEN);
		}
		rebuildWidgets();
	}

	private void refreshServerData() {
		MinecraftServer server = ModMaker.server();
		if (server != null) {
			server.execute(() -> RuntimeDataPack.rewriteAndReload(server));
		}
	}

	private void setStatus(Component text, int color) {
		status = text;
		statusColor = color;
	}

	@Override
	protected void extractExtra(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int listX = width / 2 - 150;
		int listY = 36;
		for (int i = 0; i < PAGE_SIZE; i++) {
			int index = page * PAGE_SIZE + i;
			if (index >= entries.size()) break;
			Entry entry = entries.get(index);
			if (!entry.icon().isEmpty()) {
				graphics.item(entry.icon(), listX + 2, listY + i * 24 + 2);
			}
		}
	}
}
