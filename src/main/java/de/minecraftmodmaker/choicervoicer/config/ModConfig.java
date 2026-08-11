package de.minecraftmodmaker.choicervoicer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;

import java.io.IOException;
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
        double silenceThreshold,
        boolean webEnabled,
        int webPort,
        String webPublicBaseUrl
) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static ModConfig defaults() {
        return new ModConfig(3, 3, 750, 2, 256L * 1024 * 1024,
                512L * 1024 * 1024, 2_000, 0.008D, true, 8765, "");
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
            JsonObject object = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if (!object.has("webEnabled")) {
                object.addProperty("webEnabled", true);
            }
            if (!object.has("webPort")) {
                object.addProperty("webPort", 8765);
            }
            if (!object.has("webPublicBaseUrl")) {
                object.addProperty("webPublicBaseUrl", "");
            }
            ModConfig loaded = GSON.fromJson(object, ModConfig.class);
            return loaded == null ? defaults() : loaded.validated();
        } catch (IOException | RuntimeException exception) {
            logger.error("Could not load {}, using defaults", path, exception);
            return defaults();
        }
    }

    private ModConfig validated() {
        String baseUrl = webPublicBaseUrl == null ? "" : webPublicBaseUrl.trim();
        return new ModConfig(
                Math.clamp(rounds, 1, 20),
                Math.clamp(countdownSeconds, 1, 10),
                Math.clamp(recordingTailMillis, 0, 5_000),
                Math.clamp(operatorPermissionLevel, 0, 4),
                Math.max(maxArchiveBytes, 1024),
                Math.max(maxExtractedBytes, 1024),
                Math.clamp(maxPackFiles, 1, 20_000),
                Math.clamp(silenceThreshold, 0.0001D, 0.5D),
                webEnabled,
                Math.clamp(webPort <= 0 ? 8765 : webPort, 1, 65_535),
                baseUrl
        );
    }
}
