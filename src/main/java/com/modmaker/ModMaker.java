package com.modmaker;

import com.modmaker.content.ContentManager;
import com.modmaker.registry.PlaceholderPool;
import com.modmaker.script.ScriptManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModMaker implements ModInitializer {
	public static final String MOD_ID = "modmaker";
	public static final Logger LOGGER = LoggerFactory.getLogger("ModMaker");

	private static volatile MinecraftServer server;

	@Override
	public void onInitialize() {
		ModMakerPaths.createAll();
		PlaceholderPool.registerAll();
		ContentManager.get().loadAll();
		ScriptManager.get().init();
		EventBridge.register();
		ModMakerCommands.register();
		LOGGER.info("ModMaker ready ({} item slots, {} block slots)",
				PlaceholderPool.ITEM_SLOTS, PlaceholderPool.BLOCK_SLOTS);
	}

	/** The running server (integrated or dedicated), null while no world is loaded. */
	public static MinecraftServer server() {
		return server;
	}

	public static void setServer(MinecraftServer newServer) {
		server = newServer;
	}
}
