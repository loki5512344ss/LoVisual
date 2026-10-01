package dev.loki.lovisual.features.gui.hud.base;

import dev.loki.lovisual.config.definition.ConfigNameProvider;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.definition.SettingOwner;
import dev.loki.lovisual.config.io.ConfigSerializer;
import dev.loki.lovisual.config.io.JsonConfigObject;
import dev.loki.lovisual.config.io.SettingDefProvider;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.collection.SetValue;
import dev.loki.lovisual.config.values.color.KeyBindValue;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.BindMode;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.events.impl.render.RenderPrewarmCollectEvent;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;
import dev.loki.lovisual.features.gui.hud.anchor.HudRenderSpace;
import dev.loki.lovisual.features.gui.hud.registry.HudElementInfo;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Common base for every HUD element: owns identity, the enabled flag, screen bounds, the
 * settings-definition lifecycle and the protected config-value DSL used by subclasses inside
 * {@link #defineSettings(List)}. Def storage lives in {@link HudSettingRegistry} and id/title
 * naming in {@link HudElementNamer}; this class exposes only the stable subclass-facing API.
 */
public abstract class BaseHudElement implements JsonConfigObject, ConfigNameProvider, SettingOwner, SettingDefProvider {

    private final String id;
    private final String title;
    private final BooleanValue enabled;
    private final HudSettingRegistry settings = new HudSettingRegistry();
    protected float x;
    protected float y;
    protected float width;
    protected float height;
    private boolean settingsInitialized = false;
    private boolean previewEnabled = false;

    protected BaseHudElement() {
        HudElementInfo info = getClass().getAnnotation(HudElementInfo.class);
        if (info == null) throw new IllegalStateException(getClass().getName() + " must be annotated with @HudElementInfo");
        this.id = info.id();
        this.title = info.displayName();
        this.enabled = new BooleanValue("enabled", info.enabledByDefault());
    }

    protected BaseHudElement(String id, String title, boolean defaultEnabled) {
        this.id = id;
        this.title = title;
        this.enabled = new BooleanValue("enabled", defaultEnabled);
    }

    /** Populate this element's settings; called once, lazily, via {@link #ensureSettingsInitialized()}. */
    protected void defineSettings(List<SettingDef> defs) { }

    /** Hook invoked after the persisted config has been loaded. */
    protected void onLoaded() { }

    /** Hook to contribute extra (non-def) config values to the serialization map. */
    protected void collectExtraConfigValues(Map<String, ConfigValue<?>> map) { }

    // ---- protected config-value DSL (thin delegators to HudSettingRegistry) ------------------
    protected final BooleanValue bool(String c, boolean def) { return bool(c, settingIdFromConfigName(c), def); }
    protected final BooleanValue bool(String c, String settingId, boolean def) { return settings.bool(c, settingId, def); }
    protected final NumberValue<Integer> num(String c, int def, int min, int max) { return num(c, settingIdFromConfigName(c), def, min, max); }
    protected final NumberValue<Integer> num(String c, String settingId, int def, int min, int max) { return settings.numInt(c, settingId, def, min, max); }
    protected final NumberValue<Float> num(String c, float def, float min, float max) { return num(c, settingIdFromConfigName(c), def, min, max); }
    protected final NumberValue<Float> num(String c, String settingId, float def, float min, float max) { return settings.numFloat(c, settingId, def, min, max); }
    protected final NumberValue<Double> num(String c, double def, double min, double max) { return num(c, settingIdFromConfigName(c), def, min, max); }
    protected final NumberValue<Double> num(String c, String settingId, double def, double min, double max) { return settings.numDouble(c, settingId, def, min, max); }
    protected final ModeValue mode(String c, String def, String... options) { return mode(c, settingIdFromConfigName(c), def, options); }
    protected final ModeValue mode(String c, String settingId, String def, String... options) { return settings.mode(c, settingId, def, options); }
    protected final <E extends Enum<E>> EnumValue<E> enumSetting(String c, E def) { return enumSetting(c, settingIdFromConfigName(c), def); }
    protected final <E extends Enum<E>> EnumValue<E> enumSetting(String c, String settingId, E def) { return settings.enumSetting(c, settingId, def); }
    protected final <E extends Enum<E>> EnumValue<E> enumSetting(String c, E def, Class<E> enumClass) { return enumSetting(c, settingIdFromConfigName(c), def, enumClass); }
    protected final <E extends Enum<E>> EnumValue<E> enumSetting(String c, String settingId, E def, Class<E> enumClass) { return settings.enumSetting(c, settingId, def, enumClass); }
    @SafeVarargs
    protected final <E extends Enum<E>> EnumValue<E> enumSetting(String c, E def, E... options) { return enumSetting(c, settingIdFromConfigName(c), def, options); }
    @SafeVarargs
    protected final <E extends Enum<E>> EnumValue<E> enumSetting(String c, String settingId, E def, E... options) { return settings.enumSetting(c, settingId, def, options); }
    protected final RGBAColorValue color(String c, String defHex) { return color(c, settingIdFromConfigName(c), defHex); }
    protected final RGBAColorValue color(String c, String settingId, String defHex) { return settings.color(c, settingId, defHex); }
    protected final RGBColorValue colorNoAlpha(String c, String defHex) { return colorNoAlpha(c, settingIdFromConfigName(c), defHex); }
    protected final RGBColorValue colorNoAlpha(String c, String settingId, String defHex) { return settings.colorNoAlpha(c, settingId, defHex); }
    protected final SetValue textList(String c) { return textList(c, settingIdFromConfigName(c), TextListSetting.PickerMode.ALL); }
    protected final SetValue textList(String c, String settingId, TextListSetting.PickerMode pickerMode) { return settings.textList(c, settingId, pickerMode); }
    protected final KeyBindValue bind(String c, String def, BindMode mode) { return bind(c, settingIdFromConfigName(c), def, mode); }
    protected final KeyBindValue bind(String c, String settingId, String def, BindMode mode) { return settings.bind(c, settingId, def, mode); }
    protected final BooleanMapValue group(String c, String settingId, Map<String, Boolean> defaults) { return settings.group(c, settingId, defaults); }
    protected final <V extends ConfigValue<?>> V visibleWhen(V value, Supplier<Boolean> supplier) { return settings.visibleWhen(value, supplier); }
    protected final <V extends ConfigValue<?>> V common(V value, String commonI18nKey) { return settings.common(value, commonI18nKey); }
    protected final SettingDef settingDef(ConfigValue<?> value) { return settings.settingDef(value); }
    protected final SettingDef enabledSettingDef() { return SettingDef.bool(enabled); }

    /** Register a pre-built value/def pair directly (bypasses the typed builders). */
    protected final <V extends ConfigValue<?>> V declareSetting(V value, SettingDef def) {
        settings.declare(value, def);
        return value;
    }

    protected final void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    private String settingIdFromConfigName(String configName) {
        return HudElementNamer.settingId(configName, getId(), getClass().getSimpleName());
    }

    private void ensureSettingsInitialized() {
        if (settingsInitialized) return;
        settingsInitialized = true;
        defineSettings(settings.defs());
        SettingDef.applyI18nAnnotations(this, settings.defs());
    }

    // ---- lifecycle + render hooks (overridable defaults) --------------------------------------
    public void collectRenderPrewarm(RenderPrewarmCollectEvent event) { HudPrewarm.standard(event); }
    public void onTick() { }
    public void renderEngine(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) { }
    public void renderEngineForeground(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) { }
    public boolean usesEngineRenderer() { return false; }
    public boolean isMouseOverInteractive(float mx, float my) { return false; }
    public boolean onMouseClicked(float mx, float my, int button) { return false; }
    public HudPhase getHudPhase() { return HudPhase.AFTER_SUBTITLES; }
    public HudRenderSpace getRenderSpace() { return HudRenderSpace.UNSCALED_LOGICAL; }
    public int getRenderOrder() { return 0; }

    // ---- identity, enabled state + geometry ----------------------------------------------------
    public final void postInit() {
        ensureSettingsInitialized();
        ConfigSerializer.load(this);
        onLoaded();
    }

    public final String getId() { return id; }
    public final String getTitle() { return HudElementNamer.translateTitle(title, getTranslationKeyPrefix(), id); }
    /** Short settings-card description from {@code hud.<id>.description}; empty when none is translated. */
    public final String getDescription() { return HudElementNamer.translateDescription(id); }
    public boolean isEnabled() { return enabled.get() || previewEnabled; }
    public void setRuntimeEnabledTransient(boolean value) { enabled.set(value); }
    public void setPreviewEnabled(boolean value) { previewEnabled = value; }
    public BooleanValue enabledValue() { return enabled; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public void setEnabled(boolean value) {
        enabled.set(value);
        ConfigSerializer.requestSave(this);
    }
    public void moveTo(float x, float y) {
        this.x = x;
        this.y = y;
    }
    public boolean contains(float mx, float my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    // ---- config-object contract ---------------------------------------------------------------
    @Override
    public final String name() { return id; }
    @Override
    public final void saveConfig() { ConfigSerializer.requestSave(this); }

    @Override
    public final List<SettingDef> getSettingDefs() {
        ensureSettingsInitialized();
        return new ArrayList<>(settings.defs());
    }

    @Override
    public List<ConfigValue<?>> getConfigValues() {
        ensureSettingsInitialized();
        Map<String, ConfigValue<?>> map = new LinkedHashMap<>();
        map.put(enabled.getName(), enabled);
        collectExtraConfigValues(map);
        for (SettingDef def : settings.defs()) {
            ConfigValue<?> v = def.value();
            if (v != null) map.put(v.getName(), v);
        }
        return new ArrayList<>(map.values());
    }
}
