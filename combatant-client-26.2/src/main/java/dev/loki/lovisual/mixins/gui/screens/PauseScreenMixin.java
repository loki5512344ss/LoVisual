/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.gui.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.combat.PvpCooldowns;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {
    @Shadow
    @Nullable
    private Button disconnectButton;

    @Unique
    private Button lovisual$lastExitButton;
    @Unique
    private boolean lovisual$disconnectBlocked;
    @Unique
    private boolean lovisual$previousExitActive = true;

    @Inject(method = "init", at = @At("TAIL"))
    private void lovisual$blockDisconnectOnInit(CallbackInfo ci) {
        lovisual$syncDisconnectButton();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void lovisual$blockDisconnectOnRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        lovisual$syncDisconnectButton();
    }

    @Unique
    private void lovisual$syncDisconnectButton() {
        Button button = disconnectButton;
        if (button == null) {
            lovisual$lastExitButton = null;
            lovisual$disconnectBlocked = false;
            return;
        }

        if (button != lovisual$lastExitButton) {
            lovisual$lastExitButton = button;
            lovisual$disconnectBlocked = false;
            lovisual$previousExitActive = button.active;
        }

        PvpCooldowns cooldowns = Modules.get(PvpCooldowns.class);
        boolean shouldBlock = cooldowns != null && cooldowns.shouldBlockDisconnect();
        if (shouldBlock) {
            if (!lovisual$disconnectBlocked) {
                lovisual$previousExitActive = button.active;
            }
            lovisual$disconnectBlocked = true;
            button.active = false;
            button.setTooltip(Tooltip.create(cooldowns.getDisconnectBlockTooltip()));
            return;
        }

        if (lovisual$disconnectBlocked) {
            button.active = lovisual$previousExitActive;
            button.setTooltip(null);
            lovisual$disconnectBlocked = false;
        }
    }
}
