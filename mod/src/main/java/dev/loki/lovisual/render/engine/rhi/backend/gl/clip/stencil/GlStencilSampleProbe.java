package dev.loki.lovisual.render.engine.rhi.backend.gl.clip;

import dev.loki.lovisual.render.engine.msaa.MsaaTextureRegistry;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.opengl.GLCapabilities;

final class GlStencilSampleProbe {

    private final GlStencilGl gl;
    private boolean textureSamplesDsaProbeDisabled;

    GlStencilSampleProbe(GlStencilGl gl) {
        this.gl = gl;
    }

    int detectBoundFramebufferSamples() {
        int colorSamples = attachmentSamples(GL30C.GL_COLOR_ATTACHMENT0);
        int depthSamples = attachmentSamples(GL30C.GL_DEPTH_ATTACHMENT);
        int stencilSamples = attachmentSamples(GL30C.GL_STENCIL_ATTACHMENT);
        int max = Math.max(colorSamples, Math.max(depthSamples, stencilSamples));
        return Math.max(1, max);
    }

    int attachmentSamples(int attachmentPoint) {
        int type = GL30C.glGetFramebufferAttachmentParameteri(
                GL30C.GL_FRAMEBUFFER,
                attachmentPoint,
                GL30C.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE
        );
        if (type == GL11C.GL_NONE) {
            return 1;
        }
        if (type == GL30C.GL_RENDERBUFFER) {
            int renderbuffer = GL30C.glGetFramebufferAttachmentParameteri(
                    GL30C.GL_FRAMEBUFFER,
                    attachmentPoint,
                    GL30C.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME
            );
            return renderbufferSamples(renderbuffer);
        }
        if (type == GL11C.GL_TEXTURE) {
            int texture = GL30C.glGetFramebufferAttachmentParameteri(
                    GL30C.GL_FRAMEBUFFER,
                    attachmentPoint,
                    GL30C.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME
            );
            return textureSamples(texture);
        }
        return 1;
    }

    int textureSamples(int texture) {
        if (texture == 0) {
            return 1;
        }
        int registeredSamples = MsaaTextureRegistry.getSamples(texture);
        if (registeredSamples > 1) {
            return registeredSamples;
        }
        if (textureSamplesDsaProbeDisabled) {
            return 1;
        }
        GLCapabilities capabilities = GL.getCapabilities();
        if (capabilities == null || (!capabilities.OpenGL45 && !capabilities.GL_ARB_direct_state_access)) {
            return 1;
        }
        try {
            gl.clearGlErrorsSilently();
            int samples;
            if (capabilities.OpenGL45) {
                samples = GL45C.glGetTextureLevelParameteri(texture, 0, GL32C.GL_TEXTURE_SAMPLES);
            } else {
                samples = ARBDirectStateAccess.glGetTextureLevelParameteri(texture, 0, GL32C.GL_TEXTURE_SAMPLES);
            }
            int error = GL11C.glGetError();
            if (error == GL11C.GL_NO_ERROR) {
                return Math.max(1, samples);
            }
            textureSamplesDsaProbeDisabled = true;
        } catch (Throwable ignored) {
            textureSamplesDsaProbeDisabled = true;
        }
        gl.clearGlErrorsSilently();
        return 1;
    }

    int renderbufferSamples(int renderbuffer) {
        if (renderbuffer == 0) {
            return 1;
        }
        int previousRenderbuffer = GL11C.glGetInteger(GL30C.GL_RENDERBUFFER_BINDING);
        try {
            GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, renderbuffer);
            int samples = GL30C.glGetRenderbufferParameteri(GL30C.GL_RENDERBUFFER, GL30C.GL_RENDERBUFFER_SAMPLES);
            return Math.max(1, samples);
        } catch (Throwable ignored) {
            gl.clearGlErrorsSilently();
            return 1;
        } finally {
            GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, previousRenderbuffer);
        }
    }
}
