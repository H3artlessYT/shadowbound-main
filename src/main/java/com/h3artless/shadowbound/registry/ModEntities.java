package com.h3artless.shadowbound.registry;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.entity.GrimoireEntity;
import com.h3artless.shadowbound.entity.RitualEntity;
import com.h3artless.shadowbound.entity.ShadowCloneEntity;
import com.h3artless.shadowbound.entity.SoulPylonEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
    public static final EntityType<ShadowCloneEntity> SHADOW_CLONE = Registry.register(Registries.ENTITY_TYPE,
            Shadowbound.id("shadow_clone"),
            FabricEntityTypeBuilder.<ShadowCloneEntity>create(SpawnGroup.MISC, ShadowCloneEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    .trackRangeBlocks(64)
                    .fireImmune()
                    .build());

    public static final EntityType<RitualEntity> RITUAL = Registry.register(Registries.ENTITY_TYPE,
            Shadowbound.id("ritual"),
            FabricEntityTypeBuilder.<RitualEntity>create(SpawnGroup.MISC, RitualEntity::new)
                    .dimensions(EntityDimensions.fixed(0.5f, 0.5f))
                    .trackRangeBlocks(128)
                    .trackedUpdateRate(1)
                    .fireImmune()
                    .build());

    public static final EntityType<SoulPylonEntity> SOUL_PYLON = Registry.register(Registries.ENTITY_TYPE,
            Shadowbound.id("soul_pylon"),
            FabricEntityTypeBuilder.<SoulPylonEntity>create(SpawnGroup.MISC, SoulPylonEntity::new)
                    .dimensions(EntityDimensions.fixed(0.8f, 2.6f))
                    .trackRangeBlocks(96)
                    .trackedUpdateRate(2)
                    .fireImmune()
                    .build());

    public static final EntityType<GrimoireEntity> GRIMOIRE = Registry.register(Registries.ENTITY_TYPE,
            Shadowbound.id("grimoire"),
            FabricEntityTypeBuilder.<GrimoireEntity>create(SpawnGroup.MISC, GrimoireEntity::new)
                    .dimensions(EntityDimensions.fixed(0.7f, 1.6f))
                    .trackRangeBlocks(64)
                    .trackedUpdateRate(10)
                    .fireImmune()
                    .build());

    private ModEntities() {}

    public static void init() {
        FabricDefaultAttributeRegistry.register(SHADOW_CLONE, ShadowCloneEntity.createCloneAttributes());
    }
}
