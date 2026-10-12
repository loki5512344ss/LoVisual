package dev.loki.lovisual.render.engine.optimize.gl.state;

/**
 * Tracks the last program / VAO bound through {@code GlStateManager} so
 * redundant driver calls can be skipped. Vanilla's {@code _glUseProgram} and
 * {@code _glBindVertexArray} hit the driver every time — see
 * {@code com.mojang.blaze3d.opengl.GlStateManager} bytecode. Framebuffer,
 * texture, blend, depth, cull and scissor already ride the vanilla
 * {@code BLEND/DEPTH/CULL/SCISSOR/TEXTURES/...} state arrays, so this cache
 * only fills the two slots vanilla forgot.
 *
 * <p>Two independent slots:
 * <ul>
 *   <li>{@link #program} — {@code glUseProgram} id, {@code -1} = unknown.</li>
 *   <li>{@link #vao} — {@code glBindVertexArray} id, {@code -1} = unknown.</li>
 * </ul>
 *
 * <p>{@code -1} is the safe state: the next call through the mixin always
 * reaches the driver. {@link #reset()} returns to that state; call it whenever
 * anything outside the patched {@code GlStateManager} path may have changed
 * the binding (device reset, shader recompile that internally calls
 * {@code glUseProgram}, {@code F3+T} resource reload, world change).
 *
 * <p>Backed by the Stage 1 {@code micro_opts} gate; the mixin checks
 * {@code OptimizeToggles.microOpts()} before consulting this holder so
 * turning micro-opts off also disables the cache.
 *
 * <p>Part of Stage 3 "Дополнительные GL-оптимизации" — the extended GL state
 * cache item in {@code mod/docs/optimize-layer-design.md}.
 */
public final class GlStateCache {

    /** Unknown / stale binding — the next call always reaches the driver. */
    public static final int UNKNOWN = -1;

    private static volatile int program = UNKNOWN;
    private static volatile int vao = UNKNOWN;

    private GlStateCache() {
    }

    /** True when a {@code glUseProgram(program)} would be a no-op. */
    public static boolean programAlreadyBound(int id) {
        return program == id;
    }

    /** True when a {@code glBindVertexArray(id)} would be a no-op. */
    public static boolean vaoAlreadyBound(int id) {
        return vao == id;
    }

    /** Record the program that just reached the driver. */
    public static void markProgram(int id) {
        program = id;
    }

    /** Record the VAO that just reached the driver. */
    public static void markVao(int id) {
        vao = id;
    }

    /** Program id the driver believes is bound (for tests / diagnostics). */
    public static int program() {
        return program;
    }

    /** VAO id the driver believes is bound (for tests / diagnostics). */
    public static int vao() {
        return vao;
    }

    /**
     * Forget both cached bindings — next {@code glUseProgram}/{@code glBindVertexArray}
     * always goes through. Called from {@code clearPipelineCache} (F3+T), device
     * shutdown, and the {@code Optimize} module's {@code onDisable}.
     */
    public static void reset() {
        program = UNKNOWN;
        vao = UNKNOWN;
    }
}
