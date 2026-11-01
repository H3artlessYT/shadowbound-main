package com.h3artless.shadowbound.registry;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.block.BoxControlBlock;
import com.h3artless.shadowbound.block.DematLeverBlock;
import com.h3artless.shadowbound.block.PedestalBlock;
import com.h3artless.shadowbound.block.PedestalBlockEntity;
import com.h3artless.shadowbound.box.ShadowBoxes;
import com.h3artless.shadowbound.block.ShadowBoxBlock;
import com.h3artless.shadowbound.block.ShadowBoxBlockEntity;
import com.h3artless.shadowbound.block.ShadowChestBlock;
import com.h3artless.shadowbound.block.ShadowConsoleBlock;
import com.h3artless.shadowbound.block.ShadowConsoleBlockEntity;
import com.h3artless.shadowbound.block.UmbralDoorBlock;
import com.h3artless.shadowbound.block.ShadowChestBlockEntity;
import com.h3artless.shadowbound.block.ShadowVisageBlock;
import com.h3artless.shadowbound.block.SnuffedLightBlock;
import com.h3artless.shadowbound.block.SnuffedLightBlockEntity;
import com.h3artless.shadowbound.block.SoulChokedPillarBlock;
import com.h3artless.shadowbound.block.SoulVessel;
import com.h3artless.shadowbound.block.SoulVesselBlockEntity;
import com.h3artless.shadowbound.block.WailingLanternBlock;
import com.h3artless.shadowbound.block.WeepingFaceBlock;
import com.h3artless.shadowbound.item.DecorBlockItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.Instrument;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

import java.util.function.ToIntFunction;

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

    public static final Block SHADOW_VISAGE = block("shadow_visage",
            new ShadowVisageBlock(stone().luminance(lit(5))), 3);

    public static final Block SOUL_CHOKED_PILLAR = block("soul_choked_pillar",
            new SoulChokedPillarBlock(stone().luminance(lit(3))), 3);

    public static final Block WAILING_LANTERN = block("wailing_lantern",
            new WailingLanternBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.IRON_GRAY)
                    .solid()
                    .requiresTool()
                    .strength(3.5f)
                    .sounds(BlockSoundGroup.LANTERN)
                    .luminance(lit(11))
                    .nonOpaque()
                    .pistonBehavior(PistonBehavior.DESTROY)), 3);

    public static final Block WEEPING_FACE = block("weeping_face",
            new WeepingFaceBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.BLACK)
                    .noCollision()
                    .strength(1.5f)
                    .sounds(BlockSoundGroup.DEEPSLATE_TILES)
                    .luminance(lit(2))
                    .nonOpaque()
                    .pistonBehavior(PistonBehavior.DESTROY)), 3);

    public static final Block SHADOW_CHEST = block("shadow_chest",
            new ShadowChestBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.BLACK)
                    .instrument(Instrument.BASEDRUM)
                    .strength(3.5f, 6f)
                    .sounds(BlockSoundGroup.DEEPSLATE_TILES)
                    .luminance(state -> 1)), 2);

    public static final BlockEntityType<ShadowChestBlockEntity> SHADOW_CHEST_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Shadowbound.id("shadow_chest"),
            BlockEntityType.Builder.create(ShadowChestBlockEntity::new, SHADOW_CHEST).build(null));

    public static final Block SHADOW_BOX = Registry.register(Registries.BLOCK, Shadowbound.id("shadow_box"),
            new ShadowBoxBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.BLACK)
                    .requiresTool()
                    .strength(4f, 3600000f)
                    .sounds(BlockSoundGroup.DEEPSLATE_TILES)
                    .nonOpaque()
                    .dropsNothing()
                    .pistonBehavior(PistonBehavior.BLOCK)
                    .luminance(state -> state.get(ShadowBoxBlock.HALF) == DoubleBlockHalf.UPPER ? 10 : 4)));

    public static final Block SHADOW_BOX_TOP = Registry.register(Registries.BLOCK, Shadowbound.id("shadow_box_top"),
            new com.h3artless.shadowbound.block.ShadowBoxTopBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.BLACK)
                    .requiresTool()
                    .strength(4f, 3600000f)
                    .sounds(BlockSoundGroup.DEEPSLATE_TILES)
                    .nonOpaque()
                    .dropsNothing()
                    .pistonBehavior(PistonBehavior.BLOCK)
                    .luminance(state -> state.contains(com.h3artless.shadowbound.block.ShadowBoxTopBlock.TIP) && state.get(com.h3artless.shadowbound.block.ShadowBoxTopBlock.TIP) ? 14 : 11)));

    public static final BlockEntityType<ShadowBoxBlockEntity> SHADOW_BOX_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Shadowbound.id("shadow_box"),
            BlockEntityType.Builder.create(ShadowBoxBlockEntity::new, SHADOW_BOX).build(null));

    public static final Block SHADOW_CONSOLE = Registry.register(Registries.BLOCK, Shadowbound.id("shadow_console"),
            new ShadowConsoleBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.BLACK)
                    .strength(-1f, 3600000f)
                    .sounds(BlockSoundGroup.DEEPSLATE_TILES)
                    .nonOpaque()
                    .dropsNothing()
                    .luminance(state -> 12)));

    public static final BlockEntityType<ShadowConsoleBlockEntity> SHADOW_CONSOLE_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Shadowbound.id("shadow_console"),
            BlockEntityType.Builder.create(ShadowConsoleBlockEntity::new, SHADOW_CONSOLE).build(null));

    public static final Block UMBRAL_DOOR = Registry.register(Registries.BLOCK, Shadowbound.id("umbral_door"),
            new UmbralDoorBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.BLACK)
                    .strength(-1f, 3600000f)
                    .sounds(BlockSoundGroup.DEEPSLATE_TILES)
                    .dropsNothing()
                    .luminance(state -> 3)));

    public static final Block NAV_PANEL = control("navigation_panel", ShadowBoxes.NAV);
    public static final Block MEMORY_PANEL = control("memory_bank", ShadowBoxes.MEMORY);
    public static final Block FUEL_PANEL = control("fuel_tank", ShadowBoxes.FUEL);
    public static final Block SCANNER_PANEL = control("scanner", ShadowBoxes.SCANNER);

    public static final Block DEMAT_LEVER = Registry.register(Registries.BLOCK, Shadowbound.id("demat_lever"),
            new DematLeverBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.BLACK)
                    .strength(-1f, 3600000f)
                    .sounds(BlockSoundGroup.METAL)
                    .nonOpaque()
                    .dropsNothing()
                    .luminance(state -> 6)));

    public static final BlockEntityType<SoulVesselBlockEntity> SOUL_VESSEL_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Shadowbound.id("soul_vessel"),
            BlockEntityType.Builder.create(SoulVesselBlockEntity::new,
                    SHADOW_VISAGE, SOUL_CHOKED_PILLAR, WAILING_LANTERN, WEEPING_FACE).build(null));

    public static final Block PEDESTAL = block("pedestal",
            new PedestalBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.BLACK)
                    .instrument(Instrument.BASEDRUM)
                    .requiresTool()
                    .strength(2.5f, 6f)
                    .sounds(BlockSoundGroup.POLISHED_DEEPSLATE)
                    .nonOpaque()
                    .luminance(state -> 3)), 3);

    public static final BlockEntityType<PedestalBlockEntity> PEDESTAL_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE, Shadowbound.id("pedestal"),
            BlockEntityType.Builder.create(PedestalBlockEntity::new, PEDESTAL).build(null));

    private ModBlocks() {}

    private static Block control(String name, int screen) {
        return Registry.register(Registries.BLOCK, Shadowbound.id(name),
                new BoxControlBlock(screen, AbstractBlock.Settings.create()
                        .mapColor(MapColor.BLACK)
                        .strength(-1f, 3600000f)
                        .sounds(BlockSoundGroup.DEEPSLATE_TILES)
                        .nonOpaque()
                        .dropsNothing()
                        .luminance(state -> 8)));
    }

    private static AbstractBlock.Settings stone() {
        return AbstractBlock.Settings.create()
                .mapColor(MapColor.BLACK)
                .instrument(Instrument.BASEDRUM)
                .requiresTool()
                .strength(2.5f, 6f)
                .sounds(BlockSoundGroup.DEEPSLATE_TILES);
    }

    private static ToIntFunction<BlockState> lit(int level) {
        return state -> state.get(SoulVessel.POWERED) ? 0 : level;
    }

    private static Block block(String name, Block block, int tooltipLines) {
        Registry.register(Registries.BLOCK, Shadowbound.id(name), block);
        BlockItem item = tooltipLines > 0
                ? new DecorBlockItem(block, tooltipLines, new Item.Settings())
                : new BlockItem(block, new Item.Settings());
        Registry.register(Registries.ITEM, Shadowbound.id(name), item);
        return block;
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.add(SHADOW_VISAGE);
            entries.add(SOUL_CHOKED_PILLAR);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(WAILING_LANTERN);
            entries.add(WEEPING_FACE);
            entries.add(SHADOW_CHEST);
        });
    }
}
