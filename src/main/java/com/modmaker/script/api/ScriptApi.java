package com.modmaker.script.api;

import com.modmaker.ModMaker;
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

	public interface UseCallback {
		void run(PlayerApi player, int x, int y, int z);
	}

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

	/** Fired when a player right-clicks with the ModMaker item {@code defId} ("*" = any). */
	public void onUse(String defId, UseCallback callback) {
		ScriptEvents.onItemUse(defId, (player, pos) -> {
			if (player instanceof ServerPlayer serverPlayer) {
				callback.run(new PlayerApi(serverPlayer), pos.getX(), pos.getY(), pos.getZ());
			}
		});
	}

	/** Fired when a player breaks the ModMaker block {@code defId} ("*" = any). */
	public void onBlockBreak(String defId, UseCallback callback) {
		ScriptEvents.onBlockBreak(defId, (player, pos) -> {
			if (player instanceof ServerPlayer serverPlayer) {
				callback.run(new PlayerApi(serverPlayer), pos.getX(), pos.getY(), pos.getZ());
			}
		});
	}

	/** Fired when a player right-clicks the ModMaker block {@code defId} ("*" = any). */
	public void onBlockUse(String defId, UseCallback callback) {
		ScriptEvents.onBlockUse(defId, (player, pos) -> {
			if (player instanceof ServerPlayer serverPlayer) {
				callback.run(new PlayerApi(serverPlayer), pos.getX(), pos.getY(), pos.getZ());
			}
		});
	}

	/** Runs the callback every {@code intervalTicks} server ticks (20 ticks = 1 second). */
	public void onTick(int intervalTicks, TickCallback callback) {
		manager.addTickTask(intervalTicks, callback);
	}

	/** Fired for every chat message. */
	public void onChat(ChatCallback callback) {
		manager.addChatHandler(callback);
	}

	/** Fired when a player joins the server/world. */
	public void onJoin(JoinCallback callback) {
		manager.addJoinHandler(callback);
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
