package de.minecraftmodmaker.baritoneplus.ai;

import de.minecraftmodmaker.baritoneplus.config.PlusConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.FluidTags;

import java.util.ArrayList;
import java.util.List;

public final class ThreatScanner {
	public enum Kind {
		NONE,
		CREEPER,
		HOSTILE,
		LAVA,
		FIRE,
		DROWN
	}

	public record Result(Kind kind, String label, List<BlockPos> from, double distance) {
		public static Result none() {
			return new Result(Kind.NONE, "none", List.of(), Double.POSITIVE_INFINITY);
		}

		public boolean present() {
			return kind != Kind.NONE;
		}
	}

	private ThreatScanner() {
	}

	public static Result scan(Player player, Iterable<Entity> entities, PlusConfig config) {
		if (player.isInLava() || player.level().getFluidState(player.blockPosition()).is(FluidTags.LAVA)) {
			return new Result(Kind.LAVA, "lava", List.of(player.blockPosition()), 0);
		}
		if (player.getRemainingFireTicks() > 20) {
			return new Result(Kind.FIRE, "fire", List.of(player.blockPosition()), 0);
		}
		if (player.isUnderWater() && player.getAirSupply() < 60) {
			return new Result(Kind.DROWN, "drown", List.of(player.blockPosition()), 0);
		}

		List<BlockPos> creepers = new ArrayList<>();
		List<BlockPos> hostiles = new ArrayList<>();
		double closestCreeper = Double.POSITIVE_INFINITY;
		double closestHostile = Double.POSITIVE_INFINITY;

		for (Entity entity : entities) {
			if (entity == null || entity == player || !entity.isAlive()) {
				continue;
			}
			double dist = entity.distanceTo(player);
			if (entity instanceof Creeper creeper && dist <= config.creeperFleeRange) {
				float swell = creeper.getSwelling(1.0f);
				if (swell > 0.15f || dist <= 5.5) {
					creepers.add(creeper.blockPosition());
					closestCreeper = Math.min(closestCreeper, dist);
				}
			} else if (entity instanceof Enemy && dist <= config.hostileFleeRange && player.getHealth() <= config.fleeHealth) {
				hostiles.add(entity.blockPosition());
				closestHostile = Math.min(closestHostile, dist);
			}
		}

		if (!creepers.isEmpty()) {
			return new Result(Kind.CREEPER, "creeper", creepers, closestCreeper);
		}
		if (!hostiles.isEmpty()) {
			return new Result(Kind.HOSTILE, "hostile", hostiles, closestHostile);
		}
		return Result.none();
	}
}
