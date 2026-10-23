package com.h3artless.shadowbound.mixin.client;

import com.h3artless.shadowbound.client.render.BladeModels;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("HEAD"), cancellable = true)
    private void shadowbound$heldModel(ItemStack stack, ModelTransformationMode mode, boolean leftHanded, MatrixStack matrices,
                                       VertexConsumerProvider buffers, int light, int overlay, BakedModel model, CallbackInfo ci) {
        if (stack.isEmpty() || BladeModels.usesFlatModel(mode)) return;
        BakedModel held = BladeModels.heldModelFor(stack);
        if (held == null || held == model) return;
        ci.cancel();
        ((ItemRenderer) (Object) this).renderItem(stack, mode, leftHanded, matrices, buffers, light, overlay, held);
    }
}
