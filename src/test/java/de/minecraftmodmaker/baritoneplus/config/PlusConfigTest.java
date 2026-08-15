package de.minecraftmodmaker.baritoneplus.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlusConfigTest {
	@Test
	void roundTripsToDisk(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("baritoneplus.properties");
		PlusConfig config = new PlusConfig();
		config.survivalBrain = false;
		config.eatBelowHunger = 11;
		config.lootRange = 9.5;
		config.save(file);

		PlusConfig loaded = PlusConfig.load(file);
		assertFalse(loaded.survivalBrain);
		assertTrue(loaded.stuckRecovery);
		assertEquals(11, loaded.eatBelowHunger);
		assertEquals(9.5, loaded.lootRange, 0.001);
	}

	@Test
	void missingFileUsesDefaults(@TempDir Path dir) {
		PlusConfig loaded = PlusConfig.load(dir.resolve("missing.properties"));
		assertTrue(loaded.survivalBrain);
		assertTrue(loaded.hud);
		assertEquals(14, loaded.eatBelowHunger);
	}
}
