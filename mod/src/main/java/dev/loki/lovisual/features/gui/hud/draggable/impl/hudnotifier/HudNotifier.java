package dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier;

import com.mojang.blaze3d.platform.Window;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.events.impl.render.RenderPrewarmCollectEvent;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.ToastGeometry.MessengerSide;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.runtime.HudNotifierRuntime;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleManager;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.features.module.phase.ModuleStateListener;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * Public facade for the toast-based HUD notification module.
 * <p>
 * Owns the six {@link dev.loki.lovisual.config.values.ConfigValue}s, the static
 * push / render API and the {@link DraggableHudElement} lifecycle hooks. The mutable
 * toast queue lives in {@link HudNotifierRuntime} (subpackage) and every draw path
 * is delegated to {@link HudNotifierPainter}.
 */
@HudElementRegister(order = 80)
public final class HudNotifier extends DraggableHudElement implements ModuleStateListener {

    private static final HudNotifier INSTANCE = new HudNotifier();

    static final float BASE_MARGIN = 9f;
    static final float BASE_GAP = 3f;
    static final float BASE_BOTTOM_OFFSET = 160f;

    private final NumberValue<Integer> durationMs = new NumberValue<>("duration_ms", 2000, 500, 6000);
    private final NumberValue<Integer> fadeMs = new NumberValue<>("fade_ms", 250, 0, 1500);
    private final NumberValue<Double> scaleValue = new NumberValue<>("scale", 1.35, 0.5, 3.0);
    private final BooleanValue soundEnabled = new BooleanValue("sound", true);
    private final ModeValue soundMode = new ModeValue("sound_mode", "2", "1", "2", "3");
    private final NumberValue<Double> soundVolume = new NumberValue<>("sound_volume", 0.47, 0.05, 1.0);

    private final HudNotifierRuntime runtime = new HudNotifierRuntime(this);
    private final HudNotifierPainter painter = new HudNotifierPainter(this, runtime);

    private HudNotifier() {
        super("hud_notifier", "HudNotifier", true);
        ModuleManager.addListener(this);
    }

    public static void pushMessage(String text) {
        INSTANCE.runtime.enqueue(null, text, null, null);
    }

    public static void pushMessage(String text, NotifyType type) {
        INSTANCE.runtime.enqueue(null, text, null, type);
    }

    public static void pushState(String text, boolean enabled) {
        INSTANCE.runtime.enqueue(null, text, enabled, null);
    }

    public static void pushOrUpdateMessage(String key, String text) {
        INSTANCE.runtime.enqueue(key, text, null, null);
    }

    public static void pushOrUpdateMessage(String key, String text, NotifyType type) {
        INSTANCE.runtime.enqueue(key, text, null, type);
    }

    public static void clearMessage(String key) {
        INSTANCE.runtime.clearKey(key);
    }

    public static void renderEngine(HudPhase phase, Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta) {
        INSTANCE.painter.renderEngine(renderer, textRenderer);
    }

    public static void renderPreview(Renderer2D renderer, TextRenderer textRenderer, float x, float y, float w, float h) {
        INSTANCE.painter.renderPreview(renderer, textRenderer, x, y, w, h);
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
        if (hud != null) scale *= hud.getFontSize() / 18f;
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
    public void renderEngine(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        painter.renderEngine(renderer, textRenderer);
    }

    @Override
    public void onModuleStateChanged(String name, boolean enabled) {
        Module module = ModuleManager.get(name);
        // HudNotifier module-state info logging disabled; keep only warnings/errors here.
        if (module == null) return;
        if (!module.isShownInModuleList()) return;
        runtime.enqueueModuleState(module.getDisplayName(), enabled);
    }

    @Override
    public void collectRenderPrewarm(RenderPrewarmCollectEvent event) {
        super.collectRenderPrewarm(event);
        event.svg("toggle-left")
                .svg("toggle-right")
                .svg("toggle-track")
                .svg("toggle-thumb");
    }

    /** Package-private bounds passthrough so {@link HudNotifierPainter} can update the module hitbox. */
    void applyBounds(float nx, float ny, float nw, float nh) {
        setBounds(nx, ny, nw, nh);
    }

    int getDurationMs() {
        return durationMs.get();
    }

    int getFadeMs() {
        return fadeMs.get();
    }

    float getScale() {
        return scaleValue.get().floatValue();
    }

    /** Resolve which edge of the screen the toast stack slides from, based on the current module position. */
    MessengerSide getSide() {
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

    /** Public accessors used by {@link HudNotifierRuntime} (in a sibling subpackage). */
    public boolean isSoundEnabled() {
        return soundEnabled.get();
    }

    public ModeValue getSoundMode() {
        return soundMode;
    }

    public float getSoundVolume() {
        return soundVolume.get().floatValue();
    }

    /** Notification severity, resolved from the raw text or forced by the caller. */
    public enum NotifyType {
        YES,
        NO,
        WARN,
        INFO
    }
}
