package de.minecraftmodmaker.baritoneplus.input;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalBlock;
import com.mojang.blaze3d.platform.InputConstants;
import de.minecraftmodmaker.baritoneplus.ai.BrainRuntime;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

public final class PlusKeybinds {
	private static KeyMapping cancelKey;
	private static KeyMapping gotoKey;
	private static KeyMapping hudKey;
	private static KeyMapping brainKey;

	private PlusKeybinds() {
	}

	public static void register(BrainRuntime runtime) {
		cancelKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.baritoneplus.cancel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KeyMapping.Category.MISC));
		gotoKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.baritoneplus.goto", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KeyMapping.Category.MISC));
		hudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.baritoneplus.hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, KeyMapping.Category.MISC));
		brainKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.baritoneplus.brain", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, KeyMapping.Category.MISC));
	}

	public static void tick(Minecraft client, BrainRuntime runtime) {
		if (client.player == null) {
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
		while (cancelKey.consumeClick()) {
			baritone.getPathingBehavior().cancelEverything();
			tell(client, "Stopped Baritone");
		}
		while (gotoKey.consumeClick()) {
			HitResult hit = client.hitResult;
			if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
				BlockPos pos = block.getBlockPos().above();
				baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(pos));
				tell(client, "Going to " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
			} else {
				tell(client, "Look at a block first");
			}
		}
		while (hudKey.consumeClick()) {
			runtime.config().hud = !runtime.config().hud;
			tell(client, "HUD " + (runtime.config().hud ? "on" : "off"));
		}
		while (brainKey.consumeClick()) {
			runtime.config().survivalBrain = !runtime.config().survivalBrain;
			tell(client, "Survival brain " + (runtime.config().survivalBrain ? "on" : "off"));
		}
	}

	private static void tell(Minecraft client, String message) {
		if (client.player != null) {
			client.player.sendSystemMessage(Component.literal("[Plus] " + message));
		}
	}
}
