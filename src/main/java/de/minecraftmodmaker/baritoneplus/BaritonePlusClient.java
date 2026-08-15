package de.minecraftmodmaker.baritoneplus;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import de.minecraftmodmaker.baritoneplus.ai.BrainRuntime;
import de.minecraftmodmaker.baritoneplus.ai.LootAssistProcess;
import de.minecraftmodmaker.baritoneplus.ai.PathTuner;
import de.minecraftmodmaker.baritoneplus.ai.StuckRecoveryProcess;
import de.minecraftmodmaker.baritoneplus.ai.SurvivalBrainProcess;
import de.minecraftmodmaker.baritoneplus.command.PlusCommandBootstrap;
import de.minecraftmodmaker.baritoneplus.config.PlusConfig;
import de.minecraftmodmaker.baritoneplus.hud.PlusHud;
import de.minecraftmodmaker.baritoneplus.input.PlusKeybinds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public final class BaritonePlusClient implements ClientModInitializer {
	private BrainRuntime runtime;
	private Path configFile;
	private boolean commandsRegistered;

	@Override
	public void onInitializeClient() {
		configFile = FabricLoader.getInstance().getConfigDir().resolve("baritoneplus.properties");
		runtime = new BrainRuntime(PlusConfig.load(configFile));
		try {
			runtime.config().save(configFile);
		} catch (Exception exception) {
			BaritonePlus.LOGGER.warn("Could not write default config: {}", exception.toString());
		}

		PlusHud.register(runtime);
		PlusKeybinds.register(runtime);

		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
		BaritonePlus.LOGGER.info("Baritone Plus client ready (bundled baritone-meteor 26.2)");
	}

	private void onTick(Minecraft client) {
		PlusKeybinds.tick(client, runtime);
		if (client.player == null || client.level == null) {
			commandsRegistered = false;
			return;
		}
		IBaritone baritone;
		try {
			baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
		} catch (Throwable throwable) {
			BaritonePlus.LOGGER.debug("Baritone is not ready yet: {}", throwable.toString());
			return;
		}
		if (baritone == null || baritone.getPlayerContext().player() == null) {
			return;
		}
		if (!runtime.isRegisteredFor(baritone)) {
			baritone.getPathingControlManager().registerProcess(new SurvivalBrainProcess(baritone, runtime));
			baritone.getPathingControlManager().registerProcess(new StuckRecoveryProcess(baritone, runtime));
			baritone.getPathingControlManager().registerProcess(new LootAssistProcess(baritone, runtime));
			runtime.markRegistered(baritone);
			BaritonePlus.LOGGER.info("Registered Plus AI processes on {}", baritone.getClass().getName());
		}
		if (!commandsRegistered) {
			PlusCommandBootstrap.register(baritone, runtime, configFile);
			commandsRegistered = true;
		}
		PathTuner.applyIfNeeded(runtime);
		if (!runtime.greeted()) {
			client.player.sendSystemMessage(Component.literal(
					"[Baritone Plus] Standalone pathfinder ready. #goto x z  |  #ore  |  #plus  |  R stop, G look-goto"));
			runtime.markGreeted();
		}
	}
}
