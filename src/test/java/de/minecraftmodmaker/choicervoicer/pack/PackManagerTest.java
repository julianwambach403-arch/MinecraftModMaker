package de.minecraftmodmaker.choicervoicer.pack;

import de.minecraftmodmaker.choicervoicer.config.ModConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackManagerTest {
    @TempDir
    Path temporary;

    @Test
    void discoversVoiceAndJudgeFolders() throws IOException {
        Path voices = temporary.resolve("imports/packs_voice/Test Pack");
        Files.createDirectories(voices);
        Files.write(voices.resolve("hello.wav"), new byte[]{1, 2});
        Files.writeString(voices.resolve("hello.txt"), "Hallo Welt");

        Path judges = temporary.resolve("imports/packs_judges/Jury");
        Files.createDirectories(judges);
        Files.write(judges.resolve("judge1.png"), new byte[]{1});
        Files.write(judges.resolve("judge1_voice.ogg"), new byte[]{1});

        PackManager manager = manager();
        manager.initialize();

        assertEquals(1, manager.registry().voicePacks().size());
        assertEquals(1, manager.registry().judgePacks().size());
        assertEquals("Hallo Welt", manager.registry().voicePacks().values().iterator().next()
                .clips().getFirst().caption().orElseThrow());
    }

    @Test
    void rejectsZipSlip() throws IOException {
        Path imports = temporary.resolve("imports");
        Files.createDirectories(imports);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(imports.resolve("evil.zip")))) {
            zip.putNextEntry(new ZipEntry("../outside.txt"));
            zip.write("bad".getBytes());
            zip.closeEntry();
        }

        PackManager manager = manager();
        manager.initialize();
        ContentPacks.ImportReport report = manager.importArchives();

        assertFalse(report.successful());
        assertTrue(Files.notExists(temporary.resolve("outside.txt")));
    }

    @Test
    void importsNormalDirectoryEntryFromRelativeServerPath() throws IOException {
        Path imports = temporary.resolve("imports");
        Files.createDirectories(imports);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(imports.resolve("jujutsu.zip")))) {
            zip.putNextEntry(new ZipEntry("Jujutsu_Kaisen/"));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("Jujutsu_Kaisen/packs_voice/Jujutsu_Kaisen/"));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("Jujutsu_Kaisen/packs_voice/Jujutsu_Kaisen/clip.ogg"));
            zip.write(new byte[]{1, 2, 3});
            zip.closeEntry();
        }

        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        Path relativeRoot = workingDirectory.relativize(temporary.toAbsolutePath().normalize());
        PackManager manager = manager(relativeRoot);
        manager.initialize();
        ContentPacks.ImportReport report = manager.importArchives();

        assertTrue(report.successful());
        assertEquals(1, report.importedArchives());
        assertEquals(1, report.voicePacks());
    }

    private PackManager manager() {
        return manager(temporary);
    }

    private PackManager manager(Path root) {
        return new PackManager(root, ModConfig.defaults(), LoggerFactory.getLogger("test"));
    }
}
