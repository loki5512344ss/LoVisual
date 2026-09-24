package dev.loki.lovisual.render.engine.rhi.backend.gl.clip;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.mixininterface.device.IGlBackendInfo;
import dev.loki.lovisual.render.engine.rhi.backend.gl.GlBackendAccess;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;

import java.util.Map;

public final class GlStencilFramebufferSupport {

    private final GlStencilGl gl;
    private final GlStencilSampleProbe probe;
    private final GlStencilAttachmentManager manager;

    public GlStencilFramebufferSupport() {
        this.gl = new GlStencilGl();
        this.probe = new GlStencilSampleProbe(this.gl);
        this.manager = new GlStencilAttachmentManager(this.gl, this.probe);
    }

    boolean supported() {
        return GlBackendAccess.current() != null;
    }

    String lastFailure() {
        return manager.lastFailure();
    }

    String describe() {
        return manager.describe();
    }

    boolean ensureForMainFramebuffer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return manager.fail("MinecraftClient is null");
        }
        RenderTarget fb = mc.gameRenderer.mainRenderTarget();
        if (fb == null) {
            return manager.fail("main framebuffer is null");
        }
        return ensure(fb.getColorTextureView(), null);
    }

    boolean clearMainFramebufferStencil() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return manager.fail("MinecraftClient is null");
        }
        RenderTarget fb = mc.gameRenderer.mainRenderTarget();
        if (fb == null) {
            return manager.fail("main framebuffer is null");
        }
        return clear(fb.getColorTextureView(), null);
    }

    boolean ensure(@Nullable GpuTextureView colorView, @Nullable GpuTextureView depthView) {
        IGlBackendInfo backend = GlBackendAccess.current();
        if (backend == null) {
            return manager.fail("RenderSystem device is not GlBackend");
        }
        if (colorView == null) {
            return manager.fail("color view is null");
        }
        if (!(colorView instanceof GlTextureView glColorView)) {
            return manager.fail("color view is not GlTextureView: " + colorView.getClass().getName());
        }
        if (colorView.isClosed()) {
            return manager.fail("color view is closed");
        }
        try {
            int framebuffer = gl.framebufferFor(backend, glColorView, depthView);
            int width = Math.max(1, colorView.getWidth(0));
            int height = Math.max(1, colorView.getHeight(0));
            return manager.ensureAttachment(framebuffer, width, height);
        } catch (Throwable t) {
            return manager.fail("exception while preparing framebuffer: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    boolean clear(@Nullable GpuTextureView colorView, @Nullable GpuTextureView depthView) {
        if (!ensure(colorView, depthView)) {
            return false;
        }
        IGlBackendInfo backend = GlBackendAccess.current();
        if (backend == null || !(colorView instanceof GlTextureView glColorView)) {
            return manager.fail("clear target is not backed by GlTextureView");
        }
        int prevRead = GlStateManager.getFrameBuffer(GlConst.GL_READ_FRAMEBUFFER);
        int prevDraw = GlStateManager.getFrameBuffer(GlConst.GL_DRAW_FRAMEBUFFER);
        boolean scissor = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST);
        int mask = GL11C.glGetInteger(GL11C.GL_STENCIL_WRITEMASK);
        try {
            int fb = gl.framebufferFor(backend, glColorView, depthView);
            GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, fb);
            int status = GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER);
            if (status != GL30C.GL_FRAMEBUFFER_COMPLETE) {
                manager.invalidateAttachment(fb, "clear preflight incomplete status=0x" + Integer.toHexString(status));
                if (!ensure(colorView, depthView)) {
                    return false;
                }
                fb = gl.framebufferFor(backend, glColorView, depthView);
                GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, fb);
                status = GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER);
                if (status != GL30C.GL_FRAMEBUFFER_COMPLETE) {
                    return manager.fail("clear target still incomplete after rebuild: status=0x" + Integer.toHexString(status) + ", fbo=" + fb);
                }
            }
            GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
            GL11C.glStencilMask(0xFF);
            GL11C.glClearStencil(0);
            GL11C.glClear(GL11C.GL_STENCIL_BUFFER_BIT);
            GL11C.glStencilMask(mask);
            gl.checkGlErrors("clearStencil");
            return true;
        } catch (Throwable t) {
            return manager.fail("exception while clearing stencil: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        } finally {
            if (scissor) {
                GL11C.glEnable(GL11C.GL_SCISSOR_TEST);
            } else {
                GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
            }
            gl.restoreFramebufferBindings(prevRead, prevDraw);
        }
    }

    void restoreActiveAttachments() {
        int prevRead = GlStateManager.getFrameBuffer(GlConst.GL_READ_FRAMEBUFFER);
        int prevDraw = GlStateManager.getFrameBuffer(GlConst.GL_DRAW_FRAMEBUFFER);
        try {
            for (Map.Entry<Integer, GlStencilAttachment> e : manager.attachments().entrySet()) {
                GlStencilAttachment a = e.getValue();
                if (!a.attached) {
                    continue;
                }
                GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, e.getKey());
                a.restoreOriginalAttachments();
            }
        } catch (Throwable t) {
            manager.fail("exception while restoring framebuffer attachments: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        } finally {
            gl.restoreFramebufferBindings(prevRead, prevDraw);
        }
    }

    void cleanupClosedOrResized() {
        manager.cleanupClosedOrResized();
    }

    boolean hasWarnedUnsupported() {
        return manager.hasWarnedUnsupported();
    }
}
