package com.h3artless.shadowbound.client.spy;

import com.h3artless.shadowbound.client.ClientShadowState;
import com.h3artless.shadowbound.client.ShadowboundClient;
import com.h3artless.shadowbound.meter.DarknessMeter;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public final class SpyHud {
    private SpyHud() {}

    public static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer font = client.textRenderer;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        float a = SpyCamera.fade(tickDelta);
        float time = client.world == null ? 0f : (client.world.getTime() + tickDelta) / 20f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int pad = 14 + Math.round(MathHelper.sin(time * 2f) * 2f);
        int len = 18;
        int col = alpha(a * 0.8f, 0xd8283c);
        bracket(ctx, pad, pad, len, 1, 1, col);
        bracket(ctx, w - pad, pad, len, -1, 1, col);
        bracket(ctx, pad, h - pad, len, 1, -1, col);
        bracket(ctx, w - pad, h - pad, len, -1, -1, col);

        LivingEntity target = SpyCamera.target();
        if (target != null) {
            Text name = target.getDisplayName();
            ctx.drawCenteredTextWithShadow(font, Text.translatable("hud.shadowbound.spy.watching"), w / 2, pad + 6, alpha(a * 0.7f, 0x8a8a8a));
            ctx.drawCenteredTextWithShadow(font, name, w / 2, pad + 17, alpha(a, 0xe6e6e6));

            String hp = String.format("♥ %.0f / %.0f", target.getHealth(), target.getMaxHealth());
            ctx.drawCenteredTextWithShadow(font, Text.literal(hp), w / 2, pad + 28, alpha(a * 0.8f, 0xd8283c));
        }

        if (MathHelper.sin(time * 1.3f) > -0.2f) {
            float flick = (time * 7f) % 1f > 0.93f ? 0.3f : 1f;
            ctx.drawCenteredTextWithShadow(font, Text.translatable("hud.shadowbound.spy.unseen"), w / 2, h / 2 + 40,
                    alpha(a * 0.35f * flick, 0xb02030));
        }

        float frac = MathHelper.clamp(ClientShadowState.darkness() / (float) DarknessMeter.MAX, 0f, 1f);
        int barW = 120, bx = w / 2 - barW / 2, by = h - pad - 14;
        ctx.fill(bx - 1, by - 1, bx + barW + 1, by + 3, alpha(a * 0.6f, 0x121212));
        ctx.fill(bx, by, bx + Math.round(barW * frac), by + 2, alpha(a, 0xd8283c));

        Text hint = Text.translatable("hud.shadowbound.spy.leave", ShadowboundClient.spyKey.getBoundKeyLocalizedText());
        ctx.drawCenteredTextWithShadow(font, hint, w / 2, by + 6, alpha(a * 0.5f, 0x8a8a8a));

        RenderSystem.disableBlend();
    }

    private static void bracket(DrawContext ctx, int x, int y, int len, int dx, int dy, int col) {
        int x2 = x + dx * len, y2 = y + dy * len;
        ctx.fill(Math.min(x, x2), y, Math.max(x, x2), y + dy, col);
        ctx.fill(x, Math.min(y, y2), x + dx, Math.max(y, y2), col);
    }

    private static int alpha(float a, int rgb) {
        int ai = MathHelper.clamp((int) (a * 255), 4, 255);
        return (ai << 24) | (rgb & 0xffffff);
    }
}
