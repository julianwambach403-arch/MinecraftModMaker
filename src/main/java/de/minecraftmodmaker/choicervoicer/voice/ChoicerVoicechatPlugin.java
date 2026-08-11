package de.minecraftmodmaker.choicervoicer.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChoicerVoicechatPlugin implements VoicechatPlugin {
    public static final String PLUGIN_ID = "choicer_voicer";
    public static final String CATEGORY_ID = "choicer_voicer";
    private static final ChoicerVoicechatPlugin INSTANCE = new ChoicerVoicechatPlugin();

    private final Map<UUID, Recording> recordings = new ConcurrentHashMap<>();
    private volatile VoicechatApi api;
    private volatile VoicechatServerApi serverApi;

    public ChoicerVoicechatPlugin() {
    }

    public static ChoicerVoicechatPlugin instance() {
        return INSTANCE;
    }

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        INSTANCE.api = api;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, INSTANCE::serverStarted);
        registration.registerEvent(MicrophonePacketEvent.class, INSTANCE::microphonePacket);
    }

    private void serverStarted(VoicechatServerStartedEvent event) {
        serverApi = event.getVoicechat();
        serverApi.registerVolumeCategory(serverApi.volumeCategoryBuilder()
                .setId(CATEGORY_ID)
                .setName("Choicer Voicer")
                .setDescription("Voice samples and judge reactions")
                .build());
    }

    private void microphonePacket(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null) {
            return;
        }
        Recording recording = recordings.get(sender.getPlayer().getUuid());
        if (recording != null) {
            recording.accept(event.getPacket().getOpusEncodedData());
        }
    }

    public boolean isReady() {
        return api != null && serverApi != null;
    }

    public boolean isConnected(UUID playerId) {
        VoicechatServerApi current = serverApi;
        VoicechatConnection connection = current == null ? null : current.getConnectionOf(playerId);
        return connection != null && connection.isConnected() && !connection.isDisabled();
    }

    @Nullable
    public AudioPlayer play(short[] samples, Collection<UUID> targets, Runnable onStopped) {
        VoicechatServerApi current = serverApi;
        if (current == null || samples.length == 0) {
            return null;
        }
        StaticAudioChannel channel = current.createStaticAudioChannel(UUID.randomUUID());
        if (channel == null) {
            return null;
        }
        channel.setCategory(CATEGORY_ID);
        channel.setBypassGroupIsolation(true);
        for (UUID target : targets) {
            VoicechatConnection connection = current.getConnectionOf(target);
            if (connection != null && connection.isConnected()) {
                channel.addTarget(connection);
            }
        }
        AudioPlayer player = current.createAudioPlayer(channel, current.createEncoder(), samples);
        player.setOnStopped(() -> {
            channel.clearTargets();
            channel.flush();
            onStopped.run();
        });
        player.startPlaying();
        return player;
    }

    public boolean beginRecording(UUID playerId) {
        VoicechatApi current = api;
        if (current == null || !isConnected(playerId)) {
            return false;
        }
        Recording next = new Recording(current.createDecoder());
        Recording previous = recordings.putIfAbsent(playerId, next);
        if (previous != null) {
            next.close();
            return false;
        }
        return true;
    }

    public short[] finishRecording(UUID playerId) {
        Recording recording = recordings.remove(playerId);
        return recording == null ? new short[0] : recording.finish();
    }

    public void cancelRecording(UUID playerId) {
        Recording recording = recordings.remove(playerId);
        if (recording != null) {
            recording.close();
        }
    }

    private static final class Recording {
        private static final int MAX_SAMPLES = 48_000 * 65;
        private final OpusDecoder decoder;
        private short[] samples = new short[48_000];
        private int size;
        private boolean closed;

        private Recording(OpusDecoder decoder) {
            this.decoder = decoder;
        }

        private synchronized void accept(byte[] opus) {
            if (closed || size >= MAX_SAMPLES) {
                return;
            }
            short[] decoded = decoder.decode(opus);
            int accepted = Math.min(decoded.length, MAX_SAMPLES - size);
            ensureCapacity(size + accepted);
            System.arraycopy(decoded, 0, samples, size, accepted);
            size += accepted;
        }

        private synchronized short[] finish() {
            short[] result = new short[size];
            System.arraycopy(samples, 0, result, 0, size);
            close();
            samples = new short[0];
            size = 0;
            return result;
        }

        private void ensureCapacity(int required) {
            if (required <= samples.length) {
                return;
            }
            int capacity = Math.min(MAX_SAMPLES, Math.max(required, samples.length * 2));
            short[] replacement = new short[capacity];
            System.arraycopy(samples, 0, replacement, 0, size);
            samples = replacement;
        }

        private synchronized void close() {
            if (!closed) {
                decoder.close();
                closed = true;
            }
        }
    }
}
