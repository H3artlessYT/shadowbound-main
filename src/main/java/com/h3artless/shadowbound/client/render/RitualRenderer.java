package com.h3artless.shadowbound.client.render;

import com.h3artless.shadowbound.entity.RitualEntity;
import com.h3artless.shadowbound.ritual.RitualTimeline;
import com.h3artless.shadowbound.ritual.ShadowRitual;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class RitualRenderer extends EntityRenderer<RitualEntity> {
    private static final Map<ShadowRitual, List<ItemStack>> OFFERINGS = new EnumMap<>(ShadowRitual.class);
    private final ItemRenderer items;

    public RitualRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.items = ctx.getItemRenderer();
        this.shadowRadius = 0f;
    }

    @Override
    public boolean shouldRender(RitualEntity entity, Frustum frustum, double x, double y, double z) {
        return entity.squaredDistanceTo(x, y, z) < 128 * 128;
    }

    @Override
    public void render(RitualEntity ritual, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider buffers, int light) {
        float t = ritual.getTicks() + tickDelta;
        ShadowRitual kind = ritual.getRitual();
        List<ItemStack> offerings = offerings(kind);
        int n = offerings.size();
        RitualFx.queue(ritual.getLerpedPos(tickDelta), t, n, kind.accent);

        int bright = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        if (t < RitualTimeline.REVEAL) {
            for (int i = 0; i < n; i++) {
                RitualTimeline.Offering o = RitualTimeline.offering(i, n, t);
                if (o.gone() || o.scale() <= 0.01f) continue;
                matrices.push();
                matrices.translate(o.x(), o.y() - 0.15f * o.scale(), o.z());
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 4f + i * 40f));
                float s = 0.9f * o.scale();
                matrices.scale(s, s, s);
                items.renderItem(offerings.get(i), ModelTransformationMode.GROUND, bright, OverlayTexture.DEFAULT_UV,
                        matrices, buffers, ritual.getWorld(), ritual.getId() * 31 + i);
                matrices.pop();
            }
        } else {
            float age = t - RitualTimeline.REVEAL;
            float k = RitualTimeline.sm(age / 15f);
            float bob = MathHelper.sin(t * 0.08f) * 0.08f;
            matrices.push();
            matrices.translate(0, 1.2f + bob + 0.3f * k, 0);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 3f));
            float s = 0.3f + 1.5f * k;
            matrices.scale(s, s, s);
            items.renderItem(kind.result(), ModelTransformationMode.GROUND, bright, OverlayTexture.DEFAULT_UV,
                    matrices, buffers, ritual.getWorld(), ritual.getId());
            matrices.pop();
        }

        super.render(ritual, yaw, tickDelta, matrices, buffers, light);
    }

    static List<ItemStack> offerings(ShadowRitual ritual) {
        return OFFERINGS.computeIfAbsent(ritual, r -> {
            List<ItemStack> out = new ArrayList<>();
            for (ShadowRitual.Part part : r.parts) {
                for (int i = 0; i < part.count(); i++) out.add(new ItemStack(part.item()));
            }
            return out;
        });
    }

    @Override
    public Identifier getTexture(RitualEntity entity) {
        return RitualFx.SPRITES;
    }
}
