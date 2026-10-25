package com.h3artless.shadowbound.client.cast;

import com.h3artless.shadowbound.Shadowbound;
import com.h3artless.shadowbound.ritual.RitualPatterns;
import com.h3artless.shadowbound.ritual.ShadowRitual;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.List;

public class PaperScreen extends Screen {
    private static final Identifier PARCHMENT = Shadowbound.id("textures/gui/shadow_paper.png");
    private static final int W = 200, H = 250;

    private static final String[] NUMERALS = {"I", "II", "III"};

    private final ShadowRitual rite;
    private final int step;
    private final Text fragment;
    private final float[] line;
    private final float[] screen;
    private final int points;
    private final float[] start = new float[4];
    private float minX, maxX, minY, maxY;
    private List<OrderedText> hint;

    public PaperScreen(ShadowRitual rite, int step) {
        super(Text.translatable("screen.shadowbound.paper"));
        this.rite = rite;
        this.step = step;
        this.fragment = Text.translatable("screen.shadowbound.paper.fragment", NUMERALS[step]);
        int[][] pts = RitualPatterns.walk(rite.step(step), 0);
        points = pts.length;
        line = new float[points * 2];
        screen = new float[points * 2];
        minX = minY = Float.MAX_VALUE;
        maxX = maxY = -Float.MAX_VALUE;
        for (int i = 0; i < points; i++) {
            float x = RitualPatterns.x(pts[i][0], pts[i][1]), y = RitualPatterns.y(pts[i][1]);
            line[i * 2] = x;
            line[i * 2 + 1] = y;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
    }

    @Override
    protected void init() {
        hint = textRenderer.wrapLines(Text.translatable("paper.shadowbound." + rite.key())
                .append(" ").append(Text.translatable("paper.shadowbound.step." + step)), W - 44);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx);
        int x0 = (width - W) / 2, y0 = (height - H) / 2;
        ctx.drawTexture(PARCHMENT, x0, y0, 0, 0, W, H, W, H);
        ctx.drawText(textRenderer, title, x0 + (W - textRenderer.getWidth(title)) / 2, y0 + 14, 0x4a0d10, false);
        ctx.drawText(textRenderer, fragment, x0 + (W - textRenderer.getWidth(fragment)) / 2, y0 + 25, 0x7a1418, false);

        float box = 110f;
        float span = Math.max(maxX - minX, maxY - minY);
        float scale = span > 0 ? Math.min(box / span, 30f) : 30f;
        float ox = x0 + W / 2f - (minX + maxX) * 0.5f * scale;
        float oy = y0 + 44 + box / 2f - (minY + maxY) * 0.5f * scale;
        float[] screen = this.screen;
        for (int i = 0; i < points; i++) {
            screen[i * 2] = ox + line[i * 2] * scale;
            screen[i * 2 + 1] = oy + line[i * 2 + 1] * scale;
        }
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        InkRenderer.stroke(m, screen, points, Math.max(3f, scale * 0.2f), 0.32f, 0.02f, 0.03f, 0.92f);
        float t = client != null && client.world != null ? client.world.getTime() + delta : 0f;
        start[0] = screen[0];
        start[1] = screen[1];
        start[2] = Math.max(4f, scale * 0.32f) * (1f + 0.08f * MathHelper.sin(t * 0.15f));
        start[3] = 0.95f;
        InkRenderer.dots(m, start, 1, 0.55f, 0.03f, 0.05f);

        int ty = y0 + 40 + (int) box + 18;
        for (OrderedText l : hint) {
            ctx.drawText(textRenderer, l, x0 + 22, ty, 0x3a1214, false);
            ty += 10;
        }
        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        close();
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
