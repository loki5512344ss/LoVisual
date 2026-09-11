/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.blaze3d.encoder;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassDescriptor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.rhi.scissor.GlobalScissorState;
import dev.loki.lovisual.util.logging.DebugLog;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;

@Mixin(CommandEncoder.class)
public abstract class CommandEncoderMixin {
    @Unique
    private static String lovisual$label(Supplier<String> label) {
        try {
            return label != null ? label.get() : "<null-label>";
        } catch (Throwable t) {
            return "<label-threw-" + t.getClass().getSimpleName() + ">";
        }
    }

    @Unique
    private static @Nullable GpuTextureView lovisual$firstColorView(
            List<RenderPassDescriptor.Attachment<Optional<Vector4fc>>> colorAttachments
    ) {
        if (colorAttachments == null) {
            return null;
        }
        for (RenderPassDescriptor.Attachment<Optional<Vector4fc>> attachment : colorAttachments) {
            if (attachment != null) {
                return attachment.textureView();
            }
        }
        return null;
    }

    @Unique
    private static @Nullable GpuTextureView lovisual$depthView(
            @Nullable RenderPassDescriptor.Attachment<OptionalDouble> depthAttachment
    ) {
        return depthAttachment == null ? null : depthAttachment.textureView();
    }

    @Unique
    private static Boolean lovisual$vulkanBackendCached;

    @Unique
    private static boolean lovisual$isVulkanBackend() {
        // submitRenderPass runs every frame; device info is static per session — resolve once.
        Boolean cached = lovisual$vulkanBackendCached;
        if (cached != null) {
            return cached;
        }
        boolean vulkan = false;
        try {
            if (RenderSystem.getDevice() != null && RenderSystem.getDevice().getDeviceInfo() != null) {
                String backendName = RenderSystem.getDevice().getDeviceInfo().backendName();
                vulkan = backendName != null && backendName.toLowerCase(java.util.Locale.ROOT).contains("vulkan");
            }
        } catch (Throwable ignored) {
            vulkan = false;
        }
        // Only cache once the device exists; before init, retry next call.
        if (RenderSystem.getDevice() != null) {
            lovisual$vulkanBackendCached = vulkan;
        }
        return vulkan;
    }

    @Inject(method = "createRenderPass(Lcom/mojang/blaze3d/systems/RenderPassDescriptor;)Lcom/mojang/blaze3d/systems/RenderPass;", at = @At("RETURN"))
    private void lovisual$onCreateRenderPass(
            RenderPassDescriptor descriptor,
            CallbackInfoReturnable<RenderPass> cir
    ) {
        RenderPass pass = cir.getReturnValue();
        if (pass == null || descriptor == null) {
            return;
        }
        try {
            GlobalScissorState.applyTo(pass, descriptor.renderArea);
        } catch (Throwable t) {
            DebugLog.error("[Scissor] render pass scissor hook failed for %s", t, lovisual$label(descriptor.label()));
        }

        try {
            LoVisualRenderSystem.rhi().shapeClip().beginRenderPass(
                    lovisual$label(descriptor.label()),
                    lovisual$firstColorView(descriptor.colorAttachments()),
                    lovisual$depthView(descriptor.depthAttachment())
            );
        } catch (Throwable t) {
            DebugLog.error("[ShapeClip] beginRenderPass hook failed for %s", t, lovisual$label(descriptor.label()));
        }
    }

    @Inject(method = "submitRenderPass", at = @At("HEAD"))
    private void lovisual$resetRhiRenderPassState(CallbackInfo ci) {
        if (lovisual$isVulkanBackend()) {
            return;
        }
        try {
            LoVisualRenderSystem.rhi().pipelineState().resetRenderPassState();
        } catch (Throwable t) {
            DebugLog.error("[ShapeClip/GL] render pass reset hook failed", t);
        }
    }
}
