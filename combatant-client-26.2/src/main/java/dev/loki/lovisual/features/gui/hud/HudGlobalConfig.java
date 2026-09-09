/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud;

import dev.loki.lovisual.config.*;
import dev.loki.lovisual.config.values.ConfigValue;
import dev.loki.lovisual.config.values.EnumValue;
import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.render.engine.math.HudScale;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Global HUD config shared by all widgets.
 */
public final class HudGlobalConfig implements JsonConfigObject, ConfigNameProvider, SettingOwner, SettingDefProvider {

    public static final HudGlobalConfig INSTANCE = new HudGlobalConfig();
    private final EnumValue<HeaderMode> headerMode =
            new EnumValue<>("headers", HeaderMode.CHAT, HeaderMode.ALWAYS, HeaderMode.CHAT, HeaderMode.NEVER);
    private final NumberValue<Integer> blurRadius =
            new NumberValue<>("blur_radius", 12, 4, 72);
    private final List<SettingDef> settingDefs = new ArrayList<>();
    private final List<SettingDef> globalPanelDefs = new ArrayList<>();
    private boolean settingsInitialized = false;
    private HudGlobalConfig() {
        ensureSettingsInitialized();
        deleteLegacyGlobalFile();
    }

    private void deleteLegacyGlobalFile() {
        try {
            Files.deleteIfExists(ConfigPaths.root().resolve("hud").resolve("global.json"));
        } catch (IOException ignored) {
            // Best-effort cleanup only. Global HUD settings now live as code defaults.
        }
    }

    public static HudGlobalConfig get() {
        return INSTANCE;
    }

    public float getFontSize() {
        return HudScale.hudFontSize();
    }

    public HeaderMode getHeaderMode() {
        return headerMode.get();
    }

    public int getBlurRadius() {
        return blurRadius.get();
    }

    @Override
    public String name() {
        return "hud";
    }

    @Override
    public String getTranslationKeyPrefix() {
        return "setting.hud";
    }

    @Override
    public void saveConfig() {
        // Global HUD values are code defaults only; no hud/global.json is written.
    }

    @Override
    public String getConfigName() {
        return "hud";
    }

    @Override
    public List<SettingDef> getSettingDefs() {
        ensureSettingsInitialized();
        return new ArrayList<>(settingDefs);
    }

    public List<SettingDef> getGlobalPanelSettingDefs() {
        ensureSettingsInitialized();
        return new ArrayList<>(globalPanelDefs);
    }

    @Override
    public List<ConfigValue<?>> getConfigValues() {
        ensureSettingsInitialized();
        Map<String, ConfigValue<?>> map = new LinkedHashMap<>();
        map.put(headerMode.getName(), headerMode);
        map.put(blurRadius.getName(), blurRadius);
        return new ArrayList<>(map.values());
    }

    private void ensureSettingsInitialized() {
        if (settingsInitialized) return;
        settingsInitialized = true;
        SettingDef headersDef = SettingDef.mode(headerMode);

        settingDefs.add(headersDef);
        globalPanelDefs.add(headersDef);
        SettingDef.applyI18nAnnotations(this, settingDefs);
    }

    public enum HeaderMode implements EnumValue.IdProvider {
        ALWAYS("always"),
        CHAT("chat"),
        NEVER("never");

        private final String id;

        HeaderMode(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }
}
