package dev.loki.lovisual.features.gui.clickgui.render;

import dev.loki.lovisual.features.gui.clickgui.ClickGuiModel.*;
import dev.loki.lovisual.features.gui.clickgui.editor.ClickGuiEditorManager;
import dev.loki.lovisual.features.gui.clickgui.editor.ClickGuiTextEditorState;
import dev.loki.lovisual.features.gui.clickgui.font.ClickGuiFonts;
import dev.loki.lovisual.features.gui.clickgui.input.ClickGuiInputHandler;
import dev.loki.lovisual.features.gui.clickgui.input.ClickGuiKeyBindManager;
import dev.loki.lovisual.features.gui.clickgui.input.ClickGuiKeyboardHandler;
import dev.loki.lovisual.features.gui.clickgui.picker.ClickGuiPickerIconManager;
import dev.loki.lovisual.features.gui.clickgui.picker.ClickGuiPickerState;
import dev.loki.lovisual.features.gui.clickgui.picker.screen.ClickGuiPickerScreen;
import dev.loki.lovisual.features.gui.clickgui.protocol.CombatProtocolHeuristicsEditorState;
import dev.loki.lovisual.features.gui.clickgui.screen.*;
import dev.loki.lovisual.features.gui.clickgui.sections.ModulesSection;
import dev.loki.lovisual.features.gui.clickgui.sections.SettingsSection;
import dev.loki.lovisual.features.gui.clickgui.settings.cooldown.ProtocolHeuristicsSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.owner.TextEditorOwner;
import dev.loki.lovisual.features.gui.clickgui.settings.text.TextSetting;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;
import dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiTabState;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Static facade for the ClickGui: owns shared mutable state (renderer handle, mouse
 * position, lifecycle/tab animation, editor / picker slots, text-measure caches) and
 * one-line forwarders to render engine, draw helpers, text batch, editor / picker /
 * screen managers, lifecycle, input, top layer, fonts and tab state.
 */
public enum ClickGuiRenderer {
    ;
    public static final ModulesSection MODULES_SECTION = new ModulesSection();
    public static final SettingsSection SETTINGS_SECTION = new SettingsSection();
    public static final String MODULES_TAB_ID = "lovisual:modules";
    public static final String SETTINGS_TAB_ID = "lovisual:settings";
    public static final float TAB_HEIGHT = 34f, TAB_GAP = 0f, TAB_PAD_X = 18f, TAB_FONT = 17f, TAB_MIN_W = 96f;
    public static final float LIFECYCLE_OPEN_SPEED = 9.5f, LIFECYCLE_CLOSE_SPEED = 8.0f, INPUT_READY_PROGRESS = 0.72f;
    static final int TEXT_WIDTH_CACHE_LIMIT = 4096, TEXT_HEIGHT_CACHE_LIMIT = 256;
    static final ArrayList<TextBatchItem> TEXT_BATCH = new ArrayList<>();
    static final Map<TextMeasureKey, Float> TEXT_WIDTH_CACHE = new LinkedHashMap<>(512, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<TextMeasureKey, Float> e) { return size() > TEXT_WIDTH_CACHE_LIMIT; } };
    static final Map<TextHeightKey, Float> TEXT_HEIGHT_CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<TextHeightKey, Float> e) { return size() > TEXT_HEIGHT_CACHE_LIMIT; } };
    public static float[] tabX = new float[0], tabW = new float[0];
    public static final Set<Integer> pendingBindKeys = new LinkedHashSet<>();
    public static final List<PickerIcon> pickerIcons = new ArrayList<>();
    public static boolean waitingForKey = false, cursorShown = false, init = false;
    public static boolean tabAnimInit = false, closingForExit = false, finishingClose = false, pickerIconScissorActive = false;
    public static Object keyBindOwner = null;
    public static TextRenderer interRegular, interMedium, onestMedium, onestBold, monsterratRegular;
    public static String activeTabId = MODULES_TAB_ID;
    public static ClickGuiTextEditorState textEditor = null;
    public static ClickGuiPickerState picker = null;
    public static CombatProtocolHeuristicsEditorState protocolHeuristicsEditor = null;
    public static TextSetting inlineTextOwner = null;
    public static StringBuilder inlineTextBuffer = null;
    public static ClickGuiScreen mainScreen;
    public static ClickGuiPickerScreen pickerScreen;
    public static Renderer2D renderer;
    public static CompletableFuture<Void> pendingSaveTask;
    public static float mouseX, mouseY;
    public static int fbWidth, fbHeight;
    public static long lastTabAnimNs = System.nanoTime(), lastLifecycleAnimNs = System.nanoTime();
    public static float lifecycleAnim = 0.0f, tabBarX, tabBarY, tabBarW, tabBarH, tabTargetX, tabTargetW, tabAnimX, tabAnimW;
    public static float modulesAreaX, modulesAreaY, modulesAreaW, modulesAreaH, inlineTextX, inlineTextY, inlineTextW, inlineTextH;
    public static float pickerIconScissorX, pickerIconScissorY, pickerIconScissorW, pickerIconScissorH;
    static int colorClipboard = 0xFFFFFFFF;
    static float renderAlphaMultiplier = 1.0f;
    static boolean textBatchActive;

    // Mouse / keyboard / editor input (mixin hooks)
    public static void onMouseMove(double x, double y) { ClickGuiInputHandler.onMouseMove(x, y); }
    public static void onMouseButton(double x, double y, int button, boolean pressed) { ClickGuiInputHandler.onMouseButton(x, y, button, pressed); }
    public static void onMouseScroll(double x, double y, double delta) { ClickGuiInputHandler.onMouseScroll(x, y, delta); }
    public static void onMouseMoveScaled(double x, double y) { ClickGuiInputHandler.onMouseMoveScaled(x, y); }
    public static void onMouseButtonScaled(double x, double y, int b, boolean p) { ClickGuiInputHandler.onMouseButtonScaled(x, y, b, p); }
    public static void onMouseScrollScaled(double x, double y, double d) { ClickGuiInputHandler.onMouseScrollScaled(x, y, d); }
    public static void onEditorMouseMoveScaled(double x, double y) { ClickGuiInputHandler.onEditorMouseMoveScaled(x, y); }
    public static void onEditorMouseButtonScaled(double x, double y, int b, boolean p) { ClickGuiInputHandler.onEditorMouseButtonScaled(x, y, b, p); }
    public static void onEditorMouseScrollScaled(double x, double y, double d) { ClickGuiInputHandler.onEditorMouseScrollScaled(x, y, d); }
    public static boolean onEditorCharTyped(char c) { return ClickGuiKeyboardHandler.onEditorCharTyped(c); }
    public static boolean onEditorKey(int k, int s, int a, int m) { return ClickGuiKeyboardHandler.onEditorKey(k, s, a, m); }
    public static void closeGuiEditor() { ClickGuiKeyboardHandler.closeGuiEditor(); }
    public static boolean onCharTyped(char c) { return ClickGuiKeyboardHandler.onCharTyped(c); }
    public static boolean onFilesDrop(List<Path> paths) { return ClickGuiKeyboardHandler.onFilesDrop(paths); }
    public static boolean onKey(int k, int s, int a, int m) { return ClickGuiKeyboardHandler.onKey(k, s, a, m); }

    // Init / lifecycle
    public static void init() { ClickGuiLifecycle.init(); }
    public static void beginOpenAnimation() { ClickGuiLifecycle.beginOpenAnimation(); }
    public static void beginCloseAnimation() { ClickGuiLifecycle.beginCloseAnimation(); }
    public static boolean isLifecycleActive() { return ClickGuiLifecycle.isLifecycleActive(); }
    public static boolean isClosingForExit() { return ClickGuiLifecycle.isClosingForExit(); }
    public static void finishCloseImmediately() { ClickGuiLifecycle.finishCloseImmediately(); }
    public static void onClose() { ClickGuiLifecycle.onClose(); }
    public static void releaseCursorForGameplay() { ClickGuiLifecycle.releaseCursorForGameplay(); }
    public static void ensureCursorShown() { ClickGuiLifecycle.ensureCursorShown(); }

    // Render entry / top layer / island
    public static void renderEngine(Renderer2D r, TextRenderer f, GuiGraphicsExtractor ctx, float dt) { ClickGuiRenderEngine.renderEngine(r, f, ctx, dt); }
    public static boolean shouldRenderAsTopLayer() { return ClickGuiTopLayer.shouldRenderAsTopLayer(); }
    public static void renderTopLayer(GuiGraphicsExtractor ctx, float tickDelta) { ClickGuiTopLayer.renderTopLayer(ctx, tickDelta); }
    public static void renderEditor(Renderer2D r, TextRenderer f, GuiGraphicsExtractor ctx, float dt) { ClickGuiTopLayer.renderEditor(r, f, ctx, dt); }
    public static void renderModulesPreview(float x, float y, float w, float h) { ClickGuiTopLayer.renderModulesPreview(x, y, w, h); }
    public static ClickGuiIslandState islandState() { return ClickGuiTabState.islandState(); }
    public static int framebufferWidth() { return ClickGuiRenderEngine.framebufferWidth(); }
    public static int framebufferHeight() { return ClickGuiRenderEngine.framebufferHeight(); }
    public static float getAnimAlpha() { return ClickGuiRenderEngine.getAnimAlpha(); }
    public static float getMouseX() { return ClickGuiRenderEngine.getMouseX(); }
    public static float getMouseY() { return ClickGuiRenderEngine.getMouseY(); }

    // Fonts / renderer handle
    public static TextRenderer getInterRegular() { return ClickGuiFonts.getInterRegular(); }
    public static TextRenderer getInterMedium() { return ClickGuiFonts.getInterMedium(); }
    public static TextRenderer getIosevkaRegular() { return ClickGuiFonts.getIosevkaRegular(); }
    public static TextRenderer getSfMedium() { return ClickGuiFonts.getSfMedium(); }
    public static TextRenderer getSfProDisplaySemibold() { return ClickGuiFonts.getSfProDisplaySemibold(); }
    public static TextRenderer getMonsterratRegular() { return ClickGuiFonts.getMonsterratRegular(); }
    public static Renderer2D currentRenderer() { return ClickGuiFonts.currentRenderer(); }

    // Color helpers
    public static int mixColor(int from, int to, float t) { return ClickGuiDrawHelpers.mixColor(from, to, t); }
    public static int scaleAlpha(int color, float factor) { return ClickGuiDrawHelpers.scaleAlpha(color, factor); }
    public static int withAlpha(int color, int alpha) { return ClickGuiDrawHelpers.withAlpha(color, alpha); }
    public static float setRenderAlphaMultiplier(float a) { return ClickGuiDrawHelpers.setRenderAlphaMultiplier(a); }
    public static float getRenderAlphaMultiplier() { return ClickGuiDrawHelpers.getRenderAlphaMultiplier(); }
    public static void restoreRenderAlphaMultiplier(float previous) { ClickGuiDrawHelpers.restoreRenderAlphaMultiplier(previous); }
    public static int getColorClipboard() { return ClickGuiDrawHelpers.getColorClipboard(); }
    public static void setColorClipboard(int argb) { ClickGuiDrawHelpers.setColorClipboard(argb); }

    // Text batch
    public static float scaleForSize(float size) { return ClickGuiTextBatch.scaleForSize(size); }
    public static float textWidth(TextRenderer tr, String text, float size) { return ClickGuiTextBatch.textWidth(tr, text, size); }
    public static float textHeight(TextRenderer tr, float size) { return ClickGuiTextBatch.textHeight(tr, size); }
    public static void beginTextBatch() { ClickGuiTextBatch.beginTextBatch(); }
    public static void endTextBatch() { ClickGuiTextBatch.endTextBatch(); }
    public static void drawText(TextRenderer tr, String text, float x, float y, float size, int argb, boolean shadow) { ClickGuiTextBatch.drawText(tr, text, x, y, size, argb, shadow); }
    public static String fitText(TextRenderer tr, String text, float size, float maxWidth) { return ClickGuiTextBatch.fitText(tr, text, size, maxWidth); }
    public static List<String> wrapText(TextRenderer tr, String text, float size, float maxW, int maxLines) { return ClickGuiTextBatch.wrapText(tr, text, size, maxW, maxLines); }

    // Shape primitives
    public static void drawRoundedRect(float x, float y, float w, float h, float radius, int argb) { ClickGuiDrawHelpers.drawRoundedRect(x, y, w, h, radius, argb); }
    public static void drawRoundedRectStroke(float x, float y, float w, float h, float r, float t, int argb) { ClickGuiDrawHelpers.drawRoundedRectStroke(x, y, w, h, r, t, argb); }
    public static void drawRoundedRectGlow(float x, float y, float w, float h, float r, float g, int argb) { ClickGuiDrawHelpers.drawRoundedRectGlow(x, y, w, h, r, g, argb); }
    public static void drawRoundedRectShadow(float x, float y, float w, float h, float r, float so, float sp, int argb) { ClickGuiDrawHelpers.drawRoundedRectShadow(x, y, w, h, r, so, sp, argb); }
    public static void drawRoundedRectGradient(float x, float y, float w, float h, float r, int sa, int ea, float ad) { ClickGuiDrawHelpers.drawRoundedRectGradient(x, y, w, h, r, sa, ea, ad); }
    public static void drawRoundedRectStrokeGradient(float x, float y, float w, float h, float r, float t, int sa, int ea, float ad) { ClickGuiDrawHelpers.drawRoundedRectStrokeGradient(x, y, w, h, r, t, sa, ea, ad); }
    public static void drawRoundedRectCorners(float x, float y, float w, float h, float tl, float tr, float br, float bl, int argb) { ClickGuiDrawHelpers.drawRoundedRectCorners(x, y, w, h, tl, tr, br, bl, argb); }
    public static void drawRect(float x, float y, float w, float h, int argb) { ClickGuiDrawHelpers.drawRect(x, y, w, h, argb); }
    public static void flushRenderer() { ClickGuiDrawHelpers.flushRenderer(); }
    public static void drawGradientRect(float x, float y, float w, float h, int tl, int tr, int br, int bl) { ClickGuiDrawHelpers.drawGradientRect(x, y, w, h, tl, tr, br, bl); }
    public static void drawCircle(float cx, float cy, float r, int argb) { ClickGuiDrawHelpers.drawCircle(cx, cy, r, argb); }
    public static void drawLine(float x1, float y1, float x2, float y2, int argb) { ClickGuiDrawHelpers.drawLine(x1, y1, x2, y2, argb); }
    public static void drawBlur(float x, float y, float w, float h, float r, int argb, float a) { ClickGuiDrawHelpers.drawBlur(x, y, w, h, r, argb, a); }
    public static void drawGlass(float x, float y, float w, float h, float r, float a) { ClickGuiDrawHelpers.drawGlass(x, y, w, h, r, a); }
    public static float clickGuiBlurQuality() { return ClickGuiDrawHelpers.clickGuiBlurQuality(); }
    public static float clickGuiBlurDensityScale() { return ClickGuiDrawHelpers.clickGuiBlurDensityScale(); }
    public static float clickGuiGlassBlurScale() { return ClickGuiDrawHelpers.clickGuiGlassBlurScale(); }

    // Keybind / editor / picker / screens
    public static void beginKeyBind(Object owner) { ClickGuiKeyBindManager.beginKeyBind(owner); }
    public static void finishKeyBind() { ClickGuiKeyBindManager.finishKeyBind(); }
    public static String getPendingBindDisplay() { return ClickGuiKeyBindManager.getPendingBindDisplay(); }
    public static void openTextEditor(TextEditorOwner o, String title, String seed, float ax, float ay, float aw) { ClickGuiEditorManager.openTextEditor(o, title, seed, ax, ay, aw); }
    public static void closeTextEditor(boolean save) { ClickGuiEditorManager.closeTextEditor(save); }
    public static void renderTextEditor(int fbw, int fbh) { ClickGuiEditorManager.renderTextEditor(fbw, fbh); }
    public static void toggleInlineText(TextSetting owner) { ClickGuiEditorManager.toggleInlineText(owner); }
    public static boolean isInlineTextActive() { return ClickGuiEditorManager.isInlineTextActive(); }
    public static boolean isInlineTextOwner(TextSetting owner) { return ClickGuiEditorManager.isInlineTextOwner(owner); }
    public static String getInlineText(TextSetting owner) { return ClickGuiEditorManager.getInlineText(owner); }
    public static void updateInlineTextBounds(TextSetting o, float x, float y, float w, float h) { ClickGuiEditorManager.updateInlineTextBounds(o, x, y, w, h); }
    public static boolean isTextEditorActive() { return ClickGuiEditorManager.isTextEditorActive(); }
    public static boolean isPickerActive() { return ClickGuiEditorManager.isPickerActive(); }
    public static boolean isBlockingModuleKeybinds() { return ClickGuiEditorManager.isBlockingModuleKeybinds(); }
    public static void processPickerIcons(GuiGraphicsExtractor ctx) { ClickGuiPickerIconManager.processPickerIcons(ctx); }
    public static void beginPickerIconScissor(float x, float y, float w, float h) { ClickGuiPickerIconManager.beginPickerIconScissor(x, y, w, h); }
    public static void endPickerIconScissor() { ClickGuiPickerIconManager.endPickerIconScissor(); }
    public static void queuePickerIcon(ItemStack s, float x, float y, float sc) { ClickGuiPickerIconManager.queuePickerIcon(s, x, y, sc); }
    public static void queuePickerIcon(ItemStack s, float x, float y, float sc, float cx, float cy, float cw, float ch) { ClickGuiPickerIconManager.queuePickerIcon(s, x, y, sc, cx, cy, cw, ch); }
    public static void openClickGuiScreen() { ClickGuiScreenManager.openClickGuiScreen(); }
    public static void captureMainScreen() { ClickGuiScreenManager.captureMainScreen(); }
    public static ClickGuiScreen ensureMainScreen() { return ClickGuiScreenManager.ensureMainScreen(); }
    public static void clearScreens() { ClickGuiScreenManager.clearScreens(); }
    public static void openPicker(TextListSetting o, String title, TextListSetting.PickerMode mode) { ClickGuiScreenManager.openPicker(o, title, mode); }
    public static void openProtocolHeuristicsEditor(ProtocolHeuristicsSetting o, String title) { ClickGuiScreenManager.openProtocolHeuristicsEditor(o, title); }
    public static void closeProtocolHeuristicsEditor() { ClickGuiScreenManager.closeProtocolHeuristicsEditor(); }
    public static void closePickerScreen() { ClickGuiScreenManager.closePickerScreen(); }

    /** Snapshot of the tab-bar island geometry + lifecycle state, consumed by preview widgets. */
    public record ClickGuiIslandState(float tabBarX, float tabBarY, float tabBarW, float tabBarH,
                                      float activeX, float activeW, float lifecycle,
                                      String activeLabel, int tabCount, boolean pickerActive) { }
}
