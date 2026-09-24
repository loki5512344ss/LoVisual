package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.theme;

import dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiSearch;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Card drawing for {@link ThemeComponent}: theme picker card, delete-button,
 * "add theme" placeholder and the little gradient-chip strip that previews the
 * palette. State (per-swatch hover animation) is threaded in via the caller so
 * the class itself remains stateless.
 */
final class ThemeCardRenderer {

    /** Text of the "+ new theme" placeholder card. */
    static final String ADD_CARD_LABEL = "Добавить тему";
    /** Number of preview chips drawn under each card title. */
    static final int SWATCH_COUNT = 12;
    private static final float CARD_RADIUS = 5f;
    private static final float SELECTED_EPSILON = 0.001f;

    /** Reused across frames so {@link #drawSwatches} avoids three per-card allocations; single-threaded render, always refilled. */
    private static final Themes.GradientSpec[] GRADIENTS = new Themes.GradientSpec[SWATCH_COUNT];
    private static final int[] CHIP_START = new int[SWATCH_COUNT];
    private static final int[] CHIP_END = new int[SWATCH_COUNT];

    private ThemeCardRenderer() { }

    /** Theme entries matching the current search box (or all of them). */
    static List<Themes.ThemeEntry> filteredEntries() {
        List<Themes.ThemeEntry> all = Theme.themes();
        if (!ClickGuiSearch.isActive() || ClickGuiSearch.getText().isEmpty()) return all;
        List<Themes.ThemeEntry> out = new ArrayList<>();
        for (Themes.ThemeEntry entry : all) {
            if (ClickGuiSearch.matches(entry.name()) || ClickGuiSearch.matches(entry.getId())) out.add(entry);
        }
        return out;
    }

    /** Whether the "+ new theme" card matches the current search filter. */
    static boolean shouldShowAddCard() {
        if (!ClickGuiSearch.isActive() || ClickGuiSearch.getText().isEmpty()) return true;
        return ClickGuiSearch.matches(ADD_CARD_LABEL);
    }

    /** Delete-button geometry, shared between the drawer and the facade's hit rects. */
    static float deleteButtonSize(float scale) { return 12f * scale; }
    static float deleteButtonX(float x, float w, float scale) { return x + w - 16f * scale; }
    static float deleteButtonY(float y, float scale) { return y + 2.2f * scale; }

    /** Paint a single theme card, including its selection ring and swatch strip. */
    static void drawThemeCard(Themes.ThemeEntry entry,
                              float x, float y, float w, float h,
                              float mx, float my, float scale,
                              SettingsGuiPalette palette,
                              float selectedAnim,
                              Map<String, Float> swatchHoverByKey) {
        Themes.Theme t = entry.theme();
        float selected = AnimationUtility.easeInOutCubic(selectedAnim);
        if (selected > SELECTED_EPSILON) {
            LayoutRender2D.roundedSoftShadow(x - 0.8f * scale, y - 0.8f * scale, w + 1.6f * scale, h + 1.6f * scale,
                    5.8f * scale, (5.5f + 2.5f * selected) * scale, 0.025f * selected,
                    SettingsGuiPalette.withAlpha(t.accent(), Math.round(72f * selected)));
        }
        ClickGuiRenderer.drawBlur(x, y, w, h, CARD_RADIUS * scale, 0xFF000000, 200f / 255f);
        LayoutRender2D.roundedQuad(x, y, w, h, CARD_RADIUS * scale,
                palette.moduleCardTop(), palette.moduleCardTopStrong(),
                palette.moduleCardBottom(), palette.moduleCardBottomStrong());
        if (selected > SELECTED_EPSILON) drawSelectedOverlay(t, x, y, w, h, scale, selected);
        LayoutRender2D.rectQuad(x, y + 15.5f * scale, w, 0.5f * scale,
                SettingsGuiPalette.mix(palette.moduleDividerStart(), SettingsGuiPalette.withAlpha(t.accent(), 210), selected * 0.78f),
                SettingsGuiPalette.mix(palette.moduleDividerEnd(), SettingsGuiPalette.withAlpha(t.accentSoft(), 190), selected * 0.72f),
                SettingsGuiPalette.mix(palette.moduleDividerEnd(), SettingsGuiPalette.withAlpha(t.accentSoft(), 145), selected * 0.58f),
                SettingsGuiPalette.mix(palette.moduleDividerStart(), SettingsGuiPalette.withAlpha(t.accent(), 175), selected * 0.68f));

        boolean custom = !entry.builtin();
        float titleSize = 9.2f * scale;
        String title = ClickGuiRenderer.fitText(ClickGuiRenderer.getInterMedium(), entry.name(), titleSize, w - (custom ? 40f : 12f) * scale);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), title, x + 6f * scale, y + 3.8f * scale, titleSize,
                SettingsGuiPalette.mix(0xFFFFFFFF, SettingsGuiPalette.withAlpha(t.accent(), 255), selected * 0.24f), false);
        if (custom) {
            float tw = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterMedium(), title, titleSize);
            float iconX = Math.min(x + w - 28f * scale, x + 7f * scale + tw + 3f * scale);
            Renderer2D.COLOR.svg("paintbrush", iconX, y + 5.3f * scale, 7f * scale, 7f * scale,
                    SvgRenderOptions.overrideColor(SettingsGuiPalette.withAlpha(t.accent(), 230)));
            drawCardDeleteButton(x, y, w, mx, my, scale, palette);
        }
        float padX = 6f * scale;
        float chipsY = y + 20f * scale;
        float chipsH = Math.max(2f * scale, y + h - 5f * scale - chipsY);
        drawSwatches(entry, t, x + padX, chipsY, w - padX * 2f, chipsH, scale, mx, my, swatchHoverByKey);
    }

    /** Rounded fill + stroke overlay tinted by the theme accent, faded in via {@code selected}. */
    private static void drawSelectedOverlay(Themes.Theme t, float x, float y, float w, float h, float scale, float selected) {
        LayoutRender2D.roundedQuad(x, y, w, h, CARD_RADIUS * scale,
                SettingsGuiPalette.withAlpha(t.accent(), Math.round(82f * selected)),
                SettingsGuiPalette.withAlpha(t.accentSoft(), Math.round(48f * selected)),
                SettingsGuiPalette.withAlpha(t.accentSoft(), Math.round(24f * selected)),
                SettingsGuiPalette.withAlpha(t.accent(), Math.round(38f * selected)));
        LayoutRender2D.roundedStrokeQuad(x + 0.35f * scale, y + 0.35f * scale, w - 0.7f * scale, h - 0.7f * scale,
                4.7f * scale, (0.55f + 0.35f * selected) * scale,
                SettingsGuiPalette.withAlpha(t.accent(), Math.round(235f * selected)),
                SettingsGuiPalette.withAlpha(t.accentSoft(), Math.round(190f * selected)),
                SettingsGuiPalette.withAlpha(t.accentSoft(), Math.round(115f * selected)),
                SettingsGuiPalette.withAlpha(t.accent(), Math.round(180f * selected)));
    }

    /** Row of 12 tiny chips previewing the theme's key colours, with hover scale animation. */
    private static void drawSwatches(Themes.ThemeEntry entry, Themes.Theme t,
                                     float cx, float cy, float cw, float ch,
                                     float scale, float mx, float my,
                                     Map<String, Float> swatchHoverByKey) {
        fillSwatchPalette(entry, t);
        int columns = SWATCH_COUNT;
        float minGap = scale;
        float chipSize = Math.max(6f * scale, Math.min(Math.min(10.2f * scale, ch), (cw - minGap * (columns - 1)) / columns));
        float chipGap = Math.max(minGap, (cw - chipSize * columns) / (columns - 1));
        float hoverBoost = 0.75f * scale;
        float yCenterOffset = (ch - chipSize) * 0.5f;
        for (int i = 0; i < SWATCH_COUNT; i++) {
            float sx = cx + (i % columns) * (chipSize + chipGap);
            float sy = cy + yCenterOffset + (i / columns) * (chipSize + chipGap);
            boolean hover = ClickGuiMath.insideRect(mx, my, sx, sy, chipSize, chipSize);
            String key = entry.getId() + "#" + i;
            float anim = AnimationUtility.approach(swatchHoverByKey.getOrDefault(key, 0f), hover ? 1f : 0f, 0.20f);
            anim = AnimationUtility.snap(anim, hover ? 1f : 0f, 0.01f);
            swatchHoverByKey.put(key, anim);
            float size = chipSize + hoverBoost * anim;
            float pad = (size - chipSize) * 0.5f;
            drawThemeGradientRect(sx - pad, sy - pad, size, size, 2.3f * scale, GRADIENTS[i], CHIP_START[i], CHIP_END[i]);
        }
    }

    /** Re-fill the class-level chip buffers from the given theme; called once per visible card. */
    private static void fillSwatchPalette(Themes.ThemeEntry entry, Themes.Theme t) {
        Arrays.fill(GRADIENTS, null);
        GRADIENTS[0] = entry.windowGradient(); GRADIENTS[1] = entry.cardGradient();
        CHIP_START[0] = t.windowBg(); CHIP_START[1] = t.cardEnabled(); CHIP_START[2] = t.windowHeader();
        CHIP_START[3] = t.surface(); CHIP_START[4] = t.surfaceHover(); CHIP_START[5] = t.accent();
        CHIP_START[6] = t.accentSoft(); CHIP_START[7] = t.windowStroke(); CHIP_START[8] = t.strokeSoft();
        CHIP_START[9] = t.textPrimary(); CHIP_START[10] = t.textMuted(); CHIP_START[11] = t.windowBg();
        CHIP_END[0] = t.windowHeader(); CHIP_END[1] = t.accentSoft(); CHIP_END[2] = t.windowStroke();
        CHIP_END[3] = t.surfaceHover(); CHIP_END[4] = t.windowHeader(); CHIP_END[5] = t.accentSoft();
        CHIP_END[6] = t.strokeSoft(); CHIP_END[7] = t.strokeSoft(); CHIP_END[8] = t.textMuted();
        CHIP_END[9] = t.textMuted(); CHIP_END[10] = t.surface(); CHIP_END[11] = t.accentSoft();
    }

    /** Rounded rect filled with either the theme's gradient or a two-stop fallback. */
    private static void drawThemeGradientRect(float x, float y, float w, float h, float radius,
                                              Themes.GradientSpec gradient, int fallbackStart, int fallbackEnd) {
        boolean useGradient = gradient != null && gradient.enabled();
        int start = SettingsGuiPalette.withAlpha(useGradient ? gradient.start() : fallbackStart, 220);
        int end = SettingsGuiPalette.withAlpha(useGradient ? gradient.end() : fallbackEnd, 220);
        ClickGuiRenderer.drawRoundedRectGradient(x, y, w, h, radius, start, end, useGradient ? gradient.angleDeg() : 0f);
    }

    /** Small red "trash" affordance shown only on custom theme cards. */
    private static void drawCardDeleteButton(float x, float y, float w, float mx, float my, float scale, SettingsGuiPalette palette) {
        float size = deleteButtonSize(scale);
        float bx = deleteButtonX(x, w, scale);
        float by = deleteButtonY(y, scale);
        boolean hover = ClickGuiMath.insideRect(mx, my, bx, by, size, size);
        int bg = hover ? SettingsGuiPalette.withAlpha(0xFFE5484D, 64) : SettingsGuiPalette.withAlpha(palette.moduleCardBottomStrong(), 90);
        int stroke = hover ? SettingsGuiPalette.withAlpha(0xFFFF6B6F, 160) : SettingsGuiPalette.withAlpha(palette.moduleDividerEnd(), 80);
        LayoutRender2D.roundedQuad(bx, by, size, size, 3.5f * scale, bg, bg, bg, bg);
        LayoutRender2D.roundedStroke(bx, by, size, size, 3.5f * scale, 0.45f * scale, stroke);
        Renderer2D.COLOR.svg("trash-2", bx + 2.4f * scale, by + 2.3f * scale, 7.2f * scale, 7.2f * scale,
                SvgRenderOptions.overrideColor(SettingsGuiPalette.withAlpha(hover ? 0xFFFFC4C7 : 0xFFFFFFFF, hover ? 245 : 160)));
    }

    /** Dashed-looking placeholder card that opens the "create theme" flow when clicked. */
    static void drawAddCard(float x, float y, float w, float h, float scale, SettingsGuiPalette palette) {
        ClickGuiRenderer.drawBlur(x, y, w, h, CARD_RADIUS * scale, 0xFF000000, 200f / 255f);
        LayoutRender2D.roundedQuad(x, y, w, h, CARD_RADIUS * scale,
                palette.moduleCardTop(), palette.moduleCardTopStrong(),
                palette.moduleCardBottom(), palette.moduleCardBottomStrong());
        float plusSize = 11f * scale;
        float plusW = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterMedium(), "+", plusSize);
        float plusH = ClickGuiRenderer.textHeight(ClickGuiRenderer.getInterMedium(), plusSize);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), "+", x + (w - plusW) * 0.5f, y + 5.5f * scale, plusSize, palette.panelText(), false);
        float titleSize = 7.6f * scale;
        String title = ClickGuiRenderer.fitText(ClickGuiRenderer.getInterRegular(), ADD_CARD_LABEL, titleSize, w - 12f * scale);
        float tw = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterRegular(), title, titleSize);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), title, x + (w - tw) * 0.5f, y + h - plusH - 10f * scale, titleSize, palette.moduleDescriptionText(), false);
    }
}
