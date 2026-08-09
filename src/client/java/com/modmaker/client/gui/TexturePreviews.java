package com.modmaker.client.gui;

import com.modmaker.ModMaker;
import com.modmaker.ModMakerPaths;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loads PNG files from {@code modmaker/textures/} as dynamic textures so the GUI can show
 * live previews of imported textures (no resource reload required for previews).
 */
public final class TexturePreviews {
	public record Preview(Identifier id, int width, int height) {
	}

	private record CacheKey(long size, long mtime) {
	}

	private static final Map<String, Preview> LOADED = new HashMap<>();
	private static final Map<String, CacheKey> KEYS = new HashMap<>();

	private TexturePreviews() {
	}

	public static Preview get(String fileName) {
		if (fileName == null || fileName.isBlank()) return null;
		Path file = ModMakerPaths.textures().resolve(fileName);
		if (!Files.isRegularFile(file)) return null;

		try {
			CacheKey key = new CacheKey(Files.size(file), Files.getLastModifiedTime(file).toMillis());
			if (key.equals(KEYS.get(fileName))) {
				return LOADED.get(fileName);
			}

			NativeImage image = NativeImage.read(Files.readAllBytes(file));
			Identifier id = Identifier.fromNamespaceAndPath("modmaker",
					"preview/" + fileName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_"));
			Minecraft.getInstance().getTextureManager().register(id,
					new DynamicTexture(() -> "modmaker preview " + fileName, image));
			Preview preview = new Preview(id, image.getWidth(), image.getHeight());
			LOADED.put(fileName, preview);
			KEYS.put(fileName, key);
			return preview;
		} catch (Exception e) {
			ModMaker.LOGGER.warn("ModMaker could not preview texture {}: {}", fileName, e.toString());
			return null;
		}
	}
}
