package com.h3artless.shadowbound.mixin.client;

import com.h3artless.shadowbound.client.dive.DiveVision;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityOutlineColorMixin {
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void shadowbound$senseColour(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (!self.isGlowing() && DiveVision.senses(self)) cir.setReturnValue(DiveVision.SENSE_COLOR);
    }
}
