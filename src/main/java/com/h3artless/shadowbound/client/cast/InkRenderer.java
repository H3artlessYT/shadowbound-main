package com.h3artless.shadowbound.client.cast;

import com.h3artless.shadowbound.client.render.ShadowShaders;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.GlBlendState;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

final class InkRenderer {
    private InkRenderer() {}

    static boolean ready() {
        return ShadowShaders.inkStroke != null && ShadowShaders.inkDot != null;
    }

    private static BufferBuilder begin(ShaderProgram program) {
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> program);
        BufferBuilder buf = Tessellator.getInstance().getBuffer();
        buf.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        return buf;
    }

    private static void end(BufferBuilder buf) {
        BufferRenderer.drawWithGlobalProgram(buf.end());
        new GlBlendState().enable();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
    }

    static void stroke(Matrix4f m, float[] xy, int points, float width, float r, float g, float b, float a) {
        if (points < 2 || !ready()) return;
        BufferBuilder buf = begin(ShadowShaders.inkStroke);
        float hw = width * 0.5f, run = 0f;
        for (int i = 0; i < points - 1; i++) {
            float x0 = xy[i * 2], y0 = xy[i * 2 + 1], x1 = xy[i * 2 + 2], y1 = xy[i * 2 + 3];
            float dx = x1 - x0, dy = y1 - y0;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 0.001f) continue;
            float ux = dx / len, uy = dy / len;
            float nx = -uy * hw, ny = ux * hw;
            float ex = ux * hw, ey = uy * hw;
            float s0 = run / width, s1 = (run + len) / width;
            buf.vertex(m, x0 - ex + nx, y0 - ey + ny, 0).texture(s0, 0f).color(r, g, b, a).next();
            buf.vertex(m, x0 - ex - nx, y0 - ey - ny, 0).texture(s0, 1f).color(r, g, b, a).next();
            buf.vertex(m, x1 + ex - nx, y1 + ey - ny, 0).texture(s1, 1f).color(r, g, b, a).next();
            buf.vertex(m, x1 + ex + nx, y1 + ey + ny, 0).texture(s1, 0f).color(r, g, b, a).next();
            run += len;
        }
        end(buf);
    }

    static void dots(Matrix4f m, float[] xyra, int n, float r, float g, float b) {
        if (n == 0 || !ready()) return;
        BufferBuilder buf = begin(ShadowShaders.inkDot);
        for (int i = 0; i < n; i++) {
            float x = xyra[i * 4], y = xyra[i * 4 + 1], s = xyra[i * 4 + 2], a = xyra[i * 4 + 3];
            buf.vertex(m, x - s, y - s, 0).texture(0f, 0f).color(r, g, b, a).next();
            buf.vertex(m, x - s, y + s, 0).texture(0f, 1f).color(r, g, b, a).next();
            buf.vertex(m, x + s, y + s, 0).texture(1f, 1f).color(r, g, b, a).next();
            buf.vertex(m, x + s, y - s, 0).texture(1f, 0f).color(r, g, b, a).next();
        }
        end(buf);
    }
}
