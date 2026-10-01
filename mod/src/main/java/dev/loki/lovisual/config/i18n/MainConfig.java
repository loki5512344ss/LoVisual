package dev.loki.lovisual.config.i18n;

import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.collection.SetValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.render.iris.runtime.IrisRuntime;
import dev.loki.lovisual.util.logging.DebugLog;
import dev.loki.lovisual.util.logging.DebugMode;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import dev.loki.lovisual.config.definition.ConfigNameProvider;
import dev.loki.lovisual.config.definition.ConfigObject;
import dev.loki.lovisual.config.definition.SettingDef;
import dev.loki.lovisual.config.definition.SettingOwner;
import dev.loki.lovisual.config.io.ConfigSerializer;

public final class MainConfig implements ConfigObject, ConfigNameProvider, SettingOwner {

    public static final MainConfig INSTANCE = new MainConfig();
    private static final String IRIS_MSAA_REASON_KEY = "setting.main_config.msaa3d.iris_blocked";
    private static final String IRIS_MSAA_REASON_FALLBACK = "Iris shaderpack pipeline is active.";

    //@CFGComment("Debug logging mode: off / error_only / error_and_warnings / info / config / render_thread / stencil / serverdebug / all")
    private final ModeValue debug =
            new ModeValue("debug", "off", "off", "error_only", "error_and_warnings", "info", "config", "render_thread", "stencil", "serverdebug", "all");

    //@CFGComment("Menu background shader: off / waves / aurora")
    private final ModeValue menuBg =
            new ModeValue("menuBg", "waves", "off", "waves", "aurora");

    //@CFGComment("Main menu layout: orbs / hero")
    private final ModeValue menuLayout =
            new ModeValue("menuLayout", "orbs", "orbs", "hero");

    //@CFGComment("Use ClickGUI theme colors for menu background shader")
    private final BooleanValue menuBgUseTheme =
            new BooleanValue("menuBgUseTheme", true);

    //@CFGComment("Show seconds in the custom main menu clock")
    private final BooleanValue menuClockShowSeconds =
            new BooleanValue("menuClockShowSeconds", true);

    //@CFGComment("MSAA for world render: off / 2x / 4x")
    private final ModeValue msaa3d =
            new ModeValue("msaa3d", "off", "off", "2x", "4x");

    //@CFGComment("Show ClickGUI modules hotkey hints")
    private final BooleanValue clickGuiModulesHints =
            new BooleanValue("clickGuiModulesHints", true);

    //@CFGComment("Show ClickGUI HUD editor hotkey hints")
    private final BooleanValue clickGuiHudEditorHints =
            new BooleanValue("clickGuiHudEditorHints", true);

    //@CFGComment("Hard-disable narrator and narrator hotkey")
    private final BooleanValue disableNarrator =
            new BooleanValue("disableNarrator", true);

    //@CFGComment("Always-on backdoor protection master switch")
    private final BooleanValue backdoorProtection =
            new BooleanValue("backdoorProtection", true);

    //@CFGComment("Filter non-whitelisted translation keys in server-controlled sign/anvil text")
    private final BooleanValue backdoorTranslationFilter =
            new BooleanValue("backdoorTranslationFilter", true);

    //@CFGComment("Filter non-vanilla keybind text keys in server-controlled text")
    private final BooleanValue backdoorKeybindFilter =
            new BooleanValue("backdoorKeybindFilter", true);

    //@CFGComment("Block remote servers from redirecting resource-pack downloads into local/private network addresses")
    private final BooleanValue backdoorLocalHttpGuard =
            new BooleanValue("backdoorLocalHttpGuard", true);

    //@CFGComment("Isolate downloaded server-pack cache by account UUID to reduce pack-cache fingerprinting")
    private final BooleanValue backdoorPackCacheIsolation =
            new BooleanValue("backdoorPackCacheIsolation", true);

    //@CFGComment("Allow local/private HTTP requests when connected to local/integrated server")
    private final BooleanValue backdoorAllowLocalHttpWhenServerLocal =
            new BooleanValue("backdoorAllowLocalHttpWhenServerLocal", true);

    //@CFGComment("Extra allowed translation resource-pack ids for server-controlled text")
    private final SetValue backdoorAllowedTranslationPacks =
            new SetValue("backdoorAllowedTranslationPacks", defaultAllowedTranslationPacks());

    //@CFGComment("Extra allowed keybind ids for server-controlled text")
    private final SetValue backdoorAllowedKeybinds =
            new SetValue("backdoorAllowedKeybinds");

    private MainConfig() {
        ConfigSerializer.load(this);
        applyDebugMode();
    }

    public static MainConfig get() {
        return INSTANCE;
    }

    private static Set<String> defaultAllowedTranslationPacks() {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add("vanilla");
        return out;
    }

    @Override
    public String getConfigName() {
        return "mainconfig";
    }

    @Override
    public String name() {
        return "main_config";
    }

    // ================= DEBUG =================

    @Override
    public void saveConfig() {
        applyDebugMode();
        ConfigSerializer.requestSave(this);
    }

    public DebugMode getDebugMode() {
        try {
            return DebugMode.valueOf(debug.get().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DebugMode.OFF;
        }
    }

    public void setDebugMode(DebugMode mode) {
        if (mode == null) mode = DebugMode.OFF;
        debug.set(mode.name().toLowerCase());
        applyDebugMode();
        ConfigSerializer.requestSave(this);
    }

    private void applyDebugMode() {
        DebugLog.setMode(getDebugMode());
    }

    public String getMenuBackgroundMode() {
        return menuBg.get();
    }

    public String getMenuLayout() {
        return menuLayout.get();
    }

    public boolean isMenuBackgroundUseTheme() {
        return menuBgUseTheme.get();
    }

    public boolean isMenuClockShowSeconds() {
        return menuClockShowSeconds.get();
    }

    public int getMsaa3dSamples() {
        if (IrisRuntime.isShaderpackRendererActive()) return 0;
        String v = msaa3d.get();
        if (v == null) return 0;
        if (v.equalsIgnoreCase("2x")) return 2;
        if (v.equalsIgnoreCase("4x")) return 4;
        return 0;
    }

    public boolean isClickGuiModulesHintsEnabled() {
        return clickGuiModulesHints.get();
    }

    public boolean isClickGuiHintsEnabled() {
        return clickGuiModulesHints.get();
    }

    public void setClickGuiModulesHintsEnabled(boolean enabled) {
        clickGuiModulesHints.set(enabled);
        ConfigSerializer.requestSave(this);
    }

    public void setClickGuiHintsEnabled(boolean enabled) {
        setClickGuiModulesHintsEnabled(enabled);
    }

    public boolean isClickGuiHudEditorHintsEnabled() {
        return clickGuiHudEditorHints.get();
    }

    public void setClickGuiHudEditorHintsEnabled(boolean enabled) {
        clickGuiHudEditorHints.set(enabled);
        ConfigSerializer.requestSave(this);
    }

    public List<SettingDef> getImageSettingDefs() {
        List<SettingDef> list = new ArrayList<>();
        list.add(SettingDef.mode(msaa3d)
                .unavailableWhen(IrisRuntime::isShaderpackRendererActive, MainConfig::irisMsaaReason));
        return list;
    }

    /** Everything about the main menu in one group: layout, background, theme colors, clock. */
    public List<SettingDef> getMainMenuSettingDefs() {
        List<SettingDef> list = new ArrayList<>();
        list.add(SettingDef.mode(menuLayout));
        list.add(SettingDef.mode(menuBg));
        list.add(SettingDef.bool(menuBgUseTheme).visibleWhen(() -> !"off".equalsIgnoreCase(menuBg.get())));
        list.add(SettingDef.bool(menuClockShowSeconds).visibleWhen(() -> !"hero".equalsIgnoreCase(menuLayout.get())));
        return list;
    }

    private static String irisMsaaReason() {
        String translated = I18n.get(IRIS_MSAA_REASON_KEY);
        return IRIS_MSAA_REASON_KEY.equals(translated) ? IRIS_MSAA_REASON_FALLBACK : translated;
    }

    public List<SettingDef> getMiscellaneousSettingDefs() {
        List<SettingDef> list = new ArrayList<>();
        list.add(SettingDef.mode(debug));
        list.add(SettingDef.bool(disableNarrator));
        return list;
    }

    public List<SettingDef> getSecuritySettingDefs() {
        List<SettingDef> list = new ArrayList<>();
        list.add(SettingDef.bool(backdoorProtection));
        list.add(SettingDef.bool(backdoorTranslationFilter).visibleWhen(backdoorProtection::get));
        list.add(SettingDef.bool(backdoorKeybindFilter).visibleWhen(backdoorProtection::get));
        list.add(SettingDef.bool(backdoorLocalHttpGuard).visibleWhen(backdoorProtection::get));
        list.add(SettingDef.bool(backdoorPackCacheIsolation).visibleWhen(backdoorProtection::get));
        list.add(SettingDef.bool(backdoorAllowLocalHttpWhenServerLocal).visibleWhen(backdoorProtection::get));
        list.add(SettingDef.textList(backdoorAllowedTranslationPacks)
                .visibleWhen(() -> backdoorProtection.get() && backdoorTranslationFilter.get()));
        list.add(SettingDef.textList(backdoorAllowedKeybinds)
                .visibleWhen(() -> backdoorProtection.get() && backdoorKeybindFilter.get()));
        return list;
    }

    public List<SettingDef> getUtilitySettingDefs() {
        return new ArrayList<>();
    }

    public boolean isNarratorDisabled() {
        return disableNarrator.get();
    }

    public boolean isBackdoorProtectionEnabled() {
        return backdoorProtection.get();
    }

    public boolean isBackdoorTranslationFilterEnabled() {
        return backdoorProtection.get() && backdoorTranslationFilter.get();
    }

    public boolean isBackdoorKeybindFilterEnabled() {
        return backdoorProtection.get() && backdoorKeybindFilter.get();
    }

    public boolean isBackdoorLocalHttpGuardEnabled() {
        return backdoorProtection.get() && backdoorLocalHttpGuard.get();
    }

    public boolean isBackdoorPackCacheIsolationEnabled() {
        return backdoorProtection.get() && backdoorPackCacheIsolation.get();
    }

    public boolean isBackdoorAllowLocalHttpWhenServerLocal() {
        return backdoorAllowLocalHttpWhenServerLocal.get();
    }

    public Set<String> getBackdoorAllowedTranslationPacks() {
        return new LinkedHashSet<>(backdoorAllowedTranslationPacks.get());
    }

    // ================= CONFIG =================

    public Set<String> getBackdoorAllowedKeybinds() {
        return new LinkedHashSet<>(backdoorAllowedKeybinds.get());
    }

    @Override
    public List<ConfigValue<?>> getConfigValues() {
        List<ConfigValue<?>> list = new ArrayList<>();
        list.add(debug);
        list.add(menuBg);
        list.add(menuLayout);
        list.add(menuClockShowSeconds);
        list.add(menuBgUseTheme);
        list.add(msaa3d);
        list.add(clickGuiModulesHints);
        list.add(clickGuiHudEditorHints);
        list.add(disableNarrator);
        list.add(backdoorProtection);
        list.add(backdoorTranslationFilter);
        list.add(backdoorKeybindFilter);
        list.add(backdoorLocalHttpGuard);
        list.add(backdoorPackCacheIsolation);
        list.add(backdoorAllowLocalHttpWhenServerLocal);
        list.add(backdoorAllowedTranslationPacks);
        list.add(backdoorAllowedKeybinds);
        return list;
    }

}
