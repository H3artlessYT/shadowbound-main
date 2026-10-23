package com.h3artless.shadowbound.client.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

public final class SmokeLayers extends RenderLayer {
    private static final RenderPhase.ShaderProgram SMOKE = new RenderPhase.ShaderProgram(() -> ShadowShaders.smoke);

    private static final Function<Identifier, RenderLayer> BODY = Util.memoize(tex -> RenderLayer.of(
            "shadowbound_smoke_body", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS,
            256, true, true,
            MultiPhaseParameters.builder()
                    .program(SMOKE)
                    .texture(new RenderPhase.Texture(tex, false, false))
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .cull(DISABLE_CULLING)
                    .lightmap(ENABLE_LIGHTMAP)
                    .overlay(ENABLE_OVERLAY_COLOR)
                    .build(false)));

    private static final Function<Identifier, RenderLayer> SHELL = Util.memoize(tex -> RenderLayer.of(
            "shadowbound_smoke_shell", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS,
            256, false, true,
            MultiPhaseParameters.builder()
                    .program(SMOKE)
                    .texture(new RenderPhase.Texture(tex, false, false))
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .cull(DISABLE_CULLING)
                    .lightmap(ENABLE_LIGHTMAP)
                    .overlay(ENABLE_OVERLAY_COLOR)
                    .writeMaskState(COLOR_MASK)
                    .build(false)));

    private static final RenderPhase.ShaderProgram WISP_PROGRAM = new RenderPhase.ShaderProgram(() -> ShadowShaders.wisp);

    private static final RenderLayer WISP = RenderLayer.of(
            "shadowbound_wisp", VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS,
            1024, false, true,
            MultiPhaseParameters.builder()
                    .program(WISP_PROGRAM)
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .cull(DISABLE_CULLING)
                    .writeMaskState(COLOR_MASK)
                    .build(false));

    private static final RenderPhase.ShaderProgram PUDDLE_PROGRAM = new RenderPhase.ShaderProgram(() -> ShadowShaders.puddle);

    private static final RenderLayer PUDDLE = RenderLayer.of(
            "shadowbound_puddle", VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS,
            256, false, true,
            MultiPhaseParameters.builder()
                    .program(PUDDLE_PROGRAM)
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .cull(DISABLE_CULLING)
                    .writeMaskState(COLOR_MASK)
                    .layering(VIEW_OFFSET_Z_LAYERING)
                    .build(false));

    private static final RenderPhase.ShaderProgram TRAIL_PROGRAM = new RenderPhase.ShaderProgram(() -> ShadowShaders.trail);
    private static final RenderPhase.ShaderProgram PUFF_PROGRAM = new RenderPhase.ShaderProgram(() -> ShadowShaders.puff);

    private static final RenderLayer TRAIL = RenderLayer.of(
            "shadowbound_trail", VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS,
            1024, false, true,
            MultiPhaseParameters.builder()
                    .program(TRAIL_PROGRAM)
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .cull(DISABLE_CULLING)
                    .writeMaskState(COLOR_MASK)
                    .layering(VIEW_OFFSET_Z_LAYERING)
                    .build(false));

    private static final RenderLayer PUFF = RenderLayer.of(
            "shadowbound_puff", VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS,
            1024, false, true,
            MultiPhaseParameters.builder()
                    .program(PUFF_PROGRAM)
                    .texture(new RenderPhase.Texture(RitualFx.SPRITES, false, false))
                    .transparency(TRANSLUCENT_TRANSPARENCY)
                    .cull(DISABLE_CULLING)
                    .writeMaskState(COLOR_MASK)
                    .build(false));

    private static final RenderLayer PUFF_GLOW = RenderLayer.of(
            "shadowbound_puff_glow", VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS,
            256, false, true,
            MultiPhaseParameters.builder()
                    .program(PUFF_PROGRAM)
                    .texture(new RenderPhase.Texture(RitualFx.SPRITES, false, false))
                    .transparency(LIGHTNING_TRANSPARENCY)
                    .cull(DISABLE_CULLING)
                    .writeMaskState(COLOR_MASK)
                    .build(false));

    private SmokeLayers(String name, VertexFormat format, VertexFormat.DrawMode mode, int size,
                        boolean crumbling, boolean translucent, Runnable start, Runnable end) {
        super(name, format, mode, size, crumbling, translucent, start, end);
    }

    public static RenderLayer body(Identifier texture) {
        return BODY.apply(texture);
    }

    public static RenderLayer trail() {
        return TRAIL;
    }

    public static RenderLayer puff() {
        return PUFF;
    }

    public static RenderLayer puffGlow() {
        return PUFF_GLOW;
    }

    public static RenderLayer puddle() {
        return PUDDLE;
    }

    public static RenderLayer wisp() {
        return WISP;
    }

    public static RenderLayer shell(Identifier texture) {
        return SHELL.apply(texture);
    }
}
