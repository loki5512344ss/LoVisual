package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.main;

import dev.loki.lovisual.config.definition.SettingOwner;
import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.style.StyleConfig;
import dev.loki.lovisual.features.platform.PlatformConfig;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.core.SettingFactory;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRenderContext;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRenderSurface;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MainSettingsComponent {
    private final Map<Category, List<Setting>> settingsByCategory = new EnumMap<>(Category.class);
    private final List<SettingRow> rows = new ArrayList<>();
    private final List<SettingHit> settingHits = new ArrayList<>();
    private final List<CategoryHit> categoryHits = new ArrayList<>();
    private final Map<Category, Float> categoryHoverAnim = new EnumMap<>(Category.class);
    private final MainSettingsScroll scrollState = new MainSettingsScroll();
    private Category selected = Category.IMAGE;
    private float rightX;
    private float rightY;
    private float rightW;
    private float rightH;
    private float contentX;
    private float contentY;
    private float contentW;
    private float contentH;
    private float lastSettingScale = 1.0f;
    public MainSettingsComponent() {
        rebuildSettings();
    }

    private static List<Setting> buildSettings(List<SettingDef> defs, SettingOwner owner) {
        List<Setting> out = SettingFactory.fromDefs(defs);
        for (Setting setting : out) {
            setting.setParent(owner);
            setting.preflightI18n();
        }
        return out;
    }

    private static float settingScale(float menuScale) {
        return Math.max(1.0f, menuScale * 0.66f);
    }

    public void resetScroll() {
        scrollState.reset();
    }

    public void render(float menuX, float menuY, float menuW, float menuH, float mx, float my, float scale) {
        ensureSettings();
        lastSettingScale = settingScale(scale);
        SettingsGuiPalette palette = SettingsGuiPalette.current();

        float areaX = menuX + 31f * scale;
        float areaY = menuY + 33f * scale;
        float areaW = menuW - 42f * scale;
        float areaH = menuH - 39f * scale;
        float gap = 8f * scale;
        float leftW = 112f * scale;

        float leftX = areaX;
        float leftY = areaY;
        float leftH = areaH;
        rightX = leftX + leftW + gap;
        rightY = areaY;
        rightW = Math.max(1f, areaW - leftW - gap);
        rightH = areaH;

        drawPanel(leftX, leftY, leftW, leftH, scale, palette);
        drawPanel(rightX, rightY, rightW, rightH, scale, palette);
        renderCategoryList(leftX, leftY, leftW, mx, my, scale, palette);
        renderSettingsPanel(mx, my, scale, palette);
    }

    public boolean mousePressedScrollbar(float mx, float my, int button) {
        return scrollState.press(mx, my, button);
    }

    public boolean mouseClicked(float mx, float my, int button) {
        if (button != 0 && button != 1) return false;
        if (scrollState.isHovered(mx, my)) return true;

        for (CategoryHit hit : categoryHits) {
            if (!ClickGuiMath.insideRect(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) continue;
            if (hit.category() != selected) {
                selected = hit.category();
                resetScroll();
            }
            return true;
        }

        if (!ClickGuiMath.insideRect(mx, my, rightX, rightY, rightW, rightH)) return false;
        if (ClickGuiMath.insideRect(mx, my, contentX, contentY, contentW, contentH)) {
            try (SettingRenderContext.Scope ignored = SettingRenderContext.push(SettingRenderSurface.SETTINGS, lastSettingScale)) {
                for (SettingHit hit : settingHits) {
                    if (!ClickGuiMath.insideRect(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) continue;
                    hit.setting().mouseClicked(mx, my, button);
                    return true;
                }
            }
        }
        return true;
    }

    public void mouseReleased(float mx, float my, int button) {
        scrollState.release(button);
        List<Setting> settings = settingsByCategory.get(selected);
        if (settings == null) return;
        try (SettingRenderContext.Scope ignored = SettingRenderContext.push(SettingRenderSurface.SETTINGS, lastSettingScale)) {
            for (Setting setting : settings) {
                setting.mouseReleased(mx, my, button);
            }
        }
    }

    public void scroll(float mx, float my, double amount) {
        if (!ClickGuiMath.insideRect(mx, my, rightX, rightY, rightW, rightH)) return;
        scrollState.addWheel(amount);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        return false;
    }

    private void renderCategoryList(float x, float y, float w, float mx, float my, float scale, SettingsGuiPalette palette) {
        categoryHits.clear();
        float pad = 7f * scale;
        float rowH = 20f * scale;
        float rowY = y + 9f * scale;

        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            Category category = categories[i];
            boolean active = category == selected;
            boolean hover = ClickGuiMath.insideRect(mx, my, x + pad, rowY, w - pad * 2f, rowH);
            float hoverAnim = AnimationUtility.approach(categoryHoverAnim.getOrDefault(category, 0f), hover ? 1f : 0f, 0.20f);
            categoryHoverAnim.put(category, hoverAnim);
            int bg = active
                    ? LayoutRender2D.alpha(palette.menuCategorySelectedRight(), 0.86f)
                    : hoverAnim > 0.01f ? LayoutRender2D.alpha(palette.menuCategoryHoverRight(), 0.72f * hoverAnim) : 0;
            if (bg != 0) {
                LayoutRender2D.roundedQuad(x + pad, rowY, w - pad * 2f, rowH, 5f * scale, bg, bg, bg, bg);
            }

            if (i < categories.length - 1) {
                renderCategorySeparator(x, rowY + rowH + 2f * scale, w, scale, palette);
            }

            int text = active ? palette.menuHeaderText() : LayoutRender2D.alpha(palette.panelMuted(), 0.72f + 0.20f * hoverAnim);
            ClickGuiRenderer.drawText(
                    ClickGuiRenderer.getInterMedium(),
                    category.title,
                    x + 13f * scale,
                    rowY + 6f * scale,
                    7f * scale,
                    text,
                    false
            );
            categoryHits.add(new CategoryHit(category, x + pad, rowY, w - pad * 2f, rowH));
            rowY += 24f * scale;
        }
    }

    private void renderCategorySeparator(float x, float y, float w, float scale, SettingsGuiPalette palette) {
        float sx = x + 12f * scale;
        float sw = Math.max(1f, w - 24f * scale);
        LayoutRender2D.rectQuad(
                sx,
                y,
                sw,
                0.5f * scale,
                LayoutRender2D.alpha(palette.menuLineLow(), 0.24f),
                LayoutRender2D.alpha(palette.menuLineStrong(), 0.34f),
                LayoutRender2D.alpha(palette.menuLineStrong(), 0.34f),
                LayoutRender2D.alpha(palette.menuLineLow(), 0.24f)
        );
    }

    private void renderSettingsPanel(float mx, float my, float scale, SettingsGuiPalette palette) {
        buildRows();
        float pad = 8f * scale;
        float titleY = rightY + 8f * scale;
        float titleSize = 8f * scale;
        int text = palette.menuHeaderText();

        LayoutRender2D.rectQuad(
                rightX + pad,
                titleY + 11f * scale,
                rightW - pad * 2f,
                0.5f * scale,
                LayoutRender2D.alpha(palette.menuLineLow(), 0.55f),
                LayoutRender2D.alpha(palette.menuLineStrong(), 0.82f),
                LayoutRender2D.alpha(palette.menuLineStrong(), 0.82f),
                LayoutRender2D.alpha(palette.menuLineLow(), 0.55f)
        );
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), selected.title, rightX + pad, titleY, titleSize, text, false);

        contentX = rightX + pad;
        contentY = rightY + 27f * scale;
        contentW = Math.max(1f, rightW - pad * 2f - (scrollState.needsScrollbar() ? 7f * scale : 0f));
        contentH = Math.max(1f, rightH - 35f * scale);

        float totalH = totalRowsHeight();
        scrollState.layout(rightX, rightW, contentY, contentH, totalH, my, scale);

        settingHits.clear();
        boolean clipped = ScissorFunction.pushRaw(contentX, contentY, contentW, contentH);
        float y = contentY + scrollState.smoothed();
        try (SettingRenderContext.Scope ignored = SettingRenderContext.push(SettingRenderSurface.SETTINGS, lastSettingScale)) {
            for (SettingRow row : rows) {
                float h = row.height();
                if (h > 0.5f) {
                    settingHits.add(new SettingHit(row.setting(), contentX, y, contentW, h));
                    if (y + h >= contentY - 1f * scale && y <= contentY + contentH + 1f * scale) {
                        row.setting().render(contentX, y, contentW, mx, my);
                    }
                }
                y += h + row.gap();
            }
        }
        if (clipped) ScissorFunction.pop();

        scrollState.render(scale, palette);
    }

    private void buildRows() {
        rows.clear();
        List<Setting> settings = settingsByCategory.get(selected);
        if (settings == null) return;
        try (SettingRenderContext.Scope ignored = SettingRenderContext.push(SettingRenderSurface.SETTINGS, lastSettingScale)) {
            for (Setting setting : settings) {
                float anim = setting.updateVisibilityAnim();
                boolean targetVisible = setting.isVisibilityTargetVisible();
                if (!targetVisible && anim <= 0.01f) continue;
                rows.add(new SettingRow(setting, anim, setting.getHeight() * anim, 6f * anim));
            }
        }
    }

    private float totalRowsHeight() {
        float out = 0f;
        for (SettingRow row : rows) {
            out += row.height() + row.gap();
        }
        return out;
    }

    private void drawPanel(float x, float y, float w, float h, float scale, SettingsGuiPalette palette) {
        ClickGuiRenderer.drawBlur(x, y, w, h, 5f * scale, 0xFF000000, 200f / 255f);
        LayoutRender2D.roundedQuad(
                x,
                y,
                w,
                h,
                5f * scale,
                palette.moduleCardTop(),
                palette.moduleCardTopStrong(),
                palette.moduleCardBottom(),
                palette.moduleCardBottomStrong()
        );
        LayoutRender2D.roundedStroke(x, y, w, h, 5f * scale, 0.5f * scale, palette.menuWindowStroke());
    }

    private void ensureSettings() {
        if (!settingsByCategory.isEmpty()) return;
        rebuildSettings();
    }

    private void rebuildSettings() {
        settingsByCategory.clear();
        MainConfig config = MainConfig.get();
        settingsByCategory.put(Category.IMAGE, buildSettings(config.getImageSettingDefs(), config));
        settingsByCategory.put(Category.MISCELLANIOUS, buildSettings(config.getMiscellaneousSettingDefs(), config));
        settingsByCategory.put(Category.SECURITY, buildSettings(config.getSecuritySettingDefs(), config));
        List<Setting> utility = buildSettings(config.getUtilitySettingDefs(), config);
        utility.addAll(buildSettings(StyleConfig.get().getSettingDefs(), StyleConfig.get()));
        utility.addAll(buildSettings(PlatformConfig.get().getSettingDefs(), PlatformConfig.get()));
        settingsByCategory.put(Category.UTILITY, utility);
    }
}
