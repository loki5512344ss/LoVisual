/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.hud.bars;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ExperienceBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.CustomBar;
import dev.loki.lovisual.Lifecycle;

@Mixin(ExperienceBar.class)
public abstract class ExperienceBarMixin {

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void lovisual$renderXpBar(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (!Lifecycle.canRunHud()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        CustomBar bar = CustomBar.get();
        if (bar == null) return;
        if (!bar.isHudBarEnabled() || !bar.isHotbarXpBarEnabled()) return;

        // We render XP/Locator ourselves from GuiMixin to avoid vanilla bar state.
        ci.cancel();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void lovisual$renderXpAddons(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (!Lifecycle.canRunHud()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        CustomBar bar = CustomBar.get();
        if (bar == null) return;
        if (!bar.isHudBarEnabled() || !bar.isHotbarXpBarEnabled()) return;

        ci.cancel();
    }
}

