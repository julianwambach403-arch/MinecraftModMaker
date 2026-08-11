package de.minecraftmodmaker.choicervoicer.client;

import de.minecraftmodmaker.choicervoicer.network.DubPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

final class ClientVideoManager {
    private static final int MAX_CHUNKS = 12_000;

    private final Path cache;
    private final Map<String, Download> downloads = new HashMap<>();
    private final Map<String, DubPayloads.VideoControl> pending = new HashMap<>();
    private VideoOverlay overlay;

    ClientVideoManager(Path cache) {
        this.cache = cache.toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.cache);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create Choicer Voicer video cache", exception);
        }
    }

    void accept(DubPayloads.VideoChunk chunk) {
        if (!validKey(chunk.assetKey()) || chunk.total() <= 0 || chunk.total() > MAX_CHUNKS
                || chunk.index() < 0 || chunk.index() >= chunk.total()
                || chunk.data().length > DubPayloads.CHUNK_BYTES) {
            return;
        }
        Path completed = completed(chunk.assetKey());
        if (Files.exists(completed)) {
            signalReady(chunk.assetKey());
            playPending(chunk.assetKey());
            return;
        }
        try {
            Download download = downloads.computeIfAbsent(chunk.assetKey(),
                    ignored -> new Download(partial(chunk.assetKey()), chunk.total()));
            if (download.total != chunk.total()) {
                download.close();
                downloads.remove(chunk.assetKey());
                Files.deleteIfExists(partial(chunk.assetKey()));
                return;
            }
            download.write(chunk.index(), chunk.data());
            if (download.complete()) {
                download.close();
                downloads.remove(chunk.assetKey());
                Files.move(partial(chunk.assetKey()), completed, StandardCopyOption.REPLACE_EXISTING);
                signalReady(chunk.assetKey());
                playPending(chunk.assetKey());
                tell("Dub-Video geladen.");
            }
        } catch (IOException | RuntimeException exception) {
            tell("Video konnte nicht gespeichert werden.");
        }
    }

    void control(DubPayloads.VideoControl control) {
        if (!validKey(control.assetKey())) {
            return;
        }
        if (control.action() == DubPayloads.VideoControl.Action.STOP) {
            pending.remove(control.assetKey());
            closeOverlay();
            return;
        }
        if (Files.exists(completed(control.assetKey()))) {
            play(control);
        } else {
            pending.put(control.assetKey(), control);
            tell("Warte auf Dub-Video-Download …");
        }
    }

    void clientTick() {
        if (overlay != null && !overlay.tick()) {
            String failure = overlay.failure();
            closeOverlay();
            if (failure != null && !failure.isBlank()) {
                tell(failure);
            }
        }
    }

    void render(GuiGraphicsExtractor graphics) {
        if (overlay != null) {
            overlay.render(graphics);
        }
    }

    private void playPending(String assetKey) {
        DubPayloads.VideoControl control = pending.remove(assetKey);
        if (control != null) {
            play(control);
        }
    }

    private static void signalReady(String assetKey) {
        ClientPlayNetworking.send(new DubPayloads.VideoReady(assetKey));
    }

    private void play(DubPayloads.VideoControl control) {
        closeOverlay();
        try {
            FfmpegVideoDecoder decoder = new FfmpegVideoDecoder(completed(control.assetKey()),
                    control.offsetMillis(), control.durationMillis());
            overlay = new VideoOverlay(decoder, control.durationMillis());
        } catch (IOException exception) {
            tell("Choicer Voicer benötigt FFmpeg im System-PATH, um Dub-Videos anzuzeigen.");
        }
    }

    private void closeOverlay() {
        if (overlay != null) {
            overlay.close();
            overlay = null;
        }
    }

    private Path completed(String key) {
        return cache.resolve(key + ".ogv");
    }

    private Path partial(String key) {
        return cache.resolve(key + ".part");
    }

    private static boolean validKey(String key) {
        return key != null && key.length() <= 160 && key.matches("[a-z0-9_-]+");
    }

    private static void tell(String message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal("Choicer Voicer: " + message));
        }
    }

    private static final class Download {
        private final RandomAccessFile file;
        private final boolean[] received;
        private final int total;
        private int count;

        private Download(Path path, int total) {
            try {
                file = new RandomAccessFile(path.toFile(), "rw");
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
            this.total = total;
            this.received = new boolean[total];
        }

        private void write(int index, byte[] bytes) throws IOException {
            if (received[index]) {
                return;
            }
            file.seek((long) index * DubPayloads.CHUNK_BYTES);
            file.write(bytes);
            received[index] = true;
            count++;
        }

        private boolean complete() {
            return count == total;
        }

        private void close() {
            try {
                file.close();
            } catch (IOException ignored) {
            }
        }
    }
}
