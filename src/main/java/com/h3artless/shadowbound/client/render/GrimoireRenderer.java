package com.h3artless.shadowbound.client.render;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.client.cast.AirCasting;
import com.h3artless.shadowbound.entity.GrimoireEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BookModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class GrimoireRenderer extends EntityRenderer<GrimoireEntity> {
    public static final Identifier TEXTURE = Shadowbound.id("textures/entity/shadow_grimoire_book.png");

    private final BookModel model;

    public GrimoireRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.model = new BookModel(ctx.getPart(EntityModelLayers.BOOK));
        this.shadowRadius = 0.3f;
        this.shadowOpacity = 0.6f;
    }

    @Override
    public boolean shouldRender(GrimoireEntity entity, Frustum frustum, double x, double y, double z) {
        return entity.squaredDistanceTo(x, y, z) < 64 * 64 && super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(GrimoireEntity book, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider buffers, int light) {
        float t = book.age + tickDelta;
        float form = MathHelper.clamp(t / 18f, 0f, 1f);
        form = form * form * (3f - 2f * form);
        boolean casting = AirCasting.isCasting(book.getId());

        PlayerEntity me = MinecraftClient.getInstance().player;
        float near = 0f;
        if (me != null) near = 1f - MathHelper.clamp(((float) me.squaredDistanceTo(book) - 4f) / 32f, 0f, 1f);
        float since = t - book.getFizzleAge();
        float fizz = since >= 0 && since < 14 ? 1f - since / 14f : 0f;
        float glow = casting ? 0.85f + 0.15f * MathHelper.sin(t * 0.5f)
                : MathHelper.clamp(0.25f + 0.1f * MathHelper.sin(t * 0.12f) + 0.45f * near - 0.3f * fizz, 0f, 1f);

        float open = MathHelper.lerp(tickDelta, book.prevOpen, book.open);
        float page = MathHelper.lerp(tickDelta, book.prevPage, book.page);
        float turn = book.prevTurn + MathHelper.wrapDegrees((book.turn - book.prevTurn) * MathHelper.DEGREES_PER_RADIAN)
                * MathHelper.RADIANS_PER_DEGREE * tickDelta;
        float lift = casting ? 0.2f : 0f;
        float shake = fizz * MathHelper.sin(t * 2.7f) * 0.1f;
        int lit = LightmapTextureManager.pack(Math.max(6, LightmapTextureManager.getBlockLightCoordinates(light)),
                LightmapTextureManager.getSkyLightCoordinates(light));

        matrices.push();
        matrices.translate(0, GrimoireEntity.HOVER + lift + MathHelper.sin(t * 0.1f) * 0.05f, 0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-turn + shake));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(80f));
        float s = 1.25f;
        matrices.scale(s, s, s);
        float left = MathHelper.clamp(MathHelper.fractionalPart(page + 0.25f) * 1.6f - 0.3f, 0f, 1f);
        float right = MathHelper.clamp(MathHelper.fractionalPart(page + 0.75f) * 1.6f - 0.3f, 0f, 1f);
        model.setPageAngles(t, left, right, open);
        model.renderBook(matrices, buffers.getBuffer(BookLayers.book()), lit, OverlayTexture.DEFAULT_UV,
                glow, form, (book.getId() * 0.137f) % 1f, 1f);
        matrices.pop();

        super.render(book, yaw, tickDelta, matrices, buffers, light);
    }

    @Override
    public Identifier getTexture(GrimoireEntity entity) {
        return TEXTURE;
    }
}
