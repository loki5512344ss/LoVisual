package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.main;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.definition.SettingOwner;
import dev.loki.lovisual.config.style.ClickGuiLookConfig;
import dev.loki.lovisual.config.style.StyleConfig;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.look.ThemesPage;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.core.SettingFactory;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRenderContext;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRenderSurface;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Themes tab pages that edit how things look: the HUD (surface style, blur, headers) and the ClickGUI blur. */
public final class LookSettingsComponent {
    private final Map<ThemesPage, List<Setting>> settings = new EnumMap<>(ThemesPage.class);
    private final MainSettingsScroll scroll = new MainSettingsScroll();
    private final List<SettingHit> hits = new ArrayList<>();
    private ThemesPage page = ThemesPage.HUD;
    private float panelX, panelY, panelW, panelH;
    private float contentX, contentY, contentW, contentH;
    private float settingScale = 1f;

    public void resetScroll() {
        scroll.reset();
    }

    public void render(ThemesPage shown, float menuX, float menuY, float menuW, float menuH, float mx, float my, float s) {
        if (shown != page) {
            page = shown;
            scroll.reset();
        }
        SettingsGuiPalette palette = SettingsGuiPalette.current();
        settingScale = Math.max(1f, s * 0.66f);
        panelX = menuX + 31f * s;
        panelY = menuY + 33f * s;
        panelW = menuW - 42f * s;
        panelH = menuH - 39f * s;
        LayoutRender2D.roundedQuad(panelX, panelY, panelW, panelH, 5f * s, palette.moduleCardTop(), palette.moduleCardTopStrong(),
                palette.moduleCardBottom(), palette.moduleCardBottomStrong());
        LayoutRender2D.roundedStroke(panelX, panelY, panelW, panelH, 5f * s, 0.5f * s, palette.menuWindowStroke());
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), page.title(), panelX + 8f * s, panelY + 8f * s, 8f * s, palette.menuHeaderText(), false);

        contentX = panelX + 8f * s;
        contentY = panelY + 27f * s;
        contentW = Math.max(1f, panelW - 16f * s - (scroll.needsScrollbar() ? 7f * s : 0f));
        contentH = Math.max(1f, panelH - 35f * s);
        List<Setting> list = settings.computeIfAbsent(page, LookSettingsComponent::build);

        float total = 0f;
        for (Setting setting : list) total += setting.getHeight() * setting.updateVisibilityAnim() + 6f * setting.getVisibilityAnim();
        scroll.layout(panelX, panelW, contentY, contentH, total, my, s);

        hits.clear();
        boolean clipped = ScissorFunction.pushRaw(contentX, contentY, contentW, contentH);
        float y = contentY + scroll.smoothed();
        try (SettingRenderContext.Scope ignored = SettingRenderContext.push(SettingRenderSurface.SETTINGS, settingScale)) {
            for (Setting setting : list) {
                float anim = setting.getVisibilityAnim();
                if (!setting.isVisibilityTargetVisible() && anim <= 0.01f) continue;
                float h = setting.getHeight() * anim;
                if (h > 0.5f) {
                    hits.add(new SettingHit(setting, contentX, y, contentW, h));
                    if (y + h >= contentY && y <= contentY + contentH) setting.render(contentX, y, contentW, mx, my);
                }
                y += h + 6f * anim;
            }
        }
        if (clipped) ScissorFunction.pop();
        scroll.render(s, palette);
    }

    public boolean mousePressedScrollbar(float mx, float my, int button) {
        return scroll.press(mx, my, button);
    }

    public boolean mouseClicked(float mx, float my, int button) {
        if (button != 0 && button != 1) return false;
        if (scroll.isHovered(mx, my)) return true;
        if (!ClickGuiMath.insideRect(mx, my, contentX, contentY, contentW, contentH)) return false;
        try (SettingRenderContext.Scope ignored = SettingRenderContext.push(SettingRenderSurface.SETTINGS, settingScale)) {
            for (SettingHit hit : hits) {
                if (!ClickGuiMath.insideRect(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) continue;
                hit.setting().mouseClicked(mx, my, button);
                return true;
            }
        }
        return true;
    }

    public void mouseReleased(float mx, float my, int button) {
        scroll.release(button);
        List<Setting> list = settings.get(page);
        if (list == null) return;
        try (SettingRenderContext.Scope ignored = SettingRenderContext.push(SettingRenderSurface.SETTINGS, settingScale)) {
            for (Setting setting : list) setting.mouseReleased(mx, my, button);
        }
    }

    public void scroll(float mx, float my, double amount) {
        if (ClickGuiMath.insideRect(mx, my, panelX, panelY, panelW, panelH)) scroll.addWheel(amount);
    }

    private static List<Setting> build(ThemesPage page) {
        List<Setting> out = new ArrayList<>();
        if (page == ThemesPage.HUD) {
            add(out, StyleConfig.get().getSettingDefs(), StyleConfig.get());
            add(out, HudGlobalConfig.get().getLookSettingDefs(), HudGlobalConfig.get());
        } else {
            add(out, ClickGuiLookConfig.get().getSettingDefs(), ClickGuiLookConfig.get());
        }
        return out;
    }

    private static void add(List<Setting> out, List<SettingDef> defs, SettingOwner owner) {
        for (Setting setting : SettingFactory.fromDefs(defs)) {
            setting.setParent(owner);
            setting.preflightI18n();
            out.add(setting);
        }
    }
}
