package com.modmaker.script.api;

import com.modmaker.content.ContentManager;
import com.modmaker.registry.DynamicBlock;
import com.modmaker.registry.ItemRefs;
import com.modmaker.registry.PlaceholderPool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Player wrapper handed to scripts; keeps the surface small and beginner friendly. */
public class PlayerApi {
	private final ServerPlayer player;

	public PlayerApi(ServerPlayer player) {
		this.player = player;
	}

	public String name() {
		return player.getGameProfile().name();
	}

	public void tell(String message) {
		player.sendSystemMessage(Component.literal(String.valueOf(message)));
	}

	/** Gives the player a ModMaker item/block (by definition id) or any vanilla item id. */
	public void give(String itemRef, int count) {
		ItemStack stack = ItemRefs.createStack(itemRef, count);
		if (stack.isEmpty()) return;
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
	}

	public double x() {
		return player.getX();
	}

	public double y() {
		return player.getY();
	}

	public double z() {
		return player.getZ();
	}

	public float health() {
		return player.getHealth();
	}

	public void heal(float amount) {
		player.heal(amount);
	}

	public void teleport(double x, double y, double z) {
		player.teleportTo(x, y, z);
	}

	public boolean isSneaking() {
		return player.isShiftKeyDown();
	}

	/** Plays a sound only for this player, e.g. {@code player.playSound("minecraft:entity.player.levelup")}. */
	public void playSound(String soundId) {
		Identifier id = Identifier.tryParse(String.valueOf(soundId));
		if (id == null) return;
		SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(id);
		if (sound != null) {
			player.level().playSeededSound(null, player.getX(), player.getY(), player.getZ(),
					sound, SoundSource.PLAYERS, 1.0f, 1.0f, player.level().getRandom().nextLong());
		}
	}

	/** Runs a command as this player, e.g. {@code player.runCommand("say hi")}. */
	public void runCommand(String command) {
		player.level().getServer().getCommands()
				.performPrefixedCommand(player.createCommandSourceStack(), String.valueOf(command));
	}

	/** Places a block (ModMaker definition id or vanilla id) in the player's world. */
	public void setBlock(int x, int y, int z, String blockRef) {
		BlockState state = resolveBlockState(blockRef);
		if (state != null) {
			player.level().setBlockAndUpdate(new BlockPos(x, y, z), state);
		}
	}

	/** Returns the id of the block at the given position (ModMaker definition id when bound). */
	public String getBlock(int x, int y, int z) {
		Block block = player.level().getBlockState(new BlockPos(x, y, z)).getBlock();
		if (block instanceof DynamicBlock dynamic && dynamic.definition() != null) {
			return dynamic.definition().id;
		}
		Identifier key = BuiltInRegistries.BLOCK.getKey(block);
		return key != null ? key.toString() : "minecraft:air";
	}

	private static BlockState resolveBlockState(String ref) {
		if (ref == null || ref.isBlank()) return null;
		Integer slot = ContentManager.get().blockSlot(ref);
		if (slot != null) {
			DynamicBlock block = PlaceholderPool.BLOCKS[slot];
			return block.defaultBlockState().setValue(DynamicBlock.LIGHT,
					Math.max(0, Math.min(15, block.definition() != null ? block.definition().lightLevel : 0)));
		}
		Identifier id = Identifier.tryParse(ref);
		if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
			return BuiltInRegistries.BLOCK.getValue(id).defaultBlockState();
		}
		return null;
	}
}
