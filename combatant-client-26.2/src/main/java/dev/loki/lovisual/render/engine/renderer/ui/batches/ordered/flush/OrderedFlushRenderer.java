/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.flush;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessManager;
import dev.loki.lovisual.render.engine.renderer.ui.batches.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ItemBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.TextBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.order.OrderState;
import dev.loki.lovisual.render.engine.renderer.ui.blur.UiBlurResources;
import dev.loki.lovisual.render.engine.renderer.ui.blur.kawase.SharedKawaseBlur;
import dev.loki.lovisual.render.engine.renderer.ui.item.ItemBatchRenderer;
import dev.loki.lovisual.render.engine.text.render.TextRenderSystem;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.engine.uniform.impl.ui.UIBatchUniforms;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * Один проход flush'а order-списка: диспетчеризация item/text/draw записей.
 * Мутабельное состояние прохода (счётчики, ленивые fx-таргеты) живёт в Ctx.
 */
public final class OrderedFlushRenderer {
    private OrderedFlushRenderer() {
    }

    public record FlushResult(int drawCalls, int vertices, int indices) {
    }

    public static FlushResult flush(OrderState state, SharedKawaseBlur blur, Minecraft mc,
                                    GpuTextureView mainColorView, float screenW, float screenH, float uiScale) {
        Ctx ctx = new Ctx(blur, mc, mainColorView, screenW, screenH, uiScale);
        if (hasLiquidGlass(state.entries())) {
            resolveLiquidSource(ctx);
        }
        for (Object entry : state.entries()) {
            if (entry instanceof ItemBatch itemBatch) {
                if (!itemBatch.isEmpty()) {
                    ctx.drawCalls += ItemBatchRenderer.flush(itemBatch);
                }
                continue;
            }
            if (entry instanceof TextBatch textBatch) {
                if (!textBatch.isEmpty()) {
                    ctx.vertices += textBatch.mesh.getVertexCount();
                    ctx.indices += textBatch.mesh.getIndicesCount();
                    if (textBatch.liquidGlassText) {
                        ctx.drawCalls += LiquidGlassTextFlush.flush(textBatch, ctx);
                    } else {
                        ctx.drawCalls++;
                        TextRenderSystem.submitGlyphMeshImmediate(textBatch.label, textBatch.font, textBatch.mesh,
                                textBatch.pipeline, textBatch.placement);
                    }
                }
                continue;
            }
            OrderedDrawBatchFlush.render((DrawBatch) entry, ctx);
        }
        return new FlushResult(ctx.drawCalls, ctx.vertices, ctx.indices);
    }

    private static boolean hasLiquidGlass(ObjectArrayList<Object> order) {
        for (Object orderedEntry : order) {
            if (orderedEntry instanceof DrawBatch orderedBatch && orderedBatch.type == UiBatchType.LIQUID_GLASS) {
                return true;
            }
            if (orderedEntry instanceof TextBatch orderedText && orderedText.liquidGlassText) {
                return true;
            }
        }
        return false;
    }

    private static void resolveLiquidSource(Ctx ctx) {
        /*
         * The HUD/world glass source is captured before GUI extraction. Never perform a
         * fallback copy of the active main framebuffer from inside a batch flush: doing so
         * inserts a render-target transition in the middle of the HUD phase and can stall the
         * frame or temporarily discard already submitted lower layers.
         */
        TextureTarget sourceBuffer = UiBlurResources.ensureGlassSource(ctx.mc);
        GpuSampler copySampler = PostProcessManager.getSampler();
        if (UiBlurResources.isWorldSourceReady() && sourceBuffer != null && copySampler != null) {
            GpuTextureView sourceTargetView = sourceBuffer.getColorTextureView();
            if (sourceTargetView != null && sourceTargetView != ctx.mainColorView) {
                ctx.liquidSourceView = sourceTargetView;
                ctx.liquidSourceSampler = copySampler;
            }
        }
    }

    /**
     * Мутабельное состояние одного flush-прохода, общее для всех записей.
     */
    static final class Ctx {
        final SharedKawaseBlur blur;
        final Minecraft mc;
        final GpuTextureView mainColorView;
        final float screenW;
        final float screenH;
        final float uiScale;
        @Nullable GpuTextureView liquidSourceView;
        @Nullable GpuSampler liquidSourceSampler;
        @Nullable GpuBufferSlice uiBatch;
        @Nullable TextureTarget fxBuffer;
        @Nullable GpuTextureView fxView;
        @Nullable GpuSampler fxSampler;
        @Nullable MeshBuilder fxCompositeMesh;
        int drawCalls;
        int vertices;
        int indices;

        Ctx(SharedKawaseBlur blur, Minecraft mc, GpuTextureView mainColorView,
            float screenW, float screenH, float uiScale) {
            this.blur = blur;
            this.mc = mc;
            this.mainColorView = mainColorView;
            this.screenW = screenW;
            this.screenH = screenH;
            this.uiScale = uiScale;
        }

        GpuBufferSlice uiBatch() {
            if (uiBatch == null) {
                UIBatchUniforms.update(screenW, screenH);
                uiBatch = UIBatchUniforms.get();
            }
            return uiBatch;
        }

        boolean ensureFx() {
            if (fxBuffer != null) {
                return fxView != null && fxSampler != null && fxCompositeMesh != null;
            }
            fxBuffer = UiBlurResources.ensureEffects(mc);
            if (fxBuffer != null) {
                fxView = fxBuffer.getColorTextureView();
                fxSampler = PostProcessManager.getSampler();
                int compW = Math.max(1, Math.round(screenW / uiScale));
                int compH = Math.max(1, Math.round(screenH / uiScale));
                fxCompositeMesh = UiBlurResources.ensureCompositeMesh(compW, compH);
            }
            return fxBuffer != null && fxView != null && fxSampler != null && fxCompositeMesh != null;
        }
    }
}
