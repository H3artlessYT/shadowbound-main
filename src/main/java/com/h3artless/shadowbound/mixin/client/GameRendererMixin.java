package com.h3artless.shadowbound.mixin.client;

import com.h3artless.shadowbound.client.dive.DiveVision;
import com.h3artless.shadowbound.client.spy.SpyCamera;
import com.h3artless.shadowbound.item.BladeReach;
import com.h3artless.shadowbound.item.ShadowBlade;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private MinecraftClient client;

    @ModifyConstant(method = "updateTargetedEntity", constant = @Constant(doubleValue = 9.0), require = 0)
    private double shadowbound$bladeReach(double original) {
        if (BladeReach.usesAttribute()) return original;
        if (client.player != null && ShadowBlade.isBlade(client.player.getMainHandStack())) {
            double reach = 3.0 + ShadowBlade.BONUS_REACH;
            return reach * reach;
        }
        return original;
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void shadowbound$noHandsWhileSpying(MatrixStack matrices, Camera camera, float tickDelta, CallbackInfo ci) {
        if (SpyCamera.isActive() || DiveVision.isDiving()) ci.cancel();
    }
}
