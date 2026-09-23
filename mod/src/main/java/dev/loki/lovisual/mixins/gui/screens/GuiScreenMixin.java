/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.gui.screens;

import com.llamalad7.mixinextras.sugar.Local;
import dev.loki.lovisual.features.module.core.ModuleManager;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.gui.clickgui.editor.ClickGuiEditorScreen;
import dev.loki.lovisual.features.gui.clickgui.picker.screen.ClickGuiPickerScreen;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.screen.ClickGuiScreen;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.CustomBar;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland.DynamicIsland;
import dev.loki.lovisual.features.module.modules.misc.ClickGui;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.core.frame.RenderPhase;
import dev.loki.lovisual.render.engine.core.frame.RenderPhaseScope;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;

@Mixin(Gui.class)
public abstract class GuiScreenMixin {

    @Unique
    private static void lovisual$extractCustomBarHeads(GuiGraphicsExtractor drawContext, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        if (ClientScreen.current() != null) return;

        CustomBar bar = CustomBar.get();
        if (bar == null || !bar.isHudBarEnabled() || !bar.isHotbarXpBarEnabled()) return;
        if (CustomBar.shouldUseJumpBar(mc) || !CustomBar.shouldUseLocator(mc, bar)) return;

        LoVisualRenderSystem.ensureFrameContext();
        try (RenderPhaseScope ignored = LoVisualRenderSystem.phase(RenderPhase.SCREEN_TOP, "2d:hud:custom_bar_heads")) {
            Renderer2D.withDeferredLayer(Renderer2D.Deferred2DLayer.AFTER_VANILLA_GUI, () -> {
                CustomBar.renderLocatorHeads(drawContext, tickCounter, mc);
                Renderer2D.COLOR.render();
            });
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void lovisual$closeClickGuiOnScreen(Screen screen, CallbackInfo ci) {
        if (!ModuleManager.isEnabled("clickgui")) {
            return;
        }

        if (ClickGuiRenderer.waitingForKey) {
            return;
        }

        if (screen instanceof ClickGuiScreen
                || screen instanceof ClickGuiPickerScreen
                || screen instanceof ClickGuiEditorScreen) {
            return;
        }

        if (screen != null) {
            ClickGui.setSuppressScreenClose(true);
            try {
                ModuleManager.setEnabled("clickgui", false);
            } finally {
                ClickGui.setSuppressScreenClose(false);
            }
        }
    }

    @Inject(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;applyCursor(Lcom/mojang/blaze3d/platform/Window;)V"
            )
    )
    private void lovisual$extractTopLayer(DeltaTracker tickCounter, boolean renderLevel, boolean renderScreens, CallbackInfo ci,
                                           @Local(ordinal = 0) GuiGraphicsExtractor drawContext) {
        if (drawContext == null) {
            return;
        }

        drawContext.nextStratum();
        lovisual$extractCustomBarHeads(drawContext, tickCounter);
        DynamicIsland.renderScreenOverlay(drawContext, tickCounter.getGameTimeDeltaTicks());
        ClickGuiRenderer.renderTopLayer(drawContext, tickCounter.getGameTimeDeltaTicks());
    }

}
