package com.h3artless.shadowbound.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.GlBlendState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

import static com.h3artless.shadowbound.client.render.RitualFx.*;

public final class SoulPylonFx {
    private static final float[] SOUL = {0.96f, 0.86f, 0.86f};
    private static final float[] ASH = {0.2f, 0.19f, 0.2f};

    private record Job(Vec3d pos, float t, int souls, int level, float sinceBurn, float sink, float tipY,
                       @Nullable Vec3d owner, int seed) {}

    private static final List<Job> QUEUE = new ArrayList<>();

    private SoulPylonFx() {}

    public static void queue(Vec3d pos, float t, int souls, int level, float sinceBurn, float sink, float tipY,
                             @Nullable Vec3d owner, int seed) {
        QUEUE.add(new Job(pos, t, souls, level, sinceBurn, sink, tipY, owner, seed));
    }

    public static void draw(WorldRenderContext ctx) {
        if (QUEUE.isEmpty()) return;
        if (ShadowShaders.sigil == null || ShadowShaders.sprite == null || ShadowShaders.spriteAlpha == null || ShadowShaders.orb == null) {
            QUEUE.clear();
            return;
        }
        MatrixStack matrices = ctx.matrixStack();
        Vec3d cam = ctx.camera().getPos();
        Quaternionf camRot = ctx.camera().getRotation();

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        for (Job job : QUEUE) {
            double d2 = FxQuality.distSq(job.pos.x, job.pos.y, job.pos.z);
            if (d2 > 96 * 96) continue;
            RitualFx.detail = FxQuality.density(d2);
            matrices.push();
            matrices.translate(job.pos.x - cam.x, job.pos.y - cam.y, job.pos.z - cam.z);
            Matrix4f m = matrices.peek().getPositionMatrix();
            RitualFx.begin();
            build(job, RitualFx.decals(), RitualFx.items());
            drawDecals(m, RitualFx.decals());
            drawItems(m, RitualFx.items(), cam.subtract(job.pos), camRot);
            matrices.pop();
        }
        QUEUE.clear();
        RitualFx.begin();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        new GlBlendState().enable();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static float sm(float x) {
        x = MathHelper.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    private static float hash(int n) {
        n = (n << 13) ^ n;
        n = n * (n * n * 15731 + 789221) + 1376312589;
        return (n & 0x7fffffff) / (float) 0x7fffffff;
    }

    private static void build(Job j, List<Decal> decals, List<Item> items) {
        float t = j.t;
        float vis = MathHelper.clamp(1f - j.sink, 0f, 1f);
        float yOff = -2.9f * Math.min(j.sink, 1f);
        float pulse = 0.85f + 0.15f * MathHelper.sin(t * 0.15f);
        float TAU = (float) (Math.PI * 2);
        float tip = j.tipY;

        decals.add(new Decal(SPRITES, GLOW, 0.02f, 2.3f, 0, 2, 1, RED[0], RED[1], RED[2], 0.38f * vis * pulse));
        decals.add(new Decal(SPRITES, RING_CELL, 0.03f, 1.25f + 0.08f * MathHelper.sin(t * 0.1f), 0, 2, 1, RED[0], RED[1], RED[2], 0.8f * vis));

        if (tip > -1f) {
            sprite(items, 0, tip, 0, (1.1f + 0.25f * MathHelper.sin(t * 0.2f)) * vis, GLOW, RED, 0.5f * vis, true);
        }

        if (j.owner != null && vis > 0.05f && tip > -1f) {
            int segs = 14;
            float[] pts = new float[(segs + 1) * 3];
            float ox = (float) j.owner.x, oy = (float) j.owner.y, oz = (float) j.owner.z;
            for (int s = 0; s <= segs; s++) {
                float u = s / (float) segs;
                float bell = MathHelper.sin(u * (float) Math.PI);
                pts[s * 3] = MathHelper.lerp(u, 0, ox) + 0.12f * bell * MathHelper.sin(t * 0.21f + u * 9f);
                pts[s * 3 + 1] = MathHelper.lerp(u, tip, oy) + bell * 0.7f + 0.1f * bell * MathHelper.sin(t * 0.17f + u * 7f);
                pts[s * 3 + 2] = MathHelper.lerp(u, 0, oz) + 0.12f * bell * MathHelper.cos(t * 0.19f + u * 8f);
            }
            beam(items, pts, 0.22f, RED, 0.22f * vis);
            beam(items, pts, 0.05f, EMBER, 0.35f * vis);
            for (int k = 0; k < 3; k++) {
                float u = fract(t / 30f + k / 3f);
                float fu = u * segs;
                int i0 = Math.min(segs - 1, MathHelper.floor(fu));
                float f = fu - i0;
                float px = MathHelper.lerp(f, pts[i0 * 3], pts[i0 * 3 + 3]);
                float py = MathHelper.lerp(f, pts[i0 * 3 + 1], pts[i0 * 3 + 4]);
                float pz = MathHelper.lerp(f, pts[i0 * 3 + 2], pts[i0 * 3 + 5]);
                float pa = MathHelper.sin(u * (float) Math.PI) * vis;
                sprite(items, px, py, pz, 0.35f, GLOW, RED, 0.6f * pa, true);
                sprite(items, px, py, pz, 0.1f, SPARK, HOT, 0.9f * pa, true);
            }
            sprite(items, ox, oy, oz, 0.9f + 0.1f * MathHelper.sin(t * 0.3f), GLOW, RED, 0.25f * vis, true);
        }

        int life = 50, every = RitualFx.detail > 0.8f ? 2 : RitualFx.detail > 0.5f ? 3 : 5;
        for (int k = MathHelper.floor((t - life) / every); k <= MathHelper.floor(t / every); k++) {
            float age = t - k * every;
            if (age < 0 || age > life) continue;
            int id = k * 31 + j.seed * 7;
            float a0 = hash(id) * TAU + age * 0.03f;
            float r = 0.35f + 0.8f * hash(id + 1);
            float y = 0.05f + age * (0.035f + 0.02f * hash(id + 2)) + yOff * 0.3f;
            float a = sm(age / 6f) * (1 - sm((age - life * 0.6f) / (life * 0.4f))) * vis;
            sprite(items, MathHelper.cos(a0) * r, y, MathHelper.sin(a0) * r, 0.07f, SPARK, mix(HOT, RED, age / life), a, true);
        }

        for (int k = 0, puffs = Math.max(2, Math.round(7 * RitualFx.detail)); k < puffs; k++) {
            float cyc = (t + hash(k + j.seed) * 120f) / 120f;
            float ph = fract(cyc);
            int gen = MathHelper.floor(cyc);
            float a0 = hash(k * 13 + gen) * TAU + ph;
            float r = 0.6f + 0.6f * hash(k * 7 + gen);
            sprite(items, MathHelper.cos(a0) * r, 0.15f + ph * 0.9f, MathHelper.sin(a0) * r, 0.5f + ph * 0.5f,
                    PUFF + (k & 3), mix(ASH, RED, 0.3f * (1 - ph)), MathHelper.sin(ph * (float) Math.PI) * 0.4f * vis, false, k + ph);
        }

        int n = j.souls;
        for (int i = 0; i < n; i++) {
            float ang = i / (float) n * TAU + t * 0.045f;
            float y = 1.3f + 0.22f * MathHelper.sin(t * 0.07f + i * 1.9f) + yOff;
            float x = MathHelper.cos(ang) * 1.15f, z = MathHelper.sin(ang) * 1.15f;
            sprite(items, x, y, z, 0.36f * vis, GLOW, SOUL, 0.55f * vis, true);
            sprite(items, x, y, z, 0.12f * vis, SPARK, HOT, 0.9f * vis, true);
            float pa = ang - 0.18f;
            sprite(items, MathHelper.cos(pa) * 1.15f, y, MathHelper.sin(pa) * 1.15f, 0.2f * vis, GLOW, RED, 0.25f * vis, true);
        }

        float b = j.sinceBurn;
        if (b >= 0 && b <= 20) {
            float ang = n / (float) (n + 1) * TAU + (t - b) * 0.045f;
            float sx = MathHelper.cos(ang) * 1.15f, sz = MathHelper.sin(ang) * 1.15f, sy = 1.3f + yOff;
            if (b < 8) {
                float k = sm(b / 8f);
                sprite(items, MathHelper.lerp(k, sx, 0), MathHelper.lerp(k, sy, tip), MathHelper.lerp(k, sz, 0),
                        0.4f + 0.3f * k, GLOW, SOUL, 0.8f, true);
            }
            if (b >= 6 && j.owner != null) {
                float k = (b - 6) / 14f;
                float a = (b < 9 ? 1f : 1f - k) * vis;
                int segs = 10, flick = MathHelper.floor(t);
                float[] pts = new float[(segs + 1) * 3];
                for (int s = 0; s <= segs; s++) {
                    float u = s / (float) segs;
                    float jit = (s == 0 || s == segs) ? 0 : 0.35f * MathHelper.sin(u * (float) Math.PI);
                    pts[s * 3] = MathHelper.lerp(u, 0, (float) j.owner.x) + (hash(flick * 97 + s * 3) - 0.5f) * jit;
                    pts[s * 3 + 1] = MathHelper.lerp(u, tip, (float) j.owner.y) + (hash(flick * 89 + s * 5) - 0.5f) * jit
                            + MathHelper.sin(u * (float) Math.PI) * 0.5f;
                    pts[s * 3 + 2] = MathHelper.lerp(u, 0, (float) j.owner.z) + (hash(flick * 83 + s * 7) - 0.5f) * jit;
                }
                beam(items, pts, 0.42f, RED, a * 0.85f);
                beam(items, pts, 0.12f, EMBER, a);
                beam(items, pts, 0.04f, HOT, a);
                sprite(items, (float) j.owner.x, (float) j.owner.y, (float) j.owner.z, 1.3f * (0.6f + 0.6f * k), GLOW, RED, a * 0.8f, true);
                sprite(items, (float) j.owner.x, (float) j.owner.y, (float) j.owner.z, 0.9f * (0.3f + k), RING_CELL, mix(HOT, RED, k), a, true);
            }
            if (b < 10) sprite(items, 0, tip, 0, 1.6f * (1 - b / 10f) + 0.4f, GLOW, HOT, (1 - b / 10f) * 0.9f, true);
        }
    }
}
