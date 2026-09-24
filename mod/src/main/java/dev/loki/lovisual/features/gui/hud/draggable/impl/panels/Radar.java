package dev.loki.lovisual.features.gui.hud.draggable.impl.panels;

import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.hud.config.HudGlobalConfig;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * Player radar widget with a rectangular map and text list.
 */
@HudElementRegister(order = 140)
public final class Radar extends DraggableHudElement {

    {
        defaultLayout(1666.0f, 17.915222f);
    }

    static final float BASE_RADIUS = 6f;
    static final float BASE_LINE_W = 1.0f;
    static final float BASE_PAD = 3f;
    static final float BLIP_SIZE = 4f;
    static final float BLIP_MARGIN = 6f;
    static final float TEXT_SCALE_MULT = 0.6f;
    private static final String FIELD_KEY_PREFIX = "radar_field_";
    private static final Field[] FIELD_OPTIONS = {
            Field.NAME,
            Field.HP,
            Field.DISTANCE,
            Field.PING,
            Field.NONE
    };
    final HudGlobalConfig hud = HudGlobalConfig.get();
    final EnumValue<Mode> mode =
            new EnumValue<>("radar_mode", Mode.RECT, Mode.RECT, Mode.TEXT);
    final EnumValue<ColorMode> colorMode =
            new EnumValue<>("radar_color_mode", ColorMode.SYNC, ColorMode.SYNC, ColorMode.CUSTOM);
    final NumberValue<Double> size =
            new NumberValue<>("radar_size", 217.27, 20.0, 300.0);
    final RGBAColorValue bgColor =
            new RGBAColorValue("radar_bg_color", "#CC101010");
    final NumberValue<Integer> bgAlpha =
            new NumberValue<>("radar_bg_alpha", 109, 0, 255);
    final RGBColorValue textColor =
            new RGBColorValue("radar_text_color", "#FFFFFF");
    final EnumValue<Field> field1 = fieldSlot(1, Field.NAME);
    final EnumValue<Field> field2 = fieldSlot(2, Field.HP);
    final EnumValue<Field> field3 = fieldSlot(3, Field.PING);
    final EnumValue<Field> field4 = fieldSlot(4, Field.NONE);
    final EnumValue<Field> field5 = fieldSlot(5, Field.NONE);
    final BooleanValue blur =
            new BooleanValue("radar_blur", true);
    final NumberValue<Integer> blurAlpha =
            new NumberValue<>("radar_blur_alpha", 255, 0, 255);
    final BooleanValue gradient =
            new BooleanValue("radar_gradient", false);

    public Radar() {
        super("radar", "Radar", false);
    }

    private static EnumValue<Field> fieldSlot(int slot, Field def) {
        return new EnumValue<>(FIELD_KEY_PREFIX + slot, def, FIELD_OPTIONS);
    }

    @Override
    protected void defineSettings(List<SettingDef> defs) {
        defs.add(SettingDef.mode(mode));
        defs.add(SettingDef.mode(colorMode));
        defs.add(SettingDef.number(size));
        defs.add(SettingDef.color(bgColor)
                .visibleWhen(() -> colorMode.get() == ColorMode.CUSTOM));
        defs.add(SettingDef.number(bgAlpha)
                .visibleWhen(() -> colorMode.get() == ColorMode.SYNC));
        defs.add(SettingDef.colorNoAlpha(textColor)
                .visibleWhen(() -> mode.get() == Mode.TEXT
                        && colorMode.get() == ColorMode.CUSTOM));
        defs.add(SettingDef.mode(field1)
                .visibleWhen(() -> mode.get() == Mode.TEXT));
        defs.add(SettingDef.mode(field2)
                .visibleWhen(() -> mode.get() == Mode.TEXT));
        defs.add(SettingDef.mode(field3)
                .visibleWhen(() -> mode.get() == Mode.TEXT));
        defs.add(SettingDef.mode(field4)
                .visibleWhen(() -> mode.get() == Mode.TEXT));
        defs.add(SettingDef.mode(field5)
                .visibleWhen(() -> mode.get() == Mode.TEXT));
        defs.add(SettingDef.bool(blur));
        defs.add(SettingDef.number(blurAlpha).visibleWhen(blur::get));
        defs.add(SettingDef.bool(gradient)
                .visibleWhen(() -> colorMode.get() == ColorMode.SYNC));
    }

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        this.x = 120f;
        this.y = 120f;
    }

    @Override
    public boolean usesEngineRenderer() {
        return true;
    }

    void setSize(float w, float h) {
        setBounds(getX(), getY(), w, h);
    }

    void setEmpty() {
        setBounds(getX(), getY(), 0f, 0f);
    }

    @Override
    public void renderEngine(Renderer2D renderer,
                             TextRenderer textRenderer,
                             GuiGraphicsExtractor ctx,
                             float tickDelta,
                             int screenW,
                             int screenH) {
        Minecraft mc = Minecraft.getInstance();
        boolean preview = DraggableHudElementRegistry.isForceVisible();
        if (mc == null && !preview) {
            width = 0f;
            height = 0f;
            return;
        }
        if (!preview && !isEnabled()) {
            width = 0f;
            height = 0f;
            return;
        }

        if (mode.get() == Mode.TEXT) {
            RadarTextRenderer.renderText(this, textRenderer, screenW, screenH, preview);
        } else {
            RadarRectRenderer.renderRect(this, renderer, tickDelta, screenW, screenH);
        }
    }

    enum Mode implements EnumValue.IdProvider { RECT("Rect"), TEXT("Text"); private final String id; Mode(String id){this.id=id;} @Override public String id(){return id;} }
    enum ColorMode implements EnumValue.IdProvider { SYNC("Sync"), CUSTOM("Custom"); private final String id; ColorMode(String id){this.id=id;} @Override public String id(){return id;} }
    enum Field implements EnumValue.IdProvider { NAME("Name"), HP("Hp"), DISTANCE("Distance"), PING("Ping"), NONE("None"); private final String id; Field(String id){this.id=id;} @Override public String id(){return id;} }
}
