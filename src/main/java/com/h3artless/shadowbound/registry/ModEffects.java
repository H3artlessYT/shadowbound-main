package com.h3artless.shadowbound.registry;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.effect.WatchedEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEffects {
    public static final StatusEffect WATCHED = Registry.register(Registries.STATUS_EFFECT,
            Shadowbound.id("watched"), new WatchedEffect());

    private ModEffects() {}

    public static void init() {}
}
