package com.h3artless.shadowbound.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.UUID;

public final class BladeReach {
    public static final String REA = "reach-entity-attributes";
    private static final UUID ID = UUID.fromString("5c1e7a3b-9d2f-4b8e-a6c1-3f0d72e4b915");
    private static final Identifier ATTACK_RANGE = new Identifier(REA, "attack_range");

    private static Boolean loaded;

    private BladeReach() {}

    public static boolean usesAttribute() {
        if (loaded == null) loaded = FabricLoader.getInstance().isModLoaded(REA);
        return loaded;
    }

    public static Multimap<EntityAttribute, EntityAttributeModifier> withReach(
            Multimap<EntityAttribute, EntityAttributeModifier> base, EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND || !usesAttribute()) return base;
        EntityAttribute range = Registries.ATTRIBUTE.get(ATTACK_RANGE);
        if (range == null) return base;
        return ImmutableMultimap.<EntityAttribute, EntityAttributeModifier>builder()
                .putAll(base)
                .put(range, new EntityAttributeModifier(ID, "Shadow blade reach", ShadowBlade.BONUS_REACH,
                        EntityAttributeModifier.Operation.ADDITION))
                .build();
    }
}
