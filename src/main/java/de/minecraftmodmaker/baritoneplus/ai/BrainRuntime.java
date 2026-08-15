package de.minecraftmodmaker.baritoneplus.ai;

import de.minecraftmodmaker.baritoneplus.config.PlusConfig;

/**
 * Shared HUD/status snapshot written by the AI processes each tick.
 */
public final class BrainRuntime {
	private PlusConfig config;
	private String mode = "idle";
	private String threat = "none";
	private String loot = "none";
	private String stuck = "ok";
	private boolean greeted;
	private boolean tuned;
	private Object registeredBaritone;

	public BrainRuntime(PlusConfig config) {
		this.config = config;
	}

	public PlusConfig config() {
		return config;
	}

	public void setConfig(PlusConfig config) {
		this.config = config;
	}

	public String mode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode == null ? "idle" : mode;
	}

	public String threat() {
		return threat;
	}

	public void setThreat(String threat) {
		this.threat = threat == null ? "none" : threat;
	}

	public String loot() {
		return loot;
	}

	public void setLoot(String loot) {
		this.loot = loot == null ? "none" : loot;
	}

	public String stuck() {
		return stuck;
	}

	public void setStuck(String stuck) {
		this.stuck = stuck == null ? "ok" : stuck;
	}

	public boolean greeted() {
		return greeted;
	}

	public void markGreeted() {
		this.greeted = true;
	}

	public boolean tuned() {
		return tuned;
	}

	public void markTuned() {
		this.tuned = true;
	}

	public boolean isRegisteredFor(Object baritone) {
		return registeredBaritone == baritone;
	}

	public void markRegistered(Object baritone) {
		this.registeredBaritone = baritone;
	}

	public String statusLine() {
		return "AI " + mode + " | threat " + threat + " | loot " + loot + " | stuck " + stuck;
	}
}
