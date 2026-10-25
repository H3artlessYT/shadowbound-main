package com.h3artless.shadowbound.ritual;

import com.h3artless.shadowbound.registry.ModEnchantments;
import com.h3artless.shadowbound.registry.ModItems;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public enum ShadowRitual {
    CLEAVER(0xff5a3a, new String[]{"qwqwqwq", "wwaqqwqqa", "edeqaqe"},
            List.of(new Part(Items.NETHERITE_AXE, 1), new Part(Items.NETHERITE_INGOT, 1), new Part(Items.GOLD_BLOCK, 1))) {
        @Override public ItemStack result() { return new ItemStack(ModItems.SHADOW_CLEAVER); }
    },
    LONGSWORD(0xff2a3c, new String[]{"wwwwdd", "qaqwqaq", "wdwawdw"},
            List.of(new Part(Items.NETHERITE_SWORD, 1), new Part(Items.NETHERITE_INGOT, 1), new Part(Items.DIAMOND_BLOCK, 1))) {
        @Override public ItemStack result() { return new ItemStack(ModItems.SHADOW_LONGSWORD); }
    },
    TOTEM(0xd0102c, new String[]{"eeeeeqw", "aqaaqa", "qqwqqwqq"},
            List.of(new Part(Items.TOTEM_OF_UNDYING, 1), new Part(Items.DIAMOND_BLOCK, 2), new Part(Items.BLAZE_ROD, 10))) {
        @Override public ItemStack result() { return new ItemStack(ModItems.TOTEM_OF_SHADOWNESS); }
    },
    SOUL_PYLON(0xb01020, new String[]{"dadadad", "dwwdaawwd", "awawqwa"},
            List.of(new Part(Items.BOOK, 1), new Part(Items.ECHO_SHARD, 4), new Part(Items.SOUL_LANTERN, 1))) {
        @Override public ItemStack result() {
            return EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(ModEnchantments.SOUL_PYLON, 1));
        }
    };

    public record Part(Item item, int count) {}

    private static final ShadowRitual[] ALL = values();

    public static final int STEPS = 3;

    public final int accent;
    private final String[] steps;
    public final List<Part> parts;

    ShadowRitual(int accent, String[] steps, List<Part> parts) {
        this.accent = accent;
        this.steps = steps;
        this.parts = parts;
    }

    public String step(int i) {
        return steps[i];
    }

    public boolean stepMatches(int i, String drawn) {
        return !drawn.isEmpty() && (steps[i].equals(drawn) || steps[i].equals(RitualPatterns.reverse(drawn)));
    }

    public abstract ItemStack result();

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int offeringCount() {
        int n = 0;
        for (Part part : parts) n += part.count();
        return n;
    }

    public static ShadowRitual byId(int id) {
        return ALL[Math.floorMod(id, ALL.length)];
    }

    @Nullable
    public static ShadowRitual byKey(String key) {
        for (ShadowRitual r : ALL) if (r.key().equals(key)) return r;
        return null;
    }

    public static boolean anyContinues(List<String> done, String drawn) {
        int step = done.size();
        if (step >= STEPS) return false;
        for (ShadowRitual r : ALL) {
            if (r.follows(done) && r.stepMatches(step, drawn)) return true;
        }
        return false;
    }

    public boolean follows(List<String> done) {
        for (int i = 0; i < done.size(); i++) {
            if (!stepMatches(i, done.get(i))) return false;
        }
        return true;
    }

    @Nullable
    public static ShadowRitual bySteps(List<String> drawn) {
        if (drawn.size() != STEPS) return null;
        for (ShadowRitual r : ALL) {
            if (r.follows(drawn)) return r;
        }
        return null;
    }

    public static ShadowRitual random(net.minecraft.util.math.random.Random random) {
        return ALL[random.nextInt(ALL.length)];
    }
}
