package de.minecraftmodmaker.choicervoicer.client;

import de.minecraftmodmaker.choicervoicer.network.DubPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public final class ChoicerVoicerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientVideoManager videos = new ClientVideoManager(
                FabricLoader.getInstance().getConfigDir().resolve("choicer_voicer/videos"));
        ClientPlayNetworking.registerGlobalReceiver(DubPayloads.VideoChunk.TYPE,
                (payload, context) -> context.client().execute(() -> videos.accept(payload)));
        ClientPlayNetworking.registerGlobalReceiver(DubPayloads.VideoControl.TYPE,
                (payload, context) -> context.client().execute(() -> videos.control(payload)));
    }
}
