package com.h3artless.shadowbound.client.dive;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.dive.DiveHolder;
import com.h3artless.shadowbound.dive.ShadowDive;
import com.h3artless.shadowbound.mixin.client.PostEffectProcessorAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

public final class DiveVision {
    private static final Identifier POST = Shadowbound.id("shaders/post/shadow_dive.json");
    public static final int SENSE_COLOR = 0xc8141e;
    private static final double SENSE_RANGE = 32.0;

    @Nullable private static PostEffectProcessor post;
    private static int postW, postH;
    private static float fade, prevFade;
    private static float danger, prevDanger;
    private static float speed, prevSpeed;
    private static boolean wasDiving;

    private DiveVision() {}

    public static boolean isDiving() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        return p != null && ShadowDive.isDiving(p);
    }

    public static boolean senses(Entity e) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        return p != null && e != p && e instanceof LivingEntity && e.isAlive()
                && ShadowDive.isDiving(p) && e.squaredDistanceTo(p) < SENSE_RANGE * SENSE_RANGE;
    }

    public static void tick(MinecraftClient client) {
        prevFade = fade;
        prevDanger = danger;
        prevSpeed = speed;
        ClientPlayerEntity p = client.player;
        boolean diving = p != null && ShadowDive.isDiving(p);

        fade = MathHelper.clamp(fade + (diving ? 0.09f : -0.14f), 0f, 1f);
        if (diving) {
            int light = ((DiveHolder) p).shadowbound$getDiveLight();
            float want = MathHelper.clamp((light - (ShadowDive.MAX_ENTER_LIGHT - 1)) / (float) (ShadowDive.BURN_LIGHT - ShadowDive.MAX_ENTER_LIGHT + 1), 0f, 1f);
            danger += (want - danger) * 0.2f;
            float sp = (float) p.getVelocity().horizontalLength();
            speed += (MathHelper.clamp(sp * 4f, 0f, 1f) - speed) * 0.25f;
            if (danger > 0.6f && p.age % 6 == 0) p.playSound(SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.15f, 1.6f);
        } else {
            danger *= 0.8f;
            speed *= 0.8f;
        }

        if (diving && !wasDiving && p != null) {
            p.playSound(SoundEvents.AMBIENT_UNDERWATER_ENTER, 0.6f, 0.5f);
        } else if (!diving && wasDiving && p != null) {
            p.playSound(SoundEvents.AMBIENT_UNDERWATER_EXIT, 0.6f, 0.6f);
        }
        wasDiving = diving;

        if (fade == 0f && post != null) close();
    }

    public static void renderPost(float tickDelta) {
        float f = MathHelper.lerp(tickDelta, prevFade, fade);
        if (f <= 0.001f) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (post == null && !load(client)) return;

        int w = client.getWindow().getFramebufferWidth(), h = client.getWindow().getFramebufferHeight();
        if (w != postW || h != postH) {
            post.setupDimensions(w, h);
            postW = w;
            postH = h;
        }

        float clock = client.world == null ? 0f : ((client.world.getTime() % 72000L) + tickDelta) / 20f;
        for (PostEffectPass pass : ((PostEffectProcessorAccessor) post).shadowbound$passes()) {
            pass.getProgram().getUniformByNameOrDummy("Fade").set(f);
            pass.getProgram().getUniformByNameOrDummy("Clock").set(clock);
            pass.getProgram().getUniformByNameOrDummy("Danger").set(MathHelper.lerp(tickDelta, prevDanger, danger));
            pass.getProgram().getUniformByNameOrDummy("Speed").set(MathHelper.lerp(tickDelta, prevSpeed, speed));
        }

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        post.render(tickDelta);
        client.getFramebuffer().beginWrite(true);
        RenderSystem.enableDepthTest();
    }

    private static boolean load(MinecraftClient client) {
        try {
            post = new PostEffectProcessor(client.getTextureManager(), client.getResourceManager(), client.getFramebuffer(), POST);
            postW = postH = -1;
            return true;
        } catch (Exception e) {
            Shadowbound.LOGGER.error("couldn't load shadow dive shader", e);
            post = null;
            fade = prevFade = 0f;
            return false;
        }
    }

    public static void reset() {
        fade = prevFade = 0f;
        danger = prevDanger = 0f;
        wasDiving = false;
        close();
    }

    private static void close() {
        if (post != null) {
            post.close();
            post = null;
        }
    }
}
