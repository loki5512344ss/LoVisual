package dev.loki.lovisual.render.engine.rhi.resource.pool;
import dev.loki.lovisual.render.engine.rhi.resource.manager.RenderResourceManager;

/**
 * Stable descriptor for framebuffer resources owned by RenderResourceManager.
 */
public record FramebufferDescriptor(
        String name,
        int width,
        int height,
        boolean depth,
        boolean persistent,
        boolean sizeDependent,
        String owner
) {
    public FramebufferDescriptor {
        name = name == null || name.isBlank() ? "lovisual-framebuffer" : name;
        width = Math.max(1, width);
        height = Math.max(1, height);
        owner = owner == null || owner.isBlank() ? "unknown" : owner;
    }

    public static FramebufferDescriptor persistent(String name, int width, int height, boolean depth, String owner) {
        return new FramebufferDescriptor(name, width, height, depth, true, true, owner);
    }

    public static FramebufferDescriptor temporary(String name, int width, int height, boolean depth, String owner) {
        return new FramebufferDescriptor(name, width, height, depth, false, true, owner);
    }

    public String poolKey() {
        return name + "|" + depth + "|" + persistent + "|" + owner;
    }

    public String tempKey() {
        return depth + "|" + width + "x" + height + "|" + owner;
    }
}
