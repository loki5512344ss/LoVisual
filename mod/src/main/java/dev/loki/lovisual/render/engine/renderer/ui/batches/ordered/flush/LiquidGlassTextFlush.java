package dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.flush;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.renderer.MeshRenderer;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.batches.TextBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.flush.OrderedFlushRenderer.Ctx;
import dev.loki.lovisual.render.engine.renderer.ui.blur.kawase.SharedKawaseBlur;
import dev.loki.lovisual.render.engine.text.glyph.GlyphFont;
import dev.loki.lovisual.render.engine.uniform.impl.ui.MsdfTextUniforms;
import dev.loki.lovisual.render.engine.uniform.impl.ui.UIBatchUniforms;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.Nullable;

/**
 * Отрисовка текстового batch'а с liquid-glass подложкой: текст сэмплит
 * исходник сцены и разделяемый блюр поверх glyph-текстуры.
 */
final class LiquidGlassTextFlush {
    private LiquidGlassTextFlush() {
    }

    static int flush(TextBatch textBatch, Ctx ctx) {
        if (textBatch == null || textBatch.mesh == null || textBatch.font == null) return 0;
        if (!textBatch.font.isReady()) return 0;

        AbstractTexture glyphTexture = textBatch.font.getTexture();
        if (glyphTexture == null || glyphTexture.getTextureView() == null || glyphTexture.getSampler() == null) return 0;

        GpuTextureView sourceView = ctx.liquidSourceView;
        GpuSampler sourceSampler = ctx.liquidSourceSampler;
        if (sourceView == null || sourceSampler == null || sourceView == ctx.mainColorView) return 0;

        SharedKawaseBlur blur = ctx.blur;
        int blurPassCalls;
        if (textBatch.shapeClipActive) {
            blur.adoptCached(sourceView, sourceSampler, ctx.screenW, ctx.screenH, ctx.uiScale,
                    Renderer2D.DEFAULT_LIQUID_GLASS_BLUR_QUALITY, Renderer2D.LIQUID_GLASS_KAWASE_OFFSET_PX);
            blurPassCalls = 0;
        } else {
            blurPassCalls = blur.prepare(ctx.mc, sourceView, sourceSampler, ctx.screenW, ctx.screenH, ctx.uiScale,
                    Renderer2D.DEFAULT_LIQUID_GLASS_BLUR_QUALITY, Renderer2D.LIQUID_GLASS_KAWASE_OFFSET_PX);
        }
        GpuTextureView blurView = blur.matchingView(sourceView, sourceSampler);
        GpuSampler blurSampler = blur.matchingSampler(sourceView, sourceSampler);
        if (blurView == null || blurSampler == null) {
            blurView = sourceView;
            blurSampler = sourceSampler;
        }
        if (blurView == ctx.mainColorView) return 0;

        UIBatchUniforms.update(ctx.screenW, ctx.screenH);
        MeshRenderer builder = MeshRenderer.begin()
                .attachments(ctx.mainColorView, null)
                .pipeline(textBatch.pipeline)
                .mesh(textBatch.mesh)
                .uniform("UIBatch", UIBatchUniforms.get())
                .sampler("u_Texture", glyphTexture.getTextureView(), glyphTexture.getSampler())
                .sampler("u_SceneTexture", sourceView, sourceSampler)
                .sampler("u_BlurTexture", blurView, blurSampler);

        if (isMsdf(textBatch.font)) {
            MsdfTextUniforms.update(textBatch.font.getPxRange(), textBatch.font.getAtlasWidth(), textBatch.font.getAtlasHeight());
            builder.uniform("MsdfText", MsdfTextUniforms.get());
        }

        builder.end();
        return 1 + blurPassCalls;
    }

    private static boolean isMsdf(@Nullable GlyphFont font) {
        return font != null && font.isMsdf();
    }
}
