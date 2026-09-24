package dev.loki.lovisual.render.engine.renderer.ui.dispatch;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.MeshRenderer;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;
import net.minecraft.client.Minecraft;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.OrderedUiBatcher;
import dev.loki.lovisual.render.engine.renderer.ui.deferred.DeferredTexturedSubmit;
import dev.loki.lovisual.render.engine.renderer.ui.deferred.sched.UiDeferredScheduler;

/** Immediate textured submission path kept outside the Renderer2D drawing facade. */
public final class UiDirectTexturedRenderer {
    private UiDirectTexturedRenderer() {
    }

    public static void submit(
            MeshBuilder mesh,
            String samplerName,
            GpuTextureView samplerView,
            GpuSampler sampler) {
        try {
            if (mesh == null || samplerView == null || sampler == null) return;
            if (mesh.isBuilding()) mesh.end();
            if (mesh.getIndicesCount() <= 0) return;

            if (UiDeferredScheduler.shouldDefer()) {
                MeshBuilder copy = copyMesh(mesh, LoVisualRenderPipelines.UI_TEXTURED);
                if (copy != null) {
                    UiDeferredScheduler.enqueue(new DeferredTexturedSubmit(
                            UiDeferredScheduler.layerForCurrentPhase(false),
                            UiDeferredScheduler.snapshotViewport(),
                            ScissorFunction.currentFramebufferScissor(),
                            samplerName,
                            samplerView,
                            sampler,
                            copy
                    ));
                }
                return;
            }

            UiRenderDispatcher.recordBackendCommand("TEXTURED_DIRECT");
            OrderedUiBatcher batcher = Renderer2D.UI_BATCHER;
            if (batcher.isActive()) {
                if (batcher.hasPendingWork() || UiRenderDispatcher.hasPendingCommands()) {
                    Renderer2D.BATCH_STATS.noteFlushReason(Renderer2D.FlushReason.TEXTURED_DIRECT);
                }
                batcher.flush(false);
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null) return;
            RenderTarget framebuffer = minecraft.gameRenderer.mainRenderTarget();
            if (framebuffer == null) return;

            MeshRenderer.begin()
                    .attachments(framebuffer.getColorTextureView(), null)
                    .pipeline(LoVisualRenderPipelines.UI_TEXTURED)
                    .mesh(mesh)
                    .sampler(samplerName, samplerView, sampler)
                    .end();
        } finally {
            UiRenderDispatcher.flushLayer();
        }
    }

    private static MeshBuilder copyMesh(MeshBuilder source, RenderPipeline pipeline) {
        if (source == null) return null;
        if (source.isBuilding()) source.end();
        if (source.getIndicesCount() <= 0 || source.getVertexCount() <= 0) return null;

        MeshBuilder copy = new MeshBuilder(pipeline);
        copy.beginScreen();
        copy.appendMesh(source);
        copy.end();
        return copy;
    }
}
