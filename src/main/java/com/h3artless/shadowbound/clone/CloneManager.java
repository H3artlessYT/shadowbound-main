package com.h3artless.shadowbound.clone;

import com.h3artless.shadowbound.meter.DarknessMeter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.h3artless.shadowbound.cooldown.Ability;
import com.h3artless.shadowbound.cooldown.Cooldowns;
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
    public static final int RESPAWN_DARKNESS = 400;
    private static final Map<UUID, Slot> SLOTS = new HashMap<>();

    private static final class Slot {
        UUID cloneId;
        ShadowCloneEntity entity;
        boolean died;
        long reformAt;
        long deadUntil;
        boolean dove;
    }

    private CloneManager() {}

    public static void tick(ServerPlayerEntity player) {
        Slot slot = SLOTS.computeIfAbsent(player.getUuid(), k -> new Slot());

        if (SpyManager.isSpying(player)) return;

        boolean tracked = slot.cloneId != null;
        ShadowCloneEntity clone = find(player, slot);
        long now = player.getServer().getTicks();
        if (tracked && clone == null) markDead(player, slot);
        if (com.h3artless.shadowbound.dive.ShadowDive.isDiving(player)) {
            if (clone != null) {
                sink(clone);
                slot.cloneId = null;
                slot.entity = null;
                slot.dove = true;
            }
            return;
        }
        if (slot.dove) {
            slot.dove = false;
            slot.reformAt = 0;
        }
        boolean wants = TotemOfShadownessItem.isHolding(player) && player.isAlive() && !player.isSpectator();

        if (!wants) {
            if (clone != null) {
                clone.poof();
                slot.reformAt = now + com.h3artless.shadowbound.config.ShadowConfig.totemRespawnTicks();
            }
            slot.cloneId = null;
            slot.entity = null;
            return;
        }

        if (clone != null) {
            if (clone.getWorld() != player.getWorld() || clone.squaredDistanceTo(player) > 48 * 48) {
                clone.poof();
                slot.cloneId = null;
                slot.entity = null;
            } else {
                return;
            }
        }

        long dead = slot.deadUntil - now;
        if (dead > 0) {
            if (dead % 20 == 0) player.sendMessage(Text.translatable("message.shadowbound.shadow.reforming", (dead + 19) / 20).formatted(Formatting.DARK_RED), true);
            return;
        }

        if (!slot.died) {
            long wait = slot.reformAt - now;
            if (wait > 0) {
                if (wait % 20 == 0) player.sendMessage(Text.translatable("message.shadowbound.shadow.reforming", (wait + 19) / 20).formatted(Formatting.DARK_RED), true);
                return;
            }
            slot.cloneId = summon(player);
        } else if (DarknessMeter.get(player) >= RESPAWN_DARKNESS / 2) {
            DarknessMeter.set(player, DarknessMeter.get(player) - RESPAWN_DARKNESS / 2);
            slot.died = false;
            slot.cloneId = summon(player);
            player.sendMessage(Text.translatable("message.shadowbound.shadow.respawn_dark", RESPAWN_DARKNESS / 2 * 100 / DarknessMeter.MAX).formatted(Formatting.DARK_RED), true);
        } else if (now % 40 == 0) {
            player.sendMessage(Text.translatable("message.shadowbound.shadow.respawn_need", RESPAWN_DARKNESS / 2 * 100 / DarknessMeter.MAX).formatted(Formatting.DARK_RED), true);
        }
    }

    private static void markDead(ServerPlayerEntity player, Slot slot) {
        slot.cloneId = null;
        slot.entity = null;
        slot.died = true;
        slot.reformAt = 0;
        slot.deadUntil = player.getServer().getTicks() + Ability.SHADOW_RESPAWN.ticks;
        Cooldowns.start(player, Ability.SHADOW_RESPAWN);
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

    private static final net.minecraft.particle.DustParticleEffect BLOOD = new net.minecraft.particle.DustParticleEffect(new org.joml.Vector3f(0.6f, 0.02f, 0.03f), 1.3f);

    private static void sink(net.minecraft.entity.LivingEntity clone) {
        if (clone.getWorld() instanceof ServerWorld w) {
            w.spawnParticles(ParticleTypes.SQUID_INK, clone.getX(), clone.getBodyY(0.4), clone.getZ(), 35, 0.3, 0.6, 0.3, 0.04);
            w.spawnParticles(ParticleTypes.LARGE_SMOKE, clone.getX(), clone.getBodyY(0.3), clone.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
            w.spawnParticles(BLOOD, clone.getX(), clone.getBodyY(0.5), clone.getZ(), 20, 0.35, 0.6, 0.35, 0.0);
            w.playSound(null, clone.getX(), clone.getY(), clone.getZ(), SoundEvents.ENTITY_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.7f, 0.5f);
        }
        clone.discard();
    }

    public static void dismiss(ServerPlayerEntity player) {
        if (!player.isAlive() || SpyManager.isSpying(player) || com.h3artless.shadowbound.dive.ShadowDive.isDiving(player)) return;
        ServerWorld world = player.getServerWorld();
        java.util.List<net.minecraft.entity.LivingEntity> mine = new java.util.ArrayList<>(world.getEntitiesByClass(ShadowCloneEntity.class, player.getBoundingBox().expand(48),
                c -> player.getUuid().equals(c.getOwnerUuid())));
        mine.addAll(world.getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class, player.getBoundingBox().expand(48),
                e -> e instanceof com.h3artless.shadowbound.shadow.ShadowMob sm && sm.shadowbound$isShadow() && player.getUuid().equals(sm.shadowbound$getOwner())));
        if (mine.isEmpty()) {
            player.sendMessage(Text.translatable("message.shadowbound.shadow.none").formatted(Formatting.GRAY), true);
            return;
        }
        Slot slot = SLOTS.computeIfAbsent(player.getUuid(), k -> new Slot());
        Vec3d to = player.getPos().add(0, player.getHeight() * 0.6, 0);
        for (net.minecraft.entity.LivingEntity c : mine) {
            Vec3d from = c.getPos().add(0, c.getHeight() * 0.6, 0);
            for (int i = 0; i <= 10; i++) {
                Vec3d p = from.lerp(to, i / 10.0);
                world.spawnParticles(BLOOD, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
            }
            sink(c);
        }
        slot.cloneId = null;
        slot.entity = null;
        slot.died = false;
        slot.reformAt = player.getServer().getTicks() + com.h3artless.shadowbound.config.ShadowConfig.totemRespawnTicks();
        int n = mine.size();
        int duration = Math.min(200, 90 + 30 * (n - 1));
        int amp = n >= 3 ? 1 : 0;
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.REGENERATION, duration, amp, false, true, true));
        world.spawnParticles(BLOOD, to.x, to.y, to.z, 18, 0.3, 0.4, 0.3, 0.0);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 0.6f, 1.3f);
        player.sendMessage(Text.translatable("message.shadowbound.shadow.dismissed", n).formatted(Formatting.DARK_RED), true);
    }

    public static boolean isCurrent(PlayerEntity owner, ShadowCloneEntity clone) {
        Slot slot = SLOTS.get(owner.getUuid());
        return slot != null && clone.getUuid().equals(slot.cloneId);
    }

    public static void onCloneDied(PlayerEntity owner) {
        if (owner instanceof ServerPlayerEntity sp) markDead(sp, SLOTS.computeIfAbsent(sp.getUuid(), k -> new Slot()));
    }

    public static void sacrifice(ServerPlayerEntity player) {
        Slot slot = SLOTS.computeIfAbsent(player.getUuid(), k -> new Slot());
        ShadowCloneEntity clone = find(player, slot);
        if (clone != null) {
            Taunts.onSacrifice(clone, player);
            clone.poof();
        }
        markDead(player, slot);
    }

    public static void forget(ServerPlayerEntity player) {
        Slot slot = SLOTS.remove(player.getUuid());
        if (slot != null) {
            ShadowCloneEntity c = find(player, slot);
            if (c != null) c.discard();
        }
    }
}
