package com.modmaker.content;

/**
 * A user-created block, stored as JSON in {@code modmaker/blocks/<id>.json}.
 * Bound to a placeholder block slot at runtime.
 */
public class BlockDefinition {
	public String id = "";
	public String displayName = "New Block";
	/** File name of a PNG inside {@code modmaker/textures/}, empty for the default texture. */
	public String texture = "";
	public String tooltip = "";
	/** Destroy time in seconds-ish scale like vanilla hardness. -1 = unbreakable. */
	public float hardness = 1.5f;
	public float resistance = 6.0f;
	/** 0-15 light emitted by the block. */
	public int lightLevel = 0;
	/** stone | wood | grass | gravel | sand | metal | glass | wool */
	public String soundType = "stone";
	public boolean requiresTool = false;
	/** If true the block drops itself; otherwise {@link #dropItem} (may be empty for no drop). */
	public boolean dropSelf = true;
	/** Item reference (ModMaker id or vanilla id like "minecraft:diamond") dropped when {@link #dropSelf} is false. */
	public String dropItem = "";

	public int version = 1;
}
