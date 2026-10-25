package com.h3artless.shadowbound.client.cast;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.HashSet;
import java.util.Set;

public final class ClientCastPoses {
    private static final Set<Integer> POINTING = new HashSet<>();

    private ClientCastPoses() {}

    public static void set(int id, boolean on) {
        if (on) POINTING.add(id);
        else POINTING.remove(id);
    }

    public static void clear() {
        POINTING.clear();
    }

    public static boolean isPointing(Entity e) {
        if (e == MinecraftClient.getInstance().player) return AirCasting.active();
        return POINTING.contains(e.getId());
    }
}
