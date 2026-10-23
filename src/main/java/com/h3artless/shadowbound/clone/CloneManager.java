package com.h3artless.shadowbound.clone;

import com.h3artless.shadowbound.entity.ShadowCloneEntity;
import com.h3artless.shadowbound.item.TotemOfShadownessItem;
import com.h3artless.shadowbound.registry.ModEntities;
import com.h3artless.shadowbound.spy.SpyManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CloneManager {
    private static final int RESPAWN_COOLDOWN = 20 * 30;
    private static final Map<UUID, Slot> SLOTS = new HashMap<>();

    private static final class Slot {
        UUID cloneId;
        ShadowCloneEntity entity;
        int cooldown;
    }

    private CloneManager() {}

    public static void tick(ServerPlayerEntity player) {
        Slot slot = SLOTS.computeIfAbsent(player.getUuid(), k -> new Slot());
        if (slot.cooldown > 0) slot.cooldown--;

        if (SpyManager.isSpying(player)) return;

        ShadowCloneEntity clone = find(player, slot);
        boolean wants = TotemOfShadownessItem.isHolding(player) && player.isAlive() && !player.isSpectator();

        if (!wants) {
            if (clone != null) clone.poof();
            slot.cloneId = null;
            return;
        }

        if (clone != null) {
            if (clone.getWorld() != player.getWorld() || clone.squaredDistanceTo(player) > 48 * 48) {
                clone.poof();
                slot.cloneId = null;
            } else {
                return;
            }
        }

        if (slot.cooldown == 0) {
            slot.cloneId = summon(player);
        }
    }

    private static ShadowCloneEntity find(ServerPlayerEntity player, Slot slot) {
        if (slot.cloneId == null) {
            slot.entity = null;
            return null;
        }
        ShadowCloneEntity cached = slot.entity;
        if (cached != null && cached.isAlive() && !cached.isRemoved() && slot.cloneId.equals(cached.getUuid())) return cached;
        slot.entity = null;
        for (ServerWorld world : player.getServer().getWorlds()) {
            Entity e = world.getEntity(slot.cloneId);
            if (e instanceof ShadowCloneEntity c && c.isAlive()) {
                slot.entity = c;
                return c;
            }
        }
        slot.cloneId = null;
        return null;
    }

    private static UUID summon(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        ShadowCloneEntity clone = ModEntities.SHADOW_CLONE.create(world);
        if (clone == null) return null;

        float yaw = player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        Vec3d pos = player.getPos().add(MathHelper.sin(yaw) * 1.5, 0, -MathHelper.cos(yaw) * 1.5);
        clone.refreshPositionAndAngles(pos.x, pos.y, pos.z, player.getYaw(), 0);
        clone.setup(player);
        world.spawnEntity(clone);

        world.spawnParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 1, pos.z, 30, 0.3, 0.8, 0.3, 0.05);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y + 1, pos.z, 10, 0.3, 0.8, 0.3, 0.02);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.8f, 0.6f);
        Taunts.onSpawn(clone, player);
        return clone.getUuid();
    }

    public static boolean isCurrent(PlayerEntity owner, ShadowCloneEntity clone) {
        Slot slot = SLOTS.get(owner.getUuid());
        return slot != null && clone.getUuid().equals(slot.cloneId);
    }

    public static void onCloneDied(PlayerEntity owner) {
        Slot slot = SLOTS.get(owner.getUuid());
        if (slot != null) {
            slot.cloneId = null;
            slot.cooldown = RESPAWN_COOLDOWN;
        }
    }

    public static void sacrifice(ServerPlayerEntity player) {
        Slot slot = SLOTS.computeIfAbsent(player.getUuid(), k -> new Slot());
        ShadowCloneEntity clone = find(player, slot);
        if (clone != null) {
            Taunts.onSacrifice(clone, player);
            clone.poof();
        }
        slot.cloneId = null;
        slot.cooldown = RESPAWN_COOLDOWN;
    }

    public static void forget(ServerPlayerEntity player) {
        Slot slot = SLOTS.remove(player.getUuid());
        if (slot != null) {
            ShadowCloneEntity c = find(player, slot);
            if (c != null) c.discard();
        }
    }
}
