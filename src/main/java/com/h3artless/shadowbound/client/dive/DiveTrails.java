package com.h3artless.shadowbound.client.dive;

import com.h3artless.shadowbound.dive.ShadowDive;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class DiveTrails {
    public static final int HISTORY = 32;

    public record Sample(double x, double y, double z, long tick, double odo) {}

    private static final Map<Integer, Deque<Sample>> TRAILS = new HashMap<>();

    private DiveTrails() {}

    public static void tick(MinecraftClient client) {
        if (client.world == null) {
            TRAILS.clear();
            return;
        }
        long now = client.world.getTime();
        Set<Integer> seen = new HashSet<>();
        for (PlayerEntity p : client.world.getPlayers()) {
            if (!ShadowDive.isDiving(p)) continue;
            seen.add(p.getId());
            Deque<Sample> d = TRAILS.computeIfAbsent(p.getId(), k -> new ArrayDeque<>());
            Sample last = d.peekFirst();
            double odo = last == null ? 0 : last.odo() + Math.hypot(p.getX() - last.x(), p.getZ() - last.z());
            d.addFirst(new Sample(p.getX(), p.getY(), p.getZ(), now, odo));
            while (d.size() > HISTORY) d.removeLast();
        }
        TRAILS.keySet().removeIf(id -> !seen.contains(id));
    }

    public static Deque<Sample> get(int entityId) {
        Deque<Sample> d = TRAILS.get(entityId);
        return d == null ? new ArrayDeque<>() : d;
    }

    public static void reset() {
        TRAILS.clear();
    }
}
