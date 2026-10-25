package com.h3artless.shadowbound.ritual;

import com.h3artless.shadowbound.net.ModPackets;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class CastPoses {
    private static final Set<UUID> CASTING = new HashSet<>();

    private CastPoses() {}

    public static void set(ServerPlayerEntity player, boolean on) {
        boolean changed = on ? CASTING.add(player.getUuid()) : CASTING.remove(player.getUuid());
        if (!changed) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(player.getId());
        buf.writeBoolean(on);
        for (ServerPlayerEntity p : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(p, ModPackets.CAST_POSE_SYNC, PacketByteBufs.copy(buf));
        }
    }

    public static void forget(ServerPlayerEntity player) {
        set(player, false);
    }
}
