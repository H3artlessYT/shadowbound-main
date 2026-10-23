package com.h3artless.shadowbound.entity;

import com.h3artless.shadowbound.ritual.RitualTimeline;
import com.h3artless.shadowbound.ritual.RitualTimeline.Arc;
import com.h3artless.shadowbound.ritual.ShadowRitual;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.joml.Vector3f;

import static com.h3artless.shadowbound.ritual.RitualTimeline.*;

public class RitualEntity extends Entity {
    private static final DustParticleEffect EMBER = new DustParticleEffect(new Vector3f(0.9f, 0.08f, 0.1f), 0.9f);

    private static final TrackedData<Integer> RITUAL = DataTracker.registerData(RitualEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TICKS = DataTracker.registerData(RitualEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public RitualEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(RITUAL, 0);
        this.dataTracker.startTracking(TICKS, 0);
    }

    public void setRitual(ShadowRitual ritual) {
        this.dataTracker.set(RITUAL, ritual.ordinal());
    }

    public ShadowRitual getRitual() {
        return ShadowRitual.byId(this.dataTracker.get(RITUAL));
    }

    public int getTicks() {
        return this.dataTracker.get(TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(getWorld() instanceof ServerWorld world)) return;

        int t = getTicks() + 1;
        this.dataTracker.set(TICKS, t);
        script(world, t);

        if (t >= DURATION) {
            ItemEntity drop = new ItemEntity(world, getX(), getY() + 1.4, getZ(), getRitual().result(), 0, 0.05, 0);
            drop.setPickupDelay(10);
            drop.setGlowing(true);
            world.spawnEntity(drop);
            discard();
        }
    }

    private void script(ServerWorld world, int t) {
        double x = getX(), y = getY(), z = getZ();

        if (t == 1) {
            sound(world, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.5f, 0.5f);
            sound(world, SoundEvents.BLOCK_SCULK_CATALYST_BLOOM, 1.2f, 0.5f);
        }

        for (int k = 0; k < 5; k++) {
            if (t == nodeIgnite(k)) {
                sound(world, SoundEvents.ITEM_FIRECHARGE_USE, 0.9f, 0.6f + k * 0.05f);
                sound(world, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5f, 0.6f);
            }
        }

        if (t > IGNITE_END && t < HUSH) {
            int every = t < GATHER_END ? 80 : 40;
            if (t % every == 0) sound(world, SoundEvents.BLOCK_BEACON_AMBIENT, 1.2f, 0.5f);
        }

        if (t == IGNITE_END) sound(world, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.5f);

        for (int i = 0; i < 8; i++) {
            if (t == glyphBorn(i)) sound(world, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.5f + i * 0.04f);
        }

        int n = getRitual().offeringCount();
        for (int i = 0; i < n; i++) {
            int end = Math.round(dissolveStart(i, n)) + DISSOLVE_TIME;
            if (t == end - DISSOLVE_TIME) sound(world, SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, 0.6f, 0.6f);
            if (t == end) sound(world, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.5f);
        }

        Arc arc = arcAt(t);
        if (arc != null && arc.age() < 1f) {
            sound(world, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.25f, 1.6f + random.nextFloat() * 0.3f);
            sound(world, SoundEvents.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 0.5f);
        }

        if (t > GATHER_END && t < HUSH) {
            int every = t < CRESC_END ? 24 : 12;
            if (t % every == 0) sound(world, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.0f + (t - GATHER_END) / 600f, 0.7f);
        }

        if (t == CRESC_END) {
            sound(world, SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK, 2.0f, 0.6f);
            for (ServerPlayerEntity p : world.getPlayers(p -> p.squaredDistanceTo(this) < 24 * 24)) {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, HUSH - CRESC_END + 30, 0, false, false));
            }
        }
        if (t == HUSH - 30) sound(world, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 2.0f, 0.7f);

        if (t == REVEAL) {
            sound(world, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 2.5f, 0.7f);
            sound(world, SoundEvents.ITEM_TOTEM_USE, 0.7f, 0.5f);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.3, z, 60, 2.5, 0.1, 2.5, 0.2);
            world.spawnParticles(EMBER, x, y + ORB_Y, z, 50, 0.4, 0.4, 0.4, 0.4);
            for (Entity e : world.getOtherEntities(this, new Box(getBlockPos()).expand(5), e -> e instanceof ServerPlayerEntity)) {
                double dx = e.getX() - x, dz = e.getZ() - z;
                double len = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
                e.addVelocity(dx / len * 0.6, 0.3, dz / len * 0.6);
                e.velocityModified = true;
            }
        }

        if (t > IGNITE_END && t < HUSH && t % 10 == 0) {
            world.spawnParticles(ParticleTypes.ASH, x, y + 1, z, 4, RADIUS * 0.5, 0.6, RADIUS * 0.5, 0.0);
        }
    }

    private void sound(ServerWorld world, SoundEvent s, float vol, float pitch) {
        world.playSound(null, getX(), getY(), getZ(), s, SoundCategory.BLOCKS, vol, pitch);
    }

    @Override
    public boolean shouldRender(double distance) {
        return distance < 128 * 128;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.dataTracker.set(RITUAL, nbt.getInt("Ritual"));
        this.dataTracker.set(TICKS, nbt.getInt("Ticks"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Ritual", this.dataTracker.get(RITUAL));
        nbt.putInt("Ticks", getTicks());
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }
}
