package com.h3artless.shadowbound.meter;

import com.h3artless.shadowbound.dive.ShadowDive;
import com.h3artless.shadowbound.item.TotemOfShadownessItem;
import com.h3artless.shadowbound.net.ModPackets;
import com.h3artless.shadowbound.spy.SpyManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DarknessMeter {
    public static final int MAX = 1200;
    public static final int LIGHT_THRESHOLD = 13;

    private static final Map<UUID, Integer> LAST_SENT = new HashMap<>();

    private DarknessMeter() {}

    public static int get(ServerPlayerEntity p) {
        return ((DarknessHolder) p).shadowbound$getDarkness();
    }

    public static void set(ServerPlayerEntity p, int value) {
        ((DarknessHolder) p).shadowbound$setDarkness(MathHelper.clamp(value, 0, MAX));
    }

    public static void tick(ServerPlayerEntity player) {
        if (TotemOfShadownessItem.isHolding(player) && !SpyManager.isSpying(player) && !ShadowDive.isDiving(player)) {
            int light = player.getWorld().getLightLevel(BlockPos.ofFloored(player.getEyePos()));
            int v = get(player);
            if (light < LIGHT_THRESHOLD) {
                set(player, v + 1);
            } else if (player.age % 4 == 0) {
                set(player, v - 1);
            }
        }

        int now = get(player);
        Integer last = LAST_SENT.get(player.getUuid());
        if (last == null || (last != now && (player.age % 5 == 0 || now == 0 || now == MAX))) {
            sync(player);
        }
    }

    public static void sync(ServerPlayerEntity player) {
        int now = get(player);
        LAST_SENT.put(player.getUuid(), now);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(now);
        ServerPlayNetworking.send(player, ModPackets.METER_SYNC, buf);
    }

    public static void forget(ServerPlayerEntity player) {
        LAST_SENT.remove(player.getUuid());
    }

    public static void copy(ServerPlayerEntity from, ServerPlayerEntity to) {
        set(to, get(from));
        LAST_SENT.remove(to.getUuid());
    }
}
