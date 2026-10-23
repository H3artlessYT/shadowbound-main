package com.h3artless.shadowbound.dive;

import com.h3artless.shadowbound.item.TotemOfShadownessItem;
import com.h3artless.shadowbound.meter.DarknessMeter;
import com.h3artless.shadowbound.shadow.ShadowAllies;
import com.h3artless.shadowbound.spy.SpyManager;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ShadowDive {
    public static final int ENTRY_COST = 60;
    public static final int MAX_ENTER_LIGHT = 7;
    public static final int BURN_LIGHT = 10;
    private static final int BURN_GRACE = 12;

    private static final UUID SPEED_ID = UUID.fromString("7d3c6a1e-5b2f-4e8a-9c41-2f6b8d0e1a77");
    private static final EntityAttributeModifier SPEED = new EntityAttributeModifier(SPEED_ID,
            "Shadow dive", 0.65, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);

    private static final DustParticleEffect EMBER = new DustParticleEffect(new Vector3f(0.9f, 0.08f, 0.1f), 0.8f);

    private static final Map<UUID, Integer> BURN = new HashMap<>();

    private ShadowDive() {}

    public static boolean isDiving(PlayerEntity p) {
        return ((DiveHolder) p).shadowbound$isDiving();
    }

    public static void forget(ServerPlayerEntity p) {
        BURN.remove(p.getUuid());
    }

    public static int lightAt(ServerPlayerEntity p) {
        return p.getWorld().getLightLevel(BlockPos.ofFloored(p.getX(), p.getY() + 0.2, p.getZ()));
    }

    public static void toggle(ServerPlayerEntity p) {
        if (isDiving(p)) {
            exit(p, false);
        } else {
            tryEnter(p);
        }
    }

    private static void tryEnter(ServerPlayerEntity p) {
        if (!p.isAlive() || p.isSpectator() || SpyManager.isSpying(p)) return;
        if (!TotemOfShadownessItem.isHolding(p)) return;

        if (lightAt(p) > MAX_ENTER_LIGHT) {
            p.sendMessage(Text.translatable("message.shadowbound.dive.too_bright").formatted(Formatting.GRAY), true);
            return;
        }
        if (DarknessMeter.get(p) < ENTRY_COST) {
            p.sendMessage(Text.translatable("message.shadowbound.dive.no_darkness").formatted(Formatting.GRAY), true);
            return;
        }

        p.stopRiding();
        DarknessMeter.set(p, DarknessMeter.get(p) - ENTRY_COST);
        ((DiveHolder) p).shadowbound$setDiving(true);
        ((DiveHolder) p).shadowbound$setDiveLight(lightAt(p));
        BURN.remove(p.getUuid());

        EntityAttributeInstance speed = p.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(SPEED)) speed.addTemporaryModifier(SPEED);
        p.setInvisible(true);
        loseTrackOf(p, 32);

        ServerWorld w = p.getServerWorld();
        w.spawnParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 0.3, p.getZ(), 40, 0.4, 0.3, 0.4, 0.08);
        w.spawnParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 0.5, p.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
        w.spawnParticles(EMBER, p.getX(), p.getY() + 0.4, p.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
        w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_SQUID_SQUIRT, SoundCategory.PLAYERS, 1.0f, 0.5f);
        w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_SCULK_CATALYST_BLOOM, SoundCategory.PLAYERS, 0.8f, 0.6f);
    }

    public static void exit(ServerPlayerEntity p, boolean burned) {
        if (!isDiving(p)) return;
        ((DiveHolder) p).shadowbound$setDiving(false);
        BURN.remove(p.getUuid());

        EntityAttributeInstance speed = p.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SPEED_ID);
        p.setInvisible(p.hasStatusEffect(StatusEffects.INVISIBILITY));
        p.fallDistance = 0;

        ServerWorld w = p.getServerWorld();
        w.spawnParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 0.5, p.getZ(), 30, 0.3, 0.6, 0.3, 0.06);
        w.spawnParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 0.8, p.getZ(), 12, 0.3, 0.6, 0.3, 0.03);
        w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.9f, 0.7f);

        if (burned) {
            w.spawnParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 0.8, p.getZ(), 30, 0.3, 0.6, 0.3, 0.05);
            w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.8f, 0.8f);
            p.damage(p.getDamageSources().magic(), 2.0f);
            p.sendMessage(Text.translatable("message.shadowbound.dive.burned").formatted(Formatting.RED), true);
        }
    }

    public static void tick(ServerPlayerEntity p) {
        if (!isDiving(p)) return;

        if (!p.isAlive() || p.isSpectator() || !TotemOfShadownessItem.isHolding(p)) {
            exit(p, false);
            return;
        }

        int left = DarknessMeter.get(p) - 1;
        if (left <= 0) {
            DarknessMeter.set(p, 0);
            p.sendMessage(Text.translatable("message.shadowbound.dive.drained").formatted(Formatting.GRAY), true);
            exit(p, false);
            return;
        }
        DarknessMeter.set(p, left);

        int light = lightAt(p);
        ((DiveHolder) p).shadowbound$setDiveLight(light);
        int burn = BURN.getOrDefault(p.getUuid(), 0);
        burn = light >= BURN_LIGHT ? burn + 1 : Math.max(0, burn - 2);
        BURN.put(p.getUuid(), burn);
        if (burn >= BURN_GRACE) {
            exit(p, true);
            return;
        }

        p.fallDistance = 0;
        if (p.age % 10 == 0) loseTrackOf(p, 24);

        ServerWorld w = p.getServerWorld();
        boolean moving = p.getVelocity().horizontalLengthSquared() > 0.001;
        if (moving && p.age % 3 == 0) {
            w.spawnParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 0.1, p.getZ(), 1, 0.15, 0.02, 0.15, 0.0);
        }
        if (p.age % 9 == 0) {
            w.spawnParticles(EMBER, p.getX(), p.getY() + 0.2, p.getZ(), 1, 0.3, 0.1, 0.3, 0.01);
        }
        if (burn > 0 && p.age % 2 == 0) {
            w.spawnParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 0.3, p.getZ(), 2, 0.2, 0.1, 0.2, 0.02);
        }
    }

    private static void loseTrackOf(ServerPlayerEntity p, double range) {
        for (MobEntity mob : p.getServerWorld().getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(range),
                m -> m.getTarget() == p)) {
            ShadowAllies.aim(mob, null);
        }
    }
}
