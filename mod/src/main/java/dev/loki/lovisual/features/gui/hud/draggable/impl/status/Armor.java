package dev.loki.lovisual.features.gui.hud.draggable.impl.status;

import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import dev.loki.lovisual.features.gui.hud.registry.HudElementInfo;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

//todo Description
@HudElementInfo(
        id = "armor",
        displayName = "Armor",
        enabledByDefault = false,
        order = 210
)
public final class Armor extends DraggableHudElement {

    {
        defaultLayout(1616.315f, 1003.6977f);
    }

    static final int SLOTS = 4;
    static final float DEFAULT_SLOT_SIZE = 18f;
    static final float DEFAULT_SLOT_GAP = 2f;
    static final float PANEL_RADIUS = 4.0f;
    static final float GLASS_BLUR_STRENGTH = 1.0f;
    static final float GLASS_ALPHA = 1.0f;
    static final float DEFAULT_MARGIN = 16f;
    static final float DIVIDER_THICKNESS = 0.5f;

    final HudGlobalConfig hud = HudGlobalConfig.get();
    final NumberValue<Double> scaleValue = num("armor_scale", 3.68, 0.5, 5.0);
    final EnumValue<LayoutMode> layoutMode =
            enumSetting("armor_layout", LayoutMode.HORIZONTAL, LayoutMode.VERTICAL, LayoutMode.HORIZONTAL);
    final BooleanValue reverse = bool("armor_reverse", false);
    final EnumValue<StateMode> stateMode =
            enumSetting("armor_state_mode", StateMode.TEXT, StateMode.BAR, StateMode.TEXT);
    final NumberValue<Integer> stateBarThreshold =
            visibleWhen(num("armor_state_bar_threshold", 70, 0, 100), () -> stateMode.get() == StateMode.BAR);
    final NumberValue<Integer> stateThreshold =
            visibleWhen(num("armor_state_threshold", 70, 0, 100), () -> stateMode.get() == StateMode.TEXT);
    final NumberValue<Integer> stateColorThreshold =
            visibleWhen(num("armor_state_color_threshold", 70, 0, 100), () -> stateMode.get() == StateMode.TEXT);
    float baseX;
    float baseY;
    float slotSize;
    float slotGap;
    float slotRadius;
    float itemScale;
    boolean layoutReady;
    net.minecraft.world.item.ItemStack[] cachedStacks;
    float renderX;
    float renderY;

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        float scale = HudScale.scale(screenW, screenH) * scaleValue.get().floatValue();
        float size = DEFAULT_SLOT_SIZE * scale;
        float gap = DEFAULT_SLOT_GAP * scale;
        boolean horizontal = isHorizontal();
        float w = horizontal ? SLOTS * size + (SLOTS - 1) * gap : size;
        float h = horizontal ? size : SLOTS * size + (SLOTS - 1) * gap;
        if (horizontal) {
            this.x = screenW * 0.5f - w * 0.5f;
            this.y = screenH - h - DEFAULT_MARGIN * scale;
        } else {
            this.x = screenW - w - DEFAULT_MARGIN * scale;
            this.y = screenH * 0.5f - h * 0.5f;
        }
    }

    @Override
    public boolean usesEngineRenderer() {
        return true;
    }

    @Override
    public void renderEngine(Renderer2D renderer,
                             TextRenderer textRenderer,
                             GuiGraphicsExtractor ctx,
                             float tickDelta,
                             int screenW,
                             int screenH) {
        ArmorRenderer.render(this, renderer, textRenderer, ctx, tickDelta, screenW, screenH);
    }

    @Override
    public void renderEngineForeground(Renderer2D renderer,
                                       TextRenderer textRenderer,
                                       GuiGraphicsExtractor ctx,
                                       float tickDelta,
                                       int screenW,
                                       int screenH) {
        ArmorRenderer.renderForeground(this, renderer, textRenderer, ctx, tickDelta, screenW, screenH);
    }

    boolean isHorizontal() {
        return layoutMode.get() == LayoutMode.HORIZONTAL;
    }

    boolean isReverseOrder() {
        return reverse.get();
    }

    void setArmorSize(float w, float h) {
        setBounds(getX(), getY(), w, h);
    }

    void setEmptySize() {
        setBounds(getX(), getY(), 0f, 0f);
    }

    @Override
    public boolean contains(float mx, float my) {
        return mx >= renderX && mx <= renderX + width && my >= renderY && my <= renderY + height;
    }

    enum LayoutMode implements EnumValue.IdProvider {
        VERTICAL("Vertical"),
        HORIZONTAL("Horizontal");

        private final String id;

        LayoutMode(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    enum StateMode implements EnumValue.IdProvider {
        BAR("Bar"),
        TEXT("Text");

        private final String id;

        StateMode(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

}
