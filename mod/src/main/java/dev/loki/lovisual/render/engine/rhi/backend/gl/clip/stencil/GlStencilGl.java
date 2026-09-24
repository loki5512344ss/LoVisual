package dev.loki.lovisual.render.engine.rhi.backend.gl.clip;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.mixininterface.device.IGlBackendInfo;
import dev.loki.lovisual.util.logging.DebugLog;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;

import java.util.List;
import com.mojang.blaze3d.opengl.FrameBufferAttachment;

final class GlStencilGl {

    void restoreFramebufferBindings(int readFramebuffer, int drawFramebuffer) {
        if (readFramebuffer == drawFramebuffer) {
            GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, readFramebuffer);
            return;
        }
        GlStateManager._glBindFramebuffer(GlConst.GL_READ_FRAMEBUFFER, readFramebuffer);
        GlStateManager._glBindFramebuffer(GlConst.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
    }

    int framebufferFor(IGlBackendInfo backend, GlTextureView colorView, @Nullable GpuTextureView depthView) {
        FrameBufferAttachment depthAttachment = null;
        if (depthView != null) {
            if (!(depthView instanceof GlTextureView glDepthView)) {
                throw new IllegalArgumentException("depth view is not GlTextureView: " + depthView.getClass().getName());
            }
            depthAttachment = glDepthView;
        }
        return backend.lovisual$frameBufferCache().getFbo(
                backend.lovisual$directStateAccess(),
                List.of(colorView),
                depthAttachment
        );
    }

    void clearGlErrorsSilently() {
        int guard = 0;
        while (GL11C.glGetError() != GL11C.GL_NO_ERROR && guard++ < 16) {
            // drain
        }
    }

    void checkGlErrors(String where) {
        int guard = 0;
        int error;
        while ((error = GL11C.glGetError()) != GL11C.GL_NO_ERROR && guard++ < 16) {
            DebugLog.errorOnChange("shapeclip.framebuffer.gl.error", where + "|" + error,
                    "[ShapeClip/GL] gl error at %s: 0x%s", where, Integer.toHexString(error));
        }
    }
}
