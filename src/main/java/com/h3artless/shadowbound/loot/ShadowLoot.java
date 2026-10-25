package com.h3artless.shadowbound.loot;

import com.h3artless.shadowbound.registry.ModItems;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.util.Identifier;

import java.util.Map;

public final class ShadowLoot {
    private static final Map<Identifier, Float> CHANCES = Map.of(
            LootTables.STRONGHOLD_LIBRARY_CHEST, 0.8f,
            LootTables.ANCIENT_CITY_CHEST, 0.6f,
            LootTables.WOODLAND_MANSION_CHEST, 0.55f,
            LootTables.SIMPLE_DUNGEON_CHEST, 0.4f,
            LootTables.ABANDONED_MINESHAFT_CHEST, 0.3f,
            LootTables.BASTION_TREASURE_CHEST, 0.45f,
            LootTables.NETHER_BRIDGE_CHEST, 0.35f,
            LootTables.DESERT_PYRAMID_CHEST, 0.35f,
            LootTables.JUNGLE_TEMPLE_CHEST, 0.4f,
            LootTables.PILLAGER_OUTPOST_CHEST, 0.3f);

    private ShadowLoot() {}

    public static void init() {
        LootTableEvents.MODIFY.register((resources, manager, id, table, source) -> {
            Float chance = CHANCES.get(id);
            if (chance == null || !source.isBuiltin()) return;
            table.pool(LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1))
                    .conditionally(RandomChanceLootCondition.builder(chance))
                    .with(ItemEntry.builder(ModItems.SHADOW_PAPER)));
        });
    }
}
