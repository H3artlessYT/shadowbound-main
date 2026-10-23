package com.h3artless.shadowbound.mixin.client;

import com.h3artless.shadowbound.client.spy.SpyHud;
import com.h3artless.shadowbound.client.spy.SpyCamera;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void shadowbound$spyHud(DrawContext ctx, float tickDelta, CallbackInfo ci) {
        if (SpyCamera.isActive()) {
            SpyHud.render(ctx, tickDelta);
            ci.cancel();
        }
    }
}
