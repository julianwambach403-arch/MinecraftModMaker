/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.game.PlayerKillEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.SessionStats;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class KillEffects extends Module {
    public enum Effect {
        Lightning,
        Totem,
        Explosion,
        Confetti
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Effect> effect = sgGeneral.add(new EnumSetting.Builder<Effect>()
        .name("effect")
        .description("Visual effect spawned on a kill.")
        .defaultValue(Effect.Lightning)
        .build()
    );

    private final Setting<Boolean> sound = sgGeneral.add(new BoolSetting.Builder()
        .name("sound")
        .description("Play a sound when you get a kill.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> particleCount = sgGeneral.add(new IntSetting.Builder()
        .name("particles")
        .description("How many particles to spawn.")
        .defaultValue(40)
        .min(4)
        .sliderRange(8, 120)
        .build()
    );

    private final Setting<Double> lightningDuration = sgGeneral.add(new DoubleSetting.Builder()
        .name("lightning-seconds")
        .description("How long drawn lightning bolts last.")
        .defaultValue(0.7)
        .min(0.1)
        .sliderRange(0.2, 3)
        .visible(() -> effect.get() == Effect.Lightning)
        .build()
    );

    private final Setting<Boolean> chat = sgGeneral.add(new BoolSetting.Builder()
        .name("chat")
        .description("Send a MinClient kill message in client chat.")
        .defaultValue(true)
        .build()
    );

    private final List<Bolt> bolts = new ArrayList<>();

    public KillEffects() {
        super(Categories.Render, "kill-effects", "Plays flashy effects when you get a kill.");
    }

    @Override
    public void onDeactivate() {
        bolts.clear();
    }

    @EventHandler
    private void onKill(PlayerKillEvent event) {
        if (mc.level == null || mc.player == null) return;

        Vec3 pos = event.pos;
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        switch (effect.get()) {
            case Lightning -> {
                synchronized (bolts) {
                    bolts.add(new Bolt(pos, System.currentTimeMillis()));
                }
            }
            case Totem -> spawnBurst(pos, rng, true);
            case Explosion -> {
                mc.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 1, pos.z, 0, 0, 0);
                spawnBurst(pos, rng, false);
            }
            case Confetti -> spawnConfetti(pos, rng);
        }

        if (sound.get()) {
            mc.level.playSound(mc.player, mc.player, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.AMBIENT, 4.0f, 1.1f);
        }

        if (chat.get()) {
            info("Eliminated (highlight)%s (default)· streak (highlight)%d", event.name, SessionStats.getKillStreak());
        }
    }

    private void spawnBurst(Vec3 pos, ThreadLocalRandom rng, boolean totem) {
        for (int i = 0; i < particleCount.get(); i++) {
            double vx = rng.nextDouble(-0.4, 0.4);
            double vy = rng.nextDouble(0.1, 0.8);
            double vz = rng.nextDouble(-0.4, 0.4);
            if (totem) {
                mc.level.addParticle(ParticleTypes.TOTEM_OF_UNDYING, pos.x, pos.y + 1, pos.z, vx, vy, vz);
            } else {
                mc.level.addParticle(ParticleTypes.END_ROD, pos.x, pos.y + 1, pos.z, vx, vy, vz);
            }
        }
    }

    private void spawnConfetti(Vec3 pos, ThreadLocalRandom rng) {
        for (int i = 0; i < particleCount.get(); i++) {
            double vx = rng.nextDouble(-0.6, 0.6);
            double vy = rng.nextDouble(0.2, 1.1);
            double vz = rng.nextDouble(-0.6, 0.6);
            mc.level.addParticle(ParticleTypes.FIREWORK, pos.x, pos.y + 1, pos.z, vx, vy, vz);
            mc.level.addParticle(ParticleTypes.END_ROD, pos.x, pos.y + 1, pos.z, vx * 0.4, vy, vz * 0.4);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        long now = System.currentTimeMillis();
        long lifetime = (long) (lightningDuration.get() * 1000);

        synchronized (bolts) {
            bolts.removeIf(bolt -> now - bolt.spawnedAt > lifetime);
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        long now = System.currentTimeMillis();
        long lifetime = (long) (lightningDuration.get() * 1000);

        synchronized (bolts) {
            for (Bolt bolt : bolts) {
                double age = (now - bolt.spawnedAt) / (double) lifetime;
                int alpha = (int) (255 * (1.0 - age));
                Color color = new Color(180, 220, 255, Math.max(0, alpha));

                double x = bolt.pos.x - event.offsetX;
                double y = bolt.pos.y - event.offsetY;
                double z = bolt.pos.z - event.offsetZ;

                double px = x;
                double py = y;
                double pz = z;
                for (int i = 1; i <= 8; i++) {
                    double nx = x + Math.sin(bolt.seed + i * 1.7) * 0.35;
                    double ny = y + i * 1.15;
                    double nz = z + Math.cos(bolt.seed + i * 1.3) * 0.35;
                    event.renderer.line(px, py, pz, nx, ny, nz, color);
                    px = nx;
                    py = ny;
                    pz = nz;
                }
            }
        }
    }

    private static final class Bolt {
        private final Vec3 pos;
        private final long spawnedAt;
        private final double seed;

        private Bolt(Vec3 pos, long spawnedAt) {
            this.pos = pos;
            this.spawnedAt = spawnedAt;
            this.seed = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
        }
    }
}
