package de.minecraftmodmaker.baritoneplus.ai;

import baritone.api.IBaritone;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.input.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

public final class StuckRecoveryProcess implements IBaritoneProcess {
	private final IBaritone baritone;
	private final BrainRuntime runtime;
	private final StuckTracker tracker = new StuckTracker();
	private StuckTracker.Action action = StuckTracker.Action.NONE;
	private int offsetSeed;

	public StuckRecoveryProcess(IBaritone baritone, BrainRuntime runtime) {
		this.baritone = baritone;
		this.runtime = runtime;
	}

	@Override
	public boolean isActive() {
		if (!runtime.config().stuckRecovery) {
			tracker.reset();
			runtime.setStuck("ok");
			return false;
		}
		LocalPlayer player = baritone.getPlayerContext().player();
		if (player == null) {
			return false;
		}
		boolean pathing = baritone.getPathingBehavior().isPathing() || baritone.getPathingBehavior().getGoal() != null;
		tracker.update(player.getX(), player.getY(), player.getZ(), pathing);
		action = tracker.action(runtime.config());
		if (action == StuckTracker.Action.NONE) {
			runtime.setStuck("ok");
			clearMotion();
			return false;
		}
		runtime.setStuck(action.name().toLowerCase() + "/" + tracker.stillTicks());
		return true;
	}

	@Override
	public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
		LocalPlayer player = baritone.getPlayerContext().player();
		Goal current = baritone.getPathingBehavior().getGoal();
		if (player == null) {
			return new PathingCommand(current, PathingCommandType.DEFER);
		}
		return switch (action) {
			case JUMP -> {
				baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, true);
				baritone.getInputOverrideHandler().setInputForceState(Input.SNEAK, tracker.stillTicks() % 14 > 7);
				yield new PathingCommand(current, PathingCommandType.REQUEST_PAUSE);
			}
			case REPATH -> {
				clearMotion();
				if (current == null) {
					yield new PathingCommand(null, PathingCommandType.DEFER);
				}
				yield new PathingCommand(current, PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH);
			}
			case OFFSET -> {
				clearMotion();
				offsetSeed++;
				int dx = ((offsetSeed * 7) % 9) - 4;
				int dz = ((offsetSeed * 13) % 9) - 4;
				if (dx == 0 && dz == 0) {
					dx = 3;
				}
				BlockPos here = player.blockPosition();
				Goal sidestep = current != null
						? new GoalNear(here.offset(dx, 0, dz), 1)
						: new GoalXZ(here.getX() + dx * 4, here.getZ() + dz * 4);
				yield new PathingCommand(sidestep, PathingCommandType.SET_GOAL_AND_PATH);
			}
			default -> new PathingCommand(current, PathingCommandType.DEFER);
		};
	}

	@Override
	public boolean isTemporary() {
		return true;
	}

	@Override
	public void onLostControl() {
		clearMotion();
		action = StuckTracker.Action.NONE;
	}

	@Override
	public double priority() {
		return 4.4;
	}

	@Override
	public String displayName0() {
		return "Plus Unstuck " + action.name().toLowerCase();
	}

	private void clearMotion() {
		baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, false);
		baritone.getInputOverrideHandler().setInputForceState(Input.SNEAK, false);
	}
}
