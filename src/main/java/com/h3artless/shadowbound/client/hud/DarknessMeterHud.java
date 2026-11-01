package com.h3artless.shadowbound.client.hud;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.client.ClientShadowState;
import com.h3artless.shadowbound.meter.DarknessMeter;
import com.h3artless.shadowbound.registry.ModItems;
import com.h3artless.shadowbound.spy.SpyManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class DarknessMeterHud {
    private static final Identifier FRAME = Shadowbound.id("textures/gui/darkness_meter.png");
    private static final Identifier FILL = Shadowbound.id("textures/gui/darkness_fill.png");

    private static final int FRAME_W = 14, FRAME_H = 92;
    private static final int TEX_W = 32, TEX_H = 96;
    private static final int INNER_X = 4, INNER_Y = 10, INNER_W = 6, INNER_H = 72;
    private static final int PATTERN_W = 8, PATTERN_H = 32;

    private static float alpha, prevAlpha;
    private static float shown, prevShown;

    private DarknessMeterHud() {}

    public static void tick(MinecraftClient client) {
        prevAlpha = alpha;
        prevShown = shown;
        boolean visible = client.player != null && (DarknessMeter.attuned(client.player)
                || client.player.getMainHandStack().isOf(ModItems.SHADOW_KEY) || client.player.getOffHandStack().isOf(ModItems.SHADOW_KEY));
        alpha = MathHelper.clamp(alpha + (visible ? 0.12f : -0.08f), 0f, 1f);
        shown += (ClientShadowState.darkness() - shown) * 0.25f;
    }

    public static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.player == null) return;
        float a = MathHelper.lerp(tickDelta, prevAlpha, alpha);
        if (a <= 0.01f) return;

        float value = MathHelper.lerp(tickDelta, prevShown, shown);
        float frac = MathHelper.clamp(value / DarknessMeter.MAX, 0f, 1f);
        float time = (client.player.age + tickDelta) / 20f;

        int x = 4 - Math.round((1f - a) * 10f);
        int y = ctx.getScaledWindowHeight() / 2 - FRAME_H / 2;
        int ix = x + INNER_X, iy = y + INNER_Y, bottom = iy + INNER_H;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        ctx.fill(ix, iy, ix + INNER_W, bottom, argb(0.85f * a, 0x0a0405));

        RenderSystem.setShaderColor(1f, 1f, 1f, a);
        float surface = bottom - frac * INNER_H;
        int scroll = (int) (time * 14f);
        boolean full = frac >= 0.999f;
        int[] tops = new int[INNER_W];
        for (int c = 0; c < INNER_W; c++) {
            float wobble = full || frac <= 0.001f ? 0f
                    : MathHelper.sin(time * 4.2f + c * 1.1f) * 1.1f + MathHelper.sin(time * 2.3f - c * 0.7f) * 0.6f;
            int top = MathHelper.clamp(Math.round(surface + wobble), iy, bottom);
            tops[c] = top;
            if (top < bottom) {
                tileColumn(ctx, ix + c, top, bottom - top, c % PATTERN_W, Math.floorMod(top + scroll, PATTERN_H));
            }
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        if (frac > 0.001f && !full) {
            for (int c = 0; c < INNER_W; c++) {
                ctx.fill(ix + c, tops[c], ix + c + 1, tops[c] + 1, argb(0.9f * a, 0xff5a5a));
            }
        }

        int fillH = Math.round(frac * INNER_H);
        if (fillH > 3) {
            for (int i = 0; i < 6; i++) {
                float speed = 6f + (i * 37 % 7);
                float yy = (time * speed + i * 23.7f) % fillH;
                int my = bottom - 1 - (int) yy;
                int mx = ix + (i * 5 + (int) (time * 0.7f + i)) % INNER_W;
                if (my > tops[mx - ix]) {
                    float tw = 0.5f + 0.5f * MathHelper.sin(time * 6f + i);
                    ctx.fill(mx, my, mx + 1, my + 1, argb(a * (0.4f + 0.5f * tw), 0xffc8c8));
                }
            }
        }

        float pulse = full ? 0.75f + 0.25f * MathHelper.sin(time * 5f) : 1f;
        RenderSystem.setShaderColor(1f, pulse, pulse, a);
        ctx.drawTexture(FRAME, x, y, 0, 0, FRAME_W, FRAME_H, TEX_W, TEX_H);

        if (value >= SpyManager.START_COST) {
            float blink = 0.6f + 0.4f * MathHelper.sin(time * 3f);
            RenderSystem.setShaderColor(1f, 1f, 1f, a * blink);
            ctx.drawTexture(FRAME, x, y, 16, 0, FRAME_W, FRAME_H, TEX_W, TEX_H);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        int notchY = bottom - Math.round(INNER_H * SpyManager.START_COST / (float) DarknessMeter.MAX);
        int notchCol = value >= SpyManager.START_COST ? 0xff4a5a : 0x4a1a1e;
        ctx.fill(x + FRAME_W, notchY, x + FRAME_W + 2, notchY + 1, argb(a, notchCol));

        RenderSystem.disableBlend();
    }

    private static void tileColumn(DrawContext ctx, int x, int y, int h, int u, int v) {
        while (h > 0) {
            int seg = Math.min(h, PATTERN_H - v);
            ctx.drawTexture(FILL, x, y, u, v, 1, seg, PATTERN_W, PATTERN_H);
            y += seg;
            h -= seg;
            v = 0;
        }
    }

    static int argb(float a, int rgb) {
        return (MathHelper.clamp((int) (a * 255f), 0, 255) << 24) | (rgb & 0xffffff);
    }
}
