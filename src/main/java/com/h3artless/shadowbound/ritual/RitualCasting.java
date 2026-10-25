package com.h3artless.shadowbound.ritual;

import com.h3artless.shadowbound.entity.GrimoireEntity;
import com.h3artless.shadowbound.entity.RitualEntity;
import com.h3artless.shadowbound.registry.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class RitualCasting {
    private static final double REACH_SQ = 8 * 8;

    private RitualCasting() {}

    public static void cast(ServerPlayerEntity player, int bookId, List<String> drawn) {
        if (drawn.size() != ShadowRitual.STEPS) return;
        ServerWorld world = player.getServerWorld();
        Entity e = world.getEntityById(bookId);
        if (!(e instanceof GrimoireEntity book) || !book.isAlive() || player.squaredDistanceTo(book) > REACH_SQ) return;

        ShadowRitual ritual = ShadowRitual.bySteps(drawn);
        if (ritual == null) {
            fail(player, book, "message.shadowbound.cast.unknown");
            return;
        }
        if (!world.isNight()) {
            fail(player, book, "message.shadowbound.cast.day");
            return;
        }
        if (!world.getEntitiesByClass(RitualEntity.class, book.getBoundingBox().expand(6), r -> true).isEmpty()) {
            fail(player, book, "message.shadowbound.cast.busy");
            return;
        }
        if (!player.isCreative()) {
            if (!carries(player.getInventory(), ritual)) {
                fail(player, book, "message.shadowbound.cast.offerings");
                return;
            }
            take(player.getInventory(), ritual);
        }

        Vec3d at = book.getPos();
        RitualEntity rite = ModEntities.RITUAL.create(world);
        if (rite == null) return;
        rite.setRitual(ritual);
        rite.refreshPositionAndAngles(at.x, at.y, at.z, 0, 0);
        world.spawnEntity(rite);
        book.returnTo(player, true);
    }

    private static void fail(ServerPlayerEntity player, GrimoireEntity book, String key) {
        player.sendMessage(Text.translatable(key).formatted(Formatting.DARK_RED), true);
        book.fizzle();
    }

    private static boolean usable(int slot) {
        return slot < PlayerInventory.MAIN_SIZE || slot == PlayerInventory.OFF_HAND_SLOT;
    }

    private static int count(PlayerInventory inv, Item item) {
        int n = 0;
        for (int i = 0; i < inv.size(); i++) {
            if (!usable(i)) continue;
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) n += s.getCount();
        }
        return n;
    }

    private static boolean carries(PlayerInventory inv, ShadowRitual ritual) {
        for (ShadowRitual.Part part : ritual.parts) {
            if (count(inv, part.item()) < part.count()) return false;
        }
        return true;
    }

    private static void take(PlayerInventory inv, ShadowRitual ritual) {
        for (ShadowRitual.Part part : ritual.parts) {
            int need = part.count();
            for (int i = 0; i < inv.size() && need > 0; i++) {
                if (!usable(i)) continue;
                ItemStack s = inv.getStack(i);
                if (!s.isOf(part.item())) continue;
                int t = Math.min(need, s.getCount());
                s.decrement(t);
                need -= t;
            }
        }
        inv.markDirty();
    }
}
