package de.minecraftmodmaker.baritoneplus.ai;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import de.minecraftmodmaker.baritoneplus.BaritonePlus;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Safer, more reliable pathfinder defaults. Applied once per session and only
 * overwrites Baritone settings that are still at their built-in defaults.
 */
public final class PathTuner {
	private PathTuner() {
	}

	public static void applyIfNeeded(BrainRuntime runtime) {
		if (runtime.tuned() || !runtime.config().tunePathfinder) {
			return;
		}
		Settings settings = BaritoneAPI.getSettings();
		setIfDefault(settings.allowSprint, true);
		setIfDefault(settings.autoTool, true);
		setIfDefault(settings.allowInventory, true);
		setIfDefault(settings.mineScanDroppedItems, true);
		setIfDefault(settings.avoidance, true);
		setIfDefault(settings.antiCheatCompatibility, true);
		setIfDefault(settings.allowParkourAscend, true);
		setIfDefault(settings.allowDiagonalAscend, true);
		setIfDefault(settings.allowDiagonalDescend, true);
		setIfDefault(settings.sprintAscends, true);
		setIfDefault(settings.itemSaver, true);
		setIfDefault(settings.chatControl, true);
		setIfDefault(settings.prefixControl, true);
		setIfDefault(settings.renderPath, true);
		setIfDefault(settings.renderGoal, true);

		if (settings.mobAvoidanceRadius.value.equals(settings.mobAvoidanceRadius.defaultValue)) {
			settings.mobAvoidanceRadius.value = 14;
		}
		if (settings.mobAvoidanceCoefficient.value.equals(settings.mobAvoidanceCoefficient.defaultValue)) {
			settings.mobAvoidanceCoefficient.value = 2.2;
		}
		if (settings.primaryTimeoutMS.value.equals(settings.primaryTimeoutMS.defaultValue)) {
			settings.primaryTimeoutMS.value = 2500L;
		}
		if (settings.planAheadPrimaryTimeoutMS.value.equals(settings.planAheadPrimaryTimeoutMS.defaultValue)) {
			settings.planAheadPrimaryTimeoutMS.value = 4000L;
		}
		if (settings.costHeuristic.value.equals(settings.costHeuristic.defaultValue)) {
			// Slightly greedier heuristic: faster first paths, still admissible enough in practice.
			settings.costHeuristic.value = 3.4;
		}
		if (settings.backtrackCostFavoringCoefficient.value.equals(settings.backtrackCostFavoringCoefficient.defaultValue)) {
			settings.backtrackCostFavoringCoefficient.value = 0.6;
		}
		if (settings.movementTimeoutTicks.value.equals(settings.movementTimeoutTicks.defaultValue)) {
			settings.movementTimeoutTicks.value = 120;
		}

		var avoid = new ArrayList<>(settings.blocksToAvoid.value);
		List.of(
				Blocks.MAGMA_BLOCK,
				Blocks.CACTUS,
				Blocks.SWEET_BERRY_BUSH,
				Blocks.WITHER_ROSE,
				Blocks.FIRE,
				Blocks.SOUL_FIRE,
				Blocks.CAMPFIRE,
				Blocks.SOUL_CAMPFIRE,
				Blocks.POWDER_SNOW
		).forEach(block -> {
			if (!avoid.contains(block)) {
				avoid.add(block);
			}
		});
		settings.blocksToAvoid.value = avoid;
		runtime.markTuned();
		BaritonePlus.LOGGER.info("Tuned Baritone pathfinder defaults for safer, faster travel");
	}

	private static void setIfDefault(Settings.Setting<Boolean> setting, boolean value) {
		if (setting.value.equals(setting.defaultValue)) {
			setting.value = value;
		}
	}
}
