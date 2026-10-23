package com.h3artless.shadowbound.ritual;

import com.h3artless.shadowbound.registry.ModEnchantments;
import com.h3artless.shadowbound.registry.ModItems;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public enum ShadowRitual {
    CLEAVER(0xff5a3a, Items.NETHERITE_AXE,
            List.of(new Part(Items.NETHERITE_AXE, 1), new Part(Items.NETHERITE_INGOT, 1), new Part(Items.GOLD_BLOCK, 1))) {
        @Override public ItemStack result() { return new ItemStack(ModItems.SHADOW_CLEAVER); }
    },
    LONGSWORD(0xff2a3c, Items.NETHERITE_SWORD,
            List.of(new Part(Items.NETHERITE_SWORD, 1), new Part(Items.NETHERITE_INGOT, 1), new Part(Items.DIAMOND_BLOCK, 1))) {
        @Override public ItemStack result() { return new ItemStack(ModItems.SHADOW_LONGSWORD); }
    },
    TOTEM(0xd0102c, Items.TOTEM_OF_UNDYING,
            List.of(new Part(Items.TOTEM_OF_UNDYING, 1), new Part(Items.DIAMOND_BLOCK, 2), new Part(Items.BLAZE_ROD, 10))) {
        @Override public ItemStack result() { return new ItemStack(ModItems.TOTEM_OF_SHADOWNESS); }
    },
    SOUL_PYLON(0xb01020, Items.BOOK,
            List.of(new Part(Items.BOOK, 1), new Part(Items.ECHO_SHARD, 4), new Part(Items.SOUL_LANTERN, 1))) {
        @Override public ItemStack result() {
            return EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(ModEnchantments.SOUL_PYLON, 1));
        }
    };

    public record Part(Item item, int count) {}

    public final int accent;
    public final Item anchor;
    public final List<Part> parts;

    ShadowRitual(int accent, Item anchor, List<Part> parts) {
        this.accent = accent;
        this.anchor = anchor;
        this.parts = parts;
    }

    public abstract ItemStack result();

    public int offeringCount() {
        int n = 0;
        for (Part part : parts) n += part.count();
        return n;
    }

    public static ShadowRitual byId(int id) {
        ShadowRitual[] all = values();
        return all[Math.floorMod(id, all.length)];
    }
}
