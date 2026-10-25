package com.h3artless.shadowbound.client.render;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.client.cast.AirCasting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlBlendState;
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

import java.util.List;

import static com.h3artless.shadowbound.client.render.RitualFx.*;

public final class AirCastFx {
    private static final Identifier ATLAS = Shadowbound.id("textures/misc/cast_sigil.png");
    private static final float[] ASH = {0.3f, 0.28f, 0.3f};
    private static final int MAX_PTS = 64;
    private static final float[] SEAL_ANGLES = {(float) Math.toRadians(90), (float) Math.toRadians(210), (float) Math.toRadians(330)};
    private static final float[] PX = new float[MAX_PTS], PY = new float[MAX_PTS], PZ = new float[MAX_PTS];

    private static float rx, rz, nx, nz;

    private AirCastFx() {}

    private static float sm(float x) {
        x = MathHelper.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    private static float hash(int n) {
        n = (n << 13) ^ n;
        n = n * (n * n * 15731 + 789221) + 1376312589;
        return (n & 0x7fffffff) / (float) 0x7fffffff;
    }

    public static void draw(WorldRenderContext ctx) {
        if (!AirCasting.visible()) return;
        if (ShadowShaders.airStroke == null || ShadowShaders.airGlyph == null || ShadowShaders.airSigil == null
                || ShadowShaders.sprite == null || ShadowShaders.spriteAlpha == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        float delta = ctx.tickDelta();
        AirCasting.frame(delta);
        float now = mc.world.getTime() + delta;
        int state = AirCasting.state();
        float since = now - AirCasting.stateAt();
        float open = sm((now - AirCasting.openAt()) / 12f);
        float fade = state == AirCasting.CLOSING ? 1f - MathHelper.clamp(since / AirCasting.CLOSE_TICKS, 0f, 1f) : 1f;
        if (fade <= 0f) return;

        Vec3d c = AirCasting.center();
        Vec3d cam = ctx.camera().getPos();
        Quaternionf camRot = ctx.camera().getRotation();
        rx = (float) AirCasting.right().x;
        rz = (float) AirCasting.right().z;
        nx = (float) AirCasting.normal().x;
        nz = (float) AirCasting.normal().z;
        Vec3d bk = AirCasting.book().subtract(c);
        float bx = (float) bk.x, by = (float) bk.y, bz = (float) bk.z;

        MatrixStack matrices = ctx.matrixStack();
        matrices.push();
        matrices.translate(c.x - cam.x, c.y - cam.y, c.z - cam.z);
        Matrix4f m = matrices.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        float succ = state == AirCasting.SUCCESS ? sm(since / 14f) : 0f;
        float fail = state == AirCasting.FAIL ? MathHelper.clamp(since / 20f, 0f, 1f) : 0f;
        float stepK = state == AirCasting.STEP ? sm(since / 12f) : 0f;
        int done = AirCasting.stepsDone();

        float reach = AirCasting.RADIUS * AirCasting.SPACING;
        mc.getTextureManager().getTexture(ATLAS).setFilter(true, false);
        float disc = reach * 1.42f * (0.55f + 0.45f * open) * (1f - 0.6f * succ);
        float sigA = open * fade * (1f - fail) * (1f + 0.6f * succ);
        BufferBuilder buf = begin(ShadowShaders.airSigil);
        sprite3(buf, m, 0f, 0f, -0.05f, disc * 1.08f, now * 0.006f + succ * 2f, 0f, 0f, 0.5f, 0.5f, 0.9f, 0.05f, 0.07f, 0.85f * sigA);
        sprite3(buf, m, 0f, 0f, -0.04f, disc * 0.8f, -now * 0.011f - succ * 3f, 0.5f, 0f, 1f, 0.5f, 0.85f, 0.04f, 0.06f, 0.7f * sigA);
        end(buf);

        List<int[]> path = AirCasting.path();
        int[] aim = AirCasting.aim();
        buf = begin(ShadowShaders.airGlyph);
        float orbit = disc * 1.36f;
        float sealR = disc * 1.12f;
        for (int k = 0; k < 3; k++) {
            float ang = SEAL_ANGLES[k];
            float sx = MathHelper.cos(ang) * sealR, sy = MathHelper.sin(ang) * sealR;
            boolean lit = k < done;
            boolean next = k == done && state != AirCasting.FAIL;
            float flash = lit && k == done - 1 && state == AirCasting.STEP ? 1f - MathHelper.clamp((since - 10f) / 10f, 0f, 1f) : 0f;
            float beat = next ? 0.5f + 0.5f * MathHelper.sin(now * 0.25f) : 0f;
            float size = 0.14f * (1f + 0.4f * flash + 0.12f * beat) * open;
            float a = (lit ? 1f : next ? 0.45f + 0.35f * beat : 0.22f) * sigA;
            float g = lit ? 0.35f + 0.4f * flash : 0.06f, b = lit ? 0.28f + 0.35f * flash : 0.08f;
            sprite3(buf, m, sx, sy, -0.01f, size * 1.25f, now * (lit ? 0.08f : 0.02f) + k, 0f, 0.5f, 0.125f, 0.625f, 1f, g, b, a * 0.8f);
            float gu = 0.5f + k * 0.125f;
            sprite3(buf, m, sx, sy, 0f, size, 0f, gu, 0.625f, gu + 0.125f, 0.75f, 1f, g, b, a);
        }
        for (int i = 0; i < 8; i++) {
            float ang = i / 8f * (float) (Math.PI * 2) + now * 0.004f;
            float bob = MathHelper.sin(now * 0.07f + i * 1.3f) * 0.03f;
            float appear = sm((now - AirCasting.openAt() - 6f - i * 1.5f) / 8f);
            float gu = 0.5f + (i % 4) * 0.125f, gv = 0.5f + (i / 4) * 0.125f;
            sprite3(buf, m, MathHelper.cos(ang) * orbit, MathHelper.sin(ang) * orbit + bob, -0.02f, 0.085f * appear, 0f,
                    gu, gv, gu + 0.125f, gv + 0.125f, 0.9f, 0.08f, 0.1f, 0.8f * appear * sigA);
        }
        for (int[] d : AirCasting.grid()) {
            int hd = (Math.abs(d[0]) + Math.abs(d[1]) + Math.abs(d[0] + d[1])) / 2;
            float appear = sm((now - AirCasting.openAt() - hd * 2.5f) / 7f);
            if (appear <= 0f) continue;
            boolean hover = d == aim && state == AirCasting.IDLE;
            boolean used = path.contains(d);
            float pulse = 0.5f + 0.5f * MathHelper.sin(now * 0.12f + d[0] * 0.9f + d[1] * 1.7f);
            float size = AirCasting.SPACING * (0.3f + 0.05f * pulse + (hover ? 0.22f : 0f) + (used ? 0.1f : 0f)) * appear;
            float a = (0.55f + 0.3f * pulse + (hover ? 0.3f : 0f)) * appear * fade * (1f - fail) * (1f - succ);
            float r = used || hover ? 1f : 0.8f, g = hover ? 0.5f : used ? 0.28f : 0.07f, b = hover ? 0.4f : used ? 0.2f : 0.09f;
            float spin = now * (hover ? 0.15f : 0.03f) + d[0] * 0.7f + d[1];
            sprite3(buf, m, AirCasting.lx(d), AirCasting.ly(d), 0f, size, spin, 0f, 0.5f, 0.125f, 0.625f, r, g, b, a);
        }
        if (AirCasting.aimValid() && AirCasting.active() && state == AirCasting.IDLE
                && Math.abs(AirCasting.aimX()) < reach * 1.3f && Math.abs(AirCasting.aimY()) < reach * 1.3f) {
            float blink = 0.85f + 0.15f * MathHelper.sin(now * 0.4f);
            sprite3(buf, m, AirCasting.aimX(), AirCasting.aimY(), 0.02f, AirCasting.SPACING * (aim != null ? 0.55f : 0.38f), 0f,
                    0.125f, 0.5f, 0.25f, 0.625f, 1f, 0.35f, 0.3f, 0.9f * fade * blink);
        }
        end(buf);

        int n = Math.min(path.size(), MAX_PTS);
        for (int i = 0; i < n; i++) {
            int[] d = path.get(i);
            float lx = AirCasting.lx(d), ly = AirCasting.ly(d);
            float x = rx * lx, y = ly, z = rz * lx;
            if (succ > 0f) {
                float k = succ * succ;
                x = MathHelper.lerp(k, x, bx);
                y = MathHelper.lerp(k, y, by);
                z = MathHelper.lerp(k, z, bz);
            }
            if (fail > 0f) y -= fail * fail * (0.9f + 0.4f * hash(i * 7));
            if (stepK > 0f && done > 0) {
                float ang = SEAL_ANGLES[done - 1];
                float tx = MathHelper.cos(ang) * disc * 1.12f + lx * 0.18f, ty = MathHelper.sin(ang) * disc * 1.12f + ly * 0.18f;
                float k = stepK * stepK;
                x = MathHelper.lerp(k, x, rx * tx);
                y = MathHelper.lerp(k, y, ty);
                z = MathHelper.lerp(k, z, rz * tx);
            }
            PX[i] = x;
            PY[i] = y;
            PZ[i] = z;
        }

        buf = begin(ShadowShaders.airStroke);
        RenderSystem.setShaderTexture(0, ATLAS);
        float run = 0f;
        for (int i = 0; i + 1 < n; i++) {
            float age = now - AirCasting.addedAt(i + 1);
            float fresh = 1f - MathHelper.clamp(age / 8f, 0f, 1f);
            float w = 0.16f * (1f + 0.7f * fresh) * (1f + 0.8f * succ) * (1f - 0.4f * fail);
            float r = MathHelper.lerp(fail, 0.95f, 0.26f), g = MathHelper.lerp(fail, 0.1f + 0.35f * fresh + 0.5f * succ, 0.24f);
            float b = MathHelper.lerp(fail, 0.12f + 0.3f * fresh + 0.4f * succ, 0.25f);
            float a = fade * (1f - fail) * (1f - sm((since - 12f) / 6f) * (state == AirCasting.SUCCESS ? 1f : 0f))
                    * (1f - sm((since - 9f) / 5f) * (state == AirCasting.STEP ? 1f : 0f));
            w *= 1f - 0.7f * stepK;
            run = ribbon(buf, m, PX[i], PY[i], PZ[i], PX[i + 1], PY[i + 1], PZ[i + 1], w, run, r, g, b, a);
        }
        if (AirCasting.drawing() && AirCasting.aimValid() && n > 0) {
            float ax = rx * AirCasting.aimX(), az = rz * AirCasting.aimX();
            ribbon(buf, m, PX[n - 1], PY[n - 1], PZ[n - 1], ax, AirCasting.aimY(), az, 0.1f, run, 0.9f, 0.12f, 0.14f, 0.5f * fade);
        }
        int shown = state == AirCasting.STEP ? done - 1 : done;
        for (int k = 0; k < shown && k < 3; k++) {
            List<int[]> mark = AirCasting.donePath(k);
            float ang = SEAL_ANGLES[k];
            float sx = MathHelper.cos(ang) * disc * 1.12f, sy = MathHelper.sin(ang) * disc * 1.12f;
            float mx = 0f, my = 0f;
            for (int[] d : mark) {
                mx += AirCasting.lx(d);
                my += AirCasting.ly(d);
            }
            mx /= mark.size();
            my /= mark.size();
            float mrun = 0f;
            float ma = fade * (1f - fail) * (0.75f + 0.25f * MathHelper.sin(now * 0.2f + k));
            for (int i = 0; i + 1 < mark.size(); i++) {
                float ax0 = sx + (AirCasting.lx(mark.get(i)) - mx) * 0.18f, ay0 = sy + (AirCasting.ly(mark.get(i)) - my) * 0.18f;
                float ax1 = sx + (AirCasting.lx(mark.get(i + 1)) - mx) * 0.18f, ay1 = sy + (AirCasting.ly(mark.get(i + 1)) - my) * 0.18f;
                mrun = ribbon(buf, m, rx * ax0 + nx * 0.02f, ay0, rz * ax0 + nz * 0.02f, rx * ax1 + nx * 0.02f, ay1, rz * ax1 + nz * 0.02f,
                        0.05f, mrun, 1f, 0.3f, 0.22f, ma);
            }
        }
        end(buf);

        RitualFx.begin();
        List<Item> items = RitualFx.items();
        int per = FxQuality.low() ? 1 : FxQuality.level() == FxQuality.HIGH ? 3 : 2;
        for (int i = 0; i + 1 < n; i++) {
            for (int j = 0; j < per; j++) {
                float cyc = now * 0.035f + hash(i * 13 + j * 31);
                float ph = fract(cyc);
                int gen = MathHelper.floor(cyc);
                float u = hash(i * 7 + j * 3 + gen * 11);
                float x = MathHelper.lerp(u, PX[i], PX[i + 1]), y = MathHelper.lerp(u, PY[i], PY[i + 1]), z = MathHelper.lerp(u, PZ[i], PZ[i + 1]);
                float sway = MathHelper.sin(ph * 6f + i) * 0.04f;
                float a = MathHelper.sin(ph * (float) Math.PI) * fade * (fail > 0 ? 0.3f : 0.9f);
                if (fail > 0f) {
                    sprite(items, x + rx * sway, y - ph * 0.5f, z + rz * sway, 0.08f + ph * 0.1f, PUFF + (j & 3), ASH, a * (1f - fail) * 0.8f, false, ph * 3f);
                } else {
                    sprite(items, x + rx * sway + nx * ph * 0.1f, y + ph * 0.35f, z + rz * sway + nz * ph * 0.1f,
                            0.035f + 0.02f * (1f - ph), SPARK, mix(HOT, RED, ph), a, true);
                }
            }
            if (i == n - 2 && state == AirCasting.IDLE) {
                float age = now - AirCasting.addedAt(i + 1);
                if (age < 8f) sprite(items, PX[i + 1], PY[i + 1], PZ[i + 1], 0.35f * (1f - age / 8f) + 0.1f, GLOW, EMBER, (1f - age / 8f) * fade, true);
            }
        }

        float bottom = -reach * 0.866f - 0.12f;
        float tether = 0.45f * open * fade * (1f - succ);
        if (tether > 0.01f) {
            int segs = 8;
            float[] pts = new float[(segs + 1) * 3];
            for (int s = 0; s <= segs; s++) {
                float u = s / (float) segs;
                float wob = MathHelper.sin(now * 0.3f + u * 9f) * 0.04f * MathHelper.sin(u * (float) Math.PI);
                pts[s * 3] = MathHelper.lerp(u, bx, 0f) + rx * wob;
                pts[s * 3 + 1] = MathHelper.lerp(u, by + 0.2f, bottom);
                pts[s * 3 + 2] = MathHelper.lerp(u, bz, 0f) + rz * wob;
            }
            beam(items, pts, 0.16f, RED, tether * 0.6f);
            beam(items, pts, 0.04f, HOT, tether);
        }
        sprite(items, 0f, 0f, -0.06f, reach * 1.8f, GLOW, RED, 0.22f * open * fade * (1f - fail), true);

        if (state == AirCasting.STEP && done > 0) {
            float wave = MathHelper.clamp((since - 8f) / 14f, 0f, 1f);
            if (wave > 0f && wave < 1f) {
                sprite(items, 0f, 0f, 0.02f, disc * (0.3f + 1.6f * wave), RING_CELL, mix(HOT, RED, wave), (1f - wave) * 0.9f, true);
                sprite(items, 0f, 0f, 0.02f, disc * (0.8f + 0.6f * wave), GLOW, RED, (1f - wave) * 0.6f, true);
                float ang = SEAL_ANGLES[done - 1];
                float sx = MathHelper.cos(ang) * disc * 1.12f, sy = MathHelper.sin(ang) * disc * 1.12f;
                sprite(items, rx * sx, sy, rz * sx, 0.25f + 0.6f * wave, GLOW, HOT, (1f - wave), true);
                int sparks = FxQuality.low() ? 8 : 18;
                for (int s = 0; s < sparks; s++) {
                    float a0 = hash(s * 11 + done * 101) * (float) (Math.PI * 2);
                    float dist = wave * (0.25f + 0.5f * hash(s * 7 + done));
                    float px = sx + MathHelper.cos(a0) * dist, py = sy + MathHelper.sin(a0) * dist;
                    sprite(items, rx * px, py, rz * px, 0.04f, SPARK, mix(HOT, RED, wave), 1f - wave, true);
                }
            }
        }

        if (AirCasting.active() && AirCasting.aimValid() && state == AirCasting.IDLE && mc.player != null) {
            Vec3d eye = mc.player.getCameraPosVec(delta);
            Vec3d look = mc.player.getRotationVec(delta);
            Vec3d side = look.crossProduct(new Vec3d(0, 1, 0));
            if (side.lengthSquared() < 1e-4) side = AirCasting.right();
            side = side.normalize();
            boolean first = mc.options.getPerspective().isFirstPerson();
            float hand = mc.player.getMainArm() == net.minecraft.util.Arm.RIGHT ? 1f : -1f;
            Vec3d h = first
                    ? eye.add(look.multiply(0.55)).add(side.multiply(0.2 * hand)).add(0, -0.14, 0)
                    : eye.add(look.multiply(0.75)).add(side.multiply(0.36 * hand)).add(0, -0.22, 0);
            Vec3d hr = h.subtract(c);
            float hx = (float) hr.x, hy = (float) hr.y, hz = (float) hr.z;
            float tx = rx * AirCasting.aimX(), ty = AirCasting.aimY(), tz = rz * AirCasting.aimX();
            int segs = 9, flick = MathHelper.floor(now * 1.5f);
            float[] pts = new float[(segs + 1) * 3];
            for (int s = 0; s <= segs; s++) {
                float u = s / (float) segs;
                float j = (s == 0 || s == segs) ? 0f : 0.09f * MathHelper.sin(u * (float) Math.PI);
                pts[s * 3] = MathHelper.lerp(u, hx, tx) + (hash(flick * 97 + s * 3) - 0.5f) * j;
                pts[s * 3 + 1] = MathHelper.lerp(u, hy, ty) + (hash(flick * 89 + s * 5) - 0.5f) * j;
                pts[s * 3 + 2] = MathHelper.lerp(u, hz, tz) + (hash(flick * 83 + s * 7) - 0.5f) * j;
            }
            float ba = (AirCasting.drawing() ? 0.75f : 0.35f) * fade * (0.7f + 0.3f * hash(flick * 13));
            beam(items, pts, AirCasting.drawing() ? 0.07f : 0.045f, RED, ba);
            beam(items, pts, 0.018f, HOT, ba);
            sprite(items, hx, hy, hz, 0.09f, GLOW, EMBER, ba, true);
        }

        if (state == AirCasting.SUCCESS) {
            float b = MathHelper.clamp((since - 11f) / 10f, 0f, 1f);
            if (b > 0f && b < 1f) {
                sprite(items, bx, by + 0.2f, bz, 0.4f + 2.2f * b, GLOW, mix(HOT, RED, b), (1f - b) * 1.2f, true);
                sprite(items, bx, by + 0.2f, bz, 0.3f + 1.6f * b, RING_CELL, mix(HOT, RED, b), 1f - b, true);
                int sparks = FxQuality.low() ? 12 : 28;
                for (int s = 0; s < sparks; s++) {
                    float ang = hash(s * 5) * (float) (Math.PI * 2);
                    float up = hash(s * 9) * 0.8f - 0.2f;
                    float dist = b * (0.6f + 0.9f * hash(s * 3));
                    sprite(items, bx + MathHelper.cos(ang) * dist, by + 0.2f + up * dist, bz + MathHelper.sin(ang) * dist,
                            0.06f, SPARK, mix(HOT, RED, b), 1f - b, true);
                }
            }
        }

        drawItems(m, items, cam.subtract(c), camRot);
        RitualFx.begin();
        matrices.pop();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        new GlBlendState().enable();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static BufferBuilder begin(ShaderProgram program) {
        RenderSystem.setShader(() -> program);
        RenderSystem.setShaderTexture(0, ATLAS);
        BufferBuilder buf = Tessellator.getInstance().getBuffer();
        buf.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        return buf;
    }

    private static void end(BufferBuilder buf) {
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private static void sprite3(BufferBuilder buf, Matrix4f m, float lx, float ly, float depth, float half, float rot,
                                float u0, float v0, float u1, float v1, float r, float g, float b, float a) {
        if (a <= 0.003f || half <= 0.0001f) return;
        float cr = MathHelper.cos(rot) * half, sr = MathHelper.sin(rot) * half;
        float cx = rx * lx + nx * depth, cz = rz * lx + nz * depth;
        corner(buf, m, cx, ly, cz, -cr + sr, -sr - cr, u0, v1, r, g, b, a);
        corner(buf, m, cx, ly, cz, -cr - sr, -sr + cr, u0, v0, r, g, b, a);
        corner(buf, m, cx, ly, cz, cr - sr, sr + cr, u1, v0, r, g, b, a);
        corner(buf, m, cx, ly, cz, cr + sr, sr - cr, u1, v1, r, g, b, a);
    }

    private static void corner(BufferBuilder buf, Matrix4f m, float cx, float cy, float cz, float ox, float oy,
                               float u, float v, float r, float g, float b, float a) {
        buf.vertex(m, cx + rx * ox, cy + oy, cz + rz * ox).texture(u, v).color(r, g, b, a).next();
    }

    private static float ribbon(BufferBuilder buf, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1,
                                float width, float run, float r, float g, float b, float a) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-4f || a <= 0.003f) return run + len;
        dx /= len;
        dy /= len;
        dz /= len;
        float sx = dy * nz, sy = dz * nx - dx * nz, sz = -dy * nx;
        float sl = MathHelper.sqrt(sx * sx + sy * sy + sz * sz);
        if (sl < 1e-4f) return run + len;
        float hw = width * 0.5f;
        sx = sx / sl * hw;
        sy = sy / sl * hw;
        sz = sz / sl * hw;
        float ex = dx * hw, ey = dy * hw, ez = dz * hw;
        float u0 = run * 4f, u1 = (run + len) * 4f;
        buf.vertex(m, x0 - ex + sx, y0 - ey + sy, z0 - ez + sz).texture(u0, 0f).color(r, g, b, a).next();
        buf.vertex(m, x0 - ex - sx, y0 - ey - sy, z0 - ez - sz).texture(u0, 1f).color(r, g, b, a).next();
        buf.vertex(m, x1 + ex - sx, y1 + ey - sy, z1 + ez - sz).texture(u1, 1f).color(r, g, b, a).next();
        buf.vertex(m, x1 + ex + sx, y1 + ey + sy, z1 + ez + sz).texture(u1, 0f).color(r, g, b, a).next();
        return run + len;
    }
}
