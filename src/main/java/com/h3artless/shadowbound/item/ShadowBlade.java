package com.h3artless.shadowbound.item;

import com.h3artless.shadowbound.cooldown.Ability;
import com.h3artless.shadowbound.cooldown.Cooldowns;
import com.h3artless.shadowbound.entity.ShadowCloneEntity;
import com.h3artless.shadowbound.entity.SoulPylonEntity;
import com.h3artless.shadowbound.registry.ModEnchantments;
import com.h3artless.shadowbound.registry.ModEntities;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import com.h3artless.shadowbound.shadow.ShadowMob;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public interface ShadowBlade {
    int MAX_SHADOWS = 5;
    float SHADOW_MAX_HEALTH = 10.0f;
    int SHADOW_LIFETIME = 20 * 60;
    double BONUS_REACH = 0.5;
    String NBT_KEY = "Shadows";
    int BAR_COLOR = 0x8a3cff;

    static boolean isBlade(ItemStack stack) {
        return stack.getItem() instanceof ShadowBlade;
    }

    static NbtList shadows(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return new NbtList();
        return nbt.getList(NBT_KEY, NbtElement.COMPOUND_TYPE);
    }

    static int count(ItemStack stack) {
        return shadows(stack).size();
    }

    static void onKill(ServerWorld world, ServerPlayerEntity player, LivingEntity killed) {
        ItemStack stack = player.getMainHandStack();
        if (!isBlade(stack) || !canCapture(killed)) return;
        if (ShadowPouchItem.tryStore(world, player, killed)) return;

        NbtList list = shadows(stack);
        if (list.size() >= MAX_SHADOWS) return;

        NbtCompound entry = new NbtCompound();
        entry.putString("id", Registries.ENTITY_TYPE.getId(killed.getType()).toString());
        if (killed.hasCustomName()) {
            entry.putString("name", Text.Serializer.toJson(killed.getCustomName()));
        }
        list.add(entry);
        stack.getOrCreateNbt().put(NBT_KEY, list);

        Vec3d from = killed.getPos().add(0, killed.getHeight() * 0.5, 0);
        Vec3d to = player.getPos().add(0, player.getHeight() * 0.6, 0);
        for (int i = 0; i <= 16; i++) {
            Vec3d p = from.lerp(to, i / 16.0);
            world.spawnParticles(ParticleTypes.SCULK_SOUL, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.01);
        }
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, from.x, from.y, from.z, 12, 0.3, 0.4, 0.3, 0.02);
        world.playSound(null, killed.getX(), killed.getY(), killed.getZ(),
                SoundEvents.BLOCK_SCULK_CATALYST_BLOOM, SoundCategory.PLAYERS, 1.0f, 0.6f);
        if (list.size() == MAX_SHADOWS) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.8f, 0.5f);
        }
    }

    private static TypedActionResult<ItemStack> raisePylon(World world, PlayerEntity user, Hand hand, ItemStack stack, int souls, int level) {
        if (world instanceof ServerWorld server) {
            HitResult hit = user.raycast(6.0, 0f, false);
            Vec3d at;
            if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
                at = Vec3d.ofBottomCenter(bh.getBlockPos().offset(bh.getSide()));
            } else {
                Vec3d look = user.getRotationVector().multiply(1, 0, 1).normalize().multiply(2.0);
                at = user.getPos().add(look);
            }

            SoulPylonEntity pylon = ModEntities.SOUL_PYLON.create(server);
            if (pylon == null) return TypedActionResult.pass(stack);
            pylon.refreshPositionAndAngles(at.x, at.y, at.z, 0f, 0f);
            pylon.setup(user, souls, level);
            server.spawnEntity(pylon);

            stack.removeSubNbt(NBT_KEY);
            if (user instanceof ServerPlayerEntity sp) Cooldowns.start(sp, Ability.PYLON);
        }
        user.swingHand(hand);
        return TypedActionResult.success(stack, world.isClient());
    }

    static boolean canCapture(LivingEntity e) {
        if (!(e instanceof MobEntity)) return false;
        if (e instanceof EnderDragonEntity || e instanceof WitherEntity || e instanceof WardenEntity) return false;
        if (e instanceof ShadowCloneEntity) return false;
        if (!com.h3artless.shadowbound.config.ShadowConfig.mayCast(e.getType())) return false;
        return !(e instanceof ShadowMob s && s.shadowbound$isShadow());
    }

    static TypedActionResult<ItemStack> release(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        NbtList list = shadows(stack);
        if (list.isEmpty()) return TypedActionResult.pass(stack);

        int pylon = EnchantmentHelper.getLevel(ModEnchantments.SOUL_PYLON, stack);
        Ability ability = pylon > 0 ? Ability.PYLON : Ability.SHADOWS;
        if (!Cooldowns.ready(user, ability)) {
            if (user instanceof ServerPlayerEntity sp) Cooldowns.deny(sp, ability);
            return TypedActionResult.fail(stack);
        }
        if (pylon > 0) {
            return raisePylon(world, user, hand, stack, list.size(), pylon);
        }

        if (world instanceof ServerWorld server) {
            float baseYaw = user.getYaw();
            int n = list.size();
            for (int i = 0; i < n; i++) {
                NbtCompound entry = list.getCompound(i);
                float spread = n == 1 ? 0 : (i / (float) (n - 1) - 0.5f) * 120f;
                float yaw = (baseYaw + spread) * MathHelper.RADIANS_PER_DEGREE;
                Vec3d pos = user.getPos().add(-MathHelper.sin(yaw) * 2.2, 0.1, MathHelper.cos(yaw) * 2.2);
                spawnShadow(server, user, entry, pos, baseYaw + spread);
            }
            stack.removeSubNbt(NBT_KEY);
            if (user instanceof ServerPlayerEntity sp) Cooldowns.start(sp, Ability.SHADOWS);

            server.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.0f, 0.5f);
            server.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 0.6f, 1.4f);
        }
        user.swingHand(hand);
        return TypedActionResult.success(stack, world.isClient());
    }

    private static void spawnShadow(ServerWorld world, PlayerEntity owner, NbtCompound entry, Vec3d pos, float yaw) {
        EntityType<?> type = EntityType.get(entry.getString("id")).orElse(null);
        if (type == null) return;
        Entity created = type.create(world);
        if (!(created instanceof MobEntity mob)) {
            if (created != null) created.discard();
            return;
        }

        mob.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0);
        mob.initialize(world, world.getLocalDifficulty(mob.getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
        if (entry.contains("name")) {
            Text name = Text.Serializer.fromJson(entry.getString("name"));
            if (name != null) mob.setCustomName(name);
        }

        EntityAttributeInstance hp = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (hp != null) {
            hp.setBaseValue(SHADOW_MAX_HEALTH);
        }
        mob.setHealth(mob.getMaxHealth());
        mob.setCanPickUpLoot(false);

        ((ShadowMob) mob).shadowbound$bind(owner.getUuid(), SHADOW_LIFETIME);
        world.spawnEntityAndPassengers(mob);

        world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y + 0.6, pos.z, 18, 0.35, 0.6, 0.35, 0.03);
        world.spawnParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 0.2, pos.z, 10, 0.4, 0.1, 0.4, 0.02);
    }

    static void tooltip(ItemStack stack, List<Text> tooltip) {
        NbtList list = shadows(stack);
        tooltip.add(Text.translatable("tooltip.shadowbound.blade.count", list.size(), MAX_SHADOWS)
                .formatted(Formatting.DARK_RED));
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            Text name = displayName(entry);
            tooltip.add(Text.literal(" ◆ ").formatted(Formatting.DARK_GRAY)
                    .append(name.copy().formatted(Formatting.GRAY)));
        }
        if (list.isEmpty()) {
            tooltip.add(Text.translatable("tooltip.shadowbound.blade.empty").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        } else {
            if (EnchantmentHelper.getLevel(ModEnchantments.SOUL_PYLON, stack) > 0) {
                tooltip.add(Text.translatable("tooltip.shadowbound.blade.pylon").formatted(Formatting.DARK_RED, Formatting.ITALIC));
            } else {
                tooltip.add(Text.translatable("tooltip.shadowbound.blade.release").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
            }
        }
    }

    private static Text displayName(NbtCompound entry) {
        if (entry.contains("name")) {
            Text t = Text.Serializer.fromJson(entry.getString("name"));
            if (t != null) return t;
        }
        Identifier id = Identifier.tryParse(entry.getString("id"));
        if (id == null) return Text.literal("???");
        return Registries.ENTITY_TYPE.getOrEmpty(id).map(EntityType::getName).orElse(Text.literal(id.toString()));
    }

    static int barStep(ItemStack stack) {
        return Math.round(13.0f * count(stack) / MAX_SHADOWS);
    }
}
