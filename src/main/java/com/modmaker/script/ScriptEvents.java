package com.modmaker.script;

import com.modmaker.ModMaker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dispatch hub between game events and script handlers. Scripts register handlers through
 * the {@code mm} API; game code (dynamic items/blocks, Fabric event bridges) fires them here.
 * Kept separate from the Rhino engine so registry classes have no scripting dependency.
 */
public final class ScriptEvents {
	/** Handler key used to listen to events from any item/block. */
	public static final String ANY = "*";

	public interface PlayerHandler {
		void handle(Player player, BlockPos pos);
	}

	private static final Map<String, List<PlayerHandler>> ITEM_USE = new ConcurrentHashMap<>();
	private static final Map<String, List<PlayerHandler>> BLOCK_BREAK = new ConcurrentHashMap<>();
	private static final Map<String, List<PlayerHandler>> BLOCK_USE = new ConcurrentHashMap<>();

	private ScriptEvents() {
	}

	public static void clear() {
		ITEM_USE.clear();
		BLOCK_BREAK.clear();
		BLOCK_USE.clear();
	}

	public static void onItemUse(String defId, PlayerHandler handler) {
		ITEM_USE.computeIfAbsent(defId, k -> new CopyOnWriteArrayList<>()).add(handler);
	}

	public static void onBlockBreak(String defId, PlayerHandler handler) {
		BLOCK_BREAK.computeIfAbsent(defId, k -> new CopyOnWriteArrayList<>()).add(handler);
	}

	public static void onBlockUse(String defId, PlayerHandler handler) {
		BLOCK_USE.computeIfAbsent(defId, k -> new CopyOnWriteArrayList<>()).add(handler);
	}

	public static void fireItemUse(String defId, Player player, InteractionHand hand) {
		fire(ITEM_USE, defId, player, player.blockPosition());
	}

	public static void fireBlockBreak(String defId, Player player, BlockPos pos) {
		fire(BLOCK_BREAK, defId, player, pos);
	}

	public static void fireBlockUse(String defId, Player player, BlockPos pos) {
		fire(BLOCK_USE, defId, player, pos);
	}

	private static void fire(Map<String, List<PlayerHandler>> handlers, String defId, Player player, BlockPos pos) {
		invokeAll(handlers.get(defId), player, pos);
		invokeAll(handlers.get(ANY), player, pos);
	}

	private static void invokeAll(List<PlayerHandler> list, Player player, BlockPos pos) {
		if (list == null) return;
		for (PlayerHandler handler : list) {
			try {
				handler.handle(player, pos);
			} catch (Exception e) {
				ModMaker.LOGGER.error("ModMaker script handler failed", e);
				ScriptManager.get().reportRuntimeError(e);
			}
		}
	}
}
