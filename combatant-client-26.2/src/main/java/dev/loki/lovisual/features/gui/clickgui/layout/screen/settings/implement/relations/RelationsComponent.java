/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.relations;

import dev.loki.lovisual.features.gui.clickgui.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.ClickGuiSearch;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.relations.RelationPlayerCardComponent.CardHit;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.features.module.modules.misc.DefineTarget;
import dev.loki.lovisual.features.relations.PlayerRelations;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.SvgRenderOptions;
import dev.loki.lovisual.render.helpers.ScissorFunction;
import dev.loki.lovisual.render.helpers.SystemCursor;
import dev.loki.lovisual.util.text.ChatNameUtil;
import dev.loki.lovisual.util.text.ClipboardUtil;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RelationsComponent {
    private static final long STATUS_DURATION_MS = 2200L;
    private static final String I18N = "clickgui.settings.relations.";

    private final RelationPlayerCardComponent cardComponent = new RelationPlayerCardComponent();
    private final OnlineRelationPlayerPickerComponent onlinePicker;
    private final List<CardEntryHit> cardHits = new ArrayList<>();

    private RelationTab tab = RelationTab.FRIENDS;
    private ActiveField activeField = ActiveField.NONE;
    private String playerInput = "";
    private String statusMessage;
    private long statusUntilMs;

    private Rect friendsPill = Rect.ZERO;
    private Rect enemiesPill = Rect.ZERO;
    private Rect staffPill = Rect.ZERO;
    private Rect playerInputRect = Rect.ZERO;
    private Rect addTypedButton = Rect.ZERO;
    private Rect reservedPickerButton = Rect.ZERO;

    private float scroll;
    private float smoothedScroll;
    private boolean scrollbarVisible;
    private boolean draggingScrollbar;
    private float scrollbarX;
    private float scrollbarY;
    private float scrollbarW;
    private float scrollbarH;
    private float scrollbarThumbY;
    private float scrollbarThumbH;
    private float scrollbarMaxScroll;
    private float scrollbarDragOffset;
    private float listX;
    private float listY;
    private float listW;
    private float listH;
    private float modeAnim;
    private float friendsHoverAnim;
    private float enemiesHoverAnim;
    private float staffHoverAnim;
    private float addHoverAnim;
    private float pickerHoverAnim;
    private static boolean movementInputBlocked;

    public RelationsComponent() {
        this.onlinePicker = new OnlineRelationPlayerPickerComponent(this::setStatus);
    }

    public void resetScroll() {
        scroll = 0f;
        smoothedScroll = 0f;
        draggingScrollbar = false;
        activeField = ActiveField.NONE;
        onlinePicker.close();
        onlinePicker.resetScroll();
        movementInputBlocked = false;
    }

    public static boolean isMovementInputBlocked() {
        return movementInputBlocked;
    }

    public void render(float menuX, float menuY, float menuW, float menuH, float mx, float my, float scale) {
        cardHits.clear();
        SettingsGuiPalette palette = SettingsGuiPalette.current();
        movementInputBlocked = activeField != ActiveField.NONE || onlinePicker.blocksMovementInput();

        float areaX = menuX + 31f * scale;
        float areaY = menuY + 33f * scale;
        float areaW = menuW - 42f * scale;
        float areaH = menuH - 39f * scale;

        renderToolbar(areaX, areaY, areaW, mx, my, scale, palette);

        float y = areaY + 24f * scale;
        renderStatus(areaX, y, areaW, scale, palette);
        y += 13f * scale;

        listX = areaX;
        listY = y;
        listW = areaW;
        listH = Math.max(1f, areaY + areaH - y);
        if (onlinePicker.isVisible()) {
            onlinePicker.render(listX, listY, listW, listH, mx, my, scale, palette);
            return;
        }

        listX = areaX;
        listY = y;
        listW = areaW;
        listH = Math.max(1f, areaY + areaH - y);
        renderCards(listX, listY, listW, listH, filteredEntries(tab.entries()), mx, my, scale, palette);
    }

    public boolean mousePressedScrollbar(float mx, float my, int button) {
        if (onlinePicker.isVisible()) return onlinePicker.mousePressedScrollbar(mx, my, button);
        if (button != 0 || !isScrollbarHovered(mx, my)) return false;
        draggingScrollbar = true;
        scrollbarDragOffset = ClickGuiMath.insideRect(mx, my, scrollbarX, scrollbarThumbY, scrollbarW, scrollbarThumbH)
                ? my - scrollbarThumbY
                : scrollbarThumbH * 0.5f;
        scrollToMouse(my);
        return true;
    }

    public void mouseReleased(int button) {
        onlinePicker.mouseReleased(button);
        if (button == 0) draggingScrollbar = false;
    }

    public void scroll(float mx, float my, double amount) {
        if (onlinePicker.isVisible()) {
            onlinePicker.scroll(mx, my, amount);
            return;
        }
        if (!ClickGuiMath.insideRect(mx, my, listX, listY, listW, listH)) return;
        scroll += (float) (amount * 22f);
    }

    public boolean click(float mx, float my, int button) {
        if (button != 0) return false;

        if (friendsPill.contains(mx, my)) return switchTab(RelationTab.FRIENDS);
        if (enemiesPill.contains(mx, my)) return switchTab(RelationTab.ENEMIES);
        if (staffPill.contains(mx, my)) return switchTab(RelationTab.STAFF);

        if (playerInputRect.contains(mx, my)) {
            if (onlinePicker.isOpen()) {
                activeField = ActiveField.NONE;
                onlinePicker.focusSearch();
            } else {
                activeField = ActiveField.PLAYER;
            }
            movementInputBlocked = activeField != ActiveField.NONE || onlinePicker.blocksMovementInput();
            clearStatus();
            return true;
        }
        if (addTypedButton.contains(mx, my)) {
            if (onlinePicker.isOpen()) {
                onlinePicker.clearSearch();
                onlinePicker.focusSearch();
                movementInputBlocked = onlinePicker.blocksMovementInput();
                return true;
            }
            commitPlayerInput();
            return true;
        }
        if (reservedPickerButton.contains(mx, my)) {
            activeField = ActiveField.NONE;
            onlinePicker.toggle(tab.pickerMode());
            movementInputBlocked = onlinePicker.blocksMovementInput();
            clearStatus();
            return true;
        }
        if (onlinePicker.isVisible() && onlinePicker.click(mx, my, button)) {
            movementInputBlocked = onlinePicker.blocksMovementInput();
            return true;
        }
        if (onlinePicker.isVisible()) {
            return false;
        }

        for (CardEntryHit entryHit : cardHits) {
            CardHit hit = entryHit.hit();
            if (inside(hit.delete(), mx, my)) {
                tab.remove(entryHit.name());
                setStatus(tr("status.removed", "Removed: %s", entryHit.name()));
                return true;
            }
            if (ClickGuiMath.insideRect(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) {
                return true;
            }
        }

        activeField = ActiveField.NONE;
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (onlinePicker.keyPressed(keyCode, scanCode, modifiers)) {
            movementInputBlocked = activeField != ActiveField.NONE || onlinePicker.blocksMovementInput();
            return true;
        }
        if (activeField == ActiveField.NONE) return false;
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            activeField = ActiveField.NONE;
            movementInputBlocked = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            commitPlayerInput();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            setFieldText(activeField, dropLast(fieldText(activeField)));
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            appendToField(activeField, ClipboardUtil.get());
            return true;
        }
        return true;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (onlinePicker.charTyped(chr, modifiers)) {
            movementInputBlocked = activeField != ActiveField.NONE || onlinePicker.blocksMovementInput();
            return true;
        }
        if (activeField == ActiveField.NONE) return false;
        if (chr >= 32 && chr != 127) {
            appendToField(activeField, String.valueOf(chr));
        }
        return true;
    }

    private void renderToolbar(float x, float y, float w, float mx, float my, float scale, SettingsGuiPalette palette) {
        float pillW = 165f * scale;
        float pillH = 15f * scale;
        float targetMode = tab.ordinal();
        float dt = AnimationUtility.deltaTime();
        modeAnim = AnimationUtility.approach(modeAnim, targetMode, dt, 12f);
        modeAnim = AnimationUtility.snap(modeAnim, targetMode, 0.01f);

        friendsPill = new Rect(x, y, pillW / 3f, pillH);
        enemiesPill = new Rect(x + pillW / 3f, y, pillW / 3f, pillH);
        staffPill = new Rect(x + pillW * 2f / 3f, y, pillW / 3f, pillH);
        friendsHoverAnim = updateHover(friendsHoverAnim, friendsPill.contains(mx, my), dt);
        enemiesHoverAnim = updateHover(enemiesHoverAnim, enemiesPill.contains(mx, my), dt);
        staffHoverAnim = updateHover(staffHoverAnim, staffPill.contains(mx, my), dt);

        ClickGuiRenderer.drawBlur(x, y, pillW, pillH, 4f * scale, palette.panelBlurTint(), 145f / 255f);
        LayoutRender2D.roundedQuad(x, y, pillW, pillH, 3.5f * scale,
                SettingsGuiPalette.mix(palette.panelPillBase(), palette.panelBgLeft(), 0.34f),
                SettingsGuiPalette.mix(palette.panelPillBase(), palette.panelBgRight(), 0.44f),
                SettingsGuiPalette.mix(palette.panelPillBase(), palette.panelBgRight(), 0.44f),
                SettingsGuiPalette.mix(palette.panelPillBase(), palette.panelBgLeft(), 0.34f));
        LayoutRender2D.roundedStrokeQuad(x, y, pillW, pillH, 3.5f * scale, 0.5f * scale,
                palette.panelStroke(), palette.moduleDividerEnd(), palette.moduleDividerEnd(), palette.panelStroke());

        float pad = 1.2f * scale;
        float segment = (pillW - pad * 2f) / 3f;
        float activeX = x + pad + segment * modeAnim;
        LayoutRender2D.roundedQuad(activeX, y + pad, segment, pillH - pad * 2f, 3f * scale,
                SettingsGuiPalette.mix(palette.panelPillActive(), palette.menuCategorySelectedLeft(), 0.22f),
                SettingsGuiPalette.mix(palette.panelPillActive(), palette.menuCategorySelectedRight(), 0.22f),
                SettingsGuiPalette.mix(SettingsGuiPalette.darken(palette.panelPillActive(), 0.16f), palette.menuCategorySelectedRight(), 0.18f),
                SettingsGuiPalette.mix(palette.panelPillActive(), palette.menuCategorySelectedLeft(), 0.22f));

        drawToolbarSeparator(x + pad + segment, y, pillH, scale, palette);
        drawToolbarSeparator(x + pad + segment * 2f, y, pillH, scale, palette);

        drawSegment(friendsPill, tr("tab.friends", "Friends"), tab == RelationTab.FRIENDS, friendsHoverAnim, scale, palette);
        drawSegment(enemiesPill, tr("tab.enemies", "Enemies"), tab == RelationTab.ENEMIES, enemiesHoverAnim, scale, palette);
        drawSegment(staffPill, tr("tab.staff", "Staff"), tab == RelationTab.STAFF, staffHoverAnim, scale, palette);

        float fieldX = x + pillW + 8f * scale;
        float picker = 18f * scale;
        float action = 18f * scale;
        float gap = 4f * scale;
        boolean onlineMode = onlinePicker.isOpen();
        float fieldW = Math.max(80f * scale, w - (fieldX - x) - picker - action - gap * 2f);
        playerInputRect = new Rect(fieldX, y, fieldW, pillH);
        addTypedButton = new Rect(fieldX + fieldW + gap, y, action, pillH);
        reservedPickerButton = new Rect(addTypedButton.x() + action + gap, y, picker, pillH);

        if (onlineMode) {
            String query = onlinePicker.searchText();
            drawInput(playerInputRect, query, tr("placeholder.search_online", "Search online"), onlinePicker.isSearchFocused(), mx, my, scale, palette);
            addHoverAnim = updateHover(addHoverAnim, addTypedButton.contains(mx, my), dt);
            drawIconButton(addTypedButton, "x", addHoverAnim, query == null || query.isBlank(), scale, palette);
        } else {
            drawInput(playerInputRect, playerInput, tr("placeholder.nick", "Nick"), activeField == ActiveField.PLAYER, mx, my, scale, palette);
            addHoverAnim = updateHover(addHoverAnim, addTypedButton.contains(mx, my), dt);
            drawIconButton(addTypedButton, "check", addHoverAnim, false, scale, palette);
        }
        pickerHoverAnim = updateHover(pickerHoverAnim, reservedPickerButton.contains(mx, my) || onlinePicker.isVisible(), dt);
        drawIconButton(reservedPickerButton, onlineMode ? "x" : "user-plus", pickerHoverAnim, false, scale, palette);
    }

    private void renderCards(float x, float y, float w, float h, List<String> entries, float mx, float my, float scale, SettingsGuiPalette palette) {
        float gap = 6f * scale;
        float reservedScrollbarW = 9f * scale;
        float cardAreaW = Math.max(1f, w - reservedScrollbarW);
        int columns = cardAreaW > 230f * scale ? 2 : 1;
        float cardW = (cardAreaW - gap * (columns - 1)) / columns;
        float cardH = 36f * scale;
        float stepY = cardH + gap;
        int rows = (entries.size() + columns - 1) / columns;
        float contentH = rows <= 0 ? 0f : rows * stepY - gap;
        float maxScroll = Math.max(0f, contentH - h);
        updateScrollbarMetrics(x, y, w, h, maxScroll, scale);
        if (draggingScrollbar) scrollToMouse(ClickGuiRenderer.getMouseY());

        scroll = ClickGuiMath.clamp(scroll, -maxScroll, 0f);
        smoothedScroll = AnimationUtility.approach(smoothedScroll, scroll, draggingScrollbar ? 0.55f : 0.2f);
        smoothedScroll = AnimationUtility.snap(smoothedScroll, scroll, draggingScrollbar ? 0.01f : 0.05f);

        if (entries.isEmpty()) {
            String empty = ClickGuiSearch.isActive() && !ClickGuiSearch.getText().isBlank()
                    ? tr("empty.no_matches", "No matching players.")
                    : tr("empty.none_added", "No players added.");
            ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), empty, x + 4f * scale, y + 4f * scale, 7.4f * scale, palette.panelMuted(), false);
            return;
        }

        boolean clipped = ScissorFunction.pushRaw(x, y, w, h);
        for (int i = 0; i < entries.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            float cx = x + col * (cardW + gap);
            float cy = y + row * stepY + smoothedScroll;
            if (cy + cardH < y || cy > y + h) continue;
            String name = entries.get(i);
            CardHit hit = cardComponent.render(name, cx, cy, cardW, cardH, mx, my, scale, palette);
            cardHits.add(new CardEntryHit(name, hit));
        }
        if (clipped) ScissorFunction.pop();
        renderScrollbar(x, y, w, h, maxScroll, scale, palette);
    }

    private void drawSegment(Rect rect, String label, boolean active, float hover, float scale, SettingsGuiPalette palette) {
        float size = 7.5f * scale;
        float tw = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterMedium(), label, size);
        float th = ClickGuiRenderer.textHeight(ClickGuiRenderer.getInterMedium(), size);
        int color = active ? palette.panelText() : SettingsGuiPalette.mix(palette.panelMuted(), palette.panelText(), 0.20f * hover);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), label, rect.x() + (rect.w() - tw) * 0.5f, rect.y() + (rect.h() - th) * 0.5f, size, color, false);
    }

    private void drawToolbarSeparator(float x, float y, float h, float scale, SettingsGuiPalette palette) {
        float backingW = Math.max(1.25f * scale, 1.05f);
        float backingH = h - 4.4f * scale;
        float backingY = y + (h - backingH) * 0.5f;
        int backingTop = SettingsGuiPalette.withAlpha(palette.panelBgLeft(), 98);
        int backingBottom = SettingsGuiPalette.withAlpha(palette.panelBgRight(), 78);
        LayoutRender2D.roundedQuad(
                x - backingW * 0.5f,
                backingY,
                backingW,
                backingH,
                backingW * 0.5f,
                backingTop,
                backingTop,
                backingBottom,
                backingBottom
        );

        float lineW = Math.max(0.5f * scale, 0.75f);
        float lineH = h - 6.0f * scale;
        float lineY = y + (h - lineH) * 0.5f;
        int top = SettingsGuiPalette.withAlpha(SettingsGuiPalette.mix(palette.moduleDividerStart(), palette.menuCategorySelectedLeft(), 0.34f), 146);
        int mid = SettingsGuiPalette.withAlpha(SettingsGuiPalette.mix(palette.moduleDividerEnd(), palette.menuCategorySelectedRight(), 0.40f), 210);
        int bottom = SettingsGuiPalette.withAlpha(SettingsGuiPalette.mix(palette.moduleDividerEnd(), palette.panelText(), 0.24f), 136);
        LayoutRender2D.roundedQuad(
                x - lineW * 0.5f,
                lineY,
                lineW,
                lineH,
                lineW * 0.5f,
                top,
                mid,
                bottom,
                top
        );
    }

    private void drawInput(Rect rect, String value, String placeholder, boolean active, float mx, float my, float scale, SettingsGuiPalette palette) {
        boolean hover = rect.contains(mx, my);
        int bgA = SettingsGuiPalette.mix(palette.panelPillBase(), palette.menuCategoryHoverLeft(), hover || active ? 0.20f : 0.06f);
        int bgB = SettingsGuiPalette.mix(palette.panelPillBase(), palette.menuCategoryHoverRight(), hover || active ? 0.18f : 0.06f);
        LayoutRender2D.roundedQuad(rect.x(), rect.y(), rect.w(), rect.h(), 3.5f * scale, bgA, bgB, bgB, bgA);
        LayoutRender2D.roundedStrokeQuad(rect.x(), rect.y(), rect.w(), rect.h(), 3.5f * scale, 0.45f * scale,
                active ? palette.moduleDividerEnd() : LayoutRender2D.alpha(palette.moduleDividerStart(), 0.75f),
                active ? palette.moduleDividerEnd() : LayoutRender2D.alpha(palette.moduleDividerEnd(), 0.75f),
                active ? palette.moduleDividerEnd() : LayoutRender2D.alpha(palette.moduleDividerEnd(), 0.65f),
                active ? palette.moduleDividerStart() : LayoutRender2D.alpha(palette.moduleDividerStart(), 0.75f));

        boolean empty = value == null || value.isEmpty();
        String raw = empty ? (active ? "" : placeholder) : value;
        int color = empty && !active ? palette.panelMuted() : palette.panelText();
        float size = 6.8f * scale;
        String text = ClickGuiRenderer.fitText(ClickGuiRenderer.getInterRegular(), raw, size, rect.w() - 8f * scale);
        float textY = rect.y() + (rect.h() - ClickGuiRenderer.textHeight(ClickGuiRenderer.getInterRegular(), size)) * 0.5f;
        if (!text.isEmpty()) {
            ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), text, rect.x() + 4f * scale, textY, size, color, false);
        }
        if (active && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            float tw = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterRegular(), text, size);
            LayoutRender2D.rect(rect.x() + Math.min(tw + 5.5f * scale, rect.w() - 3f * scale), rect.y() + 3f * scale, 0.6f * scale, rect.h() - 6f * scale, palette.panelText());
        }
    }

    private void drawIconButton(Rect rect, String icon, float hover, boolean disabled, float scale, SettingsGuiPalette palette) {
        int bgA = disabled
                ? LayoutRender2D.alpha(palette.panelPillBase(), 0.42f)
                : SettingsGuiPalette.mix(palette.panelPillBase(), palette.menuCategoryHoverLeft(), 0.12f + hover * 0.22f);
        int bgB = disabled
                ? LayoutRender2D.alpha(palette.panelPillBase(), 0.32f)
                : SettingsGuiPalette.mix(palette.panelPillBase(), palette.menuCategoryHoverRight(), 0.12f + hover * 0.24f);
        LayoutRender2D.roundedQuad(rect.x(), rect.y(), rect.w(), rect.h(), 3.5f * scale, bgA, bgB, bgB, bgA);
        int iconColor = disabled ? LayoutRender2D.alpha(palette.panelMuted(), 0.58f) : palette.menuCategoryText();
        float iconSize = Math.min(rect.w(), rect.h()) - 7f * scale;
        Renderer2D.COLOR.svg(icon, rect.x() + (rect.w() - iconSize) * 0.5f, rect.y() + (rect.h() - iconSize) * 0.5f,
                iconSize, iconSize, SvgRenderOptions.overrideColor(iconColor));
    }

    private boolean switchTab(RelationTab next) {
        if (tab != next) {
            tab = next;
            activeField = ActiveField.NONE;
            resetScroll();
            if (onlinePicker.isVisible()) {
                onlinePicker.open(tab.pickerMode());
            }
            clearStatus();
        }
        return true;
    }

    private void commitPlayerInput() {
        String cleaned = ChatNameUtil.normalizeNickCandidate(playerInput);
        if (!ChatNameUtil.isNickLike(cleaned)) {
            setStatus(tr("status.invalid_nick", "Invalid nick"));
            return;
        }
        if (tab.add(cleaned)) {
            playerInput = "";
            activeField = ActiveField.NONE;
            movementInputBlocked = false;
            setStatus(tr("status.added", "Added: %s", cleaned));
        } else {
            setStatus(tr("status.already_exists", "Already exists: %s", cleaned));
        }
    }

    private List<String> filteredEntries(List<String> entries) {
        if (!ClickGuiSearch.isActive() || ClickGuiSearch.getText().isBlank()) return entries;
        String q = ClickGuiSearch.getText().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String entry : entries) {
            if (entry != null && entry.toLowerCase(Locale.ROOT).contains(q)) out.add(entry);
        }
        return out;
    }

    private static List<String> sorted(Set<String> src) {
        List<String> out = new ArrayList<>();
        if (src != null) {
            for (String value : src) {
                if (value != null && !value.isBlank()) out.add(value);
            }
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    private static boolean inside(RelationPlayerCardComponent.ActionHit hit, float mx, float my) {
        return hit != null && ClickGuiMath.insideRect(mx, my, hit.x(), hit.y(), hit.w(), hit.h());
    }

    private void appendToField(ActiveField field, String raw) {
        if (raw == null || raw.isEmpty()) return;
        String current = fieldText(field);
        int max = 32;
        String next = current + raw;
        if (next.length() > max) next = next.substring(0, max);
        setFieldText(field, next);
    }

    private String fieldText(ActiveField field) {
        return switch (field) {
            case PLAYER -> playerInput;
            default -> "";
        };
    }

    private void setFieldText(ActiveField field, String value) {
        String next = value == null ? "" : value;
        switch (field) {
            case PLAYER -> playerInput = next;
            default -> {
            }
        }
    }

    private static String dropLast(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, text.length() - 1);
    }

    private float updateHover(float current, boolean hovered, float dt) {
        return AnimationUtility.snap(AnimationUtility.approach(current, hovered ? 1f : 0f, dt, 12f), hovered ? 1f : 0f, 0.01f);
    }

    private void renderStatus(float x, float y, float w, float scale, SettingsGuiPalette palette) {
        if (statusMessage == null || statusMessage.isBlank()) return;
        if (System.currentTimeMillis() > statusUntilMs) {
            clearStatus();
            return;
        }
        float size = 6.8f * scale;
        String text = ClickGuiRenderer.fitText(ClickGuiRenderer.getInterRegular(), statusMessage, size, w - 8f * scale);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), text, x + 4f * scale, y + 2.6f * scale, size, palette.panelMuted(), false);
    }

    private void setStatus(String message) {
        statusMessage = message;
        statusUntilMs = System.currentTimeMillis() + STATUS_DURATION_MS;
    }

    private static String tr(String key, String fallback, Object... args) {
        return ClickGuiI18n.tr(I18N + key, fallback, args);
    }

    private void clearStatus() {
        statusMessage = null;
        statusUntilMs = 0L;
    }

    private void renderScrollbar(float x, float y, float w, float h, float maxScroll, float scale, SettingsGuiPalette palette) {
        updateScrollbarMetrics(x, y, w, h, maxScroll, scale);
        if (!scrollbarVisible) return;
        float mx = ClickGuiRenderer.getMouseX();
        float my = ClickGuiRenderer.getMouseY();
        if (isScrollbarHovered(mx, my) || draggingScrollbar) SystemCursor.set(SystemCursor.CursorType.SCROLL);

        LayoutRender2D.roundedQuad(scrollbarX, scrollbarY, scrollbarW, scrollbarH, 2f * scale,
                palette.moduleScrollTrackA(), palette.moduleScrollTrackB(), palette.moduleScrollTrackB(), palette.moduleScrollTrackA());
        LayoutRender2D.roundedQuad(scrollbarX, scrollbarThumbY, scrollbarW, scrollbarThumbH, 2f * scale,
                palette.moduleScrollHandleA(), palette.moduleScrollHandleB(), palette.moduleScrollHandleB(), palette.moduleScrollHandleA());
    }

    private void updateScrollbarMetrics(float x, float y, float w, float h, float maxScroll, float scale) {
        scrollbarVisible = maxScroll > 0.5f;
        scrollbarMaxScroll = Math.max(0f, maxScroll);
        if (!scrollbarVisible) {
            draggingScrollbar = false;
            scrollbarX = scrollbarY = scrollbarW = scrollbarH = scrollbarThumbY = scrollbarThumbH = 0f;
            return;
        }
        scrollbarW = 3f * scale;
        scrollbarX = x + w - scrollbarW - 2f * scale;
        scrollbarY = y + 3f * scale;
        scrollbarH = Math.max(1f, h - 6f * scale);
        scrollbarThumbH = Math.max(18f * scale, scrollbarH * (scrollbarH / (scrollbarMaxScroll + scrollbarH)));
        float scrollRatio = scrollbarMaxScroll <= 0f ? 0f : (-smoothedScroll / scrollbarMaxScroll);
        scrollbarThumbY = scrollbarY + (scrollbarH - scrollbarThumbH) * AnimationUtility.clamp(scrollRatio, 0f, 1f);
    }

    private boolean isScrollbarHovered(float mx, float my) {
        if (!scrollbarVisible) return false;
        float pad = 4f;
        return ClickGuiMath.insideRect(mx, my, scrollbarX - pad, scrollbarY - pad, scrollbarW + pad * 2f, scrollbarH + pad * 2f);
    }

    private void scrollToMouse(float my) {
        if (!scrollbarVisible || scrollbarMaxScroll <= 0f) return;
        float span = scrollbarH - scrollbarThumbH;
        if (span <= 0.5f) return;
        float thumbTop = AnimationUtility.clamp(my - scrollbarDragOffset, scrollbarY, scrollbarY + span);
        float ratio = (thumbTop - scrollbarY) / span;
        scroll = -scrollbarMaxScroll * AnimationUtility.clamp(ratio, 0f, 1f);
    }

    private enum ActiveField {
        NONE,
        PLAYER
    }

    private enum RelationTab {
        FRIENDS,
        ENEMIES,
        STAFF;

        private List<String> entries() {
            PlayerRelations rel = PlayerRelations.get();
            return switch (this) {
                case FRIENDS -> sorted(rel.getFriends());
                case ENEMIES -> sorted(rel.getEnemies());
                case STAFF -> sorted(rel.getStaff());
            };
        }

        private boolean add(String name) {
            PlayerRelations rel = PlayerRelations.get();
            boolean changed = switch (this) {
                case FRIENDS -> rel.addFriend(name);
                case ENEMIES -> rel.addEnemy(name);
                case STAFF -> rel.addStaff(name);
            };
            if (changed) rel.save();
            return changed;
        }

        private boolean remove(String name) {
            PlayerRelations rel = PlayerRelations.get();
            boolean changed = switch (this) {
                case FRIENDS -> rel.removeFriend(name);
                case ENEMIES -> rel.removeEnemy(name);
                case STAFF -> rel.removeStaff(name);
            };
            if (changed) rel.save();
            return changed;
        }

        private DefineTarget.RelationTargetMode pickerMode() {
            return switch (this) {
                case FRIENDS -> DefineTarget.RelationTargetMode.FRIEND;
                case ENEMIES -> DefineTarget.RelationTargetMode.ENEMY;
                case STAFF -> DefineTarget.RelationTargetMode.STAFF;
            };
        }
    }

    private record CardEntryHit(String name, CardHit hit) {
    }

    private record Rect(float x, float y, float w, float h) {
        static final Rect ZERO = new Rect(0f, 0f, 0f, 0f);

        boolean contains(float mx, float my) {
            return ClickGuiMath.insideRect(mx, my, x, y, w, h);
        }
    }
}
