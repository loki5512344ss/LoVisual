package dev.loki.lovisual.render.engine.renderer;

import dev.loki.lovisual.render.engine.renderer.facade.shape.Renderer2DItem;
import dev.loki.lovisual.render.engine.renderer.warp.RenderWarp;
import dev.loki.lovisual.render.engine.renderer.warp.RenderWarpFactory;
import dev.loki.lovisual.render.engine.renderer.warp.RenderWarpStack;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.command.core.UiCommand;
import dev.loki.lovisual.render.engine.command.effect.UiEffectRegionCommand;
import dev.loki.lovisual.render.engine.command.shape.UiPathCommand;
import dev.loki.lovisual.render.engine.command.shape.UiShapeCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.renderer.ui.blur.FrameBlurCacheEntry;
import dev.loki.lovisual.render.engine.renderer.ui.item.ItemBatchRenderer;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.OrderedUiBatcher;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.UiBatchStats;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.ui.blur.UiBlurResources;
import dev.loki.lovisual.render.engine.renderer.ui.deferred.sched.UiDeferredScheduler;
import dev.loki.lovisual.render.engine.renderer.ui.dispatch.UiDirectTexturedRenderer;
import dev.loki.lovisual.render.engine.renderer.ui.dispatch.UiRenderDispatcher;
import dev.loki.lovisual.render.engine.renderer.ui.draw.box.UiBoxShape;
import dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiStroke;
import dev.loki.lovisual.render.engine.renderer.ui.draw.effect.UiEffectSpec;
import dev.loki.lovisual.render.engine.renderer.ui.draw.paint.UiPaint;
import dev.loki.lovisual.render.engine.renderer.ui.draw.shape.UiShape;
import dev.loki.lovisual.render.engine.renderer.renderer2d.Rects2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.fx.Blur2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.path.Polygons2D;
import dev.loki.lovisual.render.engine.text.glyph.GlyphFont;
import dev.loki.lovisual.render.engine.text.backend.placement.TextPlacementMode;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import java.util.function.Consumer;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiMeshGeometry.*;

public class Renderer2D extends Renderer2DItem {
    public static OrderedUiBatcher UI_BATCHER = new OrderedUiBatcher();
    public static final ItemBatchRenderer ITEM_BATCH_RENDERER = new ItemBatchRenderer();
    public static final BatchStats BATCH_STATS = new BatchStats();

    /**
     * Frame-local blurred framebuffer cache.
     *
     * <p>HUD widgets often get split by text, item, scissor or nested compatibility flushes.
     * The cached texture is a Dual Kawase result for a concrete source, quality and offset.
     * Liquid glass is a material call and prepares this blur internally; callers no longer need
     * to draw an extra blur rectangle under it.</p>
     */
    public static final FrameBlurCacheEntry FRAME_BLUR_CACHE = UiBlurResources.frameCache();

    public enum Deferred2DLayer {
        /**
         * Legacy safety bucket for 2D extracted outside a concrete HUD slot.
         * Drained before vanilla GuiRenderer.render().
         */
        BEFORE_VANILLA_GUI,

        HUD_FIRST,
        HUD_BEFORE_MISC_OVERLAYS,
        HUD_AFTER_MISC_OVERLAYS,
        HUD_AFTER_BOSS_BAR,
        HUD_BEFORE_DEMO_TIMER,
        HUD_BEFORE_CHAT,
        HUD_AFTER_SUBTITLES,
        HUD_LAST,

        /**
         * Explicit top screen bucket. This is intentionally not used by normal HudPhase slots.
         */
        AFTER_VANILLA_GUI
    }

    public enum BlurQuality {
        LOW(0, 2),
        MEDIUM(1, 3),
        HIGH(2, 4),
        ULTRA(3, 5);

        public final int id;
        public final int iterations;

        BlurQuality(int id, int iterations) {
            this.id = id;
            this.iterations = iterations;
        }
    }

    public static final BlurQuality DEFAULT_BLUR_QUALITY = BlurQuality.MEDIUM;
    public static final BlurQuality DEFAULT_LIQUID_GLASS_BLUR_QUALITY = BlurQuality.HIGH;
    public static final float DEFAULT_KAWASE_OFFSET_PX = 1.0f;
    public static final float LIQUID_GLASS_KAWASE_OFFSET_PX = 1.15f;

    public static Renderer2D COLOR;
    public static Renderer2D TEXTURE;

    public Renderer2D(boolean textured) {
        super(textured);
    }

    public static void init() {
        COLOR = new Renderer2D(false);
        TEXTURE = new Renderer2D(true);
        ITEM_BATCH_RENDERER.init();
    }

    // ---- Public drawing facade -------------------------------------------------

    public void setAlpha(double alpha) {
        this.alpha = Mth.clamp(alpha, 0.0, 1.0);
    }

    public double getAlpha() {
        return alpha;
    }

    public boolean isTextured() {
        return textured;
    }

    /** Direct-text triangle mesh; non-null only on the TEXTURE renderer. */
    public MeshBuilder texturedMesh() {
        return texturedTriangles;
    }

    public void shape(UiShape shape, UiPaint paint) {
        shape(shape, paint, UiStroke.NONE, true);
    }

    public void shapeStroke(UiShape shape, UiPaint paint, UiStroke stroke) {
        shape(shape, paint, stroke, false);
    }

    public void box(UiBoxShape box, UiPaint paint) {
        box(box, paint, UiStroke.NONE, true);
    }

    public void boxStroke(UiBoxShape box, UiPaint paint, UiStroke stroke) {
        box(box, paint, stroke, false);
    }

    public void box(UiBoxShape box, UiPaint paint, UiStroke stroke, boolean fill) {
        Polygons2D.box(this, box, paint, stroke, fill);
    }

    public void shape(UiShape shape, UiPaint paint, UiStroke stroke, boolean fill) {
        if (shape == null || paint == null) return;
        recordUi(new UiShapeCommand(shape, paint, stroke == null ? UiStroke.NONE : stroke, fill));
    }

    public void path(UiShape path, UiPaint paint, UiStroke stroke, boolean fill) {
        if (path == null || paint == null) return;
        recordUi(new UiPathCommand(path, paint, stroke == null ? UiStroke.NONE : stroke, fill));
    }

    public void effect(UiEffectSpec effect) {
        if (effect == null) return;
        recordUi(new UiEffectRegionCommand(effect));
    }

    public void begin() {
        beginUiLayer();
        if (textured) {
            texturedTriangles.begin();
        } else {
            UI_BATCHER.begin();
        }
    }

    public void end() {
        if (textured && texturedTriangles.isBuilding()) {
            texturedTriangles.end();
        }
    }

    public void render() {
        if (textured) {
            render(null, null, null);
            return;
        }
        if (UI_BATCHER.hasPendingWork() || UiRenderDispatcher.hasPendingCommands()) {
            BATCH_STATS.noteFlushReason(FlushReason.RENDER_END);
        }
        UI_BATCHER.flush(true);
        flushUiLayer();
    }

    public void render(GpuTextureView textureView, GpuSampler sampler) {
        if (!textured) {
            throw new IllegalStateException("Renderer2D is not configured for textures.");
        }
        render("u_Texture", textureView, sampler);
    }

    public void render(Identifier textureId) {
        if (!textured) {
            throw new IllegalStateException("Renderer2D is not configured for textures.");
        }
        boolean submitted = false;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || textureId == null) return;
            AbstractTexture tex = mc.getTextureManager().getTexture(textureId);
            if (tex == null) return;
            GpuTextureView view = tex.getTextureView();
            GpuSampler sampler = tex.getSampler();
            if (view == null || sampler == null) return;
            submitted = true;
            render("u_Texture", view, sampler);
        } finally {
            if (!submitted) {
                flushUiLayer();
            }
        }
    }

    public void render(String samplerName, GpuTextureView samplerView, GpuSampler sampler) {
        UiDirectTexturedRenderer.submit(texturedTriangles, samplerName, samplerView, sampler);
    }

    public void quadBatch(Consumer<QuadBatch> consumer) {
        Rects2D.quadBatch(this, consumer);
    }

    public void blurRect(double x, double y, double w, double h,
                         float radius, float quality, float brightness, float alpha, int ignoredTintRgb) {
        Blur2D.blurRect(this, x, y, w, h, radius, quality, brightness, alpha, ignoredTintRgb);
    }

    public void blurChamferedRect(double x, double y, double w, double h,
                                  double chamfer, float quality, float brightness, float alpha, int ignoredTintRgb) {
        Blur2D.blurChamferedRect(this, x, y, w, h, chamfer, quality, brightness, alpha, ignoredTintRgb);
    }

    public void blurComposite(Consumer<BlurComposite> consumer) {
        Blur2D.blurComposite(this, consumer);
    }

    // ---- Package-private plumbing for helpers in the same package ------------

    boolean beginAutoBatch() {
        return UiRenderDispatcher.beginAutoBatch();
    }

    void endAutoBatch(boolean auto) {
        UiRenderDispatcher.endAutoBatch(auto);
    }

    static void recordUi(UiCommand command) {
        UiRenderDispatcher.record(command);
    }

    static void beginUiLayer() {
        UiRenderDispatcher.beginLayer();
    }

    // ---- Engine integration and compatibility bridge --------------------------

    public static boolean isBatching() {
        return UI_BATCHER.isActive();
    }

    public static BatchStats getBatchStats() {
        return BATCH_STATS;
    }

    public static void beginDeferredExtractFrame() {
        UiDeferredScheduler.beginExtractFrame();
    }

    public static void endDeferredExtractFrame() {
        UiDeferredScheduler.endExtractFrame();
    }

    public static void drainDeferred2D(Deferred2DLayer layer) {
        UiDeferredScheduler.drain(layer);
    }

    public static boolean isDeferredExtractRecording() {
        return UiDeferredScheduler.shouldDefer();
    }

    public static void withDeferredLayer(Deferred2DLayer layer, Runnable action) {
        UiDeferredScheduler.withLayer(layer, action);
    }

    public static void deferRenderThreadAction(Runnable action) {
        UiDeferredScheduler.deferAction(action);
    }

    public static void releaseDeferredBatcher(OrderedUiBatcher batcher) {
        UiDeferredScheduler.releaseBatcher(batcher);
    }

    public static ViewportContext snapshotViewport() {
        return UiDeferredScheduler.snapshotViewport();
    }

    public static void flushUiLayer() {
        UiRenderDispatcher.flushLayer();
    }

    public static void recordBackendCommand(UiBatchType type) {
        UiRenderDispatcher.recordBackendCommand(type);
    }

    public static void recordBackendCommand(String batchType) {
        UiRenderDispatcher.recordBackendCommand(batchType);
    }

    public static boolean enqueueTextMesh(String label,
                                          GlyphFont font,
                                          MeshBuilder sourceMesh,
                                          RenderPipeline pipeline,
                                          TextPlacementMode placement) {
        return UiRenderDispatcher.enqueueTextMesh(label, font, sourceMesh, pipeline, placement, false);
    }

    public static boolean enqueueLiquidGlassTextMesh(String label,
                                                     GlyphFont font,
                                                     MeshBuilder sourceMesh,
                                                     RenderPipeline pipeline,
                                                     TextPlacementMode placement) {
        return UiRenderDispatcher.enqueueTextMesh(label, font, sourceMesh, pipeline, placement, true);
    }

    public static boolean submitLiquidGlassTextMeshImmediate(String label,
                                                             GlyphFont font,
                                                             MeshBuilder sourceMesh,
                                                             RenderPipeline pipeline,
                                                             TextPlacementMode placement) {
        return UiRenderDispatcher.submitLiquidGlassTextMeshImmediate(label, font, sourceMesh, pipeline, placement);
    }

    public static RenderWarpStack.Scope pushWarp(RenderWarp warp) {
        return RenderWarpStack.push(warp);
    }

    public static RenderWarpStack.Scope pushPerspectiveWarp(float x,
                                                            float y,
                                                            float width,
                                                            float height,
                                                            float yawDeg,
                                                            float pitchDeg,
                                                            float rollDeg,
                                                            float depth,
                                                            float perspective,
                                                            float scale) {
        return RenderWarpStack.push(RenderWarpFactory.perspective(
                x, y, width, height, yawDeg, pitchDeg, rollDeg, depth, perspective, scale
        ));
    }

    /** Flushes pending UI work without ending the current ordered batch. */
    public static void flushBatch() {
        flushBatch(FlushReason.EXPLICIT);
    }

    public static void flushBatch(FlushReason reason) {
        UiRenderDispatcher.flushBatch(reason);
    }

    public static boolean isFlushingBatch() {
        return UiRenderDispatcher.isFlushingBatch();
    }

    public static void requestLiquidGlassBlur() {
        UiBlurResources.requestLiquidGlassBlur();
    }

    public static void requestLiquidGlassBlurBeforeNextShapeClip() {
        UiBlurResources.requestBeforeNextShapeClip();
    }

    public static void prepareLiquidGlassBlurBeforeShapeClipIfRequested() {
        UiBlurResources.prepareBeforeShapeClipIfRequested();
    }

    public static void captureWorldGlassSource() {
        UiBlurResources.captureWorldSource();
    }

    public static void invalidateWorldGlassSource() {
        UiBlurResources.invalidateWorldSource();
    }

    public enum FlushReason {
        UNKNOWN,
        EXPLICIT,
        PROJECTION,
        SCISSOR,
        VANILLA_TEXT,
        UI_TEXTURE,
        TEXTURED_DIRECT,
        AUTO_BATCH,
        RENDER_END
    }

    public static final class BlurComposite {
        private final MeshBuilder mesh;
        private final float globalAlpha;

        public BlurComposite(MeshBuilder mesh, float globalAlpha) {
            this.mesh = mesh;
            this.globalAlpha = globalAlpha;
        }

        public void roundedRect(double x, double y, double w, double h,
                                float radius, float quality, float brightness, float alpha, int ignoredTintRgb) {
            roundedRectCorners(x, y, w, h, radius, radius, radius, radius, quality, brightness, alpha, ignoredTintRgb);
        }

        public void roundedRectCorners(double x, double y, double w, double h,
                                       float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                       float quality, float brightness, float alpha, int ignoredTintRgb) {
            if (w <= 0.0 || h <= 0.0) return;
            float finalAlpha = clamp01(alpha * globalAlpha);
            if (finalAlpha <= 0.001f) return;

            mesh.ensureQuadCapacity();

            int a = Math.max(0, Math.min(255, Math.round(finalAlpha * 255.0f)));

            float smoothness = 2.0f;

            int i1 = mesh.vec2(x, y).local2(x, y).color(255, 255, 255, a)
                    .vec4(x, y, w, h)
                    .vec4(radiusTL, radiusTR, radiusBR, radiusBL)
                    .vec4(quality, brightness, smoothness, 0.0f)
                    .next();
            int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(255, 255, 255, a)
                    .vec4(x, y, w, h)
                    .vec4(radiusTL, radiusTR, radiusBR, radiusBL)
                    .vec4(quality, brightness, smoothness, 0.0f)
                    .next();
            int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(255, 255, 255, a)
                    .vec4(x, y, w, h)
                    .vec4(radiusTL, radiusTR, radiusBR, radiusBL)
                    .vec4(quality, brightness, smoothness, 0.0f)
                    .next();
            int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(255, 255, 255, a)
                    .vec4(x, y, w, h)
                    .vec4(radiusTL, radiusTR, radiusBR, radiusBL)
                    .vec4(quality, brightness, smoothness, 0.0f)
                    .next();
            mesh.quad(i1, i2, i3, i4);
        }
    }

    public static final class QuadBatch {
        private final MeshBuilder mesh;

        public QuadBatch(MeshBuilder mesh) {
            this.mesh = mesh;
        }

        public void quad(double x, double y, double width, double height, int argb) {
            quad(x, y, width, height, argb, argb, argb, argb);
        }

        public void quad(double x, double y, double width, double height,
                         int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
            appendColoredQuad(mesh, x, y, width, height, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
        }
    }

    public static final class BatchStats extends UiBatchStats {
    }
}
