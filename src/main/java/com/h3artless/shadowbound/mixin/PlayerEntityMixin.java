package com.h3artless.shadowbound.mixin;

import com.h3artless.shadowbound.cooldown.Ability;
import com.h3artless.shadowbound.cooldown.CooldownHolder;
import com.h3artless.shadowbound.meter.DarknessHolder;
import com.h3artless.shadowbound.spy.SpyReturnHolder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements DarknessHolder, SpyReturnHolder, CooldownHolder {
    @Unique private int shadowbound$darkness;
    @Unique private final int[] shadowbound$cooldowns = new int[Ability.values().length];

    @Override
    public int[] shadowbound$cooldowns() {
        return shadowbound$cooldowns;
    }
    @Unique private NbtCompound shadowbound$spyReturn = new NbtCompound();

    @Override
    public NbtCompound shadowbound$getSpyReturn() {
        return shadowbound$spyReturn;
    }

    @Override
    public void shadowbound$setSpyReturn(NbtCompound nbt) {
        shadowbound$spyReturn = nbt;
    }

    @Override
    public int shadowbound$getDarkness() {
        return shadowbound$darkness;
    }

    @Override
    public void shadowbound$setDarkness(int value) {
        shadowbound$darkness = value;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void shadowbound$save(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt("ShadowboundDarkness", shadowbound$darkness);
        if (!shadowbound$spyReturn.isEmpty()) nbt.put("ShadowboundSpyReturn", shadowbound$spyReturn);
        nbt.putIntArray("ShadowboundCooldowns", shadowbound$cooldowns);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void shadowbound$load(NbtCompound nbt, CallbackInfo ci) {
        shadowbound$darkness = nbt.getInt("ShadowboundDarkness");
        shadowbound$spyReturn = nbt.getCompound("ShadowboundSpyReturn");
        int[] saved = nbt.getIntArray("ShadowboundCooldowns");
        System.arraycopy(saved, 0, shadowbound$cooldowns, 0, Math.min(saved.length, shadowbound$cooldowns.length));
    }
}
