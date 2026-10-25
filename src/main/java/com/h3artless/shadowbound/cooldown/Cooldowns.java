package com.h3artless.shadowbound.cooldown;

import com.h3artless.shadowbound.net.ModPackets;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class Cooldowns {
    private static final Ability[] ALL = Ability.values();

    private Cooldowns() {}

    private static int[] of(PlayerEntity p) {
        return ((CooldownHolder) p).shadowbound$cooldowns();
    }

    public static int remaining(PlayerEntity p, Ability a) {
        return of(p)[a.ordinal()];
    }

    public static float progress(PlayerEntity p, Ability a) {
        return 1f - remaining(p, a) / (float) a.ticks;
    }

    public static boolean ready(PlayerEntity p, Ability a) {
        return p.getAbilities().creativeMode || remaining(p, a) <= 0;
    }

    public static void start(ServerPlayerEntity p, Ability a) {
        if (p.getAbilities().creativeMode) return;
        of(p)[a.ordinal()] = a.ticks;
        sync(p);
    }

    public static void tick(PlayerEntity p) {
        int[] cd = of(p);
        for (int i = 0; i < cd.length; i++) {
            if (cd[i] > 0) cd[i]--;
        }
    }

    public static void deny(ServerPlayerEntity p, Ability a) {
        int secs = (remaining(p, a) + 19) / 20;
        p.sendMessage(Text.translatable("message.shadowbound.cooldown",
                Text.translatable("ability.shadowbound." + a.key()), secs).formatted(Formatting.DARK_RED), true);
    }

    public static void copy(PlayerEntity from, PlayerEntity to) {
        System.arraycopy(of(from), 0, of(to), 0, ALL.length);
    }

    public static void sync(ServerPlayerEntity p) {
        int[] cd = of(p);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(cd.length);
        for (int v : cd) buf.writeVarInt(v);
        ServerPlayNetworking.send(p, ModPackets.COOLDOWN_SYNC, buf);
    }

    public static void apply(PlayerEntity p, int[] values) {
        int[] cd = of(p);
        System.arraycopy(values, 0, cd, 0, Math.min(values.length, cd.length));
    }
}
