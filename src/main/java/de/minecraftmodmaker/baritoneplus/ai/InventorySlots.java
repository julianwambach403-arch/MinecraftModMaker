package de.minecraftmodmaker.baritoneplus.ai;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class InventorySlots {
	private InventorySlots() {
	}

	public static Inventory inventory(LocalPlayer player) {
		return player.getInventory();
	}

	public static NonNullList<ItemStack> main(LocalPlayer player) {
		return inventory(player).getNonEquipmentItems();
	}

	public static int selectedSlot(LocalPlayer player) {
		return inventory(player).getSelectedSlot();
	}

	public static void selectHotbar(LocalPlayer player, int slot) {
		if (slot < 0 || slot > 8) {
			return;
		}
		inventory(player).setSelectedSlot(slot);
	}

	public static ItemStack selected(LocalPlayer player) {
		return inventory(player).getSelectedItem();
	}

	public static boolean isFull(LocalPlayer player) {
		for (ItemStack stack : main(player)) {
			if (stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}
}
