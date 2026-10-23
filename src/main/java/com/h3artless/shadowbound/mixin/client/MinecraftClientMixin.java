package com.h3artless.shadowbound.mixin.client;

import com.h3artless.shadowbound.client.dive.DiveVision;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void shadowbound$senseTheLiving(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (DiveVision.senses(entity)) cir.setReturnValue(true);
    }
}
