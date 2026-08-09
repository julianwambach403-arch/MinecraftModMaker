package com.modmaker.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.modmaker.ModMaker;
import com.modmaker.ModMakerPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.stream.Stream;

/**
 * Export/import of the whole workspace as a shareable {@code .mmpack} file (a ZIP with
 * {@code manifest.json} plus the items/blocks/recipes/scripts/textures folders).
 */
public final class MmpackIO {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final List<String> FOLDERS = List.of("items", "blocks", "recipes", "scripts", "textures");

	private MmpackIO() {
	}

	/** Zips the whole workspace into {@code modmaker/exports/} and returns the file path. */
	public static Path exportAll() throws IOException {
		Files.createDirectories(ModMakerPaths.exports());
		String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
		Path target = ModMakerPaths.exports().resolve("modmaker-" + stamp + ".mmpack");

		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
			JsonObject manifest = new JsonObject();
			manifest.addProperty("format", 1);
			manifest.addProperty("game_version", "26.2");
			manifest.addProperty("created", stamp);
			zip.putNextEntry(new ZipEntry("manifest.json"));
			zip.write(GSON.toJson(manifest).getBytes());
			zip.closeEntry();

			for (String folder : FOLDERS) {
				Path dir = ModMakerPaths.root().resolve(folder);
				if (!Files.isDirectory(dir)) continue;
				try (Stream<Path> stream = Files.list(dir)) {
					for (Path file : stream.filter(Files::isRegularFile).sorted().toList()) {
						zip.putNextEntry(new ZipEntry(folder + "/" + file.getFileName()));
						try (InputStream in = Files.newInputStream(file)) {
							in.transferTo(zip);
						}
						zip.closeEntry();
					}
				}
			}
		}
		ModMaker.LOGGER.info("ModMaker exported workspace to {}", target);
		return target;
	}

	/**
	 * Extracts a .mmpack/.zip into the workspace (merging with existing content).
	 *
	 * @return number of imported files
	 */
	public static int importPack(Path source) throws IOException {
		int imported = 0;
		try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(source))) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (entry.isDirectory()) continue;
				String name = entry.getName().replace('\\', '/');
				int slash = name.indexOf('/');
				if (slash <= 0) continue;
				String folder = name.substring(0, slash);
				String fileName = name.substring(slash + 1);
				// only known folders, only plain file names - no path traversal
				if (!FOLDERS.contains(folder) || fileName.isBlank() || fileName.contains("/") || fileName.contains("..")) {
					continue;
				}
				Path targetDir = ModMakerPaths.root().resolve(folder);
				Files.createDirectories(targetDir);
				Path target = targetDir.resolve(fileName);
				try (OutputStream out = Files.newOutputStream(target)) {
					zip.transferTo(out);
				}
				imported++;
			}
		}
		ModMaker.LOGGER.info("ModMaker imported {} file(s) from {}", imported, source);
		return imported;
	}
}
