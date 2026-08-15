package de.minecraftmodmaker.baritoneplus.ai;

import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public final class LootAssistProcess implements IBaritoneProcess {
	private final IBaritone baritone;
	private final BrainRuntime runtime;
	private ItemEntity target;

	public LootAssistProcess(IBaritone baritone, BrainRuntime runtime) {
		this.baritone = baritone;
		this.runtime = runtime;
	}

	@Override
	public boolean isActive() {
		if (!runtime.config().lootAssist) {
			runtime.setLoot("none");
			target = null;
			return false;
		}
		LocalPlayer player = baritone.getPlayerContext().player();
		if (player == null || InventorySlots.isFull(player)) {
			runtime.setLoot(player != null && InventorySlots.isFull(player) ? "inv-full" : "none");
			target = null;
			return false;
		}
		target = findBest(player);
		if (target == null) {
			runtime.setLoot("none");
			return false;
		}
		runtime.setLoot(idOf(target.getItem()) + "@" + (int) target.distanceTo(player));
		return true;
	}

	@Override
	public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
		if (target == null || !target.isAlive()) {
			return new PathingCommand(null, PathingCommandType.DEFER);
		}
		return new PathingCommand(new GoalGetToBlock(target.blockPosition()), PathingCommandType.SET_GOAL_AND_PATH);
	}

	@Override
	public boolean isTemporary() {
		return true;
	}

	@Override
	public void onLostControl() {
		target = null;
	}

	@Override
	public double priority() {
		return 1.25;
	}

	@Override
	public String displayName0() {
		return "Plus Loot " + (target == null ? "" : idOf(target.getItem()));
	}

	private ItemEntity findBest(LocalPlayer player) {
		ItemEntity best = null;
		int bestValue = runtime.config().minLootValue - 1;
		double bestDist = runtime.config().lootRange;
		for (Entity entity : baritone.getPlayerContext().entities()) {
			if (!(entity instanceof ItemEntity item) || !item.isAlive()) {
				continue;
			}
			double dist = item.distanceTo(player);
			if (dist > runtime.config().lootRange) {
				continue;
			}
			int value = LootRegistry.valueOf(idOf(item.getItem()));
			if (value < runtime.config().minLootValue) {
				continue;
			}
			if (value > bestValue || (value == bestValue && dist < bestDist)) {
				best = item;
				bestValue = value;
				bestDist = dist;
			}
		}
		return best;
	}

	private static String idOf(ItemStack stack) {
		return String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
	}
}
