package com.h3artless.shadowbound.client.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

public final class BookLayers extends RenderLayer {
    private static final RenderLayer BOOK = RenderLayer.of(
            "shadowbound_book", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS,
            2048, true, false,
            MultiPhaseParameters.builder()
                    .program(new ShaderProgram(() -> ShadowShaders.book))
                    .texture(new Texture(GrimoireRenderer.TEXTURE, false, false))
                    .transparency(NO_TRANSPARENCY)
                    .cull(ENABLE_CULLING)
                    .lightmap(ENABLE_LIGHTMAP)
                    .overlay(ENABLE_OVERLAY_COLOR)
                    .build(true));

    private BookLayers(String name, VertexFormat format, VertexFormat.DrawMode mode, int size,
                       boolean crumbling, boolean translucent, Runnable start, Runnable end) {
        super(name, format, mode, size, crumbling, translucent, start, end);
    }

    public static RenderLayer book() {
        return BOOK;
    }
}
