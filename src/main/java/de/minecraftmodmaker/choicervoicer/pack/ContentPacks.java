package de.minecraftmodmaker.choicervoicer.pack;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ContentPacks {
    private ContentPacks() {
    }

    public record VoiceClip(
            String id,
            String title,
            Path audio,
            Optional<String> caption,
            Optional<Path> image,
            Map<String, Object> metadata
    ) {
    }

    public record VoicePack(
            String id,
            String displayName,
            Path root,
            List<VoiceClip> clips,
            Optional<Path> icon,
            Map<String, Object> metadata
    ) {
    }

    public record Judge(
            int number,
            Optional<String> name,
            Optional<Path> image,
            Optional<Path> successImage,
            Optional<Path> voice,
            Optional<Path> scoreBlip
    ) {
    }

    public record JudgePack(
            String id,
            String displayName,
            Path root,
            List<Judge> judges,
            Map<String, Object> config
    ) {
    }

    public record Registry(Map<String, VoicePack> voicePacks, Map<String, JudgePack> judgePacks) {
        public static Registry empty() {
            return new Registry(Map.of(), Map.of());
        }
    }

    public record ImportIssue(Severity severity, String source, String message) {
        public enum Severity {
            WARNING, ERROR
        }
    }

    public record ImportReport(int importedArchives, int voicePacks, int judgePacks,
                               List<ImportIssue> issues) {
        public boolean successful() {
            return issues.stream().noneMatch(issue -> issue.severity() == ImportIssue.Severity.ERROR);
        }
    }
}
