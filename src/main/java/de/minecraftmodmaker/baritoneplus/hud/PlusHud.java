package de.minecraftmodmaker.baritoneplus.hud;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import de.minecraftmodmaker.baritoneplus.BaritonePlus;
import de.minecraftmodmaker.baritoneplus.ai.BrainRuntime;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class PlusHud {
	private PlusHud() {
	}

	public static void register(BrainRuntime runtime) {
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(BaritonePlus.MOD_ID, "overlay"),
				(graphics, deltaTracker) -> render(graphics, deltaTracker, runtime)
		);
	}

	private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, BrainRuntime runtime) {
		if (!runtime.config().hud) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null || minecraft.gui.hud.isHidden()) {
			return;
		}
		IBaritone baritone;
		try {
			baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
		} catch (Throwable ignored) {
			return;
		}
		if (baritone == null) {
			return;
		}

		Font font = minecraft.font;
		var pathing = baritone.getPathingBehavior();
		String process = baritone.getPathingControlManager().mostRecentInControl()
				.map(item -> item.displayName())
				.orElse("idle");
		String goal = String.valueOf(pathing.getGoal());
		if (goal.length() > 42) {
			goal = goal.substring(0, 42) + "…";
		}
		String eta = pathing.estimatedTicksToGoal()
				.map(ticks -> String.format("%.0fs", ticks / 20.0))
				.orElse("-");
		String health = String.format("HP %.0f  hunger %d", minecraft.player.getHealth(), minecraft.player.getFoodData().getFoodLevel());

		int x = 6;
		int y = 6;
		int width = 230;
		int height = 62;
		graphics.fill(x, y, x + width, y + height, 0xAA101820);
		graphics.fill(x, y, x + 3, y + height, 0xFF50C8A0);
		draw(graphics, font, "Baritone Plus", x + 10, y + 5, 0xFF7DFFC8);
		draw(graphics, font, process, x + 10, y + 16, 0xFFFFFFFF);
		draw(graphics, font, "goal " + goal, x + 10, y + 27, 0xFFD0D8E0);
		draw(graphics, font, "eta " + eta + "   " + health, x + 10, y + 38, 0xFFB8C4D0);
		draw(graphics, font, runtime.statusLine(), x + 10, y + 49, 0xFFFFD27A);
	}

	private static void draw(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
		graphics.text(font, text, x, y, color, true);
	}
}
