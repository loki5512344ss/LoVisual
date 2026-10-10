package dev.loki.lovisual.features.module.modules.misc.optimize.gl;

import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GL backend implementation of {@link RenderBackendOps}.
 *
 * <p>Resolves the current GL device via {@code GlBackendAccess.current()} —
 * returns {@code null} on Vulkan so all mixins short-circuit to the vanilla
 * call. The cache state is static and render-thread-only (ZakoOpt does the
 * same — the GL command encoder runs single-threaded).
 *
 * <p>Ported from ZakoOpt's three GL caches:
 * <ul>
 *   <li>{@code zako.opt.mixin.gl.GlCommandEncoderClearMixin} (lazy clear) — wrapped in {@link #skipBindFramebufferZero(int)}.</li>
 *   <li>{@code zako.opt.gl.TexBufferCache} + {@code zako.opt.mixin.gl.GlCommandEncoderTexBufferMixin} — wrapped in the TBO-cache methods.</li>
 *   <li>{@code zako.opt.mixin.gl.RenderTypesMixin} — wrapped in the RenderTypes-cache methods.</li>
 * </ul>
 *
 * <p>Each toggle is gated by {@link OptimizeToggles#microOpts()} (the Stage 1
 * "micro_opts" toggle wired into {@code Optimize.java}); ZakoOpt splits this
 * into {@code lookupCaches}, {@code microOpts}, {@code tboCache} — Stage 1
 * collapses them, Stage 2 may split if profiling warrants.
 */
public final class GlRenderBackendOps implements RenderBackendOps {

    private static final GlRenderBackendOps INSTANCE = new GlRenderBackendOps();

    // --- TBO cache (ported from zako.opt.gl.TexBufferCache) ---
    /** Maps bound-texture-id -> packed (format << 32) | (buffer & 0xFFFFFFFFL). -1 = nothing attached. */
    private final Int2LongOpenHashMap tboAttached = new Int2LongOpenHashMap();
    private int boundTboTexture;

    // --- RenderTypes cache (ported from zako.opt.mixin.gl.RenderTypesMixin) ---
    private final Map<Identifier, Object> translucentCache = new ConcurrentHashMap<>();

    private GlRenderBackendOps() {
        tboAttached.defaultReturnValue(-1L);
    }

    /**
     * @return the singleton GL backend instance, or {@code null} if the
     *         current device is not GL (e.g. Vulkan backend active).
     */
    public static RenderBackendOps current() {
        // Stage 3-prep: we don't yet wire the GL device check (would require
        // the GL device accessor mixin). Return INSTANCE unconditionally for
        // now; the actual device-type check is added alongside the first GL
        // mixin that needs it (RenderTypesMixin — Stage 3b).
        return INSTANCE;
    }

    // --- Lazy clear ---

    @Override
    public boolean lazyClearSupported() {
        return OptimizeToggles.microOpts();
    }

    @Override
    public boolean skipBindFramebufferZero(int framebuffer) {
        if (!lazyClearSupported()) return false;
        return framebuffer == 0;
    }

    // --- TBO cache ---

    @Override
    public boolean tboCacheSupported() {
        return OptimizeToggles.microOpts();
    }

    @Override
    public void trackBoundTboTexture(int texture) {
        boundTboTexture = texture;
    }

    @Override
    public boolean tboAlreadyAttached(int format, int buffer) {
        if (!tboCacheSupported()) return false;
        return tboAttached.getOrDefault(boundTboTexture, -1L) == packTboKey(format, buffer);
    }

    @Override
    public void markTboAttached(int format, int buffer) {
        if (!tboCacheSupported()) return;
        tboAttached.put(boundTboTexture, packTboKey(format, buffer));
    }

    @Override
    public void forgetTboCache() {
        tboAttached.clear();
    }

    private static long packTboKey(int format, int buffer) {
        return ((long) format << 32) | (buffer & 0xFFFFFFFFL);
    }

    // --- RenderTypes cache ---

    @Override
    public boolean renderTypesCacheSupported() {
        return OptimizeToggles.microOpts();
    }

    @Override
    public Object renderTypeFor(Identifier id) {
        if (!renderTypesCacheSupported()) return null;
        return translucentCache.get(id);
    }

    @Override
    public void cacheRenderType(Identifier id, Object type) {
        if (!renderTypesCacheSupported()) return;
        translucentCache.put(id, type);
    }

    // --- Test helpers ---

    /** Test-only: clear all caches. */
    static void resetForTesting() {
        INSTANCE.tboAttached.clear();
        INSTANCE.boundTboTexture = 0;
        INSTANCE.translucentCache.clear();
    }

    /** Test-only: peek the bound TBO texture. */
    static int boundTboTextureForTesting() {
        return INSTANCE.boundTboTexture;
    }

    /** Test-only: count of cached render types. */
    static int cachedRenderTypeCountForTesting() {
        return INSTANCE.translucentCache.size();
    }
}
