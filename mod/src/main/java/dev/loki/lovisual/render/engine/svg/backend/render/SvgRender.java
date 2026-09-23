package dev.loki.lovisual.render.engine.svg.backend;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.model.SvgColorMode;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;

/**
 * Рендер SVG → Renderer2D (выбор texture vs mesh).
 */
final class SvgRender {
    private SvgRender() {}

    static void render(Renderer2D renderer, SvgMeshBackend.SvgDoc doc, double x, double y, double width, double height, SvgRenderOptions options) {
        if (doc.vw() <= SvgMeshBackend.EPS || doc.vh() <= SvgMeshBackend.EPS) return;
        if (options.textureCache()) {
            SvgMeshBackend.CompiledTexture texture = SvgTexture.getOrRasterize(doc, width, height, options);
            if (texture != null && texture.isReady()) {
                SvgMeshBackend.traceOnce("svg-texture-draw:" + doc.cacheKey() + ':' + texture.widthPx() + 'x' + texture.heightPx() + ':' + texture.scale(),
                        "[LoVisual][SVG] GPU texture path active: doc=%s tex=%dx%d logical=%.2fx%.2f scale=%d",
                        doc.cacheKey(), texture.widthPx(), texture.heightPx(), Math.abs(width), Math.abs(height), texture.scale());
                renderer.textureQuad(texture.texture().getTextureView(), texture.texture().getSampler(), x, y, width, height, SvgTexture.textureTint(options));
                return;
            }
            SvgMeshBackend.traceOnce("svg-texture-fallback:" + doc.cacheKey() + ':' + SvgMeshBackend.quantize(width, 16.0) + 'x' + SvgMeshBackend.quantize(height, 16.0),
                    "[LoVisual][SVG] GPU texture path unavailable, falling back to mesh: doc=%s logical=%.2fx%.2f",
                    doc.cacheKey(), Math.abs(width), Math.abs(height));
        }
        SvgMeshBackend.CompiledGeometry geometry = SvgGeometry.getOrCompile(doc, width, height, options);
        renderer.quadBatch(batch -> {
            for (SvgMeshBackend.QuadCmd quad : geometry.quads) {
                batch.quad(x + quad.x(), y + quad.y(), quad.w(), quad.h(), quad.argb());
            }
        });
    }
}
