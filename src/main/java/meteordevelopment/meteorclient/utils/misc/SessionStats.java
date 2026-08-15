/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.PlayerKillEvent;
import meteordevelopment.meteorclient.events.game.TotemPopEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class SessionStats {
    private static long sessionStartMs;
    private static int kills;
    private static int deaths;
    private static int totemPops;
    private static int killStreak;
    private static int bestKillStreak;

    private static UUID lastTargetId;
    private static long lastAttackMs;
    private static Vec3 lastTargetPos = Vec3.ZERO;
    private static String lastTargetName = "";
    private static boolean wasDead;

    private static final Map<UUID, Long> countedKills = new ConcurrentHashMap<>();

    private SessionStats() {
    }

    @PreInit
    public static void init() {
        reset();
        MeteorClient.EVENT_BUS.subscribe(SessionStats.class);
    }

    public static void reset() {
        sessionStartMs = System.currentTimeMillis();
        kills = 0;
        deaths = 0;
        totemPops = 0;
        killStreak = 0;
        lastTargetId = null;
        lastAttackMs = 0;
        lastTargetPos = Vec3.ZERO;
        lastTargetName = "";
        wasDead = false;
        countedKills.clear();
    }

    public static int getKills() {
        return kills;
    }

    public static int getDeaths() {
        return deaths;
    }

    public static int getTotemPops() {
        return totemPops;
    }

    public static int getKillStreak() {
        return killStreak;
    }

    public static int getBestKillStreak() {
        return bestKillStreak;
    }

    public static long getPlaytimeMs() {
        return Math.max(0, System.currentTimeMillis() - sessionStartMs);
    }

    public static String getPlaytimeString() {
        long seconds = getPlaytimeMs() / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) return "%d:%02d:%02d".formatted(hours, minutes, secs);
        return "%d:%02d".formatted(minutes, secs);
    }

    public static String getKdString() {
        if (deaths == 0) return kills + ".00";
        return "%.2f".formatted(kills / (double) deaths);
    }

    public static Vec3 getLastTargetPos() {
        return lastTargetPos;
    }

    public static String getLastTargetName() {
        return lastTargetName;
    }

    @EventHandler
    private static void onJoin(GameJoinedEvent event) {
        reset();
    }

    @EventHandler
    private static void onLeave(GameLeftEvent event) {
        lastTargetId = null;
        wasDead = false;
    }

    @EventHandler
    private static void onAttack(AttackEntityEvent event) {
        if (mc.player == null || event.entity == mc.player) return;
        if (!(event.entity instanceof Player player)) return;

        lastTargetId = player.getUUID();
        lastAttackMs = System.currentTimeMillis();
        lastTargetPos = player.position();
        lastTargetName = player.getName().getString();
    }

    @EventHandler
    private static void onPacket(PacketEvent.Receive event) {
        if (mc.level == null || mc.player == null) return;
        if (!(event.packet instanceof ClientboundEntityEventPacket packet)) return;
        if (packet.getEventId() != EntityEvent.PROTECTED_FROM_DEATH) return;

        Entity entity = packet.getEntity(mc.level);
        if (!(entity instanceof Player player)) return;

        if (player == mc.player) totemPops++;
        MeteorClient.EVENT_BUS.post(TotemPopEvent.get(player.getName().getString(), player == mc.player));
    }

    @EventHandler
    private static void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        long now = System.currentTimeMillis();
        countedKills.entrySet().removeIf(entry -> now - entry.getValue() > 15000);

        boolean dead = mc.player.isDeadOrDying() || mc.player.getHealth() <= 0;
        if (dead && !wasDead) {
            deaths++;
            killStreak = 0;
        }
        wasDead = dead;

        if (lastTargetId != null && now - lastAttackMs <= 8000) {
            Player target = findPlayer(lastTargetId);
            if (target != null) {
                lastTargetPos = target.position();
                lastTargetName = target.getName().getString();
                if (target.isDeadOrDying() || target.getHealth() <= 0) {
                    recordKill(target.getUUID(), target.getName().getString(), target.position());
                }
            }
        }
    }

    private static Player findPlayer(UUID id) {
        for (Player player : mc.level.players()) {
            if (player.getUUID().equals(id)) return player;
        }
        return null;
    }

    private static void recordKill(UUID id, String name, Vec3 pos) {
        if (countedKills.putIfAbsent(id, System.currentTimeMillis()) != null) return;

        kills++;
        killStreak++;
        if (killStreak > bestKillStreak) bestKillStreak = killStreak;
        lastTargetPos = pos;
        lastTargetName = name;

        MeteorClient.EVENT_BUS.post(PlayerKillEvent.get(name, pos));
    }
}
