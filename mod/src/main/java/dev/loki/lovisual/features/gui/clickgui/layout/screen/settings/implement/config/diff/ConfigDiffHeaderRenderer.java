package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.config.diff;

import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.helpers.util.SystemCursor;

/**
 * Header chrome of the diff panel: the save/close icon buttons — their rects,
 * hover animation, painting and hit-testing. Owned by
 * {@code ConfigDiffPanelComponent}, which forwards the panel's fade value here.
 */
public final class ConfigDiffHeaderRenderer {
    private float saveX;
    private float saveY;
    private float saveW;
    private float saveH;
    private float closeX;
    private float closeY;
    private float closeW;
    private float closeH;
    private float saveHoverAnim;
    private float closeHoverAnim;

    public void render(float x,
                       float y,
                       float w,
                       float mx,
                       float my,
                       float scale,
                       SettingsGuiPalette palette,
                       float openAnim) {
        float btn = 16f * scale;
        closeX = x + w - btn - 4f * scale;
        closeY = y + 3f * scale;
        closeW = btn;
        closeH = btn;
        saveX = closeX - btn - scale;
        saveY = closeY;
        saveW = btn;
        saveH = btn;

        boolean saveHover = ClickGuiMath.insideRect(mx, my, saveX, saveY, saveW, saveH);
        boolean closeHover = ClickGuiMath.insideRect(mx, my, closeX, closeY, closeW, closeH);
        saveHoverAnim = AnimationUtility.approach(saveHoverAnim, saveHover ? 1f : 0f, AnimationUtility.deltaTime(), 10f);
        closeHoverAnim = AnimationUtility.approach(closeHoverAnim, closeHover ? 1f : 0f, AnimationUtility.deltaTime(), 10f);

        drawIcon(saveX, saveY, btn, "save", saveHoverAnim, scale, palette, openAnim);
        LayoutRender2D.rect(closeX - scale, y + 6f * scale, 0.5f * scale, 10f * scale, LayoutRender2D.alpha(palette.panelDivider(), openAnim));
        drawIcon(closeX, closeY, btn, "x", closeHoverAnim, scale, palette, openAnim);
        if (saveHover || closeHover) SystemCursor.set(SystemCursor.CursorType.MOVE);
    }

    public boolean saveHit(float mx, float my) {
        return ClickGuiMath.insideRect(mx, my, saveX, saveY, saveW, saveH);
    }

    public boolean closeHit(float mx, float my) {
        return ClickGuiMath.insideRect(mx, my, closeX, closeY, closeW, closeH);
    }

    private void drawIcon(float x,
                          float y,
                          float size,
                          String icon,
                          float hover,
                          float scale,
                          SettingsGuiPalette palette,
                          float openAnim) {
        boolean save = "save".equals(icon);
        int bgA = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.moduleCardTop(), save ? palette.menuCategorySelectedLeft() : palette.menuCategoryHoverLeft(), 0.14f + hover * 0.22f), openAnim);
        int bgB = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.moduleCardTopStrong(), save ? palette.menuCategorySelectedRight() : palette.menuCategoryHoverRight(), 0.12f + hover * 0.18f), openAnim);
        int bgC = LayoutRender2D.alpha(SettingsGuiPalette.mix(SettingsGuiPalette.darken(palette.moduleCardTopStrong(), 0.12f), save ? palette.menuCategorySelectedRight() : palette.menuCategoryHoverRight(), 0.08f + hover * 0.15f), openAnim);
        LayoutRender2D.roundedSoftShadow(
                x,
                y + 0.4f * scale,
                size,
                size,
                3.5f * scale,
                4f * scale,
                0.015f + hover * 0.02f,
                LayoutRender2D.alpha(palette.menuShadow(), openAnim * (0.50f + hover * 0.45f))
        );
        LayoutRender2D.roundedQuad(x, y, size, size, 3.5f * scale, bgA, bgB, bgC, bgA);
        LayoutRender2D.roundedStrokeQuad(
                x,
                y,
                size,
                size,
                3.5f * scale,
                (0.35f + hover * 0.18f) * scale,
                LayoutRender2D.alpha(palette.moduleDividerStart(), openAnim * (0.54f + hover * 0.24f)),
                LayoutRender2D.alpha(palette.moduleDividerEnd(), openAnim * (0.62f + hover * 0.24f)),
                LayoutRender2D.alpha(palette.moduleDividerEnd(), openAnim * (0.48f + hover * 0.20f)),
                LayoutRender2D.alpha(palette.moduleDividerStart(), openAnim * (0.54f + hover * 0.24f))
        );

        int color = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelText(), palette.menuCategoryText(), 0.16f + hover * 0.38f), openAnim);
        Renderer2D.COLOR.svg(
                icon,
                x + 4.6f * scale,
                y + 4.6f * scale,
                size - 9.2f * scale,
                size - 9.2f * scale,
                SvgRenderOptions.overrideColor(color)
        );
    }
}
