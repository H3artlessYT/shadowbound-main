package com.h3artless.shadowbound.mixin.client;

import com.h3artless.shadowbound.client.cast.AirCasting;
import com.h3artless.shadowbound.registry.ModItems;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Shadow
    protected abstract void renderArmHoldingItem(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                                 float equipProgress, float swingProgress, Arm arm);

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void shadowbound$pointingHand(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                                          float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
                                          VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!AirCasting.active()) {
            if (!item.isEmpty() && item.isOf(ModItems.SHADOW_ROBOT) && !player.isInvisible()) {
                Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
                matrices.push();
                renderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                matrices.pop();
            }
            return;
        }
        ci.cancel();
        if (hand != Hand.MAIN_HAND) return;
        Arm arm = player.getMainArm();
        float side = arm == Arm.RIGHT ? 1f : -1f;
        float t = player.age + tickDelta;
        matrices.push();
        matrices.translate(-0.18f * side, 0.32f + MathHelper.sin(t * 0.13f) * 0.012f, -0.25f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-18f + MathHelper.sin(t * 0.21f) * 1.5f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-8f * side));
        renderArmHoldingItem(matrices, vertexConsumers, light, 0f, 0f, arm);
        matrices.pop();
    }
}
