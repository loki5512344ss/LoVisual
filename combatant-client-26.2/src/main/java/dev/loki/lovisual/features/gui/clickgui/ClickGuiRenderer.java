/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui;

import dev.loki.lovisual.features.gui.clickgui.editor.ClickGuiTextEditorState;
import dev.loki.lovisual.features.gui.clickgui.picker.ClickGuiPickerState;
import dev.loki.lovisual.features.gui.clickgui.protocol.CombatProtocolHeuristicsEditorState;
import dev.loki.lovisual.features.gui.clickgui.sections.ModulesSection;
import dev.loki.lovisual.features.gui.clickgui.sections.SettingsSection;
import dev.loki.lovisual.features.gui.clickgui.settings.cooldown.ProtocolHeuristicsSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.owner.TextEditorOwner;
import dev.loki.lovisual.features.gui.clickgui.settings.text.TextSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public enum ClickGuiRenderer {
    ;

    static final ModulesSection MODULES_SECTION = new ModulesSection();
    static final SettingsSection SETTINGS_SECTION = new SettingsSection();
    static final String MODULES_TAB_ID = "lovisual:modules";
    static final String SETTINGS_TAB_ID = "lovisual:settings";
    static final float TAB_HEIGHT = 34f;
    static final float TAB_GAP = 0f;
    static final float TAB_PAD_X = 18f;
    static final float TAB_FONT = 17f;
    static final float TAB_MIN_W = 96f;
    static float[] tabX = new float[0];
    static float[] tabW = new float[0];
    static final float LIFECYCLE_OPEN_SPEED = 9.5f;
    static final float LIFECYCLE_CLOSE_SPEED = 8.0f;
    static final float INPUT_READY_PROGRESS = 0.72f;
    static final Set<Integer> pendingBindKeys = new LinkedHashSet<>();
    static final List<PickerIcon> pickerIcons = new ArrayList<>();
    static final ArrayList<TextBatchItem> TEXT_BATCH = new ArrayList<>();
    static final int TEXT_WIDTH_CACHE_LIMIT = 4096;
    static final int TEXT_HEIGHT_CACHE_LIMIT = 256;
    static final Map<TextMeasureKey, Float> TEXT_WIDTH_CACHE = new LinkedHashMap<>(512, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<TextMeasureKey, Float> eldest) {
            return size() > TEXT_WIDTH_CACHE_LIMIT;
        }
    };
    static final Map<TextHeightKey, Float> TEXT_HEIGHT_CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<TextHeightKey, Float> eldest) {
            return size() > TEXT_HEIGHT_CACHE_LIMIT;
        }
    };
    public static boolean waitingForKey = false;
    public static Object keyBindOwner = null;
    static int colorClipboard = 0xFFFFFFFF;
    static boolean init = false;
    static float mouseX, mouseY;
    static boolean cursorShown = false;
    static int fbWidth, fbHeight;
    static TextRenderer interRegular;
    static TextRenderer interMedium;
    static TextRenderer onestMedium;
    static TextRenderer onestBold;
    static TextRenderer monsterratRegular;
    static String activeTabId = MODULES_TAB_ID;
    static float tabBarX, tabBarY, tabBarW, tabBarH;
    static float tabTargetX, tabTargetW;
    static float tabAnimX, tabAnimW;
    static boolean tabAnimInit = false;
    static long lastTabAnimNs = System.nanoTime();
    static float lifecycleAnim = 0.0f;
    static boolean closingForExit = false;
    static boolean finishingClose = false;
    static long lastLifecycleAnimNs = System.nanoTime();
    static float modulesAreaX, modulesAreaY, modulesAreaW, modulesAreaH;
    static CompletableFuture<Void> pendingSaveTask;
    static ClickGuiTextEditorState textEditor = null;
    static ClickGuiPickerState picker = null;
    static CombatProtocolHeuristicsEditorState protocolHeuristicsEditor = null;
    static TextSetting inlineTextOwner = null;
    static StringBuilder inlineTextBuffer = null;
    static float inlineTextX, inlineTextY, inlineTextW, inlineTextH;
    static boolean pickerIconScissorActive = false;
    static float pickerIconScissorX, pickerIconScissorY, pickerIconScissorW, pickerIconScissorH;
    static Renderer2D renderer;
    static float renderAlphaMultiplier = 1.0f;
    static ClickGuiScreen mainScreen;
    static ClickGuiPickerScreen pickerScreen;
    static boolean textBatchActive;

    // =========================
    //  API for MIXIN MOUSE
    // =========================
    public static void onMouseMove(double x, double y) {
        ClickGuiInputHandler.onMouseMove(x, y);
    }

    public static void onMouseButton(double x, double y, int button, boolean pressed) {
        ClickGuiInputHandler.onMouseButton(x, y, button, pressed);
    }

    public static void onMouseScroll(double x, double y, double delta) {
        ClickGuiInputHandler.onMouseScroll(x, y, delta);
    }

    public static void onMouseMoveScaled(double x, double y) {
        ClickGuiInputHandler.onMouseMoveScaled(x, y);
    }

    public static void onMouseButtonScaled(double x, double y, int button, boolean pressed) {
        ClickGuiInputHandler.onMouseButtonScaled(x, y, button, pressed);
    }

    public static void onMouseScrollScaled(double x, double y, double delta) {
        ClickGuiInputHandler.onMouseScrollScaled(x, y, delta);
    }

    public static void onEditorMouseMoveScaled(double x, double y) {
        ClickGuiInputHandler.onEditorMouseMoveScaled(x, y);
    }

    public static void onEditorMouseButtonScaled(double x, double y, int button, boolean pressed) {
        ClickGuiInputHandler.onEditorMouseButtonScaled(x, y, button, pressed);
    }

    public static void onEditorMouseScrollScaled(double x, double y, double delta) {
        ClickGuiInputHandler.onEditorMouseScrollScaled(x, y, delta);
    }

    public static boolean onEditorCharTyped(char c) {
        return ClickGuiKeyboardHandler.onEditorCharTyped(c);
    }

    public static boolean onEditorKey(int key, int scancode, int action, int mods) {
        return ClickGuiKeyboardHandler.onEditorKey(key, scancode, action, mods);
    }

    public static void closeGuiEditor() {
        ClickGuiKeyboardHandler.closeGuiEditor();
    }

    public static boolean onCharTyped(char c) {
        return ClickGuiKeyboardHandler.onCharTyped(c);
    }

    public static boolean onFilesDrop(List<Path> paths) {
        return ClickGuiKeyboardHandler.onFilesDrop(paths);
    }

    public static boolean onKey(int key, int scancode, int action, int mods) {
        return ClickGuiKeyboardHandler.onKey(key, scancode, action, mods);
    }

    // =========================
    //  INIT
    // =========================
    public static void init() {
        ClickGuiLifecycle.init();
    }

    public static void beginOpenAnimation() {
        ClickGuiLifecycle.beginOpenAnimation();
    }

    public static void beginCloseAnimation() {
        ClickGuiLifecycle.beginCloseAnimation();
    }

    // =========================
    //  CURSOR OPEN/CLOSE
    // =========================
    public static boolean isLifecycleActive() {
        return ClickGuiLifecycle.isLifecycleActive();
    }

    public static boolean isClosingForExit() {
        return ClickGuiLifecycle.isClosingForExit();
    }

    public static void finishCloseImmediately() {
        ClickGuiLifecycle.finishCloseImmediately();
    }

    public static void onClose() {
        ClickGuiLifecycle.onClose();
    }

    public static void releaseCursorForGameplay() {
        ClickGuiLifecycle.releaseCursorForGameplay();
    }

    public static void ensureCursorShown() {
        ClickGuiLifecycle.ensureCursorShown();
    }

    public static void renderEngine(Renderer2D rendererIn, TextRenderer fallback, GuiGraphicsExtractor ctx, float tickDelta) {
        ClickGuiRenderEngine.renderEngine(rendererIn, fallback, ctx, tickDelta);
    }

    public static boolean shouldRenderAsTopLayer() {
        return ClickGuiTopLayer.shouldRenderAsTopLayer();
    }

    public static void renderTopLayer(GuiGraphicsExtractor ctx, float tickDelta) {
        ClickGuiTopLayer.renderTopLayer(ctx, tickDelta);
    }

    public static void renderEditor(Renderer2D rendererIn, TextRenderer fallback, GuiGraphicsExtractor ctx, float tickDelta) {
        ClickGuiTopLayer.renderEditor(rendererIn, fallback, ctx, tickDelta);
    }

    public static void renderModulesPreview(float x, float y, float w, float h) {
        ClickGuiTopLayer.renderModulesPreview(x, y, w, h);
    }

    public static ClickGuiIslandState islandState() {
        return ClickGuiTabState.islandState();
    }

    public static TextRenderer getInterRegular() {
        return ClickGuiFonts.getInterRegular();
    }

    public static Renderer2D currentRenderer() {
        return ClickGuiFonts.currentRenderer();
    }

    public static TextRenderer getInterMedium() {
        return ClickGuiFonts.getInterMedium();
    }

    public static TextRenderer getIosevkaRegular() {
        return ClickGuiFonts.getIosevkaRegular();
    }

    public static TextRenderer getSfMedium() {
        return ClickGuiFonts.getSfMedium();
    }

    public static TextRenderer getSfProDisplaySemibold() {
        return ClickGuiFonts.getSfProDisplaySemibold();
    }

    public static TextRenderer getMonsterratRegular() {
        return ClickGuiFonts.getMonsterratRegular();
    }

    public static int framebufferWidth() {
        return ClickGuiRenderEngine.framebufferWidth();
    }

    public static int framebufferHeight() {
        return ClickGuiRenderEngine.framebufferHeight();
    }

    public static float getAnimAlpha() {
        return ClickGuiRenderEngine.getAnimAlpha();
    }

    public static float getMouseX() {
        return ClickGuiRenderEngine.getMouseX();
    }

    public static float getMouseY() {
        return ClickGuiRenderEngine.getMouseY();
    }

    public static int mixColor(int from, int to, float t) {
        return ClickGuiDrawHelpers.mixColor(from, to, t);
    }

    public static int scaleAlpha(int color, float factor) {
        return ClickGuiDrawHelpers.scaleAlpha(color, factor);
    }

    public static int withAlpha(int color, int alpha) {
        return ClickGuiDrawHelpers.withAlpha(color, alpha);
    }

    public static float setRenderAlphaMultiplier(float alpha) {
        return ClickGuiDrawHelpers.setRenderAlphaMultiplier(alpha);
    }

    public static float getRenderAlphaMultiplier() {
        return ClickGuiDrawHelpers.getRenderAlphaMultiplier();
    }

    public static void restoreRenderAlphaMultiplier(float previous) {
        ClickGuiDrawHelpers.restoreRenderAlphaMultiplier(previous);
    }

    public static float scaleForSize(float size) {
        return ClickGuiTextBatch.scaleForSize(size);
    }

    public static float textWidth(TextRenderer tr, String text, float size) {
        return ClickGuiTextBatch.textWidth(tr, text, size);
    }

    public static float textHeight(TextRenderer tr, float size) {
        return ClickGuiTextBatch.textHeight(tr, size);
    }

    public static void beginTextBatch() {
        ClickGuiTextBatch.beginTextBatch();
    }

    public static void endTextBatch() {
        ClickGuiTextBatch.endTextBatch();
    }

    public static void drawText(TextRenderer tr, String text, float x, float y, float size, int argb, boolean shadow) {
        ClickGuiTextBatch.drawText(tr, text, x, y, size, argb, shadow);
    }

    public static String fitText(TextRenderer tr, String text, float size, float maxWidth) {
        return ClickGuiTextBatch.fitText(tr, text, size, maxWidth);
    }

    public static List<String> wrapText(TextRenderer tr, String text, float size, float maxWidth, int maxLines) {
        return ClickGuiTextBatch.wrapText(tr, text, size, maxWidth, maxLines);
    }

    public static void drawRoundedRect(float x, float y, float w, float h, float radius, int argb) {
        ClickGuiDrawHelpers.drawRoundedRect(x, y, w, h, radius, argb);
    }

    public static void drawRoundedRectStroke(float x, float y, float w, float h, float radius, float thickness, int argb) {
        ClickGuiDrawHelpers.drawRoundedRectStroke(x, y, w, h, radius, thickness, argb);
    }

    public static void drawRoundedRectGlow(float x, float y, float w, float h,
                                           float radius, float glow, int argb) {
        ClickGuiDrawHelpers.drawRoundedRectGlow(x, y, w, h, radius, glow, argb);
    }

    public static void drawRoundedRectShadow(float x, float y, float w, float h,
                                             float radius, float softness, float spread, int argb) {
        ClickGuiDrawHelpers.drawRoundedRectShadow(x, y, w, h, radius, softness, spread, argb);
    }

    public static void drawRoundedRectGradient(float x, float y, float w, float h, float radius,
                                               int startArgb, int endArgb, float angleDeg) {
        ClickGuiDrawHelpers.drawRoundedRectGradient(x, y, w, h, radius, startArgb, endArgb, angleDeg);
    }

    public static void drawRoundedRectStrokeGradient(float x, float y, float w, float h, float radius, float thickness,
                                                     int startArgb, int endArgb, float angleDeg) {
        ClickGuiDrawHelpers.drawRoundedRectStrokeGradient(x, y, w, h, radius, thickness, startArgb, endArgb, angleDeg);
    }

    public static void drawRoundedRectCorners(float x, float y, float w, float h,
                                              float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                              int argb) {
        ClickGuiDrawHelpers.drawRoundedRectCorners(x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL, argb);
    }

    public static void drawRect(float x, float y, float w, float h, int argb) {
        ClickGuiDrawHelpers.drawRect(x, y, w, h, argb);
    }

    public static void flushRenderer() {
        ClickGuiDrawHelpers.flushRenderer();
    }

    public static void drawGradientRect(float x, float y, float w, float h,
                                        int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        ClickGuiDrawHelpers.drawGradientRect(x, y, w, h, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public static void drawCircle(float cx, float cy, float r, int argb) {
        ClickGuiDrawHelpers.drawCircle(cx, cy, r, argb);
    }

    public static void drawLine(float x1, float y1, float x2, float y2, int argb) {
        ClickGuiDrawHelpers.drawLine(x1, y1, x2, y2, argb);
    }

    public static void drawBlur(float x, float y, float w, float h, float radius, int argb, float alpha) {
        ClickGuiDrawHelpers.drawBlur(x, y, w, h, radius, argb, alpha);
    }

    public static void drawGlass(float x, float y, float w, float h, float radius, float alpha) {
        ClickGuiDrawHelpers.drawGlass(x, y, w, h, radius, alpha);
    }

    public static float clickGuiBlurQuality() {
        return ClickGuiDrawHelpers.clickGuiBlurQuality();
    }

    public static float clickGuiBlurDensityScale() {
        return ClickGuiDrawHelpers.clickGuiBlurDensityScale();
    }

    public static float clickGuiGlassBlurScale() {
        return ClickGuiDrawHelpers.clickGuiGlassBlurScale();
    }

    public static int getColorClipboard() {
        return ClickGuiDrawHelpers.getColorClipboard();
    }

    public static void setColorClipboard(int argb) {
        ClickGuiDrawHelpers.setColorClipboard(argb);
    }

    public static void beginKeyBind(Object owner) {
        ClickGuiKeyBindManager.beginKeyBind(owner);
    }

    public static void finishKeyBind() {
        ClickGuiKeyBindManager.finishKeyBind();
    }

    public static String getPendingBindDisplay() {
        return ClickGuiKeyBindManager.getPendingBindDisplay();
    }

    public static void openTextEditor(TextEditorOwner owner, String title, String seedText, float anchorX, float anchorY, float anchorW) {
        ClickGuiEditorManager.openTextEditor(owner, title, seedText, anchorX, anchorY, anchorW);
    }

    public static void closeTextEditor(boolean save) {
        ClickGuiEditorManager.closeTextEditor(save);
    }

    public static void renderTextEditor(int fbw, int fbh) {
        ClickGuiEditorManager.renderTextEditor(fbw, fbh);
    }

    public static void toggleInlineText(TextSetting owner) {
        ClickGuiEditorManager.toggleInlineText(owner);
    }

    public static boolean isInlineTextActive() {
        return ClickGuiEditorManager.isInlineTextActive();
    }

    public static boolean isInlineTextOwner(TextSetting owner) {
        return ClickGuiEditorManager.isInlineTextOwner(owner);
    }

    public static String getInlineText(TextSetting owner) {
        return ClickGuiEditorManager.getInlineText(owner);
    }

    public static void updateInlineTextBounds(TextSetting owner, float x, float y, float w, float h) {
        ClickGuiEditorManager.updateInlineTextBounds(owner, x, y, w, h);
    }

    public static boolean isTextEditorActive() {
        return ClickGuiEditorManager.isTextEditorActive();
    }

    public static boolean isPickerActive() {
        return ClickGuiEditorManager.isPickerActive();
    }

    public static boolean isBlockingModuleKeybinds() {
        return ClickGuiEditorManager.isBlockingModuleKeybinds();
    }

    public static void processPickerIcons(GuiGraphicsExtractor ctx) {
        ClickGuiPickerIconManager.processPickerIcons(ctx);
    }

    public static void beginPickerIconScissor(float x, float y, float w, float h) {
        ClickGuiPickerIconManager.beginPickerIconScissor(x, y, w, h);
    }

    public static void endPickerIconScissor() {
        ClickGuiPickerIconManager.endPickerIconScissor();
    }

    public static void queuePickerIcon(ItemStack stack, float x, float y, float scale) {
        ClickGuiPickerIconManager.queuePickerIcon(stack, x, y, scale);
    }

    public static void queuePickerIcon(ItemStack stack,
                                       float x,
                                       float y,
                                       float scale,
                                       float clipX,
                                       float clipY,
                                       float clipW,
                                       float clipH) {
        ClickGuiPickerIconManager.queuePickerIcon(stack, x, y, scale, clipX, clipY, clipW, clipH);
    }

    public static void openClickGuiScreen() {
        ClickGuiScreenManager.openClickGuiScreen();
    }

    public static void captureMainScreen() {
        ClickGuiScreenManager.captureMainScreen();
    }

    public static ClickGuiScreen ensureMainScreen() {
        return ClickGuiScreenManager.ensureMainScreen();
    }

    public static void clearScreens() {
        ClickGuiScreenManager.clearScreens();
    }

    public static void openPicker(TextListSetting owner, String title, TextListSetting.PickerMode mode) {
        ClickGuiScreenManager.openPicker(owner, title, mode);
    }

    public static void openProtocolHeuristicsEditor(ProtocolHeuristicsSetting owner, String title) {
        ClickGuiScreenManager.openProtocolHeuristicsEditor(owner, title);
    }

    public static void closeProtocolHeuristicsEditor() {
        ClickGuiScreenManager.closeProtocolHeuristicsEditor();
    }

    public static void closePickerScreen() {
        ClickGuiScreenManager.closePickerScreen();
    }

    public record ClickGuiIslandState(float tabBarX,
                                      float tabBarY,
                                      float tabBarW,
                                      float tabBarH,
                                      float activeX,
                                      float activeW,
                                      float lifecycle,
                                      String activeLabel,
                                      int tabCount,
                                      boolean pickerActive) {
    }

}
