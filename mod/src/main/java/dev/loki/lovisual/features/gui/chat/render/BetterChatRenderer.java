package dev.loki.lovisual.features.gui.chat.render;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.features.gui.hud.draggable.impl.BetterChat;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

import dev.loki.lovisual.util.text.edit.TextSelection;
import dev.loki.lovisual.features.gui.chat.input.BetterChatInputHandler;
import dev.loki.lovisual.features.gui.chat.input.BetterChatMouseClickHandler;
import dev.loki.lovisual.features.gui.chat.menu.BetterChatContextMenuHandler;
import dev.loki.lovisual.features.gui.chat.menu.BetterChatContextMenuModel;
import dev.loki.lovisual.features.gui.chat.menu.BetterChatTooltipRenderer;
import dev.loki.lovisual.features.gui.chat.model.BetterChatModel;
import dev.loki.lovisual.features.gui.chat.model.BetterChatStore;
import dev.loki.lovisual.features.gui.chat.model.ChatLine;
import dev.loki.lovisual.features.gui.chat.suggestions.CommandSuggestorBridge;
import dev.loki.lovisual.features.gui.chat.text.BetterChatLayoutEngine;
import dev.loki.lovisual.features.gui.chat.text.util.ChatHoverUtil;

/**
 * Facade for the BetterChat HUD renderer. This enum singleton is now a thin delegation point:
 * it owns the shared per-frame state and constants read/written by the package-private helpers,
 * and re-exports the original public API exactly as before so no caller changes.
 */
public enum BetterChatRenderer {
    ;

    // ---- Constants (package-private; read by helpers) ----
    public static final int MAX_MESSAGE_CHARS = 256;
    public static final float PADDING = 8f;
    public static final float LINE_SPACING = 3f;
    public static final float RADIUS = 24f;
    public static final float MESSAGE_PAD_X = 8f;
    public static final float MESSAGE_PAD_Y = 5f;
    public static final float MESSAGE_GAP = 4f;
    public static final float MESSAGE_INPUT_GAP = 8f;
    public static final float MESSAGE_RADIUS = 9f;
    public static final float MESSAGE_TS_GAP = 8f;
    public static final float INPUT_RADIUS = 12f;
    public static final float SEARCH_ICON_WIDTH = 34f;
    public static final float SEARCH_ICON_HEIGHT = 22f;
    public static final float NEW_MESSAGE_REVEAL_SECONDS = 0.34f;
    public static final float NEW_MESSAGE_SLIDE_PX = 18f;
    public static final int MAX_HISTORY = 32000;
    public static final float CHAT_TOOLTIP_OFFSET_X = 6f;
    public static final float CHAT_TOOLTIP_OFFSET_Y = 12f;
    public static final double DRAG_SELECT_THRESHOLD = 4.0;
    public static final RenderColor TMP_COLOR = new RenderColor(0xFFFFFFFF);

    // ---- Layout caches (package-private) ----
    public static final IdentityHashMap<ChatLine, BetterChatModel.CachedMessageLayout> MESSAGE_LAYOUT_CACHE = new IdentityHashMap<>();
    public static final IdentityHashMap<ChatLine, List<BetterChatModel.Segment>> SEGMENT_CACHE = new IdentityHashMap<>();
    public static final IdentityHashMap<CommandOutput.MessageLine, ChatLine> COMMAND_LINE_CACHE = new IdentityHashMap<>();
    public static final TextSelection selection = new TextSelection();

    // ---- Persistent UI state (package-private) ----
    public static Renderer2D renderer;
    public static float lastRenderWidth = 0f;
    public static float lastRenderHeight = 0f;
    public static int scrollOffsetLines = 0;
    public static float smoothScrollOffsetLines = 0f;
    public static int maxScrollLines = 0;
    public static float lastFontSizeForUi = 16f;
    public static List<ChatLine> lastMessages = Collections.emptyList();
    public static boolean draggingScrollbar = false;
    public static float sbTrackX, sbTrackY, sbTrackW, sbTrackH;
    public static float sbThumbY, sbThumbH;
    public static float sbDragOffset = 0f;
    public static long lastScrollInteractionMs = 0L;
    public static boolean sbEnabled = false;
    public static int sbTotalLines = 0;
    public static int sbVisibleLines = 0;
    public static float inputBoxX = 0f, inputBoxY = 0f, inputBoxW = 0f, inputBoxH = 0f;
    public static boolean hasInputBox = false;
    public static boolean suggestActive = false;
    public static float suggestX, suggestY, suggestW, suggestH, suggestItemH;
    public static float suggestRowsX, suggestRowsY, suggestRowsW, suggestRowsH;
    public static int suggestStart, suggestVisible, suggestTotal;
    public static net.minecraft.client.gui.components.CommandSuggestions.SuggestionsList suggestWindow;
    public static List<Suggestion> suggestEntries = Collections.emptyList();
    public static boolean lastHoverWasOutsideSuggest = true;
    public static boolean suggestHasScrollbar = false;
    public static boolean suggestDraggingScrollbar = false;
    public static float suggestTrackX, suggestTrackY, suggestTrackW, suggestTrackH, suggestThumbH;
    public static int suggestCurrentStart = -1;
    public static int suggestCurrentSelection = -1;
    public static double lastMouseFx = 0.0;
    public static double lastMouseFy = 0.0;
    public static boolean lastMouseValid = false;
    public static boolean tooltipCaptured = false;
    public static String lastSuggestInput = "";
    public static int lastSuggestCursor = -1;
    public static int lastSuggestSelStart = -1;
    public static int lastSuggestSelEnd = -1;
    public static CommandSuggestorBridge.SuggestionSnapshot lastSnapshot = null;
    public static long debugPerfWindowStartNs = 0L;
    public static long debugPerfAccumLayoutNs = 0L;
    public static long debugPerfAccumBuildNs = 0L;
    public static int debugPerfAccumFrames = 0;
    public static BetterChatFrameModel.FrameLayout frame = BetterChatFrameModel.FrameLayout.empty();
    public static BetterChatContextMenuModel.ContextMenu contextMenu = BetterChatContextMenuModel.ContextMenu.closed();
    public static boolean selecting = false;
    public static boolean searchHit = false;
    public static float searchRectX = 0f, searchRectY = 0f, searchRectW = 0f, searchRectH = 0f;
    public static boolean searchHotkeyDown = false;
    public static float chatHitX = 0f, chatHitY = 0f, chatHitW = 0f, chatHitH = 0f;
    public static String hoverEntityUuid = null;
    public static ChatScreen storedScreen;
    public static BetterChatStore previewStore;
    public static int lastSuggestionHash = 0;
    public static boolean leftPending = false;
    public static double leftDownFx = 0.0, leftDownFy = 0.0;
    public static int leftDownMsgIndex = -1;
    public static int leftDownCharIndex = -1;
    public static Style leftDownStyle = null;

    // ---- Per-frame transient context (package-private; populated by the phased helpers) ----
    public static GuiGraphicsExtractor ctxGui;
    public static float ctxBaseX, ctxBaseY;
    public static BetterChat ctxSettings;
    public static net.minecraft.client.Minecraft ctxMc;
    public static boolean ctxPreview, ctxChatOpen, ctxActiveSurface;
    public static boolean ctxShowTs, ctxHasSeconds, ctxTipDate, ctxTipUnix, ctxAutoFollow;
    public static int ctxTsColor, ctxTotalMessages, ctxVisibleLimit, ctxMessagesSeen;
    public static float ctxWidth, ctxContentWidth, ctxTsWidth;
    public static float ctxLineHeight, ctxBoxHeight, ctxLineYOffset, ctxTargetVisible;
    public static float ctxStableBoxHeight, ctxFixedInputY, ctxFrameY;
    public static int[] ctxMessageGroups;
    public static List<BetterChatModel.VisualLine> ctxAllLines;
    public static List<BetterChatModel.VisualLine> ctxVisible;
    public static long ctxPerfT0, ctxPerfT1;
    public static ChatHoverUtil.HoverTip ctxHoverTip;

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
