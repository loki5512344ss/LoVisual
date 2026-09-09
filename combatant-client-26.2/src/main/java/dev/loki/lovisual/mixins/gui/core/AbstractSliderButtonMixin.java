/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.gui.core;

import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.BetterButtons;
import dev.loki.lovisual.mixins.accessors.widget.SliderWidgetAccessor;

@Mixin(AbstractSliderButton.class)
public abstract class AbstractSliderButtonMixin {

    @Inject(method = "extractWidgetRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("HEAD"), cancellable = true)
    private void lovisual$renderWidget(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        BetterButtons buttons = BetterButtons.get();
        if (buttons == null || !buttons.useUiButtons()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && ClientScreen.current() instanceof MerchantScreen) return;
        AbstractSliderButton self = (AbstractSliderButton) (Object) this;
        AbstractWidget widget = self;

        boolean hovered = widget.isMouseOver(mouseX, mouseY);
        boolean dragging = ((SliderWidgetAccessor) self).lovisual$isDragging();
        float hover = BetterButtons.updateHover(widget, hovered || dragging);

        double value = ((SliderWidgetAccessor) self).lovisual$getValue();
        BetterButtons.enqueueSlider(widget, (float) value, dragging, hover);
        if (hovered) {
            BetterButtons.captureTooltip(widget, mouseX, mouseY);
        }
        ci.cancel();
    }
}

