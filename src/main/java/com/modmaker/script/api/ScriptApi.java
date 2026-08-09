package com.modmaker.script.api;

import com.modmaker.ModMaker;
import com.modmaker.script.RhinoCalls;
import com.modmaker.script.ScriptEvents;
import com.modmaker.script.ScriptManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The global {@code mm} object available to every ModMaker script.
 *
 * <p>Example:
 * <pre>{@code
 * mm.onUse("magic_wand", function(player, x, y, z) {
 *     player.tell("Zap!");
 *     mm.broadcast(player.name() + " used the wand");
 * });
 * }</pre>
 */
public class ScriptApi {
	private final ScriptManager manager;

	public ScriptApi(ScriptManager manager) {
		this.manager = manager;
	}

	// ------------------------------------------------------------------ callbacks

	public interface TickCallback {
		void run();
	}

	public interface ChatCallback {
		void run(PlayerApi player, String message);
	}

	public interface JoinCallback {
		void run(PlayerApi player);
	}

	// ------------------------------------------------------------------ event registration
	// JS functions are kept as raw Rhino Function objects and invoked via RhinoCalls
	// (interface adapters would create reflection proxies blocked by the sandbox).

	/** Fired when a player right-clicks with the ModMaker item {@code defId} ("*" = any). */
	public void onUse(String defId, org.mozilla.javascript.Function callback) {
		ScriptEvents.onItemUse(defId, (player, pos) -> {
			if (player instanceof ServerPlayer serverPlayer) {
				RhinoCalls.call(manager, callback, new PlayerApi(serverPlayer), pos.getX(), pos.getY(), pos.getZ());
			}
		});
	}

	/** Fired when a player breaks the ModMaker block {@code defId} ("*" = any). */
	public void onBlockBreak(String defId, org.mozilla.javascript.Function callback) {
		ScriptEvents.onBlockBreak(defId, (player, pos) -> {
			if (player instanceof ServerPlayer serverPlayer) {
				RhinoCalls.call(manager, callback, new PlayerApi(serverPlayer), pos.getX(), pos.getY(), pos.getZ());
			}
		});
	}

	/** Fired when a player right-clicks the ModMaker block {@code defId} ("*" = any). */
	public void onBlockUse(String defId, org.mozilla.javascript.Function callback) {
		ScriptEvents.onBlockUse(defId, (player, pos) -> {
			if (player instanceof ServerPlayer serverPlayer) {
				RhinoCalls.call(manager, callback, new PlayerApi(serverPlayer), pos.getX(), pos.getY(), pos.getZ());
			}
		});
	}

	/** Runs the callback every {@code intervalTicks} server ticks (20 ticks = 1 second). */
	public void onTick(int intervalTicks, org.mozilla.javascript.Function callback) {
		manager.addTickTask(intervalTicks, () -> RhinoCalls.call(manager, callback));
	}

	/** Fired for every chat message. */
	public void onChat(org.mozilla.javascript.Function callback) {
		manager.addChatHandler((player, message) -> RhinoCalls.call(manager, callback, player, message));
	}

	/** Fired when a player joins the server/world. */
	public void onJoin(org.mozilla.javascript.Function callback) {
		manager.addJoinHandler(player -> RhinoCalls.call(manager, callback, player));
	}

	// ------------------------------------------------------------------ actions

	/** Sends a chat message to every online player. */
	public void broadcast(String message) {
		MinecraftServer server = ModMaker.server();
		if (server == null) return;
		server.getPlayerList().broadcastSystemMessage(Component.literal(String.valueOf(message)), false);
	}

	/** Runs a command as the server console (e.g. {@code mm.runCommand("time set day")}). */
	public void runCommand(String command) {
		MinecraftServer server = ModMaker.server();
		if (server == null) return;
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), String.valueOf(command));
	}

	public void log(String message) {
		ModMaker.LOGGER.info("[Script] {}", message);
	}

	public double random(double min, double max) {
		return min + ThreadLocalRandom.current().nextDouble() * (max - min);
	}

	public int randomInt(int min, int max) {
		return ThreadLocalRandom.current().nextInt(min, max + 1);
	}
}
