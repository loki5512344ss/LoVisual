package dev.loki.lovisual.features.gui.clickgui;

import dev.loki.lovisual.mixininterface.IGuiGraphics;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.helpers.ScissorFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public final class ClickGuiPickerIconManager {

    private ClickGuiPickerIconManager() {
    }

    static void processPickerIcons(GuiGraphicsExtractor ctx) {
        if (ClickGuiRenderer.pickerIcons.isEmpty()) {
            ClickGuiRenderer.pickerIconScissorActive = false;
            return;
        }
        if (ctx == null) {
            ClickGuiRenderer.pickerIcons.clear();
            ClickGuiRenderer.pickerIconScissorActive = false;
            return;
        }
        if (!(ctx instanceof IGuiGraphics accessor)) {
            ClickGuiRenderer.pickerIcons.clear();
            ClickGuiRenderer.pickerIconScissorActive = false;
            return;
        }
        if (Minecraft.getInstance() == null) {
            ClickGuiRenderer.pickerIcons.clear();
            ClickGuiRenderer.pickerIconScissorActive = false;
            return;
        }
        int seed = 0;
        for (PickerIcon icon : ClickGuiRenderer.pickerIcons) {
            ItemStack stack = icon.stack();
            if (stack == null || stack.isEmpty()) continue;
            boolean clippedGlobal = false;
            boolean clippedLocal = false;
            if (icon.hasGlobalClip()) {
                clippedGlobal = ScissorFunction.pushRaw(
                        icon.globalClipX(),
                        icon.globalClipY(),
                        icon.globalClipW(),
                        icon.globalClipH()
                );
            }
            if (icon.hasClip()) {
                clippedLocal = ScissorFunction.pushRaw(
                        icon.clipX(),
                        icon.clipY(),
                        icon.clipW(),
                        icon.clipH()
                );
            }
            Renderer2D.COLOR.begin();
            // Picker screen uses beginUnscaledLogical(), so queued icon coordinates are already logical.
            Renderer2D.COLOR.item(
                    stack,
                    icon.x(),
                    icon.y(),
                    icon.scale(),
                    seed++,
                    Renderer2D.ITEM_OVERLAY_ALL,
                    null
            );
            Renderer2D.COLOR.render();
            if (clippedLocal) ScissorFunction.pop();
            if (clippedGlobal) ScissorFunction.pop();
        }
        ClickGuiRenderer.pickerIcons.clear();
        ClickGuiRenderer.pickerIconScissorActive = false;
    }

    static void beginPickerIconScissor(float x, float y, float w, float h) {
        ClickGuiRenderer.pickerIconScissorActive = true;
        ClickGuiRenderer.pickerIconScissorX = x;
        ClickGuiRenderer.pickerIconScissorY = y;
        ClickGuiRenderer.pickerIconScissorW = w;
        ClickGuiRenderer.pickerIconScissorH = h;
    }

    static void endPickerIconScissor() {
        ClickGuiRenderer.pickerIconScissorActive = false;
    }

    static void queuePickerIcon(ItemStack stack, float x, float y, float scale) {
        if (stack == null || stack.isEmpty()) return;
        ClickGuiRenderer.pickerIcons.add(new PickerIcon(
                stack,
                x,
                y,
                scale,
                currentPickerIconScissorActive(),
                ClickGuiRenderer.pickerIconScissorX,
                ClickGuiRenderer.pickerIconScissorY,
                ClickGuiRenderer.pickerIconScissorW,
                ClickGuiRenderer.pickerIconScissorH,
                false,
                0f,
                0f,
                0f,
                0f
        ));
    }

    static void queuePickerIcon(ItemStack stack,
                                float x,
                                float y,
                                float scale,
                                float clipX,
                                float clipY,
                                float clipW,
                                float clipH) {
        if (stack == null || stack.isEmpty()) return;
        boolean hasClip = clipW > 0f && clipH > 0f;
        ClickGuiRenderer.pickerIcons.add(new PickerIcon(
                stack,
                x,
                y,
                scale,
                currentPickerIconScissorActive(),
                ClickGuiRenderer.pickerIconScissorX,
                ClickGuiRenderer.pickerIconScissorY,
                ClickGuiRenderer.pickerIconScissorW,
                ClickGuiRenderer.pickerIconScissorH,
                hasClip,
                clipX,
                clipY,
                clipW,
                clipH
        ));
    }

    private static boolean currentPickerIconScissorActive() {
        return ClickGuiRenderer.pickerIconScissorActive && ClickGuiRenderer.pickerIconScissorW > 0f && ClickGuiRenderer.pickerIconScissorH > 0f;
    }
}
