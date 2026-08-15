/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.game;

public class TotemPopEvent {
    private static final TotemPopEvent INSTANCE = new TotemPopEvent();

    public String name;
    public boolean self;

    public static TotemPopEvent get(String name, boolean self) {
        INSTANCE.name = name;
        INSTANCE.self = self;
        return INSTANCE;
    }
}
