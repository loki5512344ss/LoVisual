package dev.loki.lovisual.features.gui.mainmenu.hero;

import dev.loki.lovisual.features.account.AccountConfig;
import dev.loki.lovisual.features.gui.mainmenu.LoVisualAddonManagerScreen;
import dev.loki.lovisual.features.gui.mainmenu.MainMenuTextUtil;
import dev.loki.lovisual.features.gui.mainmenu.MainMenus;
import dev.loki.lovisual.features.gui.mainmenu.alt.LoVisualAltManagerScreen;
import dev.loki.lovisual.features.gui.mainmenu.hero.logic.HeroLayout;
import dev.loki.lovisual.features.gui.mainmenu.hero.logic.HeroLayout.Layout;
import dev.loki.lovisual.features.gui.mainmenu.hero.paint.HeroButtonPainter;
import dev.loki.lovisual.features.gui.mainmenu.hero.paint.HeroMenuRenderer;
import dev.loki.lovisual.features.gui.mainmenu.hero.paint.HeroPaint;
import dev.loki.lovisual.features.gui.mainmenu.hero.paint.HeroWidgetPainter;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
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

import java.time.LocalTime;
import java.util.List;

import static dev.loki.lovisual.features.gui.mainmenu.hero.paint.HeroPaint.easeOutCubic;

/**
 * "Hero" main-menu layout: greeting and big title on the left, account card and clock widget on
 * the top corners. Reuses the shared shader background, the alt manager and the global themes.
 */
public final class LoVisualHeroMenuScreen extends Screen {
    private static final long APPEAR_MS = 700L;
    private static final int THEME_DOTS = 6;

    private final float[] hover = new float[HeroLayout.BUTTON_COUNT];
    private float accountHover;
    private int hoveredDot = -1;
    private int hoveredButton = -1;
    private boolean hoveredAccount;
    private boolean initialized;
    private long openTime;
    private long lastTime;
    private Layout layout;
    private float appear;

    public LoVisualHeroMenuScreen() {
        super(Component.literal("LoVisual"));
    }

    @Override
    protected void init() {
        initialized = false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        Minecraft mc = minecraft;
        if (mc == null) return;
        if (MainMenus.shouldUseVanillaTitleScreen() || !HeroMenuRenderer.background(mc)) {
            MainMenus.fallbackToVanilla(null);
            return;
        }
        long now = Util.getMillis();
        if (!initialized) {
            initialized = true;
            openTime = now;
            lastTime = now;
        }
        float dt = Mth.clamp((now - lastTime) / 1000f, 0f, 0.1f);
        lastTime = now;
        appear = easeOutCubic(Mth.clamp((now - openTime) / (float) APPEAR_MS, 0f, 1f));
        try {
            ViewportContext.beginUnscaledLogical(ctx);
            updateLayout(mc);
            updateHover(toFixed(mouseX, mc), toFixed(mouseY, mc), dt);
            Renderer2D.COLOR.begin();
            draw(ctx, mc);
            Renderer2D.COLOR.render();
        } catch (Throwable t) {
            MainMenus.fallbackToVanilla(t);
        } finally {
            ViewportContext.end(ctx);
        }
    }

    private void updateLayout(Minecraft mc) {
        int fbw = mc.getWindow().getWidth();
        int fbh = mc.getWindow().getHeight();
        float rise = (1f - appear) * 18f * HeroLayout.scale(MainMenuTextUtil.virtualHeight(fbw, fbh));
        layout = HeroLayout.compute(MainMenuTextUtil.virtualWidth(fbw, fbh), MainMenuTextUtil.virtualHeight(fbw, fbh),
                dots().size(), rise, HeroPaint.metrics());
    }

    private void updateHover(float mx, float my, float dt) {
        boolean live = appear > 0.8f;
        hoveredButton = live ? HeroLayout.buttonAt(layout, mx, my) : -1;
        hoveredDot = live ? HeroLayout.dotAt(layout, dots().size(), mx, my) : -1;
        hoveredAccount = live && layout.account().contains(mx, my);
        float step = Math.min(1f, dt * 12f);
        for (int i = 0; i < hover.length; i++) {
            hover[i] = Mth.lerp(step, hover[i], hoveredButton == i ? 1f : 0f);
        }
        accountHover = Mth.lerp(step, accountHover, hoveredAccount ? 1f : 0f);
    }

    private void draw(GuiGraphicsExtractor ctx, Minecraft mc) {
        float w = MainMenuTextUtil.virtualWidth(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        float h = MainMenuTextUtil.virtualHeight(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        String name = accountName(mc);
        boolean needsAuth = AccountConfig.get().getMicrosoftAuthorizationRequiredAccount() != null;
        HeroMenuRenderer.vignette(w, h, appear);
        HeroMenuRenderer.account(ctx, layout, appear, accountHover, name, needsAuth);
        HeroWidgetPainter.draw(layout, appear, dots(), Theme.currentId(), hoveredDot, LocalTime.now());
        HeroMenuRenderer.hero(layout, appear, name);
        for (int i = 0; i < HeroLayout.BUTTON_COUNT; i++) {
            HeroButtonPainter.draw(layout, i, appear, hover[i], needsAuth && i == HeroLayout.ACCOUNTS);
        }
        HeroMenuRenderer.footer(layout, appear, mc.getLaunchedVersion());
    }

    private static List<Themes.ThemeEntry> dots() {
        List<Themes.ThemeEntry> presets = Themes.presetEntries();
        return presets.subList(0, Math.min(THEME_DOTS, presets.size()));
    }

    private static String accountName(Minecraft mc) {
        String active = AccountConfig.get().getActiveAccountName();
        if (active != null && !active.isBlank()) return active;
        return mc.getUser() != null ? mc.getUser().getName() : "LoVisual";
    }

    private static float toFixed(float screen, Minecraft mc) {
        float scale = HudScale.scale(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        return scale <= 0f ? screen : screen * mc.getWindow().getGuiScale() / scale;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        Minecraft mc = minecraft;
        if (click.button() != 0 || mc == null || layout == null || appear <= 0.8f) return true;
        float mx = toFixed((float) click.x(), mc);
        float my = toFixed((float) click.y(), mc);
        int dot = HeroLayout.dotAt(layout, dots().size(), mx, my);
        if (dot >= 0) {
            Theme.setTheme(dots().get(dot).id());
        } else if (layout.account().contains(mx, my)) {
            open(mc, HeroLayout.ACCOUNTS);
        } else {
            int button = HeroLayout.buttonAt(layout, mx, my);
            if (button >= 0) open(mc, button);
        }
        return true;
    }

    private void open(Minecraft mc, int button) {
        switch (button) {
            case HeroLayout.MULTIPLAYER -> ClientScreen.show(mc, new JoinMultiplayerScreen(this));
            case HeroLayout.SINGLEPLAYER -> ClientScreen.show(mc, new SelectWorldScreen(this));
            case HeroLayout.ACCOUNTS -> ClientScreen.show(mc, new LoVisualAltManagerScreen(this,
                    AccountConfig.get().getMicrosoftAuthorizationRequiredAccount() != null));
            case HeroLayout.ADDONS -> ClientScreen.show(mc, new LoVisualAddonManagerScreen(this));
            case HeroLayout.OPTIONS -> ClientScreen.show(mc, new OptionsScreen(this, mc.options, false));
            case HeroLayout.QUIT -> mc.stop();
            default -> { }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        return input.key() == 256 || super.keyPressed(input);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
