package com.h3artless.shadowbound.mixin.client;

import com.h3artless.shadowbound.client.cast.AirCasting;
import com.h3artless.shadowbound.client.dive.DiveVision;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void shadowbound$senseTheLiving(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (DiveVision.senses(entity)) cir.setReturnValue(true);
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void shadowbound$castingHand(CallbackInfo ci) {
        if (AirCasting.active()) ci.cancel();
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void shadowbound$castingNoSwing(CallbackInfoReturnable<Boolean> cir) {
        if (AirCasting.active()) cir.setReturnValue(false);
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void shadowbound$castingNoMining(boolean breaking, CallbackInfo ci) {
        if (AirCasting.active()) ci.cancel();
    }
}
