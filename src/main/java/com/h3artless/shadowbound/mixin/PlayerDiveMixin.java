package com.h3artless.shadowbound.mixin;

import com.h3artless.shadowbound.dive.DiveHolder;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerDiveMixin extends LivingEntity implements DiveHolder {
    @Unique
    private static final TrackedData<Boolean> SHADOWBOUND_DIVING =
            DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique
    private static final TrackedData<Byte> SHADOWBOUND_DIVE_LIGHT =
            DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.BYTE);

    protected PlayerDiveMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void shadowbound$trackDive(CallbackInfo ci) {
        this.dataTracker.startTracking(SHADOWBOUND_DIVING, false);
        this.dataTracker.startTracking(SHADOWBOUND_DIVE_LIGHT, (byte) 0);
    }

    @Override
    public boolean shadowbound$isDiving() {
        return this.dataTracker.get(SHADOWBOUND_DIVING);
    }

    @Override
    public void shadowbound$setDiving(boolean diving) {
        this.dataTracker.set(SHADOWBOUND_DIVING, diving);
    }

    @Override
    public int shadowbound$getDiveLight() {
        return this.dataTracker.get(SHADOWBOUND_DIVE_LIGHT);
    }

    @Override
    public void shadowbound$setDiveLight(int light) {
        byte b = (byte) light;
        if (this.dataTracker.get(SHADOWBOUND_DIVE_LIGHT) != b) this.dataTracker.set(SHADOWBOUND_DIVE_LIGHT, b);
    }

    @Inject(method = "updatePose", at = @At("HEAD"), cancellable = true)
    private void shadowbound$divePose(CallbackInfo ci) {
        if (shadowbound$isDiving()) {
            if (getPose() != EntityPose.SWIMMING) setPose(EntityPose.SWIMMING);
            ci.cancel();
        }
    }
}
