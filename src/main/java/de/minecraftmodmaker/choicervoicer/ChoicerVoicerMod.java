package de.minecraftmodmaker.choicervoicer;

import de.minecraftmodmaker.choicervoicer.command.ChoicerCommands;
import de.minecraftmodmaker.choicervoicer.config.ModConfig;
import de.minecraftmodmaker.choicervoicer.game.GameSession;
import de.minecraftmodmaker.choicervoicer.network.DubPayloads;
import de.minecraftmodmaker.choicervoicer.network.VideoTransferManager;
import de.minecraftmodmaker.choicervoicer.pack.PackManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public final class ChoicerVoicerMod implements ModInitializer {
    public static final String MOD_ID = "choicer_voicer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static ChoicerVoicerMod instance;

    private ModConfig config;
    private PackManager packs;
    private GameSession session;
    private VideoTransferManager videoTransfers;

    public static ChoicerVoicerMod instance() {
        if (instance == null) {
            throw new IllegalStateException("Choicer Voicer has not initialized yet");
        }
        return instance;
    }

    @Override
    public void onInitialize() {
        instance = this;
        Path root = FabricLoader.getInstance().getConfigDir().resolve("choicer_voicer");
        config = ModConfig.load(root.resolve("config.json"), LOGGER);
        packs = new PackManager(root, config, LOGGER);
        videoTransfers = new VideoTransferManager(LOGGER);
        DubPayloads.register();
        ServerPlayNetworking.registerGlobalReceiver(DubPayloads.VideoReady.TYPE,
                (payload, context) -> videoTransfers.markReady(context.player(), payload.assetKey()));
        try {
            packs.initialize();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialize Choicer Voicer pack directories", exception);
        }

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ChoicerCommands.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                session = new GameSession(server, packs, config, videoTransfers, LOGGER));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            videoTransfers.tick(id -> server.getPlayerList().getPlayer(id));
            GameSession current = session;
            if (current != null) {
                current.tick();
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (session != null) {
                session.stop(null);
                session = null;
            }
            videoTransfers.close();
        });
        LOGGER.info("Choicer Voicer initialized with {} voice packs and {} judge packs",
                packs.registry().voicePacks().size(), packs.registry().judgePacks().size());
    }

    public ModConfig config() {
        return config;
    }

    public PackManager packs() {
        return packs;
    }

    public GameSession session() {
        if (session == null) {
            throw new IllegalStateException("No Minecraft server is running");
        }
        return session;
    }
}
