package com.h3artless.shadowbound.cooldown;

import java.util.Locale;

public enum Ability {
    PYLON(20 * 30),
    SHADOWS(20 * 60),
    TOTEM(20 * 120),
    SHADOW_RESPAWN(20 * 120);

    public final int ticks;

    Ability(int ticks) {
        this.ticks = ticks;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
