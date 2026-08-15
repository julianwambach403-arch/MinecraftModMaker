/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.misc.SessionStats;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class SessionStatsHud extends HudElement {
    public static final HudElementInfo<SessionStatsHud> INFO = new HudElementInfo<>(Hud.GROUP, "session-stats", "Session kills, deaths, pops and playtime.", SessionStatsHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    private final Setting<Boolean> playtime = sgGeneral.add(new BoolSetting.Builder()
        .name("playtime")
        .description("Show session playtime.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> kd = sgGeneral.add(new BoolSetting.Builder()
        .name("kills-deaths")
        .description("Show kills, deaths and K/D.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> streak = sgGeneral.add(new BoolSetting.Builder()
        .name("streak")
        .description("Show the current kill streak.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pops = sgGeneral.add(new BoolSetting.Builder()
        .name("totem-pops")
        .description("Show your totem pops this session.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Text shadow.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> labelColor = sgGeneral.add(new ColorSetting.Builder()
        .name("label-color")
        .description("Label color.")
        .defaultValue(new SettingColor(170, 170, 170))
        .build()
    );

    private final Setting<SettingColor> valueColor = sgGeneral.add(new ColorSetting.Builder()
        .name("value-color")
        .description("Value color.")
        .defaultValue(new SettingColor(0, 229, 204))
        .build()
    );

    private final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
        .name("custom-scale")
        .description("Applies a custom scale to this hud element.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Custom scale.")
        .visible(customScale::get)
        .defaultValue(1)
        .min(0.5)
        .sliderRange(0.5, 3)
        .build()
    );

    private final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
        .name("background")
        .description("Draws a background.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Background color.")
        .visible(background::get)
        .defaultValue(new SettingColor(15, 15, 18, 90))
        .build()
    );

    public SessionStatsHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        double s = getScale();
        double lineHeight = renderer.textHeight(shadow.get(), s) + 2;
        double yOff = 0;
        double maxWidth = 0;

        if (background.get()) {
            renderer.quad(x - 2, y - 2, getWidth() + 4, getHeight() + 4, backgroundColor.get());
        }

        if (playtime.get()) maxWidth = Math.max(maxWidth, line(renderer, "Time", SessionStats.getPlaytimeString(), yOff, s));
        if (playtime.get()) yOff += lineHeight;
        if (kd.get()) maxWidth = Math.max(maxWidth, line(renderer, "K/D", SessionStats.getKills() + " / " + SessionStats.getDeaths() + "  (" + SessionStats.getKdString() + ")", yOff, s));
        if (kd.get()) yOff += lineHeight;
        if (streak.get()) maxWidth = Math.max(maxWidth, line(renderer, "Streak", String.valueOf(SessionStats.getKillStreak()), yOff, s));
        if (streak.get()) yOff += lineHeight;
        if (pops.get()) maxWidth = Math.max(maxWidth, line(renderer, "Pops", String.valueOf(SessionStats.getTotemPops()), yOff, s));
        if (pops.get()) yOff += lineHeight;

        if (yOff == 0) {
            maxWidth = line(renderer, "Session", "MinClient", 0, s);
            yOff = lineHeight;
        }

        setSize(maxWidth, Math.max(lineHeight, yOff - 2));
    }

    private double line(HudRenderer renderer, String label, String value, double yOff, double s) {
        Color left = labelColor.get();
        Color right = valueColor.get();
        double x2 = renderer.text(label + " ", x, y + yOff, left, shadow.get(), s);
        x2 = renderer.text(value, x2, y + yOff, right, shadow.get(), s);
        return x2 - x;
    }

    private double getScale() {
        return customScale.get() ? scale.get() : Hud.get().getTextScale();
    }
}
