/*
 * Ported/adapted from Spatial GUI (https://github.com/tastytrash/Spatial-GUI), MIT License.
 * Copyright (c) tastytrash and Spatial GUI contributors.
 *
 * This file is part of LoVisual and is distributed under LoVisual's own GPLv3 license terms
 * for the modifications made here (see mod/LICENSE and mod/THIRD_PARTY_NOTICES.md); the MIT-licensed
 * portions ported from Spatial GUI remain available under the MIT terms above.
 */
package dev.loki.lovisual.mixins.render.spatialgui;

import dev.loki.lovisual.features.module.modules.visuals.spatialgui.SpatialGui;
import dev.loki.lovisual.render.spatialgui.SpatialGuiCapture;
import dev.loki.lovisual.util.logging.DebugLog;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.WindowRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adapted from Spatial-GUI's {@code GameRendererMixin}: right before the main GUI pass the capture texture is
 * (re)sized to the frame's window state and cleared to transparent, and at the end of the frame the isolated
 * screen renderer built by {@link SpatialGuiCapture} runs its own prepare/draw cycle so the open screen lands
 * in the capture texture while the flat overlay stays absent.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererSpatialGuiMixin {

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V"))
    private void lovisual$prepareSpatialCapture(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        SpatialGui module = SpatialGui.getInstance();
        if (module == null || !module.isCapturing()) {
            DebugLog.renderThreadOnChange("sg.prepare", "notCapturing",
                    "[SpatialGui] prepare skipped: not capturing");
            return;
        }

        WindowRenderState windowState = ((GameRenderer) (Object) this).gameRenderState().windowRenderState;
        if (windowState == null || windowState.width <= 0 || windowState.height <= 0) {
            DebugLog.renderThreadOnChange("sg.prepare", "badWindowState",
                    "[SpatialGui] prepare skipped: windowState=%s", String.valueOf(windowState));
            return;
        }

        SpatialGuiCapture capture = SpatialGuiCapture.get();
        capture.ensure(windowState.width, windowState.height);
        capture.clearTransparent();
        DebugLog.renderThreadOnChange("sg.prepare", windowState.width + "x" + windowState.height,
                "[SpatialGui] capture prepared and cleared %dx%d", windowState.width, windowState.height);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void lovisual$renderIsolatedSpatialScreen(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        SpatialGui module = SpatialGui.getInstance();
        if (module == null || !module.isCapturing()) {
            DebugLog.renderThreadOnChange("sg.tail", "notCapturing",
                    "[SpatialGui] isolated TAIL skipped: not capturing");
            return;
        }
        if (!SpatialGuiCapture.get().isReady()) {
            DebugLog.renderThreadOnChange("sg.tail", "notReady",
                    "[SpatialGui] isolated TAIL skipped: capture not ready");
            return;
        }

        DebugLog.renderThreadOnChange("sg.tail", "run",
                "[SpatialGui] isolated render frame");
        SpatialGuiCapture.get().renderIsolatedFrame();
    }
}
