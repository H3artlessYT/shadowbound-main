package com.h3artless.shadowbound.client.spy;

import net.minecraft.util.Identifier;

import java.util.UUID;

public record SpyTarget(UUID uuid, String name, Identifier dimension, double distance,
                        float health, float maxHealth, boolean spectator) {
}
