package dev.loki.lovisual.render.engine.rhi.backend.gl.clip;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.jetbrains.annotations.Nullable;
import dev.loki.lovisual.render.engine.rhi.backend.gl.clip.GlStencilNativeState.Mode;
import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipRenderPassContract;
import dev.loki.lovisual.util.logging.DebugLog;

/**
 * Render-pass and pipeline attachment bookkeeping for
 * {@link GlStencilShapeClipBackend}: tracks the currently-bound pass target,
 * prepares its stencil framebuffer, applies pipeline contracts and drains the
 * requested clears. Operates on the backend's package-private state; the backend
 * keeps the clip-stack mode/reference decisions and the native GL emission.
 * Package-private helper of {@link GlStencilShapeClipBackend} (impl subfolder,
 * {@code gl.clip} package).
 */
final class GlStencilPassLifecycle {

    private GlStencilPassLifecycle() {
    }

    static void beginRenderPass(GlStencilShapeClipBackend m, String label,
            @Nullable GpuTextureView colorView, @Nullable GpuTextureView depthView) {
        m.currentPassLabel = label == null ? "<unnamed-pass>" : label;
        m.currentColorView = colorView;
        m.currentDepthView = depthView;
        m.currentPassPrepared = false;

        if (m.offscreenPassBypassDepth > 0) {
            m.nativeState.resetNativeState();
            if (DebugLog.isStencilDebugEnabled()) {
                m.checkGlErrors("beginRenderPass(offscreen-bypass)");
            }
            return;
        }

        boolean shouldPrepare = m.renderPassAttachmentRequired || m.mode != Mode.DISABLED || m.clearRequested;
        if (!shouldPrepare) {
            // Hot path (every vanilla/Sodium pass with clip idle): glGetError syncs with the
            // driver — skip it unless explicitly debugging stencil.
            return;
        }

        if (!prepareCurrentPassTarget(m, "beginRenderPass")) {
            DebugLog.errorOnChange("shapeclip.renderpass.prepare.failed", m.currentPassLabel + "|" + m.attachmentReason + "|" + m.lastFailureReason(), "[ShapeClip/GL] render pass stencil prepare failed: pass=%s reason=%s lastFailure=%s", m.currentPassLabel, m.attachmentReason, m.lastFailureReason());
            m.checkGlErrors("beginRenderPass(prepare-failed)");
            return;
        }

        if (m.clearRequested) {
            boolean cleared = m.framebuffers.clear(m.currentColorView, m.currentDepthView);
            if (!cleared) {
                m.lastFailure = "clear failed: " + m.framebuffers.lastFailure();
                DebugLog.errorOnChange("shapeclip.renderpass.clear.failed", m.currentPassLabel + "|" + m.clearReason + "|" + m.lastFailure, "[ShapeClip/GL] render pass stencil clear failed: pass=%s reason=%s lastFailure=%s", m.currentPassLabel, m.clearReason, m.lastFailure);
            }
            m.clearRequested = false;
            m.clearReason = "none";
        }

        m.checkGlErrors("beginRenderPass");
    }

    static void endRenderPass(GlStencilShapeClipBackend m) {
        // Capture before endRenderPass resets renderPassAttachmentRequired below.
        boolean errorsOfInterest = m.mode != Mode.DISABLED || m.renderPassAttachmentRequired
                || DebugLog.isStencilDebugEnabled();
        m.nativeState.resetNativeState();
        m.currentColorView = null;
        m.currentDepthView = null;
        m.currentPassLabel = "<no-pass>";
        m.currentPassPrepared = false;
        m.currentPipelineContract = ShapeClipRenderPassContract.NONE;
        m.currentPipelineName = "<no-pipeline>";
        if (m.offscreenPassBypassDepth == 0) {
            m.renderPassAttachmentRequired = false;
            m.attachmentReason = "none";
        }
        if (errorsOfInterest) {
            m.checkGlErrors(m.offscreenPassBypassDepth > 0 ? "endRenderPass(offscreen-bypass)" : "endRenderPass");
        }
    }

    static void bindPipeline(GlStencilShapeClipBackend m, RenderPipeline pipeline, ShapeClipRenderPassContract contract) {
        m.currentPipelineContract = contract == null ? ShapeClipRenderPassContract.NONE : contract;
        m.currentPipelineName = pipeline == null || pipeline.getLocation() == null ? "<unknown>" : pipeline.getLocation().toString();
        if (m.offscreenPassBypassDepth > 0) {
            return;
        }
        if (m.mode != Mode.DISABLED && m.currentPipelineContract == ShapeClipRenderPassContract.NONE) {
            DebugLog.warnOnce(
                    "shapeclip.stale.pipeline.contract." + m.currentPipelineName,
                    "[ShapeClip/GL] stale clip state risk: active mode=%s ref=%d but pipeline has no shape-clip contract. pipeline=%s pass=%s",
                    m.mode,
                    m.reference,
                    m.currentPipelineName,
                    m.currentPassLabel
            );
        }
        if (m.currentPipelineContract.requiresAttachment(m.mode != Mode.DISABLED || m.renderPassAttachmentRequired)) {
            m.requireRenderPassAttachment("pipeline contract " + m.currentPipelineContract + " for " + m.currentPipelineName);
            if (m.currentColorView != null && !m.currentPassPrepared) {
                prepareCurrentPassTarget(m, "bindPipeline");
            }
        }
    }

    static boolean prepareCurrentPassTarget(GlStencilShapeClipBackend m, String source) {
        if (m.currentColorView == null) {
            m.lastFailure = "no current render pass target while " + source + " requested stencil state";
            DebugLog.warnOnChange("shapeclip.no.current.pass", m.lastFailure, "[ShapeClip/GL] stale state: %s", m.lastFailure);
            return false;
        }
        if (m.currentPassPrepared && m.stencilAvailable) return true;
        boolean ok = m.framebuffers.ensure(m.currentColorView, m.currentDepthView);
        m.currentPassPrepared = ok;
        m.stencilAvailable = ok;
        if (!ok) {
            m.lastFailure = m.framebuffers.lastFailure();
            DebugLog.errorOnChange("shapeclip.prepare.current.target.failed", source + "|" + m.currentPassLabel + "|" + GlStencilNativeState.describeView(m.currentColorView) + "|" + GlStencilNativeState.describeView(m.currentDepthView) + "|" + m.lastFailure, "[ShapeClip/GL] prepareCurrentPassTarget failed from=%s pass=%s targetColor=%s targetDepth=%s failure=%s",
                    source, m.currentPassLabel, GlStencilNativeState.describeView(m.currentColorView), GlStencilNativeState.describeView(m.currentDepthView), m.lastFailure);
            return false;
        }
        m.lastFailure = "none";
        return true;
    }
}
