package com.h3artless.shadowbound.shadow;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.Hand;

import java.util.EnumSet;

public class ShadowStrikeGoal extends Goal {
    private static final float DAMAGE = 3.0f;
    private final PathAwareEntity mob;
    private int cooldown;
    private int repath;

    public ShadowStrikeGoal(PathAwareEntity mob) {
        this.mob = mob;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity t = mob.getTarget();
        return t != null && t.isAlive();
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity t = mob.getTarget();
        if (t == null) return;
        mob.getLookControl().lookAt(t, 30f, 30f);

        if (--repath <= 0) {
            repath = 8;
            mob.getNavigation().startMovingTo(t, 1.25);
        }
        if (cooldown > 0) cooldown--;

        double reach = mob.getWidth() * 2.0f + t.getWidth();
        if (cooldown == 0 && mob.squaredDistanceTo(t) <= reach * reach) {
            cooldown = 20;
            mob.swingHand(Hand.MAIN_HAND);
            t.damage(mob.getDamageSources().mobAttack(mob), DAMAGE);
        }
    }
}
