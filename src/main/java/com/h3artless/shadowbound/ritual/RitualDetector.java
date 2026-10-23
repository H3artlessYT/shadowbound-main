package com.h3artless.shadowbound.ritual;

import com.h3artless.shadowbound.entity.RitualEntity;
import com.h3artless.shadowbound.registry.ModEntities;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class RitualDetector {
    private static final double GATHER = 1.75;
    private static final ShadowRitual[] RITUALS = ShadowRitual.values();

    private RitualDetector() {}

    public static void check(ServerWorld world, ItemEntity anchor) {
        if (!world.isNight()) return;
        ItemStack stack = anchor.getStack();
        for (ShadowRitual ritual : RITUALS) {
            if (stack.isOf(ritual.anchor)) {
                tryRitual(world, anchor, ritual);
                return;
            }
        }
    }

    private static void tryRitual(ServerWorld world, ItemEntity anchor, ShadowRitual ritual) {
        if (!world.isNight()) return;

        Box area = anchor.getBoundingBox().expand(GATHER, 1.0, GATHER);
        if (!world.getEntitiesByClass(RitualEntity.class, area.expand(3), e -> true).isEmpty()) return;

        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, area, e -> e.isAlive() && !e.getStack().isEmpty());
        for (ShadowRitual.Part part : ritual.parts) {
            int have = 0;
            for (ItemEntity e : items) {
                if (e.getStack().isOf(part.item())) have += e.getStack().getCount();
            }
            if (have < part.count()) return;
        }

        for (ShadowRitual.Part part : ritual.parts) {
            int need = part.count();
            for (ItemEntity e : items) {
                if (need <= 0) break;
                ItemStack s = e.getStack();
                if (!s.isOf(part.item())) continue;
                int take = Math.min(need, s.getCount());
                s.decrement(take);
                need -= take;
                if (s.isEmpty()) e.discard();
                else e.setStack(s);
            }
        }

        Vec3d at = Vec3d.ofBottomCenter(anchor.getBlockPos());
        RitualEntity ritualEntity = ModEntities.RITUAL.create(world);
        if (ritualEntity == null) return;
        ritualEntity.setRitual(ritual);
        ritualEntity.refreshPositionAndAngles(at.x, anchor.getY(), at.z, 0, 0);
        world.spawnEntity(ritualEntity);
    }
}
