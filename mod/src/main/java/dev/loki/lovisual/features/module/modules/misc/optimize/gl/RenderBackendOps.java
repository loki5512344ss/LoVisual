package dev.loki.lovisual.features.module.modules.misc.optimize.gl;

/**
 * Backend-agnostic surface for the optimization layer's GL/Vulkan hooks.
 *
 * <p>Stage 3 of {@code mod/docs/optimize-layer-design.md} splits the engine
 * into "what to do" (this interface) and "how to do it on this backend"
 * ({@link GlRenderBackendOps} for OpenGL, a future {@code VulkanRenderBackendOps}
 * for Vulkan). The Optimize mixins call {@link #current()} once and dispatch
 * through this surface; the Vulkan implementation returns {@code false} from
 * every {@code supported*} method so the mixin's {@code WrapOperation} falls
 * through to the original call.
 *
 * <p>Stage 3 ships:
 * <ul>
 *   <li>{@link #lazyClearSupported()} / {@link #skipBindFramebufferZero(int)} — lazy clear</li>
 *   <li>{@link #tboCacheSupported()} / {@link #tboAlreadyAttached(int, int)} /
 *       {@link #markTboAttached(int, int)} / {@link #trackBoundTboTexture(int)} /
 *       {@link #forgetTboCache()} — TBO cache</li>
 *   <li>{@link #renderTypesCacheSupported()} / {@link #renderTypeFor(Identifier)} /
 *       {@link #cacheRenderType(Identifier, Object)} — RenderTypes cache</li>
 * </ul>
 *
 * <p>Stage 4 will add Vulkan-specific paths ({@code vkCmdCopyImage},
 * {@code VkPipelineCache}) by implementing this surface with a no-op strategy
 * for the GL-only methods.
 */
public interface RenderBackendOps {

    /** Resolve the current backend — GL backend if GL is active, otherwise {@code null}. */
    static RenderBackendOps current() {
        return GlRenderBackendOps.current();
    }

    // --- Lazy clear ---

    /** True when this backend can skip the redundant bind-framebuffer-0 after a clear. */
    boolean lazyClearSupported();

    /**
     * Called from {@code GlCommandEncoder.clearColorAndDepthTextures} on the
     * {@code GlStateManager._glBindFramebuffer(target, framebuffer)} call.
     *
     * @param framebuffer the framebuffer GlStateManager is about to bind
     * @return {@code true} to skip the bind (lazy clear — next pass binds its
     *         own target anyway, the GlStateManager cache skips redundant binds)
     */
    boolean skipBindFramebufferZero(int framebuffer);

    // --- TBO cache ---

    /** True when this backend can cache {@code glTexBuffer} attachments per-texture. */
    boolean tboCacheSupported();

    /** Track the most recent {@code glBindTexture} target's texture ID. */
    void trackBoundTboTexture(int texture);

    /** True when the {@code (format, buffer)} pair is already attached to the bound texture. */
    boolean tboAlreadyAttached(int format, int buffer);

    /** Mark the {@code (format, buffer)} pair as attached to the bound texture. */
    void markTboAttached(int format, int buffer);

    /** Forget all TBO attachments — called when a {@code GlBuffer.Direct} is closed. */
    void forgetTboCache();

    // --- RenderTypes cache ---

    /** True when this backend can cache {@code RenderTypes.entityTranslucent(id, flag=true)}. */
    boolean renderTypesCacheSupported();

    /**
     * Look up a cached render type for the given identifier.
     *
     * @param id the texture identifier
     * @return the cached render type, or {@code null} if not cached
     */
    Object renderTypeFor(Identifier id);

    /**
     * Cache a render type for the given identifier.
     *
     * @param id   the texture identifier
     * @param type the render type returned by the original lookup
     */
    void cacheRenderType(Identifier id, Object type);

    /** Tiny value-class carrying a texture identifier — kept out of the public API of {@link RenderBackendOps}. */
    final class Identifier {
        private final String namespace;
        private final String path;

        public Identifier(String namespace, String path) {
            this.namespace = namespace;
            this.path = path;
        }

        public String namespace() { return namespace; }
        public String path() { return path; }

        @Override
        public int hashCode() {
            return namespace.hashCode() * 31 + path.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Identifier i
                    && i.namespace.equals(namespace)
                    && i.path.equals(path);
        }

        @Override
        public String toString() {
            return namespace + ":" + path;
        }
    }
}
