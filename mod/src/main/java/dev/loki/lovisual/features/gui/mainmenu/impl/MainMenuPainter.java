package dev.loki.lovisual.features.gui.mainmenu;

import dev.loki.lovisual.features.account.AccountConfig;
import dev.loki.lovisual.features.account.AccountEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * Layout / animation / hit-test helpers for {@link LoVisualMainMenuScreen}. Owns the
 * per-frame UI metric recomputation, cursor coordinate remapping (screen to fixed
 * logical viewport), button hover / scale interpolation and the click-target query
 * used by both the extract-render-state pass and the mouse-click handler. Kept in a
 * sibling folder sharing the parent's package so the screen class stays compact.
 */
final class MainMenuPainter {

    /** Rectangle in fixed-logical coordinates; used for hit-testing the auth warning card. */
    record Bounds(float x, float y, float w, float h) {
        boolean contains(float mx, float my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    private final LoVisualMainMenuScreen owner;

    MainMenuPainter(LoVisualMainMenuScreen owner) {
        this.owner = owner;
    }

    static float easeOutCubic(float x) { return 1f - (float) Math.pow(1f - x, 3f); }
    static float easeOutQuart(float x) { return 1f - (float) Math.pow(1f - x, 4f); }
    static int withAlpha(int rgb, int alpha) { return (rgb & 0x00FFFFFF) | (Mth.clamp(alpha, 0, 255) << 24); }
    static String tr(String key, Object... args) { return I18n.get(key, args); }

    /** True once the fade-in is past {@code 0.8}; blocks clicks during the initial slide. */
    boolean canInteract(float menuProgress) { return menuProgress > 0.8f; }

    /** Elapsed time since open, clamped to {@code MENU_APPEAR_DURATION}; 0 before init. */
    float getMenuProgress(long now) {
        if (!owner.initialized) return 0f;
        return Mth.clamp((now - owner.openTime) / (float) LoVisualMainMenuScreen.MENU_APPEAR_DURATION, 0f, 1f);
    }

    /** Recompute {@code fixedWidth} / {@code fixedHeight} from the framebuffer using {@link dev.loki.lovisual.render.engine.math.HudScale}. */
    void updateUiMetrics() {
        Minecraft mc = owner.mc();
        if (mc == null) {
            owner.fixedWidth = owner.screenW();
            owner.fixedHeight = owner.screenH();
            owner.renderScale = 1f;
            return;
        }
        int fbw = mc.getWindow().getWidth();
        int fbh = mc.getWindow().getHeight();
        owner.fixedWidth = MainMenuTextUtil.virtualWidth(fbw, fbh);
        owner.fixedHeight = MainMenuTextUtil.virtualHeight(fbw, fbh);
        owner.renderScale = 1f;
    }

    /** Map a screen-space x to the fixed logical viewport used by painter draw calls. */
    float toFixedX(float screenX) {
        Minecraft mc = owner.mc();
        if (owner.screenW() <= 0 || mc == null) return screenX;
        float hudScale = dev.loki.lovisual.render.engine.math.HudScale.scale(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        return hudScale <= 0f ? screenX : screenX * mc.getWindow().getGuiScale() / hudScale;
    }

    float toFixedY(float screenY) {
        Minecraft mc = owner.mc();
        if (owner.screenH() <= 0 || mc == null) return screenY;
        float hudScale = dev.loki.lovisual.render.engine.math.HudScale.scale(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        return hudScale <= 0f ? screenY : screenY * mc.getWindow().getGuiScale() / hudScale;
    }

    /** Index of the button whose circle encloses {@code (mouseX, mouseY)}; {@code -1} if none / not interactive. */
    int getHoveredButton(float mouseX, float mouseY, float menuProgress) {
        if (!canInteract(menuProgress)) return -1;
        float totalWidth = LoVisualMainMenuScreen.BUTTON_SIZE * LoVisualMainMenuScreen.BUTTON_ICONS.length
                + LoVisualMainMenuScreen.BUTTON_SPACING * (LoVisualMainMenuScreen.BUTTON_ICONS.length - 1);
        float startX = (owner.fixedWidth - totalWidth) * 0.5f;
        float y = owner.fixedHeight * 0.5f + 34f * LoVisualMainMenuScreen.MENU_SCALE + (1f - menuProgress) * 60f * LoVisualMainMenuScreen.MENU_SCALE;
        for (int i = 0; i < LoVisualMainMenuScreen.BUTTON_ICONS.length; i++) {
            float centerX = startX + i * (LoVisualMainMenuScreen.BUTTON_SIZE + LoVisualMainMenuScreen.BUTTON_SPACING) + LoVisualMainMenuScreen.BUTTON_SIZE * 0.5f;
            float centerY = y + LoVisualMainMenuScreen.BUTTON_SIZE * 0.5f;
            float dx = mouseX - centerX, dy = mouseY - centerY;
            if (dx * dx + dy * dy <= LoVisualMainMenuScreen.BUTTON_RADIUS * LoVisualMainMenuScreen.BUTTON_RADIUS) return i;
        }
        return -1;
    }

    /** The auth-warning card sits below the button row, clamped so it never leaves the viewport. */
    Bounds authWarningBounds(float menuProgress) {
        float s = LoVisualMainMenuScreen.MENU_SCALE;
        float slideOffset = (1f - menuProgress) * 60f * s;
        float buttonY = owner.fixedHeight * 0.5f + 34f * s + slideOffset;
        float y = Math.min(buttonY + LoVisualMainMenuScreen.BUTTON_SIZE + 18f * s, owner.fixedHeight - LoVisualMainMenuScreen.AUTH_WARNING_H - 16f * s);
        return new Bounds((owner.fixedWidth - LoVisualMainMenuScreen.AUTH_WARNING_W) * 0.5f, y,
                LoVisualMainMenuScreen.AUTH_WARNING_W, LoVisualMainMenuScreen.AUTH_WARNING_H);
    }

    /** The pending-authentication account that triggers the warning card, or {@code null}. */
    AccountEntry getAuthWarningAccount() {
        return AccountConfig.get().getMicrosoftAuthorizationRequiredAccount();
    }

    /** Per-frame hover / scale interpolation for buttons, the quit tint and the warning card. */
    void updateButtonAnimations(float deltaTime) {
        for (int i = 0; i < LoVisualMainMenuScreen.BUTTON_ICONS.length; i++) {
            float targetHover = owner.hoveredButton == i ? 1f : 0f;
            owner.buttonHoverProgress[i] = Mth.lerp(Math.min(1f, deltaTime * 10f), owner.buttonHoverProgress[i], targetHover);
            float targetScale = owner.hoveredButton == i ? LoVisualMainMenuScreen.BUTTON_HOVER_SCALE : 1f;
            owner.buttonScales[i] = Mth.lerp(Math.min(1f, deltaTime * 12f), owner.buttonScales[i], targetScale);
        }
        float targetQuit = owner.hoveredButton == LoVisualMainMenuScreen.BUTTON_ICONS.length - 1 ? 1f : 0f;
        owner.quitHoverProgress = Mth.lerp(Math.min(1f, deltaTime * 8f), owner.quitHoverProgress, targetQuit);
        owner.authWarningHoverProgress = Mth.lerp(Math.min(1f, deltaTime * 10f), owner.authWarningHoverProgress, owner.authWarningHovered ? 1f : 0f);
    }

    /** ESC is swallowed to keep the user on the custom menu; everything else falls through. */
    boolean onKeyPressed(KeyEvent input) {
        return input.key() == 256 || owner.superKeyPressed(input);
    }
}
