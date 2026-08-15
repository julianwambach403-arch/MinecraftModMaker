/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.game.PlayerKillEvent;
import meteordevelopment.meteorclient.events.game.TotemPopEvent;
import meteordevelopment.meteorclient.events.meteor.ActiveModulesChangedEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.SessionStats;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MinNotifications extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> lifetime = sgGeneral.add(new IntSetting.Builder()
        .name("lifetime-ms")
        .description("How long each notification stays on screen.")
        .defaultValue(2200)
        .min(400)
        .sliderRange(800, 6000)
        .build()
    );

    private final Setting<Integer> maxNotifs = sgGeneral.add(new IntSetting.Builder()
        .name("max")
        .description("Maximum notifications shown at once.")
        .defaultValue(6)
        .min(1)
        .sliderRange(1, 12)
        .build()
    );

    private final Setting<Boolean> modules = sgGeneral.add(new BoolSetting.Builder()
        .name("modules")
        .description("Notify when modules are toggled.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> kills = sgGeneral.add(new BoolSetting.Builder()
        .name("kills")
        .description("Notify when you get a kill.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pops = sgGeneral.add(new BoolSetting.Builder()
        .name("totem-pops")
        .description("Notify when a totem pops nearby.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> accent = sgGeneral.add(new ColorSetting.Builder()
        .name("accent")
        .description("Left accent bar color.")
        .defaultValue(new SettingColor(0, 229, 204, 255))
        .build()
    );

    private final Setting<SettingColor> background = sgGeneral.add(new ColorSetting.Builder()
        .name("background")
        .description("Notification background.")
        .defaultValue(new SettingColor(12, 16, 20, 190))
        .build()
    );

    private final List<Toast> toasts = new ArrayList<>();
    private final Set<Module> lastActive = new HashSet<>();
    private boolean primed;

    public MinNotifications() {
        super(Categories.Misc, "min-notifications", "Sleek on-screen toasts for toggles, pops and kills.");
    }

    @Override
    public void onActivate() {
        lastActive.clear();
        lastActive.addAll(Modules.get().getActive());
        primed = true;
    }

    @EventHandler
    private void onKill(PlayerKillEvent event) {
        if (kills.get()) push("Kill", event.name + "  ·  streak " + SessionStats.getKillStreak());
    }

    @EventHandler
    private void onPop(TotemPopEvent event) {
        if (pops.get()) push("Totem Pop", event.name);
    }

    @Override
    public void onDeactivate() {
        toasts.clear();
        primed = false;
    }

    public void push(String title, String body) {
        synchronized (toasts) {
            toasts.add(0, new Toast(title, body, System.currentTimeMillis()));
            while (toasts.size() > maxNotifs.get()) toasts.removeLast();
        }
    }

    @EventHandler
    private void onModulesChanged(ActiveModulesChangedEvent event) {
        if (!modules.get() || !primed) return;

        Set<Module> current = new HashSet<>(Modules.get().getActive());
        for (Module module : current) {
            if (!lastActive.contains(module) && module != this) {
                push("Enabled", module.title);
            }
        }
        for (Module module : lastActive) {
            if (!current.contains(module) && module != this) {
                push("Disabled", module.title);
            }
        }
        lastActive.clear();
        lastActive.addAll(current);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        long now = System.currentTimeMillis();
        synchronized (toasts) {
            toasts.removeIf(toast -> now - toast.createdAt > lifetime.get());
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        Font font = mc.font;
        long now = System.currentTimeMillis();
        int padding = 6;
        int width = 150;
        int height = 28;
        int x = event.screenWidth - width - 8;
        int y = 8;

        synchronized (toasts) {
            Renderer2D.COLOR.begin();
            for (Toast toast : toasts) {
                float life = 1f - ((now - toast.createdAt) / (float) lifetime.get());
                int alpha = (int) (255 * Math.min(1f, life * 6));

                Color bg = new Color(background.get());
                bg.a = Math.min(background.get().a, alpha);
                Color bar = new Color(accent.get());
                bar.a = Math.min(accent.get().a, alpha);

                Renderer2D.COLOR.quad(x, y, 3, height, bar);
                Renderer2D.COLOR.quad(x + 3, y, width - 3, height, bg);
                y += height + padding;
            }
            Renderer2D.COLOR.render();

            y = 8;
            for (Toast toast : toasts) {
                event.graphics.text(font, toast.title, x + 10, y + 3, 0xFF00E5CC);
                event.graphics.text(font, toast.body, x + 10, y + 14, 0xFFEDEDED);
                y += height + padding;
            }
        }
    }

    private record Toast(String title, String body, long createdAt) {}
}
