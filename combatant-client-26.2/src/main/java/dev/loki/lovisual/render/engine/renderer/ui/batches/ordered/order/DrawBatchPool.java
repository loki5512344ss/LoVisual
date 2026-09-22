/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.order;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.batches.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.ui.blur.UiBlurResources;
import dev.loki.lovisual.render.engine.renderer.ui.dispatch.UiRenderDispatcher;
import dev.loki.lovisual.render.helpers.clip.ClipFunction;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Пул DrawBatch (шейп/блюр/msdf) с переиспользованием по cursor'у и склейкой
 * соседних совместимых batch'ей. Порядок отрисовки задаёт общий order-список.
 */
final class DrawBatchPool {
    @SuppressWarnings("unchecked")
    private final ObjectArrayList<DrawBatch>[] pools = new ObjectArrayList[UiBatchType.values().length];
    private final int[] poolCursor = new int[UiBatchType.values().length];
    private final ObjectArrayList<Object> order;

    DrawBatchPool(ObjectArrayList<Object> order) {
        this.order = order;
        for (int i = 0; i < pools.length; i++) {
            pools[i] = new ObjectArrayList<>();
        }
    }

    DrawBatch getOrCreate(UiBatchType type, GpuTextureView view, GpuSampler sampler) {
        boolean shapeClipActive = ClipFunction.isShapeClipActive();
        DrawBatch last = lastMergeable(type, view, sampler, shapeClipActive);
        if (last != null) return last;
        DrawBatch batch = obtain(type);
        batch.begin(view, sampler, shapeClipActive);
        order.add(batch);
        UiRenderDispatcher.recordBackendCommand(type);
        return batch;
    }

    DrawBatch getOrCreateBlur(UiBatchType type, GpuTextureView view, GpuSampler sampler,
                              Renderer2D.BlurQuality quality, float offsetPx) {
        if (type == UiBatchType.LIQUID_GLASS) {
            UiBlurResources.requestLiquidGlassBlur();
        }
        boolean shapeClipActive = ClipFunction.isShapeClipActive();
        Renderer2D.BlurQuality normalizedQuality = quality != null ? quality : Renderer2D.DEFAULT_BLUR_QUALITY;
        float normalizedOffset = Float.isFinite(offsetPx) ? Math.max(0.0f, offsetPx) : Renderer2D.DEFAULT_KAWASE_OFFSET_PX;
        DrawBatch last = lastMergeableBlur(type, view, sampler, normalizedQuality, normalizedOffset, shapeClipActive);
        if (last != null) return last;
        DrawBatch batch = obtain(type);
        batch.beginBlur(view, sampler, normalizedQuality, normalizedOffset, shapeClipActive);
        order.add(batch);
        UiRenderDispatcher.recordBackendCommand(type);
        return batch;
    }

    DrawBatch getOrCreateMsdf(GpuTextureView view, GpuSampler sampler, float pxRange, int atlasWidth, int atlasHeight) {
        boolean shapeClipActive = ClipFunction.isShapeClipActive();
        DrawBatch last = lastMergeableMsdf(view, sampler, pxRange, atlasWidth, atlasHeight, shapeClipActive);
        if (last != null) return last;
        DrawBatch batch = obtain(UiBatchType.SVG_MSDF);
        batch.beginMsdf(view, sampler, pxRange, atlasWidth, atlasHeight, shapeClipActive);
        order.add(batch);
        UiRenderDispatcher.recordBackendCommand(UiBatchType.SVG_MSDF);
        return batch;
    }

    private DrawBatch lastMergeable(UiBatchType type, GpuTextureView view, GpuSampler sampler, boolean shapeClipActive) {
        if (order.isEmpty()) return null;
        Object last = order.get(order.size() - 1);
        return last instanceof DrawBatch drawBatch && drawBatch.canMerge(type, view, sampler, shapeClipActive)
                ? drawBatch
                : null;
    }

    private DrawBatch lastMergeableBlur(UiBatchType type, GpuTextureView view, GpuSampler sampler,
                                        Renderer2D.BlurQuality quality, float offsetPx, boolean shapeClipActive) {
        if (order.isEmpty()) return null;
        Object last = order.get(order.size() - 1);
        return last instanceof DrawBatch drawBatch
                && drawBatch.canMergeBlur(type, view, sampler, quality, offsetPx, shapeClipActive)
                ? drawBatch
                : null;
    }

    private DrawBatch lastMergeableMsdf(GpuTextureView view, GpuSampler sampler, float pxRange,
                                        int atlasWidth, int atlasHeight, boolean shapeClipActive) {
        if (order.isEmpty()) return null;
        Object last = order.get(order.size() - 1);
        return last instanceof DrawBatch drawBatch
                && drawBatch.canMergeMsdf(view, sampler, pxRange, atlasWidth, atlasHeight, shapeClipActive)
                ? drawBatch
                : null;
    }

    private DrawBatch obtain(UiBatchType type) {
        int idx = type.ordinal();
        ObjectArrayList<DrawBatch> pool = pools[idx];
        int cursor = poolCursor[idx];
        DrawBatch batch;
        if (cursor < pool.size()) {
            batch = pool.get(cursor);
        } else {
            batch = new DrawBatch(type);
            pool.add(batch);
        }
        poolCursor[idx] = cursor + 1;
        return batch;
    }

    void resetCursors() {
        for (int i = 0; i < poolCursor.length; i++) {
            poolCursor[i] = 0;
        }
    }

    int poolTotal() {
        int total = 0;
        for (ObjectArrayList<DrawBatch> pool : pools) {
            total += pool.size();
        }
        return total;
    }

    void closeRetained() {
        for (ObjectArrayList<DrawBatch> pool : pools) {
            for (int i = 0, size = pool.size(); i < size; i++) {
                DrawBatch batch = pool.get(i);
                if (batch != null && batch.mesh != null) {
                    batch.mesh.close();
                }
            }
        }
    }
}
