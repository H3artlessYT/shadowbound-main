package com.h3artless.shadowbound.spy;

import com.h3artless.shadowbound.dive.ShadowDive;
import com.h3artless.shadowbound.item.TotemOfShadownessItem;
import com.h3artless.shadowbound.meter.DarknessMeter;
import com.h3artless.shadowbound.net.ModPackets;
import com.h3artless.shadowbound.registry.ModEffects;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SpyManager {
    public static final int START_COST = 300;
    public static final int DRAIN_PER_TICK = 3;

    private static final int SETTLE = 8;

    private static final class Session {
        final UUID target;
        int settle = SETTLE;
        boolean attached;

        Session(UUID target) {
            this.target = target;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private SpyManager() {}

    public static boolean isSpying(ServerPlayerEntity p) {
        return SESSIONS.containsKey(p.getUuid());
    }

    public static void sendList(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        List<ServerPlayerEntity> others = server.getPlayerManager().getPlayerList().stream()
                .filter(p -> p != player && !isSpying(p))
                .toList();

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(DarknessMeter.get(player));
        buf.writeVarInt(others.size());
        for (ServerPlayerEntity p : others) {
            buf.writeUuid(p.getUuid());
            buf.writeString(p.getGameProfile().getName());
            buf.writeIdentifier(p.getWorld().getRegistryKey().getValue());
            buf.writeDouble(p.getWorld() == player.getWorld() ? p.distanceTo(player) : -1.0);
            buf.writeFloat(p.getHealth());
            buf.writeFloat(p.getMaxHealth());
            buf.writeBoolean(p.isSpectator());
        }
        ServerPlayNetworking.send(player, ModPackets.SPY_LIST, buf);
    }

    public static void start(ServerPlayerEntity player, UUID targetId) {
        if (isSpying(player) || !player.isAlive()) return;
        if (!TotemOfShadownessItem.isHolding(player)) return;

        if (DarknessMeter.get(player) < START_COST) {
            player.sendMessage(Text.translatable("message.shadowbound.spy.too_bright").formatted(Formatting.GRAY), true);
            return;
        }

        ServerPlayerEntity target = player.getServer().getPlayerManager().getPlayer(targetId);
        if (target == null || target == player || target.isSpectator() || isSpying(target)) {
            player.sendMessage(Text.translatable("message.shadowbound.spy.gone").formatted(Formatting.GRAY), true);
            return;
        }

        ShadowDive.exit(player, false);
        saveReturn(player);
        DarknessMeter.set(player, DarknessMeter.get(player) - START_COST / 3);
        SESSIONS.put(player.getUuid(), new Session(targetId));

        player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDERMAN_STARE, SoundCategory.PLAYERS, 0.4f, 0.5f);

        player.stopRiding();
        player.changeGameMode(GameMode.SPECTATOR);
        player.teleport(target.getServerWorld(), target.getX(), target.getY() + 1.0, target.getZ(), target.getYaw(), target.getPitch());
    }

    public static void stop(ServerPlayerEntity player, boolean notify) {
        if (SESSIONS.remove(player.getUuid()) == null) return;
        player.setCameraEntity(player);
        restore(player);
        if (notify) {
            ServerPlayNetworking.send(player, ModPackets.SPY_STOP, PacketByteBufs.empty());
            player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.4f, 0.4f);
        }
    }

    public static void onDisconnect(ServerPlayerEntity player) {
        SESSIONS.remove(player.getUuid());
    }

    public static void onJoin(ServerPlayerEntity player) {
        if (!((SpyReturnHolder) player).shadowbound$getSpyReturn().isEmpty()) {
            restore(player);
        }
    }

    public static void tick(ServerPlayerEntity player) {
        Session s = SESSIONS.get(player.getUuid());
        if (s == null) return;

        ServerPlayerEntity target = player.getServer().getPlayerManager().getPlayer(s.target);
        int left = DarknessMeter.get(player) - DRAIN_PER_TICK;
        boolean ok = target != null && target.isAlive() && !target.isSpectator()
                && TotemOfShadownessItem.isHolding(player);

        if (!ok || left <= 0) {
            DarknessMeter.set(player, Math.max(left, 0));
            stop(player, true);
            return;
        }
        DarknessMeter.set(player, left);

        if (target.getWorld() != player.getWorld()) {
            player.setCameraEntity(player);
            player.teleport(target.getServerWorld(), target.getX(), target.getY() + 1.0, target.getZ(), target.getYaw(), target.getPitch());
            s.attached = false;
            s.settle = SETTLE;
            return;
        }

        if (!s.attached) {
            if (--s.settle > 0) return;
            player.setCameraEntity(target);
            s.attached = true;

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeVarInt(target.getId());
            ServerPlayNetworking.send(player, ModPackets.SPY_START, buf);
            target.addStatusEffect(new StatusEffectInstance(ModEffects.WATCHED, 100, 0, false, false, true), player);
            return;
        }

        if (player.getCameraEntity() != target) {
            stop(player, true);
            return;
        }

        if (player.age % 20 == 0) {
            target.addStatusEffect(new StatusEffectInstance(ModEffects.WATCHED, 100, 0, false, false, true), player);
        }
    }

    private static void saveReturn(ServerPlayerEntity player) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Dim", player.getWorld().getRegistryKey().getValue().toString());
        nbt.putDouble("X", player.getX());
        nbt.putDouble("Y", player.getY());
        nbt.putDouble("Z", player.getZ());
        nbt.putFloat("Yaw", player.getYaw());
        nbt.putFloat("Pitch", player.getPitch());
        nbt.putInt("Mode", player.interactionManager.getGameMode().getId());
        ((SpyReturnHolder) player).shadowbound$setSpyReturn(nbt);
    }

    private static void restore(ServerPlayerEntity player) {
        SpyReturnHolder holder = (SpyReturnHolder) player;
        NbtCompound nbt = holder.shadowbound$getSpyReturn();
        if (nbt.isEmpty()) return;
        holder.shadowbound$setSpyReturn(new NbtCompound());

        MinecraftServer server = player.getServer();
        ServerWorld world = null;
        Identifier dim = Identifier.tryParse(nbt.getString("Dim"));
        if (dim != null) world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, dim));
        if (world == null) world = server.getOverworld();

        player.teleport(world, nbt.getDouble("X"), nbt.getDouble("Y"), nbt.getDouble("Z"), nbt.getFloat("Yaw"), nbt.getFloat("Pitch"));
        player.setVelocity(0, 0, 0);
        player.fallDistance = 0;
        player.changeGameMode(GameMode.byId(nbt.getInt("Mode")));
    }
}
