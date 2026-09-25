package dev.loki.lovisual.features.gui.hud.draggable.impl.panels;

import dev.loki.lovisual.features.gui.hud.config.HudBgStyles;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.color.KeyBindValue;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.BindMode;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.hud.base.HudTextEffects;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.visibility.NoRender;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@HudElementRegister(order = 110)
public final class Scoreboard extends DraggableHudElement {

    {
        defaultLayout(1721.69f, 158.0f);
    }

    static final float BASE_PAD_X = 8.0f;
    static final float BASE_PAD_Y = 8.0f;
    static final float BASE_ROW_GAP = 2.0f;
    static final float BASE_HEADER_GAP = 8.0f;
    static final float BASE_SCORE_GAP = 8.0f;
    static final float BASE_FOOTER_GAP = 8.0f;
    static final float BASE_RADIUS = 4.0f;
    static final float BASE_SOFTNESS = 1.0f;
    static final float BASE_STROKE = 0.45f;
    static final float BASE_TITLE_SCALE = 0.78f;
    static final float BASE_TEXT_SCALE = 0.68f;
    static final float BASE_TITLE_LINE_GAP = 1.0f;

    final HudGlobalConfig hud = HudGlobalConfig.get();
    final KeyBindValue toggleBind = new KeyBindValue("toggle_bind", "Э");
    final NumberValue<Double> scale = new NumberValue<>("scoreboard_scale", 1.93, 0.5, 5.0);
    final ModeValue colorMode = new ModeValue("scoreboard_color_mode", "Theme", "Theme", "Custom");
    final ModeValue bgEffect = new ModeValue("scoreboard_bg_effect", HudBgStyles.AUTO, HudBgStyles.NONE, HudBgStyles.BLUR, HudBgStyles.AUTO);
    final NumberValue<Integer> blurAlpha = new NumberValue<>("scoreboard_blur_alpha", 255, 0, 255);
    final NumberValue<Integer> bgAlpha = new NumberValue<>("scoreboard_bg_alpha", 182, 0, 255);
    final RGBAColorValue bg = new RGBAColorValue("scoreboard_bg", "#820A0A0A");
    final RGBAColorValue bg2 = new RGBAColorValue("scoreboard_bg_secondary", "#B8141414");
    final RGBColorValue stroke = new RGBColorValue("scoreboard_stroke", "#4A4A4A");
    final RGBColorValue titleColor = new RGBColorValue("scoreboard_title_color", "#FFCC00");
    final RGBColorValue labelColor = new RGBColorValue("scoreboard_label_color", "#FFFFFF");
    final RGBColorValue valueColor = new RGBColorValue("scoreboard_value_color", "#FF4A4A");
    final RGBColorValue footerColor = new RGBColorValue("scoreboard_footer_color", "#FFD44A");
    final EnumValue<HudTextEffects.Effect> textEffect = new EnumValue<>("scoreboard_text_effect",
            HudTextEffects.Effect.STRIPE, HudTextEffects.Effect.NONE, HudTextEffects.Effect.MIX,
            HudTextEffects.Effect.FLOW, HudTextEffects.Effect.PULSE, HudTextEffects.Effect.STRIPE);
    final NumberValue<Integer> textEffectSpeed = new NumberValue<>("scoreboard_text_effect_speed", 18, 1, 60);
    final BooleanMapValue textEffectSections = new BooleanMapValue("scoreboard_text_effect_sections", defaultTextEffectSections());
    final List<SidebarLine> lines = new ArrayList<>();
    boolean toggleArmed = true;
    boolean toggleInit = false;
    int uiBgPrimary;
    int uiBgSecondary;
    int uiHeaderBg;
    int uiStroke;
    int uiTitleColor;
    int uiLabelColor;
    int uiValueColor;
    int uiFooterColor;
    float displayLabelWidth = -1.0f;
    float displayValueWidth = -1.0f;
    float displayFooterWidth = -1.0f;

    public Scoreboard() {
        super("scoreboard", "Scoreboard", false);
    }

    public static boolean shouldReplaceVanilla() {
        return DraggableHudElementRegistry.isEnabled(Scoreboard.class) && !isHiddenByNoRender();
    }

    static float smoothWidth(float current, float target) {
        if (current < 0.0f) return target;
        if (target >= current) return target;
        float next = AnimationUtility.approach(current, target, AnimationUtility.deltaTime(), 12.0f);
        return AnimationUtility.snap(next, target, 0.25f);
    }

    static boolean isHiddenByNoRender() {
        NoRender noRender = Modules.get(NoRender.class);
        return noRender != null && noRender.hideScoreboard();
    }

    boolean isHidden() {
        return isHiddenByNoRender();
    }

    private static Map<String, Boolean> defaultTextEffectSections() {
        Map<String, Boolean> sections = new LinkedHashMap<>();
        sections.put("header", true);
        sections.put("labels", false);
        sections.put("values", false);
        sections.put("footer", false);
        return sections;
    }

    @Override
    protected void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.bind(toggleBind, BindMode.PRESS));
        defs.add(SettingDef.number(scale));
        defs.add(SettingDef.mode(colorMode));
        defs.add(SettingDef.mode(bgEffect));
        defs.add(SettingDef.number(bgAlpha).visibleWhen(this::isThemeMode));
        defs.add(SettingDef.number(blurAlpha).visibleWhen(this::hasEffect));
        defs.add(SettingDef.color(bg).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.color(bg2).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(stroke).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(titleColor).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(labelColor).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(valueColor).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(footerColor).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.mode(textEffect));
        defs.add(SettingDef.number(textEffectSpeed).visibleWhen(() -> textEffect.get() != HudTextEffects.Effect.NONE));
        defs.add(SettingDef.group(textEffectSections).visibleWhen(() -> textEffect.get() != HudTextEffects.Effect.NONE));
    }

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        this.x = 16.0f;
        this.y = screenH * 0.5f - 96.0f;
    }

    @Override
    public boolean usesEngineRenderer() {
        return true;
    }

    @Override
    public HudPhase getHudPhase() {
        return HudPhase.BEFORE_CHAT;
    }

    @Override
    public void onTick() {
        dev.loki.lovisual.features.gui.hud.draggable.impl.panels.ScoreboardDataHelper.handleTick(this);
    }

    private void initToggleState() {
        toggleArmed = true;
        String combo = toggleBind.get();
        if (combo != null && !combo.isBlank() && !"NONE".equalsIgnoreCase(combo)
                && net.minecraft.client.Minecraft.getInstance() != null
                && dev.loki.lovisual.util.screen.ClientScreen.current() == null
                && dev.loki.lovisual.util.input.KeyManager.isComboHeldAllowScreen(combo)) {
            toggleArmed = false;
        }
    }

    private Objective resolveObjective() {
        return dev.loki.lovisual.features.gui.hud.draggable.impl.panels.ScoreboardDataHelper.resolveObjective();
    }

    private Component resolveTitle(Objective objective, boolean preview) {
        return dev.loki.lovisual.features.gui.hud.draggable.impl.panels.ScoreboardDataHelper.resolveTitle(objective, preview);
    }

    private List<SidebarLine> resolveLines(Objective objective, boolean preview) {
        return dev.loki.lovisual.features.gui.hud.draggable.impl.panels.ScoreboardDataHelper.resolveLines(objective, preview);
    }

    private void updatePalette() {
        dev.loki.lovisual.features.gui.hud.draggable.impl.panels.ScoreboardRenderHelper.updatePalette(this);
    }

    private void drawBackground(Renderer2D renderer, float x, float y, float w, float h, float radius, float s) {
        dev.loki.lovisual.features.gui.hud.draggable.impl.panels.ScoreboardRenderHelper.drawBackground(renderer, this, x, y, w, h, radius, s);
    }

    private void drawHeader(Renderer2D renderer, float x, float y, float w, float hh, float radius) {
        dev.loki.lovisual.features.gui.hud.draggable.impl.panels.ScoreboardRenderHelper.drawHeader(renderer, this, x, y, w, hh, radius);
    }

    private void drawBlur(float x, float y, float w, float h, float radius) {
        dev.loki.lovisual.features.gui.hud.draggable.impl.panels.ScoreboardRenderHelper.drawBlur(this, x, y, w, h, radius);
    }

    @Override
    public void renderEngine(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx,
                             float tickDelta, int screenW, int screenH) {
        dev.loki.lovisual.features.gui.hud.draggable.impl.panels.ScoreboardRenderHelper.render(this, renderer, textRenderer, ctx, tickDelta, screenW, screenH);
    }

    @Override
    public boolean supportsWidgetAnchoring() {
        return true;
    }

    boolean isThemeMode() {
        return "Theme".equals(colorMode.get());
    }

    boolean isCustomMode() {
        return "Custom".equals(colorMode.get());
    }

    boolean isBlurEffect() {
        return HudBgStyles.isBlur(bgEffect.get());
    }

    boolean hasEffect() {
        return HudBgStyles.hasEffect(bgEffect.get());
    }

    void setSize(float w, float h) {
        this.width = w;
        this.height = h;
    }

    record SidebarLine(Component label, Component value, boolean footer) {
    }
}
