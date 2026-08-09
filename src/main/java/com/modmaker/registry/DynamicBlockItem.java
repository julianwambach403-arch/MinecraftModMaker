package com.modmaker.registry;

import com.modmaker.content.BlockDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Block item for placeholder blocks; shows the live display name and tooltip of the bound definition. */
public class DynamicBlockItem extends BlockItem {
	private final DynamicBlock block;

	public DynamicBlockItem(DynamicBlock block, Properties properties) {
		super(block, properties);
		this.block = block;
	}

	public DynamicBlock dynamicBlock() {
		return block;
	}

	@Override
	public Component getName(ItemStack stack) {
		BlockDefinition def = block.definition();
		return def != null ? Component.literal(def.displayName) : super.getName(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> output, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, output, flag);
		BlockDefinition def = block.definition();
		if (def != null && def.tooltip != null && !def.tooltip.isBlank()) {
			output.accept(Component.literal(def.tooltip).withStyle(ChatFormatting.GRAY));
		}
	}
}
