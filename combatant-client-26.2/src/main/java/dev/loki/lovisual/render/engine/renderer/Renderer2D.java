/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.command.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.core.RenderPhase;
import dev.loki.lovisual.render.engine.core.ViewportContext;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.ui.BlurSource;
import dev.loki.lovisual.render.engine.renderer.ui.Deferred2DSubmit;
import dev.loki.lovisual.render.engine.renderer.ui.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.FrameBlurCacheEntry;
import dev.loki.lovisual.render.engine.renderer.ui.ItemBatchRenderer;
import dev.loki.lovisual.render.engine.renderer.ui.OrderedUiBatcher;
import dev.loki.lovisual.render.engine.renderer.ui.UiBatchStats;
import dev.loki.lovisual.render.engine.renderer.ui.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.ui.UiBlurResources;
import dev.loki.lovisual.render.engine.renderer.ui.UiDeferredScheduler;
import dev.loki.lovisual.render.engine.renderer.ui.UiDirectTexturedRenderer;
import dev.loki.lovisual.render.engine.renderer.ui.UiItemSubmission;
import dev.loki.lovisual.render.engine.renderer.ui.UiRenderDispatcher;
import dev.loki.lovisual.render.engine.postprocess.PostProcessManager;
import dev.loki.lovisual.render.engine.renderer.ui.draw.*;
import dev.loki.lovisual.render.engine.svg.SvgMeshBackend;
import dev.loki.lovisual.render.engine.svg.SvgRegistry;
import dev.loki.lovisual.render.engine.svg.SvgRenderOptions;
import dev.loki.lovisual.render.engine.text.GlyphFont;
import dev.loki.lovisual.render.engine.text.backend.TextPlacementMode;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.UiMeshGeometry.*;

public final class Renderer2D {
    public static final int ITEM_OVERLAY_NONE = 0;
    public static final int ITEM_OVERLAY_COUNT = 1;
    public static final int ITEM_OVERLAY_DURABILITY = 1 << 1;
    public static final int ITEM_OVERLAY_COOLDOWN = 1 << 2;
    public static final int ITEM_OVERLAY_DURABILITY_TEXT = 1 << 3;
    public static final int ITEM_OVERLAY_ALL = ITEM_OVERLAY_COUNT | ITEM_OVERLAY_DURABILITY | ITEM_OVERLAY_COOLDOWN;
    private static final int DEFAULT_ITEM_DURABILITY_THRESHOLD = 100;
    private static final int DEFAULT_ITEM_DURABILITY_TEXT_COLOR_THRESHOLD = 70;
    public static OrderedUiBatcher UI_BATCHER = new OrderedUiBatcher();
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

    /**
     * Compatibility mirror for older renderer integrations. New code should use
     * {@link #isWorldGlassSourceReady()} instead of mutating this field.
     */
    @Deprecated
    public static boolean uiGlassWorldSourceReady;

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
    final boolean textured;
    final MeshBuilder texturedTriangles;
    final int[] gradientTmp = new int[4];
    final float[] cornerRadiiTmp = new float[4];
    final double[] rectShapeTmp = new double[256];
    final double[] connectorTmp = new double[64];
    final double[] connectorAnchorTmp = new double[4];
    final int[] polygonIndexTmp = new int[64];
    final int[] polygonVertexTmp = new int[64];
    final double[] progressShapeTmp = new double[256];
    double alpha = 1.0;

    public Renderer2D(boolean textured) {
        this.textured = textured;
        this.texturedTriangles = textured ? new MeshBuilder(LoVisualRenderPipelines.UI_TEXTURED) : null;
    }

    public static void init() {
        COLOR = new Renderer2D(false);
        TEXTURE = new Renderer2D(true);
        ItemBatchRenderer.init();
    }

    // ---- Public drawing facade -------------------------------------------------

    public void setAlpha(double alpha) {
        this.alpha = Mth.clamp(alpha, 0.0, 1.0);
    }

    public double getAlpha() {
        return alpha;
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
        Renderer2DPolygons.box(this, box, paint, stroke, fill);
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

    // Use this path when the current projection is already the target space
    // (for example UNSCALED_LOGICAL HUD / ClickGUI coordinates).
    public void item(ItemStack stack, double x, double y) {
        item(stack, x, y, 1.0f, 0, ITEM_OVERLAY_NONE, null);
    }

    public void item(ItemStack stack, double x, double y, float scale) {
        item(stack, x, y, scale, 0, ITEM_OVERLAY_NONE, null);
    }

    public void item(ItemStack stack, double x, double y, float scale, int seed) {
        item(stack, x, y, scale, seed, ITEM_OVERLAY_NONE, null);
    }

    public void item(ItemStack stack, double x, double y, float scale, int seed, boolean overlay) {
        item(stack, x, y, scale, seed, overlay ? ITEM_OVERLAY_ALL : ITEM_OVERLAY_NONE, null);
    }

    public void item(ItemStack stack, double x, double y, float scale, int seed, int overlayFlags) {
        item(stack, x, y, scale, seed, overlayFlags, null);
    }

    public void item(ItemStack stack,
                     double x,
                     double y,
                     float scale,
                     int seed,
                     int overlayFlags,
                     @Nullable String stackCountText) {
        item(null, stack, x, y, scale, seed, overlayFlags, stackCountText);
    }

    public void item(ItemStack stack,
                     double x,
                     double y,
                     float scale,
                     int seed,
                     int overlayFlags,
                     @Nullable String stackCountText,
                     int durabilityThresholdPercent,
                     int durabilityTextColorThresholdPercent) {
        item(null, stack, x, y, scale, seed, overlayFlags, stackCountText,
                durabilityThresholdPercent, durabilityTextColorThresholdPercent);
    }

    public void item(@Nullable LocalPlayer player,
                     ItemStack stack,
                     double x,
                     double y,
                     float scale,
                     int seed,
                     int overlayFlags,
                     @Nullable String stackCountText) {
        item(player, stack, x, y, scale, seed, overlayFlags, stackCountText,
                DEFAULT_ITEM_DURABILITY_THRESHOLD, DEFAULT_ITEM_DURABILITY_TEXT_COLOR_THRESHOLD);
    }

    public void item(@Nullable LocalPlayer player,
                     ItemStack stack,
                     double x,
                     double y,
                     float scale,
                     int seed,
                     int overlayFlags,
                     @Nullable String stackCountText,
                     int durabilityThresholdPercent,
                     int durabilityTextColorThresholdPercent) {
        item(player, stack, x, y, scale, scale, 0.0f, 0.0f, false, seed, true, overlayFlags, stackCountText,
                durabilityThresholdPercent, durabilityTextColorThresholdPercent);
    }

    public void itemPivot(ItemStack stack,
                          double x,
                          double y,
                          float scaleX,
                          float scaleY,
                          float pivotX,
                          float pivotY,
                          int seed,
                          int overlayFlags,
                          @Nullable String stackCountText) {
        item(null, stack, x, y, scaleX, scaleY, pivotX, pivotY, true, seed, true, overlayFlags, stackCountText,
                DEFAULT_ITEM_DURABILITY_THRESHOLD, DEFAULT_ITEM_DURABILITY_TEXT_COLOR_THRESHOLD);
    }

    public void itemOverlay(ItemStack stack,
                            double x,
                            double y,
                            float scale,
                            int overlayFlags,
                            @Nullable String stackCountText) {
        item(null, stack, x, y, scale, scale, 0.0f, 0.0f, false, 0, false, overlayFlags, stackCountText,
                DEFAULT_ITEM_DURABILITY_THRESHOLD, DEFAULT_ITEM_DURABILITY_TEXT_COLOR_THRESHOLD);
    }

    // Use this only when call-site coordinates are in raw framebuffer/unscaled space
    // and must be converted into the currently active Renderer2D projection.
    public void itemUnscaled(ItemStack stack,
                             double x,
                             double y,
                             float scale,
                             int seed,
                             int overlayFlags,
                             @Nullable String stackCountText) {
        float ratio = getUnscaledItemRatio();
        item(stack, x * ratio, y * ratio, scale * ratio, seed, overlayFlags, stackCountText);
    }

    public void itemPivotUnscaled(ItemStack stack,
                                  double x,
                                  double y,
                                  float scaleX,
                                  float scaleY,
                                  float pivotX,
                                  float pivotY,
                                  int seed,
                                  int overlayFlags,
                                  @Nullable String stackCountText) {
        float ratio = getUnscaledItemRatio();
        itemPivot(
                stack,
                x * ratio,
                y * ratio,
                scaleX * ratio,
                scaleY * ratio,
                pivotX * ratio,
                pivotY * ratio,
                seed,
                overlayFlags,
                stackCountText
        );
    }

    public void itemOverlayUnscaled(ItemStack stack,
                                    double x,
                                    double y,
                                    float scale,
                                    int overlayFlags,
                                    @Nullable String stackCountText) {
        float ratio = getUnscaledItemRatio();
        itemOverlay(stack, x * ratio, y * ratio, scale * ratio, overlayFlags, stackCountText);
    }

    public void item(@Nullable LocalPlayer player,
                     ItemStack stack,
                     double x,
                     double y,
                     float scaleX,
                     float scaleY,
                     float pivotX,
                     float pivotY,
                     boolean pivoted,
                     int seed,
                     boolean drawItem,
                     int overlayFlags,
                     @Nullable String stackCountText) {
        item(player, stack, x, y, scaleX, scaleY, pivotX, pivotY, pivoted, seed, drawItem, overlayFlags, stackCountText,
                DEFAULT_ITEM_DURABILITY_THRESHOLD, DEFAULT_ITEM_DURABILITY_TEXT_COLOR_THRESHOLD);
    }

    public void item(@Nullable LocalPlayer player,
                     ItemStack stack,
                     double x,
                     double y,
                     float scaleX,
                     float scaleY,
                     float pivotX,
                     float pivotY,
                     boolean pivoted,
                     int seed,
                     boolean drawItem,
                     int overlayFlags,
                     @Nullable String stackCountText,
                     int durabilityThresholdPercent,
                     int durabilityTextColorThresholdPercent) {
        Renderer2DItems.submit(this,
                player, stack, x, y, scaleX, scaleY, pivotX, pivotY, pivoted, seed, drawItem, overlayFlags,
                stackCountText, durabilityThresholdPercent, durabilityTextColorThresholdPercent);
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

    public void svg(Identifier svgId, double x, double y, double width, double height) {
        svg(svgId, x, y, width, height, SvgRenderOptions.DEFAULT);
    }

    public void svg(String svgName, double x, double y, double width, double height) {
        svg(svgName, x, y, width, height, SvgRenderOptions.DEFAULT);
    }

    public void svg(String svgName,
                    double x,
                    double y,
                    double width,
                    double height,
                    SvgRenderOptions options) {
        if (textured) {
            throw new IllegalStateException("SVG mesh drawing is supported only on Renderer2D.COLOR.");
        }
        Identifier id = SvgRegistry.resolve(svgName);
        if (id == null) return;
        SvgMeshBackend.draw(this, id, x, y, width, height, options);
    }

    public void svg(Identifier svgId,
                    double x,
                    double y,
                    double width,
                    double height,
                    SvgRenderOptions options) {
        if (textured) {
            throw new IllegalStateException("SVG mesh drawing is supported only on Renderer2D.COLOR.");
        }
        SvgMeshBackend.draw(this, svgId, x, y, width, height, options);
    }

    public void svg(Path svgPath, double x, double y, double width, double height) {
        svg(svgPath, x, y, width, height, SvgRenderOptions.DEFAULT);
    }

    public void svg(Path svgPath,
                    double x,
                    double y,
                    double width,
                    double height,
                    SvgRenderOptions options) {
        if (textured) {
            throw new IllegalStateException("SVG mesh drawing is supported only on Renderer2D.COLOR.");
        }
        SvgMeshBackend.draw(this, svgPath, x, y, width, height, options);
    }

    public void textureQuad(GpuTextureView samplerView,
                            GpuSampler sampler,
                            double x,
                            double y,
                            double w,
                            double h) {
        textureQuad(samplerView, sampler, x, y, w, h, 0xFFFFFFFF);
    }

    public void textureQuad(GpuTextureView samplerView,
                            GpuSampler sampler,
                            double x,
                            double y,
                            double w,
                            double h,
                            int argb) {
        Renderer2DTextures.textureQuad(this, samplerView, sampler, x, y, w, h, argb);
    }

    public void msdfTextureQuad(GpuTextureView samplerView,
                                GpuSampler sampler,
                                double x,
                                double y,
                                double w,
                                double h,
                                int argb,
                                float pxRange,
                                int atlasWidth,
                                int atlasHeight) {
        msdfTextureQuad(samplerView, sampler, x, y, w, h, argb, argb, argb, argb, pxRange, atlasWidth, atlasHeight);
    }

    public void msdfTextureQuad(GpuTextureView samplerView,
                                GpuSampler sampler,
                                double x,
                                double y,
                                double w,
                                double h,
                                int topLeft,
                                int topRight,
                                int bottomRight,
                                int bottomLeft,
                                float pxRange,
                                int atlasWidth,
                                int atlasHeight) {
        Renderer2DTextures.msdfTextureQuad(this, samplerView, sampler, x, y, w, h,
                topLeft, topRight, bottomRight, bottomLeft, pxRange, atlasWidth, atlasHeight);
    }

    public void quadBatch(Consumer<QuadBatch> consumer) {
        Renderer2DRects.quadBatch(this, consumer);
    }

    public void line(double x1, double y1, double x2, double y2, int argb) {
        Renderer2DLines.line(this, x1, y1, x2, y2, argb);
    }

    public void line(double x1, double y1, double x2, double y2, RenderColor color) {
        Renderer2DLines.line(this, x1, y1, x2, y2, color);
    }

    public void boxLines(double x, double y, double width, double height, int argb) {
        Renderer2DLines.boxLines(this, x, y, width, height, argb);
    }

    public void quad(double x, double y, double width, double height, int argb) {
        quad(x, y, width, height, argb, argb, argb, argb);
    }

    public void quad(double x, double y, double width, double height, int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        Renderer2DRects.quadCorners(this, x, y, width, height, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void roundedRect(double x, double y, double w, double h,
                            float radius, float softness, int argb) {
        roundedRectMasked(x, y, w, h, x, y, w, h, radius, 0.0f, argb);
    }

    public void roundedRect(double x, double y, double w, double h,
                            float radius, int argb) {
        roundedRect(x, y, w, h, radius, 0.0f, argb);
    }

    public void circle(double cx, double cy, double radius, int argb) {
        circle(cx, cy, radius, 1.1f, argb);
    }

    public void circle(double cx, double cy, double radius, float softness, int argb) {
        Renderer2DCircles.circle(this, cx, cy, radius, softness, argb);
    }

    public void circleSoftShadow(double cx, double cy, double radius, float blur, float innerAlpha, int argb) {
        if (radius <= 0.0) return;
        double size = radius * 2.0;
        roundedRectSoftShadow(cx - radius, cy - radius, size, size, (float) radius, blur, innerAlpha, argb);
    }

    public void circleStroke(double cx, double cy, double radius, double thickness, int argb) {
        circleStroke(cx, cy, radius, thickness, 1.1f, argb);
    }

    public void circleStroke(double cx, double cy, double radius, double thickness, float softness, int argb) {
        Renderer2DCircles.circleStroke(this, cx, cy, radius, thickness, softness, argb);
    }

    public void arcStroke(double cx, double cy, double radius, double thickness,
                          float startAngleDeg, float endAngleDeg, int argb) {
        arcStroke(cx, cy, radius, thickness, startAngleDeg, endAngleDeg, 1.1f, argb);
    }

    public void arcStroke(double cx, double cy, double radius, double thickness,
                          float startAngleDeg, float endAngleDeg, float softness, int argb) {
        arcStrokeQuad(cx, cy, radius, thickness, startAngleDeg, endAngleDeg, softness, true,
                argb, argb, argb, argb);
    }

    public void arcStrokeFlat(double cx, double cy, double radius, double thickness,
                              float startAngleDeg, float endAngleDeg, float softness, int argb) {
        arcStrokeQuad(cx, cy, radius, thickness, startAngleDeg, endAngleDeg, softness, false,
                argb, argb, argb, argb);
    }

    public void arcStrokeFlat(double cx, double cy, double radius, double thickness,
                              float startAngleDeg, float endAngleDeg, int argb) {
        arcStrokeFlat(cx, cy, radius, thickness, startAngleDeg, endAngleDeg, 0.0f, argb);
    }

    public void arcStrokeGradient(double cx, double cy, double radius, double thickness,
                                  float startAngleDeg, float endAngleDeg, float softness,
                                  int startArgb, int endArgb, float angleDeg) {
        arcStrokeGradient(cx, cy, radius, thickness, startAngleDeg, endAngleDeg, softness,
                startArgb, endArgb, angleDeg, 0.0f);
    }

    public void arcStrokeGradient(double cx, double cy, double radius, double thickness,
                                  float startAngleDeg, float endAngleDeg,
                                  int startArgb, int endArgb, float angleDeg) {
        arcStrokeGradient(cx, cy, radius, thickness, startAngleDeg, endAngleDeg, 0.0f,
                startArgb, endArgb, angleDeg, 0.0f);
    }

    public void arcStrokeGradient(double cx, double cy, double radius, double thickness,
                                  float startAngleDeg, float endAngleDeg,
                                  int startArgb, int endArgb, float angleDeg, float offsetPx) {
        arcStrokeGradient(cx, cy, radius, thickness, startAngleDeg, endAngleDeg, 0.0f,
                startArgb, endArgb, angleDeg, offsetPx);
    }

    public void arcStrokeGradient(double cx, double cy, double radius, double thickness,
                                  float startAngleDeg, float endAngleDeg, float softness,
                                  int startArgb, int endArgb, float angleDeg, float offsetPx) {
        double outerRadius = radius + Math.max(0.0, thickness) * 0.5 + softness;
        computeLinearGradientColors((float) (outerRadius * 2.0), (float) (outerRadius * 2.0),
                startArgb, endArgb, angleDeg, offsetPx, gradientTmp);
        arcStrokeQuad(cx, cy, radius, thickness, startAngleDeg, endAngleDeg, softness, true,
                gradientTmp[0], gradientTmp[1], gradientTmp[2], gradientTmp[3]);
    }

    public void arcStrokeQuad(double cx, double cy, double radius, double thickness,
                              float startAngleDeg, float endAngleDeg, float softness,
                              int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        arcStrokeQuad(cx, cy, radius, thickness, startAngleDeg, endAngleDeg, softness, true,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void arcStrokeQuad(double cx, double cy, double radius, double thickness,
                              float startAngleDeg, float endAngleDeg, float softness, boolean caps,
                              int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        Renderer2DCircles.arcStrokeQuad(this, cx, cy, radius, thickness, startAngleDeg, endAngleDeg, softness, caps,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void orbizRing(double cx, double cy,
                         double radius, double thickness,
                         float startAngleDeg, float sweepAngleDeg,
                         float softness, float glowRadius, float glowStrength,
                         int primaryArgb, int secondaryArgb,
                         int colorMode, float angularOffset, float angularCycles,
                         float trackAlpha, float bodyAlpha, float headBoost,
                         float trackGlow, boolean caps) {
        Renderer2DOrbiz.orbizRing(this, cx, cy, radius, thickness, startAngleDeg, sweepAngleDeg,
                softness, glowRadius, glowStrength, primaryArgb, secondaryArgb,
                colorMode, angularOffset, angularCycles, trackAlpha, bodyAlpha, headBoost, trackGlow, caps);
    }

    public void roundedRectMasked(double x, double y, double w, double h,
                                  double maskX, double maskY, double maskW, double maskH,
                                  float radius, float softness, int argb) {
        roundedRectMaskedQuad(x, y, w, h, maskX, maskY, maskW, maskH, radius, 0.0f, argb, argb, argb, argb);
    }

    public void roundedRectStroke(double x, double y, double w, double h,
                                  float radius, float softness, float thickness, int argb) {
        Renderer2DRectsExtra.roundedRectStroke(this, x, y, w, h, radius, softness, thickness, argb);
    }

    public void roundedRectStroke(double x, double y, double w, double h,
                                  float radius, float thickness, int argb) {
        roundedRectStroke(x, y, w, h, radius, 0.0f, thickness, argb);
    }

    public void roundedRectStrokeAngularGradient(double x, double y, double w, double h,
                                                 float radius, float softness, float thickness,
                                                 int startArgb, int endArgb, float offset) {
        Renderer2DRectsExtra.roundedRectStrokeAngularGradient(this, x, y, w, h, radius, softness, thickness,
                startArgb, endArgb, offset);
    }

    public void roundedRectStrokeCorners(double x, double y, double w, double h,
                                         float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                         float softness, float thickness, int argb) {
        Renderer2DRectsExtra.roundedRectStrokeCorners(this, x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL,
                softness, thickness, argb);
    }

    public void roundedRectStrokeCorners(double x, double y, double w, double h,
                                         float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                         float thickness, int argb) {
        roundedRectStrokeCorners(x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL, 0.0f, thickness, argb);
    }

    public void roundedRectCorners(double x, double y, double w, double h,
                                   float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                   float softness, int argb) {
        roundedRectCornersQuad(x, y, w, h,
                radiusTL, radiusTR, radiusBR, radiusBL,
                0.0f,
                argb, argb, argb, argb);
    }

    public void roundedRectCorners(double x, double y, double w, double h,
                                   float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                   int argb) {
        roundedRectCorners(x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL, 0.0f, argb);
    }

    public void roundedRectCornersQuad(double x, double y, double w, double h,
                                       float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                       float softness,
                                       int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        Renderer2DRects.roundedRectCornersQuad(this, x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL, softness,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void roundedRectCornersQuad(double x, double y, double w, double h,
                                       float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                       int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        roundedRectCornersQuad(x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL, 0.0f,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void roundedRectGradient(double x, double y, double w, double h,
                                    float radius, float softness,
                                    int startArgb, int endArgb, float angleDeg) {
        roundedRectGradient(x, y, w, h, radius, 0.0f, startArgb, endArgb, angleDeg, 0.0f);
    }

    public void roundedRectGradient(double x, double y, double w, double h,
                                    float radius, int startArgb, int endArgb, float angleDeg) {
        roundedRectGradient(x, y, w, h, radius, 0.0f, startArgb, endArgb, angleDeg, 0.0f);
    }

    public void roundedRectGradient(double x, double y, double w, double h,
                                    float radius, float softness,
                                    int startArgb, int endArgb, float angleDeg, float offsetPx) {
        computeLinearGradientColors((float) w, (float) h, startArgb, endArgb, angleDeg, offsetPx, gradientTmp);
        roundedRectGradientQuad(x, y, w, h, radius, 0.0f, gradientTmp[0], gradientTmp[1], gradientTmp[2], gradientTmp[3]);
    }

    public void roundedRectGradient(double x, double y, double w, double h,
                                    float radius,
                                    int startArgb, int endArgb, float angleDeg, float offsetPx) {
        roundedRectGradient(x, y, w, h, radius, 0.0f, startArgb, endArgb, angleDeg, offsetPx);
    }

    /**
     * Draws a progress segment clipped by the full rounded bar outline.
     *
     * <p>Unlike drawing a rounded rectangle with the current fill width, this keeps the moving
     * progress edge straight and lets only the fixed outer bar bounds define rounded ends. That
     * prevents small health values from becoming a pill/capsule and prevents the fill from leaking
     * outside the left/right rounded shell when the progress edge crosses an outer corner.</p>
     */
    public void roundedProgressRectGradient(double x, double y, double w, double h,
                                            float radius, float softness,
                                            float progress,
                                            boolean fromRight,
                                            int startArgb, int endArgb,
                                            float angleDeg, float offsetPx) {
        Renderer2DRectsExtra.roundedProgressRectGradient(this, x, y, w, h, radius, softness, progress, fromRight,
                startArgb, endArgb, angleDeg, offsetPx);
    }

    public void roundedProgressRectGradient(double x, double y, double w, double h,
                                            float radius,
                                            float progress,
                                            boolean fromRight,
                                            int startArgb, int endArgb,
                                            float angleDeg, float offsetPx) {
        roundedProgressRectGradient(x, y, w, h, radius, 0.0f, progress, fromRight,
                startArgb, endArgb, angleDeg, offsetPx);
    }

    public void roundedSmokeFill(double x, double y, double w, double h,
                                 float radius, float softness,
                                 float fillRatio,
                                 boolean fromRight,
                                 int firstArgb,
                                 int secondArgb,
                                 int thirdArgb,
                                 float time,
                                 float smokeScale,
                                 float smokeMix,
                                 int octaves,
                                 float flowX,
                                 float flowY,
                                 float intensity) {
        Renderer2DSmokeFill.roundedSmokeFill(this, x, y, w, h, radius, softness, fillRatio, fromRight,
                firstArgb, secondArgb, thirdArgb, time, smokeScale, smokeMix, octaves, flowX, flowY, intensity);
    }

    public void roundedSmokeFill(double x, double y, double w, double h,
                                 float radius,
                                 float fillRatio,
                                 boolean fromRight,
                                 int firstArgb,
                                 int secondArgb,
                                 int thirdArgb,
                                 float time,
                                 float smokeScale,
                                 float smokeMix,
                                 int octaves,
                                 float flowX,
                                 float flowY,
                                 float intensity) {
        roundedSmokeFill(x, y, w, h, radius, 0.0f, fillRatio, fromRight,
                firstArgb, secondArgb, thirdArgb, time, smokeScale, smokeMix, octaves, flowX, flowY, intensity);
    }

    public void roundedRectStrokeGradient(double x, double y, double w, double h,
                                          float radius, float softness, float thickness,
                                          int startArgb, int endArgb, float angleDeg) {
        roundedRectStrokeGradient(x, y, w, h, radius, 0.0f, thickness, startArgb, endArgb, angleDeg, 0.0f);
    }

    public void roundedRectStrokeGradient(double x, double y, double w, double h,
                                          float radius, float thickness,
                                          int startArgb, int endArgb, float angleDeg) {
        roundedRectStrokeGradient(x, y, w, h, radius, 0.0f, thickness, startArgb, endArgb, angleDeg, 0.0f);
    }

    public void roundedRectStrokeGradient(double x, double y, double w, double h,
                                          float radius, float softness, float thickness,
                                          int startArgb, int endArgb, float angleDeg, float offsetPx) {
        computeLinearGradientColors((float) w, (float) h, startArgb, endArgb, angleDeg, offsetPx, gradientTmp);
        roundedRectStrokeGradientQuad(x, y, w, h, radius, 0.0f, thickness, gradientTmp[0], gradientTmp[1], gradientTmp[2], gradientTmp[3]);
    }

    public void roundedRectStrokeGradient(double x, double y, double w, double h,
                                          float radius, float thickness,
                                          int startArgb, int endArgb, float angleDeg, float offsetPx) {
        roundedRectStrokeGradient(x, y, w, h, radius, 0.0f, thickness, startArgb, endArgb, angleDeg, offsetPx);
    }

    public void roundedRectGlow(double x, double y, double w, double h,
                                float radius, float softness, float glow, int argb) {
        Renderer2DRectsGlow.roundedRectGlow(this, x, y, w, h, radius, softness, glow, argb);
    }

    public void roundedRectShadow(double x, double y, double w, double h,
                                  float radius, float softness, float spread, int argb) {
        Renderer2DRectsGlow.roundedRectShadow(this, x, y, w, h, radius, softness, spread, argb);
    }

    public void roundedRectSoftShadow(double x, double y, double w, double h,
                                      float radius, float blur, float innerAlpha, int argb) {
        Renderer2DRectsGlow.roundedRectSoftShadow(this, x, y, w, h, radius, blur, innerAlpha, argb);
    }

    public void radialGlowMasked(double x, double y, double w, double h,
                                 float radius, float softness,
                                 float glowRadius, float cx, float cy,
                                 int argb) {
        Renderer2DRectsGlow.radialGlowMasked(this, x, y, w, h, radius, softness, glowRadius, cx, cy, argb);
    }

    public void roundedRectGradientQuad(double x, double y, double w, double h,
                                        float radius, float softness,
                                        int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        roundedRectMaskedQuad(x, y, w, h, x, y, w, h, radius, 0.0f, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void roundedRectGradientQuad(double x, double y, double w, double h,
                                        float radius,
                                        int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        roundedRectGradientQuad(x, y, w, h, radius, 0.0f, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void roundedRectMaskedQuad(double x, double y, double w, double h,
                                      double maskX, double maskY, double maskW, double maskH,
                                      float radius, float softness,
                                      int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        Renderer2DRects.roundedRectMaskedQuad(this, x, y, w, h, maskX, maskY, maskW, maskH, radius, softness,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void roundedRectStrokeGradientQuad(double x, double y, double w, double h,
                                              float radius, float softness, float thickness,
                                              int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        Renderer2DRectsExtra.roundedRectStrokeGradientQuad(this, x, y, w, h, radius, softness, thickness,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void roundedRectStrokeGradientQuad(double x, double y, double w, double h,
                                              float radius, float thickness,
                                              int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        roundedRectStrokeGradientQuad(x, y, w, h, radius, 0.0f, thickness,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void roundedTexRect(double x, double y, double w, double h,
                               float radius, float softness,
                               double texX1, double texY1, double texX2, double texY2,
                               int argb, GpuTextureView samplerView, GpuSampler sampler) {
        Renderer2DTextures.roundedTexRect(this, x, y, w, h, radius, softness, texX1, texY1, texX2, texY2,
                argb, samplerView, sampler);
    }

    public void roundedTexMaskRect(double x, double y, double w, double h,
                                   float radius, float softness,
                                   double texX1, double texY1, double texX2, double texY2,
                                   int argb, GpuTextureView samplerView, GpuSampler sampler) {
        Renderer2DTextures.roundedTexMaskRect(this, x, y, w, h, radius, softness, texX1, texY1, texX2, texY2,
                argb, samplerView, sampler);
    }

    public void roundedTexRect(double x, double y, double w, double h,
                               float radius, float softness, int argb,
                               Identifier textureId) {
        if (!textured) {
            throw new IllegalStateException("Renderer2D is not configured for textures.");
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || textureId == null) return;
        AbstractTexture tex = mc.getTextureManager().getTexture(textureId);
        if (tex == null) return;
        GpuTextureView view = tex.getTextureView();
        GpuSampler sampler = tex.getSampler();
        if (view == null || sampler == null) return;
        roundedTexRect(x, y, w, h, radius, 0.0f, 0, 0, 1, 1, argb, view, sampler);
    }

    public void roundedTexRect(double x, double y, double w, double h,
                               float radius, int argb,
                               Identifier textureId) {
        roundedTexRect(x, y, w, h, radius, 0.0f, argb, textureId);
    }

    public void roundedTexMaskRect(double x, double y, double w, double h,
                                   float radius, float softness, int argb,
                                   Identifier textureId) {
        if (!textured) {
            throw new IllegalStateException("Renderer2D is not configured for textures.");
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || textureId == null) return;
        AbstractTexture tex = mc.getTextureManager().getTexture(textureId);
        if (tex == null) return;
        GpuTextureView view = tex.getTextureView();
        GpuSampler sampler = tex.getSampler();
        if (view == null || sampler == null) return;
        roundedTexMaskRect(x, y, w, h, radius, 0.0f, 0, 0, 1, 1, argb, view, sampler);
    }

    public void roundedTexMaskRect(double x, double y, double w, double h,
                                   float radius, int argb,
                                   Identifier textureId) {
        roundedTexMaskRect(x, y, w, h, radius, 0.0f, argb, textureId);
    }

    public void roundedTexRect(double x, double y, double w, double h,
                               float radius, float softness,
                               double texX1, double texY1, double texX2, double texY2,
                               int argb, Identifier textureId) {
        if (!textured) {
            throw new IllegalStateException("Renderer2D is not configured for textures.");
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || textureId == null) return;
        AbstractTexture tex = mc.getTextureManager().getTexture(textureId);
        if (tex == null) return;
        GpuTextureView view = tex.getTextureView();
        GpuSampler sampler = tex.getSampler();
        if (view == null || sampler == null) return;
        roundedTexRect(x, y, w, h, radius, 0.0f, texX1, texY1, texX2, texY2, argb, view, sampler);
    }

    public void roundedTexMaskRect(double x, double y, double w, double h,
                                   float radius, float softness,
                                   double texX1, double texY1, double texX2, double texY2,
                                   int argb, Identifier textureId) {
        if (!textured) {
            throw new IllegalStateException("Renderer2D is not configured for textures.");
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || textureId == null) return;
        AbstractTexture tex = mc.getTextureManager().getTexture(textureId);
        if (tex == null) return;
        GpuTextureView view = tex.getTextureView();
        GpuSampler sampler = tex.getSampler();
        if (view == null || sampler == null) return;
        roundedTexMaskRect(x, y, w, h, radius, 0.0f, texX1, texY1, texX2, texY2, argb, view, sampler);
    }

    public void texQuad(double x, double y, double width, double height, int argb) {
        texQuad(x, y, width, height, 0, 0, 1, 1, argb);
    }

    public void quadGradient(double x, double y, double width, double height,
                             int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        quad(x, y, width, height, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void quadGradientLinear(double x, double y, double width, double height,
                                   int startArgb, int endArgb, float angleDeg) {
        quadGradientLinear(x, y, width, height, startArgb, endArgb, angleDeg, 0.0f);
    }

    public void quadGradientLinear(double x, double y, double width, double height,
                                   int startArgb, int endArgb, float angleDeg, float offsetPx) {
        computeLinearGradientColors((float) width, (float) height, startArgb, endArgb, angleDeg, offsetPx, gradientTmp);
        quad(x, y, width, height, gradientTmp[0], gradientTmp[1], gradientTmp[2], gradientTmp[3]);
    }

    public void quadStrokeGradientLinear(double x, double y, double width, double height,
                                         float thickness,
                                         int startArgb, int endArgb, float angleDeg) {
        quadStrokeGradientLinear(x, y, width, height, thickness, startArgb, endArgb, angleDeg, 0.0f);
    }

    public void quadStrokeGradientLinear(double x, double y, double width, double height,
                                         float thickness,
                                         int startArgb, int endArgb, float angleDeg, float offsetPx) {
        roundedRectStrokeGradient(x, y, width, height, 0.0f, 0.0f, thickness, startArgb, endArgb, angleDeg, offsetPx);
    }

    public void chamferedRect(double x, double y, double width, double height, double chamfer, int argb) {
        chamferedRectQuad(x, y, width, height, chamfer, argb, argb, argb, argb);
    }

    public void chamferedRect(double x, double y, double width, double height,
                              double chamferTL, double chamferTR, double chamferBR, double chamferBL,
                              int argb) {
        chamferedRectQuad(x, y, width, height,
                chamferTL, chamferTR, chamferBR, chamferBL,
                argb, argb, argb, argb);
    }

    public void chamferedRectGradient(double x, double y, double width, double height,
                                      double chamfer, int startArgb, int endArgb, float angleDeg, float offsetPx) {
        computeLinearGradientColors((float) width, (float) height, startArgb, endArgb, angleDeg, offsetPx, gradientTmp);
        chamferedRectQuad(x, y, width, height, chamfer, gradientTmp[0], gradientTmp[1], gradientTmp[2], gradientTmp[3]);
    }

    public void chamferedRectGradient(double x, double y, double width, double height,
                                      double chamferTL, double chamferTR, double chamferBR, double chamferBL,
                                      int startArgb, int endArgb, float angleDeg, float offsetPx) {
        computeLinearGradientColors((float) width, (float) height, startArgb, endArgb, angleDeg, offsetPx, gradientTmp);
        chamferedRectQuad(x, y, width, height,
                chamferTL, chamferTR, chamferBR, chamferBL,
                gradientTmp[0], gradientTmp[1], gradientTmp[2], gradientTmp[3]);
    }

    public void chamferedRectStroke(double x, double y, double width, double height,
                                    double chamfer, double thickness, int argb) {
        chamferedRectStrokeQuad(x, y, width, height, chamfer, thickness, argb, argb, argb, argb);
    }

    public void chamferedRectStroke(double x, double y, double width, double height,
                                    double chamferTL, double chamferTR, double chamferBR, double chamferBL,
                                    double thickness, int argb) {
        chamferedRectStrokeQuad(x, y, width, height,
                chamferTL, chamferTR, chamferBR, chamferBL,
                thickness, argb, argb, argb, argb);
    }

    public void chamferedRectStrokeGradient(double x, double y, double width, double height,
                                            double chamfer, double thickness,
                                            int startArgb, int endArgb, float angleDeg, float offsetPx) {
        computeLinearGradientColors((float) width, (float) height, startArgb, endArgb, angleDeg, offsetPx, gradientTmp);
        chamferedRectStrokeQuad(x, y, width, height, chamfer, thickness,
                gradientTmp[0], gradientTmp[1], gradientTmp[2], gradientTmp[3]);
    }

    public void chamferedRectStrokeGradient(double x, double y, double width, double height,
                                            double chamferTL, double chamferTR, double chamferBR, double chamferBL,
                                            double thickness,
                                            int startArgb, int endArgb, float angleDeg, float offsetPx) {
        computeLinearGradientColors((float) width, (float) height, startArgb, endArgb, angleDeg, offsetPx, gradientTmp);
        chamferedRectStrokeQuad(x, y, width, height,
                chamferTL, chamferTR, chamferBR, chamferBL,
                thickness, gradientTmp[0], gradientTmp[1], gradientTmp[2], gradientTmp[3]);
    }

    public void chamferedRectQuad(double x, double y, double w, double h,
                                  double chamfer,
                                  int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        chamferedRectQuad(x, y, w, h,
                chamfer, chamfer, chamfer, chamfer,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void chamferedRectQuad(double x, double y, double w, double h,
                                  double chamferTL, double chamferTR, double chamferBR, double chamferBL,
                                  int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        normalizeChamfers(w, h,
                (float) chamferTL,
                (float) chamferTR,
                (float) chamferBR,
                (float) chamferBL,
                cornerRadiiTmp);
        chamferedRectQuadAxes(x, y, w, h,
                cornerRadiiTmp[0], cornerRadiiTmp[0],
                cornerRadiiTmp[1], cornerRadiiTmp[1],
                cornerRadiiTmp[2], cornerRadiiTmp[2],
                cornerRadiiTmp[3], cornerRadiiTmp[3],
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void chamferedRectQuadAxes(double x, double y, double w, double h,
                                      double chamferTLX, double chamferTLY,
                                      double chamferTRX, double chamferTRY,
                                      double chamferBRX, double chamferBRY,
                                      double chamferBLX, double chamferBLY,
                                      int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        Renderer2DChamfered.chamferedRectQuadAxes(this, x, y, w, h,
                chamferTLX, chamferTLY, chamferTRX, chamferTRY, chamferBRX, chamferBRY, chamferBLX, chamferBLY,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void chamferedRectStrokeQuad(double x, double y, double w, double h,
                                        double chamfer, double thickness,
                                        int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        chamferedRectStrokeQuad(x, y, w, h,
                chamfer, chamfer, chamfer, chamfer,
                thickness, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void chamferedRectStrokeQuad(double x, double y, double w, double h,
                                        double chamferTL, double chamferTR, double chamferBR, double chamferBL,
                                        double thickness,
                                        int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        Renderer2DChamfered.chamferedRectStrokeQuad(this, x, y, w, h,
                chamferTL, chamferTR, chamferBR, chamferBL, thickness,
                cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void cutCornerRect(double x, double y, double width, double height, double cut, int argb) {
        chamferedRect(x, y, width, height, cut, argb);
    }

    public void cutCornerRectGradient(double x, double y, double width, double height,
                                      double cut, int startArgb, int endArgb, float angleDeg, float offsetPx) {
        chamferedRectGradient(x, y, width, height, cut, startArgb, endArgb, angleDeg, offsetPx);
    }

    public void cutCornerRectStroke(double x, double y, double width, double height,
                                    double cut, double thickness, int argb) {
        chamferedRectStroke(x, y, width, height, cut, thickness, argb);
    }

    public void cutCornerRectStrokeGradient(double x, double y, double width, double height,
                                            double cut, double thickness,
                                            int startArgb, int endArgb, float angleDeg, float offsetPx) {
        chamferedRectStrokeGradient(x, y, width, height, cut, thickness, startArgb, endArgb, angleDeg, offsetPx);
    }

    public void notchedRect(double x, double y, double width, double height,
                            double notchWidth, double notchDepth, int argb) {
        int count = buildNotchedRect(rectShapeTmp, x, y, width, height, notchWidth, notchDepth);
        Renderer2DPolygons.polygon(this, rectShapeTmp, count, argb);
    }

    public void notchedRectGradient(double x, double y, double width, double height,
                                    double notchWidth, double notchDepth,
                                    int startArgb, int endArgb, float angleDeg, float offsetPx) {
        int count = buildNotchedRect(rectShapeTmp, x, y, width, height, notchWidth, notchDepth);
        Renderer2DPolygons.polygonGradient(this, rectShapeTmp, count, x, y, width, height, startArgb, endArgb, angleDeg, offsetPx);
    }

    public void notchedRectStroke(double x, double y, double width, double height,
                                  double notchWidth, double notchDepth, double thickness, int argb) {
        int count = buildNotchedRect(rectShapeTmp, x, y, width, height, notchWidth, notchDepth);
        Renderer2DPolygons.polygonStroke(this, rectShapeTmp, count, thickness, true, argb);
    }

    public void notchedRectStrokeGradient(double x, double y, double width, double height,
                                          double notchWidth, double notchDepth, double thickness,
                                          int startArgb, int endArgb, float angleDeg, float offsetPx) {
        int count = buildNotchedRect(rectShapeTmp, x, y, width, height, notchWidth, notchDepth);
        Renderer2DPolygons.polylineLinearGradient(this, rectShapeTmp, count, thickness, true,
                x, y, width, height, startArgb, endArgb, angleDeg, offsetPx, false);
    }

    public void beveledRect(double x, double y, double width, double height,
                            double bevel, int fillArgb, int highlightArgb, int shadowArgb) {
        Renderer2DChamfered.beveledRect(this, x, y, width, height, bevel, fillArgb, highlightArgb, shadowArgb);
    }

    public void beveledRectStroke(double x, double y, double width, double height,
                                  double bevel, double thickness, int argb) {
        chamferedRectStroke(x, y, width, height, bevel, thickness, argb);
    }

    public void beveledRectGradient(double x, double y, double width, double height,
                                    double bevel, int startArgb, int endArgb,
                                    int highlightArgb, int shadowArgb, float angleDeg, float offsetPx) {
        Renderer2DChamfered.beveledRectGradient(this, x, y, width, height, bevel, startArgb, endArgb,
                highlightArgb, shadowArgb, angleDeg, offsetPx);
    }

    public void beveledRectStrokeGradient(double x, double y, double width, double height,
                                          double bevel, double thickness,
                                          int startArgb, int endArgb, float angleDeg, float offsetPx) {
        chamferedRectStrokeGradient(x, y, width, height, bevel, thickness, startArgb, endArgb, angleDeg, offsetPx);
    }

    public void connector(double x1, double y1, double x2, double y2, double thickness, int argb) {
        Renderer2DConnectors.connector(this, x1, y1, x2, y2, thickness, argb);
    }

    public void connectorGradient(double x1, double y1, double x2, double y2,
                                  double thickness, int startArgb, int endArgb) {
        Renderer2DConnectors.connectorGradient(this, x1, y1, x2, y2, thickness, startArgb, endArgb);
    }

    public void wire(double x1, double y1, double x2, double y2, double thickness, int argb) {
        connector(x1, y1, x2, y2, thickness, argb);
    }

    public void wireGradient(double x1, double y1, double x2, double y2,
                             double thickness, int startArgb, int endArgb) {
        connectorGradient(x1, y1, x2, y2, thickness, startArgb, endArgb);
    }

    public void cable(double x1, double y1, double x2, double y2,
                      double thickness, int outerArgb, int innerArgb) {
        connector(x1, y1, x2, y2, thickness + 2.0, outerArgb);
        connector(x1, y1, x2, y2, Math.max(1.0, thickness), innerArgb);
    }

    public void cableGradient(double x1, double y1, double x2, double y2,
                              double thickness, int outerStartArgb, int outerEndArgb,
                              int innerStartArgb, int innerEndArgb) {
        connectorGradient(x1, y1, x2, y2, thickness + 2.0, outerStartArgb, outerEndArgb);
        connectorGradient(x1, y1, x2, y2, Math.max(1.0, thickness), innerStartArgb, innerEndArgb);
    }

    /**
     * Connects two rounded rectangles by their visible border, not by their centers.
     * The connector uses the centers only as the direction vector used to find each border anchor.
     */
    public void roundedRectConnector(double x1, double y1, double w1, double h1, double radius1,
                                     double x2, double y2, double w2, double h2, double radius2,
                                     double thickness, int argb) {
        Renderer2DConnectors.roundedRectConnector(this, x1, y1, w1, h1, radius1, x2, y2, w2, h2, radius2, thickness, argb);
    }

    public void roundedRectConnectorGradient(double x1, double y1, double w1, double h1, double radius1,
                                             double x2, double y2, double w2, double h2, double radius2,
                                             double thickness, int startArgb, int endArgb) {
        Renderer2DConnectors.roundedRectConnectorGradient(this, x1, y1, w1, h1, radius1, x2, y2, w2, h2, radius2,
                thickness, startArgb, endArgb);
    }

    /**
     * Draws a Bezier node graph edge between two rounded rectangles, anchored on their borders.
     */
    public void roundedRectNodeGraphEdge(double x1, double y1, double w1, double h1, double radius1,
                                         double x2, double y2, double w2, double h2, double radius2,
                                         double thickness, int argb) {
        Renderer2DConnectors.roundedRectNodeGraphEdge(this, x1, y1, w1, h1, radius1, x2, y2, w2, h2, radius2, thickness, argb);
    }

    public void roundedRectNodeGraphEdgeGradient(double x1, double y1, double w1, double h1, double radius1,
                                                 double x2, double y2, double w2, double h2, double radius2,
                                                 double thickness, int startArgb, int endArgb) {
        Renderer2DConnectors.roundedRectNodeGraphEdgeGradient(this, x1, y1, w1, h1, radius1, x2, y2, w2, h2, radius2,
                thickness, startArgb, endArgb);
    }

    /**
     * Draws an orthogonal connector between two rounded rectangles, anchored on their borders.
     */
    public void roundedRectOrthogonalConnector(double x1, double y1, double w1, double h1, double radius1,
                                               double x2, double y2, double w2, double h2, double radius2,
                                               double thickness, int argb) {
        roundedRectOrthogonalConnector(x1, y1, w1, h1, radius1, x2, y2, w2, h2, radius2,
                (x1 + w1 * 0.5 + x2 + w2 * 0.5) * 0.5, thickness, argb);
    }

    /**
     * Draws an orthogonal connector between two rounded rectangles, anchored on their borders.
     */
    public void roundedRectOrthogonalConnector(double x1, double y1, double w1, double h1, double radius1,
                                               double x2, double y2, double w2, double h2, double radius2,
                                               double midX, double thickness, int argb) {
        Renderer2DConnectors.roundedRectOrthogonalConnector(this, x1, y1, w1, h1, radius1, x2, y2, w2, h2, radius2,
                midX, thickness, argb);
    }

    public void roundedRectOrthogonalConnectorGradient(double x1, double y1, double w1, double h1, double radius1,
                                                       double x2, double y2, double w2, double h2, double radius2,
                                                       double midX, double thickness, int startArgb, int endArgb) {
        Renderer2DConnectors.roundedRectOrthogonalConnectorGradient(this, x1, y1, w1, h1, radius1,
                x2, y2, w2, h2, radius2, midX, thickness, startArgb, endArgb);
    }

    public void orthogonalConnector(double x1, double y1, double x2, double y2,
                                    double thickness, int argb) {
        orthogonalConnector(x1, y1, x2, y2, (x1 + x2) * 0.5, thickness, argb);
    }

    public void orthogonalConnector(double x1, double y1, double x2, double y2,
                                    double midX, double thickness, int argb) {
        Renderer2DConnectors.orthogonalConnector(this, x1, y1, x2, y2, midX, thickness, argb);
    }

    public void orthogonalConnectorGradient(double x1, double y1, double x2, double y2,
                                            double midX, double thickness, int startArgb, int endArgb) {
        Renderer2DConnectors.orthogonalConnectorGradient(this, x1, y1, x2, y2, midX, thickness, startArgb, endArgb);
    }

    public void bezierConnector(double x1, double y1,
                                double cx1, double cy1,
                                double cx2, double cy2,
                                double x2, double y2,
                                double thickness,
                                int argb) {
        bezierConnector(x1, y1, cx1, cy1, cx2, cy2, x2, y2, 20, thickness, argb);
    }

    public void bezierConnector(double x1, double y1,
                                double cx1, double cy1,
                                double cx2, double cy2,
                                double x2, double y2,
                                int segments,
                                double thickness,
                                int argb) {
        Renderer2DConnectors.bezierConnector(this, x1, y1, cx1, cy1, cx2, cy2, x2, y2, segments, thickness, argb);
    }

    public void bezierConnectorGradient(double x1, double y1,
                                        double cx1, double cy1,
                                        double cx2, double cy2,
                                        double x2, double y2,
                                        int segments,
                                        double thickness,
                                        int startArgb,
                                        int endArgb) {
        Renderer2DConnectors.bezierConnectorGradient(this, x1, y1, cx1, cy1, cx2, cy2, x2, y2, segments, thickness, startArgb, endArgb);
    }

    public void nodeGraphEdge(double x1, double y1, double x2, double y2, double thickness, int argb) {
        Renderer2DConnectors.nodeGraphEdge(this, x1, y1, x2, y2, thickness, argb);
    }

    public void nodeGraphEdgeGradient(double x1, double y1, double x2, double y2,
                                      double thickness, int startArgb, int endArgb) {
        Renderer2DConnectors.nodeGraphEdgeGradient(this, x1, y1, x2, y2, thickness, startArgb, endArgb);
    }

    public void spline(double[] points, int pointCount, double thickness, boolean closed, int argb) {
        if (points == null || pointCount < 2) return;
        Renderer2DPolygons.polyline(this, points, pointCount, thickness, closed, argb, true);
    }

    public void splineGradient(double[] points, int pointCount, double thickness, boolean closed,
                               int startArgb, int endArgb) {
        if (points == null || pointCount < 2) return;
        Renderer2DPolygons.polylineGradient(this, points, pointCount, thickness, closed, startArgb, endArgb, true);
    }

    public void texQuad(double x, double y, double width, double height,
                        double texX1, double texY1, double texX2, double texY2,
                        int argb) {
        Renderer2DTextures.texQuad(this, x, y, width, height, texX1, texY1, texX2, texY2, argb);
    }

    public void liquidGlassCircle(double cx, double cy, double radius,
                                  float softness,
                                  int tintArgb,
                                  float globalAlpha,
                                  float fresnelPower,
                                  float fresnelAlpha,
                                  float baseAlpha,
                                  float fresnelMix,
                                  float distortPx,
                                  float squirclePower) {
        if (radius <= 0.0) return;
        double size = radius * 2.0;
        liquidGlassRect(cx - radius, cy - radius, size, size,
                (float) radius, softness,
                tintArgb,
                globalAlpha,
                fresnelPower,
                fresnelAlpha,
                baseAlpha,
                fresnelMix,
                distortPx,
                squirclePower);
    }

    public void liquidGlassCircle(double cx, double cy, double radius,
                                  int tintArgb,
                                  float globalAlpha,
                                  LiquidGlassPreset preset) {
        if (radius <= 0.0) return;
        double size = radius * 2.0;
        liquidGlassRect(cx - radius, cy - radius, size, size,
                (float) radius,
                tintArgb,
                globalAlpha,
                preset);
    }

    //Если squirclePower < 1.5f, поведение без squircle
    public void liquidGlassRect(double x, double y, double w, double h,
                                float radius, float softness,
                                int tintArgb,
                                float globalAlpha,
                                float fresnelPower,
                                float fresnelAlpha,
                                float baseAlpha,
                                float fresnelMix,
                                float distortPx) {
        liquidGlassRect(x, y, w, h,
                radius, softness,
                tintArgb,
                globalAlpha,
                fresnelPower,
                fresnelAlpha,
                baseAlpha,
                fresnelMix,
                distortPx,
                0.0f);
    }

    public void liquidGlassRect(double x, double y, double w, double h,
                                float radius, float softness,
                                int tintArgb,
                                float globalAlpha,
                                float fresnelPower,
                                float fresnelAlpha,
                                float baseAlpha,
                                float fresnelMix,
                                float distortPx,
                                float squirclePower) {
        liquidGlassRect(x, y, w, h,
                radius, softness,
                tintArgb,
                globalAlpha,
                globalAlpha,
                fresnelPower,
                fresnelAlpha,
                baseAlpha,
                fresnelMix,
                distortPx,
                squirclePower);
    }

    public void liquidGlassRect(double x, double y, double w, double h,
                                float radius, float softness,
                                int tintArgb,
                                float glassAlpha,
                                float blurAlpha,
                                float fresnelPower,
                                float fresnelAlpha,
                                float baseAlpha,
                                float fresnelMix,
                                float distortPx,
                                float squirclePower) {
        liquidGlassRectCorners(x, y, w, h,
                radius, radius, radius, radius,
                softness,
                tintArgb,
                glassAlpha,
                blurAlpha,
                fresnelPower,
                fresnelAlpha,
                baseAlpha,
                fresnelMix,
                distortPx,
                squirclePower);
    }

    public void liquidGlassRect(double x, double y, double w, double h,
                                float radius,
                                int tintArgb,
                                float globalAlpha,
                                LiquidGlassPreset preset) {
        liquidGlassRect(x, y, w, h,
                radius,
                tintArgb,
                globalAlpha,
                globalAlpha,
                preset,
                1.0f,
                0.0f);
    }

    public void liquidGlassRect(double x, double y, double w, double h,
                                float radius,
                                int tintArgb,
                                float glassAlpha,
                                float blurAlpha,
                                LiquidGlassPreset preset) {
        liquidGlassRect(x, y, w, h,
                radius,
                tintArgb,
                glassAlpha,
                blurAlpha,
                preset,
                1.0f,
                0.0f);
    }

    public void liquidGlassRect(double x, double y, double w, double h,
                                float radius,
                                int tintArgb,
                                float globalAlpha,
                                LiquidGlassPreset preset,
                                float squirclePower) {
        liquidGlassRect(x, y, w, h,
                radius,
                tintArgb,
                globalAlpha,
                globalAlpha,
                preset,
                1.0f,
                squirclePower);
    }

    public void liquidGlassRect(double x, double y, double w, double h,
                                float radius,
                                int tintArgb,
                                float glassAlpha,
                                float blurAlpha,
                                LiquidGlassPreset preset,
                                float squirclePower) {
        liquidGlassRect(x, y, w, h,
                radius,
                tintArgb,
                glassAlpha,
                blurAlpha,
                preset,
                1.0f,
                squirclePower);
    }

    public void liquidGlassRect(double x, double y, double w, double h,
                                float radius,
                                int tintArgb,
                                float globalAlpha,
                                LiquidGlassPreset preset,
                                float thicknessScale,
                                float squirclePower) {
        liquidGlassRect(x, y, w, h, radius, tintArgb, globalAlpha, globalAlpha, preset, thicknessScale, squirclePower);
    }

    public void liquidGlassRect(double x, double y, double w, double h,
                                float radius,
                                int tintArgb,
                                float glassAlpha,
                                float blurAlpha,
                                LiquidGlassPreset preset,
                                float thicknessScale,
                                float squirclePower) {
        LiquidGlassPreset p = preset != null ? preset : LiquidGlassPreset.BALANCED;
        float materialAlpha = clamp01(glassAlpha);
        float blurStrength = clamp01(blurAlpha);
        float scale = Math.max(0.001f, thicknessScale);
        liquidGlassRect(x, y, w, h,
                radius, p.thicknessPx * scale,
                tintArgb,
                materialAlpha,
                blurStrength,
                p.fresnelPower,
                p.fresnelAlpha,
                p.baseAlpha,
                p.fresnelMix,
                p.distortPx * materialAlpha,
                squirclePower);
    }

    public void liquidGlassRectCorners(double x, double y, double w, double h,
                                       float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                       int tintArgb,
                                       float globalAlpha,
                                       LiquidGlassPreset preset) {
        liquidGlassRectCorners(x, y, w, h,
                radiusTL, radiusTR, radiusBR, radiusBL,
                tintArgb,
                globalAlpha,
                globalAlpha,
                preset,
                1.0f,
                0.0f);
    }

    public void liquidGlassRectCorners(double x, double y, double w, double h,
                                       float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                       int tintArgb,
                                       float glassAlpha,
                                       float blurAlpha,
                                       LiquidGlassPreset preset) {
        liquidGlassRectCorners(x, y, w, h,
                radiusTL, radiusTR, radiusBR, radiusBL,
                tintArgb,
                glassAlpha,
                blurAlpha,
                preset,
                1.0f,
                0.0f);
    }

    public void liquidGlassRectCorners(double x, double y, double w, double h,
                                       float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                       int tintArgb,
                                       float globalAlpha,
                                       LiquidGlassPreset preset,
                                       float squirclePower) {
        liquidGlassRectCorners(x, y, w, h,
                radiusTL, radiusTR, radiusBR, radiusBL,
                tintArgb,
                globalAlpha,
                globalAlpha,
                preset,
                1.0f,
                squirclePower);
    }

    public void liquidGlassRectCorners(double x, double y, double w, double h,
                                       float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                       int tintArgb,
                                       float glassAlpha,
                                       float blurAlpha,
                                       LiquidGlassPreset preset,
                                       float squirclePower) {
        liquidGlassRectCorners(x, y, w, h,
                radiusTL, radiusTR, radiusBR, radiusBL,
                tintArgb,
                glassAlpha,
                blurAlpha,
                preset,
                1.0f,
                squirclePower);
    }

    public void liquidGlassRectCorners(double x, double y, double w, double h,
                                       float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                       int tintArgb,
                                       float globalAlpha,
                                       LiquidGlassPreset preset,
                                       float thicknessScale,
                                       float squirclePower) {
        liquidGlassRectCorners(x, y, w, h,
                radiusTL, radiusTR, radiusBR, radiusBL,
                tintArgb,
                globalAlpha,
                globalAlpha,
                preset,
                thicknessScale,
                squirclePower);
    }

    public void liquidGlassRectCorners(double x, double y, double w, double h,
                                       float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                       int tintArgb,
                                       float glassAlpha,
                                       float blurAlpha,
                                       LiquidGlassPreset preset,
                                       float thicknessScale,
                                       float squirclePower) {
        LiquidGlassPreset p = preset != null ? preset : LiquidGlassPreset.BALANCED;
        float materialAlpha = clamp01(glassAlpha);
        float blurStrength = clamp01(blurAlpha);
        float scale = Math.max(0.001f, thicknessScale);
        liquidGlassRectCorners(x, y, w, h,
                radiusTL, radiusTR, radiusBR, radiusBL,
                p.thicknessPx * scale,
                tintArgb,
                materialAlpha,
                blurStrength,
                p.fresnelPower,
                p.fresnelAlpha,
                p.baseAlpha,
                p.fresnelMix,
                p.distortPx * materialAlpha,
                squirclePower);
    }

    public void liquidGlassRectCorners(double x, double y, double w, double h,
                                       float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                       float softness,
                                       int tintArgb,
                                       float globalAlpha,
                                       float fresnelPower,
                                       float fresnelAlpha,
                                       float baseAlpha,
                                       float fresnelMix,
                                       float distortPx) {
        liquidGlassRectCorners(x, y, w, h,
                radiusTL, radiusTR, radiusBR, radiusBL,
                softness,
                tintArgb,
                globalAlpha,
                fresnelPower,
                fresnelAlpha,
                baseAlpha,
                fresnelMix,
                distortPx,
                0.0f);
    }

    public void liquidGlassRectCorners(double x, double y, double w, double h,
                                       float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                       float softness,
                                       int tintArgb,
                                       float globalAlpha,
                                       float fresnelPower,
                                       float fresnelAlpha,
                                       float baseAlpha,
                                       float fresnelMix,
                                       float distortPx,
                                       float squirclePower) {
        liquidGlassRectCorners(x, y, w, h,
                radiusTL, radiusTR, radiusBR, radiusBL,
                softness,
                tintArgb,
                globalAlpha,
                globalAlpha,
                fresnelPower,
                fresnelAlpha,
                baseAlpha,
                fresnelMix,
                distortPx,
                squirclePower);
    }

    public void liquidGlassRectCorners(double x, double y, double w, double h,
                                       float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                       float softness,
                                       int tintArgb,
                                       float glassAlpha,
                                       float blurAlpha,
                                       float fresnelPower,
                                       float fresnelAlpha,
                                       float baseAlpha,
                                       float fresnelMix,
                                       float distortPx,
                                       float squirclePower) {
        Renderer2DLiquidGlass.liquidGlassRectCorners(this, x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL, softness,
                tintArgb, glassAlpha, blurAlpha, fresnelPower, fresnelAlpha, baseAlpha, fresnelMix, distortPx, squirclePower);
    }

    public void blurRect(double x, double y, double w, double h,
                         float radius, float quality, float brightness, float alpha, int ignoredTintRgb) {
        Renderer2DBlur.blurRect(this, x, y, w, h, radius, quality, brightness, alpha, ignoredTintRgb, UiBatchType.BLUR);
    }

    public void glassBlurRect(double x, double y, double w, double h,
                              float radius, float quality, float brightness, float alpha, int ignoredTintRgb) {
        Renderer2DBlur.glassBlurRect(this, x, y, w, h, radius, quality, brightness, alpha, ignoredTintRgb);
    }

    public void blurChamferedRect(double x, double y, double w, double h,
                                  double chamfer, float quality, float brightness, float alpha, int ignoredTintRgb) {
        Renderer2DBlur.blurChamferedRect(this, x, y, w, h, chamfer, quality, brightness, alpha, ignoredTintRgb);
    }

    public void blurComposite(Consumer<BlurComposite> consumer) {
        Renderer2DBlur.blurComposite(this, consumer);
    }

    public void polygon(double[] points, int pointCount, int argb) {
        Renderer2DPolygons.polygon(this, points, pointCount, argb);
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

    public static UiStatsSnapshot getUiStatsSnapshot() {
        return UiRenderDispatcher.statsSnapshot();
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

    public static boolean shouldDefer2DSubmit() {
        return UiDeferredScheduler.shouldDefer();
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

    public static void enqueueDeferred2D(Deferred2DSubmit submit) {
        UiDeferredScheduler.enqueue(submit);
    }

    public static Deferred2DLayer deferredLayerForCurrentPhase(boolean pureItemOverlaySubmit) {
        return UiDeferredScheduler.layerForCurrentPhase(pureItemOverlaySubmit);
    }

    public static OrderedUiBatcher obtainDeferredBatcher() {
        return UiDeferredScheduler.obtainBatcher();
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
        return RenderWarpStack.push(RenderWarp.perspective(
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

    public static float getUnscaledItemRatio() {
        float scaleFactor = ViewportContext.getScaleFactor();
        if (scaleFactor == 0.0f) {
            return 1.0f;
        }
        return ViewportContext.getUiScale() / scaleFactor;
    }

    public static TextureTarget ensureUiEffects(Minecraft mc) {
        return UiBlurResources.ensureEffects(mc);
    }

    public static TextureTarget ensureUiGlassSource(Minecraft mc) {
        return UiBlurResources.ensureGlassSource(mc);
    }

    public static boolean copyMainColorToGlassSource(RenderTarget source, TextureTarget target) {
        return UiBlurResources.copyMainColor(source, target);
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
        uiGlassWorldSourceReady = UiBlurResources.isWorldSourceReady();
    }

    public static void invalidateWorldGlassSource() {
        UiBlurResources.invalidateWorldSource();
        uiGlassWorldSourceReady = false;
    }

    public static boolean isWorldGlassSourceReady() {
        return UiBlurResources.isWorldSourceReady();
    }

    public static TextureTarget ensureUiBlurEffects(Minecraft mc) {
        return UiBlurResources.ensureKawaseDown(mc, 0);
    }

    public static TextureTarget ensureUiBlurEffectsAlt(Minecraft mc) {
        return UiBlurResources.ensureKawaseUp(mc, 0);
    }

    public static TextureTarget ensureUiKawaseDown(Minecraft mc, int level) {
        return UiBlurResources.ensureKawaseDown(mc, level);
    }

    public static TextureTarget ensureUiKawaseUp(Minecraft mc, int level) {
        return UiBlurResources.ensureKawaseUp(mc, level);
    }

    public static MeshBuilder ensureUiCompositeMesh(int width, int height) {
        return UiBlurResources.ensureCompositeMesh(width, height);
    }

    public static long currentFrameBlurFrameId() {
        return UiBlurResources.currentFrameId();
    }

    public static RenderPhase currentFrameBlurPhase() {
        return UiBlurResources.currentPhase();
    }

    public static int blurScaleBits(float uiScale) {
        return UiBlurResources.scaleBits(uiScale);
    }

    public static @Nullable FrameBlurCacheEntry findReusableFrameBlur(
            long frameId,
            RenderPhase phase,
            @Nullable GpuTextureView sourceView,
            @Nullable GpuSampler sourceSampler,
            float screenW,
            float screenH,
            float uiScale,
            BlurQuality blurQuality,
            float offsetPx) {
        return UiBlurResources.findReusable(
                frameId, phase, sourceView, sourceSampler, screenW, screenH, uiScale, blurQuality, offsetPx
        );
    }

    public static void rememberFrameBlur(
            long frameId,
            RenderPhase phase,
            @Nullable GpuTextureView sourceView,
            @Nullable GpuSampler sourceSampler,
            @Nullable GpuTextureView blurredView,
            @Nullable GpuSampler blurredSampler,
            float screenW,
            float screenH,
            float uiScale,
            BlurQuality blurQuality,
            float offsetPx) {
        UiBlurResources.remember(
                frameId, phase, sourceView, sourceSampler, blurredView, blurredSampler,
                screenW, screenH, uiScale, blurQuality, offsetPx
        );
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

    public enum LiquidGlassPreset {
        LIGHT(7.0f, -5.50f, 0.70f, 0.42f, 0.40f, 0.030f),
        BALANCED(11.0f, -7.50f, 0.78f, 0.52f, 0.42f, 0.040f),
        HEAVY(15.0f, -9.00f, 0.86f, 0.62f, 0.48f, 0.052f),
        HUD_SMALL(11.0f, -12.00f, 0.84f, 0.78f, 0.22f, 0.055f),
        HUD_LARGE(13.5f, -14.00f, 0.88f, 0.72f, 0.20f, 0.060f),
        HEALTH_BAR(3.25f, -18.00f, 0.52f, 0.96f, 0.055f, 0.0030f);

        private final float thicknessPx;
        private final float fresnelPower;
        private final float fresnelAlpha;
        private final float baseAlpha;
        private final float fresnelMix;
        private final float distortPx;

        LiquidGlassPreset(float thicknessPx,
                          float fresnelPower,
                          float fresnelAlpha,
                          float baseAlpha,
                          float fresnelMix,
                          float distortPx) {
            this.thicknessPx = thicknessPx;
            this.fresnelPower = fresnelPower;
            this.fresnelAlpha = fresnelAlpha;
            this.baseAlpha = baseAlpha;
            this.fresnelMix = fresnelMix;
            this.distortPx = distortPx;
        }
    }

    public static final class BlurComposite {
        private final MeshBuilder mesh;
        private final float globalAlpha;

        BlurComposite(MeshBuilder mesh, float globalAlpha) {
            this.mesh = mesh;
            this.globalAlpha = globalAlpha;
        }

        public void rect(double x, double y, double w, double h,
                         float quality, float brightness, float alpha, int ignoredTintRgb) {
            roundedRect(x, y, w, h, 0.0f, quality, brightness, alpha, ignoredTintRgb);
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

        QuadBatch(MeshBuilder mesh) {
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