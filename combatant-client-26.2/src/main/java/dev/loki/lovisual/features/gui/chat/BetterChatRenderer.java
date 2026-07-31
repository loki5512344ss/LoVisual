/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.loki.lovisual.features.command.CommandOutput;
import dev.loki.lovisual.features.gui.hud.draggable.impl.BetterChat;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

import dev.loki.lovisual.util.text.TextSelection;

/**
 * Facade for the BetterChat HUD renderer. This enum singleton is now a thin delegation point:
 * it owns the shared per-frame state and constants read/written by the package-private helpers,
 * and re-exports the original public API exactly as before so no caller changes.
 */
public enum BetterChatRenderer {
    ;

    // ---- Constants (package-private; read by helpers) ----
    static final int MAX_MESSAGE_CHARS = 256;
    static final float PADDING = 8f;
    static final float LINE_SPACING = 3f;
    static final float RADIUS = 24f;
    static final float MESSAGE_PAD_X = 8f;
    static final float MESSAGE_PAD_Y = 5f;
    static final float MESSAGE_GAP = 4f;
    static final float MESSAGE_INPUT_GAP = 8f;
    static final float MESSAGE_RADIUS = 9f;
    static final float MESSAGE_TS_GAP = 8f;
    static final float INPUT_RADIUS = 12f;
    static final float SEARCH_ICON_WIDTH = 34f;
    static final float SEARCH_ICON_HEIGHT = 22f;
    static final float NEW_MESSAGE_REVEAL_SECONDS = 0.34f;
    static final float NEW_MESSAGE_SLIDE_PX = 18f;
    static final int MAX_HISTORY = 32000;
    static final float CHAT_TOOLTIP_OFFSET_X = 6f;
    static final float CHAT_TOOLTIP_OFFSET_Y = 12f;
    static final double DRAG_SELECT_THRESHOLD = 4.0;
    static final RenderColor TMP_COLOR = new RenderColor(0xFFFFFFFF);

    // ---- Layout caches (package-private) ----
    static final IdentityHashMap<ChatLine, BetterChatModel.CachedMessageLayout> MESSAGE_LAYOUT_CACHE = new IdentityHashMap<>();
    static final IdentityHashMap<ChatLine, List<BetterChatModel.Segment>> SEGMENT_CACHE = new IdentityHashMap<>();
    static final IdentityHashMap<CommandOutput.MessageLine, ChatLine> COMMAND_LINE_CACHE = new IdentityHashMap<>();
    static final TextSelection selection = new TextSelection();

    // ---- Persistent UI state (package-private) ----
    static Renderer2D renderer;
    static float lastRenderWidth = 0f;
    static float lastRenderHeight = 0f;
    static int scrollOffsetLines = 0;
    static float smoothScrollOffsetLines = 0f;
    static int maxScrollLines = 0;
    static float lastFontSizeForUi = 16f;
    static List<ChatLine> lastMessages = Collections.emptyList();
    static boolean draggingScrollbar = false;
    static float sbTrackX, sbTrackY, sbTrackW, sbTrackH;
    static float sbThumbY, sbThumbH;
    static float sbDragOffset = 0f;
    static long lastScrollInteractionMs = 0L;
    static boolean sbEnabled = false;
    static int sbTotalLines = 0;
    static int sbVisibleLines = 0;
    static float inputBoxX = 0f, inputBoxY = 0f, inputBoxW = 0f, inputBoxH = 0f;
    static boolean hasInputBox = false;
    static boolean suggestActive = false;
    static float suggestX, suggestY, suggestW, suggestH, suggestItemH;
    static float suggestRowsX, suggestRowsY, suggestRowsW, suggestRowsH;
    static int suggestStart, suggestVisible, suggestTotal;
    static net.minecraft.client.gui.components.CommandSuggestions.SuggestionsList suggestWindow;
    static List<Suggestion> suggestEntries = Collections.emptyList();
    static boolean lastHoverWasOutsideSuggest = true;
    static boolean suggestHasScrollbar = false;
    static boolean suggestDraggingScrollbar = false;
    static float suggestTrackX, suggestTrackY, suggestTrackW, suggestTrackH, suggestThumbH;
    static int suggestCurrentStart = -1;
    static int suggestCurrentSelection = -1;
    static double lastMouseFx = 0.0;
    static double lastMouseFy = 0.0;
    static boolean lastMouseValid = false;
    static boolean tooltipCaptured = false;
    static String lastSuggestInput = "";
    static int lastSuggestCursor = -1;
    static int lastSuggestSelStart = -1;
    static int lastSuggestSelEnd = -1;
    static CommandSuggestorBridge.SuggestionSnapshot lastSnapshot = null;
    static long debugPerfWindowStartNs = 0L;
    static long debugPerfAccumLayoutNs = 0L;
    static long debugPerfAccumBuildNs = 0L;
    static int debugPerfAccumFrames = 0;
    static BetterChatFrameModel.FrameLayout frame = BetterChatFrameModel.FrameLayout.empty();
    static BetterChatContextMenuModel.ContextMenu contextMenu = BetterChatContextMenuModel.ContextMenu.closed();
    static boolean selecting = false;
    static boolean searchHit = false;
    static float searchRectX = 0f, searchRectY = 0f, searchRectW = 0f, searchRectH = 0f;
    static boolean searchHotkeyDown = false;
    static float chatHitX = 0f, chatHitY = 0f, chatHitW = 0f, chatHitH = 0f;
    static String hoverEntityUuid = null;
    static ChatScreen storedScreen;
    static BetterChatStore previewStore;
    static int lastSuggestionHash = 0;
    static boolean leftPending = false;
    static double leftDownFx = 0.0, leftDownFy = 0.0;
    static int leftDownMsgIndex = -1;
    static int leftDownCharIndex = -1;
    static Style leftDownStyle = null;

    // ---- Per-frame transient context (package-private; populated by the phased helpers) ----
    static GuiGraphicsExtractor ctxGui;
    static float ctxBaseX, ctxBaseY;
    static BetterChat ctxSettings;
    static net.minecraft.client.Minecraft ctxMc;
    static boolean ctxPreview, ctxChatOpen, ctxActiveSurface;
    static boolean ctxShowTs, ctxHasSeconds, ctxTipDate, ctxTipUnix, ctxAutoFollow;
    static int ctxTsColor, ctxTotalMessages, ctxVisibleLimit, ctxMessagesSeen;
    static float ctxWidth, ctxContentWidth, ctxTsWidth;
    static float ctxLineHeight, ctxBoxHeight, ctxLineYOffset, ctxTargetVisible;
    static float ctxStableBoxHeight, ctxFixedInputY, ctxFrameY;
    static int[] ctxMessageGroups;
    static List<BetterChatModel.VisualLine> ctxAllLines;
    static List<BetterChatModel.VisualLine> ctxVisible;
    static long ctxPerfT0, ctxPerfT1;
    static ChatHoverUtil.HoverTip ctxHoverTip;

    // ---- Public API (delegating) ----

    public static void markLayoutDirty() {
        BetterChatLayoutEngine.markLayoutDirty();
    }

    public static void renderEngine(Renderer2D rendererIn,
                                     TextRenderer fallback,
                                     GuiGraphicsExtractor ctx,
                                     int screenW,
                                     float baseX,
                                     float baseY) {
        BetterChatFrameRenderer.renderEngine(rendererIn, fallback, ctx, screenW, baseX, baseY);
    }

    public static void renderCapturedTooltip(GuiGraphicsExtractor ctx) {
        BetterChatTooltipRenderer.renderCapturedTooltip(ctx);
    }

    public static boolean onScroll(double delta) {
        return BetterChatInputHandler.onScroll(delta);
    }

    public static boolean onMouseButton(double mx, double my, int button, boolean pressed) {
        return BetterChatMouseClickHandler.onMouseButton(mx, my, button, pressed);
    }

    public static void onMouseMove(double mx, double my) {
        BetterChatInputHandler.onMouseMove(mx, my);
    }

    public static void copySelectionToClipboard() {
        BetterChatContextMenuHandler.copySelectionToClipboard();
    }

    public static void resetScroll() {
        BetterChatInputHandler.resetScroll();
    }

    public static void applyAutoPrefixIfNeeded(EditBox field) {
        // Compact BetterChat no longer owns chat-prefix chips; hook kept for compatibility.
    }

    public static void setSuggestionContext(ChatScreen screen, GuiGraphicsExtractor context, int mouseX, int mouseY) {
        storedScreen = screen;
    }

    public static void onNewMessage() {
        BetterChatInputHandler.onNewMessage();
    }

    public static float getLastWidth() {
        return lastRenderWidth;
    }

    public static float getLastHeight() {
        return lastRenderHeight;
    }

    public static boolean contains(float mx, float my) {
        return BetterChatInputHandler.contains(mx, my);
    }

    public static boolean isInteractive(float mx, float my) {
        return BetterChatInputHandler.isInteractive(mx, my);
    }

    public record Layout(float x, float y) {
    }
}