package com.h3artless.shadowbound.shadow;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;
import java.util.function.Supplier;

public class ShadowFollowOwnerGoal extends Goal {
    private final MobEntity mob;
    private final Supplier<UUID> owner;
    private final double speed;
    private final float startDist;
    private final float stopDist;
    @Nullable
    private PlayerEntity cached;
    private int repath;

    public ShadowFollowOwnerGoal(MobEntity mob, Supplier<UUID> owner, double speed, float startDist, float stopDist) {
        this.mob = mob;
        this.owner = owner;
        this.speed = speed;
        this.startDist = startDist;
        this.stopDist = stopDist;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Nullable
    private PlayerEntity owner() {
        UUID id = owner.get();
        return id == null ? null : mob.getWorld().getPlayerByUuid(id);
    }

    @Override
    public boolean canStart() {
        if (mob.getTarget() != null) return false;
        PlayerEntity p = owner();
        if (p == null || p.isSpectator()) return false;
        if (mob.squaredDistanceTo(p) < startDist * startDist) return false;
        cached = p;
        return true;
    }

    @Override
    public boolean shouldContinue() {
        return cached != null && mob.getTarget() == null && !mob.getNavigation().isIdle()
                && mob.squaredDistanceTo(cached) > stopDist * stopDist;
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        cached = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (cached == null) return;
        mob.getLookControl().lookAt(cached, 10f, mob.getMaxLookPitchChange());
        if (--repath > 0) return;
        repath = 10;

        if (mob.squaredDistanceTo(cached) > 24 * 24) {
            teleportNear(cached);
        } else {
            mob.getNavigation().startMovingTo(cached, speed);
        }
    }

    private void teleportNear(PlayerEntity p) {
        BlockPos base = p.getBlockPos();
        for (int i = 0; i < 10; i++) {
            int dx = MathHelper.nextInt(mob.getRandom(), -3, 3);
            int dz = MathHelper.nextInt(mob.getRandom(), -3, 3);
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue;
            BlockPos pos = base.add(dx, 0, dz);
            if (mob.getWorld().isAir(pos) && mob.getWorld().isAir(pos.up())
                    && mob.getWorld().getBlockState(pos.down()).isSolidBlock(mob.getWorld(), pos.down())) {
                mob.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, mob.getYaw(), mob.getPitch());
                mob.getNavigation().stop();
                return;
            }
        }
    }
}
