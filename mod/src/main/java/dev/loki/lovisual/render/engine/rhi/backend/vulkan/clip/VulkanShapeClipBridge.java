package dev.loki.lovisual.render.engine.rhi.backend.vulkan.clip;

import com.mojang.blaze3d.systems.RenderPassDescriptor;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.jetbrains.annotations.Nullable;
import dev.loki.lovisual.render.engine.rhi.backend.vulkan.util.VulkanRenderStateBridge;
import dev.loki.lovisual.render.engine.rhi.backend.vulkan.clip.VulkanStencilAttachmentManager;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;

/**
 * Static entry used by Vulkan mixins without reaching through LoVisualRenderSystem repeatedly.
 */
public enum VulkanShapeClipBridge {
    ;
    private static VulkanStencilAttachmentManager attachments;

    public static void install(VulkanStencilAttachmentManager manager) {
        attachments = manager;
    }

    public static void uninstall(VulkanStencilAttachmentManager manager) {
        if (attachments == manager) attachments = null;
    }

    public static @Nullable GpuTextureView stencilAttachmentFor(RenderPassDescriptor descriptor) {
        if (!VulkanRenderStateBridge.currentRenderPassNeedsStencilAttachment()) return null;
        return attachments == null ? null : attachments.attachmentFor(descriptor);
    }

    public static boolean consumeStencilClear() {
        return attachments != null && attachments.consumeLastAttachmentClear();
    }
}
