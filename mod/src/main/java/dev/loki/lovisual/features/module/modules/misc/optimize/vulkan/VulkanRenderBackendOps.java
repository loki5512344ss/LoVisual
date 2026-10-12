package dev.loki.lovisual.features.module.modules.misc.optimize.vulkan;

import dev.loki.lovisual.features.module.modules.misc.optimize.gl.RenderBackendOps;

/**
 * Vulkan-side implementation of {@link RenderBackendOps}. All GPU-object ops
 * (lazy clear, TBO cache, shared depth FBO, RenderTypes cache) currently live
 * in the GL mixin set — under Vulkan those mixins never load, so this class is
 * a no-op placeholder that lets {@code RenderBackendOps.current()} dispatch by
 * actual backend instead of unconditionally returning the GL impl.
 *
 * <p>Every {@code *Supported} reports {@code false}. Callers already gate on
 * supported before attempting, so the effect is the same as "Optimize GPU
 * features off" for backend-dependent items. Stage 4 adds Vulkan-specific
 * paths (persistent {@code VkPipelineCache}, {@code VulkanRenderPass} state
 * cache) by flipping the corresponding {@code *Supported} here to true and
 * implementing the op body.
 *
 * <p>Doc reference: {@code mod/docs/optimize-layer-design.md}, "Этап 4" and
 * "Что уже решено в архитектуре" → "Рендер-бэкенды".
 */
public final class VulkanRenderBackendOps implements RenderBackendOps {

    private static final VulkanRenderBackendOps INSTANCE = new VulkanRenderBackendOps();

    private VulkanRenderBackendOps() {
    }

    /** Singleton — no state, no per-device instance needed. */
    public static VulkanRenderBackendOps current() {
        return INSTANCE;
    }

    // --- Lazy clear ---

    @Override
    public boolean lazyClearSupported() {
        return false;
    }

    @Override
    public boolean skipBindFramebufferZero(int framebuffer) {
        return false;
    }

    // --- TBO cache ---

    @Override
    public boolean tboCacheSupported() {
        return false;
    }

    @Override
    public void trackBoundTboTexture(int texture) {
    }

    @Override
    public boolean tboAlreadyAttached(int format, int buffer) {
        return false;
    }

    @Override
    public void markTboAttached(int format, int buffer) {
    }

    @Override
    public void forgetTboCache() {
    }

    // --- FBO share ---

    @Override
    public boolean fboShareSupported() {
        return false;
    }

    @Override
    public boolean clearDepthInPlace(int depthId, double value) {
        return false;
    }

    // --- RenderTypes cache ---

    @Override
    public boolean renderTypesCacheSupported() {
        return false;
    }

    @Override
    public Object renderTypeFor(Identifier id) {
        return null;
    }

    @Override
    public void cacheRenderType(Identifier id, Object type) {
    }
}
