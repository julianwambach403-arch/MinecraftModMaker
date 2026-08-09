package com.modmaker;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Central place for all filesystem locations used by ModMaker.
 * Everything lives under {@code <gameDir>/modmaker/} so content is shared between worlds
 * and easy to back up or edit externally.
 */
public final class ModMakerPaths {
	private ModMakerPaths() {
	}

	public static Path root() {
		return FabricLoader.getInstance().getGameDir().resolve("modmaker");
	}

	public static Path items() {
		return root().resolve("items");
	}

	public static Path blocks() {
		return root().resolve("blocks");
	}

	public static Path recipes() {
		return root().resolve("recipes");
	}

	public static Path scripts() {
		return root().resolve("scripts");
	}

	public static Path textures() {
		return root().resolve("textures");
	}

	public static Path exports() {
		return root().resolve("exports");
	}

	public static Path bindingsFile() {
		return root().resolve("bindings.json");
	}

	/** Directory the generated client resource pack is written to. */
	public static Path runtimeResourcePack() {
		return FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve("ModMakerRuntime");
	}

	public static void createAll() {
		try {
			Files.createDirectories(items());
			Files.createDirectories(blocks());
			Files.createDirectories(recipes());
			Files.createDirectories(scripts());
			Files.createDirectories(textures());
			Files.createDirectories(exports());
		} catch (IOException e) {
			throw new RuntimeException("ModMaker could not create its data directories", e);
		}
	}
}
