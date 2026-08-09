package com.modmaker.registry;

import com.modmaker.content.ItemDefinition;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies the data components described by an {@link ItemDefinition} onto item stacks.
 *
 * <p>Because placeholder items are registered before any definition exists, food, tool and
 * similar properties cannot be baked into the {@code Item} instance. Instead every stack is
 * "stamped" with the matching components. A version marker stored in custom data lets us
 * re-stamp stacks whenever their definition is edited, so changes apply to items players
 * already carry.
 */
public final class StackStamper {
	private static final String VERSION_KEY = "modmaker_version";

	private StackStamper() {
	}

	public static void stamp(ItemStack stack, ItemDefinition def) {
		stack.set(DataComponents.MAX_STACK_SIZE, Math.max(1, Math.min(99, def.stackSize)));
		stack.set(DataComponents.RARITY, parseRarity(def.rarity));
		if (def.glint) {
			stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		} else {
			stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
		}

		if (def.foodEnabled) {
			stack.set(DataComponents.FOOD, new FoodProperties(def.foodNutrition, def.foodSaturation, def.foodAlwaysEdible));
			stack.set(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD);
		} else {
			stack.remove(DataComponents.FOOD);
			stack.remove(DataComponents.CONSUMABLE);
		}

		if (def.isTool()) {
			stack.set(DataComponents.TOOL, buildTool(def));
			stack.set(DataComponents.ATTRIBUTE_MODIFIERS, buildAttributes(def));
			if (def.durability > 0) {
				stack.set(DataComponents.MAX_STACK_SIZE, 1);
				stack.set(DataComponents.MAX_DAMAGE, def.durability);
			}
		} else {
			stack.remove(DataComponents.TOOL);
			stack.remove(DataComponents.ATTRIBUTE_MODIFIERS);
			stack.remove(DataComponents.MAX_DAMAGE);
		}

		CompoundTag tag = new CompoundTag();
		tag.putInt(VERSION_KEY, def.version);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	private static Tool buildTool(ItemDefinition def) {
		TagKey<Block> mineable = switch (def.toolType) {
			case "axe" -> BlockTags.MINEABLE_WITH_AXE;
			case "shovel" -> BlockTags.MINEABLE_WITH_SHOVEL;
			case "hoe" -> BlockTags.MINEABLE_WITH_HOE;
			case "sword" -> null;
			default -> BlockTags.MINEABLE_WITH_PICKAXE;
		};
		List<Tool.Rule> rules = new ArrayList<>();
		if (mineable != null) {
			BuiltInRegistries.BLOCK.get(mineable).ifPresent(holders ->
					rules.add(Tool.Rule.minesAndDrops(holders, Math.max(1.0f, def.miningSpeed))));
		}
		return new Tool(rules, 1.0f, def.durability > 0 ? 1 : 0, true);
	}

	private static ItemAttributeModifiers buildAttributes(ItemDefinition def) {
		return ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE,
						new AttributeModifier(Identifier.fromNamespaceAndPath("modmaker", "attack_damage"),
								def.attackDamage, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED,
						new AttributeModifier(Identifier.fromNamespaceAndPath("modmaker", "attack_speed"),
								def.attackSpeed, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.build();
	}

	public static Rarity parseRarity(String raw) {
		return switch (raw == null ? "" : raw) {
			case "uncommon" -> Rarity.UNCOMMON;
			case "rare" -> Rarity.RARE;
			case "epic" -> Rarity.EPIC;
			default -> Rarity.COMMON;
		};
	}

	private static int stampedVersion(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) return -1;
		return data.copyTag().getIntOr(VERSION_KEY, -1);
	}

	/**
	 * Called once per second from the server tick loop: walks player inventories and
	 * re-stamps any ModMaker stack whose definition changed since it was created
	 * (including bare stacks from {@code /give} or crafting results).
	 */
	public static void tickPlayers(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Inventory inventory = player.getInventory();
			for (int i = 0; i < inventory.getContainerSize(); i++) {
				ItemStack stack = inventory.getItem(i);
				if (stack.isEmpty() || !(stack.getItem() instanceof DynamicItem dynamic)) continue;
				var def = dynamic.definition();
				if (def != null && stampedVersion(stack) != def.version) {
					stamp(stack, def);
				}
			}
		}
	}
}
