package com.modmaker.registry;

import com.modmaker.content.BlockDefinition;
import com.modmaker.content.ContentManager;
import com.modmaker.script.ScriptEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * One of the pre-registered placeholder blocks. Hardness, sounds, drops and light are
 * resolved from the bound {@link BlockDefinition} at call time so edits apply instantly.
 * Light level is a block state property because light emission must be part of the state.
 */
public class DynamicBlock extends Block {
	public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);

	private final int slot;

	public DynamicBlock(int slot, Properties properties) {
		super(properties);
		this.slot = slot;
		registerDefaultState(stateDefinition.any().setValue(LIGHT, 0));
	}

	public int slot() {
		return slot;
	}

	public BlockDefinition definition() {
		return ContentManager.get().blockBySlot(slot);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIGHT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockDefinition def = definition();
		int light = def != null ? Math.max(0, Math.min(15, def.lightLevel)) : 0;
		return defaultBlockState().setValue(LIGHT, light);
	}

	@Override
	public MutableComponent getName() {
		BlockDefinition def = definition();
		return def != null ? Component.literal(def.displayName) : super.getName();
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		BlockDefinition def = definition();
		if (def == null) return super.getDestroyProgress(state, player, level, pos);
		float hardness = def.hardness;
		if (hardness < 0.0f) return 0.0f; // unbreakable
		if (hardness == 0.0f) return 1.0f; // instant break
		boolean canHarvest = !def.requiresTool || player.hasCorrectToolForDrops(state);
		return player.getDestroySpeed(state) / hardness / (canHarvest ? 30 : 100);
	}

	@Override
	public float getExplosionResistance() {
		BlockDefinition def = definition();
		return def != null ? Math.max(0.0f, def.resistance) : super.getExplosionResistance();
	}

	@Override
	protected SoundType getSoundType(BlockState state) {
		BlockDefinition def = definition();
		return def != null ? parseSoundType(def.soundType) : super.getSoundType(state);
	}

	@Override
	public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
		super.playerDestroy(level, player, pos, state, blockEntity, tool);
		BlockDefinition def = definition();
		if (def == null || level.isClientSide()) return;

		ScriptEvents.fireBlockBreak(def.id, player, pos);

		if (player.isCreative()) return;
		if (def.requiresTool && !player.hasCorrectToolForDrops(state)) return;

		if (def.dropSelf) {
			popResource(level, pos, new ItemStack(this));
		} else if (def.dropItem != null && !def.dropItem.isBlank()) {
			ItemStack drop = ItemRefs.createStack(def.dropItem, 1);
			if (!drop.isEmpty()) popResource(level, pos, drop);
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		BlockDefinition def = definition();
		if (def != null && !level.isClientSide()) {
			ScriptEvents.fireBlockUse(def.id, player, pos);
		}
		return super.useWithoutItem(state, level, pos, player, hitResult);
	}

	public static SoundType parseSoundType(String raw) {
		return switch (raw == null ? "" : raw) {
			case "wood" -> SoundType.WOOD;
			case "grass" -> SoundType.GRASS;
			case "gravel" -> SoundType.GRAVEL;
			case "sand" -> SoundType.SAND;
			case "metal" -> SoundType.METAL;
			case "glass" -> SoundType.GLASS;
			case "wool" -> SoundType.WOOL;
			default -> SoundType.STONE;
		};
	}
}
