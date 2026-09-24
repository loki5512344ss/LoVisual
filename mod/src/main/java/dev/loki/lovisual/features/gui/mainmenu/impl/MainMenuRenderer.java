package dev.loki.lovisual.features.gui.mainmenu;

import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.features.account.AccountEntry;
import dev.loki.lovisual.features.account.SkinManager;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.mainmenu.alt.LoVisualAltManagerScreen;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.postprocess.render.MenuBackgroundRenderer;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.player.PlayerHeadRenderer;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Draw pass for {@link LoVisualMainMenuScreen}: shader background, dimmer gradient,
 * clock + date header, the six round action buttons (with liquid-glass circle and
 * SVG / icon-font glyphs) and the Microsoft-auth warning card. All state reads go
 * through the owner so this class stays purely functional.
 */
final class MainMenuRenderer {

    private final LoVisualMainMenuScreen owner;
    private final MainMenuPainter painter;

    MainMenuRenderer(LoVisualMainMenuScreen owner, MainMenuPainter painter) {
        this.owner = owner;
        this.painter = painter;
    }

    /** Kick off the aurora / plain menu background; hands control back to vanilla if the mode is off. */
    void renderShaderBackground(GuiGraphicsExtractor context) {
        Minecraft mc = owner.mc();
        if (mc == null) return;
        MainConfig cfg = MainConfig.get();
        String mode = cfg != null ? cfg.getMenuBackgroundMode() : "off";
        if (mode == null || mode.equalsIgnoreCase("off")) {
            switchToVanillaTitleScreen(null, false);
            return;
        }
        MenuBackgroundRenderer.render(mc, mode.equalsIgnoreCase("aurora"));
    }

    /** Fall back to the vanilla {@link TitleScreen} on any draw error or when the user disabled the menu. */
    void switchToVanillaTitleScreen(Throwable t, boolean sticky) {
        if (sticky) LoVisualMainMenuScreen.forceVanillaTitleScreen = true;
        if (t != null) dev.loki.lovisual.util.logging.DebugLog.errorOnce("main_menu_vanilla_fallback", "Main menu failed, switching to vanilla title screen", t);
        else if (sticky) dev.loki.lovisual.util.logging.DebugLog.warnOnce("main_menu_vanilla_fallback", "Main menu switched to vanilla title screen");
        Minecraft mc = owner.mc() != null ? owner.mc() : Minecraft.getInstance();
        if (mc != null && !(ClientScreen.current() instanceof TitleScreen)) ClientScreen.show(mc, new TitleScreen(false));
    }

    /** Two-stop vertical dimmer over the shader, darkening the bottom half a bit more. */
    void drawDimmer() {
        Themes.Theme theme = Theme.theme();
        int base = HudRenderUtil.mixColor(theme.windowBg(), 0xFF000000, 0.46f);
        int soft = MainMenuPainter.withAlpha(base, 150);
        int deep = MainMenuPainter.withAlpha(HudRenderUtil.mixColor(theme.windowBg(), 0xFF000000, 0.72f), 195);
        Renderer2D.COLOR.quad(0, 0, owner.fixedWidth, owner.fixedHeight, soft, soft, deep, deep);
    }

    /** Animated clock + "EEEE, MMMM d" date, sliding down as the menu fades in. */
    void renderTime(float opacity) {
        float s = LoVisualMainMenuScreen.MENU_SCALE;
        float centerX = owner.fixedWidth * 0.5f;
        float centerY = owner.fixedHeight * 0.5f - 55f * s + (1f - opacity) * 40f * s;
        long nowMs = Util.getMillis();
        MainConfig cfg = MainConfig.get();
        boolean showSeconds = cfg != null && cfg.isMenuClockShowSeconds();
        owner.clockText.update(LocalTime.now(), showSeconds, nowMs);
        String dateText = LocalDate.now().format(LoVisualMainMenuScreen.DATE_FORMAT);
        TextRenderer timeRenderer = MainMenuTextUtil.interBold(TextRenderer.get());
        TextRenderer dateRenderer = MainMenuTextUtil.interMedium(timeRenderer);
        int timeColor = MainMenuPainter.withAlpha(0xFFFFFF, Math.round(opacity * 255f));
        int dateColor = MainMenuPainter.withAlpha(0xFFFFFF, Math.round(opacity * 200f));
        float timeHeight = owner.clockText.height(timeRenderer, LoVisualMainMenuScreen.TIME_FONT);
        owner.clockText.renderLiquidGlassCentered(timeRenderer, centerX, centerY - timeHeight * 0.5f, LoVisualMainMenuScreen.TIME_FONT, timeColor, nowMs);
        MainMenuTextUtil.drawCenteredText(dateRenderer, dateText, centerX, centerY + timeHeight * 0.5f + 4f * s, LoVisualMainMenuScreen.DATE_FONT, dateColor);
    }

    /** Row of six circular buttons, each with a per-index slide / fade stagger. */
    void renderButtons(float opacity) {
        float s = LoVisualMainMenuScreen.MENU_SCALE;
        int n = LoVisualMainMenuScreen.BUTTON_ICONS.length;
        float totalWidth = LoVisualMainMenuScreen.BUTTON_SIZE * n + LoVisualMainMenuScreen.BUTTON_SPACING * (n - 1);
        float startX = (owner.fixedWidth - totalWidth) * 0.5f;
        float y = owner.fixedHeight * 0.5f + 34f * s + (1f - opacity) * 60f * s;
        for (int i = 0; i < n; i++) {
            float delay = i * 0.12f;
            float progress = Mth.clamp((opacity - delay) / (1f - delay * 0.5f), 0f, 1f);
            float eased = MainMenuPainter.easeOutCubic(progress);
            if (eased <= 0f) continue;
            renderCircleButton(i, startX + i * (LoVisualMainMenuScreen.BUTTON_SIZE + LoVisualMainMenuScreen.BUTTON_SPACING), y, eased * opacity);
        }
    }

    /** Liquid-glass circle + shadow + glyph for one button; {@code quit} tints red on hover. */
    void renderCircleButton(int index, float x, float y, float opacity) {
        if (opacity < 0.01f) return;
        float s = LoVisualMainMenuScreen.MENU_SCALE;
        float scale = owner.buttonScales[index];
        float radius = LoVisualMainMenuScreen.BUTTON_RADIUS * scale;
        float centerX = x + LoVisualMainMenuScreen.BUTTON_RADIUS;
        float centerY = y + LoVisualMainMenuScreen.BUTTON_RADIUS;
        float hover = owner.buttonHoverProgress[index];
        boolean quit = index == LoVisualMainMenuScreen.BUTTON_ICONS.length - 1;
        Themes.Theme theme = Theme.theme();
        int tint;
        if (quit) {
            tint = HudRenderUtil.mixColor(0xFFFF4C4C, 0xFFFF1F32, owner.quitHoverProgress * 0.55f + hover * 0.25f);
        } else {
            int baseTint = HudRenderUtil.mixColor(theme.accent(), 0xFFFFFFFF, 0.18f);
            tint = HudRenderUtil.mixColor(baseTint, 0xFFFFFFFF, hover * 0.10f);
        }
        int tintArgb = MainMenuPainter.withAlpha(tint, Math.round(opacity * (quit ? 235f : 220f)));
        int icon = quit
                ? MainMenuPainter.withAlpha(HudRenderUtil.mixColor(0xFFFFFFFF, 0xFFFFD6D6, owner.quitHoverProgress), Math.round(opacity * 255f))
                : MainMenuPainter.withAlpha(0xFFFFFF, Math.round(opacity * 255f));
        int shadow = MainMenuPainter.withAlpha(quit ? 0x1F0508 : 0x060810, Math.round(opacity * (quit ? 118f : 92f)));
        Renderer2D.COLOR.circleSoftShadow(centerX, centerY, radius, LoVisualMainMenuScreen.BUTTON_SHADOW, 0.052f + hover * 0.020f, shadow);
        Renderer2D.COLOR.liquidGlassCircle(centerX, centerY, radius, LoVisualMainMenuScreen.BUTTON_SOFTNESS, tintArgb, opacity,
                -20.0f, 1.0f, 1.0f, quit ? 0.66f : 0.58f, (0.100f + hover * 0.032f) * s, 0.0f);
        String svg = LoVisualMainMenuScreen.BUTTON_SVGS[index];
        if (svg != null && !svg.isBlank()) {
            float iconSize = 20.5f * s * scale;
            Renderer2D.COLOR.svg(svg, centerX - iconSize * 0.5f, centerY - iconSize * 0.5f - 0.45f * s, iconSize, iconSize, SvgRenderOptions.overrideColor(icon));
        } else {
            TextRenderer icons = MainMenuTextUtil.iconFont(TextRenderer.get());
            String glyph = LoVisualMainMenuScreen.BUTTON_ICONS[index];
            float iconSize = LoVisualMainMenuScreen.ICON_FONT * scale;
            float iconW = MainMenuTextUtil.measureWidth(icons, glyph, iconSize);
            float iconH = MainMenuTextUtil.measureHeight(icons, iconSize);
            MainMenuTextUtil.drawText(icons, glyph, centerX - iconW * 0.5f + 0.5f * s, centerY - iconH * 0.5f + LoVisualMainMenuScreen.ICON_Y_OFFSET, iconSize, icon);
        }
    }

    /** Rounded card under the button row prompting Microsoft re-auth for a specific account. */
    void renderAuthWarning(GuiGraphicsExtractor context, float opacity, long now) {
        AccountEntry entry = painter.getAuthWarningAccount();
        if (entry == null || opacity <= 0.01f) return;
        float s = LoVisualMainMenuScreen.MENU_SCALE;
        MainMenuPainter.Bounds b = painter.authWarningBounds(opacity);
        float hover = AnimationUtility.easeOutCubic(owner.authWarningHoverProgress);
        float y = b.y() - hover * 1.4f * s;
        Themes.Theme theme = Theme.theme();
        int shadow = MainMenuPainter.withAlpha(0x060810, Math.round(opacity * (96f + hover * 24f)));
        int top = HudRenderUtil.mixColor(MainMenuPainter.withAlpha(0x171B22, Math.round(opacity * 226f)), MainMenuPainter.withAlpha(theme.accent(), Math.round(opacity * 78f)), 0.16f + hover * 0.10f);
        int bottom = HudRenderUtil.mixColor(MainMenuPainter.withAlpha(0x0D1118, Math.round(opacity * 236f)), MainMenuPainter.withAlpha(theme.accent(), Math.round(opacity * 52f)), 0.08f + hover * 0.08f);
        int stroke = HudRenderUtil.mixColor(MainMenuPainter.withAlpha(0x46505D, Math.round(opacity * 170f)), MainMenuPainter.withAlpha(theme.accent(), Math.round(opacity * 218f)), 0.32f + hover * 0.28f);
        Renderer2D.COLOR.roundedRectSoftShadow(b.x(), y + 1.2f * s, b.w(), b.h(), LoVisualMainMenuScreen.AUTH_WARNING_R, 10f * s, 0.026f + hover * 0.014f, shadow);
        Renderer2D.COLOR.roundedRectGradientQuad(b.x(), y, b.w(), b.h(), LoVisualMainMenuScreen.AUTH_WARNING_R, 1f, top, top, bottom, bottom);
        Renderer2D.COLOR.roundedRectStrokeGradient(b.x() + 0.65f * s, y + 0.65f * s, b.w() - 1.3f * s, b.h() - 1.3f * s,
                LoVisualMainMenuScreen.AUTH_WARNING_R - 0.65f * s, 1f, 0.65f * s, HudRenderUtil.mixColor(stroke, 0xFFFFFFFF, 0.08f), stroke, 90f);
        float progress = (now % LoVisualMainMenuScreen.AUTH_WARNING_PROGRESS_MS) / (float) LoVisualMainMenuScreen.AUTH_WARNING_PROGRESS_MS;
        float lineX = b.x() + 8f * s, lineY = y + 4f * s, lineW = b.w() - 16f * s, lineH = 1.35f * s;
        Renderer2D.COLOR.roundedRect(lineX, lineY, lineW, lineH, lineH * 0.5f, 1f, MainMenuPainter.withAlpha(0xFFFFFF, Math.round(opacity * 42f)));
        Renderer2D.COLOR.roundedRect(lineX, lineY, lineW * (1f - progress), lineH, lineH * 0.5f, 1f, MainMenuPainter.withAlpha(theme.accent(), Math.round(opacity * 170f)));
        float headX = b.x() + 10f * s, headY = y + 10f * s;
        PlayerHeadRenderer.drawRounded(context, headX, headY, LoVisualMainMenuScreen.AUTH_WARNING_HEAD, 4f * s, SkinManager.getSkin(entry.getName()),
                new RenderColor(MainMenuPainter.withAlpha(0xFFFFFF, Math.round(opacity * 255f))), true,
                new RenderColor(MainMenuPainter.withAlpha(theme.accent(), Math.round(opacity * 120f))), 0.75f * s, false);
        TextRenderer title = MainMenuTextUtil.interBold(TextRenderer.get());
        TextRenderer body = MainMenuTextUtil.interMedium(title);
        float textX = headX + LoVisualMainMenuScreen.AUTH_WARNING_HEAD + 8f * s;
        MainMenuTextUtil.drawText(title, MainMenuPainter.tr("screen.lovisual.main_menu.auth_warning.title"), textX, y + 12.2f * s, 0.72f * s, MainMenuPainter.withAlpha(0xFFFFFF, Math.round(opacity * 245f)));
        String message = MainMenuPainter.tr("screen.lovisual.main_menu.auth_warning.body", entry.getName());
        MainMenuTextUtil.drawText(body, MainMenuTextUtil.ellipsize(body, message, 0.55f * s, b.x() + b.w() - textX - 10f * s), textX, y + 25.4f * s, 0.55f * s, MainMenuPainter.withAlpha(0xC8D0DC, Math.round(opacity * 225f)));
    }

    /** Route the warning-card click to the alt manager. Returns true when handled. */
    boolean handleAuthWarningClick(float fixedX, float fixedY, float menuProgress) {
        AccountEntry warningAccount = painter.getAuthWarningAccount();
        Minecraft mc = owner.mc();
        if (warningAccount == null || mc == null || !painter.canInteract(menuProgress)) return false;
        if (!painter.authWarningBounds(menuProgress).contains(fixedX, fixedY)) return false;
        ClientScreen.show(mc, new LoVisualAltManagerScreen(owner, true));
        return true;
    }
}
