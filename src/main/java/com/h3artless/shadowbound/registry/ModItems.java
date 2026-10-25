package com.h3artless.shadowbound.registry;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.item.ShadowCleaverItem;
import com.h3artless.shadowbound.item.ShadowGrimoireItem;
import com.h3artless.shadowbound.item.ShadowPaperItem;
import com.h3artless.shadowbound.item.ShadowLongswordItem;
import com.h3artless.shadowbound.item.TotemOfShadownessItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

public final class ModItems {
    public static final Item SHADOW_LONGSWORD = register("shadow_longsword",
            new ShadowLongswordItem(ToolMaterials.NETHERITE, 5, -3.0f,
                    new Item.Settings().fireproof().rarity(Rarity.EPIC)));

    public static final Item SHADOW_CLEAVER = register("shadow_cleaver",
            new ShadowCleaverItem(ToolMaterials.NETHERITE, 3.0f, -2.4f,
                    new Item.Settings().fireproof().rarity(Rarity.EPIC)));

    public static final Item TOTEM_OF_SHADOWNESS = register("totem_of_shadowness",
            new TotemOfShadownessItem(new Item.Settings().maxCount(1).fireproof().rarity(Rarity.EPIC)));

    public static final Item SHADOW_GRIMOIRE = register("shadow_grimoire",
            new ShadowGrimoireItem(new Item.Settings().maxCount(1).fireproof().rarity(Rarity.RARE)));

    public static final Item SHADOW_PAPER = register("shadow_paper",
            new ShadowPaperItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));

    private ModItems() {}

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Shadowbound.id(name), item);
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(SHADOW_LONGSWORD);
            entries.add(SHADOW_CLEAVER);
            entries.add(TOTEM_OF_SHADOWNESS);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(SHADOW_GRIMOIRE);
            entries.add(SHADOW_PAPER);
        });
    }
}
