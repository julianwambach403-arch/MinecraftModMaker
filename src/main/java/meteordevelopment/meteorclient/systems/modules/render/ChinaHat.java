/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class ChinaHat extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> radius = sgGeneral.add(new DoubleSetting.Builder()
        .name("radius")
        .description("Radius of the hat brim.")
        .defaultValue(0.65)
        .min(0.1)
        .sliderRange(0.2, 1.5)
        .build()
    );

    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
        .name("height")
        .description("How tall the hat cone is.")
        .defaultValue(0.35)
        .min(0.05)
        .sliderRange(0.1, 1)
        .build()
    );

    private final Setting<Double> yOffset = sgGeneral.add(new DoubleSetting.Builder()
        .name("y-offset")
        .description("Extra offset above the player's head.")
        .defaultValue(0.35)
        .sliderRange(-0.5, 1)
        .build()
    );

    private final Setting<Integer> sides = sgGeneral.add(new IntSetting.Builder()
        .name("sides")
        .description("How smooth the hat circle is.")
        .defaultValue(24)
        .min(6)
        .sliderRange(8, 64)
        .build()
    );

    private final Setting<Double> spinSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("spin-speed")
        .description("How fast the rainbow hat spins.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 5)
        .build()
    );

    private final Setting<Boolean> rainbow = sgGeneral.add(new BoolSetting.Builder()
        .name("rainbow")
        .description("Rainbow brim colors.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("Solid hat color when rainbow is off.")
        .defaultValue(new SettingColor(0, 229, 204, 180))
        .visible(() -> !rainbow.get())
        .build()
    );

    private final Setting<Boolean> self = sgGeneral.add(new BoolSetting.Builder()
        .name("self")
        .description("Render the hat on yourself.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> others = sgGeneral.add(new BoolSetting.Builder()
        .name("others")
        .description("Render the hat on other players.")
        .defaultValue(false)
        .build()
    );

    public ChinaHat() {
        super(Categories.Render, "china-hat", "Renders a spinning rainbow cone above players.");
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.level == null || mc.player == null) return;

        for (Player player : mc.level.players()) {
            if (player == mc.player && !self.get()) continue;
            if (player != mc.player && !others.get()) continue;
            if (player.isInvisible()) continue;

            renderHat(event, player);
        }
    }

    private void renderHat(Render3DEvent event, Player player) {
        double px = Mth.lerp(event.tickDelta, player.xo, player.getX()) - event.offsetX;
        double py = Mth.lerp(event.tickDelta, player.yo, player.getY()) - event.offsetY + player.getBbHeight() + yOffset.get();
        double pz = Mth.lerp(event.tickDelta, player.zo, player.getZ()) - event.offsetZ;

        int count = sides.get();
        double spin = (System.currentTimeMillis() / 20.0) * spinSpeed.get();
        double brimY = py;
        double tipY = py + height.get();

        double prevX = px + Math.cos(Math.toRadians(spin)) * radius.get();
        double prevZ = pz + Math.sin(Math.toRadians(spin)) * radius.get();

        for (int i = 1; i <= count; i++) {
            double angle = Math.toRadians(spin + (360.0 * i / count));
            double x = px + Math.cos(angle) * radius.get();
            double z = pz + Math.sin(angle) * radius.get();

            Color rim = rainbow.get()
                ? Color.fromHsv((spin + (360.0 * i / count)) % 360.0, 0.85, 1)
                : color.get();
            rim.a = rainbow.get() ? 200 : color.get().a;

            event.renderer.line(prevX, brimY, prevZ, x, brimY, z, rim);
            event.renderer.line(px, tipY, pz, x, brimY, z, rim);

            prevX = x;
            prevZ = z;
        }
    }
}
