package dev.loki.lovisual.features.gui.hud.draggable.impl;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.hud.registry.HudElementInfo;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.base.HudTextEffects;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.model.CapturePolicy;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.model.DisplayState;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.TriangulatorSolver.CachedThrow;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.TriangulatorSolver.SolveResult;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.TriangulatorSolver;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.TriangulatorTracker;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.TriangulatorView;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.render.TriangulatorMeasure;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.render.TriangulatorPainter;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.text.edit.ClipboardUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.Level;
import dev.loki.lovisual.util.time.GameClock;

import java.util.LinkedHashMap;
import java.util.List;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedTriangulatorHudPanel;

@HudElementInfo(
        id = "triangulator",
        displayName = "Triangulator",
        enabledByDefault = false,
        order = 940
)
public final class Triangulator extends DraggableHudElement {

    // Baked HUD layout default from user cfg.
    {
        defaultLayout(1302.04f, 330.69208f);
    }

    private static final String COLOR_THEME = "Theme";
    private static final String COLOR_CUSTOM = "Custom";
    private static final String EFFECT_NONE = "None";
    private static final String EFFECT_BLUR = "Blur";

    private final HudGlobalConfig hud = HudGlobalConfig.get();
    private final ScriptedTriangulatorHudPanel scriptedPanel = new ScriptedTriangulatorHudPanel();
    private final TriangulatorTracker tracker = new TriangulatorTracker();
    private final TriangulatorView view = new TriangulatorView();

    private final NumberValue<Double> scaleValue = new NumberValue<>("triangulator_scale", 1.68, 0.5, 5.0);
    private final ModeValue colorMode = new ModeValue("triangulator_color_mode", "Theme", COLOR_THEME, COLOR_CUSTOM);
    private final ModeValue bgEffect = new ModeValue("triangulator_bg_effect", "Blur", EFFECT_NONE, EFFECT_BLUR);
    private final RGBAColorValue bg = new RGBAColorValue("triangulator_bg", "#F7343434");
    private final RGBAColorValue bg2 = new RGBAColorValue("triangulator_bg_secondary", "#F7161616");
    private final NumberValue<Integer> bgAlpha = new NumberValue<>("triangulator_bg_alpha", 225, 0, 255);
    private final RGBColorValue stroke = new RGBColorValue("triangulator_stroke", "#5A5A5A");
    private final RGBColorValue text = new RGBColorValue("triangulator_text", "#FFFFFF");
    private final RGBColorValue muted = new RGBColorValue("triangulator_muted", "#A5A5A5");
    private final NumberValue<Integer> blurAlpha = new NumberValue<>("triangulator_blur_alpha", 140, 0, 255);
    private final EnumValue<HudTextEffects.Effect> iconEffect =
            new EnumValue<>("triangulator_icon_effect", HudTextEffects.Effect.NONE,
                    HudTextEffects.Effect.NONE, HudTextEffects.Effect.MIX, HudTextEffects.Effect.FLOW,
                    HudTextEffects.Effect.PULSE, HudTextEffects.Effect.STRIPE);
    private final NumberValue<Integer> iconEffectSpeed = new NumberValue<>("triangulator_icon_effect_speed", 18, 1, 60);
    private final NumberValue<Integer> minObservationTicks =
            new NumberValue<>("triangulator_min_observation_ticks", 6, 2, 20);
    private final NumberValue<Double> stabilityThreshold =
            new NumberValue<>("triangulator_stability_threshold", 0.99, 0.900, 0.999);
    private final NumberValue<Double> minPairAngleDeg = new NumberValue<>("triangulator_min_pair_angle", 6.0, 0.25, 35.0);
    private final NumberValue<Double> autoResetDistance =
            new NumberValue<>("triangulator_auto_reset_distance", 96.0, 16.0, 512.0);
    private final NumberValue<Integer> maxCachedThrows =
            new NumberValue<>("triangulator_max_cached_throws", 6, 2, 12);
    private final NumberValue<Integer> cacheLifetimeSec =
            new NumberValue<>("triangulator_cache_lifetime", 180, 15, 1800);
    private final BooleanValue autoReset = new BooleanValue("triangulator_auto_reset", true);

    private float displayWidth = -1.0f;
    private float displayHeight = -1.0f;
    private float visibilityAnim = 0.0f;
    private TriangulatorPainter.HeaderButtons headerButtons = TriangulatorPainter.HeaderButtons.NONE;
    private SolveResult currentResult;

    public Triangulator() {
        super("triangulator", "Triangulator", false);
    }

    @Override
    protected void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.number(scaleValue));
        defs.add(SettingDef.mode(colorMode));
        defs.add(SettingDef.color(bg).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.color(bg2).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(stroke).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.number(bgAlpha).visibleWhen(this::isThemeMode));
        defs.add(SettingDef.colorNoAlpha(text).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.colorNoAlpha(muted).visibleWhen(this::isCustomMode));
        defs.add(SettingDef.mode(bgEffect));
        defs.add(SettingDef.number(blurAlpha).visibleWhen(this::hasEffect));
        defs.add(SettingDef.mode(iconEffect));
        defs.add(SettingDef.number(iconEffectSpeed)
                .visibleWhen(() -> iconEffect.get() != HudTextEffects.Effect.NONE));
        defs.add(SettingDef.number(minObservationTicks));
        defs.add(SettingDef.number(stabilityThreshold));
        defs.add(SettingDef.number(minPairAngleDeg));
        defs.add(SettingDef.bool(autoReset));
        defs.add(SettingDef.number(autoResetDistance).visibleWhen(autoReset::get));
        defs.add(SettingDef.number(maxCachedThrows));
        defs.add(SettingDef.number(cacheLifetimeSec));
    }

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        this.x = screenW - 184.0f;
        this.y = 140.0f;
    }

    @Override
    public boolean usesEngineRenderer() {
        return true;
    }

    @Override
    public boolean isMouseOverInteractive(float mx, float my) {
        TriangulatorPainter.HeaderButtons b = headerButtons;
        return (b.clearSize() > 0.0f && tracker.throwsCount() > 0 && hit(mx, my, b.clearX(), b.clearY(), b.clearSize()))
                || (b.copySize() > 0.0f && currentCopyResult() != null && hit(mx, my, b.copyX(), b.copyY(), b.copySize()));
    }

    @Override
    public boolean onMouseClicked(float mx, float my, int button) {
        if (button != 0) {
            return false;
        }
        SolveResult copyResult = currentCopyResult();
        if (copyResult != null && headerButtons.copySize() > 0.0f
                && hit(mx, my, headerButtons.copyX(), headerButtons.copyY(), headerButtons.copySize())) {
            ClipboardUtil.copy(TriangulatorSolver.formatCopyCoordinates(copyResult));
            return true;
        }
        if (tracker.throwsCount() > 0 && headerButtons.clearSize() > 0.0f
                && hit(mx, my, headerButtons.clearX(), headerButtons.clearY(), headerButtons.clearSize())) {
            clearAllState("Manual clear", true, System.currentTimeMillis());
            return true;
        }
        return false;
    }

    @Override
    public void onTick() {
        if (!isEnabled()) {
            tracker.dropEyes();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) {
            tracker.dropEyes();
            currentResult = solve(tracker.cachedThrows());
            return;
        }

        // Кэш-экспирация и worldTime-сброс живут внутри tracker.tick
        currentResult = tracker.tick(mc.level, mc.player, capturePolicy(), currentResult);
    }

    @Override
    public void renderEngine(Renderer2D renderer,
                             TextRenderer textRenderer,
                             GuiGraphicsExtractor ctx,
                             float tickDelta,
                             int screenW,
                             int screenH) {
        boolean preview = DraggableHudElementRegistry.isForceVisible();
        Minecraft mc = Minecraft.getInstance();
        if (!preview && (!isEnabled() || mc == null || mc.player == null || mc.level == null)) {
            width = 0.0f;
            height = 0.0f;
            return;
        }

        view.update(isThemeMode(), bgAlpha.get(), bg.getArgb(), bg2.getArgb(), stroke.getArgb(), text.getArgb(), muted.getArgb());

        boolean showPreview = preview && tracker.throwsCount() == 0;
        List<CachedThrow> displayThrows = showPreview ? TriangulatorSolver.PREVIEW_THROWS : tracker.cachedThrows();
        SolveResult displayResult = showPreview ? TriangulatorSolver.previewResult() : currentResult;
        if (displayResult == null && !displayThrows.isEmpty()) {
            displayResult = solve(displayThrows);
        }
        boolean showClearButton = !displayThrows.isEmpty();
        boolean showCopyButton = displayResult != null && displayResult.hasSolution();
        DisplayState state = view.status(displayThrows, displayResult, preview,
                tracker.trackingEye(), tracker.resetReason(), tracker.resetMs(), isNether());

        visibilityAnim = HudRenderUtil.animateVisibility(visibilityAnim, true);
        float widgetScale = HudRenderUtil.visibilityScale(visibilityAnim);

        TextRenderer headerTextRenderer = Fonts.renderer("Inter", FontInfo.Type.Bold, TextRenderer.get());
        TextRenderer rowTextRenderer = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, TextRenderer.get());
        if (headerTextRenderer == null) headerTextRenderer = textRenderer;
        if (rowTextRenderer == null) rowTextRenderer = textRenderer;

        float baseScale = HudScale.scale(screenW, screenH) * 1.1f * scaleValue.get().floatValue();
        float fontScale = 0.98f * (hud.getFontSize() / 18.0f);
        TriangulatorMeasure.Metrics metrics = TriangulatorMeasure.measure(headerTextRenderer, rowTextRenderer,
                baseScale, fontScale, displayThrows, state.statusText(), state.primaryLine(), state.secondaryLine(),
                showClearButton, showCopyButton);

        displayWidth = HudRenderUtil.animateDimension(displayWidth, metrics.maxWidth());
        displayHeight = HudRenderUtil.animateDimension(displayHeight, metrics.targetHeight());
        width = displayWidth;
        height = displayHeight;

        float drawScale = Math.max(0.0f, widgetScale);
        float drawWidth = displayWidth * drawScale;
        float drawHeight = displayHeight * drawScale;
        if (drawWidth <= 0.0f || drawHeight <= 0.0f) {
            width = 0.0f;
            height = 0.0f;
            return;
        }
        float drawX = x + ((displayWidth - drawWidth) * 0.5f);
        float drawY = y + ((displayHeight - drawHeight) * 0.5f);
        float drawBaseScale = baseScale * drawScale;

        headerButtons = TriangulatorPainter.placeButtons(metrics, drawX, drawY, drawWidth, drawHeight,
                drawBaseScale, showClearButton, showCopyButton);
        int headerIconColor = TriangulatorPainter.headerIconColor(view.counter(), iconEffect.get(), iconEffectSpeed.get());
        List<LinkedHashMap<String, Object>> rows = TriangulatorPainter.buildRows(displayThrows, displayResult, view,
                autoResetDistance.get());
        scriptedPanel.render(renderer, textRenderer, ctx, tickDelta, TriangulatorPainter.panel(view, metrics,
                drawX, drawY, drawWidth, drawHeight, drawScale, drawBaseScale, fontScale * drawScale,
                displayThrows.size(), hasEffect(), view.blurStrength(isThemeMode(), blurAlpha.get()),
                headerIconColor, showClearButton, showCopyButton, state, rows));
    }

    public XaeroSnapshot getXaeroSnapshot() {
        SolveResult result = currentResult != null ? currentResult : solve(tracker.cachedThrows());
        if (result == null || !result.hasSolution()) {
            return null;
        }
        return new XaeroSnapshot(result.x(), result.z(), result.ready(), result.confidence(), tracker.throwsCount());
    }

    public record XaeroSnapshot(double overworldX,
                                double overworldZ,
                                boolean ready,
                                double confidence,
                                int eyeCount) {
    }

    private SolveResult currentCopyResult() {
        SolveResult result = currentResult != null ? currentResult : solve(tracker.cachedThrows());
        return result != null && result.hasSolution() ? result : null;
    }

    private CapturePolicy capturePolicy() {
        return new CapturePolicy(minObservationTicks.get(), stabilityThreshold.get(),
                minPairAngleDeg.get(), autoReset.get(), autoResetDistance.get(), maxCachedThrows.get(),
                cacheLifetimeSec.get() * 1000L);
    }

    private SolveResult solve(List<CachedThrow> throwsToSolve) {
        return TriangulatorSolver.solve(throwsToSolve, minPairAngleDeg.get());
    }

    private void clearAllState(String reason, boolean rememberReason, long now) {
        tracker.clear(reason, rememberReason, now);
        currentResult = null;
    }

    private boolean isThemeMode() {
        return COLOR_THEME.equals(colorMode.get());
    }

    private boolean isCustomMode() {
        return COLOR_CUSTOM.equals(colorMode.get());
    }

    private boolean hasEffect() {
        return !EFFECT_NONE.equals(bgEffect.get());
    }

    private boolean isNether() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.level != null && mc.level.dimension() == Level.NETHER;
    }

    private static boolean hit(float mx, float my, float x, float y, float size) {
        return mx >= x && mx <= x + size && my >= y && my <= y + size;
    }
}
