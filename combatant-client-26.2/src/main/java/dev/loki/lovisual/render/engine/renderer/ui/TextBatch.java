/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.loki.lovisual.render.engine.text.GlyphFont;
import dev.loki.lovisual.render.engine.text.backend.TextPlacementMode;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

public final class TextBatch {
    public String label;
    public GlyphFont font;
    public RenderPipeline pipeline;
    public TextPlacementMode placement;
    public boolean liquidGlassText;
    public boolean shapeClipActive;
    public MeshBuilder mesh;

    public void begin(String label, GlyphFont font, RenderPipeline pipeline,
               TextPlacementMode placement, boolean liquidGlassText, boolean shapeClipActive) {
        this.label = label != null ? label : "LoVisual UI Text Batch";
        this.font = font;
        this.pipeline = pipeline;
        this.placement = placement != null ? placement : TextPlacementMode.UI;
        this.liquidGlassText = liquidGlassText;
        this.shapeClipActive = shapeClipActive;
        if (mesh == null) {
            mesh = new MeshBuilder(pipeline);
        } else if (mesh.isBuilding()) {
            mesh.end();
        }
        mesh.begin();
    }

    public boolean canMerge(GlyphFont font, RenderPipeline pipeline, TextPlacementMode placement, boolean liquidGlassText, boolean shapeClipActive) {
        return this.font == font
                && this.pipeline == pipeline
                && this.placement == (placement != null ? placement : TextPlacementMode.UI)
                && this.liquidGlassText == liquidGlassText
                && this.shapeClipActive == shapeClipActive;
    }

    public void append(MeshBuilder source) {
        if (source == null) return;
        if (source.isBuilding()) source.end();
        if (source.getIndicesCount() <= 0) return;
        if (mesh == null || !mesh.isBuilding()) {
            begin(label, font, pipeline, placement, liquidGlassText, shapeClipActive);
        }
        mesh.appendMesh(source);
    }

    public boolean isEmpty() {
        if (mesh == null) return true;
        if (mesh.isBuilding()) mesh.end();
        return mesh.getIndicesCount() <= 0;
    }
}
