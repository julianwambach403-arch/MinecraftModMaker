package de.minecraftmodmaker.choicervoicer.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class DubPayloads {
    public static final int CHUNK_BYTES = 48 * 1024;
    public static final int MAX_PACKET_BYTES = CHUNK_BYTES + 1024;

    private DubPayloads() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().registerLarge(
                VideoChunk.TYPE, VideoChunk.CODEC, MAX_PACKET_BYTES);
        PayloadTypeRegistry.clientboundPlay().register(VideoControl.TYPE, VideoControl.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(VideoReady.TYPE, VideoReady.CODEC);
    }

    public record VideoChunk(String assetKey, int index, int total, byte[] data)
            implements CustomPacketPayload {
        public static final Type<VideoChunk> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("choicer_voicer", "video_chunk"));
        public static final StreamCodec<RegistryFriendlyByteBuf, VideoChunk> CODEC =
                CustomPacketPayload.codec(VideoChunk::write, VideoChunk::read);

        private static VideoChunk read(RegistryFriendlyByteBuf buffer) {
            return new VideoChunk(buffer.readUtf(160), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readByteArray(CHUNK_BYTES));
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeUtf(assetKey, 160);
            buffer.writeVarInt(index);
            buffer.writeVarInt(total);
            buffer.writeByteArray(data);
        }

        @Override
        public Type<VideoChunk> type() {
            return TYPE;
        }
    }

    public record VideoControl(String assetKey, Action action, long offsetMillis, long durationMillis)
            implements CustomPacketPayload {
        public enum Action {
            PLAY, STOP
        }

        public static final Type<VideoControl> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("choicer_voicer", "video_control"));
        public static final StreamCodec<RegistryFriendlyByteBuf, VideoControl> CODEC =
                CustomPacketPayload.codec(VideoControl::write, VideoControl::read);

        private static VideoControl read(RegistryFriendlyByteBuf buffer) {
            int action = buffer.readVarInt();
            if (action < 0 || action >= Action.values().length) {
                action = Action.STOP.ordinal();
            }
            return new VideoControl(buffer.readUtf(160), Action.values()[action],
                    buffer.readLong(), buffer.readLong());
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeVarInt(action.ordinal());
            buffer.writeUtf(assetKey, 160);
            buffer.writeLong(offsetMillis);
            buffer.writeLong(durationMillis);
        }

        @Override
        public Type<VideoControl> type() {
            return TYPE;
        }
    }

    public record VideoReady(String assetKey) implements CustomPacketPayload {
        public static final Type<VideoReady> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("choicer_voicer", "video_ready"));
        public static final StreamCodec<RegistryFriendlyByteBuf, VideoReady> CODEC =
                CustomPacketPayload.codec(VideoReady::write, VideoReady::read);

        private static VideoReady read(RegistryFriendlyByteBuf buffer) {
            return new VideoReady(buffer.readUtf(160));
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeUtf(assetKey, 160);
        }

        @Override
        public Type<VideoReady> type() {
            return TYPE;
        }
    }
}
