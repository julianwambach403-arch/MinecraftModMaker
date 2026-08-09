package com.modmaker.registry;

import com.modmaker.content.ContentManager;
import com.modmaker.content.ItemDefinition;
import com.modmaker.script.ScriptEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * One of the pre-registered placeholder items. All user-visible behaviour is looked up
 * from the bound {@link ItemDefinition} at call time, which is what makes live editing work.
 */
public class DynamicItem extends Item {
	private final int slot;

	public DynamicItem(int slot, Properties properties) {
		super(properties);
		this.slot = slot;
	}

	public int slot() {
		return slot;
	}

	public ItemDefinition definition() {
		return ContentManager.get().itemBySlot(slot);
	}

	@Override
	public Component getName(ItemStack stack) {
		ItemDefinition def = definition();
		return def != null ? Component.literal(def.displayName) : super.getName(stack);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemDefinition def = definition();
		if (def != null && !level.isClientSide()) {
			ScriptEvents.fireItemUse(def.id, player, hand);
		}
		return super.use(level, player, hand);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> output, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, output, flag);
		ItemDefinition def = definition();
		if (def != null && def.tooltip != null && !def.tooltip.isBlank()) {
			output.accept(Component.literal(def.tooltip).withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public ItemStack getDefaultInstance() {
		ItemStack stack = super.getDefaultInstance();
		ItemDefinition def = definition();
		if (def != null) {
			StackStamper.stamp(stack, def);
		}
		return stack;
	}
}
