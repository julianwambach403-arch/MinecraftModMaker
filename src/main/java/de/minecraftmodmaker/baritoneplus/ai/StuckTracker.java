package de.minecraftmodmaker.baritoneplus.ai;

import de.minecraftmodmaker.baritoneplus.config.PlusConfig;

/**
 * Detects when the bot is pathing but not actually moving, then escalates recovery steps.
 */
public final class StuckTracker {
	public enum Action {
		NONE,
		JUMP,
		REPATH,
		OFFSET
	}

	private double lastX;
	private double lastY;
	private double lastZ;
	private boolean hasSample;
	private int stillTicks;

	public void reset() {
		hasSample = false;
		stillTicks = 0;
	}

	public void update(double x, double y, double z, boolean pathing) {
		if (!pathing) {
			reset();
			lastX = x;
			lastY = y;
			lastZ = z;
			hasSample = true;
			return;
		}
		if (!hasSample) {
			lastX = x;
			lastY = y;
			lastZ = z;
			hasSample = true;
			stillTicks = 0;
			return;
		}
		double dx = x - lastX;
		double dy = y - lastY;
		double dz = z - lastZ;
		double moved = Math.sqrt(dx * dx + dy * dy + dz * dz);
		lastX = x;
		lastY = y;
		lastZ = z;
		if (moved < 0.08) {
			stillTicks++;
		} else {
			stillTicks = 0;
		}
	}

	public int stillTicks() {
		return stillTicks;
	}

	public Action action(PlusConfig config) {
		if (stillTicks >= config.stuckOffsetTicks) {
			return Action.OFFSET;
		}
		if (stillTicks >= config.stuckRepathTicks) {
			return Action.REPATH;
		}
		if (stillTicks >= config.stuckJumpTicks) {
			return Action.JUMP;
		}
		return Action.NONE;
	}
}
