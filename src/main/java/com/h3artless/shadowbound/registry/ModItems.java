package com.h3artless.shadowbound.registry;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.item.ShadowCleaverItem;
import com.h3artless.shadowbound.item.ShadowGrimoireItem;
import com.h3artless.shadowbound.item.ShadowPaperItem;
import com.h3artless.shadowbound.item.ShadowLongswordItem;
import com.h3artless.shadowbound.item.ShadowTridentItem;
import com.h3artless.shadowbound.item.ShadowCodexItem;
import com.h3artless.shadowbound.item.ShadowPouchItem;
import com.h3artless.shadowbound.item.ShadowKatanaItem;
import com.h3artless.shadowbound.item.ShadowHookItem;
import com.h3artless.shadowbound.item.ShadowKeyItem;
import com.h3artless.shadowbound.item.BottledDarknessItem;
import com.h3artless.shadowbound.item.DecorBlockItem;
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

    public static final Item SHADOW_TRIDENT = register("shadow_trident",
            new ShadowTridentItem(new Item.Settings().maxDamage(500).fireproof().rarity(Rarity.EPIC)));

    public static final Item SHADOW_CODEX = register("shadow_codex",
            new ShadowCodexItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));

    public static final Item SHADOW_KATANA = register("shadow_katana",
            new ShadowKatanaItem(new Item.Settings().maxDamage(2500).fireproof().rarity(Rarity.EPIC)));

    public static final Item SHADOW_HOOK = register("shadow_hook",
            new ShadowHookItem(new Item.Settings().maxDamage(768).fireproof().rarity(Rarity.RARE)));

    public static final Item SHADOW_KEY = register("shadow_key",
            new ShadowKeyItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));

    public static final Item BOTTLED_DARKNESS = register("bottled_darkness",
            new BottledDarknessItem(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON)));

    public static final Item SHADOW_BOX = register("shadow_box",
            new DecorBlockItem(ModBlocks.SHADOW_BOX, 3, new Item.Settings().maxCount(1).fireproof().rarity(Rarity.EPIC)));

    public static final Item SHADOW_POUCH = register("shadow_pouch",
            new ShadowPouchItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));

    public static final Item TOTEM_OF_SHADOWNESS = register("totem_of_shadowness",
            new TotemOfShadownessItem(new Item.Settings().maxCount(1).fireproof().rarity(Rarity.EPIC)));

    public static final Item SHADOW_GRIMOIRE = register("shadow_grimoire",
            new ShadowGrimoireItem(new Item.Settings().maxCount(1).fireproof().rarity(Rarity.RARE)));

    public static final Item SHADOW_PAPER = register("shadow_paper",
            new ShadowPaperItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));

    public static final Item SHADOW_ROBOT = register("shadow_robot",
            new com.h3artless.shadowbound.item.ShadowRobotItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));

    public static final Item SHADOW_SATCHEL = net.minecraft.item.Items.AIR;


    private ModItems() {}

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Shadowbound.id(name), item);
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(SHADOW_LONGSWORD);
            entries.add(SHADOW_CLEAVER);
            entries.add(SHADOW_TRIDENT);
            entries.add(SHADOW_KATANA);
            entries.add(TOTEM_OF_SHADOWNESS);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(SHADOW_GRIMOIRE);
            entries.add(SHADOW_PAPER);
            entries.add(SHADOW_CODEX);
            entries.add(SHADOW_POUCH);
            entries.add(SHADOW_HOOK);
            entries.add(SHADOW_KEY);
            entries.add(BOTTLED_DARKNESS);
            entries.add(SHADOW_BOX);
        });
    }
}
