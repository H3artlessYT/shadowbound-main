package com.h3artless.shadowbound.mixin;

import net.minecraft.entity.Entity;
import com.h3artless.shadowbound.item.ShadowPouchItem;
import com.h3artless.shadowbound.dive.ShadowDive;
import com.h3artless.shadowbound.item.ShadowTotem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import com.h3artless.shadowbound.shadow.ShadowMob;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "canTarget(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void shadowbound$cantSeeDivers(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (target instanceof PlayerEntity p && ShadowDive.isDiving(p)) cir.setReturnValue(false);
    }

    @Inject(method = "tryUseTotem", at = @At("HEAD"), cancellable = true)
    private void shadowbound$shadowTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayerEntity player && ShadowTotem.tryRescue(player, source)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "shouldDropLoot", at = @At("HEAD"), cancellable = true)
    private void shadowbound$noLoot(CallbackInfoReturnable<Boolean> cir) {
        if (this instanceof ShadowMob s && s.shadowbound$isShadow()) cir.setReturnValue(false);
        if (((Entity) (Object) this).getCommandTags().contains(ShadowPouchItem.TAG)) cir.setReturnValue(false);
    }

    @Inject(method = "shouldDropXp", at = @At("HEAD"), cancellable = true)
    private void shadowbound$noXp(CallbackInfoReturnable<Boolean> cir) {
        if (this instanceof ShadowMob s && s.shadowbound$isShadow()) cir.setReturnValue(false);
        if (((Entity) (Object) this).getCommandTags().contains(ShadowPouchItem.TAG)) cir.setReturnValue(false);
    }
}
