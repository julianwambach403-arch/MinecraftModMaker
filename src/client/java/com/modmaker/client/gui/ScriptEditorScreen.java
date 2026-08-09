package com.modmaker.client.gui;

import com.modmaker.ModMaker;
import com.modmaker.ModMakerPaths;
import com.modmaker.script.ScriptManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * In-game JavaScript editor with hot reload. Script errors are collected by the
 * {@link ScriptManager} and shown at the bottom instead of crashing the game.
 */
public class ScriptEditorScreen extends BaseScreen {
	private static final String TEMPLATE = """
			// ModMaker Script - JavaScript
			// Beispiel / example:
			//   mm.onUse("mein_item", function(player, x, y, z) {
			//       player.tell("Hallo " + player.name() + "!");
			//   });
			//   mm.onTick(100, function() { mm.broadcast("5 Sekunden..."); });

			console.log("Script geladen!");
			""";

	private List<Path> scripts = List.of();
	private Path currentFile;
	private MultiLineEditBox editor;
	private String pendingContent = "";
	private Component status = Component.empty();
	private int statusColor = GREEN;

	public ScriptEditorScreen(Screen parent) {
		super(Component.translatable("modmaker.gui.scripts.title"), parent);
	}

	@Override
	protected void init() {
		super.init();
		scripts = ScriptManager.get().listScripts();
		if (currentFile == null && !scripts.isEmpty()) {
			currentFile = scripts.get(0);
			pendingContent = readFile(currentFile);
		}

		int leftX = 10;
		int y = 30;
		addButton(leftX, y, 110, 20, Component.translatable("modmaker.gui.scripts.new"), this::createScript);
		y += 24;

		for (Path script : scripts) {
			if (y > height - 60) break;
			final Path file = script;
			String name = script.getFileName().toString();
			if (name.length() > 16) name = name.substring(0, 13) + "...";
			Component label = file.equals(currentFile)
					? Component.literal("> " + name)
					: Component.literal(name);
			addButton(leftX, y, 110, 20, label, () -> {
				saveCurrent(false);
				currentFile = file;
				pendingContent = readFile(file);
				rebuildWidgets();
			});
			y += 22;
		}

		int editorX = 128;
		int editorWidth = width - editorX - 10;
		int editorHeight = height - 92;
		editor = addRenderableWidget(MultiLineEditBox.builder()
				.setX(editorX)
				.setY(30)
				.build(font, editorWidth, editorHeight, Component.translatable("modmaker.gui.scripts.editor")));
		editor.setCharacterLimit(100000);
		editor.setValue(pendingContent);
		editor.setValueListener(value -> pendingContent = value);

		int by = height - 54;
		addButton(editorX, by, 110, 20, Component.translatable("modmaker.gui.save"), () -> {
			saveCurrent(true);
			rebuildWidgets();
		});
		addButton(editorX + 114, by, 150, 20, Component.translatable("modmaker.gui.scripts.save_reload"), () -> {
			saveCurrent(true);
			ScriptManager.get().reload();
			List<String> errors = ScriptManager.get().errors();
			if (errors.isEmpty()) {
				status = Component.translatable("modmaker.gui.scripts.reloaded");
				statusColor = GREEN;
			} else {
				status = Component.literal(errors.get(errors.size() - 1));
				statusColor = RED;
			}
			rebuildWidgets();
		});
		addButton(width - 90, by, 80, 20, Component.translatable("gui.back"), () -> {
			saveCurrent(false);
			onClose();
		});

		label(status, editorX, height - 30, statusColor);
	}

	private void createScript() {
		saveCurrent(false);
		try {
			Files.createDirectories(ModMakerPaths.scripts());
			int n = 1;
			Path file;
			do {
				file = ModMakerPaths.scripts().resolve("script_" + n + ".js");
				n++;
			} while (Files.exists(file));
			Files.writeString(file, TEMPLATE);
			currentFile = file;
			pendingContent = TEMPLATE;
		} catch (IOException e) {
			ModMaker.LOGGER.error("ModMaker could not create script", e);
		}
		rebuildWidgets();
	}

	private void saveCurrent(boolean notify) {
		if (currentFile == null) return;
		try {
			Files.writeString(currentFile, pendingContent);
			if (notify) {
				status = Component.translatable("modmaker.gui.scripts.saved", currentFile.getFileName().toString());
				statusColor = GREEN;
			}
		} catch (IOException e) {
			status = Component.literal("Save failed: " + e.getMessage());
			statusColor = RED;
			ModMaker.LOGGER.error("ModMaker could not save script", e);
		}
	}

	private String readFile(Path file) {
		try {
			return Files.readString(file);
		} catch (IOException e) {
			return "";
		}
	}

	@Override
	protected void extractExtra(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		if (currentFile == null) {
			graphics.text(font, Component.translatable("modmaker.gui.scripts.none"), 128, 34, GRAY);
		}
	}

	@Override
	public void onClose() {
		saveCurrent(false);
		super.onClose();
	}
}
