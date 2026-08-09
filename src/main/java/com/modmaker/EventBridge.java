package com.modmaker;

import com.modmaker.registry.StackStamper;
import com.modmaker.script.ScriptManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Wires Fabric events to the script engine, the stack stamper and the runtime data pack. */
public final class EventBridge {
	private EventBridge() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			ModMaker.setServer(server);
			com.modmaker.pack.RuntimeDataPack.writeToWorld(server);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ModMaker.setServer(null));

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ScriptManager.get().tick(server);
			if (server.getTickCount() % 20 == 0) {
				StackStamper.tickPlayers(server);
			}
		});

		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) ->
				ScriptManager.get().fireChat(sender, message.signedContent()));

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				ScriptManager.get().fireJoin(handler.player));
	}
}
