package com.h3artless.shadowbound.client.render;

import com.h3artless.shadowbound.entity.ShadowCloneEntity;
import com.h3artless.shadowbound.shadow.ShadowMob;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class ShadowRenderState {
    @Nullable public static LivingEntity current;
    @Nullable public static VertexConsumerProvider buffers;
    public static float tickDelta;

    private ShadowRenderState() {}

    public static boolean isShadowy(LivingEntity e) {
        return e instanceof ShadowCloneEntity || (e instanceof ShadowMob s && s.shadowbound$isShadow());
    }
}
