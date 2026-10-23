package com.h3artless.shadowbound.item;

import com.h3artless.shadowbound.clone.CloneManager;
import com.h3artless.shadowbound.meter.DarknessMeter;
import com.h3artless.shadowbound.registry.ModItems;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;

public final class ShadowTotem {
    public static final int RECHARGE = 20 * 60 * 5;

    private ShadowTotem() {}

    public static boolean tryRescue(ServerPlayerEntity player, DamageSource source) {
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;

        ItemStack totem = null;
        for (Hand hand : Hand.values()) {
            ItemStack s = player.getStackInHand(hand);
            if (s.isOf(ModItems.TOTEM_OF_SHADOWNESS)) {
                totem = s;
                break;
            }
        }
        if (totem == null || player.getItemCooldownManager().isCoolingDown(ModItems.TOTEM_OF_SHADOWNESS)) return false;

        player.incrementStat(Stats.USED.getOrCreateStat(ModItems.TOTEM_OF_SHADOWNESS));
        Criteria.USED_TOTEM.trigger(player, totem);

        player.setHealth(1.0f);
        player.clearStatusEffects();
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 100, 0, false, false, true));

        player.getItemCooldownManager().set(ModItems.TOTEM_OF_SHADOWNESS, RECHARGE);
        DarknessMeter.set(player, 0);
        CloneManager.sacrifice(player);

        ServerWorld world = player.getServerWorld();
        world.sendEntityStatus(player, EntityStatuses.USE_TOTEM_OF_UNDYING);
        world.spawnParticles(ParticleTypes.SQUID_INK, player.getX(), player.getBodyY(0.5), player.getZ(), 60, 0.5, 0.8, 0.5, 0.15);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getBodyY(0.5), player.getZ(), 25, 0.4, 0.6, 0.4, 0.08);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.6f, 1.6f);

        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(5.0),
                e -> e != player && e.isAlive())) {
            double dx = e.getX() - player.getX(), dz = e.getZ() - player.getZ();
            double len = Math.max(0.3, Math.sqrt(dx * dx + dz * dz));
            e.takeKnockback(1.2, -dx / len, -dz / len);
            if (e instanceof PlayerEntity other) {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0));
            }
        }
        return true;
    }
}
