/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl;

import com.mojang.blaze3d.platform.Window;
import dev.loki.lovisual.config.SettingDef;
import dev.loki.lovisual.config.values.BooleanValue;
import dev.loki.lovisual.config.values.ModeValue;
import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.events.impl.RenderPrewarmCollectEvent;
import dev.loki.lovisual.features.gui.hud.HudElementRegister;
import dev.loki.lovisual.features.gui.hud.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.ToastCardRenderer;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.ToastGeometry;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.ToastGeometry.MessengerSide;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.ToastGeometry.ToastMetrics;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.ToastGeometry.ToastPosition;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.ToastModel;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.ToastModel.PreviewToast;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.ToastModel.Toast;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.ToastModel.ToastDraw;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.ToastModel.ToastVisual;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.ToastStyle;
import dev.loki.lovisual.features.module.HudPhase;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleManager;
import dev.loki.lovisual.features.module.ModuleStateListener;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.FontInfo;
import dev.loki.lovisual.render.engine.text.Fonts;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.util.screen.ClientScreen;
import dev.loki.lovisual.util.wav.CustomSoundEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@HudElementRegister(order = 80)
public final class HudNotifier extends DraggableHudElement implements ModuleStateListener {

    private static final HudNotifier INSTANCE = new HudNotifier();

    private static final int MAX_TOASTS = 6;
    private static final float BASE_MARGIN = 9f;
    private static final float BASE_GAP = 3f;
    private static final float BASE_BOTTOM_OFFSET = 160f;
    private static final float LAYOUT_SMOOTH_SPEED = 14.0f;
    private static final float LAYOUT_SNAP_EPS = 0.12f;

    private final NumberValue<Integer> durationMs = new NumberValue<>("duration_ms", 2000, 500, 6000);
    private final NumberValue<Integer> fadeMs = new NumberValue<>("fade_ms", 250, 0, 1500);
    private final NumberValue<Double> scaleValue = new NumberValue<>("scale", 1.35, 0.5, 3.0);
    private final BooleanValue soundEnabled = new BooleanValue("sound", true);
    private final ModeValue soundMode = new ModeValue("sound_mode", "2", "1", "2", "3");
    private final NumberValue<Double> soundVolume = new NumberValue<>("sound_volume", 0.47, 0.05, 1.0);

    private final List<Toast> toasts = new ArrayList<>();
    private final RenderColor colorTmp = new RenderColor(0xFFFFFFFF);
    private boolean stackLayoutReady;
    private float stackWidthAnim;
    private float stackHeightAnim;

    private HudNotifier() {
        super("hud_notifier", "HudNotifier", true);
        ModuleManager.addListener(this);
    }

    public static void pushMessage(String text) {
        INSTANCE.enqueue(null, text, null, null);
    }

    public static void pushMessage(String text, NotifyType type) {
        INSTANCE.enqueue(null, text, null, type);
    }

    public static void pushState(String text, boolean enabled) {
        INSTANCE.enqueue(null, text, enabled, null);
    }

    public static void pushOrUpdateMessage(String key, String text) {
        INSTANCE.enqueue(key, text, null, null);
    }

    public static void pushOrUpdateMessage(String key, String text, NotifyType type) {
        INSTANCE.enqueue(key, text, null, type);
    }

    public static void clearMessage(String key) {
        INSTANCE.clearKey(key);
    }

    public static void renderEngine(HudPhase phase,
                                    Renderer2D renderer,
                                    TextRenderer textRenderer,
                                    GuiGraphicsExtractor ctx,
                                    float tickDelta) {
        INSTANCE.renderEngineInternal(renderer, textRenderer);
    }

    public static void renderPreview(Renderer2D renderer,
                                     TextRenderer textRenderer,
                                     float x,
                                     float y,
                                     float w,
                                     float h) {
        INSTANCE.renderPreviewInternal(renderer, textRenderer, x, y, w, h);
    }

    @Override
    protected void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.number(durationMs));
        defs.add(SettingDef.number(fadeMs));
        defs.add(SettingDef.number(scaleValue));
        defs.add(SettingDef.bool(soundEnabled));
        defs.add(SettingDef.mode(soundMode).visibleWhen(soundEnabled::get));
        defs.add(SettingDef.number(soundVolume).visibleWhen(soundEnabled::get));
    }

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        HudGlobalConfig hud = HudGlobalConfig.get();
        float scale = HudScale.scale(screenW, screenH);
        if (hud != null) {
            scale *= hud.getFontSize() / 18f;
        }
        scale *= getScale();

        float margin = BASE_MARGIN * scale;
        float width = 170f * scale;
        float height = ToastGeometry.BASE_BOX_HEIGHT * scale;
        // Position side is intentionally automatic now; the draggable position itself defines slide direction.
        this.x = Math.max(margin, screenW - margin - width);
        this.y = Math.max(margin, screenH - margin - BASE_BOTTOM_OFFSET * scale - height);
    }

    @Override
    public boolean usesEngineRenderer() {
        return true;
    }

    @Override
    public HudPhase getHudPhase() {
        return HudPhase.AFTER_SUBTITLES;
    }

    @Override
    public void renderEngine(Renderer2D renderer,
                             TextRenderer textRenderer,
                             GuiGraphicsExtractor ctx,
                             float tickDelta,
                             int screenW,
                             int screenH) {
        renderEngineInternal(renderer, textRenderer);
    }

    private void renderEngineInternal(Renderer2D renderer,
                                      TextRenderer textRenderer) {
        Minecraft mc = Minecraft.getInstance();

        if (mc == null || mc.level == null || mc.player == null) return;

        HudGlobalConfig hud = HudGlobalConfig.get();
        if (hud == null) return;

        boolean preview = DraggableHudElementRegistry.isForceVisible() || ClientScreen.current() instanceof ChatScreen;
        if (!enabledValue().get() && !preview) return;
        if (hud.getHeaderMode() == HudGlobalConfig.HeaderMode.NEVER && !preview) return;

        int duration = Math.max(1, getDurationMs());
        int fade = Math.max(0, getFadeMs());
        long now = System.currentTimeMillis();

        pruneExpiredToasts(now, duration);

        boolean hasRealToasts = !toasts.isEmpty();
        boolean sample = preview && !hasRealToasts;
        List<Toast> source = sample ? previewToasts(now) : toasts;

        if (source.isEmpty()) {
            resetStackLayout();
            setBounds(x, y, 0f, 0f);
            return;
        }

        Window window = mc.getWindow();
        int fbw = window.getWidth();
        int fbh = window.getHeight();
        int screenW = Math.max(1, Math.round(HudScale.virtualWidth(fbw, fbh)));
        int screenH = Math.max(1, Math.round(HudScale.virtualHeight(fbw, fbh)));

        float baseScale = HudScale.scale(screenW, screenH) * (hud.getFontSize() / 18f) * getScale();
        float gap = BASE_GAP * baseScale;

        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        TextRenderer lineRenderer = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, fallback);
        TextRenderer iconRenderer = Fonts.renderer("Icons", FontInfo.Type.Regular, fallback);
        if (lineRenderer == null) lineRenderer = fallback;
        if (iconRenderer == null) iconRenderer = fallback;
        float lineScale = baseScale * 0.95f;
        float iconScale = baseScale * 1.08f;

        List<ToastDraw> draws = new ArrayList<>();
        float maxW = 0f;
        float totalH = 0f;

        for (int i = source.size() - 1; i >= 0; i--) {
            Toast toast = source.get(i);
            float anim = sample ? 1.0f : toast.anim(now, duration, fade);
            if (anim <= 0.001f) continue;

            ToastMetrics metrics = ToastGeometry.measureToast(toast, lineRenderer, iconRenderer, lineScale, iconScale, baseScale);
            float remaining = sample ? 0.45f : toast.remaining(now, duration);
            draws.add(new ToastDraw(toast, metrics, remaining, anim, 0f, 0f));
            maxW = Math.max(maxW, metrics.boxW);
            totalH += metrics.boxH * anim;
            if (i > 0) {
                totalH += gap * anim;
            }
        }

        if (draws.isEmpty()) {
            resetStackLayout();
            setBounds(x, y, 0f, 0f);
            return;
        }

        LayoutSize layout = resolveStackLayout(maxW, totalH, sample);
        float layoutW = Math.max(maxW, layout.width());
        float layoutH = Math.max(totalH, layout.height());

        MessengerSide side = getSide();
        float cursorY = y;
        for (int i = 0; i < draws.size(); i++) {
            ToastDraw draw = draws.get(i);
            ToastMetrics metrics = draw.metrics;
            float targetX = switch (side) {
                case LEFT -> x;
                case TOP, BOTTOM -> x + (layoutW - metrics.boxW) * 0.5f;
                default -> x + (layoutW - metrics.boxW);
            };
            float targetY = cursorY;
            ToastPosition pos = resolveToastPosition(draw.toast, targetX, targetY, sample, baseScale);
            ToastPosition slide = ToastGeometry.toastSlideOffset(side, draw.anim, baseScale);
            draws.set(i, new ToastDraw(draw.toast, metrics, draw.remaining, draw.anim, pos.x() + slide.x(), pos.y() + slide.y()));
            cursorY += (metrics.boxH + gap) * draw.anim;
        }

        setBounds(x, y, layoutW, Math.max(1f, layoutH));

        ToastGeometry.drawGlassBlurBatch(draws, baseScale);
        for (ToastDraw draw : draws) {
            ToastCardRenderer.drawToastCard(
                    renderer,
                    colorTmp,
                    draw.toast,
                    lineRenderer,
                    iconRenderer,
                    draw.x,
                    draw.y,
                    draw.metrics,
                    draw.remaining,
                    draw.anim,
                    baseScale
            );
        }
    }

    private void pruneExpiredToasts(long now, int duration) {
        Iterator<Toast> it = toasts.iterator();
        while (it.hasNext()) {
            Toast t = it.next();
            if (t.isExpired(now, duration)) {
                it.remove();
            }
        }
    }

    private LayoutSize resolveStackLayout(float targetW, float targetH, boolean sample) {
        if (sample) {
            resetStackLayout();
            return new LayoutSize(targetW, targetH);
        }

        if (!stackLayoutReady) {
            stackWidthAnim = targetW;
            stackHeightAnim = targetH;
            stackLayoutReady = true;
            return new LayoutSize(targetW, targetH);
        }

        float dt = AnimationUtility.deltaTime();
        stackWidthAnim = AnimationUtility.approach(stackWidthAnim, targetW, dt, LAYOUT_SMOOTH_SPEED);
        stackHeightAnim = AnimationUtility.approach(stackHeightAnim, targetH, dt, LAYOUT_SMOOTH_SPEED);
        stackWidthAnim = AnimationUtility.snap(stackWidthAnim, targetW, LAYOUT_SNAP_EPS);
        stackHeightAnim = AnimationUtility.snap(stackHeightAnim, targetH, LAYOUT_SNAP_EPS);
        return new LayoutSize(stackWidthAnim, stackHeightAnim);
    }

    private ToastPosition resolveToastPosition(Toast toast, float targetX, float targetY, boolean sample, float baseScale) {
        if (toast == null || sample) {
            return new ToastPosition(targetX, targetY);
        }

        if (!toast.layoutReady) {
            toast.layoutX = targetX;
            toast.layoutY = targetY;
            toast.layoutReady = true;
            return new ToastPosition(targetX, targetY);
        }

        float dt = AnimationUtility.deltaTime();
        float eps = Math.max(0.05f, LAYOUT_SNAP_EPS * baseScale);
        toast.layoutX = AnimationUtility.approach(toast.layoutX, targetX, dt, LAYOUT_SMOOTH_SPEED);
        toast.layoutY = AnimationUtility.approach(toast.layoutY, targetY, dt, LAYOUT_SMOOTH_SPEED);
        toast.layoutX = AnimationUtility.snap(toast.layoutX, targetX, eps);
        toast.layoutY = AnimationUtility.snap(toast.layoutY, targetY, eps);
        return new ToastPosition(toast.layoutX, toast.layoutY);
    }

    private void resetStackLayout() {
        stackLayoutReady = false;
        stackWidthAnim = 0f;
        stackHeightAnim = 0f;
    }

    private List<Toast> previewToasts(long now) {
        return List.of(previewToast(now));
    }

    private Toast previewToast(long now) {
        PreviewToast preview = ToastModel.previewExample(now);
        long start = ToastModel.previewStart(now);
        return new Toast(ToastModel.PREVIEW_KEY, preview.line(), preview.type(), start, preview.visual(), preview.enabled());
    }

    private void renderPreviewInternal(Renderer2D renderer,
                                       TextRenderer textRenderer,
                                       float originX,
                                       float originY,
                                       float width,
                                       float height) {
        if (renderer == null) return;
        HudGlobalConfig hud = HudGlobalConfig.get();
        if (hud == null) return;
        if (width <= 2f || height <= 2f) return;

        int screenW = Math.max(1, Math.round(width));
        int screenH = Math.max(1, Math.round(height));

        float baseScale = HudScale.scale(screenW, screenH) * (hud.getFontSize() / 18f) * getScale();
        float margin = BASE_MARGIN * baseScale;
        float gap = BASE_GAP * baseScale;

        MessengerSide side = getSide();
        float cursorY = margin;

        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        TextRenderer lineRenderer = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, fallback);
        TextRenderer iconRenderer = Fonts.renderer("Icons", FontInfo.Type.Regular, fallback);
        if (lineRenderer == null) lineRenderer = fallback;
        if (iconRenderer == null) iconRenderer = fallback;
        float lineScale = baseScale * 0.95f;
        float iconScale = baseScale * 1.08f;

        long now = System.currentTimeMillis();
        int duration = Math.max(1, getDurationMs());
        int fade = Math.max(0, getFadeMs());
        pruneExpiredToasts(now, duration);

        boolean sampleMode = toasts.isEmpty();
        List<Toast> source = sampleMode ? previewToasts(now) : toasts;

        List<ToastDraw> draws = new ArrayList<>();
        float maxW = 0f;
        for (int i = source.size() - 1; i >= 0; i--) {
            Toast toast = source.get(i);
            float anim = sampleMode ? 1.0f : toast.anim(now, duration, fade);
            if (anim <= 0.001f) continue;

            ToastMetrics metrics = ToastGeometry.measureToast(toast, lineRenderer, iconRenderer, lineScale, iconScale, baseScale);
            float remaining = sampleMode ? 0.45f : toast.remaining(now, duration);
            maxW = Math.max(maxW, metrics.boxW);
            draws.add(new ToastDraw(toast, metrics, remaining, anim, 0f, 0f));
        }

        for (int i = 0; i < draws.size(); i++) {
            ToastDraw draw = draws.get(i);
            ToastMetrics metrics = draw.metrics;
            float targetX = switch (side) {
                case LEFT -> originX + margin;
                case TOP, BOTTOM -> originX + (screenW - metrics.boxW) * 0.5f;
                default -> originX + screenW - margin - metrics.boxW;
            };
            float targetY = originY + cursorY;
            ToastPosition slide = ToastGeometry.toastSlideOffset(side, draw.anim, baseScale);
            draws.set(i, new ToastDraw(draw.toast, metrics, draw.remaining, draw.anim, targetX + slide.x(), targetY + slide.y()));
            cursorY += (metrics.boxH + gap) * draw.anim;
        }

        ToastGeometry.drawGlassBlurBatch(draws, baseScale);
        for (ToastDraw draw : draws) {
            ToastCardRenderer.drawToastCard(
                    renderer,
                    colorTmp,
                    draw.toast,
                    lineRenderer,
                    iconRenderer,
                    draw.x,
                    draw.y,
                    draw.metrics,
                    draw.remaining,
                    draw.anim,
                    baseScale
            );
        }
    }

    @Override
    public void onModuleStateChanged(String name, boolean enabled) {
        Module module = ModuleManager.get(name);
        // HudNotifier module-state info logging disabled; keep only warnings/errors here.
        if (module == null) {
            // HudNotifier module-state info logging disabled; keep only warnings/errors here.
            return;
        }
        if (!module.isShownInModuleList()) {
            // HudNotifier module-state info logging disabled; keep only warnings/errors here.
            return;
        }
        enqueueModuleState(module.getDisplayName(), enabled);
    }

    private void enqueueModuleState(String text, boolean enabled) {
        enqueueInternal(null, text, enabled, enabled ? NotifyType.YES : NotifyType.NO, ToastVisual.MODULE_TOGGLE);
    }

    private void enqueue(String key, String text, Boolean enabled, NotifyType forcedType) {
        NotifyType type = forcedType != null ? forcedType : ToastStyle.resolveType(text, enabled);
        enqueueInternal(key, text, enabled, type, ToastVisual.STANDARD);
    }

    private void enqueueInternal(String key, String text, Boolean enabled, NotifyType type, ToastVisual visual) {
        if (text == null || text.isBlank()) return;

        HudGlobalConfig hud = HudGlobalConfig.get();
        if (hud == null) return;
        if (!enabledValue().get()) return;
        if (hud.getHeaderMode() == HudGlobalConfig.HeaderMode.NEVER) return;

        String line = ToastStyle.resolveLine(text, enabled);
        long now = System.currentTimeMillis();

        if (key != null) {
            for (Toast toast : toasts) {
                if (key.equals(toast.key)) {
                    toast.line = line;
                    toast.type = type;
                    toast.visual = visual == null ? ToastVisual.STANDARD : visual;
                    toast.stateEnabled = enabled;
                    toast.startMs = now;
                    return;
                }
            }
        }

        toasts.add(new Toast(key, line, type, now, visual, enabled));
        if (toasts.size() > MAX_TOASTS) {
            toasts.remove(0);
        }

        if (enabled != null) {
            playSound(enabled);
        }
    }

    private int getDurationMs() {
        return durationMs.get();
    }

    private int getFadeMs() {
        return fadeMs.get();
    }

    private MessengerSide getSide() {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc == null ? null : mc.getWindow();
        if (window == null) return MessengerSide.RIGHT;
        int fbw = Math.max(1, window.getWidth());
        int fbh = Math.max(1, window.getHeight());
        float screenW = HudScale.virtualWidth(fbw, fbh);
        float screenH = HudScale.virtualHeight(fbw, fbh);
        float cx = x + Math.max(1f, getWidth()) * 0.5f;
        float cy = y + Math.max(1f, getHeight()) * 0.5f;
        if (cy < screenH * 0.18f) return MessengerSide.TOP;
        if (cy > screenH * 0.82f) return MessengerSide.BOTTOM;
        return cx < screenW * 0.50f ? MessengerSide.LEFT : MessengerSide.RIGHT;
    }

    private float getScale() {
        return scaleValue.get().floatValue();
    }

    private float getSoundVolume() {
        return soundVolume.get().floatValue();
    }

    private void playSound(boolean enabled) {
        if (ModuleManager.isToggleSoundSuppressed()) return;
        if (!soundEnabled.get()) return;
        Identifier snd = resolveSound(enabled, soundMode.get());
        if (snd == null) return;
        double vol = Math.min(1.0, getSoundVolume());
        CustomSoundEngine.get().play(snd, vol, 1.0, false, true);
    }

    private void clearKey(String key) {
        if (key == null) return;
        toasts.removeIf(t -> key.equals(t.key));
    }

    private Identifier resolveSound(boolean enabled, String mode) {
        String base = switch (mode) {
            case "2" -> "enable/enable2";
            case "3" -> "enable/enable3";
            default -> "enable/enable1";
        };
        if (!enabled) {
            base = base.replace("enable/", "disable/").replace("enable", "disable");
        }
        return Identifier.fromNamespaceAndPath("lovisual", "sounds/" + base + ".wav");
    }

    public enum NotifyType {
        YES,
        NO,
        WARN,
        INFO
    }

    private record LayoutSize(float width, float height) {
    }

    @Override
    public void collectRenderPrewarm(RenderPrewarmCollectEvent event) {
        super.collectRenderPrewarm(event);
        event.svg("toggle-left")
                .svg("toggle-right")
                .svg("toggle-track")
                .svg("toggle-thumb");
    }

}
