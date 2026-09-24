package dev.loki.lovisual.render.engine.rhi.command;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.jetbrains.annotations.Nullable;
import dev.loki.lovisual.render.engine.rhi.pipeline.layout.RenderPipelineRegistry;
import dev.loki.lovisual.render.engine.rhi.pipeline.pipeline.RenderPipelineSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import dev.loki.lovisual.render.engine.rhi.command.RhiSamplerBinding;
import dev.loki.lovisual.render.engine.rhi.command.RhiUniformBinding;

public final class FullscreenDrawCommand {
    public final String label;
    public final RenderPipeline pipeline;
    public final RenderPipelineSpec pipelineSpec;
    public final GpuTextureView colorAttachment;
    public final OptionalInt clearColor;
    public final List<RhiUniformBinding> uniforms;
    public final List<RhiSamplerBinding> samplers;

    private FullscreenDrawCommand(Builder b) {
        this.label = b.label;
        this.pipeline = b.pipeline;
        this.pipelineSpec = b.pipelineSpec;
        this.colorAttachment = b.colorAttachment;
        this.clearColor = b.clearColor;
        this.uniforms = List.copyOf(b.uniforms);
        this.samplers = List.copyOf(b.samplers);
    }

    public static Builder builder(String label) {
        return new Builder(label);
    }

    public static final class Builder {
        private final String label;
        private final List<RhiUniformBinding> uniforms = new ArrayList<>();
        private final List<RhiSamplerBinding> samplers = new ArrayList<>();
        private RenderPipeline pipeline;
        private RenderPipelineSpec pipelineSpec;
        private GpuTextureView colorAttachment;
        private OptionalInt clearColor = OptionalInt.empty();

        private Builder(String label) {
            this.label = label;
        }

        public Builder pipeline(RenderPipeline pipeline) {
            this.pipeline = pipeline;
            return this;
        }

        public Builder pipelineSpec(RenderPipelineSpec pipelineSpec) {
            this.pipelineSpec = pipelineSpec;
            return this;
        }

        public Builder colorAttachment(GpuTextureView colorAttachment) {
            this.colorAttachment = colorAttachment;
            return this;
        }

        public Builder clearColor(@Nullable Integer argb) {
            this.clearColor = argb != null ? OptionalInt.of(argb) : OptionalInt.empty();
            return this;
        }

        public Builder uniform(String name, GpuBufferSlice slice) {
            if (name != null && slice != null) uniforms.add(new RhiUniformBinding(name, slice));
            return this;
        }

        public Builder sampler(String name, GpuTextureView view, GpuSampler sampler) {
            if (name != null && view != null && sampler != null)
                samplers.add(new RhiSamplerBinding(name, view, sampler));
            return this;
        }

        public FullscreenDrawCommand build() {
            if (pipeline == null) throw new IllegalStateException("Fullscreen command without pipeline");
            if (pipelineSpec == null) pipelineSpec = RenderPipelineRegistry.global().require(pipeline);
            if (colorAttachment == null) throw new IllegalStateException("Fullscreen command without color attachment");
            return new FullscreenDrawCommand(this);
        }
    }
}
