package dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier;

import com.mojang.blaze3d.platform.Window;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.ToastGeometry.MessengerSide;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.ToastGeometry.ToastMetrics;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.ToastGeometry.ToastPosition;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastModel.Toast;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model.ToastModel.ToastDraw;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.runtime.HudNotifierRuntime;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the HudNotifier toast stack, both in-game and inside the drag-preview panel.
 * <p>
 * Also owns the smooth stack-size animation and the per-toast layout lerp so cards
 * glide to their target slot instead of snapping. Reads the live queue from
 * {@link HudNotifierRuntime}.
 */
final class HudNotifierPainter {

    private static final float LAYOUT_SMOOTH_SPEED = 14.0f;
    private static final float LAYOUT_SNAP_EPS = 0.12f;

    private final HudNotifier owner;
    private final HudNotifierRuntime runtime;
    private final RenderColor colorTmp = new RenderColor(0xFFFFFFFF);
    private boolean stackLayoutReady;
    private float stackWidthAnim, stackHeightAnim;
    private TextRenderer lineRenderer, iconRenderer;
    private float baseScale, lineScale, iconScale;

    HudNotifierPainter(HudNotifier owner, HudNotifierRuntime runtime) {
        this.owner = owner;
        this.runtime = runtime;
    }

    /** Animated stack dimensions consumed by the drag hit-test / bounding box. */
    record LayoutSize(float width, float height) { }

    /** Draw the live toast stack anchored at the module's current screen position. */
    void renderEngine(Renderer2D renderer, TextRenderer textRenderer) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return;
        HudGlobalConfig hud = HudGlobalConfig.get();
        if (hud == null) return;
        boolean preview = DraggableHudElementRegistry.isForceVisible() || ClientScreen.current() instanceof ChatScreen;
        if (!owner.enabledValue().get() && !preview) return;
        if (hud.getHeaderMode() == HudGlobalConfig.HeaderMode.NEVER && !preview) return;
        int duration = Math.max(1, owner.getDurationMs()), fade = Math.max(0, owner.getFadeMs());
        long now = System.currentTimeMillis();
        runtime.pruneExpiredToasts(now, duration);
        float x = owner.getX(), y = owner.getY();
        List<Toast> source = runtime.toasts();
        boolean sample = preview && source.isEmpty();
        if (sample) source = previewToasts(now);
        if (source.isEmpty()) { resetStackLayout(); owner.applyBounds(x, y, 0f, 0f); return; }
        Window window = mc.getWindow();
        int fbw = window.getWidth(), fbh = window.getHeight();
        setupRenderers(textRenderer, hud, Math.max(1, Math.round(HudScale.virtualWidth(fbw, fbh))), Math.max(1, Math.round(HudScale.virtualHeight(fbw, fbh))));
        float[] mwTh = new float[2];
        List<ToastDraw> draws = buildDraws(source, sample, duration, fade, now, true, mwTh);
        if (draws.isEmpty()) { resetStackLayout(); owner.applyBounds(x, y, 0f, 0f); return; }
        LayoutSize layout = resolveStackLayout(mwTh[0], mwTh[1], sample);
        float layoutW = Math.max(mwTh[0], layout.width()), layoutH = Math.max(mwTh[1], layout.height());
        MessengerSide side = owner.getSide();
        float gap = HudNotifier.BASE_GAP * baseScale, cursorY = y;
        for (int i = 0; i < draws.size(); i++) {
            ToastDraw draw = draws.get(i);
            ToastMetrics metrics = draw.metrics;
            float targetX = switch (side) {
                case LEFT -> x;
                case TOP, BOTTOM -> x + (layoutW - metrics.boxW) * 0.5f;
                default -> x + (layoutW - metrics.boxW);
            };
            ToastPosition pos = resolveToastPosition(draw.toast, targetX, cursorY, sample, baseScale);
            ToastPosition slide = ToastGeometry.toastSlideOffset(side, draw.anim, baseScale);
            draws.set(i, new ToastDraw(draw.toast, metrics, draw.remaining, draw.anim, pos.x() + slide.x(), pos.y() + slide.y()));
            cursorY += (metrics.boxH + gap) * draw.anim;
        }
        owner.applyBounds(x, y, layoutW, Math.max(1f, layoutH));
        drawCards(renderer, draws);
    }

    /** Draw the toast stack inside the given rect used by the clickgui drag preview. */
    void renderPreview(Renderer2D renderer, TextRenderer textRenderer, float originX, float originY, float width, float height) {
        if (renderer == null || width <= 2f || height <= 2f) return;
        HudGlobalConfig hud = HudGlobalConfig.get();
        if (hud == null) return;
        setupRenderers(textRenderer, hud, Math.max(1, Math.round(width)), Math.max(1, Math.round(height)));
        int screenW = Math.max(1, Math.round(width));
        float margin = HudNotifier.BASE_MARGIN * baseScale, gap = HudNotifier.BASE_GAP * baseScale;
        long now = System.currentTimeMillis();
        int duration = Math.max(1, owner.getDurationMs()), fade = Math.max(0, owner.getFadeMs());
        runtime.pruneExpiredToasts(now, duration);
        boolean sampleMode = runtime.toasts().isEmpty();
        List<Toast> source = sampleMode ? previewToasts(now) : runtime.toasts();
        List<ToastDraw> draws = buildDraws(source, sampleMode, duration, fade, now, false, null);
        MessengerSide side = owner.getSide();
        float cursorY = margin;
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
        drawCards(renderer, draws);
    }

    private void setupRenderers(TextRenderer textRenderer, HudGlobalConfig hud, int screenW, int screenH) {
        baseScale = HudScale.scale(screenW, screenH) * (hud.getFontSize() / 18f) * owner.getScale();
        lineScale = baseScale * 0.95f;
        iconScale = baseScale * 1.08f;
        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        lineRenderer = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, fallback);
        iconRenderer = Fonts.renderer("Icons", FontInfo.Type.Regular, fallback);
        if (lineRenderer == null) lineRenderer = fallback;
        if (iconRenderer == null) iconRenderer = fallback;
    }

    private List<ToastDraw> buildDraws(List<Toast> source, boolean sample, int duration, int fade, long now, boolean wantMeasure, float[] outMaxWTotalH) {
        List<ToastDraw> draws = new ArrayList<>();
        float gap = HudNotifier.BASE_GAP * baseScale, maxW = 0f, totalH = 0f;
        for (int i = source.size() - 1; i >= 0; i--) {
            Toast toast = source.get(i);
            float anim = sample ? 1.0f : toast.anim(now, duration, fade);
            if (anim <= 0.001f) continue;
            ToastMetrics m = ToastGeometry.measureToast(toast, lineRenderer, iconRenderer, lineScale, iconScale, baseScale);
            float remaining = sample ? 0.45f : toast.remaining(now, duration);
            draws.add(new ToastDraw(toast, m, remaining, anim, 0f, 0f));
            if (wantMeasure) {
                maxW = Math.max(maxW, m.boxW);
                totalH += m.boxH * anim;
                if (i > 0) totalH += gap * anim;
            }
        }
        if (outMaxWTotalH != null) { outMaxWTotalH[0] = maxW; outMaxWTotalH[1] = totalH; }
        return draws;
    }

    private void drawCards(Renderer2D renderer, List<ToastDraw> draws) {
        ToastGeometry.drawGlassBlurBatch(draws, baseScale);
        for (ToastDraw d : draws) {
            ToastCardRenderer.drawToastCard(renderer, colorTmp, d.toast, lineRenderer, iconRenderer, d.x, d.y, d.metrics, d.remaining, d.anim, baseScale);
        }
    }
    private List<Toast> previewToasts(long now) { return List.of(HudNotifierRuntime.previewToast(now)); }
    private LayoutSize resolveStackLayout(float targetW, float targetH, boolean sample) {
        if (sample) { resetStackLayout(); return new LayoutSize(targetW, targetH); }
        if (!stackLayoutReady) { stackWidthAnim = targetW; stackHeightAnim = targetH; stackLayoutReady = true; return new LayoutSize(targetW, targetH); }
        float dt = AnimationUtility.deltaTime();
        stackWidthAnim = AnimationUtility.snap(AnimationUtility.approach(stackWidthAnim, targetW, dt, LAYOUT_SMOOTH_SPEED), targetW, LAYOUT_SNAP_EPS);
        stackHeightAnim = AnimationUtility.snap(AnimationUtility.approach(stackHeightAnim, targetH, dt, LAYOUT_SMOOTH_SPEED), targetH, LAYOUT_SNAP_EPS);
        return new LayoutSize(stackWidthAnim, stackHeightAnim);
    }
    private ToastPosition resolveToastPosition(Toast toast, float targetX, float targetY, boolean sample, float scale) {
        if (toast == null || sample) return new ToastPosition(targetX, targetY);
        if (!toast.layoutReady) { toast.layoutX = targetX; toast.layoutY = targetY; toast.layoutReady = true; return new ToastPosition(targetX, targetY); }
        float dt = AnimationUtility.deltaTime(), eps = Math.max(0.05f, LAYOUT_SNAP_EPS * scale);
        toast.layoutX = AnimationUtility.snap(AnimationUtility.approach(toast.layoutX, targetX, dt, LAYOUT_SMOOTH_SPEED), targetX, eps);
        toast.layoutY = AnimationUtility.snap(AnimationUtility.approach(toast.layoutY, targetY, dt, LAYOUT_SMOOTH_SPEED), targetY, eps);
        return new ToastPosition(toast.layoutX, toast.layoutY);
    }
    private void resetStackLayout() { stackLayoutReady = false; stackWidthAnim = 0f; stackHeightAnim = 0f; }
}
