package de.minecraftmodmaker.baritoneplus.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Plain-properties config so it can be unit-tested without Minecraft or Gson.
 */
public final class PlusConfig {
	public boolean survivalBrain = true;
	public boolean stuckRecovery = true;
	public boolean lootAssist = true;
	public boolean tunePathfinder = true;
	public boolean hud = true;
	public boolean germanAliases = true;

	public int eatBelowHunger = 14;
	public float fleeHealth = 8.0f;
	public double creeperFleeRange = 7.0;
	public double hostileFleeRange = 10.0;
	public double lootRange = 12.0;
	public int minLootValue = 40;
	public int stuckJumpTicks = 25;
	public int stuckRepathTicks = 55;
	public int stuckOffsetTicks = 80;

	public static PlusConfig load(Path file) {
		PlusConfig config = new PlusConfig();
		if (!Files.isRegularFile(file)) {
			return config;
		}
		Properties properties = new Properties();
		try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			properties.load(reader);
		} catch (IOException ignored) {
			return config;
		}
		config.survivalBrain = bool(properties, "survivalBrain", config.survivalBrain);
		config.stuckRecovery = bool(properties, "stuckRecovery", config.stuckRecovery);
		config.lootAssist = bool(properties, "lootAssist", config.lootAssist);
		config.tunePathfinder = bool(properties, "tunePathfinder", config.tunePathfinder);
		config.hud = bool(properties, "hud", config.hud);
		config.germanAliases = bool(properties, "germanAliases", config.germanAliases);
		config.eatBelowHunger = integer(properties, "eatBelowHunger", config.eatBelowHunger);
		config.fleeHealth = floating(properties, "fleeHealth", config.fleeHealth);
		config.creeperFleeRange = floating(properties, "creeperFleeRange", (float) config.creeperFleeRange);
		config.hostileFleeRange = floating(properties, "hostileFleeRange", (float) config.hostileFleeRange);
		config.lootRange = floating(properties, "lootRange", (float) config.lootRange);
		config.minLootValue = integer(properties, "minLootValue", config.minLootValue);
		config.stuckJumpTicks = integer(properties, "stuckJumpTicks", config.stuckJumpTicks);
		config.stuckRepathTicks = integer(properties, "stuckRepathTicks", config.stuckRepathTicks);
		config.stuckOffsetTicks = integer(properties, "stuckOffsetTicks", config.stuckOffsetTicks);
		return config;
	}

	public void save(Path file) throws IOException {
		Files.createDirectories(file.getParent());
		StringBuilder out = new StringBuilder();
		out.append("# Baritone Plus — survival AI overlay for standalone Baritone\n");
		for (Map.Entry<String, String> entry : asMap().entrySet()) {
			out.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
		}
		Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
	}

	public Map<String, String> asMap() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("survivalBrain", String.valueOf(survivalBrain));
		map.put("stuckRecovery", String.valueOf(stuckRecovery));
		map.put("lootAssist", String.valueOf(lootAssist));
		map.put("tunePathfinder", String.valueOf(tunePathfinder));
		map.put("hud", String.valueOf(hud));
		map.put("germanAliases", String.valueOf(germanAliases));
		map.put("eatBelowHunger", String.valueOf(eatBelowHunger));
		map.put("fleeHealth", String.valueOf(fleeHealth));
		map.put("creeperFleeRange", String.valueOf(creeperFleeRange));
		map.put("hostileFleeRange", String.valueOf(hostileFleeRange));
		map.put("lootRange", String.valueOf(lootRange));
		map.put("minLootValue", String.valueOf(minLootValue));
		map.put("stuckJumpTicks", String.valueOf(stuckJumpTicks));
		map.put("stuckRepathTicks", String.valueOf(stuckRepathTicks));
		map.put("stuckOffsetTicks", String.valueOf(stuckOffsetTicks));
		return map;
	}

	public String summarize() {
		return "brain=" + onOff(survivalBrain)
				+ " stuck=" + onOff(stuckRecovery)
				+ " loot=" + onOff(lootAssist)
				+ " tune=" + onOff(tunePathfinder)
				+ " hud=" + onOff(hud);
	}

	private static String onOff(boolean value) {
		return value ? "on" : "off";
	}

	private static boolean bool(Properties properties, String key, boolean fallback) {
		String raw = properties.getProperty(key);
		if (raw == null) {
			return fallback;
		}
		return Boolean.parseBoolean(raw.trim());
	}

	private static int integer(Properties properties, String key, int fallback) {
		String raw = properties.getProperty(key);
		if (raw == null) {
			return fallback;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException ignored) {
			return fallback;
		}
	}

	private static float floating(Properties properties, String key, float fallback) {
		String raw = properties.getProperty(key);
		if (raw == null) {
			return fallback;
		}
		try {
			return Float.parseFloat(raw.trim());
		} catch (NumberFormatException ignored) {
			return fallback;
		}
	}
}
