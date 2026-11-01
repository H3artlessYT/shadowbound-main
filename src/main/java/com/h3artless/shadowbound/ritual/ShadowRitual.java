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
            List.of(new Part(Items.NETHERITE_AXE, 1), new Part(Items.NETHERITE_INGOT, 1), new Part(Items.GOLD_BLOCK, 2))) {
        @Override public ItemStack result() { return new ItemStack(ModItems.SHADOW_CLEAVER); }
    },
    LONGSWORD(0xff2a3c, new String[]{"wwwwdd", "qaqwqaq", "wdwawdw"},
            List.of(new Part(Items.NETHERITE_SWORD, 1), new Part(Items.NETHERITE_INGOT, 1), new Part(Items.DIAMOND_BLOCK, 1))) {
        @Override public ItemStack result() { return new ItemStack(ModItems.SHADOW_LONGSWORD); }
    },
    TOTEM(0xd0102c, new String[]{"eeeeeqw", "aqaaqa", "qqwqqwqq"},
            List.of(new Part(Items.TOTEM_OF_UNDYING, 1), new Part(Items.DIAMOND_BLOCK, 2), new Part(Items.NETHERITE_INGOT, 10))) {
        @Override public ItemStack result() { return new ItemStack(ModItems.TOTEM_OF_SHADOWNESS); }
    },
    SOUL_PYLON(0xb01020, new String[]{"dadadad", "dwwdaawwd", "awawqwa"},
            List.of(new Part(Items.BOOK, 1), new Part(Items.ECHO_SHARD, 4), new Part(Items.SOUL_LANTERN, 1))) {
        @Override public ItemStack result() {
            return EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(ModEnchantments.SOUL_PYLON, 1));
        }
    },
    TRIDENT(0xc8142e, new String[]{"wwwaqwqd", "eqwqedwd", "aawwaww"},
            List.of(new Part(Items.BLAZE_ROD, 1), new Part(Items.DIAMOND_BLOCK, 2), new Part(Items.NETHERITE_INGOT, 1))) {
        @Override public ItemStack result() { return new ItemStack(ModItems.SHADOW_TRIDENT); }
    },
    KATANA(0x9a1030, new String[]{"wwwwwwq", "wwdwwdww", "eaeaewe"},
            List.of(new Part(Items.NETHERITE_SWORD, 1), new Part(Items.NETHER_STAR, 1), new Part(Items.NETHERITE_INGOT, 8),
                    new Part(Items.BONE, 16))) {
        @Override public ItemStack result() { return new ItemStack(ModItems.SHADOW_KATANA); }
    },
    HOOK(0xb01a30, new String[]{"wwwqqww", "qeqeqdd", "adadwad"},
            List.of(new Part(Items.CHAIN, 8), new Part(Items.ECHO_SHARD, 2), new Part(Items.NETHERITE_SCRAP, 1))) {
        @Override public ItemStack result() { return new ItemStack(ModItems.SHADOW_HOOK); }
    },
    BOX(0x7a1030, new String[]{"wweewweew", "qqwwqqww", "ewwaawwe"},
            List.of(new Part(Items.RECOVERY_COMPASS, 1), new Part(Items.ENDER_PEARL, 8), new Part(Items.ECHO_SHARD, 4),
                    new Part(Items.CRYING_OBSIDIAN, 16))) {
        @Override public ItemStack result() { return new ItemStack(ModItems.SHADOW_BOX); }
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

    public static final int MAX_SHOWN = 12;
    private List<Item> shown;

    public List<Item> shown() {
        if (shown == null) {
            List<Item> out = new java.util.ArrayList<>();
            int[] left = new int[parts.size()];
            for (int i = 0; i < left.length; i++) left[i] = parts.get(i).count();
            boolean any = true;
            while (any && out.size() < MAX_SHOWN) {
                any = false;
                for (int i = 0; i < left.length && out.size() < MAX_SHOWN; i++) {
                    if (left[i] <= 0) continue;
                    left[i]--;
                    out.add(parts.get(i).item());
                    any = true;
                }
            }
            shown = List.copyOf(out);
        }
        return shown;
    }

    public int offeringCount() {
        return shown().size();
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
