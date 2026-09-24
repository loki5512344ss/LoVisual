package dev.loki.lovisual.features.gui.hud.draggable.impl.combat;

import dev.loki.lovisual.features.gui.hud.base.HudTextEffects;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.draggable.impl.HudPanelLayoutModes;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedInventoryHudPanel;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedListHudPanel;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.input.KeyManager;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

final class InventoryRenderHelper {

    private InventoryRenderHelper() {}

    static void handleTick(Inventory host) {
        Minecraft mc = Minecraft.getInstance();
        if (!host.toggleInit) {
            if (mc == null || mc.getWindow() == null) return;
            host.toggleArmed = true;
            String c = host.toggleBind.get();
            if (c != null && !c.isBlank() && !"NONE".equalsIgnoreCase(c) && ClientScreen.current() == null && KeyManager.isComboHeldAllowScreen(c)) host.toggleArmed = false;
            host.toggleInit = true;
        }
        String combo = host.toggleBind.get();
        if (combo == null || combo.isBlank() || "NONE".equalsIgnoreCase(combo)) return;
        if (mc == null || mc.getWindow() == null) return;
        boolean pressed = ClientScreen.current() == null && KeyManager.isComboHeldAllowScreen(combo);
        if (!pressed) { host.toggleArmed = true; return; }
        if (host.toggleArmed) { host.displayVisible = !host.displayVisible; host.toggleArmed = false; }
    }

    static List<ItemStack> collectStacks(Inventory host) {
        Minecraft mc = Minecraft.getInstance();
        List<ItemStack> stacks = new ArrayList<>(InventoryConstants.LAST_INV_SLOT - InventoryConstants.FIRST_INV_SLOT);
        if (mc == null || mc.player == null) {
            return stacks;
        }
        for (int slot = InventoryConstants.FIRST_INV_SLOT; slot < InventoryConstants.LAST_INV_SLOT; slot++) {
            stacks.add(mc.player.getInventory().getItem(slot));
        }
        return stacks;
    }

    static void render(Inventory host, Renderer2D renderer, TextRenderer textRenderer,
                       GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        host.layoutReady = false;
        host.hitboxActive = false;
        host.pendingItemTasks.clear();

        boolean forceVisible = DraggableHudElementRegistry.isForceVisible();
        if (mc == null || (mc.player == null && !forceVisible)) {
            host.setSize(0.0f, 0.0f);
            return;
        }
        if (!host.isEnabled() && !forceVisible) {
            host.setSize(0.0f, 0.0f);
            return;
        }
        boolean chatPreview = ClientScreen.current() instanceof ChatScreen;
        List<ItemStack> stacks = collectStacks(host);
        boolean hasAnyItems = stacks.stream().anyMatch(stack -> stack != null && !stack.isEmpty());
        boolean displayTargetVisible = host.displayVisible;
        boolean showWidget = forceVisible || (displayTargetVisible && (chatPreview || hasAnyItems));
        host.visibilityAnim = HudRenderUtil.animateVisibility(host.visibilityAnim, showWidget);
        float widgetScale = HudRenderUtil.visibilityScale(host.visibilityAnim);
        if (!showWidget && host.visibilityAnim <= 0.0f) {
            host.setSize(0.0f, 0.0f);
            return;
        }
        InventoryPalette.updatePalette(host);

        TextRenderer headerIconRenderer = Fonts.renderer("IconsNur", FontInfo.Type.Regular, TextRenderer.get());
        TextRenderer headerTextRenderer = Fonts.renderer("Inter", FontInfo.Type.Bold, TextRenderer.get());
        TextRenderer countRenderer = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, TextRenderer.get());
        if (headerIconRenderer == null) headerIconRenderer = textRenderer;
        if (headerTextRenderer == null) headerTextRenderer = textRenderer;
        if (countRenderer == null) countRenderer = textRenderer;

        float baseScale = HudScale.scale(screenW, screenH) * 1.1f * HudPanelLayoutModes.effectiveScale(host.scaleValue);
        float fontScale = 0.98f * (host.hud.getFontSize() / 18.0f);
        long itemCount = 0L;
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                itemCount += stack.getCount();
            }
        }
        countRenderer.begin(fontScale * 0.92f, false, false);
        String itemCountText = Long.toString(Math.max(0L, itemCount));
        float countLabelWidth = (float) countRenderer.getWidth("Items:", false);
        float countValueWidth = (float) countRenderer.getWidth(itemCountText, false);
        float countTextHeight = (float) countRenderer.getHeight(false);
        countRenderer.end();

        headerIconRenderer.begin(fontScale, false, false);
        headerTextRenderer.begin(fontScale, false, false);
        float headerIconH = (float) headerIconRenderer.getHeight(false);
        float headerTextH = (float) headerTextRenderer.getHeight(false);
        float titleWidth = (float) headerTextRenderer.getWidth("Inventory", false);
        headerTextRenderer.end();
        headerIconRenderer.end();

        float targetWidth = InventoryConstants.BASE_WIDTH * baseScale;
        float headerWidth = (InventoryConstants.TITLE_TEXT_X * baseScale) + titleWidth
                + countLabelWidth + countValueWidth
                + ((InventoryConstants.COUNT_LABEL_OFFSET + 12.0f) * baseScale);
        targetWidth = Math.max(targetWidth, headerWidth);
        float targetHeight = InventoryConstants.BASE_HEIGHT * baseScale;

        host.displayWidth = HudRenderUtil.animateDimension(host.displayWidth, targetWidth);
        host.displayHeight = HudRenderUtil.animateDimension(host.displayHeight, targetHeight);
        host.setSize(host.displayWidth, host.displayHeight);

        float drawScale = Math.max(0.0f, widgetScale);
        float drawWidth = host.displayWidth * drawScale;
        float drawHeight = host.displayHeight * drawScale;
        if (drawWidth <= 0.0f || drawHeight <= 0.0f) {
            host.setSize(0.0f, 0.0f);
            return;
        }
        float drawX = host.getX() + ((host.displayWidth - drawWidth) * 0.5f);
        float drawY = host.getY() + ((host.displayHeight - drawHeight) * 0.5f);
        float drawBaseScale = baseScale * drawScale;
        float drawFontScale = fontScale * drawScale;
        float drawHeaderIconH = headerIconH * drawScale;
        float drawHeaderTextH = headerTextH * drawScale;
        float drawCountTextHeight = countTextHeight * drawScale;
        float drawCountLabelWidth = countLabelWidth * drawScale;
        float drawCountValueWidth = countValueWidth * drawScale;
        host.hitboxActive = forceVisible || showWidget;

        int resolvedHeaderIconColor = host.uiCounter;
        if (host.headerIconPulse.get()) {
            int pulsed = HudTextEffects.animatedColor(host.uiCounter, HudTextEffects.Effect.PULSE,
                    host.headerIconPulseSpeed.get(), (float) (Util.getMillis() / 1000.0), 0.0f);
            resolvedHeaderIconColor = HudRenderUtil.mixColor(host.uiCounter, pulsed, host.headerIconPulseIntensity.get() / 100.0f);
        }
        host.scriptedPanel.render(renderer, textRenderer, ctx, tickDelta,
                new ScriptedInventoryHudPanel.Panel(drawX, drawY, drawWidth, drawHeight, drawScale,
                        drawBaseScale, drawFontScale, drawHeaderIconH, drawHeaderTextH,
                        drawCountTextHeight, drawCountLabelWidth, drawCountValueWidth, itemCount,
                        host.hasEffect(), Math.min(1.0f, (host.blurAlpha.get() / 255.0f) * (host.isThemeMode() ? 1.15f : 1.0f)),
                        resolvedHeaderIconColor, host.uiGridDivider, HudPanelLayoutModes.current(host.layoutMode),
                        new ScriptedListHudPanel.Palette(host.uiHeaderLeft, host.uiHeaderRight, host.uiBodyLeft, host.uiBodyRight,
                                host.uiOutline, host.uiText, host.uiMuted, host.uiCounter, host.uiTitleText, host.uiDivider, host.uiBlurTint),
                        ScriptedInventoryHudPanel.cells(stacks)));
    }

    static void renderForeground(Inventory host, Renderer2D renderer, TextRenderer textRenderer,
                                 GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        if (!host.layoutReady || ctx == null || host.pendingItemTasks.isEmpty()) {
            host.pendingItemTasks.clear();
            return;
        }
        for (Inventory.QueuedItemIcon task : host.pendingItemTasks) {
            renderer.item(task.stack(), task.x(), task.y(), task.scale(), task.seed(), Renderer2D.ITEM_OVERLAY_ALL, null);
        }
        host.pendingItemTasks.clear();
    }
}
