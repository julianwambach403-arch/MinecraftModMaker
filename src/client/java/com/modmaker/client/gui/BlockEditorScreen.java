package com.modmaker.client.gui;

import com.modmaker.ModMaker;
import com.modmaker.content.BlockDefinition;
import com.modmaker.content.ContentManager;
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

/** Wizard for creating or editing a block definition; changes apply without restart. */
public class BlockEditorScreen extends BaseScreen {
	private final BlockDefinition original;
	private final BlockDefinition def;
	private final boolean isNew;

	private EditBox idBox;
	private EditBox nameBox;
	private EditBox tooltipBox;
	private EditBox hardnessBox;
	private EditBox lightBox;
	private Button textureButton;
	private Button dropItemButton;

	public BlockEditorScreen(WorkspaceScreen parent, BlockDefinition existing) {
		super(Component.translatable(existing == null ? "modmaker.gui.block.title_new" : "modmaker.gui.block.title_edit"), parent);
		this.original = existing;
		this.isNew = existing == null;
		this.def = existing == null ? new BlockDefinition() : copy(existing);
	}

	private static BlockDefinition copy(BlockDefinition source) {
		BlockDefinition copy = new BlockDefinition();
		copy.id = source.id;
		copy.displayName = source.displayName;
		copy.texture = source.texture;
		copy.tooltip = source.tooltip;
		copy.hardness = source.hardness;
		copy.resistance = source.resistance;
		copy.lightLevel = source.lightLevel;
		copy.soundType = source.soundType;
		copy.requiresTool = source.requiresTool;
		copy.dropSelf = source.dropSelf;
		copy.dropItem = source.dropItem;
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

		label(Component.translatable("modmaker.gui.field.hardness"), lx, y + 6);
		hardnessBox = addEditBox(lFieldX, y, 80, 20, String.valueOf(def.hardness), Component.literal("1.5"));
		y += step;

		label(Component.translatable("modmaker.gui.field.light"), lx, y + 6);
		lightBox = addEditBox(lFieldX, y, 80, 20, String.valueOf(def.lightLevel), Component.literal("0"));

		// right column -----------------------------------------------------
		y = 36;
		addCycleButton(rx, y, 168, 20, Component.translatable("modmaker.gui.field.sound"),
				List.of("stone", "wood", "grass", "gravel", "sand", "metal", "glass", "wool"), def.soundType,
				value -> Component.translatable("modmaker.gui.sound." + value),
				value -> def.soundType = value);
		y += step;

		addToggleButton(rx, y, 168, 20, Component.translatable("modmaker.gui.field.requires_tool"),
				def.requiresTool, value -> def.requiresTool = value);
		y += step;

		addToggleButton(rx, y, 168, 20, Component.translatable("modmaker.gui.field.drop_self"),
				def.dropSelf, value -> def.dropSelf = value);
		y += step;

		dropItemButton = addButton(rx, y, 168, 20, dropItemLabel(), () ->
				open(new ItemPickerScreen(this, ref -> {
					def.dropItem = ref == null ? "" : ref;
					dropItemButton.setMessage(dropItemLabel());
				})));

		// bottom row -------------------------------------------------------
		int by = height - 28;
		addButton(cx - 160, by, 76, 20, Component.translatable("modmaker.gui.save"), this::save);
		if (ModMaker.server() != null && !isNew) {
			addButton(cx - 80, by, 76, 20, Component.translatable("modmaker.gui.give"), this::giveToPlayer);
		}
		if (!isNew) {
			addButton(cx, by, 76, 20, Component.translatable("modmaker.gui.delete"), () -> {
				ContentManager.get().deleteBlock(original.id);
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

	private Component dropItemLabel() {
		String name = def.dropItem == null || def.dropItem.isBlank()
				? Component.translatable("modmaker.gui.drop.nothing").getString()
				: def.dropItem;
		return Component.translatable("modmaker.gui.field.drop_item", name);
	}

	private void save() {
		def.id = isNew ? ContentManager.sanitizeId(idBox.getValue()) : original.id;
		def.displayName = nameBox.getValue().isBlank() ? def.id : nameBox.getValue();
		def.tooltip = tooltipBox.getValue();
		def.hardness = parseFloat(hardnessBox.getValue(), 1.5f);
		def.lightLevel = Math.max(0, Math.min(15, parseInt(lightBox.getValue(), 0)));
		ContentManager.get().saveBlock(def);
		onClose();
	}

	private void giveToPlayer() {
		MinecraftServer server = ModMaker.server();
		assert minecraft != null;
		if (server == null || minecraft.player == null) return;
		var uuid = minecraft.player.getUUID();
		server.execute(() -> {
			ServerPlayer player = server.getPlayerList().getPlayer(uuid);
			Integer slot = ContentManager.get().blockSlot(def.id);
			if (player == null || slot == null) return;
			ItemStack stack = new ItemStack(PlaceholderPool.BLOCK_ITEMS[slot], 16);
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
			Integer slot = ContentManager.get().blockSlot(original.id);
			if (slot != null) {
				graphics.item(new ItemStack(PlaceholderPool.BLOCK_ITEMS[slot]), width / 2 + 140, height - 62);
			}
		}
	}
}
