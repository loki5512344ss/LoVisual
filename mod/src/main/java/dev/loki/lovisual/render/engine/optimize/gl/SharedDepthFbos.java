package dev.loki.lovisual.render.engine.optimize.gl;

import com.mojang.blaze3d.opengl.GlStateManager;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;

/**
 * Stage 3 step 2: depth-texture id → FBO that already has that texture
 * attached at mip 0. Vanilla's {@code clearDepthTexture} attaches the depth
 * texture to a scratch FBO, clears, detaches and unbinds every time; with a
 * cached FBO the clear is a single {@code glClearNamedFramebufferfv}.
 *
 *
 * <p>{@link #remember}/{@link #forget} are driven by
 * {@code mixins/optimize/gl/fbo/FrameBufferCacheMixin} on every
 * {@code getFbo}/{@code destroyFbo}; {@link #clear} is called (gated on the
 * {@code fbo_share} toggle) from {@code GlCommandEncoderDepthClearMixin}.
 * Without GL 4.5 / {@code ARB_direct_state_access} the map still tracks, but
 * {@code clear} returns {@code false} and the vanilla path runs.
 *
 * <p>GL-only: on Vulkan this class is never reached (the mixins that feed it
 * target {@code com.mojang.blaze3d.opengl.*}).
 */
public enum SharedDepthFbos {
    ;

    private static final Int2IntOpenHashMap FBO_BY_DEPTH = new Int2IntOpenHashMap();
    private static final float[] VALUE = new float[1];
    private static Boolean dsa;

    /** Remember that {@code fbo} has {@code depthId} attached at mip 0. */
    public static void remember(int depthId, int fbo) {
        FBO_BY_DEPTH.put(depthId, fbo);
    }

    /** Drop every mapping pointing at {@code fbo} — its id may be reused by the next FBO. */
    public static void forget(int fbo) {
        FBO_BY_DEPTH.values().removeIf(f -> f == fbo);
    }

    /**
     * Clear the depth texture in place through its cached FBO.
     *
     * @param depthId the GL id of the depth texture
     * @param depth   the clear value (0..1)
     * @return {@code true} when the clear ran (caller should skip vanilla);
     *         {@code false} to fall through to the vanilla attach/clear/detach
     */
    public static boolean clear(int depthId, double depth) {
        int fbo = FBO_BY_DEPTH.getOrDefault(depthId, 0);
        if (fbo == 0) {
            return false;
        }
        if (dsa == null) {
            dsa = GL.getCapabilities().OpenGL45 || GL.getCapabilities().GL_ARB_direct_state_access;
        }
        if (!dsa) {
            return false;
        }
        // glClearNamedFramebuffer obeys the depth write mask and scissor test,
        // like glClear in the vanilla path — but make both states explicit.
        GlStateManager._depthMask(true);
        GlStateManager._disableScissorTest();
        VALUE[0] = (float) depth;
        ARBDirectStateAccess.glClearNamedFramebufferfv(fbo, GL11C.GL_DEPTH, 0, VALUE);
        return true;
    }

    // --- Test helpers (headless-safe: never touch GL) ---

    /** Test-only: drop all mappings and the cached DSA capability. */
    static void resetForTesting() {
        FBO_BY_DEPTH.clear();
        dsa = null;
    }

    /** Test-only: number of tracked depth textures. */
    static int sizeForTesting() {
        return FBO_BY_DEPTH.size();
    }

    /** Test-only: the FBO currently mapped for {@code depthId}, or 0. */
    static int fboForTesting(int depthId) {
        return FBO_BY_DEPTH.getOrDefault(depthId, 0);
    }
}
