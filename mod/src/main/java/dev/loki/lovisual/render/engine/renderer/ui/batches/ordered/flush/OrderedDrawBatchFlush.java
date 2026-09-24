package dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.flush;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.MeshRenderer;
import dev.loki.lovisual.render.engine.renderer.ui.batches.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.flush.OrderedFlushRenderer.Ctx;
import dev.loki.lovisual.render.engine.uniform.impl.ui.MsdfTextUniforms;

/**
 * Пути отрисовки DrawBatch: прямой блюр-оверлей, direct liquid-glass,
 * offscreen fx-композит и обычная отрисовка. Соответствует прежнему
 * телу цикла flush() по эффектам 1:1.
 */
final class OrderedDrawBatchFlush {
    private OrderedDrawBatchFlush() {
    }

    static void render(DrawBatch batch, Ctx ctx) {
        if (batch.mesh.isBuilding()) batch.mesh.end();
        if (batch.mesh.getIndicesCount() <= 0) return;

        ctx.drawCalls++;
        ctx.vertices += batch.mesh.getVertexCount();
        ctx.indices += batch.mesh.getIndicesCount();

        if (isFx(batch.type)) {
            int blurPassCalls = ctx.blur.prepare(ctx.mc, batch.view, batch.sampler, ctx.screenW, ctx.screenH, ctx.uiScale,
                    batch.blurQuality, batch.blurOffsetPx);
            if (ctx.blur.blurredView() != null && ctx.blur.blurredSampler() != null) {
                MeshRenderer.begin()
                        .attachments(ctx.mainColorView, null)
                        .pipeline(batch.type.pipeline)
                        .mesh(batch.mesh)
                        .uniform("UIBatch", ctx.uiBatch())
                        .sampler("u_Texture", ctx.blur.blurredView(), ctx.blur.blurredSampler())
                        .end();
                ctx.drawCalls += blurPassCalls;
                return;
            }
            if (batch.view == ctx.mainColorView) {
                return;
            }
        }

        if (batch.type == UiBatchType.LIQUID_GLASS) {
            renderLiquidGlass(batch, ctx);
            return;
        }

        if (isFx(batch.type)) {
            if (renderOffscreenFx(batch, ctx)) {
                return;
            }
            // Never sample from the main color attachment while writing to it.
            // If offscreen FX target is unavailable, skip this pass to avoid undefined behavior.
            if (batch.type.usesSampler
                    && batch.view == ctx.mainColorView
                    && !(batch.type == UiBatchType.LIQUID_GLASS && ctx.liquidSourceView != null)) {
                return;
            }
        }

        if (isSamplerOnMainGuarded(batch, ctx)) {
            return;
        }

        renderPlain(batch, ctx);
    }

    private static void renderLiquidGlass(DrawBatch batch, Ctx ctx) {
        GpuTextureView sourceView = ctx.liquidSourceView != null ? ctx.liquidSourceView : batch.view;
        GpuSampler sourceSampler = ctx.liquidSourceSampler != null ? ctx.liquidSourceSampler : batch.sampler;
        boolean clippedComposite = batch.shapeClipActive;
        if (clippedComposite) {
            ctx.blur.adoptCached(sourceView, sourceSampler, ctx.screenW, ctx.screenH, ctx.uiScale,
                    batch.blurQuality, batch.blurOffsetPx);
        } else {
            ctx.drawCalls += ctx.blur.prepare(ctx.mc, sourceView, sourceSampler, ctx.screenW, ctx.screenH, ctx.uiScale,
                    batch.blurQuality, batch.blurOffsetPx);
        }
        GpuTextureView liquidBlurView = ctx.blur.matchingView(sourceView, sourceSampler);
        GpuSampler liquidBlurSampler = ctx.blur.matchingSampler(sourceView, sourceSampler);
        if (liquidBlurView == null || liquidBlurSampler == null) {
            liquidBlurView = sourceView;
            liquidBlurSampler = sourceSampler;
        }

        if (sourceView == null || sourceSampler == null
                || liquidBlurView == null || liquidBlurSampler == null
                || sourceView == ctx.mainColorView
                || liquidBlurView == ctx.mainColorView) {
            return;
        }

        MeshRenderer.begin()
                .attachments(ctx.mainColorView, null)
                .pipeline(batch.type.pipeline)
                .mesh(batch.mesh)
                .uniform("UIBatch", ctx.uiBatch())
                .sampler("u_Texture", sourceView, sourceSampler)
                .sampler("u_BlurTexture", liquidBlurView, liquidBlurSampler)
                .end();
    }

    private static boolean renderOffscreenFx(DrawBatch batch, Ctx ctx) {
        if (!ctx.ensureFx()) {
            return false;
        }
        MeshRenderer fxBuilder = MeshRenderer.begin()
                .attachments(ctx.fxBuffer)
                .clearColor(0x00000000)
                .pipeline(batch.type.pipeline)
                .mesh(batch.mesh);

        if (batch.type.needsUiBatch) {
            fxBuilder.uniform("UIBatch", ctx.uiBatch());
        }
        if (batch.type.usesSampler) {
            GpuTextureView sourceView = batch.type == UiBatchType.LIQUID_GLASS && ctx.liquidSourceView != null ? ctx.liquidSourceView : batch.view;
            GpuSampler sourceSampler = batch.type == UiBatchType.LIQUID_GLASS && ctx.liquidSourceSampler != null ? ctx.liquidSourceSampler : batch.sampler;
            fxBuilder.sampler("u_Texture", sourceView, sourceSampler);
        }
        if (batch.type == UiBatchType.LIQUID_GLASS) {
            GpuTextureView liquidBlurView = ctx.blur.blurredView() != null ? ctx.blur.blurredView() : batch.view;
            GpuSampler liquidBlurSampler = ctx.blur.blurredSampler() != null ? ctx.blur.blurredSampler() : batch.sampler;
            if (liquidBlurView != null && liquidBlurSampler != null) {
                fxBuilder.sampler("u_BlurTexture", liquidBlurView, liquidBlurSampler);
            }
        }

        fxBuilder.end();

        MeshRenderer.begin()
                .attachments(ctx.mainColorView, null)
                .pipeline(LoVisualRenderPipelines.UI_TEXTURED)
                .mesh(ctx.fxCompositeMesh)
                .sampler("u_Texture", ctx.fxView, ctx.fxSampler)
                .end();

        ctx.drawCalls++;
        return true;
    }

    private static void renderPlain(DrawBatch batch, Ctx ctx) {
        MeshRenderer builder = MeshRenderer.begin()
                .attachments(ctx.mainColorView, null)
                .pipeline(batch.type.pipeline)
                .mesh(batch.mesh);

        if (batch.type.needsUiBatch) {
            builder.uniform("UIBatch", ctx.uiBatch());
        }
        if (batch.type.usesSampler) {
            GpuTextureView sourceView = batch.type == UiBatchType.LIQUID_GLASS && ctx.liquidSourceView != null ? ctx.liquidSourceView : batch.view;
            GpuSampler sourceSampler = batch.type == UiBatchType.LIQUID_GLASS && ctx.liquidSourceSampler != null ? ctx.liquidSourceSampler : batch.sampler;
            builder.sampler("u_Texture", sourceView, sourceSampler);
        }
        if (batch.type == UiBatchType.SVG_MSDF) {
            MsdfTextUniforms.update(batch.msdfPxRange, batch.msdfAtlasWidth, batch.msdfAtlasHeight);
            builder.uniform("MsdfText", MsdfTextUniforms.get());
        }
        if (batch.type == UiBatchType.LIQUID_GLASS) {
            GpuTextureView liquidBlurView = ctx.blur.blurredView() != null ? ctx.blur.blurredView() : batch.view;
            GpuSampler liquidBlurSampler = ctx.blur.blurredSampler() != null ? ctx.blur.blurredSampler() : batch.sampler;
            if (liquidBlurView != null && liquidBlurSampler != null) {
                builder.sampler("u_BlurTexture", liquidBlurView, liquidBlurSampler);
            }
        }

        builder.end();
    }

    private static boolean isFx(UiBatchType type) {
        return type == UiBatchType.BLUR || type == UiBatchType.GLASS_BLUR || type == UiBatchType.BLUR_COMPLEX;
    }

    private static boolean isSamplerOnMainGuarded(DrawBatch batch, Ctx ctx) {
        if (!isFx(batch.type) && batch.type != UiBatchType.LIQUID_GLASS) return false;
        return batch.type.usesSampler
                && batch.view == ctx.mainColorView
                && !(batch.type == UiBatchType.LIQUID_GLASS && ctx.liquidSourceView != null);
    }
}
