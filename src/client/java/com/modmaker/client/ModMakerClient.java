package com.modmaker.client;

import com.modmaker.ModMaker;
import com.modmaker.client.gui.WorkspaceScreen;
import com.modmaker.content.ContentManager;
import com.modmaker.pack.RuntimeResourcePack;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class ModMakerClient implements ClientModInitializer {
	private static KeyMapping openWorkspaceKey;

	@Override
	public void onInitializeClient() {
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			boolean contentChanged = RuntimeResourcePack.writeFull();
			boolean justEnabled = enablePack(client);
			if (contentChanged || justEnabled) {
				client.reloadResourcePacks();
			}
		});

		// Whenever content changes (GUI save, /modmaker reload, imports), refresh the pack
		// and only run the (slow) client resource reload when files actually changed.
		ContentManager.get().addChangeListener(() -> {
			Minecraft client = Minecraft.getInstance();
			client.execute(() -> {
				boolean changed = RuntimeResourcePack.writeFull();
				if (changed) {
					client.reloadResourcePacks();
				}
				refreshCreativeTabs(client);
			});
		});

		openWorkspaceKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.modmaker.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, KeyMapping.Category.MISC));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openWorkspaceKey.consumeClick()) {
				client.gui.setScreen(new WorkspaceScreen(client.gui.screen()));
			}
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
				dispatcher.register(ClientCommands.literal("modmakergui").executes(context -> {
					Minecraft client = Minecraft.getInstance();
					client.schedule(() -> client.gui.setScreen(new WorkspaceScreen(null)));
					return 1;
				})));
	}

	/** Rebuilds creative tab contents so newly created elements appear without re-entering the world. */
	private static void refreshCreativeTabs(Minecraft client) {
		if (client.level == null || client.player == null) return;
		try {
			net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(
					client.level.enabledFeatures(),
					client.player.canUseGameMasterBlocks(),
					client.level.registryAccess());
		} catch (Exception e) {
			ModMaker.LOGGER.warn("ModMaker could not refresh creative tabs: {}", e.toString());
		}
	}

	/** Adds the runtime pack to the enabled resource packs; returns true when newly added. */
	private static boolean enablePack(Minecraft client) {
		if (client.options.resourcePacks.contains(RuntimeResourcePack.PACK_ID)) {
			return false;
		}
		client.options.resourcePacks.add(RuntimeResourcePack.PACK_ID);
		client.options.save();
		ModMaker.LOGGER.info("ModMaker enabled its runtime resource pack");
		return true;
	}
}
