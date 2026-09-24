package dev.loki.lovisual.render.engine.rhi.backend.gl.clip;

import com.mojang.blaze3d.textures.GpuTextureView;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;
import dev.loki.lovisual.util.logging.DebugLog;

import java.util.function.Supplier;

/**
 * Low-level GL plumbing for {@link GlStencilShapeClipBackend}: emits the stencil
 * state for a given clip mode, saves/restores the ambient depth and cull state it
 * temporarily overrides, and scans for GL errors. Pure "how" — the backend keeps
 * the "when" (mode/reference decisions and render-pass lifecycle). Package-private
 * helper of {@link GlStencilShapeClipBackend} (impl subfolder, {@code gl.clip} package).
 */
final class GlStencilNativeState {

    /** Logical clip-stack phase driven by the backend state machine. */
    enum Mode {
        DISABLED,
        WRITE,
        RESTORE,
        TEST
    }

    private boolean depthStateCaptured;
    private boolean previousDepthTest;
    private boolean previousDepthMask;
    private boolean cullStateCaptured;
    private boolean previousCullFace;

    static int clampRef(int value) {
        if (value < 0) return 0;
        return Math.min(255, value);
    }

    static String describeView(@Nullable GpuTextureView view) {
        if (view == null) return "null";
        try {
            return view.getClass().getSimpleName()
                    + "["
                    + view.getWidth(0)
                    + "x"
                    + view.getHeight(0)
                    + ", closed="
                    + view.isClosed()
                    + "]";
        } catch (Throwable t) {
            return view.getClass().getSimpleName() + "[describe failed: " + t.getClass().getSimpleName() + "]";
        }
    }

    static void checkGlErrors(String where, Supplier<String> state) {
        int guard = 0;
        int error;
        while ((error = GL11C.glGetError()) != GL11C.GL_NO_ERROR && guard++ < 16) {
            DebugLog.errorOnChange("shapeclip.gl.error", where + "|" + error,
                    "[ShapeClip/GL] gl error at %s: 0x%s state=%s", where, Integer.toHexString(error), state.get());
        }
    }

    /**
     * Emit the native stencil state for the requested clip phase. Assumes the caller
     * has already prepared the pass target and handled the DISABLED/bypass short-circuits.
     */
    void apply(Mode mode, int reference, int compareReference) {
        captureDepthStateIfNeeded();
        GL11C.glDisable(GL11C.GL_DEPTH_TEST);
        GL11C.glDepthMask(false);
        GL11C.glEnable(GL11C.GL_STENCIL_TEST);

        switch (mode) {
            case WRITE -> {
                captureCullStateIfNeeded();
                GL11C.glDisable(GL11C.GL_CULL_FACE);
                GL11C.glStencilMask(0xFF);
                GL20C.glStencilMaskSeparate(GL11C.GL_FRONT_AND_BACK, 0xFF);
                int compareFunc = compareReference == 0 ? GL11C.GL_ALWAYS : GL11C.GL_EQUAL;
                GL11C.glStencilFunc(compareFunc, compareReference, 0xFF);
                GL20C.glStencilFuncSeparate(GL11C.GL_FRONT_AND_BACK, compareFunc, compareReference, 0xFF);
                // The stack model stores child = parent + 1. GL_REPLACE cannot express
                // "compare against parent but write child", because REPLACE writes the
                // same ref value supplied to glStencilFunc. Increment/decrement keeps the
                // compare ref independent from the value transition.
                GL11C.glStencilOp(GL11C.GL_KEEP, GL11C.GL_INCR, GL11C.GL_INCR);
                GL20C.glStencilOpSeparate(GL11C.GL_FRONT_AND_BACK, GL11C.GL_KEEP, GL11C.GL_INCR, GL11C.GL_INCR);
                GL11C.glColorMask(false, false, false, false);
            }
            case RESTORE -> {
                captureCullStateIfNeeded();
                GL11C.glDisable(GL11C.GL_CULL_FACE);
                GL11C.glStencilMask(0xFF);
                GL20C.glStencilMaskSeparate(GL11C.GL_FRONT_AND_BACK, 0xFF);
                GL11C.glStencilFunc(GL11C.GL_EQUAL, compareReference, 0xFF);
                GL20C.glStencilFuncSeparate(GL11C.GL_FRONT_AND_BACK, GL11C.GL_EQUAL, compareReference, 0xFF);
                GL11C.glStencilOp(GL11C.GL_KEEP, GL11C.GL_DECR, GL11C.GL_DECR);
                GL20C.glStencilOpSeparate(GL11C.GL_FRONT_AND_BACK, GL11C.GL_KEEP, GL11C.GL_DECR, GL11C.GL_DECR);
                GL11C.glColorMask(false, false, false, false);
            }
            case TEST -> {
                restoreCullStateIfNeeded();
                GL11C.glStencilMask(0x00);
                GL20C.glStencilMaskSeparate(GL11C.GL_FRONT_AND_BACK, 0x00);
                GL11C.glStencilFunc(GL11C.GL_EQUAL, reference, 0xFF);
                GL20C.glStencilFuncSeparate(GL11C.GL_FRONT_AND_BACK, GL11C.GL_EQUAL, reference, 0xFF);
                GL11C.glStencilOp(GL11C.GL_KEEP, GL11C.GL_KEEP, GL11C.GL_KEEP);
                GL20C.glStencilOpSeparate(GL11C.GL_FRONT_AND_BACK, GL11C.GL_KEEP, GL11C.GL_KEEP, GL11C.GL_KEEP);
                GL11C.glColorMask(true, true, true, true);
            }
            case DISABLED -> resetNativeState();
        }
    }

    void resetNativeState() {
        GL11C.glDisable(GL11C.GL_STENCIL_TEST);
        GL11C.glStencilMask(0x00);
        GL20C.glStencilMaskSeparate(GL11C.GL_FRONT_AND_BACK, 0x00);
        GL11C.glStencilFunc(GL11C.GL_ALWAYS, 0, 0xFF);
        GL20C.glStencilFuncSeparate(GL11C.GL_FRONT_AND_BACK, GL11C.GL_ALWAYS, 0, 0xFF);
        GL11C.glStencilOp(GL11C.GL_KEEP, GL11C.GL_KEEP, GL11C.GL_KEEP);
        GL20C.glStencilOpSeparate(GL11C.GL_FRONT_AND_BACK, GL11C.GL_KEEP, GL11C.GL_KEEP, GL11C.GL_KEEP);
        GL11C.glColorMask(true, true, true, true);
        restoreDepthStateIfNeeded();
        restoreCullStateIfNeeded();
    }

    private void captureDepthStateIfNeeded() {
        if (depthStateCaptured) return;
        previousDepthTest = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST);
        previousDepthMask = GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK);
        depthStateCaptured = true;
    }

    private void restoreDepthStateIfNeeded() {
        if (!depthStateCaptured) return;
        if (previousDepthTest) {
            GL11C.glEnable(GL11C.GL_DEPTH_TEST);
        } else {
            GL11C.glDisable(GL11C.GL_DEPTH_TEST);
        }
        GL11C.glDepthMask(previousDepthMask);
        depthStateCaptured = false;
    }

    private void captureCullStateIfNeeded() {
        if (cullStateCaptured) return;
        previousCullFace = GL11C.glIsEnabled(GL11C.GL_CULL_FACE);
        cullStateCaptured = true;
    }

    private void restoreCullStateIfNeeded() {
        if (!cullStateCaptured) return;
        if (previousCullFace) {
            GL11C.glEnable(GL11C.GL_CULL_FACE);
        } else {
            GL11C.glDisable(GL11C.GL_CULL_FACE);
        }
        cullStateCaptured = false;
    }
}
