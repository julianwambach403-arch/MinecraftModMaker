package de.minecraftmodmaker.baritoneplus.ai;

import java.util.Locale;
import java.util.Map;

/**
 * Item-id value table used by loot assist. Pure Java so the ranking can be unit-tested.
 */
public final class LootRegistry {
	private static final Map<String, Integer> VALUES = Map.ofEntries(
			Map.entry("netherite_ingot", 140),
			Map.entry("netherite_scrap", 120),
			Map.entry("ancient_debris", 115),
			Map.entry("netherite_upgrade_smithing_template", 110),
			Map.entry("elytra", 130),
			Map.entry("totem_of_undying", 150),
			Map.entry("enchanted_golden_apple", 125),
			Map.entry("golden_apple", 55),
			Map.entry("diamond", 100),
			Map.entry("diamond_block", 110),
			Map.entry("diamond_ore", 85),
			Map.entry("deepslate_diamond_ore", 90),
			Map.entry("emerald", 70),
			Map.entry("emerald_block", 80),
			Map.entry("enchanted_book", 75),
			Map.entry("nether_star", 145),
			Map.entry("beacon", 120),
			Map.entry("shulker_box", 95),
			Map.entry("ender_chest", 60),
			Map.entry("gold_ingot", 45),
			Map.entry("raw_gold", 40),
			Map.entry("gold_block", 70),
			Map.entry("iron_ingot", 25),
			Map.entry("iron_block", 40),
			Map.entry("lapis_lazuli", 20),
			Map.entry("redstone", 15),
			Map.entry("quartz", 18),
			Map.entry("ender_pearl", 50),
			Map.entry("blaze_rod", 45),
			Map.entry("ghast_tear", 55),
			Map.entry("wither_skeleton_skull", 100),
			Map.entry("trident", 90),
			Map.entry("mace", 100),
			Map.entry("heavy_core", 95)
	);

	private LootRegistry() {
	}

	public static String normalizeId(String id) {
		if (id == null || id.isBlank()) {
			return "";
		}
		String trimmed = id.toLowerCase(Locale.ROOT).trim();
		int slash = trimmed.indexOf(':');
		return slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
	}

	public static int valueOf(String itemId) {
		String id = normalizeId(itemId);
		Integer exact = VALUES.get(id);
		if (exact != null) {
			return exact;
		}
		if (id.contains("netherite")) {
			return 125;
		}
		if (id.contains("diamond")) {
			return 90;
		}
		if (id.contains("shulker")) {
			return 90;
		}
		if (id.endsWith("_head") || id.endsWith("_skull")) {
			return 80;
		}
		if (id.contains("enchanted")) {
			return 70;
		}
		if (id.contains("golden") && (id.contains("apple") || id.contains("carrot"))) {
			return 50;
		}
		return 0;
	}

	public static boolean isValuable(String itemId, int minValue) {
		return valueOf(itemId) >= minValue;
	}
}
