package de.minecraftmodmaker.baritoneplus.ai;

import de.minecraftmodmaker.baritoneplus.config.PlusConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StuckTrackerTest {
	@Test
	void staysIdleWhileMoving() {
		StuckTracker tracker = new StuckTracker();
		PlusConfig config = new PlusConfig();
		for (int i = 0; i < 40; i++) {
			tracker.update(i * 0.3, 64, 0, true);
		}
		assertEquals(StuckTracker.Action.NONE, tracker.action(config));
	}

	@Test
	void escalatesWhenFrozenOnAPath() {
		StuckTracker tracker = new StuckTracker();
		PlusConfig config = new PlusConfig();
		config.stuckJumpTicks = 5;
		config.stuckRepathTicks = 10;
		config.stuckOffsetTicks = 15;
		tracker.update(1, 64, 1, true);
		for (int i = 0; i < 6; i++) {
			tracker.update(1, 64, 1, true);
		}
		assertEquals(StuckTracker.Action.JUMP, tracker.action(config));
		for (int i = 0; i < 5; i++) {
			tracker.update(1, 64, 1, true);
		}
		assertEquals(StuckTracker.Action.REPATH, tracker.action(config));
		for (int i = 0; i < 5; i++) {
			tracker.update(1, 64, 1, true);
		}
		assertEquals(StuckTracker.Action.OFFSET, tracker.action(config));
	}

	@Test
	void resetsWhenNotPathing() {
		StuckTracker tracker = new StuckTracker();
		PlusConfig config = new PlusConfig();
		config.stuckJumpTicks = 3;
		for (int i = 0; i < 8; i++) {
			tracker.update(0, 64, 0, true);
		}
		tracker.update(0, 64, 0, false);
		assertEquals(0, tracker.stillTicks());
		assertEquals(StuckTracker.Action.NONE, tracker.action(config));
	}
}
