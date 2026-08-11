package de.minecraftmodmaker.choicervoicer.client;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

final class FfmpegVideoDecoder implements AutoCloseable {
    static final int WIDTH = 854;
    static final int HEIGHT = 480;
    static final int FPS = 24;
    private static final int FRAME_BYTES = WIDTH * HEIGHT * 4;

    private final Process process;
    private final AtomicReference<byte[]> latestFrame = new AtomicReference<>();
    private final AtomicReference<String> error = new AtomicReference<>();
    private volatile boolean closed;
    private volatile boolean finished;

    FfmpegVideoDecoder(Path video, long offsetMillis, long durationMillis) throws IOException {
        if (!Files.isRegularFile(video)) {
            throw new IOException("Video fehlt: " + video);
        }
        String ffmpeg = resolveFfmpeg();
        List<String> command = new ArrayList<>(List.of(
                ffmpeg, "-hide_banner", "-loglevel", "error",
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
        process = new ProcessBuilder(command).start();
        Thread.ofVirtual().name("choicer-video-decoder").start(this::decode);
        Thread.ofVirtual().name("choicer-video-stderr").start(this::drainErrors);
    }

    static int frameBytes() {
        return FRAME_BYTES;
    }

    byte[] takeLatestFrame() {
        return latestFrame.getAndSet(null);
    }

    boolean finished() {
        return finished;
    }

    String errorMessage() {
        return error.get();
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
        } catch (IOException exception) {
            if (!closed) {
                error.compareAndSet(null, "Videodekodierung fehlgeschlagen: " + exception.getMessage());
            }
        } finally {
            finished = true;
            if (!closed) {
                process.destroy();
            }
        }
    }

    private void drainErrors() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (InputStream input = process.getErrorStream()) {
            input.transferTo(buffer);
        } catch (IOException ignored) {
        }
        String message = buffer.toString().trim();
        if (!message.isEmpty()) {
            error.compareAndSet(null, message.lines().findFirst().orElse(message));
        }
        if (!process.isAlive() && process.exitValue() != 0) {
            error.compareAndSet(null, "FFmpeg beendete sich mit Code " + process.exitValue());
        }
    }

    @Override
    public void close() {
        closed = true;
        process.destroyForcibly();
        latestFrame.set(null);
        finished = true;
    }

    private static String seconds(long milliseconds) {
        return String.format(Locale.ROOT, "%.3f", Math.max(0L, milliseconds) / 1000D);
    }

    private static String resolveFfmpeg() throws IOException {
        List<String> candidates = new ArrayList<>();
        candidates.add("ffmpeg");
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            candidates.add("C:\\ProgramData\\chocolatey\\bin\\ffmpeg.exe");
            candidates.add("C:\\ffmpeg\\bin\\ffmpeg.exe");
            String local = System.getenv("LOCALAPPDATA");
            if (local != null) {
                candidates.add(local + "\\Microsoft\\WinGet\\Links\\ffmpeg.exe");
            }
            String programFiles = System.getenv("ProgramFiles");
            if (programFiles != null) {
                candidates.add(programFiles + "\\ffmpeg\\bin\\ffmpeg.exe");
            }
        } else if (os.contains("mac")) {
            candidates.add("/opt/homebrew/bin/ffmpeg");
            candidates.add("/usr/local/bin/ffmpeg");
        } else {
            candidates.add("/usr/bin/ffmpeg");
            candidates.add("/usr/local/bin/ffmpeg");
        }
        IOException last = null;
        for (String candidate : candidates) {
            try {
                Process probe = new ProcessBuilder(candidate, "-version")
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .start();
                if (probe.waitFor() == 0) {
                    return candidate;
                }
            } catch (IOException exception) {
                last = exception;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("FFmpeg-Suche unterbrochen", exception);
            }
        }
        throw new IOException("FFmpeg nicht gefunden", last);
    }
}
