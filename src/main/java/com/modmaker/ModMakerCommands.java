package com.modmaker;

import com.modmaker.content.BlockDefinition;
import com.modmaker.content.ContentManager;
import com.modmaker.content.ItemDefinition;
import com.modmaker.pack.RuntimeDataPack;
import com.modmaker.registry.ItemRefs;
import com.modmaker.script.ScriptManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Server commands: {@code /modmaker reload}, {@code /modmaker give <id> [count]},
 * {@code /modmaker list}. The GUI is opened client-side (keybind or client command).
 */
public final class ModMakerCommands {
	private ModMakerCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("modmaker")
						.then(Commands.literal("reload").executes(context -> {
							reloadAll(context.getSource().getServer());
							context.getSource().sendSuccess(() ->
									Component.translatable("modmaker.command.reloaded"), true);
							int errors = ScriptManager.get().errors().size();
							if (errors > 0) {
								context.getSource().sendFailure(
										Component.translatable("modmaker.command.script_errors", errors));
							}
							return 1;
						}))
						.then(Commands.literal("give")
								.then(Commands.argument("id", StringArgumentType.word())
										.suggests((context, builder) -> {
											ContentManager content = ContentManager.get();
											content.allItems().forEach(def -> builder.suggest(def.id));
											content.allBlocks().forEach(def -> builder.suggest(def.id));
											return builder.buildFuture();
										})
										.executes(context -> give(context.getSource().getPlayerOrException(),
												StringArgumentType.getString(context, "id"), 1))
										.then(Commands.argument("count", IntegerArgumentType.integer(1, 99))
												.executes(context -> give(context.getSource().getPlayerOrException(),
														StringArgumentType.getString(context, "id"),
														IntegerArgumentType.getInteger(context, "count"))))))
						.then(Commands.literal("list").executes(context -> {
							ContentManager content = ContentManager.get();
							StringBuilder text = new StringBuilder();
							for (ItemDefinition def : content.allItems()) {
								text.append("\n- [Item] ").append(def.id).append(" (").append(def.displayName).append(')');
							}
							for (BlockDefinition def : content.allBlocks()) {
								text.append("\n- [Block] ").append(def.id).append(" (").append(def.displayName).append(')');
							}
							String result = text.isEmpty() ? "-" : text.toString();
							context.getSource().sendSuccess(() ->
									Component.translatable("modmaker.command.list", result), false);
							return 1;
						}))));
	}

	private static int give(ServerPlayer player, String id, int count) {
		ItemStack stack = ItemRefs.createStack(id, count);
		if (stack.isEmpty()) {
			player.sendSystemMessage(Component.translatable("modmaker.command.unknown_id", id));
			return 0;
		}
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
		return 1;
	}

	/** Full reload: definitions, scripts, runtime data pack (recipes) and change listeners. */
	public static void reloadAll(MinecraftServer server) {
		ContentManager.get().loadAll();
		ScriptManager.get().reload();
		if (server != null) {
			RuntimeDataPack.rewriteAndReload(server);
		}
		ContentManager.get().fireChanged();
	}
}
