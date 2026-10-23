package com.h3artless.shadowbound.registry;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.block.SnuffedLightBlock;
import com.h3artless.shadowbound.block.SnuffedLightBlockEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModBlocks {
    public static final Block SNUFFED_LIGHT = Registry.register(Registries.BLOCK, Shadowbound.id("snuffed_light"),
            new SnuffedLightBlock(AbstractBlock.Settings.create()
                    .nonOpaque()
                    .dropsNothing()
                    .pistonBehavior(PistonBehavior.BLOCK)
                    .strength(0.5f)));

    public static final BlockEntityType<SnuffedLightBlockEntity> SNUFFED_LIGHT_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Shadowbound.id("snuffed_light"),
            BlockEntityType.Builder.create(SnuffedLightBlockEntity::new, SNUFFED_LIGHT).build(null));

    private ModBlocks() {}

    public static void init() {}
}
