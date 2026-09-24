package dev.loki.lovisual.render.engine.renderer.renderer2d.fx;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.optimization.FpsOptimizer;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessManager;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.Batch2D;
import dev.loki.lovisual.render.engine.renderer.ui.blur.BlurSource;
import dev.loki.lovisual.render.engine.renderer.ui.batches.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.ui.draw.effect.UiEffectSpec;
import dev.loki.lovisual.render.engine.renderer.ui.draw.shape.UiShape;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiMeshGeometry.clamp01;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiMeshGeometry.clampChamfer;

/** Background blur rectangles fed from the main framebuffer. */
public final class Blur2D {
    private Blur2D() {
    }

    private static Renderer2D.BlurQuality legacyBlurQuality(float legacyQuality) {
        if (legacyQuality >= 32.0f) return Renderer2D.BlurQuality.ULTRA;
        if (legacyQuality >= 12.0f) return Renderer2D.BlurQuality.HIGH;
        if (legacyQuality >= 5.0f) return Renderer2D.BlurQuality.MEDIUM;
        return Renderer2D.BlurQuality.LOW;
    }

    private static float legacyKawaseOffset(float legacyQuality) {
        if (!Float.isFinite(legacyQuality)) return Renderer2D.DEFAULT_KAWASE_OFFSET_PX;
        return Mth.clamp(legacyQuality / 18.0f, 0.85f, 3.25f);
    }

    /** Main framebuffer color texture plus the shared post-process sampler. */
    public static BlurSource source() {
        FpsOptimizer fpsOptimizer = Modules.get(FpsOptimizer.class);
        if (fpsOptimizer != null && fpsOptimizer.isGlassDisabled()) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;
        RenderTarget fb = mc.gameRenderer.mainRenderTarget();
        if (fb == null) return null;
        GpuTextureView src = fb.getColorTextureView();
        if (src == null) return null;
        GpuSampler sampler = PostProcessManager.getSampler();
        if (sampler == null) return null;
        return new BlurSource(src, sampler);
    }

    public static void blurRect(Renderer2D r, double x, double y, double w, double h,
                                float radius, float quality, float brightness, float alpha, int ignoredTintRgb) {
        if (w <= 0.0 || h <= 0.0) return;
        if (alpha <= 0.001f) return;

        r.effect(UiEffectSpec.blur(UiShape.roundedRect(x, y, w, h, radius), radius, ignoredTintRgb));
        BlurSource source = source();
        if (source == null) return;

        DrawBatch batch = Batch2D.openBlur(r, UiBatchType.BLUR, source.view, source.sampler,
                legacyBlurQuality(quality), legacyKawaseOffset(quality));
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = 1.0;

        float finalAlpha = (float) (alpha * r.getAlpha());
        mesh.ensureQuadCapacity();
        int i1 = mesh.vec2(x, y).local2(x, y).color(255, 255, 255, 255).vec4(x, y, w, h).vec4(radius, quality, brightness, finalAlpha).next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(255, 255, 255, 255).vec4(x, y, w, h).vec4(radius, quality, brightness, finalAlpha).next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(255, 255, 255, 255).vec4(x, y, w, h).vec4(radius, quality, brightness, finalAlpha).next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(255, 255, 255, 255).vec4(x, y, w, h).vec4(radius, quality, brightness, finalAlpha).next();
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    public static void glassBlurRect(Renderer2D r, double x, double y, double w, double h,
                                     float radius, float quality, float brightness, float alpha, int ignoredTintRgb) {
        if (w <= 0.0 || h <= 0.0) return;
        if (alpha <= 0.001f) return;

        r.effect(UiEffectSpec.blur(UiShape.roundedRect(x, y, w, h, radius), radius, ignoredTintRgb));
        BlurSource source = source();
        if (source == null) return;

        DrawBatch batch = Batch2D.openBlur(r, UiBatchType.GLASS_BLUR, source.view, source.sampler,
                Renderer2D.BlurQuality.HIGH, legacyKawaseOffset(quality));
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.alpha = 1.0;

        float finalAlpha = clamp01((float) (alpha * r.getAlpha()));
        if (finalAlpha <= 0.001f) {
            Batch2D.close();
            return;
        }

        int a = Math.max(0, Math.min(255, Math.round(finalAlpha * 255.0f)));
        float smoothness = 2.0f;
        float rad = Math.max(0.0f, radius);

        mesh.ensureQuadCapacity();
        int i1 = mesh.vec2(x, y).local2(x, y).color(255, 255, 255, a)
                .vec4(x, y, w, h).vec4(rad, rad, rad, rad).vec4(quality, brightness, smoothness, 0.0f).next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(255, 255, 255, a)
                .vec4(x, y, w, h).vec4(rad, rad, rad, rad).vec4(quality, brightness, smoothness, 0.0f).next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(255, 255, 255, a)
                .vec4(x, y, w, h).vec4(rad, rad, rad, rad).vec4(quality, brightness, smoothness, 0.0f).next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(255, 255, 255, a)
                .vec4(x, y, w, h).vec4(rad, rad, rad, rad).vec4(quality, brightness, smoothness, 0.0f).next();
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    public static void blurChamferedRect(Renderer2D r, double x, double y, double w, double h,
                                         double chamfer, float quality, float brightness, float alpha, int ignoredTintRgb) {
        if (w <= 0.0 || h <= 0.0) return;
        if (alpha <= 0.001f) return;
        float cut = clampChamfer(chamfer, w, h);
        if (cut <= 0.0f) {
            blurRect(r, x, y, w, h, 0.0f, quality, brightness, alpha, ignoredTintRgb);
            return;
        }
        blurRect(r, x, y, w, h, -cut, quality, brightness, alpha, ignoredTintRgb);
    }

    public static void blurComposite(Renderer2D r, Consumer<Renderer2D.BlurComposite> consumer) {
        if (consumer == null) return;

        BlurSource source = source();
        if (source == null) return;

        DrawBatch batch = Batch2D.openBlur(r, UiBatchType.BLUR_COMPLEX, source.view, source.sampler,
                Renderer2D.DEFAULT_BLUR_QUALITY, Renderer2D.DEFAULT_KAWASE_OFFSET_PX);
        if (batch == null) return;
        batch.mesh.alpha = 1.0;
        try {
            consumer.accept(new Renderer2D.BlurComposite(batch.mesh, (float) r.getAlpha()));
        } finally {
            Batch2D.close();
        }
    }
}
