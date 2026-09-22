/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.batches.ordered;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.batches.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ItemBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.TextBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.flush.OrderedDeferral;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.flush.OrderedFlushRenderer;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.order.OrderState;
import dev.loki.lovisual.render.engine.renderer.ui.blur.kawase.SharedKawaseBlur;
import dev.loki.lovisual.render.engine.renderer.ui.deferred.sched.UiDeferredScheduler;
import dev.loki.lovisual.render.engine.text.backend.placement.TextPlacementMode;
import dev.loki.lovisual.render.engine.text.glyph.GlyphFont;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Координатор упорядоченного UI-батчинга: аккумулирует записи в OrderState,
 * при flush диспетчеризует их через OrderedFlushRenderer либо уходит в
 * deferred-слой через OrderedDeferral. Разделяемый блюр — SharedKawaseBlur.
 */
public final class OrderedUiBatcher {
    private final OrderState state = new OrderState();
    private final SharedKawaseBlur blur = new SharedKawaseBlur();
    boolean active;
    boolean flushing;

    public static void prewarmLiquidGlassBlur(Minecraft mc,
                                              @Nullable GpuTextureView sourceView,
                                              @Nullable GpuSampler sourceSampler,
                                              float screenW,
                                              float screenH,
                                              float uiScale) {
        SharedKawaseBlur.prewarmLiquidGlassBlur(mc, sourceView, sourceSampler, screenW, screenH, uiScale);
    }

    public void begin() {
        state.resetOrder();
        blur.reset();
        active = true;
        Renderer2D.BATCH_STATS.setActive(true);
    }

    public boolean isActive() {
        return active;
    }

    public boolean isFlushing() {
        return flushing;
    }

    public boolean hasPendingWork() {
        return state.hasPendingWork();
    }

    public DrawBatch getOrCreate(UiBatchType type, GpuTextureView view, GpuSampler sampler) {
        if (!active) return null;
        return state.getOrCreate(type, view, sampler);
    }

    public DrawBatch getOrCreateBlur(UiBatchType type, GpuTextureView view, GpuSampler sampler,
                                     Renderer2D.BlurQuality quality, float offsetPx) {
        if (!active) return null;
        return state.getOrCreateBlur(type, view, sampler, quality, offsetPx);
    }

    public DrawBatch getOrCreateMsdf(GpuTextureView view, GpuSampler sampler, float pxRange, int atlasWidth, int atlasHeight) {
        if (!active) return null;
        return state.getOrCreateMsdf(view, sampler, pxRange, atlasWidth, atlasHeight);
    }

    public ItemBatch getOrCreateItemBatch(@Nullable GuiGraphicsExtractor context, ItemStack stack) {
        if (!active) return null;
        return state.getOrCreateItemBatch(context, stack);
    }

    public TextBatch getOrCreateTextBatch(String label, GlyphFont font, RenderPipeline pipeline,
                                          TextPlacementMode placement, boolean liquidGlassText) {
        if (!active) return null;
        return state.getOrCreateTextBatch(label, font, pipeline, placement, liquidGlassText);
    }

    public void flush(boolean finish) {
        if (!active) return;
        if (this == Renderer2D.UI_BATCHER && UiDeferredScheduler.shouldDefer()) {
            OrderedDeferral.defer(this, state, blur, finish);
            return;
        }
        flushing = true;
        try {
            if (!state.hasPendingWork()) {
                Renderer2D.BATCH_STATS.noteEmpty(active, state.poolTotal());
                finishPhase(finish);
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                flushFailed("mc null", finish);
                return;
            }
            RenderTarget fb = mc.gameRenderer.mainRenderTarget();
            if (fb == null) {
                flushFailed("framebuffer null", finish);
                return;
            }
            GpuTextureView mainColorView = fb.getColorTextureView();
            if (mainColorView == null) {
                flushFailed("framebuffer color view null", finish);
                return;
            }

            float screenW = mc.getWindow().getWidth();
            float screenH = mc.getWindow().getHeight();
            float uiScale = ViewportContext.getUiScale();
            if (uiScale <= 0.0f) uiScale = 1.0f;
            blur.reset();

            OrderedFlushRenderer.FlushResult result =
                    OrderedFlushRenderer.flush(state, blur, mc, mainColorView, screenW, screenH, uiScale);

            int batches = state.entryCount();
            Renderer2D.BATCH_STATS.update(active, batches, result.drawCalls(), result.vertices(), result.indices(), state.poolTotal());
            Renderer2D.BATCH_STATS.addFrame(batches, result.drawCalls(), result.vertices(), result.indices());

            state.resetOrder();
            finishPhase(finish);
        } finally {
            flushing = false;
        }
    }

    public void prepareAsReplacement(boolean active) {
        state.resetOrder();
        blur.reset();
        this.active = active;
    }

    public void releaseAfterDeferred() {
        state.resetOrder();
        blur.reset();
        active = false;
        flushing = false;
    }

    public void closeRetainedBuffers() {
        state.resetOrder();
        blur.reset();
        active = false;
        flushing = false;
        state.closeRetained();
    }

    public void deactivate() {
        active = false;
        Renderer2D.BATCH_STATS.setActive(false);
    }

    private void flushFailed(String reason, boolean finish) {
        Renderer2D.BATCH_STATS.noteFailure(reason);
        state.resetOrder();
        blur.reset();
        finishPhase(finish);
    }

    private void finishPhase(boolean finish) {
        if (finish) {
            blur.reset();
            active = false;
            Renderer2D.BATCH_STATS.setActive(false);
        }
    }
}
