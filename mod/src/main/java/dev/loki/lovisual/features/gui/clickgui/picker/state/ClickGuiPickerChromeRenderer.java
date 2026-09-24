package dev.loki.lovisual.features.gui.clickgui.picker;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;

final class ClickGuiPickerChromeRenderer {
    private ClickGuiPickerChromeRenderer() {}

    static void drawWindow(ClickGuiPickerState s, SettingsGuiPalette p) {
        ClickGuiRenderer.drawBlur(s.windowX, s.windowY, s.windowW(), ClickGuiPickerState.WINDOW_H, 8f * ClickGuiPickerState.SCALE, 0xFF000000, 200f / 255f);
        LayoutRender2D.roundedSoftShadow(s.windowX, s.windowY, s.windowW(), ClickGuiPickerState.WINDOW_H, 8f * ClickGuiPickerState.SCALE, 8f * ClickGuiPickerState.SCALE, 0.018f, p.menuShadow());
        LayoutRender2D.roundedQuad(s.windowX, s.windowY, s.windowW(), ClickGuiPickerState.WINDOW_H, 8f * ClickGuiPickerState.SCALE, p.menuWindowBgLeft(), p.menuWindowBgRight(), p.menuWindowBgRight(), p.menuWindowBgLeft());
        LayoutRender2D.roundedStroke(s.windowX, s.windowY, s.windowW(), ClickGuiPickerState.WINDOW_H, 8f * ClickGuiPickerState.SCALE, 0.1f * ClickGuiPickerState.SCALE, p.menuWindowStroke());
    }

    static void drawHeader(ClickGuiPickerState s, SettingsGuiPalette p) {
        float sepY = s.windowY + ClickGuiPickerState.HEADER_H;
        LayoutRender2D.rectQuad(s.windowX + ClickGuiPickerState.PADDING, sepY, s.windowW() - ClickGuiPickerState.PADDING * 2f, 0.5f * ClickGuiPickerState.SCALE, p.menuLineStrong(), p.menuLineMid(), p.menuLineMid(), p.menuLineStrong());
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), s.title, s.windowX + ClickGuiPickerState.PADDING, s.windowY + 7f * ClickGuiPickerState.SCALE, 11f * ClickGuiPickerState.SCALE, p.menuHeaderText(), false);
        String count = s.owner.getValueSet().size() + " selected";
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), count, s.windowX + ClickGuiPickerState.PADDING, s.windowY + 15.5f * ClickGuiPickerState.SCALE, 8f * ClickGuiPickerState.SCALE, p.panelMuted(), false);
    }

    static void drawCloseButton(ClickGuiPickerState s, float mx, float my, SettingsGuiPalette p, Themes.Theme t) {
        boolean hover = ClickGuiPickerState.inside(mx, my, s.closeX, s.closeY, ClickGuiPickerState.CLOSE_SIZE, ClickGuiPickerState.CLOSE_SIZE);
        int base = hover ? p.panelPillActive() : p.panelPillBase();
        LayoutRender2D.rounded(s.closeX, s.closeY, ClickGuiPickerState.CLOSE_SIZE, ClickGuiPickerState.CLOSE_SIZE, 3.5f * ClickGuiPickerState.SCALE, base);
        LayoutRender2D.roundedStroke(s.closeX, s.closeY, ClickGuiPickerState.CLOSE_SIZE, ClickGuiPickerState.CLOSE_SIZE, 3.5f * ClickGuiPickerState.SCALE, 0.5f * ClickGuiPickerState.SCALE, p.panelStroke());
        float pad = 3f * ClickGuiPickerState.SCALE;
        float x1 = s.closeX + pad; float y1 = s.closeY + pad;
        float x2 = s.closeX + ClickGuiPickerState.CLOSE_SIZE - pad; float y2 = s.closeY + ClickGuiPickerState.CLOSE_SIZE - pad;
        int c = hover ? t.accent() : p.panelText();
        ClickGuiRenderer.drawLine(x1, y1, x2, y2, c);
        ClickGuiRenderer.drawLine(x2, y1, x1, y2, c);
    }

    static void drawSearchField(ClickGuiPickerState s, float mx, float my, SettingsGuiPalette p, Themes.Theme th) {
        boolean hover = ClickGuiPickerState.inside(mx, my, s.searchX, s.searchY, ClickGuiPickerState.SEARCH_W, ClickGuiPickerState.SEARCH_H);
        boolean active = s.listening;
        int bgL = p.panelBgLeft(); int bgR = p.panelBgRight();
        if (hover) { bgL = ClickGuiRenderer.mixColor(bgL, p.panelPillActive(), 0.18f); bgR = ClickGuiRenderer.mixColor(bgR, p.panelPillActive(), 0.18f); }
        if (active) { bgL = ClickGuiRenderer.mixColor(bgL, th.accentSoft(), 0.16f); bgR = ClickGuiRenderer.mixColor(bgR, th.accentSoft(), 0.16f); }
        ClickGuiRenderer.drawBlur(s.searchX, s.searchY, ClickGuiPickerState.SEARCH_W, ClickGuiPickerState.SEARCH_H, 3f * ClickGuiPickerState.SCALE, p.panelBlurTint(), 170f / 255f);
        LayoutRender2D.roundedQuad(s.searchX, s.searchY, ClickGuiPickerState.SEARCH_W, ClickGuiPickerState.SEARCH_H, 3f * ClickGuiPickerState.SCALE, bgL, bgR, bgR, bgL);
        LayoutRender2D.roundedStroke(s.searchX, s.searchY, ClickGuiPickerState.SEARCH_W, ClickGuiPickerState.SEARCH_H, 3f * ClickGuiPickerState.SCALE, 0.5f * ClickGuiPickerState.SCALE, p.panelStroke());
        float dividerX = s.searchX + ClickGuiPickerState.SEARCH_W - 13f * ClickGuiPickerState.SCALE;
        LayoutRender2D.rect(dividerX, s.searchY + 3f * ClickGuiPickerState.SCALE, 0.5f * ClickGuiPickerState.SCALE, ClickGuiPickerState.SEARCH_H - 6f * ClickGuiPickerState.SCALE, SettingsGuiPalette.withAlpha(p.panelMuted(), 95));
        TextRenderer font = ClickGuiRenderer.getInterRegular();
        float textSize = 7.5f * ClickGuiPickerState.SCALE;
        boolean empty = s.search.length() == 0;
        boolean caret = active && dev.loki.lovisual.render.engine.animation.AnimationUtility.blink(500L);
        String shown = empty && !active ? "Search" : s.search.toString();
        if (active && caret) shown += "|";
        int color = empty && !active ? p.panelMuted() : p.panelText();
        float textY = s.searchY + (ClickGuiPickerState.SEARCH_H - ClickGuiRenderer.textHeight(font, textSize)) * 0.5f;
        float textX = s.searchX + 4f * ClickGuiPickerState.SCALE;
        boolean clipped = ScissorFunction.pushRaw(textX, s.searchY + ClickGuiPickerState.SCALE, Math.max(1f, dividerX - textX - 1.5f * ClickGuiPickerState.SCALE), ClickGuiPickerState.SEARCH_H - 2f * ClickGuiPickerState.SCALE);
        ClickGuiRenderer.drawText(font, shown, textX, textY, textSize, color, false);
        if (clipped) ScissorFunction.pop();
        TextRenderer icons = Fonts.renderer("Icons", FontInfo.Type.Regular, ClickGuiRenderer.getInterRegular());
        String icon = "s"; float iconSize = 8f * ClickGuiPickerState.SCALE;
        float iconW = ClickGuiRenderer.textWidth(icons, icon, iconSize); float iconH = ClickGuiRenderer.textHeight(icons, iconSize);
        float iconX = dividerX + ((s.searchX + ClickGuiPickerState.SEARCH_W) - dividerX - iconW) * 0.5f;
        float iconY = s.searchY + (ClickGuiPickerState.SEARCH_H - iconH) * 0.5f - 0.35f * ClickGuiPickerState.SCALE;
        int iconColor = active ? p.panelText() : p.panelMuted();
        ClickGuiRenderer.drawText(icons, icon, iconX, iconY, iconSize, iconColor, false);
    }
}
