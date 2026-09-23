package dev.loki.lovisual.features.gui.clickgui.protocol;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.util.SystemCursor;
import dev.loki.lovisual.util.combat.protocol.heuristics.CombatProtocolHeuristicSource;
import dev.loki.lovisual.util.combat.protocol.heuristics.CombatProtocolHeuristicsConfig;

final class CombatProtocolRenderer {
    private CombatProtocolRenderer() {}

    static void render(CombatProtocolHeuristicsEditorState s, int width, int height) {
        float dt = AnimationUtility.deltaTime();
        s.openAnim = AnimationUtility.snap(AnimationUtility.approach(s.openAnim, 1f, dt, 8.5f), 1f, 0.008f);
        float eased = easeOutCubic(s.openAnim);
        s.windowX = (width - CombatProtocolHeuristicsEditorState.WINDOW_W) * 0.5f;
        s.windowY = (height - CombatProtocolHeuristicsEditorState.WINDOW_H) * 0.5f + (1f - eased) * 10f * CombatProtocolHeuristicsEditorState.SCALE;
        SettingsGuiPalette palette = SettingsGuiPalette.current();
        Themes.Theme theme = Theme.theme();
        TextRenderer regular = ClickGuiRenderer.getInterRegular();
        TextRenderer medium = ClickGuiRenderer.getInterMedium();
        float mx = ClickGuiRenderer.getMouseX();
        float my = ClickGuiRenderer.getMouseY();
        float previousAlpha = ClickGuiRenderer.setRenderAlphaMultiplier(eased);
        try {
            int shadow = SettingsGuiPalette.withAlpha(palette.menuShadow(), Math.round(255f * eased));
            ClickGuiRenderer.drawBlur(s.windowX, s.windowY, CombatProtocolHeuristicsEditorState.WINDOW_W, CombatProtocolHeuristicsEditorState.WINDOW_H, 9f * CombatProtocolHeuristicsEditorState.SCALE, 0xFF000000, 210f / 255f);
            ClickGuiRenderer.drawRoundedRectShadow(s.windowX, s.windowY, CombatProtocolHeuristicsEditorState.WINDOW_W, CombatProtocolHeuristicsEditorState.WINDOW_H, 9f * CombatProtocolHeuristicsEditorState.SCALE, 13f * CombatProtocolHeuristicsEditorState.SCALE, 1.5f * CombatProtocolHeuristicsEditorState.SCALE, shadow);
            ClickGuiRenderer.drawRoundedRectGradient(s.windowX, s.windowY, CombatProtocolHeuristicsEditorState.WINDOW_W, CombatProtocolHeuristicsEditorState.WINDOW_H, 9f * CombatProtocolHeuristicsEditorState.SCALE, palette.menuWindowBgLeft(), palette.menuWindowBgRight(), 0f);
            ClickGuiRenderer.drawRoundedRectStrokeGradient(s.windowX, s.windowY, CombatProtocolHeuristicsEditorState.WINDOW_W, CombatProtocolHeuristicsEditorState.WINDOW_H, 9f * CombatProtocolHeuristicsEditorState.SCALE, 0.55f * CombatProtocolHeuristicsEditorState.SCALE, SettingsGuiPalette.mix(palette.menuWindowStroke(), theme.accentSoft(), 0.18f), SettingsGuiPalette.mix(palette.menuWindowStroke(), theme.accent(), 0.10f), 0f);
            renderHeader(s, regular, medium, mx, my, palette, theme);
            float contentY = s.windowY + CombatProtocolHeuristicsEditorState.HEADER_H + CombatProtocolHeuristicsEditorState.PADDING;
            float contentH = CombatProtocolHeuristicsEditorState.WINDOW_H - CombatProtocolHeuristicsEditorState.HEADER_H - CombatProtocolHeuristicsEditorState.PADDING * 2f;
            renderSources(s, s.windowX + CombatProtocolHeuristicsEditorState.PADDING, contentY, CombatProtocolHeuristicsEditorState.SOURCE_W, contentH, regular, medium, mx, my, palette, theme);
            float detailsX = s.windowX + CombatProtocolHeuristicsEditorState.PADDING + CombatProtocolHeuristicsEditorState.SOURCE_W + CombatProtocolHeuristicsEditorState.GAP;
            float detailsW = CombatProtocolHeuristicsEditorState.WINDOW_W - CombatProtocolHeuristicsEditorState.PADDING * 2f - CombatProtocolHeuristicsEditorState.SOURCE_W - CombatProtocolHeuristicsEditorState.GAP;
            renderDetails(s, detailsX, contentY, detailsW, contentH, regular, medium, mx, my, palette, theme);
        } finally {
            ClickGuiRenderer.restoreRenderAlphaMultiplier(previousAlpha);
        }
    }

    private static void renderHeader(CombatProtocolHeuristicsEditorState s, TextRenderer regular, TextRenderer medium, float mx, float my, SettingsGuiPalette palette, Themes.Theme theme) {
        float accentX = s.windowX + CombatProtocolHeuristicsEditorState.PADDING;
        float accentY = s.windowY + 7f * CombatProtocolHeuristicsEditorState.SCALE;
        ClickGuiRenderer.drawRoundedRect(accentX, accentY, 2f * CombatProtocolHeuristicsEditorState.SCALE, 17f * CombatProtocolHeuristicsEditorState.SCALE, CombatProtocolHeuristicsEditorState.SCALE, theme.accent());
        ClickGuiRenderer.drawRoundedRectGlow(accentX, accentY, 2f * CombatProtocolHeuristicsEditorState.SCALE, 17f * CombatProtocolHeuristicsEditorState.SCALE, CombatProtocolHeuristicsEditorState.SCALE, 7f * CombatProtocolHeuristicsEditorState.SCALE, SettingsGuiPalette.withAlpha(theme.accent(), 90));
        ClickGuiRenderer.drawText(medium, s.title, accentX + 7f * CombatProtocolHeuristicsEditorState.SCALE, s.windowY + 5.5f * CombatProtocolHeuristicsEditorState.SCALE, 10.5f * CombatProtocolHeuristicsEditorState.SCALE, palette.menuHeaderText(), false);
        String subtitle = tr("subtitle", "Configure protocol signals independently for each source");
        ClickGuiRenderer.drawText(regular, subtitle, accentX + 7f * CombatProtocolHeuristicsEditorState.SCALE, s.windowY + 17f * CombatProtocolHeuristicsEditorState.SCALE, 6.3f * CombatProtocolHeuristicsEditorState.SCALE, palette.panelMuted(), false);
        float separatorY = s.windowY + CombatProtocolHeuristicsEditorState.HEADER_H;
        ClickGuiRenderer.drawGradientRect(s.windowX + CombatProtocolHeuristicsEditorState.PADDING, separatorY, CombatProtocolHeuristicsEditorState.WINDOW_W - CombatProtocolHeuristicsEditorState.PADDING * 2f, 0.5f * CombatProtocolHeuristicsEditorState.SCALE, palette.menuLineStrong(), palette.menuLineMid(), palette.menuLineMid(), palette.menuLineStrong());
        s.close = new CombatProtocolHeuristicsEditorState.Rect(s.windowX + CombatProtocolHeuristicsEditorState.WINDOW_W - CombatProtocolHeuristicsEditorState.PADDING - CombatProtocolHeuristicsEditorState.CLOSE_SIZE, s.windowY + 9f * CombatProtocolHeuristicsEditorState.SCALE, CombatProtocolHeuristicsEditorState.CLOSE_SIZE, CombatProtocolHeuristicsEditorState.CLOSE_SIZE);
        s.closeHover = animate(s.closeHover, s.close.contains(mx, my), 14f);
        int bg = SettingsGuiPalette.mix(palette.panelPillBase(), palette.panelPillActive(), 0.22f + s.closeHover * 0.58f);
        ClickGuiRenderer.drawRoundedRect(s.close.x(), s.close.y(), s.close.w(), s.close.h(), 3.5f * CombatProtocolHeuristicsEditorState.SCALE, bg);
        ClickGuiRenderer.drawRoundedRectStroke(s.close.x(), s.close.y(), s.close.w(), s.close.h(), 3.5f * CombatProtocolHeuristicsEditorState.SCALE, 0.45f * CombatProtocolHeuristicsEditorState.SCALE, SettingsGuiPalette.mix(palette.panelStroke(), theme.accentSoft(), s.closeHover * 0.45f));
        float pad = 3.2f * CombatProtocolHeuristicsEditorState.SCALE;
        int color = SettingsGuiPalette.mix(palette.panelMuted(), theme.accent(), s.closeHover);
        ClickGuiRenderer.drawLine(s.close.x() + pad, s.close.y() + pad, s.close.x() + s.close.w() - pad, s.close.y() + s.close.h() - pad, color);
        ClickGuiRenderer.drawLine(s.close.x() + s.close.w() - pad, s.close.y() + pad, s.close.x() + pad, s.close.y() + s.close.h() - pad, color);
        if (s.close.contains(mx, my)) SystemCursor.set(SystemCursor.CursorType.HAND);
    }

    private static void renderSources(CombatProtocolHeuristicsEditorState s, float x, float y, float w, float h, TextRenderer regular, TextRenderer medium, float mx, float my, SettingsGuiPalette palette, Themes.Theme theme) {
        s.sourceHits.clear();
        panel(x, y, w, h, palette);
        ClickGuiRenderer.drawText(medium, tr("sources", "Sources"), x + 8f * CombatProtocolHeuristicsEditorState.SCALE, y + 7f * CombatProtocolHeuristicsEditorState.SCALE, 7.2f * CombatProtocolHeuristicsEditorState.SCALE, palette.panelText(), false);
        ClickGuiRenderer.drawText(regular, tr("source_controls", "LMB toggle \u00b7 RMB configure"), x + 8f * CombatProtocolHeuristicsEditorState.SCALE, y + 16f * CombatProtocolHeuristicsEditorState.SCALE, 5.3f * CombatProtocolHeuristicsEditorState.SCALE, palette.panelMuted(), false);
        float rowY = y + 28f * CombatProtocolHeuristicsEditorState.SCALE;
        float rowH = 39f * CombatProtocolHeuristicsEditorState.SCALE;
        float rowGap = 5f * CombatProtocolHeuristicsEditorState.SCALE;
        for (CombatProtocolHeuristicSource source : CombatProtocolHeuristicSource.values()) {
            CombatProtocolHeuristicsEditorState.Rect card = new CombatProtocolHeuristicsEditorState.Rect(x + 5f * CombatProtocolHeuristicsEditorState.SCALE, rowY, w - 10f * CombatProtocolHeuristicsEditorState.SCALE, rowH);
            boolean hovered = card.contains(mx, my);
            boolean active = source == s.selected;
            boolean enabled = s.sourceEnabled(source);
            float hover = animate(s.sourceHover.get(source), hovered, 13f);
            float select = animate(s.sourceSelect.get(source), active, 11f);
            s.sourceHover.put(source, hover);
            s.sourceSelect.put(source, select);
            int baseLeft = SettingsGuiPalette.mix(palette.panelPillBase(), palette.menuCategoryHoverLeft(), hover * 0.30f);
            int baseRight = SettingsGuiPalette.mix(palette.panelPillBase(), palette.menuCategoryHoverRight(), hover * 0.30f);
            float accentMix = Math.max(enabled ? 0.42f : 0f, select * 0.32f);
            int left = SettingsGuiPalette.mix(baseLeft, theme.accentSoft(), accentMix);
            int right = SettingsGuiPalette.mix(baseRight, theme.accent(), accentMix * 0.72f);
            ClickGuiRenderer.drawRoundedRectGradient(card.x(), card.y(), card.w(), card.h(), 5.5f * CombatProtocolHeuristicsEditorState.SCALE, left, right, 0f);
            ClickGuiRenderer.drawRoundedRectStrokeGradient(card.x(), card.y(), card.w(), card.h(), 5.5f * CombatProtocolHeuristicsEditorState.SCALE, 0.5f * CombatProtocolHeuristicsEditorState.SCALE, SettingsGuiPalette.mix(palette.panelStroke(), theme.accentSoft(), Math.max(select, enabled ? 0.55f : 0f)), SettingsGuiPalette.mix(palette.panelStroke(), theme.accent(), Math.max(select * 0.7f, enabled ? 0.32f : 0f)), 0f);
            float markerW = (1.5f + 1.5f * select) * CombatProtocolHeuristicsEditorState.SCALE;
            int marker = enabled ? theme.accent() : SettingsGuiPalette.withAlpha(palette.panelMuted(), 100);
            ClickGuiRenderer.drawRoundedRect(card.x() + 3f * CombatProtocolHeuristicsEditorState.SCALE, card.y() + 7f * CombatProtocolHeuristicsEditorState.SCALE, markerW, card.h() - 14f * CombatProtocolHeuristicsEditorState.SCALE, markerW * 0.5f, marker);
            if (enabled) ClickGuiRenderer.drawRoundedRectGlow(card.x() + 3f * CombatProtocolHeuristicsEditorState.SCALE, card.y() + 7f * CombatProtocolHeuristicsEditorState.SCALE, markerW, card.h() - 14f * CombatProtocolHeuristicsEditorState.SCALE, markerW * 0.5f, 5f * CombatProtocolHeuristicsEditorState.SCALE, SettingsGuiPalette.withAlpha(theme.accent(), Math.round(55 + 55 * hover)));
            String name = sourceName(source);
            ClickGuiRenderer.drawText(medium, name, card.x() + 9f * CombatProtocolHeuristicsEditorState.SCALE, card.y() + 7f * CombatProtocolHeuristicsEditorState.SCALE, 7f * CombatProtocolHeuristicsEditorState.SCALE, palette.panelText(), false);
            String state = enabled ? tr("enabled", "Enabled") : tr("disabled", "Disabled");
            int stateColor = enabled ? theme.accent() : palette.panelMuted();
            ClickGuiRenderer.drawText(regular, state, card.x() + 9f * CombatProtocolHeuristicsEditorState.SCALE, card.y() + 20f * CombatProtocolHeuristicsEditorState.SCALE, 5.8f * CombatProtocolHeuristicsEditorState.SCALE, stateColor, false);
            int count = s.editor(source, CombatProtocolHeuristicsConfig.ProtocolFamily.LEGACY).patternCount() + s.editor(source, CombatProtocolHeuristicsConfig.ProtocolFamily.MODERN).patternCount();
            String countLabel = tr("rules_short", "%s rules", count);
            float countSize = 5.3f * CombatProtocolHeuristicsEditorState.SCALE;
            float countW = ClickGuiRenderer.textWidth(regular, countLabel, countSize);
            ClickGuiRenderer.drawText(regular, countLabel, card.x() + card.w() - 7f * CombatProtocolHeuristicsEditorState.SCALE - countW, card.y() + 20f * CombatProtocolHeuristicsEditorState.SCALE, countSize, palette.panelMuted(), false);
            s.sourceHits.add(new CombatProtocolHeuristicsEditorState.SourceHit(source, card));
            if (hovered) SystemCursor.set(SystemCursor.CursorType.HAND);
            rowY += rowH + rowGap;
        }
    }

    private static void renderDetails(CombatProtocolHeuristicsEditorState s, float x, float y, float w, float h, TextRenderer regular, TextRenderer medium, float mx, float my, SettingsGuiPalette palette, Themes.Theme theme) {
        panel(x, y, w, h, palette);
        boolean enabled = s.sourceEnabled(s.selected);
        ClickGuiRenderer.drawText(medium, sourceName(s.selected), x + 8f * CombatProtocolHeuristicsEditorState.SCALE, y + 6f * CombatProtocolHeuristicsEditorState.SCALE, 8.2f * CombatProtocolHeuristicsEditorState.SCALE, palette.panelText(), false);
        String sourceHint = tr("source_hint." + s.selected.key(), defaultSourceHint(s.selected));
        ClickGuiRenderer.drawText(regular, sourceHint, x + 8f * CombatProtocolHeuristicsEditorState.SCALE, y + 17f * CombatProtocolHeuristicsEditorState.SCALE, 5.8f * CombatProtocolHeuristicsEditorState.SCALE, palette.panelMuted(), false);
        String status = enabled ? tr("enabled", "Enabled") : tr("disabled", "Disabled");
        float statusSize = 5.7f * CombatProtocolHeuristicsEditorState.SCALE;
        float statusW = ClickGuiRenderer.textWidth(medium, status, statusSize) + 12f * CombatProtocolHeuristicsEditorState.SCALE;
        float statusX = x + w - 8f * CombatProtocolHeuristicsEditorState.SCALE - statusW;
        int statusBg = enabled ? SettingsGuiPalette.withAlpha(SettingsGuiPalette.mix(theme.accentSoft(), palette.panelPillActive(), 0.32f), 205) : palette.panelPillBase();
        ClickGuiRenderer.drawRoundedRect(statusX, y + 7f * CombatProtocolHeuristicsEditorState.SCALE, statusW, 11f * CombatProtocolHeuristicsEditorState.SCALE, 5.5f * CombatProtocolHeuristicsEditorState.SCALE, statusBg);
        ClickGuiRenderer.drawText(medium, status, statusX + 6f * CombatProtocolHeuristicsEditorState.SCALE, y + 9.6f * CombatProtocolHeuristicsEditorState.SCALE, statusSize, enabled ? theme.accent() : palette.panelMuted(), false);
        float headingH = 29f * CombatProtocolHeuristicsEditorState.SCALE;
        float footerH = 19f * CombatProtocolHeuristicsEditorState.SCALE;
        float editorGap = 6f * CombatProtocolHeuristicsEditorState.SCALE;
        float editorH = (h - headingH - footerH - editorGap * 2f) * 0.5f;
        float editorY = y + headingH;
        s.editor(s.selected, CombatProtocolHeuristicsConfig.ProtocolFamily.LEGACY).render(x + 6f * CombatProtocolHeuristicsEditorState.SCALE, editorY, w - 12f * CombatProtocolHeuristicsEditorState.SCALE, editorH, regular, medium, mx, my, palette, theme);
        editorY += editorH + editorGap;
        s.editor(s.selected, CombatProtocolHeuristicsConfig.ProtocolFamily.MODERN).render(x + 6f * CombatProtocolHeuristicsEditorState.SCALE, editorY, w - 12f * CombatProtocolHeuristicsEditorState.SCALE, editorH, regular, medium, mx, my, palette, theme);
        String resetLabel = tr("reset_source", "Reset source");
        float resetW = Math.max(58f * CombatProtocolHeuristicsEditorState.SCALE, ClickGuiRenderer.textWidth(regular, resetLabel, 5.8f * CombatProtocolHeuristicsEditorState.SCALE) + 14f * CombatProtocolHeuristicsEditorState.SCALE);
        s.reset = new CombatProtocolHeuristicsEditorState.Rect(x + w - 6f * CombatProtocolHeuristicsEditorState.SCALE - resetW, y + h - 15f * CombatProtocolHeuristicsEditorState.SCALE, resetW, 11f * CombatProtocolHeuristicsEditorState.SCALE);
        s.resetHover = animate(s.resetHover, s.reset.contains(mx, my), 13f);
        button(s.reset, resetLabel, s.resetHover, regular, palette, theme);
        if (s.reset.contains(mx, my)) SystemCursor.set(SystemCursor.CursorType.HAND);
        String hint = s.focusedEditor == null ? tr("edit_hint", "Click a pattern area to edit") : tr("keyboard_hint", "Ctrl+A/C/X/V \u00b7 Enter new line \u00b7 Esc unfocus");
        ClickGuiRenderer.drawText(regular, hint, x + 8f * CombatProtocolHeuristicsEditorState.SCALE, y + h - 12.3f * CombatProtocolHeuristicsEditorState.SCALE, 5.2f * CombatProtocolHeuristicsEditorState.SCALE, palette.panelMuted(), false);
    }

    static void panel(float x, float y, float w, float h, SettingsGuiPalette palette) {
        ClickGuiRenderer.drawBlur(x, y, w, h, 5f * CombatProtocolHeuristicsEditorState.SCALE, palette.panelBlurTint(), 175f / 255f);
        ClickGuiRenderer.drawRoundedRectGradient(x, y, w, h, 6f * CombatProtocolHeuristicsEditorState.SCALE, palette.panelBgLeft(), palette.panelBgRight(), 0f);
        ClickGuiRenderer.drawRoundedRectStroke(x, y, w, h, 6f * CombatProtocolHeuristicsEditorState.SCALE, 0.5f * CombatProtocolHeuristicsEditorState.SCALE, palette.panelStroke());
    }

    static void button(CombatProtocolHeuristicsEditorState.Rect rect, String label, float hover, TextRenderer font, SettingsGuiPalette palette, Themes.Theme theme) {
        int bg = SettingsGuiPalette.mix(palette.panelPillBase(), theme.accentSoft(), 0.08f + hover * 0.24f);
        ClickGuiRenderer.drawRoundedRect(rect.x(), rect.y(), rect.w(), rect.h(), 3.5f * CombatProtocolHeuristicsEditorState.SCALE, bg);
        ClickGuiRenderer.drawRoundedRectStroke(rect.x(), rect.y(), rect.w(), rect.h(), 3.5f * CombatProtocolHeuristicsEditorState.SCALE, 0.4f * CombatProtocolHeuristicsEditorState.SCALE, SettingsGuiPalette.mix(palette.panelStroke(), theme.accent(), hover * 0.42f));
        float size = 5.8f * CombatProtocolHeuristicsEditorState.SCALE;
        float tw = ClickGuiRenderer.textWidth(font, label, size);
        float th = ClickGuiRenderer.textHeight(font, size);
        ClickGuiRenderer.drawText(font, label, rect.x() + (rect.w() - tw) * 0.5f, rect.y() + (rect.h() - th) * 0.5f, size, SettingsGuiPalette.mix(palette.panelText(), theme.accent(), hover * 0.55f), false);
    }

    static float animate(float value, boolean active, float speed) {
        float target = active ? 1f : 0f;
        return AnimationUtility.snap(AnimationUtility.approach(value, target, AnimationUtility.deltaTime(), speed), target, 0.01f);
    }

    static float easeOutCubic(float value) {
        float t = CombatProtocolHeuristicsEditorState.clamp(value, 0f, 1f);
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    static String sourceName(CombatProtocolHeuristicSource source) {
        return ClickGuiI18n.tr("setting.common.combat.protocol_heuristics.option." + source.key(), switch (source) { case MESSAGES -> "Messages"; case OVERLAY -> "Overlay"; case BOSSBAR -> "Boss bar"; });
    }

    static String defaultSourceHint(CombatProtocolHeuristicSource source) {
        return switch (source) { case MESSAGES -> "Chat and system messages received from the server"; case OVERLAY -> "Action bar and overlay text"; case BOSSBAR -> "Visible boss bar titles"; };
    }

    static String tr(String suffix, String fallback, Object... args) {
        return ClickGuiI18n.tr("clickgui.protocol_heuristics." + suffix, fallback, args);
    }
}
