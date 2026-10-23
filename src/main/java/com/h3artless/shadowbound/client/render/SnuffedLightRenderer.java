package com.h3artless.shadowbound.client.render;

import com.h3artless.shadowbound.block.SnuffedLightBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

public class SnuffedLightRenderer implements BlockEntityRenderer<SnuffedLightBlockEntity> {
    public SnuffedLightRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(SnuffedLightBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider buffers, int light, int overlay) {
        BlockState state = be.getStored();
        if (state.isAir()) return;
        MinecraftClient.getInstance().getBlockRenderManager()
                .renderBlockAsEntity(state, matrices, buffers, light, OverlayTexture.DEFAULT_UV);
    }

    @Override
    public int getRenderDistance() {
        return FxQuality.low() ? 32 : 64;
    }
}
