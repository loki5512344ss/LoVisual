package dev.loki.lovisual.render.engine.rhi.backend.vulkan.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.loki.lovisual.render.engine.rhi.backend.vulkan.util.VulkanRenderStateBridge.StencilMode;

/**
 * Cache key identifying a concrete {@code VkPipeline} variant: the base pipeline
 * plus the active render-pass sample count, stencil-attachment presence, effective
 * stencil mode and shape-clip participation. Extracted from
 * {@link VulkanRenderStateBridge}'s nested types; referenced as a value object by
 * the Vulkan device pipeline-variant cache.
 */
public final class VulkanPipelineVariantKey {
    private final RenderPipeline pipeline;
    private final int pipelineIdentity;
    private final int samples;
    private final boolean stencilFormat;
    private final StencilMode stencilMode;
    private final boolean shapeClipParticipant;

    public VulkanPipelineVariantKey(RenderPipeline pipeline, int samples, boolean stencilFormat, StencilMode stencilMode, boolean shapeClipParticipant) {
        this.pipeline = pipeline;
        this.pipelineIdentity = System.identityHashCode(pipeline);
        this.samples = samples;
        this.stencilFormat = stencilFormat;
        this.stencilMode = stencilMode == null ? StencilMode.DISABLED : stencilMode;
        this.shapeClipParticipant = shapeClipParticipant;
    }

    public boolean usesMsaa() {
        return samples > 1;
    }

    public boolean usesStencil() {
        return stencilFormat;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VulkanPipelineVariantKey other)) return false;
        return pipeline == other.pipeline
                && samples == other.samples
                && stencilFormat == other.stencilFormat
                && stencilMode == other.stencilMode
                && shapeClipParticipant == other.shapeClipParticipant;
    }

    @Override
    public int hashCode() {
        int result = pipelineIdentity;
        result = 31 * result + samples;
        result = 31 * result + (stencilFormat ? 1 : 0);
        result = 31 * result + stencilMode.hashCode();
        result = 31 * result + (shapeClipParticipant ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PipelineVariantKey{" +
                "pipeline=" + pipeline +
                ", samples=" + samples +
                ", stencilFormat=" + stencilFormat +
                ", stencilMode=" + stencilMode +
                ", shapeClipParticipant=" + shapeClipParticipant +
                '}';
    }
}
