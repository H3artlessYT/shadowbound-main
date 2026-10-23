package com.h3artless.shadowbound.mixin.client;

import com.h3artless.shadowbound.client.render.ShadowPuddle;
import com.h3artless.shadowbound.dive.ShadowDive;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void shadowbound$puddle(AbstractClientPlayerEntity player, float yaw, float tickDelta, MatrixStack matrices,
                                    VertexConsumerProvider buffers, int light, CallbackInfo ci) {
        if (ShadowDive.isDiving(player)) {
            ShadowPuddle.render(player, tickDelta, matrices, buffers);
            ci.cancel();
        }
    }
}
