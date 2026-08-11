package de.minecraftmodmaker.choicervoicer.client;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

final class FfmpegVideoDecoder implements AutoCloseable {
    static final int WIDTH = 854;
    static final int HEIGHT = 480;
    static final int FPS = 24;
    private static final int FRAME_BYTES = WIDTH * HEIGHT * 4;

    private final Process process;
    private final AtomicReference<byte[]> latestFrame = new AtomicReference<>();
    private volatile boolean closed;

    FfmpegVideoDecoder(Path video, long offsetMillis, long durationMillis) throws IOException {
        List<String> command = new ArrayList<>(List.of(
                "ffmpeg", "-loglevel", "error",
                "-ss", seconds(offsetMillis),
                "-i", video.toAbsolutePath().toString(),
                "-an",
                "-vf", "scale=" + WIDTH + ":" + HEIGHT
                        + ":force_original_aspect_ratio=decrease,pad=" + WIDTH + ":" + HEIGHT
                        + ":(ow-iw)/2:(oh-ih)/2:black",
                "-r", Integer.toString(FPS)
        ));
        if (durationMillis > 0L) {
            command.add("-t");
            command.add(seconds(durationMillis));
        }
        command.addAll(List.of("-f", "rawvideo", "-pix_fmt", "rgba", "pipe:1"));
        process = new ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        Thread.ofVirtual().name("choicer-video-decoder").start(this::decode);
    }

    byte[] takeLatestFrame() {
        return latestFrame.getAndSet(null);
    }

    boolean ended() {
        return !process.isAlive();
    }

    private void decode() {
        try (InputStream input = process.getInputStream()) {
            while (!closed) {
                byte[] frame = input.readNBytes(FRAME_BYTES);
                if (frame.length == 0) {
                    break;
                }
                if (frame.length != FRAME_BYTES) {
                    throw new EOFException("Incomplete video frame");
                }
                latestFrame.set(frame);
            }
        } catch (IOException ignored) {
        } finally {
            process.destroy();
        }
    }

    @Override
    public void close() {
        closed = true;
        process.destroyForcibly();
        latestFrame.set(null);
    }

    private static String seconds(long milliseconds) {
        return String.format(java.util.Locale.ROOT, "%.3f", milliseconds / 1000D);
    }
}
