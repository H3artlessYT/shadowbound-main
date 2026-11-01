package com.h3artless.shadowbound.mixin.client;

import com.h3artless.shadowbound.client.spy.SpyCamera;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void shadowbound$freeze(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        if (!SpyCamera.locked()) return;
        this.pressingForward = this.pressingBack = this.pressingLeft = this.pressingRight = false;
        this.jumping = this.sneaking = false;
        this.movementForward = 0f;
        this.movementSideways = 0f;
    }
}
