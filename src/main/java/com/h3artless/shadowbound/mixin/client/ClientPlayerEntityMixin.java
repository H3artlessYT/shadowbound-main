package com.h3artless.shadowbound.mixin.client;

import com.h3artless.shadowbound.dive.ShadowDive;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @Inject(method = "shouldSlowDown", at = @At("HEAD"), cancellable = true)
    private void shadowbound$fullSpeedInShadow(CallbackInfoReturnable<Boolean> cir) {
        if (ShadowDive.isDiving((ClientPlayerEntity) (Object) this)) cir.setReturnValue(false);
    }
}
