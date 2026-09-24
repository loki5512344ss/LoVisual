package dev.loki.lovisual.render.engine.renderer.ui.blur.kawase;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.MeshRenderer;
import dev.loki.lovisual.render.engine.renderer.ui.blur.UiBlurResources;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.engine.uniform.impl.ui.UIBlurUniforms;
import net.minecraft.client.renderer.Projection;
import org.joml.Matrix4f;

/**
 * Один проход Kawase-блюра (down/up) в offscreen-таргет.
 */
final class KawasePass {
    private KawasePass() {
    }

    static boolean draw(TextureTarget target,
                        GpuTextureView sourceView,
                        GpuSampler sourceSampler,
                        int sourceW,
                        int sourceH,
                        float passOffset,
                        boolean upPass) {
        if (target == null || sourceView == null || sourceSampler == null) return false;
        MeshBuilder compositeMesh = UiBlurResources.ensureCompositeMesh(target.width, target.height);
        if (compositeMesh == null) return false;

        Matrix4f previousProjection = MeshRenderer.projection();
        try {
            MeshRenderer.setProjection(orthoProjection(target.width, target.height));
            UIBlurUniforms.update(sourceW, sourceH, target.width, target.height,
                    0f, 0f, passOffset, upPass ? 1.0f : 0.0f, 1.0f, 1.0f, 0xFFFFFF);
            MeshRenderer.begin()
                    .attachments(target)
                    .clearColor(0x00000000)
                    .pipeline(LoVisualRenderPipelines.UI_BLUR)
                    .mesh(compositeMesh)
                    .uniform("UIBlur", UIBlurUniforms.get())
                    .sampler("u_Texture", sourceView, sourceSampler)
                    .end();
            return target.getColorTextureView() != null;
        } finally {
            MeshRenderer.setProjection(previousProjection);
        }
    }

    private static Matrix4f orthoProjection(float width, float height) {
        Projection projection = new Projection();
        projection.setupOrtho(-1000.0f, 1000.0f, Math.max(1.0f, width), Math.max(1.0f, height), true);
        return projection.getMatrix(new Matrix4f());
    }
}
