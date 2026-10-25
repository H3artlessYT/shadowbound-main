package com.h3artless.shadowbound.registry;

import com.h3artless.shadowbound.Shadowbound;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModParticles {
    public static final DefaultParticleType RUNE = register("rune");
    public static final DefaultParticleType ASH = register("ash");
    public static final DefaultParticleType MOTE = register("mote");

    private ModParticles() {}

    private static DefaultParticleType register(String name) {
        return Registry.register(Registries.PARTICLE_TYPE, Shadowbound.id(name), FabricParticleTypes.simple(false));
    }

    public static void init() {
    }
}
