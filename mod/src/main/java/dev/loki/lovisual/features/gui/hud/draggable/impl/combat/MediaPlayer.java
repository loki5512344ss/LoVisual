package dev.loki.lovisual.features.gui.hud.draggable.impl.combat;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.events.impl.render.RenderPrewarmCollectEvent;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.media.session.MediaSessionService;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.List;

@HudElementRegister(order = 70)
public final class MediaPlayer extends DraggableHudElement {

    {
        defaultLayout(12.0f, 82.30222f);
    }

    final HudGlobalConfig hud = HudGlobalConfig.get();
    final MediaSessionService mediaService = MediaSessionService.get();
    final NumberValue<Double> scale = new NumberValue<>("media_player_scale", 1.51, 0.5, 5.0);
    final BooleanValue syncTheme = new BooleanValue("media_player_sync_theme", true);
    final RGBAColorValue bg = new RGBAColorValue("media_player_bg", "#60000000");
    final NumberValue<Integer> bgAlpha = new NumberValue<>("media_player_bg_alpha", 124, 0, 255);
    final RGBColorValue accent = new RGBColorValue("media_player_accent", "#FF2F2F");
    final RGBColorValue text = new RGBColorValue("media_player_text", "#FFFFFF");
    final ModeValue bgEffect = new ModeValue("media_player_bg_effect", "None", MediaPlayerConstants.EFFECT_NONE, MediaPlayerConstants.EFFECT_BLUR, MediaPlayerConstants.EFFECT_GLASS);
    final NumberValue<Integer> blurAlpha = new NumberValue<>("media_player_blur_alpha", 255, 0, 255);
    final BooleanValue gradient = new BooleanValue("media_player_gradient", true);
    float anim = 0f;
    MediaSessionService.Snapshot currentSnapshot = MediaSessionService.Snapshot.empty(MediaSessionService.isMediaAvailable());

    float playHover;
    float shuffleHover;
    float prevHover;
    float nextHover;
    float repeatHover;

    float shuffleX;
    float shuffleY;
    float playX;
    float playY;
    float prevX;
    float prevY;
    float nextX;
    float nextY;
    float repeatX;
    float repeatY;
    float buttonSize;
    int uiAccent;
    int uiAccentSoft;
    int uiText;
    int uiTextMuted;

    public MediaPlayer() {
        super("media_player", "Media Player", false);
    }

    public void onResourceReload() {
        mediaService.onResourceReload();
        currentSnapshot = MediaSessionService.Snapshot.empty(MediaSessionService.isMediaAvailable());
    }

    @Override
    protected void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.number(scale));
        defs.add(SettingDef.bool(syncTheme));
        defs.add(SettingDef.color(bg).visibleWhen(() -> !syncTheme.get() && !isGlassEffect()));
        defs.add(SettingDef.number(bgAlpha).visibleWhen(() -> syncTheme.get() && !isGlassEffect()));
        defs.add(SettingDef.colorNoAlpha(accent).visibleWhen(() -> !syncTheme.get()));
        defs.add(SettingDef.colorNoAlpha(text).visibleWhen(() -> !syncTheme.get()));
        defs.add(SettingDef.mode(bgEffect));
        defs.add(SettingDef.number(blurAlpha).visibleWhen(this::hasEffect));
        defs.add(SettingDef.bool(gradient).visibleWhen(() -> syncTheme.get() && !isGlassEffect()));
    }

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        float s = HudScale.scale(screenW, screenH) * scale.get().floatValue();
        x = 20f * s;
        y = 20f * s;
    }

    @Override
    public boolean usesEngineRenderer() {
        return true;
    }

    @Override
    public boolean isMouseOverInteractive(float mx, float my) {
        if (!(ClientScreen.current() instanceof ChatScreen)) return false;
        return MediaPlayerControls.isMouseOverInteractive(this, mx, my);
    }

    @Override
    public boolean onMouseClicked(float mx, float my, int button) {
        if (!(ClientScreen.current() instanceof ChatScreen)) return false;
        return MediaPlayerControls.handleClick(this, mx, my, button);
    }

    @Override
    public void renderEngine(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        boolean chatOpen = ClientScreen.current() instanceof ChatScreen;
        boolean preview = DraggableHudElementRegistry.isForceVisible();
        MediaSessionService.Snapshot snapshot = mediaService.snapshot();
        currentSnapshot = snapshot;
        boolean shouldShow = snapshot.hasSession() || chatOpen || preview;
        float target = shouldShow ? 1.0f : 0.0f;
        anim = MediaPlayerConstants.lerp(anim, target, 0.25f);
        if (!shouldShow && anim <= 0.02f) {
            width = 0f;
            height = 0f;
            MediaPlayerControls.updateButtons(this);
            return;
        }
        MediaPlayerRenderHelper.render(this, renderer, textRenderer, ctx, tickDelta, screenW, screenH);
    }

    void setSize(float w, float h) {
        width = w;
        height = h;
    }

    boolean hasEffect() {
        return !MediaPlayerConstants.EFFECT_NONE.equals(bgEffect.get());
    }

    boolean isBlurEffect() {
        return MediaPlayerConstants.EFFECT_BLUR.equals(bgEffect.get());
    }

    boolean isGlassEffect() {
        return MediaPlayerConstants.EFFECT_GLASS.equals(bgEffect.get());
    }

    int getBgAlpha() {
        if (syncTheme.get()) return bgAlpha.get();
        return (bg.getArgb() >>> 24) & 0xFF;
    }

    @Override
    public void collectRenderPrewarm(RenderPrewarmCollectEvent event) {
        super.collectRenderPrewarm(event);
        event.font("MediaPlayer", FontInfo.Type.Regular)
                .font("Inter", FontInfo.Type.Bold)
                .font("InterMedium", FontInfo.Type.Regular)
                .svg("repeat")
                .svg("repeat-1")
                .svg("repeat-off");
    }
}
