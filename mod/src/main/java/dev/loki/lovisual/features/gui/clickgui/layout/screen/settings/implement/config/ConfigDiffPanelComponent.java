package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.config;

import dev.loki.lovisual.config.profile.diff.ConfigProfileDiff;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileMeta;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.config.diff.ConfigDiffBodyRenderer;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.config.diff.ConfigDiffHeaderRenderer;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.config.diff.ConfigDiffScrollbar;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;

/**
 * Facade of the config diff popup: open/close animation, panel chrome, input
 * dispatch. The header buttons, body text and scrollbar live in the
 * {@code config.diff} helpers (see mod/TODO.md 8.5.3).
 */
public final class ConfigDiffPanelComponent {
    private final ConfigDiffHeaderRenderer header = new ConfigDiffHeaderRenderer();
    private final ConfigDiffScrollbar scrollbar = new ConfigDiffScrollbar();
    private float openAnim;
    private boolean openTarget;
    private float panelX;
    private float panelY;
    private float panelW;
    private float panelH;

    public void resetScroll() {
        scrollbar.reset();
    }

    public void open() {
        openTarget = true;
    }

    public void close() {
        openTarget = false;
        scrollbar.release();
    }

    public boolean isActive() {
        return openTarget || openAnim > 0.001f;
    }

    public boolean isOpen() {
        return openTarget;
    }

    public void render(float x,
                       float y,
                       float w,
                       float h,
                       ConfigProfileMeta selected,
                       ConfigProfileDiff diff,
                       String error,
                       float scale,
                       SettingsGuiPalette palette) {
        float dt = AnimationUtility.deltaTime();
        float target = openTarget ? 1f : 0f;
        openAnim = AnimationUtility.approach(openAnim, target, dt, openTarget ? 12f : 13f);
        openAnim = AnimationUtility.snap(openAnim, target, 0.01f);
        if (!openTarget && openAnim <= 0.001f) {
            openAnim = 0f;
            return;
        }

        float reveal = AnimationUtility.easeOutBack(openAnim, 0.85f);
        x += (1f - reveal) * 10f * scale;
        panelX = x;
        panelY = y;
        panelW = w;
        panelH = h;

        int shadow = LayoutRender2D.alpha(palette.panelShadow(), openAnim);
        int blurTint = LayoutRender2D.alpha(palette.panelBlurTint(), openAnim);
        int bgLeft = LayoutRender2D.alpha(palette.panelBgLeft(), openAnim);
        int bgRight = LayoutRender2D.alpha(palette.panelBgRight(), openAnim);
        int stroke = LayoutRender2D.alpha(palette.panelStroke(), openAnim);
        int divider = LayoutRender2D.alpha(palette.panelDivider(), openAnim);
        int text = LayoutRender2D.alpha(palette.panelText(), openAnim);

        LayoutRender2D.roundedSoftShadow(x, y, w, h, 8f * scale, 8f * scale, 0.018f, shadow);
        ClickGuiRenderer.drawBlur(x, y, w, h, 8f * scale, blurTint, (200f / 255f) * openAnim);
        LayoutRender2D.roundedQuad(
                x, y, w, h, 8f * scale,
                bgLeft,
                bgRight,
                bgRight,
                bgLeft
        );
        LayoutRender2D.roundedStroke(x, y, w, h, 8f * scale, 0.1f * scale, stroke);
        LayoutRender2D.rect(x, y + 22f * scale, w, 0.5f * scale, divider);

        ClickGuiRenderer.drawText(
                ClickGuiRenderer.getInterRegular(),
                ClickGuiI18n.tr("clickgui.settings.config.diff.title", "Diff Settings"),
                x + 10f * scale,
                y + 9f * scale,
                7.5f * scale,
                text,
                false
        );
        header.render(x, y, w, ClickGuiRenderer.getMouseX(), ClickGuiRenderer.getMouseY(), scale, palette, openAnim);

        float bodyX = x + 8f * scale;
        float bodyY = y + 27f * scale;
        float bodyW = w - 16f * scale;
        float bodyH = h - 31f * scale;
        float contentH = ConfigDiffBodyRenderer.contentHeight(selected, diff, error, scale);
        float maxScroll = Math.max(0f, contentH - bodyH);
        scrollbar.begin(bodyX, bodyY, bodyW, bodyH, maxScroll, scale);
        if (scrollbar.dragging()) scrollbar.dragToMouse(ClickGuiRenderer.getMouseY());
        scrollbar.smooth();

        boolean clipped = ScissorFunction.pushRaw(bodyX, bodyY, bodyW, bodyH);
        ConfigDiffBodyRenderer.render(
                bodyX,
                bodyY + scrollbar.offset(),
                bodyW - scrollbar.gutter(scale),
                bodyY,
                bodyH,
                selected,
                diff,
                error,
                scale,
                palette,
                openAnim
        );
        if (clipped) ScissorFunction.pop();

        scrollbar.draw(scale, palette, openAnim);
    }

    public HeaderAction headerAction(float mx, float my, int button) {
        if (!openTarget || openAnim <= 0.01f) return HeaderAction.NONE;
        if (button != 0) return HeaderAction.NONE;
        if (header.closeHit(mx, my)) return HeaderAction.CLOSE;
        if (header.saveHit(mx, my)) return HeaderAction.SAVE;
        return HeaderAction.NONE;
    }

    public boolean mousePressedScrollbar(float mx, float my, int button) {
        if (!openTarget || openAnim <= 0.01f) return false;
        if (button != 0) return false;
        return scrollbar.press(mx, my);
    }

    public void mouseReleased(int button) {
        if (button == 0) scrollbar.release();
    }

    public boolean scroll(float mx, float my, double amount) {
        if (!openTarget || openAnim <= 0.01f) return false;
        if (!ClickGuiMath.insideRect(mx, my, panelX, panelY, panelW, panelH)) return false;
        scrollbar.wheel(amount);
        return true;
    }

    public enum HeaderAction {
        NONE,
        SAVE,
        CLOSE
    }
}
