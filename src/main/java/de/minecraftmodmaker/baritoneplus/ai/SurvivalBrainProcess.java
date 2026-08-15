package de.minecraftmodmaker.baritoneplus.ai;

import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalRunAway;
import baritone.api.pathing.goals.GoalYLevel;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.input.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public final class SurvivalBrainProcess implements IBaritoneProcess {
	public enum Mode {
		IDLE, EAT, FLEE, HAZARD, SURFACE
	}

	private final IBaritone baritone;
	private final BrainRuntime runtime;
	private Mode mode = Mode.IDLE;
	private int previousHotbar = -1;
	private int eatTicks;
	private boolean holdingUse;

	public SurvivalBrainProcess(IBaritone baritone, BrainRuntime runtime) {
		this.baritone = baritone;
		this.runtime = runtime;
	}

	@Override
	public boolean isActive() {
		if (!runtime.config().survivalBrain) {
			mode = Mode.IDLE;
			runtime.setMode("idle");
			return false;
		}
		LocalPlayer player = baritone.getPlayerContext().player();
		if (player == null) {
			mode = Mode.IDLE;
			return false;
		}
		mode = decide(player);
		if (mode == Mode.IDLE) {
			runtime.setMode("idle");
			runtime.setThreat("none");
		}
		return mode != Mode.IDLE;
	}

	@Override
	public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
		LocalPlayer player = baritone.getPlayerContext().player();
		if (player == null) {
			return defer();
		}
		return switch (mode) {
			case EAT -> tickEat(player);
			case FLEE, HAZARD -> tickFlee(player);
			case SURFACE -> tickSurface(player);
			default -> defer();
		};
	}

	@Override
	public boolean isTemporary() {
		return true;
	}

	@Override
	public void onLostControl() {
		stopEating();
		mode = Mode.IDLE;
	}

	@Override
	public double priority() {
		return switch (mode) {
			case HAZARD, FLEE -> 6.5;
			case SURFACE -> 6.2;
			case EAT -> 5.2;
			default -> 2.0;
		};
	}

	@Override
	public String displayName0() {
		return "Plus Survival " + mode.name().toLowerCase();
	}

	private Mode decide(LocalPlayer player) {
		Iterable<Entity> entities = baritone.getPlayerContext().entities();
		ThreatScanner.Result threat = ThreatScanner.scan(player, entities, runtime.config());
		if (threat.present()) {
			runtime.setThreat(threat.label());
			runtime.setMode(threat.kind() == ThreatScanner.Kind.DROWN ? "surface" : "flee");
			return threat.kind() == ThreatScanner.Kind.DROWN ? Mode.SURFACE : (threat.kind() == ThreatScanner.Kind.LAVA || threat.kind() == ThreatScanner.Kind.FIRE ? Mode.HAZARD : Mode.FLEE);
		}
		runtime.setThreat("none");
		boolean starving = player.getFoodData().getFoodLevel() <= runtime.config().eatBelowHunger;
		boolean emergencyHeal = player.getHealth() <= runtime.config().fleeHealth;
		if ((starving || emergencyHeal) && findBestHotbarFood(player, emergencyHeal) != null) {
			runtime.setMode("eat");
			return Mode.EAT;
		}
		return Mode.IDLE;
	}

	private PathingCommand tickEat(LocalPlayer player) {
		boolean emergency = player.getHealth() <= runtime.config().fleeHealth;
		FoodHelper.RankedFood food = findBestHotbarFood(player, emergency);
		if (food == null) {
			stopEating();
			return defer();
		}
		boolean hungry = player.getFoodData().getFoodLevel() <= runtime.config().eatBelowHunger;
		if (!hungry && !(emergency && FoodHelper.canAlwaysEat(food.stack()))) {
			stopEating();
			return defer();
		}
		if (previousHotbar < 0) {
			previousHotbar = InventorySlots.selectedSlot(player);
		}
		InventorySlots.selectHotbar(player, food.slot());
		baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
		holdingUse = true;
		eatTicks++;
		if (eatTicks > 80) {
			stopEating();
		}
		return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
	}

	private PathingCommand tickFlee(LocalPlayer player) {
		stopEating();
		ThreatScanner.Result threat = ThreatScanner.scan(player, baritone.getPlayerContext().entities(), runtime.config());
		BlockPos[] from = threat.from().isEmpty()
				? new BlockPos[]{player.blockPosition()}
				: threat.from().toArray(BlockPos[]::new);
		double distance = threat.kind() == ThreatScanner.Kind.CREEPER ? 18 : 16;
		baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, player.isInLava() || player.getRemainingFireTicks() > 0);
		baritone.getInputOverrideHandler().setInputForceState(Input.SPRINT, true);
		return new PathingCommand(new GoalRunAway(distance, player.getBlockY(), from), PathingCommandType.SET_GOAL_AND_PATH);
	}

	private PathingCommand tickSurface(LocalPlayer player) {
		stopEating();
		int targetY = Math.min(player.getBlockY() + 12, player.level().getMaxY());
		return new PathingCommand(new GoalYLevel(targetY), PathingCommandType.SET_GOAL_AND_PATH);
	}

	private FoodHelper.RankedFood findBestHotbarFood(LocalPlayer player, boolean emergency) {
		FoodHelper.RankedFood best = null;
		var items = InventorySlots.main(player);
		int limit = Math.min(9, items.size());
		for (int slot = 0; slot < limit; slot++) {
			ItemStack stack = items.get(slot);
			int score = FoodHelper.score(stack, emergency);
			if (score == Integer.MIN_VALUE) {
				continue;
			}
			if (!emergency && player.getFoodData().getFoodLevel() >= 20 && !FoodHelper.canAlwaysEat(stack)) {
				continue;
			}
			if (best == null || score > best.score()) {
				best = new FoodHelper.RankedFood(slot, stack, score);
			}
		}
		return best;
	}

	private void stopEating() {
		if (holdingUse) {
			baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
			holdingUse = false;
		}
		LocalPlayer player = baritone.getPlayerContext().player();
		if (player != null && previousHotbar >= 0) {
			InventorySlots.selectHotbar(player, previousHotbar);
		}
		previousHotbar = -1;
		eatTicks = 0;
	}

	private static PathingCommand defer() {
		return new PathingCommand(null, PathingCommandType.DEFER);
	}
}
