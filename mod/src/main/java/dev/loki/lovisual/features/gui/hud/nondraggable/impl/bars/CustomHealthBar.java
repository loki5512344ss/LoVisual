package dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars;

import java.util.List;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.hud.base.AbstractHudElement;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import static dev.loki.lovisual.features.theme.Theme.theme;

@HudElementRegister(order = 50)
public final class CustomHealthBar extends AbstractHudElement {

    private static final String STYLE_TARGET_HUD = "TargetHud";
    private static final String STYLE_DYNAMIC = "Dynamic";
    public static final CustomHealthBar INSTANCE = new CustomHealthBar();
    private static final float HEALTH_BAR_WIDTH = 81f;
    private static final float HEALTH_BAR_HEIGHT = 9f;

    private final NumberValue<Integer> healthAlpha =
            new NumberValue<>("health_alpha", 255, 30, 255);
    private final ModeValue hotbarHealthBarStyle =
            new ModeValue("health_bar_style", "TargetHud", STYLE_TARGET_HUD, STYLE_DYNAMIC);
    private final NumberValue<Float> customBarRadius =
            new NumberValue<>("custom_bar_radius", 5.0f, 0.0f, 20.0f);
    private final NumberValue<Float> customTextScale =
            new NumberValue<>("custom_text_scale", 0.24f, 0.12f, 0.6f);
    private final NumberValue<Integer> customTextPadding =
            new NumberValue<>("custom_text_padding", 3, 0, 12);

    private CustomHealthBar() {
        super("vanilla_health", "Health", true);
    }

    public static CustomHealthBar get() {
        return INSTANCE;
    }

    public static boolean shouldRenderHealthBar(Minecraft mc, LocalPlayer player, float maxHealth) {
        if (player == null) return false;
        if (maxHealth <= 0.0f) return false;
        if (mc == null || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return false;

        CustomHealthBar bm = CustomHealthBar.get();
        if (bm == null || !bm.isHudHealthEnabled()) return false;
        return true;
    }

    public static HealthTextInfo renderHealthBarBackground(Renderer2D r2d,
                                                           Minecraft mc,
                                                           LocalPlayer player,
                                                           int x,
                                                           int y,
                                                           float maxHealth) {
        return HealthBarPainter.renderBackground(r2d, mc, player, x, y, maxHealth);
    }

    public static void renderHealthBarText(HealthTextInfo info) {
        HealthBarPainter.renderText(info);
    }

    @Override
    protected void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.number(healthAlpha));
        defs.add(SettingDef.mode(hotbarHealthBarStyle));
        defs.add(SettingDef.number(customBarRadius));
        defs.add(SettingDef.number(customTextScale));
        defs.add(SettingDef.number(customTextPadding));
    }

    public boolean isHudHealthEnabled() {
        return isEnabled();
    }

    public float getHudHealthAlphaFactor() {
        return getHudHealthAlpha() / 255f;
    }

    public int getHudHealthAlpha() {
        int v = healthAlpha.get();
        if (v < 0) return 0;
        if (v > 255) return 255;
        return v;
    }

    public float getBarWidth() {
        return HEALTH_BAR_WIDTH;
    }

    public float getBarHeight() {
        return HEALTH_BAR_HEIGHT;
    }

    public float getBarRadius() {
        return customBarRadius.get();
    }

    float getTextScale() {
        return customTextScale.get();
    }

    int getTextPadding() {
        return customTextPadding.get();
    }

    int getTextArgb() {
        return 0xFF000000 | (theme().textPrimary() & 0x00FFFFFF);
    }

    boolean isDynamicHealthColorMode() {
        return STYLE_DYNAMIC.equals(hotbarHealthBarStyle.get());
    }

    int themeAccentArgb() {
        return 0xFF000000 | (theme().accent() & 0x00FFFFFF);
    }

    int themeAccentSoftArgb() {
        return 0xFF000000 | (theme().accentSoft() & 0x00FFFFFF);
    }

    int themeTextPrimaryArgb() {
        return 0xFF000000 | (theme().textPrimary() & 0x00FFFFFF);
    }

    public static final class HealthTextInfo {
        public final String text;
        public final int x;
        public final int y;
        public final int color;
        public final float scale;

        HealthTextInfo(String text, int x, int y, int color, float scale) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.scale = scale;
        }
    }
}
