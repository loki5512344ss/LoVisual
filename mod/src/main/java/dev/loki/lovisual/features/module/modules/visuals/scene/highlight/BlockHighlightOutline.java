package dev.loki.lovisual.features.module.modules.visuals.scene;

import net.minecraft.world.phys.shapes.VoxelShape;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.renderer.Renderer3DBatchBindings;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.engine.uniform.impl.world.liquid.WaterHighlightUniforms;
import dev.loki.lovisual.util.time.GameClock;

final class BlockHighlightOutline {

    private static final double GEOMETRY_EXPAND = 0.0025;

    void renderBoxes(Renderer3D renderer,
                     VoxelShape worldShape,
                     boolean depthTest,
                     BlockHighlightColors colors,
                     BlockHighlightGeometry geometry,
                     BlockHighlight.ColorMode colorMode,
                     int speed,
                     int primary,
                     int secondary) {
        MeshBuilder mesh = renderer.batch(
                depthTest ? LoVisualRenderPipelines.WORLD_COLORED_DEPTH : LoVisualRenderPipelines.WORLD_COLORED,
                depthTest ? Renderer3D.DepthMode.PRE_DEPTH : Renderer3D.DepthMode.NONE
        );
        if (mesh == null) return;
        worldShape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double x1 = minX - GEOMETRY_EXPAND;
            double y1 = minY - GEOMETRY_EXPAND;
            double z1 = minZ - GEOMETRY_EXPAND;
            double x2 = maxX + GEOMETRY_EXPAND;
            double y2 = maxY + GEOMETRY_EXPAND;
            double z2 = maxZ + GEOMETRY_EXPAND;
            int c000 = colors.outlineAt(x1, y1, z1, colorMode, speed, primary, secondary);
            int c001 = colors.outlineAt(x1, y1, z2, colorMode, speed, primary, secondary);
            int c010 = colors.outlineAt(x1, y2, z1, colorMode, speed, primary, secondary);
            int c011 = colors.outlineAt(x1, y2, z2, colorMode, speed, primary, secondary);
            int c100 = colors.outlineAt(x2, y1, z1, colorMode, speed, primary, secondary);
            int c101 = colors.outlineAt(x2, y1, z2, colorMode, speed, primary, secondary);
            int c110 = colors.outlineAt(x2, y2, z1, colorMode, speed, primary, secondary);
            int c111 = colors.outlineAt(x2, y2, z2, colorMode, speed, primary, secondary);
            geometry.addGradientBox(mesh, x1, y1, z1, x2, y2, z2,
                    c000, c001, c010, c011, c100, c101, c110, c111);
        });
    }

    void renderLines(Renderer3D renderer,
                     VoxelShape worldShape,
                     boolean depthTest,
                     float thickness,
                     BlockHighlightColors colors,
                     BlockHighlightGeometry geometry,
                     BlockHighlight.ColorMode colorMode,
                     int speed,
                     int primary,
                     int secondary) {
        MeshBuilder mesh = renderer.batch(
                depthTest ? LoVisualRenderPipelines.WORLD_COLORED_DEPTH : LoVisualRenderPipelines.WORLD_COLORED,
                depthTest ? Renderer3D.DepthMode.PRE_DEPTH : Renderer3D.DepthMode.NONE
        );
        if (mesh == null) return;
        double overlap = depthTest ? Math.max(0.0015, thickness * 0.003) : 0.0008;
        worldShape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            int c1 = colors.outlineAt(x1, y1, z1, colorMode, speed, primary, secondary);
            int c2 = colors.outlineAt(x2, y2, z2, colorMode, speed, primary, secondary);
            geometry.addEdgeRibbon(mesh, x1, y1, z1, x2, y2, z2, overlap, c1, c2, thickness);
        });
    }

    void renderShader(Renderer3D renderer,
                      VoxelShape worldShape,
                      BlockHighlight.ShaderStyle style,
                      BlockHighlightColors colors,
                      BlockHighlightGeometry geometry,
                      int primary,
                      int secondary) {
        RenderPipeline pipeline = switch (style) {
            case SCAN_LINES -> LoVisualRenderPipelines.WORLD_BLOCK_HIGHLIGHT_SCANLINES;
            case HOLOGRAM -> LoVisualRenderPipelines.WORLD_BLOCK_HIGHLIGHT_HOLOGRAM;
            default -> LoVisualRenderPipelines.WORLD_BLOCK_HIGHLIGHT_WATERCAUSTIC;
        };
        float time = GameClock.millis() / 1000f;
        WaterHighlightUniforms.update(
                colors.red(primary) / 255f, colors.green(primary) / 255f,
                colors.blue(primary) / 255f, colors.alpha(primary) / 255f,
                colors.red(secondary) / 255f, colors.green(secondary) / 255f,
                colors.blue(secondary) / 255f, colors.alpha(secondary) / 255f,
                time
        );
        Renderer3DBatchBindings bindings = Renderer3DBatchBindings.none()
                .withUniform("WaterData", WaterHighlightUniforms.get());
        MeshBuilder mesh = renderer.batch(pipeline, Renderer3D.DepthMode.NONE, bindings);
        if (mesh == null) return;
        worldShape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double x1 = minX - GEOMETRY_EXPAND;
            double y1 = minY - GEOMETRY_EXPAND;
            double z1 = minZ - GEOMETRY_EXPAND;
            double x2 = maxX + GEOMETRY_EXPAND;
            double y2 = maxY + GEOMETRY_EXPAND;
            double z2 = maxZ + GEOMETRY_EXPAND;
            geometry.addTexturedBox(mesh, x1, y1, z1, x2, y2, z2);
        });
    }
}
