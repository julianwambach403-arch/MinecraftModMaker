package com.modmaker.client;

import com.modmaker.ModMaker;
import com.modmaker.content.ContentManager;
import com.modmaker.pack.RuntimeResourcePack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;

public class ModMakerClient implements ClientModInitializer {
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
			});
		});
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
