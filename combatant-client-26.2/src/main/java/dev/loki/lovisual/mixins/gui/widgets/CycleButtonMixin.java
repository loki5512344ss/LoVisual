/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.gui.widgets;

import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.BetterButtons;
import dev.loki.lovisual.mixins.accessors.widget.CyclingButtonWidgetAccessor;

@Mixin(CycleButton.class)
public abstract class CycleButtonMixin<T> {

    @Inject(method = "extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("HEAD"), cancellable = true)
    private void lovisual$drawIcon(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        BetterButtons buttons = BetterButtons.get();
        if (buttons == null || !buttons.useUiButtons()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && ClientScreen.current() instanceof MerchantScreen) return;
        CycleButton<T> self = (CycleButton<T>) (Object) this;
        AbstractWidget widget = self;

        boolean hovered = widget.isMouseOver(mouseX, mouseY);
        float hover = BetterButtons.updateHover(widget, hovered, mouseX, mouseY);

        CyclingButtonWidgetAccessor<T> accessor = (CyclingButtonWidgetAccessor<T>) self;
        Identifier icon = accessor.lovisual$getIcon() != null
                ? accessor.lovisual$getIcon().apply(self, self.getValue())
                : null;

        boolean showLabel = accessor.lovisual$getLabelType() != CycleButton.DisplayState.HIDE;
        BetterButtons.enqueueCyclingButton(self, icon, showLabel, hover, widget.isActive(), widget.isFocused());
        if (hovered) {
            BetterButtons.captureTooltip(widget, mouseX, mouseY);
        }

        ci.cancel();
    }
}

