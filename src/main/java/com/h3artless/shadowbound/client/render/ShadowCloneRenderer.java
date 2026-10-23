package com.h3artless.shadowbound.client.render;

import com.h3artless.shadowbound.entity.ShadowCloneEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.ArmorEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class ShadowCloneRenderer extends BipedEntityRenderer<ShadowCloneEntity, PlayerEntityModel<ShadowCloneEntity>> {
    public ShadowCloneRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);
        this.addFeature(new ArmorFeatureRenderer<>(this,
                new ArmorEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_INNER_ARMOR)),
                new ArmorEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
    }

    @Override
    public void render(ShadowCloneEntity clone, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider buffers, int light) {
        PlayerEntityModel<ShadowCloneEntity> model = getModel();
        model.rightArmPose = clone.getMainHandStack().isEmpty() ? BipedEntityModel.ArmPose.EMPTY : BipedEntityModel.ArmPose.ITEM;
        model.leftArmPose = BipedEntityModel.ArmPose.EMPTY;
        model.sneaking = clone.isInSneakingPose();

        super.render(clone, yaw, tickDelta, matrices, buffers, light);
    }

    @Override
    public Vec3d getPositionOffset(ShadowCloneEntity clone, float tickDelta) {
        return clone.isInSneakingPose() ? new Vec3d(0.0, -0.125, 0.0) : super.getPositionOffset(clone, tickDelta);
    }

    @Override
    protected boolean hasLabel(ShadowCloneEntity entity) {
        return false;
    }

    @Override
    public Identifier getTexture(ShadowCloneEntity clone) {
        UUID owner = clone.getOwnerUuid();
        if (owner == null) return DefaultSkinHelper.getTexture();
        ClientPlayNetworkHandler net = MinecraftClient.getInstance().getNetworkHandler();
        PlayerListEntry entry = net == null ? null : net.getPlayerListEntry(owner);
        return entry != null ? entry.getSkinTexture() : DefaultSkinHelper.getTexture(owner);
    }
}
