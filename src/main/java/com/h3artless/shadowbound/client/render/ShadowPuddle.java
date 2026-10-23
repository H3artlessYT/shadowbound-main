package com.h3artless.shadowbound.client.render;

import com.h3artless.shadowbound.client.dive.DiveTrails;
import com.h3artless.shadowbound.client.dive.DiveTrails.Sample;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

public final class ShadowPuddle {
    private static final int PUFF = 4, SPARK = 1;

    private ShadowPuddle() {}

    public static void render(PlayerEntity player, float tickDelta, MatrixStack matrices, VertexConsumerProvider buffers) {
        Matrix4f m = matrices.peek().getPositionMatrix();
        Vec3d head = player.getLerpedPos(tickDelta);
        float seed = MathHelper.fractionalPart(player.getId() * 0.618034f);
        float now = player.getWorld().getTime() + tickDelta;
        Deque<Sample> trail = DiveTrails.get(player.getId());

        drawTrail(buffers, m, head, trail, now, seed);
        drawPuddle(buffers, m, player, tickDelta, seed);
        drawSmoke(buffers, m, head, trail, now);
        ShadowWisps.draw(player, tickDelta, matrices, buffers);
    }

    private record Pt(float x, float y, float z, float age, float odo) {}

    private static void drawTrail(VertexConsumerProvider buffers, Matrix4f m, Vec3d head, Deque<Sample> trail, float now, float seed) {
        if (trail.isEmpty()) return;
        List<Pt> pts = new ArrayList<>();
        Sample newest = trail.peekFirst();
        float headOdo = (float) (newest.odo() + Math.hypot(head.x - newest.x(), head.z - newest.z()));
        pts.add(new Pt(0, 0, 0, 0, headOdo));
        for (Sample s : trail) {
            float x = (float) (s.x() - head.x), y = (float) (s.y() - head.y), z = (float) (s.z() - head.z);
            Pt last = pts.get(pts.size() - 1);
            if (Math.hypot(x - last.x(), z - last.z()) < 0.08) continue;
            pts.add(new Pt(x, y, z, Math.min(1f, (now - s.tick()) / DiveTrails.HISTORY), (float) s.odo()));
        }
        if (pts.size() < 2) return;

        int n = pts.size();
        float[] lx = new float[n], lz = new float[n], rx = new float[n], rz = new float[n], yy = new float[n];
        for (int i = 0; i < n; i++) {
            Pt a = pts.get(Math.max(0, i - 1)), b = pts.get(Math.min(n - 1, i + 1));
            float dx = a.x() - b.x(), dz = a.z() - b.z();
            float l = (float) Math.hypot(dx, dz);
            if (l < 1e-4f) l = 1;
            dx /= l;
            dz /= l;
            float age = pts.get(i).age();
            float k = Math.min(1f, age / 0.12f), taper = k * k * (3 - 2 * k);
            float w = (0.62f * (float) Math.pow(1 - age, 0.5) + 0.08f) * (0.15f + 0.85f * taper);
            Pt p = pts.get(i);
            lx[i] = p.x() - dz * w;
            lz[i] = p.z() + dx * w;
            rx[i] = p.x() + dz * w;
            rz[i] = p.z() - dx * w;
            yy[i] = p.y() + 0.015f;
        }

        VertexConsumer out = buffers.getBuffer(SmokeLayers.trail());
        for (int i = 0; i < n - 1; i++) {
            Pt a = pts.get(i), b = pts.get(i + 1);
            out.vertex(m, lx[i], yy[i], lz[i]).texture(0, a.odo()).color(seed, a.age(), 0f, 1f).next();
            out.vertex(m, rx[i], yy[i], rz[i]).texture(1, a.odo()).color(seed, a.age(), 0f, 1f).next();
            out.vertex(m, rx[i + 1], yy[i + 1], rz[i + 1]).texture(1, b.odo()).color(seed, b.age(), 0f, 1f).next();
            out.vertex(m, lx[i + 1], yy[i + 1], lz[i + 1]).texture(0, b.odo()).color(seed, b.age(), 0f, 1f).next();
        }
    }

    private static void drawPuddle(VertexConsumerProvider buffers, Matrix4f m, PlayerEntity player, float tickDelta, float seed) {
        Vec3d v = player.getVelocity();
        double speed = Math.sqrt(v.x * v.x + v.z * v.z);
        float dirRad = speed > 0.01
                ? (float) Math.atan2(v.z, v.x)
                : (player.getYaw(tickDelta) + 90f) * MathHelper.RADIANS_PER_DEGREE;
        float fx = MathHelper.cos(dirRad), fz = MathHelper.sin(dirRad);
        float sx = -fz, sz = fx;

        float breathe = 1f + 0.04f * MathHelper.sin((player.age + tickDelta) * 0.12f);
        float len = 0.6f * (1f + (float) Math.min(speed * 2.0, 0.6)) * breathe, wid = 0.6f * breathe;
        float lag = (float) Math.min(speed, 0.12);
        float cx = -fx * lag, cz = -fz * lag, y = 0.025f;
        float sp = (float) Math.min(speed * 4.0, 1.0);

        VertexConsumer out = buffers.getBuffer(SmokeLayers.puddle());
        corner(out, m, cx - fx * len - sx * wid, y, cz - fz * len - sz * wid, 0, 1, seed, sp);
        corner(out, m, cx - fx * len + sx * wid, y, cz - fz * len + sz * wid, 1, 1, seed, sp);
        corner(out, m, cx + fx * len + sx * wid, y, cz + fz * len + sz * wid, 1, 0, seed, sp);
        corner(out, m, cx + fx * len - sx * wid, y, cz + fz * len - sz * wid, 0, 0, seed, sp);
    }

    private static void corner(VertexConsumer out, Matrix4f m, float x, float y, float z, float u, float v, float seed, float speed) {
        out.vertex(m, x, y, z).texture(u, v).color(seed, speed, 0f, 1f).next();
    }

    private record Puff(float x, float y, float z, float size, int cell, float r, float g, float b, float a, float roll, boolean glow) {}

    private static void drawSmoke(VertexConsumerProvider buffers, Matrix4f m, Vec3d head, Deque<Sample> trail, float now) {
        List<Puff> puffs = new ArrayList<>();
        int stride = FxQuality.low() ? 2 : 1, idx = 0;
        for (Sample s : trail) {
            if (idx++ % stride != 0) continue;
            float a = now - s.tick();
            if (a < 1 || a > DiveTrails.HISTORY) continue;
            float k = a / DiveTrails.HISTORY;
            int tk = (int) s.tick();
            float x = (float) (s.x() - head.x), y = (float) (s.y() - head.y), z = (float) (s.z() - head.z);

            float ang = hash(tk * 7) * (float) (Math.PI * 2), out = (0.1f + 0.2f * hash(tk * 11)) * k;
            float sway = MathHelper.sin(a * 0.15f + tk) * 0.08f * k;
            puffs.add(new Puff(x + MathHelper.cos(ang) * out + sway, y + 0.1f + a * 0.028f, z + MathHelper.sin(ang) * out,
                    0.28f + a * 0.02f, PUFF + (tk & 3), 0.04f, 0.035f, 0.04f,
                    MathHelper.sin((float) Math.PI * Math.min(1f, k * 1.3f)) * 0.6f, hash(tk * 3) * 6 + a * 0.04f, false));
            if (tk % 5 == 0) {
                puffs.add(new Puff(x, y + 0.1f + a * 0.05f, z, 0.06f, SPARK, 0.95f, 0.1f, 0.1f, (1 - k) * 0.9f, 0, true));
            }
        }
        if (puffs.isEmpty()) return;

        Camera cam = MinecraftClient.getInstance().gameRenderer.getCamera();
        Quaternionf rot = cam.getRotation();
        rot.transform(1f, 0f, 0f, RIGHT);
        rot.transform(0f, 1f, 0f, UP);
        Vec3d eye = cam.getPos().subtract(head);
        puffs.sort(Comparator.comparingDouble((Puff p) -> eye.squaredDistanceTo(p.x(), p.y(), p.z())).reversed());

        VertexConsumer smoke = buffers.getBuffer(SmokeLayers.puff());
        for (Puff p : puffs) if (!p.glow()) sprite(smoke, m, rot, p);
        VertexConsumer glow = buffers.getBuffer(SmokeLayers.puffGlow());
        for (Puff p : puffs) if (p.glow()) sprite(glow, m, rot, p);
    }

    private static final Vector3f RIGHT = new Vector3f();
    private static final Vector3f UP = new Vector3f();

    private static void sprite(VertexConsumer out, Matrix4f m, Quaternionf rot, Puff p) {
        if (p.a() <= 0.004f) return;
        float cu = (p.cell() & 3) * 0.25f, cv = (p.cell() >> 2) * 0.25f;
        float cr = MathHelper.cos(p.roll()) * p.size(), sr = MathHelper.sin(p.roll()) * p.size();
        float ax = cr * RIGHT.x + sr * UP.x, ay = cr * RIGHT.y + sr * UP.y, az = cr * RIGHT.z + sr * UP.z;
        float bx = -sr * RIGHT.x + cr * UP.x, by = -sr * RIGHT.y + cr * UP.y, bz = -sr * RIGHT.z + cr * UP.z;
        float x = p.x(), y = p.y(), z = p.z();
        out.vertex(m, x - ax - bx, y - ay - by, z - az - bz).texture(cu + 0.25f, cv + 0.25f).color(p.r(), p.g(), p.b(), p.a()).next();
        out.vertex(m, x - ax + bx, y - ay + by, z - az + bz).texture(cu + 0.25f, cv).color(p.r(), p.g(), p.b(), p.a()).next();
        out.vertex(m, x + ax + bx, y + ay + by, z + az + bz).texture(cu, cv).color(p.r(), p.g(), p.b(), p.a()).next();
        out.vertex(m, x + ax - bx, y + ay - by, z + az - bz).texture(cu, cv + 0.25f).color(p.r(), p.g(), p.b(), p.a()).next();
    }

    private static float hash(int n) {
        n = (n << 13) ^ n;
        n = n * (n * n * 15731 + 789221) + 1376312589;
        return (n & 0x7fffffff) / (float) 0x7fffffff;
    }
}
