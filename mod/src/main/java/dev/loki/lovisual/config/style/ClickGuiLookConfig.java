package dev.loki.lovisual.config.style;

import dev.loki.lovisual.config.definition.ConfigNameProvider;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.definition.SettingOwner;
import dev.loki.lovisual.config.io.ConfigSerializer;
import dev.loki.lovisual.config.io.JsonConfigObject;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;

import java.util.List;

/**
 * How the ClickGUI itself is painted: blur sharpness and strength of its panels.
 * Defaults match the values that used to be hard-coded; persisted as {@code clickgui_look.json}.
 */
public final class ClickGuiLookConfig implements JsonConfigObject, ConfigNameProvider, SettingOwner {

    public static final ClickGuiLookConfig INSTANCE = new ClickGuiLookConfig();

    private final NumberValue<Float> blurQuality = new NumberValue<>("blurQuality", 27.35f, 8f, 72f);
    private final NumberValue<Float> blurDensity = new NumberValue<>("blurDensity", 1.16f, 0.3f, 2f);
    private final NumberValue<Float> glassBlur = new NumberValue<>("glassBlur", 1.28f, 0.3f, 2f);

    private ClickGuiLookConfig() {
        ConfigSerializer.load(this);
    }

    public static ClickGuiLookConfig get() {
        return INSTANCE;
    }

    public float blurQuality() {
        return blurQuality.get();
    }

    public float blurDensity() {
        return blurDensity.get();
    }

    public float glassBlur() {
        return glassBlur.get();
    }

    public List<SettingDef> getSettingDefs() {
        return List.of(SettingDef.number(blurQuality), SettingDef.number(blurDensity), SettingDef.number(glassBlur));
    }

    @Override
    public String getConfigName() {
        return "clickgui_look";
    }

    @Override
    public String name() {
        return "clickgui_look";
    }

    @Override
    public void saveConfig() {
        ConfigSerializer.requestSave(this);
    }

    @Override
    public List<ConfigValue<?>> getConfigValues() {
        return List.of(blurQuality, blurDensity, glassBlur);
    }
}
