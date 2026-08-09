package com.modmaker.client.gui;

import com.modmaker.ModMakerPaths;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Shows all PNG files from {@code modmaker/textures/} with live previews.
 * "Import..." opens the OS file browser to copy new PNGs into the folder.
 */
public class TexturePickerScreen extends BaseScreen {
	private static final int PAGE_SIZE = 6;

	private final Consumer<String> callback;
	private final List<String> textures = new ArrayList<>();
	private int page;

	public TexturePickerScreen(Screen parent, Consumer<String> callback) {
		super(Component.translatable("modmaker.gui.texture.title"), parent);
		this.callback = callback;
	}

	@Override
	protected void init() {
		super.init();
		refreshList();
		int cx = width / 2;

		addButton(cx - 130, 30, 120, 20, Component.translatable("modmaker.gui.texture.import"), () ->
				open(new FileBrowserScreen(this, List.of("png"), this::importTexture)));
		addButton(cx - 4, 30, 120, 20, Component.translatable("modmaker.gui.texture.none"), () -> {
			callback.accept(null);
			onClose();
		});

		if (textures.isEmpty()) {
			label(Component.translatable("modmaker.gui.texture.empty"), cx - 130, 66);
		}

		int listY = 56;
		for (int i = 0; i < PAGE_SIZE; i++) {
			int index = page * PAGE_SIZE + i;
			if (index >= textures.size()) break;
			String name = textures.get(index);
			addButton(cx - 102, listY + i * 24, 210, 20, Component.literal(name), () -> {
				callback.accept(name);
				onClose();
			});
		}

		if (page > 0) {
			addButton(cx + 114, listY, 20, 20, Component.literal("^"), () -> {
				page--;
				rebuildWidgets();
			});
		}
		if ((page + 1) * PAGE_SIZE < textures.size()) {
			addButton(cx + 114, listY + (PAGE_SIZE - 1) * 24, 20, 20, Component.literal("v"), () -> {
				page++;
				rebuildWidgets();
			});
		}

		addButton(cx - 40, height - 26, 80, 20, Component.translatable("gui.cancel"), this::onClose);
	}

	private void refreshList() {
		textures.clear();
		Path dir = ModMakerPaths.textures();
		if (!Files.isDirectory(dir)) return;
		try (Stream<Path> stream = Files.list(dir)) {
			stream.map(p -> p.getFileName().toString())
					.filter(name -> name.toLowerCase().endsWith(".png"))
					.sorted()
					.forEach(textures::add);
		} catch (IOException ignored) {
		}
	}

	private void importTexture(Path source) {
		try {
			Files.createDirectories(ModMakerPaths.textures());
			Path target = ModMakerPaths.textures().resolve(source.getFileName().toString());
			Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			page = 0;
		} catch (IOException e) {
			com.modmaker.ModMaker.LOGGER.error("ModMaker could not import texture {}", source, e);
		}
		rebuildWidgets();
	}

	@Override
	protected void extractExtra(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int cx = width / 2;
		int listY = 56;
		for (int i = 0; i < PAGE_SIZE; i++) {
			int index = page * PAGE_SIZE + i;
			if (index >= textures.size()) break;
			TexturePreviews.Preview preview = TexturePreviews.get(textures.get(index));
			if (preview != null) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, preview.id(), cx - 126, listY + i * 24 + 1,
						0, 0, 18, 18, preview.width(), preview.height(), preview.width(), preview.height());
			}
		}
	}
}
