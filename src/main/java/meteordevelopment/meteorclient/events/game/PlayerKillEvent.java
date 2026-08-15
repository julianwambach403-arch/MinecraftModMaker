/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.game;

import net.minecraft.world.phys.Vec3;

public class PlayerKillEvent {
    private static final PlayerKillEvent INSTANCE = new PlayerKillEvent();

    public String name;
    public Vec3 pos;

    public static PlayerKillEvent get(String name, Vec3 pos) {
        INSTANCE.name = name;
        INSTANCE.pos = pos;
        return INSTANCE;
    }
}
