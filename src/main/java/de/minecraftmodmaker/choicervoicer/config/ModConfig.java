package de.minecraftmodmaker.choicervoicer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public record ModConfig(
        int rounds,
        int countdownSeconds,
        int recordingTailMillis,
        int operatorPermissionLevel,
        long maxArchiveBytes,
        long maxExtractedBytes,
        int maxPackFiles,
        double silenceThreshold
) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static ModConfig defaults() {
        return new ModConfig(3, 3, 750, 2, 256L * 1024 * 1024,
                512L * 1024 * 1024, 2_000, 0.008D);
    }

    public static ModConfig load(Path path, Logger logger) {
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                ModConfig config = defaults();
                try (Writer writer = Files.newBufferedWriter(path)) {
                    GSON.toJson(config, writer);
                }
                return config;
            }
            try (Reader reader = Files.newBufferedReader(path)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                return loaded == null ? defaults() : loaded.validated();
            }
        } catch (IOException | RuntimeException exception) {
            logger.error("Could not load {}, using defaults", path, exception);
            return defaults();
        }
    }

    private ModConfig validated() {
        return new ModConfig(
                Math.clamp(rounds, 1, 20),
                Math.clamp(countdownSeconds, 1, 10),
                Math.clamp(recordingTailMillis, 0, 5_000),
                Math.clamp(operatorPermissionLevel, 0, 4),
                Math.max(maxArchiveBytes, 1024),
                Math.max(maxExtractedBytes, 1024),
                Math.clamp(maxPackFiles, 1, 20_000),
                Math.clamp(silenceThreshold, 0.0001D, 0.5D)
        );
    }
}
