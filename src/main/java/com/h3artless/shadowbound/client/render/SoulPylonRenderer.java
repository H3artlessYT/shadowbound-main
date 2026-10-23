package com.h3artless.shadowbound.client.render;

import com.h3artless.shadowbound.entity.SoulPylonEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class SoulPylonRenderer extends EntityRenderer<SoulPylonEntity> {
    private static final float TAU = (float) (Math.PI * 2);
    private static final int SIDES = 6;
    private static final float PANEL = 0.5f;

    public SoulPylonRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.6f;
    }

    @Override
    public boolean shouldRender(SoulPylonEntity entity, Frustum frustum, double x, double y, double z) {
        return entity.squaredDistanceTo(x, y, z) < 96 * 96;
    }

    public static float sink(SoulPylonEntity p, float t) {
        float rise = sm(t / SoulPylonEntity.RISE);
        int collapseAt = p.getCollapseAt();
        float col = collapseAt >= 0 ? sm((t - collapseAt) / SoulPylonEntity.COLLAPSE) : 0f;
        return (1f - rise) + col;
    }

    @Override
    public void render(SoulPylonEntity pylon, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider buffers, int light) {
        float t = pylon.age + tickDelta;
        float sink = sink(pylon, t);
        float vis = MathHelper.clamp(1f - sink, 0f, 1f);
        float yOff = -2.9f * Math.min(sink, 1f);
        float bob = MathHelper.sin(t * 0.06f) * 0.06f;
        float sinceBurn = t - pylon.lastBurnAge;
        int seed = pylon.getId();
        double d2 = FxQuality.distSq(pylon);
        int segs = FxQuality.low() || d2 > 32 * 32 ? 24 : 48;

        float flare = sinceBurn >= 0 && sinceBurn < 16 ? 1f - sinceBurn / 16f : 0f;
        float glow = MathHelper.clamp((0.45f + 0.15f * MathHelper.sin(t * 0.15f)) * vis + flare * 0.8f, 0f, 1.5f);
        int full = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        int stoneLight = LightmapTextureManager.pack(Math.max(4, LightmapTextureManager.getBlockLightCoordinates(light)),
                LightmapTextureManager.getSkyLightCoordinates(light));

        VertexConsumer stone = buffers.getBuffer(PylonLayers.stone());
        float total = 3.4f;

        matrices.push();
        matrices.translate(0, yOff * 0.35f, 0);
        prism(stone, matrices.peek(), 0f, 0.16f, 0.66f, 0.6f, 0f, glow * 0.5f, 0f, 0.05f / total, seed, stoneLight, 0.25f);
        prism(stone, matrices.peek(), 0.16f, 0.3f, 0.52f, 0.46f, TAU / 12f, glow * 0.6f, 0.05f / total, 0.09f / total, seed + 1, stoneLight, 0.25f);
        matrices.pop();

        float[] heights = {0.66f, 0.58f, 0.5f};
        float[] widths = {0.4f, 0.34f, 0.28f};
        float y = 0.42f;
        for (int i = 0; i < 3; i++) {
            float h = heights[i], r = widths[i];
            float gap = 0.1f + 0.05f * MathHelper.sin(t * 0.08f + i);
            float spin = (t * (i % 2 == 0 ? 1.1f : -1.6f) + i * 30f) * MathHelper.RADIANS_PER_DEGREE;
            matrices.push();
            matrices.translate(0, y + yOff + bob * (i + 1), 0);
            segment(stone, matrices.peek(), h, r, spin, glow, y / total, (y + h) / total, seed + 10 * i, stoneLight);
            matrices.pop();
            y += h + gap;
        }

        float tipY = y + 0.3f + yOff + bob * 4;
        float charge = MathHelper.clamp(0.5f + flare + 0.1f * pylon.getSouls(), 0f, 1.6f);
        VertexConsumer crystal = buffers.getBuffer(PylonLayers.crystal());
        matrices.push();
        matrices.translate(0, tipY, 0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(t * 0.05f));
        float cs = 1f + 0.05f * MathHelper.sin(t * 0.2f) + 0.15f * flare;
        matrices.scale(cs, cs, cs);
        bipyramid(crystal, matrices.peek(), 0.24f, 0.5f, 0.32f, charge, 0f, vis, full);
        bipyramid(crystal, matrices.peek(), 0.09f, 0.22f, 0.15f, charge, 1f, vis, full);
        matrices.pop();

        VertexConsumer rune = buffers.getBuffer(PylonLayers.rune());
        float ringA = vis * (0.75f + 0.25f * flare);
        matrices.push();
        matrices.translate(0, 1.45f + yOff, 0);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotation(0.22f + 0.05f * MathHelper.sin(t * 0.05f)));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(t * 0.035f));
        ring(rune, matrices.peek().getPositionMatrix(), segs, 0.95f, 0.3f, 0.85f, 0.08f, 0.1f, ringA);
        matrices.pop();
        matrices.push();
        matrices.translate(0, 2.05f + yOff, 0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotation(-0.3f + 0.05f * MathHelper.sin(t * 0.04f + 1f)));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-t * 0.05f));
        ring(rune, matrices.peek().getPositionMatrix(), segs, 0.7f, 0.22f, 0.9f, 0.15f, 0.12f, ringA * 0.85f);
        matrices.pop();

        Vec3d owner = null;
        if (pylon.getOwnerUuid() != null && pylon.getWorld() != null) {
            PlayerEntity p = pylon.getWorld().getPlayerByUuid(pylon.getOwnerUuid());
            if (p != null && p.squaredDistanceTo(pylon) <= SoulPylonEntity.OWNER_RANGE * SoulPylonEntity.OWNER_RANGE) {
                owner = p.getLerpedPos(tickDelta).add(0, p.getHeight() * 0.6, 0).subtract(pylon.getLerpedPos(tickDelta));
            }
        }
        if (owner != null && vis > 0.01f && d2 < 64 * 64) {
            VertexConsumer column = buffers.getBuffer(PylonLayers.column());
            matrices.push();
            matrices.translate(0, yOff * 0.5f, 0);
            Matrix4f m = matrices.peek().getPositionMatrix();
            column(column, m, segs * 2 / 3, 0.95f, 0.75f, 3.6f, 0.35f * vis + 0.4f * flare);
            if (!FxQuality.low()) column(column, m, segs / 2, 0.6f, 0.45f, 3.0f, 0.2f * vis + 0.3f * flare);
            matrices.pop();
        }

        SoulPylonFx.queue(pylon.getLerpedPos(tickDelta), t, pylon.getSouls(), pylon.getLevel(), sinceBurn,
                sink, tipY, owner, seed);

        super.render(pylon, yaw, tickDelta, matrices, buffers, light);
    }

    private static void segment(VertexConsumer vc, MatrixStack.Entry e, float h, float r, float rot, float glow,
                                float h0, float h1, int seed, int light) {
        float c = h * 0.14f;
        float in = r * 0.78f;
        float hm0 = MathHelper.lerp(0.14f, h0, h1), hm1 = MathHelper.lerp(0.86f, h0, h1);
        prism(vc, e, 0f, c, in, r, rot, glow * 0.7f, h0, hm0, seed, light, 0.12f);
        prism(vc, e, c, h - c, r, r, rot, glow, hm0, hm1, seed, light, -1f);
        prism(vc, e, h - c, h, r, in, rot, glow * 0.7f, hm1, h1, seed, light, 0.12f);
        cap(vc, e, 0f, in, rot, -1, glow * 0.3f, h0, seed, light);
        cap(vc, e, h, in, rot, 1, glow * 0.3f, h1, seed, light);
    }

    private static void prism(VertexConsumer vc, MatrixStack.Entry e, float y0, float y1, float r0, float r1, float rot,
                              float glow, float h0, float h1, int seed, int light, float vSpan) {
        Matrix4f m = e.getPositionMatrix();
        Matrix3f n = e.getNormalMatrix();
        float sd = (seed * 0.137f) % 1f;
        float va = vSpan < 0 ? 0f : 0.02f, vb = vSpan < 0 ? PANEL : 0.02f + vSpan * PANEL;
        for (int s = 0; s < SIDES; s++) {
            float a0 = rot + s * TAU / SIDES, a1 = rot + (s + 1) * TAU / SIDES;
            float c0 = MathHelper.cos(a0), s0 = MathHelper.sin(a0), c1 = MathHelper.cos(a1), s1 = MathHelper.sin(a1);
            float mid = (a0 + a1) * 0.5f;
            float slope = (r0 - r1) / Math.max(0.001f, y1 - y0);
            float nx = MathHelper.cos(mid), nz = MathHelper.sin(mid), ny = slope * MathHelper.cos(TAU / SIDES / 2f);
            float len = MathHelper.sqrt(nx * nx + ny * ny + nz * nz);
            nx /= len; ny /= len; nz /= len;

            put(vc, m, n, c0 * r0, y0, s0 * r0, 0f, vb, glow, h0, sd, 1f, light, nx, ny, nz);
            put(vc, m, n, c1 * r0, y0, s1 * r0, PANEL, vb, glow, h0, sd, 1f, light, nx, ny, nz);
            put(vc, m, n, c1 * r1, y1, s1 * r1, PANEL, va, glow, h1, sd, 1f, light, nx, ny, nz);
            put(vc, m, n, c0 * r1, y1, s0 * r1, 0f, va, glow, h1, sd, 1f, light, nx, ny, nz);
        }
    }

    private static void cap(VertexConsumer vc, MatrixStack.Entry e, float y, float r, float rot, int dir,
                            float glow, float h, int seed, int light) {
        Matrix4f m = e.getPositionMatrix();
        Matrix3f n = e.getNormalMatrix();
        float sd = (seed * 0.137f) % 1f;
        float cu = PANEL * 0.5f, cr = PANEL * 0.42f;
        for (int s = 0; s < SIDES; s++) {
            int a = dir > 0 ? s : s + 1, b = dir > 0 ? s + 1 : s;
            float a0 = rot + a * TAU / SIDES, a1 = rot + b * TAU / SIDES;
            float x0 = MathHelper.cos(a0), z0 = MathHelper.sin(a0), x1 = MathHelper.cos(a1), z1 = MathHelper.sin(a1);
            put(vc, m, n, 0, y, 0, cu, cu, glow, h, sd, 1f, light, 0, dir, 0);
            put(vc, m, n, 0, y, 0, cu, cu, glow, h, sd, 1f, light, 0, dir, 0);
            put(vc, m, n, x1 * r, y, z1 * r, cu + x1 * cr, cu + z1 * cr, glow, h, sd, 1f, light, 0, dir, 0);
            put(vc, m, n, x0 * r, y, z0 * r, cu + x0 * cr, cu + z0 * cr, glow, h, sd, 1f, light, 0, dir, 0);
        }
    }

    private static void bipyramid(VertexConsumer vc, MatrixStack.Entry e, float r, float top, float bottom,
                                  float charge, float core, float alpha, int light) {
        Matrix4f m = e.getPositionMatrix();
        Matrix3f n = e.getNormalMatrix();
        for (int s = 0; s < SIDES; s++) {
            float a0 = s * TAU / SIDES, a1 = (s + 1) * TAU / SIDES, mid = (a0 + a1) * 0.5f;
            float x0 = MathHelper.cos(a0) * r, z0 = MathHelper.sin(a0) * r, x1 = MathHelper.cos(a1) * r, z1 = MathHelper.sin(a1) * r;
            float ex = MathHelper.cos(mid), ez = MathHelper.sin(mid);
            float fs = s / (float) SIDES;
            float apo = r * MathHelper.cos(TAU / SIDES / 2f);
            float l = MathHelper.sqrt(top * top + apo * apo);
            float nx = ex * top / l, ny = apo / l, nz = ez * top / l;
            put(vc, m, n, 0, top, 0, 0.5f, 1f, charge, fs, core, alpha, light, nx, ny, nz);
            put(vc, m, n, 0, top, 0, 0.5f, 1f, charge, fs, core, alpha, light, nx, ny, nz);
            put(vc, m, n, x1, 0, z1, 1f, 0f, charge, fs, core, alpha, light, nx, ny, nz);
            put(vc, m, n, x0, 0, z0, 0f, 0f, charge, fs, core, alpha, light, nx, ny, nz);
            l = MathHelper.sqrt(bottom * bottom + apo * apo);
            nx = ex * bottom / l; ny = -apo / l; nz = ez * bottom / l;
            put(vc, m, n, x0, 0, z0, 0f, 0f, charge, fs, core, alpha, light, nx, ny, nz);
            put(vc, m, n, x1, 0, z1, 1f, 0f, charge, fs, core, alpha, light, nx, ny, nz);
            put(vc, m, n, 0, -bottom, 0, 0.5f, -0.7f, charge, fs, core, alpha, light, nx, ny, nz);
            put(vc, m, n, 0, -bottom, 0, 0.5f, -0.7f, charge, fs, core, alpha, light, nx, ny, nz);
        }
    }

    private static void put(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v,
                            float r, float g, float b, float a, int light, float nx, float ny, float nz) {
        vc.vertex(m, x, y, z)
                .color(MathHelper.clamp(r, 0f, 1f), MathHelper.clamp(g, 0f, 1f), MathHelper.clamp(b, 0f, 1f), MathHelper.clamp(a, 0f, 1f))
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(n, nx, ny, nz)
                .next();
    }

    private static void ring(VertexConsumer vc, Matrix4f m, int segs, float radius, float width, float red, float green, float blue, float a) {
        for (int s = 0; s < segs; s++) {
            float a0 = s * TAU / segs, a1 = (s + 1) * TAU / segs;
            float u0 = 2f * s / segs, u1 = 2f * (s + 1) / segs;
            float c0 = MathHelper.cos(a0), s0 = MathHelper.sin(a0), c1 = MathHelper.cos(a1), s1 = MathHelper.sin(a1);
            float hw = width * 0.5f;
            vc.vertex(m, c0 * radius, -hw, s0 * radius).texture(u0, 1f).color(red, green, blue, a).next();
            vc.vertex(m, c1 * radius, -hw, s1 * radius).texture(u1, 1f).color(red, green, blue, a).next();
            vc.vertex(m, c1 * radius, hw, s1 * radius).texture(u1, 0f).color(red, green, blue, a).next();
            vc.vertex(m, c0 * radius, hw, s0 * radius).texture(u0, 0f).color(red, green, blue, a).next();
        }
    }

    private static void column(VertexConsumer vc, Matrix4f m, int segs, float r0, float r1, float h, float a) {
        a = MathHelper.clamp(a, 0f, 1f);
        for (int s = 0; s < segs; s++) {
            float a0 = s * TAU / segs, a1 = (s + 1) * TAU / segs;
            float u0 = s / (float) segs, u1 = (s + 1) / (float) segs;
            float c0 = MathHelper.cos(a0), s0 = MathHelper.sin(a0), c1 = MathHelper.cos(a1), s1 = MathHelper.sin(a1);
            vc.vertex(m, c0 * r0, 0f, s0 * r0).texture(u0, 0f).color(0.85f, 0.06f, 0.08f, a).next();
            vc.vertex(m, c1 * r0, 0f, s1 * r0).texture(u1, 0f).color(0.85f, 0.06f, 0.08f, a).next();
            vc.vertex(m, c1 * r1, h, s1 * r1).texture(u1, 1f).color(0.85f, 0.06f, 0.08f, a).next();
            vc.vertex(m, c0 * r1, h, s0 * r1).texture(u0, 1f).color(0.85f, 0.06f, 0.08f, a).next();
        }
    }

    static float sm(float x) {
        x = MathHelper.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    @Override
    public Identifier getTexture(SoulPylonEntity entity) {
        return PylonLayers.TEXTURE;
    }
}
