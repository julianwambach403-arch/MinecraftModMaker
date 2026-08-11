package de.minecraftmodmaker.choicervoicer.network;

import de.minecraftmodmaker.choicervoicer.pack.ContentPacks;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class VideoTransferManager {
    private static final int CHUNKS_PER_TICK = 4;

    private final Deque<Transfer> transfers = new ArrayDeque<>();
    private final Set<String> scheduled = new HashSet<>();
    private final Set<String> ready = new HashSet<>();
    private final Logger logger;

    public VideoTransferManager(Logger logger) {
        this.logger = logger;
    }

    public boolean supportsVideo(ServerPlayer player) {
        return ServerPlayNetworking.canSend(player, DubPayloads.VideoChunk.TYPE)
                && ServerPlayNetworking.canSend(player, DubPayloads.VideoControl.TYPE);
    }

    public void queue(ServerPlayer player, ContentPacks.VoicePack pack) {
        Path video = pack.dubVideo().orElse(null);
        if (video == null || !supportsVideo(player)) {
            return;
        }
        try {
            String assetKey = assetKey(pack, video);
            String scheduleKey = player.getUUID() + ":" + assetKey;
            if (scheduled.add(scheduleKey)) {
                transfers.addLast(new Transfer(player.getUUID(), video, assetKey,
                        Math.toIntExact((Files.size(video) + DubPayloads.CHUNK_BYTES - 1)
                                / DubPayloads.CHUNK_BYTES)));
            }
        } catch (IOException | ArithmeticException exception) {
            logger.warn("Could not schedule dub video {}", video, exception);
        }
    }

    public void markReady(ServerPlayer player, String assetKey) {
        if (assetKey != null && assetKey.matches("[a-z0-9_-]{1,160}")) {
            ready.add(player.getUUID() + ":" + assetKey);
        }
    }

    public boolean isReady(ServerPlayer player, ContentPacks.VoicePack pack) {
        if (!pack.isDubPack()) {
            return true;
        }
        try {
            return ready.contains(player.getUUID() + ":"
                    + assetKey(pack, pack.dubVideo().orElseThrow()));
        } catch (IOException exception) {
            return false;
        }
    }

    public void tick(java.util.function.Function<UUID, ServerPlayer> playerLookup) {
        for (int sent = 0; sent < CHUNKS_PER_TICK && !transfers.isEmpty(); sent++) {
            Transfer transfer = transfers.removeFirst();
            ServerPlayer player = playerLookup.apply(transfer.playerId);
            if (player == null || !supportsVideo(player)) {
                transfer.close();
                continue;
            }
            try {
                byte[] bytes = transfer.next();
                ServerPlayNetworking.send(player, new DubPayloads.VideoChunk(
                        transfer.assetKey, transfer.index - 1, transfer.total, bytes));
                if (transfer.index < transfer.total) {
                    transfers.addLast(transfer);
                } else {
                    transfer.close();
                }
            } catch (IOException exception) {
                logger.warn("Video transfer failed for {}", transfer.path, exception);
                transfer.close();
            }
        }
    }

    public void play(ServerPlayer player, ContentPacks.VoicePack pack, double offsetSeconds,
                     double durationSeconds) {
        if (!supportsVideo(player) || pack.dubVideo().isEmpty()) {
            return;
        }
        try {
            ServerPlayNetworking.send(player, new DubPayloads.VideoControl(
                    assetKey(pack, pack.dubVideo().orElseThrow()), DubPayloads.VideoControl.Action.PLAY,
                    Math.max(0L, Math.round(offsetSeconds * 1000D)),
                    Math.max(0L, Math.round(durationSeconds * 1000D))));
        } catch (IOException exception) {
            logger.warn("Could not start dub video for {}", player.getScoreboardName(), exception);
        }
    }

    public void stop(ServerPlayer player, ContentPacks.VoicePack pack) {
        if (!supportsVideo(player) || pack.dubVideo().isEmpty()) {
            return;
        }
        try {
            ServerPlayNetworking.send(player, new DubPayloads.VideoControl(
                    assetKey(pack, pack.dubVideo().orElseThrow()), DubPayloads.VideoControl.Action.STOP,
                    0L, 0L));
        } catch (IOException exception) {
            logger.warn("Could not stop dub video for {}", player.getScoreboardName(), exception);
        }
    }

    public void close() {
        transfers.forEach(Transfer::close);
        transfers.clear();
        scheduled.clear();
        ready.clear();
    }

    private static String assetKey(ContentPacks.VoicePack pack, Path video) throws IOException {
        return (pack.id() + "_" + Files.size(video) + "_" + Files.getLastModifiedTime(video).toMillis())
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
    }

    private static final class Transfer {
        private final UUID playerId;
        private final Path path;
        private final String assetKey;
        private final int total;
        private int index;
        private FileChannel channel;

        private Transfer(UUID playerId, Path path, String assetKey, int total) {
            this.playerId = playerId;
            this.path = path;
            this.assetKey = assetKey;
            this.total = total;
        }

        private byte[] next() throws IOException {
            if (channel == null) {
                channel = FileChannel.open(path, StandardOpenOption.READ);
            }
            ByteBuffer buffer = ByteBuffer.allocate(DubPayloads.CHUNK_BYTES);
            int read = channel.read(buffer);
            if (read < 0) {
                return new byte[0];
            }
            index++;
            byte[] bytes = new byte[read];
            buffer.flip();
            buffer.get(bytes);
            return bytes;
        }

        private void close() {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException ignored) {
                }
                channel = null;
            }
        }
    }
}
