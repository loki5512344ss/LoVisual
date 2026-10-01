package dev.loki.lovisual.features.gui.hud.draggable.impl.voice;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.hud.config.HudBgStyles;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.impl.HudPanelLayoutModes;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedListHudPanel;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/** Draggable Simple Voice Chat panel: who is talking right now plus the state of your own microphone. */
@HudElementRegister(order = 52)
public final class VoiceChatHud extends DraggableHudElement {

    {
        defaultLayout(16.0f, 150.0f);
    }

    final HudGlobalConfig hud = HudGlobalConfig.get();
    final ScriptedListHudPanel panel = new ScriptedListHudPanel();
    final NumberValue<Double> scale = new NumberValue<>("voicechat_scale", 1.0, HudPanelLayoutModes.SCALE_MIN, HudPanelLayoutModes.SCALE_MAX);
    final ModeValue layoutMode = new ModeValue("voicechat_layout", "Split Header", HudPanelLayoutModes.SPLIT_HEADER, HudPanelLayoutModes.UNIFIED_DIVIDER);
    final ModeValue bgEffect = new ModeValue("voicechat_bg_effect", HudBgStyles.AUTO, HudBgStyles.NONE, HudBgStyles.BLUR, HudBgStyles.AUTO);
    final NumberValue<Integer> bgAlpha = new NumberValue<>("voicechat_bg_alpha", 131, 0, 255);
    final NumberValue<Integer> blurAlpha = new NumberValue<>("voicechat_blur_alpha", 255, 0, 255);
    final NumberValue<Integer> maxRows = new NumberValue<>("voicechat_max_rows", 6, 1, 12);
    final NumberValue<Integer> holdMs = new NumberValue<>("voicechat_hold_ms", 350, 100, 1500);
    final BooleanValue showSelf = new BooleanValue("voicechat_show_self", true);

    float displayWidth = -1.0f;
    float displayHeight = -1.0f;
    float visibilityAnim = 0.0f;

    public VoiceChatHud() {
        super("voicechat", "VoiceChat", false);
    }

    @Override
    protected void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.number(scale));
        defs.add(SettingDef.mode(layoutMode));
        defs.add(SettingDef.number(maxRows));
        defs.add(SettingDef.number(holdMs));
        defs.add(SettingDef.bool(showSelf));
        defs.add(SettingDef.number(bgAlpha));
        defs.add(SettingDef.mode(bgEffect));
        defs.add(SettingDef.number(blurAlpha).visibleWhen(() -> HudBgStyles.hasEffect(bgEffect.get())));
    }

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        this.x = 16.0f;
        this.y = 150.0f;
    }

    @Override
    public boolean usesEngineRenderer() {
        return true;
    }

    @Override
    public void renderEngine(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx,
                             float tickDelta, int screenW, int screenH) {
        VoiceChatHudRenderer.render(this, renderer, textRenderer, ctx, tickDelta, screenW, screenH);
    }

    void setSize(float w, float h) {
        setBounds(getX(), getY(), w, h);
    }
}
