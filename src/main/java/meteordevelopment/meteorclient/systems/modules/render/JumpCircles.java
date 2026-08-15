/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class JumpCircles extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> maxRadius = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-radius")
        .description("How far the circle expands.")
        .defaultValue(1.6)
        .min(0.3)
        .sliderRange(0.5, 4)
        .build()
    );

    private final Setting<Double> duration = sgGeneral.add(new DoubleSetting.Builder()
        .name("duration")
        .description("How long each circle lasts in seconds.")
        .defaultValue(0.85)
        .min(0.15)
        .sliderRange(0.2, 2.5)
        .build()
    );

    private final Setting<Integer> points = sgGeneral.add(new IntSetting.Builder()
        .name("points")
        .description("Circle smoothness.")
        .defaultValue(32)
        .min(8)
        .sliderRange(12, 64)
        .build()
    );

    private final Setting<Boolean> rainbow = sgGeneral.add(new BoolSetting.Builder()
        .name("rainbow")
        .description("Rainbow jump circles.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("Circle color when rainbow is off.")
        .defaultValue(new SettingColor(0, 229, 204, 200))
        .visible(() -> !rainbow.get())
        .build()
    );

    private final List<Circle> circles = new ArrayList<>();
    private boolean wasOnGround = true;

    public JumpCircles() {
        super(Categories.Render, "jump-circles", "Spawns expanding rainbow rings when you jump.");
    }

    @Override
    public void onDeactivate() {
        circles.clear();
        wasOnGround = true;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        boolean onGround = mc.player.onGround();
        if (wasOnGround && !onGround && mc.player.getDeltaMovement().y > 0) {
            circles.add(new Circle(mc.player.position(), System.currentTimeMillis()));
        }
        wasOnGround = onGround;

        long now = System.currentTimeMillis();
        long life = (long) (duration.get() * 1000);
        circles.removeIf(circle -> now - circle.spawnedAt > life);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        long now = System.currentTimeMillis();
        long life = (long) (duration.get() * 1000);
        int sides = points.get();

        for (Circle circle : circles) {
            double progress = (now - circle.spawnedAt) / (double) life;
            double radius = maxRadius.get() * progress;
            int alpha = (int) (220 * (1.0 - progress));

            double x = circle.pos.x - event.offsetX;
            double y = circle.pos.y - event.offsetY + 0.05;
            double z = circle.pos.z - event.offsetZ;

            double prevX = x + radius;
            double prevZ = z;

            for (int i = 1; i <= sides; i++) {
                double angle = Math.toRadians(360.0 * i / sides);
                double nx = x + Math.cos(angle) * radius;
                double nz = z + Math.sin(angle) * radius;

                Color rim = rainbow.get()
                    ? Color.fromHsv((progress * 180 + i * (360.0 / sides)) % 360.0, 0.8, 1)
                    : new Color(color.get());
                rim.a = alpha;

                event.renderer.line(prevX, y, prevZ, nx, y, nz, rim);
                prevX = nx;
                prevZ = nz;
            }
        }
    }

    private record Circle(Vec3 pos, long spawnedAt) {}
}
