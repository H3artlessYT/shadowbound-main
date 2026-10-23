package com.h3artless.shadowbound.client.render;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.ritual.RitualTimeline;
import com.h3artless.shadowbound.ritual.RitualTimeline.Arc;
import com.h3artless.shadowbound.ritual.RitualTimeline.Offering;
import com.h3artless.shadowbound.ritual.RitualTimeline.Phases;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.GlBlendState;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static com.h3artless.shadowbound.ritual.RitualTimeline.*;

public final class RitualFx {
    public static final Identifier RING = Shadowbound.id("textures/misc/ritual_ring.png");
    public static final Identifier SEAL = Shadowbound.id("textures/misc/ritual_seal.png");
    public static final Identifier SPRITES = Shadowbound.id("textures/misc/ritual_sprites.png");

    static final int GLOW = 0, SPARK = 1, FLAME = 2, RING_CELL = 3, PUFF = 4, GLYPH = 8;

    static final float[] RED = {0.95f, 0.08f, 0.10f};
    static final float[] EMBER = {1.0f, 0.32f, 0.18f};
    static final float[] HOT = {1.0f, 0.86f, 0.80f};

    static final int SPRITE = 0, BEAM = 1, ORB = 2;

    private record Job(Vec3d pos, float t, int offerings, float[] tint) {}

    record Decal(Identifier tex, int cell, float y, float radius, float rot, float progress,
                         float intensity, float r, float g, float b, float a) {}

    static final class Item {
        int kind;
        float x, y, z, size, roll;
        int cell;
        float r, g, b, a;
        boolean add;
        float[] pts;
        float width;
        double dist;
    }

    private static final List<Job> QUEUE = new ArrayList<>();
    private static final List<Decal> DECALS = new ArrayList<>();
    private static final List<Item> ITEMS = new ArrayList<>();
    private static final List<Item> POOL = new ArrayList<>();
    private static int poolUsed;

    private static final float[][] SCRATCH = new float[16][3];
    private static int scratchAt;

    private static final float[] ASH_DARK = {0.22f, 0.21f, 0.23f};
    private static final float[] ASH_MID = {0.26f, 0.25f, 0.27f};
    private static final float[] ASH_STREAM = {0.3f, 0.29f, 0.31f};
    private static final float[] ASH_BURST = {0.24f, 0.23f, 0.25f};

    private static final Vector3f RIGHT = new Vector3f();
    private static final Vector3f UP = new Vector3f();
    private static final Vector3f A = new Vector3f(), B = new Vector3f(), DIR = new Vector3f(), TO_CAM = new Vector3f(), SIDE = new Vector3f();

    static float detail = 1f;

    private RitualFx() {}

    public static void queue(Vec3d pos, float t, int offerings, int tintHex) {
        QUEUE.add(new Job(pos, t, offerings, rgb(tintHex)));
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
            if (d2 > 128 * 128) continue;
            detail = FxQuality.density(d2);
            matrices.push();
            matrices.translate(job.pos.x - cam.x, job.pos.y - cam.y, job.pos.z - cam.z);
            Matrix4f m = matrices.peek().getPositionMatrix();
            Vec3d eye = cam.subtract(job.pos);

            begin();
            build(job, DECALS, ITEMS);
            drawDecals(m, DECALS);
            drawItems(m, ITEMS, eye, camRot);
            matrices.pop();
        }
        QUEUE.clear();
        DECALS.clear();
        ITEMS.clear();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        new GlBlendState().enable();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    static void begin() {
        DECALS.clear();
        ITEMS.clear();
        poolUsed = 0;
    }

    static List<Decal> decals() {
        return DECALS;
    }

    static List<Item> items() {
        return ITEMS;
    }

    private static Item obtain() {
        if (poolUsed == POOL.size()) POOL.add(new Item());
        Item it = POOL.get(poolUsed++);
        it.pts = null;
        it.roll = 0f;
        return it;
    }

    private static int count(int full) {
        return Math.max(1, Math.round(full * detail));
    }

    private static void additive(boolean add) {
        if (add) RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        else RenderSystem.defaultBlendFunc();
    }

    static void drawDecals(Matrix4f m, List<Decal> decals) {
        additive(true);
        RenderSystem.setShader(() -> ShadowShaders.sigil);
        for (Decal d : decals) {
            if (d.a <= 0.003f || d.radius <= 0.001f) continue;
            RenderSystem.setShaderTexture(0, d.tex);
            set(ShadowShaders.sigil, "Progress", d.progress);
            set(ShadowShaders.sigil, "Intensity", d.intensity);
            float u0 = 0, v0 = 0, u1 = 1, v1 = 1;
            if (d.cell >= 0) {
                u0 = (d.cell % 4) / 4f;
                v0 = (d.cell / 4) / 4f;
                u1 = u0 + 0.25f;
                v1 = v0 + 0.25f;
            }
            BufferBuilder buf = Tessellator.getInstance().getBuffer();
            buf.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            float rad = d.rot * MathHelper.RADIANS_PER_DEGREE;
            float cos = MathHelper.cos(rad) * d.radius, sin = MathHelper.sin(rad) * d.radius;
            buf.vertex(m, -cos + sin, d.y, -sin - cos).texture(u0, v0).color(d.r, d.g, d.b, d.a).next();
            buf.vertex(m, -cos - sin, d.y, -sin + cos).texture(u0, v1).color(d.r, d.g, d.b, d.a).next();
            buf.vertex(m, cos - sin, d.y, sin + cos).texture(u1, v1).color(d.r, d.g, d.b, d.a).next();
            buf.vertex(m, cos + sin, d.y, sin - cos).texture(u1, v0).color(d.r, d.g, d.b, d.a).next();
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }
    }

    static void drawItems(Matrix4f m, List<Item> items, Vec3d eye, Quaternionf camRot) {
        for (Item it : items) {
            double cx = it.x, cy = it.y, cz = it.z;
            if (it.kind == BEAM) {
                int mid = (it.pts.length / 3) / 2;
                cx = it.pts[mid * 3];
                cy = it.pts[mid * 3 + 1];
                cz = it.pts[mid * 3 + 2];
            }
            it.dist = eye.squaredDistanceTo(cx, cy, cz);
        }
        items.sort(BACK_TO_FRONT);
        camRot.transform(1f, 0f, 0f, RIGHT);
        camRot.transform(0f, 1f, 0f, UP);

        BufferBuilder buf = Tessellator.getInstance().getBuffer();
        boolean open = false;
        int curKind = -1;
        boolean curAdd = false;

        for (Item it : items) {
            int kind = it.kind == ORB ? ORB : SPRITE;
            boolean add = it.kind == BEAM || (it.kind == SPRITE && it.add);
            if (!open || kind != curKind || add != curAdd) {
                if (open) BufferRenderer.drawWithGlobalProgram(buf.end());
                curKind = kind;
                curAdd = add;
                additive(add);
                if (kind == ORB) {
                    RenderSystem.setShader(() -> ShadowShaders.orb);
                } else if (add) {
                    RenderSystem.setShader(() -> ShadowShaders.sprite);
                    RenderSystem.setShaderTexture(0, SPRITES);
                } else {
                    RenderSystem.setShader(() -> ShadowShaders.spriteAlpha);
                    RenderSystem.setShaderTexture(0, SPRITES);
                }
                buf.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                open = true;
            }
            switch (it.kind) {
                case SPRITE -> spriteQuad(buf, m, camRot, it);
                case BEAM -> beamQuads(buf, m, eye, it);
                default -> orbQuad(buf, m, camRot, it);
            }
        }
        if (open) BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private static final java.util.Comparator<Item> BACK_TO_FRONT = (a, b) -> Double.compare(b.dist, a.dist);

    private static void spriteQuad(BufferBuilder buf, Matrix4f m, Quaternionf rot, Item it) {
        float cu = (it.cell & 3) * 0.25f, cv = (it.cell >> 2) * 0.25f;
        float cr = MathHelper.cos(it.roll) * it.size, sr = MathHelper.sin(it.roll) * it.size;
        float ax = cr * RIGHT.x + sr * UP.x, ay = cr * RIGHT.y + sr * UP.y, az = cr * RIGHT.z + sr * UP.z;
        float bx = -sr * RIGHT.x + cr * UP.x, by = -sr * RIGHT.y + cr * UP.y, bz = -sr * RIGHT.z + cr * UP.z;
        float x = it.x, y = it.y, z = it.z;
        buf.vertex(m, x - ax - bx, y - ay - by, z - az - bz).texture(cu + 0.25f, cv + 0.25f).color(it.r, it.g, it.b, it.a).next();
        buf.vertex(m, x - ax + bx, y - ay + by, z - az + bz).texture(cu + 0.25f, cv).color(it.r, it.g, it.b, it.a).next();
        buf.vertex(m, x + ax + bx, y + ay + by, z + az + bz).texture(cu, cv).color(it.r, it.g, it.b, it.a).next();
        buf.vertex(m, x + ax - bx, y + ay - by, z + az - bz).texture(cu, cv + 0.25f).color(it.r, it.g, it.b, it.a).next();
    }

    private static void orbQuad(BufferBuilder buf, Matrix4f m, Quaternionf rot, Item it) {
        float s = it.size, g = MathHelper.clamp(it.r, 0f, 1f);
        float ax = RIGHT.x * s, ay = RIGHT.y * s, az = RIGHT.z * s;
        float bx = UP.x * s, by = UP.y * s, bz = UP.z * s;
        float x = it.x, y = it.y, z = it.z;
        buf.vertex(m, x - ax - bx, y - ay - by, z - az - bz).texture(1, 1).color(g, 0f, 0f, 1f).next();
        buf.vertex(m, x - ax + bx, y - ay + by, z - az + bz).texture(1, 0).color(g, 0f, 0f, 1f).next();
        buf.vertex(m, x + ax + bx, y + ay + by, z + az + bz).texture(0, 0).color(g, 0f, 0f, 1f).next();
        buf.vertex(m, x + ax - bx, y + ay - by, z + az - bz).texture(0, 1).color(g, 0f, 0f, 1f).next();
    }

    private static void beamQuads(BufferBuilder buf, Matrix4f m, Vec3d eye, Item it) {
        float[] p = it.pts;
        int n = p.length / 3;
        Vector3f a = A, b = B, dir = DIR, toCam = TO_CAM, side = SIDE;
        for (int s = 0; s < n - 1; s++) {
            a.set(p[s * 3], p[s * 3 + 1], p[s * 3 + 2]);
            b.set(p[s * 3 + 3], p[s * 3 + 4], p[s * 3 + 5]);
            b.sub(a, dir).normalize();
            toCam.set((float) eye.x - (a.x + b.x) * 0.5f, (float) eye.y - (a.y + b.y) * 0.5f, (float) eye.z - (a.z + b.z) * 0.5f).normalize();
            dir.cross(toCam, side).normalize().mul(it.width * 0.5f);
            buf.vertex(m, a.x - side.x, a.y - side.y, a.z - side.z).texture(0f, 0.125f).color(it.r, it.g, it.b, it.a).next();
            buf.vertex(m, a.x + side.x, a.y + side.y, a.z + side.z).texture(0.25f, 0.125f).color(it.r, it.g, it.b, it.a).next();
            buf.vertex(m, b.x + side.x, b.y + side.y, b.z + side.z).texture(0.25f, 0.125f).color(it.r, it.g, it.b, it.a).next();
            buf.vertex(m, b.x - side.x, b.y - side.y, b.z - side.z).texture(0f, 0.125f).color(it.r, it.g, it.b, it.a).next();
        }
    }

    private static void set(ShaderProgram program, String name, float value) {
        GlUniform u = program.getUniform(name);
        if (u != null) u.set(value);
    }

    static void sprite(List<Item> list, float x, float y, float z, float size, int cell, float[] c, float a, boolean add, float roll) {
        if (a <= 0.004f || size <= 0.001f) return;
        Item it = obtain();
        it.kind = SPRITE;
        it.x = x; it.y = y; it.z = z;
        it.size = size; it.cell = cell; it.roll = roll;
        it.r = c[0]; it.g = c[1]; it.b = c[2]; it.a = a;
        it.add = add;
        list.add(it);
    }

    static void sprite(List<Item> list, float x, float y, float z, float size, int cell, float[] c, float a, boolean add) {
        sprite(list, x, y, z, size, cell, c, a, add, 0f);
    }

    static void beam(List<Item> list, float[] pts, float width, float[] c, float a) {
        if (a <= 0.004f) return;
        Item it = obtain();
        it.kind = BEAM;
        it.pts = pts;
        it.width = width;
        it.r = c[0]; it.g = c[1]; it.b = c[2]; it.a = a;
        list.add(it);
    }

    static float[] mix(float[] a, float[] b, float k) {
        float[] o = SCRATCH[scratchAt++ & 15];
        o[0] = MathHelper.lerp(k, a[0], b[0]);
        o[1] = MathHelper.lerp(k, a[1], b[1]);
        o[2] = MathHelper.lerp(k, a[2], b[2]);
        return o;
    }

    static float fract(float x) {
        return x - MathHelper.floor(x);
    }

    private static void build(Job job, List<Decal> decals, List<Item> items) {
        final float t = job.t;
        final int nOffer = job.offerings;
        final float[] red = job.tint;
        Phases p = phases(t);
        float fadeAll = 1f - p.end;
        float hush = 1f - 0.85f * sm((t - (HUSH - 10)) / 10f) * (1f - sm((t - REVEAL) / 3f));
        float power = Math.max(0.3f, 0.85f + 0.35f * p.gather + 1.0f * p.cresc + 0.8f * p.collapse - 1.4f * p.reveal) * hush;
        float progress = MathHelper.clamp(t / 240f, 0f, 1.0001f);
        final float TAU = (float) (Math.PI * 2);

        float pool = 0.32f * power * sm(t / 200f) * fadeAll;
        decals.add(new Decal(SPRITES, GLOW, 0.015f, RADIUS * 1.35f, 0, 2, 1, red[0], red[1], red[2], pool));
        decals.add(new Decal(RING, -1, 0.02f, RADIUS, spinRing(t), progress, power, 1, 1, 1, fadeAll));
        decals.add(new Decal(SEAL, -1, 0.025f, RADIUS, spinSeal(t), progress, power, 1, 1, 1, fadeAll));

        for (int k = 0; k < 5; k++) {
            float age = t - nodeIgnite(k);
            if (age < 0) continue;
            float[] np = nodePos(k, t);
            float x = np[0], z = np[2];
            float on = sm(age / 12f) * fadeAll * hush * (1f - p.collapse * 0.7f);
            float flick = 0.85f + 0.15f * MathHelper.sin(t * 0.9f + k * 2.1f) * MathHelper.sin(t * 0.37f + k);
            sprite(items, x, 0.35f, z, 0.75f * on, GLOW, red, 0.55f * on, true);
            float fs = (0.46f + 0.06f * flick) * on * (1f + 0.35f * p.cresc);
            sprite(items, x, 0.05f + fs, z, fs, FLAME, EMBER, 0.95f * on, true);
            sprite(items, x, 0.05f + fs * 0.55f, z, fs * 0.45f, FLAME, HOT, 0.85f * on, true);

            int wisps = count(6);
            for (int j = 0; j < wisps; j++) {
                float life = 70f, cyc = (age + j * life / wisps) / life;
                if (cyc < 1) continue;
                float ph = fract(cyc);
                float sway = MathHelper.sin(t * 0.05f + j * 1.3f + k) * 0.15f * ph;
                float[] c = mix(ASH_DARK, red, 0.5f * (1 - ph));
                sprite(items, x + sway, 0.55f + ph * 1.4f, z + sway * 0.6f, 0.18f + ph * 0.35f, PUFF + (j & 3), c,
                        MathHelper.sin(ph * (float) Math.PI) * 0.45f * on, false, j * 1.7f + ph);
            }

            if (age < 25) {
                float b = age / 25f;
                decals.add(new Decal(SPRITES, RING_CELL, 0.06f, 0.3f + 1.2f * b, 0, 2, 1, red[0], red[1], red[2], 1 - b));
                sprite(items, x, 0.35f, z, 1.2f * (1 - b), GLOW, HOT, (1 - b) * 0.8f, true);
                for (int s = 0, sparks = count(10); s < sparks; s++) {
                    float a = hash(k * 100 + s) * TAU;
                    float v = 0.05f + 0.05f * hash(k * 100 + s + 50);
                    sprite(items, x + MathHelper.cos(a) * v * age, 0.3f + 0.07f * age - 0.002f * age * age, z + MathHelper.sin(a) * v * age,
                            0.1f * (1 - b), SPARK, HOT, 1 - b, true);
                }
            }
        }

        {
            int life = 90, every = detail > 0.8f ? 2 : detail > 0.5f ? 3 : 5;
            int first = Math.max(0, MathHelper.floor((t - life) / every)), last = MathHelper.floor(t / every);
            float density = 0.35f + 0.65f * Math.max(p.gather, p.cresc);
            for (int k = first; k <= last; k++) {
                float t0 = k * every;
                float age = t - t0;
                if (age < 0 || age > life || t0 < 120) continue;
                if (hash(k * 3 + 1) > density) continue;
                float rr = RADIUS * MathHelper.sqrt(hash(k * 7 + 2)) * 0.95f;
                float a0 = hash(k * 11 + 3) * TAU;
                float k01 = age / life;
                float swirl = a0 + age * (0.01f + 0.04f * p.cresc);
                float pull = 1f - 0.6f * p.cresc * k01;
                float y = 0.05f + age * (0.025f + 0.02f * hash(k * 5 + 4)) * (1 + p.cresc);
                float a = sm(age / 8f) * (1 - sm((age - life * 0.6f) / (life * 0.4f))) * fadeAll * hush;
                sprite(items, MathHelper.cos(swirl) * rr * pull, y, MathHelper.sin(swirl) * rr * pull,
                        0.07f + 0.05f * hash(k * 17), SPARK, mix(HOT, red, k01), a, true);
            }
        }

        if (t > 150) {
            int n = count(26);
            for (int k = 0; k < n; k++) {
                float life = 260f;
                float cyc = (t + hash(k) * life) / life;
                int gen = MathHelper.floor(cyc);
                float ph = fract(cyc);
                float a0 = hash(k * 19 + gen * 7) * TAU + ph * 0.6f + t * 0.0015f * (1 + 6 * p.cresc);
                float rr = RADIUS * MathHelper.lerp(hash(k * 23 + gen * 3), 0.55f, 1.15f) * (1 - 0.5f * p.collapse);
                float y = 0.15f + ph * 0.6f + 0.3f * hash(k * 29 + gen);
                float a = MathHelper.sin(ph * (float) Math.PI) * 0.42f * sm((t - 150) / 200f) * fadeAll * (1 - p.reveal * 0.5f);
                float under = (float) Math.exp(-y * 2.2f) * power * 0.55f;
                float[] c = mix(ASH_MID, red, MathHelper.clamp(under, 0f, 0.75f));
                sprite(items, MathHelper.cos(a0) * rr, y, MathHelper.sin(a0) * rr, 1.1f + 0.7f * hash(k * 31) + ph * 0.5f,
                        PUFF + (k % 4), c, a, false, hash(k * 37) * 6 + ph);
            }
        }

        for (int i = 0; i < 8; i++) {
            float born = sm((t - glyphBorn(i)) / 40f);
            float eaten = sm((t - (GATHER_END + 150 + i * 45)) / 90f);
            float a = born * (1 - eaten) * fadeAll;
            if (a <= 0.01f) continue;
            float ang = i / 8f * TAU - t * (0.006f + 0.03f * p.cresc);
            float r = MathHelper.lerp(eaten, 2.75f, 0f);
            float y = MathHelper.lerp(eaten, 2.2f + 0.15f * MathHelper.sin(t * 0.04f + i * 1.3f), ORB_Y);
            float pulse = 0.75f + 0.25f * MathHelper.sin(t * 0.08f + i);
            float gx = MathHelper.cos(ang) * r, gz = MathHelper.sin(ang) * r;
            sprite(items, gx, y, gz, 0.42f * (0.7f + 0.3f * born) * (1 - 0.6f * eaten), GLOW, red, 0.35f * a, true);
            sprite(items, gx, y, gz, 0.3f * (0.7f + 0.3f * born) * (1 - 0.6f * eaten), GLYPH + i, mix(red, HOT, 0.25f), a * pulse, true);
        }

        if (t > RISE_END - 50 && t < HUSH) {
            float streamOn = sm((t - (RISE_END - 50)) / 150f) * (1 - p.collapse * 0.5f);
            float travel = 80 - 30 * p.cresc;
            int every = detail > 0.8f ? 5 : detail > 0.5f ? 7 : 11;
            for (int k = 0; k < 5; k++) {
                int first = MathHelper.floor((t - travel) / every), last = MathHelper.floor(t / every);
                for (int j = first; j <= last; j++) {
                    float age = t - j * every;
                    if (age < 0 || age > travel) continue;
                    float s = age / travel;
                    float[] n0 = nodePos(k, j * every);
                    float a0 = (float) Math.atan2(n0[2], n0[0]);
                    float ang = a0 + s * 2.4f;
                    float rr = MathHelper.lerp((float) Math.pow(s, 0.85), NODE_R, 0.15f);
                    float y = MathHelper.lerp((float) Math.pow(s, 1.6), 0.35f, ORB_Y);
                    float a = sm(age / 10f) * (1 - sm((s - 0.8f) / 0.2f)) * 0.55f * streamOn;
                    float[] c = mix(ASH_STREAM, red, 0.55f * (1 - s) * power * 0.6f);
                    sprite(items, MathHelper.cos(ang) * rr, y, MathHelper.sin(ang) * rr, MathHelper.lerp(s, 0.75f, 0.3f),
                            PUFF + ((j + k) & 3), c, a, false, j * 0.7f);
                }
            }
        }

        if (p.cresc > 0 && t < REVEAL) {
            int n = count(44);
            for (int k = 0; k < n; k++) {
                float h = fract(t * (0.0035f + 0.004f * p.cresc) + k / (float) n);
                float y = 0.3f + h * 6.5f * (1 - 0.7f * p.collapse);
                float rr = (0.55f + h * 1.6f) * (1 - 0.85f * p.collapse);
                float ang = k * 2.39996f + t * (0.05f + 0.12f * p.cresc + 0.3f * p.collapse) - h * 1.5f;
                float a = MathHelper.sin(h * (float) Math.PI) * 0.5f * p.cresc * (1 - sm((t - (HUSH - 20)) / 20f));
                float[] c = mix(ASH_DARK, red, 0.35f * (float) Math.exp(-h * 2.5f) * power);
                sprite(items, MathHelper.cos(ang) * rr, y, MathHelper.sin(ang) * rr, 0.9f + h * 1.3f, PUFF + (k & 3), c, a, false, k + t * 0.02f);
            }
        }

        for (int i = 0; i < nOffer; i++) {
            float age = t - dissolveStart(i, nOffer);
            if (age < 0 || age > DISSOLVE_TIME + 20) continue;
            Offering o = offering(i, nOffer, t);
            float k = age / DISSOLVE_TIME;
            sprite(items, o.x(), o.y(), o.z(), 0.5f * (1 - k), GLOW, red, 0.8f * Math.max(0, 1 - k), true);
            for (int s = 0, trail = count(6); s < trail; s++) {
                Offering q = offering(i, nOffer, t - s * 3);
                sprite(items, q.x(), q.y(), q.z(), 0.1f, SPARK, mix(HOT, red, s / 6f), (1 - s / 6f) * (1 - sm(k)), true);
            }
            if (age > DISSOLVE_TIME - 5) {
                float b = (age - (DISSOLVE_TIME - 5)) / 25f;
                sprite(items, 0, ORB_Y, 0, 0.6f + 1.6f * b, RING_CELL, red, 1 - b, true);
            }
        }

        Arc arc = arcAt(t);
        if (arc != null) {
            float[] from = nodePos(arc.node(), t);
            from[1] = 0.35f;
            int segs = 12, flick = MathHelper.floor(t);
            float[] pts = new float[(segs + 1) * 3];
            for (int s = 0; s <= segs; s++) {
                float k = s / (float) segs;
                float j = (s == 0 || s == segs) ? 0 : 0.7f * MathHelper.sin(k * (float) Math.PI);
                pts[s * 3] = MathHelper.lerp(k, from[0], 0) + (hash(flick * 97 + s * 3) - 0.5f) * j;
                pts[s * 3 + 1] = MathHelper.lerp(k, from[1], ORB_Y) + (hash(flick * 89 + s * 5) - 0.5f) * j + MathHelper.sin(k * (float) Math.PI) * 0.6f;
                pts[s * 3 + 2] = MathHelper.lerp(k, from[2], 0) + (hash(flick * 83 + s * 7) - 0.5f) * j;
            }
            float age = arc.age();
            float a = (age < 3 ? 1 : (1 - (age - 3) / 7f) * (0.6f + 0.4f * hash(flick * 5))) * hush;
            beam(items, pts, 0.5f, red, a * 0.9f);
            beam(items, pts, 0.16f, EMBER, a);
            beam(items, pts, 0.05f, HOT, a);

            int mid = segs / 2;
            float[] fork = new float[5 * 3];
            fork[0] = pts[mid * 3];
            fork[1] = pts[mid * 3 + 1];
            fork[2] = pts[mid * 3 + 2];
            for (int s = 1; s <= 4; s++) {
                fork[s * 3] = fork[(s - 1) * 3] + (hash(flick * 41 + s) - 0.5f) * 0.5f;
                fork[s * 3 + 1] = fork[(s - 1) * 3 + 1] - 0.15f + (hash(flick * 43 + s) - 0.5f) * 0.3f;
                fork[s * 3 + 2] = fork[(s - 1) * 3 + 2] + (hash(flick * 47 + s) - 0.5f) * 0.5f;
            }
            beam(items, fork, 0.22f, red, a * 0.7f);
            beam(items, fork, 0.04f, HOT, a * 0.8f);
            sprite(items, from[0], from[1], from[2], 1.2f, GLOW, red, a, true);
            sprite(items, 0, ORB_Y, 0, 1.6f, GLOW, EMBER, a * 0.7f, true);
        }

        float orbR = orbRadius(t);
        if (orbR > 0.01f) {
            float beat = (float) Math.pow(Math.max(0, MathHelper.sin(t * 0.26f)), 10) * p.cresc;
            sprite(items, 0, ORB_Y, 0, orbR * (3.2f + 1.5f * beat), GLOW, red, (0.55f + 0.4f * beat) * hush, true);
            Item orb = obtain();
            orb.kind = ORB;
            orb.x = 0; orb.y = ORB_Y; orb.z = 0;
            orb.size = orbR;
            orb.r = power * (0.8f + 0.6f * beat);
            items.add(orb);
        }

        if (t >= REVEAL) {
            float age = t - REVEAL;
            float k = MathHelper.clamp(age / 30f, 0f, 1f);
            sprite(items, 0, ORB_Y, 0, 1 + 7 * MathHelper.sqrt(k), GLOW, mix(HOT, red, k), (1 - k) * 1.2f, true);
            decals.add(new Decal(SPRITES, RING_CELL, 0.08f, 0.5f + 9 * (1 - (1 - k) * (1 - k)), 0, 2, 1, red[0], red[1], red[2], 1 - k));
            for (int s = 0, puffs = count(36); s < puffs; s++) {
                float a0 = s / (float) puffs * TAU + hash(s) * 0.2f;
                float dist = 0.5f + 6 * (1 - (float) Math.pow(1 - MathHelper.clamp(age / 70f, 0f, 1f), 2.5));
                float a = (1 - sm(age / 110f)) * 0.55f;
                sprite(items, MathHelper.cos(a0) * dist, 0.3f + 0.4f * hash(s + 9), MathHelper.sin(a0) * dist, 1.0f + age * 0.012f,
                        PUFF + (s & 3), ASH_BURST, a, false, s);
            }
            for (int s = 0, sparks = count(50); s < sparks; s++) {
                float a0 = hash(s * 3) * TAU;
                float v = 0.08f + 0.12f * hash(s * 5);
                float up = 0.06f + 0.12f * hash(s * 7);
                float ag = Math.min(age, 60f);
                float life = 1 - sm(age / 60f);
                sprite(items, MathHelper.cos(a0) * v * ag, ORB_Y + up * ag - 0.004f * ag * ag, MathHelper.sin(a0) * v * ag,
                        0.12f, SPARK, mix(HOT, red, age / 60f), life, true);
            }
            float hang = sm(age / 20f) * fadeAll;
            sprite(items, 0, 1.4f, 0, 1.6f, GLOW, red, 0.5f * hang, true);
        }
    }

    private static float[] rgb(int hex) {
        return new float[]{((hex >> 16) & 0xff) / 255f, ((hex >> 8) & 0xff) / 255f, (hex & 0xff) / 255f};
    }
}
