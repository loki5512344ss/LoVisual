package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.config;

import dev.loki.lovisual.config.profile.codec.ConfigProfileType;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.helpers.util.SystemCursor;

final class ConfigProfilesToolbarRenderer {
    private final ConfigProfilesState state;

    private ConfigProfilesState.Rect modulesPill = ConfigProfilesState.Rect.ZERO;
    private ConfigProfilesState.Rect hudPill = ConfigProfilesState.Rect.ZERO;
    private ConfigProfilesState.Rect themesPill = ConfigProfilesState.Rect.ZERO;
    private ConfigProfilesState.Rect saveButton = ConfigProfilesState.Rect.ZERO;

    private float modeAnim;
    private float modulesHoverAnim;
    private float hudHoverAnim;
    private float themesHoverAnim;
    private float saveHoverAnim;
    private float savePulseAnim;

    ConfigProfilesToolbarRenderer(ConfigProfilesState state) {
        this.state = state;
    }

    ConfigProfilesState.Rect modulesPill() { return modulesPill; }
    ConfigProfilesState.Rect hudPill() { return hudPill; }
    ConfigProfilesState.Rect themesPill() { return themesPill; }
    ConfigProfilesState.Rect saveButton() { return saveButton; }

    void pulseSave() { savePulseAnim = 1f; }

    void render(float x, float y, float w, float mx, float my, float scale, SettingsGuiPalette palette) {
        float pillW = 132f * scale;
        float pillH = 15f * scale;
        float targetMode = switch (state.type()) {
            case MODULES -> 0f;
            case HUD -> 1f;
            case THEMES -> 2f;
        };
        float dt = AnimationUtility.deltaTime();
        modeAnim = AnimationUtility.approach(modeAnim, targetMode, dt, 12f);
        modeAnim = AnimationUtility.snap(modeAnim, targetMode, 0.01f);

        modulesPill = new ConfigProfilesState.Rect(x, y, pillW / 3f, pillH);
        hudPill = new ConfigProfilesState.Rect(x + pillW / 3f, y, pillW / 3f, pillH);
        themesPill = new ConfigProfilesState.Rect(x + pillW * 2f / 3f, y, pillW / 3f, pillH);

        modulesHoverAnim = updateHover(modulesHoverAnim, modulesPill.contains(mx, my), dt);
        hudHoverAnim = updateHover(hudHoverAnim, hudPill.contains(mx, my), dt);
        themesHoverAnim = updateHover(themesHoverAnim, themesPill.contains(mx, my), dt);

        int baseLeft = SettingsGuiPalette.mix(palette.panelPillBase(), palette.panelBgLeft(), 0.34f);
        int baseRight = SettingsGuiPalette.mix(palette.panelPillBase(), palette.panelBgRight(), 0.44f);
        float anyHover = Math.max(modulesHoverAnim, Math.max(hudHoverAnim, themesHoverAnim));
        baseLeft = SettingsGuiPalette.mix(baseLeft, palette.menuCategoryHoverLeft(), 0.12f * anyHover);
        baseRight = SettingsGuiPalette.mix(baseRight, palette.menuCategoryHoverRight(), 0.12f * anyHover);

        ClickGuiRenderer.drawBlur(x, y, pillW, pillH, 4f * scale, palette.panelBlurTint(), 145f / 255f);
        LayoutRender2D.roundedQuad(x, y, pillW, pillH, 3.5f * scale, baseLeft, baseRight, baseRight, baseLeft);
        LayoutRender2D.roundedStrokeQuad(x, y, pillW, pillH, 3.5f * scale, 0.5f * scale,
                SettingsGuiPalette.mix(palette.panelStroke(), palette.moduleDividerStart(), 0.18f),
                SettingsGuiPalette.mix(palette.panelStroke(), palette.moduleDividerEnd(), 0.18f),
                SettingsGuiPalette.mix(palette.panelStroke(), palette.moduleDividerEnd(), 0.18f),
                SettingsGuiPalette.mix(palette.panelStroke(), palette.moduleDividerStart(), 0.18f));

        float pad = 1.2f * scale;
        float segment = (pillW - pad * 2f) / 3f;
        float activeX = x + pad + segment * modeAnim;
        float activeHover = switch (state.type()) {
            case MODULES -> modulesHoverAnim;
            case HUD -> hudHoverAnim;
            case THEMES -> themesHoverAnim;
        };
        int activeA = SettingsGuiPalette.mix(palette.panelPillActive(), palette.menuCategorySelectedLeft(), 0.22f + activeHover * 0.08f);
        int activeB = SettingsGuiPalette.mix(palette.panelPillActive(), palette.menuCategorySelectedRight(), 0.22f + activeHover * 0.12f);
        int activeC = SettingsGuiPalette.mix(SettingsGuiPalette.darken(palette.panelPillActive(), 0.16f), palette.menuCategorySelectedRight(), 0.18f + activeHover * 0.10f);
        LayoutRender2D.roundedQuad(activeX, y + pad, segment, pillH - pad * 2f, 3f * scale, activeA, activeB, activeC, activeA);
        LayoutRender2D.roundedStrokeQuad(activeX, y + pad, segment, pillH - pad * 2f, 3f * scale, 0.35f * scale,
                SettingsGuiPalette.withAlpha(palette.moduleDividerStart(), 150),
                SettingsGuiPalette.withAlpha(palette.moduleDividerEnd(), 178),
                SettingsGuiPalette.withAlpha(palette.moduleDividerEnd(), 142),
                SettingsGuiPalette.withAlpha(palette.moduleDividerStart(), 150));

        drawToolbarSeparator(x + pad + segment, y, pillH, scale, palette);
        drawToolbarSeparator(x + pad + segment * 2f, y, pillH, scale, palette);

        drawSegment(modulesPill, ClickGuiI18n.tr("clickgui.settings.config.type.modules", "Modules"), state.type() == ConfigProfileType.MODULES, modulesHoverAnim, scale, palette);
        drawSegment(hudPill, ClickGuiI18n.tr("clickgui.settings.config.type.hud", "Hud"), state.type() == ConfigProfileType.HUD, hudHoverAnim, scale, palette);
        drawSegment(themesPill, ClickGuiI18n.tr("clickgui.settings.config.type.themes", "Themes"), state.type() == ConfigProfileType.THEMES, themesHoverAnim, scale, palette);

        float saveW = 68f * scale;
        saveButton = new ConfigProfilesState.Rect(x + pillW + 7f * scale, y, saveW, pillH);
        boolean saveHover = saveButton.contains(mx, my);
        saveHoverAnim = updateHover(saveHoverAnim, saveHover, dt);
        savePulseAnim = AnimationUtility.snap(AnimationUtility.approach(savePulseAnim, 0f, dt, 7f), 0f, 0.01f);
        renderSaveButton(saveButton, saveHoverAnim, savePulseAnim, scale, palette);
        if (saveHover) SystemCursor.set(SystemCursor.CursorType.MOVE);
    }

    private float updateHover(float current, boolean hovered, float dt) {
        return AnimationUtility.snap(AnimationUtility.approach(current, hovered ? 1f : 0f, dt, 12f), hovered ? 1f : 0f, 0.01f);
    }

    private void drawToolbarSeparator(float x, float y, float h, float scale, SettingsGuiPalette palette) {
        float backingW = Math.max(1.25f * scale, 1.05f);
        float backingH = h - 4.4f * scale;
        float backingY = y + (h - backingH) * 0.5f;
        int backingTop = SettingsGuiPalette.withAlpha(palette.panelBgLeft(), 98);
        int backingBottom = SettingsGuiPalette.withAlpha(palette.panelBgRight(), 78);
        LayoutRender2D.roundedQuad(x - backingW * 0.5f, backingY, backingW, backingH, backingW * 0.5f, backingTop, backingTop, backingBottom, backingBottom);
        float lineW = Math.max(0.5f * scale, 0.75f);
        float lineH = h - 6.0f * scale;
        float lineY = y + (h - lineH) * 0.5f;
        int top = SettingsGuiPalette.withAlpha(SettingsGuiPalette.mix(palette.moduleDividerStart(), palette.menuCategorySelectedLeft(), 0.34f), 146);
        int mid = SettingsGuiPalette.withAlpha(SettingsGuiPalette.mix(palette.moduleDividerEnd(), palette.menuCategorySelectedRight(), 0.40f), 210);
        int bottom = SettingsGuiPalette.withAlpha(SettingsGuiPalette.mix(palette.moduleDividerEnd(), palette.panelText(), 0.24f), 136);
        LayoutRender2D.roundedQuad(x - lineW * 0.5f, lineY, lineW, lineH, lineW * 0.5f, top, mid, bottom, top);
    }

    private void renderSaveButton(ConfigProfilesState.Rect rect, float hover, float pulse, float scale, SettingsGuiPalette palette) {
        float lift = (hover * 0.65f + pulse * 0.85f) * scale;
        float x = rect.x();
        float y = rect.y() - lift * 0.15f;
        float w = rect.w();
        float h = rect.h();
        int bgA = SettingsGuiPalette.mix(palette.panelPillBase(), palette.menuCategoryHoverLeft(), 0.18f + hover * 0.22f + pulse * 0.24f);
        int bgB = SettingsGuiPalette.mix(palette.panelPillBase(), palette.menuCategoryHoverRight(), 0.16f + hover * 0.24f + pulse * 0.26f);
        int bgC = SettingsGuiPalette.mix(SettingsGuiPalette.darken(palette.panelPillBase(), 0.18f), palette.menuCategorySelectedRight(), 0.12f + hover * 0.18f + pulse * 0.24f);
        LayoutRender2D.roundedSoftShadow(x, y + 0.4f * scale, w, h, 3.6f * scale, 5f * scale, 0.02f + hover * 0.03f,
                SettingsGuiPalette.withAlpha(palette.menuShadow(), Math.round(65f + hover * 50f + pulse * 58f)));
        LayoutRender2D.roundedQuad(x, y, w, h, 3.6f * scale, bgA, bgB, bgC, bgA);
        LayoutRender2D.roundedStrokeQuad(x, y, w, h, 3.6f * scale, (0.45f + hover * 0.22f + pulse * 0.22f) * scale,
                SettingsGuiPalette.withAlpha(palette.moduleDividerStart(), Math.round(138f + hover * 45f + pulse * 50f)),
                SettingsGuiPalette.withAlpha(palette.moduleDividerEnd(), Math.round(158f + hover * 55f + pulse * 55f)),
                SettingsGuiPalette.withAlpha(palette.moduleDividerEnd(), Math.round(120f + hover * 45f + pulse * 50f)),
                SettingsGuiPalette.withAlpha(palette.moduleDividerStart(), Math.round(138f + hover * 45f + pulse * 50f)));
        int iconColor = SettingsGuiPalette.mix(palette.panelText(), palette.menuCategoryText(), 0.22f + hover * 0.34f + pulse * 0.34f);
        Renderer2D.COLOR.svg("save", x + 6.2f * scale, y + 4f * scale, 7f * scale, 7f * scale, SvgRenderOptions.overrideColor(iconColor));
        String label = ClickGuiI18n.tr("clickgui.settings.config.save", "Save");
        float textSize = 6.7f * scale;
        int textColor = SettingsGuiPalette.mix(palette.panelText(), palette.menuCategoryText(), hover * 0.24f + pulse * 0.24f);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), label, x + 17.4f * scale, y + 4.2f * scale, textSize, textColor, false);
    }

    private void drawSegment(ConfigProfilesState.Rect rect, String label, boolean active, float hover, float scale, SettingsGuiPalette palette) {
        float size = 7.5f * scale;
        float tw = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterMedium(), label, size);
        float th = ClickGuiRenderer.textHeight(ClickGuiRenderer.getInterMedium(), size);
        int color = active ? palette.panelText() : SettingsGuiPalette.mix(palette.panelMuted(), palette.panelText(), 0.20f * hover);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), label, rect.x() + (rect.w() - tw) * 0.5f, rect.y() + (rect.h() - th) * 0.5f, size, color, false);
    }
}
