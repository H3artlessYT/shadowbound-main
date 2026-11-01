package com.h3artless.shadowbound.entity;

import com.h3artless.shadowbound.shadow.ShadowAllies;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;

public class SoulPylonEntity extends Entity {
    public static final int LIFETIME = 200;
    public static final int RISE = 20;
    public static final int COLLAPSE = 25;
    public static final int FIRST_BURN = RISE + 10;
    public static final int LAST_BURN = LIFETIME - COLLAPSE - 10;
    public static final double OWNER_RANGE = 32.0;

    private static final TrackedData<Optional<UUID>> OWNER = DataTracker.registerData(SoulPylonEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> SOULS = DataTracker.registerData(SoulPylonEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> LEVEL = DataTracker.registerData(SoulPylonEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> COLLAPSE_AT = DataTracker.registerData(SoulPylonEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final DustParticleEffect RED = new DustParticleEffect(new Vector3f(0.9f, 0.08f, 0.1f), 1.0f);

    public int lastBurnAge = -100;
    private int knownSouls = -1;
    private int nextBurn = FIRST_BURN;

    public SoulPylonEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public void setup(PlayerEntity owner, int souls, int level) {
        dataTracker.set(OWNER, Optional.of(owner.getUuid()));
        dataTracker.set(SOULS, souls);
        dataTracker.set(LEVEL, level);
    }

    @Override
    protected void initDataTracker() {
        dataTracker.startTracking(OWNER, Optional.empty());
        dataTracker.startTracking(SOULS, 0);
        dataTracker.startTracking(LEVEL, 1);
        dataTracker.startTracking(COLLAPSE_AT, -1);
    }

    @Nullable public UUID getOwnerUuid() { return dataTracker.get(OWNER).orElse(null); }
    public int getSouls() { return dataTracker.get(SOULS); }
    public int getLevel() { return dataTracker.get(LEVEL); }
    public int getCollapseAt() { return dataTracker.get(COLLAPSE_AT); }

    @Nullable
    public PlayerEntity getOwner() {
        UUID id = getOwnerUuid();
        return id == null ? null : getWorld().getPlayerByUuid(id);
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) {
            int souls = getSouls();
            if (knownSouls >= 0 && souls < knownSouls) lastBurnAge = age;
            knownSouls = souls;
            return;
        }

        ServerWorld world = (ServerWorld) getWorld();
        if (age == 1) {
            world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.0f, 0.6f);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.2, getZ(), 30, 0.6, 0.1, 0.6, 0.05);
        }

        int collapseAt = getCollapseAt();
        if (collapseAt < 0) {
            PlayerEntity owner = getOwner();
            if (owner == null || !owner.isAlive()) {
                startCollapse();
            } else if (owner.squaredDistanceTo(this) <= OWNER_RANGE * OWNER_RANGE) {
                if (age % 20 == 1) regen(world, owner);
                if (age >= nextBurn && getSouls() > 0) {
                    burn(world, owner);
                    int left = getSouls();
                    nextBurn = left > 0 ? age + Math.max(8, (LAST_BURN - age) / left) : Integer.MAX_VALUE;
                }
            }
            if (age % 10 == 0) world.spawnParticles(RED, getX(), getY() + 0.3, getZ(), 2, 0.5, 0.2, 0.5, 0.0);
            if (age >= LIFETIME - COLLAPSE) startCollapse();
        } else if (age >= collapseAt + COLLAPSE) {
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 20, 0.4, 0.4, 0.4, 0.03);
            discard();
        }
    }

    private void regen(ServerWorld world, PlayerEntity owner) {
        int amp = Math.max(0, getLevel() - 2);
        int left = Math.max(20, LIFETIME - age);
        give(owner, amp, left);
        for (ShadowCloneEntity clone : world.getEntitiesByClass(ShadowCloneEntity.class, getBoundingBox().expand(16),
                c -> owner.getUuid().equals(c.getOwnerUuid()))) {
            give(clone, amp, left);
        }
    }

    private static void give(LivingEntity e, int amp, int duration) {
        StatusEffectInstance cur = e.getStatusEffect(StatusEffects.REGENERATION);
        if (cur != null && cur.getAmplifier() >= amp && cur.getDuration() >= 20) return;
        e.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, amp, true, true, true));
    }

    private void startCollapse() {
        if (getCollapseAt() >= 0) return;
        dataTracker.set(COLLAPSE_AT, age);
        getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 0.8f, 0.7f);
    }

    private void burn(ServerWorld world, PlayerEntity owner) {
        int level = getLevel();
        dataTracker.set(SOULS, getSouls() - 1);

        float mult = com.h3artless.shadowbound.config.ShadowConfig.pylonHeal();
        float heal = (2f + 2f * level) * mult;
        float absorbAdd = 2f * level * mult;
        float absorbCap = (4f + 4f * level) * mult;
        owner.heal(heal);
        owner.setAbsorptionAmount(Math.min(absorbCap, Math.max(owner.getAbsorptionAmount(), 0f) + absorbAdd));

        for (ShadowCloneEntity clone : world.getEntitiesByClass(ShadowCloneEntity.class, getBoundingBox().expand(16),
                c -> owner.getUuid().equals(c.getOwnerUuid()))) {
            clone.heal(heal * 0.5f);
        }

        Vec3d top = getPos().add(0, 2.6, 0);
        Vec3d to = owner.getPos().add(0, owner.getHeight() * 0.6, 0);
        for (int i = 0; i <= 12; i++) {
            Vec3d p = top.lerp(to, i / 12.0);
            world.spawnParticles(ParticleTypes.SCULK_SOUL, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        world.spawnParticles(RED, to.x, to.y, to.z, 15, 0.3, 0.4, 0.3, 0.05);
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.PARTICLE_SOUL_ESCAPE, SoundCategory.PLAYERS, 1.5f, 0.7f);
        world.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.6f, 1.4f);
    }

    @Override
    public boolean canHit() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (getWorld().isClient || isRemoved()) return false;
        Entity attacker = source.getAttacker();
        UUID owner = getOwnerUuid();
        if (attacker == null || owner == null || owner.equals(attacker.getUuid())
                || (attacker instanceof LivingEntity l && ShadowAllies.isAllyOf(l, owner))) {
            return false;
        }
        ServerWorld world = (ServerWorld) getWorld();
        world.spawnParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 1.5, getZ(), 10 + getSouls() * 6, 0.4, 0.8, 0.4, 0.1);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, 0.5f);
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_VEX_DEATH, SoundCategory.PLAYERS, 1.0f, 0.6f);
        discard();
        return true;
    }

    @Override
    public boolean shouldRender(double distance) {
        return distance < 96 * 96;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.containsUuid("Owner")) dataTracker.set(OWNER, Optional.of(nbt.getUuid("Owner")));
        dataTracker.set(SOULS, nbt.getInt("Souls"));
        dataTracker.set(LEVEL, Math.max(1, nbt.getInt("Level")));
        dataTracker.set(COLLAPSE_AT, 0);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        UUID owner = getOwnerUuid();
        if (owner != null) nbt.putUuid("Owner", owner);
        nbt.putInt("Souls", getSouls());
        nbt.putInt("Level", getLevel());
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }
}
