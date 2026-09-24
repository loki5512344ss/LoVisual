package dev.loki.lovisual.render.engine.rhi.pipeline.pipeline;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.resources.Identifier;

/**
 * Stable LoVisual pipeline key. Native RenderPipeline identity is backend detail, not policy.
 */
public record PipelineKey(String namespace, String path) {
    public PipelineKey {
        namespace = namespace == null || namespace.isBlank() ? "lovisual" : namespace;
        path = path == null ? "unknown" : path;
    }

    public static PipelineKey of(String id) {
        if (id == null || id.isBlank()) return new PipelineKey("lovisual", "unknown");
        int colon = id.indexOf(':');
        if (colon > 0 && colon < id.length() - 1) {
            return new PipelineKey(id.substring(0, colon), id.substring(colon + 1));
        }
        return new PipelineKey("lovisual", id);
    }

    public static PipelineKey of(Identifier id) {
        if (id == null) return new PipelineKey("lovisual", "unknown");
        return new PipelineKey(id.getNamespace(), id.getPath());
    }

    public static PipelineKey of(RenderPipeline pipeline) {
        return pipeline != null ? of(pipeline.getLocation()) : of("unknown");
    }

    public String id() {
        return namespace + ":" + path;
    }

    public String getId() {
        return id();
    }

    @Override
    public String toString() {
        return id();
    }
}
