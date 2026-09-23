/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.text.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.loki.lovisual.render.engine.text.backend.atlas.BitmapAtlasTextBackend;
import dev.loki.lovisual.render.engine.text.backend.atlas.MsdfTextBackend;
import dev.loki.lovisual.render.engine.text.backend.backend.TextBackendRouter;
import dev.loki.lovisual.render.engine.text.backend.backend.TextDrawCommand;
import dev.loki.lovisual.render.engine.text.backend.backend.VanillaSodiumTextBackend;
import dev.loki.lovisual.render.engine.text.backend.placement.TextPlacementMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.rhi.resource.GpuMeshHandle;
import dev.loki.lovisual.render.engine.rhi.command.RhiDrawCommand;
import dev.loki.lovisual.render.engine.rhi.resource.atlas.GlyphAtlasManager;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.engine.uniform.impl.ui.MsdfTextUniforms;
import dev.loki.lovisual.render.engine.text.backend.atlas.BitmapAtlasTextBackend;
import dev.loki.lovisual.render.engine.text.backend.atlas.MsdfTextBackend;
import dev.loki.lovisual.render.engine.text.backend.backend.TextBackendRouter;
import dev.loki.lovisual.render.engine.text.backend.backend.TextDrawCommand;
import dev.loki.lovisual.render.engine.text.backend.placement.TextPlacementMode;
import dev.loki.lovisual.render.engine.text.backend.backend.VanillaSodiumTextBackend;
import dev.loki.lovisual.render.engine.text.command.TextCommandBuffer;
import dev.loki.lovisual.render.engine.text.command.TextCommandStats;
import dev.loki.lovisual.render.engine.text.command.TextCommandStatsSnapshot;
import dev.loki.lovisual.render.engine.text.font.info.TextBackendPreference;
import dev.loki.lovisual.render.engine.text.glyph.GlyphFont;

/**
 * Stage 10 text owner: command buffer, backend router and RHI mesh submission for glyph text.
 */
public enum TextRenderSystem {
    ;
    private static final TextCommandStats STATS = new TextCommandStats();
    private static final TextCommandBuffer COMMANDS = new TextCommandBuffer(STATS);
    private static final TextBackendRouter ROUTER = new TextBackendRouter(STATS);

    static {
        ROUTER.add(new MsdfTextBackend(STATS));
        ROUTER.add(new BitmapAtlasTextBackend(STATS));
        ROUTER.add(new VanillaSodiumTextBackend(STATS));
    }

    public static TextCommandBuffer commands() {
        return COMMANDS;
    }

    public static TextBackendRouter router() {
        return ROUTER;
    }

    public static GlyphAtlasManager glyphAtlases() {
        return LoVisualRenderSystem.resources().glyphAtlases();
    }

    public static void beginFrame() {
        COMMANDS.clear();
        STATS.reset();
    }

    public static void record(TextDrawCommand command) {
        COMMANDS.record(command);
    }

    public static void flush() {
        COMMANDS.flush(ROUTER);
    }

    public static TextCommandStatsSnapshot statsSnapshot() {
        return STATS.snapshot();
    }

    public static void noteDirectAdjacentTextBatch(int commands) {
        STATS.directAdjacentTextBatch(commands);
    }

    public static void submitGlyphMesh(String label,
                                       GlyphFont font,
                                       MeshBuilder mesh,
                                       RenderPipeline pipeline,
                                       TextPlacementMode placement) {
        if (font == null || mesh == null || pipeline == null) return;
        if (mesh.isBuilding()) mesh.end();
        if (mesh.getIndicesCount() <= 0) return;

        // When UI rendering is inside a Renderer2D batch, text becomes an ordered batch entry.
        // It may merge only with adjacent compatible text runs; it must never be moved across shapes,
        // items, scissor/marquee boundaries or world/placement-specific text.
        boolean liquidGlassText = pipeline == LoVisualRenderPipelines.UI_TEXT_LIQUID_GLASS
                || pipeline == LoVisualRenderPipelines.UI_TEXT_LIQUID_GLASS_MSDF;
        if (placement == null || placement == TextPlacementMode.UI || placement == TextPlacementMode.SCREEN_SPACE) {
            boolean enqueued = liquidGlassText
                    ? Renderer2D.enqueueLiquidGlassTextMesh(label, font, mesh, pipeline, placement != null ? placement : TextPlacementMode.UI)
                    : Renderer2D.enqueueTextMesh(label, font, mesh, pipeline, placement != null ? placement : TextPlacementMode.UI);
            if (enqueued) {
                return;
            }
        }

        if (liquidGlassText && Renderer2D.submitLiquidGlassTextMeshImmediate(label, font, mesh, pipeline, placement)) {
            return;
        }

        submitGlyphMeshImmediate(label, font, mesh, pipeline, placement);
    }

    public static void submitGlyphMeshImmediate(String label,
                                                GlyphFont font,
                                                MeshBuilder mesh,
                                                RenderPipeline pipeline,
                                                TextPlacementMode placement) {
        if (font == null || mesh == null || pipeline == null) return;
        if (mesh.isBuilding()) mesh.end();
        if (mesh.getIndicesCount() <= 0) return;
        if (!font.isReady()) return;

        AbstractTexture texture = font.getTexture();
        if (texture == null || texture.getTextureView() == null || texture.getSampler() == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer.mainRenderTarget() == null) return;

        int vertexBytes = mesh.getVertexBytes();
        int indexBytes = mesh.getIndexBytes();
        GpuMeshHandle handle = LoVisualRenderSystem.rhi().dynamicMeshes().upload(mesh);
        STATS.meshUpload(vertexBytes, indexBytes);
        STATS.glyphs(Math.max(0, mesh.getVertexCount() / 4));
        if (placement != null && placement.world()) STATS.worldPlacement();
        STATS.backend(font.isMsdf() ? TextBackendPreference.MSDF : TextBackendPreference.BITMAP_ATLAS);

        RhiDrawCommand.Builder command = RhiDrawCommand.builder(label != null ? label : "LoVisual Text")
                .pipeline(pipeline)
                .colorAttachment(mc.gameRenderer.mainRenderTarget().getColorTextureView())
                .mesh(handle)
                .sampler("u_Texture", texture.getTextureView(), texture.getSampler());

        if (font.isMsdf()) {
            MsdfTextUniforms.update(font.getPxRange(), font.getAtlasWidth(), font.getAtlasHeight());
            command.uniform("MsdfText", MsdfTextUniforms.get());
        }

        LoVisualRenderSystem.rhi().drawMesh(command.build());
    }

    public static RenderPipeline uiPipelineFor(GlyphFont font) {
        return font != null && font.isMsdf() ? LoVisualRenderPipelines.UI_TEXT_MSDF : LoVisualRenderPipelines.UI_TEXT;
    }
}
