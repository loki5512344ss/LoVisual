package dev.loki.lovisual.render.engine.rhi.backend.gl.clip;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.jetbrains.annotations.Nullable;
import dev.loki.lovisual.render.engine.rhi.backend.gl.clip.GlStencilNativeState.Mode;
import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipBackend;
import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipRenderPassContract;
import dev.loki.lovisual.util.logging.DebugLog;

/**
 * GL implementation of shape clipping using a render-pass-bound stencil attachment.
 *
 * <p>This class owns the clip-stack state machine and render-pass lifecycle; the
 * low-level GL stencil emission, ambient depth/cull save-restore and GL error
 * scanning live in {@link GlStencilNativeState}.</p>
 */
public final class GlStencilShapeClipBackend implements ShapeClipBackend {
    final GlStencilFramebufferSupport framebuffers = new GlStencilFramebufferSupport();
    final GlStencilNativeState nativeState = new GlStencilNativeState();
    Mode mode = Mode.DISABLED;
    int reference;
    private int compareReference;
    boolean stencilAvailable;
    boolean renderPassAttachmentRequired;
    boolean clearRequested;
    String attachmentReason = "none";
    String clearReason = "none";
    @Nullable GpuTextureView currentColorView;
    @Nullable GpuTextureView currentDepthView;
    String currentPassLabel = "<no-pass>";
    boolean currentPassPrepared;
    ShapeClipRenderPassContract currentPipelineContract = ShapeClipRenderPassContract.NONE;
    String currentPipelineName = "<no-pipeline>";
    String lastFailure = "none";
    int offscreenPassBypassDepth;

    private int clampRef(int value) {
        return GlStencilNativeState.clampRef(value);
    }

    @Override
    public boolean supported() {
        return framebuffers.supported();
    }

    @Override
    public boolean isActive() {
        return mode != Mode.DISABLED;
    }

    @Override
    public void requireRenderPassAttachment(String reason) {
        renderPassAttachmentRequired = true;
        attachmentReason = reason == null ? "unspecified" : reason;
    }

    @Override
    public void requestClear(String reason) {
        clearRequested = true;
        clearReason = reason == null ? "unspecified" : reason;
    }

    @Override
    public void beginRenderPass(String label, @Nullable GpuTextureView colorView, @Nullable GpuTextureView depthView) {
        GlStencilPassLifecycle.beginRenderPass(this, label, colorView, depthView);
    }

    @Override
    public void endRenderPass() {
        GlStencilPassLifecycle.endRenderPass(this);
    }

    @Override
    public void bindPipeline(RenderPipeline pipeline, ShapeClipRenderPassContract contract) {
        GlStencilPassLifecycle.bindPipeline(this, pipeline, contract);
    }

    @Override
    public boolean prepareMainTarget() {
        boolean ok = framebuffers.ensureForMainFramebuffer();
        setStencilAvailable(ok);
        if (!ok) {
            lastFailure = framebuffers.lastFailure();
        }
        return ok;
    }

    @Override
    public boolean prepare(@Nullable GpuTextureView colorView, @Nullable GpuTextureView depthView) {
        boolean ok = framebuffers.ensure(colorView, depthView);
        setStencilAvailable(ok);
        if (!ok) {
            lastFailure = framebuffers.lastFailure();
        }
        return ok;
    }

    @Override
    public boolean clearMainTarget() {
        return framebuffers.clearMainFramebufferStencil();
    }

    void setStencilAvailable(boolean available) {
        stencilAvailable = available;
        if (!available) {
            mode = Mode.DISABLED;
            reference = 0;
            compareReference = 0;
        }
    }

    @Override
    public void beginWrite(int parentReference, int newReference) {
        mode = Mode.WRITE;
        compareReference = clampRef(parentReference);
        reference = clampRef(newReference);
        requireRenderPassAttachment("mask write parent=" + compareReference + " ref=" + reference);
    }

    @Override
    public void beginRestore(int currentReference, int parentReference) {
        mode = Mode.RESTORE;
        compareReference = clampRef(currentReference);
        reference = clampRef(parentReference);
        requireRenderPassAttachment("mask restore current=" + compareReference + " parent=" + reference);
    }

    @Override
    public void beginTest(int activeReference) {
        mode = Mode.TEST;
        reference = clampRef(activeReference);
        compareReference = reference;
        requireRenderPassAttachment("content test ref=" + reference);
    }

    @Override
    public void pushOffscreenPassBypass() {
        offscreenPassBypassDepth++;
        if (offscreenPassBypassDepth == 1) {
            nativeState.resetNativeState();
        }
    }

    @Override
    public void popOffscreenPassBypass() {
        if (offscreenPassBypassDepth <= 0) return;
        offscreenPassBypassDepth--;
    }

    @Override
    public void disable() {
        nativeState.resetNativeState();
        mode = Mode.DISABLED;
        reference = 0;
        compareReference = 0;
        stencilAvailable = false;
        renderPassAttachmentRequired = false;
        clearRequested = false;
        attachmentReason = "none";
        clearReason = "none";
        offscreenPassBypassDepth = 0;
        framebuffers.restoreActiveAttachments();
        checkGlErrors("disable");
    }

    @Override
    public void applyNativeState() {
        if (offscreenPassBypassDepth > 0) {
            nativeState.resetNativeState();
            if (DebugLog.isStencilDebugEnabled()) {
                checkGlErrors("applyNativeState(offscreen-bypass)");
            }
            return;
        }
        if (mode == Mode.DISABLED) {
            nativeState.resetNativeState();
            if (DebugLog.isStencilDebugEnabled()) {
                checkGlErrors("applyNativeState(disabled)");
            }
            return;
        }

        if (!prepareCurrentPassTarget("applyNativeState")) {
            nativeState.resetNativeState();
            DebugLog.errorOnChange(
                    "shapeclip.apply.prepare.failed",
                    currentPassLabel + "|" + currentPipelineName + "|" + mode + "|" + reference + "|" + lastFailureReason(),
                    "[ShapeClip/GL] cannot apply stencil state: pass=%s pipeline=%s mode=%s ref=%d reason=%s lastFailure=%s",
                    currentPassLabel,
                    currentPipelineName,
                    mode,
                    reference,
                    attachmentReason,
                    lastFailureReason()
            );
            checkGlErrors("applyNativeState(prepare-failed)");
            return;
        }

        nativeState.apply(mode, reference, compareReference);

        checkGlErrors("applyNativeState(" + mode + ")");
    }

    boolean prepareCurrentPassTarget(String source) {
        return GlStencilPassLifecycle.prepareCurrentPassTarget(this, source);
    }

    @Override
    public String debugState() {
        return toString();
    }

    @Override
    public String lastFailureReason() {
        if (!"none".equals(lastFailure)) return lastFailure;
        return framebuffers.lastFailure();
    }

    @Override
    public String toString() {
        return "GlStencilShapeClipBackend{"
                + "mode=" + mode
                + ", ref=" + reference
                + ", compareRef=" + compareReference
                + ", stencilAvailable=" + stencilAvailable
                + ", passPrepared=" + currentPassPrepared
                + ", pass='" + currentPassLabel + '\''
                + ", pipeline='" + currentPipelineName + '\''
                + ", contract=" + currentPipelineContract
                + ", attachmentRequired=" + renderPassAttachmentRequired
                + ", clearRequested=" + clearRequested
                + ", offscreenBypassDepth=" + offscreenPassBypassDepth
                + ", supported=" + supported()
                + ", framebuffer=" + framebuffers.describe()
                + '}';
    }

    public String lastFailure() {
        return lastFailureReason();
    }

    void checkGlErrors(String where) {
        GlStencilNativeState.checkGlErrors(where, this::toString);
    }
}
