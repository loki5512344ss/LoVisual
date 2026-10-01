package dev.loki.lovisual.features.gui.mainmenu;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.resources.language.I18n;

/** Caption pill that fades in under a hovered round main-menu button, so the glyph-only row stays readable. */
final class MainMenuHoverLabel {
    private static final String[] KEYS = {"singleplayer", "multiplayer", "accounts", "addons", "options", "quit"};
    private static final float TEXT_SIZE = 0.66f;

    private MainMenuHoverLabel() {
    }

    /** @param buttonX left edge of button {@code index}; @param buttonY its top edge */
    static void draw(LoVisualMainMenuScreen owner, int index, float buttonX, float buttonY, float opacity) {
        float hover = owner.buttonHoverProgress[index];
        if (hover < 0.02f || opacity < 0.02f) return;
        float s = LoVisualMainMenuScreen.MENU_SCALE;
        TextRenderer font = MainMenuTextUtil.interMedium(MainMenuTextUtil.interBold(TextRenderer.get()));
        String text = I18n.get("screen.lovisual.hero_menu." + KEYS[index]);
        float size = TEXT_SIZE * s;
        float w = MainMenuTextUtil.measureWidth(font, text, size);
        float h = MainMenuTextUtil.measureHeight(font, size);
        float padX = 9f * s;
        float padY = 4f * s;
        float cx = buttonX + LoVisualMainMenuScreen.BUTTON_RADIUS;
        float y = buttonY + LoVisualMainMenuScreen.BUTTON_SIZE + (8f + (1f - hover) * 4f) * s;
        float alpha = hover * opacity;
        Renderer2D.COLOR.roundedRect(cx - w * 0.5f - padX, y, w + padX * 2f, h + padY * 2f, (h + padY * 2f) * 0.5f,
                MainMenuPainter.withAlpha(0x0C0F16, Math.round(alpha * 190f)));
        MainMenuTextUtil.drawText(font, text, cx - w * 0.5f, y + padY, size, MainMenuPainter.withAlpha(0xFFFFFF, Math.round(alpha * 235f)));
    }
}
