package com.h3artless.shadowbound.registry;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.enchant.SoulPylonEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEnchantments {
    public static final Enchantment SOUL_PYLON = Registry.register(Registries.ENCHANTMENT,
            Shadowbound.id("soul_pylon"), new SoulPylonEnchantment());

    private ModEnchantments() {}

    public static void init() {}
}
