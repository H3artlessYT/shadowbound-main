package com.h3artless.shadowbound.clone;

import com.h3artless.shadowbound.entity.ShadowCloneEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

import java.util.List;

public final class Taunts {
    private static final int PLAYER_LINES = 18;
    private static final int OWNER_LINES = 8;
    private static final int MOB_LINES = 10;
    private static final int SPAWN_LINES = 5;
    private static final int KILL_LINES = 6;
    private static final int HURT_LINES = 5;
    private static final int DEATH_LINES = 4;
    private static final int SACRIFICE_LINES = 3;

    private static final double HEAR_RANGE = 32.0;

    private Taunts() {}

    public static void mouthOff(ShadowCloneEntity clone, PlayerEntity owner) {
        if (!(clone.getWorld() instanceof ServerWorld world)) return;
        Random r = clone.getRandom();

        List<ServerPlayerEntity> others = world.getPlayers(p -> p != owner && !p.isSpectator()
                && p.squaredDistanceTo(clone) < 20 * 20);
        if (!others.isEmpty()) {
            PlayerEntity victim = others.get(r.nextInt(others.size()));
            say(clone, owner, line("player", r, PLAYER_LINES, victim.getName()));
            return;
        }

        List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, clone.getBoundingBox().expand(16),
                m -> m != clone && m.isAlive() && !(m instanceof ShadowCloneEntity));
        if (!mobs.isEmpty() && r.nextBoolean()) {
            MobEntity victim = mobs.get(r.nextInt(mobs.size()));
            say(clone, owner, line("mob", r, MOB_LINES, victim.getDisplayName()));
            return;
        }

        say(clone, owner, line("owner", r, OWNER_LINES, owner.getName()));
    }

    public static void onSpawn(ShadowCloneEntity clone, PlayerEntity owner) {
        if (clone.getRandom().nextInt(4) != 0) return;
        say(clone, owner, line("spawn", clone.getRandom(), SPAWN_LINES, owner.getName()));
    }

    public static void onKill(ShadowCloneEntity clone, PlayerEntity owner, LivingEntity victim) {
        if (clone.getRandom().nextInt(15) != 0) return;
        say(clone, owner, line("kill", clone.getRandom(), KILL_LINES, victim.getDisplayName()));
    }

    public static void onHurt(ShadowCloneEntity clone, PlayerEntity owner) {
        say(clone, owner, line("hurt", clone.getRandom(), HURT_LINES, owner.getName()));
    }

    public static void onSacrifice(ShadowCloneEntity clone, PlayerEntity owner) {
        say(clone, owner, line("sacrifice", clone.getRandom(), SACRIFICE_LINES, owner.getName()));
    }

    public static void onDeath(ShadowCloneEntity clone, PlayerEntity owner) {
        say(clone, owner, line("death", clone.getRandom(), DEATH_LINES, owner.getName()));
    }

    private static MutableText line(String kind, Random r, int count, Text who) {
        return Text.translatable("chat.shadowbound.clone." + kind + "." + r.nextInt(count), who);
    }

    private static void say(ShadowCloneEntity clone, PlayerEntity owner, MutableText body) {
        if (!(clone.getWorld() instanceof ServerWorld world)) return;
        Text msg = Text.translatable("chat.shadowbound.clone.prefix", owner.getName())
                .formatted(Formatting.DARK_RED)
                .append(body.formatted(Formatting.GRAY, Formatting.ITALIC));

        for (ServerPlayerEntity p : world.getPlayers()) {
            if (p.squaredDistanceTo(clone) < HEAR_RANGE * HEAR_RANGE) {
                p.sendMessage(msg, false);
            }
        }
    }
}