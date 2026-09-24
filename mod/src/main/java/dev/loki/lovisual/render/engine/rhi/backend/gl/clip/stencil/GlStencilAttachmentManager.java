package dev.loki.lovisual.render.engine.rhi.backend.gl.clip;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlConst;
import dev.loki.lovisual.util.logging.DebugLog;
import org.lwjgl.opengl.GL30C;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class GlStencilAttachmentManager {
    private final Map<Integer, GlStencilAttachment> attachments;
    private final GlStencilGl gl;
    private final GlStencilSampleProbe probe;
    private boolean warnedUnsupported;
    private String lastFailure;
    GlStencilAttachmentManager(GlStencilGl gl, GlStencilSampleProbe probe) {
        this.attachments = new HashMap<>();
        this.gl = gl;
        this.probe = probe;
        this.lastFailure = "none";
    }
    boolean hasWarnedUnsupported() {
        return warnedUnsupported;
    }
    String lastFailure() {
        return lastFailure;
    }
    String describe() {
        return "attachments=" + attachments.size()
                + ", warnedUnsupported=" + warnedUnsupported
                + ", lastFailure='" + lastFailure + "'";
    }
    Map<Integer, GlStencilAttachment> attachments() {
        return attachments;
    }

    boolean ensureAttachment(int framebuffer, int width, int height) {
        GlStencilAttachment attachment = attachments.get(framebuffer);
        int prevRead = GlStateManager.getFrameBuffer(GlConst.GL_READ_FRAMEBUFFER);
        int prevDraw = GlStateManager.getFrameBuffer(GlConst.GL_DRAW_FRAMEBUFFER);
        try {
            GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, framebuffer);
            int samples = probe.detectBoundFramebufferSamples();
            if (attachment == null || attachment.width != width || attachment.height != height
                    || attachment.samples != samples || attachment.deleted) {
                if (attachment != null) {
                    attachment.restoreOriginalAttachments();
                    attachment.delete();
                }
                attachment = new GlStencilAttachment(width, height, samples, gl);
                attachments.put(framebuffer, attachment);
            }
            if (attachment.attached) {
                int status = GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER);
                if (status == GL30C.GL_FRAMEBUFFER_COMPLETE) {
                    lastFailure = "none";
                    gl.checkGlErrors("ensureAttachment(existing)");
                    return true;
                }
                DebugLog.warnOnChange("shapeclip.attachment.stale",
                        framebuffer + "|" + status + "|" + attachment.mode,
                        "[ShapeClip/GL] existing stencil attachment stale/incomplete. fbo=%d status=0x%s mode=%s",
                        framebuffer, Integer.toHexString(status), attachment.mode);
                attachment.restoreOriginalAttachments();
            }
            attachment.captureOriginalAttachments();
            if (!attachment.separateStencilRejected && tryAttachSeparateStencil(framebuffer, attachment)) {
                lastFailure = "none";
                return true;
            }
            if (tryAttachPackedDepthStencil(framebuffer, attachment)) {
                lastFailure = "none";
                return true;
            }
            attachment.restoreOriginalAttachments();
            markUnavailable();
            return false;
        } finally {
            gl.restoreFramebufferBindings(prevRead, prevDraw);
        }
    }

    void invalidateAttachment(int framebuffer, String reason) {
        GlStencilAttachment attachment = attachments.remove(framebuffer);
        if (attachment == null) {
            lastFailure = reason;
            return;
        }
        try {
            attachment.restoreOriginalAttachments();
        } catch (Throwable t) {
            DebugLog.warnOnChange("shapeclip.attachment.restore.invalidate",
                    framebuffer + "|" + t.getClass().getSimpleName(),
                    "[ShapeClip/GL] failed to restore attachment while invalidating fbo=%d reason=%s error=%s: %s",
                    framebuffer, reason, t.getClass().getSimpleName(), t.getMessage());
        } finally {
            try {
                attachment.delete();
            } catch (Throwable t) {
                DebugLog.warnOnChange("shapeclip.attachment.delete.invalidate",
                        framebuffer + "|" + t.getClass().getSimpleName(),
                        "[ShapeClip/GL] failed to delete invalidated attachment fbo=%d error=%s: %s",
                        framebuffer, t.getClass().getSimpleName(), t.getMessage());
            }
            lastFailure = reason;
            DebugLog.stencilOnChange("shapeclip.attachment.invalidated", framebuffer + "|" + reason,
                    "[ShapeClip/GL] invalidated cached stencil attachment. fbo=%d reason=%s", framebuffer, reason);
        }
    }

    boolean tryAttachSeparateStencil(int framebuffer, GlStencilAttachment attachment) {
        if (!attachment.separateStencilAllocationOk) {
            attachment.separateStencilRejected = true;
            lastFailure = "separate GL_STENCIL_INDEX8 allocation failed: fbo=" + framebuffer
                    + ", size=" + attachment.width + "x" + attachment.height + ", samples=" + attachment.samples;
            DebugLog.stencilOnce("shapeclip.separate.alloc.failed." + attachment.samples,
                    "[ShapeClip/GL] separate stencil allocation failed; packed depth-stencil will be tried. %s", lastFailure);
            return false;
        }
        GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_STENCIL_ATTACHMENT,
                GL30C.GL_RENDERBUFFER, attachment.stencilRenderbuffer);
        int status = GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER);
        if (status == GL30C.GL_FRAMEBUFFER_COMPLETE) {
            attachment.attached = true;
            attachment.mode = GlStencilAttachment.AttachmentMode.SEPARATE_STENCIL;
            DebugLog.stencilOnce("shapeclip.separate.stencil." + framebuffer,
                    "[ShapeClip/GL] using separate GL_STENCIL_INDEX8. fbo=%d size=%dx%d samples=%d",
                    framebuffer, attachment.width, attachment.height, attachment.samples);
            gl.checkGlErrors("tryAttachSeparateStencil(success)");
            return true;
        }
        GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_STENCIL_ATTACHMENT, GL30C.GL_RENDERBUFFER, 0);
        attachment.restoreOriginalAttachments();
        lastFailure = "separate GL_STENCIL_INDEX8 incomplete: status=0x" + Integer.toHexString(status)
                + ", fbo=" + framebuffer + ", size=" + attachment.width + "x" + attachment.height + ", samples=" + attachment.samples;
        attachment.separateStencilRejected = true;
        DebugLog.stencilOnce("shapeclip.separate.rejected",
                "[ShapeClip/GL] separate stencil attach failed, using packed depth-stencil path from now on. %s", lastFailure);
        return false;
    }

    boolean tryAttachPackedDepthStencil(int framebuffer, GlStencilAttachment attachment) {
        if (!attachment.packedDepthStencilAllocationOk) {
            lastFailure = "packed GL_DEPTH24_STENCIL8 allocation failed: fbo=" + framebuffer
                    + ", size=" + attachment.width + "x" + attachment.height + ", samples=" + attachment.samples;
            DebugLog.warnOnChange("shapeclip.packed.alloc.failed", framebuffer + "|" + attachment.samples,
                    "[ShapeClip/GL] packed depth-stencil allocation failed. %s", lastFailure);
            return false;
        }
        GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT,
                GL30C.GL_RENDERBUFFER, attachment.depthStencilRenderbuffer);
        GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_STENCIL_ATTACHMENT,
                GL30C.GL_RENDERBUFFER, attachment.depthStencilRenderbuffer);
        int status = GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER);
        if (status == GL30C.GL_FRAMEBUFFER_COMPLETE) {
            attachment.attached = true;
            attachment.mode = GlStencilAttachment.AttachmentMode.PACKED_DEPTH_STENCIL;
            if (!attachment.loggedPackedMode) {
                attachment.loggedPackedMode = true;
                DebugLog.stencilOnce("shapeclip.packed.depth.stencil." + framebuffer,
                        "[ShapeClip/GL] using packed depth-stencil for shape clip. fbo=%d size=%dx%d samples=%d",
                        framebuffer, attachment.width, attachment.height, attachment.samples);
            }
            gl.checkGlErrors("tryAttachPackedDepthStencil(success)");
            return true;
        }
        lastFailure = "packed GL_DEPTH24_STENCIL8 incomplete: status=0x" + Integer.toHexString(status)
                + ", fbo=" + framebuffer + ", size=" + attachment.width + "x" + attachment.height + ", samples=" + attachment.samples;
        return false;
    }

    boolean fail(String reason) {
        lastFailure = reason;
        warnedUnsupported = true;
        return false;
    }
    void markUnavailable() {
        warnedUnsupported = true;
    }
    void cleanupClosedOrResized() {
        Iterator<Map.Entry<Integer, GlStencilAttachment>> it = attachments.entrySet().iterator();
        while (it.hasNext()) {
            GlStencilAttachment a = it.next().getValue();
            if (a.deleted) {
                it.remove();
            }
        }
    }
}
