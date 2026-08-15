/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class WatermarkHud extends HudElement {
    public static final HudElementInfo<WatermarkHud> INFO = new HudElementInfo<>(Hud.GROUP, "min-watermark", "MinClient branded watermark.", WatermarkHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    private final Setting<Boolean> rainbow = sgGeneral.add(new BoolSetting.Builder()
        .name("rainbow")
        .description("Rainbow title color.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> titleColor = sgGeneral.add(new ColorSetting.Builder()
        .name("title-color")
        .description("Title color when rainbow is off.")
        .defaultValue(new SettingColor(0, 229, 204))
        .visible(() -> !rainbow.get())
        .build()
    );

    private final Setting<SettingColor> subtitleColor = sgGeneral.add(new ColorSetting.Builder()
        .name("subtitle-color")
        .description("Color of the author line.")
        .defaultValue(new SettingColor(200, 200, 200))
        .build()
    );

    private final Setting<Boolean> showAuthor = sgGeneral.add(new BoolSetting.Builder()
        .name("author")
        .description("Show 'by Minju26' under the title.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showVersion = sgGeneral.add(new BoolSetting.Builder()
        .name("version")
        .description("Show the client version.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Text shadow.")
        .defaultValue(true)
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
        .defaultValue(1.15)
        .min(0.5)
        .sliderRange(0.5, 3)
        .build()
    );

    private final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
        .name("background")
        .description("Draws a background behind the watermark.")
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

    public WatermarkHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        double s = getScale();
        String title = MeteorClient.NAME;
        String version = showVersion.get() ? " " + MeteorClient.VERSION + (MeteorClient.BUILD_NUMBER.isEmpty() ? "" : "-" + MeteorClient.BUILD_NUMBER) : "";
        String subtitle = showAuthor.get() ? "by " + MeteorClient.AUTHOR : "";

        Color rainbowColor = Color.fromHsv((System.currentTimeMillis() % 4000) / 4000.0 * 360.0, 0.75, 1);
        Color nameColor = rainbow.get() ? rainbowColor : titleColor.get();

        double titleWidth = renderer.textWidth(title + version, shadow.get(), s);
        double subWidth = subtitle.isEmpty() ? 0 : renderer.textWidth(subtitle, shadow.get(), s * 0.85);
        double width = Math.max(titleWidth, subWidth);
        double height = renderer.textHeight(shadow.get(), s);
        if (!subtitle.isEmpty()) height += 2 + renderer.textHeight(shadow.get(), s * 0.85);

        if (background.get()) {
            renderer.quad(x - 3, y - 2, width + 6, height + 4, backgroundColor.get());
        }

        renderer.text(title, x, y, nameColor, shadow.get(), s);
        if (showVersion.get()) {
            renderer.text(version, x + renderer.textWidth(title, shadow.get(), s), y, subtitleColor.get(), shadow.get(), s);
        }
        if (!subtitle.isEmpty()) {
            renderer.text(subtitle, x, y + renderer.textHeight(shadow.get(), s) + 2, subtitleColor.get(), shadow.get(), s * 0.85);
        }

        setSize(width, height);
    }

    private double getScale() {
        return customScale.get() ? scale.get() : Hud.get().getTextScale();
    }
}
