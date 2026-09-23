package dev.loki.lovisual.features.gui.clickgui.tabs;

import dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland.DynamicIsland;
import dev.loki.lovisual.render.engine.renderer.warp.RenderWarpStack;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

import java.util.List;

import static dev.loki.lovisual.features.theme.Theme.theme;
import dev.loki.lovisual.features.gui.clickgui.editor.ClickGuiEditorManager;
import dev.loki.lovisual.features.gui.clickgui.font.ClickGuiFonts;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiDrawHelpers;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiTextBatch;
import dev.loki.lovisual.features.gui.clickgui.screen.ClickGuiLifecycle;
import dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiSearch;
import dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiTabState;
import dev.loki.lovisual.features.gui.clickgui.ClickGuiModel.ClickGuiTabEntry;

public final class ClickGuiTabBar {

    private ClickGuiTabBar() {
    }

    public static void layout(int fbw, int fbh) {

        List<ClickGuiTabEntry> tabs = ClickGuiTabState.currentTabs();
        ClickGuiTabState.ensureActiveTab(tabs);
        float contentMarginX = 28f;
        float contentTop = 52f;
        float contentBottom = 18f;
        ClickGuiRenderer.modulesAreaX = contentMarginX;
        ClickGuiRenderer.modulesAreaY = contentTop;
        ClickGuiRenderer.modulesAreaW = Math.max(1f, fbw - contentMarginX * 2f);
        ClickGuiRenderer.modulesAreaH = Math.max(1f, fbh - contentTop - contentBottom);
        for (ClickGuiTabEntry tab : tabs) {
            tab.section().layout(ClickGuiRenderer.modulesAreaX, ClickGuiRenderer.modulesAreaY, ClickGuiRenderer.modulesAreaW, ClickGuiRenderer.modulesAreaH);
        }

        ClickGuiRenderer.tabBarH = ClickGuiRenderer.TAB_HEIGHT;
        ClickGuiRenderer.tabBarY = 10f;
        ClickGuiTabState.ensureTabCapacity(tabs.size());
        float totalTabs = 0f;
        for (int t = 0; t < tabs.size(); t++) {
            float wTab = ClickGuiTextBatch.textWidth(ClickGuiFonts.getSfProDisplaySemibold(), tabs.get(t).label(), ClickGuiRenderer.TAB_FONT) + ClickGuiRenderer.TAB_PAD_X * 2f;
            if (wTab < ClickGuiRenderer.TAB_MIN_W) wTab = ClickGuiRenderer.TAB_MIN_W;
            ClickGuiRenderer.tabW[t] = wTab;
            totalTabs += wTab;
        }
        if (!tabs.isEmpty()) {
            totalTabs += ClickGuiRenderer.TAB_GAP * (tabs.size() - 1);
        }
        ClickGuiRenderer.tabBarW = totalTabs;
        ClickGuiRenderer.tabBarX = (fbw - totalTabs) * 0.5f;
        float cx = ClickGuiRenderer.tabBarX;
        for (int t = 0; t < tabs.size(); t++) {
            ClickGuiRenderer.tabX[t] = cx;
            cx += ClickGuiRenderer.tabW[t] + ClickGuiRenderer.TAB_GAP;
        }

        int activeIdx = 0;
        for (int t = 0; t < tabs.size(); t++) {
            if (tabs.get(t).id().equals(ClickGuiRenderer.activeTabId)) {
                activeIdx = t;
                break;
            }
        }
        ClickGuiRenderer.tabTargetX = ClickGuiRenderer.tabX[activeIdx];
        ClickGuiRenderer.tabTargetW = ClickGuiRenderer.tabW[activeIdx];
        if (!ClickGuiRenderer.tabAnimInit) {
            ClickGuiRenderer.tabAnimX = ClickGuiRenderer.tabTargetX;
            ClickGuiRenderer.tabAnimW = ClickGuiRenderer.tabTargetW;
            ClickGuiRenderer.tabAnimInit = true;
            ClickGuiRenderer.lastTabAnimNs = System.nanoTime();
        }
    }

    public static void updateTabAnim(long now) {

        float dt = (now - ClickGuiRenderer.lastTabAnimNs) / 1_000_000_000f;
        ClickGuiRenderer.lastTabAnimNs = now;
        if (dt < 0f) dt = 0f;
        float speed = 16f;
        float step = Math.min(1f, dt * speed);
        ClickGuiRenderer.tabAnimX += (ClickGuiRenderer.tabTargetX - ClickGuiRenderer.tabAnimX) * step;
        ClickGuiRenderer.tabAnimW += (ClickGuiRenderer.tabTargetW - ClickGuiRenderer.tabAnimW) * step;
    }

    public static void drawTabs() {

        float eased = ClickGuiTabState.easeOutCubic(ClickGuiRenderer.lifecycleAnim);
        if (eased <= 0.001f) return;
        List<ClickGuiTabEntry> tabs = ClickGuiTabState.currentTabs();
        ClickGuiTabState.ensureActiveTab(tabs);
        if (tabs.isEmpty()) return;
        boolean islandShell = DynamicIsland.shouldOwnClickGuiTabShell();

        TextRenderer tabFont = ClickGuiFonts.getSfProDisplaySemibold();
        float y = ClickGuiRenderer.tabBarY - (1.0f - eased) * 18.0f;
        float outerRadius = ClickGuiRenderer.tabBarH * 0.5f;
        float outerPad = 4.0f;
        float innerX = ClickGuiRenderer.tabAnimX + outerPad;
        float innerY = y + outerPad;
        float innerW = Math.max(1.0f, ClickGuiRenderer.tabAnimW - outerPad * 2.0f);
        float innerH = Math.max(1.0f, ClickGuiRenderer.tabBarH - outerPad * 2.0f);
        float innerRadius = innerH * 0.5f;

        int outerLeft = ClickGuiDrawHelpers.scaleAlpha(ClickGuiDrawHelpers.mixColor(theme().surface(), theme().windowBg(), 0.34f), eased);
        int outerRight = ClickGuiDrawHelpers.scaleAlpha(ClickGuiDrawHelpers.mixColor(theme().surfaceHover(), theme().windowBg(), 0.22f), eased);
        int outerStrokeLeft = ClickGuiDrawHelpers.scaleAlpha(ClickGuiDrawHelpers.mixColor(theme().strokeSoft(), theme().accentSoft(), 0.18f), 0.72f * eased);
        int outerStrokeRight = ClickGuiDrawHelpers.scaleAlpha(ClickGuiDrawHelpers.mixColor(theme().strokeSoft(), theme().textPrimary(), 0.08f), 0.58f * eased);

        int activeLeft = ClickGuiDrawHelpers.scaleAlpha(ClickGuiDrawHelpers.mixColor(theme().accentSoft(), theme().accent(), 0.18f), eased);
        int activeRight = ClickGuiDrawHelpers.scaleAlpha(ClickGuiDrawHelpers.mixColor(theme().accent(), theme().textPrimary(), 0.08f), eased);
        int activeStrokeLeft = ClickGuiDrawHelpers.scaleAlpha(ClickGuiDrawHelpers.mixColor(theme().accentSoft(), theme().textPrimary(), 0.20f), 0.88f * eased);
        int activeStrokeRight = ClickGuiDrawHelpers.scaleAlpha(theme().accent(), 0.92f * eased);

        try (RenderWarpStack.Scope ignored = Renderer2D.pushPerspectiveWarp(
                ClickGuiRenderer.tabBarX,
                y,
                ClickGuiRenderer.tabBarW,
                ClickGuiRenderer.tabBarH,
                0.0f,
                -5.0f * (1.0f - eased),
                0.0f,
                3.2f,
                0.80f,
                0.965f + 0.035f * eased
        )) {
            if (!islandShell) {
                ClickGuiDrawHelpers.drawRoundedRectShadow(
                        ClickGuiRenderer.tabBarX,
                        y,
                        ClickGuiRenderer.tabBarW,
                        ClickGuiRenderer.tabBarH,
                        outerRadius,
                        10.0f,
                        1.5f,
                        ClickGuiDrawHelpers.scaleAlpha(0xCC000000, 0.34f * eased)
                );
                ClickGuiDrawHelpers.drawRoundedRectGradient(ClickGuiRenderer.tabBarX, y, ClickGuiRenderer.tabBarW, ClickGuiRenderer.tabBarH, outerRadius, outerLeft, outerRight, 0.0f);
                ClickGuiDrawHelpers.drawRoundedRectStrokeGradient(ClickGuiRenderer.tabBarX, y, ClickGuiRenderer.tabBarW, ClickGuiRenderer.tabBarH, outerRadius, 1.0f, outerStrokeLeft, outerStrokeRight, 0.0f);
            }

            ClickGuiDrawHelpers.drawRoundedRectGlow(
                    innerX,
                    innerY,
                    innerW,
                    innerH,
                    innerRadius,
                    10.0f,
                    ClickGuiDrawHelpers.scaleAlpha(theme().accentSoft(), 0.30f * eased)
            );
            ClickGuiDrawHelpers.drawRoundedRectGradient(innerX, innerY, innerW, innerH, innerRadius, activeLeft, activeRight, 0.0f);
            ClickGuiDrawHelpers.drawRoundedRectStrokeGradient(innerX, innerY, innerW, innerH, innerRadius, 1.0f, activeStrokeLeft, activeStrokeRight, 0.0f);

            for (int i = 0; i < tabs.size(); i++) {
                float x = ClickGuiRenderer.tabX[i];
                float w = ClickGuiRenderer.tabW[i];
                ClickGuiTabEntry tab = tabs.get(i);
                boolean active = tab.id().equals(ClickGuiRenderer.activeTabId);
                boolean hovered = !ClickGuiRenderer.closingForExit && ClickGuiRenderer.mouseX >= x && ClickGuiRenderer.mouseX <= x + w && ClickGuiRenderer.mouseY >= y && ClickGuiRenderer.mouseY <= y + ClickGuiRenderer.tabBarH;
                if (i > 0) {
                    float sepH = ClickGuiRenderer.tabBarH - outerPad * 3.4f;
                    float sepY = y + (ClickGuiRenderer.tabBarH - sepH) * 0.5f;
                    ClickGuiDrawHelpers.drawRect(x, sepY, 1.0f, sepH, ClickGuiDrawHelpers.scaleAlpha(theme().strokeSoft(), 0.32f * eased));
                }
                int textCol = active ? theme().textPrimary() : theme().textMuted();
                if (!active && hovered) {
                    textCol = ClickGuiDrawHelpers.mixColor(textCol, theme().textPrimary(), 0.55f);
                }
                textCol = ClickGuiDrawHelpers.scaleAlpha(textCol, eased);
                float textH = ClickGuiTextBatch.textHeight(tabFont, ClickGuiRenderer.TAB_FONT);
                float textY = y + (ClickGuiRenderer.tabBarH - textH) * 0.5f - 0.35f;
                float labelW = ClickGuiTextBatch.textWidth(tabFont, tab.label(), ClickGuiRenderer.TAB_FONT);
                float textX = x + (w - labelW) * 0.5f;
                ClickGuiTextBatch.drawText(tabFont, tab.label(), textX, textY, ClickGuiRenderer.TAB_FONT, textCol, false);
            }
        }
    }

    public static boolean handleTabClick(float mx, float my) {

        if (!ClickGuiLifecycle.isInputReady()) return false;
        if (mx < ClickGuiRenderer.tabBarX || mx > ClickGuiRenderer.tabBarX + ClickGuiRenderer.tabBarW || my < ClickGuiRenderer.tabBarY || my > ClickGuiRenderer.tabBarY + ClickGuiRenderer.tabBarH) return false;
        List<ClickGuiTabEntry> tabs = ClickGuiTabState.currentTabs();
        ClickGuiTabState.ensureActiveTab(tabs);
        for (int i = 0; i < tabs.size(); i++) {
            float x = ClickGuiRenderer.tabX[i];
            float w = ClickGuiRenderer.tabW[i];
            if (mx >= x && mx <= x + w && my >= ClickGuiRenderer.tabBarY && my <= ClickGuiRenderer.tabBarY + ClickGuiRenderer.tabBarH) {
                ClickGuiTabEntry tab = tabs.get(i);
                if (!ClickGuiRenderer.activeTabId.equals(tab.id())) {
                    dev.loki.lovisual.features.gui.clickgui.sections.ClickGuiSection previous = ClickGuiTabState.getActiveSection();
                    ClickGuiRenderer.activeTabId = tab.id();
                    previous.onDeselected();
                    ClickGuiSearch.deactivate();
                    ClickGuiEditorManager.clearInlineText();
                    ClickGuiEditorManager.closeTextEditor(false);
                    ClickGuiTabState.getActiveSection().onSelected();
                }
                return true;
            }
        }
        return false;
    }
}
