package com.modmaker.io;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.modmaker.ModMaker;
import com.modmaker.ModMakerPaths;
import com.modmaker.content.ContentManager;
import com.modmaker.script.ScriptManager;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Single entry point for the GUI "Import" action: dispatches a picked file by extension
 * (texture, script, definition JSON or whole .mmpack) and refreshes all runtime state.
 */
public final class ImportHelper {
	private static final Gson GSON = new Gson();

	private ImportHelper() {
	}

	/** Imports the file and returns a user-facing status message. */
	public static Component importFile(Path file) {
		try {
			String name = file.getFileName().toString();
			String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
			Component result = switch (extension) {
				case "png" -> importTexture(file);
				case "js" -> importScript(file);
				case "json" -> importJson(file);
				case "mmpack", "zip" -> importPack(file);
				default -> Component.translatable("modmaker.import.unsupported", extension);
			};
			ContentManager.get().loadAll();
			ScriptManager.get().reload();
			ContentManager.get().fireChanged();
			return result;
		} catch (Exception e) {
			ModMaker.LOGGER.error("ModMaker import of {} failed", file, e);
			return Component.translatable("modmaker.import.failed", String.valueOf(e.getMessage()));
		}
	}

	private static Component importTexture(Path file) throws Exception {
		Files.createDirectories(ModMakerPaths.textures());
		Files.copy(file, ModMakerPaths.textures().resolve(file.getFileName().toString()),
				StandardCopyOption.REPLACE_EXISTING);
		return Component.translatable("modmaker.import.texture", file.getFileName().toString());
	}

	private static Component importScript(Path file) throws Exception {
		Files.createDirectories(ModMakerPaths.scripts());
		Files.copy(file, ModMakerPaths.scripts().resolve(file.getFileName().toString()),
				StandardCopyOption.REPLACE_EXISTING);
		return Component.translatable("modmaker.import.script", file.getFileName().toString());
	}

	private static Component importJson(Path file) throws Exception {
		JsonObject json = GSON.fromJson(Files.readString(file), JsonObject.class);
		if (json == null || !json.has("id")) {
			return Component.translatable("modmaker.import.invalid_json");
		}
		String id = ContentManager.sanitizeId(json.get("id").getAsString());
		String folder;
		if (json.has("hardness")) {
			folder = "blocks";
		} else if (json.has("grid") || json.has("resultItem")) {
			folder = "recipes";
		} else {
			folder = "items";
		}
		Path targetDir = ModMakerPaths.root().resolve(folder);
		Files.createDirectories(targetDir);
		Files.copy(file, targetDir.resolve(id + ".json"), StandardCopyOption.REPLACE_EXISTING);
		return Component.translatable("modmaker.import.element", id);
	}

	private static Component importPack(Path file) throws Exception {
		int count = MmpackIO.importPack(file);
		return Component.translatable("modmaker.import.pack", count);
	}
}
