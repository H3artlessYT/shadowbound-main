package com.h3artless.shadowbound.client.hud;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.client.render.ShadowShaders;
import com.h3artless.shadowbound.cooldown.Ability;
import com.h3artless.shadowbound.cooldown.Cooldowns;
import com.h3artless.shadowbound.item.ShadowBlade;
import com.h3artless.shadowbound.item.TotemOfShadownessItem;
import com.h3artless.shadowbound.registry.ModEnchantments;
import com.h3artless.shadowbound.registry.ModParticles;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlBlendState;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class CooldownHud {
    private static final Identifier ATLAS = Shadowbound.id("textures/misc/cast_sigil.png");
    private static final Identifier FILL = Shadowbound.id("textures/gui/darkness_fill.png");
    private static final Ability[] ALL = Ability.values();
    private static final int FLASH_TICKS = 14;
    private static final float[][] ICONS = {
            {0.75f, 0.5f},
            {0.125f, 0.5f},
            {0.5f, 0.625f},
            {0f, 0.5f}
    };

    private static final int[] PREV = new int[ALL.length];
    private static final int[] FLASH = new int[ALL.length];
    private static final List<Ability> SHOWN = new ArrayList<>();
    private static float show, prevShow;

    private CooldownHud() {}

    private static void collect(ClientPlayerEntity p, List<Ability> out) {
        out.clear();
        for (Hand hand : Hand.values()) {
            ItemStack s = p.getStackInHand(hand);
            if (ShadowBlade.isBlade(s)) {
                Ability a = EnchantmentHelper.getLevel(ModEnchantments.SOUL_PYLON, s) > 0 ? Ability.PYLON : Ability.SHADOWS;
                if (!out.contains(a)) out.add(a);
            }
        }
        if (TotemOfShadownessItem.isHolding(p)) {
            if (!out.contains(Ability.TOTEM)) out.add(Ability.TOTEM);
            if (!out.contains(Ability.SHADOW_RESPAWN)) out.add(Ability.SHADOW_RESPAWN);
        }
    }

    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        prevShow = show;
        if (p == null) return;
        Cooldowns.tick(p);
        List<Ability> now = new ArrayList<>(4);
        collect(p, now);
        if (!now.isEmpty()) {
            SHOWN.clear();
            SHOWN.addAll(now);
        }
        show = MathHelper.clamp(show + (now.isEmpty() ? -0.15f : 0.2f), 0f, 1f);

        for (Ability a : ALL) {
            int i = a.ordinal();
            int cur = Cooldowns.remaining(p, a);
            if (PREV[i] > 0 && cur == 0) {
                FLASH[i] = FLASH_TICKS;
                burst(mc, p, now.contains(a));
            }
            PREV[i] = cur;
            if (FLASH[i] > 0) FLASH[i]--;
        }

        if (p.age % 7 == 0) {
            for (Hand hand : Hand.values()) {
                ItemStack s = p.getStackInHand(hand);
                if (!ShadowBlade.isBlade(s) || ShadowBlade.count(s) == 0) continue;
                Ability a = EnchantmentHelper.getLevel(ModEnchantments.SOUL_PYLON, s) > 0 ? Ability.PYLON : Ability.SHADOWS;
                if (!Cooldowns.ready(p, a)) continue;
                Vec3d h = handPos(p, hand);
                mc.world.addParticle(ModParticles.MOTE, h.x, h.y, h.z, 0.12, 0.01, 0);
                if (p.getRandom().nextInt(3) == 0) mc.world.addParticle(ModParticles.RUNE, h.x, h.y, h.z, 0, 0, 0);
            }
        }
    }

    private static Vec3d handPos(ClientPlayerEntity p, Hand hand) {
        boolean right = (hand == Hand.MAIN_HAND) == (p.getMainArm() == Arm.RIGHT);
        float yaw = p.bodyYaw * MathHelper.RADIANS_PER_DEGREE;
        double side = right ? -0.38 : 0.38;
        return p.getPos().add(MathHelper.cos(yaw) * side, 0.75, MathHelper.sin(yaw) * side);
    }

    private static void burst(MinecraftClient mc, ClientPlayerEntity p, boolean visible) {
        p.playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5f, 1.5f);
        p.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.6f);
        if (!visible || mc.world == null) return;
        for (int i = 0; i < 10; i++) {
            mc.world.addParticle(ModParticles.MOTE, p.getX(), p.getY() + 0.2 + i * 0.12, p.getZ(),
                    0.6 + p.getRandom().nextDouble() * 0.3, 0.02, i * 0.63);
        }
        for (int i = 0; i < 5; i++) {
            mc.world.addParticle(ModParticles.RUNE, p.getX() + (p.getRandom().nextDouble() - 0.5) * 0.8, p.getY() + 0.4,
                    p.getZ() + (p.getRandom().nextDouble() - 0.5) * 0.8, 0, 0.03, 0);
        }
    }

    public static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        float vis = MathHelper.lerp(tickDelta, prevShow, show);
        if (p == null || vis <= 0.01f || SHOWN.isEmpty() || mc.options.hudHidden) return;
        if (ShadowShaders.cooldownSeal == null || ShadowShaders.airGlyph == null) return;

        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        float size = 13f;
        float slide = (1f - vis) * 14f;
        float cy = h - 12f + slide;
        float x = w / 2f + 91f + 6f + size + 2f;
        if (p.getMainArm() == Arm.LEFT && !p.getOffHandStack().isEmpty()) x += 29f;
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        float t = (p.age + tickDelta);

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        int n = SHOWN.size();
        mc.getTextureManager().getTexture(FILL).setFilter(false, false);
        BufferBuilder buf = begin(ShadowShaders.cooldownSeal);
        RenderSystem.setShaderTexture(0, FILL);
        for (int i = 0; i < n; i++) {
            Ability a = SHOWN.get(i);
            float prog = MathHelper.clamp(1f - (Cooldowns.remaining(p, a) - tickDelta) / a.ticks, 0f, 1f);
            if (Cooldowns.remaining(p, a) <= 0) prog = 1f;
            float flash = Math.max(0f, (FLASH[a.ordinal()] - tickDelta) / FLASH_TICKS);
            float s = size * (1f + 0.25f * flash);
            quad(buf, m, x + i * (size * 2f + 4f), cy, s * 1.3f, prog, flash, 0f, vis);
        }
        end(buf);

        mc.getTextureManager().getTexture(ATLAS).setFilter(true, false);
        buf = begin(ShadowShaders.airGlyph);
        RenderSystem.setShaderTexture(0, ATLAS);
        for (int i = 0; i < n; i++) {
            Ability a = SHOWN.get(i);
            boolean ready = Cooldowns.remaining(p, a) <= 0;
            float[] ic = ICONS[a.ordinal()];
            float pulse = ready ? 0.85f + 0.15f * MathHelper.sin(t * 0.25f + i) : 0.45f;
            float s = size * (ready ? 0.62f : 0.5f);
            iconQuad(buf, m, x + i * (size * 2f + 4f), cy, s, ic[0], ic[1], 0.95f, ready ? 0.2f : 0.06f, ready ? 0.16f : 0.08f, pulse * vis);
        }
        end(buf);

        new GlBlendState().enable();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        for (int i = 0; i < n; i++) {
            Ability a = SHOWN.get(i);
            int left = Cooldowns.remaining(p, a);
            if (left <= 0) continue;
            String txt = left >= 1200 ? ((left + 1199) / 1200) + "m" : String.valueOf((left + 19) / 20);
            float cx = x + i * (size * 2f + 4f);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(cx, cy + size + 2.5f, 0);
            ctx.getMatrices().scale(0.75f, 0.75f, 1f);
            int tw = mc.textRenderer.getWidth(txt);
            int alpha = MathHelper.clamp((int) (vis * 255), 4, 255);
            ctx.drawText(mc.textRenderer, txt, -tw / 2, 0, (alpha << 24) | 0xd8323a, true);
            ctx.getMatrices().pop();
        }
    }

    private static BufferBuilder begin(ShaderProgram program) {
        RenderSystem.setShader(() -> program);
        BufferBuilder buf = Tessellator.getInstance().getBuffer();
        buf.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        return buf;
    }

    private static void end(BufferBuilder buf) {
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private static void quad(BufferBuilder buf, Matrix4f m, float cx, float cy, float half, float r, float g, float b, float a) {
        buf.vertex(m, cx - half, cy - half, 0).texture(0f, 0f).color(r, g, b, a).next();
        buf.vertex(m, cx - half, cy + half, 0).texture(0f, 1f).color(r, g, b, a).next();
        buf.vertex(m, cx + half, cy + half, 0).texture(1f, 1f).color(r, g, b, a).next();
        buf.vertex(m, cx + half, cy - half, 0).texture(1f, 0f).color(r, g, b, a).next();
    }

    private static void iconQuad(BufferBuilder buf, Matrix4f m, float cx, float cy, float half, float u, float v,
                                 float r, float g, float b, float a) {
        float u1 = u + 0.125f, v1 = v + 0.125f;
        buf.vertex(m, cx - half, cy - half, 0).texture(u, v).color(r, g, b, a).next();
        buf.vertex(m, cx - half, cy + half, 0).texture(u, v1).color(r, g, b, a).next();
        buf.vertex(m, cx + half, cy + half, 0).texture(u1, v1).color(r, g, b, a).next();
        buf.vertex(m, cx + half, cy - half, 0).texture(u1, v).color(r, g, b, a).next();
    }
}
