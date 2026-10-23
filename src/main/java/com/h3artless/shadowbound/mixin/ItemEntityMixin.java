package com.h3artless.shadowbound.mixin;

import com.h3artless.shadowbound.ritual.RitualDetector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {
    public ItemEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void shadowbound$ritualCheck(CallbackInfo ci) {
        if ((this.age + this.getId()) % 10 != 0 || this.isRemoved() || !this.isOnGround()) return;
        if (this.getWorld() instanceof ServerWorld world) {
            RitualDetector.check(world, (ItemEntity) (Object) this);
        }
    }
}
