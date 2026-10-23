package com.h3artless.shadowbound.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public final class ShadowWisps {
    private static final int SEGMENTS = 8;
    private static final float CYCLE_SPEED = 0.42f;

    private ShadowWisps() {}

    public static void draw(LivingEntity e, float tickDelta, MatrixStack matrices, VertexConsumerProvider buffers) {
        VertexConsumer out = buffers.getBuffer(SmokeLayers.wisp());
        Matrix4f m = matrices.peek().getPositionMatrix();

        float w = e.getWidth();
        float h = e.getHeight();
        int count = Math.max(1, Math.round(MathHelper.clamp(3 + Math.round(w * 4f), 4, 8) * FxQuality.density(FxQuality.distSq(e))));
        int segments = FxQuality.low() ? 4 : SEGMENTS;
        float time = (e.age + tickDelta) / 20f;
        float seed = MathHelper.fractionalPart(e.getId() * 0.618034f);

        float camYaw = MinecraftClient.getInstance().gameRenderer.getCamera().getYaw() * MathHelper.RADIANS_PER_DEGREE;
        float rx = MathHelper.cos(camYaw), rz = MathHelper.sin(camYaw);

        for (int i = 0; i < count; i++) {
            float cyc = time * CYCLE_SPEED + i / (float) count + seed * 3.1f;
            float phase = MathHelper.fractionalPart(cyc);
            int gen = MathHelper.floor(cyc);

            float ang = hash(gen * 7 + i * 13 + e.getId()) * (float) (Math.PI * 2);
            float baseY = h * (0.3f + 0.6f * hash(gen * 11 + i * 3 + e.getId() * 5));
            float baseR = w * 0.45f;
            float bx = MathHelper.cos(ang) * baseR, bz = MathHelper.sin(ang) * baseR;
            float ox = MathHelper.cos(ang), oz = MathHelper.sin(ang);

            float rise = 0.9f + h * 0.5f;
            float head = phase * rise;
            float tail = Math.max(0f, phase - 0.4f) * rise * 1.4f;
            if (head - tail < 0.02f) continue;

            float wob = i * 2.3f + seed * 10f;
            float px0 = 0, py0 = 0, pz0 = 0, hw0 = 0;
            for (int s = 0; s <= segments; s++) {
                float k = s / (float) segments;
                float d = MathHelper.lerp(k, tail, head);
                float curl = MathHelper.sin(time * 1.8f + d * 3.5f + wob) * 0.16f * d;
                float px = bx + ox * (0.08f + 0.28f * d) + (-oz) * curl;
                float pz = bz + oz * (0.08f + 0.28f * d) + ox * curl;
                float py = baseY + d;
                float hw = 0.07f + 0.17f * d;

                if (s > 0) {
                    float v0 = (s - 1) / (float) segments, v1 = k;
                    quad(out, m, px0, py0, pz0, hw0, px, py, pz, hw, rx, rz, v0, v1, seed + i * 0.13f, phase);
                }
                px0 = px; py0 = py; pz0 = pz; hw0 = hw;
            }
        }
    }

    private static void quad(VertexConsumer out, Matrix4f m,
                             float x0, float y0, float z0, float w0,
                             float x1, float y1, float z1, float w1,
                             float rx, float rz, float v0, float v1, float seed, float phase) {
        float s = MathHelper.fractionalPart(seed);
        out.vertex(m, x0 - rx * w0, y0, z0 - rz * w0).texture(0, v0).color(s, phase, 0f, 1f).next();
        out.vertex(m, x0 + rx * w0, y0, z0 + rz * w0).texture(1, v0).color(s, phase, 0f, 1f).next();
        out.vertex(m, x1 + rx * w1, y1, z1 + rz * w1).texture(1, v1).color(s, phase, 0f, 1f).next();
        out.vertex(m, x1 - rx * w1, y1, z1 - rz * w1).texture(0, v1).color(s, phase, 0f, 1f).next();
    }

    private static float hash(int n) {
        n = (n << 13) ^ n;
        n = n * (n * n * 15731 + 789221) + 1376312589;
        return (n & 0x7fffffff) / (float) 0x7fffffff;
    }
}
