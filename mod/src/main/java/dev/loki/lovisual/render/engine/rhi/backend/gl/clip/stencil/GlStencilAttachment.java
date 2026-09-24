package dev.loki.lovisual.render.engine.rhi.backend.gl.clip;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL32C;

final class GlStencilAttachment {

    final int stencilRenderbuffer;
    final int depthStencilRenderbuffer;
    final int width;
    final int height;
    final int samples;
    final boolean separateStencilAllocationOk;
    final boolean packedDepthStencilAllocationOk;
    boolean deleted;
    boolean attached;
    boolean separateStencilRejected;
    boolean loggedPackedMode;
    AttachmentMode mode = AttachmentMode.NONE;
    SavedAttachment originalDepth = createNoneAttachment();
    SavedAttachment originalStencil = createNoneAttachment();
    private final GlStencilGl gl;

    GlStencilAttachment(int width, int height, int samples, GlStencilGl gl) {
        this.width = width;
        this.height = height;
        this.samples = Math.max(1, samples);
        this.gl = gl;
        int previousRenderbuffer = GL11C.glGetInteger(GL30C.GL_RENDERBUFFER_BINDING);
        int stencilRb = 0;
        int depthStencilRb = 0;
        boolean separateOk;
        boolean packedOk;
        try {
            stencilRb = GL30C.glGenRenderbuffers();
            GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, stencilRb);
            separateOk = allocateRenderbuffer(GL30C.GL_STENCIL_INDEX8, width, height, this.samples);
            depthStencilRb = GL30C.glGenRenderbuffers();
            GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, depthStencilRb);
            packedOk = allocateRenderbuffer(GL30C.GL_DEPTH24_STENCIL8, width, height, this.samples);
        } finally {
            GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, previousRenderbuffer);
        }
        this.stencilRenderbuffer = stencilRb;
        this.depthStencilRenderbuffer = depthStencilRb;
        this.separateStencilAllocationOk = separateOk;
        this.packedDepthStencilAllocationOk = packedOk;
    }

    boolean allocateRenderbuffer(int internalFormat, int width, int height, int samples) {
        gl.clearGlErrorsSilently();
        if (samples > 1) {
            GL32C.glRenderbufferStorageMultisample(GL30C.GL_RENDERBUFFER, samples, internalFormat, width, height);
        } else {
            GL30C.glRenderbufferStorage(GL30C.GL_RENDERBUFFER, internalFormat, width, height);
        }
        int error = GL11C.glGetError();
        if (error != GL11C.GL_NO_ERROR) {
            return false;
        }
        int storedWidth = GL30C.glGetRenderbufferParameteri(GL30C.GL_RENDERBUFFER, GL30C.GL_RENDERBUFFER_WIDTH);
        int storedHeight = GL30C.glGetRenderbufferParameteri(GL30C.GL_RENDERBUFFER, GL30C.GL_RENDERBUFFER_HEIGHT);
        int storedSamples = GL30C.glGetRenderbufferParameteri(GL30C.GL_RENDERBUFFER, GL30C.GL_RENDERBUFFER_SAMPLES);
        return storedWidth == width && storedHeight == height
                && Math.max(1, storedSamples) == Math.max(1, samples);
    }

    void captureOriginalAttachments() {
        if (attached) {
            return;
        }
        originalDepth = captureAttachment(GL30C.GL_DEPTH_ATTACHMENT);
        originalStencil = captureAttachment(GL30C.GL_STENCIL_ATTACHMENT);
    }

    SavedAttachment createNoneAttachment() {
        return new SavedAttachment(GL11C.GL_NONE, 0, 0, 0, 0, false);
    }

    SavedAttachment captureAttachment(int attachmentPoint) {
        int type = GL30C.glGetFramebufferAttachmentParameteri(
                GL30C.GL_FRAMEBUFFER,
                attachmentPoint,
                GL30C.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE
        );
        if (type == GL11C.GL_NONE) {
            return createNoneAttachment();
        }
        int name = GL30C.glGetFramebufferAttachmentParameteri(
                GL30C.GL_FRAMEBUFFER,
                attachmentPoint,
                GL30C.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME
        );
        int level = 0;
        int cubeMapFace = 0;
        int layer = 0;
        boolean layered = false;
        if (type == GL11C.GL_TEXTURE) {
            level = GL30C.glGetFramebufferAttachmentParameteri(
                    GL30C.GL_FRAMEBUFFER,
                    attachmentPoint,
                    GL30C.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL
            );
            cubeMapFace = GL30C.glGetFramebufferAttachmentParameteri(
                    GL30C.GL_FRAMEBUFFER,
                    attachmentPoint,
                    GL30C.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE
            );
            try {
                layer = GL30C.glGetFramebufferAttachmentParameteri(
                        GL30C.GL_FRAMEBUFFER,
                        attachmentPoint,
                        GL30C.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LAYER
                );
                int layeredInt = GL30C.glGetFramebufferAttachmentParameteri(
                        GL30C.GL_FRAMEBUFFER,
                        attachmentPoint,
                        0x8DA7
                );
                layered = layeredInt != 0;
            } catch (Throwable ignored) {
                gl.clearGlErrorsSilently();
            }
        }
        return new SavedAttachment(type, name, level, cubeMapFace, layer, layered);
    }

    void restoreOriginalAttachments() {
        originalDepth.restore(GL30C.GL_DEPTH_ATTACHMENT);
        originalStencil.restore(GL30C.GL_STENCIL_ATTACHMENT);
        attached = false;
        mode = AttachmentMode.NONE;
    }

    void delete() {
        if (deleted) {
            return;
        }
        restoreOriginalAttachments();
        deleted = true;
        GL30C.glDeleteRenderbuffers(stencilRenderbuffer);
        GL30C.glDeleteRenderbuffers(depthStencilRenderbuffer);
    }

    enum AttachmentMode {
        NONE,
        SEPARATE_STENCIL,
        PACKED_DEPTH_STENCIL
    }

    record SavedAttachment(int type, int name, int level, int cubeMapFace, int layer, boolean layered) {

        void restore(int attachmentPoint) {
            if (type == GL11C.GL_TEXTURE) {
                if (cubeMapFace != 0) {
                    GL30C.glFramebufferTexture2D(
                            GL30C.GL_FRAMEBUFFER,
                            attachmentPoint,
                            cubeMapFace,
                            name,
                            level
                    );
                } else {
                    GL32C.glFramebufferTexture(
                            GL30C.GL_FRAMEBUFFER,
                            attachmentPoint,
                            name,
                            level
                    );
                }
                return;
            }
            if (type == GL30C.GL_RENDERBUFFER) {
                GL30C.glFramebufferRenderbuffer(
                        GL30C.GL_FRAMEBUFFER,
                        attachmentPoint,
                        GL30C.GL_RENDERBUFFER,
                        name
                );
                return;
            }
            GL30C.glFramebufferRenderbuffer(
                    GL30C.GL_FRAMEBUFFER,
                    attachmentPoint,
                    GL30C.GL_RENDERBUFFER,
                    0
            );
        }
    }
}
