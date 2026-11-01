package com.h3artless.shadowbound.client.spy;

import com.h3artless.shadowbound.client.ClientShadowState;
import com.h3artless.shadowbound.net.ModPackets;
import com.h3artless.shadowbound.spy.SpyManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SpyScreen extends Screen {
    private static final int ROW_H = 26;
    private static final int PANEL_W = 236;
    private static final int VISIBLE_ROWS = 7;

    private static final int RED = 0xd8283c;
    private static final int RED_DIM = 0x7a1622;
    private static final int GRAY = 0x9a9a9a;
    private static final int GRAY_DARK = 0x3a3a3a;
    private static final int INK = 0x0c0c0d;

    @Nullable private static SpyScreen open;

    private final List<SpyTarget> targets = new ArrayList<>();
    private boolean loaded;
    private int darkness;
    private float scroll;
    private int age;

    public SpyScreen() {
        super(Text.translatable("screen.shadowbound.spy.title"));
    }

    public static void receiveList(PacketByteBuf buf) {
        int dark = buf.readVarInt();
        int n = buf.readVarInt();
        List<SpyTarget> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(new SpyTarget(buf.readUuid(), buf.readString(), buf.readIdentifier(), buf.readDouble(),
                    buf.readFloat(), buf.readFloat(), buf.readBoolean()));
        }
        if (open != null) open.setList(list, dark);
    }

    private void setList(List<SpyTarget> list, int dark) {
        targets.clear();
        targets.addAll(list);
        targets.sort((a, b) -> {
            boolean ah = a.distance() >= 0, bh = b.distance() >= 0;
            if (ah != bh) return ah ? -1 : 1;
            if (ah) return Double.compare(a.distance(), b.distance());
            return a.name().compareToIgnoreCase(b.name());
        });
        darkness = dark;
        loaded = true;
    }

    @Override
    protected void init() {
        open = this;
        loaded = false;
        ClientPlayNetworking.send(ModPackets.SPY_LIST_REQUEST, PacketByteBufs.empty());
    }

    @Override
    public void removed() {
        if (open == this) open = null;
    }

    @Override
    public void tick() {
        age++;
        if (age % 40 == 0) ClientPlayNetworking.send(ModPackets.SPY_LIST_REQUEST, PacketByteBufs.empty());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private int panelX() { return width / 2 - PANEL_W / 2; }
    private int listY() { return height / 2 - (VISIBLE_ROWS * ROW_H) / 2 + 6; }
    private int maxScroll() { return Math.max(0, targets.size() - VISIBLE_ROWS); }
    private boolean canAfford() { return Math.max(darkness, ClientShadowState.darkness()) >= SpyManager.START_COST; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, width, height, 0xd0060606, 0xe0100406);

        int px = panelX();
        int top = listY() - 34;
        int bottom = listY() + VISIBLE_ROWS * ROW_H + 26;
        float t = (age + delta) / 20f;

        ctx.fill(px - 1, top - 1, px + PANEL_W + 1, bottom + 1, argb(1f, GRAY_DARK));
        ctx.fill(px, top, px + PANEL_W, bottom, argb(0.96f, INK));
        float pulse = 0.55f + 0.45f * MathHelper.sin(t * 2.2f);
        ctx.fill(px, top, px + PANEL_W, top + 1, argb(pulse, RED));
        ctx.fill(px, bottom - 1, px + PANEL_W, bottom, argb(pulse * 0.6f, RED_DIM));

        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, top + 8, argb(1f, RED));
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.shadowbound.spy.subtitle"),
                width / 2, top + 20, argb(1f, GRAY));

        int ly = listY();
        if (!loaded) {
            String dots = ".".repeat(1 + (age / 6) % 3);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.shadowbound.spy.listening").append(dots),
                    width / 2, ly + 40, argb(1f, GRAY));
        } else if (targets.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.shadowbound.spy.empty"),
                    width / 2, ly + 40, argb(1f, GRAY));
        } else {
            int first = MathHelper.floor(scroll);
            ctx.enableScissor(px, ly, px + PANEL_W, ly + VISIBLE_ROWS * ROW_H);
            for (int i = first; i < Math.min(targets.size(), first + VISIBLE_ROWS + 1); i++) {
                int ry = ly + Math.round((i - scroll) * ROW_H);
                drawRow(ctx, targets.get(i), px + 6, ry, PANEL_W - 12, mouseX, mouseY, t);
            }
            ctx.disableScissor();

            if (maxScroll() > 0) {
                int trackH = VISIBLE_ROWS * ROW_H;
                int barH = Math.max(12, trackH * VISIBLE_ROWS / targets.size());
                int barY = ly + Math.round((trackH - barH) * (scroll / maxScroll()));
                ctx.fill(px + PANEL_W - 3, ly, px + PANEL_W - 1, ly + trackH, argb(1f, GRAY_DARK));
                ctx.fill(px + PANEL_W - 3, barY, px + PANEL_W - 1, barY + barH, argb(1f, RED_DIM));
            }
        }

        Text cost = canAfford()
                ? Text.translatable("screen.shadowbound.spy.ready")
                : Text.translatable("screen.shadowbound.spy.too_bright");
        ctx.drawCenteredTextWithShadow(textRenderer, cost, width / 2, bottom - 15, argb(1f, canAfford() ? GRAY : RED));

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawRow(DrawContext ctx, SpyTarget target, int x, int y, int w, int mouseX, int mouseY, float t) {
        boolean hover = loaded && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + ROW_H - 2
                && mouseY >= listY() && mouseY < listY() + VISIBLE_ROWS * ROW_H;
        boolean usable = canAfford() && !target.spectator();

        ctx.fill(x, y, x + w, y + ROW_H - 2, argb(hover && usable ? 0.9f : 0.6f, hover && usable ? 0x2a0a0e : 0x161617));
        if (hover && usable) {
            ctx.fill(x, y, x + 2, y + ROW_H - 2, argb(1f, RED));
        }

        PlayerListEntry entry = client.getNetworkHandler() == null ? null : client.getNetworkHandler().getPlayerListEntry(target.uuid());
        Identifier skin = entry != null ? entry.getSkinTexture() : DefaultSkinHelper.getTexture(target.uuid());
        PlayerSkinDrawer.draw(ctx, skin, x + 5, y + 3, 18);
        if (!usable) ctx.fill(x + 5, y + 3, x + 23, y + 21, argb(0.55f, INK));

        ctx.drawTextWithShadow(textRenderer, Text.literal(target.name()), x + 29, y + 3, argb(1f, usable ? 0xe6e6e6 : GRAY));

        Text where = Text.translatable(dimensionKey(target.dimension()));
        if (target.distance() >= 0) {
            where = Text.translatable("screen.shadowbound.spy.where_near", where, Math.round(target.distance()));
        }
        ctx.drawTextWithShadow(textRenderer, where, x + 29, y + 13, argb(1f, 0x6e6e6e));

        float frac = target.maxHealth() <= 0 ? 0 : MathHelper.clamp(target.health() / target.maxHealth(), 0f, 1f);
        int bw = 40, bx = x + w - bw - 6, by = y + 10;
        ctx.fill(bx, by, bx + bw, by + 3, argb(1f, GRAY_DARK));
        ctx.fill(bx, by, bx + Math.round(bw * frac), by + 3, argb(1f, RED));

        if (hover && usable) {
            float blink = 0.5f + 0.5f * MathHelper.sin(t * 6f);
            ctx.drawTextWithShadow(textRenderer, Text.literal("◉"), bx - 12, y + 7, argb(0.5f + 0.5f * blink, RED));
        }
    }

    private static String dimensionKey(Identifier dim) {
        return switch (dim.toString()) {
            case "minecraft:overworld" -> "screen.shadowbound.spy.dim.overworld";
            case "minecraft:the_nether" -> "screen.shadowbound.spy.dim.nether";
            case "minecraft:the_end" -> "screen.shadowbound.spy.dim.end";
            default -> dim.toString();
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && loaded && canAfford()) {
            int ly = listY();
            if (mouseY >= ly && mouseY < ly + VISIBLE_ROWS * ROW_H && mouseX >= panelX() && mouseX < panelX() + PANEL_W) {
                int idx = MathHelper.floor((mouseY - ly) / ROW_H + scroll);
                if (idx >= 0 && idx < targets.size() && !targets.get(idx).spectator()) {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeUuid(targets.get(idx).uuid());
                    SpyCamera.request();
                    ClientPlayNetworking.send(ModPackets.SPY_REQUEST, buf);
                    if (client.player != null) client.player.playSound(SoundEvents.BLOCK_SCULK_SENSOR_CLICKING, 0.6f, 0.5f);
                    close();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scroll = MathHelper.clamp(scroll - (float) amount, 0f, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (com.h3artless.shadowbound.client.ShadowboundClient.spyKey.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static int argb(float a, int rgb) {
        return (MathHelper.clamp((int) (a * 255f), 4, 255) << 24) | (rgb & 0xffffff);
    }
}
