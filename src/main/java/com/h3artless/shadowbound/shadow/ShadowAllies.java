package com.h3artless.shadowbound.shadow;

import com.h3artless.shadowbound.entity.ShadowCloneEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class ShadowAllies {
    private ShadowAllies() {}

    @Nullable
    public static UUID sideOf(Entity e) {
        if (e instanceof PlayerEntity p) return p.getUuid();
        if (e instanceof ShadowCloneEntity clone) return clone.getOwnerUuid();
        if (e instanceof ShadowMob s && s.shadowbound$isShadow()) return s.shadowbound$getOwner();
        return null;
    }

    public static boolean sameSide(Entity a, Entity b) {
        if (a == b) return false;
        UUID sa = sideOf(a);
        return sa != null && sa.equals(sideOf(b));
    }

    public static boolean isAllyOf(Entity e, UUID owner) {
        return owner.equals(sideOf(e));
    }

    @Nullable
    public static LivingEntity pickTarget(MobEntity self, PlayerEntity owner) {
        UUID id = owner.getUuid();

        LivingEntity attacker = owner.getAttacker();
        if (valid(attacker, id)) return attacker;

        LivingEntity hit = owner.getAttacking();
        if (valid(hit, id) && owner.age - owner.getLastAttackTime() < 200) return hit;

        LivingEntity current = self.getTarget();
        if (valid(current, id)) return current;

        Box area = self.getBoundingBox().expand(12.0, 4.0, 12.0);
        List<HostileEntity> hostiles = self.getWorld().getEntitiesByClass(HostileEntity.class, area, h -> valid(h, id));
        if (hostiles.isEmpty()) return null;
        if (hostiles.size() > 1) hostiles.sort(Comparator.comparingDouble(self::squaredDistanceTo));
        int checks = Math.min(hostiles.size(), 4);
        for (int i = 0; i < checks; i++) {
            HostileEntity h = hostiles.get(i);
            if (self.canSee(h)) return h;
        }
        return null;
    }

    private static boolean valid(@Nullable LivingEntity e, UUID owner) {
        return e != null && e.isAlive() && !isAllyOf(e, owner)
                && !(e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator()));
    }

    public static void aim(MobEntity mob, @Nullable LivingEntity target) {
        if (target == mob.getTarget()) return;
        mob.setTarget(target);
        if (target != null) {
            mob.getBrain().remember(MemoryModuleType.ATTACK_TARGET, target);
        } else {
            mob.getBrain().forget(MemoryModuleType.ATTACK_TARGET);
        }
    }
}
