package dev.loki.lovisual.render.engine.postprocess.graph;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.core.frame.RenderFrameContext;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessContext;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessPass;
import dev.loki.lovisual.render.engine.rhi.resource.LoVisualRhi;

import java.util.Set;
import dev.loki.lovisual.render.engine.postprocess.graph.PostProcessGraphPass;
import dev.loki.lovisual.render.engine.postprocess.graph.resource.PostProcessGraphResources;
import dev.loki.lovisual.render.engine.postprocess.graph.resource.PostProcessResolution;
import dev.loki.lovisual.render.engine.postprocess.graph.resource.PostProcessResource;

public final class LegacyPostProcessGraphPass implements PostProcessGraphPass {
    private final PostProcessPass delegate;
    private final String id;

    public LegacyPostProcessGraphPass(PostProcessPass delegate) {
        this.delegate = delegate;
        this.id = delegate.getClass().getName();
    }

    public PostProcessPass delegate() {
        return delegate;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public int priority() {
        return delegate.getPriority();
    }

    @Override
    public PostProcessPass.Phase phase() {
        return delegate.getPhase();
    }

    @Override
    public Set<PostProcessResource> reads() {
        return Set.of(PostProcessResource.GRAPH_SOURCE_COLOR);
    }

    @Override
    public Set<PostProcessResource> writes() {
        return Set.of(PostProcessResource.GRAPH_DEST_COLOR);
    }

    @Override
    public PostProcessResolution resolution() {
        return PostProcessResolution.FULL;
    }

    @Override
    public boolean enabled(RenderFrameContext context) {
        return delegate.isActive();
    }

    @Override
    public boolean execute(RenderFrameContext context, LoVisualRhi rhi, PostProcessGraphResources resources) {
        GpuTextureView src = resources.currentSource();
        GpuTextureView dst = resources.currentDestination();
        PostProcessContext legacyContext = resources.legacyContext();
        if (src == null || dst == null || legacyContext == null) return false;
        return delegate.render(legacyContext, src, dst);
    }
}
