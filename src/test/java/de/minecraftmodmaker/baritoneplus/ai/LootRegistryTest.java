package de.minecraftmodmaker.baritoneplus.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootRegistryTest {
	@Test
	void ranksNetheriteAboveDiamond() {
		assertTrue(LootRegistry.valueOf("minecraft:netherite_ingot") > LootRegistry.valueOf("diamond"));
	}

	@Test
	void treatsTotemAsHighValue() {
		assertTrue(LootRegistry.isValuable("totem_of_undying", 40));
		assertEquals(150, LootRegistry.valueOf("minecraft:totem_of_undying"));
	}

	@Test
	void ignoresJunk() {
		assertFalse(LootRegistry.isValuable("cobblestone", 40));
		assertEquals(0, LootRegistry.valueOf("dirt"));
	}

	@Test
	void matchesAffixNames() {
		assertTrue(LootRegistry.valueOf("diamond_pickaxe") >= 90);
		assertTrue(LootRegistry.valueOf("netherite_helmet") >= 125);
	}
}
