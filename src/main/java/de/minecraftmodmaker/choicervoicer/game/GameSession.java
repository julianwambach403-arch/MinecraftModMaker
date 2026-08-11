package de.minecraftmodmaker.choicervoicer.game;

import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.minecraftmodmaker.choicervoicer.audio.AudioCodec;
import de.minecraftmodmaker.choicervoicer.audio.SimilarityScorer;
import de.minecraftmodmaker.choicervoicer.config.ModConfig;
import de.minecraftmodmaker.choicervoicer.pack.ContentPacks;
import de.minecraftmodmaker.choicervoicer.pack.PackManager;
import de.minecraftmodmaker.choicervoicer.voice.ChoicerVoicechatPlugin;
import de.minecraftmodmaker.choicervoicer.web.DubWebPortal;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class GameSession {
    public enum State {
        IDLE, LOBBY, PLAYING_REFERENCE, COUNTDOWN, RECORDING, JUDGING, REPLAY
    }

    private final MinecraftServer server;
    private final PackManager packs;
    private final ModConfig config;
    private final DubWebPortal web;
    private final ChoicerVoicechatPlugin voice;
    private final SimilarityScorer scorer;
    private final Logger logger;
    private final List<UUID> participants = new ArrayList<>();
    private final Map<UUID, Integer> points = new LinkedHashMap<>();
    private final List<Performance> performances = new ArrayList<>();

    private State state = State.IDLE;
    private ContentPacks.VoicePack voicePack;
    private ContentPacks.JudgePack judgePack;
    private ContentPacks.VoiceClip clip;
    private short[] reference = new short[0];
    private SimilarityScorer.Score score;
    private long stateDeadlineTick;
    private int playerIndex;
    private int round;
    private int revealedJudges;
    private int replayIndex;
    private long replayStartedTick;
    private long replayDurationTicks;
    private AudioPlayer activeAudio;

    public GameSession(MinecraftServer server, PackManager packs, ModConfig config,
                       DubWebPortal web, Logger logger) {
        this.server = server;
        this.packs = packs;
        this.config = config;
        this.web = web;
        this.voice = ChoicerVoicechatPlugin.instance();
        this.scorer = new SimilarityScorer();
        this.logger = logger;
    }

    public synchronized State state() {
        return state;
    }

    public synchronized String status() {
        if (state == State.IDLE) {
            return "Kein Spiel aktiv.";
        }
        String status = "Status: " + state + ", Pack: " + voicePack.displayName() + ", Spieler: "
                + participants.size() + ", Runde: " + (round + 1) + "/" + config.rounds();
        if (voicePack.isDubPack() && web.enabled()) {
            status += ", Web: " + web.publicBaseUrl();
        }
        return status;
    }

    public synchronized boolean start(ContentPacks.VoicePack selected, ServerPlayer host) {
        if (state != State.IDLE || !voice.isReady() || !voice.isConnected(host.getUUID())) {
            return false;
        }
        if (selected.isDubPack() && !web.enabled()) {
            return false;
        }
        voicePack = selected;
        judgePack = packs.registry().judgePacks().values().stream().findFirst().orElse(null);
        participants.clear();
        points.clear();
        performances.clear();
        participants.add(host.getUUID());
        points.put(host.getUUID(), 0);
        playerIndex = 0;
        round = 0;
        state = State.LOBBY;
        stateDeadlineTick = server.getTickCount() + 200;
        if (selected.isDubPack()) {
            web.preparePack(selected);
            web.publishLive("LOBBY", selected, host.getScoreboardName(), "", 0D, 0D);
        }
        broadcast(Component.literal("Choicer Voicer: " + selected.displayName()).withStyle(ChatFormatting.GOLD));
        broadcast(Component.literal("10 Sekunden Lobby – /choicervoicer join zum Mitspielen."));
        if (selected.isDubPack()) {
            broadcast(Component.literal("Dub-Video im Browser: " + web.publicBaseUrl() + "/watch")
                    .withStyle(ChatFormatting.AQUA));
            broadcast(Component.literal("Endergebnis später unter: " + web.publicBaseUrl() + "/result")
                    .withStyle(ChatFormatting.AQUA));
        }
        return true;
    }

    public synchronized boolean join(ServerPlayer player) {
        if (state != State.LOBBY || participants.contains(player.getUUID())
                || !voice.isConnected(player.getUUID())) {
            return false;
        }
        participants.add(player.getUUID());
        points.put(player.getUUID(), 0);
        broadcast(Component.literal(player.getScoreboardName() + " spielt mit.").withStyle(ChatFormatting.GREEN));
        if (voicePack.isDubPack() && web.enabled()) {
            player.sendSystemMessage(Component.literal("Dub-Video: " + web.publicBaseUrl() + "/watch")
                    .withStyle(ChatFormatting.AQUA), false);
        }
        return true;
    }

    public synchronized boolean leave(ServerPlayer player) {
        int index = participants.indexOf(player.getUUID());
        if (index < 0) {
            return false;
        }
        participants.remove(index);
        points.remove(player.getUUID());
        voice.cancelRecording(player.getUUID());
        if (index < playerIndex) {
            playerIndex--;
        }
        if (participants.isEmpty()) {
            stop("Keine Teilnehmer mehr.");
        }
        return true;
    }

    public synchronized void disconnect(UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            leave(player);
            return;
        }
        int index = participants.indexOf(playerId);
        if (index >= 0) {
            participants.remove(index);
            points.remove(playerId);
            voice.cancelRecording(playerId);
            if (participants.isEmpty()) {
                stop("Keine Teilnehmer mehr.");
            }
        }
    }

    public synchronized void tick() {
        if (state == State.IDLE) {
            return;
        }
        participants.removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        if (participants.isEmpty()) {
            stop("Keine Teilnehmer mehr.");
            return;
        }
        long tick = server.getTickCount();
        switch (state) {
            case LOBBY -> {
                if (tick >= stateDeadlineTick) {
                    prepareTurn();
                }
            }
            case COUNTDOWN -> {
                long remaining = stateDeadlineTick - tick;
                if (remaining > 0 && remaining % 20 == 0) {
                    actionbar(activePlayer(), Component.literal(Long.toString(remaining / 20))
                            .withStyle(ChatFormatting.YELLOW));
                }
                if (tick >= stateDeadlineTick) {
                    startRecording();
                }
            }
            case RECORDING -> {
                if (tick >= stateDeadlineTick) {
                    evaluate();
                }
            }
            case JUDGING -> {
                if (tick >= stateDeadlineTick) {
                    revealNextJudge();
                }
            }
            case REPLAY -> tickReplay(tick);
            default -> {
            }
        }
    }

    private void prepareTurn() {
        ServerPlayer player = activePlayer();
        if (player == null) {
            advanceTurn();
            return;
        }
        List<ContentPacks.VoiceClip> clips = voicePack.clips();
        if (voicePack.isDubPack()) {
            List<ContentPacks.VoiceClip> dubClips = clips.stream()
                    .filter(ContentPacks.VoiceClip::isDubClip)
                    .sorted(Comparator.comparingDouble(ContentPacks.VoiceClip::dubTimestampSeconds))
                    .toList();
            int turn = round * participants.size() + playerIndex;
            clip = dubClips.get(turn % dubClips.size());
        } else {
            clip = clips.get(ThreadLocalRandom.current().nextInt(clips.size()));
        }
        try {
            reference = AudioCodec.decode(clip.audio());
        } catch (IOException exception) {
            logger.warn("Skipping invalid clip {}", clip.audio(), exception);
            broadcast(Component.literal("Clip konnte nicht geladen werden; nächster Versuch.")
                    .withStyle(ChatFormatting.RED));
            advanceTurn();
            return;
        }
        state = State.PLAYING_REFERENCE;
        broadcast(Component.literal(player.getScoreboardName() + " ist dran: " + clip.title())
                .withStyle(ChatFormatting.AQUA));
        clip.caption().filter(caption -> !caption.isBlank()).ifPresent(caption ->
                player.sendSystemMessage(Component.literal("Text: " + caption), false));
        if (voicePack.isDubPack()) {
            web.publishLive("PLAYING_REFERENCE", voicePack, player.getScoreboardName(), clip.title(),
                    clip.dubTimestampSeconds(), AudioCodec.durationSeconds(reference));
        }
        activeAudio = voice.play(reference, List.of(player.getUUID()),
                () -> server.execute(this::beginCountdown));
        if (activeAudio == null) {
            stop("Das Hörbeispiel konnte nicht abgespielt werden.");
        }
    }

    private synchronized void beginCountdown() {
        if (state != State.PLAYING_REFERENCE) {
            return;
        }
        state = State.COUNTDOWN;
        stateDeadlineTick = server.getTickCount() + config.countdownSeconds() * 20L;
        if (voicePack.isDubPack()) {
            web.publishLive("COUNTDOWN", voicePack,
                    activePlayer() == null ? "" : activePlayer().getScoreboardName(),
                    clip == null ? "" : clip.title(),
                    clip == null ? 0D : clip.dubTimestampSeconds(), 0D);
        }
    }

    private void startRecording() {
        ServerPlayer player = activePlayer();
        if (player == null || !voice.beginRecording(player.getUUID())) {
            stop("Voice Chat des aktiven Spielers ist nicht verbunden.");
            return;
        }
        state = State.RECORDING;
        long durationTicks = Math.max(20L, Math.round(AudioCodec.durationSeconds(reference) * 20D));
        stateDeadlineTick = server.getTickCount() + durationTicks
                + Math.round(config.recordingTailMillis() / 50D);
        if (voicePack.isDubPack()) {
            web.publishLive("RECORDING", voicePack, player.getScoreboardName(), clip.title(),
                    clip.dubTimestampSeconds(),
                    AudioCodec.durationSeconds(reference) + config.recordingTailMillis() / 1000D);
        }
        actionbar(player, Component.literal("JETZT SPRECHEN!").withStyle(ChatFormatting.RED));
    }

    private void evaluate() {
        ServerPlayer player = activePlayer();
        if (player == null) {
            advanceTurn();
            return;
        }
        short[] performance = voice.finishRecording(player.getUUID());
        if (voicePack.isDubPack() && performance.length > 0) {
            performances.add(new Performance(player.getScoreboardName(), clip.title(),
                    clip.dubTimestampSeconds(), performance));
        }
        score = scorer.score(reference, performance, config.silenceThreshold());
        points.computeIfPresent(player.getUUID(), (ignored, current) -> current + score.points());
        state = State.JUDGING;
        revealedJudges = 0;
        stateDeadlineTick = server.getTickCount() + 10;
        if (voicePack.isDubPack()) {
            web.publishLive("JUDGING", voicePack, player.getScoreboardName(), clip.title(),
                    clip.dubTimestampSeconds(), 0D);
        }
        if (score.silent()) {
            broadcast(Component.literal("Keine verständliche Aufnahme erkannt.").withStyle(ChatFormatting.RED));
        }
    }

    private void revealNextJudge() {
        if (revealedJudges >= 5) {
            ServerPlayer player = activePlayer();
            int total = player == null ? 0 : points.getOrDefault(player.getUUID(), 0);
            broadcast(Component.literal("Ergebnis: " + score.judgeVotes() + "/5 Stimmen, "
                    + Math.round(score.similarity() * 100D) + "% Ähnlichkeit, Gesamt " + total + " Punkte.")
                    .withStyle(ChatFormatting.GOLD));
            advanceTurn();
            return;
        }
        int judgeNumber = ++revealedJudges;
        boolean vote = judgeNumber <= score.judgeVotes();
        String judgeName = judgePack == null ? "Jury " + judgeNumber
                : judgePack.judges().get(judgeNumber - 1).name().orElse("Jury " + judgeNumber);
        broadcast(Component.literal(judgeName + ": " + (vote ? "JA" : "NEIN"))
                .withStyle(vote ? ChatFormatting.GREEN : ChatFormatting.RED));
        if (vote && judgePack != null) {
            ContentPacks.Judge judge = judgePack.judges().get(judgeNumber - 1);
            judge.voice().or(judge::scoreBlip).ifPresent(path -> {
                try {
                    voice.play(AudioCodec.decode(path), List.copyOf(participants), () -> {
                    });
                } catch (IOException exception) {
                    logger.warn("Could not play judge audio {}", path, exception);
                }
            });
        }
        stateDeadlineTick = server.getTickCount() + 12;
    }

    private void advanceTurn() {
        playerIndex++;
        if (playerIndex >= participants.size()) {
            playerIndex = 0;
            round++;
        }
        if (round >= config.rounds()) {
            if (voicePack.isDubPack() && !performances.isEmpty()) {
                startReplay();
            } else {
                finish();
            }
            return;
        }
        state = State.COUNTDOWN;
        stateDeadlineTick = server.getTickCount() + 30;
        server.execute(this::prepareTurn);
    }

    private void startReplay() {
        performances.sort(Comparator.comparingDouble(Performance::timestampSeconds));
        replayIndex = 0;
        replayStartedTick = server.getTickCount();
        replayDurationTicks = performances.stream()
                .mapToLong(performance -> Math.round((performance.timestampSeconds()
                        + AudioCodec.durationSeconds(performance.samples()) + 2D) * 20D))
                .max().orElse(200L);
        state = State.REPLAY;
        List<DubWebPortal.Take> takes = performances.stream()
                .map(performance -> new DubWebPortal.Take(
                        performance.playerName(),
                        performance.clipTitle(),
                        performance.timestampSeconds(),
                        performance.samples()))
                .toList();
        web.publishResult(voicePack, takes);
        web.publishLive("REPLAY", voicePack, "", "", 0D, replayDurationTicks / 20D);
        broadcast(Component.literal("Dub-Endergebnis im Browser: " + web.publicBaseUrl() + "/result")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        voicePack.backingTrack().ifPresent(path -> {
            try {
                voice.play(AudioCodec.decode(path), List.copyOf(participants), () -> {
                });
            } catch (IOException exception) {
                logger.warn("Could not play dub backing track {}", path, exception);
            }
        });
    }

    private void tickReplay(long tick) {
        if (tick < replayStartedTick) {
            return;
        }
        double elapsedSeconds = (tick - replayStartedTick) / 20D;
        while (replayIndex < performances.size()
                && performances.get(replayIndex).timestampSeconds() <= elapsedSeconds) {
            Performance performance = performances.get(replayIndex++);
            voice.play(performance.samples(), List.copyOf(participants), () -> {
            });
        }
        if (tick - replayStartedTick >= replayDurationTicks) {
            finish();
        }
    }

    private void finish() {
        broadcast(Component.literal("Endstand").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        points.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> {
                    ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                    String name = player == null ? entry.getKey().toString() : player.getScoreboardName();
                    broadcast(Component.literal(name + ": " + entry.getValue() + " Punkte"));
                });
        if (voicePack != null && voicePack.isDubPack() && web.enabled()) {
            broadcast(Component.literal("Ergebnisseite: " + web.publicBaseUrl() + "/result")
                    .withStyle(ChatFormatting.AQUA));
        }
        stop(null);
    }

    public synchronized void stop(String reason) {
        if (activeAudio != null && !activeAudio.isStopped()) {
            activeAudio.stopPlaying();
        }
        participants.forEach(voice::cancelRecording);
        if (voicePack != null && voicePack.isDubPack()) {
            web.clearLive();
        }
        if (reason != null) {
            broadcast(Component.literal("Choicer Voicer beendet: " + reason).withStyle(ChatFormatting.RED));
        }
        state = State.IDLE;
        participants.clear();
        points.clear();
        performances.clear();
        reference = new short[0];
        activeAudio = null;
    }

    private ServerPlayer activePlayer() {
        return participants.isEmpty() || playerIndex >= participants.size() ? null
                : server.getPlayerList().getPlayer(participants.get(playerIndex));
    }

    private void broadcast(Component message) {
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static void actionbar(ServerPlayer player, Component message) {
        if (player != null) {
            player.sendSystemMessage(message, true);
        }
    }

    private record Performance(String playerName, String clipTitle, double timestampSeconds, short[] samples) {
    }
}
