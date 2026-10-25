package com.h3artless.shadowbound.client.render;

import com.h3artless.shadowbound.Shadowbound;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public final class PylonLayers extends RenderLayer {
    public static final Identifier TEXTURE = Shadowbound.id("textures/misc/soul_pylon.png");

    private static final RenderLayer STONE = RenderLayer.of(
            "shadowbound_pylon_stone", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS,
            1024, true, false,
            MultiPhaseParameters.builder()
                    .program(new ShaderProgram(() -> ShadowShaders.pylonStone))
                    .texture(new Texture(TEXTURE, true, false))
                    .transparency(NO_TRANSPARENCY)
                    .cull(DISABLE_CULLING)
                    .lightmap(ENABLE_LIGHTMAP)
                    .overlay(ENABLE_OVERLAY_COLOR)
                    .build(true));

    private static final RenderLayer CRYSTAL = RenderLayer.of(
            "shadowbound_pylon_crystal", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS,
            256, false, true,
            MultiPhaseParameters.builder()
                    .program(new ShaderProgram(() -> ShadowShaders.pylonCrystal))
                    .texture(new Texture(TEXTURE, true, false))
                    .transparency(LIGHTNING_TRANSPARENCY)
                    .cull(DISABLE_CULLING)
                    .lightmap(ENABLE_LIGHTMAP)
                    .overlay(ENABLE_OVERLAY_COLOR)
                    .writeMaskState(COLOR_MASK)
                    .build(false));

    private static final RenderLayer RUNE = RenderLayer.of(
            "shadowbound_pylon_rune", VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS,
            512, false, true,
            MultiPhaseParameters.builder()
                    .program(new ShaderProgram(() -> ShadowShaders.pylonRune))
                    .texture(new Texture(TEXTURE, true, false))
                    .transparency(LIGHTNING_TRANSPARENCY)
                    .cull(DISABLE_CULLING)
                    .writeMaskState(COLOR_MASK)
                    .build(false));

    private static final RenderLayer COLUMN = RenderLayer.of(
            "shadowbound_pylon_column", VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS,
            512, false, true,
            MultiPhaseParameters.builder()
                    .program(new ShaderProgram(() -> ShadowShaders.pylonColumn))
                    .transparency(LIGHTNING_TRANSPARENCY)
                    .cull(DISABLE_CULLING)
                    .writeMaskState(COLOR_MASK)
                    .build(false));

    private PylonLayers(String name, VertexFormat format, VertexFormat.DrawMode mode, int size,
                        boolean crumbling, boolean translucent, Runnable start, Runnable end) {
        super(name, format, mode, size, crumbling, translucent, start, end);
    }

    public static RenderLayer stone() { return STONE; }
    public static RenderLayer crystal() { return CRYSTAL; }
    public static RenderLayer rune() { return RUNE; }
    public static RenderLayer column() { return COLUMN; }
}
