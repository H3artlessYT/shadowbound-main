package com.h3artless.shadowbound.ritual;

import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

public final class RitualTimeline {
    public static final int IGNITE_END = 300;
    public static final int RISE_END = 900;
    public static final int GATHER_END = 2100;
    public static final int CRESC_END = 2700;
    public static final int HUSH = 2840;
    public static final int REVEAL = 2860;
    public static final int DURATION = 3000;

    public static final float RADIUS = 3.2f;
    public static final float ORB_Y = 1.75f;
    public static final float NODE_R = RADIUS * 0.715f;
    public static final int DISSOLVE_TIME = 70;

    private RitualTimeline() {}

    public static float sm(float x) {
        x = MathHelper.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    public static float hash(int n) {
        n = (n << 13) ^ n;
        n = n * (n * n * 15731 + 789221) + 1376312589;
        return (n & 0x7fffffff) / (float) 0x7fffffff;
    }

    public static final class Phases {
        public final float ignite, rise, gather, cresc, collapse, reveal, end;

        Phases(float t) {
            ignite = sm(t / IGNITE_END);
            rise = sm((t - IGNITE_END) / (RISE_END - IGNITE_END));
            gather = sm((t - RISE_END) / (GATHER_END - RISE_END));
            cresc = sm((t - GATHER_END) / (CRESC_END - GATHER_END));
            collapse = sm((t - CRESC_END) / (HUSH - CRESC_END));
            reveal = sm((t - REVEAL) / 30f);
            end = sm((t - (DURATION - 60)) / 60f);
        }
    }

    public static Phases phases(float t) {
        return new Phases(t);
    }

    public static float spinRing(float t) {
        Phases p = phases(t);
        return 0.05f * t + 220f * p.cresc * p.cresc + 540f * p.collapse * p.collapse;
    }

    public static float spinSeal(float t) {
        Phases p = phases(t);
        return -(0.08f * t + 360f * p.cresc * p.cresc + 900f * p.collapse * p.collapse);
    }

    public static int nodeIgnite(int k) {
        return 50 + k * 45;
    }

    public static float[] nodePos(int k, float t) {
        float a = (-90f + 72f * k + spinRing(t)) * MathHelper.RADIANS_PER_DEGREE;
        return new float[]{MathHelper.cos(a) * NODE_R, 0.05f, MathHelper.sin(a) * NODE_R};
    }

    public static float orbRadius(float t) {
        if (t >= REVEAL) return 0f;
        Phases p = phases(t);
        float grow = sm((t - RISE_END) / 1000f);
        float beat = (float) Math.pow(Math.max(0, MathHelper.sin(t * 0.26f)), 10) * 0.06f * p.cresc;
        return (0.55f * grow + 0.2f * p.cresc + beat) * (1f - 0.92f * p.collapse);
    }

    public record Arc(int id, float age, int node) {}

    @Nullable
    public static Arc arcAt(float t) {
        if (t < RISE_END + 100 || t > HUSH) return null;
        Phases p = phases(t);
        int period = Math.round(MathHelper.lerp(Math.max(p.cresc, p.collapse), 70f, 18f));
        int id = MathHelper.floor(t / period);
        float age = t - id * period;
        if (age > 9 || hash(id * 31 + 7) < 0.2f) return null;
        return new Arc(id, age, (int) (hash(id * 13 + 1) * 5) % 5);
    }

    public static float dissolveStart(int i, int n) {
        return RISE_END + 200 + i * (GATHER_END - RISE_END - 300) / (float) Math.max(1, n);
    }

    public record Offering(float x, float y, float z, float scale, boolean gone) {}

    public static Offering offering(int i, int n, float t) {
        float lift = sm((t - IGNITE_END) / 100f);
        float ang = i / (float) n * (float) (Math.PI * 2) + t * 0.012f;
        float r = 1.9f;
        float y = 0.2f + lift * (1.1f + 0.12f * MathHelper.sin(t * 0.05f + i * 1.7f));
        float bx = MathHelper.cos(ang) * r, bz = MathHelper.sin(ang) * r;
        float d = sm((t - dissolveStart(i, n)) / DISSOLVE_TIME);
        return new Offering(
                MathHelper.lerp(d * d, bx, 0f),
                MathHelper.lerp(d, y, ORB_Y),
                MathHelper.lerp(d * d, bz, 0f),
                (1f - d) * (0.25f + 0.75f * lift),
                d >= 1f);
    }

    public static int glyphBorn(int i) {
        return 420 + i * 40;
    }
}
