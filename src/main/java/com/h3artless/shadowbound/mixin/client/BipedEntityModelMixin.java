package com.h3artless.shadowbound.mixin.client;

import com.h3artless.shadowbound.client.cast.ClientCastPoses;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin<T extends LivingEntity> {
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void shadowbound$point(T entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity player) || !ClientCastPoses.isPointing(player)) return;
        boolean right = player.getMainArm() == Arm.RIGHT;
        ModelPart point = right ? rightArm : leftArm;
        ModelPart other = right ? leftArm : rightArm;
        float tremble = MathHelper.sin(age * 0.45f) * 0.025f;
        point.pitch = head.pitch - 1.5707964f + tremble;
        point.yaw = head.yaw + (right ? -0.1f : 0.1f);
        point.roll = 0f;
        other.pitch = -0.55f + MathHelper.sin(age * 0.1f) * 0.05f;
        other.yaw = right ? 0.45f : -0.45f;
        other.roll = right ? -0.25f : 0.25f;
    }
}
