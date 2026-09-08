/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins;

import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.misc.ItemScroller;
import dev.loki.lovisual.features.module.modules.visuals.Animations;
import dev.loki.lovisual.features.module.modules.visuals.ShulkerPreview;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Shadow
    protected Slot hoveredSlot;

    @Unique
    private boolean lovisual$animations$scaleApplied;

    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
    private void lovisual$inventoryMove$deferClose(CallbackInfo ci) {
        // InventoryMove module removed; keep hook for compatibility.
    }

    /**
     * Smooth open animation from the Animations module: the whole screen is drawn
     * with a scale between 0.9 and 1.0 around the screen center. The animated
     * value itself is advanced once per frame inside the module; this only reads
     * the settled value and wraps the extraction in a matching push/pop.
     */
    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("HEAD")
    )
    private void lovisual$animations$beginOpenScale(GuiGraphicsExtractor graphics,
                                                    int mouseX,
                                                    int mouseY,
                                                    float partialTick,
                                                    CallbackInfo ci) {
        lovisual$animations$scaleApplied = false;

        Animations animations = Modules.get(Animations.class);
        if (animations == null) return;

        float scale = animations.getInventoryScale();
        if (scale >= 0.999f) return;

        Matrix3x2fStack pose = graphics.pose();
        float centerX = graphics.guiWidth() * 0.5f;
        float centerY = graphics.guiHeight() * 0.5f;

        pose.pushMatrix();
        pose.translate(centerX, centerY);
        pose.scale(scale, scale);
        pose.translate(-centerX, -centerY);
        lovisual$animations$scaleApplied = true;
    }

    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("RETURN")
    )
    private void lovisual$animations$endOpenScale(GuiGraphicsExtractor graphics,
                                                  int mouseX,
                                                  int mouseY,
                                                  float partialTick,
                                                  CallbackInfo ci) {
        if (!lovisual$animations$scaleApplied) return;
        lovisual$animations$scaleApplied = false;
        graphics.pose().popMatrix();
    }

    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("TAIL")
    )
    private void lovisual$shulkerPreview$draw(GuiGraphicsExtractor graphics,
                                              int mouseX,
                                              int mouseY,
                                              float partialTick,
                                              CallbackInfo ci) {
        Slot slot = this.hoveredSlot;
        ItemStack stack = slot == null ? ItemStack.EMPTY : slot.getItem();
        if (stack.isEmpty()) return;
        ShulkerPreview.onScreenExtract(graphics, stack, mouseX, mouseY);
    }

    @Inject(
            method = "mouseScrolled(DDDD)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void lovisual$itemScroller$quickMove(double mouseX, double mouseY,
                                                 double horizontal, double vertical,
                                                 CallbackInfoReturnable<Boolean> cir) {
        ItemScroller scroller = Modules.get(ItemScroller.class);
        if (scroller == null || !scroller.isEnabled()) return;

        Slot slot = this.hoveredSlot;
        ItemStack stack = slot == null ? null : slot.getItem();
        if (scroller.onMouseScrolled(vertical, slot, stack)) {
            cir.setReturnValue(true);
        }
    }
}
