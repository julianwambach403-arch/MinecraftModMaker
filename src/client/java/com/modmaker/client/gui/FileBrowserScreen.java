package com.modmaker.client.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Minimal in-game file browser used for importing files (textures, scripts, packs).
 * Navigates the real filesystem starting from common locations (game folder, home,
 * desktop, downloads) and only offers files matching the wanted extensions.
 */
public class FileBrowserScreen extends BaseScreen {
	private static final int PAGE_SIZE = 6;

	private final List<String> extensions;
	private final Consumer<Path> callback;
	private Path currentDir;
	private final List<Path> listing = new ArrayList<>();
	private int page;

	public FileBrowserScreen(Screen parent, List<String> extensions, Consumer<Path> callback) {
		super(Component.translatable("modmaker.gui.files.title"), parent);
		this.extensions = extensions.stream().map(e -> e.toLowerCase(Locale.ROOT)).toList();
		this.callback = callback;
		this.currentDir = FabricLoader.getInstance().getGameDir();
	}

	@Override
	protected void init() {
		super.init();
		int cx = width / 2;

		// quick access row
		int qy = 28;
		addButton(cx - 160, qy, 76, 18, Component.translatable("modmaker.gui.files.game"), () ->
				navigate(FabricLoader.getInstance().getGameDir()));
		Path home = Path.of(System.getProperty("user.home", "."));
		addButton(cx - 80, qy, 76, 18, Component.translatable("modmaker.gui.files.home"), () -> navigate(home));
		Path desktop = home.resolve("Desktop");
		if (Files.isDirectory(desktop)) {
			addButton(cx, qy, 76, 18, Component.translatable("modmaker.gui.files.desktop"), () -> navigate(desktop));
		}
		Path downloads = home.resolve("Downloads");
		if (Files.isDirectory(downloads)) {
			addButton(cx + 80, qy, 76, 18, Component.translatable("modmaker.gui.files.downloads"), () -> navigate(downloads));
		}

		String pathText = currentDir.toString();
		if (pathText.length() > 52) {
			pathText = "..." + pathText.substring(pathText.length() - 49);
		}
		label(Component.literal(pathText), cx - 160, 52);

		refreshListing();
		int listY = 64;
		int row = 0;

		if (row < PAGE_SIZE && page == 0 && currentDir.getParent() != null) {
			addButton(cx - 160, listY, 296, 20, Component.literal("[..]"), () -> navigate(currentDir.getParent()));
			row++;
		}

		int offset = page * PAGE_SIZE - (page > 0 && currentDir.getParent() != null ? 1 : 0);
		for (int index = Math.max(0, offset); row < PAGE_SIZE && index < listing.size(); index++, row++) {
			Path path = listing.get(index);
			boolean dir = Files.isDirectory(path);
			String name = (dir ? "[DIR] " : "") + path.getFileName();
			if (name.length() > 48) name = name.substring(0, 45) + "...";
			addButton(cx - 160, listY + row * 24, 296, 20, Component.literal(name), () -> {
				if (dir) {
					navigate(path);
				} else {
					callback.accept(path);
					onClose();
				}
			});
		}

		if (page > 0) {
			addButton(cx + 142, listY, 20, 20, Component.literal("^"), () -> {
				page--;
				rebuildWidgets();
			});
		}
		if ((page + 1) * PAGE_SIZE < listing.size() + 1) {
			addButton(cx + 142, listY + (PAGE_SIZE - 1) * 24, 20, 20, Component.literal("v"), () -> {
				page++;
				rebuildWidgets();
			});
		}

		addButton(cx - 40, height - 26, 80, 20, Component.translatable("gui.cancel"), this::onClose);
	}

	private void navigate(Path dir) {
		currentDir = dir;
		page = 0;
		rebuildWidgets();
	}

	private void refreshListing() {
		listing.clear();
		try (Stream<Path> stream = Files.list(currentDir)) {
			stream.filter(path -> {
				String name = path.getFileName().toString();
				if (name.startsWith(".")) return false;
				if (Files.isDirectory(path)) return true;
				int dot = name.lastIndexOf('.');
				return dot >= 0 && extensions.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
			}).sorted((a, b) -> {
				boolean dirA = Files.isDirectory(a);
				boolean dirB = Files.isDirectory(b);
				if (dirA != dirB) return dirA ? -1 : 1;
				return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
			}).forEach(listing::add);
		} catch (IOException | SecurityException ignored) {
		}
	}
}
