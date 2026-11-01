package com.h3artless.shadowbound.registry;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.enchant.LaserVisionEnchantment;
import com.h3artless.shadowbound.enchant.SoulPylonEnchantment;
import com.h3artless.shadowbound.enchant.SoulWrenchingEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEnchantments {
    public static final Enchantment SOUL_PYLON = Registry.register(Registries.ENCHANTMENT,
            Shadowbound.id("soul_pylon"), new SoulPylonEnchantment());

    public static final Enchantment SOUL_WRENCHING = Registry.register(Registries.ENCHANTMENT,
            Shadowbound.id("soul_wrenching"), new SoulWrenchingEnchantment());

    public static final Enchantment LASER_VISION = Registry.register(Registries.ENCHANTMENT,
            Shadowbound.id("laser_vision"), new LaserVisionEnchantment());

    private ModEnchantments() {}

    public static void init() {}
}
