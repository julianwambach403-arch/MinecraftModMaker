package de.minecraftmodmaker.choicervoicer.pack;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.minecraftmodmaker.choicervoicer.config.ModConfig;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static de.minecraftmodmaker.choicervoicer.pack.ContentPacks.ImportIssue;

public final class PackManager {
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("wav", "mp3", "ogg");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");
    private static final Gson GSON = new Gson();

    private final Path root;
    private final Path imports;
    private final Path installed;
    private final ModConfig config;
    private final Logger logger;
    private volatile ContentPacks.Registry registry = ContentPacks.Registry.empty();

    public PackManager(Path root, ModConfig config, Logger logger) {
        this.root = root;
        this.imports = root.resolve("imports");
        this.installed = root.resolve("packs");
        this.config = config;
        this.logger = logger;
    }

    public void initialize() throws IOException {
        Files.createDirectories(imports);
        Files.createDirectories(installed);
        reload();
    }

    public ContentPacks.Registry registry() {
        return registry;
    }

    public synchronized ContentPacks.ImportReport importArchives() {
        List<ImportIssue> issues = new ArrayList<>();
        int importedCount = 0;
        try (var stream = Files.list(imports)) {
            List<Path> archives = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> extension(path).equals("zip"))
                    .sorted()
                    .toList();
            for (Path archive : archives) {
                try {
                    if (Files.size(archive) > config.maxArchiveBytes()) {
                        throw new IOException("Archiv überschreitet das Größenlimit");
                    }
                    String baseName = safeId(stripExtension(archive.getFileName().toString()));
                    Path destination = installed.resolve(baseName);
                    if (Files.exists(destination)) {
                        issues.add(new ImportIssue(ImportIssue.Severity.WARNING, archive.toString(),
                                "Bereits importiert; vorhandener Ordner wurde beibehalten"));
                        continue;
                    }
                    extractSecurely(archive, destination);
                    importedCount++;
                } catch (IOException exception) {
                    issues.add(new ImportIssue(ImportIssue.Severity.ERROR, archive.toString(), exception.getMessage()));
                }
            }
            reload(issues);
        } catch (IOException exception) {
            issues.add(new ImportIssue(ImportIssue.Severity.ERROR, imports.toString(), exception.getMessage()));
        }
        return new ContentPacks.ImportReport(importedCount, registry.voicePacks().size(),
                registry.judgePacks().size(), List.copyOf(issues));
    }

    public synchronized ContentPacks.Registry reload() {
        List<ImportIssue> issues = new ArrayList<>();
        reload(issues);
        issues.forEach(issue -> logger.warn("Pack {}: {}", issue.source(), issue.message()));
        return registry;
    }

    private void reload(List<ImportIssue> issues) {
        Map<String, ContentPacks.VoicePack> voices = new LinkedHashMap<>();
        Map<String, ContentPacks.JudgePack> judges = new LinkedHashMap<>();
        scanSource(imports, voices, judges, issues, false);
        scanSource(installed, voices, judges, issues, true);
        registry = new ContentPacks.Registry(Map.copyOf(voices), Map.copyOf(judges));
    }

    private void scanSource(Path source, Map<String, ContentPacks.VoicePack> voices,
                            Map<String, ContentPacks.JudgePack> judges, List<ImportIssue> issues,
                            boolean includeArchiveRoots) {
        if (Files.notExists(source)) {
            return;
        }
        try (var walk = Files.walk(source, 5)) {
            List<Path> directories = walk.filter(Files::isDirectory)
                    .filter(path -> !path.equals(source))
                    .sorted(Comparator.comparingInt(Path::getNameCount))
                    .toList();
            for (Path directory : directories) {
                String parentName = directory.getParent() == null ? "" :
                        directory.getParent().getFileName().toString().toLowerCase(Locale.ROOT);
                if (parentName.equals("packs_voice") || (includeArchiveRoots && looksLikeVoicePack(directory))) {
                    parseVoicePack(directory, issues).ifPresent(pack -> voices.putIfAbsent(pack.id(), pack));
                }
                if (parentName.equals("packs_judges") || (includeArchiveRoots && looksLikeJudgePack(directory))) {
                    parseJudgePack(directory, issues).ifPresent(pack -> judges.putIfAbsent(pack.id(), pack));
                }
            }
        } catch (IOException exception) {
            issues.add(new ImportIssue(ImportIssue.Severity.ERROR, source.toString(), exception.getMessage()));
        }
    }

    private boolean looksLikeVoicePack(Path directory) {
        return directFiles(directory).stream().anyMatch(path -> AUDIO_EXTENSIONS.contains(extension(path)))
                && !looksLikeJudgePack(directory);
    }

    private boolean looksLikeJudgePack(Path directory) {
        return directFiles(directory).stream()
                .map(path -> stripExtension(path.getFileName().toString()).toLowerCase(Locale.ROOT))
                .anyMatch(name -> name.matches("(judge|scoreblip)[1-5](_voice|_success)?"));
    }

    private Optional<ContentPacks.VoicePack> parseVoicePack(Path directory, List<ImportIssue> issues) {
        try {
            List<Path> files = directFiles(directory);
            Map<String, Object> metadata = readMetadata(directory, issues);
            List<ContentPacks.VoiceClip> clips = new ArrayList<>();
            for (Path audio : files.stream().filter(path -> AUDIO_EXTENSIONS.contains(extension(path))).sorted().toList()) {
                String basename = stripExtension(audio.getFileName().toString());
                if (basename.startsWith("_") || basename.matches("(judge|scoreblip)[1-5].*")) {
                    continue;
                }
                Optional<String> caption = findSibling(directory, basename, Set.of("txt"))
                        .flatMap(PackManager::readText);
                Optional<Path> image = findSibling(directory, basename, IMAGE_EXTENSIONS);
                clips.add(new ContentPacks.VoiceClip(safeId(basename), basename, audio, caption, image, Map.of()));
            }
            if (clips.isEmpty()) {
                return Optional.empty();
            }
            String id = uniqueId(directory);
            String displayName = stringValue(metadata, "name").orElse(directory.getFileName().toString());
            Optional<Path> icon = findSibling(directory, "_icon", IMAGE_EXTENSIONS)
                    .or(() -> findSibling(directory, "_pack_filler_image", IMAGE_EXTENSIONS));
            return Optional.of(new ContentPacks.VoicePack(id, displayName, directory, List.copyOf(clips), icon, metadata));
        } catch (RuntimeException exception) {
            issues.add(new ImportIssue(ImportIssue.Severity.ERROR, directory.toString(), exception.getMessage()));
            return Optional.empty();
        }
    }

    private Optional<ContentPacks.JudgePack> parseJudgePack(Path directory, List<ImportIssue> issues) {
        Map<String, Object> metadata = readMetadata(directory, issues);
        List<ContentPacks.Judge> result = new ArrayList<>();
        for (int number = 1; number <= 5; number++) {
            String judge = "judge" + number;
            result.add(new ContentPacks.Judge(
                    number,
                    stringValue(metadata, judge + "_name"),
                    findSibling(directory, judge, IMAGE_EXTENSIONS),
                    findSibling(directory, judge + "_success", IMAGE_EXTENSIONS)
                            .or(() -> findSibling(directory, "success", IMAGE_EXTENSIONS)),
                    findSibling(directory, judge + "_voice", AUDIO_EXTENSIONS),
                    findSibling(directory, "scoreblip" + number, AUDIO_EXTENSIONS)
            ));
        }
        if (result.stream().noneMatch(judge -> judge.image().isPresent() || judge.voice().isPresent()
                || judge.scoreBlip().isPresent())) {
            return Optional.empty();
        }
        String id = uniqueId(directory);
        return Optional.of(new ContentPacks.JudgePack(id,
                stringValue(metadata, "name").orElse(directory.getFileName().toString()),
                directory, List.copyOf(result), metadata));
    }

    private Map<String, Object> readMetadata(Path directory, List<ImportIssue> issues) {
        Map<String, Object> merged = new HashMap<>();
        for (Path file : directFiles(directory).stream().filter(path -> extension(path).equals("json")).toList()) {
            try (Reader reader = Files.newBufferedReader(file)) {
                Map<String, Object> values = GSON.fromJson(reader, new TypeToken<Map<String, Object>>() {
                }.getType());
                if (values != null) {
                    merged.putAll(values);
                }
            } catch (Exception exception) {
                issues.add(new ImportIssue(ImportIssue.Severity.WARNING, file.toString(),
                        "Metadaten konnten nicht gelesen werden: " + exception.getMessage()));
            }
        }
        return Map.copyOf(merged);
    }

    private void extractSecurely(Path archive, Path destination) throws IOException {
        Path temporary = Files.createTempDirectory(root, ".import-");
        boolean success = false;
        long totalBytes = 0;
        int files = 0;
        try (InputStream input = Files.newInputStream(archive); ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                files++;
                if (files > config.maxPackFiles()) {
                    throw new IOException("Archiv enthält zu viele Dateien");
                }
                Path output = temporary.resolve(entry.getName()).normalize();
                if (!output.startsWith(temporary)) {
                    throw new IOException("Unsicherer ZIP-Pfad: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                    continue;
                }
                Files.createDirectories(output.getParent());
                long fileBytes = 0;
                try (var writer = Files.newOutputStream(output)) {
                    int read;
                    while ((read = zip.read(buffer)) >= 0) {
                        fileBytes += read;
                        totalBytes += read;
                        if (totalBytes > config.maxExtractedBytes()) {
                            throw new IOException("Entpackte Daten überschreiten das Größenlimit");
                        }
                        writer.write(buffer, 0, read);
                    }
                }
                if (entry.getSize() >= 0 && fileBytes != entry.getSize()) {
                    throw new IOException("Unvollständiger ZIP-Eintrag: " + entry.getName());
                }
            }
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            success = true;
        } finally {
            if (!success) {
                deleteTree(temporary);
            }
        }
    }

    private static void deleteTree(Path path) throws IOException {
        if (Files.notExists(path)) {
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static List<Path> directFiles(Path directory) {
        try (var stream = Files.list(directory)) {
            return stream.filter(Files::isRegularFile).toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private static Optional<Path> findSibling(Path directory, String basename, Set<String> extensions) {
        return directFiles(directory).stream()
                .filter(path -> stripExtension(path.getFileName().toString()).equalsIgnoreCase(basename))
                .filter(path -> extensions.contains(extension(path)))
                .findFirst();
    }

    private static Optional<String> readText(Path path) {
        try {
            return Optional.of(Files.readString(path).strip());
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private static Optional<String> stringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof String text && !text.isBlank() ? Optional.of(text) : Optional.empty();
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    private static String uniqueId(Path path) {
        Path parent = path.getParent();
        String prefix = parent == null ? "" : parent.getFileName() + "_";
        return safeId(prefix + path.getFileName());
    }

    private static String safeId(String input) {
        String safe = input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "_")
                .replaceAll("^_+|_+$", "");
        return safe.isBlank() ? "pack" : safe;
    }
}
