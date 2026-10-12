/*
 * Ported/adapted from Spatial GUI (https://github.com/tastytrash/Spatial-GUI), MIT License.
 * Copyright (c) tastytrash and Spatial GUI contributors.
 *
 * This file is part of LoVisual and is distributed under LoVisual's own GPLv3 license terms
 * for the modifications made here (see mod/LICENSE and mod/THIRD_PARTY_NOTICES.md); the MIT-licensed
 * portions ported from Spatial GUI remain available under the MIT terms above.
 */
package dev.loki.lovisual.mixins.render.spatialgui;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.loki.lovisual.features.module.modules.visuals.spatialgui.SpatialGui;
import dev.loki.lovisual.render.spatialgui.SpatialGuiCapture;
import dev.loki.lovisual.util.logging.DebugLog;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Adapted from Spatial-GUI's {@code GuiRendererMixin}: every {@code draw()} executes its prepared draw list
 * through {@code executeDrawRange(label, target, ...)} — when the instance is the isolated screen renderer
 * built by {@link SpatialGuiCapture} while SpatialGui is capturing, the target argument is swapped for the
 * off-screen capture texture. The vanilla main GuiRenderer (HUD) keeps its normal framebuffer, and the flat
 * screen overlay is naturally absent because the screen was extracted into the isolated state instead.
 */
@Mixin(GuiRenderer.class)
public abstract class GuiRendererCaptureMixin {

    @ModifyArg(
            method = "draw",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;II)V"),
            index = 1)
    private RenderTarget lovisual$redirectSpatialCapture(RenderTarget original) {
        SpatialGui module = SpatialGui.getInstance();
        if (module == null || !module.isCapturing()) return original;
        SpatialGuiCapture capture = SpatialGuiCapture.get();
        if ((Object) this != capture.screenGuiRenderer()) return original;
        RenderTarget redirect = capture.target();
        if (redirect == null) return original;
        DebugLog.renderThreadOnce("sg.swap", "[SpatialGui] draw target swapped to capture texture");
        return redirect;
    }
}
