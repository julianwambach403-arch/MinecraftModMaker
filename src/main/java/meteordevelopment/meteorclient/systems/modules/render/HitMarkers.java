/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

public class HitMarkers extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> size = sgGeneral.add(new DoubleSetting.Builder()
        .name("size")
        .description("Length of each hitmarker line.")
        .defaultValue(8)
        .min(2)
        .sliderRange(4, 24)
        .build()
    );

    private final Setting<Double> gap = sgGeneral.add(new DoubleSetting.Builder()
        .name("gap")
        .description("Space from the crosshair to the marker.")
        .defaultValue(4)
        .min(0)
        .sliderRange(0, 16)
        .build()
    );

    private final Setting<Integer> duration = sgGeneral.add(new IntSetting.Builder()
        .name("duration-ms")
        .description("How long the marker stays on screen.")
        .defaultValue(280)
        .min(50)
        .sliderRange(80, 1000)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("Hitmarker color.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private final Setting<Boolean> sound = sgGeneral.add(new BoolSetting.Builder()
        .name("sound")
        .description("Play a tick sound on hit.")
        .defaultValue(true)
        .build()
    );

    private long hitAt;

    public HitMarkers() {
        super(Categories.Render, "hit-markers", "Shows a crisp marker whenever you land a hit.");
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (!(event.entity instanceof LivingEntity living) || living == mc.player) return;
        if (living.isDeadOrDying()) return;

        hitAt = System.currentTimeMillis();

        if (sound.get() && mc.level != null && mc.player != null) {
            mc.level.playSound(mc.player, mc.player, SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.6f, 1.35f);
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (hitAt == 0) return;

        long elapsed = System.currentTimeMillis() - hitAt;
        if (elapsed > duration.get()) return;

        float progress = 1f - (elapsed / (float) duration.get());
        Color marker = new Color(color.get());
        marker.a = (int) (color.get().a * progress);

        double cx = event.screenWidth / 2.0;
        double cy = event.screenHeight / 2.0;
        double s = size.get();
        double g = gap.get();

        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.line(cx - g - s, cy - g - s, cx - g, cy - g, marker);
        Renderer2D.COLOR.line(cx + g, cy - g, cx + g + s, cy - g - s, marker);
        Renderer2D.COLOR.line(cx - g - s, cy + g + s, cx - g, cy + g, marker);
        Renderer2D.COLOR.line(cx + g, cy + g, cx + g + s, cy + g + s, marker);
        Renderer2D.COLOR.render();
    }
}
