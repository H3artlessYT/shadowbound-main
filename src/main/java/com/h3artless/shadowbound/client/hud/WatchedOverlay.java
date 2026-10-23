package com.h3artless.shadowbound.client.hud;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.registry.ModEffects;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class WatchedOverlay {
    private static final Identifier VIGNETTE = Shadowbound.id("textures/misc/watched_vignette.png");
    private static final Identifier EYE = Shadowbound.id("textures/gui/watched_eye.png");

    private static float strength, prevStrength;
    private static int beat;

    private WatchedOverlay() {}

    public static void tick(MinecraftClient client) {
        prevStrength = strength;
        boolean watched = client.player != null && client.player.hasStatusEffect(ModEffects.WATCHED);
        strength = MathHelper.clamp(strength + (watched ? 0.03f : -0.05f), 0f, 1f);

        if (watched && --beat <= 0) {
            beat = 28 + client.player.getRandom().nextInt(20);
            client.player.playSound(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 0.35f, 0.7f);
        }
    }

    public static void render(DrawContext ctx, float tickDelta) {
        float s = MathHelper.lerp(tickDelta, prevStrength, strength);
        if (s <= 0.01f) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        float time = (client.player.age + tickDelta) / 20f;
        float breathe = 0.8f + 0.2f * MathHelper.sin(time * 1.7f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, s * breathe);
        ctx.drawTexture(VIGNETTE, 0, 0, w, h, 0, 0, 256, 256, 256, 256);

        float beatPulse = (float) Math.pow(Math.max(0.0, MathHelper.sin(time * 3.4f)), 12.0);
        float size = 32f + 4f * beatPulse;
        int ex = Math.round(w / 2f - size / 2f), ey = 6 - Math.round(beatPulse * 2f);
        RenderSystem.setShaderColor(1f, 1f, 1f, s * (0.55f + 0.45f * beatPulse));
        ctx.drawTexture(EYE, ex, ey, Math.round(size), Math.round(size), 0, 0, 16, 16, 16, 16);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}
