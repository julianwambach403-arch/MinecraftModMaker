package de.minecraftmodmaker.baritoneplus.command;

import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.datatypes.RelativeGoal;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.utils.BetterBlockPos;
import de.minecraftmodmaker.baritoneplus.ai.BrainRuntime;
import de.minecraftmodmaker.baritoneplus.config.PlusConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class PlusCommandBootstrap {
	private PlusCommandBootstrap() {
	}

	public static void register(IBaritone baritone, BrainRuntime runtime, Path configFile) {
		var registry = baritone.getCommandManager().getRegistry();
		registry.register(new PlusCommand(baritone, runtime, configFile));
		registry.register(new OreCommand(baritone));
		registry.register(new WoodCommand(baritone));
		registry.register(new FoodHuntCommand(baritone));
		registry.register(new LookGotoCommand(baritone));
		registry.register(new StatusCommand(baritone, runtime));
		if (runtime.config().germanAliases) {
			registry.register(new AliasCommand(baritone, List.of("gehe", "geh"), "goto"));
			registry.register(new AliasCommand(baritone, List.of("stopp", "halt"), "stop"));
			registry.register(new AliasCommand(baritone, List.of("folge"), "follow"));
			registry.register(new AliasCommand(baritone, List.of("erz"), "ore"));
			registry.register(new AliasCommand(baritone, List.of("holz"), "wood"));
			registry.register(new AliasCommand(baritone, List.of("status"), "plusstatus"));
		}
	}

	private static final class PlusCommand extends Command {
		private final BrainRuntime runtime;
		private final Path configFile;

		private PlusCommand(IBaritone baritone, BrainRuntime runtime, Path configFile) {
			super(baritone, "plus", "ai", "brain");
			this.runtime = runtime;
			this.configFile = configFile;
		}

		@Override
		public void execute(String label, IArgConsumer args) {
			PlusConfig config = runtime.config();
			if (!args.hasAny()) {
				logDirect("Baritone Plus: " + config.summarize());
				logDirect("Usage: plus <brain|stuck|loot|hud|tune|save> [on|off]");
				return;
			}
			String key = args.getString().toLowerCase(Locale.ROOT);
			if (key.equals("save")) {
				saveQuiet();
				logDirect("Saved config to " + configFile);
				return;
			}
			if (key.equals("status")) {
				logDirect(runtime.statusLine());
				logDirect(config.summarize());
				return;
			}
			boolean value = args.hasAny()
					? parseToggle(args.getString(), current(config, key))
					: !current(config, key);
			switch (key) {
				case "brain", "survival" -> config.survivalBrain = value;
				case "stuck" -> config.stuckRecovery = value;
				case "loot" -> config.lootAssist = value;
				case "hud" -> config.hud = value;
				case "tune" -> config.tunePathfinder = value;
				default -> {
					logDirect("Unknown toggle '" + key + "'. Use brain, stuck, loot, hud, tune, save.");
					return;
				}
			}
			saveQuiet();
			logDirect("plus " + key + " = " + (value ? "on" : "off"));
		}

		@Override
		public Stream<String> tabComplete(String label, IArgConsumer args) {
			if (args.hasExactlyOne()) {
				return Stream.of("brain", "stuck", "loot", "hud", "tune", "save", "status", "on", "off");
			}
			return Stream.empty();
		}

		@Override
		public String getShortDesc() {
			return "Toggle Baritone Plus AI extras";
		}

		@Override
		public List<String> getLongDesc() {
			return List.of(
					"Controls the standalone survival layer on top of Baritone.",
					"",
					"Usage:",
					"> plus",
					"> plus brain off",
					"> plus loot on",
					"> plus save"
			);
		}

		private void saveQuiet() {
			try {
				runtime.config().save(configFile);
			} catch (Exception ignored) {
			}
		}
	}

	private static final class OreCommand extends Command {
		private OreCommand(IBaritone baritone) {
			super(baritone, "ore", "ores");
		}

		@Override
		public void execute(String label, IArgConsumer args) {
			String[] targets = args.hasAny()
					? args.rawRest().split("\\s+")
					: new String[]{
					"diamond_ore", "deepslate_diamond_ore",
					"ancient_debris",
					"emerald_ore", "deepslate_emerald_ore",
					"gold_ore", "deepslate_gold_ore", "nether_gold_ore"
			};
			baritone.getMineProcess().mineByName(0, targets);
			logDirect("Mining " + String.join(", ", targets));
		}

		@Override
		public Stream<String> tabComplete(String label, IArgConsumer args) {
			return Stream.of("diamond_ore", "ancient_debris", "emerald_ore", "gold_ore", "iron_ore");
		}

		@Override
		public String getShortDesc() {
			return "Mine valuable ores (default: diamond, debris, emerald, gold)";
		}

		@Override
		public List<String> getLongDesc() {
			return List.of("Usage:", "> ore", "> ore diamond_ore ancient_debris");
		}
	}

	private static final class WoodCommand extends Command {
		private WoodCommand(IBaritone baritone) {
			super(baritone, "wood", "logs");
		}

		@Override
		public void execute(String label, IArgConsumer args) {
			baritone.getMineProcess().mineByName(0,
					"oak_log", "birch_log", "spruce_log", "jungle_log",
					"acacia_log", "dark_oak_log", "mangrove_log", "cherry_log",
					"pale_oak_log", "crimson_stem", "warped_stem");
			logDirect("Gathering wood");
		}

		@Override
		public Stream<String> tabComplete(String label, IArgConsumer args) {
			return Stream.empty();
		}

		@Override
		public String getShortDesc() {
			return "Chop nearby logs";
		}

		@Override
		public List<String> getLongDesc() {
			return List.of("Usage:", "> wood");
		}
	}

	private static final class FoodHuntCommand extends Command {
		private FoodHuntCommand(IBaritone baritone) {
			super(baritone, "foodhunt", "huntfood");
		}

		@Override
		public void execute(String label, IArgConsumer args) {
			baritone.getGetToBlockProcess().getToBlock(Blocks.WHEAT);
			logDirect("Pathing to wheat. Use #farm for full farming.");
		}

		@Override
		public Stream<String> tabComplete(String label, IArgConsumer args) {
			return Stream.empty();
		}

		@Override
		public String getShortDesc() {
			return "Walk to the nearest wheat crop";
		}

		@Override
		public List<String> getLongDesc() {
			return List.of("Usage:", "> foodhunt");
		}
	}

	private static final class LookGotoCommand extends Command {
		private LookGotoCommand(IBaritone baritone) {
			super(baritone, "look", "lookgoto");
		}

		@Override
		public void execute(String label, IArgConsumer args) {
			if (args.hasAny()) {
				BetterBlockPos origin = ctx.playerFeet();
				Goal goal = args.getDatatypePost(RelativeGoal.INSTANCE, origin);
				baritone.getCustomGoalProcess().setGoalAndPath(goal);
				logDirect("Going to " + goal);
				return;
			}
			HitResult hit = ctx.objectMouseOver();
			if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
				BlockPos pos = block.getBlockPos();
				baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(pos.above()));
				logDirect("Going to looked-at block " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
				return;
			}
			var entityHit = ctx.world().getEntities(ctx.player(), ctx.player().getBoundingBox().inflate(32), entity ->
					entity instanceof LivingEntity living && living != ctx.player() && entity.isAlive());
			Player self = ctx.player();
			LivingEntity closest = null;
			double best = 32;
			for (var entity : entityHit) {
				if (!(entity instanceof LivingEntity living)) {
					continue;
				}
				double dist = living.distanceTo(self);
				if (dist < best) {
					best = dist;
					closest = living;
				}
			}
			if (closest != null) {
				LivingEntity followTarget = closest;
				baritone.getFollowProcess().follow(entity -> entity == followTarget);
				logDirect("Following " + followTarget.getName().getString());
				return;
			}
			logDirect("Look at a block, or pass relative coordinates.");
		}

		@Override
		public Stream<String> tabComplete(String label, IArgConsumer args) {
			return Stream.empty();
		}

		@Override
		public String getShortDesc() {
			return "Path to the block you are looking at";
		}

		@Override
		public List<String> getLongDesc() {
			return List.of("Usage:", "> look", "> look ~ ~ ~");
		}
	}

	private static final class StatusCommand extends Command {
		private final BrainRuntime runtime;

		private StatusCommand(IBaritone baritone, BrainRuntime runtime) {
			super(baritone, "plusstatus");
			this.runtime = runtime;
		}

		@Override
		public void execute(String label, IArgConsumer args) {
			var pathing = baritone.getPathingBehavior();
			logDirect(runtime.statusLine());
			logDirect("pathing=" + pathing.isPathing()
					+ " goal=" + pathing.getGoal()
					+ " eta=" + pathing.estimatedTicksToGoal().map(ticks -> (int) (ticks / 20) + "s").orElse("-"));
			var control = baritone.getPathingControlManager().mostRecentInControl();
			control.ifPresent(process -> logDirect("process=" + process.displayName()));
		}

		@Override
		public Stream<String> tabComplete(String label, IArgConsumer args) {
			return Stream.empty();
		}

		@Override
		public String getShortDesc() {
			return "Show Baritone Plus AI status";
		}

		@Override
		public List<String> getLongDesc() {
			return List.of("Usage:", "> plusstatus");
		}
	}

	private static final class AliasCommand extends Command {
		private final String target;

		private AliasCommand(IBaritone baritone, List<String> names, String target) {
			super(baritone, names.toArray(String[]::new));
			this.target = target;
		}

		@Override
		public void execute(String label, IArgConsumer args) {
			String rest = args.hasAny() ? args.rawRest() : "";
			String command = rest.isBlank() ? target : target + " " + rest;
			baritone.getCommandManager().execute(command);
		}

		@Override
		public Stream<String> tabComplete(String label, IArgConsumer args) {
			return baritone.getCommandManager().tabComplete(target + (args.hasAny() ? " " + args.rawRest() : ""));
		}

		@Override
		public String getShortDesc() {
			return "Alias for #" + target;
		}

		@Override
		public List<String> getLongDesc() {
			return List.of("Runs #" + target);
		}

		@Override
		public boolean hiddenFromHelp() {
			return true;
		}
	}

	private static boolean current(PlusConfig config, String key) {
		return switch (key) {
			case "brain", "survival" -> config.survivalBrain;
			case "stuck" -> config.stuckRecovery;
			case "loot" -> config.lootAssist;
			case "hud" -> config.hud;
			case "tune" -> config.tunePathfinder;
			default -> false;
		};
	}

	private static boolean parseToggle(String raw, boolean fallback) {
		return switch (raw.toLowerCase(Locale.ROOT)) {
			case "on", "true", "1", "yes", "an" -> true;
			case "off", "false", "0", "no", "aus" -> false;
			case "toggle" -> !fallback;
			default -> fallback;
		};
	}

	@SuppressWarnings("unused")
	private static List<String> names(String... values) {
		return Arrays.asList(values);
	}
}
