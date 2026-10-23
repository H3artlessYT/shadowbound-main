package com.h3artless.shadowbound.mixin.client;

import com.h3artless.shadowbound.client.render.FxQuality;
import com.h3artless.shadowbound.client.render.ShadowRenderState;
import com.h3artless.shadowbound.client.render.ShadowWisps;
import com.h3artless.shadowbound.client.render.SmokeLayers;
import com.h3artless.shadowbound.entity.ShadowCloneEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity> {
    @Unique private static final String RENDER =
            "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V";

    @Inject(method = RENDER, at = @At("HEAD"))
    private void shadowbound$remember(T entity, float yaw, float tickDelta, MatrixStack matrices,
                                      VertexConsumerProvider buffers, int light, CallbackInfo ci) {
        ShadowRenderState.current = entity;
        ShadowRenderState.buffers = buffers;
        ShadowRenderState.tickDelta = tickDelta;
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void shadowbound$forget(T entity, float yaw, float tickDelta, MatrixStack matrices,
                                    VertexConsumerProvider buffers, int light, CallbackInfo ci) {
        if (ShadowRenderState.isShadowy(entity) && !entity.isInvisible() && FxQuality.distSq(entity) < 40 * 40) {
            ShadowWisps.draw(entity, tickDelta, matrices, buffers);
        }
        ShadowRenderState.current = null;
        ShadowRenderState.buffers = null;
    }

    @Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true)
    private void shadowbound$smokeLayer(T entity, boolean showBody, boolean translucent, boolean showOutline,
                                        CallbackInfoReturnable<RenderLayer> cir) {
        if (showBody && ShadowRenderState.isShadowy(entity)) {
            cir.setReturnValue(SmokeLayers.body(shadowbound$texture(entity)));
        }
    }

    @SuppressWarnings("unchecked")
    @WrapOperation(method = RENDER, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIFFFF)V"))
    private void shadowbound$smoke(EntityModel<T> model, MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                                   float r, float g, float b, float a, Operation<Void> original) {
        LivingEntity e = ShadowRenderState.current;
        if (e == null || !ShadowRenderState.isShadowy(e)) {
            original.call(model, matrices, vertices, light, overlay, r, g, b, a);
            return;
        }

        float seed = MathHelper.fractionalPart(e.getId() * 0.618034f);
        float kind = e instanceof ShadowCloneEntity ? 1f : 0f;
        VertexConsumerProvider buffers = ShadowRenderState.buffers;

        original.call(model, matrices, vertices, light, overlay, 0f, seed, kind, a);
        if (buffers == null) return;
        int shells = FxQuality.shells(FxQuality.distSq(e));
        if (shells == 0) return;

        VertexConsumer shell = buffers.getBuffer(SmokeLayers.shell(shadowbound$texture((T) e)));
        float time = (e.age + ShadowRenderState.tickDelta) / 20f;
        float pivot = 1.501f - e.getHeight() * 0.5f;

        for (int i = 0; i < shells; i++) {
            float phase = MathHelper.fractionalPart(time * 0.32f + i / (float) shells + seed);
            float rise = phase * phase * 0.8f;
            float swell = 1.03f + phase * 0.2f;
            float drift = MathHelper.sin(time * 1.3f + i * 2.1f) * 0.06f * phase;

            matrices.push();
            matrices.translate(drift, -rise, drift * 0.5f);
            matrices.translate(0, pivot, 0);
            matrices.scale(swell, swell * 1.04f, swell);
            matrices.translate(0, -pivot, 0);
            original.call(model, matrices, shell, light, OverlayTexture.DEFAULT_UV,
                    Math.max(phase, 0.01f), seed, kind, a);
            matrices.pop();
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private Identifier shadowbound$texture(T entity) {
        return ((EntityRenderer<T>) (Object) this).getTexture(entity);
    }
}
