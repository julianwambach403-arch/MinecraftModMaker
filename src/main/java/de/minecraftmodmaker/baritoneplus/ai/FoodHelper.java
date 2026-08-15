package de.minecraftmodmaker.baritoneplus.ai;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public final class FoodHelper {
	public record RankedFood(int slot, ItemStack stack, int score) {
	}

	private FoodHelper() {
	}

	public static int score(ItemStack stack, boolean emergency) {
		if (stack == null || stack.isEmpty()) {
			return Integer.MIN_VALUE;
		}
		FoodProperties food = stack.get(DataComponents.FOOD);
		if (food == null) {
			return Integer.MIN_VALUE;
		}
		int nutrition = food.nutrition();
		float saturation = food.saturation();
		int score = nutrition * 10 + Math.round(saturation * 6);
		String id = LootRegistry.normalizeId(String.valueOf(stack.getItem()));
		// Prefer real meals over chorus/gapples unless we are in an emergency.
		if (id.contains("chorus")) {
			score -= 80;
		}
		if (id.contains("rotten") || id.contains("spider") || id.contains("poisonous") || id.contains("pufferfish")) {
			score -= 120;
		}
		if (emergency && (id.contains("golden_apple") || id.contains("enchanted_golden_apple"))) {
			score += 200;
		} else if (id.contains("enchanted_golden_apple")) {
			score -= 40; // save it
		}
		if (food.canAlwaysEat()) {
			score += 5;
		}
		return score;
	}

	public static boolean isFood(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.get(DataComponents.FOOD) != null;
	}

	public static boolean canAlwaysEat(ItemStack stack) {
		if (!isFood(stack)) {
			return false;
		}
		FoodProperties food = stack.get(DataComponents.FOOD);
		return food != null && food.canAlwaysEat();
	}
}
