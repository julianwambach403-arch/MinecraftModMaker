package com.modmaker.client.gui;

import com.modmaker.ModMaker;
import com.modmaker.content.ContentManager;
import com.modmaker.content.ItemDefinition;
import com.modmaker.registry.PlaceholderPool;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * MCreator-style wizard for creating or editing an item definition. Changes apply live:
 * saving re-binds the definition, restamps carried stacks and refreshes the resource pack.
 */
public class ItemEditorScreen extends BaseScreen {
	private final ItemDefinition original;
	private final ItemDefinition def;
	private final boolean isNew;

	private EditBox idBox;
	private EditBox nameBox;
	private EditBox tooltipBox;
	private EditBox stackBox;
	private EditBox nutritionBox;
	private EditBox saturationBox;
	private EditBox speedBox;
	private EditBox damageBox;
	private EditBox durabilityBox;
	private Button textureButton;

	public ItemEditorScreen(WorkspaceScreen parent, ItemDefinition existing) {
		super(Component.translatable(existing == null ? "modmaker.gui.item.title_new" : "modmaker.gui.item.title_edit"), parent);
		this.original = existing;
		this.isNew = existing == null;
		this.def = existing == null ? new ItemDefinition() : copy(existing);
	}

	private static ItemDefinition copy(ItemDefinition source) {
		ItemDefinition copy = new ItemDefinition();
		copy.id = source.id;
		copy.displayName = source.displayName;
		copy.texture = source.texture;
		copy.tooltip = source.tooltip;
		copy.stackSize = source.stackSize;
		copy.rarity = source.rarity;
		copy.glint = source.glint;
		copy.foodEnabled = source.foodEnabled;
		copy.foodNutrition = source.foodNutrition;
		copy.foodSaturation = source.foodSaturation;
		copy.foodAlwaysEdible = source.foodAlwaysEdible;
		copy.toolType = source.toolType;
		copy.miningSpeed = source.miningSpeed;
		copy.attackDamage = source.attackDamage;
		copy.attackSpeed = source.attackSpeed;
		copy.durability = source.durability;
		copy.version = source.version;
		return copy;
	}

	@Override
	protected void init() {
		super.init();
		int cx = width / 2;
		int lx = cx - 160;
		int lFieldX = cx - 88;
		int rx = cx + 8;
		int rFieldX = cx + 78;
		int y = 36;
		int step = 24;

		// left column ------------------------------------------------------
		label(Component.translatable("modmaker.gui.field.id"), lx, y + 6);
		idBox = addEditBox(lFieldX, y, 80, 20, def.id, Component.literal("id"));
		if (!isNew) idBox.setEditable(false);
		y += step;

		label(Component.translatable("modmaker.gui.field.name"), lx, y + 6);
		nameBox = addEditBox(lFieldX, y, 80, 20, def.displayName, Component.literal("name"));
		y += step;

		label(Component.translatable("modmaker.gui.field.tooltip"), lx, y + 6);
		tooltipBox = addEditBox(lFieldX, y, 80, 20, def.tooltip, Component.literal("tooltip"));
		y += step;

		textureButton = addButton(lx, y, 168, 20, textureLabel(), () ->
				open(new TexturePickerScreen(this, texture -> {
					def.texture = texture == null ? "" : texture;
					textureButton.setMessage(textureLabel());
				})));
		y += step;

		label(Component.translatable("modmaker.gui.field.stack"), lx, y + 6);
		stackBox = addEditBox(lFieldX, y, 80, 20, String.valueOf(def.stackSize), Component.literal("64"));
		y += step;

		addCycleButton(lx, y, 168, 20, Component.translatable("modmaker.gui.field.rarity"),
				List.of("common", "uncommon", "rare", "epic"), def.rarity,
				value -> Component.translatable("modmaker.gui.rarity." + value),
				value -> def.rarity = value);
		y += step;

		addToggleButton(lx, y, 168, 20, Component.translatable("modmaker.gui.field.glint"),
				def.glint, value -> def.glint = value);

		// right column -----------------------------------------------------
		y = 36;
		addToggleButton(rx, y, 168, 20, Component.translatable("modmaker.gui.field.food"),
				def.foodEnabled, value -> def.foodEnabled = value);
		y += step;

		label(Component.translatable("modmaker.gui.field.nutrition"), rx, y + 6);
		nutritionBox = addEditBox(rFieldX, y, 80, 20, String.valueOf(def.foodNutrition), Component.literal("4"));
		y += step;

		label(Component.translatable("modmaker.gui.field.saturation"), rx, y + 6);
		saturationBox = addEditBox(rFieldX, y, 80, 20, String.valueOf(def.foodSaturation), Component.literal("0.3"));
		y += step;

		addCycleButton(rx, y, 168, 20, Component.translatable("modmaker.gui.field.tool"),
				List.of("none", "pickaxe", "axe", "shovel", "hoe", "sword"), def.toolType,
				value -> Component.translatable("modmaker.gui.tool." + value),
				value -> def.toolType = value);
		y += step;

		label(Component.translatable("modmaker.gui.field.mining_speed"), rx, y + 6);
		speedBox = addEditBox(rFieldX, y, 80, 20, String.valueOf(def.miningSpeed), Component.literal("4.0"));
		y += step;

		label(Component.translatable("modmaker.gui.field.attack"), rx, y + 6);
		damageBox = addEditBox(rFieldX, y, 80, 20, String.valueOf(def.attackDamage), Component.literal("3.0"));
		y += step;

		label(Component.translatable("modmaker.gui.field.durability"), rx, y + 6);
		durabilityBox = addEditBox(rFieldX, y, 80, 20, String.valueOf(def.durability), Component.literal("0"));

		// bottom row -------------------------------------------------------
		int by = height - 28;
		addButton(cx - 160, by, 76, 20, Component.translatable("modmaker.gui.save"), this::save);
		if (ModMaker.server() != null && !isNew) {
			addButton(cx - 80, by, 76, 20, Component.translatable("modmaker.gui.give"), this::giveToPlayer);
		}
		if (!isNew) {
			addButton(cx, by, 76, 20, Component.translatable("modmaker.gui.delete"), () -> {
				ContentManager.get().deleteItem(original.id);
				onClose();
			});
		}
		addButton(cx + 84, by, 76, 20, Component.translatable("gui.cancel"), this::onClose);
	}

	private Component textureLabel() {
		String name = def.texture == null || def.texture.isBlank()
				? Component.translatable("modmaker.gui.texture.default").getString()
				: def.texture;
		return Component.translatable("modmaker.gui.field.texture", name);
	}

	private void save() {
		def.id = isNew ? ContentManager.sanitizeId(idBox.getValue()) : original.id;
		def.displayName = nameBox.getValue().isBlank() ? def.id : nameBox.getValue();
		def.tooltip = tooltipBox.getValue();
		def.stackSize = Math.max(1, Math.min(99, parseInt(stackBox.getValue(), 64)));
		def.foodNutrition = Math.max(0, parseInt(nutritionBox.getValue(), 4));
		def.foodSaturation = Math.max(0, parseFloat(saturationBox.getValue(), 0.3f));
		def.miningSpeed = Math.max(1, parseFloat(speedBox.getValue(), 4.0f));
		def.attackDamage = parseFloat(damageBox.getValue(), 3.0f);
		def.durability = Math.max(0, parseInt(durabilityBox.getValue(), 0));
		ContentManager.get().saveItem(def);
		onClose();
	}

	private void giveToPlayer() {
		MinecraftServer server = ModMaker.server();
		assert minecraft != null;
		if (server == null || minecraft.player == null) return;
		var uuid = minecraft.player.getUUID();
		server.execute(() -> {
			ServerPlayer player = server.getPlayerList().getPlayer(uuid);
			Integer slot = ContentManager.get().itemSlot(def.id);
			if (player == null || slot == null) return;
			ItemStack stack = PlaceholderPool.ITEMS[slot].getDefaultInstance();
			if (!player.getInventory().add(stack)) {
				player.drop(stack, false);
			}
		});
	}

	@Override
	protected void extractExtra(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		TexturePreviews.Preview preview = TexturePreviews.get(def.texture);
		if (preview != null) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, preview.id(), width / 2 + 100, height - 70,
					0, 0, 32, 32, preview.width(), preview.height(), preview.width(), preview.height());
		}
		if (!isNew) {
			Integer slot = ContentManager.get().itemSlot(original.id);
			if (slot != null) {
				graphics.item(PlaceholderPool.ITEMS[slot].getDefaultInstance(), width / 2 + 140, height - 62);
			}
		}
	}
}
