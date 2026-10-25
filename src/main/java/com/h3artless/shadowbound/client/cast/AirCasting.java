package com.h3artless.shadowbound.client.cast;

import com.h3artless.shadowbound.entity.GrimoireEntity;
import com.h3artless.shadowbound.net.ModPackets;
import com.h3artless.shadowbound.ritual.RitualPatterns;
import com.h3artless.shadowbound.ritual.ShadowRitual;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import com.h3artless.shadowbound.Shadowbound;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AirCasting {
    public static final int RADIUS = 4;
    public static final float SPACING = 0.24f;
    public static final int IDLE = 0, SUCCESS = 1, FAIL = 2, CLOSING = 3, STEP = 4;
    public static final int STEP_TICKS = 16;
    private static final Identifier VIGNETTE = Shadowbound.id("textures/misc/watched_vignette.png");
    private static final String[] NUMERALS = {"I", "II", "III"};
    public static final int CLOSE_TICKS = 10;
    private static final Vec3d UP = new Vec3d(0, 1, 0);
    private static final int TIMEOUT = 20 * 30;

    private static final List<int[]> GRID = new ArrayList<>();
    private static final List<int[]> PATH = new ArrayList<>();
    private static final Set<Long> EDGES = new HashSet<>();
    private static final float[] ADDED = new float[RitualPatterns.MAX_LENGTH + 2];
    private static final List<String> DONE = new ArrayList<>();
    private static final List<List<int[]>> DONE_PATHS = new ArrayList<>();

    private static int bookId = -1;
    private static Vec3d center = Vec3d.ZERO, right = new Vec3d(1, 0, 0), normal = new Vec3d(0, 0, 1), book = Vec3d.ZERO;
    private static int state = IDLE;
    private static float stateAt, openAt, lastInput;
    private static boolean drawing, needRelease, aimValid;
    private static float aimX, aimY;
    @Nullable private static int[] aim;

    static {
        for (int q = -RADIUS; q <= RADIUS; q++) {
            for (int r = -RADIUS; r <= RADIUS; r++) {
                if (Math.abs(q) + Math.abs(r) + Math.abs(q + r) <= RADIUS * 2) GRID.add(new int[]{q, r});
            }
        }
    }

    private AirCasting() {}

    public static boolean active() {
        return bookId >= 0 && state != CLOSING;
    }

    public static boolean visible() {
        return bookId >= 0;
    }

    public static boolean isCasting(int id) {
        return active() && bookId == id;
    }

    public static List<int[]> grid() { return GRID; }
    public static List<int[]> path() { return Collections.unmodifiableList(PATH); }
    public static float addedAt(int i) { return ADDED[Math.min(i, ADDED.length - 1)]; }
    public static Vec3d center() { return center; }
    public static Vec3d right() { return right; }
    public static Vec3d normal() { return normal; }
    public static Vec3d book() { return book; }
    public static int state() { return state; }
    public static int stepsDone() { return DONE.size(); }
    public static List<int[]> donePath(int i) { return DONE_PATHS.get(i); }
    public static float stateAt() { return stateAt; }
    public static float openAt() { return openAt; }
    public static boolean drawing() { return drawing; }
    public static boolean aimValid() { return aimValid; }
    public static float aimX() { return aimX; }
    public static float aimY() { return aimY; }
    @Nullable public static int[] aim() { return aim; }

    public static float lx(int[] d) {
        return RitualPatterns.x(d[0], d[1]) * SPACING;
    }

    public static float ly(int[] d) {
        return -RitualPatterns.y(d[1]) * SPACING;
    }

    private static float now(MinecraftClient mc, float delta) {
        return mc.world == null ? 0f : mc.world.getTime() + delta;
    }

    public static void start(int id) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;
        if (bookId == id && state != CLOSING) return;
        Entity e = mc.world.getEntityById(id);
        if (!(e instanceof GrimoireEntity)) return;

        Vec3d at = e.getPos();
        Vec3d eye = p.getEyePos();
        Vec3d flat = new Vec3d(eye.x - at.x, 0, eye.z - at.z);
        if (flat.lengthSquared() < 1e-4) flat = new Vec3d(-p.getRotationVector().x, 0, -p.getRotationVector().z);
        normal = flat.normalize();
        right = normal.negate().crossProduct(UP).normalize();
        double cy = Math.max(at.y + 2.35, eye.y);
        center = new Vec3d(at.x, cy, at.z).add(normal.multiply(0.15));
        book = at.add(0, GrimoireEntity.HOVER, 0);

        bookId = id;
        state = IDLE;
        openAt = lastInput = now(mc, 0);
        needRelease = true;
        drawing = false;
        clearPath();
        clearSteps();
        sendPose(true);
        p.sendMessage(Text.translatable("message.shadowbound.cast.how").formatted(Formatting.DARK_RED), true);
        sound(SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.6f, 0.6f);
        sound(SoundEvents.BLOCK_SCULK_CATALYST_BLOOM, 0.8f, 0.8f);
    }

    public static void tick(MinecraftClient mc) {
        if (bookId < 0) return;
        float now = now(mc, 0);
        if (state == CLOSING) {
            if (now - stateAt > CLOSE_TICKS) bookId = -1;
            return;
        }
        boolean cancel = false;
        while (mc.options.attackKey.wasPressed()) cancel = true;
        while (mc.options.useKey.wasPressed()) lastInput = now;

        ClientPlayerEntity p = mc.player;
        Entity e = mc.world == null ? null : mc.world.getEntityById(bookId);
        boolean bookGone = !(e instanceof GrimoireEntity) || p == null || e.squaredDistanceTo(p) > 10 * 10;
        if (cancel || p == null || !p.isAlive() || p.isSneaking() || mc.currentScreen != null
                || (bookGone && state != SUCCESS) || now - lastInput > TIMEOUT) {
            close(now, true);
            return;
        }
        if (state == SUCCESS && now - stateAt > 18) close(now, false);
        if (state == STEP && now - stateAt > STEP_TICKS) {
            state = IDLE;
            clearPath();
        }
        if (state == FAIL && now - stateAt > 22) {
            state = IDLE;
            clearPath();
            clearSteps();
        }
        if ((state == IDLE || state == STEP) && p.getRandom().nextInt(70) == 0) {
            sound(SoundEvents.ENTITY_VEX_AMBIENT, 0.18f, 0.35f + p.getRandom().nextFloat() * 0.2f);
        }
    }

    public static void frame(float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        aimValid = false;
        aim = null;
        if (!active() || p == null) return;

        Vec3d eye = p.getCameraPosVec(tickDelta);
        Vec3d dir = p.getRotationVec(tickDelta);
        double denom = dir.dotProduct(normal);
        if (denom < -1e-3) {
            double t = center.subtract(eye).dotProduct(normal) / denom;
            if (t > 0 && t < 12) {
                Vec3d rel = eye.add(dir.multiply(t)).subtract(center);
                aimX = (float) rel.dotProduct(right);
                aimY = (float) rel.y;
                aimValid = true;
                aim = dotAt(aimX, aimY, SPACING * 0.42f);
            }
        }

        boolean holding = mc.options.useKey.isPressed();
        if (needRelease) {
            if (!holding) needRelease = false;
            return;
        }
        if (state != IDLE) return;
        float now = now(mc, tickDelta);
        if (holding) {
            lastInput = now;
            if (!drawing) {
                if (aim != null) {
                    clearPath();
                    add(aim, now);
                    drawing = true;
                    sound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.4f);
                }
            } else if (aim != null && PATH.size() < RitualPatterns.MAX_LENGTH + 1) {
                int[] last = PATH.get(PATH.size() - 1);
                if (aim != last && RitualPatterns.dirBetween(last[0], last[1], aim[0], aim[1]) >= 0 && EDGES.add(edgeKey(last, aim))) {
                    add(aim, now);
                    sound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.45f, 0.9f + PATH.size() * 0.04f);
                    sound(SoundEvents.ITEM_FIRECHARGE_USE, 0.12f, 1.6f);
                }
            }
        } else if (drawing) {
            finish(now);
        }
    }

    private static void finish(float now) {
        drawing = false;
        if (PATH.size() < 2) {
            clearPath();
            return;
        }
        String sig = RitualPatterns.signature(PATH);
        stateAt = now;
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (!ShadowRitual.anyContinues(DONE, sig)) {
            state = FAIL;
            sound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.7f, 0.7f);
            sound(SoundEvents.ENTITY_VEX_HURT, 0.4f, 0.5f);
            if (p != null) p.sendMessage(Text.translatable("message.shadowbound.cast.broken").formatted(Formatting.DARK_GRAY), true);
            return;
        }
        DONE.add(sig);
        DONE_PATHS.add(new ArrayList<>(PATH));
        if (DONE.size() < ShadowRitual.STEPS) {
            state = STEP;
            sound(SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.7f, 0.7f + DONE.size() * 0.15f);
            sound(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 0.9f, 0.6f);
            if (p != null) p.sendMessage(Text.translatable("message.shadowbound.cast.mark", NUMERALS[DONE.size() - 1]).formatted(Formatting.DARK_RED), true);
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(bookId);
        buf.writeVarInt(DONE.size());
        for (String step : DONE) buf.writeString(step, RitualPatterns.MAX_LENGTH);
        ClientPlayNetworking.send(ModPackets.CAST, buf);
        state = SUCCESS;
        sound(SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, 0.8f, 0.6f);
        sound(SoundEvents.BLOCK_BEACON_POWER_SELECT, 0.6f, 0.5f);
        sound(SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 0.5f, 1.2f);
    }

    private static void close(float now, boolean cancelled) {
        if (cancelled && state == IDLE) sound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.4f, 1.2f);
        sendPose(false);
        state = CLOSING;
        stateAt = now;
        drawing = false;
    }

    public static void reset() {
        bookId = -1;
        clearSteps();
        state = IDLE;
        drawing = false;
        clearPath();
    }

    private static void add(int[] d, float now) {
        ADDED[Math.min(PATH.size(), ADDED.length - 1)] = now;
        PATH.add(d);
    }

    private static void clearSteps() {
        DONE.clear();
        DONE_PATHS.clear();
    }

    private static void sendPose(boolean on) {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(on);
        ClientPlayNetworking.send(ModPackets.CAST_POSE, buf);
    }

    public static void renderHud(DrawContext ctx, float tickDelta) {
        if (bookId < 0) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        float now = now(mc, tickDelta);
        float a = smooth((now - openAt) / 14f) * 0.55f;
        if (state == CLOSING) a *= 1f - Math.min(1f, (now - stateAt) / CLOSE_TICKS);
        if (state == STEP) a += 0.35f * (1f - Math.min(1f, (now - stateAt) / STEP_TICKS));
        if (a <= 0.01f) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0.75f, 0.03f, 0.05f, Math.min(a, 1f));
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        ctx.drawTexture(VIGNETTE, 0, 0, 0, 0, w, h, w, h);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static float smooth(float x) {
        x = Math.max(0f, Math.min(1f, x));
        return x * x * (3f - 2f * x);
    }

    private static void clearPath() {
        PATH.clear();
        EDGES.clear();
    }

    @Nullable
    private static int[] dotAt(float x, float y, float reach) {
        float best = reach * reach;
        int[] hit = null;
        for (int[] d : GRID) {
            float dx = lx(d) - x, dy = ly(d) - y;
            float d2 = dx * dx + dy * dy;
            if (d2 < best) {
                best = d2;
                hit = d;
            }
        }
        return hit;
    }

    private static long edgeKey(int[] a, int[] b) {
        long ka = ((long) (a[0] + 64) << 8) | (a[1] + 64);
        long kb = ((long) (b[0] + 64) << 8) | (b[1] + 64);
        return ka < kb ? (ka << 16) | kb : (kb << 16) | ka;
    }

    private static void sound(SoundEvent s, float volume, float pitch) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p != null) p.playSound(s, volume, pitch);
    }
}
