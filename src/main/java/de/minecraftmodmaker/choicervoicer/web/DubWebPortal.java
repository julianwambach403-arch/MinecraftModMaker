package de.minecraftmodmaker.choicervoicer.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.minecraftmodmaker.choicervoicer.audio.AudioCodec;
import de.minecraftmodmaker.choicervoicer.audio.WavWriter;
import de.minecraftmodmaker.choicervoicer.config.ModConfig;
import de.minecraftmodmaker.choicervoicer.pack.ContentPacks;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public final class DubWebPortal implements AutoCloseable {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final ModConfig config;
    private final Path cacheRoot;
    private final Logger logger;
    private final AtomicReference<LiveStatus> live = new AtomicReference<>(LiveStatus.idle());
    private final AtomicReference<ResultPayload> result = new AtomicReference<>(ResultPayload.empty());
    private HttpServer server;
    private String publicBaseUrl = "";

    public DubWebPortal(ModConfig config, Path cacheRoot, Logger logger) {
        this.config = config;
        this.cacheRoot = cacheRoot.toAbsolutePath().normalize();
        this.logger = logger;
    }

    public void start() throws IOException {
        if (!config.webEnabled()) {
            logger.info("Choicer Voicer web portal disabled");
            return;
        }
        Files.createDirectories(cacheRoot);
        server = HttpServer.create(new InetSocketAddress(config.webPort()), 0);
        server.createContext("/", this::handleRoot);
        server.createContext("/watch", this::handleWatch);
        server.createContext("/result", this::handleResultPage);
        server.createContext("/api/status", this::handleStatus);
        server.createContext("/api/result", this::handleResultApi);
        server.createContext("/media/video", this::handleVideo);
        server.createContext("/media/take/", this::handleTake);
        server.setExecutor(Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "choicer-web");
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
        publicBaseUrl = resolvePublicBaseUrl();
        logger.info("Choicer Voicer web portal listening on {}", publicBaseUrl);
    }

    public String publicBaseUrl() {
        return publicBaseUrl.isBlank() ? "http://127.0.0.1:" + config.webPort() : publicBaseUrl;
    }

    public boolean enabled() {
        return config.webEnabled() && server != null;
    }

    public synchronized void preparePack(ContentPacks.VoicePack pack) {
        if (!pack.isDubPack()) {
            live.set(LiveStatus.idle());
            return;
        }
        Path source = pack.dubVideo().orElseThrow();
        Path prepared = prepareBrowserVideo(pack.id(), source);
        live.set(new LiveStatus(
                "LOBBY",
                pack.displayName(),
                "",
                "",
                0D,
                0D,
                true,
                mediaName(prepared),
                System.currentTimeMillis()
        ));
    }

    public synchronized void publishLive(String state, ContentPacks.VoicePack pack, String playerName,
                                         String clipTitle, double offsetSeconds, double durationSeconds) {
        if (pack == null || !pack.isDubPack()) {
            return;
        }
        LiveStatus previous = live.get();
        String media = previous.media();
        if (media == null || media.isBlank()) {
            Path prepared = prepareBrowserVideo(pack.id(), pack.dubVideo().orElseThrow());
            media = mediaName(prepared);
        }
        live.set(new LiveStatus(
                state,
                pack.displayName(),
                playerName == null ? "" : playerName,
                clipTitle == null ? "" : clipTitle,
                Math.max(0D, offsetSeconds),
                Math.max(0D, durationSeconds),
                true,
                media,
                System.currentTimeMillis()
        ));
    }

    public synchronized void clearLive() {
        LiveStatus previous = live.get();
        live.set(new LiveStatus("IDLE", previous.packName(), "", "", 0D, 0D, false,
                previous.media(), System.currentTimeMillis()));
    }

    public synchronized void publishResult(ContentPacks.VoicePack pack, List<Take> takes) {
        if (pack == null || !pack.isDubPack()) {
            return;
        }
        try {
            Path sessionDir = cacheRoot.resolve("result").resolve(sanitize(pack.id()));
            deleteRecursively(sessionDir);
            Files.createDirectories(sessionDir);
            Path video = prepareBrowserVideo(pack.id(), pack.dubVideo().orElseThrow());
            List<ResultTake> exported = new ArrayList<>();
            int index = 0;
            for (Take take : takes) {
                String fileName = "take_" + index + ".wav";
                WavWriter.write(sessionDir.resolve(fileName), take.samples(), AudioCodec.SAMPLE_RATE);
                exported.add(new ResultTake(index, take.playerName(), take.clipTitle(),
                        take.timestampSeconds(), "/media/take/" + sanitize(pack.id()) + "/" + fileName));
                index++;
            }
            result.set(new ResultPayload(
                    true,
                    pack.displayName(),
                    mediaName(video),
                    exported,
                    System.currentTimeMillis()
            ));
            live.set(new LiveStatus(
                    "RESULT",
                    pack.displayName(),
                    "",
                    "",
                    0D,
                    0D,
                    true,
                    mediaName(video),
                    System.currentTimeMillis()
            ));
        } catch (IOException exception) {
            logger.warn("Could not publish dub web result", exception);
        }
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
            return;
        }
        sendHtml(exchange, ROOT_HTML);
    }

    private void handleWatch(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
            return;
        }
        sendHtml(exchange, WATCH_HTML);
    }

    private void handleResultPage(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
            return;
        }
        sendHtml(exchange, RESULT_HTML);
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
            return;
        }
        sendJson(exchange, live.get());
    }

    private void handleResultApi(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
            return;
        }
        sendJson(exchange, result.get());
    }

    private void handleVideo(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
            return;
        }
        LiveStatus status = live.get();
        ResultPayload published = result.get();
        String media = status.media();
        if ((media == null || media.isBlank()) && published.hasResult()) {
            media = published.media();
        }
        if (media == null || media.isBlank()) {
            send(exchange, 404, "text/plain; charset=utf-8", "Kein Video");
            return;
        }
        Path file = cacheRoot.resolve("video").resolve(media).normalize();
        if (!file.startsWith(cacheRoot.resolve("video")) || !Files.isRegularFile(file)) {
            send(exchange, 404, "text/plain; charset=utf-8", "Video fehlt");
            return;
        }
        String type = media.endsWith(".webm") ? "video/webm"
                : media.endsWith(".mp4") ? "video/mp4" : "video/ogg";
        sendFile(exchange, file, type);
    }

    private void handleTake(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String prefix = "/media/take/";
        if (!path.startsWith(prefix)) {
            send(exchange, 404, "text/plain; charset=utf-8", "Nicht gefunden");
            return;
        }
        String relative = path.substring(prefix.length());
        if (relative.contains("..") || relative.isBlank()) {
            send(exchange, 400, "text/plain; charset=utf-8", "Ungültiger Pfad");
            return;
        }
        Path file = cacheRoot.resolve("result").resolve(relative).normalize();
        if (!file.startsWith(cacheRoot.resolve("result")) || !Files.isRegularFile(file)) {
            send(exchange, 404, "text/plain; charset=utf-8", "Take fehlt");
            return;
        }
        sendFile(exchange, file, "audio/wav");
    }

    private Path prepareBrowserVideo(String packId, Path source) {
        Path videoDir = cacheRoot.resolve("video");
        try {
            Files.createDirectories(videoDir);
            Path webm = videoDir.resolve(sanitize(packId) + ".webm");
            Path ogv = videoDir.resolve(sanitize(packId) + ".ogv");
            if (Files.isRegularFile(webm)
                    && Files.getLastModifiedTime(webm).toMillis() >= Files.getLastModifiedTime(source).toMillis()) {
                return webm;
            }
            if (convertWithFfmpeg(source, webm)) {
                return webm;
            }
            Files.copy(source, ogv, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return ogv;
        } catch (IOException exception) {
            logger.warn("Could not prepare browser video for {}", packId, exception);
            try {
                Path fallback = cacheRoot.resolve("video").resolve(sanitize(packId) + ".ogv");
                Files.createDirectories(fallback.getParent());
                Files.copy(source, fallback, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return fallback;
            } catch (IOException copyException) {
                throw new IllegalStateException("Could not copy dub video for web portal", copyException);
            }
        }
    }

    private boolean convertWithFfmpeg(Path source, Path target) {
        try {
            Process process = new ProcessBuilder(
                    "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
                    "-i", source.toAbsolutePath().toString(),
                    "-c:v", "libvpx", "-b:v", "1M", "-c:a", "libvorbis",
                    target.toAbsolutePath().toString()
            ).redirectError(ProcessBuilder.Redirect.DISCARD)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            boolean finished = process.waitFor(120, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0 && Files.isRegularFile(target) && Files.size(target) > 0;
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private String resolvePublicBaseUrl() {
        String configured = config.webPublicBaseUrl();
        if (configured != null && !configured.isBlank()) {
            return configured.replaceAll("/$", "");
        }
        return "http://127.0.0.1:" + config.webPort();
    }

    private static String mediaName(Path path) {
        return path.getFileName().toString();
    }

    private static String sanitize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var walk = Files.walk(root)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        }
    }

    private static void sendHtml(HttpExchange exchange, String html) throws IOException {
        send(exchange, 200, "text/html; charset=utf-8", html);
    }

    private static void sendJson(HttpExchange exchange, Object payload) throws IOException {
        send(exchange, 200, "application/json; charset=utf-8", GSON.toJson(payload));
    }

    private static void send(HttpExchange exchange, int code, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        headers.set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void sendFile(HttpExchange exchange, Path file, String contentType) throws IOException {
        long size = Files.size(file);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        headers.set("Accept-Ranges", "bytes");
        headers.set("Cache-Control", "no-store");
        String range = exchange.getRequestHeaders().getFirst("Range");
        if (range != null && range.startsWith("bytes=")) {
            String[] parts = range.substring(6).split("-", 2);
            long start = Long.parseLong(parts[0]);
            long end = parts.length > 1 && !parts[1].isBlank() ? Long.parseLong(parts[1]) : size - 1;
            end = Math.min(end, size - 1);
            long length = end - start + 1;
            headers.set("Content-Range", "bytes " + start + "-" + end + "/" + size);
            exchange.sendResponseHeaders(206, length);
            try (InputStream input = Files.newInputStream(file); OutputStream output = exchange.getResponseBody()) {
                input.skipNBytes(start);
                byte[] buffer = new byte[16_384];
                long remaining = length;
                while (remaining > 0) {
                    int read = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (read < 0) {
                        break;
                    }
                    output.write(buffer, 0, read);
                    remaining -= read;
                }
            }
            return;
        }
        exchange.sendResponseHeaders(200, size);
        try (InputStream input = Files.newInputStream(file); OutputStream output = exchange.getResponseBody()) {
            input.transferTo(output);
        }
    }

    public record Take(String playerName, String clipTitle, double timestampSeconds, short[] samples) {
    }

    private record LiveStatus(
            String state,
            String packName,
            String playerName,
            String clipTitle,
            double offsetSeconds,
            double durationSeconds,
            boolean hasVideo,
            String media,
            long updatedAt
    ) {
        private static LiveStatus idle() {
            return new LiveStatus("IDLE", "", "", "", 0D, 0D, false, "", System.currentTimeMillis());
        }
    }

    private record ResultTake(
            int index,
            String playerName,
            String clipTitle,
            double timestampSeconds,
            String audioUrl
    ) {
    }

    private record ResultPayload(
            boolean hasResult,
            String packName,
            String media,
            List<ResultTake> takes,
            long updatedAt
    ) {
        private static ResultPayload empty() {
            return new ResultPayload(false, "", "", List.of(), System.currentTimeMillis());
        }
    }

    private static final String ROOT_HTML = """
            <!doctype html>
            <html lang="de">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Choicer Voicer</title>
              <style>
                :root { color-scheme: dark; --bg:#101418; --ink:#f4f1ea; --accent:#ff8a3d; --muted:#9aa3ad; }
                * { box-sizing: border-box; }
                body { margin:0; min-height:100vh; font:16px/1.5 "Segoe UI",sans-serif; color:var(--ink);
                  background: radial-gradient(circle at top, #1c2430, var(--bg)); display:grid; place-items:center; padding:2rem; }
                main { width:min(720px,100%); }
                h1 { font-size: clamp(2.4rem, 6vw, 4rem); margin:0 0 .4rem; letter-spacing:-.04em; }
                p { color:var(--muted); margin:0 0 1.5rem; }
                a { display:inline-block; margin:0 .6rem .6rem 0; padding:.85rem 1.2rem; border-radius:999px;
                  background:var(--accent); color:#111; text-decoration:none; font-weight:700; }
                a.secondary { background:transparent; color:var(--ink); border:1px solid #3a4553; }
              </style>
            </head>
            <body>
              <main>
                <h1>Choicer Voicer</h1>
                <p>Video und Endergebnis laufen hier im Browser – nicht in Minecraft.</p>
                <a href="/watch">Live ansehen</a>
                <a class="secondary" href="/result">Endergebnis</a>
              </main>
            </body>
            </html>
            """;

    private static final String WATCH_HTML = """
            <!doctype html>
            <html lang="de">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Choicer Voicer Live</title>
              <style>
                :root { color-scheme: dark; --bg:#0b0d10; --panel:#151a21; --ink:#f5f2eb; --muted:#98a2ae; --accent:#ff8a3d; }
                * { box-sizing: border-box; }
                body { margin:0; background:var(--bg); color:var(--ink); font:15px/1.45 "Segoe UI",sans-serif; }
                header, footer { padding:1rem 1.25rem; }
                header { display:flex; justify-content:space-between; gap:1rem; align-items:end; }
                h1 { margin:0; font-size:1.4rem; }
                #meta { color:var(--muted); }
                .stage { background:#000; min-height:60vh; display:grid; place-items:center; }
                video { width:min(1280px,100%); max-height:78vh; background:#000; }
                .bar { margin:1rem 1.25rem; padding:1rem; background:var(--panel); border-radius:16px; }
                a { color:var(--accent); }
              </style>
            </head>
            <body>
              <header>
                <div>
                  <h1>Live-Dub</h1>
                  <div id="meta">Warte auf Spiel …</div>
                </div>
                <a href="/result">Zum Endergebnis</a>
              </header>
              <div class="stage"><video id="player" controls playsinline></video></div>
              <div class="bar" id="hint">Öffne diese Seite während eines Dub-Spiels. Der Server springt automatisch zur aktuellen Stelle.</div>
              <script>
                const player = document.getElementById('player');
                const meta = document.getElementById('meta');
                let lastMedia = '';
                let lastCue = '';
                async function tick() {
                  try {
                    const status = await fetch('/api/status', {cache:'no-store'}).then(r => r.json());
                    meta.textContent = [status.packName, status.state, status.playerName, status.clipTitle]
                      .filter(Boolean).join(' · ') || 'Kein aktives Dub-Spiel';
                    if (status.hasVideo && status.media && status.media !== lastMedia) {
                      lastMedia = status.media;
                      player.src = '/media/video?v=' + encodeURIComponent(status.media);
                    }
                    const cue = status.state + ':' + status.offsetSeconds + ':' + status.updatedAt;
                    if (status.hasVideo && (status.state === 'PLAYING_REFERENCE' || status.state === 'RECORDING' || status.state === 'REPLAY' || status.state === 'RESULT')) {
                      if (cue !== lastCue) {
                        lastCue = cue;
                        const target = Number(status.offsetSeconds) || 0;
                        const seek = () => {
                          try { player.currentTime = target; player.play().catch(() => {}); } catch (e) {}
                        };
                        if (player.readyState >= 1) seek(); else player.addEventListener('loadedmetadata', seek, {once:true});
                      }
                    }
                  } catch (e) {}
                }
                tick();
                setInterval(tick, 500);
              </script>
            </body>
            </html>
            """;

    private static final String RESULT_HTML = """
            <!doctype html>
            <html lang="de">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Choicer Voicer Ergebnis</title>
              <style>
                :root { color-scheme: dark; --bg:#0b0d10; --panel:#151a21; --ink:#f5f2eb; --muted:#98a2ae; --accent:#ff8a3d; }
                * { box-sizing: border-box; }
                body { margin:0; background:var(--bg); color:var(--ink); font:15px/1.45 "Segoe UI",sans-serif; }
                header { padding:1rem 1.25rem; display:flex; justify-content:space-between; align-items:end; }
                h1 { margin:0; font-size:1.4rem; }
                #meta { color:var(--muted); }
                .stage { background:#000; display:grid; place-items:center; }
                video { width:min(1280px,100%); max-height:70vh; background:#000; }
                .bar { margin:1rem 1.25rem; padding:1rem; background:var(--panel); border-radius:16px; display:flex; gap:.8rem; flex-wrap:wrap; }
                button, a { appearance:none; border:0; border-radius:999px; padding:.8rem 1.1rem; font-weight:700; cursor:pointer; text-decoration:none; }
                button { background:var(--accent); color:#111; }
                a { background:transparent; color:var(--ink); border:1px solid #3a4553; }
                ul { margin:0 1.25rem 1.5rem; padding:0; list-style:none; color:var(--muted); }
                li { padding:.35rem 0; border-bottom:1px solid #243040; }
              </style>
            </head>
            <body>
              <header>
                <div>
                  <h1>Endergebnis</h1>
                  <div id="meta">Noch kein Ergebnis</div>
                </div>
                <a href="/watch">Zur Live-Ansicht</a>
              </header>
              <div class="stage"><video id="player" controls playsinline></video></div>
              <div class="bar">
                <button id="play">Ergebnis abspielen</button>
              </div>
              <ul id="takes"></ul>
              <script>
                const player = document.getElementById('player');
                const meta = document.getElementById('meta');
                const takesEl = document.getElementById('takes');
                let takes = [];
                let timers = [];
                function clearTimers() { timers.forEach(clearTimeout); timers = []; }
                async function load() {
                  const data = await fetch('/api/result', {cache:'no-store'}).then(r => r.json());
                  if (!data.hasResult) {
                    meta.textContent = 'Noch kein Endergebnis vorhanden';
                    return;
                  }
                  meta.textContent = data.packName + ' · ' + data.takes.length + ' Takes';
                  player.src = '/media/video?v=' + encodeURIComponent(data.media || '');
                  takes = data.takes || [];
                  takesEl.innerHTML = takes.map(t =>
                    `<li>#${t.index + 1} ${t.playerName || 'Spieler'} · ${t.clipTitle || ''} @ ${Number(t.timestampSeconds).toFixed(3)}s</li>`
                  ).join('');
                }
                document.getElementById('play').onclick = async () => {
                  clearTimers();
                  player.currentTime = 0;
                  await player.play().catch(() => {});
                  for (const take of takes) {
                    const audio = new Audio(take.audioUrl);
                    const delay = Math.max(0, Number(take.timestampSeconds) * 1000);
                    timers.push(setTimeout(() => { audio.play().catch(() => {}); }, delay));
                  }
                };
                load();
                setInterval(load, 2000);
              </script>
            </body>
            </html>
            """;
}
