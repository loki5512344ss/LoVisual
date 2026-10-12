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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Adapted from Spatial-GUI's {@code GuiMixin}: while SpatialGui is capturing, the open screen's extraction
 * is diverted from the vanilla main GUI render state into {@link SpatialGuiCapture}'s isolated state, so the
 * main pass draws only the HUD and the screen content lands exclusively in the capture texture for the 3D
 * plane. Mouse coordinates arrive already reprojected onto the plane via the MouseHandler mixin.
 */
@Mixin(Gui.class)
public abstract class GuiSpatialExtractMixin {

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void lovisual$isolateScreenExtraction(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        SpatialGui module = SpatialGui.getInstance();
        boolean capturing = module != null && module.isCapturing();
        DebugLog.renderThreadOnChange("sg.extract", capturing,
                "[SpatialGui] screen extraction divert=%b screen=%s", capturing, screen.getClass().getSimpleName());
        if (capturing) {
            SpatialGuiCapture.get().extractScreen(screen, mouseX, mouseY, partialTick);
        } else {
            screen.extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, partialTick);
        }
    }
}
