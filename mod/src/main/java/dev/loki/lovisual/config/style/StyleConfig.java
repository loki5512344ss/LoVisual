package dev.loki.lovisual.config.style;

import dev.loki.lovisual.config.definition.ConfigNameProvider;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.definition.SettingOwner;
import dev.loki.lovisual.config.io.ConfigSerializer;
import dev.loki.lovisual.config.io.JsonConfigObject;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.mode.BindMode;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.color.KeyBindValue;
import dev.loki.lovisual.render.engine.renderer.style.SurfaceStyle;
import dev.loki.lovisual.util.input.KeyManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Global surface-style selection shared by every HUD widget and GUI panel.
 * Widgets resolve {@link SurfaceStyle#AUTO} against this config; persisted as
 * {@code config/lovisual/style.json} through {@link ConfigSerializer}.
 */
public final class StyleConfig implements JsonConfigObject, ConfigNameProvider, SettingOwner {

    public static final StyleConfig INSTANCE = new StyleConfig();

    private final EnumValue<SurfaceStyle> surfaceStyle =
            new EnumValue<>("surfaceStyle", SurfaceStyle.GLASS,
                    SurfaceStyle.GLASS, SurfaceStyle.BLUR, SurfaceStyle.FLAT,
                    SurfaceStyle.OUTLINE, SurfaceStyle.NONE);
    private final NumberValue<Integer> switchMs =
            new NumberValue<>("switchMs", 340, 0, 2000);
    private final BooleanValue animateSwitch =
            new BooleanValue("animateSwitch", true);
    private final KeyBindValue cycleStyleBind = new KeyBindValue("cycleStyle", "NONE");

    private StyleConfig() {
        ConfigSerializer.load(this);
        refreshCycleBind();
    }

    public static StyleConfig get() {
        return INSTANCE;
    }

    public SurfaceStyle style() {
        SurfaceStyle s = surfaceStyle.get();
        return s == null || s == SurfaceStyle.AUTO ? SurfaceStyle.GLASS : s;
    }

    public void setStyle(SurfaceStyle style) {
        if (style == null || !style.isOverrideable()) return;
        surfaceStyle.set(style);
        ConfigSerializer.requestSave(this);
    }

    public int switchMs() {
        Integer ms = switchMs.get();
        return ms == null ? 0 : Math.max(0, ms);
    }

    public boolean animateSwitch() {
        return Boolean.TRUE.equals(animateSwitch.get());
    }

    /** Binding id matches {@code FunctionBindSetting}: owner name + value name. */
    public String bindingName() {
        return name() + ":" + cycleStyleBind.getName();
    }

    /** Re-registers the cycle combo after load or after a UI rebinding. */
    public void refreshCycleBind() {
        KeyManager.unregisterAll(bindingName());
        if (!cycleStyleBind.isNone()) {
            KeyManager.registerCombo(bindingName(), cycleStyleBind.get());
        }
    }

    /** @return true when the cycle hotkey fired (caller should notify). */
    public boolean handleCycleKey() {
        if (cycleStyleBind.isNone()) return false;
        if (!KeyManager.wasPressed(bindingName())) return false;
        cycleStyle();
        return true;
    }

    /** Advances GLASS -> BLUR -> FLAT -> OUTLINE -> NONE -> GLASS. */
    public SurfaceStyle cycleStyle() {
        SurfaceStyle[] order = {
                SurfaceStyle.GLASS, SurfaceStyle.BLUR, SurfaceStyle.FLAT,
                SurfaceStyle.OUTLINE, SurfaceStyle.NONE
        };
        int idx = 0;
        while (idx < order.length && order[idx] != style()) idx++;
        SurfaceStyle next = order[(idx + 1) % order.length];
        setStyle(next);
        return next;
    }

    public List<SettingDef> getSettingDefs() {
        List<SettingDef> list = new ArrayList<>();
        list.add(SettingDef.mode(surfaceStyle));
        list.add(SettingDef.number(switchMs).visibleWhen(animateSwitch::get));
        list.add(SettingDef.bool(animateSwitch));
        list.add(SettingDef.bind(cycleStyleBind, BindMode.PRESS));
        return list;
    }

    @Override
    public String getConfigName() {
        return "style";
    }

    @Override
    public String name() {
        return "style";
    }

    @Override
    public void saveConfig() {
        ConfigSerializer.requestSave(this);
    }

    @Override
    public List<ConfigValue<?>> getConfigValues() {
        List<ConfigValue<?>> list = new ArrayList<>();
        list.add(surfaceStyle);
        list.add(switchMs);
        list.add(animateSwitch);
        list.add(cycleStyleBind);
        return list;
    }
}
