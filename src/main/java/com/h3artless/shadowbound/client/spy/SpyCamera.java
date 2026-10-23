package com.h3artless.shadowbound.client.spy;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.mixin.client.PostEffectProcessorAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

public final class SpyCamera {
    private static final Identifier POST = Shadowbound.id("shaders/post/spy.json");

    private static boolean active;
    private static int targetId = -1;
    @Nullable private static Perspective savedPerspective;
    @Nullable private static PostEffectProcessor post;
    private static int postW, postH;
    private static float fade, prevFade;

    private SpyCamera() {}

    public static boolean isActive() {
        return active;
    }

    @Nullable
    public static LivingEntity target() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || targetId < 0) return null;
        return client.world.getEntityById(targetId) instanceof LivingEntity l ? l : null;
    }

    public static float fade(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevFade, fade);
    }

    public static void begin(int entityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        targetId = entityId;
        if (!active) savedPerspective = client.options.getPerspective();
        active = true;
        client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        if (client.player != null) client.player.playSound(SoundEvents.ENTITY_ENDERMAN_STARE, 0.5f, 0.6f);
    }

    public static void end() {
        if (!active) return;
        MinecraftClient client = MinecraftClient.getInstance();
        active = false;
        targetId = -1;
        if (savedPerspective != null) client.options.setPerspective(savedPerspective);
        savedPerspective = null;
    }

    public static void reset() {
        end();
        fade = prevFade = 0f;
        closePost();
    }

    public static void tick(MinecraftClient client) {
        prevFade = fade;
        fade = MathHelper.clamp(fade + (active ? 0.07f : -0.1f), 0f, 1f);
        if (!active && fade == 0f && post != null) closePost();
        if (active && client.options.getPerspective() != Perspective.THIRD_PERSON_BACK) {
            client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
    }

    public static void renderPost(float tickDelta) {
        float f = fade(tickDelta);
        if (f <= 0.001f) return;
        MinecraftClient client = MinecraftClient.getInstance();

        if (post == null && !loadPost(client)) return;
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
        }

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        post.render(tickDelta);
        client.getFramebuffer().beginWrite(true);
        RenderSystem.enableDepthTest();
    }

    private static boolean loadPost(MinecraftClient client) {
        try {
            post = new PostEffectProcessor(client.getTextureManager(), client.getResourceManager(), client.getFramebuffer(), POST);
            postW = postH = -1;
            return true;
        } catch (Exception e) {
            Shadowbound.LOGGER.error("couldn't load spy shader", e);
            post = null;
            fade = prevFade = 0f;
            return false;
        }
    }

    private static void closePost() {
        if (post != null) {
            post.close();
            post = null;
        }
    }
}
