package dev.loki.lovisual.features.gui.mainmenu;

import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.features.gui.mainmenu.alt.LoVisualAltManagerScreen;
import dev.loki.lovisual.render.engine.animation.AnimatedClockText;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Custom main-menu screen: shader background + clock/date header + row of six
 * round action buttons (world / multiplayer / accounts / addons / options / quit)
 * + optional Microsoft-auth warning card. Draw pass lives in {@link MainMenuRenderer},
 * layout / hit-test / animation in {@link MainMenuPainter}, text primitives in
 * {@link MainMenuTextUtil}; the screen class only wires Minecraft's {@link Screen}
 * lifecycle to those helpers and owns the shared mutable state.
 */
public final class LoVisualMainMenuScreen extends Screen {

    // Layout / appearance constants (also referenced by painter + renderer helpers).
    static final float MENU_SCALE = 1.22f;
    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);
    static final float BUTTON_SIZE = 46f * MENU_SCALE;
    static final float BUTTON_RADIUS = BUTTON_SIZE * 0.5f;
    static final float BUTTON_SPACING = 18f * MENU_SCALE;
    static final float BUTTON_HOVER_SCALE = 1.09f;
    static final float BUTTON_SHADOW = 18f * MENU_SCALE;
    static final float BUTTON_SOFTNESS = 1.0f;
    static final String[] BUTTON_ICONS = {"a", "b", "x", "", "s", "i"};
    static final String[] BUTTON_SVGS = {null, null, null, "folder-pen", null, null};
    static final long MENU_APPEAR_DURATION = 800L;
    static final float TIME_FONT = 4.95f * MENU_SCALE;
    static final float DATE_FONT = 0.94f * MENU_SCALE;
    static final float ICON_FONT = 1.52f * MENU_SCALE;
    static final float ICON_Y_OFFSET = -0.75f * MENU_SCALE;
    static final float AUTH_WARNING_W = 250f * MENU_SCALE;
    static final float AUTH_WARNING_H = 42f * MENU_SCALE;
    static final float AUTH_WARNING_R = 8f * MENU_SCALE;
    static final float AUTH_WARNING_HEAD = 26f * MENU_SCALE;
    static final long AUTH_WARNING_PROGRESS_MS = 3200L;
    static volatile boolean forceVanillaTitleScreen;

    // Per-instance state; package-private so painter / renderer can read + write.
    final float[] buttonScales = new float[BUTTON_ICONS.length];
    final float[] buttonHoverProgress = new float[BUTTON_ICONS.length];
    final AnimatedClockText clockText = new AnimatedClockText();
    final MainMenuPainter painter = new MainMenuPainter(this);
    final MainMenuRenderer renderer = new MainMenuRenderer(this, painter);
    boolean initialized;
    long openTime;
    long lastRenderTime;
    int hoveredButton = -1;
    float quitHoverProgress;
    boolean authWarningHovered;
    float authWarningHoverProgress;
    float fixedWidth;
    float fixedHeight;
    float renderScale;

    public LoVisualMainMenuScreen() {
        super(Component.literal("LoVisual"));
        for (int i = 0; i < buttonScales.length; i++) { buttonScales[i] = 1f; buttonHoverProgress[i] = 0f; }
    }

    /** Whether the user disabled the custom menu (or a previous draw failed) and we should show vanilla. */
    public static boolean shouldUseVanillaTitleScreen() {
        if (forceVanillaTitleScreen) return true;
        MainConfig cfg = MainConfig.get();
        String mode = cfg != null ? cfg.getMenuBackgroundMode() : null;
        return mode != null && mode.equalsIgnoreCase("off");
    }

    Minecraft mc() { return minecraft; }
    int screenW() { return width; }
    int screenH() { return height; }
    boolean superKeyPressed(KeyEvent input) { return super.keyPressed(input); }

    @Override protected void init() { initialized = false; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (shouldUseVanillaTitleScreen()) { renderer.switchToVanillaTitleScreen(null, false); return; }
        long now = Util.getMillis();
        if (!initialized) { initialized = true; openTime = now; lastRenderTime = now; }
        float deltaTime = Mth.clamp((now - lastRenderTime) / 1000f, 0f, 0.1f);
        lastRenderTime = now;
        painter.updateUiMetrics();
        float menuProgress = MainMenuPainter.easeOutQuart(painter.getMenuProgress(now));
        float fixedMouseX = painter.toFixedX(mouseX);
        float fixedMouseY = painter.toFixedY(mouseY);
        hoveredButton = painter.canInteract(menuProgress) ? painter.getHoveredButton(fixedMouseX, fixedMouseY, menuProgress) : -1;
        authWarningHovered = painter.canInteract(menuProgress) && painter.getAuthWarningAccount() != null && painter.authWarningBounds(menuProgress).contains(fixedMouseX, fixedMouseY);
        painter.updateButtonAnimations(deltaTime);
        try {
            renderer.renderShaderBackground(context);
            dev.loki.lovisual.render.engine.core.context.ViewportContext.beginUnscaledLogical(context);
            dev.loki.lovisual.render.engine.renderer.Renderer2D.COLOR.begin();
            renderer.drawDimmer();
            if (menuProgress > 0.01f) {
                renderer.renderTime(menuProgress);
                renderer.renderButtons(menuProgress);
                renderer.renderAuthWarning(context, menuProgress, now);
            }
            dev.loki.lovisual.render.engine.renderer.Renderer2D.COLOR.render();
        } catch (Throwable t) {
            renderer.switchToVanillaTitleScreen(t, true);
        } finally {
            try {
                dev.loki.lovisual.render.engine.core.context.ViewportContext.end(context);
            } catch (Throwable t) {
                dev.loki.lovisual.util.logging.DebugLog.errorOnce("main_menu_viewport_end", "Main menu viewport end failed", t);
            }
        }
    }

    @Override public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) { }
    @Override public void mouseMoved(double mouseX, double mouseY) { }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return true;
        painter.updateUiMetrics();
        float menuProgress = MainMenuPainter.easeOutQuart(painter.getMenuProgress(Util.getMillis()));
        float fx = painter.toFixedX((float) click.x()), fy = painter.toFixedY((float) click.y());
        if (renderer.handleAuthWarningClick(fx, fy, menuProgress)) return true;
        int index = painter.canInteract(menuProgress) ? painter.getHoveredButton(fx, fy, menuProgress) : -1;
        if (index >= 0) handleButton(index);
        return true;
    }

    @Override public boolean keyPressed(KeyEvent input) { return painter.onKeyPressed(input); }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }

    /** Route index → target screen (world / multiplayer / accounts / addons / options / quit). */
    private void handleButton(int index) {
        Minecraft mc = minecraft;
        if (mc == null) return;
        switch (index) {
            case 0 -> ClientScreen.show(mc, new SelectWorldScreen(this));
            case 1 -> ClientScreen.show(mc, new JoinMultiplayerScreen(this));
            case 2 -> ClientScreen.show(mc, new LoVisualAltManagerScreen(this));
            case 3 -> ClientScreen.show(mc, new LoVisualAddonManagerScreen(this));
            case 4 -> ClientScreen.show(mc, new OptionsScreen(this, mc.options, false));
            case 5 -> mc.stop();
            default -> { }
        }
    }
}
