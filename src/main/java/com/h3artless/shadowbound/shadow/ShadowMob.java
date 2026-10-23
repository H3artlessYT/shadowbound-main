package com.h3artless.shadowbound.shadow;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface ShadowMob {
    boolean shadowbound$isShadow();

    @Nullable
    UUID shadowbound$getOwner();

    void shadowbound$bind(UUID owner, int lifetime);
}
