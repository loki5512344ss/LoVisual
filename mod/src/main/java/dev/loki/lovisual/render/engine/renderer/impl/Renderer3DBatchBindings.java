package dev.loki.lovisual.render.engine.renderer;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import dev.loki.lovisual.render.engine.rhi.command.RhiDrawCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable sampler/uniform binding set for a {@link Renderer3D} batch. Built via the
 * fluent {@code withSampler}/{@code withUniform} methods; {@link Renderer3D#batch} and
 * the {@code batchTextured} overloads thread it through to the world command buffer.
 * Extracted from {@code Renderer3DBatchBindings}; Physically under {@code impl/} but
 * declares the parent package for direct access to {@link Renderer3D.UniformResolver}.
 */
public final class Renderer3DBatchBindings {
    private static final String DEFAULT_SAMPLER = "u_Texture";

    private static final Renderer3DBatchBindings EMPTY = new Renderer3DBatchBindings(List.of(), List.of());

    private final List<SamplerBinding> samplers;
    private final List<UniformBinding> uniforms;

    private Renderer3DBatchBindings(List<SamplerBinding> samplers, List<UniformBinding> uniforms) {
        this.samplers = samplers;
        this.uniforms = uniforms;
    }

    public static Renderer3DBatchBindings none() {
        return EMPTY;
    }

    private static String nameOrDefault(String name) {
        return name != null ? name : DEFAULT_SAMPLER;
    }

    public Renderer3DBatchBindings withSampler(String name, Identifier textureId) {
        if (textureId == null) return this;
        return appendSampler(new SamplerBinding(nameOrDefault(name), textureId, null, null));
    }

    public Renderer3DBatchBindings withSampler(String name, GpuTextureView view, GpuSampler sampler) {
        if (view == null || sampler == null) return this;
        return appendSampler(new SamplerBinding(nameOrDefault(name), null, view, sampler));
    }

    public Renderer3DBatchBindings withUniform(String name, GpuBufferSlice slice) {
        if (name == null || slice == null) return this;
        return appendUniform(UniformBinding.direct(name, slice));
    }

    public Renderer3DBatchBindings withUniform(String name, Object mergeKey, Renderer3D.UniformResolver resolver) {
        if (name == null || mergeKey == null || resolver == null) return this;
        return appendUniform(UniformBinding.deferred(name, mergeKey, resolver));
    }

    private Renderer3DBatchBindings appendSampler(SamplerBinding binding) {
        ArrayList<SamplerBinding> next = new ArrayList<>(samplers.size() + 1);
        next.addAll(samplers);
        next.add(binding);
        return new Renderer3DBatchBindings(List.copyOf(next), uniforms);
    }

    private Renderer3DBatchBindings appendUniform(UniformBinding binding) {
        ArrayList<UniformBinding> next = new ArrayList<>(uniforms.size() + 1);
        next.addAll(uniforms);
        next.add(binding);
        return new Renderer3DBatchBindings(samplers, List.copyOf(next));
    }

    public boolean compatibleWith(Renderer3DBatchBindings other) {
        if (other == null) return false;
        if (samplers.size() != other.samplers.size() || uniforms.size() != other.uniforms.size()) {
            return false;
        }

        for (int i = 0; i < samplers.size(); i++) {
            if (!samplers.get(i).sameBinding(other.samplers.get(i))) {
                return false;
            }
        }

        for (int i = 0; i < uniforms.size(); i++) {
            if (!uniforms.get(i).sameBinding(other.uniforms.get(i))) {
                return false;
            }
        }

        return true;
    }

    public void applyTo(RhiDrawCommand.Builder builder, Minecraft mc) {
        for (SamplerBinding sampler : samplers) {
            sampler.apply(builder, mc);
        }
        for (UniformBinding uniform : uniforms) {
            uniform.apply(builder);
        }
    }

    private record SamplerBinding(String name,
                                  Identifier textureId,
                                  GpuTextureView view,
                                  GpuSampler sampler) {
        private boolean sameBinding(SamplerBinding other) {
            if (other == null) return false;
            if (!name.equals(other.name)) return false;
            if (textureId != null || other.textureId != null) {
                return textureId != null && textureId.equals(other.textureId);
            }
            return view == other.view && sampler == other.sampler;
        }

        private void apply(RhiDrawCommand.Builder builder, Minecraft mc) {
            if (textureId != null) {
                if (mc == null) return;
                AbstractTexture texture = mc.getTextureManager().getTexture(textureId);
                if (texture == null) return;
                GpuTextureView textureView = texture.getTextureView();
                GpuSampler textureSampler = texture.getSampler();
                if (textureView == null || textureSampler == null) return;
                builder.sampler(name, textureView, textureSampler);
                return;
            }

            if (view != null && sampler != null) {
                builder.sampler(name, view, sampler);
            }
        }
    }

    private record UniformBinding(String name,
                                  Object mergeKey,
                                  Renderer3D.UniformResolver resolver) {
        private static UniformBinding direct(String name, GpuBufferSlice slice) {
            return new UniformBinding(name, slice, () -> slice);
        }

        private static UniformBinding deferred(String name, Object mergeKey, Renderer3D.UniformResolver resolver) {
            return new UniformBinding(name, mergeKey, resolver);
        }

        private boolean sameBinding(UniformBinding other) {
            if (other == null) return false;
            return name.equals(other.name) && mergeKey.equals(other.mergeKey);
        }

        private void apply(RhiDrawCommand.Builder builder) {
            GpuBufferSlice slice = resolver.resolve();
            if (slice != null) {
                builder.uniform(name, slice);
            }
        }
    }
}
